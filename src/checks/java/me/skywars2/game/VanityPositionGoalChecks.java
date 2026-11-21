package me.skywars2.game;

import java.lang.reflect.Field;
import java.util.*;

import me.skywars2.game.match.checkpoints.MatchActorCheckpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
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
        verifyRemoval(game,f,floor.offset(12,0,0));
    }
    private static void verifyRemoval(GameController game,WoolBucketChecks.Fixture f,BlockPos floor){
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
            var pos=floor.offset(x,0,z);f.set(pos,Blocks.STONE.defaultBlockState());
            for(int y=1;y<=4;y++)f.set(pos.above(y),Blocks.AIR.defaultBlockState());
        }
        var egg=floor.above();var eggState=Blocks.TURTLE_EGG.defaultBlockState();f.set(egg,eggState);
        var mob=new Cow(EntityTypes.COW,f.level);mob.setPos(Vec3.atBottomCenterOf(egg.above()));mob.setNoAi(true);mob.setNoGravity(true);mob.setOnGround(true);
        var selector=(GoalSelector)WoolBucketChecks.read(mob,"goalSelector");selector.removeAllGoals(g->true);
        ((GoalSelector)WoolBucketChecks.read(mob,"targetSelector")).removeAllGoals(g->true);
        require(f.level.addFreshEntity(mob),"registered native block-removal actor");f.own(mob);
        var goal=new RemoveBlockGoal(Blocks.TURTLE_EGG,mob,1,1);selector.addGoal(1,goal);
        var wrapper=selector.getAvailableGoals().iterator().next();
        var rules=f.level.getGameRules();boolean griefing=rules.get(GameRules.MOB_GRIEFING);
        try{
            rules.set(GameRules.MOB_GRIEFING,false,game.server());require(!goal.canUse(),"native block destruction rejects disabled mob griefing");
            rules.set(GameRules.MOB_GRIEFING,true,game.server());selector.tick();
            require(wrapper.isRunning()&&egg.equals(WoolBucketChecks.read(goal,"blockPos")),"native search and selector actually find and admit the turtle egg");
            for(int i=0;i<11;i++)selector.tick();
            int elapsed=(Integer)WoolBucketChecks.read(goal,"ticksSinceReachedGoal");
            require(elapsed==12&&Boolean.TRUE.equals(WoolBucketChecks.read(goal,"reachedTarget"))&&f.level.getBlockState(egg).is(Blocks.TURTLE_EGG),
                    "twelve real native goal ticks progress destruction while block still exists");
            var alias=WoolBucketChecks.read(goal,"blockPos");var motion=mob.getDeltaMovement();
            var image=MatchActorCheckpoint.captureRegistered(game);int remaining=removeEgg(f,selector,goal,egg);
            require(remaining==62-elapsed,"native removal happens only after the original sixty-one-step threshold");
            selector.tick();require(!wrapper.isRunning()&&mob.fallDistance==1,"native stop after destruction updates owner fall distance");
            for(int replay=0;replay<3;replay++){
                // Terrain is restored by its own owner before the actor phase, as in the full coordinator.
                f.set(egg,eggState);restore(game,image);
                require(wrapper.isRunning()&&WoolBucketChecks.read(goal,"blockPos")==alias&&mob.getDeltaMovement().equals(motion),
                        "same running native task, original search alias and saved movement restored");
                require((Integer)WoolBucketChecks.read(goal,"ticksSinceReachedGoal")==elapsed,
                        "saved destruction progress replaces future finished progress");
                require(removeEgg(f,selector,goal,egg)==remaining,"identical remaining native ticks until actual block removal after every replay");
                selector.tick();require(!wrapper.isRunning()&&mob.fallDistance==1,"native stop remains repeatable without replay callbacks");
            }
            var inactive=MatchActorCheckpoint.captureRegistered(game);f.set(egg,eggState);
            for(int i=0;i<220&&!wrapper.isRunning();i++)selector.tick();
            require(wrapper.isRunning()&&(Integer)WoolBucketChecks.read(goal,"ticksSinceReachedGoal")<62,
                    "real future native admission starts a new destruction cycle");
            f.set(egg,Blocks.AIR.defaultBlockState());restore(game,inactive);
            require(!wrapper.isRunning()&&(Integer)WoolBucketChecks.read(goal,"ticksSinceReachedGoal")==62&&f.level.getBlockState(egg).isAir(),
                    "inactive completed image retires a future cycle without restarting or recreating its block");
        }finally{rules.set(GameRules.MOB_GRIEFING,griefing,game.server());}
    }
    private static int removeEgg(WoolBucketChecks.Fixture f,GoalSelector selector,RemoveBlockGoal goal,BlockPos egg){
        int steps=0;while(f.level.getBlockState(egg).is(Blocks.TURTLE_EGG)&&steps<80){selector.tick();steps++;}
        require(f.level.getBlockState(egg).isAir()&&(Integer)WoolBucketChecks.read(goal,"ticksSinceReachedGoal")==62,
                "genuine native removal, not a test-side block edit or fabricated progress");return steps;
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
