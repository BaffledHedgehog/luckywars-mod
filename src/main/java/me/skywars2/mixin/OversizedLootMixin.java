package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;
import me.skywars2.game.LbcBulkLoot;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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
    @Mixin(ItemStackTemplate.class)
    public abstract static class StoredContents {
        @WrapOperation(method = "create", at = @At(value = "INVOKE",
                target = "Lnet/minecraft/world/item/ItemStackTemplate;validate(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
        private ItemStack skywars2$materializeStoredQuantity(ItemStackTemplate template, ItemStack stack,
                Operation<ItemStack> original) {
            if (!LbcBulkLoot.isOversizedStoredStack(stack)) return original.call(template, stack);
            int count = stack.getCount();
            stack.setCount(1);
            ItemStack validated = original.call(template, stack);
            // Polymer also materializes raw templates directly from the packet codec.
            if (!validated.isEmpty()) validated.setCount(count);
            return validated;
        }
    }

    @Mixin(ItemStack.class)
    public abstract static class ContainerLimits {
        private static final int POTION_STACK_SIZE = 64;
        @Unique private static final CompoundTag skywars2$PURIFICATION_FLAGS = skywars2$purificationFlags();
        @Unique private static final Component skywars2$PURIFICATION_NAME =
                Component.translatable("purification_potion").withStyle(style -> style.withItalic(false));
        @Unique private static final List<Component> skywars2$SPLIT_PURIFICATION_LORE = List.of(
                Component.translatable("type").withStyle(style -> style.withColor(ChatFormatting.AQUA).withItalic(false)),
                Component.translatable("potion.lbc").withStyle(style -> style.withColor(ChatFormatting.AQUA).withItalic(false)),
                Component.translatable("purification_potion.lore").withStyle(style -> style.withColor(ChatFormatting.WHITE).withItalic(false))
        );

        @Inject(method="<init>(Lnet/minecraft/core/Holder;ILnet/minecraft/core/component/PatchedDataComponentMap;)V",at=@At("TAIL"))
        private void skywars2$potionStackLimit(Holder<Item> item,int count,PatchedDataComponentMap components,CallbackInfo ci){
            // Set an explicit component so an unmodified client sees the same limit as the server.
            if(item.value() instanceof PotionItem){
                components.set(DataComponents.MAX_STACK_SIZE,POTION_STACK_SIZE);
                if(item.value()==Items.SPLASH_POTION)skywars2$normalizePurification((ItemStack)(Object)this);
            }
        }

        @Inject(method={"applyComponentsAndValidate", "applyComponents(Lnet/minecraft/core/component/DataComponentPatch;)V"},
                at=@At("TAIL"))
        private void skywars2$normalizePatchedPurification(DataComponentPatch patch,CallbackInfo ci) {
            var stack=(ItemStack)(Object)this;
            if(stack.getItem() instanceof PotionItem&&stack.is(Items.SPLASH_POTION))skywars2$normalizePurification(stack);
        }

        @Unique
        private static CompoundTag skywars2$purificationFlags() {
            var tag=new CompoundTag();
            tag.putInt("transformable",1);
            tag.putInt("potion",1);
            return tag;
        }

        @Unique
        private static void skywars2$normalizePurification(ItemStack stack) {
            if(!skywars2$PURIFICATION_NAME.equals(stack.get(DataComponents.CUSTOM_NAME)))return;
            var data=stack.get(DataComponents.CUSTOM_DATA);
            var lore=stack.get(DataComponents.LORE);
            if(data==null||!data.matchedBy(skywars2$PURIFICATION_FLAGS)
                    ||lore==null||!lore.lines().equals(skywars2$SPLIT_PURIFICATION_LORE))return;
            var lines=lore.lines();
            stack.set(DataComponents.LORE,new ItemLore(List.of(lines.get(0).copy().append(lines.get(1)),lines.get(2))));
        }

        @ModifyVariable(method = {"applyComponentsAndValidate", "applyComponents(Lnet/minecraft/core/component/DataComponentPatch;)V"},
                at = @At("HEAD"), argsOnly = true)
        private DataComponentPatch skywars2$keepPotionStackLimit(DataComponentPatch patch){
            var stack = (ItemStack)(Object)this;
            if (!(stack.getItem() instanceof PotionItem)
                    || Integer.valueOf(POTION_STACK_SIZE).equals(patch.get(stack, DataComponents.MAX_STACK_SIZE))) return patch;
            // Older loot may still supply a smaller limit. Keep all the other
            // components and validate the patch with the same cap as a new stack.
            var parts = patch.split();
            var normalized = DataComponentPatch.builder().set(parts.added());
            parts.removed().forEach(normalized::remove);
            return normalized.set(DataComponents.MAX_STACK_SIZE, POTION_STACK_SIZE).build();
        }

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
