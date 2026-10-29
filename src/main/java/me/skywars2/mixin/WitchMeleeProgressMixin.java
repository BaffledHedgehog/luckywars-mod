package me.skywars2.mixin;

import me.skywars2.game.LbcWitchStatistics;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CombatTracker.class)
public abstract class WitchMeleeProgressMixin {
    @Shadow @Final private LivingEntity mob;
    @Inject(method="recordDamage",at=@At("HEAD"))
    private void skywars2$directMelee(DamageSource source,float damage,CallbackInfo ci){
        LbcWitchStatistics.meleeDamage(mob,source,damage);
    }
    @Mixin(net.minecraft.world.entity.player.Player.class)
    public abstract static class Attack {
        @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method={"attack","stabAttack"},at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
        private boolean skywars2$actualMelee(net.minecraft.world.entity.Entity victim,DamageSource source,float damage,
                com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original){
            return original.call(victim,LbcWitchStatistics.nativeMelee((net.minecraft.world.entity.player.Player)(Object)this,victim,source),damage);
        }
        @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="doSweepAttack",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
        private boolean skywars2$actualSweep(LivingEntity victim,net.minecraft.server.level.ServerLevel level,DamageSource source,float damage,
                com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original){
            return original.call(victim,level,LbcWitchStatistics.nativeMelee((net.minecraft.world.entity.player.Player)(Object)this,victim,source),damage);
        }
    }
}
