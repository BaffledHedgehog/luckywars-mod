package me.skywars2.game;

import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real native target selection, sight-memory expiry and accepted-hurt deduplication. */
final class VanityTargetGoalChecks {
    private VanityTargetGoalChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity target goals: "+why);}
    private static int count(Object goal,String name){return (Integer)WoolBucketChecks.read(goal,name);}

    static void verify(GameController game,WoolBucketChecks.Fixture f){
        var at=Vec3.atCenterOf(net.minecraft.core.BlockPos.containing(f.home).above(3).offset(11,0,0)).add(0,.5,0);
        var cow=new Cow(EntityTypes.COW,f.level);cow.setPos(at.add(5,0,0));cow.setNoAi(true);cow.setOnGround(true);
        var zombie=new Zombie(EntityTypes.ZOMBIE,f.level);zombie.setPos(at);zombie.setNoAi(true);zombie.setOnGround(true);
        var selector=(GoalSelector)WoolBucketChecks.read(zombie,"targetSelector");selector.removeAllGoals(g->true);
        ((GoalSelector)WoolBucketChecks.read(zombie,"goalSelector")).removeAllGoals(g->true);
        require(f.level.addFreshEntity(cow)&&f.level.addFreshEntity(zombie),"ordinary registered native actors");f.own(cow);f.own(zombie);
        // These are the exact native selector/sensing steps. No server tick or callbacks run during image replay.
        var nearest=new NearestAttackableTargetGoal<>(zombie,Cow.class,0,true,false,(entity,level)->entity==cow);
        nearest.setUnseenMemoryTicks(20);selector.addGoal(1,nearest);selector.tick();
        WrappedGoal wrapper=selector.getAvailableGoals().iterator().next();
        require(wrapper.isRunning()&&zombie.getTargetUnchecked()==cow,"native search acquired the intended visible cow");
        var wall=zombie.blockPosition().offset(2,0,0);
        for(int z=-2;z<=2;z++)for(int y=0;y<4;y++)f.set(wall.offset(0,y,z),Blocks.STONE.defaultBlockState());
        for(int i=0;i<3;i++)tick(zombie,selector);
        require(wrapper.isRunning()&&count(nearest,"unseenTicks")==3,"native occlusion has consumed three memory steps");
        var sightImage=MatchActorCheckpoint.captureRegistered(game);
        int remaining=expire(zombie,selector,wrapper);
        require(remaining>1&&zombie.getTargetUnchecked()==null,"native cleanup actually forgot the hidden target");
        for(int replay=0;replay<2;replay++){
            restore(game,sightImage);
            require(wrapper.isRunning()&&zombie.getTargetUnchecked()==cow&&WoolBucketChecks.read(nearest,"target")==cow,
                    "original target task and both actual target references restored");
            require(count(nearest,"unseenTicks")==3,"native sight-memory resumes instead of retaining future expiry");
            require(expire(zombie,selector,wrapper)==remaining,"same saved number of native selector steps until target expiry");
        }

        for(int z=-2;z<=2;z++)for(int y=0;y<4;y++)f.set(wall.offset(0,y,z),Blocks.AIR.defaultBlockState());
        selector.removeAllGoals(g->true);zombie.getSensing().tick();
        var retaliation=new HurtByTargetGoal(zombie);selector.addGoal(1,retaliation);
        zombie.tickCount++; // A newly registered body otherwise has the native zero/zero initial hurt stamp.
        require(zombie.hurtServer(f.level,zombie.damageSources().mobAttack(cow),1),"genuine accepted native mob damage");
        selector.tick();
        require(selector.getAvailableGoals().iterator().next().isRunning()&&WoolBucketChecks.read(retaliation,"targetMob")==cow,
                "native retaliation really started and cached its attacker");
        require(!retaliation.canUse(),"native same-hurt timestamp is consumed once");
        var hurtImage=MatchActorCheckpoint.captureRegistered(game);
        // Advance the body's local clock, then let actual accepted damage and native start consume the later hit.
        zombie.tickCount+=10;zombie.damageCooldownTime=0;
        require(zombie.hurtServer(f.level,zombie.damageSources().mobAttack(cow),1),"later accepted native hurt");
        selector.getAvailableGoals().iterator().next().stop();selector.tick();
        require(count(retaliation,"timestamp")==zombie.getLastHurtByMobTimestamp(),"native future retaliation consumed the later timestamp");
        for(int replay=0;replay<2;replay++){
            restore(game,hurtImage);
            require(WoolBucketChecks.read(retaliation,"targetMob")==cow,"restored native retaliation cache retains actual attacker");
            require(!retaliation.canUse(),"restored damage must not be treated as a new retaliation or alert");
        }
        verifyRetaliationFilters(game,f,zombie,cow,selector);
    }

    private static void verifyRetaliationFilters(GameController game,WoolBucketChecks.Fixture f,Zombie zombie,Cow cow,GoalSelector selector){
        selector.removeAllGoals(g->true);zombie.setTarget(null);
        var neighbor=new Zombie(EntityTypes.ZOMBIE,f.level);neighbor.setPos(zombie.position().add(0,0,3));neighbor.setNoAi(true);
        require(f.level.addFreshEntity(neighbor),"registered native neighbor for actual retaliation alerts");f.own(neighbor);
        Class<?>[] damageTypes={Cow.class},alertTypes={Zombie.class};
        var retaliation=new HurtByTargetGoal(zombie,damageTypes);selector.addGoal(1,retaliation);
        zombie.tickCount+=10;zombie.damageCooldownTime=0;
        require(zombie.hurtServer(f.level,zombie.damageSources().mobAttack(cow),1),"accepted native hit before damage filter selection");
        require(!retaliation.canUse(),"constructor's caller-owned class array really prevents retaliation against cow");
        var disabled=MatchActorCheckpoint.captureRegistered(game);
        retaliation.setAlertOthers(alertTypes);
        var enabled=MatchActorCheckpoint.captureRegistered(game);
        var wrapper=selector.getAvailableGoals().iterator().next();
        // A real later public call replaces the alert array; mutations of both originals must also rewind.
        damageTypes[0]=Pig.class;alertTypes[0]=Pig.class;retaliation.setAlertOthers(Pig.class);
        selector.tick();
        require(wrapper.isRunning()&&zombie.getTargetUnchecked()==cow&&neighbor.getTargetUnchecked()==cow,
                "future mutation admits actual native retaliation and alerts a nearby zombie");
        for(int replay=0;replay<3;replay++){
            restore(game,enabled);
            require(WoolBucketChecks.read(retaliation,"toIgnoreDamage")==damageTypes&&damageTypes[0]==Cow.class,
                    "final damage filter retains original caller array and saved contents");
            require(WoolBucketChecks.read(retaliation,"toIgnoreAlert")==alertTypes&&alertTypes[0]==Zombie.class
                    &&Boolean.TRUE.equals(WoolBucketChecks.read(retaliation,"alertSameType")),
                    "enabled alert policy restores original replaced array, contents and flag");
            require(!wrapper.isRunning()&&zombie.getTargetUnchecked()==null&&neighbor.getTargetUnchecked()==null&&!retaliation.canUse(),
                    "replay clears future targets without re-alerting or admitting the ignored hit");
            damageTypes[0]=Pig.class;selector.tick();
            require(wrapper.isRunning()&&zombie.getTargetUnchecked()==cow&&neighbor.getTargetUnchecked()==null,
                    "restored alert class filter suppresses actual native neighbor recruitment");
            wrapper.stop();zombie.setTarget(null);zombie.tickCount+=10;zombie.damageCooldownTime=0;
            alertTypes[0]=Pig.class;retaliation.setAlertOthers(Pig.class);
            require(zombie.hurtServer(f.level,zombie.damageSources().mobAttack(cow),1),"fresh accepted hit for future recruitment");
            selector.tick();
            require(wrapper.isRunning()&&neighbor.getTargetUnchecked()==cow,"future public policy change really recruits neighbor again");
        }
        restore(game,disabled);
        require(WoolBucketChecks.read(retaliation,"toIgnoreAlert")==null
                &&Boolean.FALSE.equals(WoolBucketChecks.read(retaliation,"alertSameType"))
                &&WoolBucketChecks.read(retaliation,"toIgnoreDamage")==damageTypes&&damageTypes[0]==Cow.class,
                "earlier checkpoint restores null/disabled alert policy and same final caller array");
        require(!wrapper.isRunning()&&!retaliation.canUse()&&neighbor.getTargetUnchecked()==null,
                "earlier disabled image remains reusable after three enabled-policy replays");
    }
    private static void tick(Zombie mob,GoalSelector selector){mob.getSensing().tick();selector.tick();}
    private static int expire(Zombie mob,GoalSelector selector,WrappedGoal wrapper){
        int steps=0;while(wrapper.isRunning()&&steps<24){tick(mob,selector);steps++;}
        require(!wrapper.isRunning()&&steps<24,"bounded actual native sight expiry");return steps;
    }
    private static void restore(GameController game,MatchActorCheckpoint image){
        boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
        try{image.restoreBodiesFrozen();require(image.restoreLinksFrozen().isEmpty(),"all captured native links resolve");}
        finally{game.server().tickRateManager().setFrozen(frozen);}
    }
}
