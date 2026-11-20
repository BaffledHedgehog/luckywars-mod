package me.skywars2.media.client;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.imageio.ImageIO;
import me.skywars2.media.MediaClip;
import me.skywars2.media.MediaPackets;

/** Tiny real JPEG and signed stereo PCM stream through the shared file format. */
public final class MediaClipChecks {
    private MediaClipChecks() { }

    public static void main(String[] args) throws IOException, InterruptedException {
        Path directory = Files.createTempDirectory("skywars-media-clip-check");
        try {
            var bytes = new ByteArrayOutputStream();
            var image = new BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB);
            image.setRGB(1, 2, 0x42b7ef);
            require(ImageIO.write(image, "jpeg", bytes), "test JPEG writer available");
            byte[] jpeg = bytes.toByteArray();
            byte[][] samples = {pcm((short) -128, (short) 1945), pcm((short) 32767, (short) -32768)};
            Path clip = directory.resolve("valid.lwm");
            int[] supplied = {0};
            MediaClip.write(clip, 2, 3, 2, number -> {
                require(number == supplied[0]++, "source called once per frame, in order");
                return new MediaClip.Frame(jpeg, samples[number]);
            });
            require(supplied[0] == 2, "exactly two frames requested");
            try (var reader = MediaClip.open(clip)) {
                require(reader.width() == 2 && reader.height() == 3 && reader.frameCount() == 2, "exact clip metadata");
                var first = reader.next();
                require(Arrays.equals(first.jpeg(), jpeg) && Arrays.equals(first.pcm(), samples[0]), "first JPEG and stereo samples");
                require(Arrays.equals(reader.next().pcm(), samples[1]) && reader.next() == null, "second audio and exact EOF");
                first.pcm()[0] ^= 1;
                reader.rewind();
                require(Arrays.equals(reader.next().pcm(), samples[0]), "rewind reads the original bytes");
            }

            byte[] valid = Files.readAllBytes(clip);
            for (int adjustment : new int[]{-1, 1}) {
                Path broken = directory.resolve("length-" + adjustment);
                Files.write(broken, Arrays.copyOf(valid, valid.length + adjustment));
                expectFailure(() -> {
                    try (var reader = MediaClip.open(broken)) {
                        while (reader.next() != null) { }
                    }
                }, "truncated and trailing clips are rejected");
            }

            Path failed = directory.resolve("failed.lwm");
            expectFailure(() -> MediaClip.write(failed, 2, 3, 2, number ->
                    new MediaClip.Frame(jpeg, number == 0 ? samples[0] : new byte[1])), "wrong audio size rejected");
            require(!Files.exists(failed), "writer removed its partial file");
            expectFailure(() -> MediaClip.write(failed, 3, 2, 1, number ->
                    new MediaClip.Frame(jpeg, samples[0])), "JPEG dimensions must match header");
            require(!Files.exists(failed), "invalid frame leaves no file");

            expectFailure(() -> MediaClip.write(clip, 2, 3, 1, number ->
                    new MediaClip.Frame(jpeg, samples[0])), "writer will not replace an existing clip");
            require(Arrays.equals(Files.readAllBytes(clip), valid), "existing clip retained byte for byte");
            longSegments(directory, jpeg);
            twoPreparedParts(directory);
        } finally {
            try (var paths = Files.walk(directory)) {
                for (var path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
        System.out.println("MediaClipChecks passed");
    }

    private static void longSegments(Path directory, byte[] jpeg) throws IOException {
        // Add a legal JPEG comment to produce a 5-minute stream above the old 64 MiB cap.
        var padded = ByteBuffer.allocate(jpeg.length + 2404);
        padded.put(jpeg, 0, 2).putShort((short) 0xfffe).putShort((short) 2402);
        padded.position(padded.position() + 2400);
        padded.put(jpeg, 2, jpeg.length - 2);
        byte[] image = padded.array();
        int total = MediaClip.MAX_FRAMES;
        int parts = MediaClip.segmentCount(total);
        require(parts == 150, "300 seconds at 20fps uses 150 parts");
        long cumulative = 0;
        Path path = directory.resolve("current.lwm");
        byte[] stereo = new byte[MediaClip.PCM_BYTES];
        var samples = ByteBuffer.wrap(stereo).order(ByteOrder.LITTLE_ENDIAN);
        for (long sequence = 0; sequence <= parts; sequence++) {
            int start = (int) (sequence % parts) * 40;
            int[] sourceNumber = {start};
            MediaClip.writeSegment(path, 2, 3, total, sequence, frame -> {
                require(frame == sourceNumber[0]++, "source access stays in the one exact segment interval");
                samples.putShort(0, (short) frame).putShort(2, (short) ~frame);
                return new MediaClip.Frame(image, stereo);
            });
            long size = Files.size(path);
            require(size <= MediaClip.MAX_SEGMENT_BYTES && sourceNumber[0] == start + 40, "one bounded 40-frame file");
            if (sequence < parts) cumulative += size;
            try (var reader = MediaClip.openSegment(path, 2, 3, total, sequence)) {
                for (int i = start; i < start + 40; i++) {
                    var frame = reader.next();
                    var pcm = ByteBuffer.wrap(frame.pcm()).order(ByteOrder.LITTLE_ENDIAN);
                    require(Arrays.equals(frame.jpeg(), image) && pcm.getShort() == i && pcm.getShort() == (short) ~i,
                            "unchanged JPEG and both stereo samples in every part, including the loop");
                }
                require(reader.next() == null, "each part has exact EOF");
            }
            Files.delete(path);
        }
        require(cumulative > MediaClip.MAX_BYTES, "real 5-minute JPEG/stereo clip exceeds previous whole-clip limit");
        require(MediaClip.framesInSegment(2, 81) == 1 && MediaClip.framesInSegment(3, 81) == 40,
                "partial last segment and wrapped first segment keep their exact frame counts");
        System.out.println("5-minute clip: " + cumulative + " combined bytes in " + parts + " bounded segments");
    }

    private static void twoPreparedParts(Path directory) throws IOException, InterruptedException {
        UUID identity = UUID.randomUUID();
        String url = "https://media.example.com/luckywars-media/part";
        var first = new MediaPackets.Segment(identity, 0, 81, 2, 3, url);
        var next = new MediaPackets.Segment(identity, 1, 81, 2, 3, url);
        var later = new MediaPackets.Segment(identity, 2, 81, 2, 3, url);
        Path[] temporaries = {Files.createFile(directory.resolve("current.part")), Files.createFile(directory.resolve("prefetch.part"))};
        int[] transfers = {0};
        java.util.function.Function<String, MediaSegmentQueue.Download> fetch = uri -> {
            require(uri.equals(url) && transfers[0] < 2, "only current and next opened");
            Path file = temporaries[transfers[0]++];
            return new MediaSegmentQueue.Download() {
                @Override public CompletionStage<Path> file() { return CompletableFuture.completedFuture(file); }
                @Override public void close() {
                    try { Files.delete(file); }
                    catch (IOException error) { throw new AssertionError("active temporary was not owned", error); }
                }
            };
        };
        try (var queue = new MediaSegmentQueue(first)) {
            require(queue.offer(first, fetch) && !queue.offer(first, fetch), "start only once");
            try (var current = queue.take()) {
                require(!queue.offer(next, fetch) && queue.expect(1) && queue.offer(next, fetch), "only requested prefetch accepted");
                require(!queue.expect(2) && !queue.offer(later, fetch), "third part never fetched");
                require(current.download().file().toCompletableFuture().join().equals(temporaries[0])
                                && Arrays.stream(temporaries).allMatch(Files::exists),
                        "prefetched and current temporaries stay resident until consumed or stopped");
                queue.close();
                require(Files.exists(temporaries[0]) && !Files.exists(temporaries[1])
                                && queue.take() == null && !queue.expect(2),
                        "Stop releases prefetched transfer and leaves the current reader to close safely");
            }
        }
        require(Arrays.stream(temporaries).noneMatch(Files::exists) && transfers[0] == 2, "no leftover transfer files");
    }

    private static byte[] pcm(short left, short right) {
        return ByteBuffer.allocate(MediaClip.PCM_BYTES).order(ByteOrder.LITTLE_ENDIAN)
                .putShort(left).putShort(right).array();
    }

    @FunctionalInterface
    private interface IoAction {
        void run() throws IOException;
    }

    private static void expectFailure(IoAction action, String description) {
        try {
            action.run();
        } catch (IOException expected) {
            return;
        }
        throw new AssertionError(description);
    }

    private static void require(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
