package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.skywars2.game.LbcTmtComponents;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Native data-inclusive creative picking finishes after vanilla component copying, before inventory publication. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class TmtShulkerPickMixin {
    @WrapOperation(method="handlePickItemFromBlock",at=@At(value="INVOKE",target="Lnet/minecraft/server/network/ServerGamePacketListenerImpl;tryPickItem(Lnet/minecraft/world/item/ItemStack;)V"))
    private void skywars2$cleanShulkerPick(ServerGamePacketListenerImpl listener,ItemStack stack,Operation<Void> original,
            @Local(argsOnly=true) ServerboundPickItemFromBlockPacket packet){
        var player=listener.player;var block=player.level().getBlockEntity(packet.pos());
        original.call(listener,player.hasInfiniteMaterials()&&packet.includeData()&&block!=null?LbcTmtComponents.placedDrop(block,stack):stack);
    }
}
