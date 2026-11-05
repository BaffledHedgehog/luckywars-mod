package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;

/** Original tool mechanics and source presentation; loot resolves the dynamic enchantment registry. */
public final class LbcImbaToolItems {
    public static final Item SHOVEL=register("imba_shovel","golden_shovel__48f876d02a40",Items.NETHERITE_SHOVEL,true);
    public static final Item HOE=register("imba_hoe","golden_hoe__0454f6dc573b",Items.NETHERITE_HOE,false);
    private LbcImbaToolItems(){}
    public static void register(){}
    private static Item register(String name,String definition,Item carrier,boolean shovel){
        String id="skywars2:"+name;
        var properties=LbcUtilityItemPrototypes.properties(id,"/data/lbc/loot_table/item_defs/minecraft/"+definition+".json",carrier,1,Set.of("minecraft:enchantments"));
        if(shovel)properties.shovel(ToolMaterial.NETHERITE,1.5f,-3);
        else properties.hoe(ToolMaterial.NETHERITE,-4,0);
        return Registry.register(BuiltInRegistries.ITEM,Identifier.parse(id),new SimplePolymerItem(properties.fireResistant(),carrier,true));
    }
}
