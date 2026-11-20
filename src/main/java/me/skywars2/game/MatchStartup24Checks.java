package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;

/** Completed native startup ticks on source map64 with23 actual Carpet bodies and one vanilla player. */
final class MatchStartup24Checks {
    private static Fixture running;
    private MatchStartup24Checks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Match startup24: "+why);}
    private static final class Fixture {
        final GameController game;final int countdown;final List<UUID> bots=new ArrayList<>();
        final Map<String,Double> phases=new LinkedHashMap<>();final List<Long> before=new ArrayList<>(),after=new ArrayList<>();
        final Map<String,Double> resetSteps = new LinkedHashMap<>();
        RegenerationBeltChecks.ScheduledClient entryClient;
        List<Vec3> entryPositions;
        boolean entryInCage, entryAbilities;
        final int armed;
        int handoff = -1, release = -1, cages;
        long preparationNanos, handoffNanos, releaseNanos, phaseTime, resetTime;
        boolean ready, requested, passed, closed;
        Fixture(GameController game){this.game=game;countdown=game.config().countdownSeconds;armed=game.server().getTickCount();}
        void prepare() {
            require(game.inLobby()&&!game.maps().busy()&&game.maps().spawns().size()==24,"source map64 ready with24 spawns");
            require(game.server().getPlayerList().getPlayerCount()==2,"only reusable native fixture identities online");
            var beta=game.server().getPlayerList().getPlayerByName("TestBeta");require(beta!=null,"Beta initially online");
            beta.connection.onDisconnect(new DisconnectionDetails(Component.literal("Owned startup preparation")));
            require(game.server().getPlayerList().getPlayerByName("TestBeta")==null,"Beta offline before exactly24 start");
            game.config().countdownSeconds=5;
            for(int i=0;i<23;i++){
                String name=String.format(Locale.ROOT,"botBench%02d",i);var id=GameChecks.offlineUuid(name);
                require(game.server().getPlayerList().getPlayer(id)==null,"owned bot initially offline");bots.add(id);
                var alpha=game.server().getPlayerList().getPlayerByName("TestAlpha");
                var bot=CarpetBotChecks.connect(game,name,game.maps().level(),alpha.clientInformation());
                bot.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
            }
        }
        void phase(String name){long now=System.nanoTime();if(phaseTime!=0)phases.put(name,(now-phaseTime)/1e6);phaseTime=now;if(name.equals("enter"))resetTime=now;}
        void resetStep(String name) {
            long now = System.nanoTime();
            resetSteps.put(name, (now - resetTime) / 1e6);
            resetTime = now;
        }
        void tick(){
            int now=game.server().getTickCount();require(now-armed<800,"bounded native startup window");
            if(passed){if(game.inLobby())close();return;}
            if(!ready){
                if(bots.stream().anyMatch(id->game.server().getPlayerList().getPlayer(id)==null))return;
                require(game.server().getPlayerList().getPlayerCount()==24,"exactly23 Carpet plus Alpha");
                for(var id:bots){var p=game.server().getPlayerList().getPlayer(id);require(LbcCarpetBots.bot(p)
                        &&p.connection.getClass().getName().equals("carpet.patches.NetHandlerPlayServerFake"),"real Carpet body and native listener");}
                var alpha=game.server().getPlayerList().getPlayerByName("TestAlpha");require(alpha!=null&&alpha.getClass()==ServerPlayer.class,"one real vanilla ServerPlayer");
                ready=true;SkyWars2.LOGGER.info("[check] PASS bot-startup-prepare (map64/23 actual Carpet plus native Alpha)");
                entryClient = new RegenerationBeltChecks.ScheduledClient(game, alpha);
                entryClient.packets().clear();
            }
            recordCompletedTick(now - 1);
            if(game.stage()==GameController.Stage.COUNTDOWN){
                if (handoff < 0) {
                    handoff = now;
                    captureEntry();
                }
                int n=0;for(var spawn:game.maps().spawns())if(game.maps().level().getBlockState(
                        net.minecraft.core.BlockPos.containing(spawn.position()).below()).is(Blocks.GLASS))n++;
                cages=Math.max(cages,n);require(game.alivePlayers().size()==24,"all24 enrolled before release");return;
            }
            if(!game.playing())return;
            require(game.alivePlayers().size()==24,"all24 actual bodies survive startup window");
            if(release<0){release=now;require(cages==24,"all24 native glass cage floors witnessed");}
            if (!requested || after.size() < 25) return;
            require(entryPositions != null && entryPositions.size() == 1 && entryInCage,
                    "one absolute native teleport to the enrolled cage, sent " + entryPositions);
            require(entryAbilities, "countdown begins with normal Adventure abilities");
            require(preparationNanos > 0 && handoff > armed && handoffNanos > 0 && release > handoff,
                    "distinct completed preparation and cage handoff ticks");
            require(before.size() >= 90 && before.size() == release - handoff - 1,
                    "every completed tick strictly between handoff and release belongs to the countdown");
            require(phases.keySet().containsAll(List.of("reset","cages","islands","chests","kits_and_statistics","finish")),"complete native release phase timings");
            require(releaseNanos>=phases.values().stream().mapToDouble(Double::doubleValue).sum()*1e6-1000,"full release sample contains every measured controller end-tick phase");
            require(game.maps().spawnState().islands().size()==24,"all24 actual pristine island snapshots captured");
            int containers=0,nonempty=0;
            for(var e:game.maps().level().getAllEntities())if(e instanceof Marker&&e.entityTags().stream().anyMatch(
                    t->t.equals("swrg.default")||t.equals("swrg.midle")||t.equals("swrg.center"))){
                var block=game.maps().level().getBlockEntity(net.minecraft.core.BlockPos.containing(e.position()));
                if(block instanceof RandomizableContainerBlockEntity chest){containers++;for(int i=0;i<chest.getContainerSize();i++)if(!chest.getItem(i).isEmpty()){nonempty++;break;}}
            }
            require(containers>=24&&nonempty>0,"source chest markers retain real generated loot");
            SkyWars2.LOGGER.info("[check] bot-startup-24 metrics: map64 players=24 carpet=23 vanilla=1 cages={} containers={} nonempty={} "
                            + "preparationTick={} preparationMs={} handoffTick={} handoffMs={} releaseTick={} releaseFullTickMs={} "
                            + "phasesMs={} countdown={} first25={} fullServerTicks=true resetSteps={}",
                    cages, containers, nonempty, armed, preparationNanos / 1e6, handoff, handoffNanos / 1e6, release, releaseNanos / 1e6,
                    phases, metrics(before, handoff + 1), metrics(after, release + 1), resetSteps);
            require(releaseNanos<100_000_000L,"actual24-player release stays below100ms instead of a second-scale stall");
            passed=true;SkyWars2.LOGGER.info("[check] PASS bot-startup-24 (actual native roster/map/cages/chests and full completed startup ticks)");
        }
        void recordCompletedTick(int tick) {
            long nanos = ServerTickMetrics.completedTickNanos(game.server(), tick);
            require(nanos > 0, "completed outer tick includes all end callbacks");
            // The stage at this callback belongs to the in-progress tick.
            // Read the previous tick against the boundaries observed in their own ticks.
            if (tick == armed) preparationNanos = nanos;
            if (handoff < 0) return;
            if (tick == handoff) handoffNanos = nanos;
            else if (release < 0) before.add(nanos);
            else if (tick == release) releaseNanos = nanos;
            else if (after.size() < 25) after.add(nanos);
        }
        void captureEntry() {
            var player = entryClient.player;
            var teleports = entryClient.packets().stream()
                    .filter(ClientboundPlayerPositionPacket.class::isInstance)
                    .map(ClientboundPlayerPositionPacket.class::cast).toList();
            entryPositions = teleports.stream().map(p -> p.change().position()).toList();
            entryInCage = teleports.size() == 1
                    && Collections.disjoint(teleports.getFirst().relatives(), Relative.position(true, true, true))
                    && game.maps().spawns().stream().anyMatch(spawn -> spawn.position().equals(entryPositions.getFirst()))
                    && player.position().distanceToSqr(entryPositions.getFirst()) < 1.0;
            var abilities = player.getAbilities();
            entryAbilities = player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE
                    && !abilities.flying && !abilities.mayfly && !abilities.invulnerable;
            entryClient.acknowledge();
            if (entryInCage && entryAbilities) {
                SkyWars2.LOGGER.info("[check] PASS bot-startup-direct-handoff (one native spawn position)");
            }
        }
        void close(){
            if(closed)return;closed=true;game.config().countdownSeconds=countdown;Throwable failure=null;
            if (entryClient != null) entryClient.close();
            for(var id:bots){var p=game.server().getPlayerList().getPlayer(id);if(p==null)continue;
                try{game.carpetBots().forget(p);p.connection.onDisconnect(new DisconnectionDetails(Component.literal("Owned startup cleanup")));}
                catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}}
            GameChecks.createBot(game,"TestBeta");running=null;
            if(failure!=null)throw new IllegalStateException("Startup fixture cleanup",failure);
        }
    }
    private static Map<String,Object> metrics(List<Long> values, int firstTick) {
        var sorted = values.stream().mapToLong(Long::longValue).sorted().toArray();
        long peak = sorted[sorted.length - 1];
        return Map.of("samples", sorted.length, "firstTick", firstTick, "maxTick", firstTick + values.indexOf(peak),
                "meanMs", Arrays.stream(sorted).average().orElseThrow() / 1e6,
                "p95Ms", sorted[(int) Math.floor((sorted.length - 1) * .95)] / 1e6, "maxMs", peak / 1e6);
    }
    static void prepare(GameController game){require(running==null,"exclusive fixture");var f=new Fixture(game);running=f;
        try{f.prepare();}catch(Throwable error){try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw new IllegalStateException(error);}}
    static void run(GameController game){require(running!=null&&running.ready&&game.playing(),"prepared native24 match actually started");running.requested=true;}
    static void phase(GameController game,String name){var f=running;if(f!=null&&f.game==game)f.phase(name);}
    static void resetStep(GameController game, String name) {
        var f = running;
        if (f != null && f.game == game) f.resetStep(name);
    }
    static void tick(GameController game){var f=running;if(f==null||f.game!=game)return;try{f.tick();}
        catch(Throwable error){try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL bot-startup-24 phases="+f.phases+" release="+f.release+" fullNanos="+f.releaseNanos+" cages="+f.cages+" resetSteps="+f.resetSteps,error);}}
}
