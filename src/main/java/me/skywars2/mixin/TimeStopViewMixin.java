package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.netty.channel.ChannelFutureListener;
import me.skywars2.game.TimeStopView;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.*;
import org.spongepowered.asm.mixin.Mixin;

public final class TimeStopViewMixin {
    private TimeStopViewMixin(){}
    @Mixin(ServerCommonPacketListenerImpl.class)
    public abstract static class Listener {
        @WrapMethod(method="send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V")
        private void skywars2$stoppedSound(Packet<?> packet,ChannelFutureListener listener,Operation<Void> original){
            if((Object)this instanceof ServerGamePacketListenerImpl game){var result=TimeStopView.project(game.player,packet);if(result==null){TimeStopView.completed(((MediaTransportAccess.Listener)this).skywars2$mediaConnection(),listener);return;}packet=result;}
            original.call(packet,listener);
        }
    }
    @Mixin(Connection.class)
    public abstract static class Transport {
        @WrapMethod(method="send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V")
        private void skywars2$directSound(Packet<?> packet,ChannelFutureListener listener,boolean flush,Operation<Void> original){
            var connection=(Connection)(Object)this;
            if(connection.getPacketListener() instanceof ServerGamePacketListenerImpl game){var result=TimeStopView.project(game.player,packet);if(result==null){TimeStopView.completed(connection,listener);return;}packet=result;}
            original.call(packet,listener,flush);
        }
    }
}
