package me.skywars2.mixin;

import me.skywars2.game.EffectLayers;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class EffectLayersMixin {
    private EffectLayersMixin() { }

    @Mixin(MobEffectInstance.class)
    public abstract static class Instance implements EffectLayers.Access {
        @Shadow private MobEffectInstance hiddenEffect;
        @Override public MobEffectInstance skywars2$layerHidden() { return hiddenEffect; }
        @Override public void skywars2$layerHidden(MobEffectInstance effect) { hiddenEffect = effect; }
        @Override @Invoker("setDetailsFrom")
        public abstract void skywars2$layerDetails(MobEffectInstance effect);

        @Inject(method = "update", at = @At("HEAD"), cancellable = true)
        private void skywars2$mergeLayers(MobEffectInstance grant, CallbackInfoReturnable<Boolean> cir) {
            var active = (MobEffectInstance)(Object)this;
            if (!active.is(MobEffects.ABSORPTION)) cir.setReturnValue(EffectLayers.merge(active, grant));
        }

        @Inject(method = "downgradeToHiddenEffect", at = @At("HEAD"), cancellable = true)
        private void skywars2$skipExpiredLayers(CallbackInfoReturnable<Boolean> cir) {
            var active = (MobEffectInstance)(Object)this;
            if (!active.is(MobEffects.ABSORPTION)) cir.setReturnValue(EffectLayers.downgrade(active));
        }
    }

    @Mixin(LivingEntity.class)
    public abstract static class Living {
        @Shadow protected abstract void onEffectUpdated(MobEffectInstance effect, boolean refresh, Entity source);

        // This point is after all existing HEAD guards and native canBeAffected admission.
        @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
                at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffectInstance;update(Lnet/minecraft/world/effect/MobEffectInstance;)Z"),
                cancellable = true)
        private void skywars2$acceptHiddenGrant(MobEffectInstance grant, Entity source, CallbackInfoReturnable<Boolean> cir) {
            if (grant.is(MobEffects.ABSORPTION)) return;
            if (!EffectLayers.live(grant)) { cir.setReturnValue(false); return; }
            var active = ((LivingEntity)(Object)this).getEffect(grant.getEffect());
            int oldAmplifier = active.getAmplifier();
            if (active.update(grant)) onEffectUpdated(active, true, source);
            // Hidden grants cannot restart the active effect's behavior or refresh its modifiers.
            if (grant.getAmplifier() >= oldAmplifier) grant.onEffectStarted((LivingEntity)(Object)this);
            cir.setReturnValue(true);
        }
    }
}
