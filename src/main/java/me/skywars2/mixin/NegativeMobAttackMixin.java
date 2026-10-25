package me.skywars2.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import me.skywars2.game.NegativeAttributeEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class NegativeMobAttackMixin {
    @Inject(method="doHurtTarget",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"),cancellable=true)
    private void skywars2$negativeMobMeleeHeals(ServerLevel level,Entity target,CallbackInfoReturnable<Boolean> cir,@Local(ordinal=0) float amount){
        var attacker=(Mob)(Object)this;
        if(!NegativeAttributeEffects.negativeAttack(attacker,amount))return;
        boolean healed=NegativeAttributeEffects.mobAttack(attacker,target,amount);
        attacker.postPiercingAttack();cir.setReturnValue(healed);
    }
}
