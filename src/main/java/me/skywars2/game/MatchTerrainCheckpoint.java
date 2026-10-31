package me.skywars2.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.skywars2.mixin.ArenaChunkAccess;
import me.skywars2.mixin.TerrainCheckpointTicksAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;

/**
 * Reusable terrain part of a match checkpoint. Native palette copies, full block-entity
 * NBT and exact scheduler entries are independent of later edits. This is deliberately
 * not the complete Vanity checkpoint: actors, core state and private ability queues
 * must be captured by its caller before that authority can be enabled.
 */
final class MatchTerrainCheckpoint {
    private record Key(ServerLevel level, ChunkPos pos) { }
    private record ChunkImage(Key key, LevelChunkSection[] sections,
                              Map<BlockPos, CompoundTag> entities,
                              List<ScheduledTick<Block>> blocks, long blockClock,
                              List<ScheduledTick<Fluid>> fluids, long fluidClock) { }

    private final MinecraftServer server;
    private final Map<Key, ChunkImage> images = new LinkedHashMap<>();
    private final MatchBlockEvents events = new MatchBlockEvents();
    private Restore active;

    MatchTerrainCheckpoint(MinecraftServer server) { this.server = server; }

    /** No getChunk/generation or native queue mutation while taking the snapshot. */
    static MatchTerrainCheckpoint captureLoaded(MinecraftServer server) {
        var result = new MatchTerrainCheckpoint(server);
        result.thread();
        for (var level : server.getAllLevels())
            for (var holder : ((ArenaChunkAccess) level.getChunkSource().chunkMap).skywars2$visibleChunks().values())
                if (holder.getLatestChunk() instanceof LevelChunk chunk) result.include(chunk);
        return result;
    }

    /** Include resident actor destinations explicitly, even before ChunkMap makes them visible. */
    static MatchTerrainCheckpoint captureLoaded(GameController game){
        var result=captureLoaded(game.server());
        for(var body:MatchActorCheckpoint.currentBodies(game).values()){
            if(body.isRemoved()||!(body.level() instanceof ServerLevel level))continue;
            var at=new ChunkPos(body.getBlockX() >> 4,body.getBlockZ() >> 4);var chunk=level.getChunkSource().getChunkNow(at.x(),at.z());
            if(chunk==null)throw new IllegalStateException("Prepare actor destinations before terrain capture");
            result.include(chunk);
        }
        return result;
    }

    /** Explicit extension point for a future checkpoint owner's chunk-load boundary. */
    void include(LevelChunk chunk) {
        thread();
        if (active != null) throw new IllegalStateException("Cannot extend an active terrain restore");
        if (!(chunk.getLevel() instanceof ServerLevel level) || level.getServer() != server)
            throw new IllegalArgumentException("Chunk belongs to another server");
        var key = new Key(level, chunk.getPos());
        if (images.containsKey(key)) return;
        events.include(level,chunk.getPos());
        var sections = new LevelChunkSection[chunk.getSectionsCount()];
        for (int i = 0; i < sections.length; i++) sections[i] = chunk.getSection(i).copy();
        var entities = new LinkedHashMap<BlockPos, CompoundTag>();
        for (var pos : chunk.getBlockEntitiesPos()) {
            var nbt = chunk.getBlockEntityNbtForSaving(pos, level.registryAccess());
            if (nbt != null) entities.put(pos.immutable(), nbt.copy());
        }
        var blocks = (LevelChunkTicks<Block>) chunk.getBlockTicks();
        var fluids = (LevelChunkTicks<Fluid>) chunk.getFluidTicks();
        long blockClock = LbcTimeStopSchedules.containerClock(blocks, level.getGameTime());
        long fluidClock = LbcTimeStopSchedules.containerClock(fluids, level.getGameTime());
        images.put(key, new ChunkImage(key, sections, Map.copyOf(entities),
                captureTicks(blocks, blockClock), blockClock, captureTicks(fluids, fluidClock), fluidClock));
    }

    @SuppressWarnings("unchecked")
    static <T> List<ScheduledTick<T>> captureTicks(LevelChunkTicks<T> container, long clock) {
        var result = new ArrayList<>(container.getAll().toList());
        var pending = ((TerrainCheckpointTicksAccess<T>) container).skywars2$pendingTicks();
        if (pending != null) {
            long order = -pending.size();
            for (var tick : pending) result.add(tick.unpack(clock, order++));
        }
        return List.copyOf(result);
    }

    int chunks() { return images.size(); }
    /** Reuse the match loader's existing resident tickets; map teardown owns their release. */
    void retain(GameController game){
        thread();for(var key:images.keySet())game.maps().prepareChunk(key.level,key.pos);
    }

    boolean ensureResident(GameController game){
        thread();boolean ready=true;
        for(var key:images.keySet()){
            game.maps().prepareChunk(key.level,key.pos);
            if(key.level.getChunkSource().getChunkNow(key.pos.x(),key.pos.z())!=null)continue;
            game.maps().prepareChunk(key.level,key.pos);ready=false;
        }
        return ready;
    }

    /**
     * Caller freezes native simulation, quarantines player/other writers, and keeps
     * chunks resident until completion. Native tick freeze alone still lets players
     * tick: this component does not pretend to own the entire match's input barrier.
     * Validate the entire set BEFORE the first write; never synchronously load missing
     * chunks from a half-restored world. The caller may rewind level gameTime first;
     * otherwise remaining tick delays are rebased to the current native timeline.
     */
    Restore restore(GameController game) {
        thread();
        if (game.server() != server) throw new IllegalArgumentException("Different checkpoint server");
        if (active != null) throw new IllegalStateException("Terrain restore already active");
        if (!server.tickRateManager().isFrozen()) throw new IllegalStateException("Freeze native simulation before restoring terrain");
        for (var image : images.values()) resident(image.key);
        active = new Restore(game, List.copyOf(images.values()));
        return active;
    }

    private LevelChunk resident(Key key) {
        var chunk = key.level.getChunkSource().getChunkNow(key.pos.x(), key.pos.z());
        if (chunk == null) throw new IllegalStateException("Checkpoint chunk is not resident: " + key.level.dimension() + " " + key.pos);
        return chunk;
    }

    private void thread() {
        if (!server.isSameThread()) throw new IllegalStateException("Checkpoint access requires server thread");
    }

    final class Restore implements AutoCloseable {
        private final GameController game;
        private final List<ChunkImage> work;
        private final Map<ServerLevel, MapLighting.Update> lighting = new LinkedHashMap<>();
        private final Map<ServerLevel, List<net.minecraft.world.level.chunk.ChunkAccess>> biomeChanges = new LinkedHashMap<>();
        private int chunkIndex, sectionIndex, cellIndex;
        private long visited, changed;
        private boolean writesDone, complete, closed;

        private Restore(GameController game, List<ChunkImage> work) { this.game = game; this.work = work; }
        long visited() { return visited; }
        long changed() { return changed; }
        boolean complete() { return complete; }

        /** Budget counts examined cells, including unchanged cells; no unbounded scan. */
        boolean step(int cellBudget) {
            thread();
            if (closed) throw new IllegalStateException("Closed terrain restore");
            if (cellBudget <= 0) throw new IllegalArgumentException("Positive cell budget required");
            if (!server.tickRateManager().isFrozen()) throw new IllegalStateException("Native simulation resumed during restore");
            if (complete) return true;
            int remaining = cellBudget;
            while (!writesDone && chunkIndex < work.size() && remaining > 0) {
                var image = work.get(chunkIndex);
                var chunk = resident(image.key);
                var level = image.key.level;
                var batch = lighting.computeIfAbsent(level, MapLighting.Update::new);
                var pos = new BlockPos.MutableBlockPos();
                while (sectionIndex < image.sections.length && remaining > 0) {
                    var original = image.sections[sectionIndex];
                    var current = chunk.getSection(sectionIndex);
                    // Empty palettes are overwhelmingly common in a void arena.
                    if (cellIndex == 0 && !original.maybeHas(s -> s != Blocks.AIR.defaultBlockState())
                            && !current.maybeHas(s -> s != Blocks.AIR.defaultBlockState())) {
                        sectionIndex++;
                        continue;
                    }
                    while (cellIndex < 4096 && remaining > 0) {
                        remaining--;
                        int x = cellIndex & 15, z = cellIndex >> 4 & 15, y = cellIndex >> 8;
                        cellIndex++; visited++;
                        var state = original.getBlockState(x, y, z);
                        if (current.getBlockState(x, y, z) == state) continue;
                        pos.set((chunk.getPos().x() << 4) + x, chunk.getSectionYFromSectionIndex(sectionIndex) * 16 + y,
                                (chunk.getPos().z() << 4) + z);
                        batch.changed(pos);
                        game.maps().restoreSnapshot(level, pos, new MapManager.BlockSnapshot(state, null),
                                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS
                                        | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS | Block.UPDATE_SKIP_ON_PLACE);
                        changed++;
                    }
                    if (cellIndex == 4096) { cellIndex = 0; sectionIndex++; }
                }
                if (sectionIndex == image.sections.length) {
                    finishChunk(image, chunk, batch);
                    sectionIndex = 0; chunkIndex++;
                }
            }
            if (!writesDone && chunkIndex == work.size()) {
                // Remove both old future ticks and ticks emitted by restoration callbacks.
                // Publish all saved queues only after ALL block writes have finished.
                for (var image : work) restoreTicks(image, resident(image.key));
                events.restore();
                for (var entry : biomeChanges.entrySet()) entry.getKey().getChunkSource().chunkMap.resendBiomesForChunks(entry.getValue());
                lighting.values().forEach(MapLighting.Update::finishWrites);
                writesDone = true;
            }
            if (!writesDone) return false;
            complete = true;
            for (var batch : lighting.values()) if (!batch.tick()) complete = false;
            if (complete) active = null;
            return complete;
        }

        private void finishChunk(ChunkImage image, LevelChunk chunk, MapLighting.Update batch) {
            var level = image.key.level;
            // Include same-state block entities: a chest inventory can change without a block write.
            for (var entry : image.entities.entrySet()) {
                var pos = entry.getKey();
                var state = chunk.getBlockState(pos);
                batch.changed(pos);
                var nbt = entry.getValue().copy();
                // Pending native block-entity NBT is valid even when no live object existed at capture.
                if (chunk.getBlockEntity(pos) == null) {
                    var restored = BlockEntity.loadStatic(pos, state, nbt, level.registryAccess());
                    if (restored == null) throw new IllegalStateException("Invalid saved checkpoint block entity at " + pos);
                    chunk.addAndRegisterBlockEntity(restored);
                } else {
                    game.maps().restoreSnapshot(level, pos, new MapManager.BlockSnapshot(state, nbt));
                }
                // Native BeaconBlockEntity writes Levels but does not read it. Rewind that
                // captured value here, in the terrain owner, before any current-data menu opens.
                if(chunk.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.BeaconBlockEntity beacon)
                    ((me.skywars2.mixin.BoundViewCheckpointAccess.BeaconBlock)beacon).skywars2$data().set(0,nbt.getIntOr("Levels",0));
                // Same-state NBT loads otherwise never enter ChunkHolder's update set
                // (sign text, banners, etc. can remain stale on an existing client).
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
            }
            boolean biomes = false;
            for (int i = 0; i < image.sections.length; i++) {
                var saved = image.sections[i];
                var current = chunk.getSection(i);
                boolean differs = false;
                for (int x = 0; x < 4 && !differs; x++) for (int y = 0; y < 4 && !differs; y++) for (int z = 0; z < 4; z++)
                    if (!saved.getNoiseBiome(x, y, z).equals(current.getNoiseBiome(x, y, z))) { differs = true; break; }
                if (differs) {
                    current.fillBiomesFromNoise((x, y, z) -> saved.getNoiseBiome(x & 3, y & 3, z & 3),
                            chunk.getPos().x() * 4, chunk.getSectionYFromSectionIndex(i) * 4, chunk.getPos().z() * 4);
                    biomes = true;
                }
            }
            if (biomes) biomeChanges.computeIfAbsent(level, ignored -> new ArrayList<>()).add(chunk);
            chunk.markUnsaved();
        }

        private void restoreTicks(ChunkImage image, LevelChunk chunk) {
            var level = image.key.level;
            var box = new BoundingBox(chunk.getPos().x() << 4, level.getMinY(), chunk.getPos().z() << 4,
                    (chunk.getPos().x() << 4) + 15, level.getMaxY(), (chunk.getPos().z() << 4) + 15);
            ((LevelChunkTicks<Block>) chunk.getBlockTicks()).unpack(level.getGameTime());
            ((LevelChunkTicks<Fluid>) chunk.getFluidTicks()).unpack(level.getGameTime());
            level.getBlockTicks().clearArea(box);
            level.getFluidTicks().clearArea(box);
            schedule(level.getBlockTicks(), image.blocks, image.blockClock, level.getGameTime());
            schedule(level.getFluidTicks(), image.fluids, image.fluidClock, level.getGameTime());
        }

        private <T> void schedule(LevelTicks<T> scheduler, Collection<ScheduledTick<T>> saved, long oldClock, long now) {
            for (var tick : saved) scheduler.schedule(new ScheduledTick<>(tick.type(), tick.pos(),
                    Math.addExact(now, Math.subtractExact(tick.triggerTick(), oldClock)), tick.priority(), tick.subTickOrder()));
        }

        /** Cancellation releases chunk-send gates; it does not claim a partially restored world is complete. */
        @Override public void close() {
            thread();
            if (closed) return;
            closed = true;
            lighting.values().forEach(MapLighting.Update::cancel);
            if (active == this) active = null;
        }
    }
}
