package me.skywars2.mixin;

import java.util.Collection;
import me.skywars2.game.ShadowAbsorption;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Runs after existing effect admission guards, retaining native callbacks and packets. */
@Mixin(value = LivingEntity.class, priority = 900)
public abstract class ShadowAbsorptionMixin implements ShadowAbsorption.Access {
    @Unique private final ShadowAbsorption.State skywars2$absorptionState = new ShadowAbsorption.State();
    @Override public ShadowAbsorption.State skywars2$shadowAbsorption() { return skywars2$absorptionState; }
    @Unique private LivingEntity skywars2$entity() { return (LivingEntity)(Object)this; }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true)
    private void skywars2$addStack(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (effect.is(MobEffects.ABSORPTION) && ShadowAbsorption.server(skywars2$entity()) && !skywars2$absorptionState.internal)
            cir.setReturnValue(ShadowAbsorption.add(skywars2$entity(), effect, source));
    }
    @Inject(method = "forceAddEffect", at = @At("HEAD"), cancellable = true)
    private void skywars2$forceStack(MobEffectInstance effect, Entity source, CallbackInfo ci) {
        if (effect.is(MobEffects.ABSORPTION) && ShadowAbsorption.server(skywars2$entity()) && !skywars2$absorptionState.internal) {
            ShadowAbsorption.add(skywars2$entity(), effect, source);
            ci.cancel();
        }
    }
    @Inject(method = "tickEffects", at = @At("HEAD"))
    private void skywars2$tickStacks(CallbackInfo ci) { ShadowAbsorption.tick(skywars2$entity()); }
    @Inject(method = "removeEffectNoUpdate", at = @At("RETURN"))
    private void skywars2$removeStacks(Holder<MobEffect> effect, CallbackInfoReturnable<MobEffectInstance> cir) {
        if (effect.equals(MobEffects.ABSORPTION) && ShadowAbsorption.server(skywars2$entity()))
            ShadowAbsorption.clear(skywars2$entity());
    }
    @Inject(method = "onEffectsRemoved", at = @At("HEAD"))
    private void skywars2$clearStacks(Collection<MobEffectInstance> effects, CallbackInfo ci) {
        if (effects.stream().anyMatch(e -> e.is(MobEffects.ABSORPTION)) && ShadowAbsorption.server(skywars2$entity()))
            ShadowAbsorption.clear(skywars2$entity());
    }
    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    private void skywars2$healShadow(float amount, CallbackInfo ci) {
        if (ShadowAbsorption.heal(skywars2$entity(), amount)) ci.cancel();
    }
    @Inject(method = "getMaxHealth", at = @At("RETURN"), cancellable = true)
    private void skywars2$zeroVisibleMaximum(CallbackInfoReturnable<Float> cir) {
        var s = skywars2$absorptionState;
        if (s != null && !s.reconciling && !s.loading && s.capacity > 0 && ShadowAbsorption.server(skywars2$entity()))
            cir.setReturnValue((float)Math.min(cir.getReturnValueF(), s.visibleMaximum));
    }
    @Inject(method = "onAttributeUpdated", at = @At("HEAD"))
    private void skywars2$maximumChanged(Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, CallbackInfo ci) {
        if (attribute.equals(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                && !skywars2$absorptionState.entries.isEmpty()) ShadowAbsorption.reconcile(skywars2$entity(), false);
    }
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void skywars2$saveStacks(ValueOutput output, CallbackInfo ci) { ShadowAbsorption.save(skywars2$entity(), output); }
    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void skywars2$startLoad(ValueInput input, CallbackInfo ci) { ShadowAbsorption.beforeLoad(skywars2$entity()); }
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void skywars2$loadStacks(ValueInput input, CallbackInfo ci) { ShadowAbsorption.load(skywars2$entity(), input); }

    @Mixin(MobEffect.class)
    public abstract static class Attributes {
        @Inject(method = "addAttributeModifiers", at = @At("HEAD"), cancellable = true)
        private void skywars2$ownCapacity(AttributeMap attributes, int amplifier, CallbackInfo ci) {
            if ((Object)this == MobEffects.ABSORPTION.value()) ci.cancel();
        }
        @Inject(method = "removeAttributeModifiers", at = @At("HEAD"), cancellable = true)
        private void skywars2$ownRemoval(AttributeMap attributes, CallbackInfo ci) {
            if ((Object)this == MobEffects.ABSORPTION.value()) ci.cancel();
        }
    }
    @Mixin(targets = "net.minecraft.world.effect.AbsorptionMobEffect")
    public abstract static class Started {
        @Inject(method = "onEffectStarted", at = @At("HEAD"), cancellable = true)
        private void skywars2$ownRefill(LivingEntity entity, int amplifier, CallbackInfo ci) {
            if (ShadowAbsorption.server(entity)) ci.cancel();
        }
    }
    @Mixin(MobEffectInstance.class)
    public abstract static class Clock implements ShadowAbsorption.HiddenAccess {
        @Shadow private int duration;
        @Accessor("hiddenEffect") public abstract MobEffectInstance skywars2$absorptionHidden();
        @Inject(method = "tickServer", at = @At("HEAD"), cancellable = true)
        private void skywars2$ownClock(ServerLevel level, LivingEntity entity, Runnable update, CallbackInfoReturnable<Boolean> cir) {
            var effect = (MobEffectInstance)(Object)this;
            if (effect.is(MobEffects.ABSORPTION)) {
                var entry = ShadowAbsorption.state(entity).entries.get(effect.getAmplifier());
                if (entry != null) duration = entry.getDuration();
                cir.setReturnValue(true);
            }
        }
    }
    @Mixin(Player.class)
    public abstract static class FoodHealing {
        @Inject(method = "isHurt", at = @At("RETURN"), cancellable = true)
        private void skywars2$foodCanHealShadow(CallbackInfoReturnable<Boolean> cir) {
            if (!cir.getReturnValueZ() && ShadowAbsorption.server((Player)(Object)this)
                    && ShadowAbsorption.needsHealing((Player)(Object)this)) cir.setReturnValue(true);
        }
    }
}
