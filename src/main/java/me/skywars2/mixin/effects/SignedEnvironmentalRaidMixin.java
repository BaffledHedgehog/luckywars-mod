package me.skywars2.mixin.effects;

import me.skywars2.game.effects.SignedEnvironmentalEffects;
import me.skywars2.game.modes.legacy.VanillaExpansionRules;

import com.llamalad7.mixinextras.injector.wrapoperation.*;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.raid.Raid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Raid.class)
public abstract class SignedEnvironmentalRaidMixin {
    @WrapOperation(method="absorbRaidOmen",at=@At(value="INVOKE",target="Lnet/minecraft/world/effect/MobEffectInstance;getAmplifier()I"))
    private int skywars2$boundedRaidMechanics(MobEffectInstance effect,Operation<Integer> original) {
        if(!VanillaExpansionRules.enabled()){return original.call(effect);}
        return SignedEnvironmentalEffects.omenAmplifier(original.call(effect));
    }
}
