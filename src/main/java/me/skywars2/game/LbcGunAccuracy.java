package me.skywars2.game;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** Shot-direction error only; gun recoil and muzzle positions remain independent. */
final class LbcGunAccuracy {
    static final double HIP_DEGREES=5, MAX_DEGREES=20, FULL_PENALTY_SPEED=.3;
    private LbcGunAccuracy() { }

    static double movementDegrees(ServerPlayer player) {
        double speed=Math.max(speed(player.getKnownMovement(),player.onGround()),
                speed(player.getDeltaMovement(),player.onGround()));
        return MAX_DEGREES*Math.min(1,speed/FULL_PENALTY_SPEED);
    }
    private static double speed(Vec3 motion,boolean grounded) {
        // Ignore standing gravity/contact jitter, but include jumps, falls and external pushes.
        double vertical=grounded?0:motion.y;
        double speed=Math.sqrt(motion.horizontalDistanceSqr()+vertical*vertical);
        return speed>.01?speed:0;
    }
    /** Half-angle of the next rifle shot; reading presentation state must never sample the RNG. */
    static double rifleDegrees(GameController game,ServerPlayer player) {
        double degrees=game.items().rifleZoom().scoped(player)?0:HIP_DEGREES;
        return Math.min(MAX_DEGREES,degrees+movementDegrees(player));
    }
    static double ak47Degrees(ServerPlayer player) {
        return movementDegrees(player);
    }
    /** Source narrowing occurs once per accepted shot, before either angular offset is sampled. */
    static int minigunNextShotSpread(int currentSpread) {
        return currentSpread>=1100?currentSpread-100:currentSpread;
    }
    /** Minigun offsets form a yaw/pitch square, not the rifle's solid-angle cone. */
    static float minigunDegrees(int spread) {
        return spread*.001f;
    }
    static double minigunNextShotDegrees(GameController game,ServerPlayer player) {
        return minigunDegrees(minigunNextShotSpread(game.items().laserMinigun().spread(player)));
    }
    static Vec3 rifle(GameController game,ServerPlayer player) {
        return sample(player.getLookAngle(),rifleDegrees(game,player),player.getRandom());
    }
    static Vec3 ak47(ServerPlayer player) {
        return sample(player.getLookAngle(),ak47Degrees(player),player.getRandom());
    }
    static Vec3 sample(Vec3 forward,double degrees,RandomSource random) {
        if(degrees==0)return forward;
        // Uniform solid-angle cone, with a stable basis even when looking straight up/down.
        var axis=forward.normalize();
        var side=axis.cross(Math.abs(axis.y)>.99?new Vec3(1,0,0):new Vec3(0,1,0)).normalize();
        var up=side.cross(axis);
        double cosine=1-random.nextDouble()*(1-Math.cos(Math.toRadians(degrees)));
        double sine=Math.sqrt(Math.max(0,1-cosine*cosine)),angle=random.nextDouble()*Math.PI*2;
        return axis.scale(cosine).add(side.scale(sine*Math.cos(angle))).add(up.scale(sine*Math.sin(angle)));
    }
}
