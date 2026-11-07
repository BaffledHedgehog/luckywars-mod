package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Keep a new viewer's request while an already ticking chunk waits for its send dependency. */
@Mixin(ChunkMap.class)
public abstract class InitialChunkQueueMixin {
    @WrapOperation(method="markChunkPendingToSend(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/ChunkPos;)V",
            at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ChunkMap;getChunkToSend(J)Lnet/minecraft/world/level/chunk/LevelChunk;"))
    private LevelChunk skywars2$queueGatedChunk(ChunkMap chunks,long key,Operation<LevelChunk> original){
        var ready=original.call(chunks,key);if(ready!=null)return ready;
        var holder=((ArenaChunkAccess)chunks).skywars2$visibleChunks().get(key);
        // This caller only queues a position. The actual native sender still calls
        // getChunkToSend later and waits for every light/map dependency before sending.
        return holder==null?null:holder.getTickingChunk();
    }
}
