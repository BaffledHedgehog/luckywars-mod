package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.skywars2.SkyWars2;
import me.skywars2.game.LbcBedwarsAuxiliaries;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Finite native source-item placement and player mining hooks; no independent terrain owner. */
public interface BedwarsAuxiliaryMixin {
    @Mixin(BlockItem.class)
    abstract class Placement {
        @WrapMethod(method="place")
        private InteractionResult skywars2$markedTeamChest(BlockPlaceContext context,Operation<InteractionResult> original){
            var game=SkyWars2.game();
            if(game==null||!(context.getPlayer() instanceof ServerPlayer player)||!(context.getLevel() instanceof ServerLevel level)
                    ||!LbcBedwarsAuxiliaries.chestItem(context.getItemInHand()))return original.call(context);
            var source=context.getItemInHand().copy();var pos=context.getClickedPos().immutable();
            var result=original.call(context);
            if(result instanceof InteractionResult.Success)game.bedwars().auxiliaries().placed(player,level,pos,source);
            return result;
        }
    }
    @Mixin(ServerPlayerGameMode.class)
    abstract class PlayerBreak {
        @Shadow @Final protected ServerPlayer player;
        @WrapMethod(method="destroyBlock")
        private boolean skywars2$ownedChestBreak(BlockPos pos,Operation<Boolean> original){
            var game=SkyWars2.game();
            return game==null?original.call(pos):game.bedwars().auxiliaries().withPlayerBreak(player,pos,()->original.call(pos));
        }
    }
    @Mixin(Block.class)
    abstract class MiningDrops {
        @WrapOperation(method="playerDestroy",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/Block;dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V"))
        private void skywars2$onlyMarkedChestDrops(BlockState state,Level level,BlockPos pos,BlockEntity blockEntity,Entity breaker,ItemStack tool,Operation<Void> original){
            var game=SkyWars2.game();
            if(game!=null&&state.is(Blocks.ENDER_CHEST)&&level instanceof ServerLevel server&&game.bedwars().auxiliaries().suppressMiningDrops(server,pos))return;
            original.call(state,level,pos,blockEntity,breaker,tool);
        }
    }
}
