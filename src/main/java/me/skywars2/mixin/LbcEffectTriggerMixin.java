package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.advancements.triggers.EffectsChangedTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Подхватывает исходный триггер изменения эффектов для всех штатных источников зелий. */
@Mixin(EffectsChangedTrigger.class)
public abstract class LbcEffectTriggerMixin {
    @Inject(method = "trigger", at = @At("TAIL"))
    private void skywars2$lbcEffects(ServerPlayer player, Entity source, CallbackInfo ci) {
        var game = SkyWars2.game();
        if (game != null) game.items().effectsChanged(player);
    }
}
