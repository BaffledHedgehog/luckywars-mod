package me.skywars2.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Source midasfr: six-neighbour non-air wave, blocks only, Euclidean radius 20.
 * Draft: must be adopted with the existing queued-work coordinator and a real Policy. */
public final class LbcMidas {
    public interface Policy {
        /** Source stopper_magic uses a 20-block AXIS ALIGNED box, not spherical distance. */
        boolean magicStopped(ServerLevel level,BlockPos pos);
        boolean admittedCell(ServerPlayer author,ServerLevel level,BlockPos pos);
        boolean write(ServerPlayer author,ServerLevel level,BlockPos pos,BlockState state,LbcIndirectKills.Owner cause);
        /** Match phase admission, including source ENDING celebration phase. */
        boolean active(String epoch);
    }
    private final GameController game;private final Policy policy;
    private Set<Wave> waves=new LinkedHashSet<>();private boolean restoring;
    public LbcMidas(GameController game,Policy policy){this.game=game;this.policy=Objects.requireNonNull(policy);}
    public void launch(ServerPlayer author,ServerLevel level,BlockPos origin,LbcIndirectKills.Owner cause){
        if(restoring)throw new IllegalStateException("Midas publication during restore");
        var wave=new Wave(author,level,origin.immutable(),cause);waves.add(wave);game.items().stonekivi().queuedWork(wave);
    }
    private final class Wave implements LbcStonekivi.Work {
        ServerPlayer author;final ServerLevel level;final BlockPos origin;final LbcIndirectKills.Owner cause;final String epoch;
        ArrayDeque<BlockPos> layer=new ArrayDeque<>(),next=new ArrayDeque<>();Set<Long> visited=new HashSet<>();
        int layerTick;boolean closed;
        Wave(ServerPlayer author,ServerLevel level,BlockPos origin,LbcIndirectKills.Owner cause){
            this.author=author;this.level=level;this.origin=origin;this.cause=cause;epoch=game.items().epoch();
            layer.add(origin);visited.add(origin.asLong());layerTick=game.server().getTickCount();
        }
        public boolean ready(){return !restoring&&(closed||!game.items().timeStop().stopped(level)&&game.server().getTickCount()>=layerTick);}
        /** Only called from the source golden-sword celebration. Existing queue needs this capability. */
        public boolean postgame(){return true;}
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
                    var pos=layer.peekFirst();if(!loaded(pos))return false;
                    // An atomic seven-cell quantum prevents partially expanded cells on snapshot.
                    for(var direction:Direction.values()){var neighbour=pos.relative(direction);if(within(neighbour)&&!loaded(neighbour))return false;}
                    layer.removeFirst();
                    if(!within(pos)||policy.magicStopped(level,pos))continue;
                    var state=level.getBlockState(pos);
                    if(!state.isAir()&&!state.is(Blocks.RAW_GOLD_BLOCK)&&policy.admittedCell(author,level,pos))
                        policy.write(author,level,pos,Blocks.RAW_GOLD_BLOCK.defaultBlockState(),cause);
                    // The source origin expands even if it is air; only neighbours require non-air.
                    for(var direction:Direction.values()){
                        var neighbour=pos.relative(direction);long key=neighbour.asLong();
                        if(within(neighbour)&&!visited.contains(key)&&!policy.magicStopped(level,neighbour)&&!level.getBlockState(neighbour).isAir()){
                            visited.add(key);next.addLast(neighbour.immutable());
                        }
                    }
                }
                if(layer.isEmpty()){
                    if(next.isEmpty()){cancel();return true;}
                    layer=next;next=new ArrayDeque<>();layerTick=game.server().getTickCount()+1;
                }
                return false;
            }finally{game.items().stonekivi().endNativeWork();}
        }
        boolean within(BlockPos pos){return level.isInValidBounds(pos)&&pos.distSqr(origin)<=400;}
        public void cancel(){closed=true;layer.clear();next.clear();visited.clear();waves.remove(this);}
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            var owner=graph.links.key(author);var current=List.copyOf(layer);var following=List.copyOf(next);var seen=Set.copyOf(visited);
            boolean wasClosed=closed;var clock=new MatchAuthorityCheckpoint.Clock(game,layerTick,0);
            return links->{MatchAuthorityCheckpoint.world(game,level);var rebound=(ServerPlayer)links.get(owner);
                return ()->{author=rebound;layer=new ArrayDeque<>(current);next=new ArrayDeque<>(following);visited=new HashSet<>(seen);closed=wasClosed;layerTick=clock.last(game);};};
        }
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
