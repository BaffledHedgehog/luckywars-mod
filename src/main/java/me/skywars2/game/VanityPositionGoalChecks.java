package me.skywars2.game;

import java.lang.reflect.Field;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Native block search returns a mutable position; its original alias and history containers must rewind. */
final class VanityPositionGoalChecks {
    private VanityPositionGoalChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity position goals: "+why);}
    private static void restore(GameController game,MatchActorCheckpoint image){
        boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
        try{image.restoreBodiesFrozen();require(image.restoreLinksFrozen().isEmpty(),"block-task references resolve");}finally{game.server().tickRateManager().setFrozen(frozen);}
    }
    @SuppressWarnings("unchecked") static void verify(GameController game,WoolBucketChecks.Fixture f){
        BlockPos floor=BlockPos.containing(f.home).above(3).offset(0,0,14);
        for(int x=0;x<=8;x++)for(int z=-1;z<=1;z++){
            var pos=floor.offset(x,0,z);f.set(pos,Blocks.STONE.defaultBlockState());for(int y=1;y<=3;y++)f.set(pos.above(y),Blocks.AIR.defaultBlockState());
        }
        var first=floor.offset(4,0,0);var second=floor.offset(6,0,0);f.set(first,Blocks.DIAMOND_BLOCK.defaultBlockState());
        var mob=new Cow(EntityTypes.COW,f.level);mob.setPos(Vec3.atBottomCenterOf(floor.above()));mob.setNoAi(true);mob.setNoGravity(true);mob.setOnGround(true);
        var selector=(GoalSelector)WoolBucketChecks.read(mob,"goalSelector");selector.removeAllGoals(g->true);
        ((GoalSelector)WoolBucketChecks.read(mob,"targetSelector")).removeAllGoals(g->true);
        require(f.level.addFreshEntity(mob),"ordinary native block-search actor");f.own(mob);
        var goal=new MarkerGoal(mob);selector.addGoal(1,goal);selector.tick();
        require(selector.getAvailableGoals().iterator().next().isRunning()&&goal.view() instanceof BlockPos.MutableBlockPos&&goal.view().equals(first),"actual native search retained its mutable search result");
        var alias=(BlockPos.MutableBlockPos)goal.view();int tried=(Integer)WoolBucketChecks.read(goal,"tryTicks");var path=mob.getNavigation().getPath();
        var image=MatchActorCheckpoint.captureRegistered(game);
        for(int replay=0;replay<2;replay++){
            alias.set(second);f.set(first,Blocks.STONE.defaultBlockState());f.set(second,Blocks.DIAMOND_BLOCK.defaultBlockState());
            boolean found=false;for(int i=0;i<500;i++)if(goal.canUse()){found=true;break;}
            require(found&&goal.view()!=alias&&goal.view().equals(second),"future native search replaces the field while the earlier alias is also mutated");goal.start();goal.tick();
            // Terrain belongs to a separate owner; establish its saved state before testing the actor phase.
            f.set(first,Blocks.DIAMOND_BLOCK.defaultBlockState());f.set(second,Blocks.STONE.defaultBlockState());restore(game,image);
            require(goal.view()==alias&&alias.equals(first)&&mob.getNavigation().getPath()==path,"same original mutable position and route, not a replacement position");
            require((Integer)WoolBucketChecks.read(goal,"tryTicks")==tried&&goal.canContinueToUse(),"saved native block-approach phase resumes");
        }
        selector.removeAllGoals(g->true);mob.getNavigation().stop();
        var village=new MoveThroughVillageGoal(mob,1,false,1,()->true);selector.addGoal(1,village);
        var history=(List<BlockPos>)WoolBucketChecks.read(village,"visited");
        Field poi;try{poi=MoveThroughVillageGoal.class.getDeclaredField("poiPos");poi.setAccessible(true);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        append(village,poi,first);append(village,poi,second);var saved=List.copyOf(history);var historyImage=MatchActorCheckpoint.captureRegistered(game);
        for(int replay=0;replay<2;replay++){
            for(int i=0;i<16;i++)append(village,poi,floor.offset(i,0,1));int size=history.size();village.canUse();
            require(history.size()==size-1,"actual native village admission trims old history even without a nearby village");
            restore(game,historyImage);require(WoolBucketChecks.read(village,"visited")==history&&history.equals(saved),"same history list and saved entries survive future pruning and repeated replay");
        }
    }
    private static void append(MoveThroughVillageGoal goal,Field poi,BlockPos pos){
        try{poi.set(goal,pos);}catch(IllegalAccessException e){throw new IllegalStateException(e);}goal.stop();
    }
    private static final class MarkerGoal extends MoveToBlockGoal {
        MarkerGoal(PathfinderMob mob){super(mob,1,8);}
        @Override protected boolean isValidTarget(LevelReader level,BlockPos pos){return level.getBlockState(pos).is(Blocks.DIAMOND_BLOCK);}
        BlockPos view(){return blockPos;}
    }
}
