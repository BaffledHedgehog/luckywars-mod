package me.skywars2.game;

import java.util.Comparator;
import java.util.TreeMap;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/** Independent clocks use the native saved hidden chain; only its strongest live node acts. */
public final class EffectLayers {
    private EffectLayers() { }

    public interface Access {
        MobEffectInstance skywars2$layerHidden();
        void skywars2$layerHidden(MobEffectInstance effect);
        void skywars2$layerDetails(MobEffectInstance effect);
        void skywars2$layerDuration(int duration);
    }

    /** Scale the actual remaining clocks in place; never re-admit or restart an effect. */
    public static boolean multiplyDurations(GameController game, LivingEntity target, int multiplier) {
        if (multiplier < 1) throw new IllegalArgumentException("Effect duration multiplier must be positive");
        if (multiplier == 1 || target.level().isClientSide()) return false;
        // Import legacy absorption before editing its native carrier, so it is scaled once.
        boolean changed = ShadowAbsorption.multiplyDurations(target, multiplier);
        changed |= game.items().ownedEffects().multiplyDurations(target, multiplier);
        for (var active : target.getActiveEffects()) {
            if (multiplyChainDurations(active, multiplier)) {
                changed = true;
                // Native sync/dirty flags without reapplying modifiers or onEffectStarted.
                ((OwnedEffectLayers.UpdateAccess)target).skywars2$ownedUpdated(active, false, null);
            }
        }
        return changed;
    }

    static boolean multiplyChainDurations(MobEffectInstance effect, int multiplier) {
        boolean changed = false;
        for (var node = effect; node != null; node = ((Access)node).skywars2$layerHidden()) {
            if (node.isInfiniteDuration() || node.getDuration() <= 0) continue;
            int scaled = (int)Math.min(Integer.MAX_VALUE, (long)node.getDuration() * multiplier);
            if (scaled != node.getDuration()) {
                ((Access)node).skywars2$layerDuration(scaled);
                changed = true;
            }
        }
        return changed;
    }

    /** Native copy constructor omits hidden successors; a suspension needs every clock. */
    public static MobEffectInstance copy(MobEffectInstance effect) {
        var result=new MobEffectInstance(effect);var tail=result;
        for(var next=((Access)effect).skywars2$layerHidden();next!=null;next=((Access)next).skywars2$layerHidden()){
            var node=new MobEffectInstance(next);((Access)tail).skywars2$layerHidden(node);tail=node;
        }
        return result;
    }

    public static boolean live(MobEffectInstance effect) {
        return effect.isInfiniteDuration() || effect.getDuration() > 0;
    }

    private static void collect(TreeMap<Integer, MobEffectInstance> layers, MobEffectInstance effect) {
        while (effect != null) {
            if (live(effect)) {
                var old = layers.get(effect.getAmplifier());
                if (old == null) layers.put(effect.getAmplifier(), new MobEffectInstance(effect));
                else {
                    boolean longer = !old.isInfiniteDuration()
                            && (effect.isInfiniteDuration() || effect.getDuration() > old.getDuration());
                    // Match native same-amplifier flag merging, confined to that amplifier's layer.
                    layers.put(effect.getAmplifier(), new MobEffectInstance(effect.getEffect(),
                            longer ? effect.getDuration() : old.getDuration(), effect.getAmplifier(),
                            longer ? effect.isAmbient() : old.isAmbient() && effect.isAmbient(),
                            effect.isVisible(), effect.showIcon()));
                }
            }
            effect = ((Access)effect).skywars2$layerHidden();
        }
    }

    /** Return active-detail changes, separately from whether an entity admitted the grant. */
    public static boolean merge(MobEffectInstance active, MobEffectInstance grant) {
        if (!active.getEffect().equals(grant.getEffect()) || !live(grant)) return false;
        var before = new MobEffectInstance(active);
        var layers = new TreeMap<Integer, MobEffectInstance>(Comparator.reverseOrder());
        collect(layers, active);
        collect(layers, grant);
        MobEffectInstance head = null, tail = null;
        for (var layer : layers.values()) {
            if (head == null) head = layer;
            else ((Access)tail).skywars2$layerHidden(layer);
            tail = layer;
        }
        if (head == null) return false;
        ((Access)active).skywars2$layerDetails(head);
        ((Access)active).skywars2$layerHidden(((Access)head).skywars2$layerHidden());
        return !active.equals(before);
    }

    /** Native countdown ticks all nodes; discard dead intermediates before selecting a successor. */
    public static boolean downgrade(MobEffectInstance active) {
        var previous = active;
        var next = ((Access)previous).skywars2$layerHidden();
        while (next != null) {
            var following = ((Access)next).skywars2$layerHidden();
            if (!live(next)) ((Access)previous).skywars2$layerHidden(following);
            else previous = next;
            next = following;
        }
        var hidden = ((Access)active).skywars2$layerHidden();
        if (active.getDuration() == 0 && hidden != null) {
            ((Access)active).skywars2$layerDetails(hidden);
            ((Access)active).skywars2$layerHidden(((Access)hidden).skywars2$layerHidden());
            return true;
        }
        return false;
    }
}
