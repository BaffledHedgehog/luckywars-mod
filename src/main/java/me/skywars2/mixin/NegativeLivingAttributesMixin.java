package me.skywars2.mixin;

import me.skywars2.game.VanillaExpansionRules;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.skywars2.game.NegativeAttributeEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class NegativeLivingAttributesMixin {
    @WrapOperation(method="causeFallDamage",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;calculateFallDamage(DF)I"))
    private int skywars2$negativeFallHeals(LivingEntity entity,double distance,float modifier,Operation<Integer> original){
        if(!VanillaExpansionRules.enabled()){return original.call(entity,distance,modifier);}
        return entity.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER)<0
                ?NegativeAttributeEffects.fallDamage(entity,distance,modifier):original.call(entity,distance,modifier);
    }
    @Inject(method="decreaseAirSupply",at=@At("HEAD"),cancellable=true)
    private void skywars2$negativeOxygenConsumesFaster(int current,CallbackInfoReturnable<Integer> cir){
        if(!VanillaExpansionRules.enabled()){return;}
        var entity=(LivingEntity)(Object)this;var attribute=entity.getAttribute(Attributes.OXYGEN_BONUS);
        if(attribute!=null&&attribute.getValue()<0)cir.setReturnValue(NegativeAttributeEffects.oxygen(entity,current,attribute.getValue()));
    }
}
