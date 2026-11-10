package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Custom dialog actions retain the authenticated player connection and native packet-thread boundary. */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class BuildDialogMixin {
    @Inject(method = "handleCustomClickAction", at = @At("HEAD"), cancellable = true)
    private void skywars2$build(ServerboundCustomClickActionPacket packet, CallbackInfo ci) {
        if (!packet.id().getNamespace().equals("skywars2") || !packet.id().getPath().startsWith("build/")) return;
        var game = SkyWars2.game();
        if (game != null && (Object) this instanceof ServerGamePacketListenerImpl connection) {
            PacketUtils.ensureRunningOnSameThread(packet, connection, game.server().packetProcessor());
            game.menus().builds().click(connection.player, packet.id(), packet.payload());
        }
        ci.cancel();
    }
}
