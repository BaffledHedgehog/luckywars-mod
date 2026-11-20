package me.skywars2.media;

import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Local clip with a big-endian header and per-frame JPEG length. Each JPEG is followed
 * by one tick of signed 16-bit little-endian stereo PCM.
 */
public final class MediaClip {
    public static final long MAX_BYTES = 64L * 1024 * 1024;
    public static final int FPS = 20;
    public static final int RATE = 48_000;
    public static final int CHANNELS = 2;
    public static final int PCM_BYTES = RATE / FPS * CHANNELS * Short.BYTES;

    private static final int MAGIC = 0x4c574d31; // LWM1
    private static final int HEADER_BYTES = 7 * Integer.BYTES;
    public static final int MAX_FRAMES = 6_000;
    public static final int SEGMENT_FRAMES = 40;
    public static final long MAX_SEGMENT_BYTES = HEADER_BYTES
            + (long) SEGMENT_FRAMES * (Integer.BYTES + MediaPackets.MAX_JPEG + PCM_BYTES);
    private static final int MIN_JPEG = 4;

    private MediaClip() { }

    /**
     * The source may reuse its arrays after the write of a frame returns. A reader hands each
     * frame's arrays to its caller. PCM is signed, little-endian, and interleaved left/right.
     */
    public record Frame(byte[] jpeg, byte[] pcm) {
        public Frame {
            Objects.requireNonNull(jpeg);
            Objects.requireNonNull(pcm);
        }
    }

    @FunctionalInterface
    public interface FrameSource {
        Frame frame(int number) throws IOException;
    }

    public static final class SizeLimitException extends IOException {
        private SizeLimitException() {
            super("media_clip_size");
        }
    }

    /**
     * Writes one frame at a time to a new staging file. The caller can publish it after success.
     * If writing fails, the method removes its partial file and leaves any existing destination alone.
     */
    public static void write(Path destination, int width, int height, int frameCount, FrameSource source) throws IOException {
        validateHeader(width, height, frameCount);
        Objects.requireNonNull(source);

        boolean created = false;
        try (var file = FileChannel.open(destination, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            created = true;
            var header = ByteBuffer.allocate(HEADER_BYTES);
            header.putInt(MAGIC).putInt(width).putInt(height).putInt(frameCount)
                    .putInt(FPS).putInt(RATE).putInt(CHANNELS).flip();
            writeFully(file, header);

            var size = ByteBuffer.allocate(Integer.BYTES);
            long total = HEADER_BYTES;
            for (int number = 0; number < frameCount; number++) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("media_clip_interrupted");
                Frame frame = source.frame(number);
                if (frame == null || frame.pcm().length != PCM_BYTES
                        || !MediaPackets.jpegMatches(frame.jpeg(), width, height))
                    throw new IOException("media_clip_frame");

                total += Integer.BYTES + (long) frame.jpeg().length + PCM_BYTES;
                if (total > MAX_BYTES) throw new SizeLimitException();
                size.clear();
                size.putInt(frame.jpeg().length).flip();
                writeFully(file, size);
                writeFully(file, ByteBuffer.wrap(frame.jpeg()));
                writeFully(file, ByteBuffer.wrap(frame.pcm()));
            }
        } catch (IOException | RuntimeException | Error failure) {
            if (created) {
                try {
                    Files.deleteIfExists(destination);
                } catch (IOException cleanup) {
                    failure.addSuppressed(cleanup);
                }
            }
            throw failure;
        }
    }

    public static int segmentCount(int totalFrames) {
        if (totalFrames < 1 || totalFrames > MAX_FRAMES) throw new IllegalArgumentException("media_segment_frames");
        return (totalFrames + SEGMENT_FRAMES - 1) / SEGMENT_FRAMES;
    }

    public static int segmentIndex(long sequence, int totalFrames) {
        if (sequence < 0) throw new IllegalArgumentException("media_segment_sequence");
        return (int) (sequence % segmentCount(totalFrames));
    }

    public static int framesInSegment(long sequence, int totalFrames) {
        return Math.min(SEGMENT_FRAMES, totalFrames - segmentIndex(sequence, totalFrames) * SEGMENT_FRAMES);
    }

    public static void writeSegment(Path path, int width, int height, int totalFrames, long sequence, FrameSource source)
            throws IOException {
        int start = segmentIndex(sequence, totalFrames) * SEGMENT_FRAMES;
        write(path, width, height, framesInSegment(sequence, totalFrames), number -> source.frame(start + number));
    }

    public static Reader openSegment(Path path, int width, int height, int totalFrames, long sequence) throws IOException {
        int expected = framesInSegment(sequence, totalFrames);
        if (Files.size(path) > MAX_SEGMENT_BYTES) throw new SizeLimitException();
        var reader = open(path);
        if (reader.width() == width && reader.height() == height && reader.frameCount() == expected) return reader;
        reader.close();
        throw new IOException("media_segment_header");
    }

    public static Reader open(Path path) throws IOException {
        var file = FileChannel.open(path, StandardOpenOption.READ);
        try {
            return new Reader(file);
        } catch (IOException | RuntimeException | Error failure) {
            try {
                file.close();
            } catch (IOException cleanup) {
                failure.addSuppressed(cleanup);
            }
            throw failure;
        }
    }

    /** Sequential file reader for one playback worker. Rewind before replaying the next loop. */
    public static final class Reader implements AutoCloseable {
        private final FileChannel file;
        private final long fileBytes;
        private final int width;
        private final int height;
        private final int frameCount;
        private final ByteBuffer size = ByteBuffer.allocate(Integer.BYTES);
        private int framesRead;

        private Reader(FileChannel file) throws IOException {
            this.file = file;
            fileBytes = file.size();
            if (fileBytes > MAX_BYTES) throw new SizeLimitException();

            var header = ByteBuffer.allocate(HEADER_BYTES);
            readFully(file, header);
            header.flip();
            if (header.getInt() != MAGIC) throw new IOException("media_clip_magic");
            width = header.getInt();
            height = header.getInt();
            frameCount = header.getInt();
            validateHeader(width, height, frameCount);
            if (header.getInt() != FPS || header.getInt() != RATE || header.getInt() != CHANNELS)
                throw new IOException("media_clip_format");
            if (fileBytes < HEADER_BYTES + minimumFrameBytes(frameCount)) throw new EOFException("media_clip_truncated");
        }

        public int width() { return width; }
        public int height() { return height; }
        public int frameCount() { return frameCount; }

        /** Returns null after the last frame. Corrupt or incomplete files throw IOException. */
        public Frame next() throws IOException {
            if (framesRead == frameCount) return null;
            size.clear();
            readFully(file, size);
            size.flip();
            int jpegBytes = size.getInt();
            if (jpegBytes < MIN_JPEG || jpegBytes > MediaPackets.MAX_JPEG) throw new IOException("media_clip_frame");

            long end = file.position() + jpegBytes + PCM_BYTES;
            int remaining = frameCount - framesRead - 1;
            if (end + minimumFrameBytes(remaining) > fileBytes) throw new EOFException("media_clip_truncated");
            if (remaining == 0 && end != fileBytes) throw new IOException("media_clip_trailing_data");

            var jpeg = ByteBuffer.allocate(jpegBytes);
            readFully(file, jpeg);
            if (!MediaPackets.jpegMatches(jpeg.array(), width, height)) throw new IOException("media_clip_jpeg");
            var pcm = ByteBuffer.allocate(PCM_BYTES);
            readFully(file, pcm);
            framesRead++;
            return new Frame(jpeg.array(), pcm.array());
        }

        public void rewind() throws IOException {
            file.position(HEADER_BYTES);
            framesRead = 0;
        }

        @Override public void close() throws IOException {
            file.close();
        }
    }

    private static void validateHeader(int width, int height, int frameCount) throws IOException {
        if (width < 1 || width > MediaPackets.MAX_WIDTH || height < 1 || height > MediaPackets.MAX_HEIGHT
                || frameCount < 1 || frameCount > MAX_FRAMES)
            throw new IOException("media_clip_header");
    }

    private static long minimumFrameBytes(int frames) {
        return (long) frames * (Integer.BYTES + MIN_JPEG + PCM_BYTES);
    }

    private static void readFully(FileChannel file, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            if (file.read(buffer) < 1) throw new EOFException("media_clip_truncated");
        }
    }

    private static void writeFully(FileChannel file, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            if (file.write(buffer) < 1) throw new IOException("media_clip_write");
        }
    }
}
