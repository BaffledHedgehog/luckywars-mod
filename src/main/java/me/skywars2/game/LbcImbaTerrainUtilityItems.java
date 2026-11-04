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
    private static final class Bastion extends SimplePolymerItem {
        Bastion(Properties properties){super(properties,Items.PANDA_SPAWN_EGG,true);}
        @Override public InteractionResult useOn(UseOnContext context){
            if(!(context.getPlayer() instanceof ServerPlayer player))return InteractionResult.FAIL;var level=player.level();var clicked=context.getClickedPos();
            if(!level.hasChunkAt(clicked)||!level.mayInteract(player,clicked)||!player.mayUseItemAt(clicked,context.getClickedFace(),context.getItemInHand()))return InteractionResult.FAIL;
            var at=level.getBlockState(clicked).getCollisionShape(level,clicked).isEmpty()?clicked:clicked.relative(context.getClickedFace());
            if(!level.hasChunkAt(at))return InteractionResult.FAIL;
            // Same collision-offset calculation as EntityType.getYOffset for the zero-size marker.
            var box=new AABB(at);boolean movedUp=!clicked.equals(at)&&context.getClickedFace()==Direction.UP;if(movedUp)box=box.expandTowards(0,-1,0);
            var markerBox=new AABB(at.getX()+.5,at.getY()+1,at.getZ()+.5,at.getX()+.5,at.getY()+1,at.getZ()+.5);
            double y=at.getY()+1+Shapes.collide(Direction.Axis.Y,markerBox,level.getCollisions(null,box),movedUp?-2:-1);
            return activate(player,context.getHand(),new Vec3(at.getX()+.5,y,at.getZ()+.5),at);
        }
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(!(player instanceof ServerPlayer owner))return InteractionResult.FAIL;var hit=getPlayerPOVHitResult(level,player,ClipContext.Fluid.SOURCE_ONLY);
            if(hit.getType()!=HitResult.Type.BLOCK||!(level.getBlockState(hit.getBlockPos()).getBlock() instanceof LiquidBlock))return InteractionResult.PASS;
            var at=hit.getBlockPos();if(!level.mayInteract(player,at)||!player.mayUseItemAt(at,hit.getDirection(),player.getItemInHand(hand)))return InteractionResult.FAIL;
            return activate(owner,hand,new Vec3(at.getX()+.5,at.getY(),at.getZ()+.5),at);
        }
        private InteractionResult activate(ServerPlayer player,InteractionHand hand,Vec3 origin,BlockPos at){var service=service();if(service==null||!service.useBastion(player,hand,origin))return InteractionResult.FAIL;player.level().gameEvent(player,net.minecraft.world.level.gameevent.GameEvent.ENTITY_PLACE,at);return InteractionResult.SUCCESS;}
    }
    public static ItemStack normalize(ItemStack source){
        Item target=LbcImbaTerrainUtilities.dirter(source)?DIRTER:LbcImbaTerrainUtilities.bastion(source)?BASTION:null;
        if(target==null||source.is(target))return source;var result=source.transmuteCopy(target);result.applyComponents(source.getComponents());return result;
    }
    /** Old saved vanilla carriers enter the same item implementation at the actual input boundary. */
    public static InteractionResult legacyBlock(UseOnContext context){
        var item=context.getItemInHand();
        if(!item.is(DIRTER)&&LbcImbaTerrainUtilities.dirter(item))return DIRTER.useOn(context);
        if(!item.is(BASTION)&&LbcImbaTerrainUtilities.bastion(item))return BASTION.useOn(context);
        return InteractionResult.PASS;
    }
    public static InteractionResult legacyAir(Level level,Player player,InteractionHand hand){
        var item=player.getItemInHand(hand);
        return !item.is(BASTION)&&LbcImbaTerrainUtilities.bastion(item)?BASTION.use(level,player,hand):InteractionResult.PASS;
    }
}
