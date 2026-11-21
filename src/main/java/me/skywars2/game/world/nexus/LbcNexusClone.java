package me.skywars2.game.world.nexus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import me.skywars2.SkyWars2;
import me.skywars2.game.GameController;
import me.skywars2.game.combat.LbcIndirectKills;
import me.skywars2.game.entities.mobs.hedgehogs.LbcHedgehogDimensions;
import me.skywars2.game.items.wands.LbcStonekivi;
import me.skywars2.game.modes.bedwars.LbcBedwars;
import me.skywars2.game.world.maps.MapManager;
import me.skywars2.game.world.maps.NativeChunkLoad;
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
import me.skywars2.game.modes.bedwars.LbcBedwarsArena;

/** Registered Paper clonetonexus geometry, using the shared native construction barrier.
 * This service supplies no automatic producer; its caller owns the triggering event.
 */
public final class LbcNexusClone {
    public interface Completion {
        void completed(ServerLevel destination, int tx, int ty, int tz,
                       List<Vec3> centers, List<Vec3> trees, LbcIndirectKills.Owner cause);
    }

    private static final TicketType TICKET = new TicketType(TicketType.NO_TIMEOUT,
            TicketType.FLAG_LOADING | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;
    private static final int CELLS_PER_STEP = 256;
    private static final int LOAD_WAIT_TICKS = 600;
    private final GameController game;
    private final Completion completion;
    private final BiFunction<ServerLevel, ChunkPos, CompletableFuture<?>> chunkLoader;
    private final int loadWaitTicks;
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
        var future = chunkLoader.apply(level, chunk);
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

    public static void register() {
        Registry.register(BuiltInRegistries.TICKET_TYPE, Identifier.parse("skywars2:nexus_clone"), TICKET);
    }

    public LbcNexusClone(GameController game, Completion completion) {
        this(game, completion, (level, chunk) -> level.getChunkSource().addTicketAndLoadWithRadius(TICKET, chunk, 0),
                LOAD_WAIT_TICKS);
    }

    LbcNexusClone(GameController game, Completion completion, BiFunction<ServerLevel, ChunkPos, CompletableFuture<?>> chunkLoader,
                  int loadWaitTicks) {
        this.game = game;
        this.completion = completion;
        this.chunkLoader = chunkLoader;
        this.loadWaitTicks = loadWaitTicks;
    }

    private static String loadStatus(CompletableFuture<?> future) {
        return !future.isDone() ? "pending" : NativeChunkLoad.failed(future) ? "failed" : "loaded";
    }

    /** Server thread only. Deduplication lasts until this source chunk's operation completes. */
    public boolean request(ServerLevel source, Vec3 origin, LbcIndirectKills.Owner cause) {
        var destination = game.server().getLevel(LbcHedgehogDimensions.NEXUS);
        if (source == null || origin == null || destination == null || !game.playing()) return false;
        var position = BlockPos.containing(origin);
        var key = new Source(source, new ChunkPos(position.getX() >> 4, position.getZ() >> 4));
        if (jobs.containsKey(key)) return false;
        if (destination.getHeight() < source.getHeight()) return false;
        var target = pickTarget(key, destination);
        int sampledY = source.getMinY() + source.getRandom().nextInt(source.getHeight());
        // Preserve the source draw, then fit the entire logical volume before accepting writes.
        int ty = Math.max(destination.getMinY() + 8, Math.min(sampledY,
                destination.getMinY() + destination.getHeight() - source.getHeight() + 8));
        var job = new Job(key, destination, target, ty, cause);
        jobs.put(key, job);
        game.items().stonekivi().queuedWork(job);
        return true;
    }

    private ChunkPos pickTarget(Source source, ServerLevel destination) {
        var random = source.level.getRandom();
        Source target;
        do {
            target = new Source(destination, new ChunkPos(random.nextInt(625) - 312, random.nextInt(625) - 312));
        } while (target.equals(source) || loads.containsKey(target));
        return target.chunk;
    }

    public int pending() { return jobs.size(); }
    public List<String> pendingStatus() {
        return jobs.values().stream().map(job -> job.source.chunk + " " + loadStatus(job.sourceLoad)
                + " -> " + job.targetChunk + " " + loadStatus(job.destinationLoad)
                + " (" + job.copied + "/" + job.count + ")").toList();
    }
    List<View> snapshot() {
        return jobs.values().stream().map(job -> new View(job.source.level, job.source.chunk,
                job.destination, job.targetChunk, job.targetCell(0),
                job.count, job.copied)).toList();
    }
    boolean sourceInFlight(ServerLevel level, ChunkPos chunk) { return jobs.containsKey(new Source(level, chunk)); }
    public boolean captureReady() { return jobs.isEmpty(); }
    /** Work advances only in Stonekivi, so full-world capture sees the entire logical operation. */
    void tick() { }
    public void stop() { for (var job : List.copyOf(jobs.values())) job.cancel(); }

    private final class Job implements LbcStonekivi.Work {
        final Source source;
        final ServerLevel destination;
        ChunkPos targetChunk;
        final int ty, count;
        final LbcIndirectKills.Owner cause;
        final CompletableFuture<?> sourceLoad;
        CompletableFuture<?> destinationLoad;
        int copied, loadStartedAt;
        boolean started, finished, retriedDestination;

        Job(Source source, ServerLevel destination, ChunkPos targetChunk, int ty, LbcIndirectKills.Owner cause) {
            this.source = source;
            this.destination = destination;
            this.ty = ty;
            this.cause = cause;
            this.targetChunk = targetChunk;
            count = Math.multiplyExact(256, source.level.getHeight());
            sourceLoad = prepareChunk(source.level, source.chunk);
            destinationLoad = prepareChunk(destination, targetChunk);
            loadStartedAt = game.server().getTickCount();
        }

        @Override public boolean ready() {
            if (finished) return true;
            if (NativeChunkLoad.failed(sourceLoad) || NativeChunkLoad.failed(destinationLoad)) return true;
            if (game.items().timeStop().stopped(source.level) || game.items().timeStop().stopped(destination)) return false;
            return chunksReady() || !started && game.server().getTickCount() - loadStartedAt >= loadWaitTicks;
        }

        private boolean sourceReady() {
            return sourceLoad.isDone() && source.level.getChunkSource().getChunkNow(source.chunk.x(), source.chunk.z()) != null;
        }

        private boolean chunksReady() {
            return destinationLoad.isDone() && sourceReady()
                    && destination.getChunkSource().getChunkNow(targetChunk.x(), targetChunk.z()) != null;
        }

        /** The first target is still reserved while choosing a different column. */
        private boolean retryDestination() {
            if (retriedDestination || !sourceReady()) return abandon("load deadline exceeded");
            var next = pickTarget(source, destination);
            var nextLoad = prepareChunk(destination, next);
            SkyWars2.LOGGER.warn("Nexus transfer source {}: destination {} ({}) reached the load deadline; retrying at {}",
                    source.chunk, targetChunk, loadStatus(destinationLoad), next);
            releaseChunk(destination, targetChunk);
            targetChunk = next;
            destinationLoad = nextLoad;
            loadStartedAt = game.server().getTickCount();
            retriedDestination = true;
            return false;
        }

        private boolean abandon(String reason) {
            SkyWars2.LOGGER.warn("Nexus transfer cancelled before terrain writes ({}): source {} ({}), destination {} ({})",
                    reason, source.chunk, loadStatus(sourceLoad), targetChunk, loadStatus(destinationLoad));
            release();
            return true;
        }

        BlockPos sourceCell(int index) {
            return new BlockPos(source.chunk.getMinBlockX() + (index & 15),
                    source.level.getMinY() + (index >> 8), source.chunk.getMinBlockZ() + ((index >> 4) & 15));
        }

        BlockPos targetCell(int index) {
            return new BlockPos(targetChunk.getMinBlockX() + (index & 15), ty - 8 + (index >> 8),
                    targetChunk.getMinBlockZ() + ((index >> 4) & 15));
        }

        @Override public boolean step() {
            if (finished) return true;
            if (!started) {
                if (NativeChunkLoad.failed(sourceLoad) || NativeChunkLoad.failed(destinationLoad)) return abandon("native load failed");
                if (!chunksReady()) return retryDestination();
            }
            started = true;
            if (copied < count) {
                int end = Math.min(count, copied + CELLS_PER_STEP);
                var sourceChunk = source.level.getChunkSource().getChunkNow(source.chunk.x(), source.chunk.z());
                var destinationChunk = destination.getChunkSource().getChunkNow(targetChunk.x(), targetChunk.z());
                var air = Blocks.AIR.defaultBlockState();
                game.items().indirectKills().capturedWorldChanges(destination, cause, () -> {
                    while (copied < end) {
                        var from = sourceCell(copied);
                        var to = targetCell(copied);
                        // The empty columns of a SkyWars island still occupy most of
                        // the clone volume. Identical plain air needs no world write.
                        if (sourceChunk.getBlockState(from) == air && destinationChunk.getBlockState(to) == air) {
                            copied++;
                            continue;
                        }
                        var arena=game.bedwars().arena();
                        if(!arena.allowsRemoval(source.level,from)){copied++;continue;}
                        // One server-thread MOVE: inventory is never usable in both worlds between ticks.
                        var cell = game.maps().snapshot(source.level, from);
                        if(!arena.allowsType(destination,to,cell.state())){copied++;continue;}
                        game.maps().restoreSnapshot(destination, to, cell, FLAGS);
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
            int tx = targetChunk.getMinBlockX() + 8;
            int tz = targetChunk.getMinBlockZ() + 8;
            var delta = new Vec3(tx - 8 - source.chunk.getMinBlockX(),
                    ty - 8 - source.level.getMinY(), tz - 8 - source.chunk.getMinBlockZ());
            var box = new AABB(source.chunk.getMinBlockX(), source.level.getMinY(), source.chunk.getMinBlockZ(),
                    source.chunk.getMinBlockX() + 16, source.level.getMinY() + source.level.getHeight(), source.chunk.getMinBlockZ() + 16);
            // Position membership matches a source chunk; a large bounding box crossing its edge does not.
            var candidates = source.level.getEntities((Entity) null, box,
                    e -> !(e instanceof Player) && !(e instanceof Marker) && !LbcBedwars.shopRole(e) && box.contains(e.position()));
            var eligible = Collections.newSetFromMap(new IdentityHashMap<Entity, Boolean>());
            eligible.addAll(candidates);
            var families = new IdentityHashMap<Entity, Boolean>();
            // Native cross-dimension teleport carries every descendant, including players.
            // Keep a mixed or straddling riding family intact in the source dimension.
            var entities = candidates.stream().filter(entity -> families.computeIfAbsent(entity.getRootVehicle(),
                    root -> root.getSelfAndPassengers().allMatch(eligible::contains))).toList();
            record Transit(UUID id,Vec3 position,Vec3 motion,float yaw,float pitch){}
            var transits=entities.stream().map(e->new Transit(e.getUUID(),e.position(),e.getDeltaMovement(),e.getYRot(),e.getXRot())).toList();
            for (var entity : entities) {
                if (entity.isRemoved() || entity.level() != source.level || entity.isPassenger()) continue;
                entity.teleport(new TeleportTransition(destination, entity.position().add(delta),
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
                    int y = Math.max(destination.getMinY() + 1,
                            destination.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1);
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
