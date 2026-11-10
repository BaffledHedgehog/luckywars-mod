package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.zombie.Zombie;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Native zombie/passenger payload; admission order and debit belong to LbcSpecialDrops. */
public final class LbcZombieTntDrops {
    private static final String ZOMBIE = "skywars2.zombie_tnt_drop";
    private static final String TNT = "skywars2.zombie_tnt_drop_tnt";
    private static final double[][] OFFSETS = {{1,0}, {0,1}, {-1,0}, {0,-1}};
    private static final double[][] SHAHID_OFFSETS = {{0,0}};
    // Author: TNT is WILD, while carriers must not perform any melee hit or its side effects.
    public static final boolean WILD = true;
    public static final boolean MELEE_ENABLED = false;
    private final GameController game;
    private final Set<Zombie> zombies = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<PrimedTnt> tnts = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Entity, LivingEntity> causes = new IdentityHashMap<>();
    // Chunk sleep preserves the same-match cause without retaining the loaded entity itself.
    private final Map<UUID, LivingEntity> unloadedCauses = new HashMap<>();

    public LbcZombieTntDrops(GameController game) { this.game = game; }

    public boolean ready(ItemEntity token) {
        if (token.isRemoved() || token.getAge() >= 5999 || !LbcZombieTntDropItem.matches(token.getItem())
                || !game.playing() || game.stage() == GameController.Stage.ENDING
                || token.level() != game.maps().level() || !(token.level() instanceof ServerLevel level)
                || game.items().timeStop().stopped(level)) return false;
        for (var tag : token.entityTags()) {
            if (tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag)) return false;
        }
        // Check the entire payload before emitting anything; never force-load a neighbouring chunk.
        for (var offset : offsets(token)) {
            var pos = BlockPos.containing(token.position().add(offset[0], 0, offset[1]));
            if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) return false;
        }
        return true;
    }

    public boolean spawn(ItemEntity token) {
        if (!ready(token)) return false;
        var level = (ServerLevel) token.level();
        var cause = game.items().specialDrops().owner(token);
        var offsets = offsets(token);
        var payload = new ArrayList<Zombie>(offsets.length);
        for (var offset : offsets) {
            // /summon does not finalizeSpawn: retain ordinary AI/loot without randomized equipment.
            var zombie = new Zombie(EntityTypes.ZOMBIE, level);
            zombie.setBaby(true);
            zombie.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(-10);
            zombie.setPos(token.position().add(offset[0], 0, offset[1]));
            // LivingEntity's constructor randomizes yaw; /summon loading absent Rotation resets it to zero.
            zombie.setYRot(0);
            zombie.setXRot(0);
            zombie.setYHeadRot(0);
            zombie.yBodyRot = 0;
            // The entity-type constructor matches summoned TNT's zero Motion, unlike the
            // coordinate constructor's random horizontal impulse and upward launch velocity.
            var tnt = new PrimedTnt(EntityTypes.TNT, level);
            tnt.setPos(zombie.position());
            tnt.setFuse(200);
            prepare(zombie, ZOMBIE, cause);
            prepare(tnt, TNT, zombie);
            if (!tnt.startRiding(zombie, true, true)) {
                forget(zombie);
                forget(tnt);
                zombie.discard();
                tnt.discard();
                for (var previous : payload) discardPair(previous);
                return false;
            }
            zombie.positionRider(tnt);
            payload.add(zombie);
        }
        // The real passenger relation is already present for insertion/load callbacks.
        for (var zombie : payload) {
            if (!level.tryAddFreshEntityWithPassengers(zombie)) discardPair(zombie);
        }
        return true;
    }

    /** The egg's intermediary token summons one carrier at its position, unlike the four-zombie trap. */
    private static double[][] offsets(ItemEntity token) {
        return LbcZombieTntDropItem.isShahid(token.getItem()) ? SHAHID_OFFSETS : OFFSETS;
    }

    private void prepare(Entity actor, String tag, LivingEntity cause) {
        actor.entityTags().add(tag);
        if(actor instanceof Zombie){actor.entityTags().add("skywars2.players_only_trap");game.items().spellSources().unowned(actor);}
        game.items().markCurrentItemEpoch(actor);
        causes.put(actor, cause);
        if (actor instanceof Zombie zombie) zombies.add(zombie);
        else tnts.add((PrimedTnt) actor);
        game.items().timeStop().prepareFrozen(actor);
    }

    private void forget(Entity actor) {
        zombies.remove(actor);
        tnts.remove(actor);
        causes.remove(actor);
    }

    private void discardPair(Zombie zombie) {
        for (var passenger : List.copyOf(zombie.getPassengers())) {
            forget(passenger);
            passenger.discard();
        }
        forget(zombie);
        zombie.discard();
    }

    public static boolean managed(Entity actor) {
        return actor instanceof Zombie && actor.entityTags().contains(ZOMBIE)
                || actor instanceof PrimedTnt && actor.entityTags().contains(TNT);
    }

    public LivingEntity owner(Entity actor) {
        if (actor instanceof ItemEntity token && LbcZombieTntDropItem.matches(token.getItem()))
            return game.items().specialDrops().owner(token);
        if (!managed(actor)) return null;
        var reflected = game.items().electro().reflectedOwner(actor);
        if (reflected != null) return reflected;
        if(LbcSpellSource.unownedHazard(actor))return nativeCause(actor);
        if (actor instanceof PrimedTnt tnt && tnt.getOwner() != null) return tnt.getOwner();
        return causes.get(actor);
    }

    /** Native TNT getOwner/save hooks must use this fallback, without calling getOwner again. */
    public LivingEntity retainedOwner(Entity actor) {
        if (!managed(actor)) return null;
        var reflected = game.items().electro().reflectedOwner(actor);
        if(reflected==null&&LbcSpellSource.unownedHazard(actor))return nativeCause(actor);
        return reflected != null ? reflected : causes.get(actor);
    }
    private LivingEntity nativeCause(Entity actor){
        var nativeOwner=game.items().spellSources().nativeMobSource(actor);if(nativeOwner!=null)return nativeOwner;
        var retained=causes.get(actor);return retained instanceof ServerPlayer?null:retained;
    }

    /** Called before native chain TNT insertion; retain its native fuse and velocity. */
    public void inheritChild(Entity parent, Entity child) {
        if (!managed(parent) || !active(parent) || !(child instanceof PrimedTnt)) return;
        prepare(child, TNT, owner(parent));
    }

    /** Drowning conversion keeps native converted attributes, gear and passenger transfer. */
    public void inheritConversion(Entity parent, Entity child) {
        if (!managed(parent) || !active(parent) || !(child instanceof Zombie)) return;
        prepare(child, ZOMBIE, owner(parent));
    }

    public boolean wild(Entity source) {
        return WILD && (managed(source)
                || source instanceof ItemEntity token && LbcZombieTntDropItem.matches(token.getItem()));
    }

    public boolean canAffect(Entity source, Entity target) {
        if (!managed(source)) return true;
        if (!active(source) || target == null || !game.items().wands().validTarget(target)) return false;
        return wild(source) || !(owner(source) instanceof ServerPlayer player)
                || target == player || game.items().wands().projectileTarget(player, target);
    }

    public boolean canTarget(Entity source, LivingEntity target) { return target == null || canAffect(source, target); }

    /** Carriers can pursue targets, but the author forbids all melee side effects. */
    public boolean canMelee(Entity source, Entity target) {
        return !managed(source) || MELEE_ENABLED && canAffect(source, target);
    }

    private boolean active(Entity actor) {
        return game.playing() && game.stage() != GameController.Stage.ENDING
                && actor.level() == game.maps().level() && game.items().currentEpoch(actor);
    }

    public void loaded(Entity actor) {
        if (!managed(actor)) return;
        if (!active(actor)) {
            unloadedCauses.remove(actor.getUUID());
            actor.discard();
            return;
        }
        if (!causes.containsKey(actor)) causes.put(actor, unloadedCauses.remove(actor.getUUID()));
        if (actor instanceof Zombie zombie) zombies.add(zombie); else tnts.add((PrimedTnt) actor);
        game.items().timeStop().prepareFrozen(actor);
    }

    public void unloaded(Entity actor) {
        if (!managed(actor)) return;
        zombies.remove(actor);
        tnts.remove(actor);
        if (active(actor) && actor.getRemovalReason() == Entity.RemovalReason.UNLOADED_TO_CHUNK)
            unloadedCauses.put(actor.getUUID(), owner(actor));
        // TNT discards before its synchronous native explosion/chain callbacks need this cause.
    }

    public void tick() {
        for (var zombie : List.copyOf(zombies)) {
            if (zombie.isRemoved()) zombies.remove(zombie);
            else if (!active(zombie)) { zombies.remove(zombie); zombie.discard(); }
        }
        for (var tnt : List.copyOf(tnts)) {
            if (tnt.isRemoved()) tnts.remove(tnt);
            else if (!active(tnt)) { tnts.remove(tnt); tnt.discard(); }
        }
        causes.keySet().removeIf(actor -> actor.isRemoved() || !active(actor)
                || !zombies.contains(actor) && !tnts.contains(actor));
        if (!game.playing() || game.stage() == GameController.Stage.ENDING) unloadedCauses.clear();
    }

    public void stop() {
        unloadedCauses.clear();
        var oldZombies = List.copyOf(zombies);
        var oldTnts = List.copyOf(tnts);
        zombies.clear();
        tnts.clear();
        for (var zombie : oldZombies) if (!zombie.isRemoved()) zombie.discard();
        // Include TNT detached by death/ejection; stopping never manufactures an explosion.
        for (var tnt : oldTnts) if (!tnt.isRemoved()) tnt.discard();
        causes.clear();
    }

    public int trackedCount() { return zombies.size(); }
    public List<Zombie> snapshot() { return List.copyOf(zombies); }
    public List<PrimedTnt> snapshotTnt() { return List.copyOf(tnts); }
}
