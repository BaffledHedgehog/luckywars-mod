package me.skywars2.game;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.phys.AABB;

/** One explicitly requested, persistent operator showroom. All work runs on the server thread. */
public final class ContentGallery {
    static final String TAG = "skywars2.gallery";
    private static final int COLUMNS = 32, WIDTH = COLUMNS * 2 + 5;
    private final GameController game;
    private final Path journal;
    private final boolean persistent;
    private Layout layout;
    private HolderLookup.Provider registry;
    private Catalog catalog;
    private List<ItemStack> contents = List.of();
    private final ArrayDeque<RunnablePlacement> placements = new ArrayDeque<>();
    private final LinkedHashMap<CellKey, Cell> baseline = new LinkedHashMap<>();
    private final LinkedHashMap<CellKey, Cell> restoring = new LinkedHashMap<>();
    private final List<LegacyFootprint> legacy = new ArrayList<>();
    private boolean legacyLayout;
    record CellKey(String dimension, BlockPos pos) { }
    record Cell(MapManager.BlockSnapshot before, net.minecraft.world.level.block.state.BlockState built) { }
    static final class LegacyFootprint {
        final Layout layout;
        int rows;
        LegacyFootprint(Layout layout, int rows) { this.layout=layout; this.rows=rows; }
    }
    private boolean refresh;
    private boolean enabled = true;
    private int clock;
    private int catalogErrors;
    private UUID requester;

    record Layout(String dimension, BlockPos center, UUID owner, UUID generation) {
        BlockPos chest() { return center; }
        String ownerTag() { return TAG + "." + owner; }
        String generationTag() { return TAG + ".generation." + generation; }
    }
    record RunnablePlacement(BlockPos position, int kind, int item) { }

    public ContentGallery(GameController game) { this(game, true); }
    ContentGallery(GameController game, boolean persistent) {
        this.game = game;
        this.persistent = persistent;
        this.journal = game.server().getWorldPath(LevelResource.ROOT).resolve("data/luckywars/content-gallery.json");
        if (persistent && Files.exists(journal)) {
            try {
                var data = JsonParser.parseString(Files.readString(journal)).getAsJsonObject();
                layout = new Layout(data.get("dimension").getAsString(),
                        new BlockPos(data.get("x").getAsInt(), data.get("y").getAsInt(), data.get("z").getAsInt()),
                        UUID.fromString(data.get("owner").getAsString()), UUID.randomUUID());
                enabled = !data.has("enabled") || data.get("enabled").getAsBoolean();
                refresh = enabled;
                legacyLayout = !data.has("cells");
                readCells(data);
            } catch (Exception error) { SkyWars2.LOGGER.error("Cannot restore content gallery metadata", error); }
        }
    }
    ContentGallery(GameController game, Layout fixture) { this(game, false); this.layout = fixture; }

    static boolean allowed(ServerPlayer player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    public void spawn(ServerPlayer player) {
        if (!allowed(player)) { player.sendSystemMessage(Component.literal("Gallery requires operator permission.")); return; }
        var at = player.blockPosition().below();
        if (at.getY() < player.level().getMinY() || at.getY() + 4 >= player.level().getMaxY()) {
            player.sendSystemMessage(Component.literal("Not enough vertical room for the gallery.")); return;
        }
        retireTerrain();
        restoreLoaded(Integer.MAX_VALUE);
        requester = player.getUUID();
        var owner = layout == null ? UUID.randomUUID() : layout.owner;
        // Invalidate every old generation immediately, including pending placements.
        layout = new Layout(player.level().dimension().identifier().toString(), at, owner, UUID.randomUUID());
        enabled = true;
        legacyLayout = false;
        placements.clear(); catalog = null;
        removeObsoleteFrames();
        refresh = true;
        save();
        player.sendSystemMessage(Component.literal("Gallery queued at your current location; old display frames removed."));
    }

    /** Match admission must clear presentation work before any new map is populated. */
    public void clearForMatchStart() {
        enabled = false;
        placements.clear(); catalog = null; requester = null; refresh = false;
        retireTerrain();
        removeObsoleteFrames();
        restoreLoaded(Integer.MAX_VALUE);
        save();
    }

    private void removeObsoleteFrames() {
        if (layout == null) return;
        for (var world : game.server().getAllLevels())
            for (var entity : world.getAllEntities()) if (obsolete(entity)) {
                for (var old : legacy) if (old.layout.dimension.equals(world.dimension().identifier().toString())) {
                    var at = entity.blockPosition();
                    int offset = at.getZ()-old.layout.center.getZ()-5;
                    if (at.getY()==old.layout.center.getY()+2 && offset>=0 && offset%3==0
                            && Math.abs(at.getX()-old.layout.center.getX())<COLUMNS)
                    {
                        expandLegacy(old,offset/3+1);
                        legacyCell(old.layout,at.below(),Blocks.STONE_BRICKS);
                    }
                }
                entity.discard();
            }
    }

    private ServerLevel level() {
        return layout == null ? null : game.server().getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(layout.dimension)));
    }

    public void tick() {
        if (layout == null) return;
        if (legacyLayout) { retireTerrain(); restoreLoaded(Integer.MAX_VALUE); }
        // Retain ownership after a match start, so late-loaded old frames are also removed.
        if (++clock % 20 == 0) removeObsoleteFrames();
        restoreLoaded(256);
        if (!enabled) return;
        var level = level();
        if (level == null) return;
        var current = game.server().reloadableRegistries().lookup();
        if (refresh || registry != current) {
            registry = current;
            refresh = false;
            layout = new Layout(layout.dimension, layout.center, layout.owner, UUID.randomUUID());
            placements.clear();
            catalog = new Catalog(game, level, layout.center);
            save();
        }
        if (catalog != null) {
            if (!catalog.step(24)) return;
            catalogErrors = catalog.errors;
            contents = catalog.finish();
            catalog = null;
            plan();
        }
        int attempts = Math.min(placements.size(), 256), completed = 0;
        var ready = new ArrayList<RunnablePlacement>();
        while (attempts-- > 0 && completed < 128) {
            var placement = placements.removeFirst();
            if (!level.hasChunkAt(placement.position)) { placements.addLast(placement); continue; }
            var key = new CellKey(layout.dimension,placement.position);
            if (restoring.containsKey(key)) { placements.addLast(placement); continue; }
            remember(level,placement.position,placement.kind==1?Blocks.ENDER_CHEST:Blocks.STONE_BRICKS);
            ready.add(placement);
            completed++;
        }
        // Write ahead once per bounded construction batch, preserving the first overwritten snapshot.
        if (!ready.isEmpty()) { save(); for (var placement : ready) place(level,placement); }
        if (placements.isEmpty() && requester != null) {
            var player = game.server().getPlayerList().getPlayer(requester);
            if (player != null) player.sendSystemMessage(Component.literal("Gallery ready: " + contents.size() + " distinct mod item identities. Right-click a frame to copy one; central ender chest opens perks."
                    + (catalogErrors == 0 ? "" : " WARNING: " + catalogErrors + " definitions failed; see server log.")));
            requester = null;
        }
    }

    private void plan() {
        int rows = Math.max(1, (contents.size() + COLUMNS - 1) / COLUMNS);
        for (int z = -3; z <= rows * 3 + 7; z++) for (int x = -WIDTH / 2; x <= WIDTH / 2; x++)
            placements.add(new RunnablePlacement(layout.center.offset(x, 0, z), 0, -1));
        placements.add(new RunnablePlacement(layout.chest(), 1, -1));
        for (int index = 0; index < contents.size(); index++) {
            var pedestal = layout.center.offset((index % COLUMNS) * 2 - COLUMNS + 1, 1, 5 + (index / COLUMNS) * 3);
            placements.add(new RunnablePlacement(pedestal, 2, index));
        }
    }

    private void place(ServerLevel level, RunnablePlacement placement) {
        if (placement.kind == 0) paint(level,placement.position,Blocks.STONE_BRICKS);
        else if (placement.kind == 1) paint(level,placement.position,Blocks.ENDER_CHEST);
        else {
            paint(level,placement.position,Blocks.STONE_BRICKS);
            var at = placement.position.above();
            for (var old : level.getEntitiesOfClass(ItemFrame.class, new AABB(at).inflate(.2), this::owned)) old.discard();
            level.addFreshEntity(frame(level, at, contents.get(placement.item), layout));
        }
    }

    static ItemFrame frame(ServerLevel level, BlockPos at, ItemStack item, Layout layout) {
        var frame = new ItemFrame(level, at, Direction.UP);
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        frame.saveWithoutId(output);
        var data = output.buildResult();
        data.putBoolean("Fixed", true);
        data.putBoolean("Invulnerable", true);
        data.putFloat("ItemDropChance", 0);
        frame.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data));
        frame.addTag(TAG);
        frame.addTag(layout.ownerTag());
        frame.addTag(layout.generationTag());
        frame.setItem(item.copyWithCount(1), false);
        return frame;
    }

    boolean owned(Entity entity) {
        return layout != null && entity instanceof ItemFrame && entity.entityTags().contains(TAG)
                && entity.entityTags().contains(layout.ownerTag());
    }
    /** Consume player attacks on gallery frames, including vanilla creative invulnerability bypass. */
    public boolean protectEntity(Entity entity) { return owned(entity); }
    private boolean obsolete(Entity entity) {
        return owned(entity) && (!enabled || !entity.entityTags().contains(layout.generationTag()));
    }

    public boolean useEntity(ServerPlayer player, Entity entity) {
        if (!owned(entity)) return false;
        if (allowed(player) && !obsolete(entity) && player.level() == entity.level()
                && player.distanceToSqr(entity) <= 64) {
            var copy = ((ItemFrame) entity).getItem().copyWithCount(1);
            // Do not drop rewards when the inventory is full.
            if (!player.getInventory().add(copy)) player.sendSystemMessage(Component.literal("Make room in your inventory."));
            player.inventoryMenu.broadcastChanges();
        }
        return true;
    }

    public boolean useBlock(ServerPlayer player, BlockPos position) {
        var level = level();
        if (!ownsChest(player.level(), position)
                || !level.hasChunkAt(position) || !level.getBlockState(position).is(Blocks.ENDER_CHEST)) return false;
        if (allowed(player)) game.menus().openDebug(player);
        return true;
    }
    boolean ownsChest(ServerLevel level, BlockPos position) {
        return enabled && layout != null && level == level() && position.equals(layout.chest());
    }

    private void paint(ServerLevel level,BlockPos pos,net.minecraft.world.level.block.Block block) {
        // Capture happened before this write. Clear native container contents before replacement
        // so restoring its exact baseline cannot duplicate items dropped by onRemove.
        game.maps().restoreSnapshot(level,pos,new MapManager.BlockSnapshot(block.defaultBlockState(),null),2);
    }
    void writeFixtureCell(ServerLevel level,BlockPos pos,net.minecraft.world.level.block.Block block) {
        remember(level,pos,block);save();paint(level,pos,block);
    }
    void roundTripJournalForChecks() {
        try {
            var palette=new LinkedHashMap<net.minecraft.world.level.block.state.BlockState,Integer>();
            var cells=encodeCells(baseline,palette);var queue=encodeCells(restoring,palette);
            var states=List.copyOf(palette.keySet());baseline.clear();restoring.clear();
            decodeCells(cells,states,baseline);decodeCells(queue,states,restoring);
        } catch(IOException error) { throw new IllegalStateException(error); }
    }

    void remember(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.Block block) {
        var key=new CellKey(level.dimension().identifier().toString(),pos.immutable());
        var prior=baseline.get(key);
        baseline.put(key,new Cell(prior==null?game.maps().snapshot(level,pos):prior.before,block.defaultBlockState()));
    }

    private void retireTerrain() {
        restoring.putAll(baseline); baseline.clear();
        if (legacyLayout && layout!=null) {
            var old=new LegacyFootprint(layout,0); legacy.add(old); expandLegacy(old,1);
            // Infer the known historical extent from actual owned frames, never a world-sized box.
            for (var world:game.server().getAllLevels()) for (var e:world.getAllEntities())
                if (owned(e) && world.dimension().identifier().toString().equals(layout.dimension)) {
                    int row=e.blockPosition().getZ()-layout.center.getZ()-5;
                    if (e.blockPosition().getY()==layout.center.getY()+2 && row>=0 && row%3==0
                            && Math.abs(e.blockPosition().getX()-layout.center.getX())<COLUMNS)
                    {
                        expandLegacy(old,row/3+1);
                        legacyCell(old.layout,e.blockPosition().below(),Blocks.STONE_BRICKS);
                    }
                }
            legacyLayout=false;
        }
        save();
    }

    private void expandLegacy(LegacyFootprint old,int rows) {
        if(rows<=old.rows)return;
        int previous=old.rows;old.rows=rows;
        for(int z=previous==0?-3:previous*3+8;z<=rows*3+7;z++)
            for(int x=-WIDTH/2;x<=WIDTH/2;x++)
                legacyCell(old.layout,old.layout.center.offset(x,0,z),Blocks.STONE_BRICKS);
        legacyCell(old.layout,old.layout.center,Blocks.ENDER_CHEST);
    }
    private void legacyCell(Layout at,BlockPos pos,net.minecraft.world.level.block.Block built) {
        var key=new CellKey(at.dimension,pos);
        var active=baseline.get(key);
        if(active!=null) {
            // A legacy chunk may arrive after the new showroom began rebuilding this cell.
            if(active.before.state().is(built))baseline.put(key,new Cell(MapManager.BlockSnapshot.air(),active.built));
        } else restoring.put(key,new Cell(MapManager.BlockSnapshot.air(),built.defaultBlockState()));
    }

    /** Restore only recorded writes still bearing the gallery material. Never force a chunk load. */
    private void restoreLoaded(int budget) {
        boolean changed=false;
        int attempts=Math.min(budget,restoring.size());
        while(attempts-->0) {
            var iterator=restoring.entrySet().iterator();var entry=iterator.next();iterator.remove();
            var key=entry.getKey();var cell=entry.getValue();
            var world=game.server().getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.parse(key.dimension)));
            if(world==null || !world.hasChunkAt(key.pos)) { restoring.put(key,cell);continue; }
            if(!baseline.containsKey(key) && (world.getBlockState(key.pos)==cell.built
                    || cell.built.is(Blocks.ENDER_CHEST) && world.getBlockState(key.pos).is(Blocks.STONE_BRICKS)))
                game.maps().restoreSnapshot(world,key.pos,cell.before);
            changed=true;
        }
        if(changed)save();
    }

    /** Call from Fabric CHUNK_LOAD before an imported map can replace queued old gallery cells. */
    public void chunkLoaded(ServerLevel world,net.minecraft.world.level.chunk.LevelChunk chunk) {
        String dimension=world.dimension().identifier().toString();
        boolean changed=false;
        var iterator=restoring.entrySet().iterator();
        while(iterator.hasNext()) {
            var entry=iterator.next();var key=entry.getKey();var cell=entry.getValue();
            if(!key.dimension.equals(dimension) || (key.pos.getX()>>4)!=chunk.getPos().x() || (key.pos.getZ()>>4)!=chunk.getPos().z())continue;
            var actual=chunk.getBlockState(key.pos);
            if(!baseline.containsKey(key) && (actual==cell.built || cell.built.is(Blocks.ENDER_CHEST)&&actual.is(Blocks.STONE_BRICKS)))
                game.maps().restoreSnapshot(world,key.pos,cell.before);
            iterator.remove();changed=true;
        }
        if(changed)save();
    }

    private void readCells(JsonObject data) throws IOException {
        if(!data.has("cells"))return;
        var palette=new ArrayList<net.minecraft.world.level.block.state.BlockState>();
        for(var value:data.getAsJsonArray("states"))palette.add(net.minecraft.world.level.block.state.BlockState.CODEC.parse(JsonOps.INSTANCE,value).getOrThrow());
        decodeCells(data.getAsJsonArray("cells"),palette,baseline);
        decodeCells(data.getAsJsonArray("restoring"),palette,restoring);
        if(data.has("legacy"))for(var value:data.getAsJsonArray("legacy")) {
            var row=value.getAsJsonObject();
            var at=new Layout(row.get("dimension").getAsString(),new BlockPos(row.get("x").getAsInt(),row.get("y").getAsInt(),row.get("z").getAsInt()),layout.owner,UUID.randomUUID());
            legacy.add(new LegacyFootprint(at,row.get("rows").getAsInt()));
        }
    }
    private static void decodeCells(JsonArray rows,List<net.minecraft.world.level.block.state.BlockState> palette,Map<CellKey,Cell> target) throws IOException {
        if(rows==null)return;
        for(var value:rows) {
            var row=value.getAsJsonArray();
            net.minecraft.nbt.CompoundTag nbt=null;
            if(row.size()>6)try(var stream=new java.io.ByteArrayInputStream(Base64.getDecoder().decode(row.get(6).getAsString()))) {
                nbt=net.minecraft.nbt.NbtIo.readCompressed(stream,net.minecraft.nbt.NbtAccounter.create(16L*1024*1024));
            }
            target.put(new CellKey(row.get(0).getAsString(),new BlockPos(row.get(1).getAsInt(),row.get(2).getAsInt(),row.get(3).getAsInt())),
                    new Cell(new MapManager.BlockSnapshot(palette.get(row.get(4).getAsInt()),nbt),palette.get(row.get(5).getAsInt())));
        }
    }
    private static JsonArray encodeCells(Map<CellKey,Cell> source,Map<net.minecraft.world.level.block.state.BlockState,Integer> palette) throws IOException {
        var rows=new JsonArray();
        for(var entry:source.entrySet()) {
            var key=entry.getKey();var cell=entry.getValue();var row=new JsonArray();
            row.add(key.dimension);row.add(key.pos.getX());row.add(key.pos.getY());row.add(key.pos.getZ());
            row.add(palette.computeIfAbsent(cell.before.state(),s->palette.size()));row.add(palette.computeIfAbsent(cell.built,s->palette.size()));
            if(cell.before.entity()!=null)try(var stream=new java.io.ByteArrayOutputStream()) {
                net.minecraft.nbt.NbtIo.writeCompressed(cell.before.entity(),stream);row.add(Base64.getEncoder().encodeToString(stream.toByteArray()));
            }
            rows.add(row);
        }
        return rows;
    }

    private void save() {
        if (!persistent || layout == null) return;
        try {
            Files.createDirectories(journal.getParent());
            var data = new JsonObject();
            data.addProperty("dimension", layout.dimension);
            data.addProperty("x", layout.center.getX()); data.addProperty("y", layout.center.getY()); data.addProperty("z", layout.center.getZ());
            data.addProperty("owner", layout.owner.toString());
            data.addProperty("enabled", enabled);
            var palette=new LinkedHashMap<net.minecraft.world.level.block.state.BlockState,Integer>();
            data.add("cells",encodeCells(baseline,palette));data.add("restoring",encodeCells(restoring,palette));
            var states=new JsonArray();for(var state:palette.keySet())states.add(net.minecraft.world.level.block.state.BlockState.CODEC.encodeStart(JsonOps.INSTANCE,state).getOrThrow());
            data.add("states",states);
            var oldLayouts=new JsonArray();for(var old:legacy) {
                var row=new JsonObject();row.addProperty("dimension",old.layout.dimension);row.addProperty("x",old.layout.center.getX());row.addProperty("y",old.layout.center.getY());row.addProperty("z",old.layout.center.getZ());row.addProperty("rows",old.rows);oldLayouts.add(row);
            }
            data.add("legacy",oldLayouts);
            var temporary = journal.resolveSibling(journal.getFileName() + ".tmp");
            Files.writeString(temporary, data.toString());
            Files.move(temporary, journal, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException error) { throw new IllegalStateException("Cannot save content gallery location", error); }
    }

    /** Shutdown preserves the showroom and its ownership; no foreign entities or terrain are removed. */
    public void close() { placements.clear(); catalog = null; save(); }

    /** Native equality includes every component, but ignores quantity. Stored keys never escape mutably. */
    static final class Identity {
        final ItemStack item;
        Identity(ItemStack item) { this.item = item.copyWithCount(1); }
        @Override public int hashCode() { return ItemStack.hashItemAndComponents(item); }
        @Override public boolean equals(Object other) {
            return other instanceof Identity identity && ItemStack.isSameItemSameComponents(item, identity.item);
        }
    }

    /** Enumerates actual public definitions; never samples a weighted parent until it happens to yield a rare item. */
    static final class Catalog {
        private final GameController game;
        private final LootParams params;
        private final com.mojang.serialization.DynamicOps<JsonElement> ops;
        private final ArrayDeque<Node> queue = new ArrayDeque<>();
        private final LinkedHashMap<Identity, ItemStack> items = new LinkedHashMap<>();
        private final Set<String> expanded = new HashSet<>();
        private final Set<Item> represented = Collections.newSetFromMap(new IdentityHashMap<>());
        private int errors;
        record Node(JsonObject json, List<JsonElement> modifiers, Set<String> ancestry) { }

        Catalog(GameController game, ServerLevel level, BlockPos origin) {
            this.game = game;
            params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.atCenterOf(origin)).create(LootContextParamSets.CHEST);
            var lookup = game.server().reloadableRegistries().lookup();
            ops = lookup.createSerializationContext(JsonOps.INSTANCE);
            lookup.lookupOrThrow(Registries.LOOT_TABLE).listElementIds().map(ResourceKey::identifier)
                    .filter(id -> (id.getNamespace().equals("lbc") || id.getNamespace().equals("swrg")) && LootTestCatalog.isSuggested(id))
                    .sorted(Comparator.comparing(Identifier::toString)).forEach(id -> reference(id.toString(), List.of(), Set.of()));
        }

        private void reference(String id, List<JsonElement> modifiers, Set<String> ancestry) {
            if (ancestry.contains(id)) return;
            var marker = id + modifiers;
            if (!expanded.add(marker)) return;
            var node = new JsonObject(); node.addProperty("gallery_reference", id);
            queue.addLast(new Node(node, modifiers, ancestry));
        }
        boolean step(int budget) {
            while (budget-- > 0 && !queue.isEmpty()) {
                var node = queue.removeFirst();
                try { expand(node); }
                catch (RuntimeException error) {
                    errors++;
                    SkyWars2.LOGGER.warn("Gallery skipped unsupported public loot definition (ancestry={}): {}", node.ancestry, error.toString());
                }
            }
            return queue.isEmpty();
        }
        int errorCount() { return errors; }
        private static void append(List<JsonElement> target, JsonElement value) {
            if (value == null || value.isJsonNull()) return;
            if (value.isJsonArray()) { value.getAsJsonArray().forEach(child -> append(target, child)); return; }
            // Quantity is deliberately absent from display identity. Dynamic score-based counts may
            // otherwise erase a perfectly valid canonical item outside its original match context.
            if (value.isJsonObject() && value.getAsJsonObject().has("type")
                    && value.getAsJsonObject().get("type").getAsString().equals("minecraft:set_count")) return;
            target.add(value);
        }
        private void expand(Node node) {
            var json = node.json;
            if (json.has("gallery_reference")) {
                String id = json.get("gallery_reference").getAsString();
                var ancestry = new HashSet<>(node.ancestry); ancestry.add(id);
                var table = game.server().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(id)));
                queue.addLast(new Node(LootTable.DIRECT_CODEC.encodeStart(ops, table).getOrThrow().getAsJsonObject(), node.modifiers, Set.copyOf(ancestry)));
                return;
            }
            // Inner modifiers execute before outer table/pool/reference modifiers.
            var modifiers = new ArrayList<JsonElement>(); append(modifiers, json.get("modifier")); modifiers.addAll(node.modifiers);
            for (String children : List.of("pools", "entries", "children")) if (json.has(children)) {
                for (var child : json.getAsJsonArray(children)) queue.addLast(new Node(child.getAsJsonObject(), modifiers, node.ancestry));
                return;
            }
            String type = json.has("type") ? json.get("type").getAsString() : "";
            if (type.equals("minecraft:loot_table")) {
                var value = json.get("value");
                if (value.isJsonPrimitive()) reference(value.getAsString(), modifiers, node.ancestry);
                else queue.addLast(new Node(value.getAsJsonObject(), modifiers, node.ancestry));
            } else if (type.equals("minecraft:item")) {
                var entry = json.deepCopy(); entry.remove("condition"); entry.remove("conditions"); entry.remove("weight"); entry.remove("quality");
                var functions = new JsonArray(); modifiers.forEach(functions::add);
                entry.remove("modifier");
                if (!functions.isEmpty()) entry.add("modifier", functions);
                var entries = new JsonArray(); entries.add(entry);
                var pool = new JsonObject(); pool.addProperty("rolls", 1); pool.add("entries", entries);
                var pools = new JsonArray(); pools.add(pool);
                var table = new JsonObject(); table.add("pools", pools);
                for (var item : LootTable.DIRECT_CODEC.parse(ops, table).getOrThrow().getRandomItems(params, 268361115L)) {
                    var id = BuiltInRegistries.ITEM.getKey(item.getItem());
                    if (!id.getNamespace().equals("minecraft") || !ItemStack.isSameItemSameComponents(item, new ItemStack(item.getItem()))) add(item);
                }
            }
        }
        private void add(ItemStack item) {
            if(!LootTestCatalog.publicNativeItem(item.getItem()))return;
            if (item.isEmpty()) return;
            represented.add(item.getItem());
            var identity = new Identity(item);
            items.putIfAbsent(identity, identity.item);
        }
        List<ItemStack> finish() {
            if (!queue.isEmpty()) throw new IllegalStateException("Gallery catalog still building");
            for (var item : BuiltInRegistries.ITEM) {
                var id = BuiltInRegistries.ITEM.getKey(item);
                if (id.getNamespace().equals("skywars2") && !represented.contains(item)) add(new ItemStack(item));
            }
            if (errors != 0) SkyWars2.LOGGER.warn("Gallery catalog completed with {} unsupported definitions", errors);
            return items.values().stream().map(ItemStack::copy).toList();
        }
    }
}
