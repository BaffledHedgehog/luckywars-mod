package me.skywars2.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Native weapon reassessment must not preserve future dormant-task counters or replay lifecycle callbacks. */
final class VanityRetainedGoalChecks {
    private final GameController game;
    private final WoolBucketChecks.Fixture f;
    private final ProbeSkeleton skeleton;
    private final Cow target;
    private final RangedBowAttackGoal<ProbeSkeleton> custom;
    private final Object heldBow,heldMelee;
    private final MatchActorCheckpoint image;
    private final long capturedWorld,savedCanUse;
    private final int savedStrafe,savedAttack;
    private int phase,when,cycles;
    private float health;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity retained goals: "+why);}
    private static int count(Object object,String field){return (Integer)WoolBucketChecks.read(object,field);}
    VanityRetainedGoalChecks(GameController game,WoolBucketChecks.Fixture f){
        this.game=game;this.f=f;
        VanityRoamingGoalChecks.verify(game,f);
        VanityPositionGoalChecks.verify(game,f);
        VanityPetGoalChecks.verify(game,f);
        var at=Vec3.atCenterOf(BlockPos.containing(f.home).above(3)).add(3,8,3);
        target=new Cow(EntityTypes.COW,f.level);target.setPos(at.add(10,0,0));target.setNoGravity(true);target.setNoAi(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        skeleton=new ProbeSkeleton(f.level);skeleton.setPos(at);skeleton.setNoGravity(true);skeleton.setNoAi(true);
        skeleton.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.LEATHER_HELMET));skeleton.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));
        skeleton.reassessWeaponGoal();
        require(f.level.addFreshEntity(target)&&f.level.addFreshEntity(skeleton),"ordinary registered native bodies");f.own(target);f.own(skeleton);
        heldBow=WoolBucketChecks.read(skeleton,"bowGoal");heldMelee=WoolBucketChecks.read(skeleton,"meleeGoal");
        skeleton.goals().removeAllGoals(g->true);
        ((GoalSelector)WoolBucketChecks.read(skeleton,"targetSelector")).removeAllGoals(g->true);
        custom=new RangedBowAttackGoal<>(skeleton,1,40,16);skeleton.goals().addGoal(1,custom);skeleton.goals().addGoal(9,skeleton.probe);
        skeleton.setTarget(target);skeleton.goals().tick();
        require(skeleton.goals().getAvailableGoals().stream().noneMatch(w->w.getGoal()==heldBow||w.getGoal()==heldMelee),"native retained weapon tasks are genuinely absent from selectors at capture");
        savedStrafe=count(heldBow,"strafingTime");savedAttack=count(heldMelee,"ticksUntilNextAttack");savedCanUse=(Long)WoolBucketChecks.read(heldMelee,"lastCanUseCheck");
        capturedWorld=f.level.getGameTime();image=MatchActorCheckpoint.captureRegistered(game);future();
    }
    private void future(){
        skeleton.goals().removeGoal(custom);skeleton.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));skeleton.reassessWeaponGoal();skeleton.setNoAi(false);
        phase=0;when=game.server().getTickCount();
    }
    boolean step(){
        f.client.acknowledge();f.audience.acknowledge();require(game.server().getTickCount()-when<120,"bounded native weapon activation");
        for(var shot:f.level.getEntitiesOfClass(Projectile.class,skeleton.getBoundingBox().inflate(64),p->p.getOwner()==skeleton))f.own(shot);
        if(phase==0){
            if(count(heldBow,"seeTime")<20||count(heldBow,"strafingTime")==savedStrafe)return false;
            skeleton.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.STICK));skeleton.reassessWeaponGoal();
            require(count(heldBow,"strafingTime")!=savedStrafe,"native stop leaves a genuinely changed dormant bow counter");
            target.setPos(skeleton.position().add(1,0,0));target.setDeltaMovement(Vec3.ZERO);target.damageCooldownTime=0;health=target.getHealth();phase=1;when=game.server().getTickCount();return false;
        }
        if(target.getHealth()>=health||count(heldMelee,"ticksUntilNextAttack")==savedAttack)return false;
        require(count(heldMelee,"ticksUntilNextAttack")>0,"actual native melee hit changes the retained melee cooldown");
        int starts=skeleton.probe.starts,stops=skeleton.probe.stops;
        boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
        try{image.restoreBodiesFrozen();require(image.restoreLinksFrozen().isEmpty(),"all retained-task dependencies resolve");}
        finally{game.server().tickRateManager().setFrozen(frozen);}
        require(skeleton.probe.starts==starts&&skeleton.probe.stops==stops,"native NBT/equipment reassessment cannot replay goal lifecycle callbacks");
        require(WoolBucketChecks.read(skeleton,"bowGoal")==heldBow&&WoolBucketChecks.read(skeleton,"meleeGoal")==heldMelee,"same original species-held task identities");
        require(count(heldBow,"strafingTime")==savedStrafe&&count(heldMelee,"ticksUntilNextAttack")==savedAttack,"dormant task counters rewind even when absent from both selectors");
        require((Long)WoolBucketChecks.read(heldMelee,"lastCanUseCheck")==savedCanUse+f.level.getGameTime()-capturedWorld,"dormant melee world stamp retains its original age");
        require(skeleton.goals().getAvailableGoals().stream().noneMatch(w->w.getGoal()==heldBow||w.getGoal()==heldMelee),"restored selectors do not accidentally activate retained tasks");
        if(cycles++==0){future();return false;}
        skeleton.goals().removeGoal(custom);skeleton.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));skeleton.reassessWeaponGoal();skeleton.goals().tick();
        require(count(heldBow,"seeTime")==1&&count(heldBow,"strafingTime")==savedStrafe&&skeleton.isUsingItem(),"later native activation begins from the restored dormant bow phase");
        skeleton.setNoAi(true);return true;
    }
    private static final class Probe extends Goal {
        int starts,stops;
        Probe(){setFlags(EnumSet.of(Flag.JUMP));}
        @Override public boolean canUse(){return true;}
        @Override public void start(){starts++;}
        @Override public void stop(){stops++;}
    }
    private static final class ProbeSkeleton extends Skeleton {
        private Probe probe;
        ProbeSkeleton(ServerLevel level){super(EntityTypes.SKELETON,level);probe=new Probe();reassessWeaponGoal();}
        GoalSelector goals(){return goalSelector;}
        @Override public void reassessWeaponGoal(){
            super.reassessWeaponGoal();
            if(probe!=null){goalSelector.removeGoal(probe);goalSelector.addGoal(9,probe);}
        }
    }
}
