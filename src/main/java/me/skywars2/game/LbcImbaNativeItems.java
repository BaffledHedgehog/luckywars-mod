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
    public static final Item QUIVER_STRONG_ARROW=register("strong_arrow",new TypedArrow(LbcUtilityItemPrototypes.deferredProperties("skywars2:strong_arrow","/data/lbc/loot_table/item_defs/minecraft/tipped_arrow__f6855c681848/variant_2db9ec05ff9f.json",Items.TIPPED_ARROW,64)));
    public static final Item QUIVER_WEAK_ARROW=register("weak_arrow",new TypedArrow(LbcUtilityItemPrototypes.deferredProperties("skywars2:weak_arrow","/data/lbc/loot_table/item_defs/minecraft/tipped_arrow__a83b315ade86/variant_16be374fe64a.json",Items.TIPPED_ARROW,64)));
    public static final Item QUIVER_POISON_ARROW=register("poison_arrow",new TypedArrow(LbcUtilityItemPrototypes.deferredProperties("skywars2:poison_arrow","/data/lbc/loot_table/item_defs/minecraft/tipped_arrow__129290638d78/variant_1df5252a74b4.json",Items.TIPPED_ARROW,64)));
    public static final Item QUIVER_REACTIVE_ARROW=register("reactive_arrow",new TypedArrow(LbcUtilityItemPrototypes.deferredProperties("skywars2:reactive_arrow","/data/lbc/loot_table/item_defs/minecraft/tipped_arrow__2e91b33c6339/variant_4fe32bdc2571.json",Items.TIPPED_ARROW,64)));
    public static final Item QUIVER_EXPLOSIVE_ARROW=register("explosive_arrow",new TypedArrow(LbcUtilityItemPrototypes.deferredProperties("skywars2:explosive_arrow","/data/lbc/loot_table/item_defs/minecraft/tipped_arrow__797b3af909ce/variant_d60afff3fc92.json",Items.TIPPED_ARROW,64)));
    public static final Item QUIVER_EVOKER_ARROW=register("evoker_arrow",new TypedArrow(LbcUtilityItemPrototypes.deferredProperties("skywars2:evoker_arrow","/data/lbc/loot_table/item_defs/minecraft/tipped_arrow__ba59a43bb29d/variant_5d58e57bac30.json",Items.TIPPED_ARROW,64)));
    public static final Item QUIVER_PARALYZING_ARROW=register("paralyzing_arrow",new TypedArrow(LbcUtilityItemPrototypes.deferredProperties("skywars2:paralyzing_arrow","/data/lbc/loot_table/item_defs/minecraft/tipped_arrow__c420ac48aee2/variant_8a94d060a1ae.json",Items.TIPPED_ARROW,64)));
    public static final Item QUIVER_ELECTRIC_ARROW=register("electric_arrow",new TypedArrow(LbcUtilityItemPrototypes.deferredProperties("skywars2:electric_arrow","/data/lbc/loot_table/item_defs/minecraft/tipped_arrow__93d954dbcaf5/variant_dd8e535af39f.json",Items.TIPPED_ARROW,64)));
    public static final Item QUIVER_LIGHTSHOOT_ARROW=register("lightshoot_arrow",new TypedArrow(LbcUtilityItemPrototypes.deferredProperties("skywars2:lightshoot_arrow","/data/lbc/loot_table/item_defs/minecraft/tipped_arrow__62efeb3693af.json",Items.TIPPED_ARROW,64)));
    public static final Item QUIVER_KIUASKIVI_ARROW=register("kiuaskivi_arrow",new TypedArrow(LbcUtilityItemPrototypes.deferredProperties("skywars2:kiuaskivi_arrow","/data/lbc/loot_table/item_defs/minecraft/tipped_arrow__9355d6686bac.json",Items.TIPPED_ARROW,64)));
    public static final Item QUIVER_TRAPPING_ARROW=register("trapping_arrow",new TypedArrow(LbcUtilityItemPrototypes.deferredProperties("skywars2:trapping_arrow","/data/lbc/loot_table/item_defs/minecraft/tipped_arrow__4d9d3a4c2b22.json",Items.TIPPED_ARROW,64)));
    private LbcImbaNativeItems(){}
    public static void register(){LbcImbaNativeVariants.register();}
    static java.util.List<Item> specialArrows(){return java.util.List.of(STRONG_ARROW,WEAK_ARROW,POISON_ARROW,REACTIVE_ARROW,EXPLOSIVE_ARROW,EVOKER_ARROW,PARALYZING_ARROW,ELECTRIC_ARROW);}
    static java.util.List<Item> quiverArrows(){return java.util.List.of(QUIVER_STRONG_ARROW,QUIVER_WEAK_ARROW,QUIVER_POISON_ARROW,QUIVER_REACTIVE_ARROW,QUIVER_EXPLOSIVE_ARROW,QUIVER_EVOKER_ARROW,QUIVER_PARALYZING_ARROW,QUIVER_ELECTRIC_ARROW,QUIVER_LIGHTSHOOT_ARROW,QUIVER_KIUASKIVI_ARROW,QUIVER_TRAPPING_ARROW);}
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
