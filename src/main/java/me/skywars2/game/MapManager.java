package me.skywars2.game;

import me.skywars2.SkyWars2;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.RandomSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Native, tick-budgeted placement of the imported SkyWars structure manifests. */
public final class MapManager {
    private static final String MAP_RESOURCE_ROOT = "/skywars2/maps/";
    private static final int TILE_XZ = 8;
    private static final int TILE_Y = 8;
    private static final int LOBBY_OFFSET_X = 0;
    private static final long TICK_WORK_BUDGET_NANOS = 4_000_000L;
    private static final int MAX_SLICES_PER_TICK = 64;
    // Техническая замена карты не должна выбрасывать содержимое удаляемых контейнеров.
    private static final int MAP_WRITE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;
    private static final String SWRG = "swrg.";
    private static final Identifier CHEST_COUNT_MODIFIER = Identifier.fromNamespaceAndPath("minecraft", "count");
    private static final Logger LOGGER = LoggerFactory.getLogger("skywars2-maps");
    private static final ResourceKey<Level> ARENA_KEY = ResourceKey.create(
            Registries.DIMENSION, Identifier.fromNamespaceAndPath("skywars2", "arena"));
    private static final TicketType LOADING_TICKET = new TicketType(TicketType.NO_TIMEOUT,
            TicketType.FLAG_LOADING | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);

    /** Called during mod initialization, before Minecraft freezes its registries. */
    public static void registerLoadingTicket() {
        Registry.register(BuiltInRegistries.TICKET_TYPE,
                Identifier.fromNamespaceAndPath("skywars2", "map_loading"), LOADING_TICKET);
    }

    private final MinecraftServer server;
    private final ServerLevel level;
    // Фоновый исполнитель готовит шаблоны; размещение блоков и обработка очереди work остаются на тике сервера.
    private final ExecutorService templatePreloader = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "skywars2-template-preloader");
        thread.setDaemon(true);
        return thread;
    });
    private final Path boundsJournal;
    private final Map<String, Manifest> manifests = new HashMap<>();
    private final List<MapInfo> catalog;
    private final Queue<Work> work = new ArrayDeque<>();
    private final Queue<Entity> lobbyEntityCandidates = new ArrayDeque<>();
    private int lobbyEntityAuditTicks;
    private final List<BoundingBox> activeBoxes = new ArrayList<>();
    // Journal tracks both regions; these runtime partitions let map selection preserve the live lobby.
    private List<BoundingBox> lobbyBounds = List.of();
    private List<BoundingBox> mapBounds = List.of();
    private List<LobbyCell> lobbyCells = List.of();
    private final Set<BlockPos> lobbyFootprint = new HashSet<>();
    private final Map<BlockPos, BlockSnapshot> lobbyVisible = new HashMap<>();
    private final Map<BlockPos, BlockSnapshot> mapUnderLobby = new HashMap<>();
    private boolean lobbyOverlayActive;
    private boolean placingMapSlice;
    private boolean applyingMapWork;
    private List<BoundingBox> placementExternalBounds = List.of();
    private final Set<BlockPos> mapWritesUnderLobby = new HashSet<>();
    private final Map<Long, BoundingBox> changedBlockBounds = new HashMap<>();
    private final List<BlockPos> cageBlocks = new ArrayList<>();
    private final Set<String> missingLootReported = new HashSet<>();
    private record ChunkLoad(ServerLevel world,ChunkPos position) { }
    private final Map<ChunkLoad, CompletableFuture<?>> chunkLoads = new HashMap<>();
    private Map<Identifier, PreparedTemplate> preparedTemplates = Map.of();
    private CompletableFuture<Map<Identifier, PreparedTemplate>> preparation;
    private Runnable afterPreparation;
    private long preparationStarted;
    private long preparationMillis;
    private long maxWorkTickNanos;
    private int workTicks;
    private int completedSlices;
    private List<Spawn> spawns = List.of();
    private Vec3 look = Vec3.ZERO;
    private Consumer<Result> completion;
    private String pendingMap;
    private List<BoundingBox> pendingBounds = List.of();
    private boolean busy;
    private boolean reading;
    private volatile boolean closed;
    private boolean boundsJournalValid = true;
    private boolean blockBoundsDirty;
    private boolean changeBoundsOutOfRangeReported;
    private int ticksSinceBoundsFlush;

    public MapManager(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
        this.level = server.getLevel(ARENA_KEY);
        this.boundsJournal = server.getWorldPath(LevelResource.ROOT)
                .resolve("data/luckywars/arena-bounds.json");
        this.catalog = readCatalog();
        activeBoxes.addAll(readBoundsJournal());
        diagnoseChestLootTables();
    }

    /** The isolated void arena. A missing LevelStem is reported during load/lobby callbacks. */
    public ServerLevel level() {
        return level;
    }

    /** Lobby spawn centered over the arena. */
    public Vec3 lobbyPosition() {
        return new Vec3(0.5, 200.0, 0.5);
    }

    public List<MapInfo> catalog() {
        return catalog;
    }

    public MapInfo info(String id) {
        if (id == null) return null;
        return catalog.stream().filter(entry -> entry.id().equals(id)).findFirst().orElse(null);
    }

    public boolean busy() {
        return busy;
    }

    public boolean preparing() { return busy && reading; }

    /** Keep queued native fluid ticks intact while clear/paste slices replace the world. */
    public boolean suspendsFluidTicks() { return busy && !reading; }

    public int loadingChunkCount() { return chunkLoads.size(); }

    public boolean brokenAvailable(String id) {
        Manifest manifest = manifests.get(id);
        return manifest != null && manifest.brokenAvailable && !manifest.brokenPlacements.isEmpty();
    }

    /** Places several bounded regions until the per-tick time or work-count budget is reached. */
    public void tick() {
        if (level == null) return;
        if (++ticksSinceBoundsFlush >= 20) flushChangedBlockBounds();
        if (!busy) { maintainLobbyOverlay(); return; }
        if (reading) {
            if (!preparation.isDone()) { maintainLobbyOverlay(); return; }
            try {
                preparedTemplates = preparation.join(); // Already completed; never waits on this thread.
                preparationMillis = (System.nanoTime() - preparationStarted) / 1_000_000;
                reading = false;
                afterPreparation.run();
            } catch (CompletionException error) {
                fail(Component.translatable("skywars2.map.error.preload", error.getCause().getMessage()));
            } catch (RuntimeException error) {
                fail(Component.translatable("skywars2.map.error.placement", error.getMessage()));
            }
            maintainLobbyOverlay(); return;
        }
        long started = System.nanoTime();
        long operationStarted = preparationStarted;
        long deadline = started + TICK_WORK_BUDGET_NANOS;
        workTicks++;
        try {
            // Warm only the next few slices; requests do not join chunk-generation futures.
            int ahead = 0;
            for (Work next : work) {
                chunksReady(next.bounds);
                if (++ahead >= 4 || System.nanoTime() >= deadline) break;
            }
            for (int completed = 0; completed < MAX_SLICES_PER_TICK; completed++) {
                Work next = work.peek();
                if (next == null) {
                    maxWorkTickNanos = Math.max(maxWorkTickNanos, System.nanoTime() - started);
                    finishPlacement(); return;
                }
                if (System.nanoTime() >= deadline || !chunksReady(next.bounds)) return;
                work.remove();
                applyingMapWork = true;
                try { next.action.accept(level); }
                finally { applyingMapWork = false; }
                completedSlices++;
                if (work.isEmpty()) {
                    maxWorkTickNanos = Math.max(maxWorkTickNanos, System.nanoTime() - started);
                    finishPlacement(); return;
                }
            }
        } catch (RuntimeException error) {
            fail(Component.translatable("skywars2.map.error.placement", error.getMessage()));
        } finally {
            if (operationStarted == preparationStarted) {
                maxWorkTickNanos = Math.max(maxWorkTickNanos, System.nanoTime() - started);
            }
            maintainLobbyOverlay();
        }
    }

    private boolean chunksReady(BoundingBox bounds) {
        boolean ready = true;
        // Include neighboring blocks accessed by native fluid/shape updates.
        for (int x = (bounds.minX() - 1) >> 4; x <= (bounds.maxX() + 1) >> 4; x++) {
            for (int z = (bounds.minZ() - 1) >> 4; z <= (bounds.maxZ() + 1) >> 4; z++) {
                ChunkPos pos = new ChunkPos(x, z);
                CompletableFuture<?> future = prepareChunk(pos);
                if (!future.isDone()) { ready = false; continue; }
                Object result = future.join();
                if (result instanceof ChunkResult<?> chunkResult && !chunkResult.isSuccess()) {
                    throw new IllegalStateException("Could not load map chunk " + pos);
                }
            }
        }
        return ready;
    }

    /** Общая асинхронная загрузка для карты и событий; tickets освобождаются при следующей очистке/остановке. */
    CompletableFuture<?> prepareChunk(ChunkPos pos) {
        return prepareChunk(level,pos);
    }
    CompletableFuture<?> prepareChunk(ServerLevel world,ChunkPos pos) {
        return chunkLoads.computeIfAbsent(new ChunkLoad(world,pos),
                chunk -> chunk.world.getChunkSource().addTicketAndLoadWithRadius(LOADING_TICKET, chunk.position, 0));
    }

    private void releaseChunkTickets() {
        for (ChunkLoad chunk : chunkLoads.keySet()) {
            chunk.world.getChunkSource().removeTicketWithRadius(LOADING_TICKET, chunk.position, 0);
        }
        chunkLoads.clear();
    }

    public void load(String id, boolean broken, Consumer<Result> completion) {
        if (busy) {
            completion.accept(new Result(false, Component.translatable("skywars2.map.error.busy")));
            return;
        }
        Manifest manifest = manifests.get(id);
        List<Placement> placements = manifest == null ? List.of()
                : broken ? manifest.brokenPlacements : manifest.placements;
        preloadTemplatesAsync(placements.stream().map(Placement::template).toList(),
                () -> loadPrepared(id, broken), completion);
    }

    private void loadPrepared(String id, boolean broken) {
        if (level == null) {
            fail(Component.translatable("skywars2.map.error.dimension_missing"));
            return;
        }
        if (!boundsJournalValid) {
            fail(Component.translatable("skywars2.map.error.journal_unreadable"));
            return;
        }
        Manifest manifest = manifests.get(id);
        MapInfo mapInfo = info(id);
        if (manifest == null || mapInfo == null) {
            fail(Component.translatable("skywars2.map.error.unknown", id));
            return;
        }
        if (!mapInfo.available()) {
            fail(mapInfo.unavailableReason());
            return;
        }
        List<Placement> placements = broken ? manifest.brokenPlacements : manifest.placements;
        if (broken && (!manifest.brokenAvailable || placements.isEmpty())) {
            fail(Component.translatable("skywars2.map.error.broken_missing", id));
            return;
        }
        Component missing = validateTemplates(placements);
        if (missing != null) {
            fail(missing);
            return;
        }
        Component heightError = validateHeight(placements);
        if (heightError != null) {
            fail(heightError);
            return;
        }

        List<BoundingBox> targetBounds;
        try {
            targetBounds = boundsFor(placements);
        } catch (RuntimeException error) {
            fail(Component.translatable("skywars2.map.error.inspect_map", id, error.getMessage()));
            return;
        }
        recoverLoadedFluidBounds();
        flushChangedBlockBounds();
        if (blockBoundsDirty) {
            fail(Component.translatable("skywars2.map.error.persist_changes"));
            return;
        }
        List<BoundingBox> recoveryBounds = combineBounds(activeBoxes, targetBounds);
        if (!writeBoundsJournal(recoveryBounds)) {
            fail(Component.translatable("skywars2.map.error.save_bounds"));
            return;
        }

        pendingMap = id;
        pendingBounds = targetBounds;
        placementExternalBounds = List.of();
        removeMapEntitiesWithin(mapBounds);
        restoreCages();
        clearForcedChunksOutsideLobby();
        mapUnderLobby.clear();
        for (LobbyCell cell : lobbyCells) if (insideAny(cell.pos, mapBounds) || insideAny(cell.pos, targetBounds))
            mapUnderLobby.put(cell.pos, BlockSnapshot.air());
        addClearWork(mapBounds, lobbyOverlayActive);
        mapBounds = combineBounds(mapBounds, targetBounds);
        replaceActiveBounds(recoveryBounds);
        try {
            addPasteWork(placements);
        } catch (RuntimeException error) {
            fail(Component.translatable("skywars2.map.error.prepare_map", id, error.getMessage()));
            return;
        }
        if (work.isEmpty()) finishPlacement();
    }

    public void lobby(Consumer<Result> completion) {
        if (busy) {
            completion.accept(new Result(false, Component.translatable("skywars2.map.error.busy")));
            return;
        }
        preloadTemplatesAsync(List.of("swrg:lobby"), this::lobbyPrepared, completion);
    }

    private void lobbyPrepared() {
        if (level == null) {
            fail(Component.translatable("skywars2.map.error.dimension_missing"));
            return;
        }
        if (!boundsJournalValid) {
            fail(Component.translatable("skywars2.map.error.journal_unreadable"));
            return;
        }
        PreparedTemplate template = template("swrg:lobby");
        if (template == null) {
            fail(Component.translatable("skywars2.map.error.template_missing", "swrg:lobby"));
            return;
        }
        BlockPos origin = new BlockPos(-5 + LOBBY_OFFSET_X, 199, -5);
        Placement lobby = new Placement("swrg:lobby", origin, 1.0f, 1L);
        List<BoundingBox> targetBounds;
        try {
            targetBounds = boundsFor(List.of(lobby));
        } catch (RuntimeException error) {
            fail(Component.translatable("skywars2.map.error.inspect_lobby", error.getMessage()));
            return;
        }
        recoverLoadedFluidBounds();
        flushChangedBlockBounds();
        if (blockBoundsDirty) {
            fail(Component.translatable("skywars2.map.error.persist_changes"));
            return;
        }
        List<BoundingBox> recoveryBounds = combineBounds(activeBoxes, targetBounds);
        if (!writeBoundsJournal(recoveryBounds)) {
            fail(Component.translatable("skywars2.map.error.save_bounds"));
            return;
        }

        pendingMap = null;
        pendingBounds = targetBounds;
        placementExternalBounds = List.of();
        removeArenaEntities();
        restoreCages();
        clearForcedChunks();
        boolean preserveMap = !mapBounds.isEmpty();
        if (preserveMap) mapBounds = combineBounds(mapBounds, activeBoxes);
        if (lobbyOverlayActive) removeLobbyOverlay();
        mapUnderLobby.clear();
        if (preserveMap) {
            for (LobbyCell cell : lobbyCells) {
                mapUnderLobby.put(cell.pos, snapshot(cell.pos));
            }
        }
        addClearWork(preserveMap ? List.of() : activeBoxes, false);
        lobbyBounds = targetBounds;
        if (!preserveMap) mapBounds = List.of();
        replaceActiveBounds(recoveryBounds);
        try {
            addPasteWork(List.of(lobby));
        } catch (RuntimeException error) {
            fail(Component.translatable("skywars2.map.error.prepare_lobby", error.getMessage()));
            return;
        }
        if (work.isEmpty()) finishPlacement();
    }

    public List<Spawn> spawns() {
        return spawns;
    }

    public Vec3 look() {
        return look;
    }

    /** Builds the legacy open glass spawn cage; opening it removes every glass block. */
    public void cages(boolean enabled) {
        if (level == null) return;
        if (!enabled) {
            restoreCages();
            return;
        }
        if (!cageBlocks.isEmpty()) return;
        for (Spawn spawn : spawns) {
            BlockPos feet = BlockPos.containing(spawn.position());
            // developer/bars first clears old glass in the complete volume. This
            // prevents template leftovers from surviving beneath the new cage.
            for (int y = -2; y <= 5; y++) {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockPos pos = feet.offset(x, y, z);
                        if (level.getBlockState(pos).is(Blocks.GLASS)) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }
            // game/start/bars replaces the remaining glass with the selected cage
            // material. The default variant is plain glass: five floor blocks
            // and four open posts, each four blocks high (21 blocks total).
            for (int y = -1; y <= 3; y++) {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        if (Math.abs(x) == 1 && Math.abs(z) == 1) continue;
                        if (y >= 0 && x == 0 && z == 0) continue;
                        BlockPos pos = feet.offset(x, y, z);
                        level.setBlock(pos, Blocks.GLASS.defaultBlockState(), 2);
                        cageBlocks.add(pos.immutable());
                    }
                }
            }
        }
    }

    /** Assigns ordinary SkyWars loot, or the center pool for the later upgrade phase. */
    public void fillChests(boolean upgrade) {
        if (level == null) return;
        int generatedChests = 0;
        int encodedCounts = 0;
        int multiCounts = 0;
        for (Entity marker : allArenaEntities()) {
            // Метки swrg.center/swrg.chest есть и у вагонеток АирДропа: они не являются точками блоков карты.
            if (!(marker instanceof Marker)) continue;
            String table = null;
            if (marker.entityTags().contains("swrg.center")) table = "center";
            else if (marker.entityTags().contains("swrg.midle")) table = "midle";
            else if (marker.entityTags().contains("swrg.default")) table = "default";
            if (table == null) continue;

            BlockPos pos = BlockPos.containing(marker.position());
            if (!level.getBlockState(pos).is(Blocks.CHEST)
                    && !level.getBlockState(pos).is(Blocks.BARREL)) {
                level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof RandomizableContainerBlockEntity container) {
                String lootCategory = upgrade ? "center" : table;
                String lootId = "swrg:chests/" + lootCategory + "_0";
                ResourceKey<LootTable> loot = lootKey(lootId);
                // The original SWRG refill first removes Items, then assigns a
                // new lazy loot table. Do the same so opened chests never retain
                // old-round contents alongside a newly selected table.
                container.clearContent();
                if (server.reloadableRegistries().getLootTable(loot) == LootTable.EMPTY) {
                    if (missingLootReported.add(lootId)) {
                        LOGGER.error("SWRG chest loot table {} is missing or failed to load; leaving chest unfilled.", lootId);
                    }
                    container.setChanged();
                    continue;
                }
                // Keep the table lazy, as vanilla containers do.
                container.setLootTable(loot);
                container.setLootTableSeed(level.getRandom().nextLong());
                // The legacy chest refill forces lazy generation, then moves
                // its synthetic minecraft:count modifier into stack count.
                container.getItem(0);
                int[] normalized = normalizeLootCounts(container);
                generatedChests++;
                encodedCounts += normalized[0];
                multiCounts += normalized[1];
                container.setChanged();
            }
        }
        LOGGER.info("SWRG refill upgrade={} generatedChests={} syntheticCounts={} countsAboveOne={}",
                upgrade, generatedChests, encodedCounts, multiCounts);
    }

    /** Общая нормализация старого количества и для блоков, и для вагонеток с сундуком. */
    static int[] normalizeLootCounts(Container container) {
        int encodedCounts = 0;
        int multiCounts = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) continue;
            ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
            if (modifiers == null) continue;
            List<ItemAttributeModifiers.Entry> retained = new ArrayList<>(modifiers.modifiers().size());
            Integer count = null;
            for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
                if (entry.modifier().id().equals(CHEST_COUNT_MODIFIER)) {
                    count = (int) entry.modifier().amount();
                } else {
                    retained.add(entry);
                }
            }
            if (count != null) {
                encodedCounts++;
                if (count > 1) multiCounts++;
                stack.setCount(Math.max(1, count));
                stack.set(DataComponents.ATTRIBUTE_MODIFIERS, new ItemAttributeModifiers(retained));
            }
        }
        return new int[] {encodedCounts, multiCounts};
    }

    /** Removes entities from the isolated arena before a fresh map or lobby is placed. */
    public void clearTransientEntities() {
        removeArenaEntities();
    }

    /** Called by ArenaBlockMixin only while a native map template slice is placing. */
    public void noteMapBlockWrite(Level source, BlockPos pos) {
        if (placingMapSlice && lobbyOverlayActive && source == level && insideLobby(pos))
            mapWritesUnderLobby.add(pos.immutable());
    }

    /** Reveals the map state captured beneath the lobby immediately before match play. */
    public void removeLobbyOverlay() {
        if (!lobbyOverlayActive || level == null) return;
        for (LobbyCell cell : lobbyCells) restoreSnapshot(cell.pos, mapUnderLobby.getOrDefault(cell.pos, BlockSnapshot.air()));
        lobbyOverlayActive = false;
        mapWritesUnderLobby.clear();
        removeLobbyEntities();
    }

    /** Lobby whitelist in the arena; other dimensions preserve only players. */
    public void queueLobbyEntity(Entity entity) {
        if (!(entity instanceof Player)) lobbyEntityCandidates.add(entity);
    }

    public void resetLobbyEntityCleanup() {
        lobbyEntityCandidates.clear();
        lobbyEntityAuditTicks = 0;
    }

    public void cleanupLobbyEntities() {
        // Load callbacks run before callers may finish assigning source tags.
        // Inspect queued entities here, at the end of the server tick.
        if (lobbyEntityAuditTicks-- <= 0) {
            lobbyEntityAuditTicks = 19;
            // Entry sweep also finds pre-existing/unloaded-during-transition entities.
            // Periodic audits catch direct tag edits by commands or other mods.
            for (ServerLevel world : server.getAllLevels()) {
                for (Entity entity : world.getAllEntities()) queueLobbyEntity(entity);
            }
        }
        Entity entity;
        while ((entity = lobbyEntityCandidates.poll()) != null) {
            if (!entity.isRemoved() && !keepLobbyEntity(entity)) entity.discard();
        }
    }

    private boolean keepLobbyEntity(Entity entity) {
        // Подсветка редактора не является частью карты, даже если сохранена внутри её bounds.
        if (entity.entityTags().contains("marker")
                && (entity instanceof net.minecraft.world.entity.item.FallingBlockEntity
                    || entity instanceof net.minecraft.world.entity.Display.BlockDisplay)) return false;
        // Старый дроп не становится частью подготовленной карты из-за исходных swrg.center/swrg.chest.
        if (entity.entityTags().contains("skywars2.airdrop")) return false;
        // Подготовленная карта существует одновременно с лобби: её сущности нужны при будущем старте.
        if (entity.level() == level && mapBounds.stream().anyMatch(box -> box.isInside(entity.blockPosition()))) return true;
        var game = SkyWars2.game();
        if (entity.level() == level && game != null && game.items().currentEpoch(entity)) return true;
        Set<String> tags = entity.entityTags();
        return entity.level() == level && (tags.contains("swrg.quickstart")
                || tags.contains("swrg.look") || tags.contains("swrg.spawn")
                || tags.contains("swrg.center") || tags.contains("swrg.midle")
                || tags.contains("swrg.default") || tags.contains("map_object")
                || tags.contains("strict_map_object")
                || entity instanceof net.minecraft.world.entity.AreaEffectCloud
                && (tags.contains("swrg.delete") || tags.contains("swrg.lobby")));
    }

    public void shutdown() {
        closed = true;
        resetLobbyEntityCleanup();
        // Interrupting ZipFS reads closes Minecraft's shared resource-pack channel.
        // Finish the current read, then abandon CPU preparation at the next checkpoint.
        templatePreloader.shutdown();
        if (preparation != null) preparation.cancel(false);
        try {
            if (!templatePreloader.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.warn("Map reader did not finish before resource-pack shutdown.");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        flushChangedBlockBounds();
        work.clear();
        preparedTemplates = Map.of();
        releaseChunkTickets();
        afterPreparation = null;
        busy = false;
        completion = null;
        pendingMap = null;
    }

    /** Records a server-thread block mutation in the arena for eventual cleanup. */
    public void recordBlockChange(BlockPos pos) {
        if (pos == null) return;
        // Loader writes are already covered by its persisted recovery envelope.
        // Native flow and player edits in LOBBY/LOADING still need their own bounds.
        if (applyingMapWork || placingMapSlice) return;
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        if (x < -8192 || x > 8192 || y < -2048 || y > 4096 || z < -8192 || z > 8192) {
            if (!changeBoundsOutOfRangeReported) {
                changeBoundsOutOfRangeReported = true;
                LOGGER.error("Ignoring arena block change outside bounds-journal limits: {}, {}, {}", x, y, z);
            }
            return;
        }
        int chunkX = x >> 4, chunkZ = z >> 4, yBand = y >> 7;
        // Keep each journal cuboid small even if edits in one chunk span a
        // large vertical range: 11 bits each for bounded chunk coordinates,
        // then 8 bits for the 128-block vertical band.
        long chunkKey = ((long) (chunkX + 1024) << 20)
                | ((long) (chunkZ + 1024) << 8) | ((yBand + 16) & 0xffL);
        BoundingBox point = new BoundingBox(x, y, z, x, y, z);
        BoundingBox previous = changedBlockBounds.get(chunkKey);
        changedBlockBounds.put(chunkKey, previous == null ? point : union(previous, point));
        blockBoundsDirty = true;
    }

    /** Recover fluids left by older builds or direct chunk writers without scanning every cell. */
    private void recoverLoadedFluidBounds() {
        var cache = level.getChunkSource();
        var holders = ((me.skywars2.mixin.ArenaChunkAccess) cache.chunkMap).skywars2$visibleChunks();
        for (var holder : holders.values()) {
            var pos = holder.getPos();
            var chunk = cache.getChunkNow(pos.x(), pos.z());
            if (chunk == null) continue;
            var sections = chunk.getSections();
            for (int index = 0; index < sections.length; index++) {
                var section = sections[index];
                if (section.hasOnlyAir() || !section.maybeHas(state -> !state.getFluidState().isEmpty())) continue;
                int y = chunk.getSectionYFromSectionIndex(index) << 4;
                recordBlockChange(new BlockPos(pos.getMinBlockX(), y, pos.getMinBlockZ()));
                recordBlockChange(new BlockPos(pos.getMaxBlockX(), y + 15, pos.getMaxBlockZ()));
            }
        }
    }

    private void flushChangedBlockBounds() {
        ticksSinceBoundsFlush = 0;
        if (!blockBoundsDirty || !boundsJournalValid) return;
        List<BoundingBox> expanded = new ArrayList<>(activeBoxes);
        expanded.addAll(changedBlockBounds.values());
        expanded = compactBounds(expanded);
        for (BoundingBox box : expanded) {
            if (box.maxX() - box.minX() > 2048 || box.maxY() - box.minY() > 1024
                    || box.maxZ() - box.minZ() > 2048) {
                if (!changeBoundsOutOfRangeReported) {
                    changeBoundsOutOfRangeReported = true;
                    LOGGER.error("Arena changes exceed the safe cleanup-journal bounds; retaining pending block bounds.");
                }
                return;
            }
        }
        if (writeBoundsJournal(expanded)) {
            // The next replacement clears mapBounds, not just the recovery journal.
            var edits = new ArrayList<>(changedBlockBounds.values());
            mapBounds = combineBounds(mapBounds, edits);
            if (suspendsFluidTicks()) placementExternalBounds = combineBounds(placementExternalBounds, edits);
            activeBoxes.clear();
            activeBoxes.addAll(expanded);
            changedBlockBounds.clear();
            blockBoundsDirty = false;
        }
    }

    private void begin(Consumer<Result> callback) {
        busy = true;
        completion = callback == null ? ignored -> { } : callback;
        work.clear();
    }

    private void preloadTemplatesAsync(List<String> ids, Runnable continueOnServerThread,
                                       Consumer<Result> callback) {
        if (closed) {
            callback.accept(new Result(false, Component.translatable("skywars2.map.error.shutdown")));
            return;
        }
        begin(callback);
        reading = true;
        afterPreparation = continueOnServerThread;
        preparationStarted = System.nanoTime();
        preparationMillis = maxWorkTickNanos = 0;
        workTicks = completedSlices = 0;
        var manager = server.getStructureTemplateManager();
        var blocks = server.registryAccess().lookupOrThrow(Registries.BLOCK);
        try {
            preparation = CompletableFuture.supplyAsync(() -> {
                Map<Identifier, PreparedTemplate> prepared = new HashMap<>();
                for (String rawId : ids.stream().distinct().toList()) {
                    if (closed || Thread.currentThread().isInterrupted()) {
                        throw new java.util.concurrent.CancellationException("Map loader stopped");
                    }
                    Identifier id = identifier(rawId);
                    try {
                        StructureTemplate template = manager.get(id).orElse(null);
                        if (template != null) prepared.put(id, splitTemplate(template, blocks));
                    } finally {
                        // The full template is never needed on the server thread afterwards.
                        manager.remove(id);
                    }
                }
                return Map.copyOf(prepared);
            }, templatePreloader);
        } catch (RejectedExecutionException error) {
            fail(Component.translatable("skywars2.map.error.preloader_unavailable"));
        }
    }

    /** Spell preparation shares the existing decoder, slice format and bounded-lifetime worker. */
    java.util.concurrent.CompletableFuture<Map<Identifier,List<TemplateSlice>>> prepareSpellTemplates(List<Identifier> ids) {
        var manager=server.getStructureTemplateManager();
        var blocks=server.registryAccess().lookupOrThrow(Registries.BLOCK);
        return computeFromSnapshot(()-> {
            var result=new HashMap<Identifier,List<TemplateSlice>>();
            for(var id:ids) {
                var source=manager.get(id).orElseThrow(()->new IllegalStateException("Missing spell template "+id));
                result.put(id,splitTemplate(source,blocks).slices());
            }
            return Map.copyOf(result);
        });
    }
    /** Only immutable or detached snapshots may be submitted; world access remains on the server thread. */
    <T> java.util.concurrent.CompletableFuture<T> computeFromSnapshot(java.util.function.Supplier<T> computation) {
        return java.util.concurrent.CompletableFuture.supplyAsync(computation,templatePreloader);
    }

    /** Sparse source pattern for persistent auras: explicit air is meaningful, missing records do not write. */
    static final class BlockPattern {
        final Vec3i size;
        private final BlockState[] states;
        BlockPattern(Vec3i size,BlockState[] states) { this.size=size; this.states=states; }
        BlockState state(int x,int y,int z) {
            if(x<0 || y<0 || z<0 || x>=size.getX() || y>=size.getY() || z>=size.getZ()) return null;
            return states[(y*size.getZ()+z)*size.getX()+x];
        }
    }
    java.util.concurrent.CompletableFuture<BlockPattern> prepareBlockPattern(Identifier id) {
        var manager=server.getStructureTemplateManager();
        var blocks=server.registryAccess().lookupOrThrow(Registries.BLOCK);
        return computeFromSnapshot(()-> {
            var template=manager.get(id).orElseThrow(()->new IllegalStateException("Missing aura pattern "+id));
            var data=template.save(new CompoundTag());
            if(!data.getListOrEmpty("entities").isEmpty()) throw new IllegalArgumentException("Aura pattern has entities "+id);
            var palette=new ArrayList<BlockState>();
            for(var entry:data.getListOrEmpty("palette")) palette.add(NbtUtils.readBlockState(blocks,(CompoundTag)entry));
            var size=template.getSize(); var states=new BlockState[size.getX()*size.getY()*size.getZ()];
            for(var entry:data.getListOrEmpty("blocks")) {
                var block=(CompoundTag)entry;
                if(block.contains("nbt")) throw new IllegalArgumentException("Aura pattern has block entity "+id);
                var pos=block.getListOrEmpty("pos");
                int x=pos.getIntOr(0,0),y=pos.getIntOr(1,0),z=pos.getIntOr(2,0);
                states[(y*size.getZ()+z)*size.getX()+x]=palette.get(block.getIntOr("state",0));
            }
            return new BlockPattern(size,states);
        });
    }

    /** Split once in the worker. Each native placement later sees at most 8x8x8 blocks. */
    private PreparedTemplate splitTemplate(StructureTemplate template,
                                                   net.minecraft.core.HolderGetter<net.minecraft.world.level.block.Block> blocks) {
        if (closed) throw new java.util.concurrent.CancellationException("Map loader stopped");
        CompoundTag full = template.save(new CompoundTag());
        Vec3i size = template.getSize();
        Map<BlockPos, CompoundTag> tiles = new TreeMap<>(Comparator.<BlockPos>comparingInt(BlockPos::getY)
                .thenComparingInt(BlockPos::getX).thenComparingInt(BlockPos::getZ));
        for (String kind : List.of("blocks", "entities")) {
            int inspected = 0;
            for (var tag : full.getListOrEmpty(kind)) {
                if ((inspected++ & 1023) == 0 && closed) {
                    throw new java.util.concurrent.CancellationException("Map loader stopped");
                }
                CompoundTag entry = (CompoundTag) tag;
                if (kind.equals("entities")) {
                    CompoundTag nbt = entry.getCompoundOrEmpty("nbt");
                    String id = nbt.getStringOr("id", "");
                    // Старые debug falling_block могли дропать бетон или попасть под отвердение.
                    // Убираем их в фоне до спавна/первого тика, сохраняя настоящие Marker-точки.
                    if ((id.equals("minecraft:falling_block") || id.equals("minecraft:block_display"))
                            && nbt.getListOrEmpty("Tags").contains(net.minecraft.nbt.StringTag.valueOf("marker"))) continue;
                }
                ListTag pos = entry.getListOrEmpty(kind.equals("blocks") ? "pos" : "blockPos");
                int x = pos.getIntOr(0, 0), y = pos.getIntOr(1, 0), z = pos.getIntOr(2, 0);
                // These entities were also outside all bounding boxes in the previous loader.
                if (x < 0 || y < 0 || z < 0 || x >= size.getX() || y >= size.getY() || z >= size.getZ()) continue;
                BlockPos tile = new BlockPos(x / TILE_XZ * TILE_XZ, y / TILE_Y * TILE_Y, z / TILE_XZ * TILE_XZ);
                CompoundTag partial = tiles.computeIfAbsent(tile, ignored -> {
                    CompoundTag part = new CompoundTag();
                    part.put("size", full.getListOrEmpty("size"));
                    if (full.contains("palettes")) part.put("palettes", full.getListOrEmpty("palettes"));
                    else part.put("palette", full.getListOrEmpty("palette"));
                    part.put("blocks", new ListTag());
                    part.put("entities", new ListTag());
                    return part;
                });
                partial.getListOrEmpty(kind).add(entry.copy());
            }
        }
        List<TemplateSlice> slices = new ArrayList<>();
        for (var entry : tiles.entrySet()) {
            if (closed) throw new java.util.concurrent.CancellationException("Map loader stopped");
            BlockPos min = entry.getKey();
            BoundingBox bounds = new BoundingBox(min.getX(), min.getY(), min.getZ(),
                    Math.min(min.getX() + TILE_XZ - 1, size.getX() - 1),
                    Math.min(min.getY() + TILE_Y - 1, size.getY() - 1),
                    Math.min(min.getZ() + TILE_XZ - 1, size.getZ() - 1));
            StructureTemplate part = new StructureTemplate();
            part.load(blocks, entry.getValue());
            slices.add(new TemplateSlice(part, bounds));
        }
        return new PreparedTemplate(size, List.copyOf(slices));
    }

    private void addClearWork(List<BoundingBox> boxes) { addClearWork(boxes, false); }

    private void addClearWork(List<BoundingBox> boxes, boolean protectLobby) {
        for (BoundingBox box : boxes) {
            for (int y = box.minY(); y <= box.maxY(); y += TILE_Y) {
                for (int x = box.minX(); x <= box.maxX(); x += TILE_XZ) {
                    for (int z = box.minZ(); z <= box.maxZ(); z += TILE_XZ) {
                        BoundingBox slice = new BoundingBox(x, y, z,
                                Math.min(x + TILE_XZ - 1, box.maxX()),
                                Math.min(y + TILE_Y - 1, box.maxY()),
                                Math.min(z + TILE_XZ - 1, box.maxZ()));
                        work.add(new Work(slice, world -> {
                            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
                            for (int by = slice.minY(); by <= slice.maxY(); by++) {
                                for (int bx = slice.minX(); bx <= slice.maxX(); bx++) {
                                    for (int bz = slice.minZ(); bz <= slice.maxZ(); bz++) {
                                        cursor.set(bx, by, bz);
                                        if (protectLobby && lobbyOverlayActive && insideLobby(cursor)) {
                                            mapUnderLobby.put(cursor.immutable(), BlockSnapshot.air());
                                            continue;
                                        }
                                        if (world.getBlockState(cursor) != Blocks.AIR.defaultBlockState()) {
                                            world.setBlock(cursor, Blocks.AIR.defaultBlockState(), MAP_WRITE_FLAGS);
                                        }
                                    }
                                }
                            }
                        }));
                    }
                }
            }
        }
    }

    private List<BoundingBox> boundsFor(List<Placement> placements) {
        List<BoundingBox> result = new ArrayList<>();
        for (Placement placement : placements) {
            PreparedTemplate template = Objects.requireNonNull(template(placement.template), placement.template);
            Vec3i size = template.size;
            if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0) {
                throw new IllegalStateException("Empty structure template " + placement.template);
            }
            BlockPos origin = placement.origin;
            result.add(new BoundingBox(origin.getX(), origin.getY(), origin.getZ(),
                    origin.getX() + size.getX() - 1,
                    origin.getY() + size.getY() - 1,
                    origin.getZ() + size.getZ() - 1));
        }
        return compactBounds(result);
    }

    private static List<BoundingBox> combineBounds(List<BoundingBox> first, List<BoundingBox> second) {
        List<BoundingBox> combined = new ArrayList<>(first);
        combined.addAll(second);
        return compactBounds(combined);
    }

    private static boolean insideAny(BlockPos pos, List<BoundingBox> bounds) {
        for (var box : bounds) if (box.isInside(pos)) return true;
        return false;
    }

    /** Combines touching/overlapping cuboids so the journal stays small across many map changes. */
    private static List<BoundingBox> compactBounds(List<BoundingBox> source) {
        List<BoundingBox> result = new ArrayList<>();
        for (BoundingBox candidate : source) {
            BoundingBox merged = candidate;
            boolean changed;
            do {
                changed = false;
                for (int i = 0; i < result.size(); i++) {
                    BoundingBox current = result.get(i);
                    if (touches(current, merged)) {
                        merged = union(current, merged);
                        result.remove(i);
                        changed = true;
                        break;
                    }
                }
            } while (changed);
            result.add(merged);
        }
        return List.copyOf(result);
    }

    private static boolean touches(BoundingBox a, BoundingBox b) {
        return a.minX() <= b.maxX() + 1 && b.minX() <= a.maxX() + 1
                && a.minY() <= b.maxY() + 1 && b.minY() <= a.maxY() + 1
                && a.minZ() <= b.maxZ() + 1 && b.minZ() <= a.maxZ() + 1;
    }

    private static BoundingBox union(BoundingBox a, BoundingBox b) {
        return new BoundingBox(Math.min(a.minX(), b.minX()), Math.min(a.minY(), b.minY()), Math.min(a.minZ(), b.minZ()),
                Math.max(a.maxX(), b.maxX()), Math.max(a.maxY(), b.maxY()), Math.max(a.maxZ(), b.maxZ()));
    }

    private List<BoundingBox> readBoundsJournal() {
        if (!Files.exists(boundsJournal)) return List.of();
        try (InputStream input = Files.newInputStream(boundsJournal)) {
            JsonObject root = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("version").getAsInt() != 1) throw new IOException("unsupported journal version");
            JsonArray entries = root.getAsJsonArray("bounds");
            List<BoundingBox> loaded = new ArrayList<>();
            for (JsonElement entry : entries) {
                JsonArray values = entry.getAsJsonArray();
                if (values.size() != 6) throw new IOException("invalid bounds entry length");
                int minX = values.get(0).getAsInt(), minY = values.get(1).getAsInt(), minZ = values.get(2).getAsInt();
                int maxX = values.get(3).getAsInt(), maxY = values.get(4).getAsInt(), maxZ = values.get(5).getAsInt();
                if (minX > maxX || minY > maxY || minZ > maxZ
                        || minX < -8192 || minY < -2048 || minZ < -8192
                        || maxX > 8192 || maxY > 4096 || maxZ > 8192
                        || maxX - minX > 2048 || maxY - minY > 1024 || maxZ - minZ > 2048) {
                    throw new IOException("bounds entry outside safe arena limits");
                }
                loaded.add(new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ));
            }
            return compactBounds(loaded);
        } catch (Exception error) {
            boundsJournalValid = false;
            LOGGER.error("Could not read arena bounds journal {}: {}", boundsJournal, error.getMessage());
            return List.of();
        }
    }

    /** Writes recovery bounds before any clearing or placement, then atomically replaces the journal. */
    private boolean writeBoundsJournal(List<BoundingBox> bounds) {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        JsonArray entries = new JsonArray();
        for (BoundingBox box : compactBounds(bounds)) {
            JsonArray values = new JsonArray();
            values.add(box.minX()); values.add(box.minY()); values.add(box.minZ());
            values.add(box.maxX()); values.add(box.maxY()); values.add(box.maxZ());
            entries.add(values);
        }
        root.add("bounds", entries);
        Path temporary = boundsJournal.resolveSibling(boundsJournal.getFileName() + ".tmp");
        try {
            Files.createDirectories(boundsJournal.getParent());
            Files.writeString(temporary, root.toString() + System.lineSeparator(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, boundsJournal, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, boundsJournal, StandardCopyOption.REPLACE_EXISTING);
            }
            boundsJournalValid = true;
            return true;
        } catch (IOException error) {
            LOGGER.error("Could not write arena bounds journal {}: {}", boundsJournal, error.getMessage());
            try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            return false;
        }
    }

    private void addPasteWork(List<Placement> placements) {
        for (Placement placement : placements) {
            PreparedTemplate template = template(placement.template);
            if (template == null) throw new IllegalStateException("Missing structure template " + placement.template);
            BlockPos origin = placement.origin;
            Vec3i size = template.size;
            if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0) {
                throw new IllegalStateException("Empty structure template " + placement.template);
            }
            for (TemplateSlice part : template.slices) {
                BoundingBox relative = part.bounds;
                BoundingBox bounds = new BoundingBox(
                        origin.getX() + relative.minX(), origin.getY() + relative.minY(), origin.getZ() + relative.minZ(),
                        origin.getX() + relative.maxX(), origin.getY() + relative.maxY(), origin.getZ() + relative.maxZ());
                work.add(new Work(bounds, world -> {
                    StructurePlaceSettings settings = new StructurePlaceSettings()
                            .setBoundingBox(bounds).setRandom(RandomSource.create(placement.seed));
                    if (placement.integrity < 1.0f) settings.addProcessor(INTEGRITY_FILTER);
                    placeMapSlice(world, part.template, origin, settings, placement.seed);
                }));
            }
        }
    }

    /** Постановка заклинаний в уже загруженных чанках; Stone и Ruby используют одну проверку границ. */
    void placeLoadedTemplate(net.minecraft.server.level.ServerPlayer owner,Identifier id,BlockPos origin,float integrity) {
        placeLoadedTemplate(owner,id,origin,integrity,net.minecraft.world.level.block.Rotation.NONE);
    }

    /** Поворот учитывается в bounds до обрезки по загруженным чанкам; Wall использует исходную лестницу. */
    void placeLoadedTemplate(net.minecraft.server.level.ServerPlayer owner,Identifier id,BlockPos origin,float integrity,
                             net.minecraft.world.level.block.Rotation rotation) {
        var level=owner.level(); var template=server.getStructureTemplateManager().get(id).orElse(null);
        if(template==null) return;
        var settings=new StructurePlaceSettings().setIgnoreEntities(true).setKnownShape(true)
                .setRotation(rotation)
                .addProcessor(new net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor(integrity));
        var bounds=template.getBoundingBox(settings,origin);
        int lowY=Math.max(level.getMinY(),bounds.minY()),highY=Math.min(level.getMaxY(),bounds.maxY());
        if(lowY>highY) return;
        long seed=owner.getRandom().nextLong();
        boolean fullyLoaded=true;
        for(int cx=bounds.minX()>>4;cx<=bounds.maxX()>>4;cx++)
            for(int cz=bounds.minZ()>>4;cz<=bounds.maxZ()>>4;cz++) fullyLoaded &= level.hasChunk(cx,cz);
        if(fullyLoaded) {
            settings.setBoundingBox(new BoundingBox(bounds.minX(),lowY,bounds.minZ(),bounds.maxX(),highY,bounds.maxZ()));
            placeMapSlice(level,template,origin,settings,seed);
            return;
        }
        // Ограничиваем каждую постановку загруженными чанками: template не подгружает мир ради заклинания.
        for(int cx=bounds.minX()>>4;cx<=bounds.maxX()>>4;cx++)
            for(int cz=bounds.minZ()>>4;cz<=bounds.maxZ()>>4;cz++) if(level.hasChunk(cx,cz)) {
                var chunkBounds=new BoundingBox(Math.max(bounds.minX(),cx*16),lowY,Math.max(bounds.minZ(),cz*16),
                        Math.min(bounds.maxX(),cx*16+15),highY,Math.min(bounds.maxZ(),cz*16+15));
                settings.setBoundingBox(chunkBounds);
                placeMapSlice(level,template,origin,settings,seed);
            }
    }

    /** Places a native template slice and preserves any writes under the visible lobby overlay. */
    void placeMapSlice(ServerLevel world, StructureTemplate template, BlockPos origin,
                       StructurePlaceSettings settings, long seed) {
        placingMapSlice = true;
        try {
            template.placeInWorld(world, origin, origin, settings, RandomSource.create(seed), MAP_WRITE_FLAGS);
        } finally {
            placingMapSlice = false;
            try {
                if (lobbyOverlayActive) {
                    for (BlockPos pos : List.copyOf(mapWritesUnderLobby)) {
                        mapUnderLobby.put(pos, snapshot(pos));
                        restoreLobbyCell(pos);
                    }
                }
            } finally {
                mapWritesUnderLobby.clear();
            }
        }
    }

    private void finishPlacement() {
        if (level == null) {
            fail(Component.translatable("skywars2.map.error.dimension_missing"));
            return;
        }
        List<Entity> entities = entitiesWithin(pendingBounds);
        forceMarkerChunks(entities);
        List<Spawn> foundSpawns = new ArrayList<>();
        Vec3 foundLook = pendingMap == null ? lobbyPosition() : null;
        for (Entity entity : entities) {
            if (pendingMap == null && entity.entityTags().contains("swrg.lobby")) {
                entity.setCustomName(Component.translatable("swrg.entity.lobby")
                        .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GREEN).withItalic(false)));
            }
            if (entity.entityTags().contains("swrg.spawn")) {
                foundSpawns.add(new Spawn(entity.position(), entity.getYRot(), entity.getXRot()));
            }
            if (entity.entityTags().contains("swrg.look")) foundLook = entity.position();
            else if (foundLook == null && entity.entityTags().contains("swrg.center")) {
                foundLook = entity.position();
            }
        }
        if (pendingMap == null) {
            var quickstartLabels = entities.stream()
                    .filter(entity -> entity instanceof net.minecraft.world.entity.Display.TextDisplay
                            && entity.entityTags().contains("swrg.quickstart"))
                    .map(entity -> (net.minecraft.world.entity.Display.TextDisplay) entity)
                    .sorted(Comparator.comparingDouble((net.minecraft.world.entity.Display.TextDisplay label) -> label.getY()).reversed())
                    .toList();
            // Source lobby.nbt: y=2.5 title, y=2.25 map, y=2.0 hint, relative to placement.
            String[] keys = {"swrg.lobby.quickstart.title", "swrg.lobby.quickstart.map", "swrg.lobby.quickstart.hint"};
            if (quickstartLabels.size() != keys.length) {
                LOGGER.warn("Expected {} source quickstart labels, found {}", keys.length, quickstartLabels.size());
            }
            for (int i = 0; i < Math.min(keys.length, quickstartLabels.size()); i++) {
                var text = Component.translatable(keys[i]);
                if (i == 0) text.withStyle(net.minecraft.ChatFormatting.YELLOW);
                quickstartLabels.get(i).setText(text);
            }
        }
        foundSpawns.sort(Comparator.comparingDouble((Spawn spawn) -> spawn.position().x)
                .thenComparingDouble(spawn -> spawn.position().z));
        spawns = List.copyOf(foundSpawns);
        look = foundLook == null ? new Vec3(0.5, 128.0, 0.5) : foundLook;
        if (pendingMap != null && spawns.isEmpty()) {
            fail(Component.translatable("skywars2.map.error.spawns_missing", pendingMap));
            return;
        }
        if (pendingMap == null) {
            lobbyBounds = pendingBounds;
            lobbyOverlayActive = true;
            cacheLobbyOverlay();
        } else {
            mapBounds = combineBounds(pendingBounds, placementExternalBounds);
        }
        replaceActiveBounds(combineBounds(lobbyBounds, mapBounds));
        if (blockBoundsDirty) {
            flushChangedBlockBounds();
        } else if (!writeBoundsJournal(activeBoxes)) {
            LOGGER.error("Could not compact arena bounds journal after placement; retaining the recovery envelope.");
        }
        pendingBounds = List.of();
        complete(new Result(true, pendingMap == null
                ? Component.translatable("skywars2.map.ready.lobby")
                : Component.translatable("skywars2.map.ready.map", pendingMap)));
    }

    private void forceMarkerChunks(List<Entity> entities) {
        for (Entity entity : entities) {
            if (entity.entityTags().stream().anyMatch(tag -> tag.startsWith(SWRG))) {
                level.setChunkForced(entity.chunkPosition().x(), entity.chunkPosition().z(), true);
            }
        }
    }

    private void clearForcedChunksOutsideLobby() {
        if (level == null) return;
        for (long packed : level.getForceLoadedChunks().toLongArray()) {
            int chunkX = net.minecraft.world.level.ChunkPos.getX(packed);
            int chunkZ = net.minecraft.world.level.ChunkPos.getZ(packed);
            int minX = chunkX << 4, minZ = chunkZ << 4;
            int maxX = minX + 15, maxZ = minZ + 15;
            boolean lobbyChunk = lobbyBounds.stream().anyMatch(box ->
                    minX <= box.maxX() && maxX >= box.minX()
                            && minZ <= box.maxZ() && maxZ >= box.minZ());
            if (!lobbyChunk) level.setChunkForced(chunkX, chunkZ, false);
        }
    }

    private void clearForcedChunks() {
        if (level == null) return;
        for (long packed : level.getForceLoadedChunks().toLongArray()) {
            level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(packed),
                    net.minecraft.world.level.ChunkPos.getZ(packed), false);
        }
    }

    private void diagnoseChestLootTables() {
        for (String category : List.of("default", "midle", "center")) {
            for (String suffix : List.of("_0", "_lb")) {
                String id = "swrg:chests/" + category + suffix;
                if (server.reloadableRegistries().getLootTable(lootKey(id)) == LootTable.EMPTY) {
                    missingLootReported.add(id);
                    LOGGER.error("Required SWRG chest loot table {} is missing or incompatible with this Minecraft runtime.", id);
                }
            }
        }
    }

    private static ResourceKey<LootTable> lootKey(String id) {
        return ResourceKey.create(Registries.LOOT_TABLE, identifier(id));
    }

    private List<Entity> allArenaEntities() {
        if (level == null) return List.of();
        List<Entity> entities = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (!entity.isRemoved()) entities.add(entity);
        }
        return entities;
    }

    private List<Entity> entitiesWithin(List<BoundingBox> bounds) {
        if (level == null || bounds.isEmpty()) return List.of();
        return allArenaEntities().stream().filter(entity -> {
            BlockPos pos = BlockPos.containing(entity.position());
            return bounds.stream().anyMatch(box -> box.isInside(pos));
        }).toList();
    }

    private void removeEntitiesWithin(List<BoundingBox> bounds) {
        for (Entity entity : entitiesWithin(bounds)) {
            if (!(entity instanceof Player)) entity.discard();
        }
    }

    private void removeMapEntitiesWithin(List<BoundingBox> bounds) {
        for (Entity entity : entitiesWithin(bounds)) {
            if (entity instanceof Player) continue;
            if (lobbyOverlayActive && (entity.entityTags().contains("swrg.lobby")
                    || entity.entityTags().contains("swrg.quickstart"))) continue;
            entity.discard();
        }
    }

    private void removeLobbyEntities() {
        for (Entity entity : entitiesWithin(lobbyBounds)) {
            if (!(entity instanceof Player) && (entity.entityTags().contains("swrg.lobby")
                    || entity.entityTags().contains("swrg.quickstart"))) entity.discard();
        }
    }

    private void cacheLobbyOverlay() {
        if (level == null) return;
        List<LobbyCell> cells = new ArrayList<>();
        Vec3i size = Objects.requireNonNull(template("swrg:lobby")).size;
        BlockPos origin = new BlockPos(-5 + LOBBY_OFFSET_X, 199, -5);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = 0; y < size.getY(); y++) for (int x = 0; x < size.getX(); x++) for (int z = 0; z < size.getZ(); z++) {
            cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
            cells.add(new LobbyCell(cursor.immutable(), snapshot(cursor)));
        }
        lobbyCells = List.copyOf(cells);
        lobbyFootprint.clear();
        lobbyVisible.clear();
        for (LobbyCell cell : cells) {
            lobbyFootprint.add(cell.pos);
            lobbyVisible.put(cell.pos, cell.visible);
        }
    }

    private void maintainLobbyOverlay() {
        if (!lobbyOverlayActive || level == null) return;
        for (LobbyCell cell : lobbyCells) {
            if (!sameSnapshot(cell.pos, cell.visible)) {
                // External edits under the lobby become the new revealed map state.
                mapUnderLobby.put(cell.pos, snapshot(cell.pos));
                restoreSnapshot(cell.pos, cell.visible);
            }
        }
    }

    private boolean insideLobby(BlockPos pos) {
        return lobbyFootprint.contains(pos);
    }

    BlockSnapshot snapshot(BlockPos pos) {
        return snapshot(level,pos);
    }
    BlockSnapshot snapshot(ServerLevel source,BlockPos pos) {
        BlockState state = source.getBlockState(pos);
        BlockEntity blockEntity = source.getBlockEntity(pos);
        return new BlockSnapshot(state, blockEntity == null ? null : blockEntity.saveWithFullMetadata(source.registryAccess()));
    }

    private boolean sameSnapshot(BlockPos pos, BlockSnapshot snapshot) {
        BlockState state = level.getBlockState(pos);
        if (!state.equals(snapshot.state)) return false;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (snapshot.entity == null) return blockEntity == null;
        return blockEntity != null && snapshot.entity.equals(blockEntity.saveWithFullMetadata(level.registryAccess()));
    }

    private void restoreLobbyCell(BlockPos pos) {
        BlockSnapshot visible = lobbyVisible.get(pos);
        if (visible != null) restoreSnapshot(pos, visible);
    }

    void restoreSnapshot(BlockPos pos, BlockSnapshot snapshot) {
        restoreSnapshot(level,pos,snapshot);
    }
    /** Копия полностью заменяет прежний контейнер, даже если тип блока остался тем же. */
    void restoreSnapshot(ServerLevel target,BlockPos pos,BlockSnapshot snapshot) {
        restoreSnapshot(target,pos,snapshot,MAP_WRITE_FLAGS);
    }

    /** Bulk spell writes may defer shape/on-place callbacks while retaining native lighting and client updates. */
    void restoreSnapshot(ServerLevel target,BlockPos pos,BlockSnapshot snapshot,int flags) {
        if(snapshot.entity==null && target.getBlockState(pos)==snapshot.state && target.getBlockEntity(pos)==null) return;
        if(target.getBlockEntity(pos) instanceof net.minecraft.world.Clearable previous) previous.clearContent();
        target.setBlock(pos, snapshot.state, flags);
        if (snapshot.entity != null) {
            BlockEntity blockEntity = target.getBlockEntity(pos);
            if (blockEntity != null) {
                blockEntity.loadWithComponents(TagValueInput.create(
                        ProblemReporter.DISCARDING, target.registryAccess(), snapshot.entity));
                blockEntity.setChanged();
            }
        }
    }

    private void removeArenaEntities() {
        if (level == null) return;
        for (Entity entity : allArenaEntities()) {
            if (!(entity instanceof Player)) entity.discard();
        }
        spawns = List.of();
        look = Vec3.ZERO;
    }

    private void replaceActiveBounds(List<BoundingBox> bounds) {
        activeBoxes.clear();
        activeBoxes.addAll(bounds);
    }

    private void restoreCages() {
        if (level == null) return;
        for (BlockPos pos : cageBlocks) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        // Clear any original template glass too, including pieces outside the temporary barrier shape.
        for (Spawn spawn : spawns) {
            BlockPos feet = BlockPos.containing(spawn.position());
            for (int y = -1; y <= 4; y++) {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockPos pos = feet.offset(x, y, z);
                        if (level.getBlockState(pos).is(Blocks.GLASS)) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }
        cageBlocks.clear();
    }

    private Component validateTemplates(List<Placement> placements) {
        for (Placement placement : placements) {
            if (template(placement.template) == null) {
                return Component.translatable("skywars2.map.error.template_missing", placement.template);
            }
        }
        return null;
    }

    private Component validateHeight(List<Placement> placements) {
        for (Placement placement : placements) {
            PreparedTemplate template = template(placement.template);
            if (template == null) continue;
            int minY = placement.origin.getY();
            int maxY = minY + template.size.getY() - 1;
            if (minY < level.getMinY() || maxY >= level.getMaxY()) {
                return Component.translatable("skywars2.map.error.height", placement.template,
                        minY, maxY, level.getMinY(), level.getMaxY() - 1);
            }
        }
        return null;
    }

    private PreparedTemplate template(String id) {
        return preparedTemplates.get(identifier(id));
    }

    private void fail(Component message) {
        complete(new Result(false, message));
    }

    private void complete(Result result) {
        Consumer<Result> callback = completion;
        completion = null;
        busy = false;
        reading = false;
        work.clear();
        preparedTemplates = Map.of();
        preparation = null;
        afterPreparation = null;
        LOGGER.info("Map operation {} success={} prepareMs={} workTicks={} slices={} maxWorkTickMs={}",
                pendingMap == null ? "lobby" : pendingMap, result.success(), preparationMillis,
                workTicks, completedSlices, maxWorkTickNanos / 1_000_000.0);
        pendingMap = null;
        releaseChunkTickets();
        if (callback != null) callback.accept(result);
    }

    private List<MapInfo> readCatalog() {
        List<MapInfo> entries = new ArrayList<>();
        try (InputStream input = getClass().getResourceAsStream(MAP_RESOURCE_ROOT + "catalog.json")) {
            if (input == null) throw new IOException("Missing " + MAP_RESOURCE_ROOT + "catalog.json");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement element : root.getAsJsonArray("maps")) {
                JsonObject object = element.getAsJsonObject();
                String id = object.get("id").getAsString();
                Manifest manifest = parseManifest(object);
                manifests.put(id, manifest);
                entries.add(manifest.info);
            }
        } catch (Exception error) {
            System.err.println("[skywars2] Could not read map catalog: " + error.getMessage());
        }
        return List.copyOf(entries);
    }

    private static Manifest parseManifest(JsonObject object) {
        List<Placement> placements = parsePlacements(object.getAsJsonArray("placements"));
        List<Placement> broken = object.has("brokenPlacements")
                ? parsePlacements(object.getAsJsonArray("brokenPlacements")) : List.of();
        MapInfo info = new MapInfo(object.get("id").getAsString(), object.get("name").getAsString(),
                object.get("playerCapacity").getAsInt(), object.get("available").getAsBoolean(),
                object.get("reason").getAsString());
        return new Manifest(info, placements, broken,
                object.has("brokenAvailable") && object.get("brokenAvailable").getAsBoolean());
    }

    private static List<Placement> parsePlacements(JsonArray array) {
        List<Placement> placements = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject object = element.getAsJsonObject();
            JsonArray originArray = object.getAsJsonArray("origin");
            placements.add(new Placement(object.get("template").getAsString(),
                    new BlockPos(originArray.get(0).getAsInt(), originArray.get(1).getAsInt(),
                            originArray.get(2).getAsInt()),
                    object.get("integrity").getAsFloat(), object.get("seed").getAsLong()));
        }
        return List.copyOf(placements);
    }

    private static Identifier identifier(String value) {
        int split = value.indexOf(':');
        return split < 0 ? Identifier.fromNamespaceAndPath("minecraft", value)
                : Identifier.fromNamespaceAndPath(value.substring(0, split), value.substring(split + 1));
    }

    private static final StructureProcessor INTEGRITY_FILTER = new StructureProcessor() {
        @Override
        public StructureTemplate.StructureBlockInfo processBlock(
                net.minecraft.world.level.LevelReader level, BlockPos origin, BlockPos pivot,
                BlockPos templatePos, StructureTemplate.StructureBlockInfo block,
                StructurePlaceSettings settings) {
            return settings.getRandom(templatePos).nextFloat() <= 0.75f ? block : null;
        }

        @Override
        public MapCodec<? extends StructureProcessor> codec() {
            return null; // This per-load processor is never serialized into a structure or data pack.
        }
    };

    private record Work(BoundingBox bounds, Consumer<ServerLevel> action) { }

    private record LobbyCell(BlockPos pos, BlockSnapshot visible) { }

    record BlockSnapshot(BlockState state, CompoundTag entity) {
        static BlockSnapshot air() { return new BlockSnapshot(Blocks.AIR.defaultBlockState(), null); }
    }

    record TemplateSlice(StructureTemplate template, BoundingBox bounds) { }

    private record PreparedTemplate(Vec3i size, List<TemplateSlice> slices) { }

    private record Placement(String template, BlockPos origin, float integrity, long seed) { }

    private record Manifest(MapInfo info, List<Placement> placements,
                            List<Placement> brokenPlacements, boolean brokenAvailable) { }

    public record MapInfo(String id, String name, int playerCapacity, boolean available, String reason) {
        public Component displayName() {
            Component guiName = OriginalGuiDesign.sourceMapName(id);
            return guiName != null ? guiName : Component.translatable("skywars2.map." + id + ".name");
        }

        public Component unavailableReason() {
            return Component.translatable("skywars2.map." + id + ".unavailable");
        }
    }

    public record Spawn(Vec3 position, float yaw, float pitch) { }

    public record Result(boolean success, Component message) { }
}
