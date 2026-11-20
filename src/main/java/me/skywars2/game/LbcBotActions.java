package me.skywars2.game;

import me.skywars2.mixin.LargeFireballAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Shared physical action admission for the Chef and actual player bots. */
final class LbcBotActions {
    private LbcBotActions(){}
    /** Low arrow arc with native air drag, gravity and a bounded lead of the moving target. */
    static Vec3 arrowAim(LivingEntity actor,LivingEntity target,double speed){
        var eye=actor.getEyePosition();var origin=eye.add(0,-.1,0);var centre=target.getBoundingBox().getCenter();
        var motion=target.onGround()?target.getDeltaMovement().multiply(1,0,1):target.getDeltaMovement();
        var inherited=arrowInheritedMotion(actor);
        double ticks=Math.min(20,origin.distanceTo(centre)/speed);Vec3 launch=centre.subtract(origin);
        for(int i=0;i<4;i++){
            double drag=(1-Math.pow(.99,ticks))/.01;
            double fall=.05/.01*(ticks-drag);
            launch=centre.add(motion.scale(ticks)).add(0,fall,0).subtract(origin).scale(1/Math.max(.001,drag)).subtract(inherited);
            ticks=Math.max(.01,Math.min(20,ticks*launch.length()/speed));
        }
        // The camera is at eye height; the projectile is created 0.1 blocks below it.
        return eye.add(launch);
    }
    private static Vec3 arrowInheritedMotion(LivingEntity actor){
        if(actor.getMainHandItem().getItem() instanceof net.minecraft.world.item.CrossbowItem)return Vec3.ZERO;
        var motion=actor.getKnownMovement();return actor.onGround()?motion.multiply(1,0,1):motion;
    }
    /** Check the actual curved flight, not just the straight eye-to-eye visibility ray. */
    static boolean clearArrowShot(LivingEntity actor,LivingEntity target,Vec3 aim,double speed){
        var eye=actor.getEyePosition();var from=eye.add(0,-.1,0);
        var velocity=aim.subtract(eye).normalize().scale(speed).add(arrowInheritedMotion(actor));
        var motion=target.onGround()?target.getDeltaMovement().multiply(1,0,1):target.getDeltaMovement();
        var direction=target.getBoundingBox().getCenter().subtract(from).normalize();
        double distance=target.getBoundingBox().getCenter().distanceTo(from);
        var start=from;
        for(int tick=0;tick<24;tick++){
            var end=from.add(velocity);
            var expected=target.getBoundingBox().move(motion.scale(tick)).inflate(.25);
            var contact=expected.clip(from,end);
            var wall=actor.level().clip(new ClipContext(from,contact.orElse(end),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,actor));
            if(wall.getType()!=HitResult.Type.MISS)return false;
            if(contact.isPresent()||end.subtract(start).dot(direction)>=distance)return true;
            from=end;velocity=velocity.scale(.99).add(0,-.05,0);
        }
        return false;
    }
    static BlockHitResult ray(LivingEntity actor,double range){
        var eye=actor.getEyePosition();return actor.level().clip(new ClipContext(eye,eye.add(actor.getLookAngle().scale(range)),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,actor));
    }
    static BlockHitResult placement(LivingEntity actor,BlockPos wanted,double range){
        var hit=ray(actor,range);
        return hit.getType()==HitResult.Type.BLOCK&&hit.getBlockPos().relative(hit.getDirection()).equals(wanted)?hit:null;
    }
    /** A usable face can be visible while the block centre is hidden by nearby furniture. */
    static Vec3 visibleBlockAim(LivingEntity actor,BlockPos block,Vec3 feet,double range){
        var eye=feet.add(0,actor.getEyePosition().y-actor.getY(),0);var center=Vec3.atCenterOf(block);
        for(int face=-1;face<net.minecraft.core.Direction.values().length;face++){
            var direction=face<0?null:net.minecraft.core.Direction.values()[face];
            var at=direction==null?center:center.add(direction.getStepX()*.499,direction.getStepY()*.499,direction.getStepZ()*.499);
            if(eye.distanceToSqr(at)>range*range)continue;
            var hit=actor.level().clip(new ClipContext(eye,at,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,actor));
            if(hit.getType()==HitResult.Type.BLOCK&&hit.getBlockPos().equals(block))return at;
        }
        return null;
    }
    static boolean placeHeld(net.minecraft.server.level.ServerPlayer actor,BlockPos wanted){
        var hit=placement(actor,wanted,actor.blockInteractionRange());if(hit==null)return false;
        var stack=actor.getMainHandItem();int before=stack.getCount();
        var result=actor.gameMode.useItemOn(actor,actor.level(),stack,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        return result.consumesAction()&&!actor.level().getBlockState(wanted).canBeReplaced()&&stack.getCount()<before;
    }
    static boolean melee(LivingEntity actor,LivingEntity target,double range){
        return melee(actor,(net.minecraft.world.entity.Entity)target,range);
    }
    static boolean melee(LivingEntity actor,net.minecraft.world.entity.Entity target,double range){
        var eye=actor.getEyePosition();var end=eye.add(actor.getLookAngle().scale(range));
        var wall=actor.level().clip(new ClipContext(eye,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,actor));
        if(wall.getType()!=HitResult.Type.MISS)end=wall.getLocation();
        var box=target.getBoundingBox().inflate(target.getPickRadius());
        return box.contains(eye)||box.clip(eye,end).isPresent();
    }
    /** Only trajectories which can reach this body soon; do not react to every projectile nearby. */
    static net.minecraft.world.entity.projectile.Projectile incoming(LivingEntity actor){
        net.minecraft.world.entity.projectile.Projectile closest=null;double soonest=Double.POSITIVE_INFINITY;
        for(var shot:actor.level().getEntitiesOfClass(net.minecraft.world.entity.projectile.Projectile.class,actor.getBoundingBox().inflate(16),e->e.isAlive()&&e.getOwner()!=actor)){
            double time=impactTime(actor,shot);if(time<soonest){soonest=time;closest=shot;}
        }
        return closest;
    }
    static AABB projectileTarget(Projectile shot,AABB body){
        // Hostile Terra samples occupy the positive unit cube from each point on the ray.
        // Move the cube onto the target in the opposite direction for the line test.
        if(shot instanceof LbcTerra.TerraProjectile terra&&terra.emitterStyle)
            return body.expandTowards(-1,-1,-1);
        if(shot instanceof LbcAir.Charge)return body;
        return body.inflate(.35);
    }
    /** Recheck one retained threat without rescanning nearby entities every camera frame. */
    static double impactTime(LivingEntity actor,net.minecraft.world.entity.projectile.Projectile shot){
        if(!shot.isAlive()||shot.getOwner()==actor||shot.level()!=actor.level())return Double.POSITIVE_INFINITY;
        if(shot instanceof LargeFireball fireball&&((LargeFireballAccess)fireball).skywars2$explosionPower()>0
                ||shot instanceof LbcAir.Charge air&&air.emitterStyle){
            var blast=blastImpact(actor,shot);
            return blast==null?Double.POSITIVE_INFINITY:blast.ticks();
        }
        var motion=shot.getDeltaMovement();double speed=motion.length();if(speed<.05)return Double.POSITIVE_INFINITY;
        var from=shot.position();var end=from.add(motion.scale(Math.min(20,16/speed)));
        var hit=LbcProjectileSweep.entry(projectileTarget(shot,actor.getBoundingBox()),from,end);
        if(hit==null){
            // A nearby visible homing bullet need not point along its eventual approach yet.
            if(shot instanceof net.minecraft.world.entity.projectile.ShulkerBullet&&actor.distanceToSqr(shot)<=64){
                var sight=actor.level().clip(new ClipContext(from,actor.getBoundingBox().getCenter(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,shot));
                if(sight.getType()==HitResult.Type.MISS)return from.distanceTo(actor.getBoundingBox().getCenter())/speed;
            }
            return Double.POSITIVE_INFINITY;
        }
        var wall=actor.level().clip(new ClipContext(from,hit,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,shot));
        return wall.getType()==HitResult.Type.MISS?from.distanceTo(hit)/speed:Double.POSITIVE_INFINITY;
    }
    static final double AIR_BLAST_RADIUS=8;
    record BlastImpact(Vec3 point,double radius,double ticks,boolean hitsActor,int explosions){}
    static BlastImpact blastImpact(LivingEntity actor,Projectile shot){
        if(!shot.isAlive()||shot.getOwner()==actor||shot.level()!=actor.level())return null;
        var motion=shot.getDeltaMovement();
        double speed=motion.length();
        if(speed<.05)return null;
        var from=shot.position();
        var end=from.add(motion.scale(Math.min(20,16/speed)));
        var body=actor.getBoundingBox();
        if(shot instanceof LbcAir.Charge air&&air.emitterStyle){
            var hit=LbcAir.forecastContact(air,end,null);
            if(hit==null||body.distanceToSqr(hit.getLocation())>AIR_BLAST_RADIUS*AIR_BLAST_RADIUS)return null;
            // The unchanged legacy payload creates TNT with fuse one at this contact.
            boolean hitsActor=hit instanceof EntityHitResult entity&&entity.getEntity()==actor;
            return new BlastImpact(hit.getLocation(),AIR_BLAST_RADIUS,from.distanceTo(hit.getLocation())/speed+1,
                    hitsActor,LbcAir.EMITTER_TNT_COUNT);
        }
        if(!(shot instanceof LargeFireball fireball))return null;
        double radius=2.0*((LargeFireballAccess)fireball).skywars2$explosionPower();
        if(radius<=0)return null;
        var contact=body.inflate(.35).clip(from,end);
        var wall=actor.level().clip(new ClipContext(from,contact.orElse(end),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,shot));
        var point=wall.getType()==HitResult.Type.MISS?contact.orElse(null):wall.getLocation();
        if(point==null||body.distanceToSqr(point)>radius*radius)return null;
        return new BlastImpact(point,radius,from.distanceTo(point)/speed,
                wall.getType()==HitResult.Type.MISS&&contact.isPresent(),1);
    }
}
