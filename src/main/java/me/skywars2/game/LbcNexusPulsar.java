package me.skywars2.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Accepted, detached Pulsar waves. Terrain itself belongs to the native terrain checkpoint. */
public final class LbcNexusPulsar {
    private static final TicketType TICKET = new TicketType(0,
            TicketType.FLAG_LOADING | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    private static final int[][] NEIGHBORS = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    private static final BlockState[] COLORS = {Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.WHITE).defaultBlockState(),
            Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.LIME).defaultBlockState(), Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.RED).defaultBlockState(),
            Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLACK).defaultBlockState(), Blocks.AIR.defaultBlockState()};

    /** Supplied by admission's owner; evaluated live at writes and neighbor admission. */
    @FunctionalInterface
    interface CellPolicy { boolean allows(ServerLevel level, BlockPos pos); }

    private record Chunk(ServerLevel level, ChunkPos pos) { }
    private static final class Loading {
        CompletableFuture<?> future;
        int users;
        Loading(CompletableFuture<?> future) { this.future = future; }
    }

    private static final class Wave {
        final int phase;
        final int delay;
        List<BlockPos> current = new ArrayList<>();
        List<BlockPos> next = new ArrayList<>();
        final LongOpenHashSet visited = new LongOpenHashSet();
        int index;
        int step;
        long nextLayerAge;
        boolean done;
        Chunk waiting;

        Wave(int phase, int delay, BlockPos origin) {
            this.phase = phase;
            this.delay = delay;
            current.add(origin.immutable());
            // Source deliberately does not mark its unconditional seed as visited.
        }

        Wave(Wave source) {
            phase = source.phase;
            delay = source.delay;
            current = new ArrayList<>(source.current);
            next = new ArrayList<>(source.next);
            visited.addAll(source.visited);
            index = source.index;
            step = source.step;
            nextLayerAge = source.nextLayerAge;
            done = source.done;
        }
    }

    private static final class Pulse {
        final ServerLevel level;
        final LbcIndirectKills.Owner owner;
        final List<Wave> waves = new ArrayList<>(5);
        long age = -1;
        int cursor;
        Pulse(ServerLevel level, BlockPos origin, LbcIndirectKills.Owner owner, int[] delays) {
            this.level = level;
            this.owner = owner;
            for (int phase = 0; phase < COLORS.length; phase++) waves.add(new Wave(phase, delays[phase], origin));
        }
        Pulse(Pulse source) {
            level = source.level;
            owner = source.owner;
            age = source.age;
            cursor = source.cursor;
            for (var wave : source.waves) waves.add(new Wave(wave));
        }
        boolean done() { return waves.stream().allMatch(w -> w.done); }
    }

    private final GameController game;
    private final CellPolicy policy;
    private final ArrayDeque<Pulse> pulses = new ArrayDeque<>();
    private final Map<Chunk, Loading> chunks = new HashMap<>();
    // Derived frontier ownership, never serialized or retained for visited-only cells.
    private final Map<Chunk, Integer> pending = new HashMap<>();
    private int lastTick = -1;
    private boolean restoring;

    LbcNexusPulsar(GameController game, CellPolicy policy) {
        this.game = java.util.Objects.requireNonNull(game);
        this.policy = java.util.Objects.requireNonNull(policy);
    }

    public static void register() {
        Registry.register(BuiltInRegistries.TICKET_TYPE, Identifier.parse("skywars2:nexus_pulsar"), TICKET);
    }

    /** Delays are independent start deadlines, in active world ticks, supplied by the caller. */
    void start(ServerLevel level, BlockPos origin, LbcIndirectKills.Owner owner, int[] delays) {
        if (restoring) return;
        if (delays.length != COLORS.length) throw new IllegalArgumentException("Pulsar needs five delays");
        for (int delay : delays) if (delay < 0) throw new IllegalArgumentException("Negative Pulsar delay");
        var pulse = new Pulse(level, origin, owner, delays);
        pulses.addLast(pulse);
        for (var wave : pulse.waves) retainCell(level, wave.current.getFirst());
    }

    static boolean eligible(int phase, BlockState state) {
        if (state.isAir()) return false;
        return switch (phase) {
            case 0 -> !state.is(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.WHITE)) && !state.is(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.LIME))
                    && !state.is(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.RED)) && !state.is(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLACK));
            case 1 -> !state.is(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.LIME)) && !state.is(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.RED))
                    && !state.is(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLACK));
            case 2 -> !state.is(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.RED)) && !state.is(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLACK));
            case 3 -> !state.is(Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLACK));
            case 4 -> true;
            default -> throw new IllegalArgumentException("Unknown Pulsar phase");
        };
    }

    private static Chunk chunk(ServerLevel level, BlockPos pos) {
        return new Chunk(level, new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4));
    }

    private void retainCell(ServerLevel level, BlockPos pos) {
        pending.merge(chunk(level, pos), 1, Integer::sum);
    }

    private void consumeCell(ServerLevel level, BlockPos pos) {
        var key = chunk(level, pos);
        int count = pending.get(key);
        if (count == 1) pending.remove(key);
        else pending.put(key, count - 1);
        releaseUnused(key);
    }

    private void releaseUnused(Chunk key) {
        var loading = chunks.get(key);
        if (loading != null && loading.users == 0 && !pending.containsKey(key)) {
            key.level().getChunkSource().removeTicketWithRadius(TICKET, key.pos(), 0);
            chunks.remove(key);
        }
    }

    private void release(Wave wave) {
        var key = wave.waiting;
        if (key == null) return;
        wave.waiting = null;
        var loading = chunks.get(key);
        if (loading != null) loading.users--;
        releaseUnused(key);
    }

    /** A read/write is performed immediately after readiness, on the server thread. */
    private boolean resident(ServerLevel level, BlockPos pos, Wave wave) {
        var key = chunk(level, pos);
        if (wave.waiting != null && !wave.waiting.equals(key)) release(wave);
        if (wave.waiting == null) {
            var loading = chunks.computeIfAbsent(key,
                    k -> new Loading(level.getChunkSource().addTicketAndLoadWithRadius(TICKET, k.pos(), 0)));
            loading.users++;
            wave.waiting = key;
        }
        var loading = chunks.get(key);
        // Existing resident terrain can be used immediately while the retaining ticket settles.
        if (level.getChunkSource().getChunkNow(key.pos().x(), key.pos().z()) != null) return true;
        if (loading.future.isCompletedExceptionally() || loading.future.isCancelled()) {
            // Preserve other probe/frontier references while replacing a failed request.
            level.getChunkSource().removeTicketWithRadius(TICKET, key.pos(), 0);
            loading.future = level.getChunkSource().addTicketAndLoadWithRadius(TICKET, key.pos(), 0);
            release(wave);
            return false;
        }
        return loading.future.isDone() && level.getChunkSource().getChunkNow(key.pos().x(), key.pos().z()) != null;
    }

    /** At most one admitted node; a blocked neighbor resumes at its exact source-order cursor. */
    private boolean node(Pulse pulse, Wave wave) {
        var level = pulse.level;
        var pos = wave.current.get(wave.index);
        boolean progressed = false;
        if (wave.step == 0) {
            if (!level.isOutsideBuildHeight(pos) && policy.allows(level, pos)) {
                if (!resident(level, pos, wave)) return false;
                if (eligible(wave.phase, level.getBlockState(pos))) {
                    game.items().indirectKills().capturedWorldChanges(level, pulse.owner,
                            () -> LbcDropBlocks.setBlock(level, pos, COLORS[wave.phase], true));
                }
            }
            release(wave);
            wave.step = 1;
            progressed = true;
        }
        // Admitted nodes probe neighbors even when their own live material no longer qualifies.
        while (wave.step <= NEIGHBORS.length) {
            var offset = NEIGHBORS[wave.step - 1];
            var neighbor = pos.offset(offset[0], offset[1], offset[2]);
            if (!level.isOutsideBuildHeight(neighbor) && policy.allows(level, neighbor)) {
                if (!resident(level, neighbor, wave)) return progressed;
                // Test live eligibility before adding to visited, as in the registered command.
                if (eligible(wave.phase, level.getBlockState(neighbor)) && wave.visited.add(neighbor.asLong())) {
                    wave.next.add(neighbor);
                    retainCell(level, neighbor);
                }
            }
            release(wave);
            wave.step++;
            progressed = true;
        }
        wave.step = 0;
        consumeCell(level, pos);
        if (++wave.index == wave.current.size()) {
            wave.current = wave.next;
            wave.next = new ArrayList<>();
            wave.index = 0;
            wave.nextLayerAge = pulse.age + 1;
            wave.done = wave.current.isEmpty();
        }
        return progressed;
    }

    private boolean service(Pulse pulse) {
        if (game.items().timeStop().stopped(pulse.level)) return false;
        for (int tried = 0; tried < COLORS.length; tried++) {
            var wave = pulse.waves.get(pulse.cursor);
            pulse.cursor = (pulse.cursor + 1) % COLORS.length;
            if (!wave.done && pulse.age >= wave.delay && pulse.age >= wave.nextLayerAge) {
                if (node(pulse, wave)) return true;
            }
        }
        return false;
    }

    public void tick() {
        int tick = game.server().getTickCount();
        if (restoring || lastTick == tick) return;
        lastTick = tick;
        if (!game.playing() || game.stage() == GameController.Stage.ENDING
                || !game.server().tickRateManager().runsNormally() || game.items().wands().legacyTimeStopped()) return;
        for (var pulse : pulses) if (!game.items().timeStop().stopped(pulse.level)) pulse.age++;
        long now = System.nanoTime();
        long deadline = Math.max(now + 200_000, Math.min(EffectTerrain.deadlineNanos(), now + 2_000_000));
        int work = 0, idle = 0;
        // This cap only schedules work; it never truncates a component, layer, lifetime or visited set.
        while (!pulses.isEmpty() && work < 2048 && (work < 4 || System.nanoTime() < deadline)) {
            var pulse = pulses.removeFirst();
            boolean progressed = service(pulse);
            work++;
            if (!pulse.done()) pulses.addLast(pulse);
            if (progressed) idle = 0;
            else if (++idle >= pulses.size()) break;
        }
    }

    private void clear() {
        pulses.clear();
        for (var key : chunks.keySet())
            key.level().getChunkSource().removeTicketWithRadius(TICKET, key.pos(), 0);
        chunks.clear();
        pending.clear();
    }

    public void stop() { clear(); }
    void prepareFrozen() { restoring = true; clear(); }
    void publishFrozen() { restoring = false; lastTick = game.server().getTickCount(); }
    Checkpoint checkpoint() { return new Checkpoint(); }

    final class Checkpoint {
        private final List<Pulse> images = pulses.stream().map(Pulse::new).toList();
        MatchFieldCheckpoint.Plan prepare(Function<UUID, Entity> lookup) {
            // No body or online owner is required: accepted effects survive their source player's exit.
            var restored = images.stream().map(Pulse::new).toList();
            return new MatchFieldCheckpoint.Plan(Set.of(), () -> {
                clear();
                pulses.addAll(restored);
                for (var pulse : restored) for (var wave : pulse.waves) {
                    for (int i = wave.index; i < wave.current.size(); i++)
                        retainCell(pulse.level, wave.current.get(i));
                    for (var pos : wave.next) retainCell(pulse.level, pos);
                }
            });
        }
        int count() { return images.size(); }
    }

    int count() { return pulses.size(); }
    int loadingChunks() { return chunks.size(); }
    int pendingChunks() { return pending.size(); }
    long age() { return pulses.isEmpty() ? -1 : pulses.peekFirst().age; }
    int frontierSize(int phase) {
        int count = 0;
        for (var pulse : pulses) {
            var wave = pulse.waves.get(phase);
            count += wave.current.size() - wave.index + wave.next.size();
        }
        return count;
    }
    int visitedSize(int phase) {
        int count = 0;
        for (var pulse : pulses) count += pulse.waves.get(phase).visited.size();
        return count;
    }
}
