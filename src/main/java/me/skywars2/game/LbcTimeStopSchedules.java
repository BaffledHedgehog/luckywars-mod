package me.skywars2.game;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Collections;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;

/** Keeps only the stopped dimension's scheduled ticks on its activation-time clock. */
public final class LbcTimeStopSchedules {
    private static final Map<ServerLevel, Long> STARTS = new IdentityHashMap<>();
    private static final Map<LevelTicks<?>, ServerLevel> LEVELS = new IdentityHashMap<>();
    private static final Map<LevelChunkTicks<?>, ServerLevel> CONTAINERS = new IdentityHashMap<>();
    private static final Set<LevelChunkTicks<?>> REBASING = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final ThreadLocal<Long> COPY_OFFSET = ThreadLocal.withInitial(() -> 0L);

    private LbcTimeStopSchedules() {}

    /** Implemented by the LevelTicks mixin; never changes the world's shared game time. */
    public interface Access<T> {
        Long2ObjectMap<LevelChunkTicks<T>> skywars2$containers();
        Long2LongMap skywars2$nextTicks();
        Queue<LevelChunkTicks<T>> skywars2$drainingContainers();
    }

    public static void register(ServerLevel level) {
        register(level.getBlockTicks(), level);
        register(level.getFluidTicks(), level);
    }

    private static <T> void register(LevelTicks<T> ticks, ServerLevel level) {
        if (LEVELS.put(ticks, level) == level) return;
        for (LevelChunkTicks<T> container : access(ticks).skywars2$containers().values()) {
            added(ticks, container);
        }
        refresh(ticks);
    }

    public static void begin(ServerLevel level) {
        register(level);
        STARTS.putIfAbsent(level, level.getGameTime());
    }

    public static void end(ServerLevel level) {
        Long start = STARTS.remove(level);
        if (start == null) return;
        long elapsed = Math.max(0L, level.getGameTime() - start);
        restore(level.getBlockTicks(), elapsed);
        restore(level.getFluidTicks(), elapsed);
    }

    public static void clear() {
        for (ServerLevel level : STARTS.keySet().toArray(ServerLevel[]::new)) end(level);
        LEVELS.clear();
        CONTAINERS.clear();
        REBASING.clear();
    }

    public static boolean stopped(LevelTicks<?> ticks) {
        return STARTS.containsKey(LEVELS.get(ticks));
    }

    public static long elapsed(LevelTicks<?> ticks) {
        return elapsed(LEVELS.get(ticks));
    }

    private static long elapsed(ServerLevel level) {
        Long start = STARTS.get(level);
        return start == null ? 0L : Math.max(0L, level.getGameTime() - start);
    }

    /** pack/unpack use the same clock as the attached queue, including pending saved ticks. */
    public static long containerClock(LevelChunkTicks<?> container, long currentTick) {
        return currentTick - elapsed(CONTAINERS.get(container));
    }

    public static <T> ScheduledTick<T> scheduling(LevelChunkTicks<T> container, ScheduledTick<T> tick) {
        return REBASING.contains(container) ? tick
                : shifted(tick, COPY_OFFSET.get() - elapsed(CONTAINERS.get(container)));
    }

    public static void copyArea(LevelTicks<?> source, Runnable operation) {
        long previous = COPY_OFFSET.get();
        COPY_OFFSET.set(elapsed(source));
        try {
            operation.run();
        } finally {
            if (previous == 0L) COPY_OFFSET.remove();
            else COPY_OFFSET.set(previous);
        }
    }

    public static <T> ScheduledTick<T> shifted(ScheduledTick<T> tick, long delta) {
        return delta == 0L ? tick : new ScheduledTick<>(tick.type(), tick.pos(), tick.triggerTick() + delta,
                tick.priority(), tick.subTickOrder());
    }

    /** Runs before vanilla installs the callback and indexes the new queue head. */
    public static <T> void added(LevelTicks<T> ticks, LevelChunkTicks<T> container) {
        ServerLevel level = LEVELS.get(ticks);
        if (level == null || CONTAINERS.put(container, level) == level) return;
        shift(container, -elapsed(level));
    }

    /** Restore the real clock before an unloaded chunk can be serialized without its level. */
    public static <T> void removing(LevelTicks<T> ticks, long chunkPos) {
        LevelChunkTicks<T> container = access(ticks).skywars2$containers().get(chunkPos);
        if (container == null) return;
        ServerLevel level = CONTAINERS.remove(container);
        shift(container, elapsed(level));
    }

    private static <T> void restore(LevelTicks<T> ticks, long elapsed) {
        for (LevelChunkTicks<T> container : access(ticks).skywars2$containers().values()) shift(container, elapsed);
        refresh(ticks);
    }

    private static <T> void shift(LevelChunkTicks<T> container, long delta) {
        if (delta == 0L) return;
        // Pending SavedTicks already contain relative delays and must remain untouched.
        var queued = container.getAll().toList();
        REBASING.add(container);
        try {
            container.removeIf(tick -> true);
            for (ScheduledTick<T> tick : queued) container.schedule(shifted(tick, delta));
        } finally {
            REBASING.remove(container);
        }
    }

    private static <T> void refresh(LevelTicks<T> ticks) {
        Access<T> access = access(ticks);
        access.skywars2$nextTicks().clear();
        // Usually empty between ticks; rebuilding also preserves an existing native heap.
        var draining = access.skywars2$drainingContainers().stream().toList();
        access.skywars2$drainingContainers().clear();
        access.skywars2$drainingContainers().addAll(draining);
        for (var entry : access.skywars2$containers().long2ObjectEntrySet()) {
            ScheduledTick<T> next = entry.getValue().peek();
            if (next != null) access.skywars2$nextTicks().put(entry.getLongKey(), next.triggerTick());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Access<T> access(LevelTicks<T> ticks) {
        return (Access<T>) ticks;
    }
}
