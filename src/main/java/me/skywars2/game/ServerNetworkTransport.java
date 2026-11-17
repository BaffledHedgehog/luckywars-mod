package me.skywars2.game;

import io.netty.channel.ChannelPipeline;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import io.netty.handler.flush.FlushConsolidationHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;

/** Coalesce socket flushes within a Netty event-loop turn, without buffering game packets. */
public final class ServerNetworkTransport {
    public static final String HANDLER = "skywars2_flush";
    private static final String MEDIA_TICKS="skywars2_media_ticks";
    private static final AttributeKey<Long> CLIENT_TICKS=AttributeKey.valueOf("skywars2:media_client_ticks");
    private static final ThreadLocal<Set<ServerCommonPacketListenerImpl>> GAME_FRAMES=new ThreadLocal<>();
    static long mediaClientTicks(Channel channel){var value=channel.attr(CLIENT_TICKS).get();return value==null?-1:value;}
    private ServerNetworkTransport() { }

    /** Vanilla resumes socket flushing before Fabric END_SERVER_TICK callbacks.
     * Keep the mod's whole end-tick frame in the same native write batch instead
     * of flushing after each effect. Worker-thread audio remains immediate.
     */
    public static void endTick(MinecraftServer server,Runnable work){
        if(!server.isSameThread()){work.run();return;}
        try(var frame=new GameFrame(server)){work.run();}
    }

    private static final class GameFrame implements AutoCloseable {
        private final Set<ServerCommonPacketListenerImpl> owned;
        private final ArrayList<ServerCommonPacketListenerImpl> acquired=new ArrayList<>();
        GameFrame(MinecraftServer server){
            var active=GAME_FRAMES.get();
            if(active==null){active=Collections.newSetFromMap(new IdentityHashMap<>());GAME_FRAMES.set(active);}
            owned=active;
            for(var player:server.getPlayerList().getPlayers()){
                var listener=player.connection;
                if(owned.add(listener)){listener.suspendFlushing();acquired.add(listener);}
            }
        }
        @Override public void close(){
            Throwable failure=null;
            for(var listener:acquired){
                try{listener.resumeFlushing();}
                catch(RuntimeException|Error error){if(failure==null)failure=error;else failure.addSuppressed(error);}
                finally{owned.remove(listener);}
            }
            if(owned.isEmpty())GAME_FRAMES.remove();
            if(failure instanceof Error error)throw error;
            if(failure!=null)throw (RuntimeException)failure;
        }
    }

    public static void configure(ChannelPipeline pipeline, PacketFlow receiving, boolean local, ConnectionProtocol protocol) {
        if (!local && receiving == PacketFlow.SERVERBOUND && protocol == ConnectionProtocol.PLAY) {
            // Netty drains on read completion, backpressure, close and exception as well.
            if (pipeline.get(HANDLER) == null) pipeline.addFirst(HANDLER, new FlushConsolidationHandler(256, true));
            if(pipeline.get(MEDIA_TICKS)==null&&pipeline.get("packet_handler")!=null)pipeline.addBefore("packet_handler",MEDIA_TICKS,new ChannelInboundHandlerAdapter(){
                @Override public void channelRead(ChannelHandlerContext ctx,Object message)throws Exception{
                    if(message instanceof net.minecraft.network.protocol.game.ServerboundClientTickEndPacket){
                        var count=ctx.channel().attr(CLIENT_TICKS);var previous=count.get();count.set(previous==null?0:previous+1);
                    }
                    super.channelRead(ctx,message);
                }
            });
        } else remove(pipeline);
    }

    public static void remove(ChannelPipeline pipeline) {
        // Drain encoded PLAY traffic before a protocol/compression boundary.
        if (pipeline.get(HANDLER) != null) pipeline.remove(HANDLER);
        if (pipeline.get(MEDIA_TICKS) != null) pipeline.remove(MEDIA_TICKS);
        pipeline.channel().attr(CLIENT_TICKS).set(null);
    }
}
