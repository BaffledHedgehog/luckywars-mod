package me.skywars2.mixin;

import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Native projectile collision entry used by bounded server qualification fixtures. */
public final class CreatureDropMixin {
    private CreatureDropMixin() { }
    @Mixin(ShulkerBullet.class)
    public interface BulletAccess {
        @Invoker("onHit") void skywars2$hit(HitResult hit);
    }
}
