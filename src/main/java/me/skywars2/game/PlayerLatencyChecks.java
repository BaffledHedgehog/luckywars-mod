package me.skywars2.game;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import me.skywars2.SkyWars2;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.server.permissions.PermissionSet;

/** Actual vanilla Ping/Pong endpoints and PlayerInfo wire updates; no invented RTT or fake keepalive value. */
final class PlayerLatencyChecks {
    private static Scene running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Player latency: "+why);}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final int start;
        int stage,at,id,measured,obsolete;long sent;
        final AtomicLong arrived=new AtomicLong();CompletableFuture<Void> reply;
        RegenerationBeltChecks.ScheduledClient replacement;String chat;boolean closed;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);start=game.server().getTickCount();}
        int latest(RegenerationBeltChecks.ScheduledClient client){return client.packets().stream().filter(ClientboundPingPacket.class::isInstance).map(ClientboundPingPacket.class::cast).filter(p->p.getId()<0).reduce((a,b)->b).orElseThrow().getId();}
        void setup(){
            f.prepareParticipants();commands();chat=f.owner.getDisplayName().getString();
            require(PlayerLatency.millis(f.owner.connection)<0&&f.owner.getTabListDisplayName().getString().equals(chat+" [\u2014 ms]"),"new connection shows unknown, never fabricated0ms; chat name is unchanged");
            f.client.packets().clear();sent=System.nanoTime();PlayerLatency.request(f.owner.connection);id=latest(f.client);at=game.server().getTickCount();
            f.owner.connection.handlePong(new ServerboundPongPacket(id-50));require(PlayerLatency.millis(f.owner.connection)<0,"unmatched Pong cannot supply a reading");
        }
        void commands(){
            var dispatcher=game.server().getCommands().getDispatcher();var root=dispatcher.getRoot();var skywars=root.getChild("skywars");
            var source=f.owner.createCommandSourceStack().withPermission(PermissionSet.NO_PERMISSIONS).withSuppressedOutput();
            try{
                for(var text:java.util.List.of("tps","skywars tps","ping","skywars ping","ping TestBeta","skywars ping TestBeta"))require(dispatcher.execute(text,source)==1,"public native command "+text);
                for(var name:java.util.List.of("tps","ping"))require(root.getChild(name).getCommand()==skywars.getChild(name).getCommand(),"same real command implementation behind both paths "+name);
                var spawn=root.getChild("player").getChild("spawn");require(spawn==skywars.getChild("player").getChild("spawn")&&!spawn.canUse(source)&&spawn.canUse(source.withPermission(PermissionSet.ALL_PERMISSIONS)),"bulk spawn aliases preserve identical native permission gate");
                require(root.getChild("player").getChildren().stream().anyMatch(n->n instanceof com.mojang.brigadier.tree.ArgumentCommandNode),"ordinary Carpet player-name control branch remains present");
                try{dispatcher.execute("player spawn invalid-name 1",source.withPermission(PermissionSet.ALL_PERMISSIONS));throw new IllegalStateException("invalid prefix accepted");}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){require(expected.getRawMessage().getString().contains("Use letters"),"root alias executes real validated bulk-spawn handler without creating a player");}
            }catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}
        }
        boolean step(){
            require(game.server().getTickCount()-start<60,"bounded real native round trip and reconnection scene");
            if(replacement==null)f.client.acknowledge();else replacement.acknowledge();f.audience.acknowledge();
            int elapsed=game.server().getTickCount()-at;
            if(stage==0){
                if(elapsed<3)return false;
                reply=CompletableFuture.runAsync(()->{arrived.set(System.nanoTime());try{f.client.listener.handlePong(new ServerboundPongPacket(id));}catch(RunningOnDifferentThreadException normal){}});stage=1;return false;
            }
            if(stage==1){
                if(!reply.isDone())return false;reply.join();measured=PlayerLatency.millis(f.owner.connection);
                double elapsedMs=(arrived.get()-sent)/1_000_000d;
                require(measured>=1&&Math.abs(measured-elapsedMs)<15,"actual network-thread arrival RTT matches delayed native Pong: measured="+measured+", actual="+elapsedMs);
                require(f.owner.connection.latency()==measured,"native latency getter uses the same measured sample");
                f.owner.connection.handlePong(new ServerboundPongPacket(id));require(PlayerLatency.millis(f.owner.connection)==measured,"duplicate Pong cannot replace the accepted sample");stage=2;return false;
            }
            if(stage==2){
                boolean published=f.audience.packets().stream().filter(ClientboundPlayerInfoUpdatePacket.class::isInstance).map(ClientboundPlayerInfoUpdatePacket.class::cast)
                        .filter(p->p.actions().containsAll(java.util.EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME)))
                        .flatMap(p->p.entries().stream()).anyMatch(e->e.profileId().equals(f.owner.getUUID())&&e.latency()==measured&&e.displayName()!=null&&e.displayName().getString().equals(chat+" ["+measured+" ms]"));
                require(published&&f.owner.getDisplayName().getString().equals(chat),"actual native PlayerInfo contains measured number and both wire actions; chat cosmetic name unchanged");
                PlayerLatency.request(f.client.listener);obsolete=latest(f.client);f.client.close();replacement=new RegenerationBeltChecks.ScheduledClient(game,f.owner);
                require(PlayerLatency.millis(f.owner.connection)<0,"reconnection never inherits a stale sample");
                PlayerLatency.tick(game.server());id=latest(replacement);f.client.listener.handlePong(new ServerboundPongPacket(obsolete));
                require(PlayerLatency.millis(replacement.listener)<0&&PlayerLatency.millis(f.client.listener)<0,"late old-connection Pong remains ignored after pruning");at=game.server().getTickCount();stage=3;return false;
            }
            if(elapsed<2)return false;
            replacement.listener.handlePong(new ServerboundPongPacket(id));require(PlayerLatency.millis(replacement.listener)>0&&replacement.listener.latency()==PlayerLatency.millis(replacement.listener),"new connection measures its own real round trip");return true;
        }
        @Override public void close(){if(closed)return;closed=true;try{if(reply!=null)reply.join();}finally{try{PlayerLatency.forget(f.owner);if(replacement!=null)replacement.close();}finally{f.close();}}}
    }
    static void run(GameController game){require(running==null,"exclusive native scene");running=new Scene(game);try{running.setup();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS player-latency (actual delayed network-thread Ping/Pong RTT, native latency and TAB wire values, unknown/reconnected/duplicate/late responses, public TPS/Ping and permission-preserving bulk spawn aliases)");}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL player-latency",error);}}
}
