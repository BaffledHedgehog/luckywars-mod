package me.skywars2.game;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/** Small vanilla Ping/Pong probes. Arrival is measured on the network thread, publication on the server thread. */
public final class PlayerLatency {
    private static final long INTERVAL=5_000_000_000L,STALE=15_000_000_000L;
    private static final AtomicInteger NONCE=new AtomicInteger(-1);
    private static final ConcurrentHashMap<ServerGamePacketListenerImpl,Sample> SAMPLES=new ConcurrentHashMap<>();
    private static MinecraftServer owner;
    private record Probe(int id,long sent){}
    private record Reading(int millis,long received){}
    private static final class Sample {
        final AtomicReference<Probe> pending=new AtomicReference<>();
        volatile Reading reading;
        long lastSent;
        int published=Integer.MIN_VALUE;
    }
    private PlayerLatency(){}
    private static boolean realClient(ServerPlayer player){return !player.getClass().getName().equals("carpet.patches.EntityPlayerMPFake");}
    static void request(ServerGamePacketListenerImpl listener){
        if(!realClient(listener.player)||!listener.hasClientLoaded())return;
        var sample=SAMPLES.computeIfAbsent(listener,key->new Sample());long now=System.nanoTime();
        int id=NONCE.getAndUpdate(value->value==Integer.MIN_VALUE?-1:value-1);
        sample.lastSent=now;sample.pending.set(new Probe(id,now));
        listener.send(new ClientboundPingPacket(id));
    }
    /** Called before PacketUtils marshals the same Pong to the server thread; a duplicate cannot consume a new probe. */
    public static void pong(ServerGamePacketListenerImpl listener,int id){
        if(id>=0)return;
        var sample=SAMPLES.get(listener);if(sample==null)return;
        var probe=sample.pending.get();if(probe==null||probe.id()!=id||!sample.pending.compareAndSet(probe,null))return;
        long now=System.nanoTime(),elapsed=now-probe.sent();if(elapsed<0||elapsed>STALE)return;
        int millis=(int)Math.max(1,(elapsed+999_999L)/1_000_000L);
        sample.reading=new Reading(millis,now);
    }
    public static int millis(ServerGamePacketListenerImpl listener){
        if(listener==null)return -1;
        var sample=SAMPLES.get(listener);var reading=sample==null?null:sample.reading;
        return reading==null||System.nanoTime()-reading.received()>STALE?-1:reading.millis();
    }
    static String label(ServerPlayer player){int value=millis(player.connection);return (value<0?"\u2014":Integer.toString(value))+" ms";}
    public static Component displayName(ServerPlayer player,Component original){
        return original.copy().append(Component.literal(" ["+label(player)+"]").withStyle(ChatFormatting.GRAY));
    }
    public static void tick(MinecraftServer server){
        if(owner!=server){SAMPLES.clear();owner=server;}
        SAMPLES.keySet().removeIf(listener->listener.player.connection!=listener||server.getPlayerList().getPlayer(listener.player.getUUID())!=listener.player);
        long now=System.nanoTime();var changed=new ArrayList<ServerPlayer>();
        for(var player:server.getPlayerList().getPlayers()){
            var listener=player.connection;if(listener==null||!realClient(player)||!listener.hasClientLoaded())continue;
            var sample=SAMPLES.computeIfAbsent(listener,key->new Sample());
            if(sample.lastSent==0||now-sample.lastSent>=INTERVAL)request(listener);
            int value=millis(listener);if(sample.published!=value){sample.published=value;changed.add(player);}
        }
        if(!changed.isEmpty())server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),changed));
    }
    public static void forget(ServerPlayer player){SAMPLES.keySet().removeIf(listener->listener.player==player);}
    public static void stop(){SAMPLES.clear();owner=null;}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        var node=SkyWarsCommandTree.add(dispatcher,Commands.literal("ping")
                .executes(context->report(context.getSource(),context.getSource().getPlayerOrException()))
                .then(Commands.argument("player",EntityArgument.player()).executes(context->report(context.getSource(),EntityArgument.getPlayer(context,"player")))));
        dispatcher.getRoot().addChild(node);
    }
    private static int report(CommandSourceStack source,ServerPlayer player){
        source.sendSuccess(()->Component.literal(player.getScoreboardName()+": "+label(player)),false);return 1;
    }
}
