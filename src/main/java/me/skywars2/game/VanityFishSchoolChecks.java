package me.skywars2.game;

import java.util.*;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Actual native joins/leaves and follow ticks, including omitted leader/member and historical links. */
final class VanityFishSchoolChecks {
    private VanityFishSchoolChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity fish schools: "+why);}
    private static int count(Object body,String field){return (Integer)WoolBucketChecks.read(body,field);}
    private static GoalSelector selector(Cod fish){return (GoalSelector)WoolBucketChecks.read(fish,"goalSelector");}
    private static WrappedGoal task(Cod fish){return selector(fish).getAvailableGoals().stream().filter(g->g.getGoal() instanceof FollowFlockLeaderGoal).findFirst().orElseThrow();}
    private static void restore(GameController game,MatchActorCheckpoint image,Set<UUID> omitted){
        boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
        try{
            image.restoreBodiesFrozen(omitted);
            require(image.restoreLinksFrozen().isEmpty(),"school dependencies resolve or are explicitly omitted");
            require(image.restoreLinksFrozen().isEmpty(),"repeated link replay is safe");
        }finally{game.server().tickRateManager().setFrozen(frozen);}
    }
    private static Cod spawn(WoolBucketChecks.Fixture f,BlockPos pos){
        var fish=new Cod(EntityTypes.COD,f.level);fish.setPos(Vec3.atCenterOf(pos));fish.setNoAi(true);fish.setNoGravity(true);
        require(f.level.addFreshEntity(fish),"real native Cod registered");f.own(fish);fish.tick();
        require(fish.isInWater(),"native fish tick observes the real water volume");
        selector(fish).removeAllGoals(g->!(g instanceof FollowFlockLeaderGoal));return fish;
    }
    static void verify(GameController game,WoolBucketChecks.Fixture f){
        var floor=BlockPos.containing(f.home).above(2).offset(0,0,24);
        for(int x=0;x<=12;x++)for(int z=-2;z<=2;z++)for(int y=0;y<=4;y++)
            f.set(floor.offset(x,y,z),x==0||x==12||z==-2||z==2||y==0||y==4?Blocks.STONE.defaultBlockState():Blocks.WATER.defaultBlockState());
        var leader=spawn(f,floor.offset(9,2,0));var first=spawn(f,floor.offset(3,2,0));var second=spawn(f,floor.offset(4,2,0));
        leader.addFollowers(Stream.of(first,second));selector(first).tick();selector(second).tick();
        require(count(leader,"schoolSize")==3&&task(first).isRunning()&&task(second).isRunning(),"native school formation and follow-task admission");
        var goal=(FollowFlockLeaderGoal)task(first).getGoal();var alias=task(first);var path=first.getNavigation().getPath();
        int recalc=count(goal,"timeToRecalcPath"),search=count(goal,"nextStartTick");
        require(recalc>1&&search>0&&path!=null,"actual native path and independent search/recalculation countdowns");
        var image=MatchActorCheckpoint.captureRegistered(game);
        for(int replay=0;replay<2;replay++){
            var future=spawn(f,floor.offset(7,2,0));
            for(var fish:List.of(first,second)){
                var original=task(fish).getGoal();selector(fish).removeAllGoals(g->true);
                require(!fish.isFollower()&&!original.canUse(),"real stop left old school and native search countdown advances");
                fish.startFollowing(future);selector(fish).addGoal(5,original);
                for(int attempt=0;attempt<5&&!task(fish).isRunning();attempt++)selector(fish).tick();
                require(task(fish).isRunning(),"native selector readmitted the original following task");original.tick();
            }
            require(count(leader,"schoolSize")==1&&count(future,"schoolSize")==3&&count(goal,"nextStartTick")!=search&&count(goal,"timeToRecalcPath")!=recalc,
                    "native future school and both countdowns really diverged");
            restore(game,image,Set.of());
            require(future.isRemoved()&&MatchFishSchools.leader(first)==leader&&MatchFishSchools.leader(second)==leader&&count(leader,"schoolSize")==3,
                    "original school graph and exact counters restore without native rejoining");
            require(task(first)==alias&&alias.isRunning()&&first.getNavigation().getPath()==path&&count(goal,"nextStartTick")==search&&count(goal,"timeToRecalcPath")==recalc,
                    "same task wrapper, route and both native countdowns restore");
            first.getNavigation().stop();
            for(int tick=1;tick<=recalc;tick++){
                goal.tick();
                require((first.getNavigation().getPath()!=null)==(tick==recalc),"native leader path returns only at the saved remaining task tick");
            }
        }
        restore(game,image,Set.of(leader.getUUID()));
        require(leader.isRemoved()&&MatchFishSchools.leader(first)==null&&MatchFishSchools.leader(second)==null&&!task(first).isRunning()&&!task(second).isRunning(),
                "omitted leader clears links and retires running tasks without stopFollowing(null)");
        require(first.getNavigation().getPath()==path,"native follow stop leaves the existing route intact; omission must not cancel a route another task can use");
        selector(first).tick();selector(second).tick();
        restore(game,image,Set.of(first.getUUID()));
        require(first.isRemoved()&&count(leader,"schoolSize")==2&&MatchFishSchools.leader(second)==leader&&task(second).isRunning(),
                "omitting one member adjusts leader count once, despite repeated link replay");
        restore(game,image,Set.of());
        require(count(leader,"schoolSize")==3&&MatchFishSchools.leader(first)==leader&&task(first).isRunning(),"same image can restore the entire original school again");
        var initialOmission=MatchActorCheckpoint.captureRegistered(game,Set.of(leader.getUUID()));restore(game,initialOmission,Set.of(leader.getUUID()));
        require(MatchFishSchools.leader(first)==null&&!task(first).isRunning(),"leader omitted at capture is an absent AI dependency, not a historical alias");
        restore(game,image,Set.of());

        // A genuinely older body with the SAME UUID as a live leader must still remain historical.
        var historical=new Cod(EntityTypes.COD,f.level);historical.setUUID(leader.getUUID());historical.discard();
        selector(first).removeAllGoals(g->true);first.startFollowing(historical);selector(first).addGoal(5,goal);
        var past=MatchActorCheckpoint.captureRegistered(game);first.stopFollowing();first.startFollowing(second);
        restore(game,past,Set.of(leader.getUUID()));
        require(historical.isRemoved()&&MatchFishSchools.leader(first)==historical&&!first.isFollower(),
                "removed older leader alias stays historical even when the captured same-UUID body is omitted");
        restore(game,image,Set.of());
    }
}
