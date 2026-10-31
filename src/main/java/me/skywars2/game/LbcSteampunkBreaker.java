package me.skywars2.game;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Actual block breaks and melee contacts; source fill-air-destroy geometry, not vein mining. */
public final class LbcSteampunkBreaker {
    private final GameController game;
    private int pending;
    public LbcSteampunkBreaker(GameController game){this.game=game;}
    public static boolean matches(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("steampunk_pickaxe",0)==1;}
    public int count(){return pending;}

    private boolean admitted(ServerPlayer p){return p!=null&&p.isAlive()&&!p.isRemoved()
        &&p.level()==game.maps().level()&&game.playing()&&game.stage()!=GameController.Stage.ENDING
        &&!p.isSpectator()&&game.canUseGameplayItems(p)&&!game.isObserver(p)&&LbcDimensionItems.ordinary(p)
        &&!game.items().actionsBlocked(p)&&!game.items().wands().inputTimeStopped(p)
        &&!game.items().timeStop().stopped(p.level())&&!p.entityTags().contains("time_stopped")
        &&!game.server().tickRateManager().isFrozen()&&game.server().tickRateManager().runsNormally();}
    public void used(ServerPlayer p,ItemStack sourceTool){
        if(!matches(sourceTool)||!admitted(p))return;
        // Draw once per actual break or melee contact, before the source TECH gate.
        if(p.getRandom().nextFloat()>=.25f||game.items().magic().mechanismSuppressed(p))return;
        var job=new BreakWork(p,cells(p.getEyePosition(),Vec3.directionFromRotation(p.getXRot(),p.getYRot())));
        pending++;game.items().stonekivi().queuedWork(job);
    }
    static List<BlockPos> cells(Vec3 eyes,Vec3 direction){
        var unique=new LinkedHashSet<BlockPos>();
        for(int distance=1;distance<=17;distance++){
            var center=BlockPos.containing(eyes.add(direction.scale(distance)));
            // /fill traverses x fastest, then y, then z; first visit retains that native order.
            for(var pos:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,1,1)))unique.add(pos.immutable());
        }
        return List.copyOf(unique);
    }
    private final class BreakWork implements LbcStonekivi.Work {
        final ServerPlayer owner;final ServerLevel level;final String epoch;final List<BlockPos> cells;
        final LbcIndirectKills.Owner cause;
        int cursor;boolean finished;CompletableFuture<?> loading;
        BreakWork(ServerPlayer owner,List<BlockPos> cells){this.owner=owner;level=owner.level();epoch=game.items().epoch();this.cells=cells;
            cause=game.items().indirectKills().coldCause(owner,false);}
        boolean valid(){return epoch.equals(game.items().epoch())&&game.playing()&&game.stage()!=GameController.Stage.ENDING
                &&owner.level()==level&&!owner.isRemoved()&&owner.isAlive()&&game.canUseGameplayItems(owner)
                &&!game.isObserver(owner)&&LbcDimensionItems.ordinary(owner)&&!game.items().magic().mechanismSuppressed(owner);}
        public boolean ready(){return !valid()||loading==null||loading.isDone();}
        public boolean step(){
            var shared=game.items().stonekivi();shared.beginNativeWork();try{
                if(finished)return true;
                if(!valid()){cancel();return true;}
                if(game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally()||game.items().timeStop().stopped(level)
                        ||game.items().wands().inputTimeStopped(owner)||game.items().actionsBlocked(owner))return false;
                if(loading!=null&&loading.isCompletedExceptionally()){cancel();return true;}
                long deadline=EffectTerrain.deadlineNanos();
                game.items().indirectKills().trapChanges(level,cause,()->{
                    int visits=0;
                    while(cursor<cells.size()){
                        if((visits&15)==0&&System.nanoTime()>=deadline)return;
                        var pos=cells.get(cursor);
                        if(!level.isInsideBuildHeight(pos.getY())){cursor++;visits++;continue;}
                        if(!level.hasChunkAt(pos)){
                            if(loading==null)loading=game.maps().prepareChunk(level,new ChunkPos(pos.getX()>>4,pos.getZ()>>4));
                            return;
                        }
                        loading=null;cursor++;visits++;
                        // No held-tool loot context: /fill destroy, including bedrock and containers.
                        level.destroyBlock(pos,true);
                        if(!level.getBlockState(pos).isAir())level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
                    }
                });
                if(cursor==cells.size()){cancel();return true;}return false;
            }finally{shared.endNativeWork();}
        }
        public void cancel(){if(!finished){finished=true;pending--;}}
        // Default snapshotIdle=false keeps partial accepted writes outside Vanity capture.
    }
}
