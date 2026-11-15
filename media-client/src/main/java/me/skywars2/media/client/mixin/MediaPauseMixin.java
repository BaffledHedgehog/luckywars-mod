package me.skywars2.media.client.mixin;

import me.skywars2.media.client.MediaClient;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundPostEffectsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class MediaPauseMixin {
    @Inject(method="handlePostEffects",at=@At("TAIL"))
    private void skywars2$freezeMedia(ClientboundPostEffectsPacket packet,CallbackInfo ci){
        MediaClient.frozen(packet.postEffects().contains(net.minecraft.resources.Identifier.parse("lbc:time_stop_freeze")));
    }
}
