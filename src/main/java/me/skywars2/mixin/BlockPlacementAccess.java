package me.skywars2.mixin;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Query the actual item's placement rules without placing or consuming anything. */
@Mixin(BlockItem.class)
public interface BlockPlacementAccess {
    @Invoker("getPlacementState") BlockState skywars2$placementState(BlockPlaceContext context);
}
