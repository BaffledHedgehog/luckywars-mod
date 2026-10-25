package me.skywars2.mixin;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import me.skywars2.game.AttributePolicy;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RangedAttribute.class)
public abstract class AttributeLimitsMixin {
    @Shadow @Final private double minValue;
    @Shadow @Final private double maxValue;
    @Inject(method = "sanitizeValue", at = @At("HEAD"), cancellable = true)
    private void skywars2$selectiveFiniteAttributeValue(double value, CallbackInfoReturnable<Double> cir) {
        // NaN and infinities keep vanilla's raw-field fallback, including native original bounds.
        if(Double.isFinite(value)){
            var bounds=AttributePolicy.bounds((RangedAttribute)(Object)this,minValue,maxValue);
            cir.setReturnValue(Math.max(bounds.minimum(),Math.min(bounds.maximum(),value)));
        }
    }

    @Inject(method = "getMinValue", at = @At("HEAD"), cancellable = true)
    private void skywars2$unboundAttributeMinimum(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(AttributePolicy.bounds((RangedAttribute)(Object)this,minValue,maxValue).minimum());
    }

    @Inject(method = "getMaxValue", at = @At("HEAD"), cancellable = true)
    private void skywars2$unboundAttributeMaximum(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(AttributePolicy.bounds((RangedAttribute)(Object)this,minValue,maxValue).maximum());
    }
}
