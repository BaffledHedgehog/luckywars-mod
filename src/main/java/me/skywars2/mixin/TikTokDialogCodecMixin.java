package me.skywars2.mixin;

import io.netty.buffer.ByteBuf;
import me.skywars2.game.TikTokDialogPayloads;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerboundCustomClickActionPacket.class)
public abstract class TikTokDialogCodecMixin {
    @Shadow @Final @Mutable public static StreamCodec<ByteBuf,ServerboundCustomClickActionPacket> STREAM_CODEC;
    @Inject(method="<clinit>",at=@At("RETURN"))
    private static void skywars2$cookieBudget(CallbackInfo ci){STREAM_CODEC=TikTokDialogPayloads.wrap(STREAM_CODEC);}
}
