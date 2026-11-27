package me.skywars2.mixin.enchantments;

import me.skywars2.game.combat.WandPowerEffectAccess;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.skywars2.game.combat.LbcIndirectKills;
import me.skywars2.game.combat.WeaponEnchantments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Provenance travels with native copy/hidden-effect and only while that effect wins. */
@Mixin(MobEffectInstance.class)
public abstract class WandPowerEffectMixin implements WandPowerEffectAccess {
    @org.spongepowered.asm.mixin.Shadow private MobEffectInstance hiddenEffect;
    @Unique private LbcIndirectKills.Owner skywars2$healingCause;
    @Override public MobEffectInstance skywars2$hiddenEffect(){return hiddenEffect;}
    @Override public LbcIndirectKills.Owner skywars2$powerCause(){return skywars2$healingCause;}
    @Override public void skywars2$powerCause(LbcIndirectKills.Owner cause){skywars2$healingCause=cause;}

    @Inject(method="setDetailsFrom",at=@At("TAIL"))
    private void skywars2$copyHealingCause(MobEffectInstance source,CallbackInfo ci){
        skywars2$healingCause=((WandPowerEffectAccess)(Object)source).skywars2$powerCause();
    }
    @WrapMethod(method="update")
    private boolean skywars2$updateHealingCause(MobEffectInstance incoming,Operation<Boolean> original){
        var self=(MobEffectInstance)(Object)this;
        boolean wins=incoming.getAmplifier()>self.getAmplifier()
                ||incoming.getAmplifier()==self.getAmplifier()&&self.getDuration()!=-1
                  &&(incoming.getDuration()==-1||incoming.getDuration()>self.getDuration());
        boolean changed=original.call(incoming);
        if(wins)skywars2$healingCause=((WandPowerEffectAccess)(Object)incoming).skywars2$powerCause();
        return changed;
    }
    @WrapOperation(method="tickServer",at=@At(value="INVOKE",target="Lnet/minecraft/world/effect/MobEffect;applyEffectTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;I)Z"))
    private boolean skywars2$healScope(MobEffect effect,ServerLevel level,LivingEntity target,int amplifier,Operation<Boolean> original){
        try(var scope=WeaponEnchantments.effectHealing(skywars2$healingCause)){
            return original.call(effect,level,target,amplifier);
        }
    }
}
