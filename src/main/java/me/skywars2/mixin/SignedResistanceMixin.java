package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.skywars2.game.SignedEffectMath;
import me.skywars2.game.LbcGreed;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class SignedResistanceMixin {
    @ModifyVariable(method="getDamageAfterMagicAbsorb",at=@At("HEAD"),argsOnly=true)
    private float skywars2$signedResistance(float damage,DamageSource source) {
        var game=me.skywars2.SkyWars2.game();if(game!=null&&game.balanceLegacy())return damage;
        var entity=(LivingEntity)(Object)this;
        if(source.is(DamageTypeTags.BYPASSES_EFFECTS)||source.is(DamageTypeTags.BYPASSES_RESISTANCE))return damage;
        long level=SignedEffectMath.level(entity,MobEffects.RESISTANCE);
        if(level==0)return damage;
        float result=SignedEffectMath.finite(damage*(1-.2*level));
        if(result<0) {
            if(!LbcGreed.active(entity))entity.heal(-result);
            result=0;
        }
        float resisted=damage-result;
        if(resisted>0&&resisted<3.4028235E37f) {
            if(entity instanceof ServerPlayer player)player.awardStat(Stats.DAMAGE_RESISTED,Math.round(resisted*10));
            else if(source.getEntity() instanceof ServerPlayer player)player.awardStat(Stats.DAMAGE_DEALT_RESISTED,Math.round(resisted*10));
        }
        return result;
    }
    @WrapOperation(method="getDamageAfterMagicAbsorb",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z"))
    private boolean skywars2$skipOverflowingNativeResistance(LivingEntity entity,Holder<MobEffect> effect,Operation<Boolean> original) {
        var game=me.skywars2.SkyWars2.game();if(game!=null&&game.balanceLegacy())return original.call(entity,effect);
        return effect.is(MobEffects.RESISTANCE)?false:original.call(entity,effect);
    }
}
