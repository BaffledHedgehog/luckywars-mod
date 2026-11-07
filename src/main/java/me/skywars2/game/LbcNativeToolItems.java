package me.skywars2.game;

import eu.pb4.polymer.core.api.item.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;

/** Canonical tools retain native interactions and existing ability/recipe authorities. */
public final class LbcNativeToolItems {
    public enum Kind {
        EFFICIENT_SHOVEL("efficient_shovel","ported/efficient_shovel",Items.IRON_SHOVEL),
        RANDOM_ENCHANTED_SHOVEL("random_enchanted_shovel","ported/random_enchanted_shovel",Items.IRON_SHOVEL),
        RANDOM_ENCHANTED_AXE("random_enchanted_axe","item_defs/randchanted_axe__8b8721ff55d6",Items.IRON_AXE),
        RANDOM_ENCHANTED_PICKAXE("random_enchanted_pickaxe","item_defs/randchanted_pickaxe__bc03ee7cf779",Items.IRON_PICKAXE),
        ENHANCED_FLINT("enhanced_flint","ported/enhanced_flint",Items.FLINT_AND_STEEL),
        HAMMER("hammer","ported/hammer",Items.IRON_AXE),
        MORTIS("mortis","ported/mortis",Items.NETHERITE_SHOVEL),
        POLEAXE("poleaxe","ported/poleaxe",Items.NETHERITE_AXE),
        OP_HOE("op_hoe","op_hoe",Items.NETHERITE_HOE),
        SICKLE("sickle","ported/sickle",Items.IRON_HOE),
        SILK_PICKAXE("silk_pickaxe","item_defs/minecraft/diamond_pickaxe__b730b9a4a36b",Items.DIAMOND_PICKAXE),
        TOMAHAWK("tomahawk","ported/tomahawk",Items.GOLDEN_AXE),
        KEY("key","item_defs/key",Items.STICK),
        LUCK_KEY("key_luck","item_defs/key_luck",Items.STICK);
        public final String id,definition;
        public final Item carrier,item;
        Kind(String id,String definition,Item carrier){
            this.id=id;this.definition="/data/lbc/loot_table/"+definition+".json";this.carrier=carrier;
            var properties=LbcUtilityItemPrototypes.deferredProperties("skywars2:"+id,this.definition,carrier,64);
            if(carrier==Items.STICK)properties.cookingFuel(net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders.COOKING_TIME_DRY_PLANTS);
            item=Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:"+id),
                    carrier==Items.FLINT_AND_STEEL?new NativeFlint(properties):new SimplePolymerItem(properties,carrier,true));
        }
        public boolean random(){return id.startsWith("random_enchanted_");}
    }
    private LbcNativeToolItems(){}
    public static void register(){Kind.values();}
    static java.util.List<Item> items(){return java.util.Arrays.stream(Kind.values()).map(k->k.item).toList();}
    private static final class NativeFlint extends FlintAndSteelItem implements PolymerItem {
        NativeFlint(Properties properties){super(properties);}
        @Override public Item getPolymerItem(ItemStack stack,net.fabricmc.fabric.api.networking.v1.context.PacketContext context){return Items.FLINT_AND_STEEL;}
    }
}
