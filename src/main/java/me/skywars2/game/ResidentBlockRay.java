package me.skywars2.game;

import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Native block traversal with an empty-section jump for shapes known to ignore air. */
public final class ResidentBlockRay {
    private static final double MIN_JUMP_DISTANCE_SQUARED = 16 * 16;
    private static final double DDA_COORDINATE_LIMIT = 100_000_000;

    private ResidentBlockRay() {}

    public static BlockHitResult clip(ServerLevel level, Vec3 from, Vec3 to,
            BiFunction<BlockPos, BlockState, VoxelShape> shape, boolean airTransparent, boolean voidTransparent) {
        if (!(from.distanceToSqr(to) >= MIN_JUMP_DISTANCE_SQUARED) || level.isDebug()
                || !safeCoordinates(from) || !safeCoordinates(to) || !airTransparent) {
            return BlockGetter.traverseBlocks(from, to, level, (world, pos) ->
                    (world.hasChunkAt(pos) ? shape.apply(pos, world.getBlockState(pos)) : Shapes.block())
                            .clip(from, to, pos), world -> null);
        }

        var cursor = new Cursor(from, to);
        var pos = new BlockPos.MutableBlockPos();
        int columnX = Integer.MIN_VALUE;
        int columnZ = Integer.MIN_VALUE;
        int sectionY = Integer.MIN_VALUE;
        boolean resident = false;
        LevelChunk chunk = null;
        boolean transparentSection = false;
        do {
            int x = cursor.x >> 4;
            int y = cursor.y >> 4;
            int z = cursor.z >> 4;
            if (x != columnX || z != columnZ) {
                columnX = x;
                columnZ = z;
                sectionY = Integer.MIN_VALUE;
                // Keep the old cold boundary even when a prior FULL chunk is still cached.
                resident = level.hasChunk(x, z);
                chunk = resident ? level.getChunkSource().getChunkNow(x, z) : null;
            }
            if (y != sectionY) {
                sectionY = y;
                pos.set(cursor.x, cursor.y, cursor.z);
                transparentSection = resident && (level.isInValidBounds(pos)
                        ? chunk != null && chunk.getSection(chunk.getSectionIndexFromSectionY(y)).hasOnlyAir()
                        : voidTransparent);
            }
            if (transparentSection) {
                if (!cursor.skipSection()) return null;
                continue;
            }

            pos.set(cursor.x, cursor.y, cursor.z);
            VoxelShape cell = Shapes.block();
            if (resident) {
                // A visible FULL ticket can precede the actual LevelChunk. Use the old
                // native read for that transient case so it cannot be mistaken for air.
                BlockState state = !level.isInValidBounds(pos) ? Blocks.VOID_AIR.defaultBlockState()
                        : chunk == null ? level.getBlockState(pos) : chunk.getBlockState(pos);
                cell = shape.apply(pos, state);
            }
            var hit = cell.clip(from, to, pos);
            if (hit != null) return hit;
            if (!cursor.step()) return null;
        } while (true);
    }

    private static boolean safeCoordinates(Vec3 point) {
        return Math.abs(point.x) < DDA_COORDINATE_LIMIT && Math.abs(point.y) < DDA_COORDINATE_LIMIT
                && Math.abs(point.z) < DDA_COORDINATE_LIMIT;
    }

    private static final class Cursor {
        int x, y, z;
        private final int signX, signY, signZ;
        private final double deltaX, deltaY, deltaZ;
        private double timeX, timeY, timeZ;

        Cursor(Vec3 from, Vec3 to) {
            // Match native BlockGetter.traverseBlocks. Clip uses the original endpoints.
            double toX = Mth.lerp(-1.0E-7, to.x, from.x);
            double toY = Mth.lerp(-1.0E-7, to.y, from.y);
            double toZ = Mth.lerp(-1.0E-7, to.z, from.z);
            double fromX = Mth.lerp(-1.0E-7, from.x, to.x);
            double fromY = Mth.lerp(-1.0E-7, from.y, to.y);
            double fromZ = Mth.lerp(-1.0E-7, from.z, to.z);
            x = Mth.floor(fromX);
            y = Mth.floor(fromY);
            z = Mth.floor(fromZ);
            double dx = toX - fromX, dy = toY - fromY, dz = toZ - fromZ;
            signX = Mth.sign(dx);
            signY = Mth.sign(dy);
            signZ = Mth.sign(dz);
            deltaX = signX == 0 ? Double.MAX_VALUE : signX / dx;
            deltaY = signY == 0 ? Double.MAX_VALUE : signY / dy;
            deltaZ = signZ == 0 ? Double.MAX_VALUE : signZ / dz;
            timeX = deltaX * (signX > 0 ? 1.0 - Mth.frac(fromX) : Mth.frac(fromX));
            timeY = deltaY * (signY > 0 ? 1.0 - Mth.frac(fromY) : Mth.frac(fromY));
            timeZ = deltaZ * (signZ > 0 ? 1.0 - Mth.frac(fromZ) : Mth.frac(fromZ));
        }

        boolean step() {
            if (!(timeX <= 1.0 || timeY <= 1.0 || timeZ <= 1.0)) return false;
            if (timeZ <= timeX && timeZ <= timeY) {
                z += signZ;
                timeZ += deltaZ;
            } else if (timeY <= timeX) {
                y += signY;
                timeY += deltaY;
            } else {
                x += signX;
                timeX += deltaX;
            }
            return true;
        }

        boolean skipSection() {
            double exit = Math.min(sectionExit(x, signX, timeX, deltaX),
                    Math.min(sectionExit(y, signY, timeY, deltaY), sectionExit(z, signZ, timeZ, deltaZ)));
            if (!(exit <= 1.0)) return false;
            // Internal steps commute. Leave events tied with the first exit to the
            // original Z/Y/X priority so a ray touching a corner visits the same cells.
            while (timeX < exit) {
                x += signX;
                timeX += deltaX;
            }
            while (timeY < exit) {
                y += signY;
                timeY += deltaY;
            }
            while (timeZ < exit) {
                z += signZ;
                timeZ += deltaZ;
            }
            return step();
        }

        private static double sectionExit(int coordinate, int sign, double time, double delta) {
            if (sign == 0) return Double.POSITIVE_INFINITY;
            int offset = coordinate & 15;
            int internal = sign > 0 ? 15 - offset : offset;
            // The additions are deliberate. Multiplication rounds differently near
            // simultaneous axis crossings and can change which cell wins a corner.
            for (int i = 0; i < internal; i++) time += delta;
            return time;
        }
    }
}
