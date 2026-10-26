package me.skywars2.mixin;

import me.skywars2.game.SignedEnvironmentalEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets={"net.minecraft.world.effect.BadOmenMobEffect","net.minecraft.world.effect.RaidOmenMobEffect"})
public abstract class SignedEnvironmentalOmenMixin {
    @ModifyVariable(method="applyEffectTick",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private int skywars2$boundedOmenConsumer(int amplifier) {
        // Also avoids Bad Omen's conversion manufacturing reserved Raid Omen 40..42 markers.
        return SignedEnvironmentalEffects.omenAmplifier(amplifier);
    }
    @Inject(method="applyEffectTick",at=@At("HEAD"),cancellable=true)
    private void skywars2$inertOmen(ServerLevel level,LivingEntity mob,int amplifier,CallbackInfoReturnable<Boolean> ci) {
        if(SignedEnvironmentalEffects.level(amplifier)<=0)ci.setReturnValue(true);
    }
}
