package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Incremental native port of the legacy /magmify world transformation.
 * Bedwars bed-marker protection has no native contract in this project yet and is not applied here.
 */
public final class LbcMagmify {
    private static final int SPIKE_CHANCE_PERCENT = 1;
    private static final int SPIKE_MIN_LENGTH = 3;
    private static final int SPIKE_MAX_LENGTH = 10;
    private static final int CENTER_SAMPLE_SIZE = 30;
    private static final double SURFACE_RANGE = 1.0;
    private static final double SURFACE_VELOCITY = 3.0;
    private static final float SURFACE_DAMAGE = 40.0F;
    private static final int SURFACE_PARTICLES = 50;
    private static final int SURFACE_SLOWNESS_TICKS = 200;
    private static final int SURFACE_SLOWNESS_AMPLIFIER = 4;
    private static final int MAP_WRITE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;

    private static final List<BlockPos> FACE_NEIGHBORS = List.of(
            new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
            new BlockPos(0, 1, 0), new BlockPos(0, -1, 0),
            new BlockPos(0, 0, 1), new BlockPos(0, 0, -1));
    private static final List<BlockPos> SIDE_NEIGHBORS = List.of(
            new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
            new BlockPos(0, 0, 1), new BlockPos(0, 0, -1));
    private static final List<BlockPos> CONNECTED_26 = buildConnected26();
    private static final List<net.minecraft.world.level.block.Block> ORE_BLOCKS = collectOreBlocks();
    private static final List<net.minecraft.world.level.block.Block> STONE_BLOCKS = collectStoneBlocks();

    private LbcMagmify() { }
    static void prepare() { } // Registry-каталоги прогреваются при загрузке ресурсов, до первого солнца.

    public static LbcStonekivi.Work create(GameController game, ServerPlayer owner, BlockPos origin) {
        return new Work(game, owner, origin);
    }

    private static final class Work implements LbcStonekivi.Work {
        private final GameController game;
        private final ServerPlayer owner;
        private final ServerLevel level;
        private final int minBuildY;
        private final int maxBuildY;
        private final Map<BlockPos, Cell> captured = new HashMap<>();
        private final ArrayDeque<BlockPos> captureQueue = new ArrayDeque<>();
        private final Set<BlockPos> visited = new HashSet<>();
        private Phase phase = Phase.CAPTURE;
        private CompletableFuture<Plan> planFuture;
        private Plan plan;
        private java.util.Iterator<Map.Entry<BlockPos, net.minecraft.world.level.block.state.BlockState>> writeIterator;
        private java.util.Iterator<BlockPos> lavaIterator;
        private List<LivingEntity> entities;
        private int entityCursor;
        private boolean cancelled;

        private Work(GameController game, ServerPlayer owner, BlockPos origin) {
            this.game = game;
            this.owner = owner;
            this.level = owner.level();
            this.minBuildY = level.getMinY();
            this.maxBuildY = level.getMaxY()+1;
            BlockPos start = origin.immutable();
            if (!inBuildHeight(start) || !level.hasChunkAt(start)) {
                phase = Phase.FINISHED;
                return;
            }
            var state = level.getBlockState(start);
            if (!state.isAir()) {
                captured.put(start, new Cell(state, state.isSolidRender()));
                visited.add(start);
                captureQueue.addLast(start);
            } else {
                phase = Phase.FINISHED;
            }
        }

        @Override
        public boolean step() {
            if (cancelled || phase == Phase.FINISHED) return true;
            if (phase == Phase.CAPTURE) {
                captureOneNode();
                return false;
            }
            if (phase == Phase.PLAN_WAIT) {
                if (planFuture == null || !planFuture.isDone()) return false;
                try {
                    plan = planFuture.join();
                } catch (CompletionException | java.util.concurrent.CancellationException failure) {
                    phase = Phase.FINISHED;
                    return true;
                }
                planFuture = null;
                writeIterator = plan.replacements.entrySet().iterator();
                phase = Phase.WRITE;
                return false;
            }
            if (phase == Phase.WRITE) {
                writeOneCell();
                return false;
            }
            if (phase == Phase.LAVA_TICKS) {
                scheduleOneLavaTick();
                return false;
            }
            if (phase == Phase.IMPACT) {
                impactOneEntity();
                return false;
            }
            phase = Phase.FINISHED;
            return true;
        }

        @Override
        public boolean ready() {
            return phase == Phase.FINISHED || !cancelled && (phase != Phase.PLAN_WAIT || planFuture == null || planFuture.isDone());
        }

        @Override
        public void cancel() {
            cancelled = true;
            if (planFuture != null) planFuture.cancel(false);
            phase = Phase.FINISHED;
            captureQueue.clear();
            // The detached planner may still be reading captured; release the Work instead of mutating its snapshot.
            if (plan != null) plan = null;
            if (entities != null) entities.clear();
        }

        private void captureOneNode() {
            BlockPos current = captureQueue.pollFirst();
            if (current == null) {
                Map<BlockPos, Cell> detached = java.util.Collections.unmodifiableMap(captured);
                try {
                    planFuture = game.maps().computeFromSnapshot(() -> buildPlan(detached, minBuildY, maxBuildY));
                    phase = Phase.PLAN_WAIT;
                } catch (RuntimeException unavailable) {
                    phase = Phase.FINISHED;
                }
                return;
            }
            for (BlockPos offset : CONNECTED_26) {
                BlockPos next = current.offset(offset);
                if (!inBuildHeight(next) || !level.hasChunkAt(next) || !visited.add(next)) continue;
                var state = level.getBlockState(next);
                if (state.isAir()) continue;
                captured.put(next, new Cell(state, state.isSolidRender()));
                captureQueue.addLast(next);
            }
        }

        private void writeOneCell() {
            if (writeIterator.hasNext()) {
                var entry = writeIterator.next();
                BlockPos pos = entry.getKey();
                Cell original = captured.get(pos);
                if (!inBuildHeight(pos) || !level.hasChunkAt(pos)) return;
                var current = level.getBlockState(pos);
                // Connected cells are protected against edits made while the worker was planning.
                // Spike cells outside the capture are sampled and written in this same server step,
                // matching the legacy behavior of allowing a spike to replace its destination.
                if (original != null && !current.equals(original.state)) return;
                // The legacy operation consumes replaced containers. Writing an empty snapshot removes
                // the block entity without spawning its contents; restoring its old NBT would duplicate them.
                Runnable write=()->game.maps().restoreSnapshot(pos,new MapManager.BlockSnapshot(entry.getValue(),null));
                if(owner.level()==level) game.items().indirectKills().trapChanges(owner,write);
                else game.items().indirectKills().worldChanges(level,owner,write);
                return;
            }
            lavaIterator = plan.lavaPositions.iterator();
            phase = Phase.LAVA_TICKS;
        }

        private void scheduleOneLavaTick() {
            if (lavaIterator.hasNext()) {
                BlockPos pos = lavaIterator.next();
                if (level.hasChunkAt(pos) && level.getBlockState(pos).is(Blocks.LAVA)) {
                    level.scheduleTick(pos, Fluids.LAVA, 1);
                }
                return;
            }
            if (plan.surfaceBlocks.isEmpty()) {
                phase = Phase.FINISHED;
                return;
            }
            AABB query = new AABB(plan.minX - SURFACE_RANGE, plan.minY - SURFACE_RANGE, plan.minZ - SURFACE_RANGE,
                    plan.maxX + 1 + SURFACE_RANGE, plan.maxY + 1 + SURFACE_RANGE, plan.maxZ + 1 + SURFACE_RANGE);
            // One native chunk-indexed broad-phase query, then a tiny local BlockPos lookup per entity.
            entities = level.getEntitiesOfClass(LivingEntity.class, query, entity -> true);
            phase = Phase.IMPACT;
        }

        private void impactOneEntity() {
            if (entities == null || entityCursor >= entities.size()) {
                phase = Phase.FINISHED;
                return;
            }
            LivingEntity target = entities.get(entityCursor++);
            if (!target.isAlive() || target.isRemoved() || target.isSpectator()) return;
            if (target instanceof ServerPlayer player && game.isObserver(player)) return;
            if (!game.items().wands().target(owner, target)) return;
            AABB box = target.getBoundingBox();
            if (!touchesSurfaceWithinRange(box, plan.surfaceBlocks)) return;

            Vec3 center = box.getCenter();
            DamageSource source = new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                    .getOrThrow(DamageTypes.FALLING_BLOCK), owner);
            target.hurtServer(level, source, SURFACE_DAMAGE);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SURFACE_SLOWNESS_TICKS,
                    SURFACE_SLOWNESS_AMPLIFIER, false, true, true), owner);
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.IRON_NUGGET), true, false,
                    center.x, center.y, center.z, SURFACE_PARTICLES,
                    (box.maxX - box.minX) * .5, (box.maxY - box.minY) * .5, (box.maxZ - box.minZ) * .5, .001);
            Vec3 away = center.subtract(plan.center);
            if (away.lengthSqr() < 1.0e-6) away = new Vec3(0, 1, 0);
            target.setDeltaMovement(away.normalize().scale(SURFACE_VELOCITY));
            if(target.isAlive()) game.items().indirectKills().displaced(target,owner);
            if (target instanceof ServerPlayer player) {
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
            }
        }

        private boolean touchesSurfaceWithinRange(AABB box, Set<BlockPos> surface) {
            int minX = (int) Math.floor(box.minX - SURFACE_RANGE);
            int minY = (int) Math.floor(box.minY - SURFACE_RANGE);
            int minZ = (int) Math.floor(box.minZ - SURFACE_RANGE);
            int maxX = (int) Math.floor(box.maxX + SURFACE_RANGE);
            int maxY = (int) Math.floor(box.maxY + SURFACE_RANGE);
            int maxZ = (int) Math.floor(box.maxZ + SURFACE_RANGE);
            for (int x = minX; x <= maxX; x++) for (int y = minY; y <= maxY; y++) for (int z = minZ; z <= maxZ; z++) {
                BlockPos pos = new BlockPos(x, y, z);
                if (!surface.contains(pos)) continue;
                double dx = axisDistance(box.minX, box.maxX, x, x + 1.0);
                double dy = axisDistance(box.minY, box.maxY, y, y + 1.0);
                double dz = axisDistance(box.minZ, box.maxZ, z, z + 1.0);
                if (dx * dx + dy * dy + dz * dz <= SURFACE_RANGE * SURFACE_RANGE) return true;
            }
            return false;
        }

        private boolean inBuildHeight(BlockPos pos) {
            return pos.getY() >= minBuildY && pos.getY() < maxBuildY;
        }
    }

    private enum Phase { CAPTURE, PLAN_WAIT, WRITE, LAVA_TICKS, IMPACT, FINISHED }

    private record Cell(net.minecraft.world.level.block.state.BlockState state, boolean solidRender) { }

    private record Plan(
            Map<BlockPos, net.minecraft.world.level.block.state.BlockState> replacements,
            Set<BlockPos> lavaPositions,
            Set<BlockPos> surfaceBlocks,
            int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ,
            Vec3 center) { }

    private static Plan buildPlan(Map<BlockPos, Cell> captured, int minBuildY, int maxBuildY) {
        Map<BlockPos, net.minecraft.world.level.block.state.BlockState> replacements = new HashMap<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (var entry : captured.entrySet()) {
            BlockPos pos = entry.getKey();
            var original = entry.getValue().state;
            net.minecraft.world.level.block.state.BlockState replacement = null;
            if (surroundedBySolid(captured, pos)) replacement = Blocks.LAVA.defaultBlockState();
            else if (hasNoFaceConnections(captured, pos)) replacement = Blocks.MAGMA_BLOCK.defaultBlockState();
            else if (!captured.containsKey(pos.below())) replacement = Blocks.REINFORCED_DEEPSLATE.defaultBlockState();
            else if (hasFreeSide(captured, pos)) replacement = randomState(ORE_BLOCKS, Blocks.DIAMOND_ORE, random);
            else if (!captured.containsKey(pos.above())) replacement = randomState(STONE_BLOCKS, Blocks.STONE, random);
            if (replacement != null && !replacement.equals(original)) replacements.put(pos, replacement);
        }

        planLavaSpikes(captured, replacements, minBuildY, maxBuildY, random);
        Set<BlockPos> lavaPositions = new HashSet<>();
        replacements.forEach((pos, state) -> { if (state.is(Blocks.LAVA)) lavaPositions.add(pos); });

        Set<BlockPos> surface = new HashSet<>();
        ArrayList<BlockPos> centerSample = new ArrayList<>(CENTER_SAMPLE_SIZE);
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        double sumX = 0, sumY = 0, sumZ = 0;
        int seen = 0;
        for (BlockPos pos : captured.keySet()) {
            minX = Math.min(minX, pos.getX()); minY = Math.min(minY, pos.getY()); minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX()); maxY = Math.max(maxY, pos.getY()); maxZ = Math.max(maxZ, pos.getZ());
            seen++;
            if (centerSample.size() < CENTER_SAMPLE_SIZE) centerSample.add(pos);
            else {
                int index = random.nextInt(seen);
                if (index < CENTER_SAMPLE_SIZE) centerSample.set(index, pos);
            }
            if (!surroundedByCapturedFaces(captured, pos)) surface.add(pos);
        }
        for (BlockPos pos : centerSample) { sumX += pos.getX() + .5; sumY += pos.getY() + .5; sumZ += pos.getZ() + .5; }
        Vec3 center = new Vec3(sumX / centerSample.size(), sumY / centerSample.size(), sumZ / centerSample.size());
        return new Plan(Map.copyOf(replacements), Set.copyOf(lavaPositions), Set.copyOf(surface),
                minX, minY, minZ, maxX, maxY, maxZ, center);
    }

    private static void planLavaSpikes(Map<BlockPos, Cell> captured,
                                       Map<BlockPos, net.minecraft.world.level.block.state.BlockState> replacements,
                                       int minY, int maxY, ThreadLocalRandom random) {
        for (BlockPos pos : captured.keySet()) {
            List<BlockPos> occupied = occupiedFaces(captured, pos);
            if (occupied.size() < 3 || occupied.size() >= 6 || random.nextInt(100) >= SPIKE_CHANCE_PERCENT) continue;
            List<BlockPos> free = freeFaces(captured, pos);
            BlockPos direction = selectSpikeDirection(occupied, free, random);
            if (direction == null || direction.equals(BlockPos.ZERO)) continue;
            int length = random.nextInt(SPIKE_MIN_LENGTH, SPIKE_MAX_LENGTH + 1);
            for (int step = 0; step < length; step++) {
                BlockPos spike = pos.offset(direction.getX() * step, direction.getY() * step, direction.getZ() * step);
                if (spike.getY() < minY || spike.getY() >= maxY) break;
                replacements.put(spike, Blocks.LAVA.defaultBlockState());
            }
        }
    }

    private static boolean surroundedBySolid(Map<BlockPos, Cell> captured, BlockPos pos) {
        for (BlockPos offset : FACE_NEIGHBORS) {
            Cell cell = captured.get(pos.offset(offset));
            if (cell == null || !cell.solidRender) return false;
        }
        return true;
    }
    private static boolean surroundedByCapturedFaces(Map<BlockPos, Cell> captured, BlockPos pos) {
        for (BlockPos offset : FACE_NEIGHBORS) if (!captured.containsKey(pos.offset(offset))) return false;
        return true;
    }
    private static boolean hasNoFaceConnections(Map<BlockPos, Cell> captured, BlockPos pos) {
        for (BlockPos offset : FACE_NEIGHBORS) if (captured.containsKey(pos.offset(offset))) return false;
        return true;
    }
    private static boolean hasFreeSide(Map<BlockPos, Cell> captured, BlockPos pos) {
        for (BlockPos offset : SIDE_NEIGHBORS) if (!captured.containsKey(pos.offset(offset))) return true;
        return false;
    }
    private static List<BlockPos> occupiedFaces(Map<BlockPos, Cell> captured, BlockPos pos) {
        List<BlockPos> result = new ArrayList<>(6);
        for (BlockPos offset : FACE_NEIGHBORS) if (captured.containsKey(pos.offset(offset))) result.add(offset);
        return result;
    }
    private static List<BlockPos> freeFaces(Map<BlockPos, Cell> captured, BlockPos pos) {
        List<BlockPos> result = new ArrayList<>(6);
        for (BlockPos offset : FACE_NEIGHBORS) if (!captured.containsKey(pos.offset(offset))) result.add(offset);
        return result;
    }
    private static BlockPos selectSpikeDirection(List<BlockPos> occupied, List<BlockPos> free, ThreadLocalRandom random) {
        return switch (occupied.size()) {
            case 5 -> free.isEmpty() ? null : free.getFirst();
            case 4 -> {
                if (free.size() < 2) yield free.isEmpty() ? null : free.getFirst();
                BlockPos first = free.get(0), second = free.get(1);
                if (first.getX() != -second.getX() || first.getY() != -second.getY() || first.getZ() != -second.getZ())
                    yield signVector(first.getX() + second.getX(), first.getY() + second.getY(), first.getZ() + second.getZ());
                yield free.get(random.nextInt(free.size()));
            }
            case 3 -> {
                int x = 0, y = 0, z = 0;
                for (BlockPos face : occupied) { x -= face.getX(); y -= face.getY(); z -= face.getZ(); }
                BlockPos preferred = signVector(x, y, z);
                if (!preferred.equals(BlockPos.ZERO)) yield preferred;
                yield free.isEmpty() ? null : free.get(random.nextInt(free.size()));
            }
            default -> null;
        };
    }
    private static BlockPos signVector(int x, int y, int z) {
        return new BlockPos(Integer.signum(x), Integer.signum(y), Integer.signum(z));
    }
    private static net.minecraft.world.level.block.state.BlockState randomState(
            List<net.minecraft.world.level.block.Block> blocks,
            net.minecraft.world.level.block.Block fallback,
            ThreadLocalRandom random) {
        return blocks.isEmpty() ? fallback.defaultBlockState() : blocks.get(random.nextInt(blocks.size())).defaultBlockState();
    }
    private static List<net.minecraft.world.level.block.Block> collectOreBlocks() {
        List<net.minecraft.world.level.block.Block> result = new ArrayList<>();
        for (var block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
            if (block.defaultBlockState().isSolid() && id.getPath().toUpperCase(java.util.Locale.ROOT).contains("ORE")) result.add(block);
        }
        return List.copyOf(result);
    }
    private static List<net.minecraft.world.level.block.Block> collectStoneBlocks() {
        List<net.minecraft.world.level.block.Block> result = new ArrayList<>();
        for (var block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            var state = block.defaultBlockState();
            var path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath().toUpperCase(java.util.Locale.ROOT);
            if (!state.isSolid() || !state.canOcclude() || !path.contains("STONE") || path.contains("ORE") || path.startsWith("INFESTED_")) continue;
            if (path.endsWith("SLAB") || path.endsWith("STAIRS") || path.endsWith("WALL") || path.endsWith("BUTTON") || path.endsWith("PRESSURE_PLATE")) continue;
            if (block == Blocks.STONECUTTER || block == Blocks.GRINDSTONE || block == Blocks.LODESTONE || block == Blocks.REDSTONE_BLOCK) continue;
            result.add(block);
        }
        return List.copyOf(result);
    }
    private static List<BlockPos> buildConnected26() {
        List<BlockPos> result = new ArrayList<>(26);
        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++)
            if (x != 0 || y != 0 || z != 0) result.add(new BlockPos(x, y, z));
        return List.copyOf(result);
    }
    private static double axisDistance(double aMin, double aMax, double bMin, double bMax) {
        if (aMax < bMin) return bMin - aMax;
        if (bMax < aMin) return aMin - bMax;
        return 0.0;
    }
}
