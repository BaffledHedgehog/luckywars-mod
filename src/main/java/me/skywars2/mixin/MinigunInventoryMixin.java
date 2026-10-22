package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;

/** An animated weapon cannot keep publishing held frames across an inventory transfer. */
@Mixin(AbstractContainerMenu.class)
public abstract class MinigunInventoryMixin {
    @WrapMethod(method="clicked")
    private void skywars2$transfer(int slot,int button,ContainerInput input,Player player,Operation<Void> original) {
        var game=SkyWars2.game();
        boolean interrupted=game!=null && player instanceof ServerPlayer owner
                && game.items().laserMinigun().interruptInventory(owner);
        original.call(slot,button,input,player);
        // Snapshot the resulting slots and cursor, rather than replaying a pre-click held item.
        if(interrupted) ((AbstractContainerMenu)(Object)this).sendAllDataToRemote();
    }
}
