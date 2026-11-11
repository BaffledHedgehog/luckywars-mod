package me.skywars2.game;

import java.util.function.IntConsumer;
import net.minecraft.world.item.ItemStack;

/** Bounded private selectors for native inventory/recipe owners; no independent reward logic. */
final class LbcTmtIntake {
    private LbcTmtIntake(){}
    static boolean has(ItemStack stack,String flag){
        if(!LbcTmtItems.matches(stack))return false;
        for(var property:LbcTmtTriggers.effective(stack))
            if(((ItemData.IntReader)(Object)property.data()).skywars2$readInt(flag,0)==1)return true;
        return false;
    }
    static boolean touhou(ItemStack stack,IntConsumer accept){
        boolean found=false;
        for(var property:LbcTmtTriggers.effective(stack)){
            int type=((ItemData.IntReader)(Object)property.data()).skywars2$readInt("touhou_item",0);
            if(type>=1&&type<=10){accept.accept(type);found=true;}
        }
        return found;
    }
    static int wires(ItemStack stack){
        return (has(stack,"nexus_wire_red")?1:0)|(has(stack,"nexus_wire_black")?2:0)|(has(stack,"nexus_wire_blue")?4:0);
    }
}
