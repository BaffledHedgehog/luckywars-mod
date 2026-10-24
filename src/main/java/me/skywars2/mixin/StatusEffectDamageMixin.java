package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.PoisonMobEffect;
import net.minecraft.world.effect.WitherMobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Сохраняет источник нативных тиков яда и иссушения, не подменяя их DamageSource. */
@Mixin(value={PoisonMobEffect.class, WitherMobEffect.class},targets="net.minecraft.world.effect.HealOrHarmMobEffect")
public abstract class StatusEffectDamageMixin {
    @WrapOperation(method="applyEffectTick",at=@At(value="INVOKE",
            target="Lnet/minecraft/world/entity/LivingEntity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean skywars2$statusOwner(LivingEntity target, ServerLevel level, DamageSource source,
                                       float amount, Operation<Boolean> original) {
        var game=SkyWars2.game();
        if(game==null) return original.call(target,level,source,amount);
        var effect=source.is(DamageTypes.WITHER)?MobEffects.WITHER:(Object)this instanceof PoisonMobEffect?MobEffects.POISON:
                (Object)this==MobEffects.INSTANT_DAMAGE.value()?MobEffects.INSTANT_DAMAGE:MobEffects.INSTANT_HEALTH;
        return game.items().indirectKills().statusDamage(target,effect,
                ()->original.call(target,level,source,amount));
    }
}
