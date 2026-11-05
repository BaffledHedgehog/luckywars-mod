package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import me.skywars2.SkyWars2;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.Shapes;

/** Real coarse-dirt BlockItem and a source-marker egg with no vanilla panda publication. */
public final class LbcImbaTerrainUtilityItems {
    public static final Item DIRTER=Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:dirter"),new Dirter(LbcUtilityItemPrototypes.properties("skywars2:dirter","/data/lbc/loot_table/item_defs/minecraft/coarse_dirt__1374abee7412.json",Items.COARSE_DIRT,64)));
    public static final Item BASTION=Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:bedrock_bastion"),new Bastion(LbcUtilityItemPrototypes.properties("skywars2:bedrock_bastion","/data/lbc/loot_table/item_defs/minecraft/bedrock__d62764262deb.json",Items.PANDA_SPAWN_EGG,64)));
    private LbcImbaTerrainUtilityItems(){}
    public static void register(){}
    private static LbcImbaTerrainUtilities service(){var game=SkyWars2.game();return game==null?null:game.items().imbaTerrainUtilities();}
    private static final class Dirter extends BlockItem implements PolymerItem {
        Dirter(Properties properties){super(net.minecraft.world.level.block.Blocks.COARSE_DIRT,properties);}
        public Item getPolymerItem(ItemStack stack,PacketContext context){return Items.COARSE_DIRT;}
        @Override public InteractionResult place(BlockPlaceContext context){
            if(!(context.getPlayer() instanceof ServerPlayer player))return InteractionResult.FAIL;var service=service();var origin=player.position();
            if(service==null||!LbcImbaTerrainUtilities.dirter(context.getItemInHand())||!service.canActivate(player,LbcImbaTerrainUtilities.Kind.DIRTER,origin))return InteractionResult.FAIL;
            var source=context.getItemInHand().copy();var result=super.place(context);
            if(result.consumesAction())service.dirterPlaced(player,context.getHand(),source,origin);return result;
        }
    }
    private static final class Bastion extends LbcMarkerEggItem {
        Bastion(Properties properties){super(properties);}
        protected InteractionResult activate(ServerPlayer player,InteractionHand hand,Vec3 origin,BlockPos at){var service=service();if(service==null||!service.useBastion(player,hand,origin))return InteractionResult.FAIL;player.level().gameEvent(player,net.minecraft.world.level.gameevent.GameEvent.ENTITY_PLACE,at);return InteractionResult.SUCCESS;}
    }
    public static ItemStack normalize(ItemStack source){
        Item target=LbcImbaTerrainUtilities.dirter(source)?DIRTER:LbcImbaTerrainUtilities.bastion(source)?BASTION:null;
        if(target==null||source.is(target))return source;var result=source.transmuteCopy(target);result.applyComponents(source.getComponents());return result;
    }
    /** Old saved vanilla carriers enter the same item implementation at the actual input boundary. */
    public static InteractionResult legacyBlock(UseOnContext context){
        var item=context.getItemInHand();
        var followup=LbcImbaFollowupItems.legacyTarget(item);if(followup!=null&&!item.is(followup))return followup.useOn(context);
        if(!item.is(DIRTER)&&LbcImbaTerrainUtilities.dirter(item))return DIRTER.useOn(context);
        if(!item.is(BASTION)&&LbcImbaTerrainUtilities.bastion(item))return BASTION.useOn(context);
        return InteractionResult.PASS;
    }
    public static InteractionResult legacyAir(Level level,Player player,InteractionHand hand){
        var item=player.getItemInHand(hand);
        var followup=LbcImbaFollowupItems.legacyTarget(item);if(followup!=null&&!item.is(followup))return followup.use(level,player,hand);
        return !item.is(BASTION)&&LbcImbaTerrainUtilities.bastion(item)?BASTION.use(level,player,hand):InteractionResult.PASS;
    }
}
