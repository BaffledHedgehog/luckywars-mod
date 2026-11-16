package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Смена инвентаря планирует один пересчёт артефакта за тик вместо постоянного опроса игроков. */
@Mixin(InventoryChangeTrigger.class)
public abstract class LbcInventoryTriggerMixin {
    @Inject(method = "trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/ItemStack;)V", at = @At("TAIL"))
    private void skywars2$inventory(ServerPlayer player, Inventory inventory, ItemStack changed, CallbackInfo ci) {
        // The criterion receives a menu snapshot, not the actual slot stack.
        // Canonicalize real inventory items so packet and server data agree.
        if(me.skywars2.game.ItemData.intValue(changed,"antimagic",0)==1)
            for(int slot=0;slot<inventory.getContainerSize();slot++)
                me.skywars2.game.LbcPendingSwords.canonicalizeAntimagic(inventory.getItem(slot));
        var game = SkyWars2.game();
        if (game != null) {
            game.lbc().inventoryChanged(player);
            game.items().magic().inventoryChanged(player);
            game.items().supernova().craftChanged(player);
            game.items().balance().inventoryChanged(player);game.items().cursedPumpkin().inventoryChanged(player);game.items().touhouCollectibles().inventoryChanged(player);game.items().reactiveTrinkets().inventoryChanged(player);
            game.items().malevich().inventoryChanged(player,changed);
            game.items().refreshMinigunMovement(player);
        }
    }
}
