package me.skywars2.game;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.ActorCheckpointAccess;
import me.skywars2.mixin.ArenaChunkAccess;
import me.skywars2.mixin.RandomCheckpointAccess;
import me.skywars2.mixin.ScoreboardCheckpointAccess;
import me.skywars2.mixin.TerrainCheckpointEventsAccess;
import me.skywars2.mixin.TerrainCheckpointTicksAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.ticks.LevelChunkTicks;

/** Capture-only measurements on the actual resident match. Never restores or constructs a test arena. */
public final class VanitySnapshotPerfChecks {
    private static Run running;
    private static final int WARMUP_INDEX=1,MEASURED_START=2,SAMPLE_COUNT=7;
    private VanitySnapshotPerfChecks() { }

    public static void run(GameController game){start(game,false);}
    public static void runServices(GameController game){start(game,true);}
    private static void start(GameController game,boolean services){
        if(running!=null)throw new IllegalStateException("Snapshot performance check already active");
        running=new Run(game,services);
        SkyWars2.LOGGER.info("[check] START {} (first capture, one warm-up, five measured captures on separate end-server-ticks; no load/spawn/write/restore/forced-GC; timer overhead included)",running.suite);
    }

    /** Hook at the existing end-server-tick check dispatcher; run() itself never takes a snapshot. */
    public static void tick(GameController game){
        var current=running;if(current==null)return;
        try{
            if(current.game!=game)throw new IllegalStateException("Different benchmark server/controller");
            int frame=game.server().getTickCount();if(current.lastFrame==frame)return;current.lastFrame=frame;
            current.sample();
            if(current.index<SAMPLE_COUNT)return;
            current.summary();running=null;
            SkyWars2.LOGGER.info("[check] PASS {} (actual loaded native snapshots; per-phase wall time/server-thread allocation, raw cold and warm-up, five-sample median/max; heap delta is GC-sensitive, not retained size; acquisition remains gated)",current.suite);
        }catch(Throwable failure){running=null;SkyWars2.LOGGER.error("[check] FAIL "+current.suite,failure);}
    }

    private record Metric(long nanos,long allocated) { }
    private record Sample(Map<String,Metric> phases,Metric total) { }
    private record Gc(long collections,long millis) { }
    private record Dimension(String name,long chunks,long sections,long nonAirSections,long blockEntities,
                             long blockTicks,long pendingBlockTicks,long fluidTicks,long pendingFluidTicks,
                             long events,long deferred,long actors,long living) { }
    private record Counts(List<Dimension> dimensions,int players,int objectives,int teams,int scoreOwners,long scores,int sequences) { }

    private static final class Run {
        final GameController game;final boolean services;final String suite,epoch;
        final List<Sample> measured=new ArrayList<>();
        final MemoryMXBean memory=ManagementFactory.getMemoryMXBean();
        final List<GarbageCollectorMXBean> collectors=ManagementFactory.getGarbageCollectorMXBeans();
        final com.sun.management.ThreadMXBean allocations;
        int index,lastFrame=Integer.MIN_VALUE;
        Run(GameController game,boolean services){
            this.game=game;this.services=services;suite=services?"vanity-snapshot-perf-services":"vanity-snapshot-perf";
            stable();epoch=game.items().epoch();
            var bean=ManagementFactory.getThreadMXBean();com.sun.management.ThreadMXBean supported=null;
            if(bean instanceof com.sun.management.ThreadMXBean candidate&&candidate.isThreadAllocatedMemorySupported())try{
                if(!candidate.isThreadAllocatedMemoryEnabled())candidate.setThreadAllocatedMemoryEnabled(true);
                if(candidate.isThreadAllocatedMemoryEnabled())supported=candidate;
            }catch(RuntimeException unavailable){/* Unsupported/security-disabled allocation counters remain explicitly n/a. */}
            allocations=supported;
        }
        void stable(){
            if(!game.server().isSameThread()||!game.playing()||game.maps().busy()||game.stage()==GameController.Stage.ENDING)
                throw new IllegalStateException("Measure snapshots at a stable actual match end-server-tick");
        }
        long allocated(){return allocations==null?-1:allocations.getThreadAllocatedBytes(Thread.currentThread().threadId());}
        Gc gc(){
            long count=0,millis=0;boolean hasCount=false,hasTime=false;
            for(var collector:collectors){long c=collector.getCollectionCount(),t=collector.getCollectionTime();
                if(c>=0){count+=c;hasCount=true;}if(t>=0){millis+=t;hasTime=true;}}
            return new Gc(hasCount?count:-1,hasTime?millis:-1);
        }
        static long difference(long before,long after){return before<0||after<0?-1:after-before;}
        <T>T phase(Map<String,Metric> rows,String name,Supplier<T> capture,List<Object> keep){
            long bytes=allocated(),started=System.nanoTime();T value=capture.get();long elapsed=System.nanoTime()-started,after=allocated();
            rows.put(name,new Metric(elapsed,difference(bytes,after)));keep.add(value);return value;
        }
        void sample(){
            stable();if(!game.items().currentEpoch(epoch))throw new IllegalStateException("Match epoch changed during capture benchmark");
            var players=List.copyOf(game.server().getPlayerList().getPlayers());
            var counts=count(players); // Enumeration/reporting are outside capture timers and allocation counters.
            var rows=new LinkedHashMap<String,Metric>();var keep=new ArrayList<Object>();
            long heapBefore=memory.getHeapMemoryUsage().getUsed();var gcBefore=gc();
            long bytesBefore=allocated(),started=System.nanoTime();
            var terrain=phase(rows,"terrain",()->MatchTerrainCheckpoint.captureLoaded(game.server()),keep);
            var actors=phase(rows,"actors",()->MatchActorCheckpoint.captureRegistered(game),keep);
            phase(rows,"players",()->players.stream().map(player->new MatchPlayerCheckpoint(game,player)).toList(),keep);
            phase(rows,"core",()->new MatchCoreCheckpoint(game),keep);
            phase(rows,"world",()->new MatchWorldCheckpoint(game),keep);
            phase(rows,"world-rng",()->new MatchWorldRandomCheckpoint(game),keep);
            if(services){var sources=actors.damageSources();
                phase(rows,"time-stop",()->game.items().timeStop().checkpoint(sources),keep);
                phase(rows,"attribution",()->game.items().indirectKills().checkpoint(sources),keep);
                phase(rows,"spells",()->new MatchSpellCheckpoint(game,sources),keep);
                phase(rows,"authorities",()->new MatchAuthorityCheckpoint(game,sources),keep);
                phase(rows,"queued-work",()->new MatchQueuedWorkCheckpoint(game,sources),keep);
                phase(rows,"fpv",()->game.items().fpvDrone().checkpoint(sources),keep);
                phase(rows,"blasts",()->new MatchBlastCheckpoint(game,sources),keep);
            }
            long totalNanos=System.nanoTime()-started,bytesAfter=allocated();
            long heapAfter=memory.getHeapMemoryUsage().getUsed();var gcAfter=gc();
            var sample=new Sample(Collections.unmodifiableMap(new LinkedHashMap<>(rows)),new Metric(totalNanos,difference(bytesBefore,bytesAfter)));
            String label=index==0?"cold":index==WARMUP_INDEX?"warmup":"measured-"+(index-MEASURED_START+1);
            long countedChunks=counts.dimensions.stream().mapToLong(Dimension::chunks).sum();
            long countedActors=counts.dimensions.stream().mapToLong(Dimension::actors).sum();
            if(terrain.chunks()!=countedChunks||actors.size()!=countedActors)throw new IllegalStateException("Native resident population changed during one synchronous capture");
            SkyWars2.LOGGER.info("[snapshot-perf] sample={} label={} serverTick={} matchTick={} nativeFrozen={} scope=all-loaded-dimensions chunks={} actors={} players={} objectives={} teams={} scoreOwners={} scores={} namedRngSequences={}",
                    index,label,lastFrame,game.matchTicks(),game.server().tickRateManager().isFrozen(),terrain.chunks(),actors.size(),counts.players,counts.objectives,counts.teams,counts.scoreOwners,counts.scores,counts.sequences);
            for(var dimension:counts.dimensions)SkyWars2.LOGGER.info("[snapshot-perf] sample={} dimension={} chunks={} sections={} nonAirSections={} bePositions={} blockTicks={} pendingBlockTicks={} fluidTicks={} pendingFluidTicks={} events={} deferredEvents={} actors={} livingActors={}",
                    index,dimension.name,dimension.chunks,dimension.sections,dimension.nonAirSections,dimension.blockEntities,dimension.blockTicks,dimension.pendingBlockTicks,dimension.fluidTicks,dimension.pendingFluidTicks,dimension.events,dimension.deferred,dimension.actors,dimension.living);
            rows.forEach((name,value)->logMetric(index,label,name,value));logMetric(index,label,"total",sample.total);
            SkyWars2.LOGGER.info("[snapshot-perf] sample={} heapUsedBefore={} heapUsedAfter={} heapUsedDeltaGcSensitive={} gcCollectionsDelta={} gcMillisDelta={} allocationScope=server-thread-only retainedSnapshotBytes=not-measured",
                    index,heapBefore,heapAfter,heapAfter-heapBefore,available(difference(gcBefore.collections,gcAfter.collections)),available(difference(gcBefore.millis,gcAfter.millis)));
            // Keep exactly this one full bundle live through heap measurement, then release it.
            Reference.reachabilityFence(keep);
            if(index>=MEASURED_START)measured.add(sample);index++;
        }
        Counts count(List<ServerPlayer> players){
            var worlds=new ArrayList<Dimension>();
            for(var level:game.server().getAllLevels()){
                long chunks=0,sections=0,nonAir=0,entities=0,blockTicks=0,pendingBlocks=0,fluidTicks=0,pendingFluids=0;
                for(var holder:((ArenaChunkAccess)level.getChunkSource().chunkMap).skywars2$visibleChunks().values())
                    if(holder.getLatestChunk() instanceof LevelChunk chunk){
                        chunks++;sections+=chunk.getSectionsCount();for(var section:chunk.getSections())if(!section.hasOnlyAir())nonAir++;
                        var blockQueue=(LevelChunkTicks<?>)chunk.getBlockTicks();var fluidQueue=(LevelChunkTicks<?>)chunk.getFluidTicks();
                        entities+=chunk.getBlockEntitiesPos().size();blockTicks+=blockQueue.getAll().count();fluidTicks+=fluidQueue.getAll().count();
                        var blocks=((TerrainCheckpointTicksAccess<?>)blockQueue).skywars2$pendingTicks();
                        var fluids=((TerrainCheckpointTicksAccess<?>)fluidQueue).skywars2$pendingTicks();
                        if(blocks!=null)pendingBlocks+=blocks.size();if(fluids!=null)pendingFluids+=fluids.size();
                    }
                var manager=((ActorCheckpointAccess.LevelEntities)level).skywars2$manager();var storage=((ActorCheckpointAccess.Sections)manager).skywars2$sections();
                long[] actors={0,0};for(long chunk:storage.getAllChunksWithExistingSections().toLongArray())
                    storage.getExistingSectionsInChunk(chunk).forEach(section->section.getEntities().forEach(entity->{
                        if(!(entity instanceof ServerPlayer)&&!entity.isRemoved()){actors[0]++;if(entity instanceof LivingEntity)actors[1]++;}}));
                var events=(TerrainCheckpointEventsAccess)level;
                worlds.add(new Dimension(level.dimension().identifier().toString(),chunks,sections,nonAir,entities,blockTicks,pendingBlocks,fluidTicks,pendingFluids,
                        events.skywars2$events().size(),events.skywars2$deferred().size(),actors[0],actors[1]));
            }
            var board=game.server().getScoreboard();var scoreboard=(ScoreboardCheckpointAccess)board;long scores=0;
            for(var info:scoreboard.skywars2$players().values())scores+=((ScoreboardCheckpointAccess.Player)info).skywars2$scores().size();
            return new Counts(List.copyOf(worlds),players.size(),board.getObjectives().size(),board.getPlayerTeams().size(),scoreboard.skywars2$players().size(),scores,
                    ((RandomCheckpointAccess.Sequences)game.server().getRandomSequences()).skywars2$sequences().size());
        }
        void summary(){
            if(measured.size()!=5)throw new IllegalStateException("Expected exactly five measured snapshot samples");
            for(var name:measured.getFirst().phases.keySet())summary(name,measured.stream().map(sample->sample.phases.get(name)).toList());
            summary("total",measured.stream().map(Sample::total).toList());
        }
        void summary(String phase,List<Metric> values){
            long[] times=values.stream().mapToLong(Metric::nanos).sorted().toArray();
            long[] bytes=values.stream().mapToLong(Metric::allocated).toArray();boolean known=Arrays.stream(bytes).allMatch(value->value>=0);Arrays.sort(bytes);
            SkyWars2.LOGGER.info("[snapshot-perf] summary phase={} measuredSamples=5 medianNs={} medianMs={} maxNs={} maxMs={} medianAllocatedBytes={} maxAllocatedBytes={}",
                    phase,times[2],millis(times[2]),times[4],millis(times[4]),known?Long.toString(bytes[2]):"n/a",known?Long.toString(bytes[4]):"n/a");
        }
    }
    private static String millis(long nanos){return String.format(Locale.ROOT,"%.3f",nanos/1_000_000.0);}
    private static String available(long value){return value<0?"n/a":Long.toString(value);}
    private static void logMetric(int index,String label,String phase,Metric metric){
        SkyWars2.LOGGER.info("[snapshot-perf] sample={} label={} phase={} ns={} ms={} allocatedBytes={}",index,label,phase,metric.nanos,millis(metric.nanos),available(metric.allocated));
    }
}
