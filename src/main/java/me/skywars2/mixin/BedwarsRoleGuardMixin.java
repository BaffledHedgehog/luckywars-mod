package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.skywars2.game.LbcBedwarsRoleGuards;
import me.skywars2.game.LbcBedwarsHarm;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Finite native shop admission points; no generic setter/removal interception or team override. */
public final class BedwarsRoleGuardMixin {
    private BedwarsRoleGuardMixin(){}
    @Mixin(value=LivingEntity.class,priority=5000)
    public abstract static class Living {
        @WrapMethod(method="hurtServer")
        private boolean skywars2$shopDamage(ServerLevel level,DamageSource source,float amount,Operation<Boolean> original){
            return LbcBedwarsRoleGuards.affectable((Entity)(Object)this)&&original.call(level,source,amount);
        }
        @Inject(method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",at=@At("HEAD"),cancellable=true)
        private void skywars2$shopEffect(MobEffectInstance effect,Entity source,CallbackInfoReturnable<Boolean> cir){
            if(!LbcBedwarsRoleGuards.affectable((Entity)(Object)this)||!LbcBedwarsHarm.effectAllowed((LivingEntity)(Object)this,effect,source))cir.setReturnValue(false);
        }
        @Inject(method="forceAddEffect",at=@At("HEAD"),cancellable=true)
        private void skywars2$shopForcedEffect(MobEffectInstance effect,Entity source,CallbackInfo ci){
            if(!LbcBedwarsRoleGuards.affectable((Entity)(Object)this)||!LbcBedwarsHarm.effectAllowed((LivingEntity)(Object)this,effect,source))ci.cancel();
        }
        @Inject(method="heal",at=@At("HEAD"),cancellable=true)
        private void skywars2$shopHeal(float amount,CallbackInfo ci){if(!LbcBedwarsRoleGuards.affectable((Entity)(Object)this))ci.cancel();}
        @Inject(method="canBeAffected",at=@At("HEAD"),cancellable=true)
        private void skywars2$shopStatusTarget(MobEffectInstance effect,CallbackInfoReturnable<Boolean> cir){if(!LbcBedwarsRoleGuards.affectable((Entity)(Object)this))cir.setReturnValue(false);}
        @Inject(method="isAffectedByPotions",at=@At("HEAD"),cancellable=true)
        private void skywars2$shopPotionTarget(CallbackInfoReturnable<Boolean> cir){if(LbcBedwarsRoleGuards.shop((Entity)(Object)this))cir.setReturnValue(false);}
        @Inject(method="isPushable",at=@At("HEAD"),cancellable=true)
        private void skywars2$shopCollisionTarget(CallbackInfoReturnable<Boolean> cir){if(LbcBedwarsRoleGuards.shop((Entity)(Object)this))cir.setReturnValue(false);}
        @Inject(method="knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V",at=@At("HEAD"),cancellable=true)
        private void skywars2$shopKnockback(double power,double x,double z,DamageSource source,float damage,boolean effect,CallbackInfo ci){if(!LbcBedwarsRoleGuards.affectable((Entity)(Object)this)||!LbcBedwarsHarm.damageAllowed((Entity)(Object)this,source))ci.cancel();}
    }
    @Mixin(value=Mob.class,priority=5000)
    public abstract static class Mobs {
        @Inject(method="canAttack",at=@At("HEAD"),cancellable=true)
        private void skywars2$shopAttackSelection(LivingEntity target,CallbackInfoReturnable<Boolean> cir){
            if(LbcBedwarsRoleGuards.shop(target)||!LbcBedwarsRoleGuards.canEmitFrom((Entity)(Object)this)){cir.setReturnValue(false);return;}
            var game=me.skywars2.SkyWars2.game();if(game!=null&&game.bedwars()!=null&&!game.bedwars().auxiliaries().canAttack((Entity)(Object)this,target))cir.setReturnValue(false);
        }
        @Inject(method="convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;",at=@At("HEAD"),cancellable=true)
        private void skywars2$shopBeforeConversion(EntityType<?> type,ConversionParams params,EntitySpawnReason reason,ConversionParams.AfterConversion<?> after,CallbackInfoReturnable<Mob> cir){
            if(!LbcBedwarsRoleGuards.affectable((Entity)(Object)this))cir.setReturnValue(null);
        }
        @Inject(method="interact",at=@At("HEAD"),cancellable=true)
        private void skywars2$shopNoVanillaItemFallback(Player player,InteractionHand hand,Vec3 at,CallbackInfoReturnable<InteractionResult> cir){
            // The ordinary registered shop callback owns its UI before vanilla Mob/item fallback.
            if(LbcBedwarsRoleGuards.shop((Entity)(Object)this))cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
    @Mixin(value=Entity.class,priority=5000)
    public abstract static class Impulses {
        @Inject(method="push(DDD)V",at=@At("HEAD"),cancellable=true)
        private void skywars2$shopImpulse(double x,double y,double z,CallbackInfo ci){if(!LbcBedwarsRoleGuards.affectable((Entity)(Object)this))ci.cancel();}
        @Inject(method="igniteForTicks",at=@At("HEAD"),cancellable=true)
        private void skywars2$shopIgnition(int ticks,CallbackInfo ci){if(!LbcBedwarsRoleGuards.affectable((Entity)(Object)this))ci.cancel();}
        @Inject(method="thunderHit",at=@At("HEAD"),cancellable=true)
        private void skywars2$shopGenericLightning(ServerLevel level,LightningBolt bolt,CallbackInfo ci){if(!LbcBedwarsRoleGuards.affectable((Entity)(Object)this))ci.cancel();}
    }
    @Mixin(value=net.minecraft.world.entity.npc.villager.Villager.class,priority=5000)
    public abstract static class Villagers {
        @Inject(method="thunderHit",at=@At("HEAD"),cancellable=true)
        private void skywars2$shopBeforeWitchFactory(ServerLevel level,LightningBolt bolt,CallbackInfo ci){if(!LbcBedwarsRoleGuards.affectable((Entity)(Object)this))ci.cancel();}
    }
    @Mixin(value=ServerLevel.class,priority=5000)
    public abstract static class Publications {
        @Inject(method="addFreshEntity",at=@At("HEAD"),cancellable=true)
        private void skywars2$shopCannotEmit(Entity entity,CallbackInfoReturnable<Boolean> cir){if(!LbcBedwarsRoleGuards.publication((ServerLevel)(Object)this))cir.setReturnValue(false);}
    }
    @Mixin(net.minecraft.world.entity.projectile.Projectile.class)
    public interface ProjectileOwner {
        @org.spongepowered.asm.mixin.gen.Accessor("owner") EntityReference<Entity> skywars2$owner();
        @org.spongepowered.asm.mixin.gen.Invoker("canHitEntity") boolean skywars2$canHit(Entity target);
    }
    @Mixin(AreaEffectCloud.class)
    public interface CloudOwner {
        @org.spongepowered.asm.mixin.gen.Accessor("owner") EntityReference<LivingEntity> skywars2$owner();
        @org.spongepowered.asm.mixin.gen.Accessor("potionContents") net.minecraft.world.item.alchemy.PotionContents skywars2$potion();
        @org.spongepowered.asm.mixin.gen.Accessor("potionDurationScale") float skywars2$scale();
    }
    @Mixin(net.minecraft.world.entity.item.PrimedTnt.class)
    public interface TntOwner {
        @org.spongepowered.asm.mixin.gen.Accessor("owner") EntityReference<LivingEntity> skywars2$owner();
    }
    @Mixin(AreaEffectCloud.class)
    public abstract static class Clouds {
        @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method="serverTick",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
        private java.util.List<LivingEntity> skywars2$alliedCloudRecipients(java.util.List<LivingEntity> original){
            if(!LbcBedwarsHarm.active())return original;var cloud=(AreaEffectCloud)(Object)this;var access=(CloudOwner)(Object)this;
            var effects=new java.util.ArrayList<MobEffectInstance>();access.skywars2$potion().forEachEffect(effects::add,access.skywars2$scale());
            var admitted=new java.util.ArrayList<>(original);admitted.removeIf(target->LbcBedwarsRoleGuards.shop(target)||effects.stream().noneMatch(effect->LbcBedwarsHarm.effectAllowed(target,effect,cloud)));return admitted;
        }
    }
}
