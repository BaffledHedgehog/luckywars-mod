package me.skywars2.game;

import java.util.*;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/** Shared source Midas wave; the cosmetic entry retains blocks-only radius 20. */
public final class LbcMidas {
    public interface Policy {
        /** Source stopper_magic uses a 20-block AXIS ALIGNED box, not spherical distance. */
        boolean magicStopped(ServerLevel level,BlockPos pos);
        boolean admittedCell(ServerPlayer author,ServerLevel level,BlockPos pos);
        boolean write(ServerPlayer author,ServerLevel level,BlockPos pos,BlockState state,LbcIndirectKills.Owner cause);
        /** Match phase admission, including source ENDING celebration phase. */
        boolean active(String epoch);
        /** Native integration owns allegiance, protected roles and composite actor admission. */
        default boolean admittedEntity(ServerPlayer author,Entity target){return false;}
        /** Override for an actor's terminal lifecycle (for example Chef), before its body is filled. */
        default void affectEntity(ServerPlayer author,Entity target,LbcIndirectKills.Owner cause){
            if(target instanceof ServerPlayer p&&p.isSpectator())return;
            if(target instanceof LivingEntity living)living.setHealth(0);else target.discard();
        }
    }
    private final GameController game;private final Policy policy;
    private Set<Wave> waves=new LinkedHashSet<>();private boolean restoring;
    /** Read-only native verification view; no world scans or queue mutations. */
    public record WaveView(String epoch,BlockPos origin,boolean affectEntities,double radiusSquared,int layerCells,int nextCells,int visitedCells,int layerTick,BlockPos pendingCell,int pendingTargets,BlockPos bodyMin,BlockPos bodyMax,BlockPos bodyNext){}
    public List<WaveView> views(){return waves.stream().map(wave->new WaveView(wave.epoch,wave.origin,wave.affectEntities,wave.radiusSquared,wave.layer.size(),wave.next.size(),wave.visited.size(),wave.layerTick,wave.pendingCell,wave.pendingTargets.size(),wave.body==null?null:new BlockPos(wave.body.minX,wave.body.minY,wave.body.minZ),wave.body==null?null:new BlockPos(wave.body.maxX,wave.body.maxY,wave.body.maxZ),wave.body==null?null:wave.body.pos())).toList();}
    public LbcMidas(GameController game,Policy policy){this.game=game;this.policy=Objects.requireNonNull(policy);}
    public void launch(ServerPlayer author,ServerLevel level,BlockPos origin,LbcIndirectKills.Owner cause){
        launch(author,level,origin,cause,false,20);
    }
    /** Negative spread distance is the source unlimited connected non-air mode. */
    public void launch(ServerPlayer author,ServerLevel level,BlockPos origin,LbcIndirectKills.Owner cause,boolean affectEntities,int maxSpreadDistance){
        launch(author,level,origin,cause,affectEntities,maxSpreadDistance,policy);
    }
    /** A second producer may share this checkpointed owner while retaining its own source policy. */
    public void launch(ServerPlayer author,ServerLevel level,BlockPos origin,LbcIndirectKills.Owner cause,boolean affectEntities,int maxSpreadDistance,Policy wavePolicy){
        if(restoring)throw new IllegalStateException("Midas publication during restore");
        var wave=new Wave(author,level,origin.immutable(),cause,affectEntities,maxSpreadDistance,Objects.requireNonNull(wavePolicy));waves.add(wave);game.items().stonekivi().queuedWork(wave);
    }
    private final class Wave implements LbcStonekivi.Work {
        ServerPlayer author;final ServerLevel level;final BlockPos origin;final LbcIndirectKills.Owner cause;final String epoch;
        ArrayDeque<BlockPos> layer=new ArrayDeque<>(),next=new ArrayDeque<>();LongOpenHashSet visited=new LongOpenHashSet();
        int layerTick;boolean closed;
        final boolean affectEntities;final double radiusSquared;final Policy policy;
        BlockPos pendingCell;ArrayDeque<Entity> pendingTargets=new ArrayDeque<>();BodyFill body;
        Wave(ServerPlayer author,ServerLevel level,BlockPos origin,LbcIndirectKills.Owner cause,boolean affectEntities,int maxSpreadDistance,Policy policy){
            this.author=author;this.level=level;this.origin=origin;this.cause=cause;epoch=game.items().epoch();
            this.policy=policy;this.affectEntities=affectEntities;radiusSquared=maxSpreadDistance<0?-1:(double)maxSpreadDistance*maxSpreadDistance;
            layer.add(origin);visited.add(origin.asLong());layerTick=game.server().getTickCount();
        }
        public boolean ready(){return !restoring&&(closed||!game.items().timeStop().stopped(level)&&game.server().getTickCount()>=layerTick);}
        /** Only called from the source golden-sword celebration. Existing queue needs this capability. */
        public boolean postgame(){return !affectEntities;}
        public boolean snapshotIdle(){return true;}
        private boolean loaded(BlockPos pos){
            if(level.hasChunkAt(pos))return true;game.maps().prepareChunk(level,new ChunkPos(pos.getX()>>4,pos.getZ()>>4));return false;
        }
        public boolean step(){
            if(restoring)return false;game.items().stonekivi().beginNativeWork();
            try{
                if(closed)return true;if(!policy.active(epoch)){cancel();return true;}if(!ready())return false;
                int budget=128;long deadline=Math.min(EffectTerrain.deadlineNanos(),System.nanoTime()+350_000);
                while(!layer.isEmpty()&&budget-->0&&System.nanoTime()<deadline){
                    if(affectEntities&&pendingCell!=null){
                        if(body!=null){
                            var fill=body.pos();
                            if(level.isInValidBounds(fill)){
                                if(!loaded(fill))return false;
                                if(!policy.magicStopped(level,fill)&&policy.admittedCell(author,level,fill))policy.write(author,level,fill,Blocks.RAW_GOLD_BLOCK.defaultBlockState(),cause);
                            }
                            if(!body.advance())body=null;continue;
                        }
                        if(!pendingTargets.isEmpty()){
                            var target=pendingTargets.removeFirst();
                            if(!target.isRemoved()&&target.level()==level&&policy.admittedEntity(author,target)){
                                body=BodyFill.of(target.getBoundingBox(),level);
                                policy.affectEntity(author,target,cause);
                            }
                            continue;
                        }
                        // All entity body cells precede source neighbour discovery.
                        for(var direction:Direction.values())if(within(pendingCell.relative(direction))&&!loaded(pendingCell.relative(direction)))return false;
                        expand(pendingCell);layer.removeFirst();pendingCell=null;continue;
                    }
                    var pos=layer.peekFirst();if(!loaded(pos))return false;
                    // An atomic seven-cell quantum prevents partially expanded cells on snapshot.
                    for(var direction:Direction.values()){var neighbour=pos.relative(direction);if(within(neighbour)&&!loaded(neighbour))return false;}
                    if(!within(pos)||policy.magicStopped(level,pos)){layer.removeFirst();continue;}
                    var state=level.getBlockState(pos);
                    if(!state.isAir()&&!state.is(Blocks.RAW_GOLD_BLOCK)&&policy.admittedCell(author,level,pos))
                        policy.write(author,level,pos,Blocks.RAW_GOLD_BLOCK.defaultBlockState(),cause);
                    if(affectEntities){
                        pendingCell=pos;
                        double x=pos.getX(),y=pos.getY(),z=pos.getZ();
                        var query=new AABB(x+.5-1.2,y+1.5-2.5,z+.5-1.2,x+.5+1.2,y+1.5+2.5,z+.5+1.2);
                        var volume=new AABB(x,y+1,z,x+1,y+3,z+1);
                        pendingTargets.addAll(level.getEntities((Entity)null,query,target->target.getBoundingBox().intersects(volume)));
                        continue;
                    }
                    layer.removeFirst();expand(pos);
                }
                if(layer.isEmpty()){
                    if(next.isEmpty()){cancel();return true;}
                    layer=next;next=new ArrayDeque<>();layerTick=game.server().getTickCount()+1;
                }
                return false;
            }finally{game.items().stonekivi().endNativeWork();}
        }
        private void expand(BlockPos pos){
            // The source origin expands even if it is air; only neighbours require non-air.
            for(var direction:Direction.values()){
                var neighbour=pos.relative(direction);long key=neighbour.asLong();
                if(within(neighbour)&&!visited.contains(key)&&!policy.magicStopped(level,neighbour)&&!level.getBlockState(neighbour).isAir()){
                    visited.add(key);next.addLast(neighbour.immutable());
                }
            }
        }
        boolean within(BlockPos pos){return level.isInValidBounds(pos)&&(radiusSquared<0||pos.distSqr(origin)<=radiusSquared);}
        public void cancel(){closed=true;layer.clear();next.clear();visited.clear();pendingTargets.clear();pendingCell=null;body=null;waves.remove(this);}
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            var owner=graph.links.key(author);var current=List.copyOf(layer);var following=List.copyOf(next);var seen=visited.clone();
            boolean wasClosed=closed;var clock=new MatchAuthorityCheckpoint.Clock(game,layerTick,0);
            var targets=pendingTargets.stream().map(graph.links::key).toList();var cell=pendingCell;var fill=body==null?null:body.copy();
            return links->{MatchAuthorityCheckpoint.world(game,level);var rebound=(ServerPlayer)links.get(owner);
                var reboundTargets=targets.stream().map(links::get).toList();
                return ()->{author=rebound;layer=new ArrayDeque<>(current);next=new ArrayDeque<>(following);visited=seen.clone();closed=wasClosed;layerTick=clock.last(game);pendingCell=cell;pendingTargets=new ArrayDeque<>(reboundTargets);body=fill==null?null:fill.copy();};};
        }
    }
    /** A partially written body is typed queue state, independent of the removed original actor. */
    private static final class BodyFill {
        final int minX,minY,minZ,maxX,maxY,maxZ;int x,y,z;
        BodyFill(int minX,int minY,int minZ,int maxX,int maxY,int maxZ){this.minX=minX;this.minY=minY;this.minZ=minZ;this.maxX=maxX;this.maxY=maxY;this.maxZ=maxZ;x=minX;y=minY;z=minZ;}
        static BodyFill of(AABB box,ServerLevel level){
            int minX=(int)Math.floor(box.minX),minY=Math.max((int)Math.floor(box.minY),level.getMinY()),minZ=(int)Math.floor(box.minZ);
            int maxX=(int)Math.ceil(box.maxX)-1,maxY=Math.min((int)Math.ceil(box.maxY)-1,level.getMaxY()-1),maxZ=(int)Math.ceil(box.maxZ)-1;
            return minX>maxX||minY>maxY||minZ>maxZ?null:new BodyFill(minX,minY,minZ,maxX,maxY,maxZ);
        }
        BlockPos pos(){return new BlockPos(x,y,z);}
        boolean advance(){if(z<maxZ){z++;return true;}z=minZ;if(y<maxY){y++;return true;}y=minY;if(x<maxX){x++;return true;}return false;}
        BodyFill copy(){var copy=new BodyFill(minX,minY,minZ,maxX,maxY,maxZ);copy.x=x;copy.y=y;copy.z=z;return copy;}
    }
    public void stop(){for(var wave:List.copyOf(waves))wave.cancel();}
    void prepareFrozen(){restoring=true;waves=new LinkedHashSet<>();}
    void publishFrozen(){restoring=false;}
    Checkpoint checkpoint(MatchQueuedWorkCheckpoint.Graph graph){return new Checkpoint(graph);}
    final class Checkpoint {
        private final Set<Wave> saved;
        Checkpoint(MatchQueuedWorkCheckpoint.Graph graph){saved=new LinkedHashSet<>(waves);saved.forEach(graph::capture);}
        Runnable prepare(){return ()->waves=new LinkedHashSet<>(saved);}
    }
}
