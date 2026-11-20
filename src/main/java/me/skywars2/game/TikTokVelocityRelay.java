package me.skywars2.game;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import me.skywars2.media.MediaClip;
import me.skywars2.media.MediaRelayEndpoint;

/** Stages the current phone clip for HTTPS playback without passing it through the game connection. */
final class TikTokVelocityRelay implements AutoCloseable {
    private static final int LEASE_SECONDS = 480;
    private static final int MAX_READY_SEGMENTS = 40;
    private static final long SEGMENT_CACHE_NANOS = TimeUnit.SECONDS.toNanos(3);
    private static final String CLIPS = "/luckywars-media/v1/";
    record Delivery(URI uri, boolean mp4) {}
    private record ClipFormat(String clipId, boolean mp4, boolean requireMp4) {}
    private record SegmentKey(TikTokMedia clip, int index) {}
    private record ReadySegment(long until, Delivery delivery) {}
    private record Publication(long deadline, CompletableFuture<Delivery> link) {}

    private final URI origin;
    private final String key;
    private final HttpClient http;
    private final LinkedHashMap<ClipFormat, Publication> publications = new LinkedHashMap<>();
    private final LinkedHashMap<SegmentKey, SegmentJob> segmentJobs = new LinkedHashMap<>();
    private final LinkedHashMap<SegmentKey, ReadySegment> readySegments = new LinkedHashMap<>();
    private boolean closed;
    private final ThreadPoolExecutor sender = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(20), task -> {
                var thread = new Thread(task, "skywars-tiktok-velocity");
                thread.setDaemon(true);
                thread.setPriority(Thread.MIN_PRIORITY);
                return thread;
            });

    private TikTokVelocityRelay(URI origin, String key) throws IOException, GeneralSecurityException {
        this.origin = origin;
        this.key = key;
        var builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8));
        try (var certificate = MediaRelayEndpoint.certificate(origin)) {
            if (certificate != null) {
                var anchors = KeyStore.getInstance(KeyStore.getDefaultType());
                anchors.load(null, null);
                anchors.setCertificateEntry("velocity", CertificateFactory.getInstance("X.509").generateCertificate(certificate));
                var trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trust.init(anchors);
                var context = SSLContext.getInstance("TLS");
                context.init(null, trust.getTrustManagers(), null);
                builder.sslContext(context);
            }
        }
        http = builder.build();
    }

    static TikTokVelocityRelay configured() {
        String endpoint = System.getenv("SKYWARS2_MEDIA_RELAY_URI");
        String key = System.getenv("SKYWARS2_MEDIA_RELAY_KEY");
        try {
            if (key == null) {
                var keyFile = Path.of("config", "skywars2", "tiktok-relay.key");
                if (!Files.isRegularFile(keyFile) || Files.size(keyFile) > 128) return null;
                key = Files.readString(keyFile).strip();
            }
            if (!key.matches("[0-9a-f]{64}")) return null;
            URI origin = endpoint == null ? MediaRelayEndpoint.VELOCITY : URI.create(endpoint);
            if (!"https".equalsIgnoreCase(origin.getScheme()) || origin.getHost() == null
                    || origin.getRawUserInfo() != null || origin.getRawQuery() != null || origin.getRawFragment() != null
                    || origin.getPort() == 0 || origin.getPort() > 65535
                    || !(origin.getRawPath().isEmpty() || origin.getRawPath().equals("/"))) return null;
            return new TikTokVelocityRelay(origin, key);
        } catch (IllegalArgumentException | IOException | GeneralSecurityException invalid) { return null; }
    }

    synchronized CompletableFuture<Delivery> prepare(TikTokMedia clip, boolean mp4, boolean requireMp4) {
        long now = Instant.now().getEpochSecond();
        publications.values().removeIf(item -> item.deadline - now < 45 || item.link.isCompletedExceptionally());
        var format = new ClipFormat(clip.id, mp4, requireMp4);
        var existing = publications.get(format);
        if (existing != null) return existing.link;
        if (publications.size() >= 20) publications.pollFirstEntry();
        long deadline = now + LEASE_SECONDS;
        var link = new CompletableFuture<Delivery>();
        try {
            sender.execute(() -> {
                try { link.complete(upload(clip, mp4, requireMp4, deadline)); }
                catch (InterruptedException canceled) { Thread.currentThread().interrupt();link.cancel(false); }
                catch (IOException | GeneralSecurityException | RuntimeException failure) { link.completeExceptionally(failure); }
            });
        } catch (RejectedExecutionException busy) { link.completeExceptionally(busy); }
        publications.put(format, new Publication(deadline, link));
        return link;
    }


    /** A subscriber owns only its receipt. The last canceled receipt also retires queued encoding. */
    final class SegmentLease implements AutoCloseable {
        final CompletableFuture<Delivery> link;
        private SegmentJob job;
        private SegmentLease(CompletableFuture<Delivery> link, SegmentJob job) {
            this.link = link;
            this.job = job;
        }
        @Override public void close() {
            SegmentJob canceled = null;
            synchronized (TikTokVelocityRelay.this) {
                if (job != null && --job.readers == 0 && segmentJobs.get(job.key) == job) canceled = cancelSegment(job);
                job = null;
            }
            if (canceled != null) canceled.link.cancel(false);
        }
    }

    synchronized SegmentLease prepareSegment(TikTokMedia clip, long sequence, Runnable pin, Runnable release) {
        if (closed) throw new RejectedExecutionException("relay_closed");
        long now = System.nanoTime();
        readySegments.values().removeIf(item -> now - item.until >= 0);
        var key = new SegmentKey(clip, MediaClip.segmentIndex(sequence, clip.frames));
        var ready = readySegments.get(key);
        if (ready != null) return new SegmentLease(CompletableFuture.completedFuture(ready.delivery), null);
        var job = segmentJobs.get(key);
        if (job == null) {
            job = new SegmentJob(key, release);
            pin.run();
            segmentJobs.put(key, job);
            try { sender.execute(job); }
            catch (RejectedExecutionException busy) {
                segmentJobs.remove(key);
                release.run();
                throw busy;
            }
        }
        job.readers++;
        return new SegmentLease(job.link, job);
    }

    /** Called under the relay lock. The running worker retains its pin until after its last disk read. */
    private SegmentJob cancelSegment(SegmentJob job) {
        segmentJobs.remove(job.key, job);
        job.canceled = true;
        if (job.runner == null) {
            sender.remove(job);
            job.release.run();
        } else job.runner.interrupt();
        return job;
    }

    private final class SegmentJob implements Runnable {
        final SegmentKey key;
        final Runnable release;
        final CompletableFuture<Delivery> link = new CompletableFuture<>();
        Thread runner;
        int readers;
        boolean canceled;
        SegmentJob(SegmentKey key, Runnable release) { this.key = key; this.release = release; }

        @Override public void run() {
            synchronized (TikTokVelocityRelay.this) {
                if (canceled) return;
                runner = Thread.currentThread();
            }
            Delivery delivery = null;
            Exception error = null;
            try { delivery = uploadSegment(key.clip, key.index); }
            catch (InterruptedException interruption) {
                Thread.currentThread().interrupt();
                error = interruption;
            }
            catch (IOException | GeneralSecurityException | RuntimeException failure) { error = failure; }
            finally {
                synchronized (TikTokVelocityRelay.this) {
                    runner = null;
                    segmentJobs.remove(key, this);
                    if (!canceled && delivery != null) {
                        while (readySegments.size() >= MAX_READY_SEGMENTS) readySegments.pollFirstEntry();
                        readySegments.put(key, new ReadySegment(System.nanoTime() + SEGMENT_CACHE_NANOS, delivery));
                    }
                }
                release.run();
            }
            if (delivery != null) link.complete(delivery);
            else link.completeExceptionally(error);
        }
    }

    private Delivery uploadSegment(TikTokMedia clip, int index)
            throws IOException, InterruptedException, GeneralSecurityException {
        var staging = Files.createTempDirectory(clip.directory, ".velocity-");
        var prepared = staging.resolve("part.clip");
        try {
            MediaClip.writeSegment(prepared, TikTokMedia.JPEG_WIDTH, TikTokMedia.JPEG_HEIGHT,
                    clip.frames, index, frame -> clientFrame(clip, frame));
            return new Delivery(publish(prepared, Instant.now().getEpochSecond() + LEASE_SECONDS), false);
        } finally {
            Files.deleteIfExists(prepared);
            Files.deleteIfExists(staging);
        }
    }

    private static MediaClip.Frame clientFrame(TikTokMedia clip, int index) throws IOException {
        var frame = clip.clientFrame(index);
        return new MediaClip.Frame(frame.jpeg(), frame.pcm());
    }

    private Delivery upload(TikTokMedia clip, boolean mp4, boolean requireMp4, long deadline)
            throws IOException, InterruptedException, GeneralSecurityException {
        var original = clip.directory.resolve("original.mp4");
        if (mp4 && Files.isRegularFile(original, LinkOption.NOFOLLOW_LINKS)) {
            long bytes = Files.size(original);
            if (bytes > 0 && bytes <= MediaClip.MAX_BYTES) return new Delivery(publish(original, deadline), true);
        }
        if (requireMp4) throw new IOException("relay_mp4_unavailable");
        var staging = Files.createTempDirectory(clip.directory, ".velocity-");
        var prepared = staging.resolve("video.clip");
        try {
            MediaClip.write(prepared, TikTokMedia.JPEG_WIDTH, TikTokMedia.JPEG_HEIGHT, clip.frames,
                    index -> clientFrame(clip, index));
            return new Delivery(publish(prepared, deadline), false);
        } finally {
            Files.deleteIfExists(prepared);
            Files.deleteIfExists(staging);
        }
    }

    private URI publish(Path file, long deadline) throws IOException, InterruptedException, GeneralSecurityException {
        var digest = MessageDigest.getInstance("SHA-256");
        try (var bytes = new DigestInputStream(Files.newInputStream(file), digest)) {
            bytes.transferTo(OutputStream.nullOutputStream());
        }
        String identity = HexFormat.of().formatHex(digest.digest());
        var request = HttpRequest.newBuilder(origin.resolve(CLIPS + identity)).timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + key)
                .PUT(HttpRequest.BodyPublishers.ofFile(file)).build();
        if (http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() != 201) {
            throw new IOException("relay_upload_failed");
        }
        var hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(key.getBytes(StandardCharsets.US_ASCII), "HmacSHA256"));
        String lease = identity + "/" + deadline;
        String signature = HexFormat.of().formatHex(hmac.doFinal(lease.getBytes(StandardCharsets.US_ASCII)));
        return origin.resolve(CLIPS + lease + "/" + signature);
    }

    @Override public void close() {
        ArrayList<SegmentJob> canceled = new ArrayList<>();
        ArrayList<Publication> old;
        synchronized (this) {
            closed = true;
            old = new ArrayList<>(publications.values());
            publications.clear();
            for (var job : new ArrayList<>(segmentJobs.values())) canceled.add(cancelSegment(job));
            readySegments.clear();
            sender.shutdownNow();
            http.shutdownNow();
        }
        for (var publication : old) publication.link.cancel(false);
        for (var job : canceled) job.link.cancel(false);
    }
}
