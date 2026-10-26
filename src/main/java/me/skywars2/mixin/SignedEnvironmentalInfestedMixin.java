package me.skywars2.mixin;

import me.skywars2.game.SignedEnvironmentalEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="net.minecraft.world.effect.InfestedMobEffect")
public abstract class SignedEnvironmentalInfestedMixin {
    @Inject(method="onMobHurt",at=@At("HEAD"),cancellable=true)
    private void skywars2$signedInfested(ServerLevel level,LivingEntity mob,int amplifier,DamageSource source,float damage,CallbackInfo ci) {
        SignedEnvironmentalEffects.infested(level,mob,amplifier);ci.cancel();
    }
}
