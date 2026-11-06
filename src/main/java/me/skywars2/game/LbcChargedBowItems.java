package me.skywars2.game;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;

/** Real native bows retain vanilla charge, ammunition, enchantment and durability handling. */
public final class LbcChargedBowItems {
    public static final Item RAILGUN=register("railgun"),SHISHGUN=register("shishgun");
    private LbcChargedBowItems(){}
    public static void register(){LbcChargedBows.register();}
    public static boolean railgun(ItemStack stack){return !stack.isEmpty()&&ItemData.intValue(stack,"railgun_bow",0)==1;}
    public static boolean shishgun(ItemStack stack){return !stack.isEmpty()&&ItemData.intValue(stack,"shish_gun",0)==1;}
    public static boolean matches(ItemStack stack){return railgun(stack)||shishgun(stack);}
    private static Item register(String id){
        var properties=LbcUtilityItemPrototypes.deferredProperties("skywars2:"+id,"/data/lbc/loot_table/item_defs/lbc/"+id+".json",Items.BOW,1);
        return Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:"+id),LbcProjectileWeaponItems.bow(properties));
    }
}
