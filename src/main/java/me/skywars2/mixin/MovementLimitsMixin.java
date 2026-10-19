package me.skywars2.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MovementLimitsMixin {
    @Inject(method = "shouldCheckPlayerMovement", at = @At("HEAD"), cancellable = true)
    private void skywars2$disablePlayerSpeedValidation(boolean isFallFlying, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    @ModifyConstant(
            method = "handleMoveVehicle",
            constant = @Constant(doubleValue = 100.0),
            require = 1,
            allow = 1
    )
    private double skywars2$removeVehicleSpeedThreshold(double original) {
        return Double.POSITIVE_INFINITY;
    }
}
