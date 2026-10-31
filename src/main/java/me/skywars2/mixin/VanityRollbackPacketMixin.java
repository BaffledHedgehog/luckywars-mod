package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import me.skywars2.game.MatchPlayerCheckpoint;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A frozen native server still processes player packets. ACKs remain available for restored views. */
@Mixin(value=ServerGamePacketListenerImpl.class,priority=2000)
public abstract class VanityRollbackPacketMixin implements MatchPlayerCheckpoint.SimulatedAcknowledgement {
    @Shadow public ServerPlayer player;
    @Shadow private boolean waitingForRespawn;
    @Override public boolean skywars2$waitingForRespawn(){return waitingForRespawn;}
    @org.spongepowered.asm.mixin.gen.Invoker("restartClientLoadTimerAfterRespawn")
    public abstract void skywars2$beginRespawn();
    @Shadow private int awaitingTeleport;
    @Shadow private net.minecraft.world.phys.Vec3 awaitingPositionFromClient;
    @Override public void skywars2$acknowledgeRestore(){
        var listener=(ServerGamePacketListenerImpl)(Object)this;
        if(awaitingPositionFromClient!=null){var point=awaitingPositionFromClient;
            listener.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket(awaitingTeleport,
                    point.x,point.y,point.z,player.getYRot(),player.getXRot()));}
        listener.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
    }
    @Unique private static final String skywars2$THREAD_CHECK=
            "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V";
    @Inject(method={"handleMovePlayer","handleMoveVehicle","handlePlayerInput",
            "handleContainerClick","handleContainerClose","handleContainerButtonClick","handlePlaceRecipe",
            "handleSetCreativeModeSlot","handleSetCarriedItem","handlePickItemFromBlock","handlePickItemFromEntity",
            "handleRenameItem","handleSelectTrade","handleSetBeaconPacket","handleContainerSlotStateChanged",
            "handlePlayerAction","handleUseItemOn","handleUseItem","handleAttack","handlePunch","handleInteract",
            "handleClientCommand"},at=@At(value="INVOKE",target=skywars2$THREAD_CHECK,shift=At.Shift.AFTER),cancellable=true)
    private void skywars2$quarantine(CallbackInfo ci){
        var game=SkyWars2.game();
        if(game!=null&&game.vanity().blocked(player)&&!MatchPlayerCheckpoint.restoring(player))ci.cancel();
    }
    @Mixin(ServerPlayer.class)
    public abstract static class Simulation {
        @Inject(method="doTick",at=@At("HEAD"),cancellable=true)
        private void skywars2$pause(CallbackInfo ci){
            var game=SkyWars2.game();if(game!=null&&game.vanity().blocked((ServerPlayer)(Object)this))ci.cancel();
        }
    }
}
