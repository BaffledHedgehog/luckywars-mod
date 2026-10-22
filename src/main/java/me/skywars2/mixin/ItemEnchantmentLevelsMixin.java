package me.skywars2.mixin;

import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Widen item storage, not enchantment definitions or loot/table level-selection algorithms. */
@Mixin(ItemEnchantments.class)
public abstract class ItemEnchantmentLevelsMixin {
    @ModifyConstant(method="<init>",constant=@Constant(intValue=255))
    private int skywars2$positiveIntegerLevels(int vanillaLimit) { return Integer.MAX_VALUE; }

    @ModifyConstant(method="<clinit>",constant=@Constant(intValue=255))
    private static int skywars2$persistentIntegerLevels(int vanillaLimit) { return Integer.MAX_VALUE; }

    @Mixin(ItemEnchantments.Mutable.class)
    public abstract static class Mutable {
        @ModifyConstant(method={"set","upgrade"},constant=@Constant(intValue=255))
        private int skywars2$unlimitedMutableLevels(int vanillaLimit) { return Integer.MAX_VALUE; }
    }
}
