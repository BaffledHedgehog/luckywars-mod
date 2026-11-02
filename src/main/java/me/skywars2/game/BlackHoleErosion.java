package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiConsumer;

/** A bounded sphere cursor, always relative to the live hole rather than old world positions. */
final class BlackHoleErosion {
    private static final int MAX_RADIUS = 24;
    private static final int[][] OFFSETS = createOffsets();
    private static final int[] CORE = OFFSETS[3];
    private static final int[] EMPTY = new int[0];
    private final ServerLevel level;
    private final long seed;
    private final boolean noisy;
    private Vec3 center;
    private double radiusSquared;
    private double guaranteedCoreSquared;
    private int baseX, baseY, baseZ;
    private int[] offsets = EMPTY;
    private int cursor, coreCursor = CORE.length;
    private int clock, completedClock;
    private long pass;
    private boolean completed;

    BlackHoleErosion(ServerLevel level, long seed, boolean noisy) {
        this.level = Objects.requireNonNull(level);
        this.seed = seed;
        this.noisy = noisy;
    }

    /** Refresh geometry without restarting an unfinished pass, even when the hole moves. */
    void move(Vec3 next, double radius, int clock) {
        Objects.requireNonNull(next);
        if (!Double.isFinite(radius) || radius < 0 || radius > MAX_RADIUS
                || !Double.isFinite(next.x) || !Double.isFinite(next.y) || !Double.isFinite(next.z))
            throw new IllegalArgumentException("Black hole requires finite center and radius in [0,24]");
        var block = BlockPos.containing(next);
        boolean moved = center == null || block.getX() != baseX || block.getY() != baseY || block.getZ() != baseZ;
        boolean resized = center == null || radiusSquared != radius * radius;
        center = next;
        baseX = block.getX(); baseY = block.getY(); baseZ = block.getZ();
        radiusSquared = radius * radius;
        double guaranteedCore = Math.min(1.5, radius * .3);
        guaranteedCoreSquared = guaranteedCore * guaranteedCore;
        this.clock = clock;
        if (resized) {
            offsets = OFFSETS[(int) Math.ceil(radius)];
            cursor = 0;
            completed = false;
            pass++;
        } else if (completed && completedClock != clock) {
            cursor = 0;
            completed = false;
            pass++;
        }
        if (moved || resized) coreCursor = 0;
    }

    boolean hasWork() { return center != null && (cursor < offsets.length || coreCursor < CORE.length); }

    /** Candidate count, including cheap masked/air/unresident skips; never stores an old-position queue. */
    int queuedBlockCount() {
        return center == null ? 0 : offsets.length - cursor + CORE.length - coreCursor;
    }

    void clear() {
        center = null;
        offsets = EMPTY;
        cursor = 0;
        coreCursor = CORE.length;
        completed = false;
    }

    /** Returns inspected candidates (not writes), respecting both the allowance and the CPU deadline. */
    int step(int allowance, long deadline, BiConsumer<BlockPos, BlockState> erase) {
        if (allowance <= 0 || !hasWork() || System.nanoTime() >= deadline) return 0;
        Objects.requireNonNull(erase);
        // At most five sections per axis for radius24, including arbitrary fractional centers.
        int sectionMinX = (baseX - 25) >> 4, sectionMinY = (baseY - 25) >> 4, sectionMinZ = (baseZ - 25) >> 4;
        var sections = new LevelChunkSection[125];
        var visited = new boolean[125];
        int inspected = 0;
        // New core cells get prompt attention, while at least 3/4 of the budget advances the outer cursor.
        int coreBudget = Math.min(96, allowance / 4);
        while (coreCursor < CORE.length && inspected < coreBudget && System.nanoTime() < deadline) {
            inspect(CORE[coreCursor++], true, sections, visited, sectionMinX, sectionMinY, sectionMinZ, erase);
            inspected++;
        }
        while (cursor < offsets.length && inspected < allowance && System.nanoTime() < deadline) {
            inspect(offsets[cursor++], false, sections, visited, sectionMinX, sectionMinY, sectionMinZ, erase);
            inspected++;
        }
        // Drain a small final core tail if the main sphere already finished.
        while (cursor == offsets.length && coreCursor < CORE.length && inspected < allowance && System.nanoTime() < deadline) {
            inspect(CORE[coreCursor++], true, sections, visited, sectionMinX, sectionMinY, sectionMinZ, erase);
            inspected++;
        }
        if (!hasWork()) {
            completed = true;
            completedClock = clock;
        }
        return inspected;
    }

    private void inspect(int offset, boolean core, LevelChunkSection[] sections, boolean[] visited,
                         int minX, int minY, int minZ, BiConsumer<BlockPos, BlockState> erase) {
        int x = baseX + (offset & 63) - 32;
        int y = baseY + ((offset >>> 6) & 63) - 32;
        int z = baseZ + ((offset >>> 12) & 63) - 32;
        double dx = x + .5 - center.x, dy = y + .5 - center.y, dz = z + .5 - center.z;
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        if (distanceSquared > radiusSquared || (core && distanceSquared > 9)
                || y < level.getMinY() || y > level.getMaxY()) return;
        if (noisy && distanceSquared > guaranteedCoreSquared) {
            double remaining = radiusSquared == 0 ? 1 : 1 - distanceSquared / radiusSquared;
            double chance = .04 + .76 * remaining * remaining;
            long epoch = core ? pass ^ (0x9e3779b97f4a7c15L * clock) : pass;
            long noise = mix(seed ^ BlockPos.asLong(x, y, z) ^ mix(epoch));
            if ((noise >>> 11) * 0x1.0p-53 >= chance) return;
        }
        int sx = x >> 4, sy = y >> 4, sz = z >> 4;
        int index = (sx - minX) * 25 + (sy - minY) * 5 + sz - minZ;
        if (!visited[index]) {
            visited[index] = true;
            var chunk = level.getChunkSource().getChunkNow(sx, sz);
            if (chunk != null) {
                var section = chunk.getSection(level.getSectionIndexFromSectionY(sy));
                if (!section.hasOnlyAir() && section.maybeHas(state -> !state.isAir())) sections[index] = section;
            }
        }
        var section = sections[index];
        if (section == null || section.hasOnlyAir()) return;
        var state = section.getBlockState(x & 15, y & 15, z & 15);
        if (!state.isAir()) erase.accept(new BlockPos(x, y, z), state);
    }

    /** Shared spheres: two-block radial bands shuffled within each band, without rigid shell traversal. */
    private static int[][] createOffsets() {
        int extent = MAX_RADIUS + 1;
        long[] ordered = new long[(extent * 2 + 1) * (extent * 2 + 1) * (extent * 2 + 1)];
        int count = 0;
        double padding = Math.sqrt(.75);
        for (int x = -extent; x <= extent; x++)
            for (int y = -extent; y <= extent; y++)
                for (int z = -extent; z <= extent; z++) {
                    double distance = Math.sqrt(x * x + y * y + z * z);
                    if (distance > MAX_RADIUS + padding) continue;
                    int packed = x + 32 | (y + 32) << 6 | (z + 32) << 12;
                    long shuffle = mix(packed ^ 0x6a09e667f3bcc909L) & ((1L << 40) - 1);
                    ordered[count++] = ((long) (distance / 2) << 58) | shuffle << 18 | packed;
                }
        Arrays.sort(ordered, 0, count);
        int[][] result = new int[MAX_RADIUS + 1][];
        for (int radius = 0; radius <= MAX_RADIUS; radius++) {
            int[] sphere = new int[count];
            int size = 0;
            double limit = (radius + padding) * (radius + padding);
            for (int i = 0; i < count; i++) {
                int packed = (int) (ordered[i] & ((1 << 18) - 1));
                int x = (packed & 63) - 32, y = ((packed >>> 6) & 63) - 32, z = ((packed >>> 12) & 63) - 32;
                if (x * x + y * y + z * z <= limit) sphere[size++] = packed;
            }
            result[radius] = Arrays.copyOf(sphere, size);
        }
        return result;
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    /** Copy relative scan progress, never the effects of an earlier terrain pass. */
    BlackHoleErosion copyTranslated(Vec3 offset) {
        var copy=new BlackHoleErosion(level,seed,noisy);
        copy.center=center==null?null:center.add(offset);copy.radiusSquared=radiusSquared;
        copy.guaranteedCoreSquared=guaranteedCoreSquared;copy.offsets=offsets.clone();
        copy.cursor=cursor;copy.coreCursor=coreCursor;copy.clock=clock;copy.completedClock=completedClock;
        copy.pass=pass;copy.completed=completed;
        if(copy.center!=null){var block=BlockPos.containing(copy.center);copy.baseX=block.getX();copy.baseY=block.getY();copy.baseZ=block.getZ();}
        return copy;
    }

}
