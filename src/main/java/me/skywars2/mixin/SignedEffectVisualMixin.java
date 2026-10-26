package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.skywars2.game.SignedEffectVisuals;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.*;

public final class SignedEffectVisualMixin {
    private SignedEffectVisualMixin() { }
    @Mixin(Entity.class)
    public interface Flags {
        @Accessor("DATA_SHARED_FLAGS_ID") static EntityDataAccessor<Byte> flags() { throw new AssertionError(); }
    }
    @Mixin(LivingEntity.class)
    public interface Particles {
        @Accessor("DATA_EFFECT_PARTICLES") static EntityDataAccessor<List<ParticleOptions>> particles() { throw new AssertionError(); }
    }
    @Mixin(LivingEntity.class)
    public abstract static class Living {
        @ModifyExpressionValue(method="updateInvisibilityStatus",at=@At(value="INVOKE",
                target="Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z"))
        private boolean positiveInvisibility(boolean original) {
            return original && SignedEffectVisuals.level((LivingEntity)(Object)this, MobEffects.INVISIBILITY) > 0;
        }
        @ModifyExpressionValue(method="isCurrentlyGlowing",at=@At(value="INVOKE",
                target="Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/core/Holder;)Z"))
        private boolean signedGlowing(boolean original) {
            return original && SignedEffectVisuals.level((LivingEntity)(Object)this, MobEffects.GLOWING) > 0;
        }
        @Inject(method="isCurrentlyGlowing",at=@At("RETURN"),cancellable=true)
        private void negativeInvisibilityGlow(CallbackInfoReturnable<Boolean> ci) {
            if (SignedEffectVisuals.level((LivingEntity)(Object)this, MobEffects.INVISIBILITY) < 0) ci.setReturnValue(true);
        }
        @Inject(method={"onEffectAdded","onEffectUpdated","onEffectsRemoved","readAdditionalSaveData"},at=@At("TAIL"))
        private void effectsChanged(CallbackInfo ci) { SignedEffectVisuals.effectsChanged((LivingEntity)(Object)this); }
        @Inject(method="updateInvisibilityStatus",at=@At("TAIL"))
        private void visibilityUpdated(CallbackInfo ci) { SignedEffectVisuals.visibilityUpdated((LivingEntity)(Object)this); }
    }
    @Mixin(net.minecraft.world.entity.player.Player.class)
    public abstract static class Player {
        @ModifyExpressionValue(method="isMobilityRestricted",at=@At(value="INVOKE",
                target="Lnet/minecraft/world/entity/player/Player;hasEffect(Lnet/minecraft/core/Holder;)Z"))
        private boolean positiveBlindness(boolean original) {
            return original && SignedEffectVisuals.level((LivingEntity)(Object)this,MobEffects.BLINDNESS)>0;
        }
    }
    @Mixin(ClientboundSetEntityDataPacket.class)
    public abstract static class Metadata {
        @Shadow public abstract int id();
        @ModifyArg(method="write",at=@At(value="INVOKE",target=
                "Lnet/minecraft/network/protocol/game/ClientboundSetEntityDataPacket;pack(Ljava/util/List;Lnet/minecraft/network/RegistryFriendlyByteBuf;)V"),index=0)
        private List<SynchedEntityData.DataValue<?>> perViewer(List<SynchedEntityData.DataValue<?>> fields) {
            var context = PacketContext.get();
            return context == null ? fields : SignedEffectVisuals.project(id(), fields, context);
        }
    }
    @Mixin(ClientboundUpdateMobEffectPacket.class)
    public abstract static class ClientEffect {
        @Shadow @Final private Holder<MobEffect> effect;
        @Shadow @Final private int effectAmplifier;
        @ModifyExpressionValue(method="write",at=@At(value="FIELD",target=
                "Lnet/minecraft/network/protocol/game/ClientboundUpdateMobEffectPacket;effectDurationTicks:I"))
        private int nonpositiveDuration(int duration) {
            var context = PacketContext.get();
            return context == null || eu.pb4.polymer.common.api.PolymerCommonUtils.getPlayer(context) == null
                    ? duration : SignedEffectVisuals.clientDuration(effect, effectAmplifier, duration);
        }
    }
}
