package me.skywars2.mixin.projectiles;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import me.skywars2.game.combat.FixedArrowDamage;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.LivingEntity;

@Mixin(AbstractArrow.class)
public abstract class FixedArrowDamageMixin {
    @WrapOperation(method="doKnockback",at=@At(value="INVOKE",target="Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 skywars2$fullProjectileDirection(Vec3 motion,double x,double y,double z,Operation<Vec3> original){return motion;}
    @WrapOperation(method="doKnockback",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;push(DDD)V"))
    private void skywars2$directPunch(LivingEntity victim,double x,double y,double z,Operation<Void> original,@Local Vec3 impulse){
        original.call(victim,x,impulse.y,z);
    }
    @ModifyExpressionValue(method="onHitEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/phys/Vec3;length()D"))
    private double skywars2$fixedPower(double nativeSpeed){return this instanceof FixedArrowDamage?1:nativeSpeed;}
    @ModifyArg(method="onHitEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/enchantment/EnchantmentHelper;modifyDamage(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F"),index=4)
    private float skywars2$fixedArrow(float nativeDamage){return this instanceof FixedArrowDamage arrow?arrow.fixedArrowDamage():nativeDamage;}
}
