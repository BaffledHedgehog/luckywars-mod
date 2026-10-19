package me.skywars2.mixin;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RangedAttribute.class)
public abstract class AttributeLimitsMixin {
    @Inject(method = "sanitizeValue", at = @At("HEAD"), cancellable = true)
    private void skywars2$allowAnyFiniteAttributeValue(double value, CallbackInfoReturnable<Double> cir) {
        // Keep vanilla's finite fallback for NaN and infinities; only finite values bypass bounds.
        if (Double.isFinite(value)) cir.setReturnValue(value);
    }

    @Inject(method = "getMinValue", at = @At("HEAD"), cancellable = true)
    private void skywars2$unboundAttributeMinimum(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(-Double.MAX_VALUE);
    }

    @Inject(method = "getMaxValue", at = @At("HEAD"), cancellable = true)
    private void skywars2$unboundAttributeMaximum(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(Double.MAX_VALUE);
    }
}
