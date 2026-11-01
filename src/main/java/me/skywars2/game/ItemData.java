package me.skywars2.game;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/** Scalar reads borrow no mutable NBT and allocate no whole-component copies. */
public final class ItemData {
    private ItemData(){}
    public interface IntReader { int skywars2$readInt(String key,int fallback); }
    public static int intValue(ItemStack stack,String key,int fallback){
        if(stack.isEmpty())return fallback;
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return data==null?fallback:((IntReader)(Object)data).skywars2$readInt(key,fallback);
    }
}
