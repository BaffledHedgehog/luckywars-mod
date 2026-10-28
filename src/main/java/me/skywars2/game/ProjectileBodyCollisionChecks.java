package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** Native pair impulses, without publishing actors or advancing the world clock. */
public final class ProjectileBodyCollisionChecks {
    private ProjectileBodyCollisionChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Projectile body collision: "+why);}
    public static void run(GameController game){
        var level=game.server().overworld();
        var mob=new Cow(EntityTypes.COW,level);var control=new Cow(EntityTypes.COW,level);
        List<Projectile> shots=List.of(new Arrow(EntityTypes.ARROW,level),new Snowball(EntityTypes.SNOWBALL,level),
                new SmallFireball(EntityTypes.SMALL_FIREBALL,level),new ShulkerBullet(EntityTypes.SHULKER_BULLET,level),
                new LbcClouds.AcidProjectile(LbcClouds.ACID_PROJECTILE,level),
                new LbcExasperato.Charge(LbcExasperato.TYPE,level),new LbcFartus.Shot(LbcFartus.TYPE,level));
        try{
            for(var shot:shots){
                shot.setPos(0,200,0);mob.setPos(.2,200,0);
                for(boolean projectileFirst:List.of(true,false)){
                    shot.setDeltaMovement(.13,.07,-.21);mob.setDeltaMovement(-.11,.03,.19);
                    Vec3 projectileVelocity=shot.getDeltaMovement(),mobVelocity=mob.getDeltaMovement();
                    if(projectileFirst)shot.push(mob);else mob.push(shot);
                    require(shot.getDeltaMovement().equals(projectileVelocity)&&mob.getDeltaMovement().equals(mobVelocity),
                            shot.getType()+" preserves both velocities when "+(projectileFirst?"projectile":"mob")+" initiates pair push");
                }
            }
            mob.setPos(0,200,0);control.setPos(.2,200,0);mob.setDeltaMovement(Vec3.ZERO);control.setDeltaMovement(Vec3.ZERO);
            mob.push(control);
            require(mob.getDeltaMovement().x<0&&control.getDeltaMovement().x>0,"ordinary cow bodies still exchange native push impulses");
            var snow=shots.get(1);snow.setDeltaMovement(.13,.07,-.21);var before=snow.getDeltaMovement();
            snow.push(.2,.1,-.3);
            require(snow.getDeltaMovement().distanceTo(before.add(.2,.1,-.3))<1e-10,"explicit projectile velocity impulse remains available");
        }finally{shots.forEach(Entity::discard);mob.discard();control.discard();}
        SkyWars2.LOGGER.info("[check] PASS projectile-body-collision (seven native/custom projectile types, both pair-push directions, ordinary mob and explicit impulse controls; no world ticks)");
    }
}
