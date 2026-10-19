package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancements.class)
public abstract class ObserverAdvancementMixin {
    @Shadow private ServerPlayer player;

    /** Копия инвентаря и местоположение камеры не должны открывать наблюдателю достижения цели. */
    @Inject(method = "award", at = @At("HEAD"), cancellable = true)
    private void skywars2$noWatchUnlocks(AdvancementHolder advancement, String criterion, CallbackInfoReturnable<Boolean> ci) {
        if (SkyWars2.game() != null && SkyWars2.game().isObserver(player)) ci.setReturnValue(false);
    }

    @Inject(method = "award", at = @At("RETURN"))
    private void skywars2$notifyNativeUnlock(AdvancementHolder advancement, String criterion, CallbackInfoReturnable<Boolean> ci) {
        if (Boolean.TRUE.equals(ci.getReturnValue()) && SkyWars2.game() != null) {
            SkyWars2.game().lbc().advancementAwarded(player, advancement);
        }
    }
}
