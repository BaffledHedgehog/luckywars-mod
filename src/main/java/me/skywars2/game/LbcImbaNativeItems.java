package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import java.util.Set;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;

/** Authored variants inherit the real vanilla component set and projectile factories. */
public final class LbcImbaNativeItems {
    public static final Item INFINITE_ARROW=register("infinite_arrow",new InfiniteArrow(properties("infinite_arrow","/data/lbc/loot_table/imba/infinite_arrow.json",Items.ARROW,64,Set.of("minecraft:enchantments"))));
    public static final Item HOMING_ARROW=register("imba_homing_arrow",new HomingArrow(properties("imba_homing_arrow","/data/lbc/loot_table/item_defs/minecraft/tipped_arrow__61769fd8e715.json",Items.TIPPED_ARROW,64,Set.of())));
    public static final Item ELYTRA=register("imba_elytra",new SimplePolymerItem(properties("imba_elytra","/data/lbc/loot_table/item_defs/minecraft/elytra__38fe0e2c373a.json",Items.ELYTRA,1,Set.of()),Items.ELYTRA,true));
    public static final Item FIREWORK=register("imba_firework",new Rocket(properties("imba_firework","/data/lbc/loot_table/item_defs/minecraft/firework_rocket__e2f07060e0e4.json",Items.FIREWORK_ROCKET,64,Set.of())));
    private LbcImbaNativeItems(){}
    public static void register(){LbcImbaNativeVariants.register();}
    private static Item register(String name,Item item){return Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:"+name),item);}
    private static Item.Properties properties(String name,String resource,Item carrier,int maximum,Set<String> dynamic){
        return LbcUtilityItemPrototypes.properties("skywars2:"+name,resource,carrier,maximum,dynamic,true);
    }
    private static final class InfiniteArrow extends ArrowItem implements PolymerItem {
        InfiniteArrow(Properties properties){super(properties);}
        public Item getPolymerItem(ItemStack stack,PacketContext context){return Items.ARROW;}
    }
    private static final class HomingArrow extends TippedArrowItem implements PolymerItem {
        HomingArrow(Properties properties){super(properties);}
        public Item getPolymerItem(ItemStack stack,PacketContext context){return Items.TIPPED_ARROW;}
    }
    private static final class Rocket extends FireworkRocketItem implements PolymerItem {
        Rocket(Properties properties){super(properties);}
        public Item getPolymerItem(ItemStack stack,PacketContext context){return Items.FIREWORK_ROCKET;}
    }
}
