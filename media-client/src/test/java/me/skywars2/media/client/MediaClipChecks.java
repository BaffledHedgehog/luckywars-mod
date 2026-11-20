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
import javax.imageio.ImageIO;
import me.skywars2.media.MediaClip;

/** Tiny real JPEG and signed stereo PCM stream through the shared file format. */
public final class MediaClipChecks {
    private MediaClipChecks() { }

    public static void main(String[] args) throws IOException {
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
        } finally {
            try (var paths = Files.walk(directory)) {
                for (var path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
        System.out.println("MediaClipChecks passed");
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
