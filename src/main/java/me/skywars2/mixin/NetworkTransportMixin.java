package me.skywars2.mixin;

import io.netty.channel.ChannelPipeline;
import me.skywars2.game.ServerNetworkTransport;
import net.minecraft.network.BandwidthDebugMonitor;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
abstract class NetworkTransportMixin {
    @Inject(method = "configureSerialization", at = @At("RETURN"))
    private static void skywars2$configureTransport(ChannelPipeline pipeline, PacketFlow receiving,
            boolean local, BandwidthDebugMonitor monitor, CallbackInfo ci) {
        ServerNetworkTransport.configure(pipeline, receiving, local);
    }
}
