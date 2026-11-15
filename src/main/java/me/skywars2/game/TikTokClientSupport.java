package me.skywars2.game;

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
    private static final ConcurrentMap<UUID,ServerGamePacketListenerImpl> peers=new ConcurrentHashMap<>();
    private TikTokClientSupport(){}
    static void register(){
        MediaPackets.register();
        ServerPlayConnectionEvents.JOIN.register((listener,sender,server)->peers.remove(listener.player.getUUID()));
        ServerPlayConnectionEvents.DISCONNECT.register((listener,server)->peers.remove(listener.player.getUUID(),listener));
        ServerLifecycleEvents.SERVER_STOPPED.register(server->peers.clear());
        ServerPlayNetworking.registerGlobalReceiver(MediaPackets.Hello.TYPE,(hello,context)->{
            var player=context.player();
            if(hello.supported()&&ServerPlayNetworking.canSend(player,MediaPackets.Frame.TYPE)&&ServerPlayNetworking.canSend(player,MediaPackets.Stop.TYPE))
                peers.put(player.getUUID(),player.connection);
            else peers.remove(player.getUUID(),player.connection);
        });
    }
    public static boolean enhanced(ServerPlayer player){return player.connection!=null&&peers.get(player.getUUID())==player.connection;}
}
