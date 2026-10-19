package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Plans an artifact refresh when native menu input or carried stacks actually change. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class LbcCursorCraftingMixin {
    @Shadow public ServerPlayer player;

    @Unique private ItemStack skywars2$carriedBefore;
    @Unique private Map<Container, List<ItemStack>> skywars2$craftingBefore;

    @Inject(method = "handleContainerClick(Lnet/minecraft/network/protocol/game/ServerboundContainerClickPacket;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V", shift = At.Shift.AFTER))
    private void skywars2$beforeClick(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        skywars2$snapshotInputs();
    }

    @Inject(method = "handleContainerClick(Lnet/minecraft/network/protocol/game/ServerboundContainerClickPacket;)V", at = @At("TAIL"))
    private void skywars2$afterClick(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        skywars2$refreshIfChanged();
    }

    @Inject(method = "handlePlaceRecipe(Lnet/minecraft/network/protocol/game/ServerboundPlaceRecipePacket;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V", shift = At.Shift.AFTER))
    private void skywars2$beforeRecipePlacement(ServerboundPlaceRecipePacket packet, CallbackInfo ci) {
        skywars2$snapshotInputs();
    }

    @Inject(method = "handlePlaceRecipe(Lnet/minecraft/network/protocol/game/ServerboundPlaceRecipePacket;)V", at = @At("TAIL"))
    private void skywars2$afterRecipePlacement(ServerboundPlaceRecipePacket packet, CallbackInfo ci) {
        skywars2$refreshIfChanged();
    }

    @Unique
    private void skywars2$snapshotInputs() {
        AbstractContainerMenu menu = this.player.containerMenu;
        skywars2$carriedBefore = menu.getCarried().copy();
        skywars2$craftingBefore = new IdentityHashMap<>();
        for (Slot slot : menu.slots) {
            if (slot.container instanceof CraftingContainer crafting) {
                skywars2$craftingBefore.computeIfAbsent(crafting, container -> {
                    int size = container.getContainerSize();
                    java.util.ArrayList<ItemStack> items = new java.util.ArrayList<>(size);
                    for (int i = 0; i < size; i++) items.add(container.getItem(i).copy());
                    return List.copyOf(items);
                });
            }
        }
    }

    @Unique
    private void skywars2$refreshIfChanged() {
        ItemStack beforeCarried = skywars2$carriedBefore;
        Map<Container, List<ItemStack>> beforeCrafting = skywars2$craftingBefore;
        skywars2$carriedBefore = null;
        skywars2$craftingBefore = null;
        if (beforeCarried == null || beforeCrafting == null) return;

        boolean changed = !ItemStack.matches(beforeCarried, this.player.containerMenu.getCarried());
        if (!changed) {
            for (var entry : beforeCrafting.entrySet()) {
                Container container = entry.getKey();
                List<ItemStack> before = entry.getValue();
                if (container.getContainerSize() != before.size()) { changed = true; break; }
                for (int i = 0; i < before.size(); i++) {
                    if (!ItemStack.matches(before.get(i), container.getItem(i))) { changed = true; break; }
                }
                if (changed) break;
            }
        }
        if (changed) {
            var game = SkyWars2.game();
            if (game != null) game.items().magic().inventoryChanged(this.player);
        }
    }
}
