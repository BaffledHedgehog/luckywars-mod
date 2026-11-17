package me.skywars2.mixin;

import me.skywars2.game.genie.GenieRuntime;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Preserve the real shooter and native contact behavior; only tagged genie shots alter native power. */
@Mixin(Snowball.class)
public abstract class GenieProjectileMixin {
    @ModifyArg(method="onHitEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"),index=1)
    private float skywars2$geniePower(float ordinary){return GenieRuntime.projectilePower((Projectile)(Object)this,ordinary);}
    @Mixin(SmallFireball.class)
    public abstract static class Fireball {
        @ModifyArg(method="onHitEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"),index=2)
        private float skywars2$genieFireballPower(float ordinary){return GenieRuntime.projectilePower((Projectile)(Object)this,ordinary);}
    }
}
