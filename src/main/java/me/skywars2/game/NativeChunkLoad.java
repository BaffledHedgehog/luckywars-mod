package me.skywars2.game;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;

final class NativeChunkLoad {
    private NativeChunkLoad() {}

    /** A FULL ticket can exist before the chunk data becomes available without blocking. */
    static boolean resident(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    /** Vanilla can report a failed load as a successful future containing an error result. */
    static boolean failed(CompletableFuture<?> future) {
        if (!future.isDone()) return false;
        return future.isCompletedExceptionally()
                || future.getNow(null) instanceof ChunkResult<?> result && !result.isSuccess();
    }
}
