package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ground aggregates become legal slot quantities only at the pickup boundary. */
public final class GroundItemTransferMixin {
    private GroundItemTransferMixin() { }

    @Mixin(ItemEntity.class)
    public abstract static class Pickup {
        @WrapOperation(method = "playerTouch", at = @At(value = "INVOKE", target =
                "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z"))
        private boolean skywars2$legalPickup(Inventory inventory, ItemStack source, Operation<Boolean> original,
                @Share("picked") LocalIntRef picked) {
            int before = source.getCount();
            if (before <= source.getMaxStackSize()) {
                boolean result = original.call(inventory, source);
                picked.set(before - source.getCount());
                return result;
            }
            // Damaged items use a native fast path which inserts the entire offered
            // stack. Offer legal portions; bound creative/full-inventory work too.
            for (int slots = 0; !source.isEmpty() && slots <= inventory.getContainerSize(); slots++) {
                int count = Math.min(source.getCount(), source.getMaxStackSize());
                var portion = source.copyWithCount(count);
                original.call(inventory, portion);
                int accepted = count - portion.getCount();
                if (accepted <= 0) break;
                source.shrink(accepted);
            }
            picked.set(before - source.getCount());
            // Publish the reduced count; mutating the tracked ItemStack in place
            // does not dirty native entity data after a partial pickup.
            if (!source.isEmpty()) ((ItemEntity) (Object) this).setItem(source.copy());
            return picked.get() > 0;
        }

        @ModifyArg(method = "playerTouch", index = 1, at = @At(value = "INVOKE", target =
                "Lnet/minecraft/world/entity/player/Player;take(Lnet/minecraft/world/entity/Entity;I)V"))
        private int skywars2$pickupAnimationCount(int original, @Share("picked") LocalIntRef picked) {
            return picked.get();
        }

        @ModifyArg(method = "playerTouch", index = 1, at = @At(value = "INVOKE", target =
                "Lnet/minecraft/world/entity/player/Player;awardStat(Lnet/minecraft/stats/Stat;I)V"))
        private int skywars2$pickupStatisticCount(int original, @Share("picked") LocalIntRef picked) {
            return picked.get();
        }
    }

    @Mixin(HopperBlockEntity.class)
    public abstract static class Hopper {
        @Inject(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/item/ItemEntity;)Z",
                at = @At("HEAD"), cancellable = true)
        private static void skywars2$legalGroundInsertion(Container destination, ItemEntity entity,
                CallbackInfoReturnable<Boolean> cir) {
            var source = entity.getItem();
            if (source.getCount() <= source.getMaxStackSize()) return;
            int remaining = source.getCount();
            int maximum = Math.max(1, Math.min(source.getMaxStackSize(), destination.getMaxStackSize(source)));
            // Never pass an overstack to setItem: its clamping/storage semantics
            // intentionally differ for authored container loot.
            for (int slots = 0; remaining > 0 && slots <= destination.getContainerSize(); slots++) {
                int count = Math.min(remaining, maximum);
                var rejected = HopperBlockEntity.addItem(null, destination, source.copyWithCount(count), null);
                int accepted = count - rejected.getCount();
                if (accepted <= 0) break;
                remaining -= accepted;
            }
            if (remaining == 0) { entity.setItem(ItemStack.EMPTY); entity.discard(); }
            else if (remaining != source.getCount()) entity.setItem(source.copyWithCount(remaining));
            cir.setReturnValue(remaining < source.getCount());
        }
    }
}
