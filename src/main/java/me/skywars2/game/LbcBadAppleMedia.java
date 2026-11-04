package me.skywars2.game;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Immutable source BAF2 bytes. Playback and checkpoint cursors belong to the caller. */
final class LbcBadAppleMedia {
    private static final String RESOURCE = "/skywars2/badapple.baf2";
    private static final int HEADER_BYTES = 16;
    private static final int SOURCE_BYTES = 10_678_950;
    private static final String SOURCE_SHA256 =
            "e53daa853cc81ae7356e09f9757401ecefe9c5efb2c39b86448ca01281de23cb";

    private final byte[] media;
    private final int width;
    private final int height;
    private final int frameCount;
    private final int pixelCount;
    private final int frameBytes;

    private LbcBadAppleMedia(byte[] ownedBytes) {
        if (ownedBytes.length < HEADER_BYTES) throw new IllegalArgumentException("Truncated BAF2 header");
        ByteBuffer header = ByteBuffer.wrap(ownedBytes, 0, HEADER_BYTES).order(ByteOrder.BIG_ENDIAN);
        if (header.getInt() != 0x42414632) throw new IllegalArgumentException("Invalid BAF2 magic");
        width = header.getInt();
        height = header.getInt();
        frameCount = header.getInt();
        long pixels = (long) width * height;
        long stride = (pixels + 7L) / 8L;
        if (width <= 0 || height <= 0 || frameCount <= 0 || pixels > Integer.MAX_VALUE
                || stride > Integer.MAX_VALUE
                || HEADER_BYTES + stride * frameCount != ownedBytes.length)
            throw new IllegalArgumentException("Invalid BAF2 dimensions or payload length");
        if (width != 228 || height != 171 || frameCount != 2191 || ownedBytes.length != SOURCE_BYTES)
            throw new IllegalArgumentException("Unexpected Bad Apple source media");
        pixelCount = (int) pixels;
        frameBytes = (int) stride;
        try {
            String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(ownedBytes));
            if (!SOURCE_SHA256.equals(sha)) throw new IllegalArgumentException("Bad Apple source SHA-256 mismatch");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
        // The only constructor caller transfers its fresh resource array; it is never exposed.
        media = ownedBytes;
    }

    private static final class Holder {
        private static final LbcBadAppleMedia INSTANCE = load();
    }

    /** Call during bootstrap to complete the sole resource read before any playback work. */
    static LbcBadAppleMedia instance() { return Holder.INSTANCE; }

    private static LbcBadAppleMedia load() {
        try (InputStream input = LbcBadAppleMedia.class.getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IllegalStateException("Missing Bad Apple media " + RESOURCE);
            // Bounded even for an accidentally replaced resource; reject trailing and missing bytes.
            return new LbcBadAppleMedia(input.readNBytes(SOURCE_BYTES + 1));
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot load Bad Apple media", failure);
        }
    }

    int width() { return width; }
    int height() { return height; }
    int frameCount() { return frameCount; }
    int pixelCount() { return pixelCount; }
    int frameBytes() { return frameBytes; }

    /** Literal frame zero followed by XOR deltas; no decoded frames are cached globally. */
    void decodeFrame(int targetFrame, byte[] destination) {
        checkFrame(targetFrame);
        checkBuffer(destination);
        System.arraycopy(media, HEADER_BYTES, destination, 0, frameBytes);
        for (int frame = 1; frame <= targetFrame; frame++) xorUnchecked(frame, destination);
    }

    /** Caller-owned packed absolute frame, useful for activation or checkpoint reconstruction. */
    byte[] newFrame(int targetFrame) {
        byte[] result = new byte[frameBytes];
        decodeFrame(targetFrame, result);
        return result;
    }

    /**
     * Move an absolute caller buffer forward or backward in O(abs(target-current) * stride).
     * XOR is reversible. A current index of -1 initializes the caller buffer from frame zero.
     */
    void moveFrame(int currentFrame, int targetFrame, byte[] destination) {
        checkFrame(targetFrame);
        checkBuffer(destination);
        if (currentFrame == -1) {
            decodeFrame(targetFrame, destination);
            return;
        }
        checkFrame(currentFrame);
        int first = Math.min(currentFrame, targetFrame) + 1;
        int last = Math.max(currentFrame, targetFrame);
        for (int frame = first; frame <= last; frame++) xorUnchecked(frame, destination);
    }

    /** XOR the named transition (frame-1 -> frame), also valid for the reverse transition. */
    void applyDelta(int frame, byte[] destination) {
        checkFrame(frame);
        if (frame == 0) throw new IllegalArgumentException("Frame zero is literal, not a delta");
        checkBuffer(destination);
        xorUnchecked(frame, destination);
    }

    private void xorUnchecked(int frame, byte[] destination) {
        int offset = HEADER_BYTES + frame * frameBytes;
        for (int i = 0; i < frameBytes; i++) destination[i] ^= media[offset + i];
    }

    /** Source order: z * width + x, least significant bit first; one means white concrete. */
    boolean white(byte[] absoluteFrame, int pixel) {
        checkBuffer(absoluteFrame);
        Objects.checkIndex(pixel, pixelCount);
        return (absoluteFrame[pixel >>> 3] & (1 << (pixel & 7))) != 0;
    }

    private void checkFrame(int frame) { Objects.checkIndex(frame, frameCount); }

    private void checkBuffer(byte[] destination) {
        if (destination.length != frameBytes)
            throw new IllegalArgumentException("Bad Apple frame buffer must have " + frameBytes + " bytes");
    }
}
