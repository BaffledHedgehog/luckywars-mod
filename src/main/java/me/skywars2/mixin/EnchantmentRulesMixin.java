package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Author rules: distinct enchantments coexist; books can enchant any nonempty anvil input. */
public final class EnchantmentRulesMixin {
    private EnchantmentRulesMixin() { }

    @Mixin(Enchantment.class)
    public abstract static class Compatibility {
        @Inject(method="areCompatible",at=@At("HEAD"),cancellable=true)
        private static void skywars2$distinctEnchantments(Holder<Enchantment> first,Holder<Enchantment> second,
                                                        CallbackInfoReturnable<Boolean> cir) {
            cir.setReturnValue(!first.equals(second));
        }
    }

    @Mixin(AnvilMenu.class)
    public abstract static class AnvilBooks {
        @Unique private boolean skywars2$usingBook() {
            return ((AnvilMenu)(Object)this).getSlot(1).getItem().has(DataComponents.STORED_ENCHANTMENTS);
        }

        @WrapOperation(method="createResult",at=@At(value="INVOKE",target=
                "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;canStoreEnchantments(Lnet/minecraft/world/item/ItemStack;)Z"))
        private boolean skywars2$bookComponentGate(ItemStack input,Operation<Boolean> original) {
            return !input.isEmpty() && skywars2$usingBook() || original.call(input);
        }

        @WrapOperation(method="createResult",at=@At(value="INVOKE",target=
                "Lnet/minecraft/world/item/enchantment/Enchantment;canEnchant(Lnet/minecraft/world/item/ItemStack;)Z"))
        private boolean skywars2$bookAppliesToAnyItem(Enchantment enchantment,ItemStack input,Operation<Boolean> original) {
            return !input.isEmpty() && skywars2$usingBook() || original.call(enchantment,input);
        }
    }

    /** Native menu fixture restores the player's random enchantment seed as well as its inventory/XP. */
    @Mixin(Player.class)
    public interface PlayerSeedAccess {
        @Accessor("enchantmentSeed") void skywars2$setEnchantmentSeed(int value);
    }
}
