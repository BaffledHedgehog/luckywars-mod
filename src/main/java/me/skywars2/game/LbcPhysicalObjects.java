package me.skywars2.game;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

/** Shared source display-object movement; callers own contacts, actors and lifetime. */
final class LbcPhysicalObjects {
    private LbcPhysicalObjects(){}
    @FunctionalInterface interface Segment { boolean stop(Vec3 from,Vec3 to); }
    /** Both callbacks run before movement. Return true to retain the caller's contact position/velocity. */
    static void step(Entity actor,double halfExtent,double bounce,double friction,double airFriction,
            Segment beforeClip,Segment beforeMove){
        var level=(ServerLevel)actor.level();var velocity=actor.getDeltaMovement().add(0,-.08,0);
        int n=Math.max(1,(int)Math.ceil(Math.max(Math.abs(velocity.x),Math.max(Math.abs(velocity.y),Math.abs(velocity.z)))/.5));
        var step=velocity.scale(1d/n);boolean ground=false;
        for(int i=0;i<n&&!actor.isRemoved();i++)for(var axis:Direction.Axis.values()){
            double requested=axis==Direction.Axis.X?step.x:axis==Direction.Axis.Y?step.y:step.z;
            var delta=axis==Direction.Axis.X?new Vec3(requested,0,0):axis==Direction.Axis.Y?new Vec3(0,requested,0):new Vec3(0,0,requested);
            var from=actor.position();if(beforeClip.stop(from,from.add(delta)))return;
            var box=new AABB(from,from).inflate(halfExtent);
            double allowed=Shapes.collide(axis,box,level.getBlockCollisions(actor,box.expandTowards(delta)),requested);
            var actual=axis==Direction.Axis.X?new Vec3(allowed,0,0):axis==Direction.Axis.Y?new Vec3(0,allowed,0):new Vec3(0,0,allowed);
            if(beforeMove.stop(from,from.add(actual)))return;
            actor.setPos(from.add(actual));
            if(Math.abs(allowed-requested)>1e-9){
                if(axis==Direction.Axis.Y&&requested<0)ground=true;
                velocity=axis==Direction.Axis.X?new Vec3(-velocity.x*bounce,velocity.y,velocity.z):axis==Direction.Axis.Y?new Vec3(velocity.x,-velocity.y*bounce,velocity.z):new Vec3(velocity.x,velocity.y,-velocity.z*bounce);
            }
        }
        double drag=ground?friction:airFriction;
        actor.setDeltaMovement(velocity.multiply(drag,ground?Math.max(0,drag-.02):drag,drag));
    }
}
