package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.skywars2.game.DirectionalKnockback;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Retains native attack admission, attacker slowdown and the PvP motion packet/restoration. */
@Mixin(LivingEntity.class)
public abstract class DirectionalKnockbackMixin {
    @Inject(method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V",
            at = @At("HEAD"), cancellable = true)
    private void skywars2$lookDirectedAttribute(double power, double x, double z,
                                               DamageSource source, float damage, boolean fromEffect,
                                               CallbackInfo ci) {
        // Run inside the native implementation, so immune mobs' overrides still win.
        if (fromEffect && source.getEntity() instanceof LivingEntity attacker
                && source.getDirectEntity() == attacker
                && DirectionalKnockback.living(attacker, (LivingEntity)(Object)this, power, x, z, true))
            ci.cancel();
    }

    @Mixin(Player.class)
    public abstract static class Nonliving {
        @WrapOperation(method = "causeExtraKnockback", at = @At(value = "INVOKE", target =
                "Lnet/minecraft/world/entity/Entity;push(DDD)V"))
        private void skywars2$lookDirectedNonliving(Entity target, double x, double y, double z,
                                                   Operation<Void> original,
                                                   @Local(argsOnly = true, ordinal = 0) float power,
                                                   @Local(argsOnly = true) boolean fromEffect) {
            var impulse = DirectionalKnockback.nonliving((LivingEntity)(Object)this, power, fromEffect, x, y, z);
            original.call(target, impulse.x, impulse.y, impulse.z);
        }
    }
}
