package me.skywars2.mixin.enchantments;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.skywars2.game.combat.WeaponEnchantments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Scope only the actual native periodic burn and lava contact invocations. */
@Mixin(Entity.class)
public abstract class WandPowerFireEntityMixin {
    @WrapOperation(method={"baseTick","lavaHurt"},at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean skywars2$nativeFire(Entity entity,ServerLevel level,DamageSource source,float amount,Operation<Boolean> original){
        if(!(entity instanceof LivingEntity living)||!(source.is(DamageTypes.ON_FIRE)||source.is(DamageTypes.LAVA)))
            return original.call(entity,level,source,amount);
        try(var ignored=WeaponEnchantments.nativeFire(living,source)){return original.call(entity,level,source,amount);}
    }
}
