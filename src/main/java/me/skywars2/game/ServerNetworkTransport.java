package me.skywars2.game;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.flush.FlushConsolidationHandler;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;

/** Coalesce socket flushes within a Netty event-loop turn, without buffering game packets. */
public final class ServerNetworkTransport {
    public static final String HANDLER = "skywars2_flush";
    private ServerNetworkTransport() { }

    public static void configure(ChannelPipeline pipeline, PacketFlow receiving, boolean local, ConnectionProtocol protocol) {
        if (!local && receiving == PacketFlow.SERVERBOUND && protocol == ConnectionProtocol.PLAY) {
            // Netty drains on read completion, backpressure, close and exception as well.
            if (pipeline.get(HANDLER) == null) pipeline.addFirst(HANDLER, new FlushConsolidationHandler(256, true));
        } else remove(pipeline);
    }

    public static void remove(ChannelPipeline pipeline) {
        // Drain encoded PLAY traffic before a protocol/compression boundary.
        if (pipeline.get(HANDLER) != null) pipeline.remove(HANDLER);
    }
}
