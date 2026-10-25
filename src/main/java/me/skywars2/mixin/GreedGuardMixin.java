package me.skywars2.mixin;

import me.skywars2.game.LbcGreed;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reject active-Greed damage before native immunity exceptions and native impact side effects. */
@Mixin(value=LivingEntity.class,priority=2000)
public abstract class GreedGuardMixin {
    @Inject(method="setHealth",at=@At("HEAD"),cancellable=true)
    private void skywars2$greedRejectsDirectHealthLoss(float health,CallbackInfo ci){
        var target=(LivingEntity)(Object)this;
        if(LbcGreed.active(target)&&(Float.isNaN(health)
                ||net.minecraft.util.Mth.clamp(health,0,target.getMaxHealth())<target.getHealth()))ci.cancel();
    }
    @Inject(method="hurtServer",at=@At("HEAD"),cancellable=true)
    private void skywars2$greedRejectsDamage(ServerLevel level,DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci){
        if(LbcGreed.active((LivingEntity)(Object)this))ci.setReturnValue(false);
    }
    @Inject(method="knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V",at=@At("HEAD"),cancellable=true)
    private void skywars2$greedRejectsKnockback(double strength,double x,double z,DamageSource source,float damage,boolean fromEffect,CallbackInfo ci){
        if(LbcGreed.active((LivingEntity)(Object)this))ci.cancel();
    }

    @Mixin(value=ServerPlayer.class,priority=2000)
    public abstract static class Players {
        @Inject(method="hurtServer",at=@At("HEAD"),cancellable=true)
        private void skywars2$greedRejectsPlayerDamage(ServerLevel level,DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci){
            if(LbcGreed.active((ServerPlayer)(Object)this))ci.setReturnValue(false);
        }
    }

    /** Only the native actor-collision impulse is filtered; ordinary movement setters remain intact. */
    @Mixin(value=Entity.class,priority=2000)
    public abstract static class Collision {
        @WrapOperation(method="push(Lnet/minecraft/world/entity/Entity;)V",at=@At(value="INVOKE",target=
                "Lnet/minecraft/world/entity/Entity;push(DDD)V"))
        private void skywars2$greedRejectsCollisionImpulse(Entity recipient,double x,double y,double z,Operation<Void> original){
            if(!LbcGreed.active(recipient))original.call(recipient,x,y,z);
        }
    }

    @Mixin(value=Projectile.class,priority=2000)
    public abstract static class Projectiles {
        @Inject(method="hitTargetOrDeflectSelf",at=@At("HEAD"),cancellable=true)
        private void skywars2$greedReflectsBeforeImpact(HitResult hit,CallbackInfoReturnable<ProjectileDeflection> ci){
            if(hit instanceof EntityHitResult entityHit&&LbcGreed.reflect((Projectile)(Object)this,entityHit.getEntity()))
                ci.setReturnValue(ProjectileDeflection.REVERSE);
        }
    }

    /** Friendly arrows must reach the absolute Greed deflector before native team rejection. */
    @Mixin(value=net.minecraft.world.entity.projectile.arrow.AbstractArrow.class,priority=2000)
    public abstract static class Arrows {
        @WrapOperation(method="canHitEntity",at=@At(value="INVOKE",target=
                "Lnet/minecraft/world/entity/player/Player;canHarmPlayer(Lnet/minecraft/world/entity/player/Player;)Z"))
        private boolean skywars2$greedInterceptsFriendlyArrow(net.minecraft.world.entity.player.Player owner,
                net.minecraft.world.entity.player.Player target,Operation<Boolean> original) {
            return LbcGreed.active(target)||me.skywars2.game.LbcVexSword.wild((net.minecraft.world.entity.Entity)(Object)this)||original.call(owner,target);
        }
    }
}
