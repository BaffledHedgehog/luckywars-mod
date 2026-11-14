package me.skywars2.game;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Authored sword identity adds missing native properties without replacing its combat stats. */
public final class LbcSwordTools {
    private LbcSwordTools(){}
    public static boolean sword(ItemStack stack){
        return !stack.isEmpty()&&(ItemData.stringValue(stack,"type","").equals("sword")||ItemData.intValue(stack,"sword",0)==1
                ||ItemData.intValue(stack,"blazefire_sword",0)==1||ItemData.intValue(stack,"triple_golden_sword",0)==1);
    }
    public static void repair(ItemStack stack){
        if(!sword(stack)||LbcTmtAbilities.active(stack)||Items.IRON_SWORD==null)return;
        supply(stack,DataComponents.TOOL);supply(stack,DataComponents.ENCHANTABLE);supply(stack,DataComponents.REPAIRABLE);
        supply(stack,DataComponents.WEAPON);supply(stack,DataComponents.ENCHANTMENTS);
    }
    private static <T> void supply(ItemStack stack,DataComponentType<T> type){
        if(!stack.has(type)){var value=Items.IRON_SWORD.components().get(type);if(value!=null)stack.set(type,value);}
    }
}
