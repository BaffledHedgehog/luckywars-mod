package me.skywars2.mixin;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SynchedEntityData.DataValue.class)
public abstract class EntityItemMetadataMixin {
    @ModifyArg(method = "write", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/codec/StreamCodec;encode(Ljava/lang/Object;Ljava/lang/Object;)V"), index = 1)
    private Object skywars2$limitItemCountForEntityMetadata(Object value) {
        // Ground stacks can exceed the limit of the ItemStack codec used in Polymer's metadata.
        return value instanceof ItemStack stack && stack.getCount() > Item.ABSOLUTE_MAX_STACK_SIZE
                ? stack.copyWithCount(Item.ABSOLUTE_MAX_STACK_SIZE) : value;
    }
}
