package me.skywars2.mixin;

import me.skywars2.game.VanillaExpansionRules;

import me.skywars2.game.SignedEffectMath;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class SignedEffectFormulaMixin {
    private SignedEffectFormulaMixin() { }
    @Mixin(targets={"net.minecraft.world.effect.RegenerationMobEffect","net.minecraft.world.effect.PoisonMobEffect","net.minecraft.world.effect.WitherMobEffect"})
    public abstract static class Periodic {
        @Inject(method="shouldApplyEffectTickThisTick",at=@At("HEAD"),cancellable=true)
        private void skywars2$signedCadence(int ticks,int amplifier,CallbackInfoReturnable<Boolean> cir) {
        if(!VanillaExpansionRules.enabled()){return;}
            int base=(Object)this==MobEffects.REGENERATION.value()?50:(Object)this==MobEffects.POISON.value()?25:40;
            cir.setReturnValue(SignedEffectMath.due(ticks,amplifier,base));
        }
        @Inject(method="applyEffectTick",at=@At("HEAD"),cancellable=true)
        private void skywars2$signedPeriodic(ServerLevel world,LivingEntity entity,int amplifier,CallbackInfoReturnable<Boolean> cir) {
        if(!VanillaExpansionRules.enabled()){return;}
            var effect=net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder((MobEffect)(Object)this);
            cir.setReturnValue(SignedEffectMath.periodic(world,entity,effect,amplifier));
        }
    }
    @Mixin(targets="net.minecraft.world.effect.HealOrHarmMobEffect",priority=900)
    public abstract static class Instant {
        @Shadow @Final private boolean isHarm;
        @Inject(method="applyEffectTick",at=@At("HEAD"),cancellable=true)
        private void skywars2$signedDirect(ServerLevel world,LivingEntity target,int amplifier,CallbackInfoReturnable<Boolean> cir) {
        if(!VanillaExpansionRules.enabled()){return;}
            SignedEffectMath.instant(world,null,null,target,amplifier,1,isHarm);cir.setReturnValue(true);
        }
        // Existing priority1000 admission runs first (artifact, ownership and absolute Greed guards).
        @Inject(method="applyInstantaneousEffect",at=@At("HEAD"),cancellable=true)
        private void skywars2$signedSplash(ServerLevel world,Entity source,Entity owner,LivingEntity target,int amplifier,double scale,CallbackInfo ci) {
        if(!VanillaExpansionRules.enabled()){return;}
            SignedEffectMath.instant(world,source,owner,target,amplifier,scale,isHarm);ci.cancel();
        }
    }
    @Mixin(targets="net.minecraft.world.effect.HungerMobEffect")
    public abstract static class Hunger {
        @Inject(method="applyEffectTick",at=@At("HEAD"),cancellable=true)
        private void skywars2$signedHunger(ServerLevel world,LivingEntity target,int amplifier,CallbackInfoReturnable<Boolean> cir) {
        if(!VanillaExpansionRules.enabled()){return;}
            SignedEffectMath.hunger(target,amplifier);cir.setReturnValue(true);
        }
    }
    @Mixin(targets="net.minecraft.world.effect.SaturationMobEffect")
    public abstract static class Saturation {
        @Inject(method="applyEffectTick",at=@At("HEAD"),cancellable=true)
        private void skywars2$signedSaturation(ServerLevel world,LivingEntity target,int amplifier,CallbackInfoReturnable<Boolean> cir) {
        if(!VanillaExpansionRules.enabled()){return;}
            SignedEffectMath.saturation(target,amplifier);cir.setReturnValue(true);
        }
    }
    @Mixin(net.minecraft.world.food.FoodData.class)
    public abstract static class Food implements SignedEffectMath.SignedFoodAccess {
        @Shadow private float exhaustionLevel;
        @Shadow private float saturationLevel;
        @Shadow private int foodLevel;
        @Override public void skywars2$restoreExhaustion(float amount) {
            float restored=Math.min(Math.max(0,exhaustionLevel),amount);exhaustionLevel-=restored;
            // Four exhaustion normally costs one saturation point: reverse that same rate.
            saturationLevel=Math.min(foodLevel,saturationLevel+(amount-restored)/4);
        }
    }
    @Mixin(targets="net.minecraft.world.effect.MobEffect$AttributeTemplate")
    public abstract static class AttributeMath {
        @Shadow @Final private double amount;
        @Shadow @Final private net.minecraft.resources.Identifier id;
        @Shadow @Final private net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation operation;
        @Inject(method="create",at=@At("HEAD"),cancellable=true)
        private void skywars2$wideModifier(int amplifier,CallbackInfoReturnable<net.minecraft.world.entity.ai.attributes.AttributeModifier> cir) {
        if(!VanillaExpansionRules.enabled()){return;}
            cir.setReturnValue(new net.minecraft.world.entity.ai.attributes.AttributeModifier(id,amount*SignedEffectMath.level(amplifier),operation));
        }
    }
}
