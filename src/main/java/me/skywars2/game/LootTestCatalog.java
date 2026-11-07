package me.skywars2.game;

import com.google.gson.JsonParser;
import com.mojang.brigadier.suggestion.Suggestions;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;

/** Public test entries are separate from the complete registry needed by internal loot references. */
public final class LootTestCatalog {
    private static final Set<Identifier> TABLES;
    private static final Set<String> MANAGED_NAMESPACES;
    private static final List<String> PREFIXES;

    static {
        try (var stream = LootTestCatalog.class.getResourceAsStream("/skywars2/loot_test_catalog.json")) {
            if (stream == null) throw new IllegalStateException("Missing loot test catalog");
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Set<Identifier> tables = new HashSet<>();
            json.getAsJsonArray("tables").forEach(value -> tables.add(Identifier.parse(value.getAsString())));
            TABLES = Set.copyOf(tables);
            Set<String> namespaces = new HashSet<>();
            json.getAsJsonArray("managed_namespaces").forEach(value -> namespaces.add(value.getAsString()));
            MANAGED_NAMESPACES = Set.copyOf(namespaces);
            PREFIXES = java.util.stream.StreamSupport.stream(json.getAsJsonArray("id_prefixes").spliterator(), false)
                    .map(value -> value.getAsString()).toList();
        } catch (Exception failure) {
            throw new IllegalStateException("Invalid loot test catalog", failure);
        }
    }

    private LootTestCatalog() { }

    /** Bare generated-item transports are not ready gameplay prototypes. */
    public static boolean publicNativeItem(net.minecraft.world.item.Item item) {
        return !LbcTmtItems.output(item);
    }
    public static boolean publicNativeStack(net.minecraft.world.item.ItemStack stack){
        return publicNativeItem(stack.getItem())||LbcTmtItems.output(stack.getItem())&&!LbcTmtAbilities.read(stack).isEmpty();
    }

    public static boolean isSuggested(Identifier id) {
        return !MANAGED_NAMESPACES.contains(id.getNamespace()) || TABLES.contains(id)
                || PREFIXES.stream().anyMatch(prefix -> id.toString().startsWith(prefix));
    }

    public static Suggestions filter(Suggestions suggestions) {
        // Preserve Brigadier's replacement ranges and tooltips; never remove registry entries or parse access.
        var visible = suggestions.getList().stream().filter(suggestion -> {
            var id = Identifier.tryParse(suggestion.getText());
            return id == null || isSuggested(id);
        }).toList();
        return visible.size() == suggestions.getList().size() ? suggestions
                : new Suggestions(suggestions.getRange(), visible);
    }
}
