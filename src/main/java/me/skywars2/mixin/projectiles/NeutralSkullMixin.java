package me.skywars2.mixin.projectiles;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import me.skywars2.game.items.wands.LbcNeutral;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Adding real ownership must not turn source ownerless skulls'5 damage into8 or add owner healing. */
@Mixin(WitherSkull.class)
public abstract class NeutralSkullMixin {
    @WrapOperation(method="onHitEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean skywars2$wandWither(LivingEntity target,net.minecraft.world.effect.MobEffectInstance effect,Entity source,Operation<Boolean> original){
        var skull=(WitherSkull)(Object)this;
        var game=me.skywars2.SkyWars2.game();
        me.skywars2.game.combat.LbcIndirectKills.Owner cause=null;
        if(game!=null&&LbcNeutral.managed(skull)&&skull.getOwner() instanceof net.minecraft.server.level.ServerPlayer owner
                &&effect.is(net.minecraft.world.effect.MobEffects.WITHER)){
            cause=me.skywars2.game.combat.WeaponEnchantments.actorCause(skull,owner);
            if(cause!=null)me.skywars2.game.combat.WeaponEnchantments.markHealingEffect(effect,cause);
        }
        boolean accepted=original.call(target,effect,source);
        if(accepted&&cause!=null)game.items().indirectKills().acceptedInheritedEffect(target,effect,cause);
        return accepted;
    }
    @WrapOperation(method="onHitEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean skywars2$sourceDamage(Entity victim,ServerLevel level,DamageSource source,float amount,Operation<Boolean> original){
        var skull=(WitherSkull)(Object)this;
        return LbcNeutral.managed(skull)?original.call(victim,level,new DamageSource(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getOrThrow(net.minecraft.world.damagesource.DamageTypes.MAGIC),skull,skull.getOwner()),5f):original.call(victim,level,source,amount);
    }
    @WrapWithCondition(method="onHitEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;heal(F)V"))
    private boolean skywars2$noAddedHeal(LivingEntity owner,float amount){return !LbcNeutral.managed((Entity)(Object)this);}
    @WrapWithCondition(method="onHitEntity",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/enchantment/EnchantmentHelper;doPostAttackEffects(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private boolean skywars2$noAddedEnchant(ServerLevel level,Entity victim,DamageSource source){return !LbcNeutral.managed((Entity)(Object)this);}
}
