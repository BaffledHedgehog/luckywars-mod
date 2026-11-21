package me.skywars2.mixin.entities;

import me.skywars2.game.entities.CoreEntityMerging;
import me.skywars2.game.items.drops.LbcUltraponos;
import me.skywars2.game.match.checkpoints.MatchActorCheckpoint;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import me.skywars2.game.GameController;
import me.skywars2.game.skills.LbcCombatRewards;

public final class CoreEntityMergeMixin {
    private CoreEntityMergeMixin() { }

    @Mixin(ItemEntity.class)
    public abstract static class Items implements CoreEntityMerging.Items {
        @Shadow private int age;
        @Shadow private int pickupDelay;
        @Shadow private java.util.UUID target;

        @Override public int skywars2$itemPickupDelay() { return pickupDelay; }
        @Override public java.util.UUID skywars2$itemPickupTarget() { return target; }

        @Override public boolean skywars2$mergeableItem() {
            var self = (ItemEntity) (Object) this;
            return self.isAlive() && !self.getItem().isEmpty() && pickupDelay != 32767
                    && age != -32768 && age < 6000 && !self.isPassenger() && !self.isVehicle()
                    && !self.entityTags().contains(LbcUltraponos.UNMERGEABLE_REWARD);
        }

        @Override public boolean skywars2$absorbItem(ItemEntity incoming) {
            var self = (ItemEntity) (Object) this;
            var other = (CoreEntityMerging.Items) incoming;
            if (self == incoming || !skywars2$mergeableItem() || !other.skywars2$mergeableItem()
                    || !java.util.Objects.equals(target, other.skywars2$itemPickupTarget())
                    || !CoreEntityMerging.sameDrop(self, incoming)) return false;
            long total = (long) self.getItem().getCount() + incoming.getItem().getCount();
            if (total > Integer.MAX_VALUE) return false; // preserve both groups, never overflow
            self.setItem(self.getItem().copyWithCount((int) total));
            pickupDelay = Math.max(pickupDelay, other.skywars2$itemPickupDelay());
            age = Math.min(age, incoming.getAge());
            var game = me.skywars2.SkyWars2.game();
            if (game != null) game.itemQueue().merged(self, incoming);
            incoming.discard();
            return true;
        }

        @Inject(method = "isMergable", at = @At("HEAD"), cancellable = true)
        private void skywars2$ignoreComponentMaximum(CallbackInfoReturnable<Boolean> cir) {
            cir.setReturnValue(skywars2$mergeableItem());
        }

        @Inject(method = "tryToMerge", at = @At("HEAD"), cancellable = true)
        private void skywars2$oneGroundGroup(ItemEntity other, CallbackInfo ci) {
            skywars2$absorbItem(other);
            ci.cancel();
        }

        @WrapOperation(method = "addAdditionalSaveData", at = @At(value = "INVOKE", target =
                "Lnet/minecraft/world/level/storage/ValueOutput;store(Ljava/lang/String;Lcom/mojang/serialization/Codec;Ljava/lang/Object;)V"))
        private <T> void skywars2$saveLargeGroundCount(ValueOutput output, String key, Codec<T> codec,
                T value, Operation<Void> original) {
            if (value instanceof ItemStack stack && stack.getCount() > 99) {
                output.putInt("skywars2:ground_count", stack.getCount());
                @SuppressWarnings("unchecked") T legal = (T) stack.copyWithCount(1);
                original.call(output, key, codec, legal);
            } else original.call(output, key, codec, value);
        }

        @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
        private void skywars2$loadLargeGroundCount(ValueInput input, CallbackInfo ci) {
            var self = (ItemEntity) (Object) this;
            int count = input.getIntOr("skywars2:ground_count", 0);
            if (count > 99 && !self.getItem().isEmpty()) self.setItem(self.getItem().copyWithCount(count));
        }

        @ModifyArgs(method = "mergeWithNeighbours", at = @At(value = "INVOKE", target =
                "Lnet/minecraft/world/phys/AABB;inflate(DDD)Lnet/minecraft/world/phys/AABB;"))
        private void skywars2$mergeRadius(Args args) {
            double radius = CoreEntityMerging.settings().itemMergeRadius;
            if (radius <= 0) return; // zero selects the original vanilla search
            args.set(0, radius);
            args.set(1, radius);
            args.set(2, radius);
        }
    }

    @Mixin(ServerLevel.class)
    public abstract static class Level {
        @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
        private void skywars2$mergeBeforePublication(Entity entity, CallbackInfoReturnable<Boolean> cir) {
            if (!(entity instanceof ItemEntity) && !(entity instanceof ExperienceOrb)) return;
            var level = (ServerLevel) (Object) this;
            // The native registry also reserves UUIDs in hidden sections. During replay each saved body must return separately.
            if (MatchActorCheckpoint.restoring(entity)
                    || ((ActorCheckpointAccess.LevelEntities) level).skywars2$manager().isLoaded(entity.getUUID())) return;
            if (entity instanceof ItemEntity item) {
                var game = me.skywars2.SkyWars2.game();
                var rewards = game == null ? null : game.items().combatRewards();
                if (rewards != null) rewards.captureIncomingItem(level, item);
                if (CoreEntityMerging.mergeOnSpawn(level, item)) {
                    if (rewards != null) rewards.absorbedItem(level, item);
                    cir.setReturnValue(true);
                }
                return;
            }
            if (entity instanceof ExperienceOrb orb && CoreEntityMerging.mergeOnSpawn(level, orb)) {
                cir.setReturnValue(true); // successful XP spawn, absorbed by a resident orb
            }
        }
    }

    @Mixin(ExperienceOrb.class)
    public abstract static class Orbs implements CoreEntityMerging.Orb {
        @Shadow private int count;
        @Shadow private int age;
        @Shadow public abstract int getValue();
        @Shadow private void setValue(int value) { throw new AssertionError(); }

        @Override public long skywars2$totalExperience() {
            return (long) getValue() * count;
        }

        @Override public int skywars2$experienceAge() { return age; }

        @Override public void skywars2$normalizeExperience() {
            long total = skywars2$totalExperience();
            if (total > 0 && total <= Integer.MAX_VALUE) {
                setValue((int) total);
                count = 1;
            }
        }

        @Override public boolean skywars2$absorbExperience(ExperienceOrb other) {
            var self = (ExperienceOrb) (Object) this;
            if (self == other || self.isRemoved() || other.isRemoved()) return false;
            long mine = skywars2$totalExperience();
            long theirs = ((CoreEntityMerging.Orb) other).skywars2$totalExperience();
            // Preserve oversized legacy groups separately rather than losing any XP.
            if (mine <= 0 || theirs <= 0 || mine > Integer.MAX_VALUE || theirs > Integer.MAX_VALUE - mine) return false;
            setValue((int) (mine + theirs));
            count = 1;
            age = Math.min(age, ((CoreEntityMerging.Orb) other).skywars2$experienceAge());
            other.discard();
            return true;
        }

        @Inject(method = "awardWithDirection", at = @At("HEAD"), cancellable = true)
        private static void skywars2$oneAward(ServerLevel level, Vec3 pos, Vec3 direction, int amount, CallbackInfo ci) {
            if (!CoreEntityMerging.enabled(level)) return;
            if (amount > 0) level.addFreshEntity(new ExperienceOrb(level, pos, direction, amount));
            ci.cancel();
        }

        @Inject(method = "scanForMerges", at = @At("HEAD"), cancellable = true)
        private void skywars2$mergeMixedValues(CallbackInfo ci) {
            var self = (ExperienceOrb) (Object) this;
            if (self.level() instanceof ServerLevel level && CoreEntityMerging.enabled(level)) {
                CoreEntityMerging.mergeNearby(self);
                ci.cancel();
            }
        }

        @Inject(method = "playerTouch", at = @At("HEAD"))
        private void skywars2$collectWholeGroup(Player player, CallbackInfo ci) {
            var self = (ExperienceOrb) (Object) this;
            if (self.level() instanceof ServerLevel level && CoreEntityMerging.enabled(level)) skywars2$normalizeExperience();
        }

        // Vanilla stores Value as a short. Aggregated awards must survive saves above 32767.
        @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
        private void skywars2$saveFullExperience(ValueOutput output, CallbackInfo ci) {
            output.putInt("Value", getValue());
        }

        @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
        private void skywars2$readFullExperience(ValueInput input, CallbackInfo ci) {
            setValue(input.getIntOr("Value", getValue()));
        }

        // Same native Mending selection and rounding, with wide proportional arithmetic.
        @Inject(method = "repairPlayerItems", at = @At("HEAD"), cancellable = true)
        private void skywars2$safeMending(ServerPlayer player, int amount, CallbackInfoReturnable<Integer> cir) {
            while (amount > 0) {
                var selected = EnchantmentHelper.getRandomItemWith(EnchantmentEffectComponents.REPAIR_WITH_XP, player, ItemStack::isDamaged);
                if (selected.isEmpty()) { cir.setReturnValue(amount); return; }
                var stack = selected.get().itemStack();
                int capacity = EnchantmentHelper.modifyDurabilityToRepairFromXp(player.level(), stack, amount);
                int repair = Math.min(capacity, stack.getDamageValue());
                stack.setDamageValue(stack.getDamageValue() - repair);
                if (repair <= 0) { cir.setReturnValue(0); return; }
                amount -= (int) ((long) repair * amount / capacity);
            }
            cir.setReturnValue(0);
        }
    }
}
