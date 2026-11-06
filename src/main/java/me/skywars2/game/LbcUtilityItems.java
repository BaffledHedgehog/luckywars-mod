package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;

/** Source-backed utility identities; gameplay remains in the existing equipment/item owners. */
public final class LbcUtilityItems {
    public enum Kind {
        BUCKET_HEAD("bucket_head","imba/bucket_head",Items.LEATHER_HELMET),
        MOISEI_BOOTS("moisei_boots","ported/moisei_boots",Items.LEATHER_BOOTS),
        MALEVICH("malevich","malevich_rnd",Items.REPEATING_COMMAND_BLOCK);
        public final String id,definition;
        public final Item carrier,item;
        Kind(String id,String definition,Item carrier){
            this.id=id;this.definition="/data/lbc/loot_table/"+definition+".json";this.carrier=carrier;
            // A random attribute is rolled for each real Malevich output, never during item registration.
            var generated=id.equals("malevich")?Set.of("minecraft:attribute_modifiers"):Set.<String>of();
            var properties=LbcUtilityItemPrototypes.deferredProperties("skywars2:"+id,this.definition,carrier,64,generated);
            item=Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:"+id),new SimplePolymerItem(properties,carrier,true));
        }
    }
    private LbcUtilityItems(){}
    public static void register(){Kind.values();}
    static java.util.List<Item> items(){return java.util.Arrays.stream(Kind.values()).map(k->k.item).toList();}
}
