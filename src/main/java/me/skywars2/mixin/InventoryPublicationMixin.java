package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.skywars2.game.ContainerClickSync;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.RemoteSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Keep the outgoing slot image consistent with vanilla's remembered remote image. */
@Mixin(AbstractContainerMenu.class)
public abstract class InventoryPublicationMixin implements ContainerClickSync {
    @Unique private ServerPlayer skywars2$clickPlayer;
    @Unique private ServerboundContainerClickPacket skywars2$click;

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
