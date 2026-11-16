package me.skywars2.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Actual task transitions; probes instrument sound only, never replace native action or movement. */
final class VanityActionGoalChecks {
    private VanityActionGoalChecks(){}
    static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity action goals: "+why);}
    static int count(Object value,String name){return (Integer)WoolBucketChecks.read(value,name);}
    static GoalSelector selector(Mob mob){return (GoalSelector)WoolBucketChecks.read(mob,"goalSelector");}
    static <T extends Mob>T add(WoolBucketChecks.Fixture f,T mob,Vec3 at){
        mob.setPos(at);mob.setNoAi(true);mob.setNoGravity(true);mob.setOnGround(true);
        selector(mob).removeAllGoals(g->true);((GoalSelector)WoolBucketChecks.read(mob,"targetSelector")).removeAllGoals(g->true);
        require(f.level.addFreshEntity(mob),"ordinary native action actor");f.own(mob);return mob;
    }
    static WrappedGoal admit(Mob mob,Goal goal){
        var selector=selector(mob);selector.addGoal(1,goal);var wrapper=selector.getAvailableGoals().stream().filter(g->g.getGoal()==goal).findFirst().orElseThrow();
        for(int i=0;i<8&&!wrapper.isRunning();i++)selector.tick();require(wrapper.isRunning(),"actual native admission: "+goal.getClass().getSimpleName());return wrapper;
    }
    static void restore(GameController game,MatchActorCheckpoint image,MatchPlayerCheckpoint player,Set<UUID> omitted){
        boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
        try{image.restoreBodiesFrozen(omitted);if(player!=null)player.restoreFrozen();require(image.restoreLinksFrozen().isEmpty(),"action dependencies resolve");}
        finally{game.server().tickRateManager().setFrozen(frozen);}
    }
    static void verify(GameController game,WoolBucketChecks.Fixture f){
        var before=new MatchPlayerCheckpoint(game,f.owner);var actors=MatchActorCheckpoint.captureRegistered(game);
        try{beg(game,f);dolphin(game,f);rider(game,f);zombie(game,f);VanitySpearGoalChecks.verify(game,f);}
        finally{restore(game,actors,before,Set.of());f.client.acknowledge();}
    }
    private static void beg(GameController game,WoolBucketChecks.Fixture f){
        var at=Vec3.atCenterOf(BlockPos.containing(f.home).above(3).offset(0,0,44));f.move(f.owner,at);
        f.owner.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BONE));
        var wolf=add(f,new Wolf(EntityTypes.WOLF,f.level),at.add(2,0,0));var goal=new BegGoal(wolf,8);var wrapper=admit(wolf,goal);
        require(wolf.isInterested()&&WoolBucketChecks.read(goal,"player")==f.owner,"native bone request selected actual player and interested metadata");
        int remaining=count(goal,"lookTime");var image=MatchActorCheckpoint.captureRegistered(game);
        for(int replay=0;replay<2;replay++){
            int ticks=0;while(goal.canContinueToUse()&&ticks<80){goal.tick();ticks++;}
            require(ticks==remaining,"native beg duration expires at saved remaining task tick");wrapper.stop();require(!wolf.isInterested(),"native stop clears interested pose");
            restore(game,image,null,Set.of());require(wolf.isInterested()&&wrapper.isRunning()&&count(goal,"lookTime")==remaining,"saved begging pose and countdown resume");
        }
        // Typed link-phase policy input; actual player disconnection is covered by the actor integration suite.
        var binding=new MatchMobGoals(wolf);binding.captureReferences(new MatchDamageSources(game,List.of(wolf)));var pending=new HashSet<UUID>();
        MatchDamageSources.withAbsent(Set.of(f.owner.getUUID()),()->binding.restoreReferences(id->null,pending));
        require(pending.isEmpty()&&!wolf.isInterested()&&!wrapper.isRunning()&&WoolBucketChecks.read(goal,"player")==null,"absent-player binding clears native interested pose without stop callback");
        restore(game,image,null,Set.of());
    }
    private static void dolphin(GameController game,WoolBucketChecks.Fixture f){
        var water=BlockPos.containing(f.home).above(3).offset(16,0,44);
        for(int x=-1;x<=1;x++)for(int z=0;z<=9;z++){
            var p=water.offset(x,0,z);f.set(p,Blocks.WATER.defaultBlockState());f.set(p.above(),Blocks.AIR.defaultBlockState());f.set(p.above(2),Blocks.AIR.defaultBlockState());
        }
        var mob=add(f,new ProbeDolphin(f.level),Vec3.atCenterOf(water));mob.setYRot(0);mob.tick();mob.setOnGround(false);
        var goal=new DolphinJumpGoal(mob,1);var selector=selector(mob);selector.addGoal(1,goal);var wrapper=selector.getAvailableGoals().iterator().next();
        require(goal.canUse(),"actual native water/surface jump admission");wrapper.start();
        var before=MatchActorCheckpoint.captureRegistered(game);int sounds=mob.jumps;goal.tick();require(mob.jumps==sounds+1,"actual native first breach emits jump sound");
        var after=MatchActorCheckpoint.captureRegistered(game);
        for(int replay=0;replay<2;replay++){
            restore(game,after,null,Set.of());sounds=mob.jumps;goal.tick();require(mob.jumps==sounds,"restored already-breached phase never repeats sound");
            restore(game,before,null,Set.of());sounds=mob.jumps;goal.tick();require(mob.jumps==sounds+1,"restored pre-breach phase emits exactly once on next native tick");
        }
    }
    private static void rider(GameController game,WoolBucketChecks.Fixture f){
        var at=Vec3.atCenterOf(BlockPos.containing(f.home).above(5).offset(0,0,56));f.move(f.owner,at);
        var boat=Objects.requireNonNull(EntityTypes.OAK_BOAT.create(f.level,EntitySpawnReason.COMMAND));boat.setPos(at);boat.setNoGravity(true);
        require(f.level.addFreshEntity(boat),"ordinary native boat");f.own(boat);require(f.owner.startRiding(boat,true,false),"actual player mounted native boat");
        f.owner.baseTick();f.owner.setPos(f.owner.position().add(.25,0,0));f.owner.baseTick();
        require(f.owner.hasMovedHorizontallyRecently()&&boat.getControllingPassenger()==f.owner,"native base tick records actual mounted player displacement");
        var mob=add(f,new Cow(EntityTypes.COW,f.level),f.owner.position().add(2,0,0));var goal=new FollowPlayerRiddenEntityGoal(mob,Boat.class);var wrapper=admit(mob,goal);goal.tick();
        var phase=WoolBucketChecks.read(goal,"currentGoal");int delay=count(goal,"timeToRecalcPath");
        require(phase.toString().equals("GO_IN_ENTITY_DIRECTION")&&delay>0&&WoolBucketChecks.read(goal,"following")==f.owner,"native rider-follow phase and countdown");
        var player=new MatchPlayerCheckpoint(game,f.owner);var image=MatchActorCheckpoint.captureRegistered(game);
        for(int replay=0;replay<2;replay++){
            f.owner.setPos(mob.position().add(16,0,0));f.owner.baseTick();
            for(int tick=0;tick<delay;tick++)goal.tick();
            f.owner.baseTick();require(!f.owner.hasMovedHorizontallyRecently()&&WoolBucketChecks.read(goal,"currentGoal")!=phase,"native future changes motion admission and direction phase");wrapper.stop();
            restore(game,image,player,Set.of());f.client.acknowledge();
            require(wrapper.isRunning()&&WoolBucketChecks.read(goal,"following")==f.owner&&WoolBucketChecks.read(goal,"currentGoal")==phase&&count(goal,"timeToRecalcPath")==delay,
                    "same current rider body and native phase/countdown restore");
            require(f.owner.hasMovedHorizontallyRecently()&&f.owner.getVehicle()==boat&&goal.canContinueToUse(),"restored native speed history and passenger graph permit continuation");
        }
        f.owner.stopRiding();
    }
    private static void zombie(GameController game,WoolBucketChecks.Fixture f){
        var at=Vec3.atCenterOf(BlockPos.containing(f.home).above(5).offset(12,0,56));
        var mob=add(f,new Zombie(EntityTypes.ZOMBIE,f.level),at);var target=add(f,new Cow(EntityTypes.COW,f.level),at.add(1,0,0));
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        mob.setTarget(target);var goal=new ZombieAttackGoal(mob,1,true);admit(mob,goal);
        require(target.getHealth()<100,"real accepted native zombie attack");while(count(goal,"raiseArmTicks")<4)goal.tick();
        require(!mob.isAggressive(),"native raised-arm transition is still pending");var image=MatchActorCheckpoint.captureRegistered(game);int remaining=raised(mob,goal);
        for(int replay=0;replay<2;replay++){goal.stop();goal.start();restore(game,image,null,Set.of());require(!mob.isAggressive()&&count(goal,"raiseArmTicks")==4,"native arm phase and metadata restore");require(raised(mob,goal)==remaining,"same remaining native ticks to raised arms");}
    }
    private static int raised(Zombie mob,ZombieAttackGoal goal){int ticks=0;while(!mob.isAggressive()&&ticks<24){goal.tick();ticks++;}require(mob.isAggressive()&&ticks<24,"bounded native arm transition");return ticks;}
    private static final class ProbeDolphin extends Dolphin {
        int jumps;
        ProbeDolphin(net.minecraft.server.level.ServerLevel level){super(EntityTypes.DOLPHIN,level);}
        @Override public void playSound(SoundEvent sound,float volume,float pitch){if(sound==SoundEvents.DOLPHIN_JUMP)jumps++;super.playSound(sound,volume,pitch);}
    }
}
