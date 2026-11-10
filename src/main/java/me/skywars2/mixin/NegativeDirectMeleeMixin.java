package me.skywars2.mixin;

import me.skywars2.game.VanillaExpansionRules;

import me.skywars2.game.NegativeAttributeEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Covers native stab/custom-mob direct melee before damage admission or credit side effects. */
@Mixin(value={LivingEntity.class,ServerPlayer.class},priority=2500)
public abstract class NegativeDirectMeleeMixin {
    @Inject(method="hurtServer",at=@At("HEAD"),cancellable=true)
    private void skywars2$signedDirectMeleeHeals(ServerLevel level,DamageSource source,float amount,CallbackInfoReturnable<Boolean> cir){
        if(!VanillaExpansionRules.enabled()){return;}
        if(NegativeAttributeEffects.directMelee((LivingEntity)(Object)this,source,amount))cir.setReturnValue(false);
    }
}
