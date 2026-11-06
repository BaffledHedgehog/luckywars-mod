package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Canonical passive accessories share the existing inventory and combat authorities. */
public final class LbcAccessoryItems {
    public enum Kind {
        SAND_AMULET("sand_amulet","item_defs/lbc/gaara__e201b9517340"),
        SAVE_PLATFORM("save_platform","item_defs/lbc/saveplatform__b47589a7d4ec"),
        REGENERATION_BELT("regeneration_belt","item_defs/lbc/regenerationbelt__4491f5355c33"),
        MANA_BELT("mana_belt","item_defs/lbc/manaregenerationbelt__a4ad4f3da589"),
        MAGNET("magnet","ported/magnet"),
        DEVIL_STOMACH("devil_stomach","item_defs/lbc/kishki__b485b77f3ab1"),
        INFINITE_QUIVER("infinite_quiver","item_defs/lbc/quiver__f6b7b6ebfc8e");
        public final String id,definition;
        public final Item item;
        Kind(String id,String definition){
            this.id=id;this.definition="/data/lbc/loot_table/"+definition+".json";
            var properties=LbcUtilityItemPrototypes.deferredProperties("skywars2:"+id,this.definition,Items.REPEATING_COMMAND_BLOCK,64);
            item=Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:"+id),
                    new SimplePolymerItem(properties,Items.REPEATING_COMMAND_BLOCK,true));
        }
    }
    private LbcAccessoryItems(){}
    public static void register(){Kind.values();}
}
