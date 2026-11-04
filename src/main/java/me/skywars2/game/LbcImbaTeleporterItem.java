package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Direct native use consumes the accepted hand; no vanilla snowball or nearest-player lookup. */
public final class LbcImbaTeleporterItem {
    public static final Item TOKEN=Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:teleporter_imba"),
            new SimplePolymerItem(LbcUtilityItemPrototypes.properties("skywars2:teleporter_imba",
                    "/data/lbc/loot_table/item_defs/lbc/teleporter__aeae257af80d.json",Items.SNOWBALL,16),Items.SNOWBALL,true){
                @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
                    var game=me.skywars2.SkyWars2.game();
                    return game!=null&&player instanceof ServerPlayer owner&&game.items().teleportGadgets().use(owner,hand)
                            ?InteractionResult.SUCCESS:InteractionResult.FAIL;
                }
            });
    private LbcImbaTeleporterItem(){}
    public static void register(){}
}
