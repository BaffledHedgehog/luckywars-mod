package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Entity contact for simulated projectiles whose movement is a swept segment. */
final class LbcProjectileSweep {
    private static final double HALF_ARROW = EntityTypes.ARROW.getDimensions().width() / 2.0;

    private LbcProjectileSweep() {}

    static AABB arrowBounds(AABB box) {
        return box.inflate(HALF_ARROW);
    }

    static Vec3 entry(AABB box, Vec3 from, Vec3 to) {
        return box.contains(from) ? from : box.clip(from, to).orElse(null);
    }

    static Vec3 arrowContact(ServerLevel level, Entity source, AABB body, Vec3 from, Vec3 to, boolean throughBlocks) {
        var point = entry(arrowBounds(body), from, to);
        if (point == null || throughBlocks) return point;

        var center = body.getCenter();
        var surface = body.clip(point, center).orElse(center);
        // A grazing contact also needs a clear path from the sweep to the actual target.
        // The resident traversal treats unavailable chunks as solid.
        return LbcTerra.surfaceHit(level, point, surface, source) == null ? point : null;
    }

    private static Vec3 contact(ServerLevel level, Entity source, Entity target, Vec3 from, Vec3 to, boolean throughBlocks) {
        var body = target.getBoundingBox();
        var point = entry(body, from, to);
        return point != null ? point : arrowContact(level, source, body, from, to, throughBlocks);
    }

    static EntityHitResult first(ServerLevel level, Entity source, Vec3 from, Vec3 to, Predicate<Entity> target, boolean throughBlocks) {
        double nearestDistance = Double.POSITIVE_INFINITY;
        EntityHitResult nearest = null;
        for (var entity : level.getEntities(source, arrowBounds(new AABB(from, to)), target)) {
            var point = contact(level, source, entity, from, to, throughBlocks);
            if (point == null) continue;
            double distance = point.distanceToSqr(from);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = new EntityHitResult(entity, point);
            }
        }
        return nearest;
    }

    static List<EntityHitResult> all(ServerLevel level, Entity source, Vec3 from, Vec3 to, Predicate<Entity> target) {
        var hits = new ArrayList<EntityHitResult>();
        for (var entity : level.getEntities(source, arrowBounds(new AABB(from, to)), target)) {
            var point = contact(level, source, entity, from, to, false);
            if (point != null) hits.add(new EntityHitResult(entity, point));
        }
        return hits;
    }
}
