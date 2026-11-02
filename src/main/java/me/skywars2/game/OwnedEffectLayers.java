package me.skywars2.game;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Comparator;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Reversible, amplifier-local leases over the ordinary native effect chain. Server thread only. */
public final class OwnedEffectLayers {
    public interface ClockAccess {
        void skywars2$ownedTickDuration();
    }

    public interface UpdateAccess {
        void skywars2$ownedUpdated(MobEffectInstance effect, boolean refresh, Entity source);
    }

    private static final class Layer {
        MobEffectInstance baseline;
        final Map<String, MobEffectInstance> owners = new HashMap<>();
    }

    private final Map<LivingEntity, Map<Holder<MobEffect>, Map<Integer, Layer>>> targets = new IdentityHashMap<>();
    private int internal;

    public boolean grant(LivingEntity target, String ownerKey, MobEffectInstance effect, Entity source) {
        if (ownerKey == null || ownerKey.isEmpty()) throw new IllegalArgumentException("Empty effect owner");
        if (effect.is(MobEffects.ABSORPTION)) throw new IllegalArgumentException("Owned absorption is unsupported");
        if (target.level().isClientSide() || !EffectLayers.live(effect)) return false;
        var types = targets.get(target);
        var levels = types == null ? null : types.get(effect.getEffect());
        var layer = levels == null ? null : levels.get(effect.getAmplifier());
        var baseline = layer == null ? matching(target.getEffect(effect.getEffect()), effect.getAmplifier()) : null;
        boolean accepted;
        internal++;
        try { accepted = target.addEffect(single(effect), source); }
        finally { internal--; }
        if (!accepted) return false;
        // A Frozen Heart conversion accepted Resistance rather than an owned Slowness lease.
        if(target instanceof net.minecraft.server.level.ServerPlayer player&&effect.is(MobEffects.SLOWNESS)
                &&me.skywars2.SkyWars2.game()!=null&&me.skywars2.SkyWars2.game().items().containedSkills().convertsSlowness(player,effect))return true;
        if (layer == null) {
            layer = new Layer();
            layer.baseline = baseline;
            targets.computeIfAbsent(target, key -> new HashMap<>())
                    .computeIfAbsent(effect.getEffect(), key -> new HashMap<>()).put(effect.getAmplifier(), layer);
        }
        layer.owners.put(ownerKey, merge(layer.owners.get(ownerKey), effect));
        return true;
    }

    /** Withdraw every contribution made by this key, retaining all other amplifiers and owners. */
    public void withdraw(LivingEntity target, String ownerKey) {
        var types = targets.get(target);
        if (types == null) return;
        for (var type : new HashMap<>(types).entrySet()) {
            boolean changed = false;
            for (var layer : type.getValue().values()) changed |= layer.owners.remove(ownerKey) != null;
            if (changed) rebuild(target, type.getKey(), type.getValue());
            type.getValue().values().removeIf(layer -> layer.owners.isEmpty());
            if (type.getValue().isEmpty()) types.remove(type.getKey());
        }
        if (types.isEmpty()) targets.remove(target);
    }

    Object suspend(LivingEntity target) { return targets.remove(target); }
    /** Frozen Heart consumes slowdowns, including their old leases, but keeps useful reversed layers. */
    void purgePositiveSlowness(LivingEntity target) {
        var types=targets.get(target);if(types==null)return;
        var levels=types.get(MobEffects.SLOWNESS);if(levels==null)return;
        levels.keySet().removeIf(level->level>=0);
        if(levels.isEmpty())types.remove(MobEffects.SLOWNESS);
        if(types.isEmpty())targets.remove(target);
    }
    @SuppressWarnings("unchecked") void resume(LivingEntity target,Object saved) {
        if(saved!=null)targets.put(target,(Map<Holder<MobEffect>,Map<Integer,Layer>>)saved);
    }
    /** Forget only harmful amplifier leases before the native chain is cleansed. */
    public void purgeDebuffs(LivingEntity target) {
        var types=targets.get(target);if(types==null)return;
        for(var type:new HashMap<>(types).entrySet()){
            type.getValue().entrySet().removeIf(entry->LbcParasite.rejects(target,
                    new MobEffectInstance(type.getKey(),1,entry.getKey())));
            if(type.getValue().isEmpty())types.remove(type.getKey());
        }
        if(types.isEmpty())targets.remove(target);
    }
    public void forget(LivingEntity target) { targets.remove(target); }

    /** Keep independently ticking baselines and leases aligned with their native projection. */
    boolean multiplyDurations(LivingEntity target, int multiplier) {
        var types = targets.get(target);
        if (types == null) return false;
        boolean changed = false;
        for (var levels : types.values()) for (var layer : levels.values()) {
            changed |= EffectLayers.multiplyChainDurations(layer.baseline, multiplier);
            for (var effect : layer.owners.values())
                changed |= EffectLayers.multiplyChainDurations(effect, multiplier);
        }
        return changed;
    }

    /** Non-destructive value image; leases must survive replacement player objects. */
    Object checkpoint(LivingEntity target) { return copyLayers(targets.get(target)); }
    @SuppressWarnings("unchecked") void restoreCheckpoint(LivingEntity target,Object image) {
        targets.remove(target);
        if(image!=null)targets.put(target,copyLayers((Map<Holder<MobEffect>,Map<Integer,Layer>>)image));
    }
    @SuppressWarnings("unchecked") static Object copyCheckpoint(Object image) {return copyLayers((Map<Holder<MobEffect>,Map<Integer,Layer>>)image);}
    private static Map<Holder<MobEffect>,Map<Integer,Layer>> copyLayers(Map<Holder<MobEffect>,Map<Integer,Layer>> source) {
        if(source==null)return null;
        var result=new HashMap<Holder<MobEffect>,Map<Integer,Layer>>();
        source.forEach((type,levels)->{
            var saved=new HashMap<Integer,Layer>();
            levels.forEach((amplifier,layer)->{
                var copy=new Layer();copy.baseline=layer.baseline==null?null:EffectLayers.copy(layer.baseline);
                layer.owners.forEach((owner,effect)->copy.owners.put(owner,EffectLayers.copy(effect)));
                saved.put(amplifier,copy);
            });result.put(type,saved);
        });return result;
    }

    public void stop() {
        for (var target : new IdentityHashMap<>(targets).keySet()) {
            var types = targets.get(target);
            for (var entry : types.entrySet()) {
                for (var layer : entry.getValue().values()) layer.owners.clear();
                rebuild(target, entry.getKey(), entry.getValue());
            }
        }
        targets.clear();
    }

    public int debugTargetCount() { return targets.size(); }

    /** Only called after native admission succeeds, including admitted hidden grants. */
    public void acceptedExternal(LivingEntity target, MobEffectInstance grant) {
        if (internal != 0) return;
        var types = targets.get(target);
        var levels = types == null ? null : types.get(grant.getEffect());
        if (levels == null) return;
        for (var node = grant; node != null; node = ((EffectLayers.Access)node).skywars2$layerHidden()) {
            var layer = levels.get(node.getAmplifier());
            if (layer != null && EffectLayers.live(node)) layer.baseline = merge(layer.baseline, node);
        }
    }

    /** Native removal/replacement invalidates all ownership and baseline for this effect type. */
    public void removedExternal(LivingEntity target, Holder<MobEffect> type) {
        if (internal != 0) return;
        var types = targets.get(target);
        if (types == null) return;
        types.remove(type);
        if (types.isEmpty()) targets.remove(target);
    }

    /** Called exactly where the real tickServer is about to count down its chain. */
    public void tickNative(LivingEntity target, MobEffectInstance active) {
        var types = targets.get(target);
        var levels = types == null ? null : types.get(active.getEffect());
        if (levels == null || target.getEffect(active.getEffect()) != active) return;
        for (var layer : levels.values()) {
            if (layer.baseline != null) {
                ((ClockAccess)layer.baseline).skywars2$ownedTickDuration();
                if (!EffectLayers.live(layer.baseline)) layer.baseline = null;
            }
            for (var owned : layer.owners.values()) ((ClockAccess)owned).skywars2$ownedTickDuration();
            layer.owners.values().removeIf(owned -> !EffectLayers.live(owned));
        }
        levels.values().removeIf(layer -> layer.owners.isEmpty());
        if (levels.isEmpty()) types.remove(active.getEffect());
        if (types.isEmpty()) targets.remove(target);
    }

    private void rebuild(LivingEntity target, Holder<MobEffect> type, Map<Integer, Layer> owned) {
        var active = target.getEffect(type);
        if (active == null) return;
        var nodes = new TreeMap<Integer, MobEffectInstance>(Comparator.reverseOrder());
        for (var node = active; node != null; node = ((EffectLayers.Access)node).skywars2$layerHidden()) {
            if (!owned.containsKey(node.getAmplifier()) && EffectLayers.live(node))
                nodes.put(node.getAmplifier(), single(node));
        }
        for (var entry : owned.entrySet()) {
            var layer = entry.getValue();
            MobEffectInstance result = layer.baseline == null ? null : single(layer.baseline);
            for (var value : layer.owners.values()) result = merge(result, value);
            if (result != null && EffectLayers.live(result)) nodes.put(entry.getKey(), result);
        }
        MobEffectInstance head = null, tail = null;
        for (var node : nodes.values()) {
            if (head == null) head = node;
            else ((EffectLayers.Access)tail).skywars2$layerHidden(node);
            tail = node;
        }
        internal++;
        try {
            if (head == null) target.removeEffect(type);
            else {
                // This edits an already admitted chain. Rechecking admission here could keep
                // the withdrawn owner alive when an effect-cleansing artifact was just equipped.
                ((EffectLayers.Access)active).skywars2$layerDetails(head);
                ((EffectLayers.Access)active).skywars2$layerHidden(((EffectLayers.Access)head).skywars2$layerHidden());
                ((UpdateAccess)target).skywars2$ownedUpdated(active, true, null);
            }
        } finally { internal--; }
    }

    private static MobEffectInstance matching(MobEffectInstance active, int amplifier) {
        MobEffectInstance result = null;
        for (var node = active; node != null; node = ((EffectLayers.Access)node).skywars2$layerHidden())
            if (node.getAmplifier() == amplifier && EffectLayers.live(node)) result = merge(result, node);
        return result;
    }

    private static MobEffectInstance single(MobEffectInstance effect) {
        return new MobEffectInstance(effect.getEffect(), effect.getDuration(), effect.getAmplifier(),
                effect.isAmbient(), effect.isVisible(), effect.showIcon());
    }

    private static MobEffectInstance merge(MobEffectInstance old, MobEffectInstance grant) {
        if (old == null) return single(grant);
        var result = single(old);
        EffectLayers.merge(result, single(grant));
        return result;
    }
}
