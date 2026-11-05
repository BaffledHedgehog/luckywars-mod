package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** A distinct native token, with the source prototype decoded once during registry bootstrap. */
public final class LbcBadAppleItem {
    private static final String DEFINITION =
            "/data/lbc/loot_table/item_defs/lbc/badapple1__53d62d9b2e05.json";
    public static final Item TOKEN = registerAuthored();

    private LbcBadAppleItem() { }
    public static void register() { }

    private static Item registerAuthored() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:bad_apple_imba"));
        var properties=LbcUtilityItemPrototypes.properties("skywars2:bad_apple_imba",DEFINITION,Items.REPEATING_COMMAND_BLOCK,64,java.util.Set.of(),true)
                .overrideDescription("block.minecraft.repeating_command_block");
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(properties,Items.REPEATING_COMMAND_BLOCK,true));
    }


}
