package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public abstract class ObserverInventoryMixin {
    @Shadow @Final public Player player;

    /** Зеркальные предметы существуют для просмотра, а не для запуска их inventoryTick. */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void skywars2$displayOnly(CallbackInfo ci) {
        if (player instanceof ServerPlayer viewer && SkyWars2.game() != null && SkyWars2.game().isObserver(viewer)) ci.cancel();
    }
}
