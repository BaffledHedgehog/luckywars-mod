package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

/** Full completed server-tick timings with 24 actual Carpet players, not a timer around the AI method. */
final class BotPopulationChecks {
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Bot population: "+why);}
    private static final class Fixture extends CarpetBotChecks.Fixture {
        final List<Long> baseline=new ArrayList<>(),looting=new ArrayList<>(),combat=new ArrayList<>();
        final Map<UUID,Vec3> start=new LinkedHashMap<>();final Set<UUID> acted=new HashSet<>(),preexisting=new HashSet<>();
        final List<BlockPos> starters=new ArrayList<>();PlayerTeam team;int at,spawnedAt,minimumCombat=24;boolean released;
        final boolean eruption,movie,oldSkills,oldGriefing;final String oldCache;
        final List<Long> eruptionTicks=new ArrayList<>(),burstTicks=new ArrayList<>();final ArrayDeque<Long> recent=new ArrayDeque<>();
        int eruptionTrial,trialStarted,burstObserved=-1;long cachedAt;
        Fixture(GameController game,boolean eruption,boolean movie){
            super(game,new Vec3(.5,120,.5));this.eruption=eruption;this.movie=movie;oldSkills=game.config().lbcSkills;
            oldGriefing=base.level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING);
            oldCache=System.getProperty("skywars2.disableExplosionCache");
        }
        int movieStarted,completeFrame=-1;long movieCoalesced;
        final List<Long> movieTicks=new ArrayList<>();final Set<Integer> movieFrames=new HashSet<>();
        boolean movieStopping;
        void startMovie(){
            var center=BlockPos.containing(origin);
            for(int x=-114>>4;x<=113>>4;x++)for(int z=-85>>4;z<=85>>4;z++){
                long key=ChunkPos.pack(x,z);if(!base.level.getForceLoadedChunks().contains(key)){forced.add(key);base.level.setChunkForced(x,z,true);}base.level.getChunk(x,z);
            }
            require(game.items().badApple().start(actors.getFirst(),base.level,Vec3.ZERO,game.items().indirectKills().wildCause(actors.getFirst())),"real full-size movie begins after all footprint chunks are resident");
            movieStarted=game.server().getTickCount();
        }
        boolean movieStep(int now,int live){
            require(live==24,"all24 active bot bodies remain alive throughout the real full-media terrain benchmark");
            var state=game.items().badApple().view();
            if(movieStopping){
                if(getMovieCurrent()!=null)return false;
                SkyWars2.LOGGER.info("[check] badapple-24 metrics: fullServerTicks={} distinctCompletedFrames={} coalescedFrames={} nativeMedia=228x171 actualFps=10 liveBots=24 surfaceCacheNativeInvalidation=true",
                        metrics(movieTicks),movieFrames.size(),movieCoalesced);
                require(movieFrames.size()>=190&&movieCoalesced==0,"at least190 distinct full frames complete across the 20second window, without coalescing");
                return true;
            }
            if(!state.soundStarted())return false;
            if(state.logicalTicks()>4)movieTicks.add(previousTick());
            if(state.appliedFrame()>=0&&state.appliedFrame()!=completeFrame){completeFrame=state.appliedFrame();movieFrames.add(completeFrame);}
            movieCoalesced=state.coalesced();
            require(state.desiredFrame()-state.appliedFrame()<=1,"each full native movie frame is applied within its two-tick display interval: desired="+state.desiredFrame()+" applied="+state.appliedFrame()+" cursor="+state.columnCursor());
            if(state.logicalTicks()<400)return false;
            require(movieTicks.size()>=390,"twenty actual seconds of full native completed-server-tick samples");
            game.items().badApple().requestStop();movieStopping=true;return false;
        }
        Object getMovieCurrent(){
            try{var field=LbcBadApple.class.getDeclaredField("current");field.setAccessible(true);return field.get(game.items().badApple());}
            catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        }
        void startEruption(){
            require(game.items().ultraponos().pendingBlastCount()==0&&game.items().ultraponos().emitterCount()==0,"exclusive actual trap queue");
            game.config().lbcSkills=true;base.level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING,true,game.server());
            System.setProperty("skywars2.disableExplosionCache",eruptionTrial==0?"true":"false");
            var center=BlockPos.containing(origin);
            for(int x=-24;x<=75;x++)for(int z=-24;z<=37;z++)base.set(center.offset(x,-1,z),Blocks.STONE.defaultBlockState());
            for(int i=0;i<actors.size();i++){
                var actor=actors.get(i);var spot=origin.add((i%8)*10,0,(i/8)*12);
                // Stress-only HP keeps all24 alive through eight WILD hits; no invulnerability or skipped native damage.
                actor.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000);actor.setHealth(10000);actor.setAbsorptionAmount(0);
                actor.setInvulnerableTime(0);actor.damageCooldownTime=0;actor.setDeltaMovement(Vec3.ZERO);
                actor.teleportTo(base.level,spot.x,spot.y,spot.z,Set.of(),0,0,true);
                game.players().get(actor.getUUID()).lbcSkill=i==0?9:0;
            }
            var at=origin.add(35,0,12);var amplifier=actors.getFirst();amplifier.teleportTo(base.level,at.x,at.y,at.z,Set.of(),0,0,true);
            var token=new net.minecraft.world.entity.item.ItemEntity(base.level,at.x,at.y,at.z,
                    new net.minecraft.world.item.ItemStack(LbcUltraponosDropItem.Kind.NORMAL.item()));
            token.setNoGravity(true);token.setDeltaMovement(Vec3.ZERO);game.items().markCurrentItemEpoch(token);
            require(base.level.addFreshEntity(token)&&game.items().specialDrops().spawn(token),"real WILD item dispatch arms the native trap");
            require(game.items().ultraponos().pendingBlastCount()==8,"actual nearest skill9 player adds all four mega and four base bursts");
            var bolt=new net.minecraft.world.entity.LightningBolt(net.minecraft.world.entity.EntityTypes.LIGHTNING_BOLT,base.level);
            bolt.setPos(Vec3.atCenterOf(BlockPos.containing(at)));bolt.setSilent(true);bolt.setVisualOnly(false);
            require(base.level.addFreshEntity(bolt),"real charging lightning");bolt.tick();bolt.discard();
            require(game.items().ultraponos().pendingBlasts().stream().map(LbcUltraponos.BlastSnapshot::radius).toList()
                    .equals(List.of(24,28,32,36,12,14,16,18)),"native lightning really arms worst-case charged mega strengths");
            trialStarted=game.server().getTickCount();cachedAt=LbcExasperato.optimizedBlockSearches();burstObserved=-1;
            eruptionTicks.clear();burstTicks.clear();recent.clear();
        }
        boolean eruptionStep(int now,int live){
            require(live==24,"all24 real bot bodies remain live and damage-eligible throughout the actual eruption measurement");
            long completed=previousTick();recent.addLast(completed);while(recent.size()>3)recent.removeFirst();
            if(game.items().ultraponos().pendingBlastCount()>0){
                if(now-trialStarted>20)eruptionTicks.add(completed);return false;
            }
            if(burstObserved<0){
                burstObserved=now;burstTicks.addAll(recent);
                require(LbcExasperato.optimizedBlockSearches()-cachedAt==(eruptionTrial==0?0:8),"all eight bursts use only the selected original/shared collector");
            }else burstTicks.add(completed);
            if(now-burstObserved<3)return false;
            require(eruptionTicks.size()>=260&&burstTicks.size()>=6,"completed native server ticks include the whole burst boundary, not just its Java collector");
            SkyWars2.LOGGER.info("[check] ultraponos-24 metrics: collector={} liveBots=24 chargedMega=true countdown={} burstWindow={} fullServerTicks=true stressOnlyHp=10000",
                    eruptionTrial==0?"original":"shared-spherical",metrics(eruptionTicks),metrics(burstTicks));
            if(++eruptionTrial<2){startEruption();return false;}
            return true;
        }
        @Override void setup(){for(var actor:base.level.getAllEntities())preexisting.add(actor.getUUID());super.setup();disconnect(bot);actors.clear();
            if(base.level.getBlockEntity(firstChest) instanceof net.minecraft.world.Container old)old.clearContent();
            base.set(firstChest,Blocks.AIR.defaultBlockState());at=game.server().getTickCount();
        }
        void population(){
            var center=BlockPos.containing(origin);
            for(int x=(center.getX()-25)>>4;x<=(center.getX()+77)>>4;x++)for(int z=(center.getZ()-25)>>4;z<=(center.getZ()+38)>>4;z++){
                long key=ChunkPos.pack(x,z);if(!base.level.getForceLoadedChunks().contains(key)){forced.add(key);base.level.setChunkForced(x,z,true);}base.level.getChunk(x,z);
            }
            for(int x=-24;x<=75;x++)for(int z=-24;z<=37;z++)for(int y=-2;y<=4;y++)
                base.set(center.offset(x,y,z),y==-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
            var board=game.server().getScoreboard();require(board.getPlayerTeam("botBenchmark")==null,"owned temporary team name unused");
            team=board.addPlayerTeam("botBenchmark");
            for(int i=0;i<24;i++){
                var spot=origin.add((i%8)*10,0,(i/8)*12);
                var p=create(String.format(Locale.ROOT,"botBench%02d",i),spot);
                p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);p.setHealth(20);p.getFoodData().setFoodLevel(20);
                board.addPlayerToTeam(p.getScoreboardName(),team);start.put(p.getUUID(),spot);
                for(int j=0;j<3;j++){
                    var pos=BlockPos.containing(spot).offset(j==2?3:0,0,j==0?-2:2);starters.add(pos);base.set(pos,Blocks.CHEST.defaultBlockState());
                    var chest=(ChestBlockEntity)base.level.getBlockEntity(pos);chest.clearContent();
                    chest.setLootTable(MapManager.chestLootTable(2,"default",false));chest.setLootTableSeed(11000+i*3+j);
                    chest.getItem(0);MapManager.normalizeLootCounts(chest);
                    // A repeatable population measurement excludes random trap payloads;
                    // the separate full trap suites retain all ordinary hazards and 20 HP.
                    for(int slot=0;slot<chest.getContainerSize();slot++)if(chest.getItem(slot).getItem() instanceof net.minecraft.world.item.BlockItem block
                            &&LbcLuckyBlocks.kind(block.getBlock().defaultBlockState())!=null)chest.setItem(slot,net.minecraft.world.item.ItemStack.EMPTY);
                }
            }
            require(actors.size()==24&&actors.stream().allMatch(p->p.isAlive()&&game.allowDamage(p)&&LbcCarpetBots.bot(p)),"24 native live participants with ordinary HP");
            spawnedAt=game.server().getTickCount();
        }
        long previousTick(){int tick=game.server().getTickCount()-1;return game.server().getTickTimesNanos()[Math.floorMod(tick,100)];}
        @Override boolean step(){
            base.client.acknowledge();base.audience.acknowledge();int now=game.server().getTickCount();
            if(start.isEmpty()){
                if(now-at>20)baseline.add(previousTick());
                if(now-at>=100)population();return false;
            }
            int elapsed=now-spawnedAt;
            int live=0;
            for(var entry:start.entrySet()){
                var p=game.server().getPlayerList().getPlayer(entry.getKey());if(p==null||!p.isAlive()||!game.allowDamage(p))continue;live++;
                var observation=game.carpetBots().observation(p);
                if(p.position().distanceToSqr(entry.getValue())>.25||observation.looted()>0||observation.attempts()>0)acted.add(p.getUUID());
            }
            if(!released){
                require(live==24,"all 24 bots remain alive during the shared-team native looting phase");
                if(elapsed>100)looting.add(previousTick());
                if(elapsed<400)return false;
                require(acted.size()==24,"every one of the 24 scheduled bots actually moves, loots or attacks");
                if(movie){released=true;startMovie();return false;}
                if(eruption){released=true;startEruption();return false;}
                for(var actor:actors)game.server().getScoreboard().removePlayerFromTeam(actor.getScoreboardName());released=true;
                return false;
            }
            if(movie)return movieStep(now,live);
            if(eruption)return eruptionStep(now,live);
            minimumCombat=Math.min(minimumCombat,live);combat.add(previousTick());
            if(elapsed<600)return false;
            require(baseline.size()>=75&&looting.size()>=290&&combat.size()>=190,"full native tick sample windows, excluding population startup");
            SkyWars2.LOGGER.info("[check] bot-population-24 metrics: baseline={} looting24={} combat={} minimumLiveCombat={} startupExcluded=true fullServerTicks=true",
                    metrics(baseline),metrics(looting),metrics(combat),minimumCombat);
            return true;
        }
        @Override public void close(){
            if(movie)game.items().badApple().stop();
            if(eruption){
                game.items().ultraponos().stop();game.config().lbcSkills=oldSkills;
                base.level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING,oldGriefing,game.server());
                if(oldCache==null)System.clearProperty("skywars2.disableExplosionCache");else System.setProperty("skywars2.disableExplosionCache",oldCache);
            }
            for(var actor:base.level.getAllEntities())if(!(actor instanceof ServerPlayer)&&!preexisting.contains(actor.getUUID())&&actor.position().distanceToSqr(origin)<160*160)base.owned.add(actor.getUUID());
            Throwable failure=null;try{super.close();}catch(Throwable error){failure=error;}
            var board=game.server().getScoreboard();if(team!=null&&board.getPlayerTeam(team.getName())==team)board.removePlayerTeam(team);
            if(failure!=null)throw new IllegalStateException("Population cleanup",failure);
        }
    }
    private static Map<String,Object> metrics(List<Long> values){
        var sorted=values.stream().mapToLong(Long::longValue).sorted().toArray();
        return Map.of("samples",sorted.length,"meanMs",Arrays.stream(sorted).average().orElseThrow()/1e6,
                "p95Ms",sorted[(int)Math.floor((sorted.length-1)*.95)]/1e6,"p99Ms",sorted[(int)Math.floor((sorted.length-1)*.99)]/1e6,"maxMs",sorted[sorted.length-1]/1e6);
    }
    static void run(GameController game){run(game,false,false);}
    static void runUltraponos(GameController game){run(game,true,false);}
    static void runBadApple(GameController game){run(game,false,true);}
    private static void run(GameController game,boolean eruption,boolean movie){require(running==null,"exclusive fixture");running=new Fixture(game,eruption,movie);try{running.setup();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    static void tick(GameController game){
        if(running==null)return;
        String suite=running.movie?"bot-badapple-24":running.eruption?"bot-ultraponos-24":"bot-population-24";
        try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS {} (actual24 bot bodies/full server ticks/scoped cleanup)",suite);}
        catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL "+suite,error);}
    }
}
