package me.skywars2.game;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/** Restore the vanilla on-block -> air -> next-hand order hidden by the client's note block. */
public final class LbcCubeInteractions {
    private LbcCubeInteractions(){}
    public static InteractionResult useOn(ServerPlayerGameMode mode,ServerPlayer player,Level level,ItemStack stack,
            InteractionHand hand,BlockHitResult hit,Operation<InteractionResult> original){
        boolean masked=!player.isSecondaryUseActive()&&!player.isSpectator()
                &&level.getBlockState(hit.getBlockPos()).getBlock() instanceof LbcBlockModels.Cube;
        var result=original.call(player,level,stack,hand,hit);
        if(!masked||result!=InteractionResult.PASS)return result;
        return afterBlock(mode,player,level,hand,hit,original);
    }
    /** Interaction proxies need the same client ordering, starting with native block use. */
    public static InteractionResult throughBlock(ServerPlayer player,InteractionHand hand,BlockHitResult hit){
        if(player.isUsingItem())return InteractionResult.CONSUME;
        var level=player.level();var mode=player.gameMode;
        boolean cubeHandlesFallback=!player.isSecondaryUseActive()&&!player.isSpectator()
                &&level.getBlockState(hit.getBlockPos()).getBlock() instanceof LbcBlockModels.Cube;
        var result=mode.useItemOn(player,level,player.getItemInHand(hand),hand,hit);
        if(result==InteractionResult.PASS&&!cubeHandlesFallback)result=afterBlock(mode,player,level,hand,hit,null);
        return result;
    }
    // Inside the Cube wrapper use its original call; a proxy starts outside that wrapper.
    private static InteractionResult afterBlock(ServerPlayerGameMode mode,ServerPlayer player,Level level,
            InteractionHand hand,BlockHitResult hit,Operation<InteractionResult> original){
        var air=air(mode,player,level,hand);
        if(air instanceof InteractionResult.Success)return air;
        if(hand!=InteractionHand.MAIN_HAND)return air;
        var offhand=player.getOffhandItem();
        var animation=offhand.getInteractAnimation();
        if(!offhand.isItemEnabled(level.enabledFeatures()))return InteractionResult.PASS;
        var other=original==null?mode.useItemOn(player,level,offhand,InteractionHand.OFF_HAND,hit)
                :original.call(player,level,offhand,InteractionHand.OFF_HAND,hit);
        if(other==InteractionResult.PASS)other=air(mode,player,level,InteractionHand.OFF_HAND);
        if(other instanceof InteractionResult.Success success){
            if(success.swingSource()!=InteractionResult.SwingSource.NONE)player.swing(InteractionHand.OFF_HAND,animation,true);
            // The packet addressed the main hand; the actual successful action used the offhand.
            return new InteractionResult.Success(InteractionResult.SwingSource.NONE,success.itemContext());
        }
        return other;
    }
    private static InteractionResult air(ServerPlayerGameMode mode,ServerPlayer player,Level level,InteractionHand hand){
        var current=player.getItemInHand(hand);
        return current.isEmpty()||!current.isItemEnabled(level.enabledFeatures())?InteractionResult.PASS:mode.useItem(player,level,current,hand);
    }
}
