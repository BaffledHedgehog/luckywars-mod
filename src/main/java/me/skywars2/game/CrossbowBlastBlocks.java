package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;

/** Skips block discovery when the starting cell absorbs every ray of an accepted Erecto blast. */
public final class CrossbowBlastBlocks {
    private record Scope(ServerLevel level, Vec3 center, DamageSource damage) {}
    private static final ThreadLocal<Scope> ACTIVE = new ThreadLocal<>();

    private CrossbowBlastBlocks() {}

    public static void scoped(ServerLevel level, Vec3 center, DamageSource damage, Runnable action) {
        var previous = ACTIVE.get();
        ACTIVE.set(new Scope(level, center, damage));
        try {
            action.run();
        } finally {
            if (previous == null) ACTIVE.remove();
            else ACTIVE.set(previous);
        }
    }

    public static boolean absorbsAllRays(ServerExplosion blast, ExplosionDamageCalculator calculator) {
        var scope = ACTIVE.get();
        if (scope == null || scope.level != blast.level() || scope.damage != blast.getDamageSource()
                || !scope.center.equals(blast.center()) || blast.getDirectSourceEntity() != null
                || calculator.getClass() != ExplosionDamageCalculator.class
                || Boolean.getBoolean("skywars2.disableExplosionCache")) return false;
        var level = scope.level;
        var pos = BlockPos.containing(scope.center);
        if (!level.isInWorldBounds(pos) || !NativeChunkLoad.resident(level, pos)) return false;
        var resistance = calculator.getBlockExplosionResistance(blast, level, pos, level.getBlockState(pos), level.getFluidState(pos));
        if (resistance.isEmpty() || !((resistance.get() + .3F) * .3F >= blast.radius() * (.7F + .6F))) return false;

        // Vanilla draws one power per boundary ray even when none escapes the first block.
        var random = level.getRandom();
        for (int ray = 0; ray < BlastRayTrace.rayCount(); ray++) random.nextFloat();
        return true;
    }
}
