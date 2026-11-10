package me.skywars2.mixin;

import me.skywars2.game.VanillaExpansionRules;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.skywars2.game.SignedEffectMath;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class SignedMovementEffectsMixin {
    private SignedMovementEffectsMixin() { }
    @Mixin(LivingEntity.class)
    public abstract static class Living {
        @Inject(method="baseTick",at=@At("TAIL"))
        private void skywars2$swimmingAttributes(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if(!VanillaExpansionRules.enabled()){return;}
            SignedEffectMath.dolphin((LivingEntity)(Object)this);
        }
        @Inject(method="getJumpBoostPower",at=@At("HEAD"),cancellable=true)
        private void skywars2$jump(CallbackInfoReturnable<Float> cir) {
        if(!VanillaExpansionRules.enabled()){return;}
            var entity=(LivingEntity)(Object)this;long level=SignedEffectMath.level(entity,MobEffects.JUMP_BOOST);
            cir.setReturnValue(entity instanceof Player&&level<0?0:SignedEffectMath.finite(.1*level));
        }
        @ModifyExpressionValue(method="travelInAir",at=@At(value="INVOKE",target="Lnet/minecraft/world/effect/MobEffectInstance;getAmplifier()I"))
        private int skywars2$widenLevitation(int amplifier) {
        if(!VanillaExpansionRules.enabled()){return amplifier;} return 0; }
        @ModifyConstant(method="travelInAir",constant=@Constant(doubleValue=.05))
        private double skywars2$levitationVelocity(double constant) {
        if(!VanillaExpansionRules.enabled()){return constant;}
            var entity=(LivingEntity)(Object)this;
            var effect=entity.getEffect(MobEffects.LEVITATION);
            if(effect==null)return constant;
            long level=entity instanceof Player?Math.max(0,Math.min(255,effect.getAmplifier()))+1L:SignedEffectMath.level(effect.getAmplifier());
            return constant*level;
        }
        @WrapOperation(method="calculateFallPower",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
        private double skywars2$jumpSafeFall(LivingEntity entity,Holder<Attribute> attribute,Operation<Double> original) {
        if(!VanillaExpansionRules.enabled()){return original.call(entity,attribute);}
            double value=original.call(entity,attribute);
            return attribute.is(Attributes.SAFE_FALL_DISTANCE)?SignedEffectMath.safeJumpFallDistance(entity,SignedEffectMath.finite(value)):value;
        }
        @WrapOperation(method={"getEffectiveGravity","aiStep","causeFallDamage"},at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z"),require=0)
        private boolean skywars2$nonpositiveSlowFall(LivingEntity entity,Holder<MobEffect> effect,Operation<Boolean> original) {
        if(!VanillaExpansionRules.enabled()){return original.call(entity,effect);}
            // A negative slow-falling effect is neutral for players: no corrective motion packets.
            return effect.is(MobEffects.SLOW_FALLING)?SignedEffectMath.level(entity,effect)>0:original.call(entity,effect);
        }
        @WrapOperation(method="travelInWater",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z"))
        private boolean skywars2$dolphinPositive(LivingEntity entity,Holder<MobEffect> effect,Operation<Boolean> original) {
        if(!VanillaExpansionRules.enabled()){return original.call(entity,effect);}
            return effect.is(MobEffects.DOLPHINS_GRACE)?SignedEffectMath.level(entity,effect)>0:original.call(entity,effect);
        }
        @Inject(method="getModifiedSwingDuration",at=@At("HEAD"),cancellable=true)
        private void skywars2$finiteSwing(net.minecraft.world.item.component.SwingAnimation animation,CallbackInfoReturnable<Integer> cir) {
        if(!VanillaExpansionRules.enabled()){return;}
            var entity=(LivingEntity)(Object)this;
            double duration=animation.duration();
            if(MobEffectUtil.hasDigSpeed(entity))duration-=SignedEffectMath.level(MobEffectUtil.getDigSpeedAmplification(entity));
            else duration+=2*SignedEffectMath.level(entity,MobEffects.MINING_FATIGUE);
            cir.setReturnValue((int)Math.max(1,Math.min(Integer.MAX_VALUE,duration)));
        }
    }
    @Mixin(MobEffectUtil.class)
    public abstract static class DigSpeed {
        @Inject(method="getDigSpeedAmplification",at=@At("HEAD"),cancellable=true)
        private static void skywars2$actualDigEffects(LivingEntity entity,CallbackInfoReturnable<Integer> cir) {
        if(!VanillaExpansionRules.enabled()){return;}
            MobEffectInstance haste=entity.getEffect(MobEffects.HASTE),conduit=entity.getEffect(MobEffects.CONDUIT_POWER);
            cir.setReturnValue(haste==null?(conduit==null?-1:conduit.getAmplifier()):conduit==null?haste.getAmplifier():Math.max(haste.getAmplifier(),conduit.getAmplifier()));
        }
    }
    @Mixin(Player.class)
    public abstract static class Mining {
        @Inject(method="getDestroySpeed",at=@At("HEAD"),cancellable=true)
        private void skywars2$finiteDigging(BlockState state,CallbackInfoReturnable<Float> cir) {
        if(!VanillaExpansionRules.enabled()){return;}
            var entity=(Player)(Object)this;
            double speed=entity.getInventory().getSelectedItem().getDestroySpeed(state);
            if(speed>1)speed+=entity.getAttributeValue(Attributes.MINING_EFFICIENCY);
            if(MobEffectUtil.hasDigSpeed(entity))speed*=1+.2*SignedEffectMath.level(MobEffectUtil.getDigSpeedAmplification(entity));
            var fatigue=entity.getEffect(MobEffects.MINING_FATIGUE);
            if(fatigue!=null)speed*=SignedEffectMath.fatigue(fatigue.getAmplifier());
            speed*=entity.getAttributeValue(Attributes.BLOCK_BREAK_SPEED);
            if(entity.isEyeInFluid(net.minecraft.tags.FluidTags.WATER))speed*=entity.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED);
            if(!entity.onGround())speed/=5;
            cir.setReturnValue(SignedEffectMath.finite(speed));
        }
    }
}
