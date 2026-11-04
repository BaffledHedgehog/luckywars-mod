package me.skywars2.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Registered Paper clonetonexus geometry, using the shared native construction barrier.
 * This service supplies no automatic producer; its caller owns the triggering event.
 */
final class LbcNexusClone {
    interface Completion {
        void completed(ServerLevel destination, int tx, int ty, int tz,
                       List<Vec3> centers, List<Vec3> trees, LbcIndirectKills.Owner cause);
    }

    private static final TicketType TICKET = new TicketType(TicketType.NO_TIMEOUT,
            TicketType.FLAG_LOADING | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;
    private static final int CELLS_PER_STEP = 256;
    private final GameController game;
    private final Completion completion;
    private final Map<Source, Job> jobs = new HashMap<>();
    private final Map<Source, Load> loads = new HashMap<>();
    private record Source(ServerLevel level, ChunkPos chunk) { }
    record View(ServerLevel sourceLevel, ChunkPos sourceChunk, ServerLevel destination,
                ChunkPos destinationChunk, BlockPos targetMin, int count, int completed) { }
    private static final class Load {
        final CompletableFuture<?> future;
        int users = 1;
        Load(CompletableFuture<?> future) { this.future = future; }
    }

    private CompletableFuture<?> prepareChunk(ServerLevel level, ChunkPos chunk) {
        var key = new Source(level, chunk);
        var held = loads.get(key);
        if (held != null) { held.users++; return held.future; }
        var future = level.getChunkSource().addTicketAndLoadWithRadius(TICKET, chunk, 0);
        loads.put(key, new Load(future));
        return future;
    }

    private void releaseChunk(ServerLevel level, ChunkPos chunk) {
        var key = new Source(level, chunk);
        var held = loads.get(key);
        if (held != null && --held.users == 0) {
            level.getChunkSource().removeTicketWithRadius(TICKET, chunk, 0);
            loads.remove(key);
        }
    }

    static void register() {
        Registry.register(BuiltInRegistries.TICKET_TYPE, Identifier.parse("skywars2:nexus_clone"), TICKET);
    }

    LbcNexusClone(GameController game, Completion completion) {
        this.game = game;
        this.completion = completion;
    }

    /** Server thread only. Deduplication lasts until this source chunk's operation completes. */
    boolean request(ServerLevel source, Vec3 origin, LbcIndirectKills.Owner cause) {
        var destination = game.server().getLevel(LbcHedgehogDimensions.NEXUS);
        if (source == null || origin == null || destination == null || !game.playing()) return false;
        var position = BlockPos.containing(origin);
        var key = new Source(source, new ChunkPos(position.getX() >> 4, position.getZ() >> 4));
        if (jobs.containsKey(key)) return false;
        var random = source.getRandom();
        int tx, tz;
        do {
            tx = (random.nextInt(625) - 312) * 16 + 8;
            tz = (random.nextInt(625) - 312) * 16 + 8;
        } while (source == destination && key.chunk.x() == tx >> 4 && key.chunk.z() == tz >> 4);
        if (destination.getHeight() < source.getHeight()) return false;
        int sampledY = source.getMinY() + random.nextInt(source.getHeight());
        // Preserve the source draw, then fit the entire logical volume before accepting writes.
        int ty = Math.max(destination.getMinY() + 8, Math.min(sampledY,
                destination.getMinY() + destination.getHeight() - source.getHeight() + 8));
        var job = new Job(key, destination, tx, ty, tz, cause);
        jobs.put(key, job);
        game.items().stonekivi().queuedWork(job);
        return true;
    }

    int pending() { return jobs.size(); }
    List<View> snapshot() {
        return jobs.values().stream().map(job -> new View(job.source.level, job.source.chunk,
                job.destination, job.targetChunk, new BlockPos(job.tx - 8, job.ty - 8, job.tz - 8),
                job.count, job.copied)).toList();
    }
    boolean sourceInFlight(ServerLevel level, ChunkPos chunk) { return jobs.containsKey(new Source(level, chunk)); }
    boolean captureReady() { return jobs.isEmpty(); }
    /** Work advances only in Stonekivi, so full-world capture sees the entire logical operation. */
    void tick() { }
    void stop() { for (var job : List.copyOf(jobs.values())) job.cancel(); }

    private final class Job implements LbcStonekivi.Work {
        final Source source;
        final ServerLevel destination;
        final ChunkPos targetChunk;
        final int tx, ty, tz, count;
        final LbcIndirectKills.Owner cause;
        final CompletableFuture<?> sourceLoad, destinationLoad;
        int copied;
        boolean started, finished;

        Job(Source source, ServerLevel destination, int tx, int ty, int tz, LbcIndirectKills.Owner cause) {
            this.source = source;
            this.destination = destination;
            this.tx = tx;
            this.ty = ty;
            this.tz = tz;
            this.cause = cause;
            this.targetChunk = new ChunkPos(tx >> 4, tz >> 4);
            count = Math.multiplyExact(256, source.level.getHeight());
            sourceLoad = prepareChunk(source.level, source.chunk);
            destinationLoad = prepareChunk(destination, targetChunk);
        }

        @Override public boolean ready() {
            if (finished) return true;
            if (!sourceLoad.isDone() || !destinationLoad.isDone()) return false;
            if (sourceLoad.isCompletedExceptionally() || destinationLoad.isCompletedExceptionally()
                    || sourceLoad.isCancelled() || destinationLoad.isCancelled()) return true;
            return source.level.getChunkSource().getChunkNow(source.chunk.x(), source.chunk.z()) != null
                    && destination.getChunkSource().getChunkNow(targetChunk.x(), targetChunk.z()) != null
                    && !game.items().timeStop().stopped(source.level)
                    && !game.items().timeStop().stopped(destination);
        }

        BlockPos sourceCell(int index) {
            return new BlockPos(source.chunk.getMinBlockX() + (index & 15),
                    source.level.getMinY() + (index >> 8), source.chunk.getMinBlockZ() + ((index >> 4) & 15));
        }

        BlockPos targetCell(int index) {
            return new BlockPos(tx - 8 + (index & 15), ty - 8 + (index >> 8), tz - 8 + ((index >> 4) & 15));
        }

        @Override public boolean step() {
            if (finished) return true;
            if (!started && (sourceLoad.isCompletedExceptionally() || destinationLoad.isCompletedExceptionally()
                    || sourceLoad.isCancelled() || destinationLoad.isCancelled())) { release(); return true; }
            started = true;
            if (copied < count) {
                int end = Math.min(count, copied + CELLS_PER_STEP);
                game.items().indirectKills().capturedWorldChanges(destination, cause, () -> {
                    while (copied < end) {
                        var from = sourceCell(copied);
                        var arena=game.bedwars().arena();
                        if(!arena.allowsRemoval(source.level,from)){copied++;continue;}
                        // One server-thread MOVE: inventory is never usable in both worlds between ticks.
                        var cell = game.maps().snapshot(source.level, from);
                        if(!arena.allowsType(destination,targetCell(copied),cell.state())){copied++;continue;}
                        game.maps().restoreSnapshot(destination, targetCell(copied), cell, FLAGS);
                        game.items().indirectKills().capturedWorldChanges(source.level, cause, () ->
                                game.maps().restoreSnapshot(source.level, from, MapManager.BlockSnapshot.air(), FLAGS));
                        copied++;
                    }
                });
                return false;
            }
            try { complete(); } finally { release(); }
            return true;
        }

        private void complete() {
            var delta = new Vec3(tx - 8 - source.chunk.getMinBlockX(),
                    ty - 8 - source.level.getMinY(), tz - 8 - source.chunk.getMinBlockZ());
            var box = new AABB(source.chunk.getMinBlockX(), source.level.getMinY(), source.chunk.getMinBlockZ(),
                    source.chunk.getMinBlockX() + 16, source.level.getMinY() + source.level.getHeight(), source.chunk.getMinBlockZ() + 16);
            // Position membership matches a source chunk; a large bounding box crossing its edge does not.
            var entities = List.copyOf(source.level.getEntities((Entity) null, box,
                    e -> !(e instanceof Player) && !(e instanceof Marker) && !LbcBedwars.shopRole(e) && box.contains(e.position())));
            record Transit(UUID id,Vec3 position,Vec3 motion,float yaw,float pitch){}
            var transits=entities.stream().map(e->new Transit(e.getUUID(),e.position(),e.getDeltaMovement(),e.getYRot(),e.getXRot())).toList();
            for (var entity : entities) {
                if (entity.isRemoved() || entity.level() != source.level
                        || entity.isPassenger() && entities.contains(entity.getVehicle())) continue;
                var moved = entity.teleport(new TeleportTransition(destination, entity.position().add(delta),
                        entity.getDeltaMovement(), entity.getYRot(), entity.getXRot(), TeleportTransition.DO_NOTHING));
            }
            // Vanilla computes passenger offsets after dismount relocation. Restore the captured
            // group geometry before any next entity tick, retaining native bodies and riding links.
            for(var transit:transits){
                var moved=destination.getEntity(transit.id());
                if(moved==null||moved.isRemoved())continue;
                var point=transit.position().add(delta);
                moved.teleportTo(point.x,point.y,point.z);moved.setYRot(transit.yaw());moved.setXRot(transit.pitch());
                moved.setDeltaMovement(transit.motion());
                if(moved instanceof LivingEntity living)game.items().indirectKills().displaced(living,cause);
            }
            var centers = new ArrayList<Vec3>(3);
            game.items().indirectKills().capturedWorldChanges(destination, cause, () -> {
                for (int i = 0; i < 3; i++) {
                    int x = tx - 8 + destination.getRandom().nextInt(16);
                    int z = tz - 8 + destination.getRandom().nextInt(16);
                    // Bukkit highestBlockYAt is the highest occupied block, not the first air above it.
                    int y = destination.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
                    var support = new BlockPos(x, y - 1, z);
                    if (!destination.isOutsideBuildHeight(support) && !destination.getBlockState(support).isSolid())
                        game.maps().restoreSnapshot(destination, support,
                                new MapManager.BlockSnapshot(Blocks.CONCRETE.pick(DyeColor.PURPLE).defaultBlockState(), null), FLAGS);
                    centers.add(new Vec3(x + .5, y + .5, z + .5));
                }
            });
            var trees = new ArrayList<Vec3>(16);
            for (int i = 0; i < 16; i++) trees.add(new Vec3(tx + .5,
                    destination.getMinY() + (destination.getHeight() - 1) * i / 15.0 + .5, tz + .5));
            completion.completed(destination, tx, ty, tz, List.copyOf(centers), List.copyOf(trees), cause);
        }

        /** Before any writes cancellation is harmless; accepted mutations must finish as one structure. */
        @Override public void cancel() {
            if (finished) return;
            if (!started) { release(); return; }
            while (!step()) { /* Retained loaded chunks: shutdown needs no asynchronous wait. */ }
        }

        private void release() {
            if (finished) return;
            finished = true;
            releaseChunk(source.level, source.chunk);
            releaseChunk(destination, targetChunk);
            jobs.remove(source, this);
        }
    }
}
