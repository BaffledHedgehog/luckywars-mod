package me.skywars2.mixin;

import me.skywars2.game.SoundNormalization;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Audience selection has already happened; each listener receives only their own gain projection. */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class SoundNormalizationMixin {
    @ModifyVariable(method="send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V",
            at=@At("HEAD"),argsOnly=true)
    private Packet<?> skywars2$normalizeSound(Packet<?> packet){
        return (Object)this instanceof ServerGamePacketListenerImpl connection
                ?SoundNormalization.project(connection.player,packet):packet;
    }
}
