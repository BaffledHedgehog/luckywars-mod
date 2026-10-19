package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public abstract class LbcCraftingResultMixin {
    @Shadow @Final private CraftingContainer craftSlots;

    /** Обычный клик и Shift-клик приходят сюда до расхода ингредиентов; preview сюда не приходит. */
    @Inject(method = "onTake", at = @At("HEAD"))
    private void skywars2$craftEffects(Player player, ItemStack result, CallbackInfo ci) {
        var game = SkyWars2.game();
        if (game != null && player instanceof ServerPlayer serverPlayer) {
            me.skywars2.game.LbcCraftingRecipe.craftTaken(serverPlayer,craftSlots.asCraftInput());
        }
    }
}
