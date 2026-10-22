package me.skywars2.game;

import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Deferred, ownerless native chest placement for the five source drop flags. */
public final class LbcChestDrops {
    private final GameController game;
    private final Map<ItemEntity, String> pending = new IdentityHashMap<>();
    private final Map<ItemEntity, Integer> lastAttempt = new IdentityHashMap<>();
    private int lastTick = Integer.MIN_VALUE;

    public LbcChestDrops(GameController game) { this.game = game; }

    public void loaded(Entity entity) {
        // /give creates a temporary visual item and marks it fake after ENTITY_LOAD.
        // Do not place blocks from inside the entity insertion callback.
        if (entity instanceof ItemEntity token && LbcChestDropItem.kind(token.getItem()) != null
                && active(token)) pending.putIfAbsent(token, game.items().epoch());
    }

    public void unloaded(Entity entity) {
        pending.remove(entity);
        lastAttempt.remove(entity);
    }

    private boolean active(ItemEntity token) {
        return game.playing() && game.stage() != GameController.Stage.ENDING && token.level() == game.maps().level();
    }

    public void tick() {
        int now = game.server().getTickCount();
        if (lastTick == now) return;
        lastTick = now;
        for (var token : List.copyOf(pending.keySet())) {
            if (token.isRemoved() || token.getAge() >= 5999 || !active(token)
                    || !game.items().currentEpoch(pending.get(token)) || LbcChestDropItem.kind(token.getItem()) == null) {
                unloaded(token);
                continue;
            }
            spawn(token);
            if (token.isRemoved() || token.getItem().isEmpty()) unloaded(token);
        }
    }

    /** Returns whether an active loaded attempt was dispatched, even when native placement fails. */
    boolean spawn(ItemEntity token) {
        if (token.isRemoved() || token.getAge() >= 5999 || !active(token)
                || !(token.level() instanceof ServerLevel level) || game.items().timeStop().stopped(level)) return false;
        if (pending.containsKey(token) && !game.items().currentEpoch(pending.get(token))) return false;
        var first = LbcChestDropItem.kind(token.getItem());
        if (first == null) return false;
        int now = game.server().getTickCount();
        if (lastAttempt.getOrDefault(token, Integer.MIN_VALUE) == now) return false;
        var pos = BlockPos.containing(token.position());
        if (!resident(level, pos)) return false;
        lastAttempt.put(token, now);
        // The source dispatcher executes every matching flag in order, then debits once.
        if (!level.isOutsideBuildHeight(pos) && !level.isDebug()) {
            for (var kind : LbcChestDropItem.Kind.values()) if (LbcChestDropItem.matches(token.getItem(), kind)) {
                place(level, pos, kind);
            }
        }
        var remaining = token.getItem().copy();
        remaining.shrink(1);
        if (remaining.isEmpty()) token.discard(); else token.setItem(remaining);
        return true;
    }

    private static boolean resident(ServerLevel level, BlockPos pos) {
        // Shape and neighbour updates may inspect blocks on the other side of a chunk border.
        int minX = (pos.getX() - 1) >> 4, maxX = (pos.getX() + 1) >> 4;
        int minZ = (pos.getZ() - 1) >> 4, maxZ = (pos.getZ() + 1) >> 4;
        for (int chunkX = minX; chunkX <= maxX; chunkX++) for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
            if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) return false;
        }
        return true;
    }

    private static void place(ServerLevel level, BlockPos pos, LbcChestDropItem.Kind kind) {
        if (!replaceContainer(level, pos)) return;
        var oldState = level.getBlockState(pos);
        var tag = new CompoundTag();
        tag.putString("LootTable", kind.table().identifier().toString());
        // Same default-state shape resolution and flags as native /setblock ... replace.
        var input = new BlockInput(Blocks.CHEST.defaultBlockState(), Set.of(), tag);
        if (input.place(level, pos, 258)) level.updateNeighboursOnBlockSet(pos, oldState);
    }

    private static boolean replaceContainer(ServerLevel level, BlockPos pos) {
        var old = level.getBlockEntity(pos);
        // Author: eject the replaced inventory. Only this block entity is used,
        // never the combined double-chest inventory or its neighbour's contents.
        if (old instanceof RandomizableContainer randomizable) randomizable.unpackLootTable(null);
        if (old instanceof Container container) {
            // Native splitting retains complete components and empties every stack.
            Containers.dropContents(level, pos, container);
            container.clearContent();
        }
        if (old instanceof ChestBlockEntity) {
            // Same-state setBlock can skip BE creation. Replace the whole payload,
            // including old name/lock/components/seed, before installing the new table.
            var state = level.getBlockState(pos);
            level.removeBlockEntity(pos);
            level.setBlockEntity(new ChestBlockEntity(pos, state));
        }
        return true;
    }

    public void stop() {
        pending.clear();
        lastAttempt.clear();
        lastTick = Integer.MIN_VALUE;
    }

    int pendingCount() { return pending.size(); }
}
