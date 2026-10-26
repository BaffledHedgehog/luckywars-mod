package me.skywars2.game;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.PortalShape;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.WeakHashMap;

/** Shared terrain time budget: nearby results first, with fair progress for every active source. */
final class EffectTerrain {
    // The user accepts brief 18-19 TPS: use current tick headroom rather than delaying for seconds.
    // Leave time for the remaining end-tick hooks; this is a soft deadline, not an interruptible write.
    private static final long TICK_TARGET_NANOS=54_000_000,TAIL_RESERVE_NANOS=2_000_000;
    private static final int MAX_VISITS=1_048_576,QUANTUM=1024,REFRESH_TICKS=5;
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final int[][] SPHERES=new int[16][];
    private static final Map<LivingEntity,Field> fires=new WeakHashMap<>(),waters=new WeakHashMap<>();
    private static final ArrayDeque<Job> pending=new ArrayDeque<>();
    private static long ticks,visited,writes,tickNanos,tickStarted;

    private EffectTerrain() { }
    static long visited() { return visited; }
    static long writes() { return writes; }
    static long tickNanos() { return tickNanos; }
    static int queued() { return pending.size(); }
    static void beginTick() { tickStarted=System.nanoTime(); }

    static void fire(LivingEntity source,int radius) { field(source,radius,false); }
    static void water(LivingEntity source,int radius) { field(source,radius,true); }
    static void removeFire(LivingEntity source) {
        var old=fires.remove(source);if(old!=null)pending.remove(old);
    }
    static void cancel(LivingEntity source) {
        fires.remove(source);waters.remove(source);pending.removeIf(job->job.source()==source);
    }
    static void clear() {
        fires.clear();waters.clear();pending.clear();ticks=visited=writes=tickNanos=0;
    }
    private static void field(LivingEntity source,int radius,boolean water) {
        var fields=water?waters:fires;
        var job=fields.get(source);
        if(job==null||job.level!=source.level()||job.radius!=radius) {
            if(job!=null)pending.remove(job);
            job=new Field(source,radius,water);fields.put(source,job);
        }
        if(!job.queued&&(!job.center.equals(source.blockPosition())||ticks-job.finished>=REFRESH_TICKS)) {
            job.center=source.blockPosition();job.cursor=0;job.queued=true;pending.addLast(job);
        }
    }
    static void weave(ServerLevel level,LivingEntity source,long amount) {
        pending.addLast(new Web(level,source,amount));
    }
    static void tick() {
        long started=System.nanoTime();ticks++;
        long deadline=(tickStarted==0?started:tickStarted)+TICK_TARGET_NANOS-TAIL_RESERVE_NANOS;
        var budget=new Budget(deadline);
        int paused=0;
        while(!pending.isEmpty()&&budget.available()) {
            var job=pending.removeFirst();var source=job.source();
            if(source==null||!job.valid(source)) { job.retire(source);continue; }
            if(job.blocked(source)) {
                pending.addLast(job);
                if(++paused>=pending.size())break;
                continue;
            }
            paused=0;
            job.step(source,budget);
            if(job.done())job.finished(source);else pending.addLast(job);
        }
        tickNanos=System.nanoTime()-started;
    }
    private static final class Budget {
        final long deadline;int remaining=MAX_VISITS;
        Budget(long deadline) { this.deadline=deadline; }
        boolean available() { return remaining>0&&System.nanoTime()<deadline; }
        boolean visit(int inQuantum) {
            if(remaining<=0||((inQuantum&15)==0&&System.nanoTime()>=deadline))return false;
            remaining--;visited++;return true;
        }
    }
    private interface Job {
        LivingEntity source();
        boolean valid(LivingEntity source);
        default boolean blocked(LivingEntity source) { return SignedEnvironmentalEffects.terrainPaused(source); }
        void step(LivingEntity source,Budget budget);
        boolean done();
        default void retire(LivingEntity source) { }
        default void finished(LivingEntity source) { }
    }
    private static final class Field implements Job {
        final WeakReference<LivingEntity> source;final ServerLevel level;final int radius;
        final boolean water;final int[] offsets;
        BlockPos center;int cursor;boolean queued;long finished=-REFRESH_TICKS;
        Field(LivingEntity source,int radius,boolean water) {
            this.source=new WeakReference<>(source);level=(ServerLevel)source.level();this.radius=radius;
            this.water=water;center=source.blockPosition();offsets=sphere(radius);
        }
        public LivingEntity source() { return source.get(); }
        public boolean valid(LivingEntity source) {
            return !source.isRemoved()&&source.isAlive()&&source.level()==level
                    &&SignedEnvironmentalEffects.level(source,water?MobEffects.CONDUIT_POWER:MobEffects.FIRE_RESISTANCE)>3;
        }
        public boolean blocked(LivingEntity source) { return SignedEnvironmentalEffects.protectedTarget(source); }
        public void retire(LivingEntity source) {
            var fields=water?waters:fires;
            if(source!=null&&fields.get(source)==this)fields.remove(source);
            queued=false;
        }
        public void step(LivingEntity source,Budget budget) {
            if(SignedEnvironmentalEffects.protectedTarget(source))return;
            // Keep the unfinished scan when moving; skip its old fringe, then follow the latest center.
            // Repeated movement must not restart the first few offsets forever.
            var latest=source.blockPosition();
            Runnable changes=()->{
                for(int n=0;n<QUANTUM&&cursor<offsets.length&&budget.visit(n);n++) {
                    int packed=offsets[cursor++];var pos=center.offset(x(packed),y(packed),z(packed));
                    if(pos.distSqr(latest)>radius*radius||!level.hasChunkAt(pos))continue;
                    if(water)waterCell(level,pos);else fireCell(level,pos);
                }
            };
            var game=SkyWars2.game();
            if(water&&game!=null)game.items().indirectKills().effectWorldChanges(source,MobEffects.CONDUIT_POWER,changes);
            else changes.run();
        }
        public boolean done() { return cursor==offsets.length; }
        public void finished(LivingEntity source) {
            queued=false;finished=ticks;
            if(!center.equals(source.blockPosition())) {
                center=source.blockPosition();cursor=0;queued=true;pending.addLast(this);
            }
        }
    }
    static void fireCell(ServerLevel level,BlockPos pos) {
        if(!level.hasChunkAt(pos)||!level.getBlockState(pos).isAir())return;
        var fire=Blocks.FIRE.defaultBlockState();
        // Avoid a place/remove pair for unsupported fire, retaining native portal ignition first.
        boolean place=fire.canSurvive(level,pos)||((level.dimension()==Level.OVERWORLD||level.dimension()==Level.NETHER)
                &&PortalShape.findEmptyPortalShape(level,pos,Direction.Axis.X).isPresent());
        if(place&&level.setBlock(pos,fire,Block.UPDATE_CLIENTS))writes++;
    }
    static void waterCell(ServerLevel level,BlockPos pos) {
        if(!level.hasChunkAt(pos))return;var old=level.getBlockState(pos);
        if(old.is(AIRS)&&level.setBlock(pos,Blocks.WATER.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS)) {
            writes++;level.updateNeighboursOnBlockSet(pos,old);
        }
    }
    private static final class Web implements Job {
        final ServerLevel level;final LivingEntity source;final BlockPos center;final int radius;
        final boolean negative;long remaining;int shell,index;
        Web(ServerLevel level,LivingEntity source,long amount) {
            this.level=level;this.source=source;center=source.blockPosition();
            radius=Math.min(50,(int)Math.ceil(Math.sqrt(Math.abs(amount))));negative=amount<0;remaining=2*Math.abs(amount);
        }
        public LivingEntity source() { return source; }
        public boolean valid(LivingEntity source) {
            var game=SkyWars2.game();return game==null||game.playing()&&level==game.maps().level();
        }
        public void step(LivingEntity source,Budget budget) {
            for(int n=0;n<QUANTUM&&!done()&&budget.visit(n);n++) {
                int packed=next();var pos=center.offset(x(packed),y(packed),z(packed));
                if(!level.hasChunkAt(pos))continue;var state=level.getBlockState(pos);
                if(negative?state.is(Blocks.COBWEB):state.canBeReplaced()&&level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP)) {
                    if(level.setBlockAndUpdate(pos,(negative?Blocks.AIR:Blocks.COBWEB).defaultBlockState())) { remaining--;writes++; }
                }
            }
        }
        public boolean done() { return shell>radius||remaining==0; }
        private int next() {
            if(shell==0) { shell=1;return pack(0,0,0); }
            int s=shell,side=2*s+1,inner=2*s-1,face=side*side,c=index,xx,yy,zz;
            if(c<2*face) {
                zz=c<face?-s:s;c%=face;xx=c%side-s;yy=c/side-s;
            } else {
                c-=2*face;face=side*inner;
                if(c<2*face) { xx=c<face?-s:s;c%=face;zz=c%inner-s+1;yy=c/inner-s; }
                else { c-=2*face;face=inner*inner;yy=c<face?-s:s;c%=face;xx=c%inner-s+1;zz=c/inner-s+1; }
            }
            if(++index==24*s*s+2) { shell++;index=0; }
            return pack(xx,yy,zz);
        }
    }
    private static int[] sphere(int radius) {
        var cached=SPHERES[radius];if(cached!=null)return cached;
        var shells=new IntArrayList[radius+1];for(int i=0;i<shells.length;i++)shells[i]=new IntArrayList();
        for(int x=-radius;x<=radius;x++)for(int y=-radius;y<=radius;y++)for(int z=-radius;z<=radius;z++)
            if(x*x+y*y+z*z<=radius*radius)shells[Math.max(Math.abs(x),Math.max(Math.abs(y),Math.abs(z)))].add(pack(x,y,z));
        var all=new IntArrayList();for(var shell:shells)all.addAll(shell);
        return SPHERES[radius]=all.toIntArray();
    }
    private static int pack(int x,int y,int z) { return x+64|((y+64)<<7)|((z+64)<<14); }
    private static int x(int packed) { return (packed&127)-64; }
    private static int y(int packed) { return (packed>>7&127)-64; }
    private static int z(int packed) { return (packed>>14&127)-64; }
}
