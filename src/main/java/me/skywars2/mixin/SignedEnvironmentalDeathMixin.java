package me.skywars2.mixin;

import me.skywars2.game.SignedEnvironmentalEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets={"net.minecraft.world.effect.WindChargedMobEffect","net.minecraft.world.effect.WeavingMobEffect","net.minecraft.world.effect.OozingMobEffect"})
public abstract class SignedEnvironmentalDeathMixin {
    @Inject(method="onMobRemoved",at=@At("HEAD"),cancellable=true)
    private void skywars2$signedDeath(ServerLevel level,LivingEntity mob,int amplifier,Entity.RemovalReason reason,CallbackInfo ci) {
        SignedEnvironmentalEffects.removed(level,mob,amplifier,reason,(MobEffect)(Object)this);ci.cancel();
    }
}
