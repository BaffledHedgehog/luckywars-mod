package me.skywars2.mixin;

import me.skywars2.game.SignedEffectVisuals;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Covers command, portal and native dimension transfers without a second visual timer. */
@Mixin(ServerPlayer.class)
public abstract class DimensionPostEffectsMixin {
    @Inject(method="setServerLevel",at=@At("TAIL"))
    private void skywars2$worldProjection(CallbackInfo ci){
        var player=(ServerPlayer)(Object)this;
        if(player.connection!=null)SignedEffectVisuals.synchronize(player);
    }
}
