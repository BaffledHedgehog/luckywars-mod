package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.level.Level;

/** The source snowball is a launch token; the authored melon display owns flight. */
public final class LbcKapustaItem extends SnowballItem implements PolymerItem {
    public static final Item ITEM=Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:kapusta"),
            new LbcKapustaItem(LbcUtilityItemPrototypes.properties("skywars2:kapusta",
                    "/data/lbc/loot_table/item_defs/lbc/kapusta__e3a9b9e791f5.json",Items.SNOWBALL,16)));
    public static final Item JACK=Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:halloween_jack"),
            new LbcKapustaItem(LbcUtilityItemPrototypes.properties("skywars2:halloween_jack",
                    "/data/lbc/loot_table/item_defs/minecraft/jack_o_lantern__cc18e55635ba.json",Items.SNOWBALL,16)));
    private LbcKapustaItem(Properties properties){super(properties);}
    public static void register(){}
    @Override public Item getPolymerItem(ItemStack stack,PacketContext context){return Items.SNOWBALL;}
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        var game=me.skywars2.SkyWars2.game();
        return game!=null&&player instanceof ServerPlayer owner&&game.items().kapusta().use(owner,hand)
                ?InteractionResult.SUCCESS:InteractionResult.FAIL;
    }
}
