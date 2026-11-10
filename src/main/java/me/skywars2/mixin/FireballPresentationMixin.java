package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Keep custom native fireball visuals on the server's per-tick position path, including late pairing. */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class FireballPresentationMixin {
    @ModifyVariable(method="send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V",at=@At("HEAD"),argsOnly=true)
    private Packet<?> skywars2$fireball(Packet<?> packet){
        var game=SkyWars2.game();
        return game!=null&&(Object)this instanceof ServerGamePacketListenerImpl connection
                ?game.items().specialFireballs().clientPacket(connection.player,packet):packet;
    }
}
