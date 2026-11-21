package me.skywars2.game;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Native segment clipping and legacy selector dose math, independent of decorative particle samples. */
public final class LbcBeamGeometry {
    private LbcBeamGeometry() {}

    public static boolean intersects(AABB box, Vec3 from, Vec3 to) {
        return box.contains(from) || box.clip(from, to).isPresent();
    }
    private static AABB selectorOrigins(AABB body){return new AABB(body.minX-1,body.minY-1,body.minZ-1,body.maxX,body.maxY,body.maxZ).deflate(1e-7);}
    public static boolean selectorIntersects(AABB body,Vec3 from,Vec3 to){return intersects(selectorOrigins(body),from,to);}
    /** Number of legacy dx=0/dy=0/dz=0 doses, after a separate continuous hit has been admitted. */
    public static int selectorSamples(AABB body,Vec3 from,Vec3 to,double spacing){
        // A selector cell is [sample,sample+1] on each axis. This expands sample origins,
        // not the physical ray's collision width. Strict intersections exclude the cell faces.
        var origins=selectorOrigins(body);
        var entry=origins.contains(from)?from:origins.clip(from,to).orElse(null);
        if(entry==null)return 1;
        var exit=origins.contains(to)?to:origins.clip(to,from).orElse(entry);
        return Math.max(1,(int)Math.floor(from.distanceTo(exit)/spacing)-(int)Math.ceil(from.distanceTo(entry)/spacing)+1);
    }
}
