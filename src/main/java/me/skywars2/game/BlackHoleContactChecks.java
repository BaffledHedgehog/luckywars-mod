package me.skywars2.game;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.Vec3;

/** Native dimensions and HP probes; no actor ticks or world blocks are needed. */
final class BlackHoleContactChecks {
    private BlackHoleContactChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Black hole contact: "+why);}
    private static void near(double actual,double expected,String why){require(Math.abs(actual-expected)<1.0e-8,why+": "+actual);}
    private static final class ProbeCow extends Cow {
        int nativeHurts;
        ProbeCow(net.minecraft.server.level.ServerLevel level){super(EntityTypes.COW,level);}
        @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel level,DamageSource source,float amount){nativeHurts++;return super.hurtServer(level,source,amount);}
    }
    static void run(GameController game,ServerPlayer player){
        var cow=new ProbeCow(player.level());cow.setPos(player.position().add(8,8,8));
        var pose=player.getPose();
        try{
            player.setPose(Pose.STANDING);player.refreshDimensions();
            var standing=player.getBoundingBox();var head=new Vec3(player.getX(),standing.maxY+.5,player.getZ());
            require(player.position().distanceToSqr(head)>1,"head-only fixture excludes feet");
            near(BlackHoleDamage.distanceSquared(player,head),.25,"standing head intersects radius one");
            player.setPose(Pose.SWIMMING);player.refreshDimensions();
            require(player.getBoundingBox().maxY<standing.maxY&&BlackHoleDamage.distanceSquared(player,head)>1,"native swimming pose removes old head contact");
            player.setPose(pose);player.refreshDimensions();

            cow.getAttribute(Attributes.SCALE).setBaseValue(4);cow.refreshDimensions();
            var box=cow.getBoundingBox();var top=new Vec3(cow.getX(),box.maxY+.5,cow.getZ());
            require(cow.position().distanceToSqr(top)>25,"scaled native body extends beyond radius-five feet test");
            near(BlackHoleDamage.distanceSquared(cow,top),.25,"scaled native head contact");
            near(BlackHoleDamage.distanceSquared(cow,box.getCenter()),0,"sphere center inside body");
            var tangent=new Vec3(box.maxX+1,box.getCenter().y,box.getCenter().z);
            near(BlackHoleDamage.distanceSquared(cow,tangent),1,"exact face tangent remains in closed damage sphere");
            var corner=new Vec3(box.maxX+.8,box.maxY+.8,box.maxZ+.8);
            require(box.inflate(1).contains(corner),"corner belongs to broad-phase cube");
            near(BlackHoleDamage.distanceSquared(cow,corner),1.92,"cube corner remains outside spherical damage");

            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);
            cow.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(8);cow.setAbsorptionAmount(8);cow.setInvulnerableTime(0);cow.damageCooldownTime=0;
            cow.hurtTime=0;cow.hurtDuration=0;var velocity=new Vec3(.13,-.2,.31);cow.setDeltaMovement(velocity);
            var source=new DamageSource(player.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                    .getOrThrow(net.minecraft.world.damagesource.DamageTypes.GENERIC),player);
            require(BlackHoleDamage.apply(game,cow,source,.25f)&&BlackHoleDamage.apply(game,cow,source,.25f),"consecutive contacts both reduce HP");
            near(cow.getHealth(),99.5,"exact direct HP reduction");
            require(cow.getAbsorptionAmount()==8,"direct HP does not spend absorption");
            require(cow.nativeHurts==0&&cow.hurtTime==0&&cow.hurtDuration==0&&cow.getInvulnerableTime()==0&&cow.damageCooldownTime==0,
                    "contacts do not invoke native hurt or start hurt animation/cooldowns");
            require(cow.getDeltaMovement().equals(velocity),"HP damage leaves pull velocity unchanged");
            require(cow.getKillCredit()==player,"accepted HP contact records native selected-author credit");
            cow.setPermanentlyInvulnerable(true);float health=cow.getHealth();
            require(!BlackHoleDamage.apply(game,cow,source,1)&&cow.getHealth()==health,"current native invulnerability rejects contact");
        }finally{player.setPose(pose);player.refreshDimensions();cow.discard();}
    }
}
