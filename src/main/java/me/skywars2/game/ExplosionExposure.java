package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;

/** Block-only clipping for ServerExplosion's COLLIDER / Fluid.NONE exposure rays. */
public final class ExplosionExposure {
    private ExplosionExposure() {}

    public static BlockHitResult clipSolid(BlockGetter blocks, ClipContext context) {
        var from = context.getFrom();
        var to = context.getTo();
        return BlockGetter.traverseBlocks(from, to, context, (ray, pos) -> {
            var state = blocks.getBlockState(pos);
            if (state.isAir()) return null;

            var shape = ray.getBlockShape(state, blocks, pos);
            return blocks.clipWithInteractionOverride(from, to, pos, shape, state);
        }, ray -> BlockHitResult.miss(to, Direction.getApproximateNearest(from.subtract(to)),
                BlockPos.containing(to)));
    }
}
