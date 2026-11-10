package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.skywars2.game.LbcCubeInteractions;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerPlayerGameMode.class)
public abstract class CubeInteractionMixin {
    @WrapMethod(method="useItem")
    private InteractionResult skywars2$shieldAfterServerUse(ServerPlayer player,Level level,ItemStack stack,
            InteractionHand hand,Operation<InteractionResult> original){
        var before=stack.copy();var result=original.call(player,level,stack,hand);
        me.skywars2.game.LbcUseInteractions.shieldAfter((ServerPlayerGameMode)(Object)this,player,level,before,hand,result);
        return me.skywars2.game.LbcUseInteractions.quietResult(me.skywars2.game.LbcUseInteractions.quietUse(player,before),result);
    }
    @WrapMethod(method="useItemOn")
    private InteractionResult skywars2$cubeInput(ServerPlayer player,Level level,ItemStack stack,InteractionHand hand,
            BlockHitResult hit,Operation<InteractionResult> original){
        boolean quiet=me.skywars2.game.LbcUseInteractions.quietUse(player,stack);
        return me.skywars2.game.LbcUseInteractions.quietResult(quiet,
                LbcCubeInteractions.useOn((ServerPlayerGameMode)(Object)this,player,level,stack,hand,hit,original));
    }
}
