package me.skywars2.mixin;

import me.skywars2.game.VanillaExpansionRules;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import me.skywars2.game.SignedEnvironmentalEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets="net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerStateData")
public abstract class SignedEnvironmentalTrialMixin {
    @WrapOperation(method="findPlayerWithOminousEffect",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;hasEffect(Lnet/minecraft/core/Holder;)Z"))
    private static boolean skywars2$inertTrialOmen(Player player,Holder<MobEffect> effect,Operation<Boolean> original) {
        if(!VanillaExpansionRules.enabled()){return original.call(player,effect);}
        return original.call(player,effect)&&SignedEnvironmentalEffects.level(player,effect)>0;
    }
    @WrapOperation(method="transformBadOmenIntoTrialOmen",at=@At(value="INVOKE",target="Lnet/minecraft/world/effect/MobEffectInstance;getAmplifier()I"))
    private static int skywars2$boundedTrialDuration(MobEffectInstance effect,Operation<Integer> original) {
        if(!VanillaExpansionRules.enabled()){return original.call(effect);}
        return SignedEnvironmentalEffects.omenAmplifier(original.call(effect));
    }
}
