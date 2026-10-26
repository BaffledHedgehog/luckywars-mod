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
import java.util.List;
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
    private static final int NODES_PER_TURN = 32;
    private static final long TURN_NANOS = 500_000;
    private static final int WRITE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS
            | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SKIP_ON_PLACE;

    private static final List<BlockPos> FACE_NEIGHBORS = List.of(
            new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
            new BlockPos(0, 1, 0), new BlockPos(0, -1, 0),
            new BlockPos(0, 0, 1), new BlockPos(0, 0, -1));
    private static final List<BlockPos> CONNECTED_26 = buildConnected26();
    private static final List<net.minecraft.world.level.block.Block> ORE_BLOCKS = collectOreBlocks();
    private static final List<net.minecraft.world.level.block.Block> STONE_BLOCKS = collectStoneBlocks();

    private LbcMagmify() { }
    static void prepare() { } // Registry-каталоги прогреваются при загрузке ресурсов, до первого солнца.

    public static LbcStonekivi.Work create(GameController game, ServerPlayer owner, BlockPos origin) {
        return new Work(game, owner, origin);
    }

    /** One cast owns its observed originals; no full-region capture/planner barrier delays its first writes. */
    private static final class Work implements LbcStonekivi.Work {
        private final GameController game;
        private final ServerPlayer owner;
        private final ServerLevel level;
        private final it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<Cell> observed =
                new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>();
        private final it.unimi.dsi.fastutil.longs.LongOpenHashSet discovered = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        private final it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue frontier = new it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue();
        private final it.unimi.dsi.fastutil.longs.LongOpenHashSet surfaceBlocks = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        private final it.unimi.dsi.fastutil.longs.LongOpenHashSet lava = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        private final ArrayDeque<Spike> spikes = new ArrayDeque<>();
        private final BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        private final long[] centerSample = new long[CENTER_SAMPLE_SIZE];
        private final ThreadLocalRandom random = ThreadLocalRandom.current();
        private Phase phase = Phase.TRANSFORM;
        private it.unimi.dsi.fastutil.longs.LongIterator lavaIterator;
        private List<LivingEntity> entities;
        private int entityCursor, seen;
        private int minX=Integer.MAX_VALUE,minY=Integer.MAX_VALUE,minZ=Integer.MAX_VALUE;
        private int maxX=Integer.MIN_VALUE,maxY=Integer.MIN_VALUE,maxZ=Integer.MIN_VALUE;
        private Vec3 center;

        private Work(GameController game, ServerPlayer owner, BlockPos origin) {
            this.game=game; this.owner=owner; this.level=owner.level();
            if (!discover(origin.getX(),origin.getY(),origin.getZ())) phase=Phase.FINISHED;
        }

        @Override public boolean step() {
            if (phase==Phase.FINISHED) return true;
            if (phase==Phase.TRANSFORM || phase==Phase.SPIKES) {
                // One shared attribution frame per batch, rather than allocating it for every block.
                game.items().indirectKills().trapChanges(level,owner,this::transformBatch);
            } else if (phase==Phase.LAVA_TICKS) scheduleLavaBatch();
            else if (phase==Phase.IMPACT) impactOneEntity();
            return phase==Phase.FINISHED;
        }

        @Override public void cancel() {
            phase=Phase.FINISHED;
            observed.clear(); discovered.clear(); frontier.clear(); surfaceBlocks.clear(); lava.clear(); spikes.clear();
            if (entities!=null) entities.clear();
        }

        private Cell sample(int x,int y,int z) {
            if (y<level.getMinY() || y>level.getMaxY()) return null;
            long key=BlockPos.asLong(x,y,z);
            Cell cell=observed.get(key);
            if (cell!=null) return cell;
            probe.set(x,y,z);
            if (!level.hasChunkAt(probe)) return null;
            cell=new Cell(level.getBlockState(probe)); observed.put(key,cell);
            return cell;
        }

        private boolean discover(int x,int y,int z) {
            Cell cell=sample(x,y,z);
            if (cell==null || cell.original.isAir()) return false;
            long key=BlockPos.asLong(x,y,z);
            if (discovered.add(key)) frontier.enqueue(key);
            return true;
        }

        private void transformBatch() {
            long started=System.nanoTime();
            for (int n=0;n<NODES_PER_TURN;n++) {
                if (phase==Phase.TRANSFORM) {
                    if (frontier.isEmpty()) { phase=Phase.SPIKES; continue; }
                    transformNode(frontier.dequeueLong());
                } else if (phase==Phase.SPIKES) {
                    Spike spike=spikes.pollFirst();
                    if (spike==null) {
                        lavaIterator=lava.iterator(); phase=Phase.LAVA_TICKS; return;
                    }
                    for (int i=0;i<spike.length;i++) {
                        int x=spike.x+spike.direction.getX()*i, y=spike.y+spike.direction.getY()*i,
                                z=spike.z+spike.direction.getZ()*i;
                        if (y<level.getMinY() || y>level.getMaxY()) break;
                        writeCell(x,y,z,Blocks.LAVA.defaultBlockState());
                    }
                } else return;
                // A cast yields regularly, so later casts and unrelated shared jobs also get a turn.
                if ((n&7)==7 && System.nanoTime()-started>=TURN_NANOS) return;
            }
        }

        private void transformNode(long key) {
            int x=BlockPos.getX(key),y=BlockPos.getY(key),z=BlockPos.getZ(key);
            // Capture the complete original halo BEFORE writing. Future nodes see original topology,
            // even when a previous node has already become lava or another crystallized material.
            for (BlockPos offset:CONNECTED_26) discover(x+offset.getX(),y+offset.getY(),z+offset.getZ());
            int occupied=0,solid=0;
            for (int i=0;i<FACE_NEIGHBORS.size();i++) {
                var offset=FACE_NEIGHBORS.get(i);
                var cell=sample(x+offset.getX(),y+offset.getY(),z+offset.getZ());
                if (cell!=null && !cell.original.isAir()) {
                    occupied|=1<<i;
                    if (cell.original.isSolidRender()) solid|=1<<i;
                }
            }
            net.minecraft.world.level.block.state.BlockState replacement=null;
            if (solid==63) replacement=Blocks.LAVA.defaultBlockState();
            else if (occupied==0) replacement=Blocks.MAGMA_BLOCK.defaultBlockState();
            else if ((occupied&8)==0) replacement=Blocks.REINFORCED_DEEPSLATE.defaultBlockState();
            else if ((occupied&51)!=51) replacement=randomState(ORE_BLOCKS,Blocks.DIAMOND_ORE,random);
            else if ((occupied&4)==0) replacement=randomState(STONE_BLOCKS,Blocks.STONE,random);
            if (replacement!=null) writeCell(x,y,z,replacement);

            minX=Math.min(minX,x); minY=Math.min(minY,y); minZ=Math.min(minZ,z);
            maxX=Math.max(maxX,x); maxY=Math.max(maxY,y); maxZ=Math.max(maxZ,z);
            seen++;
            int slot=seen<=CENTER_SAMPLE_SIZE ? seen-1 : random.nextInt(seen);
            if (slot<CENTER_SAMPLE_SIZE) centerSample[slot]=key;
            if (occupied!=63) surfaceBlocks.add(key);
            int count=Integer.bitCount(occupied);
            if (count>=3 && count<6 && random.nextInt(100)<SPIKE_CHANCE_PERCENT) {
                BlockPos direction=spikeDirection(occupied,random);
                if (direction!=null && !direction.equals(BlockPos.ZERO))
                    spikes.addLast(new Spike(x,y,z,direction,random.nextInt(SPIKE_MIN_LENGTH,SPIKE_MAX_LENGTH+1)));
            }
        }

        private void writeCell(int x,int y,int z,net.minecraft.world.level.block.state.BlockState replacement) {
            if (phase==Phase.FINISHED) return;
            Cell cell=sample(x,y,z);
            if (cell==null) return;
            probe.set(x,y,z);
            if (!level.hasChunkAt(probe)) return;
            var current=level.getBlockState(probe);
            // Only this cast's acknowledged writes advance expected: concurrent casts/player edits win.
            if (!current.equals(cell.expected) || current.equals(replacement)) return;
            var pos=probe.immutable();
            // Fluid flow starts after discovery: it must not turn original air into new flood-fill nodes.
            game.maps().restoreSnapshot(level,pos,new MapManager.BlockSnapshot(replacement,null),WRITE_FLAGS);
            cell.expected=replacement;
            if (replacement.is(Blocks.LAVA)) lava.add(BlockPos.asLong(x,y,z));
        }

        private void scheduleLavaBatch() {
            for (int i=0;i<NODES_PER_TURN && lavaIterator.hasNext();i++) {
                long key=lavaIterator.nextLong();
                probe.set(BlockPos.getX(key),BlockPos.getY(key),BlockPos.getZ(key));
                if (level.hasChunkAt(probe) && level.getBlockState(probe).is(Blocks.LAVA))
                    level.scheduleTick(probe,Fluids.LAVA,1);
            }
            if (lavaIterator.hasNext()) return;
            if (surfaceBlocks.isEmpty()) { phase=Phase.FINISHED; return; }
            double sumX=0,sumY=0,sumZ=0;
            int count=Math.min(seen,CENTER_SAMPLE_SIZE);
            for (int i=0;i<count;i++) {
                sumX+=BlockPos.getX(centerSample[i])+.5;
                sumY+=BlockPos.getY(centerSample[i])+.5;
                sumZ+=BlockPos.getZ(centerSample[i])+.5;
            }
            center=new Vec3(sumX/count,sumY/count,sumZ/count);
            var query=new AABB(minX-SURFACE_RANGE,minY-SURFACE_RANGE,minZ-SURFACE_RANGE,
                    maxX+1+SURFACE_RANGE,maxY+1+SURFACE_RANGE,maxZ+1+SURFACE_RANGE);
            entities=level.getEntitiesOfClass(LivingEntity.class,query,entity->true);
            phase=Phase.IMPACT;
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
            if (!touchesSurfaceWithinRange(box, surfaceBlocks)) return;

            Vec3 center = box.getCenter();
            DamageSource source = new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                    .getOrThrow(DamageTypes.FALLING_BLOCK), owner);
            target.hurtServer(level, source, SURFACE_DAMAGE);
            target.addEffect(ItemEffectLevels.create(MobEffects.SLOWNESS, SURFACE_SLOWNESS_TICKS,
                    SURFACE_SLOWNESS_AMPLIFIER, false, true, true), owner);
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.IRON_NUGGET), true, false,
                    center.x, center.y, center.z, SURFACE_PARTICLES,
                    (box.maxX - box.minX) * .5, (box.maxY - box.minY) * .5, (box.maxZ - box.minZ) * .5, .001);
            Vec3 away = center.subtract(this.center);
            if (away.lengthSqr() < 1.0e-6) away = new Vec3(0, 1, 0);
            target.setDeltaMovement(away.normalize().scale(SURFACE_VELOCITY));
            if(target.isAlive()) game.items().indirectKills().displaced(target,owner);
            if (target instanceof ServerPlayer player) {
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
            }
        }

        private boolean touchesSurfaceWithinRange(AABB box, it.unimi.dsi.fastutil.longs.LongSet surface) {
            int minX = (int) Math.floor(box.minX - SURFACE_RANGE);
            int minY = (int) Math.floor(box.minY - SURFACE_RANGE);
            int minZ = (int) Math.floor(box.minZ - SURFACE_RANGE);
            int maxX = (int) Math.floor(box.maxX + SURFACE_RANGE);
            int maxY = (int) Math.floor(box.maxY + SURFACE_RANGE);
            int maxZ = (int) Math.floor(box.maxZ + SURFACE_RANGE);
            for (int x = minX; x <= maxX; x++) for (int y = minY; y <= maxY; y++) for (int z = minZ; z <= maxZ; z++) {
                if (!surface.contains(BlockPos.asLong(x, y, z))) continue;
                double dx = axisDistance(box.minX, box.maxX, x, x + 1.0);
                double dy = axisDistance(box.minY, box.maxY, y, y + 1.0);
                double dz = axisDistance(box.minZ, box.maxZ, z, z + 1.0);
                if (dx * dx + dy * dy + dz * dz <= SURFACE_RANGE * SURFACE_RANGE) return true;
            }
            return false;
        }

    }

    private enum Phase { TRANSFORM, SPIKES, LAVA_TICKS, IMPACT, FINISHED }

    private static final class Cell {
        final net.minecraft.world.level.block.state.BlockState original;
        net.minecraft.world.level.block.state.BlockState expected;
        Cell(net.minecraft.world.level.block.state.BlockState state) { original=state; expected=state; }
    }
    private record Spike(int x,int y,int z,BlockPos direction,int length) { }

    private static BlockPos spikeDirection(int occupiedMask,ThreadLocalRandom random) {
        var occupied=new ArrayList<BlockPos>(6); var free=new ArrayList<BlockPos>(3);
        for (int i=0;i<FACE_NEIGHBORS.size();i++)
            ((occupiedMask&(1<<i))!=0 ? occupied : free).add(FACE_NEIGHBORS.get(i));
        return switch (occupied.size()) {
            case 5 -> free.getFirst();
            case 4 -> {
                BlockPos first=free.get(0),second=free.get(1);
                if (first.getX()!=-second.getX() || first.getY()!=-second.getY() || first.getZ()!=-second.getZ())
                    yield signVector(first.getX()+second.getX(),first.getY()+second.getY(),first.getZ()+second.getZ());
                yield free.get(random.nextInt(free.size()));
            }
            case 3 -> {
                int x=0,y=0,z=0;
                for (BlockPos face:occupied) { x-=face.getX(); y-=face.getY(); z-=face.getZ(); }
                BlockPos preferred=signVector(x,y,z);
                yield !preferred.equals(BlockPos.ZERO) ? preferred : free.get(random.nextInt(free.size()));
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
