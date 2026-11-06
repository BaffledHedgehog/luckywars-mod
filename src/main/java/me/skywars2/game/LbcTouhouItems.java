package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;

/** Finite source identities; inventory events and shared bomb/clock input own every ability. */
public final class LbcTouhouItems {
    public enum Kind {
        TOUHOU_COLLECTIBLE_1("touhou_collectible_1","ported/touhou_collectible_1",Items.REPEATING_COMMAND_BLOCK),
        TOUHOU_COLLECTIBLE_2("touhou_collectible_2","ported/touhou_collectible_2",Items.REPEATING_COMMAND_BLOCK),
        TOUHOU_COLLECTIBLE_3("touhou_collectible_3","ported/touhou_collectible_3",Items.REPEATING_COMMAND_BLOCK),
        TOUHOU_COLLECTIBLE_4("touhou_collectible_4","ported/touhou_collectible_4",Items.REPEATING_COMMAND_BLOCK),
        TOUHOU_COLLECTIBLE_5("touhou_collectible_5","ported/touhou_collectible_5",Items.REPEATING_COMMAND_BLOCK),
        TOUHOU_COLLECTIBLE_6("touhou_collectible_6","ported/touhou_collectible_6",Items.REPEATING_COMMAND_BLOCK),
        TOUHOU_COLLECTIBLE_7("touhou_collectible_7","ported/touhou_collectible_7",Items.REPEATING_COMMAND_BLOCK),
        TOUHOU_COLLECTIBLE_8("touhou_collectible_8","ported/touhou_collectible_8",Items.REPEATING_COMMAND_BLOCK),
        TOUHOU_COLLECTIBLE_9("touhou_collectible_9","ported/touhou_collectible_9",Items.REPEATING_COMMAND_BLOCK),
        TOUHOU_COLLECTIBLE_10("touhou_collectible_10","ported/touhou_collectible_10",Items.REPEATING_COMMAND_BLOCK),
        TOUHOU_BOMB_ITEM("touhou_bomb_item","bomb_item",Items.SNOWBALL),
        TOUHOU_TIME_STOPPER("touhou_time_stopper","time_stopper",Items.SNOWBALL);
        public final String id,definition;
        public final Item carrier,item;
        Kind(String id,String definition,Item carrier){
            this.id=id;this.definition="/data/lbc/loot_table/"+definition+".json";this.carrier=carrier;
            var properties=LbcUtilityItemPrototypes.deferredProperties("skywars2:"+id,this.definition,carrier,carrier==Items.SNOWBALL?16:64);
            item=Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:"+id),
                    carrier==Items.SNOWBALL?new Consumable(properties):new SimplePolymerItem(properties,carrier,true));
        }
    }
    private LbcTouhouItems(){}
    public static void register(){Kind.values();}
    private static final class Consumable extends SimplePolymerItem {
        Consumable(Properties properties){super(properties,Items.SNOWBALL,true);}
        @Override public net.minecraft.world.InteractionResult use(net.minecraft.world.level.Level level,
                net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand){
            var game=me.skywars2.SkyWars2.game();
            return game!=null&&player instanceof net.minecraft.server.level.ServerPlayer owner
                    &&game.items().touhouCollectibles().use(owner,hand)
                    ?net.minecraft.world.InteractionResult.SUCCESS:net.minecraft.world.InteractionResult.FAIL;
        }
    }
}
