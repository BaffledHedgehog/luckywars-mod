package me.skywars2.game;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.skywars2.SkyWars2;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Server-owned absorption ledger. One independently expiring entry per signed amplifier. */
public final class ShadowAbsorption {
    private ShadowAbsorption() {}
    private static final Identifier HEALTH = Identifier.fromNamespaceAndPath("skywars2", "shadow_health");
    private static final Identifier YELLOW = Identifier.fromNamespaceAndPath("skywars2", "signed_absorption");
    private static final String SAVE = "skywars2_absorption_stacks";
    public interface Access { State skywars2$shadowAbsorption(); }
    public interface HiddenAccess { MobEffectInstance skywars2$absorptionHidden(); }
    public static final class State {
        public final Map<Integer, MobEffectInstance> entries = new LinkedHashMap<>();
        public boolean internal, reconciling, loading;
        public double capacity, stored;
        public double visibleMaximum;
        public double positiveCapacity;
        public boolean zeroMaximum;
    }
    public static State state(LivingEntity entity) { return ((Access)entity).skywars2$shadowAbsorption(); }
    public static boolean server(LivingEntity entity) { return !entity.level().isClientSide(); }
    public static boolean frozen(LivingEntity entity) {
        var game = SkyWars2.game();
        return game != null && game.items().timeStop().frozen(entity);
    }
    private static double finite(double value) {
        return Double.isNaN(value) ? 0 : Math.max(0, Math.min(Float.MAX_VALUE, value));
    }
    public static long level(int amplifier) { return (long)amplifier + 1L; }
    private static double maximumWithoutShadow(AttributeInstance attribute) {
        double added = attribute.getBaseValue();
        for (var modifier : attribute.getModifiers()) if (!modifier.id().equals(HEALTH)
                && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) added += modifier.amount();
        double value = added;
        for (var modifier : attribute.getModifiers()) if (!modifier.id().equals(HEALTH)
                && modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) value += added * modifier.amount();
        for (var modifier : attribute.getModifiers()) if (!modifier.id().equals(HEALTH)
                && modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) value *= 1 + modifier.amount();
        return finite(attribute.getAttribute().value().sanitizeValue(value));
    }
    private static void modifier(AttributeInstance attribute, Identifier id, double amount, AttributeModifier.Operation operation) {
        var old = attribute.getModifier(id);
        if (amount == 0) { if (old != null) attribute.removeModifier(id); return; }
        if (old != null && old.amount() == amount && old.operation() == operation) return;
        attribute.removeModifier(id);
        attribute.addTransientModifier(new AttributeModifier(id, amount, operation));
    }
    public static double net(State state) {
        // There are at most 2^32 distinct int keys; their exact sum fits a signed long.
        long levels = 0;
        for (int amplifier : state.entries.keySet()) levels += level(amplifier);
        return 4.0 * levels;
    }
    public static boolean needsHealing(LivingEntity entity) {
        if (entity.getHealth() <= 0) return false;
        if (entity.getHealth() < entity.getMaxHealth()) return true;
        return entity instanceof Access && state(entity).stored < state(entity).capacity;
    }
    public static boolean add(LivingEntity entity, MobEffectInstance effect, Entity source) {
        var s = state(entity);
        if (!entity.canBeAffected(effect) || (LbcGreed.active(entity) && source != entity)) return false;
        if (effect.getDuration() != -1 && effect.getDuration() <= 0) return false;
        var old = s.entries.get(effect.getAmplifier());
        var updated = old == null ? new MobEffectInstance(effect) : new MobEffectInstance(old);
        boolean changed = old == null || updated.update(effect);
        if (changed) s.entries.put(effect.getAmplifier(), updated);
        reconcile(entity, level(effect.getAmplifier()) > 0);
        if (changed) publish(entity, source);
        return changed;
    }
    private static void publish(LivingEntity entity, Entity source) {
        var s = state(entity);
        s.internal = true;
        try {
            if (s.entries.isEmpty()) entity.removeEffect(MobEffects.ABSORPTION);
            else entity.forceAddEffect(new MobEffectInstance(s.entries.values().iterator().next()), source);
        } finally { s.internal = false; }
    }
    private static void importEffect(State state, MobEffectInstance effect) {
        while (effect != null) {
            if (effect.is(MobEffects.ABSORPTION) && (effect.getDuration() == -1 || effect.getDuration() > 0)) {
                var old = state.entries.get(effect.getAmplifier());
                if (old == null) state.entries.put(effect.getAmplifier(), new MobEffectInstance(effect));
                else old.update(effect);
            }
            effect = ((HiddenAccess)effect).skywars2$absorptionHidden();
        }
    }
    public static void tick(LivingEntity entity) {
        var s = state(entity);
        if (!server(entity) || frozen(entity) || s.loading) return;
        // Support old saves and native code that inserted directly into the active effect map.
        if (s.entries.isEmpty() && entity.hasEffect(MobEffects.ABSORPTION)) {
            importEffect(s, entity.getEffect(MobEffects.ABSORPTION));
            reconcile(entity, false);
        }
        if (s.entries.isEmpty()) return;
        boolean changed = false;
        var iterator = s.entries.entrySet().iterator();
        boolean exhausted = net(s) > 0 && entity.getAbsorptionAmount() <= 0;
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var effect = entry.getValue();
            if (exhausted && level(entry.getKey()) > 0) { iterator.remove(); changed = true; continue; }
            if (effect.getDuration() == -1) continue;
            int duration = effect.getDuration() - 1;
            if (duration <= 0) { iterator.remove(); changed = true; }
            else entry.setValue(new MobEffectInstance(MobEffects.ABSORPTION, duration, effect.getAmplifier(),
                    effect.isAmbient(), effect.isVisible(), effect.showIcon()));
        }
        reconcile(entity, false);
        if (changed) publish(entity, null);
    }
    public static void clear(LivingEntity entity) {
        var s = state(entity);
        if (s.internal || s.loading || s.entries.isEmpty()) return;
        s.entries.clear();
        reconcile(entity, false);
    }
    /** Reconcile against the current native maximum, including equipment and health boost. */
    public static void reconcile(LivingEntity entity, boolean refill) {
        var s = state(entity);
        if (!server(entity) || s.reconciling || s.loading) return;
        s.reconciling = true;
        try {
            var health = entity.getAttribute(Attributes.MAX_HEALTH);
            var yellow = entity.getAttribute(Attributes.MAX_ABSORPTION);
            if (health == null || yellow == null) return;
            s.zeroMaximum = false;
            double base = maximumWithoutShadow(health);
            double contribution = net(s);
            double capacity = Math.min(base, Math.max(0, -contribution));
            // Greed's absolute health-loss guard also applies to an expiring positive counterweight.
            if (LbcGreed.active(entity) && capacity > s.capacity) capacity = Math.min(base, s.capacity);
            double visible = finite(entity.getHealth());
            double stored = Math.min(s.capacity, finite(s.stored));
            if (capacity > s.capacity) {
                // Conceal the top segment of the health bar, including its empty hearts.
                double moved = Math.max(0, visible - Math.max(0, base - capacity));
                visible -= moved;
                stored += moved;
            } else if (capacity < s.capacity) {
                double restored = Math.min(stored, s.capacity - capacity);
                visible += restored;
                stored -= restored;
            }
            s.capacity = capacity;
            s.stored = Math.min(capacity, stored);
            s.visibleMaximum = Math.max(0, base - capacity);
            s.zeroMaximum = base > 0 && capacity >= base;
            modifier(health, HEALTH, base > 0 ? -capacity / base : 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            double positive = Math.max(0, contribution);
            // Migrate the vanilla permanent modifier from saves made before this ledger existed.
            yellow.removeModifier(Identifier.withDefaultNamespace("effect.absorption"));
            modifier(yellow, YELLOW, positive, AttributeModifier.Operation.ADD_VALUE);
            float amount = entity.getAbsorptionAmount();
            entity.setAbsorptionAmount((float)finite(refill || positive > s.positiveCapacity ? Math.max(amount, positive) : amount));
            s.positiveCapacity = positive;
            // setHealth preserves all native damage/death rules: shadow HP never absorbs a hit.
            entity.setHealth((float)Math.min(visible, s.visibleMaximum));
        } finally { s.reconciling = false; }
    }
    /** Return true when the native heal was handled here. Negative heals stay native damage. */
    public static boolean heal(LivingEntity entity, float amount) {
        var s = state(entity);
        if (!server(entity) || s.loading || s.reconciling || s.capacity <= 0) return false;
        if (Float.isNaN(amount)) return true;
        if (!(amount > 0)) return false;
        reconcile(entity, false);
        if (entity.getHealth() <= 0) return true;
        double remaining = finite(amount);
        double visible = finite(entity.getHealth());
        double used = Math.min(remaining, Math.max(0, entity.getMaxHealth() - visible));
        entity.setHealth((float)finite(visible + used));
        remaining -= used;
        s.stored = Math.min(s.capacity, s.stored + remaining);
        return true;
    }
    public static void save(LivingEntity entity, ValueOutput output) {
        var s = state(entity);
        if (!s.entries.isEmpty()) {
            output.store(SAVE, MobEffectInstance.CODEC.listOf(), List.copyOf(s.entries.values()));
            output.putDouble(SAVE + "_capacity", s.capacity);
            output.putDouble(SAVE + "_health", s.stored);
        }
    }
    public static void beforeLoad(LivingEntity entity) {
        var s = state(entity);
        s.loading = true;
        var health = entity.getAttribute(Attributes.MAX_HEALTH);
        var yellow = entity.getAttribute(Attributes.MAX_ABSORPTION);
        if (health != null) health.removeModifier(HEALTH);
        if (yellow != null) yellow.removeModifier(YELLOW);
        s.capacity = s.stored = s.positiveCapacity = 0;
        s.zeroMaximum = false;
        s.entries.clear();
    }
    public static void load(LivingEntity entity, ValueInput input) {
        var s = state(entity);
        s.entries.clear();
        var saved = input.read(SAVE, MobEffectInstance.CODEC.listOf());
        var effects = saved.orElseGet(() -> {
            var effect = entity.getEffect(MobEffects.ABSORPTION);
            return effect == null ? List.of() : List.of(effect);
        });
        for (var effect : effects) importEffect(s, effect);
        s.capacity = saved.isPresent() ? finite(input.getDoubleOr(SAVE + "_capacity", 0)) : 0;
        s.stored = Math.min(s.capacity, finite(input.getDoubleOr(SAVE + "_health", 0)));
        s.positiveCapacity = Math.max(0, net(s));
        s.loading = false;
        reconcile(entity, false);
        if (!s.entries.isEmpty()) publish(entity, null);
    }
}
