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
    public static final Item HOMING_ARROW=register("imba_homing_arrow",new TypedArrow(properties("imba_homing_arrow","/data/lbc/loot_table/item_defs/minecraft/tipped_arrow__61769fd8e715.json",Items.TIPPED_ARROW,64,Set.of())));
    public static final Item ELYTRA=register("imba_elytra",new SimplePolymerItem(properties("imba_elytra","/data/lbc/loot_table/item_defs/minecraft/elytra__38fe0e2c373a.json",Items.ELYTRA,1,Set.of()),Items.ELYTRA,true));
    public static final Item FIREWORK=register("imba_firework",new Rocket(properties("imba_firework","/data/lbc/loot_table/item_defs/minecraft/firework_rocket__e2f07060e0e4.json",Items.FIREWORK_ROCKET,64,Set.of())));
    public static final Item STRONG_ARROW=register("imba_strong_arrow",new TypedArrow(LbcUtilityItemPrototypes.deferredProperties("skywars2:imba_strong_arrow","/data/lbc/loot_table/item_defs/minecraft/tipped_arrow__6e933ca5dfdd.json",Items.TIPPED_ARROW,64)));
    public static final Item WEAK_ARROW=register("imba_weak_arrow",new TypedArrow(LbcUtilityItemPrototypes.deferredProperties("skywars2:imba_weak_arrow","/data/lbc/loot_table/item_defs/minecraft/tipped_arrow__08d47b0cbea1.json",Items.TIPPED_ARROW,64)));
    public static final Item POISON_ARROW=register("imba_poison_arrow",new TypedArrow(LbcUtilityItemPrototypes.deferredProperties("skywars2:imba_poison_arrow","/data/lbc/loot_table/item_defs/minecraft/tipped_arrow__aeb24809f14b.json",Items.TIPPED_ARROW,64)));
    public static final Item REACTIVE_ARROW=register("imba_reactive_arrow",new TypedArrow(LbcUtilityItemPrototypes.deferredProperties("skywars2:imba_reactive_arrow","/data/lbc/loot_table/item_defs/minecraft/tipped_arrow__542a1f35e483.json",Items.TIPPED_ARROW,64)));
    public static final Item EXPLOSIVE_ARROW=register("imba_explosive_arrow",new TypedArrow(LbcUtilityItemPrototypes.deferredProperties("skywars2:imba_explosive_arrow","/data/lbc/loot_table/item_defs/minecraft/tipped_arrow__92b1f1fe7523.json",Items.TIPPED_ARROW,64)));
    public static final Item EVOKER_ARROW=register("imba_evoker_arrow",new TypedArrow(LbcUtilityItemPrototypes.deferredProperties("skywars2:imba_evoker_arrow","/data/lbc/loot_table/item_defs/minecraft/tipped_arrow__4ffdef18a9cb.json",Items.TIPPED_ARROW,64)));
    public static final Item PARALYZING_ARROW=register("imba_paralyzing_arrow",new TypedArrow(LbcUtilityItemPrototypes.deferredProperties("skywars2:imba_paralyzing_arrow","/data/lbc/loot_table/item_defs/minecraft/tipped_arrow__0cc65f50a29e.json",Items.TIPPED_ARROW,64)));
    public static final Item ELECTRIC_ARROW=register("imba_electric_arrow",new TypedArrow(LbcUtilityItemPrototypes.deferredProperties("skywars2:imba_electric_arrow","/data/lbc/loot_table/item_defs/minecraft/tipped_arrow__56437910eed7.json",Items.TIPPED_ARROW,64)));
    private LbcImbaNativeItems(){}
    public static void register(){LbcImbaNativeVariants.register();}
    static java.util.List<Item> specialArrows(){return java.util.List.of(STRONG_ARROW,WEAK_ARROW,POISON_ARROW,REACTIVE_ARROW,EXPLOSIVE_ARROW,EVOKER_ARROW,PARALYZING_ARROW,ELECTRIC_ARROW);}
    private static Item register(String name,Item item){return Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:"+name),item);}
    private static Item.Properties properties(String name,String resource,Item carrier,int maximum,Set<String> dynamic){
        return LbcUtilityItemPrototypes.properties("skywars2:"+name,resource,carrier,maximum,dynamic,true);
    }
    private static final class InfiniteArrow extends ArrowItem implements PolymerItem {
        InfiniteArrow(Properties properties){super(properties);}
        public Item getPolymerItem(ItemStack stack,PacketContext context){return Items.ARROW;}
    }
    private static final class TypedArrow extends TippedArrowItem implements PolymerItem {
        TypedArrow(Properties properties){super(properties);}
        @Override public ItemStack getDefaultInstance(){return new ItemStack(this);}
        public Item getPolymerItem(ItemStack stack,PacketContext context){return Items.TIPPED_ARROW;}
    }
    private static final class Rocket extends FireworkRocketItem implements PolymerItem {
        Rocket(Properties properties){super(properties);}
        public Item getPolymerItem(ItemStack stack,PacketContext context){return Items.FIREWORK_ROCKET;}
    }
}
