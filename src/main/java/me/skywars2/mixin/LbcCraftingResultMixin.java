package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ResultSlot.class)
public abstract class LbcCraftingResultMixin {
    @Shadow @Final private CraftingContainer craftSlots;
    @org.spongepowered.asm.mixin.Unique private int skywars2$wholeDonor=-1;

    /** Обычный клик и Shift-клик приходят сюда до расхода ингредиентов; preview сюда не приходит. */
    @Inject(method = "onTake", at = @At("HEAD"))
    private void skywars2$craftEffects(Player player, ItemStack result, CallbackInfo ci) {
        skywars2$wholeDonor=-1;
        var game = SkyWars2.game();
        if (game != null && player instanceof ServerPlayer serverPlayer) {
            var input=craftSlots.asCraftInput();var kind=me.skywars2.game.LbcCraftingRecipe.craftingKind(input);
            if(game.canUseGameplayItems(serverPlayer)&&(kind==me.skywars2.game.LbcCraftingRecipe.Kind.MULTIPLY_2||kind==me.skywars2.game.LbcCraftingRecipe.Kind.MULTIPLY_4))
                skywars2$wholeDonor=me.skywars2.game.LbcMultiplierCrafting.wholeDonor(input);
            me.skywars2.game.LbcCraftingRecipe.craftTaken(serverPlayer,craftSlots.asCraftInput());
        }
    }
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="onTake",at=@At(value="INVOKE",target="Lnet/minecraft/world/inventory/CraftingContainer;removeItem(II)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack skywars2$wholeStack(CraftingContainer grid,int slot,int count,com.llamalad7.mixinextras.injector.wrapoperation.Operation<ItemStack> original){
        return original.call(grid,slot,slot==skywars2$wholeDonor?grid.getItem(slot).getCount():count);
    }
}
