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
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.numbers.FixedFormat;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/** Small vanilla Ping/Pong probes. Arrival is measured on the network thread, publication on the server thread. */
public final class PlayerLatency {
    public static final String TAB_OBJECTIVE="skywars2.ping";
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
        int publishedHealth=Integer.MIN_VALUE;
    }
    private PlayerLatency(){}
    private static boolean realClient(ServerPlayer player){return !player.getClass().getName().equals("carpet.patches.EntityPlayerMPFake");}
    public static int nextProbeId() {
        return NONCE.getAndUpdate(value -> value == Integer.MIN_VALUE ? -1 : value - 1);
    }
    public static void request(ServerGamePacketListenerImpl listener){
        if(!realClient(listener.player)||!listener.hasClientLoaded())return;
        var sample=SAMPLES.computeIfAbsent(listener,key->new Sample());long now=System.nanoTime();
        int id=nextProbeId();
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
    public static int tabHealth(ServerPlayer player){var game=me.skywars2.SkyWars2.game();return game!=null&&(game.config().healthDisplay&2)!=0?(int)Math.ceil(player.getHealth()+player.getAbsorptionAmount()):-1;}
    public static Objective tabObjective(MinecraftServer server){var board=server.getScoreboard();var objective=board.getObjective(TAB_OBJECTIVE);return objective!=null?objective:board.addObjective(TAB_OBJECTIVE,ObjectiveCriteria.DUMMY,Component.literal("Ping"),ObjectiveCriteria.RenderType.INTEGER,false,format(1));}
    public static NumberFormat format(int value){return value<0?new FixedFormat(Component.literal("\u2014").withStyle(ChatFormatting.GRAY)):new StyledFormat(Style.EMPTY.withColor(value<150?ChatFormatting.GREEN:value<300?ChatFormatting.YELLOW:ChatFormatting.RED));}
    public static void tick(MinecraftServer server){
        if(owner!=server){SAMPLES.clear();owner=server;}
        SAMPLES.keySet().removeIf(listener->listener.player.connection!=listener||server.getPlayerList().getPlayer(listener.player.getUUID())!=listener.player);
        long now=System.nanoTime();var changed=new ArrayList<ServerPlayer>();var objective=tabObjective(server);var board=server.getScoreboard();
        for(var player:server.getPlayerList().getPlayers()){
            var listener=player.connection;if(listener==null||!listener.hasClientLoaded())continue;
            var sample=SAMPLES.computeIfAbsent(listener,key->new Sample());
            if(realClient(player)&&(sample.lastSent==0||now-sample.lastSent>=INTERVAL))request(listener);
            int value=millis(listener),health=tabHealth(player);var previous=board.getPlayerScoreInfo(player,objective);
            if(sample.published!=value||sample.publishedHealth!=health||previous==null||previous.value()!=value){
                var score=board.getOrCreatePlayerScore(player,objective);if(previous==null||previous.value()!=value)score.set(value);
                score.numberFormatOverride(health<0?format(value):new FixedFormat(TabPresentation.score(health,value)));sample.publishedHealth=health;
            }
            if(sample.published!=value){sample.published=value;changed.add(player);}
        }
        if(!changed.isEmpty())server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY),changed));
        TabPresentation.tick(server);
    }
    public static void forget(ServerPlayer player){SAMPLES.keySet().removeIf(listener->listener.player==player);TabPresentation.forget(player);var board=player.level().getServer().getScoreboard();var objective=board.getObjective(TAB_OBJECTIVE);if(objective!=null)board.resetSinglePlayerScore(player,objective);}
    public static void stop(){SAMPLES.clear();owner=null;TabPresentation.stop();}
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
