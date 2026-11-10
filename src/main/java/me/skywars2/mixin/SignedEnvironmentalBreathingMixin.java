package me.skywars2.mixin;

import me.skywars2.game.VanillaExpansionRules;

import me.skywars2.game.SignedEnvironmentalEffects;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffectUtil.class)
public abstract class SignedEnvironmentalBreathingMixin {
    @Inject(method="hasWaterBreathing",at=@At("HEAD"),cancellable=true)
    private static void skywars2$signedBreathing(LivingEntity mob,CallbackInfoReturnable<Boolean> ci) {
        if(!VanillaExpansionRules.enabled()){return;}
        ci.setReturnValue(SignedEnvironmentalEffects.breathes(mob));
    }
    @Inject(method="shouldEffectsRefillAirsupply",at=@At("HEAD"),cancellable=true)
    private static void skywars2$positiveNautilusRefills(LivingEntity mob,CallbackInfoReturnable<Boolean> ci) {
        if(!VanillaExpansionRules.enabled()){return;}
        ci.setReturnValue(true);
    }
}
