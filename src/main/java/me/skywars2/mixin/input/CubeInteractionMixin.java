package me.skywars2.mixin.input;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import eu.pb4.polymer.core.api.item.PolymerItem;
import me.skywars2.game.items.tmt.LbcTmtTriggers;
import me.skywars2.game.players.inventory.LbcUseInteractions;
import me.skywars2.game.world.terrain.LbcCubeInteractions;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import me.skywars2.game.GameController;

@Mixin(ServerPlayerGameMode.class)
public abstract class CubeInteractionMixin implements LbcUseInteractions.BlockInput {
    @org.spongepowered.asm.mixin.Unique private LbcUseInteractions.TargetClick skywars2$pendingTargetInput;
    @org.spongepowered.asm.mixin.Unique private LbcUseInteractions.AirClick skywars2$pendingAirInput;
    @org.spongepowered.asm.mixin.Unique private boolean skywars2$resolvingBlockUse;
    @org.spongepowered.asm.mixin.Unique private boolean skywars2$shieldClick;
    @Override public void skywars2$finishBlockInput(){skywars2$pendingTargetInput=null;skywars2$pendingAirInput=null;skywars2$shieldClick=false;}
    @Override public void skywars2$entityPress(ServerPlayer player, InteractionHand hand, Entity target){
        var targetInput=skywars2$pendingTargetInput;
        skywars2$pendingTargetInput=null;
        skywars2$pendingAirInput=null;
        if(hand!=InteractionHand.OFF_HAND || targetInput==null
                || !targetInput.pairsOffhand(player.level(),target,player.getMainHandItem()))skywars2$rightClick(player,hand);
    }
    @Override public void skywars2$rememberEntityPass(ServerPlayer player, InteractionHand hand, Entity target, ItemStack stack){
        skywars2$pendingTargetInput=new LbcUseInteractions.TargetClick(player.level(),new EntityHitResult(target),hand,stack,!stack.isEmpty());
    }
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
        var targetInput=skywars2$pendingTargetInput;
        var airInput=skywars2$pendingAirInput;
        boolean pairedTarget=!forced && targetInput!=null && targetInput.pairsAir(level,hand,stack);
        boolean pairedAir=!forced && hand==InteractionHand.OFF_HAND && airInput!=null
                && airInput.pairsOffhand(level,player.getMainHandItem(),stack);
        if(!forced&&!skywars2$resolvingBlockUse){
            skywars2$pendingTargetInput=pairedTarget && hand==InteractionHand.MAIN_HAND ? targetInput.afterAir() : null;
            skywars2$pendingAirInput=null;
        }
        if(LbcUseInteractions.holdingOffhandShield(player))return InteractionResult.FAIL;
        if(me.skywars2.game.entities.LbcDimensionItems.nexus(level))return original.call(player,level,stack,hand);
        if(!forced&&game!=null&&game.items().academy().tryUse(player,hand))return InteractionResult.SUCCESS;
        if(LbcUseInteractions.deferAirToOffhand(player,stack,hand,pairedTarget?targetInput.blockHit():null))return InteractionResult.PASS;
        if(!forced&&me.skywars2.game.items.tmt.LbcTmtTriggers.tryCycle(player,stack,hand))return me.skywars2.game.players.inventory.LbcUseInteractions.quietResult(true,InteractionResult.SUCCESS);
        if(forced || (!pairedTarget && !pairedAir && !skywars2$resolvingBlockUse))skywars2$rightClick(player,hand);
        var before=stack.copy();var result=me.skywars2.game.items.tmt.LbcTmtShotCompletion.call(stack,()->original.call(player,level,stack,hand));
        // A passive main-hand item lets the client try its offhand for the same press.
        if(!forced&&!pairedTarget&&!skywars2$resolvingBlockUse&&hand==InteractionHand.MAIN_HAND&&result==InteractionResult.PASS
                &&stack==player.getMainHandItem()&&!stack.isEmpty()&&!player.getOffhandItem().isEmpty())
            skywars2$pendingAirInput=new LbcUseInteractions.AirClick(level,stack,player.getOffhandItem());
        me.skywars2.game.players.inventory.LbcUseInteractions.shieldAfter((ServerPlayerGameMode)(Object)this,player,level,hand,result);
        return me.skywars2.game.players.inventory.LbcUseInteractions.quietResult(me.skywars2.game.players.inventory.LbcUseInteractions.quietUse(player,before),result);
    }

    @WrapOperation(method="useItemOn", at=@At(value="INVOKE", target="Lnet/minecraft/world/item/ItemStack;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult skywars2$shieldAfterCustomItemOnBlock(ItemStack stack, UseOnContext context,
            Operation<InteractionResult> original) {
        var item = stack.getItem();
        if(context.getPlayer() instanceof ServerPlayer owner&&me.skywars2.game.entities.LbcDimensionItems.nexus(context.getLevel())
                &&!me.skywars2.game.entities.LbcDimensionItems.allowed(owner,stack))return InteractionResult.FAIL;
        boolean custom=me.skywars2.game.entities.LbcDimensionItems.custom(stack);
        var game=me.skywars2.SkyWars2.game();
        // This is after vanilla block interaction: chests still open, while a scripted
        // replacement cannot accidentally place/consume its cake/block/egg carrier.
        var scripted=game!=null&&context.getPlayer() instanceof ServerPlayer owner
                ?game.items().genie().runtime().replaceUse(owner,context.getHand()):InteractionResult.PASS;
        var result = scripted==InteractionResult.PASS?original.call(stack, context):scripted;
        // A successful client carrier use may suppress the offhand packet. Native block
        // interactions and block placement keep their own priority and do not start a shield.
        if ((item instanceof PolymerItem || custom) && !(item instanceof BlockItem) && context.getPlayer() instanceof ServerPlayer player) {
            LbcUseInteractions.shieldAfter((ServerPlayerGameMode)(Object)this, player, context.getLevel(), context.getHand(), result);
        }
        return result;
    }

    @WrapMethod(method="useItemOn")
    private InteractionResult skywars2$cubeInput(ServerPlayer player,Level level,ItemStack stack,InteractionHand hand,
            BlockHitResult hit,Operation<InteractionResult> original){
        var game=me.skywars2.SkyWars2.game();
        var targetInput=skywars2$pendingTargetInput;
        skywars2$pendingTargetInput=null;
        skywars2$pendingAirInput=null;
        if(LbcUseInteractions.holdingOffhandShield(player))return InteractionResult.FAIL;
        if(me.skywars2.game.entities.LbcDimensionItems.nexus(level))
            return LbcCubeInteractions.useOn((ServerPlayerGameMode)(Object)this,player,level,stack,hand,hit,original);
        if(game!=null&&game.items().academy().tryUse(player,hand))return InteractionResult.SUCCESS;
        if(me.skywars2.game.items.tmt.LbcTmtTriggers.tryCycle(player,stack,hand))return me.skywars2.game.players.inventory.LbcUseInteractions.quietResult(true,InteractionResult.SUCCESS);
        // Vanilla moves to the offhand with the same hit; a new click starts from the main hand.
        boolean paired=hand==InteractionHand.OFF_HAND && targetInput!=null
                && targetInput.pairsOffhand(level,hit,player.getMainHandItem());
        if(!paired){
            skywars2$rightClick(player,hand);
            LbcTmtTriggers.input(player,LbcTmtTriggers.Input.BLOCK_USE,null,hit.getBlockPos(),level.getBlockState(hit.getBlockPos()));
        }
        boolean quiet=me.skywars2.game.players.inventory.LbcUseInteractions.quietUse(player,stack);
        boolean wasResolving=skywars2$resolvingBlockUse;
        skywars2$resolvingBlockUse=true;
        try {
            var result=me.skywars2.game.players.inventory.LbcUseInteractions.quietResult(quiet,
                    me.skywars2.game.items.tmt.LbcTmtShotCompletion.call(stack,()->LbcCubeInteractions.useOn((ServerPlayerGameMode)(Object)this,player,level,stack,hand,hit,original)));
            if(result==InteractionResult.PASS)
                skywars2$pendingTargetInput=new LbcUseInteractions.TargetClick(level,hit,hand,stack,!stack.isEmpty());
            return result;
        } finally {
            skywars2$resolvingBlockUse=wasResolving;
        }
    }
}
