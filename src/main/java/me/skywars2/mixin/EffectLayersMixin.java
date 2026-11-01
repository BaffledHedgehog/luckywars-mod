package me.skywars2.mixin;

import me.skywars2.game.EffectLayers;
import me.skywars2.game.OwnedEffectLayers;
import me.skywars2.SkyWars2;
import java.util.Collection;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class EffectLayersMixin {
    private EffectLayersMixin() { }

    @Mixin(MobEffectInstance.class)
    public abstract static class Instance implements EffectLayers.Access, OwnedEffectLayers.ClockAccess {
        @Shadow private MobEffectInstance hiddenEffect;
        @Shadow private int duration;
        @Override public void skywars2$layerDuration(int value) { duration = value; }
        @Override public MobEffectInstance skywars2$layerHidden() { return hiddenEffect; }
        @Override public void skywars2$layerHidden(MobEffectInstance effect) { hiddenEffect = effect; }
        @Override @Invoker("setDetailsFrom")
        public abstract void skywars2$layerDetails(MobEffectInstance effect);

        @Override @Invoker("tickDownDuration")
        public abstract void skywars2$ownedTickDuration();

        @Inject(method = "tickServer", at = @At(value = "INVOKE",
                target = "Lnet/minecraft/world/effect/MobEffectInstance;tickDownDuration()V"))
        private void skywars2$clockOwnedLayers(ServerLevel level, LivingEntity target, Runnable update,
                                              CallbackInfoReturnable<Boolean> cir) {
            var game = SkyWars2.game();
            if (game != null) game.items().ownedEffects().tickNative(target, (MobEffectInstance)(Object)this);
        }

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
    public abstract static class Living implements OwnedEffectLayers.UpdateAccess {
        @Shadow protected abstract void onEffectUpdated(MobEffectInstance effect, boolean refresh, Entity source);

        @Override @Invoker("onEffectUpdated")
        public abstract void skywars2$ownedUpdated(MobEffectInstance effect, boolean refresh, Entity source);

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
            // Cancellation returns before the original method's RETURN injection sites.
            var game = SkyWars2.game();
            if (game != null) game.items().ownedEffects().acceptedExternal((LivingEntity)(Object)this, grant);
            cir.setReturnValue(true);
        }

        @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
                at = @At("RETURN"))
        private void skywars2$observeAcceptedGrant(MobEffectInstance grant, Entity source, CallbackInfoReturnable<Boolean> cir) {
            var game = SkyWars2.game();
            if (game != null && cir.getReturnValueZ())
                game.items().ownedEffects().acceptedExternal((LivingEntity)(Object)this, grant);
        }

        @Inject(method = "forceAddEffect", at = @At("RETURN"))
        private void skywars2$observeReplacement(MobEffectInstance grant, Entity source, CallbackInfo ci) {
            var entity = (LivingEntity)(Object)this;
            var game = SkyWars2.game();
            if (game != null && entity.getEffect(grant.getEffect()) == grant)
                game.items().ownedEffects().removedExternal(entity, grant.getEffect());
        }

        @Inject(method = "removeEffectNoUpdate", at = @At("RETURN"))
        private void skywars2$observeRemoval(Holder<MobEffect> type, CallbackInfoReturnable<MobEffectInstance> cir) {
            var game = SkyWars2.game();
            if (game != null && cir.getReturnValue() != null)
                game.items().ownedEffects().removedExternal((LivingEntity)(Object)this, type);
        }

        @Inject(method = "onEffectsRemoved", at = @At("HEAD"))
        private void skywars2$observeClear(Collection<MobEffectInstance> effects, CallbackInfo ci) {
            var game = SkyWars2.game();
            if (game != null) for (var effect : effects)
                game.items().ownedEffects().removedExternal((LivingEntity)(Object)this, effect.getEffect());
        }
    }
}
