package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

/** Authored pumpkin appearance and marker placement, with no vanilla animal publication. */
public final class LbcPumpkinatorItem extends SimplePolymerItem {
    public static final Item TOKEN=Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:pumpkinator"),new LbcPumpkinatorItem());
    private LbcPumpkinatorItem(){super(LbcUtilityItemPrototypes.properties("skywars2:pumpkinator","/data/lbc/loot_table/item_defs/minecraft/pumpkin__eed30a39b1a0.json",Items.PANDA_SPAWN_EGG,64),Items.PANDA_SPAWN_EGG,true);}
    public static void register(){}
    @Override public InteractionResult useOn(UseOnContext context){
        if(!(context.getPlayer() instanceof ServerPlayer owner))return InteractionResult.FAIL;
        var level=owner.level();var clicked=context.getClickedPos();
        if(!level.hasChunkAt(clicked)||!level.mayInteract(owner,clicked)||!owner.mayUseItemAt(clicked,context.getClickedFace(),context.getItemInHand()))return InteractionResult.FAIL;
        var at=level.getBlockState(clicked).getCollisionShape(level,clicked).isEmpty()?clicked:clicked.relative(context.getClickedFace());
        if(!level.hasChunkAt(at))return InteractionResult.FAIL;
        // Vanilla EntityType placement computes this collision offset even for a zero-size marker.
        var box=new AABB(at);boolean movedUp=!clicked.equals(at)&&context.getClickedFace()==Direction.UP;
        if(movedUp)box=box.expandTowards(0,-1,0);
        var markerBox=new AABB(at.getX()+.5,at.getY()+1,at.getZ()+.5,at.getX()+.5,at.getY()+1,at.getZ()+.5);
        double y=at.getY()+1+Shapes.collide(Direction.Axis.Y,markerBox,level.getCollisions(null,box),movedUp?-2:-1);
        return activate(owner,context.getHand(),new Vec3(at.getX()+.5,y,at.getZ()+.5),at);
    }
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        if(!(player instanceof ServerPlayer owner))return InteractionResult.FAIL;
        var hit=getPlayerPOVHitResult(level,player,ClipContext.Fluid.SOURCE_ONLY);
        if(hit.getType()!=HitResult.Type.BLOCK||!(level.getBlockState(hit.getBlockPos()).getBlock() instanceof LiquidBlock))return InteractionResult.PASS;
        var at=hit.getBlockPos();if(!level.mayInteract(player,at)||!player.mayUseItemAt(at,hit.getDirection(),player.getItemInHand(hand)))return InteractionResult.FAIL;
        return activate(owner,hand,new Vec3(at.getX()+.5,at.getY(),at.getZ()+.5),at);
    }
    private InteractionResult activate(ServerPlayer owner,InteractionHand hand,Vec3 markerPosition,BlockPos at){
        var game=SkyWars2.game();if(game==null||!game.items().pumpkinator().use(owner,hand,markerPosition))return InteractionResult.FAIL;
        owner.level().gameEvent(owner,GameEvent.ENTITY_PLACE,at);return InteractionResult.SUCCESS;
    }
}
