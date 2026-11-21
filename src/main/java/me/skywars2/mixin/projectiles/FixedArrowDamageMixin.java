package me.skywars2.mixin.projectiles;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import me.skywars2.game.combat.FixedArrowDamage;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(AbstractArrow.class)
public abstract class FixedArrowDamageMixin {
    @ModifyExpressionValue(method="onHitEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/phys/Vec3;length()D"))
    private double skywars2$fixedPower(double nativeSpeed){return this instanceof FixedArrowDamage?1:nativeSpeed;}
    @ModifyArg(method="onHitEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/enchantment/EnchantmentHelper;modifyDamage(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F"),index=4)
    private float skywars2$fixedArrow(float nativeDamage){return this instanceof FixedArrowDamage arrow?arrow.fixedArrowDamage():nativeDamage;}
}
