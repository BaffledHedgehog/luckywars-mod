package me.skywars2.mixin;

import java.util.List;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.skywars2.game.LbcTmtComponents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Actual survival loot and creative shulker publication both restore the physical TMT carrier. */
@Mixin(ShulkerBoxBlock.class)
public abstract class TmtShulkerDropsMixin {
    @WrapMethod(method="getDrops")
    private List<ItemStack> skywars2$outerShulkerDrops(BlockState state,LootParams.Builder params,Operation<List<ItemStack>> original){
        var drops=original.call(state,params);var block=params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if(block==null)return drops;
        return drops.stream().map(stack->stack.getItem()==state.getBlock().asItem()?LbcTmtComponents.placedDrop(block,stack):stack).toList();
    }
    @WrapOperation(method="playerWillDestroy",at=@At(value="NEW",target="(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity skywars2$creativeOuterShulker(Level level,double x,double y,double z,ItemStack stack,Operation<ItemEntity> original){
        var block=level.getBlockEntity(net.minecraft.core.BlockPos.containing(x,y,z));
        return original.call(level,x,y,z,block==null?stack:LbcTmtComponents.placedDrop(block,stack));
    }
}
