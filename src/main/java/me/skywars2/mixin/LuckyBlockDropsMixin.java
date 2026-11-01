package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.skywars2.game.LbcLuckyBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Existing map mushroom states and new Polymer blocks share native loot and causal admission. */
@Mixin(BlockBehaviour.class)
public abstract class LuckyBlockDropsMixin {
    @Inject(method="getDrops",at=@At("HEAD"))
    private void skywars2$luckyScore(BlockState state,LootParams.Builder params,CallbackInfoReturnable<java.util.List<ItemStack>> cir){
        LbcLuckyBlocks.prepare(state,params);
    }
    @Mixin(Block.class)
    public abstract static class Resources {
        @WrapMethod(method="dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V")
        private static void skywars2$luckyAuthor(BlockState state,Level level,BlockPos pos,BlockEntity data,Entity breaker,ItemStack tool,Operation<Void> original){
            try(var scope=LbcLuckyBlocks.rewardScope(state,breaker)){original.call(state,level,pos,data,breaker,tool);}
        }
    }
}
