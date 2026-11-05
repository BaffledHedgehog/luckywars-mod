package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.Shapes;

/** Shared vanilla zero-size spawn-egg placement, including source-fluid activation. */
abstract class LbcMarkerEggItem extends SimplePolymerItem {
    LbcMarkerEggItem(Properties properties){super(properties,Items.PANDA_SPAWN_EGG,true);}
    @Override public InteractionResult useOn(UseOnContext context){
        if(!(context.getPlayer() instanceof ServerPlayer player))return InteractionResult.FAIL;
        var level=player.level();var clicked=context.getClickedPos();
        if(!level.hasChunkAt(clicked)||!level.mayInteract(player,clicked)||!player.mayUseItemAt(clicked,context.getClickedFace(),context.getItemInHand()))return InteractionResult.FAIL;
        var at=level.getBlockState(clicked).getCollisionShape(level,clicked).isEmpty()?clicked:clicked.relative(context.getClickedFace());
        if(!level.hasChunkAt(at))return InteractionResult.FAIL;
        var box=new AABB(at);boolean up=!clicked.equals(at)&&context.getClickedFace()==Direction.UP;if(up)box=box.expandTowards(0,-1,0);
        var markerBox=new AABB(at.getX()+.5,at.getY()+1,at.getZ()+.5,at.getX()+.5,at.getY()+1,at.getZ()+.5);
        double y=at.getY()+1+Shapes.collide(Direction.Axis.Y,markerBox,level.getCollisions(null,box),up?-2:-1);
        return activate(player,context.getHand(),new Vec3(at.getX()+.5,y,at.getZ()+.5),at);
    }
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        if(!(player instanceof ServerPlayer owner))return InteractionResult.FAIL;
        var hit=getPlayerPOVHitResult(level,player,ClipContext.Fluid.SOURCE_ONLY);
        if(hit.getType()!=HitResult.Type.BLOCK||!(level.getBlockState(hit.getBlockPos()).getBlock() instanceof LiquidBlock))return InteractionResult.PASS;
        var at=hit.getBlockPos();if(!level.mayInteract(player,at)||!player.mayUseItemAt(at,hit.getDirection(),player.getItemInHand(hand)))return InteractionResult.FAIL;
        var result=activate(owner,hand,new Vec3(at.getX()+.5,at.getY(),at.getZ()+.5),at);
        // ItemStack.useOn records block use itself; vanilla spawn eggs record fluid use here.
        if(result==InteractionResult.SUCCESS)owner.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
        return result;
    }
    protected abstract InteractionResult activate(ServerPlayer player,InteractionHand hand,Vec3 origin,BlockPos at);
}
