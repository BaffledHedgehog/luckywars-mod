package me.skywars2.game;

import carpet.patches.NetHandlerPlayServerFake;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import jdk.jfr.Recording;
import me.skywars2.SkyWars2;
import me.skywars2.game.entities.bots.LbcCarpetBots;
import me.skywars2.game.entities.projectiles.LbcHoming;
import me.skywars2.game.items.wands.LbcBolts;
import me.skywars2.game.items.wands.LbcSauvojen;
import me.skywars2.game.items.wands.LbcWands;
import me.skywars2.game.match.checkpoints.MatchCoreCheckpoint;
import me.skywars2.game.match.checkpoints.MatchRandomState;
import me.skywars2.game.net.metrics.ServerTickMetrics;
import me.skywars2.game.world.maps.MapManager;
import me.skywars2.validation.mixin.FixtureAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;
import me.skywars2.game.effects.magic.LbcMana;

/** A native, offline stress check. The runner owns a disposable server save. */
final class SauvojenStressChecks {
    private static final int BOTS=15, CASTS=10, WARMUP_TICKS=100, IDLE_TICKS=80, BURST_TICKS=420;
    private static final int FLUID_SAMPLE_INTERVAL=10;
    private static final int WATER_TAIL_LIMIT=3600;
    private static Fixture running;

    private SauvojenStressChecks() {}

    static void run(GameController game){
        require(running==null,"only one stress fixture");
        var next=new Fixture(game);
        try{next.setup();running=next;}
        catch(Throwable error){
            try{next.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}
            throw new IllegalStateException("Sauvojen stress setup",error);
        }
    }

    static void tick(GameController game){
        if(running==null)return;
        var current=running;
        try{
            require(game==current.game,"same server");
            if(!current.step())return;
            running=null;current.close();
            SkyWars2.LOGGER.info("[check] PASS sauvojen-stress (15 Carpet bots, 10 paid same-tick Academy casts, personal homing={}, idle and cast native tick metrics)",current.homing);
        }catch(Throwable error){
            running=null;
            try{current.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}
            SkyWars2.LOGGER.error("[check] FAIL sauvojen-stress",error);
        }
    }

    private static void require(boolean condition,String reason){
        if(!condition)throw new IllegalStateException("Sauvojen stress: "+reason);
    }

    private record AirBranchRandom(RandomSource delegate) implements RandomSource {
        @Override public RandomSource fork() { return delegate.fork(); }
        @Override public PositionalRandomFactory forkPositional() { return delegate.forkPositional(); }
        @Override public void setSeed(long seed) { delegate.setSeed(seed); }
        @Override public int nextInt() { return delegate.nextInt(); }
        @Override public int nextInt(int bound) {
            return bound == 9 ? LbcSauvojen.Kind.AIR.ordinal() : delegate.nextInt(bound);
        }
        @Override public int nextInt(int origin,int bound) { return delegate.nextInt(origin,bound); }
        @Override public int nextIntBetweenInclusive(int min,int max) { return delegate.nextIntBetweenInclusive(min,max); }
        @Override public long nextLong() { return delegate.nextLong(); }
        @Override public boolean nextBoolean() { return delegate.nextBoolean(); }
        @Override public float nextFloat() { return delegate.nextFloat(); }
        @Override public double nextDouble() { return delegate.nextDouble(); }
        @Override public double nextGaussian() { return delegate.nextGaussian(); }
        @Override public double triangle(double mean,double spread) { return delegate.triangle(mean,spread); }
        @Override public float triangle(float mean,float spread) { return delegate.triangle(mean,spread); }
        @Override public void consumeCount(int rounds) { delegate.consumeCount(rounds); }
    }

    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final ServerPlayer owner, other;
        final ServerLevel level;
        final RegenerationBeltChecks.Snapshot saved, otherSaved;
        final MatchCoreCheckpoint core;
        final MatchRandomState random;
        final Set<Entity> nativeProjectiles;
        final Set<?> watering;
        final List<ServerPlayer> bots=new ArrayList<>();
        final List<ChunkPos> forced=new ArrayList<>();
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();
        final boolean homing=Boolean.parseBoolean(System.getProperty("skywars2.check.sauvojenHoming","true"));
        final boolean allAir=Boolean.getBoolean("skywars2.check.sauvojenAllAir");
        final boolean suppressWatering=Boolean.getBoolean("skywars2.check.sauvojenSuppressWatering");
        final Path jfr=Path.of(System.getProperty("skywars2.check.sauvojenJfr","sauvojen-stress.jfr"));
        final Path csv=Path.of(System.getProperty("skywars2.check.sauvojenCsv","sauvojen-stress.csv"));
        final boolean waterTail=Boolean.getBoolean("skywars2.check.sauvojenWaterTail");
        final Measurement idle=new Measurement("idle",IDLE_TICKS);
        final Measurement burst=new Measurement("burst",waterTail?WATER_TAIL_LIMIT:BURST_TICKS);
        Recording recording;
        Instant profileStart;
        long profileClock;
        int setupTick,removedWatering;
        boolean closed;

        Fixture(GameController game){
            this.game=game;
            var players=game.server().getPlayerList();
            owner=players.getPlayer(GameChecks.offlineUuid("TestAlpha"));
            other=players.getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(owner!=null&&game.playing()&&game.allowDamage(owner)&&game.canUseGameplayItems(owner)
                    &&owner.level()==game.maps().level()&&!game.balanceLegacy(),"active ordinary TestAlpha match");
            require(players.getPlayers().stream().allMatch(player->player==owner||player==other)
                    &&game.items().sauvojen().count()==0&&game.items().homing().count()==0,"exclusive disposable match");
            level=owner.level();core=new MatchCoreCheckpoint(game);
            saved=new RegenerationBeltChecks.Snapshot(game,owner);
            otherSaved=other==null?null:new RegenerationBeltChecks.Snapshot(game,other);
            random=MatchRandomState.capture(owner.getRandom());
            nativeProjectiles=((FixtureAccess.Homing)(Object)game.items().homing()).validation$projectiles();
            watering=((FixtureAccess.BoltActors)(Object)game.items().bolts()).validation$waters();
        }

        void setup() {
            game.config().noBorders=true;
            game.config().lbcSkills=true;
            game.config().ranked=true;
            game.players().get(owner.getUUID()).lbcSkill=homing?30:0;
            require(game.lbc().hasSkill(owner,30)==homing,"personal Homing skill toggle");

            int y=Math.min(level.getMaxY()-16,Math.max(160,owner.blockPosition().getY()+48));
            var floor=BlockPos.containing(owner.getX(),y-1,owner.getZ());
            var origin=Vec3.atBottomCenterOf(floor.above());
            for(int x=(floor.getX()-16)>>4;x<=(floor.getX()+16)>>4;x++){
                for(int z=(floor.getZ()-16)>>4;z<=(floor.getZ()+16)>>4;z++){
                    long key=ChunkPos.pack(x,z);
                    if(!level.getForceLoadedChunks().contains(key)){
                        forced.add(new ChunkPos(x,z));level.setChunkForced(x,z,true);
                    }
                    level.getChunk(x,z);
                }
            }
            for(int dx=-16;dx<=16;dx++)for(int dz=-16;dz<=16;dz++){
                var pos=floor.offset(dx,0,dz);
                terrain.put(pos,game.maps().snapshot(level,pos));
                level.setBlock(pos,Blocks.BEDROCK.defaultBlockState(),2|16|32);
            }
            owner.removeAllEffects();owner.getInventory().clearContent();
            fortify(owner);move(owner,origin);
            owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcWands.ACADEMY_SAUVOJEN));
            game.items().mana().maximum(owner,1_000_000);
            game.items().mana().set(owner,1_000_000);
            game.items().mana().noMagic(owner,0);

            for(int index=0;index<BOTS;index++){
                String name=String.format(Locale.ROOT,"botSauv%02d",index);
                var bot=CarpetBotChecks.connect(game,name,level,owner.clientInformation());
                bots.add(bot);
                require(game.returnFromGameModeCommand(bot,GameType.SURVIVAL),"ordinary Carpet participant "+name);
                bot.setGameMode(GameType.SURVIVAL);
                require(LbcCarpetBots.bot(bot)&&bot.connection instanceof NetHandlerPlayServerFake,"genuine Carpet player and connection "+name);
                bot.getInventory().clearContent();bot.removeAllEffects();fortify(bot);
                game.players().get(bot.getUUID()).lbcSkill=0;
                game.server().getScoreboard().removePlayerFromTeam(bot.getScoreboardName());
                double angle=index*2*Math.PI/BOTS;
                move(bot,origin.add(Math.cos(angle)*12,0,Math.sin(angle)*12));
                bot.hasChangedDimension();bot.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
                bot.getRandom().setSeed(0x53415556L+index);
            }
            if(other!=null){game.spectators().enter(other);other.setGameMode(GameType.SPECTATOR);}
            game.server().getScoreboard().removePlayerFromTeam(owner.getScoreboardName());
            setupTick=game.server().getTickCount();
            SkyWars2.LOGGER.info("[check] Sauvojen stress setup: tick={} utc={} bots={} homing={} allAir={} suppressWatering={} origin={}",setupTick,Instant.now(),bots.size(),homing,allAir,suppressWatering,origin);
        }

        void fortify(ServerPlayer player){
            player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10_000);
            player.setHealth(10_000);player.setAbsorptionAmount(0);player.setInvulnerableTime(0);
        }

        void move(ServerPlayer player,Vec3 at){
            require(player.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true),"native fixture position for "+player.getScoreboardName());
            player.setDeltaMovement(Vec3.ZERO);player.setKnownMovement(Vec3.ZERO);player.setOnGround(true);
        }

        boolean step() throws Exception {
            int tick=game.server().getTickCount();
            require(game.playing()&&game.allowDamage(owner)&&tick-setupTick<(waterTail?WATER_TAIL_LIMIT+300:800),"bounded live stress run");
            if(recording==null){
                if(tick-setupTick<WARMUP_TICKS)return false;
                beginRecording();idle.begin(tick+1);return false;
            }
            if(!idle.complete()){
                idle.recordScene(tick);
                if(idle.collect(tick-1)){
                    idle.report();burst.begin(tick);cast();
                    burst.recordScene(tick);
                }
                return false;
            }
            if(suppressWatering)removeNewWatering();
            burst.recordScene(tick);
            boolean exhausted=burst.collect(tick-1);
            boolean tailSettled=waterTail&&burst.count>=BURST_TICKS&&burst.count%FLUID_SAMPLE_INTERVAL==0&&wateringSettled();
            if(!exhausted&&!tailSettled)return false;
            burst.report();
            require(bots.stream().allMatch(bot->bot.isAlive()&&bot.level()==level&&game.allowDamage(bot)),"all fifteen Carpet bodies stayed live in the measured match");
            require(burst.peakProjectiles>0&&game.items().sauvojen().count()==0,"native projectiles and full Academy carrier lifetime in the measured window, peak="+burst.peakProjectiles+", drones="+game.items().sauvojen().count());
            require(!suppressWatering||allAir||removedWatering>0,"mixed run suppressed real BoltBolt watering before its first fill");
            SkyWars2.LOGGER.info("[check] Sauvojen stress flight: nativeProjectilePeak={} finalDrones={} homing={} suppressedWatering={}",burst.peakProjectiles,game.items().sauvojen().count(),homing,removedWatering);
            if(waterTail)SkyWars2.LOGGER.info("[check] Sauvojen stress watering tail: drained={} scheduledFluidTicks={} remainingActors={}",
                    tailSettled,level.getFluidTicks().count(),watering.size());
            writeMetrics();
            require(!waterTail||tailSettled,"all authored water work and native water queue drain before the bounded tail limit");
            return true;
        }

        private boolean wateringSettled(){
            if(!watering.isEmpty()||level.getFluidTicks().count()>idle.peakFluidTicks+512)return false;
            for(var work:game.items().stonekivi().queuedSnapshot()){
                if(game.items().bolts().isWaterWork(work,owner,level))return false;
            }
            return true;
        }

        void removeNewWatering(){
            var iterator=watering.iterator();
            while(iterator.hasNext()){
                var water=(LbcBolts.Water)iterator.next();
                if(water.owner!=owner||water.level!=level)continue;
                // This callback follows bolts.tick, so a new producer has only reached age one.
                require(water.age<=1,"watering suppression precedes any fill: age="+water.age);
                iterator.remove();removedWatering++;
            }
        }

        void beginRecording(){
            require(game.carpetBots().count()>=BOTS,"all real bots entered their native AI scheduler");
            recording=new Recording();
            recording.setName("Sauvojen Academy stress");
            recording.enable("jdk.ExecutionSample").withPeriod(Duration.ofMillis(5)).withStackTrace();
            recording.enable("jdk.NativeMethodSample").withPeriod(Duration.ofMillis(10)).withStackTrace();
            recording.enable("jdk.GarbageCollection");
            recording.enable("jdk.ClassLoad").withThreshold(Duration.ofMillis(5)).withStackTrace();
            for(String event:List.of("jdk.ThreadPark","jdk.JavaMonitorEnter","jdk.FileRead","jdk.FileWrite"))
                recording.enable(event).withThreshold(Duration.ofMillis(10)).withStackTrace();
            recording.start();profileClock=System.nanoTime();profileStart=Instant.now();
        }

        void cast() {
            var service=game.items().sauvojen();
            require(service.count()==0,"idle window starts without Sauvojen drones");
            require(game.lbc().hasSkill(owner,30)==homing,"same Homing setting at launch");
            owner.setShiftKeyDown(false);
            var mana=game.items().mana();
            int price=mana.score(ScoreHolder.forNameOnly("sauvojen_wand_mana_use"),"lbc.math").get();
            require(price>0&&price<=100_000,"ordinary configured mana price");
            mana.set(owner,price*CASTS);
            int tick=game.server().getTickCount();

            var access=(FixtureAccess.EntityRandom)owner;
            var original=owner.getRandom();var seed=RandomSource.create(3);
            RandomSource selected=allAir?new AirBranchRandom(seed):seed;
            access.validation$random(selected);long started=System.nanoTime();
            try{
                for(int i=0;i<CASTS;i++)require(owner.getMainHandItem().use(level,owner,InteractionHand.MAIN_HAND).consumesAction(),"native paid cast "+(i+1));
            }
            finally{access.validation$random(original);}
            double launchMs=(System.nanoTime()-started)/1_000_000d;
            var drones=service.snapshot();
            require(game.server().getTickCount()==tick&&drones.size()==CASTS&&mana.get(owner)==0
                    &&drones.stream().allMatch(drone->drone.academy&&drone.owner==owner&&drone.lifetime==0
                    &&level.getEntity(drone.carrier.getUUID())==drone.carrier),"ten same-tick, published Academy drones and ten mana debits");
            var branches=new EnumMap<LbcSauvojen.Kind,Integer>(LbcSauvojen.Kind.class);
            for(var drone:drones)branches.merge(drone.kind,1,Integer::sum);
            require(allAir?branches.size()==1&&branches.containsKey(LbcSauvojen.Kind.AIR)
                    :branches.keySet().containsAll(List.of(LbcSauvojen.Kind.AIR,LbcSauvojen.Kind.TERRA)),"fixed native Academy branch mix: "+branches);
            SkyWars2.LOGGER.info("[check] Sauvojen stress casts: tick={} utc={} launchMs={} homing={} branches={} bots={}",tick,Instant.now(),launchMs,homing,branches,bots.size());
        }

        void writeMetrics() throws Exception {
            var rows=new StringBuilder("phase,tick,mspt,observed_at_utc,native_projectiles,scheduled_fluid_ticks\n");
            idle.rows(rows);burst.rows(rows);
            Files.writeString(csv,rows);
            var projectiles=new StringBuilder("phase,entity_type,variant,peak,entity_ticks,peak_caster_owned,caster_owned_ticks\n");
            idle.projectileRows(projectiles);burst.projectileRows(projectiles);
            Files.writeString(csv.resolveSibling(csv.getFileName()+".projectiles.csv"),projectiles);
            recording.stop();recording.dump(jfr);
        }

        @Override public void close(){
            if(closed)return;closed=true;
            try{
                if(recording!=null)recording.close();
                game.items().sauvojen().forget(owner);
                for(var bot:bots){
                    game.carpetBots().forget(bot);game.items().indirectKills().clear(bot);
                    bot.connection.onDisconnect(new DisconnectionDetails(Component.literal("Sauvojen stress complete")));
                    bot.discard();
                }
                terrain.forEach((pos,state)->game.maps().restoreSnapshot(level,pos,state,2|16|32));
            }finally{
                for(var chunk:forced)level.setChunkForced(chunk.x(),chunk.z(),false);
                var ticks=game.server().tickRateManager();
                boolean frozen=ticks.isFrozen();
                ticks.setFrozen(true);
                try {
                    core.restoreFrozen();saved.restoreAttributes();saved.restore(game);
                    if(otherSaved!=null){otherSaved.restoreAttributes();otherSaved.restore(game);}
                    random.restore(owner.getRandom());
                } finally {
                    ticks.setFrozen(frozen);
                }
            }
        }

        private final class Measurement {
            final String name;
            final long[] nanos, observed;
            final int[] projectiles, fluidTicks;
            final Map<EntityType<?>,EnumMap<ProjectileVariant,Census>> census=new IdentityHashMap<>();
            int first,count,peakProjectiles,peakFluidTicks,fluidSamples;
            Instant started;
            Measurement(String name,int samples){
                this.name=name;nanos=new long[samples];observed=new long[samples];
                projectiles=new int[samples];fluidTicks=new int[samples];Arrays.fill(fluidTicks,-1);
            }
            void begin(int tick){
                first=tick;started=Instant.now();
                SkyWars2.LOGGER.info("[check] Sauvojen stress window start: phase={} firstTick={} utc={}",name,first,started);
            }
            boolean complete(){return count==nanos.length;}
            void recordScene(int tick){
                int index=tick-first;
                if(index<0||index>=nanos.length)return;
                if(index%FLUID_SAMPLE_INTERVAL==0){
                    // LevelTicks.count walks the loaded chunk queues. Sample at ten-tick intervals.
                    fluidTicks[index]=level.getFluidTicks().count();fluidSamples++;
                    peakFluidTicks=Math.max(peakFluidTicks,fluidTicks[index]);
                }
                int total=0;
                for(var projectile:nativeProjectiles){
                    if(projectile.isRemoved()||projectile.level()!=level)continue;
                    var variants=census.computeIfAbsent(projectile.getType(),ignored->new EnumMap<>(ProjectileVariant.class));
                    var group=variants.computeIfAbsent(ProjectileVariant.of(projectile),ignored->new Census());
                    group.add(tick,projectile instanceof Projectile physical&&physical.getOwner()==owner);total++;
                }
                projectiles[index]=total;peakProjectiles=Math.max(peakProjectiles,total);
            }
            boolean collect(int tick){
                if(tick<first)return false;
                require(tick==first+count,"contiguous native duration rows: "+name+" tick="+tick);
                long duration=ServerTickMetrics.completedTickNanos(game.server(),tick);
                require(duration>=0,"complete native tick duration for "+tick);
                nanos[count]=duration;observed[count++]=System.nanoTime();return complete();
            }
            void report(){
                var measured=Arrays.copyOf(nanos,count);
                var ordered=measured.clone();Arrays.sort(ordered);
                double mean=Arrays.stream(measured).average().orElseThrow()/1_000_000d;
                double tps=(count-1)*1_000_000_000d/(observed[count-1]-observed[0]);
                double median=ordered[(int)Math.ceil(count*.50)-1]/1_000_000d;
                double p95=ordered[(int)Math.ceil(count*.95)-1]/1_000_000d;
                double p99=ordered[(int)Math.ceil(count*.99)-1]/1_000_000d;
                SkyWars2.LOGGER.info("[check] Sauvojen stress window: phase={} homing={} firstTick={} lastTick={} startUtc={} endUtc={} samples={} tps={} meanMspt={} p50Mspt={} p95Mspt={} p99Mspt={} maxMspt={} over50ms={}",
                        name,homing,first,first+count-1,started,Instant.now(),count,tps,mean,median,p95,p99,
                        ordered[count-1]/1_000_000d,Arrays.stream(measured).filter(value->value>50_000_000).count());
                SkyWars2.LOGGER.info("[check] Sauvojen stress fluid: phase={} sampleEveryTicks={} samples={} sampledScheduledPeak={} projectilePeak={}",name,FLUID_SAMPLE_INTERVAL,fluidSamples,peakFluidTicks,peakProjectiles);
            }
            void rows(StringBuilder out){
                for(int i=0;i<count;i++){
                    out.append(name).append(',').append(first+i).append(',').append(nanos[i]/1_000_000d).append(',')
                            .append(profileStart.plusNanos(observed[i]-profileClock)).append(',').append(projectiles[i]).append(',');
                    if(fluidTicks[i]>=0)out.append(fluidTicks[i]);
                    out.append('\n');
                }
            }
            void projectileRows(StringBuilder out){
                census.entrySet().stream().sorted(Comparator.comparing(entry->BuiltInRegistries.ENTITY_TYPE.getKey(entry.getKey()).toString()))
                        .forEach(entry->entry.getValue().forEach((variant,group)->out.append(name).append(',')
                                .append(BuiltInRegistries.ENTITY_TYPE.getKey(entry.getKey())).append(',')
                                .append(variant.name().toLowerCase(Locale.ROOT)).append(',').append(group.peak).append(',')
                                .append(group.entityTicks).append(',').append(group.peakOwned).append(',').append(group.ownedTicks).append('\n')));
            }
        }
    }

    private enum ProjectileVariant {
        ORDINARY, SISYPHUS, HOMING_FIREBALL, FALLING_STARS;
        static ProjectileVariant of(Entity body){
            var tags=body.entityTags();
            if(tags.contains(LbcHoming.SLOW_TAG))return SISYPHUS;
            if(tags.contains(LbcHoming.TAG))return HOMING_FIREBALL;
            if(tags.contains(LbcHoming.DONOR_TAG))return FALLING_STARS;
            return ORDINARY;
        }
    }

    private static final class Census {
        int tick=-1,live,owned,peak,peakOwned;
        long entityTicks,ownedTicks;
        void add(int now,boolean casterOwned){
            if(tick!=now){tick=now;live=owned=0;}
            peak=Math.max(peak,++live);entityTicks++;
            if(casterOwned){peakOwned=Math.max(peakOwned,++owned);ownedTicks++;}
        }
    }
}
