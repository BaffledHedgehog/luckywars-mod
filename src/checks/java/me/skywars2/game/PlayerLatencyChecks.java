package me.skywars2.game;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import me.skywars2.SkyWars2;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
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
        BulkPlayerProfileChecks bulkProfiles;
        Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);start=game.server().getTickCount();}
        int latest(RegenerationBeltChecks.ScheduledClient client){return client.packets().stream().filter(ClientboundPingPacket.class::isInstance).map(ClientboundPingPacket.class::cast).filter(p->p.getId()<0).reduce((a,b)->b).orElseThrow().getId();}
        void setup(){
            f.prepareParticipants();commands();chat=f.owner.getDisplayName().getString();
            require(PlayerLatency.millis(f.owner.connection)<0&&f.owner.getTabListDisplayName().getString().equals(chat),"TAB names keep original cosmetics; new connection has no fabricated0ms");
            require(game.server().getScoreboard().getDisplayObjective(net.minecraft.world.scores.DisplaySlot.LIST).getName().equals(PlayerLatency.TAB_OBJECTIVE),"native numeric column immediately before the ping icon");
            require(PlayerLatency.format(-1).format(-1).getString().equals("\u2014"),"unknown ping is a dash, not zero");
            PlayerLatency.tick(game.server());
            require(f.audience.packets().stream().filter(ClientboundTabListPacket.class::isInstance).map(ClientboundTabListPacket.class::cast).anyMatch(p->p.header().getString().matches("TPS: [0-9]+\\.[0-9]")),"actual native TAB header publishes the measured TPS");
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
        boolean step() throws Exception {
            if (bulkProfiles != null) return bulkProfiles.step();
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
                        .filter(p->p.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY))
                        .flatMap(p->p.entries().stream()).anyMatch(e->e.profileId().equals(f.owner.getUUID())&&e.latency()==measured);
                String expectedDigits=PlayerLatency.tabHealth(f.owner)<0?Integer.toString(measured):Integer.toString(measured).chars().mapToObj(c->Character.toString(0xE100+c-'0')).collect(java.util.stream.Collectors.joining());
                boolean column=f.audience.packets().stream().filter(ClientboundSetScorePacket.class::isInstance).map(ClientboundSetScorePacket.class::cast).anyMatch(p->p.owner().equals(f.owner.getScoreboardName())&&p.objectiveName().equals(PlayerLatency.TAB_OBJECTIVE)&&p.score()==measured&&p.numberFormat().orElseThrow().format(measured).getString().replaceAll("[\\uE200-\\uE220]","").endsWith(expectedDigits));
                require(published&&column&&f.owner.getDisplayName().getString().equals(chat),"real PlayerInfo icon latency and right-aligned score both use measured RTT; chat name unchanged");
                var board=game.server().getScoreboard();var objective=PlayerLatency.tabObjective(game.server());var before=board.getPlayerScoreInfo(f.owner,objective).value();
                f.audience.packets().clear();PlayerLatency.tick(game.server());require(f.audience.packets().stream().noneMatch(p->p instanceof ClientboundSetScorePacket||p instanceof ClientboundPlayerInfoUpdatePacket),"unchanged latency and health produce no extra TAB traffic");
                if(PlayerLatency.tabHealth(f.owner)>=0){float hp=f.owner.getHealth();f.owner.setHealth(19);PlayerLatency.tick(game.server());var current=board.getPlayerScoreInfo(f.owner,objective);var text=current.formatValue(net.minecraft.network.chat.numbers.StyledFormat.NO_STYLE).getString();require(text.chars().filter(c->c==0xE000).count()==10&&text.chars().filter(c->c==0xE001).count()==9&&text.chars().filter(c->c==0xE002).count()==1,"actual score format renders ten containers, nine full hearts and one half");require(f.owner.getTabListDisplayName().getString().equals(chat)&&current.value()==before&&f.audience.packets().stream().noneMatch(p->p instanceof ClientboundPlayerInfoUpdatePacket update&&update.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME)),"HP and ping share one score component; health never changes player names");
                    for(float value:new float[]{21,22,43,44,44.1f,45,46}){f.owner.setHealth(value);PlayerLatency.tick(game.server());String view=board.getPlayerScoreInfo(f.owner,objective).formatValue(net.minecraft.network.chat.numbers.StyledFormat.NO_STYLE).getString();if(value==21)require(view.chars().filter(c->c==0xE004).count()==1,"separate native gold half-heart sprite");if(value==22)require(view.chars().filter(c->c==0xE003).count()==1,"separate native gold full-heart sprite");if(value==43)require(view.indexOf(0xE000)>=0,"last native graphical HP range");if(value>=44){String digits=view.replaceAll("[\\uE200-\\uE220]","");String expected=Integer.toString((int)Math.ceil(value/2)).chars().mapToObj(c->Character.toString(0xE100+c-'0')).collect(java.util.stream.Collectors.joining());require(view.indexOf(0xE000)<0&&digits.startsWith(expected)&&view.indexOf(0xE10A)<0,"crowded hearts use ceil integer HP without decimal glyph at "+value);}}f.owner.setHealth(hp);}
                PlayerLatency.request(f.client.listener);obsolete=latest(f.client);f.client.close();replacement=new RegenerationBeltChecks.ScheduledClient(game,f.owner);
                require(PlayerLatency.millis(f.owner.connection)<0,"reconnection never inherits a stale sample");
                PlayerLatency.tick(game.server());id=latest(replacement);f.client.listener.handlePong(new ServerboundPongPacket(obsolete));
                require(PlayerLatency.millis(replacement.listener)<0&&PlayerLatency.millis(f.client.listener)<0,"late old-connection Pong remains ignored after pruning");at=game.server().getTickCount();stage=3;return false;
            }
            if(elapsed<2)return false;
            replacement.listener.handlePong(new ServerboundPongPacket(id));require(PlayerLatency.millis(replacement.listener)>0&&replacement.listener.latency()==PlayerLatency.millis(replacement.listener),"new connection measures its own real round trip");
            if (Boolean.getBoolean("skywars2.check.bulk.profileLookup")) {
                bulkProfiles = new BulkPlayerProfileChecks(game.server(), f.owner.createCommandSourceStack());
                return false;
            }
            return true;
        }
        @Override public void close() throws Exception {if(closed)return;closed=true;try{if(bulkProfiles!=null)bulkProfiles.close();if(reply!=null)reply.join();}finally{try{PlayerLatency.forget(f.owner);if(replacement!=null)replacement.close();}finally{f.close();}}}
    }
    static void run(GameController game){require(running==null,"exclusive native scene");running=new Scene(game);try{running.setup();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS player-latency (real delayed RTT/native right-aligned TAB scores, sprite-font HP/half-heart overlays, actual TPS header, no unchanged-state traffic, reconnection/late responses and command permissions)");}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL player-latency",error);}}
}
