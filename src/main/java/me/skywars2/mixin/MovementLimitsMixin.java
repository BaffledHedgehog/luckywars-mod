package me.skywars2.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MovementLimitsMixin {
    @Shadow public ServerPlayer player;
    @Shadow public abstract void resetPosition();
    @Shadow private boolean clientIsFloating;
    @Shadow private int aboveGroundTickCount;

    /** Vanilla snaps to the client's old coordinates after jumpFromGround returns. */
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method="handlePlayerPositionChange")
    private void skywars2$finishMovementBeforeJump(double x,double y,double z,float yaw,float pitch,
            boolean ground,boolean horizontalCollision,com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original){
        me.skywars2.game.GroundJumpEvents.afterMovement(player,()->original.call(x,y,z,yaw,pitch,ground,horizontalCollision));
    }

    /** Observe accepted positions only; rotation packets never interrupt the stationary charge. */
    @Inject(method="handleMovePlayer",at=@At("RETURN"))
    private void skywars2$gadgetMovement(ServerboundMovePlayerPacket packet,CallbackInfo ci) {
        var game=SkyWars2.game();
        if(game!=null&&game.server().isSameThread())game.items().teleportGadgets().moved(player);
    }

    @Unique private boolean skywars2$stunned() {
        var game=SkyWars2.game();
        return game!=null && (game.items().shooters().stunned(player) || game.items().timeStop().frozen(player) || game.items().fpvDrone().controlling(player));
    }

    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method="handlePlayerInput")
    private void skywars2$jumpPress(ServerboundPlayerInputPacket packet,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        var game=SkyWars2.game();
        if(game==null||!game.server().isSameThread()){original.call(packet);return;}
        boolean before=player.getLastClientInput().jump();
        original.call(packet);
        if(!before&&player.getLastClientInput().jump())game.items().rocketJumpPressed(player);
    }

    /** 26.3 already performs travel in doTick; normally this snap discards its server position. */
    @Redirect(method="tickPlayer",at=@At(value="INVOKE",target=
            "Lnet/minecraft/server/level/ServerPlayer;absSnapTo(DDDFF)V"))
    private void skywars2$retainStunPhysics(ServerPlayer player,double x,double y,double z,float yaw,float pitch) {
        if(!skywars2$stunned()) { player.absSnapTo(x,y,z,yaw,pitch); return; }
        var movement=player.position().subtract(x,y,z);
        player.doCheckFallDamage(movement.x,movement.y,movement.z,player.onGround());
        player.setKnownMovement(movement);
        player.checkMovementStatistics(movement.x,movement.y,movement.z);
        player.level().getChunkSource().move(player);
        clientIsFloating=false; aboveGroundTickCount=0;
        resetPosition();
    }

    @Inject(method="handleMovePlayer",at=@At(value="INVOKE",target=
            "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift=At.Shift.AFTER),cancellable=true)
    private void skywars2$rejectStunMovement(ServerboundMovePlayerPacket packet,CallbackInfo ci) {
        var game=SkyWars2.game();
        if(game!=null&&game.items().fpvDrone().look(player,packet)){ci.cancel();return;}
        if(skywars2$stunned()) {
            SkyWars2.game().items().timeStop().handleFrozenPlayer(player);
            ci.cancel();
        }
    }

    @Inject(method="handlePlayerInput",at=@At(value="INVOKE",target=
            "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift=At.Shift.AFTER),cancellable=true)
    private void skywars2$rejectStunInput(ServerboundPlayerInputPacket packet,CallbackInfo ci) {
        var game=SkyWars2.game();
        if(game!=null&&game.items().fpvDrone().input(player,packet.input())){ci.cancel();return;}
        if(!skywars2$stunned()) return;
        SkyWars2.game().items().shooters().prepareStunnedInput(player);
        ci.cancel();
    }

    /** РЈРїСЂР°РІР»РµРЅРёРµ С‚СЂР°РЅСЃРїРѕСЂС‚РѕРј Рё РїРµСЂРµРєР»СЋС‡РµРЅРёРµ РїРѕР»С‘С‚Р° С‚РѕР¶Рµ СЏРІР»СЏСЋС‚СЃСЏ РґРѕР±СЂРѕРІРѕР»СЊРЅС‹Рј РІРІРѕРґРѕРј. */
    @Inject(method={"handleMoveVehicle","handlePlayerCommand","handlePlayerAbilities"},at=@At(value="INVOKE",target=
            "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift=At.Shift.AFTER),cancellable=true)
    private void skywars2$rejectStunControls(@org.spongepowered.asm.mixin.injection.Coerce net.minecraft.network.protocol.Packet<?> packet,CallbackInfo ci) {
        if(!skywars2$stunned()) return;
        SkyWars2.game().items().shooters().prepareStunnedInput(player);
        player.onUpdateAbilities();
        ci.cancel();
    }

    /** Accept real teleport IDs, but their client position/rotation cannot override server simulation. */
    @Redirect(method="handleAcceptTeleportPacket",at=@At(value="INVOKE",target=
            "Lnet/minecraft/server/level/ServerPlayer;absSnapTo(DDDFF)V"))
    private void skywars2$ackStunPosition(ServerPlayer player,double x,double y,double z,float yaw,float pitch) {
        if(!skywars2$stunned()) player.absSnapTo(x,y,z,yaw,pitch);
    }

    @Inject(method="handlePlayerPositionChange",at=@At("HEAD"),cancellable=true)
    private void skywars2$rejectStunAckMovement(double x,double y,double z,float yaw,float pitch,
            boolean onGround,boolean horizontalCollision,CallbackInfo ci) {
        if(!skywars2$stunned()) return;
        resetPosition();
        SkyWars2.game().items().shooters().prepareStunnedInput(player);
        ci.cancel();
    }

    @Inject(method = "shouldCheckPlayerMovement", at = @At("HEAD"), cancellable = true)
    private void skywars2$disablePlayerSpeedValidation(boolean isFallFlying, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @ModifyConstant(
            method = "handleMoveVehicle",
            constant = @Constant(doubleValue = 100.0),
            require = 1,
            allow = 1
    )
    private double skywars2$removeVehicleSpeedThreshold(double original) {
        return Double.POSITIVE_INFINITY;
    }
}
