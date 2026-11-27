package me.skywars2.mixin.visuals;

import java.util.List;
import me.skywars2.game.presentation.PlayerDeathPresentation;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public interface PlayerDeathPresentationMixin {
    @Mixin(LivingEntity.class)
    abstract class Health {
        @Unique private float skywars2$previousHealth;
        @Inject(method = "setHealth", at = @At("HEAD"))
        private void skywars2$beforeHealth(float health, CallbackInfo ci) {
            if ((Object)this instanceof ServerPlayer player) skywars2$previousHealth = player.getHealth();
        }
        @Inject(method = "setHealth", at = @At("TAIL"))
        private void skywars2$afterHealth(float health, CallbackInfo ci) {
            if ((Object)this instanceof ServerPlayer player && skywars2$previousHealth <= 0 && player.getHealth() > 0)
                PlayerDeathPresentation.request(player);
        }
    }

    @Mixin(ServerEntity.class)
    abstract class Pairing implements PlayerDeathPresentation.Pairing {
        @Shadow @Final private Entity entity;
        @Shadow @Final private ServerEntity.Synchronizer synchronizer;
        @Shadow private List<SynchedEntityData.DataValue<?>> trackedDataValues;
        public void skywars2$refreshMetadata() { trackedDataValues = entity.getEntityData().getNonDefaultValues(); }
        @Inject(method = "sendPairingData", at = @At("HEAD"))
        private void skywars2$currentPlayerPairing(CallbackInfo ci) {
            // A new viewer can pair after healing but before sendChanges replaces
            // its former zero-health cache. Refresh only this player's pairing data.
            if (entity instanceof ServerPlayer) skywars2$refreshMetadata();
        }
        @Inject(method = "sendChanges", at = @At("TAIL"))
        private void skywars2$repairLiveDeathPose(CallbackInfo ci) {
            if (entity instanceof ServerPlayer player)
                PlayerDeathPresentation.synchronize(player, (ServerEntity)(Object)this, synchronizer);
        }
    }

}
