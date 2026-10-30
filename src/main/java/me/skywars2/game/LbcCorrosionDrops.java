package me.skywars2.game;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;

/** Sparse all-air placement; admission/debit remain in the shared special-drop dispatcher. */
public final class LbcCorrosionDrops {
    private static final int SIZE = 48;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final DustParticleOptions DUST = new DustParticleOptions(
            ARGB.colorFromFloat(1, .255f, .204f, .302f) & 0xffffff, 2);
    private final GameController game;
    private HashSet<Job> jobs = new HashSet<>();
    private int inProgress;

    public LbcCorrosionDrops(GameController game) { this.game = game; }

    public boolean ready(ItemEntity token, LbcCorrosionDropItem.Kind kind) {
        if (token.isRemoved() || token.getAge() >= 5999 || !LbcCorrosionDropItem.matches(token.getItem(), kind)
                || !game.playing() || game.stage() == GameController.Stage.ENDING
                || token.level() != game.maps().level() || !(token.level() instanceof ServerLevel level)
                || game.items().timeStop().stopped(level)) return false;
        for (var tag : token.entityTags())
            if (tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag)) return false;
        var origin = token.blockPosition().offset(-24, -24, -24);
        // Native PlaceCommand checks origin + size, including the extra upper endpoint.
        return resident(level, origin.getX(), origin.getZ(), origin.getX() + SIZE, origin.getZ() + SIZE);
    }

    public boolean spawn(ItemEntity token, LbcCorrosionDropItem.Kind kind) {
        return spawnForCheck(token, kind, net.minecraft.util.Util.getMillis());
    }

    boolean spawnForCheck(ItemEntity token, LbcCorrosionDropItem.Kind kind, long seed) {
        ++inProgress;game.items().stonekivi().beginNativeWork();try {
            if (!ready(token, kind)) return false;
            var cause = game.items().specialDrops().owner(token);
            var job = new Job((ServerLevel) token.level(), token.position(), kind,
                    cause instanceof ServerPlayer player ? player : null, seed);
            jobs.add(job);
            game.items().stonekivi().queuedWork(job);
            return true;
        }finally{game.items().stonekivi().endNativeWork();--inProgress;}
    }

    public int count() { return jobs.size(); }

    public void stop() {
        ++inProgress;game.items().stonekivi().beginNativeWork();try {
            for (var job : new ArrayList<>(jobs)) job.cancel();
        }finally{game.items().stonekivi().endNativeWork();--inProgress;}
    }

    /** Main-thread native fixtures can finish a ready job without introducing another tick budget. */
    int drainForCheck() {
        int steps = 0;
        for (var job : new ArrayList<>(jobs))
            while (steps < 100_000 && jobs.contains(job) && job.ready()) { job.step(); steps++; }
        return steps;
    }

    private static boolean resident(ServerLevel level, int minX, int minZ, int maxX, int maxZ) {
        for (int x = minX >> 4; x <= maxX >> 4; x++)
            for (int z = minZ >> 4; z <= maxZ >> 4; z++)
                if (level.getChunkSource().getChunkNow(x, z) == null) return false;
        return true;
    }

    private final class Job implements LbcStonekivi.Work {
        final ServerLevel level;
        final Vec3 point;
        final BlockPos center, origin;
        final LbcCorrosionDropItem.Kind kind;
        ServerPlayer cause;
        final String epoch = game.items().epoch();
        final RandomSource random;
        // Only successful writes are retained: no template cells or per-cell BlockPos objects.
        final IntArrayList placed = new IntArrayList();
        final BitSet occupied = new BitSet(SIZE * SIZE * SIZE);
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();
        final int minSectionY, maxSectionY;
        int chunkX, chunkZ, sectionY, cell, phase, follow, face;
        boolean cancelled;

        Job(ServerLevel level, Vec3 point, LbcCorrosionDropItem.Kind kind, ServerPlayer cause, long seed) {
            this.level = level; this.point = point; this.kind = kind; this.cause = cause;
            center = BlockPos.containing(point); origin = center.offset(-24, -24, -24);
            random = RandomSource.create(seed);
            minSectionY = Math.max(origin.getY(), level.getMinY()) >> 4;
            maxSectionY = Math.min(origin.getY() + SIZE - 1, level.getMaxY()) >> 4;
            chunkX = origin.getX() >> 4; chunkZ = origin.getZ() >> 4; sectionY = minSectionY;
            if (level.isDebug() || minSectionY > maxSectionY) phase = 3;
        }

        boolean live() {
            return !cancelled && game.playing() && game.stage() != GameController.Stage.ENDING
                    && level == game.maps().level() && game.items().currentEpoch(epoch);
        }

        @Override public boolean ready() {
            if (!live()) return true; // Let the shared queue reap stale jobs even while unloaded.
            return !game.items().timeStop().stopped(level)
                    // Native shape reads need the one-block halo, never a synchronous chunk load.
                    && resident(level, origin.getX() - 1, origin.getZ() - 1,
                    origin.getX() + SIZE, origin.getZ() + SIZE);
        }

        @Override public boolean step() {
            ++inProgress;game.items().stonekivi().beginNativeWork();try {
                if (!live()) { cancel(); return true; }
                if (!ready()) return false;
                long deadline = System.nanoTime() + 500_000;
                int startPhase = phase, writesBefore = placed.size();
                int allowance = phase == 0 ? 4096 : 128;
                game.items().indirectKills().worldChanges(level, cause, true, () -> {
                    // Air reads are cheap; budget successful writes separately so sparse terrain progresses.
                    for (int operations = 0; operations < allowance && phase == startPhase
                            && placed.size() - writesBefore < 128 && System.nanoTime() < deadline; operations++) {
                        switch (phase) {
                            case 0 -> erase();
                            case 1 -> edge();
                            case 2 -> update();
                            case 3 -> finish();
                            default -> throw new IllegalStateException("corrosion phase");
                        }
                    }
                });
                if (phase == 4) { cancel(); return true; }
                return false;
            }finally{game.items().stonekivi().endNativeWork();--inProgress;}
        }

        @Override public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            checkpointBoundary();var author=graph.links.key(cause);var entropy=MatchRandomState.capture(random);
            var writes=placed.toIntArray();var bits=(BitSet)occupied.clone();
            var scratch=pos.immutable();var adjacent=neighbor.immutable();
            int savedChunkX=chunkX,savedChunkZ=chunkZ,savedSectionY=sectionY,savedCell=cell;
            int savedPhase=phase,savedFollow=follow,savedFace=face;boolean dead=cancelled;
            return links->{checkpointBoundary();MatchAuthorityCheckpoint.world(game,level);var current=(ServerPlayer)links.get(author);
                return ()->{cause=current;placed.clear();for(int value:writes)placed.add(value);occupied.clear();occupied.or(bits);
                    pos.set(scratch);neighbor.set(adjacent);chunkX=savedChunkX;chunkZ=savedChunkZ;sectionY=savedSectionY;cell=savedCell;
                    phase=savedPhase;follow=savedFollow;face=savedFace;cancelled=dead;entropy.restore(random);};};
        }

        /** Each resident section is visited once; canonical-air sections require no cell scan. */
        void erase() {
            if (chunkX > (origin.getX() + SIZE - 1) >> 4) { phase = 1; return; }
            var chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
            var section = chunk.getSection(level.getSectionIndexFromSectionY(sectionY));
            // LevelChunk itself rejects AIR writes into hasOnlyAir sections, including air variants.
            // Mixed sections still inspect cave_air/void_air instead of filtering all isAir states.
            if (cell == 0 && section.hasOnlyAir()) {
                nextSection(); return;
            }
            int local = cell++;
            int x = (chunkX << 4) + (local & 15);
            int y = (sectionY << 4) + (local >> 8);
            int z = (chunkZ << 4) + ((local >> 4) & 15);
            if (cell == 4096) nextSection();
            if (x < origin.getX() || x >= origin.getX() + SIZE || z < origin.getZ() || z >= origin.getZ() + SIZE
                    || y < origin.getY() || y >= origin.getY() + SIZE || y < level.getMinY() || y > level.getMaxY()) return;
            if (section.getBlockState(x & 15, y & 15, z & 15) == AIR || random.nextFloat() > .5f) return;
            pos.set(x, y, z);
            // Flags2 retain native inventory ejection and block side effects (unlike fill flags258).
            if (level.setBlock(pos, AIR, 2)) {
                int index = index(x - origin.getX(), y - origin.getY(), z - origin.getZ());
                placed.add(index); occupied.set(index);
            }
        }

        void nextSection() {
            cell = 0;
            if (++sectionY > maxSectionY) {
                sectionY = minSectionY;
                if (++chunkZ > (origin.getZ() + SIZE - 1) >> 4) { chunkZ = origin.getZ() >> 4; chunkX++; }
            }
        }

        void position(int index) {
            int x = index % SIZE, remainder = index / SIZE;
            pos.set(origin.getX() + x, origin.getY() + remainder / SIZE, origin.getZ() + remainder % SIZE);
        }

        /** Native StructureTemplate.updateShapeAtEdge, split at occupied-cell faces. */
        void edge() {
            if (follow == placed.size()) { phase = 2; follow = 0; return; }
            position(placed.getInt(follow));
            var direction = DIRECTIONS[face++];
            if (face == DIRECTIONS.length) { face = 0; follow++; }
            neighbor.setWithOffset(pos, direction);
            int x = neighbor.getX() - origin.getX(), y = neighbor.getY() - origin.getY(), z = neighbor.getZ() - origin.getZ();
            if (x >= 0 && x < SIZE && y >= 0 && y < SIZE && z >= 0 && z < SIZE && occupied.get(index(x, y, z))) return;
            var state = level.getBlockState(pos);
            var neighborState = level.getBlockState(neighbor);
            var updated = state.updateShape(level, level, pos, direction, neighbor, neighborState, level.getRandom());
            if (state != updated) level.setBlock(pos, updated, 2);
            var neighborUpdated = neighborState.updateShape(level, level, neighbor, direction.getOpposite(), pos, updated, level.getRandom());
            if (neighborState != neighborUpdated) level.setBlock(neighbor, neighborUpdated, 2);
        }

        void update() {
            if (follow == placed.size()) { phase = 3; return; }
            position(placed.getInt(follow++));
            var state = level.getBlockState(pos);
            var updated = Block.updateFromNeighbourShapes(state, level, pos);
            if (state != updated) level.setBlock(pos, updated, 18);
            level.updateNeighborsAt(pos, updated.getBlock());
        }

        void finish() {
            if (kind == LbcCorrosionDropItem.Kind.OLD && !level.isDebug())
                LbcDropBlocks.fill(level, center, center.above(), Blocks.AIR, true);
            if (kind == LbcCorrosionDropItem.Kind.OLD)
                level.sendParticles(ParticleTypes.LARGE_SMOKE, point.x, point.y, point.z, 5000, 1, 1, 1, 1);
            else level.sendParticles(DUST, point.x, point.y, point.z, 300, 1, 1, 1, 1);
            phase = 4;
        }

        @Override public void cancel() {
            ++inProgress;game.items().stonekivi().beginNativeWork();try {
                cancelled = true; placed.clear(); occupied.clear(); jobs.remove(this);
            }finally{game.items().stonekivi().endNativeWork();--inProgress;}
        }
    }

    void checkpointBoundary(){if(inProgress!=0)throw new IllegalStateException("Finish corrosion native callbacks before checkpointing");}
    Checkpoint checkpoint(MatchQueuedWorkCheckpoint.Graph graph){checkpointBoundary();return new Checkpoint(graph);}
    final class Checkpoint {
        private final HashSet<Job> saved;
        @SuppressWarnings("unchecked") Checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            saved=(HashSet<Job>)jobs.clone();saved.forEach(graph::capture);
        }
        @SuppressWarnings("unchecked") Runnable prepare(MatchSpellCheckpoint.Links links){
            checkpointBoundary();var restored=(HashSet<Job>)saved.clone();return ()->jobs=restored;
        }
    }

    private static int index(int x, int y, int z) { return (y * SIZE + z) * SIZE + x; }
}
