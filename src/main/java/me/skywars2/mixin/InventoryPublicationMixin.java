package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.skywars2.game.ContainerClickSync;
import me.skywars2.game.LbcBulkLoot;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.RemoteSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep the outgoing slot image consistent with vanilla's remembered remote image. */
@Mixin(AbstractContainerMenu.class)
public abstract class InventoryPublicationMixin implements ContainerClickSync {
    @Unique private ServerPlayer skywars2$clickPlayer;
    @Unique private ServerboundContainerClickPacket skywars2$click;

    @Inject(method = "doClick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;getItem(I)Lnet/minecraft/world/item/ItemStack;"),
            cancellable = true)
    private void skywars2$swapStoredStack(int index, int button, ContainerInput input, Player player, CallbackInfo ci) {
        // This call site is in SWAP after vanilla resets any drag and validates the hotbar/offhand index.
        if (input != ContainerInput.SWAP || !(player instanceof ServerPlayer owner)
                || !owner.level().getServer().isSameThread() || index < 0) return;
        var menu = (AbstractContainerMenu)(Object)this;
        if (!menu.isValidSlotIndex(index)) return;
        var slot = menu.getSlot(index);
        var stack = slot.getItem();
        if (!LbcBulkLoot.isOversizedStoredStack(stack)) return;

        var inventory = owner.getInventory();
        if (inventory.getItem(button).isEmpty()) {
            int limit = inventory.getMaxStackSize(stack);
            slot.tryRemove(limit, limit, owner).ifPresent(taken -> {
                inventory.setItem(button, taken);
                inventory.setChanged();
                slot.onTake(owner, taken);
            });
        }
        // An occupied destination cannot exchange a legal portion while the source retains a remainder.
        // Chest menus have no offhand slot, so their regular slot corrections cannot fix its prediction.
        if (menu.findSlot(inventory, button).isEmpty()) owner.connection.send(inventory.createInventoryUpdatePacket(button));
        ci.cancel();
    }

    @Override
    public void skywars2$publishClick(ServerPlayer player, ServerboundContainerClickPacket click, Runnable broadcast) {
        var previousPlayer = skywars2$clickPlayer;
        var previousClick = skywars2$click;
        skywars2$clickPlayer = player;
        skywars2$click = click;
        try {
            broadcast.run();
        } finally {
            skywars2$clickPlayer = previousPlayer;
            skywars2$click = previousClick;
        }
    }

    @WrapOperation(method = "synchronizeSlotToRemote", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/RemoteSlot;matches(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean skywars2$confirmSlot(RemoteSlot expected, ItemStack current, Operation<Boolean> original,
            @Local(argsOnly = true) int index) {
        boolean matches = original.call(expected, current);
        if (matches && skywars2$click != null && skywars2$click.changedSlots().containsKey(index)) {
            var menu = (AbstractContainerMenu)(Object)this;
            // A confirmation of this same revision prevents chained clicks from requesting full resyncs.
            skywars2$clickPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                    menu.containerId, menu.getStateId(), index, current.copy()));
        }
        return matches;
    }

    @WrapOperation(method = "synchronizeCarriedToRemote", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/RemoteSlot;matches(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean skywars2$confirmCursor(RemoteSlot expected, ItemStack current, Operation<Boolean> original) {
        boolean matches = original.call(expected, current);
        if (matches && skywars2$click != null && (skywars2$click.containerInput() != ContainerInput.QUICK_CRAFT
                || AbstractContainerMenu.getQuickcraftHeader(skywars2$click.buttonNum()) == 2)) {
            skywars2$clickPlayer.connection.send(new ClientboundSetCursorItemPacket(current.copy()));
        }
        return matches;
    }

    @ModifyArg(method="synchronizeSlotToRemote",index=2,at=@At(value="INVOKE",
            target="Lnet/minecraft/world/inventory/ContainerSynchronizer;sendSlotChange(Lnet/minecraft/world/inventory/AbstractContainerMenu;ILnet/minecraft/world/item/ItemStack;)V"))
    private ItemStack skywars2$currentSlotImage(ItemStack outgoing,@Local(argsOnly=true) ItemStack current){
        // broadcastChanges memoizes the listener image BEFORE inventory callbacks run.
        // Those callbacks can edit current in place. Vanilla then caches current in
        // RemoteSlot, but sends the old memoized image and never notices the mismatch.
        return ItemStack.matches(outgoing,current)?outgoing:current.copy();
    }
}
