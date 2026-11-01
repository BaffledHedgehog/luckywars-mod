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
    private boolean refresh;
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
                refresh = true;
            } catch (Exception error) { SkyWars2.LOGGER.error("Cannot restore content gallery metadata", error); }
        }
    }
    ContentGallery(GameController game, Layout fixture) { this(game, false); this.layout = fixture; }

    static boolean allowed(ServerPlayer player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    public void spawn(ServerPlayer player) {
        if (!allowed(player)) { player.sendSystemMessage(Component.literal("Gallery requires operator permission.")); return; }
        requester = player.getUUID();
        if (layout == null) {
            var at = player.blockPosition().below();
            if (at.getY() < player.level().getMinY() || at.getY() + 4 >= player.level().getMaxY()) {
                player.sendSystemMessage(Component.literal("Not enough vertical room for the gallery.")); return;
            }
            layout = new Layout(player.level().dimension().identifier().toString(), at, UUID.randomUUID(), UUID.randomUUID());
        }
        refresh = true;
        player.sendSystemMessage(Component.literal("Gallery refresh queued. Existing gallery location is reused; construction waits for loaded chunks."));
    }

    private ServerLevel level() {
        return layout == null ? null : game.server().getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(layout.dimension)));
    }

    public void tick() {
        if (layout == null) return;
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
        // Bounded late cleanup also catches obsolete frames whose chunks were unloaded during refresh.
        if (++clock % 100 == 0) {
            int removed = 0;
            for (var entity : level.getAllEntities()) {
                if (obsolete(entity)) { entity.discard(); if (++removed >= 128) break; }
            }
        }
        int attempts = Math.min(placements.size(), 256), completed = 0;
        while (attempts-- > 0 && completed < 128) {
            var placement = placements.removeFirst();
            if (!level.hasChunkAt(placement.position)) { placements.addLast(placement); continue; }
            place(level, placement);
            completed++;
        }
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
        if (placement.kind == 0) level.setBlock(placement.position, Blocks.STONE_BRICKS.defaultBlockState(), 2);
        else if (placement.kind == 1) level.setBlock(placement.position, Blocks.ENDER_CHEST.defaultBlockState(), 3);
        else {
            level.setBlock(placement.position, Blocks.STONE_BRICKS.defaultBlockState(), 2);
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
                && entity.entityTags().contains(layout.ownerTag()) && entity.level() == level();
    }
    /** Consume player attacks on gallery frames, including vanilla creative invulnerability bypass. */
    public boolean protectEntity(Entity entity) { return owned(entity); }
    private boolean obsolete(Entity entity) {
        return owned(entity) && !entity.entityTags().contains(layout.generationTag());
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
        return layout != null && level == level() && position.equals(layout.chest());
    }

    private void save() {
        if (!persistent || layout == null) return;
        try {
            Files.createDirectories(journal.getParent());
            var data = new JsonObject();
            data.addProperty("dimension", layout.dimension);
            data.addProperty("x", layout.center.getX()); data.addProperty("y", layout.center.getY()); data.addProperty("z", layout.center.getZ());
            data.addProperty("owner", layout.owner.toString());
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
