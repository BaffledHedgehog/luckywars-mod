package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
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
    /** Recheck one retained threat without rescanning nearby entities every camera frame. */
    static double impactTime(LivingEntity actor,net.minecraft.world.entity.projectile.Projectile shot){
        if(!shot.isAlive()||shot.getOwner()==actor||shot.level()!=actor.level())return Double.POSITIVE_INFINITY;
        var motion=shot.getDeltaMovement();double speed=motion.length();if(speed<.05)return Double.POSITIVE_INFINITY;
        var from=shot.position();var end=from.add(motion.scale(Math.min(20,16/speed)));
        var hit=actor.getBoundingBox().inflate(.35).clip(from,end);
        if(hit.isEmpty()){
            // A nearby visible homing bullet need not point along its eventual approach yet.
            if(shot instanceof net.minecraft.world.entity.projectile.ShulkerBullet&&actor.distanceToSqr(shot)<=64){
                var sight=actor.level().clip(new ClipContext(from,actor.getBoundingBox().getCenter(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,shot));
                if(sight.getType()==HitResult.Type.MISS)return from.distanceTo(actor.getBoundingBox().getCenter())/speed;
            }
            return Double.POSITIVE_INFINITY;
        }
        var wall=actor.level().clip(new ClipContext(from,hit.get(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,shot));
        return wall.getType()==HitResult.Type.MISS?from.distanceTo(hit.get())/speed:Double.POSITIVE_INFINITY;
    }
}
