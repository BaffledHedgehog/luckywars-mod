package me.skywars2.game;

import net.minecraft.commands.arguments.coordinates.LocalCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec2;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Dispatch/debit belong to LbcSpecialDrops; these actors keep native mob AI and physics. */
public final class LbcCreeperDrops {
    private static final String CREEPER = "skywars2.creeper_drop";
    private static final String CHILD = "skywars2.creeper_drop_child";
    private static final double[][] NORMAL = {
            {2,0}, {-2,0}, {0,2}, {0,-2}, {2,-1}, {-1,2}, {-2,-1}, {-1,-2}, {-2,-2}, {2,2},
            {2,0}, {-2,0}, {0,2}, {0,-2}, {2,1}, {1,2}, {-2,1}, {1,-2}, {-2,-2}, {2,2}
    };
    private static final LocalCoordinates OLD_FORWARD = new LocalCoordinates(0, 0, 4);
    private final GameController game;
    private final Set<Creeper> tracked = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Entity> children = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Entity, LivingEntity> causes = new IdentityHashMap<>();
    // Native chunk saves retain the actor UUID. Keep causal references while its chunk sleeps.
    private final Map<UUID, LivingEntity> unloadedCauses = new HashMap<>();

    public LbcCreeperDrops(GameController game) { this.game = game; }

    public boolean ready(ItemEntity token, LbcCreeperDropItem.Kind kind) {
        if (token.isRemoved() || token.getAge() >= 5999 || !LbcCreeperDropItem.matches(token.getItem(), kind)
                || !game.playing() || game.stage() == GameController.Stage.ENDING
                || token.level() != game.maps().level() || !(token.level() instanceof ServerLevel level)
                || game.items().timeStop().stopped(level)) return false;
        for (var tag : token.entityTags()) {
            if (tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag)) return false;
        }
        int count = kind == LbcCreeperDropItem.Kind.NORMAL ? NORMAL.length : 16;
        float yaw = token.getYRot();
        for (int i = 0; i < count; i++) {
            var at = kind == LbcCreeperDropItem.Kind.NORMAL
                    ? token.position().add(NORMAL[i][0], 0, NORMAL[i][1])
                    : OLD_FORWARD.apply(token.position(), new Vec2(token.getXRot(), yaw + i * 20));
            var pos = BlockPos.containing(at);
            if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) return false;
        }
        return true;
    }

    public boolean spawn(ItemEntity token, LbcCreeperDropItem.Kind kind) {
        if (!ready(token, kind)) return false;
        var level = (ServerLevel) token.level();
        int count = kind == LbcCreeperDropItem.Kind.NORMAL ? NORMAL.length : 16;
        float yaw = token.getYRot();
        var owner = game.items().specialDrops().owner(token);
        for (int i = 0; i < count; i++) {
            var creeper = new Creeper(EntityTypes.CREEPER, level);
            var preset = new CompoundTag();
            preset.putByte("ExplosionRadius", (byte) (i % 2 + 1));
            if (kind == LbcCreeperDropItem.Kind.NORMAL) preset.putString("DeathLootTable", "lbc:creeper_wand");
            creeper.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), preset));
            creeper.setPos(kind == LbcCreeperDropItem.Kind.NORMAL
                    ? token.position().add(NORMAL[i][0], 0, NORMAL[i][1])
                    : OLD_FORWARD.apply(token.position(), new Vec2(token.getXRot(), yaw + i * 20)));
            creeper.entityTags().add(CREEPER);
            game.items().markCurrentItemEpoch(creeper);
            causes.put(creeper, owner);
            tracked.add(creeper);
            game.items().timeStop().prepareFrozen(creeper);
            if (!level.addFreshEntity(creeper)) {
                tracked.remove(creeper);
                causes.remove(creeper);
            }
        }
        if (kind == LbcCreeperDropItem.Kind.OLD) token.setYRot(yaw + 300);
        return true;
    }

    public static boolean managed(Entity entity) {
        return entity instanceof Creeper && entity.entityTags().contains(CREEPER)
                || (entity instanceof PrimedTnt || entity instanceof AreaEffectCloud) && entity.entityTags().contains(CHILD);
    }

    /** Captured causal thrower only; does not turn the hostile mob into a friendly summon. */
    public LivingEntity owner(Entity entity) {
        if (!managed(entity)) return null;
        if (entity instanceof PrimedTnt) {
            var reflected = game.items().electro().reflectedOwner(entity);
            if (reflected != null) return reflected;
        }
        return causes.get(entity);
    }

    private boolean active(Entity creeper) {
        return game.playing() && game.stage() != GameController.Stage.ENDING
                && creeper.level() == game.maps().level() && game.items().currentEpoch(creeper);
    }

    /** Native potion clouds and chained TNT retain the trap cause before insertion. */
    public void inheritChild(Entity parent, Entity child) {
        if (!managed(parent) || !active(parent) || !(child instanceof AreaEffectCloud || child instanceof PrimedTnt)) return;
        child.entityTags().add(CHILD);
        game.items().markCurrentItemEpoch(child);
        var cause = owner(parent);
        causes.put(child, cause);
        if (child instanceof AreaEffectCloud cloud) cloud.setOwner(cause);
        children.add(child);
        game.items().timeStop().prepareFrozen(child);
    }

    public void loaded(Entity entity) {
        if (!managed(entity)) return;
        if (!active(entity)) {
            unloadedCauses.remove(entity.getUUID());
            entity.discard();
        } else {
            if (!causes.containsKey(entity)) causes.put(entity, unloadedCauses.remove(entity.getUUID()));
            if (entity instanceof Creeper creeper) tracked.add(creeper); else children.add(entity);
            game.items().timeStop().prepareFrozen(entity);
        }
    }

    public void unloaded(Entity entity) {
        if (!managed(entity)) return;
        tracked.remove(entity);
        children.remove(entity);
        if (active(entity) && entity.getRemovalReason() == Entity.RemovalReason.UNLOADED_TO_CHUNK) {
            unloadedCauses.put(entity.getUUID(), causes.get(entity));
        }
        // Keep the instance cause through synchronous native explosion/chain/cloud callbacks.
    }

    public void tick() {
        for (var creeper : List.copyOf(tracked)) {
            if (creeper.isRemoved()) tracked.remove(creeper);
            else if (!active(creeper)) { tracked.remove(creeper); creeper.discard(); }
        }
        for (var child : List.copyOf(children)) {
            if (child.isRemoved()) children.remove(child);
            else if (!active(child)) { children.remove(child); child.discard(); }
        }
        causes.keySet().removeIf(entity -> entity.isRemoved() || !active(entity) || !tracked.contains(entity) && !children.contains(entity));
        if (!game.playing() || game.stage() == GameController.Stage.ENDING) unloadedCauses.clear();
    }

    /** Shared role/ally protection applies before native exposure, damage and knockback. */
    public boolean explosionTarget(Entity source, Entity target) {
        if (!managed(source)) return true;
        if (!active(source) || !game.items().wands().validTarget(target)) return false;
        return !(owner(source) instanceof ServerPlayer owner)
                || target == owner || game.items().wands().projectileTarget(owner, target);
    }

    public boolean allowDamage(LivingEntity victim, DamageSource source) {
        return explosionTarget(source.getDirectEntity(), victim);
    }

    public void stop() {
        unloadedCauses.clear();
        var detached = List.copyOf(tracked);
        tracked.clear();
        for (var creeper : detached) if (!creeper.isRemoved()) creeper.discard();
        var detachedChildren = List.copyOf(children);
        children.clear();
        for (var child : detachedChildren) if (!child.isRemoved()) child.discard();
        causes.clear();
    }

    public int trackedCount() { return tracked.size(); }
    public List<Creeper> snapshot() { return List.copyOf(tracked); }
}
