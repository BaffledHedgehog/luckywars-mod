package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Collection;
import net.minecraft.core.Holder;
import net.minecraft.server.commands.EnchantCommand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Operator command uses the already widened native storage, without changing survival selection. */
@Mixin(EnchantCommand.class)
public abstract class EnchantCommandRulesMixin {
    @WrapOperation(method="enchant",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/enchantment/Enchantment;getMaxLevel()I"))
    private static int skywars2$commandLevel(Enchantment enchantment,Operation<Integer> original){return Integer.MAX_VALUE;}

    @WrapOperation(method="enchant",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/enchantment/Enchantment;canEnchant(Lnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean skywars2$commandItem(Enchantment enchantment,ItemStack stack,Operation<Boolean> original){return !stack.isEmpty();}

    @WrapOperation(method="enchant",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/enchantment/EnchantmentHelper;isEnchantmentCompatible(Ljava/util/Collection;Lnet/minecraft/core/Holder;)Z"))
    private static boolean skywars2$commandCompatibility(Collection<Holder<Enchantment>> existing,Holder<Enchantment> next,Operation<Boolean> original){return true;}
}
