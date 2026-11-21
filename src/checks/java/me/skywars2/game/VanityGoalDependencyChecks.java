package me.skywars2.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Excluded captured AI dependencies differ from genuinely older same-UUID body references. */
final class VanityGoalDependencyChecks {
    private VanityGoalDependencyChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity goal dependencies: "+why);}
    private record Case(Mob owner,GoalSelector selector,WrappedGoal wrapper,Goal goal,String field,boolean rawTarget){
        void verify(Entity expected,boolean running){
            require(WoolBucketChecks.read(goal,field)==expected,"exact "+goal.getClass().getSimpleName()+"."+field+" dependency");
            require(wrapper.isRunning()==running,"saved/retired "+goal.getClass().getSimpleName()+" wrapper");
            if(rawTarget)require(owner.getTargetUnchecked()==expected,"raw native target agrees with "+goal.getClass().getSimpleName()+" cache");
        }
    }
    private static GoalSelector selector(Mob mob,boolean target){return (GoalSelector)WoolBucketChecks.read(mob,target?"targetSelector":"goalSelector");}
    private static <T extends Mob>T spawn(WoolBucketChecks.Fixture f,T mob,Vec3 position){
        mob.setPos(position);mob.setNoAi(true);mob.setNoGravity(true);mob.setOnGround(true);
        selector(mob,false).removeAllGoals(g->true);selector(mob,true).removeAllGoals(g->true);
        require(f.level.addFreshEntity(mob),"real native dependency actor registered");f.own(mob);return mob;
    }
    private static Case admit(Mob owner,Goal goal,boolean target,String field,boolean rawTarget){
        var selector=selector(owner,target);selector.addGoal(1,goal);var wrapper=selector.getAvailableGoals().iterator().next();
        for(int attempt=0;attempt<5&&!wrapper.isRunning();attempt++)selector.tick();
        require(wrapper.isRunning(),"native admission really started "+goal.getClass().getSimpleName());
        return new Case(owner,selector,wrapper,goal,field,rawTarget);
    }
    private static void restore(GameController game,MatchActorCheckpoint image,Set<UUID> omitted){
        boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
        try{
            image.restoreBodiesFrozen(omitted);
            require(image.restoreLinksFrozen().isEmpty()&&image.restoreLinksFrozen().isEmpty(),"both native link replays resolve without pending dependencies");
        }finally{game.server().tickRateManager().setFrozen(frozen);}
    }
    static void verify(GameController game,WoolBucketChecks.Fixture f){
        var floor=BlockPos.containing(f.home).above(3).offset(0,0,36);
        for(int x=0;x<=10;x++)for(int z=-3;z<=3;z++){
            var p=floor.offset(x,0,z);f.set(p,Blocks.STONE.defaultBlockState());
            for(int y=1;y<=4;y++)f.set(p.above(y),Blocks.AIR.defaultBlockState());
        }
        var at=Vec3.atBottomCenterOf(floor.above());
        var victim=spawn(f,new Cow(EntityTypes.COW,f.level),at.add(8,0,0));victim.setInLove(null);
        var ranged=spawn(f,new SnowGolem(EntityTypes.SNOW_GOLEM,f.level),at.add(1,0,-2));ranged.setTarget(victim);
        var hunter=spawn(f,new Zombie(EntityTypes.ZOMBIE,f.level),at.add(1,0,2));hunter.setTarget(victim);
        var searching=spawn(f,new Zombie(EntityTypes.ZOMBIE,f.level),at.add(2,0,-1));
        var retaliating=spawn(f,new Zombie(EntityTypes.ZOMBIE,f.level),at.add(2,0,1));
        var mother=spawn(f,new Cow(EntityTypes.COW,f.level),at.add(3,0,0));mother.setInLove(null);
        retaliating.tickCount++;
        require(retaliating.hurtServer(f.level,retaliating.damageSources().mobAttack(victim),1),"actual accepted native hurt establishes retaliation");
        var cases=List.of(
                admit(ranged,new RangedAttackGoal(ranged,1,100,16),false,"target",true),
                admit(hunter,new OcelotAttackGoal(hunter),false,"target",true),
                admit(searching,new NearestAttackableTargetGoal<>(searching,Cow.class,0,false,false,(candidate,level)->candidate==victim),true,"target",true),
                admit(retaliating,new HurtByTargetGoal(retaliating),true,"targetMob",true),
                admit(mother,new BreedGoal(mother,1),false,"partner",false));
        cases.forEach(c->c.verify(victim,true));
        var whole=MatchActorCheckpoint.captureRegistered(game);var excluded=Set.of(victim.getUUID());
        var omitted=MatchActorCheckpoint.captureRegistered(game,excluded);
        var control=new MatchDamageSources(game,MatchActorCheckpoint.currentBodies(game).values(),excluded);
        require(control.bodyRef(victim).historical()==victim&&control.dependencyRef(victim).historical()==null,
                "negative control: combat history keeps the omitted body while AI dependency requests its absence");
        for(int replay=0;replay<2;replay++){
            restore(game,omitted,excluded);require(victim.isRemoved(),"excluded victim remains retired");
            cases.forEach(c->c.verify(null,false));
            require(retaliating.getLastDamageSource()!=null&&retaliating.getLastDamageSource().getEntity()==victim,
                    "accepted damage retains its physical author despite AI target omission");
            cases.forEach(c->c.selector.tick()); // Null cached ranged/breed targets must never tick while running.
            cases.forEach(c->c.verify(null,false));
            restore(game,whole,Set.of());cases.forEach(c->c.verify(victim,true));
        }

        // Preserve real stale caches, rather than forging a goal field or matching bodies by UUID alone.
        victim.discard();var alias=new Cow(EntityTypes.COW,f.level);alias.setUUID(victim.getUUID());spawn(f,alias,at.add(8,0,0));
        cases.forEach(c->c.verify(victim,true));
        var historical=MatchActorCheckpoint.captureRegistered(game,excluded);
        for(int replay=0;replay<2;replay++){
            for(var c:cases){c.selector.removeAllGoals(g->true);c.owner.setTarget(null);}
            restore(game,historical,excluded);
            require(victim.isRemoved()&&alias.isRemoved()&&f.level.getEntity(victim.getUUID())==null,"neither historical victim nor excluded current alias resurrects");
            cases.forEach(c->c.verify(victim,true));
            require(!cases.get(0).goal.canContinueToUse()&&!cases.get(1).goal.canContinueToUse()&&!cases.get(4).goal.canContinueToUse(),
                    "native continuation safely inspects original removed dependencies before ordinary cleanup");
        }
        restore(game,whole,Set.of());cases.forEach(c->c.verify(victim,true));
    }
}
