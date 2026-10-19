package me.skywars2.mixin;

import me.skywars2.game.FullInvisibility;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ClientboundSetPassengersPacket.class)
public abstract class FullInvisibilityPassengersMixin {
    @ModifyArg(method = "write", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/network/FriendlyByteBuf;writeVarIntArray([I)Lnet/minecraft/network/FriendlyByteBuf;"))
    private int[] skywars2$invisiblePassengers(int[] passengers) {
        return FullInvisibility.visiblePassengers(passengers, PacketContext.orElseThrow());
    }
}
