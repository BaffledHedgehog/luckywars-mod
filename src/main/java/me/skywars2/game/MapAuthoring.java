package me.skywars2.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Developer map capture. World reads belong exclusively to tick(); workers own detached NBT only. */
public final class MapAuthoring implements AutoCloseable {
    private static final TicketType CAPTURE_TICKET = new TicketType(TicketType.NO_TIMEOUT,
            TicketType.FLAG_LOADING | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    private static final int BLOCKS_PER_TICK = 16384;
    private static final long TICK_NANOS = 8_000_000L;
    private static final long MAX_VOLUME = 32L * 1024 * 1024;
    private static final int MAX_BLOCKS = 2 * 1024 * 1024;
    static void registerTicket() {
        Registry.register(BuiltInRegistries.TICKET_TYPE,
                Identifier.fromNamespaceAndPath("skywars2", "map_capture"), CAPTURE_TICKET);
    }

    private record Selection(ServerLevel world, BlockPos first, BlockPos second) { }
    private record Publication(JsonObject manifest, Path directory) { }
    private final GameController game;
    private final Map<UUID, Selection> selections = new HashMap<>();
    private final ExecutorService writer = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "skywars2-map-writer");
        thread.setDaemon(true);
        return thread;
    });
    private Capture capture;
    private CompletableFuture<Publication> publication;
    private UUID author;
    private volatile boolean closed;

    public MapAuthoring(GameController game) { this.game = Objects.requireNonNull(game); }
    public boolean busy() { return !closed && (capture != null || publication != null); }
    public void clearSelection(ServerPlayer player) { if (GameController.operator(player)) selections.remove(player.getUUID()); }
    public AABB selectedBounds(ServerPlayer player) {
        var selection = selections.get(player.getUUID());
        if (selection == null || selection.world != player.level() || selection.first == null || selection.second == null) return null;
        var a = selection.first; var b = selection.second;
        return new AABB(Math.min(a.getX(),b.getX()),Math.min(a.getY(),b.getY()),Math.min(a.getZ(),b.getZ()),
                Math.max(a.getX(),b.getX())+1.0,Math.max(a.getY(),b.getY())+1.0,Math.max(a.getZ(),b.getZ())+1.0);
    }

    public void select(ServerPlayer player, int corner) {
        if (!GameController.operator(player)) return;
        if (corner != 0 && corner != 1) throw new IllegalArgumentException("Corner must be 0 or 1");
        Selection old = selections.get(player.getUUID());
        ServerLevel world = (ServerLevel) player.level();
        if (old == null || old.world != world) old = new Selection(world, null, null);
        BlockPos point = player.blockPosition().immutable();
        selections.put(player.getUUID(), corner == 0 ? new Selection(world, point, old.second)
                : new Selection(world, old.first, point));
        tell(player, "Map position " + corner + ": " + point.getX() + ", " + point.getY() + ", " + point.getZ()
                + " (inclusive, " + world.dimension().identifier() + ")");
    }

    public void save(ServerPlayer player, String mode, String name) {
        if (!GameController.operator(player)) return;
        if (closed || busy() || game.maps().busy() || game.maps().rotationBusy() || game.playing() && !game.developerMode()) {
            tell(player, "Cannot save a map while another save, match or map operation is active."); return;
        }
        boolean bedwars = mode.equals("savebedwars") || mode.equals("bedwars");
        if (!bedwars && !mode.equals("saveskywars") && !mode.equals("skywars")) {
            tell(player, "Unknown map mode."); return;
        }
        name = name.strip();
        if (name.isEmpty() || name.length() > 80 || name.codePoints().anyMatch(Character::isISOControl)) {
            tell(player, "Use a map name of 1 to 80 visible characters."); return;
        }
        Selection selection = selections.get(player.getUUID());
        if (selection == null || selection.first == null || selection.second == null
                || selection.world != player.level()) {
            tell(player, "Set /mapsave pos 0 and /mapsave pos 1 in the same dimension first."); return;
        }
        BlockPos a = selection.first, b = selection.second;
        BlockPos min = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
        BlockPos max = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
        long width = (long) max.getX() - min.getX() + 1, height = (long) max.getY() - min.getY() + 1,
                depth = (long) max.getZ() - min.getZ() + 1;
        if (width > 1024 || depth > 1024 || height > 1024 || width * height * depth > MAX_VOLUME
                || min.getX() < -8192 || max.getX() > 8192 || min.getZ() < -8192 || max.getZ() > 8192
                || min.getY() < selection.world.getMinY() || max.getY() >= selection.world.getMaxY()) {
            tell(player, "Selection exceeds the save limits (1024 per axis, 32M blocks, X/Z within -8192..8192 and within world height)."); return;
        }
        List<ChunkPos> chunks = new ArrayList<>();
        for (int x = min.getX() >> 4; x <= max.getX() >> 4; x++)
            for (int z = min.getZ() >> 4; z <= max.getZ() >> 4; z++) chunks.add(new ChunkPos(x, z));
        if (chunks.size() > 1024) { tell(player, "Selection exceeds 1024 chunks."); return; }
        // Preflight before adding ANY ticket. Existing resident full chunks only: no terrain generation.
        for (ChunkPos chunk : chunks) {
            if (selection.world.getChunkSource().getChunkNow(chunk.x(), chunk.z()) == null
                    || !selection.world.areEntitiesLoaded(chunk.pack())) {
                tell(player, "Chunk " + chunk.x() + ", " + chunk.z()
                        + " is not fully loaded. Visit/load the entire selection, then save again; no chunks were generated.");
                return;
            }
        }
        Capture next = new Capture(selection.world, min, max, chunks, game.maps().nextLocalId(bedwars), bedwars, name);
        capture = next;
        author = player.getUUID();
        try {
            for (ChunkPos chunk : chunks) {
                next.world.getChunkSource().addTicketWithRadius(CAPTURE_TICKET, chunk, 0);
                next.pinned.add(chunk);
            }
            tell(player, "Capturing map " + next.id + " without air (" + width * height * depth + " selected blocks). Keep the selection unchanged until capture finishes.");
        } catch (RuntimeException failure) { fail(failure); }
    }

    public void tick() {
        if (closed) return;
        try {
            if (capture != null) {
                Capture work = capture;
                long deadline = System.nanoTime() + TICK_NANOS;
                int budget = BLOCKS_PER_TICK;
                while (budget-- > 0 && System.nanoTime() < deadline && work.chunkIndex < work.chunks.size()) work.readOne();
                if (work.chunkIndex == work.chunks.size()) {
                    if (work.spawns == 0) throw new IllegalStateException("Selection has no swrg.spawn markers; place spawn eggs before saving.");
                    if (work.blocks.isEmpty()) throw new IllegalStateException("Selection has no non-air blocks.");
                    CompoundTag snapshot = work.finish();
                    JsonObject manifest = work.manifest();
                    Path root = game.maps().localMapRoot();
                    String id = work.id;
                    release(work);
                    capture = null;
                    publication = CompletableFuture.supplyAsync(() -> publish(root, id, snapshot, manifest), writer);
                    tellAuthor("Capture complete. Writing native map " + id + " (" + work.spawns + " spawns).");
                }
            }
            if (publication != null && publication.isDone()) {
                Publication result = publication.join();
                game.maps().registerLocalMap(result.manifest, result.directory);
                publication = null;
                tellAuthor("Saved " + result.manifest.get("name").getAsString() + " ("
                        + result.manifest.get("playerCapacity").getAsInt() + "), map "
                        + result.manifest.get("id").getAsString() + ". It is now available in the map menu.");
                author = null;
            }
        } catch (RuntimeException failure) { fail(failure); }
    }

    private Publication publish(Path root, String id, CompoundTag snapshot, JsonObject manifest) {
        Path temporary = root.resolve(".saving-" + UUID.randomUUID());
        try {
            Files.createDirectories(root);
            Files.createDirectory(temporary);
            NbtIo.writeCompressed(snapshot, temporary.resolve("map.nbt"));
            Files.writeString(temporary.resolve("manifest.json"), manifest.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW);
            if (closed || Thread.currentThread().isInterrupted()) throw new IOException("Map save cancelled during shutdown");
            Path destination = root.resolve(id);
            if (Files.exists(destination)) throw new FileAlreadyExistsException(destination.toString());
            // A complete directory is the commit record; incomplete files never enter the catalog.
            // Atomic publication is required. Failure leaves existing maps untouched.
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
            return new Publication(manifest, destination);
        } catch (IOException failure) {
            throw new CompletionException(failure);
        } finally {
            // Only our private staging files may be removed, never a published or pre-existing map.
            try {
                Files.deleteIfExists(temporary.resolve("map.nbt"));
                Files.deleteIfExists(temporary.resolve("manifest.json"));
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) { }
        }
    }

    private void release(Capture work) {
        for (ChunkPos chunk : work.pinned) work.world.getChunkSource().removeTicketWithRadius(CAPTURE_TICKET, chunk, 0);
        work.pinned.clear();
    }
    private void fail(RuntimeException failure) {
        if (capture != null) release(capture);
        capture = null;
        publication = null;
        Throwable cause = failure instanceof CompletionException && failure.getCause() != null ? failure.getCause() : failure;
        tellAuthor("Map save failed: " + cause.getMessage() + ". Existing maps and the source selection were left untouched.");
        author = null;
    }
    private static void tell(ServerPlayer player, String message) { player.sendSystemMessage(Component.literal(message)); }
    private void tellAuthor(String message) {
        ServerPlayer player = author == null ? null : game.server().getPlayerList().getPlayer(author);
        if (player != null) tell(player, message);
    }
    @Override public void close() {
        closed = true;
        if (capture != null) release(capture);
        capture = null;
        selections.clear();
        // A completed atomic commit is retained and discovered on the next startup.
        writer.shutdownNow();
    }

    private static ListTag ints(int... values) {
        ListTag list = new ListTag(); for (int value : values) list.add(IntTag.valueOf(value)); return list;
    }
    private static ListTag doubles(double... values) {
        ListTag list = new ListTag(); for (double value : values) list.add(DoubleTag.valueOf(value)); return list;
    }
    private static final class Capture {
        final ServerLevel world;
        final BlockPos min, max;
        final List<ChunkPos> chunks, pinned = new ArrayList<>();
        final String id, name;
        final boolean bedwars;
        final ListTag blocks = new ListTag(), entities = new ListTag(), palette = new ListTag();
        final Map<BlockState, Integer> states = new HashMap<>();
        final Set<UUID> entityIds = new HashSet<>();
        final Set<String> teams = new TreeSet<>();
        int chunkIndex, x, y, z, spawns;
        boolean initialized;
        List<Entity> chunkEntities;
        int entityCursor;
        Capture(ServerLevel world, BlockPos min, BlockPos max, List<ChunkPos> chunks, String id, boolean bedwars, String name) {
            this.world = world; this.min = min; this.max = max; this.chunks = List.copyOf(chunks);
            this.id = id; this.bedwars = bedwars; this.name = name;
        }
        void readOne() {
            ChunkPos chunk = chunks.get(chunkIndex);
            var resident = world.getChunkSource().getChunkNow(chunk.x(), chunk.z());
            if (resident == null || !world.areEntitiesLoaded(chunk.pack()))
                throw new IllegalStateException("Selected chunk became unavailable: " + chunk);
            if (chunkEntities != null) {
                if (entityCursor < chunkEntities.size()) captureEntity(chunkEntities.get(entityCursor++));
                else { chunkEntities = null; entityCursor = 0; chunkIndex++; initialized = false; }
                return;
            }
            int minX = Math.max(min.getX(), chunk.getMinBlockX()), maxX = Math.min(max.getX(), chunk.getMaxBlockX());
            int minZ = Math.max(min.getZ(), chunk.getMinBlockZ()), maxZ = Math.min(max.getZ(), chunk.getMaxBlockZ());
            if (!initialized) { x = minX; y = min.getY(); z = minZ; initialized = true; }
            if (resident.getSection(world.getSectionIndex(y)).hasOnlyAir()) {
                y = Math.min(max.getY() + 1, ((y >> 4) + 1) << 4); x = minX; z = minZ;
                if (y <= max.getY()) return;
                AABB box = new AABB(minX,min.getY(),minZ,maxX+1.0,max.getY()+1.0,maxZ+1.0);
                chunkEntities = world.getEntitiesOfClass(Entity.class,box,
                        e -> !(e instanceof Player) && !e.isRemoved() && box.contains(e.position()));
                return;
            }
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = resident.getBlockState(pos);
            if (!state.isAir()) {
                if (blocks.size() >= MAX_BLOCKS) throw new IllegalStateException("Selection exceeds 2M non-air blocks");
                int index = states.computeIfAbsent(state, key -> { palette.add(NbtUtils.writeBlockState(key)); return palette.size() - 1; });
                CompoundTag block = new CompoundTag();
                block.put("pos", ints(x - min.getX(), y - min.getY(), z - min.getZ()));
                block.putInt("state", index);
                var blockEntity = resident.getBlockEntity(pos);
                if (blockEntity != null) block.put("nbt", blockEntity.saveWithFullMetadata(world.registryAccess()));
                blocks.add(block);
            }
            if (++z <= maxZ) return;
            z = minZ;
            if (++x <= maxX) return;
            x = minX;
            if (++y <= max.getY()) return;
            AABB box = new AABB(minX, min.getY(), minZ, maxX + 1.0, max.getY() + 1.0, maxZ + 1.0);
            chunkEntities = world.getEntitiesOfClass(Entity.class, box,
                    e -> !(e instanceof Player) && !e.isRemoved() && box.contains(e.position()));
        }
        void captureEntity(Entity entity) {
                if (entity.isRemoved() || !entityIds.add(entity.getUUID())) return;
                if (entityIds.size() > 4096) throw new IllegalStateException("Selection exceeds 4096 entities");
                TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, world.registryAccess());
                if (!entity.save(output)) return;
                CompoundTag nbt = output.buildResult();
                if (entity.getTeam() != null) {
                    nbt.putString("Team", entity.getTeam().getName()); teams.add(entity.getTeam().getName());
                }
                double rx = entity.getX() - min.getX(), ry = entity.getY() - min.getY(), rz = entity.getZ() - min.getZ();
                CompoundTag entry = new CompoundTag();
                entry.put("pos", doubles(rx, ry, rz));
                BlockPos anchor = entity instanceof net.minecraft.world.entity.decoration.painting.Painting painting
                        ? painting.getPos().subtract(min) : BlockPos.containing(rx, ry, rz);
                entry.put("blockPos", ints(anchor.getX(), anchor.getY(), anchor.getZ()));
                entry.put("nbt", nbt);
                entities.add(entry);
                if (entity.entityTags().contains("swrg.spawn")) spawns++;
        }
        CompoundTag finish() {
            CompoundTag root = new CompoundTag();
            root.put("size", ints(max.getX() - min.getX() + 1, max.getY() - min.getY() + 1, max.getZ() - min.getZ() + 1));
            root.put("palette", palette); root.put("blocks", blocks); root.put("entities", entities);
            return NbtUtils.addCurrentDataVersion(root);
        }
        JsonObject manifest() {
            JsonObject result = new JsonObject();
            result.addProperty("formatVersion", 1); result.addProperty("id", id); result.addProperty("name", name);
            result.addProperty("mode", bedwars ? "bedwars" : "skywars"); result.addProperty("playerCapacity", spawns);
            result.addProperty("available", true); result.addProperty("reason", "");
            JsonArray savedTeams = new JsonArray(); teams.forEach(savedTeams::add); result.add("teams", savedTeams);
            result.addProperty("sourceDimension", world.dimension().identifier().toString());
            JsonObject placement = new JsonObject(); placement.addProperty("template", "luckywars:local/" + id);
            // User: build anywhere, load around the arena's X/Z center while preserving authored Y.
            int width = max.getX()-min.getX()+1, depth = max.getZ()-min.getZ()+1;
            JsonArray origin = new JsonArray(); origin.add(-(width/2)); origin.add(min.getY()); origin.add(-(depth/2));
            JsonArray source = new JsonArray(); source.add(min.getX()); source.add(min.getY()); source.add(min.getZ());
            result.add("sourceOrigin",source); result.addProperty("centeredXZ",true);
            placement.add("origin", origin); placement.addProperty("integrity", 1); placement.addProperty("seed", 0);
            JsonArray placements = new JsonArray(); placements.add(placement); result.add("placements", placements);
            return result;
        }
    }
}
