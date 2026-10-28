package me.skywars2.game;

import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.Set;

/** Native fill semantics shared by terrain drops; each source fill keeps its own updates. */
final class LbcDropBlocks {
    private LbcDropBlocks() { }
    /** Same flags, neighbour-shape resolution and deferred updates as native /fill replace. */
    static void fill(ServerLevel level, BlockPos from, BlockPos to, Block block, boolean strict) {
        fill(level, from, to, block, strict, state -> true);
    }
    /** Source replace predicates share the same fill limits and native update semantics. */
    static void fill(ServerLevel level, BlockPos from, BlockPos to, Block block, boolean strict,
            java.util.function.Predicate<BlockState> replace) {
        if (level.isOutsideBuildHeight(from) || level.isOutsideBuildHeight(to)) return;
        long volume = ((long)Math.abs(to.getX() - from.getX()) + 1)
                * ((long)Math.abs(to.getY() - from.getY()) + 1)
                * ((long)Math.abs(to.getZ() - from.getZ()) + 1);
        if (volume > level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS)) return;
        var input = new BlockInput(block.defaultBlockState(), Set.of(), null);
        record Updated(BlockPos pos, BlockState oldState) { }
        var updated = strict ? null : new ArrayList<Updated>();
        for (var pos : BlockPos.betweenClosed(Math.min(from.getX(),to.getX()), Math.min(from.getY(),to.getY()), Math.min(from.getZ(),to.getZ()),
                Math.max(from.getX(),to.getX()), Math.max(from.getY(),to.getY()), Math.max(from.getZ(),to.getZ()))) {
            var before = level.getBlockState(pos);
            if (!replace.test(before)) continue;
            var old = strict ? null : before;
            if (input.place(level, pos, strict ? 818 : 258) && updated != null) updated.add(new Updated(pos.immutable(), old));
        }
        if (updated != null) for (var update : updated) level.updateNeighboursOnBlockSet(update.pos(), update.oldState());
    }

    /** Native /setblock has no fill-volume gamerule check, even for a one-cell write. */
    static void setBlock(ServerLevel level, BlockPos pos, BlockState state, boolean strict) {
        if (level.isDebug() || level.isOutsideBuildHeight(pos)) return;
        var old = strict ? null : level.getBlockState(pos);
        var input = new BlockInput(state, Set.of(), null);
        if (input.place(level, pos, strict ? 818 : 258) && !strict)
            level.updateNeighboursOnBlockSet(pos, old);
    }

}
