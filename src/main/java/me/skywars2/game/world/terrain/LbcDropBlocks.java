package me.skywars2.game.world.terrain;

import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.Set;
import java.util.function.Predicate;

/** Native fill semantics shared by terrain drops; each source fill keeps its own updates. */
public final class LbcDropBlocks {
    private LbcDropBlocks() { }

    private static final int STRICT = Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ALL_SIDEEFFECTS;
    private static final int DEFERRED = Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;
    /** Same flags, neighbour-shape resolution and deferred updates as native /fill replace. */
    public static void fill(ServerLevel level, BlockPos from, BlockPos to, Block block, boolean strict) {
        fill(level, from, to, block, strict, state -> true);
    }
    /** Source replace predicates share the same fill limits and native update semantics. */
    public static void fill(ServerLevel level, BlockPos from, BlockPos to, Block block, boolean strict,
            Predicate<BlockState> replace) {
        fill(level, from, to, block, strict, replace, false);
    }

    public static void fillPreservingInventory(ServerLevel level, BlockPos from, BlockPos to, Block block, boolean strict) {
        fill(level, from, to, block, strict, state -> true, true);
    }

    private static void fill(ServerLevel level, BlockPos from, BlockPos to, Block block, boolean strict,
            Predicate<BlockState> replace, boolean preserveInventory) {
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
            if (place(level, pos, before, input, strict ? STRICT : DEFERRED, preserveInventory) && updated != null)
                updated.add(new Updated(pos.immutable(), old));
        }
        if (updated != null) for (var update : updated) level.updateNeighboursOnBlockSet(update.pos(), update.oldState());
    }

    /** Native /setblock has no fill-volume gamerule check, even for a one-cell write. */
    public static void setBlock(ServerLevel level, BlockPos pos, BlockState state, boolean strict) {
        setBlock(level, pos, state, strict, false);
    }

    public static void setBlockPreservingInventory(ServerLevel level, BlockPos pos, BlockState state, boolean strict) {
        setBlock(level, pos, state, strict, true);
    }

    /** For gameplay that uses native setBlock flags directly instead of command shape resolution. */
    public static boolean setBlockPreservingInventory(ServerLevel level, BlockPos pos, BlockState state, int flags) {
        var contents = inventoryAt(level, pos, level.getBlockState(pos));
        try { return level.setBlock(pos, state, flags); }
        finally { if (contents != null) contents.dropIfRemoved(level); }
    }

    private static void setBlock(ServerLevel level, BlockPos pos, BlockState state, boolean strict, boolean preserveInventory) {
        if (level.isDebug() || level.isOutsideBuildHeight(pos)) return;
        var before = strict && !preserveInventory ? null : level.getBlockState(pos);
        var input = new BlockInput(state, Set.of(), null);
        if (place(level, pos, before, input, strict ? STRICT : DEFERRED, preserveInventory) && !strict)
            level.updateNeighboursOnBlockSet(pos, before);
    }

    private static boolean place(ServerLevel level, BlockPos pos, BlockState before, BlockInput input,
            int flags, boolean preserveInventory) {
        if (!preserveInventory) return input.place(level, pos, flags);
        var contents = inventoryAt(level, pos, before);
        try { return input.place(level, pos, flags); }
        finally { if (contents != null) contents.dropIfRemoved(level); }
    }

    public static ContainerReplacement inventoryAt(ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.hasBlockEntity()) return null;
        var block = level.getBlockEntity(pos);
        return block instanceof Container || block instanceof CampfireBlockEntity || block instanceof LecternBlockEntity
                ? new ContainerReplacement(pos.immutable(), state, block) : null;
    }

    /** Keeps the original native block entity until the write commits. Its lazy loot is still unopened. */
    public record ContainerReplacement(BlockPos position, BlockState state, BlockEntity entity) {
        public void dropIfRemoved(ServerLevel level) {
            if (!entity.isRemoved()) return;
            if (entity instanceof ShulkerBoxBlockEntity) {
                // Native block loot stores the original slots and unopened loot metadata in the box itself.
                for (var stack : Block.getDrops(state, level, position, entity)) drop(level, position, stack);
                return;
            }
            if (entity instanceof LecternBlockEntity lectern) {
                lectern.preRemoveSideEffects(position, state);
                return;
            }
            if (entity instanceof CampfireBlockEntity campfire) {
                var items = campfire.getItems();
                for (int slot = 0; slot < items.size(); slot++)
                    drop(level, position, items.set(slot, ItemStack.EMPTY));
                return;
            }
            var container = (Container) entity;
            // Drain the actual slot, without its transfer cap or a removed jukebox's change callback.
            for (int slot = 0; slot < container.getContainerSize(); slot++)
                drop(level, position, container.getItem(slot).copyAndClear());
        }
    }

    private static void drop(ServerLevel level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) return;
        var item = new ItemEntity(EntityTypes.ITEM, level);
        var random = item.getRandom();
        item.setItem(stack);
        item.setPos(pos.getX() + .25 + random.nextDouble() * .5,
                pos.getY() + .25 + random.nextDouble() * .5, pos.getZ() + .25 + random.nextDouble() * .5);
        item.setDeltaMovement(random.triangle(0, .11485), random.triangle(.2, .11485), random.triangle(0, .11485));
        // One publication per original slot, including oversized counts. Ordinary ground merging handles it.
        level.addFreshEntity(item);
    }
}
