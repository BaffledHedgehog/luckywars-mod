package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.skywars2.game.LbcDamage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Настроечная защита TNT/электричество до ванильной защиты и отдельные правила friendly fire. */
@Mixin(ServerPlayer.class)
public abstract class LbcDamageMixin {
    @Mixin(Player.class)
    public abstract static class TntProtection {
        @WrapOperation(method="hurtServer",at=@At(value="INVOKE",target=
                "Lnet/minecraft/world/entity/Avatar;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
        private boolean skywars2$protectedBlast(Player victim,
                net.minecraft.server.level.ServerLevel level,DamageSource source,float amount,Operation<Boolean> original) {
            float reduced=victim instanceof ServerPlayer ? LbcDamage.protectedTntDamage(source,amount) : amount;
            // Полная защита не должна запускать hurt cooldown/реакции от нулевого удара.
            return !(amount>0 && reduced==0) && original.call(victim,level,source,reduced);
        }
    }
    @ModifyVariable(method="hurtServer",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private float skywars2$electricAmount(float amount,@Local(argsOnly=true) DamageSource source) {
        return LbcDamage.incomingDamage((ServerPlayer)(Object)this,source,amount);
    }
    @WrapOperation(method="hurtServer",at=@At(value="INVOKE",target=
            "Lnet/minecraft/server/level/ServerPlayer;canHarmPlayer(Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean skywars2$conduction(ServerPlayer victim,Player attacker,Operation<Boolean> original,
                                       @Local(argsOnly=true) DamageSource source) {
        var game=me.skywars2.SkyWars2.game();
        if(victim==attacker&&game!=null&&(me.skywars2.game.LbcMoiseiBoots.managed(source.getDirectEntity())
                ||game.items().quiver().managedSecondary(source.getDirectEntity())
                ||game.items().babylonEffects().managedSecondary(source.getDirectEntity())
                ||source.getDirectEntity()!=null&&source.getDirectEntity().entityTags().contains("skywars2.quiver.fang")))return true;
        return LbcDamage.allowsElectricFriendlyFire(source) || LbcDamage.allowsKfcFriendlyFire(source)
                || LbcDamage.allowsTrapFriendlyFire(source)
                || source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile projectile
                    && projectile.entityTags().contains(me.skywars2.game.LbcBlaze.TRIAL_PROJECTILE)
                || original.call(victim,attacker);
    }
}
