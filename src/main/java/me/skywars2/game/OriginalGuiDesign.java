package me.skywars2.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.storage.loot.LootContextParamSets;
import net.minecraft.world.level.storage.loot.LootContextParams;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Builds the original GUI button stack from its datapack loot table, retaining raw item components. */
final class OriginalGuiDesign {
    private static final String DESIGN_RESOURCE = "/skywars2/gui_design.json";
    private static JsonObject design;
    private OriginalGuiDesign() {}

    static ItemStack button(ServerPlayer player, String namespace, int page, int slot) {
        Identifier id = Identifier.fromNamespaceAndPath(namespace, "gui/page/" + page + "/" + slot);
        return button(player, id);
    }

    static ItemStack button(ServerPlayer player, String id) {
        return button(player, Identifier.parse(id));
    }

    /** Builds the exact static map-card item/name recorded from the source function. */
    static SourceMapButton sourceMapButton(ServerPlayer player, String mapId, boolean selected) {
        String pageId;
        int sourceIndex;
        if (mapId.endsWith("b")) {
            pageId = "2000b";
            sourceIndex = Integer.parseInt(mapId.substring(0, mapId.length() - 1)) - 1;
        } else {
            int id = Integer.parseInt(mapId);
            if (id <= 25) { pageId = "2000"; sourceIndex = id - 1; }
            else if (id <= 50) { pageId = "2100"; sourceIndex = id - 26; }
            else { pageId = "2200"; sourceIndex = id - 51; }
        }
        JsonArray pages = design().getAsJsonArray("pages");
        JsonObject page = null;
        for (JsonElement element : pages) {
            JsonObject candidate = element.getAsJsonObject();
            if (candidate.get("pageId").getAsString().equals(pageId)
                    && candidate.get("namespace").getAsString().equals("swrg")) {
                page = candidate;
                break;
            }
        }
        if (page == null) return null;
        int cardIndex = 0;
        for (JsonElement slotElement : page.getAsJsonArray("slots")) {
            JsonObject slot = slotElement.getAsJsonObject();
            JsonArray sources = slot.getAsJsonArray("sources");
            boolean hasSourceItem = false;
            for (JsonElement sourceElement : sources) {
                if (sourceElement.getAsJsonObject().has("sourceItem")) {
                    hasSourceItem = true;
                    break;
                }
            }
            if (!hasSourceItem) continue;
            if (cardIndex++ != sourceIndex) continue;
            for (JsonElement sourceElement : sources) {
                JsonObject source = sourceElement.getAsJsonObject();
                if (!source.has("sourceItem")) continue;
                Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(source.get("sourceItem").getAsString()));
                if (item == null) return null;
                ItemStack stack = new ItemStack(item);
                JsonObject rawName = source.getAsJsonObject("sourceName");
                Component name = Component.translatable(source.get("translationKey").getAsString());
                String color = rawName.has("color") ? rawName.get("color").getAsString() : "white";
                ChatFormatting formatting = ChatFormatting.getByName(color);
                if (formatting != null) name = name.withStyle(formatting);
                else {
                    TextColor parsed = TextColor.parseColor(color).result().orElse(null);
                    if (parsed != null) name = name.withStyle(style -> style.withColor(parsed));
                }
                name = name.withStyle(style -> style.withItalic(false));
                stack.set(DataComponents.CUSTOM_NAME, name);
                if (selected) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                return new SourceMapButton(stack, selected);
            }
        }
        return null;
    }

    private static JsonObject design() {
        if (design != null) return design;
        try (InputStream input = OriginalGuiDesign.class.getResourceAsStream(DESIGN_RESOURCE)) {
            if (input == null) return new JsonObject();
            design = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            return design;
        } catch (Exception ignored) {
            return new JsonObject();
        }
    }

    record SourceMapButton(ItemStack stack, boolean selected) {}

    private static ItemStack button(ServerPlayer player, Identifier id) {
        MinecraftServer server = player.getServer();
        if (server == null) return ItemStack.EMPTY;
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, id);
        LootTable table = server.reloadableRegistries().getLootTable(key);
        if (table == LootTable.EMPTY) return ItemStack.EMPTY;

        LootParams params = new LootParams.Builder(player.serverLevel())
                .withParameter(LootContextParams.ORIGIN, player.position())
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .create(LootContextParamSets.COMMAND);
        for (ItemStack stack : table.getRandomItems(params)) {
            if (!stack.isEmpty()) {
                ItemStack result = stack.copy();
                applySourceText(result, id);
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void applySourceText(ItemStack stack, Identifier tableId) {
        String resource = "/data/" + tableId.getNamespace() + "/loot_table/" + tableId.getPath() + ".json";
        try (InputStream input = OriginalGuiDesign.class.getResourceAsStream(resource)) {
            if (input == null) return;
            JsonElement table = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            ComponentBinding binding = findBindings(table);
            if (binding.nameKey != null) stack.set(DataComponents.CUSTOM_NAME, sourceText(binding.nameKey));
            if (!binding.loreKeys.isEmpty()) {
                List<Component> lore = binding.loreKeys.stream().map(OriginalGuiDesign::sourceText).toList();
                stack.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(lore));
            }
        } catch (Exception ignored) {
            // A missing text binding leaves the native loot stack intact; it is reported in the import manifest.
        }
    }

    private static ComponentBinding findBindings(JsonElement element) {
        BindingBuilder builder = new BindingBuilder();
        collectBindings(element, builder);
        return new ComponentBinding(builder.nameKey, List.copyOf(builder.loreKeys));
    }

    private static void collectBindings(JsonElement element, BindingBuilder builder) {
        if (element == null || !element.isJsonObject()) return;
        JsonObject object = element.getAsJsonObject();
        if (object.has("type") && object.get("type").getAsString().endsWith(":set_name")
                && object.has("name")) {
            String path = storagePath(object.get("name"));
            if (path != null) builder.nameKey = translationKey(path);
        }
        if (object.has("type") && object.get("type").getAsString().endsWith(":set_lore")
                && object.has("lore") && object.get("lore").isJsonArray()) {
            for (JsonElement line : object.getAsJsonArray("lore")) {
                String path = storagePath(line);
                if (path != null) builder.loreKeys.add(translationKey(path));
            }
        }
        for (var entry : object.entrySet()) {
            if (entry.getValue().isJsonObject()) collectBindings(entry.getValue(), builder);
            else if (entry.getValue().isJsonArray()) for (JsonElement child : entry.getValue().getAsJsonArray())
                collectBindings(child, builder);
        }
    }

    private static String storagePath(JsonElement element) {
        if (!element.isJsonObject()) return null;
        JsonObject object = element.getAsJsonObject();
        if (!object.has("type") || !object.get("type").getAsString().equals("nbt")
                || !object.has("storage") || !object.get("storage").getAsString().equals("swrg:lang")
                || !object.has("nbt")) return null;
        return object.get("nbt").getAsString();
    }

    private static String translationKey(String storagePath) {
        String path = storagePath.replaceAll("\\.Lore\\[(\\d+)\\]$", ".$1");
        if (path.endsWith(".Name")) path = path.substring(0, path.length() - 5);
        return "swrg." + path;
    }

    private static Component sourceText(String key) {
        return Component.translatable(key).withStyle(style -> style.withItalic(false));
    }

    private record ComponentBinding(String nameKey, List<String> loreKeys) {}
    private static final class BindingBuilder {
        private String nameKey;
        private final List<String> loreKeys = new ArrayList<>();
    }
}
