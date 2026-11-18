package me.skywars2.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerChunkSender.class)
public abstract class CarpetChunkPacketMixin {
    @Inject(method = "sendChunk", at = @At("HEAD"), cancellable = true)
    private static void skywars2$skipDiscardedChunkPacket(ServerGamePacketListenerImpl connection,
            ServerLevel level, LevelChunk chunk, CallbackInfo ci) {
        // Carpet's fake connection discards outgoing packets and has no PLAY registry context.
        // The native sender has already taken the position out of its pending queue.
        if (connection.getClass().getName().equals("carpet.patches.NetHandlerPlayServerFake")) ci.cancel();
    }
}
