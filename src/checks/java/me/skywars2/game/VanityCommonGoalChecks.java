package me.skywars2.game;

import java.util.*;

import me.skywars2.game.match.checkpoints.MatchActorCheckpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Real native bow/crossbow volleys following the synchronous common-task probes. */
final class VanityCommonGoalChecks {
    private final GameController game;
    private final WoolBucketChecks.Fixture f;
    private final Skeleton bow;
    private final Pillager crossbow;
    private final RangedBowAttackGoal<Skeleton> bowGoal;
    private final RangedCrossbowAttackGoal<Pillager> crossbowGoal;
    private final Map<Mob,Set<UUID>> shots=new IdentityHashMap<>();
    private MatchActorCheckpoint image;
    private int phase,when,bowDelay,crossbowDelay;
    private boolean initialBowShot;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity common goals: "+why);}
    private static int count(Object goal,String field){return (Integer)WoolBucketChecks.read(goal,field);}
    VanityCommonGoalChecks(GameController game,WoolBucketChecks.Fixture f){
        this.game=game;this.f=f;
        for(var id:f.owned){var actor=f.level.getEntity(id);if(actor instanceof Mob mob)mob.setNoAi(true);}
        VanityMovementGoalChecks.verify(game,f);
        var at=Vec3.atCenterOf(BlockPos.containing(f.home).above(3)).add(0,.5,0);
        var bowTarget=target(at.add(12,7,-2));var crossbowTarget=target(at.add(12,12,2));
        bow=add(new Skeleton(EntityTypes.SKELETON,f.level),at.add(2,7,-2));
        crossbow=add(new Pillager(EntityTypes.PILLAGER,f.level),at.add(2,12,2));
        bow.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.LEATHER_HELMET));bow.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));
        crossbow.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.CROSSBOW));
        bowGoal=new RangedBowAttackGoal<>(bow,1,40,16);crossbowGoal=new RangedCrossbowAttackGoal<>(crossbow,1,16);
        install(bow,bowGoal);install(crossbow,crossbowGoal);bow.setTarget(bowTarget);crossbow.setTarget(crossbowTarget);
        shots.put(bow,new HashSet<>());shots.put(crossbow,new HashSet<>());when=game.server().getTickCount();
    }
    private <T extends Mob>T add(T mob,Vec3 position){
        mob.setPos(position);mob.setNoGravity(true);mob.setOnGround(true);mob.setPersistenceRequired();
        require(f.level.addFreshEntity(mob),"ordinary native ranged actor registered");f.own(mob);return mob;
    }
    private Cow target(Vec3 position){
        var target=add(new Cow(EntityTypes.COW,f.level),position);target.setNoAi(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);return target;
    }
    private static void install(Mob mob,Goal goal){
        var selector=(GoalSelector)WoolBucketChecks.read(mob,"goalSelector");selector.removeAllGoals(g->true);selector.addGoal(1,goal);
        ((GoalSelector)WoolBucketChecks.read(mob,"targetSelector")).removeAllGoals(g->true);
    }
    boolean step(){
        f.client.acknowledge();f.audience.acknowledge();int now=game.server().getTickCount(),elapsed=now-when;
        require(elapsed<150,"bounded native bow/crossbow continuation phase="+phase);
        observe();
        if(phase==0){
            initialBowShot|=!shots.get(bow).isEmpty();
            if(!initialBowShot||bow.isUsingItem()||count(bowGoal,"attackTime")<8||
                    !WoolBucketChecks.read(crossbowGoal,"crossbowState").toString().equals("CHARGED")||count(crossbowGoal,"attackDelay")<20)return false;
            bowDelay=count(bowGoal,"attackTime");crossbowDelay=count(crossbowGoal,"attackDelay");
            clearShots();image=MatchActorCheckpoint.captureRegistered(game);phase=1;when=now;return false;
        }
        if(elapsed<bowDelay+20)require(shots.get(bow).isEmpty(),"saved bow cooldown and native draw prevent an early shot");
        if(elapsed<crossbowDelay+1)require(shots.get(crossbow).isEmpty(),"saved charged crossbow countdown prevents an early shot");
        if(shots.get(bow).isEmpty()||shots.get(crossbow).isEmpty())return false;
        require(shots.get(bow).size()==1&&shots.get(crossbow).size()==1,"one actual native owner-tagged projectile from each restored task");
        clearShots();
        ((GoalSelector)WoolBucketChecks.read(bow,"goalSelector")).removeAllGoals(g->true);
        ((GoalSelector)WoolBucketChecks.read(crossbow,"goalSelector")).removeAllGoals(g->true);
        require(count(bowGoal,"attackTime")==-1&&count(crossbowGoal,"seeTime")==0,"native stop really reset the task phase");
        restore();
        if(phase++<3){when=now;return false;}
        bow.setNoAi(true);crossbow.setNoAi(true);return true;
    }
    private void observe(){
        for(var shot:f.level.getEntitiesOfClass(Projectile.class,bow.getBoundingBox().inflate(64),p->p.getOwner()==bow||p.getOwner()==crossbow)){
            shots.get((Mob)shot.getOwner()).add(shot.getUUID());f.own(shot);
        }
    }
    private void clearShots(){
        for(var shot:f.level.getEntitiesOfClass(Projectile.class,bow.getBoundingBox().inflate(64),p->p.getOwner()==bow||p.getOwner()==crossbow))shot.discard();
        shots.values().forEach(Set::clear);
    }
    private void restore(){
        boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
        try{image.restoreBodiesFrozen();require(image.restoreLinksFrozen().isEmpty(),"native targets and goal dependencies resolve");}
        finally{game.server().tickRateManager().setFrozen(frozen);}
        require(count(bowGoal,"attackTime")==bowDelay&&count(crossbowGoal,"attackDelay")==crossbowDelay&&
                WoolBucketChecks.read(crossbowGoal,"crossbowState").toString().equals("CHARGED"),"restored bow cooldown and crossbow state machine before any AI step");
    }
}
