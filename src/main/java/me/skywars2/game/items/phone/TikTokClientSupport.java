package me.skywars2.game.items.phone;

import io.netty.channel.Channel;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import me.skywars2.media.MediaPackets;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/** Connection-scoped capability: an absent/old client keeps the vanilla phone path. */
public final class TikTokClientSupport {
    private record Peer(ServerGamePacketListenerImpl listener, boolean httpsPlayback, boolean mp4, boolean segments,boolean independentHands) {}
    private static final ConcurrentMap<UUID,Peer> peers=new ConcurrentHashMap<>();
    private TikTokClientSupport(){}
    static void register(){
        MediaPackets.register();
        ServerPlayConnectionEvents.JOIN.register((listener,sender,server)->peers.remove(listener.player.getUUID()));
        ServerPlayConnectionEvents.DISCONNECT.register((listener,server)->forget(listener.player.getUUID(), listener));
        ServerLifecycleEvents.SERVER_STOPPED.register(server->peers.clear());
        ServerPlayNetworking.registerGlobalReceiver(MediaPackets.Fallback.TYPE, (request, context) -> {
            var playback = TikTokAccounts.playback();
            if (playback != null && httpsPlayback(context.player())) {
                playback.requestClientFallback(context.player(), request);
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(MediaPackets.RequestSegment.TYPE, (request, context) -> {
            var playback = TikTokAccounts.playback();
            if (playback != null && segments(context.player())) playback.requestClientSegment(context.player(), request);
        });
        ServerPlayNetworking.registerGlobalReceiver(MediaPackets.Hello.TYPE,(hello,context)->{
            var player=context.player();
            if(hello.supported()&&ServerPlayNetworking.canSend(player,MediaPackets.Frame.TYPE)&&ServerPlayNetworking.canSend(player,MediaPackets.Stop.TYPE)) {
                boolean httpsPlayback = (hello.features() & MediaPackets.HTTPS_PLAYBACK) != 0
                        && ServerPlayNetworking.canSend(player, MediaPackets.Link.TYPE);
                peers.put(player.getUUID(), new Peer(player.connection, httpsPlayback,
                        httpsPlayback && (hello.features() & MediaPackets.DIRECT_MP4) != 0,
                        httpsPlayback && (hello.features() & MediaPackets.HTTPS_SEGMENTS) != 0
                                && ServerPlayNetworking.canSend(player, MediaPackets.Segment.TYPE),
                        httpsPlayback&&(hello.features()&MediaPackets.DIRECT_MP4)!=0&&(hello.features()&MediaPackets.INDEPENDENT_HANDS)!=0
                                &&ServerPlayNetworking.canSend(player,MediaPackets.HandLink.TYPE)));
            } else forget(player.getUUID(), player.connection);
        });
    }

    private static void forget(UUID player, ServerGamePacketListenerImpl listener) {
        var peer = peers.get(player);
        if (peer != null && peer.listener == listener) peers.remove(player, peer);
    }

    public static boolean acknowledgeMedia(Channel channel, int id) {
        return TikTokOutput.acknowledge(channel, id);
    }

    public static void detachMedia(Channel channel) {
        TikTokOutput.detach(channel);
    }

    public static boolean enhanced(ServerPlayer player) {
        var peer = peers.get(player.getUUID());
        return peer != null && peer.listener == player.connection;
    }

    static boolean httpsPlayback(ServerPlayer player) {
        var peer = peers.get(player.getUUID());
        return peer != null && peer.listener == player.connection && peer.httpsPlayback;
    }

    static boolean segments(ServerPlayer player) {
        var peer = peers.get(player.getUUID());
        return peer != null && peer.listener == player.connection && peer.segments;
    }

    static boolean mp4(ServerPlayer player) {
        var peer = peers.get(player.getUUID());
        return peer != null && peer.listener == player.connection && peer.mp4;
    }
    static boolean independentHands(ServerPlayer player){var peer=peers.get(player.getUUID());return peer!=null&&peer.listener==player.connection&&peer.independentHands;}
}
