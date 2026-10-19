package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.advancements.triggers.UsedTotemTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Срабатывает до ванильного удаления эффектов, сохраняя порядок выдачи награды из датапака. */
@Mixin(UsedTotemTrigger.class)
public abstract class LbcTotemTriggerMixin {
    @Inject(method = "trigger", at = @At("TAIL"))
    private void skywars2$lbcTotem(ServerPlayer player, ItemStack stack, CallbackInfo ci) {
        var game = SkyWars2.game();
        if (game != null) game.items().usedTotem(player, stack);
    }
}
