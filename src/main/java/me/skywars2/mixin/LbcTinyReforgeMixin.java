package me.skywars2.mixin;

import me.skywars2.game.LbcReforgeCrafting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerMenu.class)
public abstract class LbcTinyReforgeMixin {
    @Inject(method="clicked",at=@At("HEAD"),cancellable=true)
    private void skywars2$tinyReforge(int slot,int button,ContainerInput action,Player player,CallbackInfo ci) {
        if(player instanceof ServerPlayer serverPlayer && (me.skywars2.game.LbcInfiniteShulker.blockTake((AbstractContainerMenu)(Object)this,slot,serverPlayer) || me.skywars2.game.LbcPhdCrafting.blockTake((AbstractContainerMenu)(Object)this,slot,serverPlayer) || me.skywars2.game.LbcMellstroyCrafting.blockTake((AbstractContainerMenu)(Object)this,slot,serverPlayer) || LbcReforgeCrafting.handleClick(
                (AbstractContainerMenu)(Object)this,slot,button,action,serverPlayer))) ci.cancel();
    }
    /** Direct calls must never enter vanilla's placeholder-based transfer/repeat path. */
    @Mixin({InventoryMenu.class,CraftingMenu.class})
    public abstract static class QuickMove {
        @Inject(method="quickMoveStack",at=@At("HEAD"),cancellable=true)
        private void skywars2$tinyQuickMove(Player player,int slot,CallbackInfoReturnable<ItemStack> cir) {
            if(player instanceof ServerPlayer serverPlayer && (me.skywars2.game.LbcInfiniteShulker.blockTake((AbstractContainerMenu)(Object)this,slot,serverPlayer) || me.skywars2.game.LbcPhdCrafting.blockTake((AbstractContainerMenu)(Object)this,slot,serverPlayer) || me.skywars2.game.LbcMellstroyCrafting.blockTake((AbstractContainerMenu)(Object)this,slot,serverPlayer) || LbcReforgeCrafting.handleClick(
                    (AbstractContainerMenu)(Object)this,slot,0,ContainerInput.QUICK_MOVE,serverPlayer)))
                cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
