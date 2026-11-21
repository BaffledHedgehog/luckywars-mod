package me.skywars2.mixin.visuals;

import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import me.skywars2.game.effects.FullInvisibility;

@Mixin(ClientboundTakeItemEntityPacket.class)
public abstract class FullInvisibilityPickupMixin {
    // Меняем только ID сборщика в представлении получателя, включая пакеты внутри bundle.
    @ModifyArg(method = "write", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/network/FriendlyByteBuf;writeVarInt(I)Lnet/minecraft/network/FriendlyByteBuf;",
            ordinal = 1))
    private int skywars2$invisibleCollector(int collectorId) {
        return FullInvisibility.pickupCollectorId(collectorId, PacketContext.orElseThrow());
    }
}
