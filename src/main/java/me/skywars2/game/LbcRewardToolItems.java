package me.skywars2.game;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Canonical source prototypes use the existing native wand input and shared tool rays. */
public final class LbcRewardToolItems {
    public enum Kind {
        SHADOW_GLOVE("shadow_glove","shadowglove"),
        TOOTHBRUSH("toothbrush","item_defs/lbc/shetka__5962f14d506e"),
        LIGHTNING("imba_lightning_bolt","item_defs/lbc/lightningegg__41cbd718ac0d");
        public final String id,definition;
        public final Item item;
        Kind(String id,String definition){
            this.id=id;this.definition="data/lbc/loot_table/"+definition+".json";
            var properties=LbcUtilityItemPrototypes.properties("skywars2:"+id,"/"+this.definition,
                    Items.CARROT_ON_A_STICK,1,java.util.Set.of(),true);
            item=Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:"+id),
                    LbcWands.nativeTool(properties,Items.CARROT_ON_A_STICK));
        }
    }
    private LbcRewardToolItems(){}
    public static void register(){Kind.values();}
}
