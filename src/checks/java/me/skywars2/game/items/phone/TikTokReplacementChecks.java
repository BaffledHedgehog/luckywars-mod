package me.skywars2.game.items.phone;

import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import me.skywars2.game.GameController;
import me.skywars2.media.MediaPackets;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/** Connection ownership for the real media clocks, including the native respawn entity-ID alias. */
final class TikTokReplacementChecks {
    private TikTokReplacementChecks() { }

    static void verify(GameController game, ServerPlayer current, TikTokMedia clip, List<Packet<?>> received)
            throws InterruptedException {
        var listener = current.connection;
        var output = TikTokOutput.forPlayer(current);
        require(output != null && output.activeFor(current) && clip != null, "actual current media channel and ready clip");
        require(TikTokClientSupport.enhanced(current) && !TikTokClientSupport.httpsPlayback(current),
                "replacement fixture observes the real legacy JPEG/PCM clock");

        // PlayerList.respawn reuses the ID and listener. The old instance remains an equal map key.
        var oldBody = new ServerPlayer(game.server(), current.level(), current.getGameProfile(), current.clientInformation());
        oldBody.setId(current.getId());
        var oldListener = new ServerGamePacketListenerImpl(game.server(), new Connection(PacketFlow.SERVERBOUND),
                oldBody, new CommonListenerCookie(oldBody.getGameProfile(), 0, oldBody.clientInformation(), false));
        oldBody.connection = listener;
        require(oldBody != current && oldBody.equals(current) && oldBody.getUUID().equals(current.getUUID()),
                "old actor is a distinct native body with an equal entity ID and UUID");
        require(!output.activeFor(oldBody) && TikTokOutput.forPlayer(oldBody) == null
                && game.server().getPlayerList().getPlayer(current.getUUID()) == current,
                "an old body cannot borrow the replacement's real channel even with the same listener");

        try (var video = new TikTokClientClock(id -> { }, id -> { })) {
            int firstPacket = received.size();
            video.play(current, clip, System.nanoTime(), output);
            await(() -> frames(received, firstPacket).size() > 0, "replacement video emits");
            UUID session = frames(received, firstPacket).getFirst().session();
            int afterFirstFrame = received.size();

            video.play(oldBody, clip, System.nanoTime(), output);
            video.stop(oldBody, listener);
            video.stop(current, oldListener);
            await(() -> frames(received, afterFirstFrame).stream().anyMatch(frame -> frame.session().equals(session)),
                    "stale body or listener cleanup cannot stop the current video");
            require(stopCount(received, firstPacket, session) == 0, "no stale Stop delivered for current video");
            video.stop(current, listener);
            require(stopCount(received, firstPacket, session) == 1, "current owner receives exactly its Stop");
        }

        try (var audio = new TikTokSoundClock(id -> { }, id -> { })) {
            audio.play(current, clip, System.nanoTime(), output);
            await(() -> audio.emittedSlices.get() > 0, "replacement audio emits");
            long emitted = audio.emittedSlices.get();
            audio.play(oldBody, clip, System.nanoTime(), output);
            audio.stop(oldBody, listener);
            audio.stop(current, oldListener);
            await(() -> audio.emittedSlices.get() > emitted + 1, "stale cleanup cannot stop replacement audio");
        }
    }

    private static List<MediaPackets.Frame> frames(List<Packet<?>> received, int from) {
        return received.stream().skip(from)
                .filter(ClientboundCustomPayloadPacket.class::isInstance)
                .map(ClientboundCustomPayloadPacket.class::cast)
                .map(ClientboundCustomPayloadPacket::payload)
                .filter(MediaPackets.Frame.class::isInstance).map(MediaPackets.Frame.class::cast).toList();
    }

    private static long stopCount(List<Packet<?>> received, int from, UUID session) {
        return received.stream().skip(from).filter(packet -> packet instanceof ClientboundCustomPayloadPacket payload
                && payload.payload() instanceof MediaPackets.Stop stop && stop.session().equals(session)).count();
    }

    private static void await(BooleanSupplier predicate, String description) throws InterruptedException {
        long deadline = System.nanoTime() + 500_000_000L;
        while (!predicate.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(10);
        require(predicate.getAsBoolean(), description);
    }

    private static void require(boolean condition, String description) {
        if (!condition) throw new IllegalStateException("TikTok replacement: " + description);
    }
}
