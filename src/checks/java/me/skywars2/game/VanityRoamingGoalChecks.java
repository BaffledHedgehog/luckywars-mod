package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Native trigger/configuration, panic and attack phases must survive cancelled future work. */
final class VanityRoamingGoalChecks {
    private VanityRoamingGoalChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity roaming goals: "+why);}
    private static GoalSelector selector(Mob mob){return (GoalSelector)WoolBucketChecks.read(mob,"goalSelector");}
    private static <T extends Mob>T add(WoolBucketChecks.Fixture f,T mob,Vec3 at){
        mob.setPos(at);mob.setNoAi(true);mob.setNoGravity(true);mob.setOnGround(true);selector(mob).removeAllGoals(g->true);
        ((GoalSelector)WoolBucketChecks.read(mob,"targetSelector")).removeAllGoals(g->true);
        require(f.level.addFreshEntity(mob),"ordinary native actor");f.own(mob);return mob;
    }
    private static WrappedGoal admit(Mob mob){
        var selector=selector(mob);for(int i=0;i<1024;i++){selector.tick();if(selector.getAvailableGoals().iterator().next().isRunning())return selector.getAvailableGoals().iterator().next();}
        throw new IllegalStateException("Native roaming admission failed");
    }
    private static Vec3 destination(Object goal){return new Vec3((Double)WoolBucketChecks.read(goal,"wantedX"),(Double)WoolBucketChecks.read(goal,"wantedY"),(Double)WoolBucketChecks.read(goal,"wantedZ"));}
    private static void restore(GameController game,MatchActorCheckpoint image){
        boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
        try{image.restoreBodiesFrozen();require(image.restoreLinksFrozen().isEmpty(),"native roaming links resolve");}finally{game.server().tickRateManager().setFrozen(frozen);}
    }
    static void verify(GameController game,WoolBucketChecks.Fixture f){
        var at=Vec3.atCenterOf(BlockPos.containing(f.home).above(3)).add(8,.5,0);
        var cow=add(f,new Cow(EntityTypes.COW,f.level),at);var stroll=new RandomStrollGoal(cow,1,7);
        selector(cow).addGoal(1,stroll);stroll.trigger();var pending=MatchActorCheckpoint.captureRegistered(game);
        var wrapper=admit(cow);var wanted=destination(stroll);stroll.setInterval(900);
        restore(game,pending);
        require((Boolean)WoolBucketChecks.read(stroll,"forceTrigger")&&(Integer)WoolBucketChecks.read(stroll,"interval")==7,"unconsumed native trigger and interval rewind");
        wrapper=admit(cow);require(destination(stroll).equals(wanted),"same native random state chooses the same real destination after replay");
        var path=cow.getNavigation().getPath();require(path!=null,"native strolling actually starts a route");var active=MatchActorCheckpoint.captureRegistered(game);
        wrapper.stop();stroll.trigger();
        boolean changed=false;for(int i=0;i<1024;i++)if(stroll.canUse()&&!destination(stroll).equals(wanted)){changed=true;break;}
        require(changed,"future native selection genuinely changed destination");stroll.setInterval(999);
        restore(game,active);
        require(destination(stroll).equals(wanted)&&cow.getNavigation().getPath()==path&&stroll.canContinueToUse()&&
                (Integer)WoolBucketChecks.read(stroll,"interval")==7,"active destination/config and same route resume without selecting another point");
        wrapper.stop();selector(cow).removeAllGoals(g->true);

        var panic=new PanicGoal(cow,1);selector(cow).addGoal(1,panic);
        require(cow.hurtServer(f.level,cow.damageSources().inFire(),1),"genuine panic-causing native damage");var panicWrapper=admit(cow);
        require(panic.isRunning()&&cow.isPanicking(),"native animal observes its actual running panic task");var panicPath=cow.getNavigation().getPath();
        var panicImage=MatchActorCheckpoint.captureRegistered(game);panicWrapper.stop();cow.getNavigation().stop();
        restore(game,panicImage);require(panic.isRunning()&&cow.isPanicking()&&cow.getNavigation().getPath()==panicPath&&panic.canContinueToUse(),"panic flag and native route resume after real stop");panicWrapper.stop();

        var victim=add(f,new Cow(EntityTypes.COW,f.level),at.add(4,0,0));victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);victim.setHealth(100);
        var attacker=add(f,new Zombie(EntityTypes.ZOMBIE,f.level),at.add(3,0,0));attacker.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.LEATHER_HELMET));attacker.setTarget(victim);
        var attack=new OcelotAttackGoal(attacker);selector(attacker).addGoal(1,attack);var attackWrapper=admit(attacker);
        require(victim.getHealth()<100&&(Integer)WoolBucketChecks.read(attack,"attackTime")==20,"real native attack starts its cooldown");
        var attackImage=MatchActorCheckpoint.captureRegistered(game);for(int i=0;i<5;i++)attack.tick();attackWrapper.stop();
        require((Integer)WoolBucketChecks.read(attack,"attackTime")==15&&WoolBucketChecks.read(attack,"target")==null,"future native ticks/stop really changed cooldown and cache");
        restore(game,attackImage);victim.damageCooldownTime=0;float health=victim.getHealth();
        for(int i=0;i<19;i++){attack.tick();require(victim.getHealth()==health,"restored attack cannot hit before its saved cooldown expires");}
        attack.tick();require(victim.getHealth()<health&&WoolBucketChecks.read(attack,"target")==victim,"actual native hit occurs at restored attack phase");attackWrapper.stop();
    }
}
