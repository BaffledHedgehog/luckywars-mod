package me.skywars2.mixin;

import io.netty.channel.Channel;
import me.skywars2.game.ServerNetworkTransport;
import net.minecraft.network.Connection;
import net.minecraft.network.ProtocolInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
abstract class NetworkTransportMixin {
    @Shadow private Channel channel;

    @Inject(method = "setupOutboundProtocol", at = @At("HEAD"))
    private void skywars2$drainTransport(ProtocolInfo<?> protocol, CallbackInfo ci) {
        ServerNetworkTransport.remove(channel.pipeline());
    }

    @Inject(method = "setupOutboundProtocol", at = @At("RETURN"))
    private void skywars2$configureTransport(ProtocolInfo<?> protocol, CallbackInfo ci) {
        var connection=(Connection)(Object)this;
        ServerNetworkTransport.configure(channel.pipeline(), connection.getReceiving(), connection.isMemoryConnection(), protocol.id());
    }
}
