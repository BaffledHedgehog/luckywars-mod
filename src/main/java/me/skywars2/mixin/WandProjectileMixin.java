package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.phys.HitResult;

/** Отсекаем защищённые цели до нативного onHitEntity, который сначала поджигает цель. */
@Mixin(Projectile.class)
public abstract class WandProjectileMixin {
    /** Перед нативной физикой обновляем самонаведение; пауза не допускает лишнего шага полёта. */
    @Mixin(net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile.class)
    public static abstract class Homing {
        @Inject(method="tick",at=@At("HEAD"),cancellable=true)
        private void skywars2$homingTick(CallbackInfo ci) {
            var game=SkyWars2.game();
            if(game!=null && !game.items().homing().beforeFireTick(
                    (net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile)(Object)this)) ci.cancel();
        }
    }
    /** Событие выбора цели вместо постоянного опроса AI подчинённых ифритов. */
    @Mixin(net.minecraft.world.entity.Mob.class)
    public static abstract class BlazeTarget {
        @Inject(method="setTarget",at=@At("TAIL"))
        private void skywars2$summonTarget(net.minecraft.world.entity.LivingEntity target,CallbackInfo ci) {
            var game=SkyWars2.game();
            if(game!=null) game.items().summons().targetChanged((net.minecraft.world.entity.Mob)(Object)this,target);
        }
        @Inject(method="setTarget",at=@At("HEAD"),cancellable=true)
        private void skywars2$blazeTarget(net.minecraft.world.entity.LivingEntity target,CallbackInfo ci) {
            var game=SkyWars2.game();
            if(game!=null && !game.items().consuming().canTrialTarget((Entity)(Object)this,target)) { ci.cancel(); return; }
            if(game!=null && (Object)this instanceof net.minecraft.world.entity.monster.Blaze blaze
                    && !game.items().blaze().canTarget(blaze,target)) ci.cancel();
        }
    }
    /** Облако использует физику стрелы; до native tick запрещаем движение при time-stop и в выгруженный чанк. */
    @Mixin(net.minecraft.world.entity.projectile.arrow.AbstractArrow.class)
    public static abstract class Cloud {
        @Inject(method="tick",at=@At("HEAD"),cancellable=true)
        private void skywars2$cloudTick(CallbackInfo ci) {
            var game=SkyWars2.game();
            if(game!=null && (Object)this instanceof net.minecraft.world.entity.projectile.arrow.Arrow arrow
                    && arrow.entityTags().contains(me.skywars2.game.LbcTeleport.CLOUD)
                    && !game.items().teleport().beforeCloudTick(arrow)) ci.cancel();
        }
        @Inject(method="onHitBlock",at=@At("TAIL"))
        private void skywars2$cloudLanded(net.minecraft.world.phys.BlockHitResult hit,CallbackInfo ci) {
            var game=SkyWars2.game();
            if(game!=null && (Object)this instanceof net.minecraft.world.entity.projectile.arrow.Arrow arrow
                    && arrow.entityTags().contains(me.skywars2.game.LbcTeleport.CLOUD)) game.items().teleport().cloudLanded(arrow);
        }
    }
    @Inject(method="canHitEntity",at=@At("HEAD"),cancellable=true)
    private void skywars2$wandTarget(Entity target,CallbackInfoReturnable<Boolean> ci) {
        var game=SkyWars2.game();
        if(game!=null && (!game.items().wands().canHit((Projectile)(Object)this,target)
                || !game.items().shooters().canHit((Projectile)(Object)this,target)
                || !game.items().electro().canHit((Projectile)(Object)this,target))) ci.setReturnValue(false);
    }
    @Inject(method="onHit",at=@At("TAIL"))
    private void skywars2$shooterHit(HitResult hit, CallbackInfo ci) {
        var game = SkyWars2.game();
        if (game != null) game.items().shooters().hit((Projectile)(Object)this, hit);
    }

    /** Поджигание происходит до hurtServer; записываем автора только принятого попадания. */
    @Mixin(net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball.class)
    public abstract static class Fire {
        @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="onHitEntity",at=@At(value="INVOKE",target=
                "Lnet/minecraft/world/entity/Entity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
        private boolean skywars2$fireOwner(Entity target,net.minecraft.server.level.ServerLevel level,
                net.minecraft.world.damagesource.DamageSource source,float amount,
                com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original) {
            boolean applied=original.call(target,level,source,amount);
            var projectile=(Projectile)(Object)this; var game=SkyWars2.game();
            if(applied && game!=null && game.items().wands().isFireProjectile(projectile)
                    && projectile.getOwner() instanceof net.minecraft.server.level.ServerPlayer owner
                    && target instanceof net.minecraft.world.entity.LivingEntity living)
                game.items().indirectKills().ignited(living,owner);
            return applied;
        }
        @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method="onHitBlock")
        private void skywars2$fireBlockOwner(net.minecraft.world.phys.BlockHitResult hit,
                com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
            var projectile=(Projectile)(Object)this; var game=SkyWars2.game();
            if(game!=null && game.items().wands().isFireProjectile(projectile)
                    && projectile.getOwner() instanceof net.minecraft.server.level.ServerPlayer owner)
                game.items().indirectKills().worldChanges(owner.level(),owner,()->original.call(hit));
            else original.call(hit);
        }
    }
}
