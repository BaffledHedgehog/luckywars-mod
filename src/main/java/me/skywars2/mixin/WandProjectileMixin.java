package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import me.skywars2.game.ProjectileLifetimes;
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
public abstract class WandProjectileMixin implements me.skywars2.game.LbcThrownUtilities.OwnerGate {
    @org.spongepowered.asm.mixin.gen.Accessor("leftOwner")
    public abstract boolean skywars2$leftOwner();
    /** Прямой setter вместо полной загрузки NBT для каждого ножа в массовом залпе. */
    @Mixin(net.minecraft.world.entity.projectile.arrow.AbstractArrow.class)
    public interface ArrowAccess {
        @org.spongepowered.asm.mixin.gen.Invoker("setPierceLevel")
        void skywars2$setPierceLevel(byte level);
        @org.spongepowered.asm.mixin.gen.Invoker("isInGround")
        boolean skywars2$isInGround();
        @org.spongepowered.asm.mixin.gen.Invoker("setInGround")
        void skywars2$setInGround(boolean value);
    }

    /** Splash status effects normally pass the shooter as source, so check allegiance at their native call site. */
    @Mixin(net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion.class)
    public static abstract class Potion {
        @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="onHitAsPotion",at=@At(value="INVOKE",target=
                "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"))
        private boolean skywars2$splashTarget(net.minecraft.world.entity.LivingEntity target,
                net.minecraft.world.effect.MobEffectInstance effect,Entity source,
                com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original) {
            var game=SkyWars2.game();
            return (game==null || game.items().indirectKills().allowProjectileEffect(target,effect,(Entity)(Object)this))
                    && original.call(target,effect,me.skywars2.game.LbcPotionSpawnerDrops.managed((Entity)(Object)this)
                            ? (Entity)(Object)this : source);
        }
    }

    @Inject(method="isPickable",at=@At("HEAD"),cancellable=true)
    private void skywars2$spellNotPickable(CallbackInfoReturnable<Boolean> ci) {
        if(ProjectileLifetimes.protectedWandProjectile((Entity)(Object)this)) ci.setReturnValue(false);
    }

    // Vanilla attacks and projectile redirection pass byAttack=true; Electro's explicit reflection passes false.
    @Inject(method="deflect(Lnet/minecraft/world/entity/projectile/ProjectileDeflection;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/EntityReference;ZLnet/minecraft/world/phys/Vec3;)Z",
            at=@At("HEAD"),cancellable=true)
    private void skywars2$spellIgnoresAttackDeflection(net.minecraft.world.entity.projectile.ProjectileDeflection deflection,
            Entity attacker,net.minecraft.world.entity.EntityReference<Entity> owner,boolean byAttack,
            net.minecraft.world.phys.Vec3 power,CallbackInfoReturnable<Boolean> ci) {
        if(byAttack && ProjectileLifetimes.protectedWandProjectile((Entity)(Object)this)) ci.setReturnValue(false);
    }

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="hitTargetOrDeflectSelf",at=@At(value="INVOKE",target=
            "Lnet/minecraft/world/entity/Entity;deflection(Lnet/minecraft/world/entity/projectile/Projectile;)Lnet/minecraft/world/entity/projectile/ProjectileDeflection;"))
    private net.minecraft.world.entity.projectile.ProjectileDeflection skywars2$spellIgnoresNativeDeflector(
            Entity target,Projectile projectile,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<net.minecraft.world.entity.projectile.ProjectileDeflection> original) {
        return ProjectileLifetimes.protectedWandProjectile(projectile)
                ? net.minecraft.world.entity.projectile.ProjectileDeflection.NONE : original.call(target,projectile);
    }

    /** Перед нативной физикой обновляем самонаведение; пауза не допускает лишнего шага полёта. */
    @Mixin(net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile.class)
    public static abstract class Homing {
        @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method="tick")
        private void skywars2$emitterTick(com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original){
            var game=SkyWars2.game();
            if(game==null){original.call();return;}
            try(var scope=game.items().spellSources().scope((Entity)(Object)this)){original.call();}
        }
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
            if(me.skywars2.game.LbcBedwarsRoleGuards.shop(target)||!me.skywars2.game.LbcBedwarsRoleGuards.canEmitFrom((Entity)(Object)this)){ci.cancel();return;}
            var ownerGame=SkyWars2.game();if(target!=null&&ownerGame!=null&&ownerGame.bedwars()!=null&&!ownerGame.bedwars().auxiliaries().canAttack((Entity)(Object)this,target)){ci.cancel();return;}
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
            if(game!=null && !game.items().madokaBow().beforeProjectile((Projectile)(Object)this)){ci.cancel();return;}
            if(game!=null && (Object)this instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow actual
                    && actual.level() instanceof net.minecraft.server.level.ServerLevel && (!game.items().homingArrows().beforeArrow(actual)||!game.items().trinketArrows().beforeArrow(actual))){ci.cancel();return;}
            if(game!=null && (Object)this instanceof net.minecraft.world.entity.projectile.arrow.Arrow arrow
                    && arrow.entityTags().contains(me.skywars2.game.LbcTeleport.CLOUD)
                    && !game.items().teleport().beforeCloudTick(arrow)) ci.cancel();
        }
        @Inject(method="doPostHurtEffects",at=@At("HEAD"))
        private void skywars2$madokaAccepted(net.minecraft.world.entity.LivingEntity target,CallbackInfo ci){var game=SkyWars2.game();if(game!=null)game.items().madokaBow().entityHit((Projectile)(Object)this,target);}
        @Inject(method="onHitBlock",at=@At("TAIL"))
        private void skywars2$cloudLanded(net.minecraft.world.phys.BlockHitResult hit,CallbackInfo ci) {
            var game=SkyWars2.game();
            if(game!=null && (Object)this instanceof net.minecraft.world.entity.projectile.arrow.Arrow arrow
                    && arrow.entityTags().contains(me.skywars2.game.LbcTeleport.CLOUD)) game.items().teleport().cloudLanded(arrow);
        }
    }
    @Inject(method="canHitEntity",at=@At("HEAD"),cancellable=true)
    private void skywars2$wandTarget(Entity target,CallbackInfoReturnable<Boolean> ci) {
        if(me.skywars2.game.LbcGreed.active(target) && target.isAlive()) {
            var shot=(Projectile)(Object)this;
            boolean leavingOwner=shot.getOwner()==target && target.getBoundingBox().inflate(.2).contains(shot.position());
            var game=SkyWars2.game();
            ci.setReturnValue(!leavingOwner && target.canBeHitByProjectile()
                    && (game==null || game.items().wands().canHit(shot,target)));
            return;
        }
        if(target instanceof Projectile && (ProjectileLifetimes.protectedWandProjectile(target)
                || ProjectileLifetimes.protectedWandProjectile((Entity)(Object)this))) {
            ci.setReturnValue(false);
            return;
        }
        var game=SkyWars2.game();
        if(game!=null && (!game.items().wands().canHit((Projectile)(Object)this,target)
                || !game.items().shooters().canHit((Projectile)(Object)this,target)
                || !game.items().electro().canHit((Projectile)(Object)this,target)
                || !game.items().helicopterEncounter().canHit((Projectile)(Object)this,target))) ci.setReturnValue(false);
        else if(game!=null && me.skywars2.game.LbcBlaze.isKfcSpirit((Entity)(Object)this))
            ci.setReturnValue(target.canBeHitByProjectile());
    }
    @Inject(method="onHit",at=@At("TAIL"))
    private void skywars2$shooterHit(HitResult hit, CallbackInfo ci) {
        var game = SkyWars2.game();
        if (game != null){
            var projectile=(Projectile)(Object)this;
            if(hit instanceof net.minecraft.world.phys.BlockHitResult block&&me.skywars2.game.LbcMadokaBow.recognizes(projectile)
                    &&!game.items().madokaBow().blockHit(projectile,block))return;
            game.items().shooters().hit(projectile,hit);game.items().trinketArrows().hit(projectile,hit);
        }
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
            if(applied && game!=null && (game.items().wands().isFireProjectile(projectile)||me.skywars2.game.LbcSpecialFireballs.managed(projectile))
                    && projectile.getOwner() instanceof net.minecraft.server.level.ServerPlayer owner
                    && target instanceof net.minecraft.world.entity.LivingEntity living)
                game.items().indirectKills().projectileIgnited(living,owner,me.skywars2.game.LbcBlaze.isKfcSpirit(projectile)||me.skywars2.game.LbcSpecialFireballs.managed(projectile));
            return applied;
        }
        @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method="onHitBlock")
        private void skywars2$fireBlockOwner(net.minecraft.world.phys.BlockHitResult hit,
                com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
            var projectile=(Projectile)(Object)this; var game=SkyWars2.game();
            if(game!=null && (game.items().wands().isFireProjectile(projectile)||me.skywars2.game.LbcSpecialFireballs.managed(projectile))
                    && projectile.getOwner() instanceof net.minecraft.server.level.ServerPlayer owner)
                game.items().indirectKills().worldChanges((net.minecraft.server.level.ServerLevel)projectile.level(),projectile,()->original.call(hit));
            else original.call(hit);
        }
    }
}
