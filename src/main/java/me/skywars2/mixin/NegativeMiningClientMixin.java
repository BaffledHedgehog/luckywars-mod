package me.skywars2.mixin;

import me.skywars2.game.NegativeMiningClient;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import java.util.List;

public final class NegativeMiningClientMixin {
    private NegativeMiningClientMixin() { }
    @Mixin(ClientboundUpdateAttributesPacket.class)
    public interface Attributes {
        @Invoker("<init>")
        static ClientboundUpdateAttributesPacket skywars2$create(int id,List<ClientboundUpdateAttributesPacket.AttributeSnapshot> values) {
            throw new AssertionError();
        }
    }
    // Run after scope's projection: it can append fresh authoritative abilities to movement updates.
    @Mixin(value=ServerCommonPacketListenerImpl.class,priority=900)
    public abstract static class Connection {
        @ModifyVariable(method="send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V",
                at=@At("HEAD"),argsOnly=true)
        private Packet<?> skywars2$miningProjection(Packet<?> packet) {
            return (Object)this instanceof ServerGamePacketListenerImpl connection
                    ?NegativeMiningClient.project(connection.player,packet):packet;
        }
    }
}
