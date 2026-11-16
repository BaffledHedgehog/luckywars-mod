package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.Vec3;

/** Real owner combat and consumed timestamps, not synthetic target assignment. */
final class VanityPetGoalChecks {
    private VanityPetGoalChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity pet goals: "+why);}
    private static GoalSelector targets(Mob mob){return (GoalSelector)WoolBucketChecks.read(mob,"targetSelector");}
    private static <T extends Mob>T add(WoolBucketChecks.Fixture f,T mob,Vec3 position){
        mob.setPos(position);mob.setNoAi(true);mob.setNoGravity(true);mob.setOnGround(true);
        ((GoalSelector)WoolBucketChecks.read(mob,"goalSelector")).removeAllGoals(g->true);targets(mob).removeAllGoals(g->true);
        require(f.level.addFreshEntity(mob),"ordinary native pet-combat actor");f.own(mob);return mob;
    }
    private static void select(Mob mob,Goal goal){
        var selector=targets(mob);selector.removeAllGoals(g->true);selector.addGoal(1,goal);selector.tick();
        require(selector.getAvailableGoals().iterator().next().isRunning(),"native owner-combat goal really admitted");
    }
    static void verify(GameController game,WoolBucketChecks.Fixture f){
        Vec3 at=Vec3.atCenterOf(BlockPos.containing(f.home).above(3)).add(3,.5,20);f.move(f.owner,at);
        var enemy=add(f,new Zombie(EntityTypes.ZOMBIE,f.level),at.add(-1,0,0));var laterEnemy=add(f,new Zombie(EntityTypes.ZOMBIE,f.level),at.add(-1,0,1));
        var victim=add(f,new Cow(EntityTypes.COW,f.level),at.add(1,0,0));var laterVictim=add(f,new Cow(EntityTypes.COW,f.level),at.add(1,0,1));
        for(var cow:new Cow[]{victim,laterVictim}){cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);cow.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);}
        var defender=add(f,new Wolf(EntityTypes.WOLF,f.level),at.add(3,0,0));var assistant=add(f,new Wolf(EntityTypes.WOLF,f.level),at.add(3,0,1));
        defender.tame(f.owner);assistant.tame(f.owner);defender.setOrderedToSit(false);assistant.setOrderedToSit(false);
        var defend=new OwnerHurtByTargetGoal(defender);var assist=new OwnerHurtTargetGoal(assistant);
        f.owner.setInvulnerableTime(0);f.owner.damageCooldownTime=0;
        require(enemy.doHurtTarget(f.level,f.owner),"genuine native enemy hit on owner");
        float before=victim.getHealth();f.owner.attack(victim);require(victim.getHealth()<before,"genuine accepted native player melee hit");
        select(defender,defend);select(assistant,assist);
        require(defender.getTargetUnchecked()==enemy&&assistant.getTargetUnchecked()==victim&&!defend.canUse()&&!assist.canUse(),"both native pet goals consume actual owner combat exactly once");
        var player=new MatchPlayerCheckpoint(game,f.owner);var image=MatchActorCheckpoint.captureRegistered(game);
        for(int replay=0;replay<2;replay++){
            f.owner.tickCount+=10;f.owner.damageCooldownTime=0;f.owner.setInvulnerableTime(0);
            require(laterEnemy.doHurtTarget(f.level,f.owner),"later genuine native owner injury");
            laterVictim.damageCooldownTime=0;before=laterVictim.getHealth();f.owner.attack(laterVictim);require(laterVictim.getHealth()<before,"later genuine native owner attack");
            select(defender,defend);select(assistant,assist);
            require(defender.getTargetUnchecked()==laterEnemy&&assistant.getTargetUnchecked()==laterVictim,"future native goals really consume different bodies and timestamps");
            boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
            try{image.restoreBodiesFrozen();require(player.restoreFrozen()==f.owner,"living native owner remains the same body");require(image.restoreLinksFrozen().isEmpty(),"all native pet/owner combat links resolve");}
            finally{game.server().tickRateManager().setFrozen(frozen);}
            f.client.acknowledge();
            require(defender.getTargetUnchecked()==enemy&&assistant.getTargetUnchecked()==victim&&
                    WoolBucketChecks.read(defend,"ownerLastHurtBy")==enemy&&WoolBucketChecks.read(assist,"ownerLastHurt")==victim,"actual saved combat bodies, not future cached targets");
            require(!defend.canUse()&&!assist.canUse(),"restored consumed timestamps cannot repeat owner retaliation or assistance");
        }
    }
}
