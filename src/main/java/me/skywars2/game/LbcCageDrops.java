package me.skywars2.game;

import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Geometry and native actors only; LbcSpecialDrops owns admission and one shared debit. */
public final class LbcCageDrops {
    private static final String ACTOR = "skywars2.cage_drop";
    private static final CompoundTag ANVIL = preset("{BlockState:\"minecraft:anvil\",Time:1,DropItem:1b,HurtEntities:1b,FallHurtMax:35,FallHurtAmount:2.0f,Motion:[0.0d,0.0d,0.0d]}");
    private static final CompoundTag FIREBALL = preset("{ExplosionPower:4,Motion:[0.0d,-0.05d,0.0d]}");
    private static final CompoundTag[] POTIONS = {potion(0), potion(1)};
    private final GameController game;
    private final Set<Entity> tracked = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Entity, LivingEntity> causes = new IdentityHashMap<>();
    private final Map<UUID, LivingEntity> unloadedCauses = new HashMap<>();

    public LbcCageDrops(GameController game) { this.game = game; }

    private static CompoundTag preset(String snbt) {
        try { return TagParser.parseCompoundFully(snbt); }
        catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) { throw new ExceptionInInitializerError(exception); }
    }

    private static CompoundTag potion(int amplifier) {
        return preset("{Item:{id:\"minecraft:splash_potion\",components:{\"minecraft:potion_contents\":{custom_color:7543569,custom_effects:[{id:\"minecraft:instant_damage\",amplifier:"
                + amplifier + "b,duration:20}]}},count:1}}");
    }

    /** Native nearest-player semantics: self/allies and Creative players remain eligible. */
    public ServerPlayer target(ItemEntity token) {
        if (!(token.level() instanceof ServerLevel level)) return null;
        ServerPlayer nearest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (var player : level.players()) {
            if (!game.items().wands().validTarget(player)) continue;
            double candidate = player.distanceToSqr(token);
            if (candidate < distance) { nearest = player; distance = candidate; }
        }
        return nearest;
    }

    public boolean ready(ItemEntity token, LbcCageDropItem.Kind kind, ServerPlayer target) {
        if (token.isRemoved() || token.getAge() >= 5999 || !LbcCageDropItem.matches(token.getItem(), kind)
                || !game.playing() || game.stage() == GameController.Stage.ENDING
                || token.level() != game.maps().level() || !(token.level() instanceof ServerLevel level)
                || game.items().timeStop().stopped(level)) return false;
        for (var tag : token.entityTags()) {
            if (tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag)) return false;
        }
        // With no eligible player the legacy execute does nothing, but still consumes its token.
        if (target == null) return true;
        if (target.level() != level || !game.items().wands().validTarget(target)) return false;
        var center = BlockPos.containing(target.position());
        // Include one neighbour around the three-wide shape for native shape/block updates.
        for (int x = (center.getX() - 2) >> 4; x <= (center.getX() + 2) >> 4; x++) {
            for (int z = (center.getZ() - 2) >> 4; z <= (center.getZ() + 2) >> 4; z++) {
                if (level.getChunkSource().getChunkNow(x, z) == null) return false;
            }
        }
        return true;
    }

    public boolean spawn(ItemEntity token, LbcCageDropItem.Kind kind, ServerPlayer target) {
        if (!ready(token, kind, target)) return false;
        if (target == null) return true;
        var level = (ServerLevel) token.level();
        var center = BlockPos.containing(target.position());
        var owner = game.items().specialDrops().owner(token);
        Runnable writes = () -> geometry(level, center, kind);
        if (owner instanceof ServerPlayer player) game.items().indirectKills().trapChanges(level, player, true, writes);
        else writes.run();
        double x = center.getX() + .5, y = target.getY(), z = center.getZ() + .5;
        switch (kind) {
            case ANVIL -> {
                insert(level, EntityTypes.FALLING_BLOCK.create(level, EntitySpawnReason.COMMAND), ANVIL, x, y + 40, z, owner);
                insert(level, EntityTypes.FALLING_BLOCK.create(level, EntitySpawnReason.COMMAND), ANVIL, x, y + 42, z, owner);
            }
            case FIREBALL -> insert(level, EntityTypes.FIREBALL.create(level, EntitySpawnReason.COMMAND), FIREBALL, x, y + 70, z, owner);
            case POTIONS -> {
                for (int i = 1; i <= 40; i++) {
                    int amplifier = i >= 10 && i <= 20 || i >= 30 ? 1 : 0;
                    insert(level, EntityTypes.SPLASH_POTION.create(level, EntitySpawnReason.COMMAND), POTIONS[amplifier], x, y + i * 50, z, owner);
                }
            }
            default -> { }
        }
        if (kind != LbcCageDropItem.Kind.OBSIDIAN) {
            target.connection.send(new ClientboundSetTitleTextPacket(Component.literal(" ")));
            target.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("look_up")));
        }
        return true;
    }

    private static void geometry(ServerLevel level, BlockPos center, LbcCageDropItem.Kind kind) {
        if (level.isDebug()) return;
        if (kind == LbcCageDropItem.Kind.OBSIDIAN) {
            fill(level, center.offset(-1,-1,-1), center.offset(1,2,1), Blocks.OBSIDIAN, true);
            fill(level, center, center.above(), Blocks.WATER, false);
            fill(level, center.offset(1,1,0), center.offset(1,1,0), Blocks.GLASS, true);
            fill(level, center.offset(0,1,1), center.offset(0,1,1), Blocks.GLASS, true);
            fill(level, center.offset(-1,1,0), center.offset(-1,1,0), Blocks.GLASS, true);
            fill(level, center.offset(0,1,-1), center.offset(0,1,-1), Blocks.GLASS, true);
        } else {
            fill(level, center.offset(-1,-1,-1), center.offset(1,-1,1), Blocks.STONE_BRICKS, false);
            fill(level, center.offset(-1,0,-1), center.offset(1,3,1), Blocks.IRON_BARS, false);
            if (kind == LbcCageDropItem.Kind.LAVA) {
                fill(level, center, center.above(2), Blocks.AIR, false);
                fill(level, center.above(3), center.above(3), Blocks.LAVA, false);
            } else {
                // The shaft only opens upward; the old absolute255 pass could erase the floor.
                if (center.getY() <= level.getMaxY())
                    fill(level, center, new BlockPos(center.getX(),level.getMaxY(),center.getZ()), Blocks.AIR, false);
            }
        }
    }

    /** Same flags, neighbour-shape resolution and deferred updates as native /fill replace. */
    private static void fill(ServerLevel level, BlockPos from, BlockPos to, Block block, boolean strict) {
        if (level.isOutsideBuildHeight(from) || level.isOutsideBuildHeight(to)) return;
        var input = new BlockInput(block.defaultBlockState(), Set.of(), null);
        record Updated(BlockPos pos, BlockState oldState) { }
        var updated = new ArrayList<Updated>();
        for (var pos : BlockPos.betweenClosed(Math.min(from.getX(),to.getX()), Math.min(from.getY(),to.getY()), Math.min(from.getZ(),to.getZ()),
                Math.max(from.getX(),to.getX()), Math.max(from.getY(),to.getY()), Math.max(from.getZ(),to.getZ()))) {
            var old = level.getBlockState(pos);
            if (input.place(level, pos, strict ? 818 : 258) && !strict) updated.add(new Updated(pos.immutable(), old));
        }
        for (var update : updated) level.updateNeighboursOnBlockSet(update.pos(), update.oldState());
    }

    private void insert(ServerLevel level, Entity actor, CompoundTag preset, double x, double y, double z, LivingEntity owner) {
        if (actor == null) return;
        actor.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), preset.copy()));
        actor.setPos(x,y,z);
        if (actor instanceof Projectile projectile) projectile.setOwner(owner);
        actor.entityTags().add(ACTOR);
        game.items().markCurrentItemEpoch(actor);
        causes.put(actor,owner);
        tracked.add(actor);
        game.items().timeStop().prepareFrozen(actor);
        if (!level.addFreshEntity(actor)) { tracked.remove(actor); causes.remove(actor); }
    }

    public static boolean managed(Entity entity) {
        return entity != null && (entity instanceof FallingBlockEntity || entity instanceof Projectile || entity instanceof PrimedTnt)
                && entity.entityTags().contains(ACTOR);
    }

    public LivingEntity owner(Entity actor) {
        if (actor instanceof ItemEntity token && LbcCageDropItem.kind(token.getItem()) != null)
            return game.items().specialDrops().owner(token);
        if (!managed(actor)) return null;
        if (actor instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity current) return current;
        var reflected = game.items().electro().reflectedOwner(actor);
        if (reflected != null) return reflected;
        if (actor instanceof PrimedTnt tnt && tnt.getOwner() != null) return tnt.getOwner();
        return causes.get(actor);
    }

    /** Native TNT getOwner/save hooks use this non-recursive fallback. */
    public LivingEntity retainedOwner(Entity actor) {
        if (!managed(actor)) return null;
        var reflected = game.items().electro().reflectedOwner(actor);
        return reflected != null ? reflected : causes.get(actor);
    }

    public void inheritChild(Entity parent, Entity child) {
        if (!managed(parent) || !active(parent) || !(child instanceof PrimedTnt)) return;
        child.entityTags().add(ACTOR);
        game.items().markCurrentItemEpoch(child);
        causes.put(child, owner(parent));
        tracked.add(child);
        game.items().timeStop().prepareFrozen(child);
    }

    public boolean wild(Entity source) {
        return managed(source) || source instanceof ItemEntity token && LbcCageDropItem.kind(token.getItem()) != null;
    }

    public boolean canAffect(Entity source, Entity target) {
        if (!managed(source)) return true;
        if (!active(source) || !game.items().wands().validTarget(target)) return false;
        return wild(source) || !(owner(source) instanceof ServerPlayer player)
                || target == player || game.items().wands().projectileTarget(player, target);
    }

    private boolean active(Entity actor) {
        return game.playing() && game.stage() != GameController.Stage.ENDING
                && actor.level() == game.maps().level() && game.items().currentEpoch(actor);
    }

    public void loaded(Entity actor) {
        if (!managed(actor)) return;
        if (!active(actor)) { unloadedCauses.remove(actor.getUUID()); actor.discard(); return; }
        if (!causes.containsKey(actor)) causes.put(actor,unloadedCauses.remove(actor.getUUID()));
        tracked.add(actor);
        game.items().timeStop().prepareFrozen(actor);
    }

    public void unloaded(Entity actor) {
        if (!managed(actor)) return;
        tracked.remove(actor);
        if (active(actor) && actor.getRemovalReason() == Entity.RemovalReason.UNLOADED_TO_CHUNK)
            unloadedCauses.put(actor.getUUID(), owner(actor));
        // Retain the instance cause through synchronous native explosion/landing callbacks.
    }

    public void tick() {
        for (var actor : List.copyOf(tracked)) {
            if (actor.isRemoved()) tracked.remove(actor);
            else if (!active(actor)) { tracked.remove(actor); actor.discard(); }
        }
        causes.keySet().removeIf(actor -> actor.isRemoved() || !active(actor) || !tracked.contains(actor));
        if (!game.playing() || game.stage() == GameController.Stage.ENDING) unloadedCauses.clear();
    }

    public void stop() {
        unloadedCauses.clear();
        var detached = List.copyOf(tracked);
        tracked.clear();
        for (var actor : detached) if (!actor.isRemoved()) actor.discard();
        causes.clear();
    }

    public int trackedCount() { return tracked.size(); }
    public List<Entity> snapshot() { return List.copyOf(tracked); }
}
