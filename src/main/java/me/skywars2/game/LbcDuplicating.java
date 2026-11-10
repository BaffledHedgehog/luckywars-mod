package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.List;
import java.util.IdentityHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

/** Duplicating хранит лёгкие центры; все чтения и записи clone идут через общую очередь Stonekivi. */
public final class LbcDuplicating {
    private static final int FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_SUPPRESS_DROPS|Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;
    private final GameController game;
    private final List<MarkerState> markers=new ArrayList<>();
    private final java.util.Set<Job> jobs=new java.util.LinkedHashSet<>();
    private final java.util.Map<net.minecraft.world.level.block.state.BlockState,MapManager.BlockSnapshot> plainSnapshots=new IdentityHashMap<>();
    private IdentityHashMap<ServerPlayer,Integer> dividerTimers=new IdentityHashMap<>();
    private boolean checkpointDraining;
    private int checkpointCallbacks;
    private static final double HOLE_INFLUENCE=16,HOLE_STRONG=7,HOLE_DAMAGE=4,HOLE_DESTRUCTION=7;
    private static final float HOLE_SHADER_RADIUS=.5f;
    private static class Owned {
        ServerPlayer owner; final ServerLevel level; final String epoch;
        boolean cancelled;
        Owned(ServerPlayer owner,String epoch) { this.owner=owner; level=owner.level(); this.epoch=epoch; }
    }
    private static final class MarkerState extends Owned {
        Vec3 point,direction; final int mode,lifetime; int age,cloneMask;
        BlackHoleVisuals.Handle visual;
        HoleTerrain terrain;
        LiveShuffle shuffle;
        Long erosionSeed;
        boolean retainedOwner;
        MarkerState(ServerPlayer owner,String epoch,Vec3 point,Vec3 direction,int mode,int lifetime) {
            super(owner,epoch); this.point=point; this.direction=direction; this.mode=mode; this.lifetime=lifetime;
        }
    }
    private static final class Seed extends Owned {
        // One operation batch's parameters; never registered as a moving center or entity.
        Vec3 point; final Vec3 center; final float pitch,yaw; final MarkerState parent; final boolean expand;
        Seed(MarkerState parent,Vec3 point,float pitch,float yaw) {
            super(parent.owner,parent.epoch); this.parent=parent; this.center=parent.point; this.point=point; this.pitch=pitch; this.yaw=yaw; expand=parent.mode==2;
        }
    }
    LbcDuplicating(GameController game) { this.game=game; }
    public void prepare() { }
    public boolean ready() { return true; }
    private boolean valid(ServerPlayer owner) {
        return owner!=null && owner.isAlive() && !owner.isRemoved() && owner.level()==game.maps().level()
                && game.canUseGameplayItems(owner) && !game.isObserver(owner) && game.stage()!=GameController.Stage.ENDING;
    }
    private boolean valid(Owned state) {
        return !state.cancelled && valid(state.owner) && state.owner.level()==state.level && game.items().currentEpoch(state.epoch);
    }
    private boolean paused(ServerPlayer owner) {
        return game.items().wands().timeStopped() || owner.entityTags().contains("time_stopped");
    }
    private boolean castable(ServerPlayer owner) {
        return valid(owner) && !paused(owner) && !game.items().frost().isFrozen(owner)
                && !game.items().magic().suppressed(owner) && !game.items().magic().suppressedAt(owner.level(),owner.getEyePosition());
    }
    public boolean use(ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean upgraded) {
        if(!castable(owner) || !ready()) return false;
        int mode=upgraded?Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("mode",1),1,4):1;
        if(upgraded && owner.isShiftKeyDown()) {
            mode=LbcModeScroll.next(mode,4); var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            tag.putInt("mode",mode); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
            owner.sendSystemMessage(Component.translatable(switch(mode) {
                case 2->"mode_expand"; case 3->"mode_divide"; case 4->"mode_black_hole"; default->"mode_default";
            })); return true;
        }
        int price=game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("dublicating_wand_mana_use"),"lbc.math").get();
        if(!game.items().mana().trySpendWand(owner,price)) return false;
        if(!upgraded && owner.isShiftKeyDown() && LbcAcademy.trial(stack)) {
            trial(owner); game.items().academy().ready(owner,hand,stack);
        } else castMode(owner,mode);
        game.items().wands().wandCasted(owner); return true;
    }

    void castMode(ServerPlayer owner,int mode) {
        enterCheckpointCallback();try {
            if(mode==3) queue(new Divide(owner));
            else {
                Vec3 direction=owner.getLookAngle().normalize();
                var marker=new MarkerState(owner,game.items().epoch(),owner.getEyePosition().add(direction.scale(mode==2?14:mode==4?8:7)),
                        direction,mode,mode==2?100:mode==4?300:200);
                if(mode==4) marker.visual=game.items().blackHoleVisuals().create(owner,marker.point,HOLE_SHADER_RADIUS);
                markers.add(marker);
            }
        }finally{leaveCheckpointCallback();}
    }
    void emitAt(ServerPlayer owner,int mode,Vec3 origin,Vec3 direction){
        enterCheckpointCallback();try {
            direction=direction.normalize();
            if(mode==3)queue(new Divide(owner,origin,direction));
            else{
                var marker=new MarkerState(owner,game.items().epoch(),origin.add(direction.scale(mode==2?14:mode==4?8:7)),
                        direction,mode,mode==2?100:mode==4?300:200);
                if(mode==4)marker.visual=game.items().blackHoleVisuals().create(owner,marker.point,HOLE_SHADER_RADIUS);
                markers.add(marker);
            }
        }finally{leaveCheckpointCallback();}
    }
    public boolean useDivider(ServerPlayer owner,ItemStack stack) {
        enterCheckpointCallback();try {
            if(!castable(owner) || game.items().mana().score(owner,"divider_cd").get()>0) return false;
            var job=new Job(owner); grid(job,owner.position(),12,true); queue(job);
            game.items().mana().score(owner,"divider_cd").set(30);
            dividerTimers.put(owner,0);
            for(float pitch:new float[]{0,.2f,.5f,.13f}) LbcOverload.sound(owner,"minecraft:block.anvil.use",4,pitch,0);
            return true;
        }finally{leaveCheckpointCallback();}
    }
    public boolean useDivider(ServerPlayer owner,InteractionHand hand,ItemStack stack) { return useDivider(owner,stack); }
    private void trial(ServerPlayer owner) {
        enterCheckpointCallback();try {
            markers.add(new MarkerState(owner,game.items().epoch(),owner.position(),new Vec3(0,-1,0),1,200));
            for(int index=0;index<90;index++) {
                Vec3 point=new Vec3(Math.floor(owner.getX())+owner.getRandom().nextInt(61)-30,owner.getY(),
                        Math.floor(owner.getZ())+owner.getRandom().nextInt(61)-30);
                markers.add(new MarkerState(owner,game.items().epoch(),point,randomDirection(owner),1,30));
            }
        }finally{leaveCheckpointCallback();}
    }
    private static float randomYaw(ServerPlayer owner) { return (owner.getRandom().nextInt(360001)-180000)*.001f; }
    private static float randomPitch(ServerPlayer owner) { return (owner.getRandom().nextInt(180001)-90000)*.001f; }
    private static Vec3 randomDirection(ServerPlayer owner) { return Vec3.directionFromRotation(randomPitch(owner),randomYaw(owner)); }
    /** Native ^ basis: local координаты задают углы AABB, блоки внутри не вращаются. */
    private static Vec3 local(Vec3 origin,float pitch,float yaw,double left,double up,double forward) {
        Vec3 front=Vec3.directionFromRotation(pitch,yaw),top=Vec3.directionFromRotation(pitch-90,yaw);
        Vec3 side=front.cross(top).scale(-1);
        return origin.add(side.scale(left)).add(top.scale(up)).add(front.scale(forward));
    }
    private static float pitch(Vec3 direction) { return (float)-Math.toDegrees(Math.asin(direction.y)); }
    private static float yaw(Vec3 direction) { return (float)Math.toDegrees(Math.atan2(-direction.x,direction.z)); }
    /** A FULL ticket is not a loaded chunk: getBlockState after hasChunkAt can still block on generation. */
    private static boolean resident(ServerLevel level,BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;
    }
    private void queue(Job job) { jobs.add(job); game.items().stonekivi().queuedWork(job); }
    private interface Operation { boolean step(Job job,long deadline,int[] budget); }
    private static final class SharedCapture { MapManager.BlockSnapshot[] blocks; }
    enum Shape { CUBOID,SLAB,LINE,SPHERE }
    enum Action { COPY,MOVE,DELETE,ROTATE,MIRROR }
    private class Job extends Owned implements LbcStonekivi.Work {
        final ArrayDeque<Operation> operations=new ArrayDeque<>();
        final java.util.Map<ServerLevel,java.util.Map<ChunkPos,CompletableFuture<?>>> loading=new IdentityHashMap<>();
        boolean held;
        Job(ServerPlayer owner) { super(owner,game.items().epoch()); }
        public boolean step() {
            enterCheckpointCallback();try {
                if(!valid(this)) { cancel(); return true; }
                if(paused(owner)) return false;
                long deadline=System.nanoTime()+500_000; int[] budget={4096};
                while(!operations.isEmpty() && budget[0]>0 && System.nanoTime()<deadline) {
                    if(!operations.peek().step(this,deadline,budget)) return false;
                    operations.remove();
                }
                if(operations.isEmpty() && !held) { jobs.remove(this); return true; } return false;
            }finally{leaveCheckpointCallback();}
        }
        @Override public boolean snapshotIdle(){return checkpointDraining&&checkpointWrites(this)==0;}
        public boolean ready() { return !held || !operations.isEmpty() || !valid(this); }
        public void cancel() {
            enterCheckpointCallback();try {cancelled=true;operations.clear();loading.clear();jobs.remove(this);
            }finally{leaveCheckpointCallback();}
        }
        @Override public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            checkpointBoundary();
            if(!checkpointDraining||checkpointWrites(this)!=0)
                throw new IllegalStateException("Finish original Duplicating writes before capture");
            return links->()->{
                cancelled=true;held=false;operations.clear();loading.clear();jobs.remove(this);
                if(this instanceof LiveShuffle shuffle){shuffle.latest=null;if(shuffle.marker.shuffle==shuffle)shuffle.marker.shuffle=null;}
                if(this instanceof HoleTerrain terrain){terrain.erosion.clear();if(terrain.marker.terrain==terrain)terrain.marker.terrain=null;}
            };
        }
        int sourceReady(ServerLevel source,BlockPos pos) {
            if(resident(source,pos)) return 1;
            var pending=loading.computeIfAbsent(source,ignored->new java.util.HashMap<>())
                    .computeIfAbsent(new ChunkPos(pos.getX()>>4,pos.getZ()>>4),chunk->game.maps().prepareChunk(source,chunk));
            return pending.isCompletedExceptionally()?-1:0;
        }
    }
    /** Одна случайная фигура: сперва полный снимок, затем удаление/перенос/преобразование без потери содержимого. */
    private final class Patch implements Operation {
        final BlockPos source,target; final int sx,sy,sz,volume; final Shape shape; final Action action;
        final Rotation rotation; final Mirror mirror; final boolean rejected;
        final MapManager.BlockSnapshot[] captured; int read,destinationReady,clear,write;
        Patch(ServerLevel level,Vec3 source,Vec3 target,int sx,int sy,int sz,Shape shape,Action action,Rotation rotation,Mirror mirror) {
            this.source=BlockPos.containing(source); this.target=BlockPos.containing(target);
            this.sx=sx; this.sy=sy; this.sz=sz; volume=sx*sy*sz; this.shape=shape; this.action=action;
            this.rotation=rotation; this.mirror=mirror; captured=new MapManager.BlockSnapshot[volume];
            rejected=sx<1 || sy<1 || sz<1 || sx>7 || sy>7 || sz>7 || this.source.getY()<level.getMinY()
                    || this.source.getY()+sy-1>level.getMaxY() || action!=Action.DELETE && (this.target.getY()<level.getMinY() || this.target.getY()+sy-1>level.getMaxY());
        }
        boolean selected(int index) {
            if(shape!=Shape.SPHERE) return true;
            double x=(index%sx-(sx-1)*.5)/(sx*.5),y=(index/(sx*sz)-(sy-1)*.5)/(sy*.5),z=((index/sx)%sz-(sz-1)*.5)/(sz*.5);
            return x*x+y*y+z*z<=1;
        }
        BlockPos sourceCell(int index) { return source.offset(index%sx,index/(sx*sz),(index/sx)%sz); }
        BlockPos destinationCell(int index) {
            int x=index%sx,y=index/(sx*sz),z=(index/sx)%sz;
            if(action==Action.ROTATE) switch(rotation) {
                case CLOCKWISE_90->{ return target.offset(sz-1-z,y,x); }
                case CLOCKWISE_180->{ return target.offset(sx-1-x,y,sz-1-z); }
                case COUNTERCLOCKWISE_90->{ return target.offset(z,y,sx-1-x); }
                default->{ }
            }
            if(action==Action.MIRROR) {
                if(mirror==Mirror.FRONT_BACK) x=sx-1-x;
                else if(mirror==Mirror.LEFT_RIGHT) z=sz-1-z;
            }
            return target.offset(x,y,z);
        }
        public boolean step(Job job,long deadline,int[] budget) {
            if(rejected) return true;
            while(read<volume && budget[0]>0 && System.nanoTime()<deadline) {
                BlockPos pos=sourceCell(read); if(!resident(job.level,pos)) return false;
                if(selected(read)) captured[read]=game.maps().snapshot(job.level,pos);
                read++; budget[0]--;
            }
            if(read<volume) return false;
            // Перенос не удаляет оригиналы, пока вся выбранная область назначения не доступна.
            if(action!=Action.DELETE) {
                while(destinationReady<volume && budget[0]>0 && System.nanoTime()<deadline) {
                    if(captured[destinationReady]!=null && !resident(job.level,destinationCell(destinationReady))) {
                        destinationReady=0; return false;
                    }
                    destinationReady++; budget[0]--;
                }
                if(destinationReady<volume) return false;
            }
            game.items().indirectKills().trapChanges(job.level,job.owner,()-> {
                if(action!=Action.COPY) while(clear<volume && budget[0]>0 && System.nanoTime()<deadline) {
                    BlockPos pos=sourceCell(clear); if(!resident(job.level,pos)) return;
                    if(action!=Action.DELETE && captured[clear]!=null && !resident(job.level,destinationCell(clear))) {
                        destinationReady=0; return;
                    }
                    if(captured[clear]!=null) {
                        game.items().indirectKills().supportRemoved(job.level,job.owner,pos);
                        job.level.setBlock(pos,Blocks.AIR.defaultBlockState(),FLAGS);
                    }
                    clear++; budget[0]--;
                }
                if((action!=Action.COPY && clear<volume) || action==Action.DELETE) return;
                while(write<volume && budget[0]>0 && System.nanoTime()<deadline) {
                    BlockPos pos=destinationCell(write); if(!resident(job.level,pos)) return;
                    var snapshot=captured[write];
                    if(snapshot!=null) {
                        var state=snapshot.state();
                        if(action==Action.ROTATE) state=state.rotate(rotation);
                        else if(action==Action.MIRROR) state=state.mirror(mirror);
                        game.items().indirectKills().supportRemoved(job.level,job.owner,pos);
                        game.maps().restoreSnapshot(job.level,pos,new MapManager.BlockSnapshot(state,snapshot.entity()));
                    }
                    write++; budget[0]--;
                }
            });
            return action==Action.DELETE?clear==volume:write==volume;
        }
    }
    void applyPatch(ServerPlayer owner,Vec3 source,Vec3 target,int sx,int sy,int sz,Shape shape,Action action,Rotation rotation,Mirror mirror) {
        enterCheckpointCallback();try {
            var job=new Job(owner); job.operations.add(new Patch(owner.level(),source,target,sx,sy,sz,shape,action,rotation,mirror)); queue(job);
        }finally{leaveCheckpointCallback();}
    }
    private MapManager.BlockSnapshot capture(ServerLevel level,BlockPos pos) {
        var state=level.getBlockState(pos);
        return state.hasBlockEntity()?game.maps().snapshot(level,pos)
                :plainSnapshots.computeIfAbsent(state,value->new MapManager.BlockSnapshot(value,null));
    }
    private boolean unchanged(ServerLevel level,BlockPos pos,MapManager.BlockSnapshot snapshot) {
        return snapshot.entity()==null && !snapshot.state().hasBlockEntity() && level.getBlockState(pos)==snapshot.state();
    }
    /** Moving centers keep one current batch and one replaceable latest request, never a queue of old positions. */
    private final class LiveShuffle extends Job {
        final MarkerState marker; Seed latest;
        LiveShuffle(MarkerState marker) { super(marker.owner); this.marker=marker; held=true; }
        void offer(Seed seed) { latest=seed; }
        @Override public boolean ready() {
            return !valid(this) || !valid(marker) || !operations.isEmpty() || latest!=null || marker.age>=marker.lifetime;
        }
        @Override public boolean step() {
            enterCheckpointCallback();try {
                if(cancelled||!valid(marker)) { cancel(); return true; }
                if(paused(owner)) return false;
                if(operations.isEmpty() && latest!=null) {
                    var seed=latest; latest=null;
                    if(!game.items().magic().suppressedAt(level,seed.point)) populate(this,seed);
                }
                held=marker.age<marker.lifetime || latest!=null;
                return super.step();
            }finally{leaveCheckpointCallback();}
        }
        @Override public void cancel() {
            enterCheckpointCallback();try {latest=null;super.cancel();
            }finally{leaveCheckpointCallback();}
        }
    }
    private final class Clone implements Operation {
        final ServerLevel source,destination; final BlockPos min,target; final int sx,sy,sz,volume;
        MapManager.BlockSnapshot[] captured; int read,write; final boolean rejected; boolean aborted;
        final SharedCapture shared;
        Clone(ServerLevel source,Vec3 first,Vec3 last,ServerLevel destination,Vec3 target) {
            this(source,first,last,destination,target,null);
        }
        Clone(ServerLevel source,Vec3 first,Vec3 last,ServerLevel destination,Vec3 target,SharedCapture shared) {
            this.source=source; this.destination=destination;
            this.shared=shared;
            var a=BlockPos.containing(first); var b=BlockPos.containing(last);
            min=new BlockPos(Math.min(a.getX(),b.getX()),Math.min(a.getY(),b.getY()),Math.min(a.getZ(),b.getZ()));
            sx=Math.abs(a.getX()-b.getX())+1; sy=Math.abs(a.getY()-b.getY())+1; sz=Math.abs(a.getZ()-b.getZ())+1;
            this.target=BlockPos.containing(target); volume=sx*sy*sz;
            boolean overlap=source==destination && min.getX()<this.target.getX()+sx && min.getX()+sx>this.target.getX()
                    && min.getY()<this.target.getY()+sy && min.getY()+sy>this.target.getY()
                    && min.getZ()<this.target.getZ()+sz && min.getZ()+sz>this.target.getZ();
            rejected=overlap || volume>32768 || min.getY()<source.getMinY() || min.getY()+sy-1>source.getMaxY()
                    || this.target.getY()<destination.getMinY() || this.target.getY()+sy-1>destination.getMaxY();
        }
        private BlockPos cell(BlockPos base,int index) { return base.offset(index%sx,index/(sx*sz),(index/sx)%sz); }
        public boolean step(Job job,long deadline,int[] budget) {
            if(rejected) return true;
            if(captured==null && shared!=null && shared.blocks!=null) { captured=shared.blocks; read=volume; }
            if(captured==null) captured=new MapManager.BlockSnapshot[volume];
            while(read<volume && budget[0]>0 && System.nanoTime()<deadline) {
                BlockPos pos=cell(min,read);
                if(!resident(source,pos)) {
                    if(source==job.level) return job instanceof LiveShuffle;
                    if(job.sourceReady(source,pos)<0) return true;
                    return false;
                }
                captured[read++]=capture(source,pos); budget[0]--;
            }
            if(read<volume) return false;
            if(shared!=null) shared.blocks=captured;
            // Один trap frame на ограниченную порцию, а не на каждую клетку.
            game.items().indirectKills().trapChanges(destination,job.owner,()-> {
                while(write<volume && budget[0]>0 && System.nanoTime()<deadline) {
                    BlockPos pos=cell(target,write);
                    if(!resident(destination,pos)) { if(job instanceof LiveShuffle) { aborted=true; write=volume; } break; }
                    var snapshot=captured[write];
                    // Skip unchanged blocks before the support/ownership spatial queries, not afterwards.
                    if(!unchanged(destination,pos,snapshot)) {
                        game.items().indirectKills().supportRemoved(destination,job.owner,pos);
                        game.maps().restoreSnapshot(destination,pos,snapshot);
                    }
                    write++; budget[0]--;
                }
            });
            if(aborted) return true;
            if(write<volume) return false;
            // Один native перенос scheduled ticks на clone: не обходим индекс очереди по каждой строке.
            destination.getBlockTicks().copyAreaFrom(source.getBlockTicks(),
                    new net.minecraft.world.level.levelgen.structure.BoundingBox(min.getX(),min.getY(),min.getZ(),
                            min.getX()+sx-1,min.getY()+sy-1,min.getZ()+sz-1),target.subtract(min));
            return true;
        }
    }
    private void clone(Job job,Vec3 a,Vec3 b,Vec3 target) { job.operations.add(new Clone(job.level,a,b,job.level,target)); }
    private void localClone(Job job,Seed seed,double ax,double ay,double az,double bx,double by,double bz,double tx,double ty,double tz) {
        clone(job,local(seed.point,seed.pitch,seed.yaw,ax,ay,az),local(seed.point,seed.pitch,seed.yaw,bx,by,bz),
                local(seed.point,seed.pitch,seed.yaw,tx,ty,tz));
    }
    void applyShuffle(ServerPlayer owner,Vec3 point,float pitch,float yaw,Vec3 center,boolean expand) {
        enterCheckpointCallback();try {
            var parent=new MarkerState(owner,game.items().epoch(),center,Vec3.ZERO,expand?2:1,1);
            seed(new Seed(parent,point,pitch,yaw));
        }finally{leaveCheckpointCallback();}
    }
    /** Sequential legacy shuffle using the shared snapshot queue. */
    private void seed(Seed seed) {
        if(game.items().magic().suppressedAt(seed.level,seed.point)) return;
        var job=new Job(seed.owner); populate(job,seed); queue(job);
    }
    private void populate(Job job,Seed seed) {
        if(seed.expand) {
            localClone(job,seed,0,0,0,0,0,15,0,0,-16);
            localClone(job,seed,0,0,0,0,15,0,0,-16,0);
            localClone(job,seed,0,0,0,15,0,0,-16,0,0);
            expandSources(job,seed.point,seed.pitch,seed.yaw);
        } else {
            localClone(job,seed,0,0,0,0,0,7,0,0,-8);
            localClone(job,seed,0,0,0,0,7,0,0,-8,0);
            localClone(job,seed,0,0,0,7,0,0,-8,0,0);
        }
        Vec3 towards=seed.center.subtract(seed.point).normalize();
        float pitch=pitch(towards),yaw=yaw(towards); int half=seed.expand?7:1,offset=seed.expand?14:4;
        // Successful clones reject source/destination overlap, so none of these five writes
        // can change their common source. Freeze the fan once, as in one legacy function,
        // even when budgeted native work spans world ticks; retain its ordered writes.
        var shared=new SharedCapture();
        for(int[] delta:new int[][]{{0,0},{1,0},{-1,0},{0,1},{0,-1}})
            job.operations.add(new Clone(job.level,seed.point.add(-half,-half,-half),seed.point.add(half,half,half),
                    job.level,local(seed.point,pitch,yaw,delta[0]*offset,delta[1]*offset,offset),shared));
        fill(job,seed.point,half);
    }
    private void crossClone(Job job,ServerLevel source,Vec3 point,float pitch,float yaw,
                            double ax,double ay,double az,double bx,double by,double bz,double tx,double ty,double tz) {
        if(source==null) return;
        var operation=new Clone(source,local(point,pitch,yaw,ax,ay,az),local(point,pitch,yaw,bx,by,bz),
                job.level,local(point,pitch,yaw,tx,ty,tz));
        if(!operation.rejected && source!=job.level)
            for(int cx=operation.min.getX()>>4;cx<=(operation.min.getX()+operation.sx-1)>>4;cx++)
                for(int cz=operation.min.getZ()>>4;cz<=(operation.min.getZ()+operation.sz-1)>>4;cz++)
                    job.sourceReady(source,new BlockPos(cx<<4,operation.min.getY(),cz<<4));
        job.operations.add(operation);
    }
    private void expandSources(Job job,Vec3 point,float pitch,float yaw) {
        var nether=game.server().getLevel(Level.NETHER); var end=game.server().getLevel(Level.END);
        var nexus=game.server().getLevel(net.minecraft.resources.ResourceKey.create(Registries.DIMENSION,Identifier.parse("minecraft:nexus")));
        crossClone(job,nether,point,pitch,yaw,8,-8,8,5,-5,5,-5,5,-5);
        crossClone(job,nether,point,pitch,yaw,-8,8,-8,-5,5,-5,5,-5,5);
        crossClone(job,end,point,pitch,yaw,0,0,0,0,0,15,0,0,-16);
        crossClone(job,end,point,pitch,yaw,0,0,0,0,15,0,0,-16,0);
        crossClone(job,nexus,point,pitch,yaw,8,-8,8,5,-5,5,5,-5,-5);
        crossClone(job,nexus,point,pitch,yaw,-8,8,-8,-5,5,-5,5,5,-5);
    }
    void applyExpandSources(ServerPlayer owner,Vec3 point,float pitch,float yaw) {
        enterCheckpointCallback();try {
            var job=new Job(owner); expandSources(job,point,pitch,yaw); queue(job);
        }finally{leaveCheckpointCallback();}
    }
    private void fill(Job job,Vec3 point,int half) {
        BlockPos min=BlockPos.containing(point.add(-half,-half,-half)),max=BlockPos.containing(point.add(half,half,half));
        job.operations.add(new Operation() {
            final int sx=max.getX()-min.getX()+1,sz=max.getZ()-min.getZ()+1;
            final int volume=sx*sz*(max.getY()-min.getY()+1); int cursor;
            public boolean step(Job parent,long deadline,int[] budget) {
                game.items().indirectKills().trapChanges(parent.owner,()-> {
                    while(cursor<volume && budget[0]>0 && System.nanoTime()<deadline) {
                        BlockPos pos=min.offset(cursor%sx,cursor/(sx*sz),(cursor/sx)%sz);
                        if(!resident(parent.level,pos)) { if(parent instanceof LiveShuffle) cursor=volume; break; }
                        if(!parent.level.getBlockState(pos).isAir()) {
                            game.items().indirectKills().supportRemoved(parent.level,parent.owner,pos);
                            parent.level.setBlock(pos,Blocks.AIR.defaultBlockState(),FLAGS);
                        }
                        cursor++; budget[0]--;
                    }
                }); return cursor==volume;
            }
        });
    }
    private void grid(Job job,Vec3 point,int half,boolean divider) {
        var shared=new SharedCapture();
        int low=-3*half-1,high=half+1,center=-half;
        int[][] ring=divider?new int[][]{{low,high},{low,center},{low,low},{center,low},{high,low},{high,center},{high,high},{center,high}}
                :new int[][]{{high,high},{center,high},{high,center},{center,low},{low,low},{low,center},{low,high},{high,low}};
        for(int layer:new int[]{center,divider?high:low,divider?low:high}) {
            if(divider && layer!=center) job.operations.add(new Clone(job.level,point.add(-half,-half,-half),point.add(half,half,half),job.level,point.add(center,layer,center),shared));
            for(int[] delta:ring) job.operations.add(new Clone(job.level,point.add(-half,-half,-half),point.add(half,half,half),job.level,point.add(delta[0],layer,delta[1]),shared));
            if(!divider && layer!=center) job.operations.add(new Clone(job.level,point.add(-half,-half,-half),point.add(half,half,half),job.level,point.add(center,layer,center),shared));
        }
    }
    private final class Divide extends Job {
        final Vec3 start,direction; int sample;
        Divide(ServerPlayer owner) {this(owner,owner.getEyePosition(),owner.getLookAngle().normalize());}
        Divide(ServerPlayer owner,Vec3 origin,Vec3 look) {
            super(owner); start=origin; direction=look;
            operations.add((job,deadline,budget)-> {
                while(sample<=250 && budget[0]>0 && System.nanoTime()<deadline) {
                    Vec3 point=start.add(direction.scale(sample*.4));
                    if(!resident(level,BlockPos.containing(point))) return false;
                    if(game.items().magic().suppressedAt(level,point)) return true;
                    sample++; budget[0]--;
                    LbcOverload.sound(owner,point,"minecraft:block.anvil.place",.05f,1,0);
                    level.sendParticles(ParticleTypes.PORTAL,point.x,point.y,point.z,30,8,.1,8,1);
                    boolean entity=!level.getEntitiesOfClass(LivingEntity.class,new AABB(point,point.add(1,1,1)),
                            target->target!=owner && game.items().wands().target(owner,target) && !target.entityTags().contains("strict_map_object")).isEmpty();
                    boolean block=!level.getBlockState(BlockPos.containing(point)).isAir();
                    if(entity || block) {
                        grid(this,point,4,false);
                        return true;
                    }
                }
                return sample>250;
            });
        }
    }
    private boolean protectedEntity(ServerPlayer owner,Entity target) {
        return target.entityTags().contains("strict_map_object")
                || target.entityTags().contains("core_marker") || target.entityTags().contains("map_object")
                || ((target instanceof Marker || target instanceof net.minecraft.world.entity.Display || target instanceof net.minecraft.world.entity.Interaction
                    || target instanceof net.minecraft.world.entity.AreaEffectCloud)
                    && target.entityTags().stream().anyMatch(tag->tag.startsWith("swrg.")))
                || !game.items().wands().target(owner,target,true);
    }
    private static final class PullSample {
        final MarkerState[] holes=new MarkerState[2]; final Vec3[] points=new Vec3[2]; int seen;
        void consider(MarkerState hole) {
            int index=seen<2?seen:hole.owner.getRandom().nextInt(seen+1);
            if(index<2) { holes[index]=hole; points[index]=hole.point; }
            seen++;
        }
    }
    /** Shared noisy radial erosion follows the latest body, without queued old positions. */
    private final class HoleTerrain extends Job {
        final MarkerState marker;BlackHoleErosion erosion;int clock=Integer.MIN_VALUE;
        HoleTerrain(MarkerState marker){
            super(marker.owner);this.marker=marker;
            if(marker.erosionSeed==null)marker.erosionSeed=owner.getRandom().nextLong();
            erosion=new BlackHoleErosion(level,marker.erosionSeed,true);
        }
        @Override public boolean ready(){return !valid(this)||!valid(marker)||erosion.hasWork()||!checkpointDraining&&clock!=marker.age;}
        @Override public boolean step(){
            enterCheckpointCallback();try {
                if(!valid(this)||!valid(marker)||game.items().magic().suppressedAt(level,marker.point)){cancel();return true;}
                if(paused(owner))return false;
                if(!checkpointDraining){clock=marker.age;erosion.move(marker.point,HOLE_DESTRUCTION,clock);}
                else if(!erosion.hasWork())return false;
                var kills=game.items().indirectKills();
                kills.withSupportCandidates(level,new AABB(marker.point,marker.point).inflate(HOLE_DESTRUCTION+1),()->
                    kills.trapChanges(level,owner,()->erosion.step(1024,System.nanoTime()+750_000,(pos,state)->{
                        if(game.items().magic().suppressedAt(level,Vec3.atCenterOf(pos)))return;
                        kills.supportRemoved(level,owner,pos);
                        if(level.getBlockEntity(pos) instanceof net.minecraft.world.Container container)container.clearContent();
                        level.setBlock(pos,Blocks.AIR.defaultBlockState(),FLAGS);
                    })));
                return false;
            }finally{leaveCheckpointCallback();}
        }
        @Override public void cancel(){
            enterCheckpointCallback();try {erosion.clear();super.cancel();
            }finally{leaveCheckpointCallback();}
        }
    }
    private void removeVisual(MarkerState marker) {
        if(marker.visual!=null) { game.items().blackHoleVisuals().remove(marker.visual); marker.visual=null; }
        if(marker.terrain!=null) { marker.terrain.cancel(); marker.terrain=null; }
    }
    private void hole(MarkerState marker,java.util.Map<Entity,PullSample> pulls) {
        if(marker.terrain==null) { marker.terrain=new HoleTerrain(marker); queue(marker.terrain); }
        if(marker.visual==null) marker.visual=game.items().blackHoleVisuals().create(marker.owner,marker.point,HOLE_SHADER_RADIUS);
        else game.items().blackHoleVisuals().move(marker.visual,marker.point);
        var source=new DamageSource(marker.owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(LbcDamage.DUPLICATING_HOLE),marker.owner);
        int damaged=0;
        for(var target:marker.level.getEntities((Entity)null,new AABB(marker.point,marker.point).inflate(HOLE_INFLUENCE),entity->
                !protectedEntity(marker.owner,entity) && game.items().wands().target(marker.owner,entity)
                        && (entity.position().distanceToSqr(marker.point)<=HOLE_INFLUENCE*HOLE_INFLUENCE
                        || entity instanceof LivingEntity&&BlackHoleDamage.distanceSquared(entity,marker.point)<=HOLE_DAMAGE*HOLE_DAMAGE))) {
            if(game.items().magic().suppressedAt(marker.level,target.position()) || target.entityTags().contains("time_stopped")) continue;
            double distance=target.position().distanceToSqr(marker.point);
            // Input includes walking, jumping, crouching and sprinting.
            if(distance<=HOLE_INFLUENCE*HOLE_INFLUENCE&&(distance<=HOLE_STRONG*HOLE_STRONG || !(target instanceof ServerPlayer player)
                    || player.getLastClientInput().equals(net.minecraft.world.entity.player.Input.EMPTY)))
                pulls.computeIfAbsent(target,ignored->new PullSample()).consider(marker);
            if(target instanceof LivingEntity living && damaged<5 && BlackHoleDamage.distanceSquared(target,marker.point)<=HOLE_DAMAGE*HOLE_DAMAGE
                    && game.items().mana().score(target,"void_resistance").get()<=0) {
                BlackHoleDamage.apply(game,living,source,.01f); damaged++;
            }
        }
    }
    private void applyPulls(java.util.Map<Entity,PullSample> pulls) {
        for(var entry:pulls.entrySet()) {
            Entity target=entry.getKey(); PullSample sample=entry.getValue(); Vec3 combined=Vec3.ZERO;
            if(!target.isAlive() || target.isRemoved()) continue;
            double strongest=0; MarkerState author=null;
            for(int index=0;index<sample.holes.length;index++) {
                var hole=sample.holes[index];
                if(hole==null || !valid(hole)) continue;
                Vec3 direction=sample.points[index].subtract(target.position()); double distance=direction.length();
                if(distance<1.0e-6) continue;
                double strength=distance<=HOLE_STRONG?.18:.012*Math.max(0,(HOLE_INFLUENCE-distance)/(HOLE_INFLUENCE-HOLE_STRONG));
                combined=combined.add(direction.scale(strength/distance));
                if(strength>strongest) { strongest=strength; author=hole; }
            }
            double length=combined.length();
            if(length<1.0e-6) continue;
            // Two sampled holes stack, with a bound on their combined acceleration.
            Vec3 force=combined.scale(Math.min(length,.35)/length);
            target.setDeltaMovement(target.getDeltaMovement().add(force)); target.needsSync=true;
            if(target instanceof ServerPlayer player)
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
            if(target instanceof LivingEntity living && author!=null) game.items().indirectKills().displaced(living,author.owner);
        }
    }
    /** Source divider_cd is scoreboard-owned; food must enroll newly positive entries for ticking. */
    void addPlayerCounters(ServerPlayer owner) {
        if(game.items().mana().hudValue(owner,"divider_cd")>0) dividerTimers.putIfAbsent(owner,0);
    }

    public void tick() {
        enterCheckpointCallback();try {
            for(var entry:List.copyOf(dividerTimers.entrySet())) {
                var owner=entry.getKey();
                if(!valid(owner)) { game.items().mana().score(owner,"divider_cd").set(0); dividerTimers.remove(owner); continue; }
                if(paused(owner)) continue;
                int phase=entry.getValue()+1;
                if(phase==20) {
                    var cd=game.items().mana().score(owner,"divider_cd"); cd.set(Math.max(0,cd.get()-1)); phase=0;
                    if(cd.get()==0) { dividerTimers.remove(owner); continue; }
                }
                dividerTimers.put(owner,phase);
            }
            // Snapshot acquisition postpones new producer work while already accepted writes drain.
            if(checkpointDraining)return;
            var pulls=new IdentityHashMap<Entity,PullSample>();
            for(var marker:List.copyOf(markers)) {
                if(marker.retainedOwner)continue;
                if(!valid(marker) || game.items().magic().suppressedAt(marker.level,marker.point)) {
                    marker.cancelled=true; removeVisual(marker);
                    markers.remove(marker); continue;
                }
                if(paused(marker.owner) || !resident(marker.level,BlockPos.containing(marker.point))) continue;
                Vec3 before=marker.point;
                if(marker.mode==4) hole(marker,pulls);
                else {
                    Vec3 point=new Vec3(Math.floor(before.x)+marker.owner.getRandom().nextInt(9)-4,
                            Math.floor(before.y)+marker.owner.getRandom().nextInt(9)-4,Math.floor(before.z)+marker.owner.getRandom().nextInt(9)-4);
                    if(marker.shuffle==null) { marker.shuffle=new LiveShuffle(marker); queue(marker.shuffle); }
                    marker.shuffle.offer(new Seed(marker,point,randomPitch(marker.owner),randomYaw(marker.owner)));
                }
                if(marker.mode!=4) {
                    marker.level.sendParticles(ParticleTypes.REVERSE_PORTAL,before.x,before.y,before.z,10,0,0,0,1);
                    LbcOverload.sound(marker.owner,before,"minecraft:entity.bee.loop_aggressive",.1f,0,30);
                }
                marker.point=before.add(marker.direction.scale(marker.mode==2?2:marker.mode==4?.3:1));
                if(marker.visual!=null) game.items().blackHoleVisuals().move(marker.visual,marker.point);
                if(++marker.age>=marker.lifetime) {
                    removeVisual(marker);
                    markers.remove(marker);
                }
            }
            applyPulls(pulls);
        }finally{leaveCheckpointCallback();}
    }
    public int count() { return markers.size()+jobs.size(); }
    public void forget(ServerPlayer owner) {
        enterCheckpointCallback();try {
            if(dividerTimers.remove(owner)!=null) game.items().mana().score(owner,"divider_cd").set(0);
            markers.removeIf(state->{ if(state.owner!=owner) return false; removeVisual(state); return true; });
            for(var job:List.copyOf(jobs)) if(job.owner==owner) job.cancel();
        }finally{leaveCheckpointCallback();}
    }
    public void stop() {
        enterCheckpointCallback();try {
            for(var marker:markers) removeVisual(marker);
            markers.clear(); for(var job:List.copyOf(jobs)) job.cancel();
            for(var owner:dividerTimers.keySet()) game.items().mana().score(owner,"divider_cd").set(0);
            dividerTimers.clear();checkpointDraining=false;
        }finally{leaveCheckpointCallback();}
    }
    public void transformOrigins(ServerLevel level,UnaryOperator<Vec3> transform) {
        enterCheckpointCallback();try {
            for(var marker:markers) if(marker.level==level) {
                Vec3 point=transform.apply(marker.point);
                if(point==null) { marker.cancelled=true; removeVisual(marker); }
                else { marker.point=point; if(marker.visual!=null) game.items().blackHoleVisuals().move(marker.visual,point); }
            }
            markers.removeIf(state->state.cancelled);
        }finally{leaveCheckpointCallback();}
    }
    /** Pause producers only. Existing shared jobs retain their original native step and chunk waits. */
    void beginCheckpointDrain(){checkpointBoundary();checkpointDraining=true;}
    void endCheckpointDrain(){checkpointBoundary();checkpointDraining=game.supernovaMapTransition();}
    private int checkpointWrites(Job job){
        if(job.cancelled)return 0;
        int pending=job.operations.size();
        if(job instanceof LiveShuffle shuffle&&shuffle.latest!=null)pending++;
        if(job instanceof HoleTerrain terrain&&terrain.erosion.hasWork())pending++;
        for(var dimension:job.loading.values())for(var future:dimension.values())if(!future.isDone())pending++;
        return pending;
    }
    int pendingCheckpointWrites(){
        checkpointBoundary();int pending=0;for(var job:jobs)pending+=checkpointWrites(job);return pending;
    }
    void rebindRetained(ServerPlayer player){
        checkpointBoundary();
        for(var marker:markers)if(marker.retainedOwner&&marker.owner.getUUID().equals(player.getUUID())){marker.owner=player;marker.retainedOwner=false;}
        for(var old:List.copyOf(dividerTimers.keySet()))if(old!=player&&old.getUUID().equals(player.getUUID())){var phase=dividerTimers.remove(old);dividerTimers.put(player,phase);}
    }
    boolean captureReady(){return checkpointDraining&&pendingCheckpointWrites()==0;}
    void checkpointBoundary(){if(checkpointCallbacks!=0)throw new IllegalStateException("Finish Duplicating native callbacks before checkpointing");}
    private void enterCheckpointCallback(){checkpointCallbacks++;game.items().stonekivi().beginNativeWork();}
    private void leaveCheckpointCallback(){game.items().stonekivi().endNativeWork();checkpointCallbacks--;}
    private void discardFutureCheckpointWork(){
        for(var marker:List.copyOf(markers)){
            if(marker.visual!=null){game.items().blackHoleVisuals().detachForCheckpoint(marker.visual);marker.visual=null;}
            marker.terrain=null;marker.shuffle=null;
        }
        for(var job:List.copyOf(jobs))job.cancel();
        markers.clear();dividerTimers.clear();
    }
    /** Rollback-only discard: capture acquisition must NEVER call this to make pending writes disappear. */
    void prepareCheckpointRestore(){
        checkpointBoundary();
        if(!game.server().isSameThread()||!game.server().tickRateManager().isFrozen())
            throw new IllegalStateException("Discard future Duplicating work on the frozen native thread");
        enterCheckpointCallback();try{checkpointDraining=true;discardFutureCheckpointWork();}finally{leaveCheckpointCallback();}
    }
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){
        checkpointBoundary();if(!captureReady())throw new IllegalStateException("Drain original Duplicating writes before taking its essentials");
        return new Checkpoint(links);
    }
    final class Checkpoint {
        private record MarkerImage(MarkerState original,MatchDamageSources.Ref owner,Vec3 point,int age,boolean cancelled,Long erosionSeed,int cloneMask,Vec3 direction) { }
        private final List<MarkerImage> saved;
        private final MatchSpellCheckpoint.IdentityImage<ServerPlayer,Integer> timers;
        Checkpoint(MatchSpellCheckpoint.Links links){
            saved=markers.stream().map(marker->new MarkerImage(marker,links.key(marker.owner),marker.point,marker.age,marker.cancelled,marker.erosionSeed,marker.cloneMask,marker.direction)).toList();
            timers=new MatchSpellCheckpoint.IdentityImage<>(dividerTimers,links);
        }
        Runnable prepare(MatchSpellCheckpoint.Links links){
            checkpointBoundary();var timerValues=timers.restore(links);var plans=new ArrayList<Runnable>();
            for(var image:saved){var current=(ServerPlayer)links.get(image.owner);MatchAuthorityCheckpoint.world(game,image.original.level);
                plans.add(()->{var marker=image.original;marker.owner=current;marker.retainedOwner=MatchOfflinePlayers.retained(current);marker.point=image.point;marker.age=image.age;marker.cancelled=image.cancelled;marker.erosionSeed=image.erosionSeed;marker.cloneMask=image.cloneMask;marker.direction=image.direction;
                    marker.visual=null;marker.terrain=null;marker.shuffle=null;markers.add(marker);});}
            return ()->{enterCheckpointCallback();try{
                checkpointDraining=true;discardFutureCheckpointWork();plans.forEach(Runnable::run);dividerTimers=timerValues;
            }finally{leaveCheckpointCallback();}};
        }
    }
    record MarkerView(ServerPlayer owner,String epoch,ServerLevel level,Vec3 point,int age,int lifetime,int mode) { }
    record SeedView(ServerPlayer owner,Vec3 point,boolean expand) { }
    List<MarkerView> markerSnapshot() { return markers.stream().map(state->new MarkerView(state.owner,state.epoch,state.level,state.point,state.age,state.lifetime,state.mode)).toList(); }
    List<SeedView> seedSnapshot() { return List.of(); }
    int pendingWorkCount() { return (int)jobs.stream().filter(job->job instanceof HoleTerrain terrain?terrain.ready():job instanceof LiveShuffle live?live.ready():!job.held || !job.operations.isEmpty()).count(); }

    int cloneNearby(ServerPlayer holder,int skill,int budget,long deadline) {
        int made=0,count=CloneVirtualAbilityStates.count(skill),bit=CloneVirtualAbilityStates.bit(skill);
        for(var original:List.copyOf(markers)){
            if(budget-made<count||System.nanoTime()>=deadline)break;
            if(checkpointDraining||!valid(original)||original.retainedOwner||paused(original.owner)
                    ||game.items().timeStop().frozen(original.owner)
                    ||!CloneVirtualAbilityStates.near(game,holder,original.level,original.point,original.cloneMask,skill))continue;
            var points=CloneVirtualAbilityStates.positions(holder,original.level,original.point,skill);if(points==null)continue;
            var created=new ArrayList<MarkerState>();
            try{
                for(var point:points){double speed=original.mode==2?2:original.mode==4?.3:1;
                    var direction=skill==15?original.direction:LbcCloneSkills.spreadMotion(original.direction.scale(speed),holder,1000,1500).scale(1/speed);
                    var copy=new MarkerState(original.owner,original.epoch,point,direction,original.mode,original.lifetime);created.add(copy);
                    copy.age=original.age;copy.cloneMask=original.cloneMask|bit;copy.erosionSeed=original.erosionSeed;markers.add(copy);
                    if(original.terrain!=null){copy.terrain=new HoleTerrain(copy);
                        copy.terrain.erosion=original.terrain.erosion.copyTranslated(point.subtract(original.point));
                        copy.terrain.clock=original.terrain.clock;queue(copy.terrain);}
                    if(original.visual!=null)copy.visual=game.items().blackHoleVisuals().create(original.owner,point,HOLE_SHADER_RADIUS);
                }
                original.cloneMask|=bit;made+=count;
                if(skill==17){double speed=original.mode==2?2:original.mode==4?.3:1;
                    original.direction=LbcCloneSkills.spreadMotion(original.direction.scale(speed),holder,10,10).scale(1/speed);}
            }catch(RuntimeException failure){for(var copy:created){markers.remove(copy);removeVisual(copy);}throw failure;}
        }
        return made;
    }


    void planetoid(ServerPlayer holder) {
        if(checkpointDraining)return;
        for(var marker:markers)if(valid(marker)&&LbcPlanetoidSkill.near(game,holder,marker.level,marker.point))
            marker.direction=LbcPlanetoidSkill.direction(holder,marker.point,82);
    }

}
