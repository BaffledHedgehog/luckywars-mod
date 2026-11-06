package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;

/** Canonical identities share the existing wand, passive, compass and vanilla death-protection authorities. */
public final class LbcSpecialArtifactItems {
    public enum Kind {
        ADMIN_ZAIKA("admin_zaika","item_defs/lbc/zaika__e7f523430e71",Items.CARROT_ON_A_STICK),
        INDOLENCE_CLAW("indolence_claw","ported/indolence_claw",Items.CARROT_ON_A_STICK),
        DEATH_COMPASS("death_compass","item_defs/minecraft/recovery_compass__45a381eb1c82",Items.RECOVERY_COMPASS),
        DEVIL_TOTEM("devil_totem","item_defs/lbc/deviltotem__76f01de85421",Items.TOTEM_OF_UNDYING),
        INFINITE_TOTEM("infinite_totem","ported/infinite_totem",Items.TOTEM_OF_UNDYING),
        CANNED_FOOD("canned_food","item_defs/lbc/konserva__38feb5476945",Items.REPEATING_COMMAND_BLOCK);
        public final String id,definition;
        public final Item carrier,item;
        Kind(String id,String definition,Item carrier){
            this.id=id;this.definition="/data/lbc/loot_table/"+definition+".json";this.carrier=carrier;
            var properties=LbcUtilityItemPrototypes.deferredProperties("skywars2:"+id,this.definition,carrier,carrier==Items.CARROT_ON_A_STICK||carrier==Items.TOTEM_OF_UNDYING?1:64);
            item=Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:"+id),
                    carrier==Items.CARROT_ON_A_STICK?LbcWands.nativeTool(properties,carrier):new SimplePolymerItem(properties,carrier,true));
        }
    }
    private LbcSpecialArtifactItems(){}
    public static void register(){Kind.values();}
    static java.util.List<Item> items(){return java.util.Arrays.stream(Kind.values()).map(k->k.item).toList();}
}
