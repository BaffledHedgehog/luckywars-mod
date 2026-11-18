package me.skywars2.game;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.Set;

/** Authored sword identity adds missing native properties without replacing its combat stats. */
public final class LbcSwordTools {
    private static final Set<Identifier> CARROT_WEAR_MODELS = Set.of(
            Identifier.parse("lbc:anal_whip"), Identifier.parse("lbc:cyberkatana"),
            Identifier.parse("lbc:lightsaber_empty"), Identifier.parse("lbc:piston_sword"),
            Identifier.parse("lbc:shit_katana"), Identifier.parse("lbc:techrunner_katana"),
            Identifier.parse("lbc:terra_razorpine"), Identifier.parse("lbc:vex_sword"),
            Identifier.parse("lbc:vexsword"));

    private LbcSwordTools(){}
    public static boolean sword(ItemStack stack){
        return !stack.isEmpty()&&(ItemData.stringValue(stack,"type","").equals("sword")||ItemData.intValue(stack,"sword",0)==1
                ||ItemData.intValue(stack,"blazefire_sword",0)==1||ItemData.intValue(stack,"triple_golden_sword",0)==1);
    }
    public static void repair(ItemStack stack){
        boolean sword=sword(stack);
        var model=stack.get(DataComponents.ITEM_MODEL);
        boolean carrierWear=model!=null&&stack.getMaxDamage()==25&&CARROT_WEAR_MODELS.contains(model);
        if((sword||carrierWear)&&!LbcTmtAbilities.active(stack)){
            // The old carrier's 25 boost uses are incidental to these weapons.
            if(carrierWear){
                stack.remove(DataComponents.MAX_DAMAGE);
                stack.remove(DataComponents.DAMAGE);
            }
            if(sword&&Items.IRON_SWORD!=null){
                supply(stack,DataComponents.TOOL);supply(stack,DataComponents.ENCHANTABLE);supply(stack,DataComponents.REPAIRABLE);
                supply(stack,DataComponents.WEAPON);supply(stack,DataComponents.ENCHANTMENTS);
            }
        }
        EnchantmentRules.prepare(stack);
    }
    private static <T> void supply(ItemStack stack,DataComponentType<T> type){
        if(!stack.has(type)){var value=Items.IRON_SWORD.components().get(type);if(value!=null)stack.set(type,value);}
    }
}
