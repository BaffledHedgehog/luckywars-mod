package me.skywars2.game;

import me.skywars2.game.match.checkpoints.MatchActorCheckpoint;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/** Native task lifecycle/expiry/navigation probes, without another movement implementation. */
final class VanityMovementGoalChecks {
    private VanityMovementGoalChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity movement goals: "+why);}
    private static int count(Object goal,String field){return (Integer)WoolBucketChecks.read(goal,field);}
    private static GoalSelector selector(Mob mob){return (GoalSelector)WoolBucketChecks.read(mob,"goalSelector");}
    private static WrappedGoal install(Mob mob,Goal goal){
        var selector=selector(mob);selector.removeAllGoals(g->true);selector.addGoal(1,goal);
        for(int i=0;i<1024;i++){mob.getSensing().tick();selector.tick();if(selector.getAvailableGoals().iterator().next().isRunning())break;}
        var wrapper=selector.getAvailableGoals().iterator().next();require(wrapper.isRunning(),"native admission: "+goal.getClass().getSimpleName());return wrapper;
    }
    private static <T extends Mob>T add(WoolBucketChecks.Fixture f,T mob,Vec3 at){
        mob.setPos(at);mob.setNoAi(true);mob.setNoGravity(true);mob.setOnGround(true);
        selector(mob).removeAllGoals(g->true);((GoalSelector)WoolBucketChecks.read(mob,"targetSelector")).removeAllGoals(g->true);
        require(f.level.addFreshEntity(mob),"ordinary native actor");f.own(mob);return mob;
    }
    private static void restore(GameController game,MatchActorCheckpoint image){
        boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
        try{image.restoreBodiesFrozen();require(image.restoreLinksFrozen().isEmpty(),"all native movement references resolve");}
        finally{game.server().tickRateManager().setFrozen(frozen);}
    }
    static void verify(GameController game,WoolBucketChecks.Fixture f){
        Vec3 at=Vec3.atCenterOf(net.minecraft.core.BlockPos.containing(f.home).above(3)).add(0,.5,0);
        f.move(f.owner,at.add(2,0,0));f.owner.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.WHEAT));
        var observer=add(f,new Cow(EntityTypes.COW,f.level),at.add(9,0,1));
        var look=new LookAtPlayerGoal(observer,Cow.class,16,1);var lookWrapper=install(observer,look);
        Entity watched=(Entity)WoolBucketChecks.read(look,"lookAt");int lookTicks=count(look,"lookTime");
        var lookImage=MatchActorCheckpoint.captureRegistered(game);
        int expiry=expire(look);require(expiry==lookTicks,"native look duration really expires");lookWrapper.stop();
        for(int i=0;i<2;i++){
            restore(game,lookImage);require(WoolBucketChecks.read(look,"lookAt")==watched,"look restores actual target before continuation");
            require(expire(look)==expiry,"look expires after the same remaining native steps");lookWrapper.stop();
        }
        var random=new RandomLookAroundGoal(observer);var randomWrapper=install(observer,random);
        var randomImage=MatchActorCheckpoint.captureRegistered(game);double x=(Double)WoolBucketChecks.read(random,"relX"),z=(Double)WoolBucketChecks.read(random,"relZ");
        int randomExpiry=expire(random);randomWrapper.stop();random.start();
        for(int i=0;i<2;i++){
            restore(game,randomImage);random.tick();
            require(observer.getLookControl().getWantedX()==observer.getX()+x&&observer.getLookControl().getWantedZ()==observer.getZ()+z,"restored native random look keeps its direction");
            require(expire(random)+1==randomExpiry,"random look keeps remaining duration");randomWrapper.stop();
        }
        var child=add(f,new Cow(EntityTypes.COW,f.level),at.add(0,0,1));child.setBaby(true);
        var parentGoal=new FollowParentGoal(child,1);var parentWrapper=install(child,parentGoal);
        Entity parent=(Entity)WoolBucketChecks.read(parentGoal,"parent");Path parentPath=child.getNavigation().getPath();
        int parentDelay=count(parentGoal,"timeToRecalcPath");require(parentPath!=null&&parentGoal.canContinueToUse(),"native child follows a real adult");
        var parentImage=MatchActorCheckpoint.captureRegistered(game);parentGoal.tick();parentGoal.tick();parentWrapper.stop();child.getNavigation().stop();
        require(count(parentGoal,"timeToRecalcPath")==parentDelay-2,"future native parent ticks really advanced the saved countdown");
        restore(game,parentImage);parentGoal.tick();
        require(WoolBucketChecks.read(parentGoal,"parent")==parent&&child.getNavigation().getPath()==parentPath&&count(parentGoal,"timeToRecalcPath")==parentDelay-1,"parent/path/cooldown resume without starting a fresh route");
        parentWrapper.stop();

        var wolf=add(f,new Wolf(EntityTypes.WOLF,f.level),at.add(7,0,-1));wolf.tame(f.owner);wolf.setOrderedToSit(false);wolf.setPathfindingMalus(PathType.WATER,6);
        var ownerGoal=new FollowOwnerGoal(wolf,1,3,2);var ownerWrapper=install(wolf,ownerGoal);
        require(WoolBucketChecks.read(ownerGoal,"owner")==f.owner&&wolf.getPathfindingMalus(PathType.WATER)==0,"native owner following starts with temporary water cost");
        int ownerDelay=count(ownerGoal,"timeToRecalcPath");var ownerImage=MatchActorCheckpoint.captureRegistered(game);ownerWrapper.stop();
        require(wolf.getPathfindingMalus(PathType.WATER)==6,"native stop restores previous water cost");
        wolf.setPathfindingMalus(PathType.WATER,11);require(ownerGoal.canUse(),"native owner task can restart in the future");ownerWrapper.start();ownerWrapper.stop();
        require(count(ownerGoal,"timeToRecalcPath")!=ownerDelay&&wolf.getPathfindingMalus(PathType.WATER)==11,"future native restart changed both countdown and remembered water cost");
        restore(game,ownerImage);ownerGoal.tick();
        require(WoolBucketChecks.read(ownerGoal,"owner")==f.owner&&wolf.getPathfindingMalus(PathType.WATER)==0&&count(ownerGoal,"timeToRecalcPath")==ownerDelay-1,"owner task and temporary navigation cost resume");
        ownerWrapper.stop();require(wolf.getPathfindingMalus(PathType.WATER)==6,"restored stop uses the saved previous cost");
        var follower=add(f,new Cow(EntityTypes.COW,f.level),at.add(14,0,1));follower.setPathfindingMalus(PathType.WATER,4);
        var follow=new FollowMobGoal(follower,1,.5F,16);var followWrapper=install(follower,follow);Entity following=(Entity)WoolBucketChecks.read(follow,"followingMob");
        int followDelay=count(follow,"timeToRecalcPath");var followImage=MatchActorCheckpoint.captureRegistered(game);followWrapper.stop();
        follower.setPathfindingMalus(PathType.WATER,9);require(follow.canUse(),"native mob task can restart in the future");followWrapper.start();followWrapper.stop();
        require(count(follow,"timeToRecalcPath")!=followDelay&&follower.getPathfindingMalus(PathType.WATER)==9,"future native mob restart changes the saved phase and previous cost");
        restore(game,followImage);follow.tick();
        require(WoolBucketChecks.read(follow,"followingMob")==following&&follower.getPathfindingMalus(PathType.WATER)==0&&count(follow,"timeToRecalcPath")==followDelay-1,"mob following resumes actual target and native recalc phase");
        followWrapper.stop();require(follower.getPathfindingMalus(PathType.WATER)==4,"mob following retains original water cost");

        var tempted=add(f,new Cow(EntityTypes.COW,f.level),at.add(6,0,0));
        var tempt=new TemptGoal(tempted,1,stack->stack.is(Items.WHEAT),true);var temptWrapper=install(tempted,tempt);
        require(WoolBucketChecks.read(tempt,"player")==f.owner&&tempt.isRunning()&&tempt.canContinueToUse(),"native wheat temptation selects the real player");
        var temptImage=MatchActorCheckpoint.captureRegistered(game);temptWrapper.stop();
        restore(game,temptImage);require(tempt.isRunning()&&tempt.canContinueToUse(),"temptation resumes without cooldown or a null player");tempt.tick();temptWrapper.stop();
        for(int i=0;i<10;i++)require(!tempt.canUse(),"native temptation cooldown advances");
        int cool=count(tempt,"calmDown");var coolImage=MatchActorCheckpoint.captureRegistered(game);
        for(int replay=0;replay<2;replay++){
            for(int i=0;i<cool;i++)require(!tempt.canUse(),"saved remaining cooldown blocks new temptation");
            require(tempt.canUse(),"native temptation readmits when cooldown expires");restore(game,coolImage);
        }
        var threat=add(f,new Zombie(EntityTypes.ZOMBIE,f.level),at.add(9,0,-1));
        var fleeing=add(f,new Cow(EntityTypes.COW,f.level),at.add(11,0,-1));
        var avoid=new AvoidEntityGoal<>(fleeing,Zombie.class,12,1,1.5);var avoidWrapper=install(fleeing,avoid);
        Entity avoided=(Entity)WoolBucketChecks.read(avoid,"toAvoid");Path path=(Path)WoolBucketChecks.read(avoid,"path");int node=path.getNextNodeIndex();
        require(avoided!=null&&fleeing.getNavigation().getPath()==path,"native avoidance computes and starts its own actual route");
        var avoidImage=MatchActorCheckpoint.captureRegistered(game);path.advance();avoidWrapper.stop();
        restore(game,avoidImage);avoid.tick();
        require(WoolBucketChecks.read(avoid,"toAvoid")==avoided&&WoolBucketChecks.read(avoid,"path")==path&&path.getNextNodeIndex()==node&&fleeing.getNavigation().getPath()==path,
                "avoidance restores the cached threat and shared original path/cursor");avoidWrapper.stop();
    }
    private static int expire(Goal goal){int count=0;while(goal.canContinueToUse()&&count<100){goal.tick();count++;}require(count<100,"bounded native task expiry");return count;}
}
