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

    private final Channel channel;
    private final ServerGamePacketListenerImpl listener;
    private final Window frames = new Window(1);
    private final Window sound = new Window(SOUND_WINDOW);

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
        return output != null && (output.frames.acknowledge(id) || output.sound.acknowledge(id));
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

    boolean soundReady() {
        return ready(sound);
    }

    boolean offerFrame(Packet<?> packet, long expires, BooleanSupplier valid) {
        return offer(frames, packet, expires, valid);
    }

    boolean offerSound(Packet<?> packet, long expires, BooleanSupplier valid) {
        return offer(sound, packet, expires, valid);
    }

    private boolean ready(Window window) {
        return writable(window) && !window.scheduled.get();
    }

    private boolean writable(Window window) {
        return window.remaining > 0 && active() && channel.isWritable();
    }

    private boolean offer(Window window, Packet<?> packet, long expires, BooleanSupplier valid) {
        if (!writable(window) || !window.scheduled.compareAndSet(false, true)) return false;

        Runnable write = () -> {
            try {
                if (!writable(window) || expires - System.nanoTime() <= 0 || !valid.getAsBoolean()) return;
                int receipt = window.reserve();
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
