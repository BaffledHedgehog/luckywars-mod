package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Quantity belongs to the stored stack, independently of its item's component maximum. */
@Mixin(LootTable.class)
public abstract class OversizedLootMixin {
    private static boolean oversized(ItemStack stack){
        // Native persistent item codecs accept counts through99. Larger ordinary
        // loot quantities keep vanilla's splitting instead of becoming unsaveable.
        return stack.getCount()>stack.getMaxStackSize()&&stack.getCount()<=99;
    }
    @Inject(method="createStackSplitter",at=@At("RETURN"),cancellable=true)
    private static void skywars2$keepLootQuantity(ServerLevel level,Consumer<ItemStack> output,
            CallbackInfoReturnable<Consumer<ItemStack>> cir){
        var vanilla=cir.getReturnValue();
        cir.setReturnValue(stack->{
            if(oversized(stack)&&stack.isItemEnabled(level.enabledFeatures()))output.accept(stack);
            else vanilla.accept(stack);
        });
    }
    @WrapOperation(method="shuffleAndSplitItems",at=@At(value="INVOKE",
            target="Lnet/minecraft/world/item/ItemStack;getCount()I",ordinal=0))
    private int skywars2$oneContainerSlot(ItemStack stack,Operation<Integer> original){
        return oversized(stack)?1:original.call(stack);
    }
    @Mixin({BaseContainerBlockEntity.class,SimpleContainer.class,AbstractFurnaceBlockEntity.class,HopperBlockEntity.class})
    public abstract static class Storage {
        @WrapOperation(method="setItem",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;limitSize(I)V"))
        private void skywars2$storedQuantity(ItemStack stack,int maximum,Operation<Void> original){
            // Preserve loot stacks on subsequent save/load and hopper put-back too.
            // Normal inventory transfers still obey their destination Slot limits.
            boolean packed=stack.getCount()>stack.getMaxStackSize()&&stack.getCount()<=99;
            original.call(stack,packed?Math.max(maximum,stack.getCount()):maximum);
        }
    }
    @Mixin(net.minecraft.world.entity.vehicle.ContainerEntity.class)
    public interface Vehicle {
        @WrapOperation(method="setChestVehicleItem",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;limitSize(I)V"))
        default void skywars2$storedQuantity(ItemStack stack,int maximum,Operation<Void> original){
            boolean packed=stack.getCount()>stack.getMaxStackSize()&&stack.getCount()<=99;
            original.call(stack,packed?Math.max(maximum,stack.getCount()):maximum);
        }
    }
    @Mixin(ItemStack.class)
    public abstract static class Durability {
        @ModifyArg(method="applyDamage",index=0,at=@At(value="INVOKE",target="Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
        private Object skywars2$oneBrokenItem(Object broken){
            var item=(ItemStack)broken;return item.getCount()>1?item.copyWithCount(1):item;
        }
        @Inject(method="applyDamage",at=@At("RETURN"))
        private void skywars2$nextItemDurability(int damage,net.minecraft.server.level.ServerPlayer owner,
                Consumer<ItemStack> onBreak,CallbackInfo ci){
            var stack=(ItemStack)(Object)this;
            if(!stack.isEmpty()&&stack.isBroken())stack.setDamageValue(0);
        }
    }
}
