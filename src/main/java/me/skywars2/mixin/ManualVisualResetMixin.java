package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Respawns and real dimension changes invalidate client-only entities. */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ManualVisualResetMixin {
    @Inject(method="send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V",at=@At("TAIL"))
    private void skywars2$worldReset(Packet<?> packet,io.netty.channel.ChannelFutureListener listener,CallbackInfo ci) {
        if((packet instanceof ClientboundRespawnPacket||packet instanceof ClientboundBundlePacket)
                &&containsReset(packet)&&(Object)this instanceof ServerGamePacketListenerImpl connection) {
            var game=SkyWars2.game();if(game!=null)game.visualTracking().clientWorldReset(connection);
        }
    }
    private static boolean containsReset(Packet<?> packet) {
        if(packet instanceof ClientboundRespawnPacket)return true;
        if(packet instanceof ClientboundBundlePacket bundle)
            for(var child:bundle.subPackets())if(containsReset(child))return true;
        return false;
    }
}
