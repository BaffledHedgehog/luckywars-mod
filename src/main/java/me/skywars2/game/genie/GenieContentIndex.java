package me.skywars2.game.genie;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import me.skywars2.SkyWars2;
import me.skywars2.game.LbcLoadouts;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.level.storage.loot.LootTable;

/** Describes the loaded loot graph and item presentation without generating any drops. */
final class GenieContentIndex {
    private static HolderLookup.Provider cachedLookup;
    private static String cachedFields;
    private final Set<String> translationKeys = new TreeSet<>();

    static synchronized String fields(HolderLookup.Provider lookup) {
        if (lookup != cachedLookup || cachedFields == null) {
            cachedFields = new GenieContentIndex().build(lookup);
            cachedLookup = lookup;
        }
        return cachedFields;
    }

    private String build(HolderLookup.Provider lookup) {
        var root = new JsonObject();
        var names = new JsonObject();
        for (var id : BuiltInRegistries.ITEM.keySet().stream().sorted().toList()) {
            var item = BuiltInRegistries.ITEM.getValue(id);
            var name = ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, item.getDefaultInstance().getHoverName());
            name.result().ifPresent(component -> names.add(id.toString(), name(component)));
        }
        root.add("item_names", names);
        var tables = new JsonObject();
        var ops = lookup.createSerializationContext(JsonOps.INSTANCE);
        var registry = lookup.lookupOrThrow(Registries.LOOT_TABLE);
        for (var key : registry.listElementIds().sorted(java.util.Comparator.comparing(k -> k.identifier().toString())).toList()) {
            var loot = registry.getOrThrow(key).value();
            var entry = new JsonObject();
            LootTable.DIRECT_CODEC.encodeStart(ops, loot).result()
                    .ifPresentOrElse(value -> describe(entry, value), () -> entry.addProperty("contents_unavailable", true));
            tables.add(key.identifier().toString(), entry);
        }
        root.add("loot_tables", tables);
        root.add("loadouts", loadouts());
        root.add("item_name_translations", translations());
        String encoded = root.toString();
        return "," + encoded.substring(1, encoded.length() - 1);
    }

    private JsonObject loadouts() {
        var catalog = new JsonObject();
        for (var kind : LbcLoadouts.Kind.values()) {
            var rows = new JsonArray();
            for (var entry : LbcLoadouts.catalog(kind)) {
                var row = new JsonObject();
                row.addProperty("id", entry.id());
                if (entry.menuTable() != null) row.addProperty("menu_table", entry.menuTable());
                if (entry.grantTable() != null) row.addProperty("grant_table", entry.grantTable());
                row.add("name_keys", references(entry.names()));
                row.add("lore_keys", references(entry.lore()));
                rows.add(row);
            }
            catalog.add(kind.name().toLowerCase(Locale.ROOT), rows);
        }
        return catalog;
    }

    private JsonArray references(Iterable<String> keys) {
        var result = new JsonArray();
        for (String key : keys) {
            result.add(key);
            translationKeys.add(key);
        }
        return result;
    }

    private void describe(JsonObject result, JsonElement table) {
        var items = new LinkedHashSet<JsonElement>();
        var tables = new LinkedHashSet<JsonElement>();
        collect(table, null, items, tables);
        if (!items.isEmpty()) result.add("items", array(items));
        if (!tables.isEmpty()) result.add("tables", array(tables));
    }

    private void collect(JsonElement tree, JsonElement inheritedName, Set<JsonElement> items, Set<JsonElement> tables) {
        if (!tree.isJsonObject()) return;
        var object = tree.getAsJsonObject();
        JsonElement effectiveName = inheritedName != null ? inheritedName : modifiedName(object.get("modifier"));
        String type = string(object.get("type"));
        switch (type) {
            case "minecraft:item" -> {
                var id = object.get("name");
                if (id instanceof JsonPrimitive) items.add(withName(id, effectiveName));
                else if (id instanceof JsonObject stack && stack.has("id")) {
                    items.add(withName(stack.get("id"), effectiveName != null ? effectiveName : componentName(stack.get("components"))));
                }
            }
            case "minecraft:loot_table" -> {
                var value = object.get("value");
                if (value instanceof JsonPrimitive) tables.add(withName(value, effectiveName));
                else if (value != null) collect(value, effectiveName, items, tables);
            }
            case "minecraft:tag" -> items.add(withName(new JsonPrimitive("#" + string(object.get("name"))), effectiveName));
            case "minecraft:dynamic", "skywars2:tmt", "skywars2:uniform_item" -> {
                var dynamic = new JsonObject();
                dynamic.addProperty("type", type);
                for (String field : new String[]{"name", "domain", "types"}) {
                    if (object.has(field)) dynamic.add(field, object.get(field));
                }
                items.add(dynamic);
            }
            default -> { }
        }
        for (String field : new String[]{"pools", "entries", "children"}) {
            if (object.get(field) instanceof JsonArray children) {
                for (var child : children) collect(child, effectiveName, items, tables);
            }
        }
    }

    private static JsonElement modifiedName(JsonElement modifier) {
        if (modifier instanceof JsonArray list) {
            for (int i = list.size() - 1; i >= 0; i--) {
                var name = modifiedName(list.get(i));
                if (name != null) return name;
            }
        } else if (modifier instanceof JsonObject object) {
            return switch (string(object.get("type"))) {
                case "minecraft:set_name" -> object.get("name");
                case "minecraft:set_components" -> componentName(object.get("components"));
                case "minecraft:sequence" -> modifiedName(object.get("modifiers"));
                default -> null;
            };
        }
        return null;
    }

    private static JsonElement componentName(JsonElement components) {
        if (!(components instanceof JsonObject object)) return null;
        return object.has("minecraft:custom_name") ? object.get("minecraft:custom_name") : object.get("minecraft:item_name");
    }

    private JsonElement withName(JsonElement id, JsonElement name) {
        if (name == null) return id;
        var pair = new JsonArray();
        pair.add(id);
        pair.add(name(name));
        return pair;
    }

    private JsonElement name(JsonElement component) {
        if (component instanceof JsonArray list) {
            var text = new JsonArray();
            for (var part : list) text.add(name(part));
            return text;
        }
        if (!(component instanceof JsonObject object)) return component;
        var text = new JsonObject();
        for (String field : new String[]{"text", "translate", "fallback", "with", "extra", "keybind"}) {
            if (object.has(field)) text.add(field, name(object.get(field)));
        }
        if (text.has("translate")) translationKeys.add(text.get("translate").getAsString());
        return text;
    }

    private JsonObject translations() {
        var languages = new JsonObject();
        var english = new JsonObject();
        var vanilla = Language.getInstance();
        for (String key : translationKeys) if (vanilla.has(key)) english.addProperty(key, vanilla.getOrDefault(key));
        languages.add("en_us", english);
        Path folder = FabricLoader.getInstance().getConfigDir().resolve("skywars2/genie/lang");
        if (!Files.isDirectory(folder)) return languages;
        try (var files = Files.list(folder)) {
            for (Path path : files.sorted().toList()) {
                String file = path.getFileName().toString().toLowerCase(Locale.ROOT);
                if (!file.matches("[a-z0-9_-]+\\.json") || !Files.isRegularFile(path)) continue;
                try {
                    String locale = file.substring(0, file.length() - 5);
                    var source = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                    var translated = locale.equals("en_us") ? english : new JsonObject();
                    for (String key : translationKeys) {
                        if (source.get(key) instanceof JsonPrimitive text && text.isString()) translated.add(key, text);
                    }
                    if (!translated.isEmpty()) languages.add(locale, translated);
                } catch (IOException | RuntimeException error) {
                    SkyWars2.LOGGER.warn("Genie could not load an item-name language file ({})", file);
                }
            }
        } catch (IOException error) {
            SkyWars2.LOGGER.warn("Genie could not list the item-name language directory");
        }
        return languages;
    }

    private static JsonArray array(Set<JsonElement> values) {
        var result = new JsonArray();
        values.forEach(result::add);
        return result;
    }

    private static String string(JsonElement value) {
        return value instanceof JsonPrimitive primitive && primitive.isString() ? primitive.getAsString() : "";
    }
}
