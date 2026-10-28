package me.skywars2.game;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Exact native hitbox clipping; particle samples and broadphase margins never widen a laser. */
final class LbcBeamGeometry {
    private LbcBeamGeometry() {}

    static boolean intersects(AABB box, Vec3 from, Vec3 to) {
        return box.contains(from) || box.clip(from, to).isPresent();
    }
}
