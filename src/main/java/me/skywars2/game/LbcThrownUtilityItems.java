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

/** Authored components remain in the canonical loot prototypes, including their translated lore. */
public final class LbcThrownUtilityItems {
    public static final Item RUCHKA=token("ruchka","/data/lbc/loot_table/item_defs/lbc/ruchka__bf1f679a17b1.json");
    public static final Item HONEW=token("honew","/data/lbc/loot_table/item_defs/minecraft/honey_block__e4ec8e496182.json");
    public static final Item THROWABLE_METAL_PIPE=token("throwable_metal_pipe","/data/lbc/loot_table/item_defs/lbc/metal_pipe__b63902a52716.json");
    private LbcThrownUtilityItems(){}
    public static void register(){}
    private static Item token(String id,String resource){
        return Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:"+id),
                new NativeSnowball(LbcUtilityItemPrototypes.properties("skywars2:"+id,resource,Items.SNOWBALL,16)));
    }
    private static final class NativeSnowball extends SnowballItem implements PolymerItem {
        NativeSnowball(Properties properties){super(properties);}
        @Override public Item getPolymerItem(ItemStack stack,PacketContext context){return Items.SNOWBALL;}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            var game=me.skywars2.SkyWars2.game();
            return game!=null&&player instanceof ServerPlayer owner&&game.items().thrownUtilities().use(owner,hand)
                    ?InteractionResult.SUCCESS:InteractionResult.FAIL;
        }
    }
}
