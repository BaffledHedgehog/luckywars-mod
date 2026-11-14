package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import me.skywars2.game.EnchantmentRules;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Author rules: distinct enchantments coexist; books can enchant any nonempty anvil input. */
public final class EnchantmentRulesMixin {
    private EnchantmentRulesMixin() { }

    @Mixin(Enchantment.class)
    public abstract static class Compatibility {
        @WrapOperation(method={"isSupportedItem","isPrimaryItem"},at=@At(value="INVOKE",target=
                "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/core/HolderSet;)Z"))
        private boolean skywars2$authoredSword(ItemStack stack,net.minecraft.core.HolderSet<net.minecraft.world.item.Item> items,Operation<Boolean> original){
            return original.call(stack,items)||me.skywars2.game.LbcSwordTools.sword(stack)&&items.contains(net.minecraft.world.item.Items.IRON_SWORD.builtInRegistryHolder());
        }

        @Inject(method="areCompatible",at=@At("HEAD"),cancellable=true)
        private static void skywars2$distinctEnchantments(Holder<Enchantment> first,Holder<Enchantment> second,
                                                        CallbackInfoReturnable<Boolean> cir) {
            cir.setReturnValue(!first.equals(second));
        }

        @Inject(method="getFullname",at=@At("HEAD"),cancellable=true)
        private static void skywars2$numericHighLevel(Holder<Enchantment> enchantment,int level,
                                                     CallbackInfoReturnable<Component> cir) {
            if(level<=10)return;
            var color=enchantment.is(EnchantmentTags.CURSE)?ChatFormatting.RED:ChatFormatting.GRAY;
            var name=ComponentUtils.mergeStyles(enchantment.value().description().copy(),Style.EMPTY.withColor(color));
            cir.setReturnValue(name.append(Component.literal(" "+level)));
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

        @ModifyArg(method="createResult",at=@At(value="INVOKE",target=
                "Lnet/minecraft/world/inventory/DataSlot;set(I)V"),index=0)
        private int skywars2$priceCap(int nativePrice) { return Math.min(nativePrice,10); }

        @Inject(method="createResult",at=@At(value="INVOKE",target=
                "Lnet/minecraft/world/item/enchantment/Enchantment;canEnchant(Lnet/minecraft/world/item/ItemStack;)Z"))
        private void skywars2$mergeLevels(CallbackInfo ci,@Local(name="current") int current,
                                         @Local(name="level") LocalIntRef level,
                                         @Local(name="entry") Object2IntMap.Entry<Holder<Enchantment>> entry) {
            level.set(EnchantmentRules.mergeLevels(current,entry.getIntValue()));
        }

        @WrapOperation(method="createResult",at=@At(value="INVOKE",target=
                "Lnet/minecraft/world/item/enchantment/Enchantment;getMaxLevel()I"))
        private int skywars2$anvilOnlyLevelLimit(Enchantment enchantment,Operation<Integer> original) {
            return Integer.MAX_VALUE;
        }

        @WrapOperation(method="createResult",at=@At(value="INVOKE",target=
                "Lnet/minecraft/world/item/enchantment/Enchantment;getAnvilCost()I"))
        private int skywars2$safeFee(Enchantment enchantment,Operation<Integer> original,
                                    @Local(name="price") LocalIntRef price,@Local(name="level") int level) {
            int fee=original.call(enchantment);
            price.set(Math.min(price.get(),10));
            // Once this contribution alone exceeds the final cap, keep arithmetic positive even
            // for INT_MAX levels. Native book fees are at least one after its divide-by-two step.
            if(level>10 && (skywars2$usingBook() || fee>0)) { price.set(0);return 1; }
            return Math.min(fee,skywars2$usingBook()?20:10);
        }
    }

    /** Native menu fixture restores the player's random enchantment seed as well as its inventory/XP. */
    @Mixin(Player.class)
    public interface PlayerSeedAccess {
        @Accessor("enchantmentSeed") void skywars2$setEnchantmentSeed(int value);
    }
}
