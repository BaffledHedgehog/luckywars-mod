package me.skywars2.media.client;

import com.mojang.blaze3d.audio.Library;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.sounds.ChannelAccess;

/** Both native pools contain the same Channel, capable of queueing streaming PCM. */
final class PhoneAudioChannels {
    private PhoneAudioChannels(){}
    static CompletableFuture<ChannelAccess.ChannelHandle> acquire(ChannelAccess access){
        // Minecraft has very few STREAMING sources. Long music must not mute the phone
        // until one finishes: use an ordinary native source when that pool is occupied.
        return access.createHandle(Library.Pool.STREAMING).thenCompose(handle->handle!=null
                ?CompletableFuture.completedFuture(handle):access.createHandle(Library.Pool.STATIC));
    }
}
