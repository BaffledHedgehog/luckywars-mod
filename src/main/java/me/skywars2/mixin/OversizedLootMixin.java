package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.StreamSupport;
import me.skywars2.game.LbcBulkLoot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Quantity belongs to the stored stack, independently of its item's component maximum. */
@Mixin(LootTable.class)
public abstract class OversizedLootMixin {
    @Inject(method="createStackSplitter",at=@At("RETURN"),cancellable=true)
    private static void skywars2$keepLootQuantity(ServerLevel level,Consumer<ItemStack> output,
            CallbackInfoReturnable<Consumer<ItemStack>> cir){
        var vanilla=cir.getReturnValue();
        cir.setReturnValue(stack->{
            if(LbcBulkLoot.isOversizedStoredStack(stack)&&stack.isItemEnabled(level.enabledFeatures()))output.accept(stack);
            else vanilla.accept(stack);
        });
    }
    @WrapOperation(method="shuffleAndSplitItems",at=@At(value="INVOKE",
            target="Lnet/minecraft/world/item/ItemStack;getCount()I",ordinal=0))
    private int skywars2$oneContainerSlot(ItemStack stack,Operation<Integer> original){
        return LbcBulkLoot.isOversizedStoredStack(stack)?1:original.call(stack);
    }
    @Mixin({BaseContainerBlockEntity.class,SimpleContainer.class,AbstractFurnaceBlockEntity.class,HopperBlockEntity.class})
    public abstract static class Storage {
        @WrapOperation(method="setItem",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;limitSize(I)V"))
        private void skywars2$storedQuantity(ItemStack stack,int maximum,Operation<Void> original){
            // Preserve loot stacks on subsequent save/load and hopper put-back too.
            // Normal inventory transfers still obey their destination Slot limits.
            boolean packed=LbcBulkLoot.isOversizedStoredStack(stack);
            original.call(stack,packed?Math.max(maximum,stack.getCount()):maximum);
        }
    }
    @Mixin(net.minecraft.world.entity.vehicle.ContainerEntity.class)
    public interface Vehicle {
        @WrapOperation(method="setChestVehicleItem",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;limitSize(I)V"))
        default void skywars2$storedQuantity(ItemStack stack,int maximum,Operation<Void> original){
            boolean packed=LbcBulkLoot.isOversizedStoredStack(stack);
            original.call(stack,packed?Math.max(maximum,stack.getCount()):maximum);
        }
    }
    @Mixin(ItemContainerContents.class)
    public abstract static class StoredContents {
        @WrapOperation(method = {"createStackFromSlot", "addToTooltip"}, at = @At(value = "INVOKE",
                target = "Lnet/minecraft/world/item/ItemStackTemplate;create()Lnet/minecraft/world/item/ItemStack;"))
        private ItemStack skywars2$restoreStoredQuantity(ItemStackTemplate template, Operation<ItemStack> original) {
            return LbcBulkLoot.isOversizedStoredStack(template)
                    ? skywars2$createStoredStack(template) : original.call(template);
        }

        @ModifyArg(method = "lambda$itemCopies$0", at = @At(value = "INVOKE",
                target = "Ljava/util/Optional;map(Ljava/util/function/Function;)Ljava/util/Optional;"))
        private static Function<ItemStackTemplate, ItemStack> skywars2$restoreOptionalStack(
                Function<ItemStackTemplate, ItemStack> original) {
            return skywars2$storedStackFactory(original);
        }

        @ModifyArg(method = "asMutable", at = @At(value = "INVOKE",
                target = "Ljava/util/Optional;map(Ljava/util/function/Function;)Ljava/util/Optional;"))
        private Function<ItemStackTemplate, ItemStack> skywars2$restoreMutableStack(
                Function<ItemStackTemplate, ItemStack> original) {
            return skywars2$storedStackFactory(original);
        }

        @ModifyArg(method = "nonEmptyItemCopyStream", at = @At(value = "INVOKE",
                target = "Ljava/util/stream/Stream;map(Ljava/util/function/Function;)Ljava/util/stream/Stream;"))
        private Function<ItemStackTemplate, ItemStack> skywars2$restoreStreamStack(
                Function<ItemStackTemplate, ItemStack> original) {
            return skywars2$storedStackFactory(original);
        }

        @Unique
        private static Function<ItemStackTemplate, ItemStack> skywars2$storedStackFactory(
                Function<ItemStackTemplate, ItemStack> original) {
            return template -> LbcBulkLoot.isOversizedStoredStack(template)
                    ? skywars2$createStoredStack(template) : original.apply(template);
        }

        @Unique
        private static ItemStack skywars2$createStoredStack(ItemStackTemplate template) {
            // Validate the original components before materializing the stored quantity.
            ItemStack stack = template.withCount(1).create();
            if (!stack.isEmpty()) stack.setCount(template.count());
            return stack;
        }
    }

    @Mixin(ItemStack.class)
    public abstract static class ContainerLimits {
        // The first validation is for the slotted CONTAINER component.
        // Bundle and crossbow payloads keep vanilla's size checks.
        @ModifyArg(method = "validateComponents", at = @At(value = "INVOKE", ordinal = 0,
                target = "Lnet/minecraft/world/item/ItemStack;validateContainedItemSizes(Ljava/lang/Iterable;)Lcom/mojang/serialization/DataResult;"))
        private static Iterable<ItemInstance> skywars2$validateStoredSlots(
                Iterable<ItemInstance> items) {
            return () -> StreamSupport.stream(items.spliterator(), false)
                    .filter(item -> !LbcBulkLoot.isOversizedStoredStack(item)).iterator();
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
