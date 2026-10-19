package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class ObserverPlayerMixin {
    /** Клиент остаётся в adventure; серверная физика и AI исключают наблюдателя. Сам @a всё ещё включает его. */
    @Inject(method = "isSpectator", at = @At("HEAD"), cancellable = true)
    private void skywars2$observer(CallbackInfoReturnable<Boolean> ci) {
        if ((Object) this instanceof ServerPlayer player && SkyWars2.game() != null
                && SkyWars2.game().isObserver(player)) ci.setReturnValue(true);
    }
}
