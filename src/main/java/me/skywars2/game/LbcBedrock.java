package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/** Бедроковые комнаты: слои возврата мира, событийная регенерация и дополнительные режимы. */
public final class LbcBedrock {
    static final Identifier ROOM = Identifier.parse("lbc:bedrock_room");
    private static final int ROOM_RADIUS = 10;
    private static final int ROOM_HALF_HEIGHT = 5;
    private static final int REGEN_BUDGET = 256;
    private static final double RAY_RANGE = 100.0;

    private final GameController game;
    private final Map<UUID, Room> normalRooms = new HashMap<>();
    private final Map<UUID, Room> regenRooms = new HashMap<>();
    private final Map<Long, Set<Room>> roomsByChunk = new HashMap<>();
    private final Map<BlockPos, List<Layer>> layers = new HashMap<>();
    private final Map<Long, LinkedHashMap<BlockPos, Restoration>> pendingRestore = new HashMap<>();
    private final ArrayDeque<Long> readyRestoreChunks = new ArrayDeque<>();
    private final Set<Long> queuedRestoreChunks = new HashSet<>();
    private final LinkedHashMap<BlockPos, Room> dirty = new LinkedHashMap<>();
    private final Set<BlockPos> suppressBlockEvents = new HashSet<>();
    private final Set<BlockPos> bedrockCells = new LinkedHashSet<>();
    private final Set<BlockPos> lightCells = new LinkedHashSet<>();
    private final Set<Cube> cubes = new LinkedHashSet<>();
    private StructureTemplate roomTemplate;
    private boolean stopped;

    LbcBedrock(GameController game) { this.game = game; }

    /** Resolve and cache the native structure before the first cast. */
    public void prepare() {
        roomTemplate = game.server().getStructureTemplateManager().get(ROOM).orElse(null);
        bedrockCells.clear();
        lightCells.clear();
        if (roomTemplate == null) return;
        StructurePlaceSettings settings = new StructurePlaceSettings();
        for (var info : roomTemplate.filterBlocks(BlockPos.ZERO, settings, Blocks.BEDROCK))
            bedrockCells.add(info.pos().immutable());
        for (var info : roomTemplate.filterBlocks(BlockPos.ZERO, settings, Blocks.LIGHT))
            lightCells.add(info.pos().immutable());
        stopped = false;
    }

    /**
     * Returns true when the input caused a room, mode, or Academy trial action.
     */
    public boolean use(ServerPlayer player, InteractionHand hand, ItemStack stack, boolean upgraded) {
        if (stopped || roomTemplate == null || stack.isEmpty() || hand == null
                || !game.canUseGameplayItems(player) || !player.isAlive() || player.isRemoved()
                || player.level() != game.maps().level() || game.stage() == GameController.Stage.ENDING
                || game.items().frost().isFrozen(player) || !game.items().mana().visible(player)
                || player.entityTags().contains("time_stopped") || game.items().wands().timeStopped()
                || game.items().magic().suppressed(player)
                || game.items().magic().suppressedAt(player.level(), player.getEyePosition())) return false;

        if (upgraded && player.isShiftKeyDown()) {
            if (player.getXRot() < 89.0f || player.getXRot() > 90.0f) {
                CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                int next = Math.floorMod(data.getIntOr("mode", 1), 5) + 1;
                data.putInt("mode", next);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(switch (next) {
                    case 2 -> "mode_regen";
                    case 3 -> "mode_remove";
                    case 4 -> "mode_place_cube";
                    case 5 -> "mode_remove_cube";
                    default -> "mode_default";
                }));
                return true;
            }
            return true; // Shift с взглядом строго вниз не переключает режим и не выполняет заклинание.
        }

        int mode = upgraded ? Math.max(1, Math.min(5,
                stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr("mode", 1))) : 1;
        if (mode <= 2) {
            boolean handled = toggleRoom(player, mode == 2);
            if (!upgraded) handled |= game.items().academy().bedrockTrial(player, hand, stack);
            return handled;
        }
        if (mode == 3) return removeBedrock(player);
        if (mode == 4) return createCube(player);
        return clearCubes(player);
    }

    boolean grimoire(ServerPlayer owner,int mode){
        if(stopped||roomTemplate==null)return false;
        if(mode<=2)toggleRoom(owner,mode==2,false);
        else if(mode==3)removeBedrock(owner);
        else if(mode==4)createCube(owner);
        else clearCubes(owner);
        return true;
    }
    private boolean toggleRoom(ServerPlayer owner, boolean regen) { return toggleRoom(owner,regen,true); }
    private boolean toggleRoom(ServerPlayer owner, boolean regen,boolean paid) {
        Map<UUID, Room> rooms = regen ? regenRooms : normalRooms;
        Room existing = rooms.get(owner.getUUID());
        if (existing != null) {
            recall(existing);
            return true;
        }
        int cost = game.items().mana().score(
                net.minecraft.world.scores.ScoreHolder.forNameOnly("bedrock_wand_mana_use"), "lbc.math").get();
        if (roomTemplate == null || paid && !game.items().mana().trySpendWand(owner, cost)) return false;
        BlockPos origin = owner.blockPosition().offset(-ROOM_RADIUS, -ROOM_HALF_HEIGHT, -ROOM_RADIUS).immutable();
        Room room = new Room(owner.getUUID(), owner, (ServerLevel) owner.level(), origin, regen, game.items().epoch());
        rooms.put(owner.getUUID(), room);
        index(room);
        captureTemplateCells(room, owner);
        game.items().indirectKills().trapChanges(owner,
                () -> game.maps().placeLoadedTemplate(owner, ROOM, origin, 1.0f));
        if (regen) seedLoadedShell(room, null);
        if (game.items().magic().beaconIntersects(room.bounds())) cancelRoom(room);
        if(paid)game.items().wands().wandCasted(owner);
        sound(owner, "minecraft:block.stone.place", 0.5f, 1.0f);
        sound(owner, "minecraft:block.stone.place", 0.5f, 0.0f);
        sound(owner, "minecraft:block.stone.place", 0.5f, 2.0f);
        return true;
    }

    private void captureTemplateCells(Room room, ServerPlayer owner) {
        for (BlockPos relative : bedrockCells) captureCell(room, owner, room.origin.offset(relative));
        for (BlockPos relative : lightCells) captureCell(room, owner, room.origin.offset(relative));
    }

    private void captureCell(Room room, ServerPlayer owner, BlockPos pos) {
        if (!room.level.hasChunkAt(pos)) return;
        BlockPos key = pos.immutable();
        flushPendingAt(key);
        MapManager.BlockSnapshot before = game.maps().snapshot(key);
        layers.computeIfAbsent(key, ignored -> new ArrayList<>()).add(new Layer(room, before));
        room.positions.add(key);
    }

    private void recall(Room room) {
        if (room.removed) return;
        room.removed = true;
        (room.regen ? regenRooms : normalRooms).remove(room.owner, room);
        unindex(room);
        var reassess = releaseDirty(room);
        game.items().indirectKills().worldChanges(room.level, source(room), () -> {
            for (BlockPos pos : room.positions) {
                List<Layer> stack = layers.get(pos);
                if (stack == null) continue;
                int at = -1;
                for (int i = 0; i < stack.size(); i++) if (stack.get(i).room == room) { at = i; break; }
                if (at < 0) continue;
                Layer removed = stack.remove(at);
                if (at < stack.size()) stack.get(at).before = removed.before;
                else restoreOrDefer(pos, removed.before, room, true);
                if (stack.isEmpty()) layers.remove(pos);
            }
        });
        room.positions.clear();
        requeueDirty(reassess);
        BlockPos center = room.origin.offset(10, 5, 10);
        ServerPlayer owner = game.server().getPlayerList().getPlayer(room.owner);
        if (owner != null && owner.level() == room.level) {
            LbcOverload.sound(owner, Vec3.atCenterOf(center), "minecraft:block.stone.break", 0.5f, 0.0f, 0);
            LbcOverload.sound(owner, Vec3.atCenterOf(center), "minecraft:block.stone.break", 0.5f, 1.0f, 0);
            LbcOverload.sound(owner, Vec3.atCenterOf(center), "minecraft:block.stone.break", 0.5f, 2.0f, 0);
        }
    }

    private void restoreOrDefer(BlockPos pos, MapManager.BlockSnapshot snapshot, Room room, boolean inAttributionContext) {
        if (!roomLevel().hasChunkAt(pos)) {
            pendingRestore.computeIfAbsent(chunkKey(pos), ignored -> new LinkedHashMap<>())
                    .put(pos.immutable(), new Restoration(snapshot, room));
        } else restoreSnapshot(pos, new Restoration(snapshot, room), !inAttributionContext);
    }

    private void restoreSnapshot(BlockPos pos, Restoration restoration) {
        restoreSnapshot(pos, restoration, true);
    }

    private void restoreSnapshot(BlockPos pos, Restoration restoration, boolean wrapAttribution) {
        Runnable write = () -> {
            suppressBlockEvents.add(pos);
            try { game.maps().restoreSnapshot(pos, restoration.snapshot); }
            finally { suppressBlockEvents.remove(pos); }
        };
        if (wrapAttribution)
            game.items().indirectKills().worldChanges(restoration.room.level, source(restoration.room), write);
        else write.run();
        // Возвращённая пробоина нижней комнаты снова участвует в её регенерации.
        blockChanged(pos, restoration.snapshot.state());
    }

    private ServerPlayer source(Room room) {
        ServerPlayer current = game.server().getPlayerList().getPlayer(room.owner);
        return current == null ? room.originalOwner : current;
    }

    private void flushPendingAt(BlockPos pos) {
        long key = chunkKey(pos);
        var restorations = pendingRestore.get(key);
        if (restorations == null) return;
        var restoration = restorations.remove(pos);
        if (restoration != null) restoreSnapshot(pos, restoration);
        if (restorations.isEmpty()) {
            pendingRestore.remove(key);
            queuedRestoreChunks.remove(key);
            readyRestoreChunks.removeIf(candidate -> candidate == key);
        }
    }

    /** Permanently forget an intersecting room while leaving its placed blocks in the world. */
    public void beaconPlaced(Vec3 center, double radius) {
        if (stopped || center == null || radius < 0) return;
        int minX = BlockPos.containing(center.x - radius, 0, center.z - radius).getX() >> 4;
        int maxX = BlockPos.containing(center.x + radius, 0, center.z + radius).getX() >> 4;
        int minZ = BlockPos.containing(center.x - radius, 0, center.z - radius).getZ() >> 4;
        int maxZ = BlockPos.containing(center.x + radius, 0, center.z + radius).getZ() >> 4;
        Set<Room> candidates = new HashSet<>();
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            Set<Room> indexed = roomsByChunk.get(chunkKey(x, z));
            if (indexed != null) candidates.addAll(indexed);
        }
        double radiusSqr = radius * radius;
        for (Room room : candidates)
            if (!room.removed && room.bounds().distanceToSqr(center) <= radiusSqr) cancelRoom(room);
    }

    private void cancelRoom(Room room) {
        if (room.removed) return;
        room.removed = true;
        (room.regen ? regenRooms : normalRooms).remove(room.owner, room);
        unindex(room);
        var reassess = releaseDirty(room);
        for (BlockPos pos : List.copyOf(room.positions)) {
            List<Layer> stack = layers.get(pos);
            var deferred = pendingRestore.get(chunkKey(pos));
            if (deferred != null) {
                deferred.remove(pos);
                if (deferred.isEmpty()) pendingRestore.remove(chunkKey(pos));
            }
            if (stack == null) continue;
            int at = -1;
            for (int i = 0; i < stack.size(); i++) if (stack.get(i).room == room) { at = i; break; }
            if (at < 0) continue;
            for (int i = 0; i <= at; i++) stack.get(i).room.positions.remove(pos);
            stack.subList(0, at + 1).clear();
            if (stack.isEmpty()) layers.remove(pos);
        }
        room.positions.clear();
        requeueDirty(reassess);
    }

    /** Одна пробоина могла принадлежать нескольким комнатам: после снятия первой ищем оставшуюся. */
    private List<BlockPos> releaseDirty(Room room) {
        var holes=new ArrayList<BlockPos>();
        dirty.entrySet().removeIf(entry -> {
            if(entry.getValue()!=room) return false;
            holes.add(entry.getKey()); return true;
        });
        return holes;
    }
    private void requeueDirty(List<BlockPos> holes) {
        for(var pos:holes) if(roomLevel().hasChunkAt(pos)) blockChanged(pos,roomLevel().getBlockState(pos));
    }

    private void index(Room room) {
        int minX = room.origin.getX() >> 4, maxX = (room.origin.getX() + 20) >> 4;
        int minZ = room.origin.getZ() >> 4, maxZ = (room.origin.getZ() + 20) >> 4;
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++)
            roomsByChunk.computeIfAbsent(chunkKey(x, z), ignored -> new HashSet<>()).add(room);
    }

    private void unindex(Room room) {
        for (Iterator<Map.Entry<Long, Set<Room>>> it = roomsByChunk.entrySet().iterator(); it.hasNext();) {
            var entry = it.next();
            entry.getValue().remove(room);
            if (entry.getValue().isEmpty()) it.remove();
        }
    }

    /** Called by the arena block-change hook; only air in a regen shell is queued. */
    public void blockChanged(BlockPos pos, BlockState state) {
        if (stopped || !state.is(Blocks.AIR) || suppressBlockEvents.contains(pos)) return;
        Set<Room> candidates = roomsByChunk.get(chunkKey(pos));
        if (candidates == null) return;
        for (Room room : List.copyOf(candidates)) {
            if (!room.regen || room.removed || !room.level.hasChunkAt(pos)) continue;
            BlockPos relative = pos.subtract(room.origin);
            if (!inside(relative) || !isShell(relative) || !bedrockCells.contains(relative)) continue;
            dirty.putIfAbsent(pos.immutable(), room);
        }
    }

    /** Restore unloaded chunk snapshots and seed only regen cells in this loaded chunk. */
    public void chunkLoaded(LevelChunk chunk) {
        var chunkPos = chunk.getPos();
        long key = chunkKey(chunkPos.x(), chunkPos.z());
        if (pendingRestore.containsKey(key) && queuedRestoreChunks.add(key)) readyRestoreChunks.addLast(key);
        if (stopped) return;
        Set<Room> rooms = roomsByChunk.get(key);
        if (rooms == null) return;
        for (Room room : List.copyOf(rooms)) if (room.regen && !room.removed) seedLoadedShell(room, chunkPos);
    }

    /** Refill changed room shell positions with a fixed work budget; global time stop pauses it. */
    public void tick() {
        if (stopped) return;
        int budget = REGEN_BUDGET;
        while (budget > 0 && !readyRestoreChunks.isEmpty()) {
            long key = readyRestoreChunks.removeFirst();
            queuedRestoreChunks.remove(key);
            var restorations = pendingRestore.get(key);
            if (restorations == null || restorations.isEmpty()) { pendingRestore.remove(key); continue; }
            if (!roomLevel().hasChunk(keyChunkX(key), keyChunkZ(key))) continue;
            Iterator<Map.Entry<BlockPos, Restoration>> it = restorations.entrySet().iterator();
            var restoration = it.next();
            it.remove();
            restoreSnapshot(restoration.getKey(), restoration.getValue());
            if (restorations.isEmpty()) pendingRestore.remove(key);
            else if (queuedRestoreChunks.add(key)) readyRestoreChunks.addLast(key);
            budget--;
        }
        if (game.items().wands().timeStopped() || game.stage() == GameController.Stage.ENDING) return;
        if(dirty.isEmpty()) return;
        Map<BlockPos, Room> ownerPaused = new LinkedHashMap<>();
        while (budget-- > 0 && !dirty.isEmpty()) {
            var iterator = dirty.entrySet().iterator();
            var entry = iterator.next();
            iterator.remove();
            BlockPos pos = entry.getKey();
            Room room = entry.getValue();
            if (room.removed || !room.regen || !game.items().currentEpoch(room.epoch)
                    || room.level != game.maps().level() || !room.level.hasChunkAt(pos)
                    || !room.level.getBlockState(pos).is(Blocks.AIR)) continue;
            BlockPos relative = pos.subtract(room.origin);
            if (!inside(relative) || !isShell(relative) || !bedrockCells.contains(relative)) continue;
            ServerPlayer owner = game.server().getPlayerList().getPlayer(room.owner);
            if (owner != null && owner.entityTags().contains("time_stopped")) {
                ownerPaused.put(pos, room);
                continue;
            }
            BlockState old = room.level.getBlockState(pos);
            BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
            if (room.level.setBlock(pos, bedrock, blockFlags()) && bedrock.isSuffocating(room.level, pos)
                    && !old.isSuffocating(room.level, pos))
                game.items().indirectKills().stonePlaced(room.level, pos, owner == null ? room.originalOwner : owner);
        }
        ownerPaused.forEach(dirty::putIfAbsent);
    }

    private void seedLoadedShell(Room room, net.minecraft.world.level.ChunkPos onlyChunk) {
        for (BlockPos relative : bedrockCells) {
            if (!isShell(relative)) continue;
            BlockPos pos = room.origin.offset(relative);
            if (onlyChunk != null && ((pos.getX() >> 4) != onlyChunk.x() || (pos.getZ() >> 4) != onlyChunk.z())) continue;
            if (room.level.hasChunkAt(pos) && room.level.getBlockState(pos).is(Blocks.AIR)) dirty.putIfAbsent(pos, room);
        }
    }

    private static boolean inside(BlockPos p) {
        return p.getX() >= 0 && p.getX() <= 20 && p.getY() >= 0 && p.getY() <= 10 && p.getZ() >= 0 && p.getZ() <= 20;
    }

    private static boolean isShell(BlockPos p) {
        int x = p.getX() - 10, y = p.getY() - 5, z = p.getZ() - 10;
        return Math.abs(x) >= 8 || Math.abs(z) >= 8 || y < -2 || y > 2;
    }

    private boolean removeBedrock(ServerPlayer owner) {
        BlockHitResult hit = ray(owner, state -> state.is(Blocks.BEDROCK));
        if (hit == null || !owner.level().hasChunkAt(hit.getBlockPos())) return false;
        BlockPos center = hit.getBlockPos();
        var level = (ServerLevel) owner.level();
        game.items().indirectKills().trapChanges(owner, () -> {
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1,-1,-1), center.offset(1,1,1)))
                if (level.hasChunkAt(pos) && level.getBlockState(pos).is(Blocks.BEDROCK))
                    level.setBlock(pos, Blocks.STRUCTURE_VOID.defaultBlockState(), blockFlags());
        });
        sound(owner, "minecraft:block.enchantment_table.use", 1.0f, 1.0f);
        owner.level().sendParticles(new net.minecraft.core.particles.DustParticleOptions(
                        net.minecraft.util.ARGB.colorFromFloat(1,.745f,.431f,.953f)&0xffffff,4),
                center.getX()+.5,center.getY()+.5,center.getZ()+.5,20,1,1,1,0);
        return true;
    }

    private boolean createCube(ServerPlayer owner) {
        BlockHitResult hit = ray(owner, null);
        if (hit == null || !owner.level().hasChunkAt(hit.getBlockPos())) return false;
        BlockPos center = hit.getBlockPos().immutable();
        var level = (ServerLevel) owner.level();
        game.items().indirectKills().trapChanges(owner, () -> {
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2,-2,-2), center.offset(2,2,2)))
                if (level.hasChunkAt(pos)) level.setBlock(pos, Blocks.BEDROCK.defaultBlockState(), blockFlags());
        });
        cubes.add(new Cube(level, center));
        sound(owner, "minecraft:block.stone.place", 1.0f, 1.0f);
        return true;
    }

    private boolean clearCubes(ServerPlayer owner) {
        BlockHitResult hit = ray(owner, null);
        if (hit == null || !owner.level().hasChunkAt(hit.getBlockPos())) return false;
        BlockPos target = hit.getBlockPos();
        var level = (ServerLevel) owner.level();
        List<Cube> remove = new ArrayList<>();
        for (Cube cube : cubes) if (cube.level == level && cube.center.distSqr(target) <= 49.0) {
            remove.add(cube);
            game.items().indirectKills().trapChanges(owner, () -> {
                for (BlockPos pos : BlockPos.betweenClosed(cube.center.offset(-2,-2,-2), cube.center.offset(2,2,2)))
                    if (level.hasChunkAt(pos) && level.getBlockState(pos).is(Blocks.BEDROCK))
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), blockFlags());
            });
            sound(owner, "minecraft:block.stone.break", 1.0f, 1.0f);
            level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                            net.minecraft.core.particles.ParticleTypes.BLOCK,Blocks.BEDROCK.defaultBlockState()),
                    cube.center.getX()+.5,cube.center.getY()+.5,cube.center.getZ()+.5,200,1.5,1.5,1.5,0);
        }
        cubes.removeAll(remove);
        return !remove.isEmpty();
    }

    private BlockHitResult ray(ServerPlayer owner, Predicate<BlockState> collidable) {
        Vec3 from = owner.getEyePosition();
        Vec3 to = from.add(Vec3.directionFromRotation(owner.getXRot(), owner.getYRot()).scale(RAY_RANGE));
        return collidable==null ? LbcTerra.wallHit(owner.level(),from,to)
                : LbcTerra.wallHit(owner.level(), from, to, collidable);
    }

    private static int blockFlags() {
        return net.minecraft.world.level.block.Block.UPDATE_CLIENTS
                | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE
                | net.minecraft.world.level.block.Block.UPDATE_SUPPRESS_DROPS
                | net.minecraft.world.level.block.Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS
                | net.minecraft.world.level.block.Block.UPDATE_SKIP_ON_PLACE;
    }

    private ServerLevel roomLevel() { return game.maps().level(); }
    private static int keyChunkX(long key) { return (int) (key >> 32); }
    private static int keyChunkZ(long key) { return (int) key; }
    private static long chunkKey(BlockPos pos) { return chunkKey(pos.getX() >> 4, pos.getZ() >> 4); }
    private static long chunkKey(int x, int z) { return ((long) x << 32) ^ (z & 0xffffffffL); }
    private static void sound(ServerPlayer player, String id, float volume, float pitch) {
        LbcOverload.sound(player, id, volume, pitch, 0);
    }
    public int count() { return normalRooms.size() + regenRooms.size(); }
    public int cubeCount() { return cubes.size(); }

    /** Forget match-scoped room metadata; the arena lifecycle owns the physical map reset. */
    public void stop() {
        if (stopped) return;
        stopped = true;
        normalRooms.clear();
        regenRooms.clear();
        dirty.clear();
        cubes.clear();
        roomsByChunk.clear();
        layers.clear();
        pendingRestore.clear();
        readyRestoreChunks.clear();
        queuedRestoreChunks.clear();
        suppressBlockEvents.clear();
        stopped = false;
    }

    private static final class Room {
        final UUID owner;
        final ServerLevel level;
        final BlockPos origin;
        final boolean regen;
        final String epoch;
        final ServerPlayer originalOwner;
        final Set<BlockPos> positions = new LinkedHashSet<>();
        boolean removed;
        Room(UUID owner, ServerPlayer originalOwner, ServerLevel level, BlockPos origin, boolean regen, String epoch) {
            this.owner = owner; this.originalOwner = originalOwner; this.level = level;
            this.origin = origin; this.regen = regen; this.epoch = epoch;
        }
        AABB bounds() {
            return new AABB(origin.getX(), origin.getY(), origin.getZ(),
                    origin.getX() + 21, origin.getY() + 11, origin.getZ() + 21);
        }
    }
    private static final class Layer {
        final Room room;
        MapManager.BlockSnapshot before;
        Layer(Room room, MapManager.BlockSnapshot before) { this.room = room; this.before = before; }
    }
    private record Restoration(MapManager.BlockSnapshot snapshot, Room room) { }
    private record Cube(ServerLevel level, BlockPos center) { }
}
