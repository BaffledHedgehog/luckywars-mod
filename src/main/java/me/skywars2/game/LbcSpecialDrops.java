package me.skywars2.game;

import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared dropped-token dispatcher: source order, one activation/debit per tick and epoch. */
public final class LbcSpecialDrops {
    private final GameController game;
    private record Pending(String epoch, LivingEntity owner) { }
    private final Map<ItemEntity, Pending> pending = new IdentityHashMap<>();
    private final Map<ItemEntity, Integer> lastAttempt = new IdentityHashMap<>();
    private int lastTick = Integer.MIN_VALUE;

    public LbcSpecialDrops(GameController game) { this.game = game; }

    private static boolean recognized(net.minecraft.world.item.ItemStack stack) {
        return LbcChestDropItem.kind(stack) != null || LbcTntDropItem.matches(stack) || LbcCreeperDropItem.kind(stack) != null || LbcCageDropItem.kind(stack) != null || LbcHoleDropItem.kind(stack) != null || LbcCorrosionDropItem.kind(stack) != null || LbcZombieTntDropItem.matches(stack) || LbcFallingNetheriteDropItem.kind(stack) != null || LbcHomeItems.matchesDrop(stack) || LbcScareDropItem.kind(stack) != null;
    }

    public void loaded(Entity entity) {
        // /give creates a temporary visual item and marks it fake after ENTITY_LOAD.
        // Do not place blocks from inside the entity insertion callback.
        if (entity instanceof ItemEntity token && recognized(token.getItem())
                && active(token)) pending.putIfAbsent(token, new Pending(game.items().epoch(),
                        token.getOwner() instanceof LivingEntity owner ? owner : null));
    }

    /** Actual thrower, retained at admission independently of the pickup target. */
    public LivingEntity owner(ItemEntity token) {
        if (token.getOwner() instanceof LivingEntity living) return living;
        var entry = pending.get(token);
        return entry == null ? null : entry.owner();
    }

    /** Only causal trap tokens reach this native merge guard; no world or tick scan. */
    public boolean canMerge(ItemEntity first, ItemEntity second) {
        var a = owner(first);
        var b = owner(second);
        return java.util.Objects.equals(a == null ? null : a.getUUID(), b == null ? null : b.getUUID());
    }

    public void unloaded(Entity entity) {
        pending.remove(entity);
        lastAttempt.remove(entity);
    }

    private boolean stale(ItemEntity token) {
        for (var tag : token.entityTags())
            if (tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag)) return true;
        return false;
    }

    private boolean active(ItemEntity token) {
        return game.playing() && game.stage() != GameController.Stage.ENDING && token.level() == game.maps().level();
    }

    public void tick() {
        int now = game.server().getTickCount();
        if (lastTick == now) return;
        lastTick = now;
        for (var token : List.copyOf(pending.keySet())) {
            if (token.isRemoved() || token.getAge() >= 5999 || stale(token) || !active(token)
                    || !game.items().currentEpoch(pending.get(token).epoch()) || !recognized(token.getItem())) {
                unloaded(token);
                continue;
            }
            spawn(token);
            if (token.isRemoved() || token.getItem().isEmpty()) unloaded(token);
        }
    }

    /** Returns whether an active loaded attempt was dispatched, even when native placement fails. */
    boolean spawn(ItemEntity token) {
        if (token.isRemoved() || token.getAge() >= 5999 || stale(token) || !active(token)
                || !(token.level() instanceof ServerLevel level) || game.items().timeStop().stopped(level)) return false;
        if (pending.containsKey(token) && !game.items().currentEpoch(pending.get(token).epoch())) return false;
        boolean chest = LbcChestDropItem.kind(token.getItem()) != null;
        boolean tnt = LbcTntDropItem.matches(token.getItem());
        boolean creeper = LbcCreeperDropItem.kind(token.getItem()) != null;
        boolean cage = LbcCageDropItem.kind(token.getItem()) != null;
        boolean hole = LbcHoleDropItem.kind(token.getItem()) != null;
        boolean corrosion = LbcCorrosionDropItem.kind(token.getItem()) != null;
        boolean zombieTnt = LbcZombieTntDropItem.matches(token.getItem());
        boolean falling = LbcFallingNetheriteDropItem.kind(token.getItem()) != null;
        boolean home = LbcHomeItems.matchesDrop(token.getItem());
        boolean scare = LbcScareDropItem.kind(token.getItem()) != null;
        if (!chest && !tnt && !creeper && !cage && !hole && !corrosion && !zombieTnt && !falling && !home && !scare) return false;
        int now = game.server().getTickCount();
        if (lastAttempt.getOrDefault(token, Integer.MIN_VALUE) == now) return false;
        var pos = BlockPos.containing(token.position());
        if (!resident(level, pos, chest ? 1 : 0)) return false;
        for (var kind : LbcCreeperDropItem.Kind.values()) {
            if (LbcCreeperDropItem.matches(token.getItem(), kind) && !game.items().creeperDrops().ready(token, kind)) return false;
        }
        for (var kind : LbcCorrosionDropItem.Kind.values()) {
            if (LbcCorrosionDropItem.matches(token.getItem(), kind) && !game.items().corrosionDrops().ready(token, kind)) return false;
        }
        if (zombieTnt && !game.items().zombieTntDrops().ready(token)) return false;
        if (home && !game.items().homes().ready(token)) return false;
        for (var kind : LbcFallingNetheriteDropItem.Kind.values()) {
            if (LbcFallingNetheriteDropItem.matches(token.getItem(), kind) && !game.items().fallingNetheriteDrops().ready(token, kind)) return false;
        }
        var target = cage || hole ? game.items().cageDrops().target(token) : null;
        if (target != null) for (var kind : LbcCageDropItem.Kind.values()) {
            if (LbcCageDropItem.matches(token.getItem(), kind) && !game.items().cageDrops().ready(token, kind, target)) return false;
        }
        if (target != null) for (var kind : LbcHoleDropItem.Kind.values()) {
            if (LbcHoleDropItem.matches(token.getItem(), kind) && !game.items().holeDrops().ready(token, kind, target)) return false;
        }
        lastAttempt.put(token, now);
        // The source dispatcher executes every matching flag in order, then debits once.
        for (var kind : LbcCreeperDropItem.Kind.values()) {
            if (LbcCreeperDropItem.matches(token.getItem(), kind)) game.items().creeperDrops().spawn(token, kind);
        }
        if (!level.isOutsideBuildHeight(pos) && !level.isDebug()) {
            for (var kind : LbcChestDropItem.Kind.values()) if (LbcChestDropItem.matches(token.getItem(), kind)) {
                place(level, pos, kind);
            }
        }
        if (tnt) game.items().tntDrops().spawn(token);
        for (var kind : LbcCorrosionDropItem.Kind.values()) {
            if (LbcCorrosionDropItem.matches(token.getItem(), kind)) game.items().corrosionDrops().spawn(token, kind);
        }
        if (zombieTnt) game.items().zombieTntDrops().spawn(token);
        if (home) game.items().homes().spawn(token);
        for (var kind : LbcFallingNetheriteDropItem.Kind.values()) {
            if (LbcFallingNetheriteDropItem.matches(token.getItem(), kind)) game.items().fallingNetheriteDrops().spawn(token, kind);
        }
        if (target != null) for (var kind : LbcHoleDropItem.Kind.values()) {
            if (LbcHoleDropItem.matches(token.getItem(), kind)) game.items().holeDrops().spawn(token, kind, target);
        }
        if (target != null) for (var kind : LbcCageDropItem.Kind.values()) {
            if (LbcCageDropItem.matches(token.getItem(), kind)) game.items().cageDrops().spawn(token, kind, target);
        }
        if (scare) {
            // This trap chooses any eligible nearest player, independently of allegiance.
            var victim = game.items().cageDrops().target(token);
            if (victim != null) for (var kind : LbcScareDropItem.Kind.values()) {
                if (LbcScareDropItem.matches(token.getItem(), kind))
                    game.items().screamer(victim, owner(token) instanceof net.minecraft.server.level.ServerPlayer player ? player : null,
                            kind == LbcScareDropItem.Kind.OLD);
            }
        }
        var remaining = token.getItem().copy();
        remaining.shrink(1);
        if (remaining.isEmpty()) token.discard(); else token.setItem(remaining);
        return true;
    }

    private static boolean resident(ServerLevel level, BlockPos pos, int border) {
        // Shape and neighbour updates may inspect blocks on the other side of a chunk border.
        int minX = (pos.getX() - border) >> 4, maxX = (pos.getX() + border) >> 4;
        int minZ = (pos.getZ() - border) >> 4, maxZ = (pos.getZ() + border) >> 4;
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
