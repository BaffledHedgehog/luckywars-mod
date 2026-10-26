package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Client acknowledgement, rather than the native timeout, releases manual-entity pairing. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ManualVisualLoadMixin {
    @Inject(method="handleAcceptPlayerLoad",at=@At("TAIL"))
    private void skywars2$loaded(ServerboundPlayerLoadedPacket packet,CallbackInfo ci) {
        var game=SkyWars2.game();
        if(game!=null)game.visualTracking().clientLoaded((ServerGamePacketListenerImpl)(Object)this);
    }
}
