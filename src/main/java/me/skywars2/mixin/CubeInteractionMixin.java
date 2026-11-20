package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.skywars2.game.LbcCubeInteractions;
import me.skywars2.game.LbcTmtTriggers;
import me.skywars2.game.LbcUseInteractions;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerPlayerGameMode.class)
public abstract class CubeInteractionMixin implements LbcUseInteractions.BlockInput {
    @org.spongepowered.asm.mixin.Unique private LbcUseInteractions.BlockClick skywars2$pendingBlockInput;
    @org.spongepowered.asm.mixin.Unique private LbcUseInteractions.AirClick skywars2$pendingAirInput;
    @org.spongepowered.asm.mixin.Unique private boolean skywars2$resolvingBlockUse;
    @org.spongepowered.asm.mixin.Unique private boolean skywars2$shieldClick;
    @Override public void skywars2$finishBlockInput(){skywars2$pendingBlockInput=null;skywars2$pendingAirInput=null;skywars2$shieldClick=false;}
    @org.spongepowered.asm.mixin.Unique private void skywars2$rightClick(ServerPlayer player,InteractionHand hand){
        if(LbcUseInteractions.forcedUse(player)){
            LbcTmtTriggers.useItemInput(player,hand);
            return;
        }
        // A use-on, an air fallback and an offhand shield packet can all describe one client click.
        if(LbcUseInteractions.usesAsShield(player.getOffhandItem())){
            if(skywars2$shieldClick)return;
            skywars2$shieldClick=true;
        }
        LbcTmtTriggers.input(player,LbcTmtTriggers.Input.RIGHT_CLICK);
    }
    @WrapMethod(method="useItem")
    private InteractionResult skywars2$shieldAfterServerUse(ServerPlayer player,Level level,ItemStack stack,
            InteractionHand hand,Operation<InteractionResult> original){
        var game=me.skywars2.SkyWars2.game();
        if(LbcUseInteractions.shieldHandoff(player,hand))return original.call(player,level,stack,hand);
        boolean forced=LbcUseInteractions.forcedUse(player);
        var blockInput=skywars2$pendingBlockInput;
        var airInput=skywars2$pendingAirInput;
        boolean pairedBlock=!forced && blockInput!=null && blockInput.pairsAir(level,hand,stack);
        boolean pairedAir=!forced && hand==InteractionHand.OFF_HAND && airInput!=null
                && airInput.pairsOffhand(level,player.getMainHandItem(),stack);
        if(!forced&&!skywars2$resolvingBlockUse){
            skywars2$pendingBlockInput=pairedBlock && hand==InteractionHand.MAIN_HAND ? blockInput.afterAir() : null;
            skywars2$pendingAirInput=null;
        }
        if(LbcUseInteractions.holdingOffhandShield(player))return InteractionResult.FAIL;
        if(!forced&&game!=null&&game.items().academy().tryUse(player,hand))return InteractionResult.SUCCESS;
        if(LbcUseInteractions.deferAirToOffhand(player,stack,hand,pairedBlock?blockInput.hit():null))return InteractionResult.PASS;
        if(!forced&&me.skywars2.game.LbcTmtTriggers.tryCycle(player,stack,hand))return me.skywars2.game.LbcUseInteractions.quietResult(true,InteractionResult.SUCCESS);
        if(forced || (!pairedBlock && !pairedAir && !skywars2$resolvingBlockUse))skywars2$rightClick(player,hand);
        var before=stack.copy();var result=me.skywars2.game.LbcTmtShotCompletion.call(stack,()->original.call(player,level,stack,hand));
        // A passive main-hand item lets the client try its offhand for the same press.
        if(!forced&&!pairedBlock&&!skywars2$resolvingBlockUse&&hand==InteractionHand.MAIN_HAND&&result==InteractionResult.PASS
                &&stack==player.getMainHandItem()&&!stack.isEmpty()&&!player.getOffhandItem().isEmpty())
            skywars2$pendingAirInput=new LbcUseInteractions.AirClick(level,stack,player.getOffhandItem());
        me.skywars2.game.LbcUseInteractions.shieldAfter((ServerPlayerGameMode)(Object)this,player,level,hand,result);
        return me.skywars2.game.LbcUseInteractions.quietResult(me.skywars2.game.LbcUseInteractions.quietUse(player,before),result);
    }
    @WrapMethod(method="useItemOn")
    private InteractionResult skywars2$cubeInput(ServerPlayer player,Level level,ItemStack stack,InteractionHand hand,
            BlockHitResult hit,Operation<InteractionResult> original){
        var game=me.skywars2.SkyWars2.game();
        var blockInput=skywars2$pendingBlockInput;
        skywars2$pendingBlockInput=null;
        skywars2$pendingAirInput=null;
        if(LbcUseInteractions.holdingOffhandShield(player))return InteractionResult.FAIL;
        if(game!=null&&game.items().academy().tryUse(player,hand))return InteractionResult.SUCCESS;
        if(me.skywars2.game.LbcTmtTriggers.tryCycle(player,stack,hand))return me.skywars2.game.LbcUseInteractions.quietResult(true,InteractionResult.SUCCESS);
        // Vanilla moves to the offhand with the same hit; a new click starts from the main hand.
        boolean paired=hand==InteractionHand.OFF_HAND && blockInput!=null
                && blockInput.pairsOffhand(level,hit,player.getMainHandItem());
        if(!paired){
            skywars2$rightClick(player,hand);
            LbcTmtTriggers.input(player,LbcTmtTriggers.Input.BLOCK_USE,null,hit.getBlockPos(),level.getBlockState(hit.getBlockPos()));
        }
        boolean quiet=me.skywars2.game.LbcUseInteractions.quietUse(player,stack);
        boolean wasResolving=skywars2$resolvingBlockUse;
        skywars2$resolvingBlockUse=true;
        try {
            var result=me.skywars2.game.LbcUseInteractions.quietResult(quiet,
                    me.skywars2.game.LbcTmtShotCompletion.call(stack,()->LbcCubeInteractions.useOn((ServerPlayerGameMode)(Object)this,player,level,stack,hand,hit,original)));
            if(result==InteractionResult.PASS)
                skywars2$pendingBlockInput=new LbcUseInteractions.BlockClick(level,hit,hand,stack,!stack.isEmpty());
            return result;
        } finally {
            skywars2$resolvingBlockUse=wasResolving;
        }
    }
}
