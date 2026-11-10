package me.skywars2.game;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** Extends armor beyond40 using the user's remaining-damage curve and handles singular toughness. */
public final class ArmorCalculation {
    private ArmorCalculation(){}
    public static boolean needsCustom(float damage,float armor,float toughness){
        return armor<0||toughness<0||armor>40||!Float.isFinite(damage)||!Float.isFinite(armor)||!Float.isFinite(toughness)||2+toughness/4==0;
    }
    public static float afterAbsorb(LivingEntity victim,float damage,DamageSource source,float armor,float toughness){
        damage=finite(damage,0);armor=finite(armor,0);toughness=finite(toughness,0);
        // The first twenty negative points are a neutral buffer for either defense attribute.
        armor=defense(armor);toughness=defense(toughness);
        float baselineArmor=Math.min(armor,40),denominator=2+toughness/4;
        float penetration=denominator==0?(damage==0?0:Math.copySign(Float.POSITIVE_INFINITY,damage)):damage/denominator;
        float realArmor=Math.max(baselineArmor*.2f,Math.min(20,baselineArmor-penetration));
        float fraction=realArmor/25;
        var weapon=source.getWeaponItem();
        if(weapon!=null&&victim.level() instanceof ServerLevel level){
            float modified=EnchantmentHelper.modifyArmorEffectiveness(level,weapon,victim,source,fraction);
            if(!Float.isNaN(modified))fraction=Math.max(0,Math.min(1,modified));
        }
        double result=(double)damage*(1-(double)fraction);
        if(armor>40)result*=40d/armor;
        // Finite negative incoming damage keeps its sign; defense alone cannot turn positive damage into healing.
        if(Double.isNaN(result))return 0;
        return (float)Math.max(-Float.MAX_VALUE,Math.min(Float.MAX_VALUE,result));
    }
    static float defense(float value){return value<0?Math.min(0,value+20):value;}
    private static float finite(float value,float fallback){
        return Float.isNaN(value)?fallback:value==Float.POSITIVE_INFINITY?Float.MAX_VALUE:value==Float.NEGATIVE_INFINITY?-Float.MAX_VALUE:value;
    }
}
