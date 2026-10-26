package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ObserverPacketMixin {
    @Shadow public ServerPlayer player;
    @org.spongepowered.asm.mixin.Unique private boolean skywars2$slrEntityAttack;

    @Inject(method = "handleUseItem", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;ackBlockChangesUpTo(I)V",
            shift = At.Shift.AFTER), cancellable = true)
    private void skywars2$blockFrozenItemUse(ServerboundUseItemPacket packet, CallbackInfo ci) {
        var game = SkyWars2.game();
        if (game != null && game.items().actionsBlocked(player)) {
            skywars2$syncInventory();
            ci.cancel();
        }
    }

    @Inject(method = "handleUseItemOn", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;ackBlockChangesUpTo(I)V",
            shift = At.Shift.AFTER), cancellable = true)
    private void skywars2$blockFrozenBlockUse(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        var game = SkyWars2.game();
        if (game != null && game.items().actionsBlocked(player)) {
            var hit = packet.hitResult();
            var pos = hit.getBlockPos();
            // Отказ не должен загружать чанки по координатам произвольного клиентского пакета.
            if (player.isWithinBlockInteractionRange(pos, 1.0)) {
                if (player.level().hasChunkAt(pos)) player.connection.send(new ClientboundBlockUpdatePacket(player.level(), pos));
                var adjacent = pos.relative(hit.getDirection());
                if (player.level().hasChunkAt(adjacent)) player.connection.send(new ClientboundBlockUpdatePacket(player.level(), adjacent));
            }
            skywars2$syncInventory();
            ci.cancel();
        }
    }

    @Inject(method = {"handleAttack", "handlePunch"}, at = @At(value = "INVOKE", target =
            "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift = At.Shift.AFTER), cancellable = true)
    private void skywars2$blockDisabledAttack(CallbackInfo ci) {
        var game = SkyWars2.game();
        if (game != null && game.items().actionsBlocked(player)) ci.cancel();
    }

    @org.spongepowered.asm.mixin.Unique
    private void skywars2$syncInventory() {
        player.containerMenu.sendAllDataToRemote();
        if (player.containerMenu != player.inventoryMenu) player.inventoryMenu.sendAllDataToRemote();
    }

    /** Vanilla sends Attack then Punch for one melee click; the latter is only its swing. */
    @Inject(method = "handleAttack", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift = At.Shift.AFTER))
    private void skywars2$rememberSlrAttack(CallbackInfo ci) {
        var main=player.getMainHandItem();
        skywars2$slrEntityAttack=me.skywars2.game.LbcShooters.isSlr(main)
                || !me.skywars2.game.LbcMagnum.isMagnum(main)
                && me.skywars2.game.LbcShooters.isSlr(player.getOffhandItem());
    }

    @Inject(method = "handleClientTickEnd", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift = At.Shift.AFTER))
    private void skywars2$expireSlrAttack(CallbackInfo ci) {
        skywars2$slrEntityAttack=false;
    }

    /** Air/mining clicks toggle scope; SLG entity attacks retain ordinary melee semantics. */
    @Inject(method = "handlePunch", at = @At("TAIL"))
    private void skywars2$toggleRifleZoom(CallbackInfo ci) {
        boolean melee=skywars2$slrEntityAttack;
        skywars2$slrEntityAttack=false;
        if(melee)return;
        var game = SkyWars2.game();
        if (game != null) game.items().rifleZoom().punch(player);
    }

    /** Обрабатываем ПКМ раньше vanilla spectator-проверки, запрещая обычное взаимодействие с целью. */
    @Inject(method = "handleInteract", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift = At.Shift.AFTER), cancellable = true)
    private void skywars2$watch(ServerboundInteractPacket packet, CallbackInfo ci) {
        var game = SkyWars2.game();
        if (game != null && game.items().actionsBlocked(player)) { ci.cancel(); return; }
        if (game == null || !game.isObserver(player)) return;
        var target = player.level().getEntityOrPart(packet.entityId());
        if (target != null && player.isWithinEntityInteractionRange(target.getBoundingBox(), 3.0)
                && player.level().getWorldBorder().isWithinBounds(target.blockPosition()))
            game.spectators().watch(player, target);
        ci.cancel();
    }
}
