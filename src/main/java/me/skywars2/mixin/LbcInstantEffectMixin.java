package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Splash/cloud instant effects обходят addEffect; отменяем их до непосредственного heal/hurt. */
@Mixin(targets = {"net.minecraft.world.effect.MobEffect", "net.minecraft.world.effect.HealOrHarmMobEffect"})
public abstract class LbcInstantEffectMixin {
    @Inject(method = "applyInstantaneousEffect", at = @At("HEAD"), cancellable = true)
    private void skywars2$artifactInstantEffect(ServerLevel level, Entity source, Entity owner,
            LivingEntity target, int amplifier, double scale, CallbackInfo ci) {
        var game = SkyWars2.game();
        if (game != null && target instanceof ServerPlayer player && game.items().magic().rejectEffect(player,
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder((MobEffect)(Object)this))) ci.cancel();
    }
}
