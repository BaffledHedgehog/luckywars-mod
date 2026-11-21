package me.skywars2.game.world.maps;

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

public final class NativeChunkLoad {
    private NativeChunkLoad() {}

    /** A FULL ticket can exist before the chunk data becomes available without blocking. */
    public static boolean resident(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    /** Vanilla can report a failed load as a successful future containing an error result. */
    public static boolean failed(CompletableFuture<?> future) {
        if (!future.isDone()) return false;
        return future.isCompletedExceptionally()
                || future.getNow(null) instanceof ChunkResult<?> result && !result.isSuccess();
    }

    public enum Residency { WAITING, READY, FAILED }

    /** Limits new cold submissions. Completed and released requests stop occupying capacity. */
    public static final class ColdRequests {
        private final int perTick, maxPending;
        private final Set<Leases.Lease> pending = new LinkedHashSet<>();
        private int tick = Integer.MIN_VALUE, started;

        public ColdRequests(int perTick, int maxPending) {
            this.perTick = perTick;
            this.maxPending = maxPending;
        }

        private boolean admit(int now) {
            if (tick != now) {
                tick = now;
                started = 0;
                pending.removeIf(lease -> lease.load.isDone());
            }
            if (started >= perTick || pending.size() >= maxPending) return false;
            started++;
            return true;
        }
    }

    /** A service owns one native ticket type. Its overlapping footprints share each ticket. */
    public static final class Leases {
        private final TicketType type;
        private final Map<ServerLevel, Map<ChunkPos, Lease>> leases = new IdentityHashMap<>();

        public Leases(TicketType type) { this.type = type; }

        public Area area(ServerLevel level) { return new Area(level); }

        public Map<ChunkPos, Lease> snapshot(ServerLevel level) {
            var resident = leases.get(level);
            return resident == null ? Map.of() : Map.copyOf(resident);
        }

        public static final class Lease {
            public CompletableFuture<?> load;
            ColdRequests coldRequests;
            int holders = 1;
            public int retryTick = Integer.MIN_VALUE;

            Lease(CompletableFuture<?> load, ColdRequests coldRequests) {
                replaceLoad(load, coldRequests);
            }

            void replaceLoad(CompletableFuture<?> load, ColdRequests coldRequests) {
                releaseRequest();
                this.load = load;
                this.coldRequests = coldRequests;
                if (coldRequests != null && !load.isDone()) coldRequests.pending.add(this);
            }

            void releaseRequest() {
                if (coldRequests != null) coldRequests.pending.remove(this);
            }
        }

        public final class Area implements AutoCloseable {
            private final ServerLevel level;
            private final Set<ChunkPos> held = new LinkedHashSet<>();

            private Area(ServerLevel level) { this.level = level; }

            public boolean empty() { return held.isEmpty(); }

            public Residency resident(Vec3 point, int reach) {
                return resident(point, reach, null);
            }

            public Residency resident(Vec3 point, int reach, ColdRequests coldRequests) {
                var pos = BlockPos.containing(point);
                var result = Residency.READY;
                var wanted = new LinkedHashSet<ChunkPos>();
                var source = level.getChunkSource();
                int now = level.getServer().getTickCount();
                for (int x = (pos.getX() - reach) >> 4; x <= (pos.getX() + reach) >> 4; x++) {
                    for (int z = (pos.getZ() - reach) >> 4; z <= (pos.getZ() + reach) >> 4; z++) {
                        var chunk = new ChunkPos(x, z);
                        wanted.add(chunk);
                        boolean warm = coldRequests != null && source.getChunkNow(x, z) != null;
                        var shared = coldRequests == null || warm ? null : leases.get(level);
                        boolean newCold = coldRequests != null && !warm && (shared == null || !shared.containsKey(chunk));
                        if (newCold && !coldRequests.admit(now)) {
                            result = Residency.WAITING;
                            continue;
                        }
                        var lease = acquire(chunk, newCold ? coldRequests : null);
                        if (warm || source.getChunkNow(x, z) != null) continue;
                        result = Residency.WAITING;
                        if (!failed(lease.load)) continue;
                        // Give vanilla one frame after reissue even if the scheduler revisits this wait.
                        if (lease.retryTick == now) continue;
                        if (lease.retryTick != Integer.MIN_VALUE) return Residency.FAILED;
                        if (coldRequests != null && !coldRequests.admit(now)) continue;
                        lease.retryTick = now;
                        source.removeTicketWithRadius(type, chunk, 0);
                        lease.replaceLoad(source.addTicketAndLoadWithRadius(type, chunk, 0), coldRequests);
                    }
                }
                for (var previous : List.copyOf(held)) if (!wanted.contains(previous)) release(previous);
                return result;
            }

            public void hold(ChunkPos chunk) { acquire(chunk, null); }

            private Lease acquire(ChunkPos chunk, ColdRequests coldRequests) {
                var chunks = leases.computeIfAbsent(level, ignored -> new LinkedHashMap<>());
                var lease = chunks.get(chunk);
                if (!held.add(chunk)) return lease;
                if (lease != null) lease.holders++;
                else {
                    lease = new Lease(level.getChunkSource().addTicketAndLoadWithRadius(type, chunk, 0), coldRequests);
                    chunks.put(chunk, lease);
                }
                return lease;
            }

            private void release(ChunkPos chunk) {
                if (!held.remove(chunk)) return;
                var chunks = leases.get(level);
                var lease = chunks.get(chunk);
                if (--lease.holders == 0) {
                    lease.releaseRequest();
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
