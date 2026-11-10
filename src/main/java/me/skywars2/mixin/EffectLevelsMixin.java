package me.skywars2.mixin;

import me.skywars2.game.VanillaExpansionRules;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Full signed-int amplifier storage; effect-specific formulas are a separate policy. */
@Mixin(MobEffectInstance.class)
public abstract class EffectLevelsMixin {
    @Shadow private int amplifier;

    @Inject(method = "<init>(Lnet/minecraft/core/Holder;IIZZZLnet/minecraft/world/effect/MobEffectInstance;)V",
            at = @At("TAIL"))
    private void skywars2$fullAmplifier(Holder<MobEffect> effect, int duration, int amplifier,
                                       boolean ambient, boolean visible, boolean showIcon,
                                       MobEffectInstance hidden, CallbackInfo ci) {
        if(!VanillaExpansionRules.enabled()){return;}
        this.amplifier = amplifier;
    }

    @Mixin(targets = "net.minecraft.world.effect.MobEffectInstance$Details")
    public abstract static class Details {
        @ModifyExpressionValue(method = "lambda$static$1", at = @At(value = "FIELD", target =
                "Lnet/minecraft/util/ExtraCodecs;UNSIGNED_BYTE:Lcom/mojang/serialization/Codec;"))
        private static Codec<Integer> skywars2$signedPersistentAmplifier(Codec<Integer> original) {
            return Codec.INT;
        }
    }
}
