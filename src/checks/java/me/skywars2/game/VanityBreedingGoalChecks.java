package me.skywars2.game;

import java.util.*;

import me.skywars2.game.match.checkpoints.MatchActorCheckpoint;
import me.skywars2.mixin.mobs.MobGoalCheckpointAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Actual server-tick breeding, with real offspring and cancellation of the discarded future. */
final class VanityBreedingGoalChecks {
    private final GameController game;
    private final WoolBucketChecks.Fixture f;
    private final Cow mother,father;
    private final BreedGoal goal;
    private final GoalSelector selector;
    private final WrappedGoal wrapper;
    private final Set<UUID> captured=new HashSet<>();
    private final List<Entity> future=new ArrayList<>();
    private MatchActorCheckpoint image;
    private int phase,when,savedLove,remaining;

    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity breeding goals: "+why);}
    private MobGoalCheckpointAccess.BreedGoalAccess breeding(){return (MobGoalCheckpointAccess.BreedGoalAccess)goal;}
    private int love(){return breeding().skywars2$goalLoveTime();}
    VanityBreedingGoalChecks(GameController game,WoolBucketChecks.Fixture f){
        this.game=game;this.f=f;
        BlockPos floor=BlockPos.containing(f.home).above(3);
        for(int x=11;x<=17;x++)for(int z=-2;z<=2;z++){
            var p=floor.offset(x,0,z);f.set(p,Blocks.STONE.defaultBlockState());
            for(int y=1;y<=4;y++)f.set(p.above(y),Blocks.AIR.defaultBlockState());
        }
        Vec3 at=Vec3.atCenterOf(floor.offset(12,0,0)).add(0,.5,0);
        mother=new Cow(EntityTypes.COW,f.level);mother.setPos(at);mother.setOnGround(true);
        father=new Cow(EntityTypes.COW,f.level);father.setPos(at.add(1.5,0,0));father.setOnGround(true);father.setNoAi(true);
        var selectors=(MobGoalCheckpointAccess.MobAccess)mother;
        selector=selectors.skywars2$goalGoalSelector();selector.removeAllGoals(g->true);
        selectors.skywars2$goalTargetSelector().removeAllGoals(g->true);
        goal=new BreedGoal(mother,1);selector.addGoal(1,goal);wrapper=selector.getAvailableGoals().iterator().next();
        mother.setInLove(null);father.setInLove(null);
        require(f.level.addFreshEntity(mother)&&f.level.addFreshEntity(father),"ordinary native parents registered");f.own(mother);f.own(father);
        when=game.server().getTickCount();
    }
    boolean step(){
        f.client.acknowledge();f.audience.acknowledge();int now=game.server().getTickCount();
        require(now-when<100,"bounded actual server-tick birth phase="+phase);
        if(phase==0){
            if(!wrapper.isRunning()||love()<8)return false;
            require(love()<20&&breeding().skywars2$goalPartner()==father,"native goal actually chose its partner before birth");
            savedLove=love();for(var actor:f.level.getAllEntities())captured.add(actor.getUUID());
            image=MatchActorCheckpoint.captureRegistered(game);phase=1;when=now;return false;
        }
        if(mother.getAge()<=0){
            if(phase>1)require(now-when<remaining,"restored native birth occurs at the saved remaining tick");
            return false;
        }
        var babies=f.level.getEntitiesOfClass(Cow.class,mother.getBoundingBox().inflate(8),cow->cow.isBaby()&&!captured.contains(cow.getUUID()));
        require(babies.size()==1&&!mother.isInLove()&&!father.isInLove(),"one real native offspring and both parents consumed their love state");
        future.clear();future.addAll(babies);
        future.addAll(f.level.getEntitiesOfClass(ExperienceOrb.class,mother.getBoundingBox().inflate(8),orb->!captured.contains(orb.getUUID())));
        future.forEach(f::own);
        if(phase==1){remaining=now-when;require(remaining>8,"saved native birth is delayed");}
        else require(now-when==remaining,"birth timing is identical after restoring the saved native phase");
        selector.removeAllGoals(g->true);
        require(breeding().skywars2$goalPartner()==null&&love()==0,"native stop actually erased partner and countdown");
        restore(Set.of());verify();
        if(phase++<3){when=now;return false;}

        restore(Set.of(father.getUUID()));
        require(father.isRemoved()&&breeding().skywars2$goalPartner()==null&&love()==0&&!wrapper.isRunning(),
                "an explicitly omitted native partner retires the task before any continuation can dereference null");
        selector.tick();require(!wrapper.isRunning(),"ordinary native admission remains safe with no partner");
        restore(Set.of());verify();require(goal.canContinueToUse(),"the same image remains reusable after explicit partner absence");
        return true;
    }
    private void restore(Set<UUID> omitted){
        boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
        try{
            image.restoreBodiesFrozen(omitted);require(image.restoreLinksFrozen().isEmpty(),"all native partner links resolve");
            for(var actor:future)require(actor.isRemoved()&&f.level.getEntity(actor.getUUID())==null,"future offspring and birth XP are removed by actor replay");
        }finally{game.server().tickRateManager().setFrozen(frozen);}
    }
    private void verify(){
        require(wrapper.isRunning()&&breeding().skywars2$goalPartner()==father&&love()==savedLove,
                "restore original running task, exact partner body and elapsed courtship before any AI step");
        require(!mother.isBaby()&&mother.getAge()==0&&father.getAge()==0&&mother.isInLove()&&father.isInLove(),
                "restored native parents are ready without spawning a child during image load");
    }
}
