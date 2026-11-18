package me.skywars2.game;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import me.skywars2.mixin.MediaTransportAccess;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/** Bounds media writes until the client has received a following vanilla Ping. */
final class TikTokOutput {
    private static final AttributeKey<TikTokOutput> KEY = AttributeKey.valueOf("skywars2:tiktok_output");
    private static final int SOUND_WINDOW = 6;
    private static final long SLOW_FRAME_NANOS = 100_000_000L;
    private static final long FAST_FRAME_NANOS = 25_000_000L;
    private static final long QUALITY_HOLD_NANOS = 5_000_000_000L;

    private final Channel channel;
    private final ServerGamePacketListenerImpl listener;
    private final Window frames = new Window(1);
    private final Window sound = new Window(SOUND_WINDOW);
    private volatile int pixelStep = 1;
    private volatile long videoBackoffUntil;
    // Single outstanding frame. Bracket only vanilla frames, whose encoded size is high.
    private int beforeFrame;
    private long beforeFrameAck;
    private long upgradeAfter;

    private static final class Window {
        final AtomicBoolean scheduled = new AtomicBoolean();
        volatile int remaining;
        // Owned by the channel event loop.
        final int[] receipts;

        Window(int capacity) {
            receipts = new int[capacity];
            remaining = capacity;
        }

        int reserve() {
            for (int slot = 0; slot < receipts.length; slot++) {
                if (receipts[slot] != 0) continue;
                int id = PlayerLatency.nextProbeId();
                receipts[slot] = id;
                remaining--;
                return id;
            }
            throw new IllegalStateException("Media window has no available receipt slot");
        }

        boolean acknowledge(int id) {
            if (id >= 0) return false;
            for (int slot = 0; slot < receipts.length; slot++) {
                if (receipts[slot] != id) continue;
                receipts[slot] = 0;
                remaining++;
                return true;
            }
            return false;
        }
    }

    private TikTokOutput(Channel channel, ServerGamePacketListenerImpl listener) {
        this.channel = channel;
        this.listener = listener;
    }

    static TikTokOutput forPlayer(ServerPlayer player) {
        var listener = player.connection;
        if (listener == null) return null;
        var connection = ((MediaTransportAccess.Listener) listener).skywars2$mediaConnection();
        var channel = ((MediaTransportAccess.Transport) connection).skywars2$mediaChannel();
        if (channel == null || !channel.isActive()) return null;
        var attr = channel.attr(KEY);
        var current = attr.get();
        if (current != null && current.listener == listener) return current;
        var output = new TikTokOutput(channel, listener);
        attr.set(output);
        return output;
    }

    static boolean acknowledge(Channel channel, int id) {
        var output = channel.attr(KEY).get();
        return output != null && output.acknowledge(id);
    }

    private boolean acknowledge(int id) {
        if (id < 0 && beforeFrame == id) {
            beforeFrame = 0;
            beforeFrameAck = System.nanoTime();
            return true;
        }
        if (!frames.acknowledge(id)) return sound.acknowledge(id);
        if (beforeFrameAck != 0) {
            long now = System.nanoTime();
            adaptVideo(now, now - beforeFrameAck);
        }
        beforeFrame = 0;
        beforeFrameAck = 0;
        return true;
    }

    private void adaptVideo(long now, long frameDelay) {
        // The two probes travel with the same RTT. The gap measures how long
        // the frame occupied client delivery, rather than the player's ping.
        if (frameDelay > SLOW_FRAME_NANOS) {
            if (pixelStep == TikTokPixels.MAX_STEP) videoBackoffUntil = now + QUALITY_HOLD_NANOS;
            pixelStep = Math.min(TikTokPixels.MAX_STEP, pixelStep * (frameDelay > 2 * SLOW_FRAME_NANOS ? 4 : 2));
            upgradeAfter = now + QUALITY_HOLD_NANOS;
        } else if (frameDelay <= FAST_FRAME_NANOS && pixelStep > 1 && now - upgradeAfter >= 0) {
            pixelStep /= 2;
            upgradeAfter = now + QUALITY_HOLD_NANOS;
        }
    }

    static void detach(Channel channel) {
        channel.attr(KEY).set(null);
    }

    boolean active() {
        return channel.isActive() && channel.attr(KEY).get() == this;
    }

    long clientTicks() {
        return ServerNetworkTransport.mediaClientTicks(channel);
    }

    boolean frameReady() {
        return ready(frames);
    }

    int vanillaPixelStep() { return pixelStep; }

    boolean vanillaFrameReady() {
        return vanillaBackoffElapsed() && frameReady();
    }

    boolean soundReady() {
        return ready(sound);
    }

    boolean offerFrame(Packet<?> packet, long expires, BooleanSupplier valid) {
        return offer(frames, packet, expires, valid, 0);
    }

    boolean offerVanillaFrame(Packet<?> packet, long expires, BooleanSupplier valid, int step) {
        if (!vanillaFrameReady() || step != pixelStep) return false;
        return offer(frames, packet, expires, () -> step == pixelStep && vanillaBackoffElapsed() && valid.getAsBoolean(), step);
    }

    private boolean vanillaBackoffElapsed() {
        long until = videoBackoffUntil;
        return until == 0 || System.nanoTime() - until >= 0;
    }

    boolean offerSound(Packet<?> packet, long expires, BooleanSupplier valid) {
        return offer(sound, packet, expires, valid, 0);
    }

    private boolean ready(Window window) {
        return writable(window) && !window.scheduled.get();
    }

    private boolean writable(Window window) {
        return window.remaining > 0 && active() && channel.isWritable();
    }

    private boolean offer(Window window, Packet<?> packet, long expires, BooleanSupplier valid, int frameStep) {
        if (!writable(window) || !window.scheduled.compareAndSet(false, true)) return false;

        Runnable write = () -> {
            try {
                if (!writable(window) || expires - System.nanoTime() <= 0 || !valid.getAsBoolean()) return;
                int receipt = window.reserve();
                if (frameStep > 0) {
                    beforeFrame = PlayerLatency.nextProbeId();
                    listener.send(new ClientboundPingPacket(beforeFrame));
                }
                listener.send(packet);
                // Each Ping follows its own media write in TCP order. A Pong for an
                // earlier write cannot grant credit for later, still queued traffic.
                listener.send(new ClientboundPingPacket(receipt));
            } finally {
                window.scheduled.set(false);
            }
        };

        var loop = channel.eventLoop();
        if (loop.inEventLoop()) {
            write.run();
        } else {
            try {
                loop.execute(write);
            } catch (RejectedExecutionException closed) {
                window.scheduled.set(false);
                return false;
            }
        }
        return true;
    }
}
