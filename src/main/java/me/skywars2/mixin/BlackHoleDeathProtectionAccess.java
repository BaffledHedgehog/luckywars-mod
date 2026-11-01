package me.skywars2.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.network.syncher.EntityDataAccessor;

/** Retain native death-protection items without invoking the hit animation pipeline. */
@Mixin(LivingEntity.class)
public interface BlackHoleDeathProtectionAccess {
    @Invoker("checkTotemDeathProtection") boolean skywars2$protectFromHole(DamageSource source);
    @Invoker("playHurtSound") void skywars2$holeHurtSound(DamageSource source);
    @Invoker("playSecondaryHurtSound") void skywars2$holeSecondarySound(DamageSource source);
    @Invoker("getDeathSound") net.minecraft.sounds.SoundEvent skywars2$holeDeathSound();
    @Invoker("getHurtSound") net.minecraft.sounds.SoundEvent skywars2$holeHurtSoundType(DamageSource source);
    @Invoker("getSoundVolume") float skywars2$holeSoundVolume();
    @Invoker("resolveMobResponsibleForDamage") void skywars2$holeMobCredit(DamageSource source);
    @Invoker("resolvePlayerResponsibleForDamage") net.minecraft.world.entity.player.Player skywars2$holePlayerCredit(DamageSource source);
    @Accessor("DATA_HEALTH_ID") static EntityDataAccessor<Float> skywars2$healthData(){throw new AssertionError();}
}
