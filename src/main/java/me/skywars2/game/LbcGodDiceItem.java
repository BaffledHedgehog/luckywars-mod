package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import me.skywars2.SkyWars2;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** One-click native input for the original snowball-carried die, without publishing a snowball. */
final class LbcGodDiceItem {
    static final Item TOKEN=Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:god_dice"),new SimplePolymerItem(
            LbcUtilityItemPrototypes.properties("skywars2:god_dice","/data/lbc/loot_table/god_dice.json",Items.SNOWBALL,16),Items.SNOWBALL,true){
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){var game=SkyWars2.game();
            return game!=null&&player instanceof ServerPlayer owner&&LbcGodDice.use(game,owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;}
    });
    private LbcGodDiceItem(){}
    static void register(){}
}
