package me.skywars2.mixin;

import me.skywars2.game.LbcItems;
import me.skywars2.game.LbcCraftingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RepairItemRecipe.class)
public abstract class LbcRepairRecipeMixin {
    /** Специальная пара жезлов должна давать свой результат, даже когда оба legacy-донора выглядят как удочки. */
    @Inject(method = "matches", at = @At("HEAD"), cancellable = true)
    private void skywars2$preserveAbility(CraftingInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
        if(LbcCraftingRecipe.craftingKind(input)!=null) { cir.setReturnValue(false); return; }
        for (var stack : input.items()) {
            if (LbcItems.rocketIngredient(stack)) { cir.setReturnValue(false); return; }
        }
    }
}
