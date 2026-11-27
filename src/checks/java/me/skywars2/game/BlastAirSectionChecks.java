package me.skywars2.game;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import me.skywars2.SkyWars2;
import me.skywars2.game.combat.BlastRayTrace;
import me.skywars2.game.match.checkpoints.MatchRandomState;
import me.skywars2.game.match.checkpoints.MatchTerrainCheckpoint;
import me.skywars2.game.world.maps.MapManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ScheduledTick;

/** Real section palettes/resistance/NBT. Only emptySection differs between old and fast views. */
public final class BlastAirSectionChecks {
    private static Fixture running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Blast air sections: "+why);}
    private enum Mode { NATIVE, FIXED, SPHERICAL }
    private enum Pattern { AIR, SPARSE, DISCONNECTED, DENSE }
    private record Trial(String name,Pattern pattern,Mode mode,Vec3 origin,float radius,long seed,int minY,int maxY){}
    private record Result(List<BlockPos> ordered,long[] tail,long states,long sections,long loss,long wall,long cpu){}
    private record Ticks(List<ScheduledTick<Block>> blocks,List<ScheduledTick<Fluid>> fluids){}
    private static final BlockPos ANCHOR=new BlockPos(-9,168,-9);
    private static final Vec3 CENTER=Vec3.atCenterOf(ANCHOR);
    private static <T> void orderTicks(List<ScheduledTick<T>> values){
        values.sort(Comparator.comparingLong((ScheduledTick<T> tick)->tick.pos().asLong())
                .thenComparingLong(ScheduledTick::triggerTick).thenComparingLong(ScheduledTick::subTickOrder)
                .thenComparing(tick->tick.priority().toString()));
    }

    private static final class View implements BlastRayTrace.Cells {
        final ServerLevel level;final Trial trial;final boolean fast;
        final ServerExplosion explosion;final ExplosionDamageCalculator calculator=new ExplosionDamageCalculator();
        final BlockPos.MutableBlockPos corners=new BlockPos.MutableBlockPos();
        final Map<Long,LevelChunk> chunks=new HashMap<>();
        long states,sections,losses;
        View(ServerLevel level,Trial trial,boolean fast){
            this.level=level;this.trial=trial;this.fast=fast;
            explosion=new ServerExplosion(level,null,level.damageSources().explosion(null,null),null,trial.origin,trial.radius,false,Explosion.BlockInteraction.DESTROY);
        }
        public boolean inside(BlockPos pos){return level.isInWorldBounds(pos);}
        public int minY(){return trial.minY;}
        public int maxY(){return trial.maxY;}
        LevelChunk chunk(BlockPos pos){
            long key=ChunkPos.pack(pos.getX()>>4,pos.getZ()>>4);
            if(!chunks.containsKey(key))chunks.put(key,level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4));
            return chunks.get(key);
        }
        public BlockState state(BlockPos pos){
            states++;var chunk=chunk(pos);if(chunk==null)return Blocks.AIR.defaultBlockState();
            var section=chunk.getSection(level.getSectionIndex(pos.getY()));
            return section.hasOnlyAir()?Blocks.AIR.defaultBlockState():section.getBlockState(pos.getX()&15,pos.getY()&15,pos.getZ()&15);
        }
        public boolean emptySection(BlockPos pos){
            if(!fast)return false;sections++;var chunk=chunk(pos);
            // Same resident-only contract as the live nuclear view: an unavailable chunk reads AIR.
            if(chunk!=null&&!chunk.getSection(level.getSectionIndex(pos.getY())).hasOnlyAir())return false;
            int x=pos.getX()&~15,y=pos.getY()&~15,z=pos.getZ()&~15;
            return level.isInWorldBounds(corners.set(x,y,z))&&level.isInWorldBounds(corners.set(x+15,y+15,z+15));
        }
        public float loss(BlockPos pos,BlockState state){
            losses++;var resistance=calculator.getBlockExplosionResistance(explosion,level,pos,state,state.getFluidState());
            return resistance.isEmpty()?0:(resistance.get()+.3F)*.3F;
        }
        public boolean destroy(BlockPos pos,BlockState state,float power){return calculator.shouldBlockExplode(explosion,level,pos,state,power);}
    }

    private static Result trace(ServerLevel level,Trial trial,boolean fast){
        var view=new View(level,trial,fast);var random=RandomSource.create(trial.seed);
        var clock=ManagementFactory.getThreadMXBean();boolean cpu=clock.isCurrentThreadCpuTimeSupported()&&clock.isThreadCpuTimeEnabled();
        long cpuAt=cpu?clock.getCurrentThreadCpuTime():0,at=System.nanoTime();
        var positions=trial.mode==Mode.SPHERICAL?BlastRayTrace.traceSpherical(trial.origin,trial.radius,random,view)
                :BlastRayTrace.trace(trial.origin,trial.radius,random,trial.mode==Mode.NATIVE,view);
        long elapsed=System.nanoTime()-at,used=cpu?clock.getCurrentThreadCpuTime()-cpuAt:-1;
        var tail=new long[8];for(int i=0;i<tail.length;i++)tail[i]=random.nextLong();
        // Independent RNG oracle: native/spherical consume exactly one nextFloat per ray; fixed consumes none.
        var expected=RandomSource.create(trial.seed);
        if(trial.mode!=Mode.FIXED)for(int i=0;i<BlastRayTrace.rayCount();i++)expected.nextFloat();
        for(long value:tail)require(value==expected.nextLong(),"exact RNG consumption "+trial.name);
        return new Result(positions,tail,view.states,view.sections,view.losses,elapsed,used);
    }

    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerLevel level;final boolean frozen;final int start;
        final BoundingBox bounds=new BoundingBox(-48,128,-48,31,207,31);
        final Map<BlockPos,MapManager.BlockSnapshot> original=new LinkedHashMap<>();
        final List<Trial> trials=new ArrayList<>();final List<LevelChunk> chunks=new ArrayList<>();
        final CompletableFuture<?> ready;final List<Long> oldCpu=new ArrayList<>(),fastCpu=new ArrayList<>(),oldWall=new ArrayList<>(),fastWall=new ArrayList<>();
        MatchRandomState random;Ticks originalTicks;Pattern pattern;Map<BlockPos,MapManager.BlockSnapshot> beforeTrace;
        int index,bench;boolean captured,closed;long oldAirReads,fastAirReads,comparisons;
        Fixture(GameController game){
            this.game=game;level=game.server().overworld();frozen=game.server().tickRateManager().isFrozen();start=game.server().getTickCount();
            require(game.playing()&&!frozen&&!game.maps().busy()&&!game.vanity().busy()&&game.items().stonekivi().count()==0,"stable isolated native terrain boundary");
            var futures=new ArrayList<CompletableFuture<?>>();
            for(int x=-3;x<=1;x++)for(int z=-3;z<=1;z++)futures.add(game.maps().prepareChunk(level,new ChunkPos(x,z)));
            ready=CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
            int min=level.getMinY(),max=level.getMaxY();
            for(var p:Pattern.values())for(var mode:Mode.values())trials.add(new Trial(p+"/"+mode,p,mode,CENTER,20,47+991L*p.ordinal()+917458L*mode.ordinal(),min,max));
            double epsilon=0x1.0p-28;
            trials.add(new Trial("grazing-negative-native",Pattern.SPARSE,Mode.NATIVE,new Vec3(-16-epsilon,160+epsilon,-16+epsilon),18,991,min,max));
            trials.add(new Trial("grazing-positive-fixed",Pattern.SPARSE,Mode.FIXED,new Vec3(-16+epsilon,160-epsilon,-16-epsilon),18,917458,min,max));
            trials.add(new Trial("grazing-exact-spherical",Pattern.SPARSE,Mode.SPHERICAL,new Vec3(-16,176,-16),18,20261127,min,max));
            trials.add(new Trial("native-floor-inside",Pattern.AIR,Mode.NATIVE,new Vec3(-.000001,min+.000001,-.000001),4,47,min,max));
            trials.add(new Trial("native-floor-outside",Pattern.AIR,Mode.FIXED,new Vec3(-.000001,min-.000001,-.000001),4,991,min,max));
            trials.add(new Trial("native-ceiling-inside",Pattern.AIR,Mode.SPHERICAL,new Vec3(-.000001,max+.999999,-.000001),4,917458,min,max));
            trials.add(new Trial("native-ceiling-outside",Pattern.AIR,Mode.NATIVE,new Vec3(-.000001,max+1.000001,-.000001),4,20261127,min,max));
            // World limits use native resident-only AIR for absent chunks; no far-world generation or writes.
            trials.add(new Trial("positive-world-limit",Pattern.AIR,Mode.NATIVE,new Vec3(29999999.999999,168,-.5),4,47,min,max));
            trials.add(new Trial("negative-world-limit",Pattern.AIR,Mode.SPHERICAL,new Vec3(-29999999.999999,168,-.5),4,991,min,max));
            trials.add(new Trial("occupied-envelope-below",Pattern.SPARSE,Mode.NATIVE,new Vec3(-8.5,159.999999,-8.5),12,917458,160,175));
            trials.add(new Trial("occupied-envelope-above",Pattern.SPARSE,Mode.FIXED,new Vec3(-8.5,176.000001,-8.5),12,20261127,160,175));
        }
        Ticks ticks(){
            var blocks=new ArrayList<ScheduledTick<Block>>();var fluids=new ArrayList<ScheduledTick<Fluid>>();
            for(var chunk:chunks){
                MatchTerrainCheckpoint.captureTicks((LevelChunkTicks<Block>)chunk.getBlockTicks(),level.getGameTime()).stream().filter(t->bounds.isInside(t.pos())).forEach(blocks::add);
                MatchTerrainCheckpoint.captureTicks((LevelChunkTicks<Fluid>)chunk.getFluidTicks(),level.getGameTime()).stream().filter(t->bounds.isInside(t.pos())).forEach(fluids::add);
            }
            orderTicks(blocks);orderTicks(fluids);return new Ticks(blocks,fluids);
        }
        void capture(){
            game.server().tickRateManager().setFrozen(true);random=MatchRandomState.capture(level.getRandom());
            require(level.getMinY()<=128&&level.getMaxY()>=207,"native mid-height fixture fits world");
            for(int x=-3;x<=1;x++)for(int z=-3;z<=1;z++){
                var chunk=level.getChunkSource().getChunkNow(x,z);require(chunk!=null,"async native section columns resident");chunks.add(chunk);
                for(int y=128;y<=207;y+=16)require(chunk.getSection(level.getSectionIndex(y)).hasOnlyAir(),"initial native fixture sections must be unborrowed AIR "+x+","+y+","+z);
            }
            originalTicks=ticks();captured=true;
        }
        void put(BlockPos pos,BlockState state){
            require(bounds.isInside(pos),"owned fixture terrain bounds");original.putIfAbsent(pos.immutable(),game.maps().snapshot(level,pos));
            if(level.getBlockEntity(pos) instanceof Container container)container.clearContent();
            level.setBlock(pos,state,2|16|32);
        }
        void material(BlockPos pos,int i){
            var block=switch(Math.floorMod(i,7)){case 0->Blocks.OBSIDIAN;case 1->Blocks.BEDROCK;case 2->Blocks.WATER;case 3->Blocks.LAVA;case 4->Blocks.GLASS;case 5->Blocks.OAK_PLANKS;default->Blocks.STONE;};
            put(pos,block.defaultBlockState());
        }
        void select(Pattern wanted){
            if(pattern==wanted)return;
            for(var entry:original.entrySet()){
                if(level.getBlockEntity(entry.getKey()) instanceof Container container)container.clearContent();
                game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue(),2|16|32);
            }
            pattern=wanted;
            if(wanted==Pattern.SPARSE){
                for(int i=0;i<14;i++)material(ANCHOR.offset((i%5-2)*3,i%3-1,(i/5-1)*4),i);
                var at=ANCHOR.offset(2,0,0);put(at,Blocks.CHEST.defaultBlockState());
                require(level.getBlockEntity(at) instanceof ChestBlockEntity,"actual native chest block entity");
                var item=new ItemStack(Items.DIAMOND,13);item.set(DataComponents.CUSTOM_NAME,Component.literal("Read-only blast fixture"));
                ((ChestBlockEntity)level.getBlockEntity(at)).setItem(4,item);
                put(ANCHOR.offset(0,0,-6),Blocks.WATER.defaultBlockState());put(ANCHOR.offset(0,0,6),Blocks.LAVA.defaultBlockState());
                require(!level.getBlockState(ANCHOR.offset(0,0,-6)).getFluidState().isEmpty()
                        &&!level.getBlockState(ANCHOR.offset(0,0,6)).getFluidState().isEmpty(),"actual native WATER/LAVA fluid palettes are present");
            }else if(wanted==Pattern.DISCONNECTED){
                for(int a=-2;a<=2;a++)for(int b=-2;b<=2;b++){
                    put(new BlockPos(17,168+a,-9+b),Blocks.STONE.defaultBlockState());
                    put(new BlockPos(-35,168+a,-9+b),Blocks.OAK_PLANKS.defaultBlockState());
                    put(new BlockPos(-9+a,168+b,17),Blocks.GLASS.defaultBlockState());
                    put(new BlockPos(-9+a,168+b,-35),Blocks.STONE.defaultBlockState());
                }
            }else if(wanted==Pattern.DENSE){
                int i=0;for(var pos:BlockPos.betweenClosed(ANCHOR.offset(-5,-5,-5),ANCHOR.offset(5,5,5)))material(pos.immutable(),i++);
                put(ANCHOR,Blocks.STONE.defaultBlockState());
            }
            beforeTrace=new LinkedHashMap<>();for(var pos:original.keySet())beforeTrace.put(pos,game.maps().snapshot(level,pos));
        }
        void unchanged(Ticks before){
            for(var entry:beforeTrace.entrySet())require(game.maps().snapshot(level,entry.getKey()).equals(entry.getValue()),"ray trace preserves actual block/container NBT at "+entry.getKey());
            require(ticks().equals(before),"ray trace performs no native block/fluid scheduling");
        }
        Result[] pair(Trial trial,boolean fastFirst){
            select(trial.pattern);var before=ticks();Result old,fast;
            if(fastFirst){fast=trace(level,trial,true);old=trace(level,trial,false);}else{old=trace(level,trial,false);fast=trace(level,trial,true);}
            require(old.ordered.equals(fast.ordered),"EXACT ordered positions "+trial.name+" old="+old.ordered.size()+" fast="+fast.ordered.size());
            require(new HashSet<>(old.ordered).equals(new HashSet<>(fast.ordered))&&new HashSet<>(old.ordered).size()==old.ordered.size(),"exact unique position set "+trial.name);
            require(Arrays.equals(old.tail,fast.tail),"RNG tail equality "+trial.name);
            for(var pos:fast.ordered)require(!level.getBlockState(pos).is(Blocks.BEDROCK),"native resistant bedrock is retained");
            if(trial.pattern==Pattern.DISCONNECTED)require(!old.ordered.isEmpty(),"disconnected actual block islands reached across whole AIR sections");
            unchanged(before);comparisons++;return new Result[]{old,fast};
        }
        boolean step(){
            require(game.server().getTickCount()-start<400,"bounded native scene");
            if(!captured){if(!ready.isDone())return false;ready.join();capture();return false;}
            if(index<trials.size()){
                var trial=trials.get(index++);var result=pair(trial,(index&1)==0);
                if(trial.pattern==Pattern.AIR&&trial.radius==20){oldAirReads+=result[0].states;fastAirReads+=result[1].states;}
                SkyWars2.LOGGER.info("[check] blast-air-sections exact {} positions={} stateReadsOldFast={}/{} wholeAirProbes={} lossReadsOldFast={}/{}",trial.name,result[0].ordered.size(),result[0].states,result[1].states,result[1].sections,result[0].loss,result[1].loss);
                return false;
            }
            // Alternate order every pair, with four unmeasured warm pairs before eight measured pairs.
            var measure=new Trial("warmed-disconnected-native",Pattern.DISCONNECTED,Mode.NATIVE,CENTER,20,917458,level.getMinY(),level.getMaxY());
            var result=pair(measure,(bench&1)!=0);
            if(bench++>=4){oldWall.add(result[0].wall);fastWall.add(result[1].wall);if(result[0].cpu>=0&&result[1].cpu>=0){oldCpu.add(result[0].cpu);fastCpu.add(result[1].cpu);}}
            if(bench<12)return false;
            require(oldAirReads>0&&fastAirReads*2<oldAirReads,"large reduction of actual state calls across three empty-section ray modes old="+oldAirReads+" fast="+fastAirReads);
            long oldNs=median(oldCpu),fastNs=median(fastCpu);
            SkyWars2.LOGGER.info("[check] blast-air-sections warmed alternating8 pairs DISCONNECTED native radius20 medianCpuMsOldFast={}/{} medianWallMsOldFast={}/{} measuredCpuFaster={} (raw observation; no speedup claim if false/unavailable)",
                    oldNs/1e6,fastNs/1e6,median(oldWall)/1e6,median(fastWall)/1e6,oldNs>=0&&fastNs>=0&&fastNs<oldNs);
            return true;
        }
        @Override public void close(){
            if(closed)return;closed=true;
            try{
                if(captured){
                    game.server().tickRateManager().setFrozen(true);
                    for(var entry:original.entrySet()){
                        if(level.getBlockEntity(entry.getKey()) instanceof Container container)container.clearContent();
                        game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue(),2|16|32);
                    }
                    level.getBlockTicks().clearArea(bounds);level.getFluidTicks().clearArea(bounds);
                    originalTicks.blocks.forEach(level.getBlockTicks()::schedule);originalTicks.fluids.forEach(level.getFluidTicks()::schedule);random.restore(level.getRandom());
                    for(var entry:original.entrySet())require(game.maps().snapshot(level,entry.getKey()).equals(entry.getValue()),"exact fixture terrain restoration");
                    require(ticks().equals(originalTicks),"exact fixture scheduler restoration");
                }
            }finally{game.server().tickRateManager().setFrozen(frozen);}
        }
    }
    private static long median(List<Long> values){if(values.isEmpty())return -1;var copy=new ArrayList<>(values);copy.sort(Long::compare);return copy.get(copy.size()/2);}
    public static void run(GameController game){require(running==null,"exclusive scene");running=new Fixture(game);SkyWars2.LOGGER.info("[check] START blast-air-sections");}
    public static void tick(GameController game){
        if(running==null)return;var fixture=running;
        try{if(!fixture.step())return;fixture.close();running=null;SkyWars2.LOGGER.info("[check] PASS blast-air-sections ({} exact ordered/set/RNG pairs; real native sparse/disconnected/dense/liquid/resistant/chest sections; grazing/negative/height/world bounds; AIR stateReads {}/{}; restored native terrain/NBT/ticks/RNG/freeze)",fixture.comparisons,fixture.oldAirReads,fixture.fastAirReads);}
        catch(Throwable error){try{fixture.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL blast-air-sections",error);}
    }
}
