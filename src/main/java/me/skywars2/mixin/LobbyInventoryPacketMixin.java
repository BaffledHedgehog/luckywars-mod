package me.skywars2.mixin;

import eu.pb4.sgui.api.SguiUtils;
import me.skywars2.SkyWars2;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Блокирует изменения инвентаря после переноса обработки пакета на серверный поток. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class LobbyInventoryPacketMixin {
    @Shadow public ServerPlayer player;
    @Unique private static final String skywars2$THREAD_CHECK =
            "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V";

    @Inject(method = "handleContainerClick", at = @At(value = "INVOKE", target = skywars2$THREAD_CHECK, shift = At.Shift.AFTER), cancellable = true)
    private void skywars2$lockContainer(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        // SGui handles its own buttons and already locks the attached player inventory.
        if (SguiUtils.getCurrentGui(player) == null) skywars2$rejectMutation(ci);
    }

    @Inject(method = "handlePlayerAction", at = @At(value = "INVOKE", target = skywars2$THREAD_CHECK, shift = At.Shift.AFTER), cancellable = true)
    private void skywars2$lockDropAndSwap(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        var game = SkyWars2.game();
        if (game != null && game.items().frost().isFrozen(player)) {
            switch (packet.getAction()) {
                case RELEASE_USE_ITEM -> {
                    player.stopUsingItem();
                    ci.cancel();
                    return;
                }
                case START_DESTROY_BLOCK, CHANGE_DESTROY_DIRECTION, ABORT_DESTROY_BLOCK, STOP_DESTROY_BLOCK -> {
                    player.connection.ackBlockChangesUpTo(packet.getSequence());
                    if (player.isWithinBlockInteractionRange(packet.getPos(), 1.0) && player.level().hasChunkAt(packet.getPos()))
                        player.connection.send(new ClientboundBlockUpdatePacket(player.level(), packet.getPos()));
                    ci.cancel();
                    return;
                }
                case STAB -> {
                    ci.cancel();
                    return;
                }
                default -> { }
            }
        }
        switch (packet.getAction()) {
            case DROP_ITEM, DROP_ALL_ITEMS, SWAP_ITEM_WITH_OFFHAND -> skywars2$rejectMutation(ci);
            default -> { }
        }
    }

    @Inject(method = "handleSetCreativeModeSlot", at = @At(value = "INVOKE", target = skywars2$THREAD_CHECK, shift = At.Shift.AFTER), cancellable = true)
    private void skywars2$lockCreative(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
        skywars2$rejectMutation(ci);
    }

    @Inject(method = "handlePlaceRecipe", at = @At(value = "INVOKE", target = skywars2$THREAD_CHECK, shift = At.Shift.AFTER), cancellable = true)
    private void skywars2$lockCrafting(ServerboundPlaceRecipePacket packet, CallbackInfo ci) {
        skywars2$rejectMutation(ci);
    }

    @Unique
    private void skywars2$rejectMutation(CallbackInfo ci) {
        var game = SkyWars2.game();
        if (game != null && game.inventoryLocked(player)) {
            game.enforceLobbyInventory(player);
            player.containerMenu.sendAllDataToRemote();
            if (player.containerMenu != player.inventoryMenu) player.inventoryMenu.sendAllDataToRemote();
            ci.cancel();
        }
    }
}
