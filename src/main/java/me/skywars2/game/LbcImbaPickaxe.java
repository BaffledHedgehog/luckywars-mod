package me.skywars2.game;

import java.util.ArrayDeque;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Actual broken-block identity seeds resident 26-connected mining through shared native harvest. */
public final class LbcImbaPickaxe {
    private static final int CELLS_PER_STEP=64,OUTPUTS_PER_STEP=4;
    // Authored neighbor order; the shared dispatcher replaces recursive command depth with a finite quantum.
    private static final int[][] NEIGHBORS={{1,0,0},{1,0,-1},{1,0,1},{1,1,0},{1,1,-1},{1,1,1},{1,-1,0},{1,-1,1},{1,-1,-1},
            {-1,0,0},{-1,0,-1},{-1,0,1},{-1,1,0},{-1,1,-1},{-1,1,1},{-1,-1,0},{-1,-1,1},{-1,-1,-1},
            {0,0,-1},{0,0,1},{0,1,0},{0,1,-1},{0,1,1},{0,-1,0},{0,-1,1},{0,-1,-1}};
    public record Progress(UUID author,Block block,BlockPos broken,int frontier,int visited,int pendingOutputs,long mined,long published){}
    private record Drop(BlockPos at,ItemStack stack){}
    private final GameController game;
    public LbcImbaPickaxe(GameController game){this.game=game;}
    public static boolean matches(ItemStack stack){return !stack.isEmpty()&&ItemData.intValue(stack,"chaos_hoe",0)==1;}
    public List<Progress> snapshot(){return game.items().stonekivi().queuedSnapshot().stream().filter(Mining.class::isInstance)
            .map(Mining.class::cast).map(Mining::view).toList();}
    private boolean live(ServerLevel level,String epoch){return game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&!game.maps().busy()&&game.items().currentEpoch(epoch)&&level==game.maps().level()
            &&LbcDimensionItems.ordinary(level)&&LbcBedwarsRoleGuards.publication(level);}
    private boolean paused(ServerLevel level){return game.items().timeStop().stopped(level)||game.items().wands().timeStopped()
            ||game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally();}
    /** The caller owns the successful root break, its one wear event and its normal native drops. */
    public boolean mined(ServerPlayer author,ItemStack actualTool,BlockPos broken,BlockState before){
        if(!game.server().isSameThread())throw new IllegalStateException("Connected mining is server-thread only");
        if(author==null||!matches(actualTool)||before.isAir()||!live(author.level(),game.items().epoch())||paused(author.level())
                ||!author.isAlive()||author.isRemoved()||author.isSpectator()||game.isObserver(author)
                ||!game.canUseGameplayItems(author)||game.awaitingRespawn(author.getUUID())||game.items().actionsBlocked(author)
                ||game.items().wands().inputTimeStopped(author)||!game.items().wands().validRoleTarget(author,false))return false;
        game.items().stonekivi().queuedWork(new Mining(author,actualTool,broken,before.getBlock()));return true;
    }
    private boolean resident(ServerLevel level,BlockPos at){return level.isInValidBounds(at)
            &&level.getChunkSource().getChunkNow(at.getX()>>4,at.getZ()>>4)!=null;}
    /** A read-only primitive FIFO image; no rotation, boxing, world discovery or reflective capture. */
    private static final class Frontier extends LongArrayFIFOQueue {
        long[] image(){var values=new long[size()];int cursor=start;
            for(int i=0;i<values.length;i++){values[i]=array[cursor];if(++cursor==length)cursor=0;}return values;}
    }
    private final class Mining implements LbcStonekivi.Work {
        ServerPlayer author;
        final UUID owner;
        final ServerLevel level;
        final String epoch;
        final LbcIndirectKills.Owner cause;
        final ItemStack tool;
        final Block block;
        final BlockPos broken;
        final Predicate<BlockState> eligible;
        final Frontier frontier=new Frontier();
        LongOpenHashSet visited=new LongOpenHashSet();
        final ArrayDeque<Drop> output=new ArrayDeque<>();
        long removed,published;
        boolean cancelled,stepping;
        Mining(ServerPlayer author,ItemStack tool,BlockPos broken,Block block){
            this.author=author;owner=author.getUUID();level=author.level();epoch=game.items().epoch();
            cause=game.items().indirectKills().coldCause(author,false);this.tool=tool.copy();this.block=block;this.broken=broken.immutable();
            eligible=state->state.is(block);visited.add(broken.asLong());neighbors(broken);
        }
        private void neighbors(BlockPos at){for(var offset:NEIGHBORS){long packed=BlockPos.asLong(at.getX()+offset[0],at.getY()+offset[1],at.getZ()+offset[2]);
            if(visited.add(packed))frontier.enqueue(packed);}}
        Progress view(){return new Progress(owner,block,broken,frontier.size(),visited.size(),output.size(),removed,published);}
        @Override public boolean ready(){return cancelled||!live(level,epoch)||!paused(level)
                &&(output.isEmpty()||resident(level,output.peekFirst().at()));}
        @Override public boolean snapshotIdle(){return !stepping;}
        @Override public boolean step(){
            if(stepping)throw new IllegalStateException("Reentrant connected mining quantum");
            if(cancelled||!live(level,epoch)){cancel();return true;}
            if(paused(level))return false;
            stepping=true;
            try{
                int emitted=0,cells=0;
                while(!cancelled&&live(level,epoch)&&!paused(level)){
                    if(!output.isEmpty()){
                        if(emitted>=OUTPUTS_PER_STEP)return false;
                        var drop=output.peekFirst();if(!resident(level,drop.at()))return false;output.removeFirst();
                        // Publication uses the ordinary native world-drop path, never inventory delivery or a reroll.
                        game.items().indirectKills().capturedWorldChanges(level,cause,()->Block.popResource(level,drop.at(),drop.stack()));
                        published++;emitted++;continue;
                    }
                    if(frontier.isEmpty()){cancel();return true;}
                    if(cells++>=CELLS_PER_STEP)return false;
                    var at=BlockPos.of(frontier.dequeueLong());
                    // A missing/protected/replaced cell terminates this branch; it cannot hold a zombie job or load terrain.
                    if(!resident(level,at))continue;
                    var state=level.getBlockState(at);if(!eligible.test(state))continue;
                    var stacks=game.items().imbaAxe().harvest(author,level,at,state,tool,cause,eligible);
                    if(stacks==null)continue;
                    if(cancelled)return true;
                    removed++;neighbors(at);
                    for(var stack:stacks)if(!stack.isEmpty())output.addLast(new Drop(at,stack));
                }
                if(cancelled||!live(level,epoch)){cancel();return true;}return false;
            }finally{stepping=false;}
        }
        @Override public void cancel(){cancelled=true;frontier.clear();output.clear();visited.clear();}
        @Override public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            game.items().stonekivi().checkpointBoundary();var authorRef=graph.links.key(author);
            var savedFrontier=frontier.image();var savedVisited=visited.clone();var savedOutput=copy(output);
            long savedRemoved=removed,savedPublished=published;boolean savedCancelled=cancelled;
            return links->{game.items().stonekivi().checkpointBoundary();MatchAuthorityCheckpoint.world(game,level);
                var rebound=(ServerPlayer)links.get(authorRef);
                return ()->{author=rebound;frontier.clear();for(long cell:savedFrontier)frontier.enqueue(cell);
                    visited=savedVisited.clone();output.clear();output.addAll(copy(savedOutput));
                    removed=savedRemoved;published=savedPublished;cancelled=savedCancelled;stepping=false;};
            };
        }
    }
    private static List<Drop> copy(Iterable<Drop> source){var result=new java.util.ArrayList<Drop>();
        for(var drop:source)result.add(new Drop(drop.at(),drop.stack().copy()));return List.copyOf(result);}
}
