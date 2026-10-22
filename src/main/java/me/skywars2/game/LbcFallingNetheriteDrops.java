package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Native falling netherite; its single lightning strike belongs to the actual ground landing. */
public final class LbcFallingNetheriteDrops {
    private static final String ACTOR = "skywars2.falling_netherite_drop";
    private static final String BOLT = "skywars2.falling_netherite_drop_bolt";
    private static final String LANDED = "skywars2.falling_netherite_drop_landed";
    // 26.3 uses BlockState.CODEC's string form. Omitted drop/hurt fields keep native defaults.
    private static final CompoundTag PAYLOAD = payload();
    private final GameController game;
    private final Set<FallingBlockEntity> actors = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<LightningBolt> bolts = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Entity, LivingEntity> causes = new IdentityHashMap<>();
    private final Map<UUID, LivingEntity> unloadedCauses = new HashMap<>();
    private long totalBoltsSpawned;

    public LbcFallingNetheriteDrops(GameController game) { this.game = game; }

    private static CompoundTag payload() {
        try {
            return TagParser.parseCompoundFully("{BlockState:\"minecraft:netherite_block\",Time:1,Motion:[0.0d,0.0d,0.0d]}");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    public boolean ready(ItemEntity token, LbcFallingNetheriteDropItem.Kind kind) {
        if (token.isRemoved() || token.getAge() >= 5999 || !LbcFallingNetheriteDropItem.matches(token.getItem(), kind)
                || !game.playing() || game.stage() == GameController.Stage.ENDING
                || token.level() != game.maps().level() || !(token.level() instanceof ServerLevel level)
                || game.items().timeStop().stopped(level)) return false;
        for (var tag : token.entityTags()) {
            if (tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag)) return false;
        }
        var center = token.blockPosition();
        // The one-cell shaft and its native neighbour updates must already be resident.
        for (int x = (center.getX() - 1) >> 4; x <= (center.getX() + 1) >> 4; x++) {
            for (int z = (center.getZ() - 1) >> 4; z <= (center.getZ() + 1) >> 4; z++) {
                if (level.getChunkSource().getChunkNow(x, z) == null) return false;
            }
        }
        return true;
    }

    public boolean spawn(ItemEntity token, LbcFallingNetheriteDropItem.Kind kind) {
        if (!ready(token, kind)) return false;
        var level = (ServerLevel) token.level();
        var center = token.blockPosition();
        var cause = game.items().specialDrops().owner(token);
        level.sendParticles(ParticleTypes.SMOKE, kind == LbcFallingNetheriteDropItem.Kind.OLD, false,
                token.getX(), token.getY() + 7, token.getZ(), 300, .3, 6, .3, 0);
        Runnable writes = () -> {
            if (!level.isDebug()) LbcDropBlocks.fill(level, center, center.above(15), Blocks.AIR,
                    kind == LbcFallingNetheriteDropItem.Kind.NORMAL);
        };
        if (cause instanceof ServerPlayer player) game.items().indirectKills().trapChanges(level, player, true, writes);
        else writes.run();
        // A rejected native /fill (height or volume rule) does not cancel the independent summon.
        var actor = EntityTypes.FALLING_BLOCK.create(level, EntitySpawnReason.COMMAND);
        if (actor == null) return true;
        actor.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), PAYLOAD.copy()));
        actor.setPos(kind == LbcFallingNetheriteDropItem.Kind.NORMAL ? center.getX() + .5 : token.getX(),
                token.getY() + 14,
                kind == LbcFallingNetheriteDropItem.Kind.NORMAL ? center.getZ() + .5 : token.getZ());
        prepare(actor, ACTOR, cause);
        if (!level.addFreshEntity(actor)) forget(actor);
        return true;
    }

    private void prepare(Entity actor, String tag, LivingEntity cause) {
        actor.entityTags().add(tag);
        game.items().markCurrentItemEpoch(actor);
        causes.put(actor, cause);
        if (actor instanceof FallingBlockEntity falling) actors.add(falling);
        else bolts.add((LightningBolt) actor);
        game.items().timeStop().prepareFrozen(actor);
    }

    private void forget(Entity actor) {
        actors.remove(actor);
        bolts.remove(actor);
        causes.remove(actor);
    }

    public static boolean managed(Entity actor) {
        return actor instanceof FallingBlockEntity && actor.entityTags().contains(ACTOR)
                || actor instanceof LightningBolt && actor.entityTags().contains(BOLT);
    }

    public LivingEntity owner(Entity actor) {
        if (actor instanceof ItemEntity token && (LbcFallingNetheriteDropItem.matches(token.getItem(), LbcFallingNetheriteDropItem.Kind.NORMAL)
                || LbcFallingNetheriteDropItem.matches(token.getItem(), LbcFallingNetheriteDropItem.Kind.OLD)))
            return game.items().specialDrops().owner(token);
        if (!managed(actor)) return null;
        var reflected = game.items().electro().reflectedOwner(actor);
        if (reflected != null) return reflected;
        if (actor instanceof LightningBolt bolt && bolt.getCause() != null) return bolt.getCause();
        return causes.get(actor);
    }

    public boolean wild(Entity source) {
        return managed(source) || source instanceof ItemEntity token
                && (LbcFallingNetheriteDropItem.matches(token.getItem(), LbcFallingNetheriteDropItem.Kind.NORMAL)
                || LbcFallingNetheriteDropItem.matches(token.getItem(), LbcFallingNetheriteDropItem.Kind.OLD));
    }

    /** Wrap native ticking so its placement/support writes retain the consumed token's cause. */
    public void withTick(FallingBlockEntity actor, Runnable nativeTick) {
        if (managed(actor) && active(actor) && actor.level() instanceof ServerLevel level) {
            if (owner(actor) instanceof ServerPlayer player)
                game.items().indirectKills().trapChanges(level, player, true, nativeTick);
            else game.items().indirectKills().worldChanges(level, actor, true, nativeTick);
        } else nativeTick.run();
    }

    /** Called at native tick RETURN, including the ordinary placement/discard branch. */
    public void afterTick(FallingBlockEntity actor) {
        if (!managed(actor) || !active(actor) || !causes.containsKey(actor) || !actor.onGround()
                || actor.getBlockState().isAir() || game.items().timeStop().stopped((ServerLevel) actor.level())
                || !actor.entityTags().add(LANDED)) return;
        var level = (ServerLevel) actor.level();
        var cause = owner(actor);
        var bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.COMMAND);
        if (bolt == null) return;
        bolt.setPos(actor.position());
        bolt.setYRot(0);
        bolt.setXRot(0);
        if (cause instanceof ServerPlayer player) bolt.setCause(player);
        prepare(bolt, BOLT, cause);
        if (level.addFreshEntity(bolt)) totalBoltsSpawned++;
        else forget(bolt);
    }

    private boolean active(Entity actor) {
        return game.playing() && game.stage() != GameController.Stage.ENDING
                && actor.level() == game.maps().level() && game.items().currentEpoch(actor);
    }

    public void loaded(Entity actor) {
        if (!managed(actor)) return;
        if (!active(actor)) { unloadedCauses.remove(actor.getUUID()); actor.discard(); return; }
        if (!causes.containsKey(actor)) causes.put(actor, unloadedCauses.remove(actor.getUUID()));
        if (actor instanceof FallingBlockEntity falling) actors.add(falling);
        else {
            var bolt = (LightningBolt) actor;
            // Native lightning does not serialize its cause; restore the retained same-match author.
            if (bolt.getCause() == null && causes.get(actor) instanceof ServerPlayer player) bolt.setCause(player);
            bolts.add(bolt);
        }
        game.items().timeStop().prepareFrozen(actor);
    }

    public void unloaded(Entity actor) {
        if (!managed(actor)) return;
        actors.remove(actor);
        bolts.remove(actor);
        if (active(actor) && actor.getRemovalReason() == Entity.RemovalReason.UNLOADED_TO_CHUNK)
            unloadedCauses.put(actor.getUUID(), owner(actor));
        // Native placement discards before tick RETURN; retain the cause until the service tick.
    }

    public void tick() {
        for (var actor : List.copyOf(actors)) {
            if (actor.isRemoved()) actors.remove(actor);
            else if (!active(actor)) { actors.remove(actor); actor.discard(); }
        }
        for (var bolt : List.copyOf(bolts)) {
            if (bolt.isRemoved()) bolts.remove(bolt);
            else if (!active(bolt)) { bolts.remove(bolt); bolt.discard(); }
        }
        causes.keySet().removeIf(actor -> actor.isRemoved() || !active(actor)
                || !actors.contains(actor) && !bolts.contains(actor));
        if (!game.playing() || game.stage() == GameController.Stage.ENDING) unloadedCauses.clear();
    }

    public void stop() {
        unloadedCauses.clear();
        var previousActors = List.copyOf(actors);
        var previousBolts = List.copyOf(bolts);
        actors.clear();
        bolts.clear();
        for (var actor : previousActors) if (!actor.isRemoved()) actor.discard();
        for (var bolt : previousBolts) if (!bolt.isRemoved()) bolt.discard();
        causes.clear();
    }

    public int count() { return actors.size(); }
    public int trackedCount() { return count(); }
    public List<FallingBlockEntity> snapshot() { return List.copyOf(actors); }
    public List<LightningBolt> snapshotBolts() { return List.copyOf(bolts); }
    public long totalBoltsSpawned() { return totalBoltsSpawned; }
}
