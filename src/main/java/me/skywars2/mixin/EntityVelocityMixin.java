package me.skywars2.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Entity.class)
public abstract class EntityVelocityMixin {
    @ModifyConstant(
            method = "load",
            constant = @Constant(doubleValue = 10.0),
            require = 3,
            allow = 3
    )
    private double skywars2$removeSavedMotionThreshold(double original) {
        // Finite saved components pass; Entity.setDeltaMovement still rejects non-finite vectors.
        return Double.POSITIVE_INFINITY;
    }
}
