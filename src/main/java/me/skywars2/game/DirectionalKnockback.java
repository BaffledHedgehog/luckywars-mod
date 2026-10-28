package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/** Directs all native melee knockback along the direct attacker's full look vector. */
public final class DirectionalKnockback {
    private DirectionalKnockback() { }

    // Native extra knockback has a positive float admission gate. Retain its original
    // signed value while passing that gate; nesting restores the enclosing attack.
    private record Call(LivingEntity attacker, double power, Call previous) { }
    private static final ThreadLocal<Call> CALL = new ThreadLocal<>();
    public static final class Scope implements AutoCloseable {
        private final Call call;
        private Scope(LivingEntity attacker, float power) {
            call = new Call(attacker, power, CALL.get());
            CALL.set(call);
        }
        @Override public void close() {
            if (call.previous == null) CALL.remove(); else CALL.set(call.previous);
        }
    }
    public static Scope enter(LivingEntity attacker, float power) { return new Scope(attacker, power); }
    public static boolean melee(DamageSource source) {
        return source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO) || source.is(DamageTypes.MACE_SMASH)
                || source.is(DamageTypes.STING) || source.is(DamageTypes.SPEAR);
    }
    public static boolean directs(LivingEntity attacker, boolean fromEffect) {
        return fromEffect && attacker.getAttributeValue(Attributes.ATTACK_KNOCKBACK) != 0;
    }
    public static float admittedPower(LivingEntity attacker, float power) {
        double directed = effectPower(attacker, power);
        return directed == 0 ? 0 : Float.isFinite(power) && power != 0 ? Math.abs(power) : Float.MIN_NORMAL;
    }
    private static double effectPower(LivingEntity attacker, double original) {
        double attribute = attacker.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
        // Native getKnockback rounds to float before applying enchantment and /2.
        // Recover its additive remainder while retaining the raw signed double.
        double nativeAttribute = (float)attribute / 2.0F;
        return finite(attribute / 2 + (Double.isFinite(original) && Double.isFinite(nativeAttribute)
                ? original - nativeAttribute : 0));
    }
    private static double power(LivingEntity attacker, double strength, boolean fromEffect) {
        if (!fromEffect) return strength;
        var call = CALL.get();
        return effectPower(attacker, call != null && call.attacker == attacker ? call.power : strength);
    }
    private static double finite(double value) {
        return Double.isInfinite(value) ? Math.copySign(Double.MAX_VALUE, value) : value;
    }
    private static double component(double old, double look, double power, double resistance) {
        return finite(old / 2 + finite(finite(look * power) * resistance));
    }

    public static boolean living(LivingEntity attacker, LivingEntity target,
                                 double strength, boolean fromEffect) {
        double power = power(attacker, strength, fromEffect);
        // A cancelled attribute/enchantment sum must not damp existing movement.
        if (power == 0 || !Double.isFinite(power)) return true;
        var game = SkyWars2.game();
        if (LbcGreed.active(target) || game != null && game.items().timeStop().frozen(target)) return true;
        double resistance = 1 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        if (resistance <= 0) return true;
        Vec3 look = attacker.getLookAngle();
        Vec3 old = target.getDeltaMovement();
        // Apply the native horizontal damping equally in every direction. Neither
        // grounded lift nor a horizontal native remainder can override pitch.
        Vec3 result = new Vec3(component(old.x, look.x, power, resistance),
                component(old.y, look.y, power, resistance), component(old.z, look.z, power, resistance));
        if (!Double.isFinite(result.x) || !Double.isFinite(result.y) || !Double.isFinite(result.z)) return true;
        target.setDeltaMovement(result);
        target.needsSync = true;
        return true;
    }

    public static Vec3 nonliving(LivingEntity attacker, double strength,
                                 boolean fromEffect) {
        double power = power(attacker, strength, fromEffect);
        if (!Double.isFinite(power)) return Vec3.ZERO;
        Vec3 look = attacker.getLookAngle();
        return new Vec3(finite(look.x * power), finite(look.y * power), finite(look.z * power));
    }

}
