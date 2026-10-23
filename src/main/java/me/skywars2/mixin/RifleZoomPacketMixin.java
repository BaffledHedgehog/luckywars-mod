package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Keep projection zoom across native ability resends without changing authoritative abilities. */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class RifleZoomPacketMixin {
    @ModifyVariable(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V",
            at = @At("HEAD"), argsOnly = true)
    private Packet<?> skywars2$rifleProjection(Packet<?> packet) {
        if (!(packet instanceof ClientboundPlayerAbilitiesPacket abilities)
                || !((Object) this instanceof ServerGamePacketListenerImpl connection)) return packet;
        var game = SkyWars2.game();
        return game == null ? packet : game.items().rifleZoom().projectAbilities(connection.player.getUUID(), abilities);
    }
}
