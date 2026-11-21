package me.skywars2.game;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.BiFunction;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.ArenaChunkAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Compares sparse resident ray traversal with the exact original native block walk. */
public final class ResidentBlockRayChecks {
    private static final TagKey<Block> AIRS = TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace("airs"));

    private ResidentBlockRayChecks() {}

    public static void run(GameController game) {
        var level = game.maps().level();
        require(!level.isDebug(), "ordinary native arena level");
        var base = emptyColumn(level);
        var saved = new LinkedHashMap<BlockPos, MapManager.BlockSnapshot>();
        try {
            var from = Vec3.atLowerCornerOf(base).add(.5, .5, .5);
            var far = from.add(0, 94, 0);
            var terminal = base.above(80);
            var caveAir = base.above(22);
            var offAxis = base.offset(2, 19, 0);
            set(game, saved, offAxis, Blocks.STONE);
            set(game, saved, caveAir, Blocks.CAVE_AIR);
            set(game, saved, terminal, Blocks.STONE);
            require(level.getBlockState(caveAir).is(Blocks.CAVE_AIR), "mixed section exposes the native cave-air state");
            int[] visits = new int[2];
            var old = vanilla(level, from, far, (pos, state) -> {
                visits[0]++;
                return wall(pos, state);
            });
            var sparse = ResidentBlockRay.clip(level, from, far, (pos, state) -> {
                visits[1]++;
                return wall(pos, state);
            }, true, true);
            equal(old, sparse, "resident mixed and empty section corridor");
            require(old != null && old.getBlockPos().equals(terminal)
                    && visits[0] >= 80 && visits[1] * 3 <= visits[0],
                    "native empty-section travel reduces checked cells with same final hit");
            checkBoth(level, from, far);
            var variant = LbcTerra.wallHit(level, from, far, state -> !state.is(Blocks.AIR));
            require(variant != null && variant.getBlockPos().equals(caveAir),
                    "custom wall predicate keeps its distinction between cave air and normal air");
            var marker = base.above(43);
            var positionRule = LbcTerra.shapeHit(level, from, far,
                    (pos, state) -> pos.equals(marker) ? Shapes.block() : Shapes.empty());
            require(positionRule != null && positionRule.getBlockPos().equals(marker),
                    "position-sensitive shape still runs in the empty section");

            // Same ray right after terrain mutations, with full-block versus shaped collision.
            var slab = base.above(36);
            set(game, saved, slab, Blocks.STONE_SLAB);
            checkBoth(level, from, far);
            checkBoth(level, from.add(0, 36, 0), far);
            checkBoth(level, far, from);
            set(game, saved, slab, Blocks.AIR);
            checkBoth(level, from, far);
            compareRays(level, base);
            worldHeight(game, saved, base);
            coldBoundary(level);

            SkyWars2.LOGGER.info("[check] PASS resident-block-rays (native oracle, mixed air, corners, shape, height and cold boundary; corridor shape probes {} -> {})",
                    visits[0], visits[1]);
        } finally {
            saved.forEach((pos, snapshot) -> game.maps().restoreSnapshot(level, pos, snapshot));
        }
    }

    private static void compareRays(ServerLevel level, BlockPos base) {
        var corner = new Vec3((base.getX() >> 4 << 4) + 16, base.getY() + 32, (base.getZ() >> 4 << 4) + 16);
        for (var delta : new Vec3[] {new Vec3(9, 9, 9), new Vec3(-9, 9, -9), new Vec3(9, -9, 0), new Vec3(9, 0, 9)}) {
            checkBoth(level, corner.subtract(delta), corner.add(delta));
            checkBoth(level, corner.add(delta), corner.subtract(delta));
            checkBoth(level, corner, corner.add(delta.scale(2)));
            checkBoth(level, corner.subtract(delta.scale(2)), corner);
        }
        var random = new Random(817325971);
        var origin = Vec3.atLowerCornerOf(base);
        for (int i = 0; i < 192; i++) {
            var from = origin.add(random.nextDouble(-8, 8), random.nextDouble(0, 96), random.nextDouble(-8, 8));
            var to = origin.add(random.nextDouble(-8, 8), random.nextDouble(0, 96), random.nextDouble(-8, 8));
            checkBoth(level, from, to);
        }
    }

    private static void worldHeight(GameController game, Map<BlockPos, MapManager.BlockSnapshot> saved, BlockPos base) {
        var level = game.maps().level();
        var roof = new BlockPos(base.getX(), level.getMaxY(), base.getZ());
        var floor = new BlockPos(base.getX(), level.getMinY(), base.getZ());
        set(game, saved, roof, Blocks.OAK_FENCE);
        set(game, saved, floor, Blocks.STONE);
        var top = Vec3.atCenterOf(roof);
        var bottom = Vec3.atCenterOf(floor);
        checkBoth(level, top.add(0, 33, 0), top.subtract(0, 4, 0));
        checkBoth(level, top.subtract(0, 4, 0), top.add(0, 33, 0));
        checkBoth(level, bottom.subtract(0, 33, 0), bottom.add(0, 4, 0));
        checkBoth(level, bottom.add(0, 4, 0), bottom.subtract(0, 33, 0));
    }

    private static void coldBoundary(ServerLevel level) {
        var source = level.getChunkSource();
        var visible = ((ArenaChunkAccess) source.chunkMap).skywars2$visibleChunks();
        for (var holder : visible.values()) {
            if (!(holder.getLatestChunk() instanceof LevelChunk chunk)) continue;
            var col = chunk.getPos();
            if (!level.hasChunk(col.x(), col.z()) || source.getChunkNow(col.x(), col.z()) != chunk) continue;
            for (var direction : Direction.Plane.HORIZONTAL) {
                var step = direction.getUnitVec3i();
                int x = col.x() + step.getX(), z = col.z() + step.getZ();
                if (level.hasChunk(x, z) || source.getChunkNow(x, z) != null) continue;
                var from = new Vec3(col.getMinBlockX() + 8.5, level.getMaxY() + 20.5, col.getMinBlockZ() + 8.5);
                var to = from.add(step.getX() * 25, 0, step.getZ() * 25);
                checkBoth(level, from, to);
                var hit = LbcTerra.wallHit(level, from, to);
                require(hit != null && ChunkPos.containing(hit.getBlockPos()).equals(new ChunkPos(x, z))
                                && !level.hasChunk(x, z) && source.getChunkNow(x, z) == null,
                        "first absent column remains an exact synthetic wall even above build height without loading");
                return;
            }
        }
        throw new IllegalStateException("Resident block ray: native resident/absent chunk boundary required");
    }

    private static BlockPos emptyColumn(ServerLevel level) {
        var source = level.getChunkSource();
        for (var holder : ((ArenaChunkAccess) source.chunkMap).skywars2$visibleChunks().values()) {
            if (!(holder.getLatestChunk() instanceof LevelChunk chunk)) continue;
            var pos = chunk.getPos();
            if (!level.hasChunk(pos.x(), pos.z()) || source.getChunkNow(pos.x(), pos.z()) != chunk) continue;
            int contiguous = 0;
            for (int i = chunk.getSections().length - 1; i >= 0; i--) {
                contiguous = chunk.getSection(i).hasOnlyAir() ? contiguous + 1 : 0;
                if (contiguous == 6) {
                    return new BlockPos(pos.getMinBlockX() + 8, chunk.getSectionYFromSectionIndex(i) << 4,
                            pos.getMinBlockZ() + 8);
                }
            }
        }
        throw new IllegalStateException("Resident block ray: 96 empty native cells required in a resident column");
    }

    private static void set(GameController game, Map<BlockPos, MapManager.BlockSnapshot> saved, BlockPos pos, Block block) {
        var level = game.maps().level();
        saved.computeIfAbsent(pos, key -> game.maps().snapshot(level, key));
        level.setBlock(pos, block.defaultBlockState(), 2 | 16 | 32);
    }

    private static void checkBoth(ServerLevel level, Vec3 from, Vec3 to) {
        equal(vanilla(level, from, to, ResidentBlockRayChecks::wall),
                LbcTerra.wallHit(level, from, to), "wall " + from + " -> " + to);
        var context = CollisionContext.empty();
        equal(vanilla(level, from, to, (pos, state) -> state.getCollisionShape(level, pos, context)),
                LbcTerra.surfaceHit(level, from, to, null), "shape " + from + " -> " + to);
    }

    private static VoxelShape wall(BlockPos pos, BlockState state) {
        return state.is(AIRS) ? Shapes.empty() : Shapes.block();
    }

    private static BlockHitResult vanilla(ServerLevel level, Vec3 from, Vec3 to,
            BiFunction<BlockPos, BlockState, VoxelShape> shape) {
        return BlockGetter.traverseBlocks(from, to, level, (world, pos) ->
                (world.hasChunkAt(pos) ? shape.apply(pos, world.getBlockState(pos)) : Shapes.block()).clip(from, to, pos),
                world -> null);
    }

    private static void equal(BlockHitResult expected, BlockHitResult actual, String witness) {
        require(expected == null ? actual == null : actual != null
                        && expected.getBlockPos().equals(actual.getBlockPos())
                        && expected.getLocation().equals(actual.getLocation())
                        && expected.getDirection() == actual.getDirection()
                        && expected.isInside() == actual.isInside(),
                "native hit differs for " + witness + " expected " + describe(expected) + ", actual " + describe(actual));
    }

    private static String describe(BlockHitResult hit) {
        return hit == null ? "none" : hit.getBlockPos() + " at " + hit.getLocation() + " facing " + hit.getDirection();
    }

    private static void require(boolean condition, String why) {
        if (!condition) throw new IllegalStateException("Resident block ray: " + why);
    }
}
