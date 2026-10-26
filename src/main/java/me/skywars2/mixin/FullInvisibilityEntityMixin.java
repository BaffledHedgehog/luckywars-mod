package me.skywars2.mixin;

import me.skywars2.game.FullInvisibility;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class FullInvisibilityEntityMixin {
    @Inject(method = "refreshDimensions", at = @At("TAIL"))
    private void skywars2$invisibleDimensions(CallbackInfo ci) {
        FullInvisibility.dimensionsChanged((Entity) (Object) this);
    }

    @Inject(method = "remove", at = @At("TAIL"))
    private void skywars2$invisibleRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
        FullInvisibility.removed((Entity) (Object) this);
        me.skywars2.game.SignedEffectVisuals.removed((Entity) (Object) this);
    }
}
