package me.skywars2.game;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Per-event views of the same inventory item; no registry work or background inventory polling. */
final class LbcTmtInventory {
    private LbcTmtInventory(){}
    /** Current type bits0..8 and legacy bits16..24; source consumers keep their own effect order. */
    static int mysticalTypes(CompoundTag data,int value){
        if(data.getIntOr("tmt_item",0)!=1)return bits(data,value);
        int result=0;
        for(var ability:LbcTmtAbilities.read(data))result|=bits(ability.data(),value);
        return result;
    }
    private static int bits(CompoundTag data,int value){
        int type=data.getIntOr("type",0);if(type<1||type>8)return 0;
        return (data.getIntOr("mystical",0)==value?1<<type:0)|(data.getIntOr("mystical_old",0)==value?1<<(type+16):0);
    }
    private static int bits(net.minecraft.world.item.component.CustomData data,int value){
        var reader=(ItemData.IntReader)(Object)data;int type=reader.skywars2$readInt("type",0);if(type<1||type>8)return 0;
        return (reader.skywars2$readInt("mystical",0)==value?1<<type:0)|(reader.skywars2$readInt("mystical_old",0)==value?1<<(type+16):0);
    }
    static boolean string(ItemStack stack,String key,String value){
        if(ItemData.stringValue(stack,key,"").equals(value))return true;
        if(!LbcTmtItems.matches(stack))return false;
        for(var ability:LbcTmtAbilities.read(stack))if(((ItemData.IntReader)(Object)ability.data()).skywars2$readString(key,"").equals(value))return true;
        return false;
    }
    static boolean matches(ItemStack stack,CompoundTag pattern){
        var data=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if(data!=null&&data.matchedBy(pattern))return true;
        if(!LbcTmtItems.matches(stack))return false;
        for(var ability:LbcTmtAbilities.read(stack))if(ability.data().matchedBy(pattern))return true;
        return false;
    }
    static boolean upgraded(ItemStack stack,String flag){
        if(!LbcTmtItems.matches(stack))return false;
        for(var ability:LbcTmtAbilities.read(stack)){
            var data=(ItemData.IntReader)(Object)ability.data();
            if(data.skywars2$readInt(flag,0)==1&&data.skywars2$readInt("upgraded",0)==1)return true;
        }
        return false;
    }
    static java.util.Set<String> strings(ItemStack stack,String key){
        if(!LbcTmtItems.matches(stack))return java.util.Set.of(ItemData.stringValue(stack,key,""));
        var result=new java.util.HashSet<String>();
        for(var ability:LbcTmtAbilities.read(stack)){
            var value=((ItemData.IntReader)(Object)ability.data()).skywars2$readString(key,"");
            if(!value.isEmpty())result.add(value);
        }
        return result;
    }
}
