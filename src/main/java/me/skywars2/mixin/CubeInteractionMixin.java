package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.skywars2.game.LbcCubeInteractions;
import me.skywars2.game.LbcTmtTriggers;
import me.skywars2.game.LbcUseInteractions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerPlayerGameMode.class)
public abstract class CubeInteractionMixin implements LbcUseInteractions.BlockInput {
    @org.spongepowered.asm.mixin.Unique private BlockHitResult skywars2$pendingBlockInput;
    @org.spongepowered.asm.mixin.Unique private boolean skywars2$shieldClick;
    @Override public void skywars2$finishBlockInput(){skywars2$pendingBlockInput=null;skywars2$shieldClick=false;}
    @org.spongepowered.asm.mixin.Unique private void skywars2$rightClick(ServerPlayer player,InteractionHand hand){
        if(LbcUseInteractions.forcedUse(player)){
            LbcTmtTriggers.useItemInput(player,hand);
            return;
        }
        // A use-on, an air fallback and an offhand shield packet can all describe one client click.
        if(player.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS)){
            if(skywars2$shieldClick)return;
            skywars2$shieldClick=true;
        }
        LbcTmtTriggers.input(player,LbcTmtTriggers.Input.RIGHT_CLICK);
    }
    @WrapMethod(method="useItem")
    private InteractionResult skywars2$shieldAfterServerUse(ServerPlayer player,Level level,ItemStack stack,
            InteractionHand hand,Operation<InteractionResult> original){
        var game=me.skywars2.SkyWars2.game();
        var blockInput=skywars2$pendingBlockInput;skywars2$pendingBlockInput=null;
        if(LbcUseInteractions.shieldHandoff(player,hand))return original.call(player,level,stack,hand);
        if(LbcUseInteractions.holdingOffhandShield(player))return InteractionResult.FAIL;
        boolean forced=LbcUseInteractions.forcedUse(player);
        if(!forced&&game!=null&&game.items().academy().tryUse(player,hand))return InteractionResult.SUCCESS;
        if(me.skywars2.game.LbcUseInteractions.deferAirToOffhand(player,stack,hand,blockInput))return InteractionResult.PASS;
        if(!forced&&me.skywars2.game.LbcTmtTriggers.tryCycle(player,stack,hand))return me.skywars2.game.LbcUseInteractions.quietResult(true,InteractionResult.SUCCESS);
        skywars2$rightClick(player,hand);
        var before=stack.copy();var result=me.skywars2.game.LbcTmtShotCompletion.call(stack,()->original.call(player,level,stack,hand));
        me.skywars2.game.LbcUseInteractions.shieldAfter((ServerPlayerGameMode)(Object)this,player,level,hand,result);
        return me.skywars2.game.LbcUseInteractions.quietResult(me.skywars2.game.LbcUseInteractions.quietUse(player,before),result);
    }
    @WrapMethod(method="useItemOn")
    private InteractionResult skywars2$cubeInput(ServerPlayer player,Level level,ItemStack stack,InteractionHand hand,
            BlockHitResult hit,Operation<InteractionResult> original){
        var game=me.skywars2.SkyWars2.game();
        skywars2$pendingBlockInput=null;
        if(LbcUseInteractions.holdingOffhandShield(player))return InteractionResult.FAIL;
        if(game!=null&&game.items().academy().tryUse(player,hand))return InteractionResult.SUCCESS;
        if(me.skywars2.game.LbcTmtTriggers.tryCycle(player,stack,hand))return me.skywars2.game.LbcUseInteractions.quietResult(true,InteractionResult.SUCCESS);
        skywars2$rightClick(player,hand);
        me.skywars2.game.LbcTmtTriggers.input(player,me.skywars2.game.LbcTmtTriggers.Input.BLOCK_USE,null,hit.getBlockPos(),level.getBlockState(hit.getBlockPos()));
        boolean quiet=me.skywars2.game.LbcUseInteractions.quietUse(player,stack);
        var result=me.skywars2.game.LbcUseInteractions.quietResult(quiet,
                me.skywars2.game.LbcTmtShotCompletion.call(stack,()->LbcCubeInteractions.useOn((ServerPlayerGameMode)(Object)this,player,level,stack,hand,hit,original)));
        if(hand==InteractionHand.MAIN_HAND&&result==InteractionResult.PASS)skywars2$pendingBlockInput=hit;
        return result;
    }
}
