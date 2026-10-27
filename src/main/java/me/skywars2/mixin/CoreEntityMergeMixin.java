package me.skywars2.mixin;

import me.skywars2.game.CoreEntityMerging;
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

public final class CoreEntityMergeMixin {
    private CoreEntityMergeMixin() { }

    @Mixin(ItemEntity.class)
    public abstract static class Items {
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
        private void skywars2$mergeExperienceBeforePublication(Entity entity, CallbackInfoReturnable<Boolean> cir) {
            if (entity instanceof ExperienceOrb orb
                    && CoreEntityMerging.mergeOnSpawn((ServerLevel) (Object) this, orb)) {
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
