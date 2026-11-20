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
    private static final String CLIPS = "/luckywars-media/v1/";
    record Delivery(URI uri, boolean mp4) {}
    private record ClipFormat(String clipId, boolean mp4) {}
    private record Publication(long deadline, CompletableFuture<Delivery> link) {}

    private final URI origin;
    private final String key;
    private final HttpClient http;
    private final LinkedHashMap<ClipFormat, Publication> publications = new LinkedHashMap<>();
    private final ThreadPoolExecutor sender = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(6), task -> {
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

    synchronized CompletableFuture<Delivery> prepare(TikTokMedia clip, boolean mp4) {
        long now = Instant.now().getEpochSecond();
        publications.values().removeIf(item -> item.deadline - now < 45 || item.link.isCompletedExceptionally());
        var format = new ClipFormat(clip.id, mp4);
        var existing = publications.get(format);
        if (existing != null) return existing.link;
        if (publications.size() >= 20) publications.pollFirstEntry();
        long deadline = now + LEASE_SECONDS;
        var link = new CompletableFuture<Delivery>();
        try {
            sender.execute(() -> {
                try { link.complete(upload(clip, mp4, deadline)); }
                catch (InterruptedException canceled) { Thread.currentThread().interrupt();link.cancel(false); }
                catch (IOException | GeneralSecurityException | RuntimeException failure) { link.completeExceptionally(failure); }
            });
        } catch (RejectedExecutionException busy) { link.completeExceptionally(busy); }
        publications.put(format, new Publication(deadline, link));
        return link;
    }

    private Delivery upload(TikTokMedia clip, boolean mp4, long deadline)
            throws IOException, InterruptedException, GeneralSecurityException {
        var original = clip.directory.resolve("original.mp4");
        if (mp4 && Files.isRegularFile(original, LinkOption.NOFOLLOW_LINKS)) {
            long bytes = Files.size(original);
            if (bytes > 0 && bytes <= MediaClip.MAX_BYTES) return new Delivery(publish(original, deadline), true);
        }
        var staging = Files.createTempDirectory(clip.directory, ".velocity-");
        var prepared = staging.resolve("video.clip");
        try {
            MediaClip.write(prepared, TikTokMedia.JPEG_WIDTH, TikTokMedia.JPEG_HEIGHT, clip.frames, index -> {
                if (Thread.currentThread().isInterrupted()) throw new IOException("relay_interrupted");
                var frame = clip.clientFrame(index);
                return new MediaClip.Frame(frame.jpeg(), frame.pcm());
            });
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

    @Override public synchronized void close() {
        for (var publication : publications.values()) publication.link.cancel(false);
        publications.clear();
        sender.shutdownNow();
        http.shutdownNow();
    }
}
