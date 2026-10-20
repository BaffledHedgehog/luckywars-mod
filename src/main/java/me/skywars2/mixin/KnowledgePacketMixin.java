package me.skywars2.mixin;

import io.netty.channel.ChannelFutureListener;
import me.skywars2.SkyWars2;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the published NoKnowledge view without touching mutable game state on network threads. */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class KnowledgePacketMixin {
    // The single-argument overload delegates here, so both native send entry points are covered.
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V",
            at = @At("HEAD"), cancellable = true)
    private void skywars2$hideKnowledge(Packet<?> packet, ChannelFutureListener listener, CallbackInfo ci) {
        if (!((Object) this instanceof ServerGamePacketListenerImpl connection)) return;
        var game = SkyWars2.game();
        if (game != null && game.lbc().shouldFilterKnowledgePacket(connection.player, packet)) ci.cancel();
    }
}
