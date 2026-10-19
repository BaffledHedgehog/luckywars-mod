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

/** Надбавка электричества до защиты и friendly fire проводимости/опасных испытаний. */
@Mixin(ServerPlayer.class)
public abstract class LbcDamageMixin {
    @ModifyVariable(method="hurtServer",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private float skywars2$electricAmount(float amount,@Local(argsOnly=true) DamageSource source) {
        return LbcDamage.incomingDamage((ServerPlayer)(Object)this,source,amount);
    }
    @WrapOperation(method="hurtServer",at=@At(value="INVOKE",target=
            "Lnet/minecraft/server/level/ServerPlayer;canHarmPlayer(Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean skywars2$conduction(ServerPlayer victim,Player attacker,Operation<Boolean> original,
                                       @Local(argsOnly=true) DamageSource source) {
        return LbcDamage.allowsElectricFriendlyFire(source)
                || source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile projectile
                    && projectile.entityTags().contains(me.skywars2.game.LbcBlaze.TRIAL_PROJECTILE)
                || original.call(victim,attacker);
    }
}
