package me.skywars2.mixin;

import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read the existing transport's backpressure; media does not create a second packet queue. */
public final class MediaTransportAccess {
    private MediaTransportAccess(){}
    @Mixin(ServerCommonPacketListenerImpl.class)
    public interface Listener {
        @Accessor("connection") Connection skywars2$mediaConnection();
    }
    @Mixin(Connection.class)
    public interface Transport {
        @Accessor("channel") Channel skywars2$mediaChannel();
    }
}
