package me.skywars2.mixin;

import me.skywars2.game.FullInvisibility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class FullInvisibilityCameraMixin {
    @Inject(method = "setCamera", at = @At("HEAD"))
    private void skywars2$invisibilityCamera(Entity target, CallbackInfo ci) {
        FullInvisibility.cameraChanging((ServerPlayer) (Object) this, target);
    }
}
