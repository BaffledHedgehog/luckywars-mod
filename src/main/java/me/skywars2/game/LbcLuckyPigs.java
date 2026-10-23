package me.skywars2.game;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.level.storage.TagValueInput;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Native reward pigs; token ordering and the one-unit debit belong to LbcSpecialDrops. */
public final class LbcLuckyPigs {
    private static final String PIG = "skywars2.lucky_pig";
    private static final double[][] OFFSETS = {{1,0}, {0,1}, {-1,0}, {0,-1}};
    private final GameController game;
    private final Set<Pig> pigs = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<ZombifiedPiglin> converted = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Entity, LivingEntity> causes = new IdentityHashMap<>();
    // Same-match chunk sleep retains the cause without retaining the unloaded actor instance.
    private final Map<UUID, LivingEntity> unloadedCauses = new HashMap<>();

    public LbcLuckyPigs(GameController game) { this.game = game; }

    public boolean ready(ItemEntity token) {
        if (token.isRemoved() || token.getAge() >= 5999 || !LbcLuckyPigDropItem.matches(token.getItem())
                || !game.playing() || game.stage() == GameController.Stage.ENDING
                || token.level() != game.maps().level() || !(token.level() instanceof ServerLevel level)
                || game.items().timeStop().stopped(level)) return false;
        for (var tag : token.entityTags()) {
            if (tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag)) return false;
        }
        // Admit all four offsets together without synchronously loading neighbouring chunks.
        for (var offset : OFFSETS) {
            var pos = BlockPos.containing(token.position().add(offset[0], 0, offset[1]));
            if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) return false;
        }
        return true;
    }

    public boolean spawn(ItemEntity token) {
        if (!ready(token)) return false;
        var level = (ServerLevel) token.level();
        var cause = game.items().specialDrops().owner(token);
        for (var offset : OFFSETS) {
            // /summon omits finalizeSpawn: leave ordinary health, AI and breeding untouched.
            var pig = new Pig(EntityTypes.PIG, level);
            var preset = new CompoundTag();
            preset.putString("DeathLootTable", "lbc:applepig");
            pig.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), preset));
            pig.setCustomName(Component.literal("Lucky Pig")
                    .withStyle(style -> style.withColor(ChatFormatting.GOLD).withItalic(false)));
            pig.setCustomNameVisible(true);
            pig.setPos(token.position().add(offset[0], 0, offset[1]));
            pig.setYRot(0);
            pig.setXRot(0);
            pig.setYHeadRot(0);
            pig.yBodyRot = 0;
            prepare(pig, cause);
            if (!level.addFreshEntity(pig)) {
                forget(pig);
                pig.discard();
            }
        }
        return true;
    }

    void adoptEgg(Pig actor, LivingEntity cause) { prepare(actor,cause); }

    private void prepare(Entity actor, LivingEntity cause) {
        actor.entityTags().add(PIG);
        game.items().markCurrentItemEpoch(actor);
        causes.put(actor, cause);
        if (actor instanceof Pig pig) pigs.add(pig);
        else converted.add((ZombifiedPiglin) actor);
        game.items().timeStop().prepareFrozen(actor);
    }

    private void forget(Entity actor) {
        pigs.remove(actor);
        converted.remove(actor);
        causes.remove(actor);
    }

    public static boolean managed(Entity actor) {
        return (actor instanceof Pig || actor instanceof ZombifiedPiglin) && actor.entityTags().contains(PIG);
    }

    public LivingEntity owner(Entity actor) {
        if (actor instanceof ItemEntity token && LbcLuckyPigDropItem.matches(token.getItem()))
            return game.items().specialDrops().owner(token);
        return managed(actor) ? causes.get(actor) : null;
    }

    /** Converted piglins keep native targeting, subject to the shared role and melee ally guards. */
    public boolean canAffect(Entity source, Entity target) {
        if (!managed(source) || target == null) return true;
        if (!active(source) || !game.items().wands().validTarget(target)) return false;
        return !(owner(source) instanceof ServerPlayer player)
                || target == player || game.items().wands().target(player, target);
    }

    public boolean canTarget(Entity source, LivingEntity target) { return canAffect(source, target); }

    private boolean active(Entity actor) {
        return game.playing() && game.stage() != GameController.Stage.ENDING
                && actor.level() == game.maps().level() && game.items().currentEpoch(actor);
    }

    /** Lightning conversion inherits only cause/cleanup; native piglin equipment and loot survive. */
    public void inheritConversion(Entity parent, Entity child) {
        if (!(parent instanceof Pig) || !managed(parent) || !active(parent)
                || !(child instanceof ZombifiedPiglin)) return;
        prepare(child, owner(parent));
    }

    public void loaded(Entity actor) {
        if (!managed(actor)) return;
        if (!active(actor)) {
            unloadedCauses.remove(actor.getUUID());
            actor.discard();
            return;
        }
        if (!causes.containsKey(actor)) causes.put(actor, unloadedCauses.remove(actor.getUUID()));
        if (actor instanceof Pig pig) pigs.add(pig); else converted.add((ZombifiedPiglin) actor);
        game.items().timeStop().prepareFrozen(actor);
    }

    public void unloaded(Entity actor) {
        if (!managed(actor)) return;
        pigs.remove(actor);
        converted.remove(actor);
        if (active(actor) && actor.getRemovalReason() == Entity.RemovalReason.UNLOADED_TO_CHUNK)
            unloadedCauses.put(actor.getUUID(), owner(actor));
        // Keep the cause through synchronous native death/conversion callbacks until the next tick.
    }

    public void tick() {
        for (var pig : List.copyOf(pigs)) {
            if (pig.isRemoved()) pigs.remove(pig);
            else if (!active(pig)) { pigs.remove(pig); pig.discard(); }
        }
        for (var piglin : List.copyOf(converted)) {
            if (piglin.isRemoved()) converted.remove(piglin);
            else if (!active(piglin)) { converted.remove(piglin); piglin.discard(); }
        }
        causes.keySet().removeIf(actor -> actor.isRemoved() || !active(actor)
                || !pigs.contains(actor) && !converted.contains(actor));
        if (!game.playing() || game.stage() == GameController.Stage.ENDING) unloadedCauses.clear();
    }

    public void stop() {
        unloadedCauses.clear();
        var oldPigs = List.copyOf(pigs);
        var oldConverted = List.copyOf(converted);
        pigs.clear();
        converted.clear();
        // Discard avoids both reward death and ordinary piglin loot on match cleanup.
        for (var pig : oldPigs) if (!pig.isRemoved()) pig.discard();
        for (var piglin : oldConverted) if (!piglin.isRemoved()) piglin.discard();
        causes.clear();
    }

    public int trackedCount() { return pigs.size(); }
    public int convertedCount() { return converted.size(); }
    public List<Pig> snapshot() { return List.copyOf(pigs); }
    public List<ZombifiedPiglin> snapshotConverted() { return List.copyOf(converted); }
}
