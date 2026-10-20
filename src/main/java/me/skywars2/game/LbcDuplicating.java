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
    private final List<Seed> seeds=new ArrayList<>();
    private final List<Job> jobs=new ArrayList<>();
    private final java.util.Map<ServerPlayer,Integer> dividerTimers=new IdentityHashMap<>();
    private CompletableFuture<MapManager.BlockPattern> preparation;
    private MapManager.BlockPattern holePattern;
    private static class Owned {
        final ServerPlayer owner; final ServerLevel level; final String epoch;
        boolean cancelled;
        Owned(ServerPlayer owner,String epoch) { this.owner=owner; level=owner.level(); this.epoch=epoch; }
    }
    private static final class MarkerState extends Owned {
        Vec3 point; final Vec3 direction; final int mode,lifetime; int age;
        Job terrain;
        MarkerState(ServerPlayer owner,String epoch,Vec3 point,Vec3 direction,int mode,int lifetime) {
            super(owner,epoch); this.point=point; this.direction=direction; this.mode=mode; this.lifetime=lifetime;
        }
    }
    private static final class Seed extends Owned {
        Vec3 point; final float pitch,yaw; final MarkerState parent; final boolean expand;
        Seed(MarkerState parent,Vec3 point,float pitch,float yaw) {
            super(parent.owner,parent.epoch); this.parent=parent; this.point=point; this.pitch=pitch; this.yaw=yaw; expand=parent.mode==2;
        }
    }
    LbcDuplicating(GameController game) { this.game=game; }
    public void prepare() {
        holePattern=null;
        preparation=game.maps().prepareBlockPattern(Identifier.parse("lbc:obsidian_sphere_air_r7"));
    }
    public boolean ready() {
        if(holePattern==null && preparation!=null && preparation.isDone()) {
            try { holePattern=preparation.join(); }
            catch(java.util.concurrent.CompletionException error) {
                me.skywars2.SkyWars2.LOGGER.error("Duplicating preparation failed",error); preparation=null;
            }
        }
        return holePattern!=null;
    }
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
            mode=mode%4+1; var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            tag.putInt("mode",mode); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
            owner.sendSystemMessage(Component.translatable(switch(mode) {
                case 2->"mode_expand"; case 3->"mode_divide"; case 4->"mode_black_hole"; default->"mode_default";
            })); return true;
        }
        int price=game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("dublicating_wand_mana_use"),"lbc.math").get();
        if(!game.items().mana().trySpendWand(owner,price)) return false;
        if(!upgraded && owner.isShiftKeyDown() && LbcAcademy.trial(stack)) {
            trial(owner); game.items().academy().ready(owner,hand,stack);
        } else if(mode==3) queue(new Divide(owner));
        else {
            Vec3 direction=owner.getLookAngle().normalize();
            markers.add(new MarkerState(owner,game.items().epoch(),owner.getEyePosition().add(direction.scale(mode==2?14:mode==4?8:7)),
                    direction,mode,mode==2?100:mode==4?300:200));
        }
        game.items().wands().wandCasted(owner); return true;
    }
    public boolean useDivider(ServerPlayer owner,ItemStack stack) {
        if(!castable(owner) || game.items().mana().score(owner,"divider_cd").get()>0) return false;
        var job=new Job(owner); grid(job,owner.position(),12,true); queue(job);
        game.items().mana().score(owner,"divider_cd").set(30);
        dividerTimers.put(owner,0);
        for(float pitch:new float[]{0,.2f,.5f,.13f}) LbcOverload.sound(owner,"minecraft:block.anvil.use",4,pitch,0);
        return true;
    }
    public boolean useDivider(ServerPlayer owner,InteractionHand hand,ItemStack stack) { return useDivider(owner,stack); }
    private void trial(ServerPlayer owner) {
        markers.add(new MarkerState(owner,game.items().epoch(),owner.position(),new Vec3(0,-1,0),1,200));
        for(int index=0;index<90;index++) {
            Vec3 point=new Vec3(Math.floor(owner.getX())+owner.getRandom().nextInt(61)-30,owner.getY(),
                    Math.floor(owner.getZ())+owner.getRandom().nextInt(61)-30);
            markers.add(new MarkerState(owner,game.items().epoch(),point,randomDirection(owner),1,30));
        }
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
            if(!valid(this)) { cancel(); return true; }
            if(paused(owner)) return false;
            long deadline=System.nanoTime()+500_000; int[] budget={512};
            while(!operations.isEmpty() && budget[0]>0 && System.nanoTime()<deadline) {
                if(!operations.peek().step(this,deadline,budget)) return false;
                operations.remove();
            }
            if(operations.isEmpty() && !held) { jobs.remove(this); return true; } return false;
        }
        public boolean ready() { return !held || !operations.isEmpty() || !valid(this); }
        public void cancel() { cancelled=true; operations.clear(); loading.clear(); jobs.remove(this); }
        int sourceReady(ServerLevel source,BlockPos pos) {
            if(source.hasChunkAt(pos)) return 1;
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
                BlockPos pos=sourceCell(read); if(!job.level.hasChunkAt(pos)) return false;
                if(selected(read)) captured[read]=game.maps().snapshot(job.level,pos);
                read++; budget[0]--;
            }
            if(read<volume) return false;
            // Перенос не удаляет оригиналы, пока вся выбранная область назначения не доступна.
            if(action!=Action.DELETE) {
                while(destinationReady<volume && budget[0]>0 && System.nanoTime()<deadline) {
                    if(captured[destinationReady]!=null && !job.level.hasChunkAt(destinationCell(destinationReady))) {
                        destinationReady=0; return false;
                    }
                    destinationReady++; budget[0]--;
                }
                if(destinationReady<volume) return false;
            }
            game.items().indirectKills().trapChanges(job.level,job.owner,()-> {
                if(action!=Action.COPY) while(clear<volume && budget[0]>0 && System.nanoTime()<deadline) {
                    BlockPos pos=sourceCell(clear); if(!job.level.hasChunkAt(pos)) return;
                    if(action!=Action.DELETE && captured[clear]!=null && !job.level.hasChunkAt(destinationCell(clear))) {
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
                    BlockPos pos=destinationCell(write); if(!job.level.hasChunkAt(pos)) return;
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
        var job=new Job(owner); job.operations.add(new Patch(owner.level(),source,target,sx,sy,sz,shape,action,rotation,mirror)); queue(job);
    }
    private void randomPatch(Seed seed) {
        var random=seed.owner.getRandom(); int sx=random.nextInt(7)+1,sy=random.nextInt(7)+1,sz=random.nextInt(7)+1;
        Shape shape=Shape.values()[random.nextInt(Shape.values().length)]; int axis=random.nextInt(3);
        if(shape==Shape.SLAB) { if(axis==0) sx=1; else if(axis==1) sy=1; else sz=1; }
        else if(shape==Shape.LINE) { if(axis!=0) sx=1; if(axis!=1) sy=1; if(axis!=2) sz=1; }
        else if(shape==Shape.SPHERE) sy=sz=sx;
        Action action=Action.values()[random.nextInt(Action.values().length)];
        Vec3 source=seed.point.add(-(sx-1)*.5,-(sy-1)*.5,-(sz-1)*.5);
        Vec3 target=action==Action.ROTATE || action==Action.MIRROR?source:source.add(random.nextInt(17)-8,random.nextInt(17)-8,random.nextInt(17)-8);
        Rotation rotation=new Rotation[]{Rotation.CLOCKWISE_90,Rotation.CLOCKWISE_180,Rotation.COUNTERCLOCKWISE_90}[random.nextInt(3)];
        Mirror mirror=random.nextBoolean()?Mirror.LEFT_RIGHT:Mirror.FRONT_BACK;
        applyPatch(seed.owner,source,target,sx,sy,sz,shape,action,rotation,mirror);
    }
    /** Снимок делается заново перед каждой командой, полностью до её первой записи. */
    private final class Clone implements Operation {
        final ServerLevel source,destination; final BlockPos min,target; final int sx,sy,sz,volume;
        MapManager.BlockSnapshot[] captured; int read,write; final boolean rejected;
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
                if(!source.hasChunkAt(pos)) {
                    if(source!=job.level && job.sourceReady(source,pos)<0) return true;
                    return false;
                }
                captured[read++]=game.maps().snapshot(source,pos); budget[0]--;
            }
            if(read<volume) return false;
            if(shared!=null) shared.blocks=captured;
            // Один trap frame на ограниченную порцию, а не на каждую клетку.
            game.items().indirectKills().trapChanges(destination,job.owner,()-> {
                while(write<volume && budget[0]>0 && System.nanoTime()<deadline) {
                    BlockPos pos=cell(target,write);
                    if(!destination.hasChunkAt(pos)) break;
                    game.items().indirectKills().supportRemoved(destination,job.owner,pos);
                    game.maps().restoreSnapshot(destination,pos,captured[write]);
                    write++; budget[0]--;
                }
            });
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
    private void seed(Seed seed) {
        if(!seed.expand) { randomPatch(seed); return; }
        var job=new Job(seed.owner);
        localClone(job,seed,0,0,0,15,0,0,-16,0,0);
        localClone(job,seed,8,8,8,5,5,5,-5,-5,-5);
        expandSources(job,seed.point,seed.pitch,seed.yaw);
        Vec3 towards=seed.parent.point.subtract(seed.point).normalize();
        float pitch=pitch(towards),yaw=yaw(towards); int half=seed.expand?7:1,offset=seed.expand?14:4;
        for(int[] delta:new int[][]{{0,0},{1,0},{-1,0},{0,1},{0,-1}})
            clone(job,seed.point.add(-half,-half,-half),seed.point.add(half,half,half),
                    local(seed.point,pitch,yaw,delta[0]*offset,delta[1]*offset,offset));
        queue(job);
    }
    private void crossClone(Job job,ServerLevel source,Vec3 point,float pitch,float yaw,
                            double ax,double ay,double az,double bx,double by,double bz,double tx,double ty,double tz) {
        if(source!=null) job.operations.add(new Clone(source,local(point,pitch,yaw,ax,ay,az),local(point,pitch,yaw,bx,by,bz),
                job.level,local(point,pitch,yaw,tx,ty,tz)));
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
        var job=new Job(owner); expandSources(job,point,pitch,yaw); queue(job);
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
                        if(!parent.level.hasChunkAt(pos)) break;
                        game.items().indirectKills().supportRemoved(parent.level,parent.owner,pos);
                        parent.level.setBlock(pos,Blocks.AIR.defaultBlockState(),FLAGS);
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
        Divide(ServerPlayer owner) {
            super(owner); start=owner.getEyePosition(); direction=owner.getLookAngle().normalize();
            operations.add((job,deadline,budget)-> {
                while(sample<=250 && budget[0]>0 && System.nanoTime()<deadline) {
                    Vec3 point=start.add(direction.scale(sample*.4));
                    if(!level.hasChunkAt(BlockPos.containing(point))) return false;
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
        return target instanceof Marker || target.entityTags().contains("strict_map_object")
                || target.entityTags().contains("core_marker") || target.entityTags().contains("map_object")
                || ((target instanceof net.minecraft.world.entity.Display || target instanceof net.minecraft.world.entity.Interaction
                    || target instanceof net.minecraft.world.entity.AreaEffectCloud)
                    && target.entityTags().stream().anyMatch(tag->tag.startsWith("swrg.")))
                || !game.items().wands().target(owner,target,true);
    }
    private void hole(MarkerState marker) {
        if(marker.terrain==null) { marker.terrain=new Job(marker.owner); marker.terrain.held=true; queue(marker.terrain); }
        var job=marker.terrain; var origin=BlockPos.containing(marker.point.add(-7,-7,-7));
        job.operations.add(new Operation() {
            int cursor; final int sx=holePattern.size.getX(),sz=holePattern.size.getZ();
            final int volume=sx*sz*holePattern.size.getY();
            public boolean step(Job parent,long deadline,int[] budget) {
                game.items().indirectKills().trapChanges(parent.owner,()-> {
                    while(cursor<volume && budget[0]>0 && System.nanoTime()<deadline) {
                        int x=cursor%sx,y=cursor/(sx*sz),z=(cursor/sx)%sz; var pos=origin.offset(x,y,z);
                        if(!parent.level.hasChunkAt(pos)) break;
                        var state=holePattern.state(x,y,z);
                        if(state!=null && parent.owner.getRandom().nextFloat()<=.65f) {
                            game.items().indirectKills().supportRemoved(parent.level,parent.owner,pos);
                            parent.level.setBlock(pos,state,FLAGS);
                        }
                        cursor++; budget[0]--;
                    }
                }); return cursor==volume;
            }
        });
        var source=new DamageSource(marker.owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(LbcDamage.DUPLICATING_HOLE),marker.owner);
        int damaged=0;
        for(var target:marker.level.getEntities((Entity)null,new AABB(marker.point,marker.point).inflate(7),entity->
                !protectedEntity(marker.owner,entity) && entity.position().distanceToSqr(marker.point)<=49)) {
            if(game.items().magic().suppressedAt(marker.level,target.position())) continue;
            boolean enemy=game.items().wands().target(marker.owner,target);
            if(target instanceof ServerPlayer && !enemy) continue;
            Vec3 point=target.position().add(marker.point.subtract(target.position()).normalize().scale(.35));
            if(marker.level.hasChunkAt(BlockPos.containing(point))) {
                target.teleportTo(point.x,point.y,point.z);
                if(target instanceof LivingEntity living) game.items().indirectKills().displaced(living,marker.owner);
            }
            if(target instanceof LivingEntity living && enemy && damaged<5 && target.position().distanceToSqr(marker.point)<=16
                    && game.items().mana().score(target,"void_resistance").get()<=0) {
                living.hurtServer(marker.level,source,.01f); damaged++;
            }
        }
    }
    public void tick() {
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
        for(var seed:List.copyOf(seeds)) {
            if(!valid(seed) || game.items().magic().suppressedAt(seed.level,seed.point)) { seeds.remove(seed); continue; }
            if(paused(seed.owner)) continue;
            if(!seed.level.hasChunkAt(BlockPos.containing(seed.point))) continue;
            seed(seed); seeds.remove(seed);
        }
        for(var marker:List.copyOf(markers)) {
            if(!valid(marker) || game.items().magic().suppressedAt(marker.level,marker.point)) {
                if(marker.terrain!=null) marker.terrain.held=false;
                markers.remove(marker); continue;
            }
            if(paused(marker.owner) || !marker.level.hasChunkAt(BlockPos.containing(marker.point))) continue;
            Vec3 before=marker.point;
            if(marker.mode==4) hole(marker);
            else {
                Vec3 point=new Vec3(Math.floor(before.x)+marker.owner.getRandom().nextInt(9)-4,
                        Math.floor(before.y)+marker.owner.getRandom().nextInt(9)-4,Math.floor(before.z)+marker.owner.getRandom().nextInt(9)-4);
                seeds.add(new Seed(marker,point,randomPitch(marker.owner),randomYaw(marker.owner)));
            }
            if(marker.mode!=4) {
                marker.level.sendParticles(ParticleTypes.REVERSE_PORTAL,before.x,before.y,before.z,10,0,0,0,1);
                LbcOverload.sound(marker.owner,before,"minecraft:entity.bee.loop_aggressive",.1f,0,30);
            }
            marker.point=before.add(marker.direction.scale(marker.mode==2?2:marker.mode==4?.3:1));
            if(++marker.age>=marker.lifetime) {
                if(marker.mode==4) { fill(marker.terrain,before,4); marker.terrain.held=false; }
                markers.remove(marker);
            }
        }
    }
    public int count() { return markers.size()+seeds.size()+jobs.size(); }
    public void forget(ServerPlayer owner) {
        if(dividerTimers.remove(owner)!=null) game.items().mana().score(owner,"divider_cd").set(0);
        markers.removeIf(state->state.owner==owner); seeds.removeIf(state->state.owner==owner);
        for(var job:List.copyOf(jobs)) if(job.owner==owner) job.cancel();
    }
    public void stop() {
        markers.clear(); seeds.clear(); for(var job:List.copyOf(jobs)) job.cancel();
        for(var owner:dividerTimers.keySet()) game.items().mana().score(owner,"divider_cd").set(0);
        dividerTimers.clear();
    }
    public void transformOrigins(ServerLevel level,UnaryOperator<Vec3> transform) {
        for(var marker:markers) if(marker.level==level) {
            Vec3 point=transform.apply(marker.point);
            if(point==null) { marker.cancelled=true; if(marker.terrain!=null) marker.terrain.cancel(); }
            else marker.point=point;
        }
        for(var seed:seeds) if(seed.level==level) { Vec3 point=transform.apply(seed.point); if(point==null) seed.cancelled=true; else seed.point=point; }
        markers.removeIf(state->state.cancelled); seeds.removeIf(state->state.cancelled);
    }
    record MarkerView(ServerPlayer owner,String epoch,ServerLevel level,Vec3 point,int age,int lifetime,int mode) { }
    record SeedView(ServerPlayer owner,Vec3 point,boolean expand) { }
    List<MarkerView> markerSnapshot() { return markers.stream().map(state->new MarkerView(state.owner,state.epoch,state.level,state.point,state.age,state.lifetime,state.mode)).toList(); }
    List<SeedView> seedSnapshot() { return seeds.stream().map(state->new SeedView(state.owner,state.point,state.expand)).toList(); }
    int pendingWorkCount() { return (int)jobs.stream().filter(job->!job.held || !job.operations.isEmpty()).count(); }
}
