package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep world items out of the fixed lobby inventory. */
@Mixin(ItemEntity.class)
public abstract class LobbyItemPickupMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void skywars2$lockPickup(Player player, CallbackInfo ci) {
        var game = SkyWars2.game();
        if (player instanceof ServerPlayer && game != null && game.inventoryLocked()) ci.cancel();
    }
}
