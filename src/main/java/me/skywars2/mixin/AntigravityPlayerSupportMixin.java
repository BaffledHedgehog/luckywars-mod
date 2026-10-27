package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class AntigravityPlayerSupportMixin implements me.skywars2.game.LbcAntigravity.PlayerTransport {
    @Shadow public ServerPlayer player;
    @Shadow private net.minecraft.world.phys.Vec3 awaitingPositionFromClient;
    @Override public boolean skywars2$hasPendingTeleport() { return awaitingPositionFromClient!=null; }

    @ModifyVariable(method="handleMovePlayer",argsOnly=true,at=@At(value="INVOKE",target=
            "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift=At.Shift.AFTER))
    private ServerboundMovePlayerPacket skywars2$observeLanding(ServerboundMovePlayerPacket packet) {
        var game=SkyWars2.game();return game==null?packet:game.items().antigravity().supportInput(player,packet);
    }

    @Inject(method="noBlocksAround",at=@At("HEAD"),cancellable=true)
    private void skywars2$movingFloorIsSupport(Entity entity,CallbackInfoReturnable<Boolean> ci) {
        var game=SkyWars2.game();
        if(game!=null&&game.items().antigravity().supported(entity))ci.setReturnValue(false);
    }
}
