package me.skywars2.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ChunkTrackingView;

/** Read only on the server thread; recovery must inspect resident chunks without loading others. */
@Mixin(ChunkMap.class)
public interface ArenaChunkAccess {
    @Accessor("visibleChunkMap")
    Long2ObjectLinkedOpenHashMap<ChunkHolder> skywars2$visibleChunks();

    @Invoker("applyChunkTrackingView")
    void skywars2$applyChunkTrackingView(ServerPlayer player,ChunkTrackingView view);
}
