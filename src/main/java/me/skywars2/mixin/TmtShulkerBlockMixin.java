package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.skywars2.game.LbcTmtComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;

/** The native component transfer consumes only the selected loot payload and stores a clean durable outer source. */
@Mixin(BlockEntity.class)
public abstract class TmtShulkerBlockMixin {
    @WrapMethod(method="applyComponentsFromItemStack")
    private void skywars2$outerShulkerSource(ItemStack stack,Operation<Void> original){
        original.call(LbcTmtComponents.placedComponents((BlockEntity)(Object)this,stack));
    }
}
