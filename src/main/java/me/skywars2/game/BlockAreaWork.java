package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.function.Predicate;
import java.util.function.BiFunction;

/** Очередь обработки области по загруженным секциям без загрузки чанков. */
final class BlockAreaWork {
    record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        boolean contains(BlockPos pos) {
            return pos.getX() >= minX && pos.getX() <= maxX && pos.getY() >= minY && pos.getY() <= maxY
                    && pos.getZ() >= minZ && pos.getZ() <= maxZ;
        }

        Bounds intersection(Bounds other) {
            return intersection(other.minX, other.minY, other.minZ, other.maxX, other.maxY, other.maxZ);
        }

        Bounds intersection(int x1, int y1, int z1, int x2, int y2, int z2) {
            int xMin = Math.max(minX, x1), yMin = Math.max(minY, y1), zMin = Math.max(minZ, z1);
            int xMax = Math.min(maxX, x2), yMax = Math.min(maxY, y2), zMax = Math.min(maxZ, z2);
            return xMin <= xMax && yMin <= yMax && zMin <= zMax
                    ? new Bounds(xMin, yMin, zMin, xMax, yMax, zMax) : null;
        }
    }

    private final ServerLevel level;
    private final Predicate<BlockState> eligible;
    private final BiFunction<BlockPos, BlockState, BlockState> replacement;
    private final int writeFlags;
    private final boolean retainUnloaded;
    private final boolean filterOnEnqueue;
    private final LinkedHashMap<Long, SectionScan> scans = new LinkedHashMap<>();
    private Bounds bounds;

    BlockAreaWork(ServerLevel level, Predicate<BlockState> eligible,
                  BiFunction<BlockPos, BlockState, BlockState> replacement, int writeFlags) {
        this(level,eligible,replacement,writeFlags,false);
    }
    BlockAreaWork(ServerLevel level, Predicate<BlockState> eligible,
                  BiFunction<BlockPos, BlockState, BlockState> replacement, int writeFlags, boolean retainUnloaded) {
        this(level,eligible,replacement,writeFlags,retainUnloaded,false);
    }
    /** Ранняя фильтрация подходит живой ауре, которая повторно получает block/chunk events. */
    BlockAreaWork(ServerLevel level, Predicate<BlockState> eligible,
                  BiFunction<BlockPos, BlockState, BlockState> replacement, int writeFlags,
                  boolean retainUnloaded, boolean filterOnEnqueue) {
        this.level = level;
        this.eligible = eligible;
        this.replacement = replacement;
        this.writeFlags = writeFlags;
        this.retainUnloaded = retainUnloaded;
        this.filterOnEnqueue = filterOnEnqueue;
    }

    /** Completed coverage only: no writes, rescan or pending-work replay. */
    void resetBounds(Bounds completed){scans.clear();bounds=completed;}

    void move(Bounds next, boolean rescan) {
        Bounds previous = bounds;
        bounds = next;
        scans.values().removeIf(scan -> {
            if (!scan.intersects(next)) return true;
            scan.retain(next);
            return scan.positions.isEmpty();
        });
        if (rescan || previous == null) {
            add(next);
            return;
        }
        enqueueDifference(next, previous);
    }

    void add(Bounds region) {
        if (region == null) return;
        Bounds clipped = region.intersection(region.minX, level.getMinY(), region.minZ,
                region.maxX, level.getMaxY(), region.maxZ);
        if (clipped == null) return;
        for (int sx = clipped.minX >> 4; sx <= clipped.maxX >> 4; sx++)
            for (int sz = clipped.minZ >> 4; sz <= clipped.maxZ >> 4; sz++)
                for (int sy = clipped.minY >> 4; sy <= clipped.maxY >> 4; sy++) {
                    if (filterOnEnqueue) {
                        var chunk = level.getChunkSource().getChunkNow(sx, sz);
                        if (chunk != null && !hasEligibleBlocks(chunk.getSection(level.getSectionIndexFromSectionY(sy))))
                            continue;
                    }
                    long key = SectionPos.asLong(sx, sy, sz);
                    SectionScan scan = scans.get(key);
                    if (scan == null) {
                        scan = new SectionScan(sx, sy, sz);
                        scans.put(key, scan);
                    }
                    scan.add(clipped);
                }
    }

    /** Неизменяемый каст объединяет точные области: перекрытия обходятся один раз, промежутки не сканируются. */
    void include(Bounds region) {
        if(region==null) return;
        bounds=bounds==null ? region : new Bounds(Math.min(bounds.minX,region.minX),Math.min(bounds.minY,region.minY),
                Math.min(bounds.minZ,region.minZ),Math.max(bounds.maxX,region.maxX),Math.max(bounds.maxY,region.maxY),
                Math.max(bounds.maxZ,region.maxZ));
        add(region);
    }

    void add(BlockPos pos) {
        add(new Bounds(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ()));
    }

    boolean hasWork() { return !scans.isEmpty(); }

    int queuedBlockCount() {
        int count = 0;
        for (SectionScan scan : scans.values()) count += scan.positions.cardinality();
        return count;
    }

    void clear() { scans.clear(); bounds = null; }

    private boolean hasEligibleBlocks(net.minecraft.world.level.chunk.LevelChunkSection section) {
        // maybeHas консервативен: старый state может остаться в палитре после замены всех клеток.
        return section != null && (!section.hasOnlyAir() || eligible.test(Blocks.AIR.defaultBlockState()))
                && section.maybeHas(eligible);
    }

    /** Один section batch; retainUnloaded сохраняет принятую работу, не загружая чанк. */
    int step(GameController game, ServerPlayer owner, int allowance, long deadline) {
        return step(game,owner,null,allowance,deadline);
    }
    /** Sustained/deferred work keeps its original author, policy and causal order. */
    int step(GameController game, ServerPlayer owner,LbcIndirectKills.Owner cause,int allowance,long deadline) {
        if (scans.isEmpty() || allowance <= 0) return 0;
        var entry = scans.firstEntry();
        SectionScan scan = entry.getValue();
        var chunk = level.getChunkSource().getChunkNow(scan.x, scan.z);
        if(chunk==null && retainUnloaded) {
            scans.putLast(entry.getKey(),scan); return 0;
        }
        var section = chunk == null ? null : chunk.getSection(level.getSectionIndexFromSectionY(scan.y));
        if (!hasEligibleBlocks(section)) {
            scans.remove(entry.getKey());
            return 1;
        }

        int[] inspected = {0};
        Runnable writes=() -> {
            var pos = new BlockPos.MutableBlockPos();
            int cursor = 0;
            for (int n = 0; n < allowance; n++) {
                if ((n & 31) == 0 && System.nanoTime() >= deadline) break;
                int bit = scan.positions.nextSetBit(cursor);
                if (bit < 0) break;
                scan.positions.clear(bit);
                cursor = bit + 1;
                inspected[0]++;
                int x = bit & 15, z = (bit >> 4) & 15, y = bit >> 8;
                pos.set((scan.x << 4) + x, (scan.y << 4) + y, (scan.z << 4) + z);
                BlockState state = section.getBlockState(x, y, z);
                if (eligible.test(state) && bounds != null && bounds.contains(pos) && replacement != null) {
                    BlockState result = replacement.apply(pos, state);
                    if (result != null && !result.equals(state)) level.setBlock(pos, result, writeFlags);
                }
            }
        };
        if(cause==null)game.items().indirectKills().trapChanges(level,owner,writes);
        else game.items().indirectKills().trapChanges(level,cause,writes);
        if (scan.positions.isEmpty()) scans.remove(entry.getKey());
        else scans.putLast(entry.getKey(), scan);
        return inspected[0];
    }

    private void enqueueDifference(Bounds next, Bounds previous) {
        Bounds overlap = next.intersection(previous);
        if (overlap == null) { add(next); return; }
        add(new Bounds(next.minX, next.minY, next.minZ, overlap.minX - 1, next.maxY, next.maxZ));
        add(new Bounds(overlap.maxX + 1, next.minY, next.minZ, next.maxX, next.maxY, next.maxZ));
        add(new Bounds(overlap.minX, next.minY, next.minZ, overlap.maxX, overlap.minY - 1, next.maxZ));
        add(new Bounds(overlap.minX, overlap.maxY + 1, next.minZ, overlap.maxX, next.maxY, next.maxZ));
        add(new Bounds(overlap.minX, overlap.minY, next.minZ, overlap.maxX, overlap.maxY, overlap.minZ - 1));
        add(new Bounds(overlap.minX, overlap.minY, overlap.maxZ + 1, overlap.maxX, overlap.maxY, next.maxZ));
    }

    private static final class SectionScan {
        final int x, y, z;
        final BitSet positions = new BitSet(4096);
        SectionScan(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
        boolean intersects(Bounds area) {
            return area.intersection(x << 4, y << 4, z << 4, (x << 4) + 15, (y << 4) + 15, (z << 4) + 15) != null;
        }
        void retain(Bounds area) {
            // Y/Z/X лежат подряд в BitSet: режем диапазоны, а не перебираем тысячи ожидающих клеток.
            int baseX = x << 4, baseY = y << 4, baseZ = z << 4;
            int minX = Math.max(0, area.minX - baseX), maxX = Math.min(15, area.maxX - baseX);
            int minY = Math.max(0, area.minY - baseY), maxY = Math.min(15, area.maxY - baseY);
            int minZ = Math.max(0, area.minZ - baseZ), maxZ = Math.min(15, area.maxZ - baseZ);
            if (minX == 0 && maxX == 15 && minY == 0 && maxY == 15 && minZ == 0 && maxZ == 15) return;
            positions.clear(0, minY << 8);
            positions.clear((maxY + 1) << 8, 4096);
            for (int ly = minY; ly <= maxY; ly++) {
                int layer = ly << 8;
                positions.clear(layer, layer + (minZ << 4));
                positions.clear(layer + ((maxZ + 1) << 4), layer + 256);
                if (minX == 0 && maxX == 15) continue;
                for (int lz = minZ; lz <= maxZ; lz++) {
                    int row = layer | (lz << 4);
                    positions.clear(row, row + minX);
                    positions.clear(row + maxX + 1, row + 16);
                }
            }
        }
        void add(Bounds area) {
            int baseX = x << 4, baseY = y << 4, baseZ = z << 4;
            int minX = Math.max(0, area.minX - baseX), maxX = Math.min(15, area.maxX - baseX);
            for (int ly = Math.max(0, area.minY - baseY); ly <= Math.min(15, area.maxY - baseY); ly++)
                for (int lz = Math.max(0, area.minZ - baseZ); lz <= Math.min(15, area.maxZ - baseZ); lz++) {
                    int row = (ly << 8) | (lz << 4);
                    positions.set(row + minX, row + maxX + 1);
                }
        }
    }
    /** Independent pending section order/bitmaps; immutable predicates/configuration stay with this worker. */
    Checkpoint checkpoint(){return new Checkpoint();}
    final class Checkpoint {
        private record Scan(long key,int x,int y,int z,BitSet cells) { }
        private final Bounds region=bounds;
        private final java.util.List<Scan> image=scans.entrySet().stream().map(e->new Scan(e.getKey(),e.getValue().x,e.getValue().y,e.getValue().z,(BitSet)e.getValue().positions.clone())).toList();
        void restore(){scans.clear();for(var saved:image){var scan=new SectionScan(saved.x,saved.y,saved.z);scan.positions.or(saved.cells);scans.put(saved.key,scan);}bounds=region;}
    }

}
