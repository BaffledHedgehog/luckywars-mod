package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.skywars2.game.LbcFakePlayer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public interface FakePlayerMixin {
    @Mixin(Villager.class)
    abstract class Persistence {
        @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
        private void skywars2$loadFakePlayer(ValueInput input, CallbackInfo ci) {
            LbcFakePlayer.read((Villager)(Object)this, input);
        }

        @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
        private void skywars2$saveFakePlayer(ValueOutput output, CallbackInfo ci) {
            LbcFakePlayer.write((Villager)(Object)this, output);
        }
    }

    @Mixin(ServerEntity.class)
    abstract class Pairing {
        @Shadow @Final private Entity entity;
        @Shadow @Final private ServerEntity.Synchronizer synchronizer;

        @Inject(method = "sendChanges", at = @At("HEAD"))
        private void skywars2$updateFakeEquipment(CallbackInfo ci) {
            var packet = LbcFakePlayer.updateEquipment(entity);
            if (packet != null) synchronizer.sendToTrackingPlayers(packet);
        }

        @WrapOperation(method = "sendPairingData", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;"))
        private ItemStack skywars2$projectFakeEquipment(LivingEntity actor, EquipmentSlot slot, Operation<ItemStack> original) {
            return LbcFakePlayer.renderEquipment(actor, slot, original.call(actor, slot));
        }
    }

    @Mixin(ChunkMap.class)
    abstract class Tracking {
        @Shadow public abstract DistanceManager getDistanceManager();

        @WrapOperation(method = "tick()V", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/entity/Entity;needsSync:Z", opcode = Opcodes.GETFIELD))
        private boolean skywars2$mirrorOutsideSimulation(Entity actor, Operation<Boolean> original) {
            boolean nativeSync = original.call(actor);
            // A moved actor short-circuits before this read. The normal path also handles an explicit sync flag
            // and simulation-range bodies. Only the tracked bodies skipped by all three need this packet here.
            if (!nativeSync && LbcFakePlayer.matches(actor)
                    && !getDistanceManager().inEntityTickingRange(actor.chunkPosition().pack())) {
                var packet = LbcFakePlayer.updateEquipment(actor);
                if (packet != null) ((ChunkMap)(Object)this).sendToTrackingPlayers(actor, packet);
            }
            return nativeSync;
        }
    }

    @Mixin(LivingEntity.class)
    abstract class DeathLoot {
        @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
        private void skywars2$fakePlayerHasNoDeathLoot(ServerLevel level, DamageSource source, CallbackInfo ci) {
            if (LbcFakePlayer.matches((LivingEntity)(Object)this)) ci.cancel();
        }
    }
}
