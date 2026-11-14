package me.skywars2.mixin;

import me.skywars2.game.LbcModeScroll;
import me.skywars2.game.PlayerLatency;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ModeScrollMixin {
    @Inject(method="handleSetCarriedItem",at=@At(value="INVOKE",target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",shift=At.Shift.AFTER),cancellable=true)
    private void skywars2$wheel(ServerboundSetCarriedItemPacket packet,CallbackInfo ci){
        if(LbcModeScroll.carried((ServerGamePacketListenerImpl)(Object)this,packet.getSlot()))ci.cancel();
    }
    @Mixin(ServerCommonPacketListenerImpl.class)
    public abstract static class Pong {
        @Shadow @Final protected MinecraftServer server;
        @Inject(method="handlePong",at=@At("HEAD"))
        private void skywars2$wheelBarrier(ServerboundPongPacket packet,CallbackInfo ci){
            var listener=(ServerCommonPacketListenerImpl)(Object)this;
            if(listener instanceof ServerGamePacketListenerImpl game){
                PlayerLatency.pong(game,packet.getId());
                PacketUtils.ensureRunningOnSameThread(packet,listener,server.packetProcessor());
                LbcModeScroll.pong(game,packet.getId());
            }
        }
        @Inject(method="latency",at=@At("RETURN"),cancellable=true)
        private void skywars2$measuredLatency(CallbackInfoReturnable<Integer> ci){
            if((Object)this instanceof ServerGamePacketListenerImpl listener){
                int value=PlayerLatency.millis(listener);if(value>=0)ci.setReturnValue(value);
            }
        }
    }
}
