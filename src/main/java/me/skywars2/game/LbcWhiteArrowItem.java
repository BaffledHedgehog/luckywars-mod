package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Exact God Dice6 white-arrow token; source ray admission and costs have one native owner. */
public final class LbcWhiteArrowItem {
    public static final Item TOKEN=Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:white_arrow"),new Arrow(LbcUtilityItemPrototypes.properties("skywars2:white_arrow","/data/lbc/loot_table/white_arrow.json",Items.CARROT_ON_A_STICK,1).durability(25)));
    private LbcWhiteArrowItem(){}
    public static void register(){}
    private static final class Arrow extends SimplePolymerItem {
        Arrow(Properties properties){super(properties,Items.CARROT_ON_A_STICK,true);}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){var game=me.skywars2.SkyWars2.game();return game!=null&&player instanceof ServerPlayer owner&&game.items().whiteArrows().use(owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;}
    }
    public static ItemStack normalize(ItemStack source){if(source.is(TOKEN)||!LbcWhiteArrows.matches(source))return source;var copy=source.transmuteCopy(TOKEN);copy.applyComponents(source.getComponents());return copy;}
}
