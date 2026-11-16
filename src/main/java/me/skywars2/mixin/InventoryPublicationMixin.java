package me.skywars2.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Keep the outgoing slot image consistent with vanilla's remembered remote image. */
@Mixin(AbstractContainerMenu.class)
public abstract class InventoryPublicationMixin {
    @ModifyArg(method="synchronizeSlotToRemote",index=2,at=@At(value="INVOKE",
            target="Lnet/minecraft/world/inventory/ContainerSynchronizer;sendSlotChange(Lnet/minecraft/world/inventory/AbstractContainerMenu;ILnet/minecraft/world/item/ItemStack;)V"))
    private ItemStack skywars2$currentSlotImage(ItemStack outgoing,@Local(argsOnly=true) ItemStack current){
        // broadcastChanges memoizes the listener image BEFORE inventory callbacks run.
        // Those callbacks can edit current in place. Vanilla then caches current in
        // RemoteSlot, but sends the old memoized image and never notices the mismatch.
        return ItemStack.matches(outgoing,current)?outgoing:current.copy();
    }
}
