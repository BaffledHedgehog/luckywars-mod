package me.skywars2.game;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

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

    enum Residency { WAITING, READY, FAILED }

    /** A service owns one native ticket type. Its overlapping footprints share each ticket. */
    static final class Leases {
        private final TicketType type;
        private final Map<ServerLevel, Map<ChunkPos, Lease>> leases = new IdentityHashMap<>();

        Leases(TicketType type) { this.type = type; }

        Area area(ServerLevel level) { return new Area(level); }

        private static final class Lease {
            CompletableFuture<?> load;
            int holders = 1, retryTick = Integer.MIN_VALUE;

            Lease(CompletableFuture<?> load) { this.load = load; }
        }

        final class Area implements AutoCloseable {
            private final ServerLevel level;
            private final Set<ChunkPos> held = new LinkedHashSet<>();

            private Area(ServerLevel level) { this.level = level; }

            boolean empty() { return held.isEmpty(); }

            Residency resident(Vec3 point, int reach) {
                var pos = BlockPos.containing(point);
                var result = Residency.READY;
                var wanted = new LinkedHashSet<ChunkPos>();
                var source = level.getChunkSource();
                for (int x = (pos.getX() - reach) >> 4; x <= (pos.getX() + reach) >> 4; x++) {
                    for (int z = (pos.getZ() - reach) >> 4; z <= (pos.getZ() + reach) >> 4; z++) {
                        var chunk = new ChunkPos(x, z);
                        wanted.add(chunk);
                        var lease = acquire(chunk);
                        if (source.getChunkNow(x, z) != null) continue;
                        result = Residency.WAITING;
                        if (!failed(lease.load)) continue;
                        int now = level.getServer().getTickCount();
                        // Give vanilla one frame after reissue even if the scheduler revisits this wait.
                        if (lease.retryTick == now) continue;
                        if (lease.retryTick != Integer.MIN_VALUE) return Residency.FAILED;
                        lease.retryTick = now;
                        source.removeTicketWithRadius(type, chunk, 0);
                        lease.load = source.addTicketAndLoadWithRadius(type, chunk, 0);
                    }
                }
                for (var previous : List.copyOf(held)) if (!wanted.contains(previous)) release(previous);
                return result;
            }

            void hold(ChunkPos chunk) { acquire(chunk); }

            private Lease acquire(ChunkPos chunk) {
                var chunks = leases.computeIfAbsent(level, ignored -> new LinkedHashMap<>());
                var lease = chunks.get(chunk);
                if (!held.add(chunk)) return lease;
                if (lease != null) lease.holders++;
                else {
                    lease = new Lease(level.getChunkSource().addTicketAndLoadWithRadius(type, chunk, 0));
                    chunks.put(chunk, lease);
                }
                return lease;
            }

            private void release(ChunkPos chunk) {
                if (!held.remove(chunk)) return;
                var chunks = leases.get(level);
                if (--chunks.get(chunk).holders == 0) {
                    chunks.remove(chunk);
                    level.getChunkSource().removeTicketWithRadius(type, chunk, 0);
                }
                if (chunks.isEmpty()) leases.remove(level);
            }

            @Override public void close() {
                for (var chunk : List.copyOf(held)) release(chunk);
            }
        }
    }
}
