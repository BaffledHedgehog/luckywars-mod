package me.skywars2.game;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/** Scalar reads borrow no mutable NBT and allocate no whole-component copies. */
public final class ItemData {
    private ItemData(){}
    public interface IntReader { int skywars2$readInt(String key,int fallback); boolean skywars2$contains(String key); String skywars2$readString(String key,String fallback); }
    public static boolean contains(ItemStack stack,String key){
        if(stack.isEmpty())return false;
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return data!=null&&((IntReader)(Object)data).skywars2$contains(key);
    }
    public static int intValue(ItemStack stack,String key,int fallback){
        if(stack.isEmpty())return fallback;
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return data==null?fallback:((IntReader)(Object)data).skywars2$readInt(key,fallback);
    }
    public static String stringValue(ItemStack stack,String key,String fallback){
        if(stack.isEmpty())return fallback;
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return data==null?fallback:((IntReader)(Object)data).skywars2$readString(key,fallback);
    }
}
