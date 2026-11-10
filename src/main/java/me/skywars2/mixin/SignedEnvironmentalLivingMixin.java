package me.skywars2.mixin;

import me.skywars2.game.VanillaExpansionRules;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import me.skywars2.game.SignedEnvironmentalEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LivingEntity.class)
public abstract class SignedEnvironmentalLivingMixin {
    @WrapOperation(method="hurtServer",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z"))
    private boolean skywars2$signedFireGuard(LivingEntity self,Holder<MobEffect> effect,Operation<Boolean> original) {
        if(!VanillaExpansionRules.enabled()){return original.call(self,effect);}
        return effect.equals(MobEffects.FIRE_RESISTANCE)?SignedEnvironmentalEffects.level(self,effect)>0:original.call(self,effect);
    }
    @ModifyVariable(method="hurtServer",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private float skywars2$signedFireDamage(float amount,ServerLevel level,DamageSource source,float originalAmount) {
        if(!VanillaExpansionRules.enabled()){return amount;}
        var self=(LivingEntity)(Object)this;
        if(!source.is(DamageTypeTags.IS_FIRE))return amount;
        return (float)Math.min(Float.MAX_VALUE,amount*SignedEnvironmentalEffects.fireMultiplier(SignedEnvironmentalEffects.level(self,MobEffects.FIRE_RESISTANCE)));
    }
    @WrapMethod(method="decreaseAirSupply")
    private int skywars2$signedAirDepletion(int current,Operation<Integer> original) {
        if(!VanillaExpansionRules.enabled()){return original.call(current);}
        var self=(LivingEntity)(Object)this;long penalty=SignedEnvironmentalEffects.airPenalty(self);
        int result=original.call(current);
        return penalty>0&&!SignedEnvironmentalEffects.protectedTarget(self)?SignedEnvironmentalEffects.drainAir(result,penalty):result;
    }
    @Inject(method="increaseAirSupply",at=@At("HEAD"),cancellable=true)
    private void skywars2$outsideBubbles(int current,CallbackInfoReturnable<Integer> ci) {
        if(!VanillaExpansionRules.enabled()){return;}
        if(SignedEnvironmentalEffects.outsideDrain((LivingEntity)(Object)this))ci.setReturnValue(current);
    }
    @Inject(method="baseTick",at=@At("TAIL"))
    private void skywars2$signedEnvironmentalTick(CallbackInfo ci) {
        if(!VanillaExpansionRules.enabled()){return;}
        SignedEnvironmentalEffects.airTick((LivingEntity)(Object)this);
        SignedEnvironmentalEffects.fireTick((LivingEntity)(Object)this);
    }
    @Inject(method="onEffectAdded",at=@At("TAIL"))
    private void skywars2$environmentAuthorAdded(net.minecraft.world.effect.MobEffectInstance effect,net.minecraft.world.entity.Entity source,CallbackInfo ci) {
        if(!VanillaExpansionRules.enabled()){return;}
        SignedEnvironmentalEffects.effectChanged((LivingEntity)(Object)this,effect,source);
    }
    @Inject(method="onEffectUpdated",at=@At("TAIL"))
    private void skywars2$environmentAuthorUpdated(net.minecraft.world.effect.MobEffectInstance effect,boolean refresh,net.minecraft.world.entity.Entity source,CallbackInfo ci) {
        if(!VanillaExpansionRules.enabled()){return;}
        if(refresh)SignedEnvironmentalEffects.effectChanged((LivingEntity)(Object)this,effect,source);
    }
}
