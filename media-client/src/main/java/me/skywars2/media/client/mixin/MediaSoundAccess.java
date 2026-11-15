package me.skywars2.media.client.mixin;

import net.minecraft.client.sounds.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

public final class MediaSoundAccess {
    private MediaSoundAccess(){}
    @Mixin(SoundManager.class)
    public interface Manager {@Accessor("soundEngine") SoundEngine skywars2$engine();}
    @Mixin(SoundEngine.class)
    public interface Engine {@Accessor("channelAccess") ChannelAccess skywars2$channels();}
}
