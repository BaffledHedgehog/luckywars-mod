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
    private final LinkedHashMap<Long, SectionScan> scans = new LinkedHashMap<>();
    private Bounds bounds;

    BlockAreaWork(ServerLevel level, Predicate<BlockState> eligible,
                  BiFunction<BlockPos, BlockState, BlockState> replacement, int writeFlags) {
        this(level,eligible,replacement,writeFlags,false);
    }
    BlockAreaWork(ServerLevel level, Predicate<BlockState> eligible,
                  BiFunction<BlockPos, BlockState, BlockState> replacement, int writeFlags, boolean retainUnloaded) {
        this.level = level;
        this.eligible = eligible;
        this.replacement = replacement;
        this.writeFlags = writeFlags;
        this.retainUnloaded = retainUnloaded;
    }

    void move(Bounds next, boolean rescan) {
        Bounds previous = bounds;
        bounds = next;
        if (rescan || previous == null) {
            scans.values().removeIf(scan -> !scan.intersects(next));
            scans.values().forEach(scan -> scan.retain(next));
            add(next);
            return;
        }
        scans.values().removeIf(scan -> !scan.intersects(next));
        scans.values().forEach(scan -> scan.retain(next));
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

    /** Один section batch; retainUnloaded сохраняет принятую работу, не загружая чанк. */
    int step(GameController game, ServerPlayer owner, int allowance, long deadline) {
        if (scans.isEmpty() || allowance <= 0) return 0;
        var entry = scans.firstEntry();
        SectionScan scan = entry.getValue();
        var chunk = level.getChunkSource().getChunkNow(scan.x, scan.z);
        if(chunk==null && retainUnloaded) {
            scans.putLast(entry.getKey(),scan); return 0;
        }
        var section = chunk == null ? null : chunk.getSection(level.getSectionIndexFromSectionY(scan.y));
        if (section == null || (section.hasOnlyAir() && !eligible.test(Blocks.AIR.defaultBlockState()))
                || !section.maybeHas(eligible)) {
            scans.remove(entry.getKey());
            return 1;
        }

        int[] inspected = {0};
        game.items().indirectKills().trapChanges(owner, () -> {
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
        });
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
            // Bits are local Y/Z/X; clear rows outside the new area.
            int baseX = x << 4, baseY = y << 4, baseZ = z << 4;
            int minX = Math.max(0, area.minX - baseX), maxX = Math.min(15, area.maxX - baseX);
            int minY = Math.max(0, area.minY - baseY), maxY = Math.min(15, area.maxY - baseY);
            int minZ = Math.max(0, area.minZ - baseZ), maxZ = Math.min(15, area.maxZ - baseZ);
            for (int bit = positions.nextSetBit(0); bit >= 0; bit = positions.nextSetBit(bit + 1)) {
                int bx = bit & 15, bz = (bit >> 4) & 15, by = bit >> 8;
                if (bx < minX || bx > maxX || bz < minZ || bz > maxZ || by < minY || by > maxY)
                    positions.clear(bit);
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
}
