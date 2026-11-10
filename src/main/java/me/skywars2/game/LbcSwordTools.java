package me.skywars2.game;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Legacy sword identity supplies native mining rules without replacing authored combat stats. */
public final class LbcSwordTools {
    private LbcSwordTools(){}
    public static void repair(ItemStack stack){
        if(stack.isEmpty()||stack.has(DataComponents.TOOL)||LbcTmtAbilities.active(stack))return;
        var custom=stack.get(DataComponents.CUSTOM_DATA);if(custom==null)return;
        var tag=custom.copyTag();
        if(!tag.getStringOr("type","").equals("sword")&&tag.getIntOr("sword",0)!=1&&tag.getIntOr("blazefire_sword",0)!=1
                &&tag.getIntOr("triple_golden_sword",0)!=1)return;
        if(Items.IRON_SWORD!=null){
            var tool=Items.IRON_SWORD.components().get(DataComponents.TOOL);
            if(tool!=null)stack.set(DataComponents.TOOL,tool);
        }
    }
}
