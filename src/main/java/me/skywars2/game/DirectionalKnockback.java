package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/** Redirects only the attribute share of native extra melee knockback. */
public final class DirectionalKnockback {
    private DirectionalKnockback() { }

    public static double attributeShare(LivingEntity attacker, double strength, boolean fromEffect) {
        if (!fromEffect || !Double.isFinite(strength) || strength <= 0) return 0;
        // Native getKnockback applies enchantments, then divides the attribute by two.
        return Math.min(strength, Math.max(0, attacker.getAttributeValue(Attributes.ATTACK_KNOCKBACK) / 2));
    }

    public static boolean living(LivingEntity attacker, LivingEntity target,
                                 double strength, double x, double z, boolean fromEffect) {
        double share = attributeShare(attacker, strength, fromEffect);
        if (share <= 0) return false;
        // Keep absolute immunity even when this replacement avoids native knockback's guard.
        var game = SkyWars2.game();
        if (LbcGreed.active(target) || game != null && game.items().timeStop().frozen(target)) return true;
        double resistance = 1 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        if (resistance <= 0) return true;
        double attributePower = share * resistance;
        double remainderPower = (strength - share) * resistance;
        Vec3 look = attacker.getLookAngle();
        Vec3 horizontal = new Vec3(x, 0, z).normalize();
        Vec3 old = target.getDeltaMovement();
        double y = target.onGround() && remainderPower > 0
                ? Math.min(.4, old.y / 2 + remainderPower) : old.y;
        Vec3 result = new Vec3(old.x / 2 - horizontal.x * remainderPower, y,
                old.z / 2 - horizontal.z * remainderPower).add(look.scale(attributePower));
        if (!Double.isFinite(result.x) || !Double.isFinite(result.y) || !Double.isFinite(result.z)) return false;
        target.setDeltaMovement(result);
        target.needsSync = true;
        return true;
    }

    public static Vec3 nonliving(LivingEntity attacker, double strength,
                                 boolean fromEffect, double x, double y, double z) {
        double share = attributeShare(attacker, strength, fromEffect);
        if (share <= 0) return new Vec3(x, y, z);
        return new Vec3(x, y, z).scale((strength - share) / strength)
                .add(attacker.getLookAngle().scale(share));
    }
}
