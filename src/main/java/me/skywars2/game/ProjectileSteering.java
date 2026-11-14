package me.skywars2.game;

import net.minecraft.world.phys.Vec3;

/** A bounded angular step, with exact final alignment instead of an asymptotic vector blend. */
final class ProjectileSteering {
    private ProjectileSteering() { }
    static Vec3 turn(Vec3 motion,Vec3 toward,double limit){
        double speed=motion.length(),distance=toward.length();
        if(speed<1e-12||distance<1e-12||!Double.isFinite(speed)||!Double.isFinite(distance)||!(limit>0))return motion;
        var a=motion.scale(1/speed);var b=toward.scale(1/distance);
        double dot=Math.max(-1,Math.min(1,a.dot(b))),angle=Math.acos(dot);
        if(angle<1e-12)return motion;
        if(angle<=limit)return b.scale(speed);
        var perpendicular=b.subtract(a.scale(dot));
        if(perpendicular.lengthSqr()<1e-24)perpendicular=a.cross(Math.abs(a.y)<.9?new Vec3(0,1,0):new Vec3(1,0,0));
        return a.scale(Math.cos(limit)).add(perpendicular.normalize().scale(Math.sin(limit))).normalize().scale(speed);
    }
}
