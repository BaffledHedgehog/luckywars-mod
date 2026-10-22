package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.Vec3;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The special-drop dispatcher owns activation/debit; this service owns only native TNT. */
public final class LbcTntDrops {
    private static final String TNT = "skywars2.tnt_drop";
    // Preserve the source emission order as well as its exact thirteen velocities.
    private static final double[][] VELOCITIES = {
            {0, 0}, {0, .1}, {0, -.1}, {.1, 0}, {.1, .1}, {.1, -.1},
            {-.1, 0}, {-.1, .1}, {-.1, -.1}, {.2, 0}, {-.2, 0}, {0, .2}, {0, -.2}
    };
    private final GameController game;
    private final Set<PrimedTnt> tracked = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<PrimedTnt, LivingEntity> causes = new IdentityHashMap<>();
    // Causal references only: activation and all pending-token work belong to the dispatcher.
    private final Map<ItemEntity, LivingEntity> throwers = new IdentityHashMap<>();

    public LbcTntDrops(GameController game) { this.game = game; }

    /** Emit the payload without consuming the item or scheduling another activation. */
    public boolean spawn(ItemEntity token) {
        if (token.isRemoved() || token.getAge() >= 5999 || !LbcTntDropItem.matches(token.getItem())
                || !game.playing() || game.stage() == GameController.Stage.ENDING
                || token.level() != game.maps().level() || !(token.level() instanceof ServerLevel level)
                || game.items().timeStop().stopped(level)) return false;
        for (var tag : token.entityTags()) {
            if (tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag)) return false;
        }
        var at = token.position().add(0, .1, 0);
        var pos = BlockPos.containing(at);
        if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) return false;
        // getOwner is ItemEntity's thrower, independently of its pickup-target UUID.
        var owner = tokenOwner(token);
        for (var velocity : VELOCITIES) {
            // Keep the exact native class so the shared TNT block-discovery cache applies.
            var tnt = new PrimedTnt(level, at.x, at.y, at.z, owner);
            tnt.setFuse(70);
            tnt.setDeltaMovement(velocity[0], 1, velocity[1]);
            tnt.entityTags().add(TNT);
            game.items().markCurrentItemEpoch(tnt);
            causes.put(tnt, owner);
            tracked.add(tnt);
            game.items().timeStop().prepareFrozen(tnt);
            if (!level.addFreshEntity(tnt)) tracked.remove(tnt);
        }
        return true;
    }

    public static boolean managed(Entity entity) {
        return entity instanceof PrimedTnt && entity.entityTags().contains(TNT);
    }

    private LivingEntity tokenOwner(ItemEntity token) {
        return token.getOwner() instanceof LivingEntity living ? living : throwers.get(token);
    }

    /** Only matching TNT tokens reach this comparison; ordinary item merges stay native. */
    public boolean canMerge(ItemEntity first, ItemEntity second) {
        var a = tokenOwner(first);
        var b = tokenOwner(second);
        return java.util.Objects.equals(a == null ? null : a.getUUID(), b == null ? null : b.getUUID());
    }

    /** Called before native insertion; preserve chain physics and its native randomized fuse. */
    public void inheritChain(Entity source, Entity spawned) {
        if (!managed(source) || !active((PrimedTnt) source) || !(spawned instanceof PrimedTnt child)) return;
        child.entityTags().add(TNT);
        game.items().markCurrentItemEpoch(child);
        causes.put(child, ((PrimedTnt) source).getOwner());
    }

    /** The native owner hook checks reflection first, then this nonrecursive causal fallback. */
    public LivingEntity retainedOwner(Entity entity) {
        return entity instanceof PrimedTnt tnt ? causes.get(tnt) : null;
    }

    private boolean active(PrimedTnt tnt) {
        return game.playing() && game.stage() != GameController.Stage.ENDING
                && tnt.level() == game.maps().level() && game.items().currentEpoch(tnt);
    }

    public void loaded(Entity entity) {
        // Native player drops assign the thrower before ENTITY_LOAD. Retain it before
        // deferred dispatch can outlive that player entity's death or disconnect.
        if (entity instanceof ItemEntity token && LbcTntDropItem.matches(token.getItem())
                && token.getOwner() instanceof LivingEntity owner) throwers.put(token, owner);
        if (!managed(entity)) return;
        var tnt = (PrimedTnt) entity;
        // Native saving preserves fuse, velocity, owner UUID and the match epoch tag.
        if (!active(tnt)) tnt.discard();
        else {
            if (!causes.containsKey(tnt)) causes.put(tnt, tnt.getOwner());
            tracked.add(tnt);
            game.items().timeStop().prepareFrozen(tnt);
        }
    }

    public void unloaded(Entity entity) {
        tracked.remove(entity);
        throwers.remove(entity);
        // Native TNT discards before explode(). Its synchronous explosion/chain callbacks
        // still need the retained cause; reclaim it on the next service tick instead.
    }

    public void tick() {
        throwers.keySet().removeIf(token -> token.isRemoved() || token.getAge() >= 5999
                || !LbcTntDropItem.matches(token.getItem()) || !game.playing()
                || game.stage() == GameController.Stage.ENDING || token.level() != game.maps().level());
        for (var tnt : List.copyOf(tracked)) {
            if (tnt.isRemoved()) tracked.remove(tnt);
            else if (!active(tnt)) {
                tracked.remove(tnt);
                tnt.discard();
            }
        }
        causes.keySet().removeIf(tnt -> !tracked.contains(tnt) || tnt.isRemoved() || !active(tnt));
    }

    /** Applied before native exposure, damage and knockback; damage scaling remains shared. */
    public boolean explosionTarget(Entity source, Entity target) {
        if (!managed(source)) return true;
        var tnt = (PrimedTnt) source;
        if (!active(tnt) || !game.items().wands().validTarget(target)) return false;
        return !(tnt.getOwner() instanceof ServerPlayer owner)
                || target == owner || game.items().wands().projectileTarget(owner, target);
    }

    public boolean allowDamage(LivingEntity victim, DamageSource source) {
        return explosionTarget(source.getDirectEntity(), victim);
    }

    public void stop() {
        throwers.clear();
        var detached = List.copyOf(tracked);
        tracked.clear();
        for (var tnt : detached) if (!tnt.isRemoved()) tnt.discard();
        causes.clear();
    }

    public int trackedCount() { return tracked.size(); }
    public List<PrimedTnt> snapshot() { return List.copyOf(tracked); }
}
