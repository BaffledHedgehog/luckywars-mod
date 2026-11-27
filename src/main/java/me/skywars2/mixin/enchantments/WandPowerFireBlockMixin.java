package me.skywars2.mixin.enchantments;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.skywars2.game.combat.WeaponEnchantments;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseFireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Scope the native fire-block callback, not unrelated in-fire damage. */
@Mixin(BaseFireBlock.class)
public abstract class WandPowerFireBlockMixin {
    @WrapOperation(method="lambda$entityInside$0",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void skywars2$nativeFireBlock(Entity entity,DamageSource source,float amount,Operation<Void> original){
        if(!(entity instanceof LivingEntity living)||!source.is(DamageTypes.IN_FIRE)){original.call(entity,source,amount);return;}
        try(var ignored=WeaponEnchantments.nativeFire(living,source)){original.call(entity,source,amount);}
    }
}
