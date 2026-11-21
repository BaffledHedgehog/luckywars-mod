package me.skywars2.mixin.world;

import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.village.poi.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.*;
import net.minecraft.world.level.lighting.DynamicGraphMinFixedPoint;
import net.minecraft.world.level.lighting.LeveledPriorityQueue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Native POI memory and storage handles. Futures and storage workers remain owned by the level. */
public final class PoiCheckpointAccess {
    private PoiCheckpointAccess() { }

    @Mixin(SectionStorage.class)
    public interface Storage {
        @Accessor("simpleRegionStorage") SimpleRegionStorage skywars2$poiRegionStorage();
        @Accessor("loadLock") Object skywars2$poiLoadLock();
        @Accessor("pendingLoads") Long2ObjectMap<?> skywars2$poiPendingLoads();
        @Accessor("storage") Long2ObjectMap<Optional<PoiSection>> skywars2$poiStorage();
        @Accessor("dirtyChunks") LongLinkedOpenHashSet skywars2$poiDirtyChunks();
        @Accessor("loadedChunks") LongSet skywars2$poiStoredChunks();
        @Invoker("writeChunk") <T> Dynamic<T> skywars2$poiEncode(ChunkPos pos, DynamicOps<T> ops);
    }

    @Mixin(SimpleRegionStorage.class)
    public interface Region {
        @Accessor("worker") IOWorker skywars2$poiWorker();
    }

    @Mixin(PoiManager.class)
    public interface Manager {
        @Accessor("loadedChunks") LongSet skywars2$poiValidatedChunks();
        @Accessor("distanceTracker") PoiManager.DistanceTracker skywars2$poiDistanceTracker();
    }

    @Mixin(PoiManager.DistanceTracker.class)
    public interface Distances {
        @Accessor("levels") Long2ByteMap skywars2$poiLevels();
    }

    @Mixin(PoiSection.class)
    public interface Section {
        @Accessor("records") Short2ObjectMap<PoiRecord> skywars2$poiRecords();
        @Accessor("byType") Map<Holder<PoiType>,Set<PoiRecord>> skywars2$poiRecordsByType();
        @Accessor("setDirty") Runnable skywars2$poiSectionDirty();
        @Accessor("isValid") void skywars2$poiValid(boolean value);
    }

    @Mixin(PoiRecord.class)
    public interface Record {
        @Accessor("setDirty") Runnable skywars2$poiRecordDirty();
        @Accessor("freeTickets") void skywars2$poiFreeTickets(int value);
    }

    @Mixin(DynamicGraphMinFixedPoint.class)
    public interface Graph {
        @Accessor("priorityQueue") LeveledPriorityQueue skywars2$poiPriorityQueue();
        @Accessor("computedLevels") Long2ByteMap skywars2$poiComputedLevels();
        @Accessor("levelCount") int skywars2$poiGraphLevelCount();
        @Accessor("hasWork") boolean skywars2$poiGraphHasWork();
        @Accessor("hasWork") void skywars2$poiGraphHasWork(boolean value);
    }

    @Mixin(LeveledPriorityQueue.class)
    public interface Queue {
        @Accessor("queues") LongLinkedOpenHashSet[] skywars2$poiQueues();
        @Accessor("firstQueuedLevel") int skywars2$poiFirstQueuedLevel();
        @Accessor("firstQueuedLevel") void skywars2$poiFirstQueuedLevel(int value);
        @Accessor("levelCount") int skywars2$poiQueueLevelCount();
    }
}
