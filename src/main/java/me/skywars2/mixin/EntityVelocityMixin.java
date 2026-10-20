package me.skywars2.mixin;

import net.minecraft.world.entity.Entity;
import me.skywars2.game.ProjectileLifetimes;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityVelocityMixin {
    // Skip the entire native blast branch, including exposure rays and redirectable-projectile owner changes.
    @Inject(method = "ignoreExplosion", at = @At("HEAD"), cancellable = true)
    private void skywars2$spellIgnoresBlast(Explosion explosion, CallbackInfoReturnable<Boolean> ci) {
        if (ProjectileLifetimes.protectedWandProjectile((Entity)(Object)this)) ci.setReturnValue(true);
    }

    @Inject(method = "pushFromExplosion", at = @At("HEAD"), cancellable = true)
    private void skywars2$spellIgnoresBlastImpulse(Vec3 impulse, CallbackInfo ci) {
        if (ProjectileLifetimes.protectedWandProjectile((Entity)(Object)this)) ci.cancel();
    }

    @Inject(method = "isAttackable", at = @At("HEAD"), cancellable = true)
    private void skywars2$spellIgnoresAttack(CallbackInfoReturnable<Boolean> ci) {
        if (ProjectileLifetimes.protectedWandProjectile((Entity)(Object)this)) ci.setReturnValue(false);
    }

    @Inject(method = "canBeHitByProjectile", at = @At("HEAD"), cancellable = true)
    private void skywars2$spellIgnoresProjectile(CallbackInfoReturnable<Boolean> ci) {
        if (ProjectileLifetimes.protectedWandProjectile((Entity)(Object)this)) ci.setReturnValue(false);
    }

    @ModifyConstant(
            method = "load",
            constant = @Constant(doubleValue = 10.0),
            require = 3,
            allow = 3
    )
    private double skywars2$removeSavedMotionThreshold(double original) {
        // Finite saved components pass; Entity.setDeltaMovement still rejects non-finite vectors.
        return Double.POSITIVE_INFINITY;
    }
}
