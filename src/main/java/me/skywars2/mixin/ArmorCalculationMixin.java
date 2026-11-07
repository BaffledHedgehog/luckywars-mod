package me.skywars2.mixin;

import me.skywars2.game.ArmorCalculation;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CombatRules.class)
public abstract class ArmorCalculationMixin {
    @Inject(method="getDamageAfterAbsorb",at=@At("HEAD"),cancellable=true)
    private static void skywars2$extendedArmor(LivingEntity victim,float damage,DamageSource source,float armor,float toughness,CallbackInfoReturnable<Float> result){
        var game=me.skywars2.SkyWars2.game();
        if((game==null||!game.balanceLegacy())&&ArmorCalculation.needsCustom(damage,armor,toughness))result.setReturnValue(ArmorCalculation.afterAbsorb(victim,damage,source,armor,toughness));
    }
    @Inject(method="getDamageAfterAbsorb",at=@At("RETURN"),cancellable=true)
    private static void skywars2$finiteArmorResult(LivingEntity victim,float damage,DamageSource source,float armor,float toughness,CallbackInfoReturnable<Float> result){
        if(!Float.isFinite(result.getReturnValue()))result.setReturnValue(ArmorCalculation.afterAbsorb(victim,damage,source,armor,toughness));
    }
}
