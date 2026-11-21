package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;

/** Live checks of native dropped-item damage; no production state is installed by this suite. */
public final class TypedItemChecks {
    private static final BlockPos BASE = new BlockPos(96, 260, 96);
    private TypedItemChecks() { }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Typed items: " + message);
    }

    private static ItemStack marked(boolean string) {
        var stack = new ItemStack(Items.PAPER);
        var tag = new CompoundTag();
        if (string) tag.putString("type", "1t"); else tag.putInt("type", 0);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    private static ItemEntity drop(ServerLevel level, ArrayList<ItemEntity> drops, ItemStack stack, double x) {
        var entity = new ItemEntity(level, BASE.getX() + x, BASE.getY() + .25, BASE.getZ() + .5, stack);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.setNoGravity(true);
        entity.setPickUpDelay(1000);
        require(level.addFreshEntity(entity), "fixture enters native entity index");
        drops.add(entity);
        return entity;
    }

    private static short health(ItemEntity entity) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
        entity.saveWithoutId(output);
        return output.buildResult().getShortOr("Health", (short) -1);
    }

    public static void run(GameController game) {
        var level = game.maps().level();
        var drops = new ArrayList<ItemEntity>();
        var cells = new LinkedHashMap<BlockPos, MapManager.BlockSnapshot>();
        try {
            for (var cell : BlockPos.betweenClosed(BASE.offset(-4, -2, -4), BASE.offset(4, 4, 4))) {
                var pos = cell.immutable();
                level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
                cells.put(pos, game.maps().snapshot(pos));
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
            var nativeStack = new ItemStack(LbcMagic.MYSTICAL_ARTIFACT);
            require(nativeStack.get(DataComponents.CUSTOM_DATA) != null
                    && nativeStack.get(DataComponents.CUSTOM_DATA).copyTag().contains("type"),
                    "registered Polymer artifact retains legacy type data");
            for (var stack : java.util.List.of(marked(false), marked(true), nativeStack)) {
                var item = drop(level, drops, stack, .5);
                var sources = item.damageSources();
                for (var source : java.util.List.of(sources.inFire(), sources.onFire(), sources.lava(),
                        sources.lightningBolt(), sources.generic(), sources.cactus(), sources.magic(),
                        sources.genericKill())) {
                    require(!item.hurtServer(level, source, 100) && !item.isRemoved() && health(item) == 5,
                            "type presence rejects " + source.getMsgId() + " without reducing health");
                }
                require(!item.isInvulnerable(), "protection never sets global invulnerability");
                require(item.hurtServer(level, sources.fellOutOfWorld(), 100) && item.isRemoved(),
                        "native void damage destroys numeric/string/Polymer item");
            }
            // Unrelated keys and nested type values must keep vanilla damage behavior.
            for (int mode = 0; mode < 3; mode++) {
                var ordinary = new ItemStack(Items.PAPER);
                if (mode != 0) {
                    var data = new CompoundTag();
                    if (mode == 1) data.putInt("cooltype", 1);
                    else { var nested = new CompoundTag(); nested.putInt("type", 1); data.put("nested", nested); }
                    ordinary.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
                }
                var item = drop(level, drops, ordinary, .5);
                require(item.hurtServer(level, item.damageSources().inFire(), 100) && item.isRemoved(),
                        "untyped vanilla/unrelated/nested markers remain vulnerable");
            }
            var mutable = drop(level, drops, marked(true), .5);
            mutable.setItem(new ItemStack(Items.PAPER));
            require(mutable.hurtServer(level, mutable.damageSources().generic(), 1) && health(mutable) == 4,
                    "replacing typed stack immediately removes protection");
            mutable.setItem(marked(false));
            require(!mutable.hurtServer(level, mutable.damageSources().generic(), 100) && health(mutable) == 4,
                    "replacing ordinary stack immediately adds protection without healing");
            var saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            mutable.saveWithoutId(saved);
            var restored = new ItemEntity(EntityTypes.ITEM, level);
            restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved.buildResult()));
            restored.setUUID(UUID.randomUUID());
            require(level.addFreshEntity(restored), "restored item enters native index");
            drops.add(restored);
            require(!restored.hurtServer(level, restored.damageSources().lava(), 100) && health(restored) == 4,
                    "save/load preserves marker and existing health");
            var from = marked(false);
            require(ItemEntity.areMergable(restored.getItem(), from)
                    && !ItemEntity.areMergable(restored.getItem(), new ItemStack(Items.PAPER)),
                    "native merge accepts equal typed components and rejects untyped components");
            restored.setItem(ItemEntity.merge(restored.getItem(), from, 64));
            require(restored.getItem().getCount() == 2 && from.isEmpty()
                    && !restored.hurtServer(level, restored.damageSources().inFire(), 100),
                    "native merged stack retains protection");
            drops.forEach(Entity::discard);

            var typed = drop(level, drops, marked(true), .25);
            var ordinary = drop(level, drops, new ItemStack(Items.PAPER), .75);
            var bolt = EntityTypes.LIGHTNING_BOLT.create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
            require(bolt != null, "native lightning fixture exists");
            try {
                typed.thunderHit(level, bolt);
                ordinary.thunderHit(level, bolt);
                require(!typed.isRemoved() && health(typed) == 5 && ordinary.isRemoved(),
                        "native thunderHit protects typed stack and destroys ordinary paper");
            } finally { bolt.discard(); }
            ordinary = drop(level, drops, new ItemStack(Items.PAPER), .75);
            level.explode(null, BASE.getX() + .5, BASE.getY() + .25, BASE.getZ() + .5,
                    1, Level.ExplosionInteraction.TNT);
            require(!typed.isRemoved() && health(typed) == 5 && ordinary.isRemoved(),
                    "actual native TNT explosion preserves typed stack and destroys ordinary paper");

            drops.forEach(Entity::discard);
            level.setBlock(BASE, Blocks.LAVA.defaultBlockState(), 2);
            typed = drop(level, drops, marked(false), .25);
            ordinary = drop(level, drops, new ItemStack(Items.PAPER), .75);
            for (int tick = 0; tick < 30; tick++) {
                typed.setPos(BASE.getX() + .25, BASE.getY() + .25, BASE.getZ() + .5);
                typed.setDeltaMovement(Vec3.ZERO);
                typed.tick();
                if (!ordinary.isRemoved()) {
                    ordinary.setPos(BASE.getX() + .75, BASE.getY() + .25, BASE.getZ() + .5);
                    ordinary.setDeltaMovement(Vec3.ZERO);
                    ordinary.tick();
                }
            }
            require(!typed.isRemoved() && health(typed) == 5 && ordinary.isRemoved(),
                    "native lava block exposure preserves typed item and destroys ordinary paper");
            require(typed.hurtServer(level, typed.damageSources().fellOutOfWorld(), 100) && typed.isRemoved(),
                    "lava survival does not block subsequent void damage");

            drops.forEach(Entity::discard);
            level.setBlock(BASE.below(), Blocks.NETHERRACK.defaultBlockState(), 2);
            level.setBlock(BASE, Blocks.FIRE.defaultBlockState(), 2);
            typed = drop(level, drops, marked(true), .25);
            ordinary = drop(level, drops, new ItemStack(Items.PAPER), .75);
            for (int tick = 0; tick < 120; tick++) {
                typed.setPos(BASE.getX() + .25, BASE.getY() + .25, BASE.getZ() + .5);
                typed.setDeltaMovement(Vec3.ZERO);
                typed.tick();
                if (!ordinary.isRemoved()) {
                    ordinary.setPos(BASE.getX() + .75, BASE.getY() + .25, BASE.getZ() + .5);
                    ordinary.setDeltaMovement(Vec3.ZERO);
                    ordinary.tick();
                }
            }
            require(!typed.isRemoved() && health(typed) == 5 && ordinary.isRemoved(),
                    "native fire block exposure preserves typed item and destroys ordinary paper");
        } finally {
            drops.forEach(Entity::discard);
            cells.forEach((pos, snapshot) -> game.maps().restoreSnapshot(pos, snapshot));
        }
    }
}
