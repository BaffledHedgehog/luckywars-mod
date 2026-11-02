package me.skywars2.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Body contact must not nudge either a projectile or the creature touching it. */
@Mixin(Entity.class)
public abstract class ProjectileBodyCollisionMixin {
    @Inject(method="push(Lnet/minecraft/world/entity/Entity;)V",at=@At("HEAD"),cancellable=true)
    private void skywars2$noProjectileBodyBump(Entity other,CallbackInfo callback) {
        // Leave hit detection, deflection and explicit vector forces on their native paths.
        if((Object)this instanceof Projectile || other instanceof Projectile
                ||me.skywars2.game.LbcFallingHedgehogs.managed((Entity)(Object)this)
                ||me.skywars2.game.LbcFallingHedgehogs.managed(other))callback.cancel();
    }
}
