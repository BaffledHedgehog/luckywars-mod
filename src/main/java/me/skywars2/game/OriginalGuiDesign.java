package me.skywars2.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.LootParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Builds original GUI button stacks from native loot tables and prepared source component templates. */
final class OriginalGuiDesign {
    private static final Logger LOGGER = LoggerFactory.getLogger(OriginalGuiDesign.class);
    private static final String DESIGN_RESOURCE = "/skywars2/gui_design.json";
    private static JsonObject design;
    private static Map<String, JsonObject> sourceBindings;
    private static Map<String, Component> sourceMapNames;
    private static Map<String, JsonObject> sourceSlots;

    private OriginalGuiDesign() {}

    /** Shared source text for gameplay messages, with the same selectors, keys and formatting as GUI. */
    static Component sourceMessage(String path,ServerPlayer player,PlayerStore.Profile profile,GameConfig config) {
        var values=new RuntimeValues(profile,config,player);
        return decodeComponent(resolveDynamic(resolveStorageComponent(path,values),values,path),path);
    }

    static ItemStack button(ServerPlayer player, String namespace, int page, int slot) {
        return button(player, namespace, Integer.toString(page), slot, null, null);
    }

    static ItemStack button(ServerPlayer player, String namespace, int page, int slot,
                            PlayerStore.Profile profile, GameConfig config) {
        return button(player, namespace, Integer.toString(page), slot, profile, config);
    }

    static ItemStack button(ServerPlayer player, String id) {
        return button(player, Identifier.parse(id), null, null);
    }

    static ItemStack button(ServerPlayer player, String id, PlayerStore.Profile profile, GameConfig config) {
        if (!id.contains(":")) return button(player, "swrg", id, 0, profile, config);
        return button(player, Identifier.parse(id), profile, config);
    }

    private static ItemStack button(ServerPlayer player, String namespace, String pageId, int slot,
                                    PlayerStore.Profile profile, GameConfig config) {
        // Берём исходную ячейку по идентификаторам страницы и слота, затем применяем её условия и оформление.
        JsonObject page = null;
        for (JsonElement element : requiredArray(design(), "pages", "prepared GUI design")) {
            JsonObject candidate = element.getAsJsonObject();
            if (namespace.equals(candidate.get("namespace").getAsString())
                    && pageId.equals(candidate.get("pageId").getAsString())) {
                page = candidate;
                break;
            }
        }
        if (page == null) throw failure("Missing imported source page " + namespace + ":" + pageId);
        if (config != null && profile != null && namespace.equals("lbc") && pageId.equals("12000") && slot == 0
                && (config.ranked || me.skywars2.SkyWars2.game() != null
                    && me.skywars2.SkyWars2.game().lbc().challengeRewardAvailable(player)
                    && unlocked(player, namespace, 12000, slot, profile, config)))
            return button(player, Identifier.parse("lbc:gui/page/12000/0_completed"), profile, config);
        for (JsonElement slotElement : requiredArray(page, "slots", "page " + pageId)) {
            JsonObject entry = slotElement.getAsJsonObject();
            if (entry.get("slot").getAsInt() != slot) continue;
            if (profile != null && config != null) {
                ItemStack locked = lockedButton(player, namespace, pageId, entry, profile, config);
                if (locked != null) return locked;
            }
            for (JsonElement sourceElement : requiredArray(entry, "sources", "page " + pageId + " slot " + slot)) {
                JsonObject source = sourceElement.getAsJsonObject();
                if (source.has("sourceLootTable")) {
                    // The source page may list gated alternatives; the first native loot item is its stable default.
                    ItemStack stack = button(player, Identifier.parse(source.get("sourceLootTable").getAsString()), profile, config);
                    applySourcePageModifiers(stack, entry, player, profile, config);
                    return stack;
                }
                if (source.has("sourceItem")) {
                    Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(source.get("sourceItem").getAsString()));
                    if (item == null) throw failure("Unknown source item " + source.get("sourceItem").getAsString());
                    return new ItemStack(item);
                }
            }
            return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    /** Builds the exact source map item using stable IDs imported from page and mapselect functions. */
    static SourceMapButton sourceMapButton(ServerPlayer player, String mapId, boolean selected) {
        for (JsonElement pageElement : design().getAsJsonArray("pages")) {
            JsonObject page = pageElement.getAsJsonObject();
            String pageId = page.get("pageId").getAsString();
            if (!List.of("2000", "2000b", "2100", "2200").contains(pageId)) continue;
            for (JsonElement slotElement : page.getAsJsonArray("slots")) {
                JsonObject slot = slotElement.getAsJsonObject();
                for (JsonElement sourceElement : slot.getAsJsonArray("sources")) {
                    JsonObject source = sourceElement.getAsJsonObject();
                    if (!source.has("sourceMapId") || !source.get("sourceMapId").getAsString().equals(mapId)) continue;
                    // Selection modifiers also carry sourceMapId; they do not define a card.
                    if (!source.has("sourceItem")) continue;
                    Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(source.get("sourceItem").getAsString()));
                    if (item == null) throw failure("Unknown source map item " + source.get("sourceItem").getAsString());
                    ItemStack stack = new ItemStack(item);
                    Component name = sourceMapName(mapId);
                    if (name == null) throw failure("Map " + mapId + " has no imported name");
                    stack.set(DataComponents.CUSTOM_NAME, name);
                    if (selected) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
                    return new SourceMapButton(stack, selected, slot.get("slot").getAsInt());
                }
            }
        }
        return null;
    }

    /** Shares the source GUI caption with chat/status without creating a button stack. */
    static Component sourceMapName(String mapId) {
        design();
        Component name = sourceMapNames.get(mapId);
        return name == null ? null : name.copy();
    }

    private static JsonObject design() {
        if (design != null) return design;
        synchronized (OriginalGuiDesign.class) {
            if (design != null) return design;
            try (InputStream input = OriginalGuiDesign.class.getResourceAsStream(DESIGN_RESOURCE)) {
                if (input == null) throw failure("Missing prepared GUI design resource " + DESIGN_RESOURCE);
                design = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
                sourceBindings = loadSourceBindings(design);
                sourceMapNames = loadSourceMapNames(design);
                Map<String, JsonObject> slots = new HashMap<>();
                for (var pageElement : design.getAsJsonArray("pages")) {
                    var page = pageElement.getAsJsonObject();
                    for (var slotElement : page.getAsJsonArray("slots")) {
                        var slot = slotElement.getAsJsonObject();
                        slots.put(page.get("namespace").getAsString() + ":" + page.get("pageId").getAsString()
                                + ":" + slot.get("slot").getAsInt(), slot);
                    }
                }
                sourceSlots = Map.copyOf(slots);
                return design;
            } catch (RuntimeException e) {
                LOGGER.error("Could not load prepared GUI design", e);
                throw e;
            } catch (Exception e) {
                LOGGER.error("Could not load prepared GUI design", e);
                throw new IllegalStateException("Could not load prepared GUI design", e);
            }
        }
    }

    /** Общая проверка GUI-гейта; ranked обходит только перки, не косметику или сезонные ограничения. */
    static boolean unlocked(ServerPlayer player, String namespace, int page, int slot,
                            PlayerStore.Profile profile, GameConfig config) {
        design();
        if (me.skywars2.SkyWars2.game() != null && me.skywars2.SkyWars2.game().debugPerks(player)
                && perkPage(namespace, Integer.toString(page))) return true;
        if (config.ranked && perkPage(namespace, Integer.toString(page))) return true;
        var entry = sourceSlots.get(namespace + ":" + page + ":" + slot);
        if (entry == null) return false;
        if (entry.has("unlockConditions")) return matchesAccess(entry.getAsJsonObject("unlockConditions"), player, profile);
        if (entry.has("lockedVariants")) for (var element : entry.getAsJsonArray("lockedVariants"))
            if (matchesAccess(element.getAsJsonObject().getAsJsonObject("conditions"), player, profile)) return false;
        return true;
    }

    private static JsonObject cosmeticDesign;

    /** Finite authored detail-page data; shares the original item and text owners. */
    private static JsonObject cosmeticDesign() {
        if (cosmeticDesign != null) return cosmeticDesign;
        try (InputStream input = OriginalGuiDesign.class.getResourceAsStream("/skywars2/cases_cosmetics_menu_source.json")) {
            if (input == null) throw failure("Missing cosmetic detail source");
            cosmeticDesign = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            return cosmeticDesign;
        } catch (Exception e) { throw new IllegalStateException("Could not load cosmetic detail source", e); }
    }

    static ItemStack cosmeticButton(ServerPlayer player, int page, int slot, int selected,
                                    PlayerStore.Profile profile, GameConfig config) {
        ItemStack stack = ItemStack.EMPTY;
        var prepared = cosmeticDesign();
        for (var element : prepared.getAsJsonObject("pages").getAsJsonArray(Integer.toString(page))) {
            var step = element.getAsJsonObject();
            if (step.get("slot").getAsInt() != slot) continue;
            if (step.has("season") && step.get("season").getAsString().equals("halloween") != config.eventHalloween) continue;
            if (step.has("gate") && LbcSourceUnlocks.has(player, profile, step.get("gate").getAsString())
                    != step.get("owned").getAsBoolean()) continue;
            if (step.has("table")) stack = button(player, step.get("table").getAsString(), profile, config);
            if (step.has("literal")) {
                try {
                    stack = new net.minecraft.commands.arguments.item.ItemParser(player.registryAccess())
                            .parse(new com.mojang.brigadier.StringReader(step.get("literal").getAsString())).createItemStack(1);
                } catch (Exception e) { throw new IllegalStateException("Invalid authored cosmetic literal", e); }
            }
            if (step.has("modifier") && !stack.isEmpty())
                cosmeticModifier(stack, step.get("modifier").getAsString(), player, profile, config);
        }
        if (!stack.isEmpty()) for (var overlay : prepared.getAsJsonObject("overlays").getAsJsonArray(Integer.toString(page))) {
            var value = overlay.getAsJsonObject();
            if (value.get("slot").getAsInt() == slot && value.get("selected").getAsInt() == selected)
                cosmeticModifier(stack, "swrg:select", player, profile, config);
        }
        return stack;
    }

    private static JsonElement cosmeticText(JsonElement source, RuntimeValues values) {
        if (source.isJsonArray()) {
            var result = new JsonArray();
            for (var child : source.getAsJsonArray()) result.add(cosmeticText(child, values));
            return result;
        }
        if (!source.isJsonObject()) return source.deepCopy();
        var object = source.getAsJsonObject();
        if (object.has("nbt") && object.has("storage") && object.get("storage").getAsString().equals("swrg:lang"))
            return resolveStorageComponent(object.get("nbt").getAsString(), values);
        var result = new JsonObject();
        for (var entry : object.entrySet()) result.add(entry.getKey(), cosmeticText(entry.getValue(), values));
        return resolveDynamic(result, values, "cosmetic source component");
    }

    private static void cosmeticModifier(ItemStack stack, String id, ServerPlayer player,
                                          PlayerStore.Profile profile, GameConfig config) {
        var values = new RuntimeValues(profile, config, player);
        for (var element : cosmeticDesign().getAsJsonObject("modifiers").getAsJsonArray(id)) {
            var function = element.getAsJsonObject();
            switch (function.get("function").getAsString()) {
                case "minecraft:set_name" -> stack.set(DataComponents.CUSTOM_NAME,
                        decodeComponent(cosmeticText(function.get("name"), values), id));
                case "minecraft:set_lore" -> {
                    var lines = new ArrayList<Component>();
                    for (var line : function.getAsJsonArray("lore"))
                        lines.add(decodeComponent(cosmeticText(line, values), id));
                    var current = stack.get(DataComponents.LORE);
                    String mode = function.has("mode") ? function.get("mode").getAsString() : "append";
                    if (current != null && mode.equals("insert")) lines.addAll(current.lines());
                    else if (current != null && mode.equals("append")) lines.addAll(0, current.lines());
                    stack.set(DataComponents.LORE, new ItemLore(lines));
                }
                case "minecraft:set_components" -> {
                    var components = function.getAsJsonObject("components");
                    if (components.has("minecraft:enchantment_glint_override"))
                        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, components.get("minecraft:enchantment_glint_override").getAsBoolean());
                    else throw failure("Unexpected cosmetic modifier components " + id);
                }
                default -> throw failure("Unexpected cosmetic modifier " + id);
            }
        }
    }

    private static boolean perkPage(String namespace, String page) {
        return namespace.equals("swrg") && (page.equals("3000") || page.equals("4000"))
                || namespace.equals("lbc") && List.of("8000", "8100", "9000", "9100",
                        "10000", "10100", "11000", "11100", "12000").contains(page);
    }

    private static boolean matchesAccess(JsonObject conditions, ServerPlayer player, PlayerStore.Profile profile) {
        for (var tagElement : conditions.getAsJsonArray("tags")) {
            String tag = tagElement.getAsString();
            boolean inverted = tag.startsWith("!");
            if (LbcSourceUnlocks.has(player, profile, inverted ? tag.substring(1) : tag) == inverted) return false;
        }
        for (var gate : conditions.getAsJsonObject("advancements").entrySet()) {
            boolean done = profile != null && profile.lbcUnlocks.contains(gate.getKey());
            if (!done) {
                var advancement = player.level().getServer().getAdvancements().get(Identifier.parse(gate.getKey()));
                done = advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
            }
            if (done != gate.getValue().getAsBoolean()) return false;
        }
        return true;
    }

    private static ItemStack lockedButton(ServerPlayer player, String namespace, String page, JsonObject entry,
                                          PlayerStore.Profile profile, GameConfig config) {
        if (!entry.has("lockedVariants") || (config.ranked || me.skywars2.SkyWars2.game() != null
                && me.skywars2.SkyWars2.game().debugPerks(player)) && perkPage(namespace, page)) return null;
        ItemStack result = null;
        for (var element : entry.getAsJsonArray("lockedVariants")) {
            var variant = element.getAsJsonObject();
            if (!matchesAccess(variant.getAsJsonObject("conditions"), player, profile)) continue;
            var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(variant.get("item").getAsString()));
            if (item == null) throw failure("Unknown source locked item " + variant.get("item"));
            result = new ItemStack(item);
            var components = variant.getAsJsonObject("components");
            if (components.has("minecraft:item_model")) result.set(DataComponents.ITEM_MODEL,
                    Identifier.parse(components.get("minecraft:item_model").getAsString()));
            for (var modifier : variant.getAsJsonArray("modifiers"))
                applySourceModifierText(result, modifier.getAsString(), player, profile, config);
        }
        return result;
    }

    private static Map<String, Component> loadSourceMapNames(JsonObject prepared) {
        Map<String, Component> names = new HashMap<>();
        for (JsonElement pageElement : prepared.getAsJsonArray("pages")) {
            JsonObject page = pageElement.getAsJsonObject();
            if (!List.of("2000", "2000b", "2100", "2200").contains(page.get("pageId").getAsString())) continue;
            for (JsonElement slotElement : page.getAsJsonArray("slots")) {
                for (JsonElement sourceElement : slotElement.getAsJsonObject().getAsJsonArray("sources")) {
                    JsonObject source = sourceElement.getAsJsonObject();
                    if (!source.has("sourceMapId") || !source.has("sourceName")) continue;
                    String mapId = source.get("sourceMapId").getAsString();
                    JsonObject name = source.getAsJsonObject("sourceName").deepCopy();
                    name.remove("text");
                    name.addProperty("translate", source.get("translationKey").getAsString());
                    names.putIfAbsent(mapId, decodeComponent(name, "source map " + mapId));
                }
            }
        }
        return Map.copyOf(names);
    }

    private static Map<String, JsonObject> loadSourceBindings(JsonObject prepared) {
        Map<String, JsonObject> bindings = new HashMap<>();
        JsonArray pages = requiredArray(prepared, "pages", "prepared GUI design");
        for (JsonElement pageElement : pages) {
            JsonObject page = pageElement.getAsJsonObject();
            for (JsonElement slotElement : requiredArray(page, "slots", "page " + page.get("pageId"))) {
                JsonObject slot = slotElement.getAsJsonObject();
                for (JsonElement sourceElement : requiredArray(slot, "sources", "slot " + slot.get("slot"))) {
                    JsonObject source = sourceElement.getAsJsonObject();
                    if (!source.has("sourceLootTable") || !source.has("sourceComponentBindings")) continue;
                    String id = source.get("sourceLootTable").getAsString();
                    JsonObject previous = bindings.putIfAbsent(id, source.getAsJsonObject("sourceComponentBindings"));
                    if (previous != null && !previous.equals(source.getAsJsonObject("sourceComponentBindings"))) {
                        throw failure("Conflicting source component bindings for " + id);
                    }
                }
            }
        }
        if (prepared.has("sharedButtons")) {
            for (JsonElement sharedElement : requiredArray(prepared, "sharedButtons", "prepared GUI design")) {
                JsonObject shared = sharedElement.getAsJsonObject();
                if (shared.has("sourceLootTable") && shared.has("sourceComponentBindings")) {
                    registerBinding(bindings, shared.get("sourceLootTable").getAsString(),
                            shared.getAsJsonObject("sourceComponentBindings"));
                }
            }
        }
        if (prepared.has("sharedModifiers")) {
            for (JsonElement modifierElement : requiredArray(prepared, "sharedModifiers", "prepared GUI design")) {
                JsonObject modifier = modifierElement.getAsJsonObject();
                if (modifier.has("modifierId") && modifier.has("sourceComponentBindings")) {
                    registerBinding(bindings, modifier.get("modifierId").getAsString(),
                            modifier.getAsJsonObject("sourceComponentBindings"));
                }
            }
        }
        return Map.copyOf(bindings);
    }

    private static void registerBinding(Map<String, JsonObject> bindings, String id, JsonObject binding) {
        JsonObject previous = bindings.putIfAbsent(id, binding);
        if (previous != null && !previous.equals(binding)) {
            throw failure("Conflicting source component bindings for " + id);
        }
    }

    record SourceMapButton(ItemStack stack, boolean selected, int slot) {}

    private static ItemStack button(ServerPlayer player, Identifier id, PlayerStore.Profile profile, GameConfig config) {
        MinecraftServer server = player.level().getServer();
        if (server == null) throw failure("Player has no server while creating GUI item " + id);
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, id);
        LootTable table = server.reloadableRegistries().getLootTable(key);
        if (table == LootTable.EMPTY) throw failure("Missing source GUI loot table " + id);

        LootParams params = new LootParams.Builder(player.level())
                .withParameter(LootContextParams.ORIGIN, player.position())
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .create(LootContextParamSets.COMMAND);
        for (ItemStack stack : table.getRandomItems(params)) {
            if (!stack.isEmpty()) {
                ItemStack result = stack.copy();
                applySourceText(result, id, player, profile, config);
                return result;
            }
        }
        throw failure("Source GUI loot table produced no item: " + id);
    }

    private static void applySourceText(ItemStack stack, Identifier tableId, ServerPlayer player,
                                        PlayerStore.Profile profile, GameConfig config) {
        design();
        JsonObject binding = sourceBindings.get(tableId.toString());
        if (binding == null) return;
        RuntimeValues values = new RuntimeValues(profile, config, player);
        if (binding.has("name")) {
            for (JsonElement nameElement : binding.getAsJsonArray("name")) {
                JsonObject name = nameElement.getAsJsonObject();
                if (!matchesCondition(name.get("condition"), values)) continue;
                stack.set(DataComponents.CUSTOM_NAME,
                        expression(name.getAsJsonArray("terms"), "name", values));
            }
        }
        if (binding.has("lore")) {
            List<Component> lore = new ArrayList<>();
            JsonArray lines = binding.getAsJsonArray("lore");
            for (int index = 0; index < lines.size(); index++) {
                JsonObject line = lines.get(index).getAsJsonObject();
                if (!matchesCondition(line.get("condition"), values)) continue;
                lore.add(expression(line.getAsJsonArray("terms"), "lore line " + index, values));
            }
            stack.set(DataComponents.LORE, new ItemLore(lore));
        }
    }

    private static void applySourcePageModifiers(ItemStack stack, JsonObject slot, ServerPlayer player,
                                                 PlayerStore.Profile profile, GameConfig config) {
        for (JsonElement sourceElement : requiredArray(slot, "sources", "source modifier slot")) {
            JsonObject source = sourceElement.getAsJsonObject();
            if (source.has("itemModifier")) {
                String modifierId = source.get("itemModifier").getAsString();
                if (modifierId.equals("swrg:balance") || modifierId.equals("swrg:balance_mem")) {
                    applySourceModifierText(stack, modifierId, player, profile, config);
                }
            }
        }
    }

    static void applySourceModifierText(ItemStack stack, String modifierId, ServerPlayer player,
                                        PlayerStore.Profile profile, GameConfig config) {
        design();
        JsonObject binding = sourceBindings.get(modifierId);
        if (binding == null) throw failure("Missing prepared source item modifier " + modifierId);
        RuntimeValues values = new RuntimeValues(profile, config, player);
        if (binding.has("name")) {
            for (JsonElement nameElement : binding.getAsJsonArray("name")) {
                JsonObject name = nameElement.getAsJsonObject();
                if (matchesCondition(name.get("condition"), values)) {
                    stack.set(DataComponents.CUSTOM_NAME,
                            expression(name.getAsJsonArray("terms"), "modifier name " + modifierId, values));
                }
            }
        }
        if (binding.has("lore")) {
            List<Component> additions = new ArrayList<>();
            String mode = null;
            JsonArray lines = binding.getAsJsonArray("lore");
            for (int index = 0; index < lines.size(); index++) {
                JsonObject line = lines.get(index).getAsJsonObject();
                if (!matchesCondition(line.get("condition"), values)) continue;
                String lineMode = line.has("mode") ? line.get("mode").getAsString() : "replace_all";
                if (mode != null && !mode.equals(lineMode)) throw failure("Mixed source lore modes in modifier " + modifierId);
                mode = lineMode;
                additions.add(expression(line.getAsJsonArray("terms"),
                        "modifier lore " + modifierId + " line " + index, values));
            }
            List<Component> result = new ArrayList<>();
            ItemLore current = stack.get(DataComponents.LORE);
            if (mode == null || mode.equals("replace_all")) {
                result.addAll(additions);
            } else if (mode.equals("append")) {
                if (current != null) result.addAll(current.lines());
                result.addAll(additions);
            } else if (mode.equals("insert")) {
                result.addAll(additions);
                if (current != null) result.addAll(current.lines());
            } else {
                throw failure("Unsupported source lore mode " + mode + " in modifier " + modifierId);
            }
            stack.set(DataComponents.LORE, new ItemLore(result));
        }
    }

    private static boolean matchesCondition(JsonElement condition, RuntimeValues values) {
        if (condition == null || condition.isJsonNull()) return true;
        JsonObject object = condition.getAsJsonObject();
        if (!"minecraft:entity_scores".equals(object.get("type").getAsString())) {
            throw failure("Unsupported source loot condition " + condition);
        }
        JsonObject scores = object.getAsJsonObject("scores");
        for (Map.Entry<String, JsonElement> entry : scores.entrySet()) {
            int current = Integer.parseInt(scoreValue("@s", entry.getKey(), values));
            JsonElement range = entry.getValue();
            if (range.isJsonPrimitive()) {
                if (current != range.getAsInt()) return false;
            } else {
                JsonObject bounds = range.getAsJsonObject();
                if (bounds.has("min") && current < bounds.get("min").getAsInt()) return false;
                if (bounds.has("max") && current > bounds.get("max").getAsInt()) return false;
            }
        }
        return true;
    }

    private static Component expression(JsonArray terms, String description, RuntimeValues values) {
        List<Component> parts = new ArrayList<>();
        for (JsonElement termElement : terms) {
            JsonObject term = termElement.getAsJsonObject();
            Component part;
            if (term.has("storagePath")) {
                String path = term.get("storagePath").getAsString();
                JsonElement prepared = resolveDynamic(resolveStorageComponent(path, values), values, description + " at " + path);
                JsonElement styled = applyWrapperStyle(prepared, term, path, values);
                part = decodeComponent(styled, description + " at " + path);
            } else if (term.has("literalComponent")) {
                JsonElement resolved = resolveDynamic(term.get("literalComponent"), values, description);
                part = decodeComponent(resolved, description + " dynamic source component");
            } else {
                throw failure("Invalid prepared component term for " + description + ": " + term);
            }
            parts.add(part);
        }
        if (parts.isEmpty()) throw failure("Empty prepared component expression for " + description);
        Component result = parts.getFirst();
        for (int index = 1; index < parts.size(); index++) result = result.copy().append(parts.get(index));
        return result;
    }

    private static JsonElement resolveStorageComponent(String path, RuntimeValues values) {
        JsonObject components = design().getAsJsonObject("localizedStorageComponents");
        String root = path;
        String suffix = "";
        if (path.endsWith(".Name")) {
            root = path.substring(0, path.length() - ".Name".length());
            suffix = "Name";
        } else {
            int lore = path.lastIndexOf(".Lore[");
            if (lore >= 0 && path.endsWith("]")) {
                root = path.substring(0, lore);
                suffix = path.substring(lore + 6, path.length() - 1);
            }
        }
        if (root.startsWith("page1000.s") && root.indexOf('-', "page1000.s".length()) < 0) {
            if (root.equals("page1000.s12") && values.config != null && values.config.tntProtection == 5) {
                // The legacy page only supplied levels 0..4. Keep its exact name style for level V.
                JsonObject component = new JsonObject();
                component.addProperty("translate", "skywars2.gui.source.page1000.s12-5.part0");
                component.addProperty("color", "white");
                component.addProperty("italic", false);
                return component;
            }
            if (root.equals("page1000.s24") && values.config != null
                    && !List.of(2, 10, 20, 40, 60).contains(values.config.health)) {
                JsonObject health = new JsonObject();
                JsonObject component = new JsonObject();
                component.addProperty("translate", "skywars2.gui.original.health");
                JsonArray arguments = new JsonArray();
                JsonObject number = new JsonObject();
                number.addProperty("text", Integer.toString(values.config.health));
                arguments.add(number);
                component.add("with", arguments);
                component.addProperty("color", "white");
                component.addProperty("italic", false);
                health.add("Name", component);
                return health.get("Name");
            }
            root = configuredStorageVariant(root, values);
        }
        if (!components.has(root)) throw failure("Missing prepared localized source component " + path);
        JsonElement value = components.get(root);
        if (path.equals("page1000.s11.Name") && values.config != null) {
            JsonObject modifier = value.getAsJsonObject().getAsJsonObject("Name").deepCopy();
            modifier.addProperty("translate", switch (values.config.luckyModifier) {
                case 0 -> "swrg.cfg_lucky_modifier_0";
                case 1 -> "swrg.cfg_lucky_modifier_1";
                case 2 -> "swrg.cfg_lucky_modifier_2";
                case 3 -> "swrg.cfg_lucky_modifier_3";
                case 4 -> "swrg.cfg_lucky_modifier_4";
                case 5 -> "swrg.cfg_lucky_modifier_5";
                case 6 -> "swrg.cfg_lucky_modifier_6";
                case 7 -> "swrg.cfg_lucky_modifier_7";
                case 8 -> "swrg.cfg_lucky_modifier_8";
                case 9 -> "swrg.cfg_lucky_modifier_9";
                case 10 -> "swrg.cfg_lucky_modifier_10";
                case 11 -> "swrg.cfg_lucky_modifier_11";
                case 12 -> "swrg.cfg_lucky_modifier_12";
                default -> throw failure("Unknown LuckyWars modifier category " + values.config.luckyModifier);
            });
            return modifier;
        }
        if (suffix.equals("Name")) {
            if (!value.isJsonObject() || !value.getAsJsonObject().has("Name")) throw failure("Missing Name in source component " + path);
            return value.getAsJsonObject().get("Name");
        }
        if (!suffix.isEmpty()) {
            if (!value.isJsonObject() || !value.getAsJsonObject().has("Lore")) throw failure("Missing Lore in source component " + path);
            JsonArray lore = value.getAsJsonObject().getAsJsonArray("Lore");
            int index;
            try { index = Integer.parseInt(suffix); }
            catch (NumberFormatException e) { throw failure("Invalid lore path " + path); }
            if (index < 0 || index >= lore.size()) throw failure("Missing lore index in source component " + path);
            return lore.get(index);
        }
        return value;
    }

    private static String configuredStorageVariant(String path, RuntimeValues values) {
        String slot = path.substring("page1000.s".length());
        int value;
        try { value = Integer.parseInt(slot); }
        catch (NumberFormatException e) { throw failure("Unknown dynamic source setting path " + path); }
        if (values.config == null) return sourceDefaultVariant(path);
        int setting;
        switch (value) {
            // gui/page/1000-lang использует отдельные storage-имена для этих настроек.
            case 8 -> { return values.config.offhandWands ? "lefthand_on" : "lefthand_off"; }
            case 9 -> { return "lives_" + values.config.lives; }
            case 10 -> { return values.config.noBorders ? "no_borders_on" : "no_borders_off"; }
            case 11 -> {
                return switch (values.config.luckyModifier) {
                    case 0 -> "cfg_lucky_modifier_0";
                    case 1 -> "cfg_lucky_modifier_1";
                    case 2 -> "cfg_lucky_modifier_2";
                    case 3 -> "cfg_lucky_modifier_3";
                    case 4 -> "cfg_lucky_modifier_4";
                    case 5 -> "cfg_lucky_modifier_5";
                    case 6 -> "cfg_lucky_modifier_6";
                    case 7 -> "cfg_lucky_modifier_7";
                    case 8 -> "cfg_lucky_modifier_8";
                    case 9 -> "cfg_lucky_modifier_9";
                    case 10 -> "cfg_lucky_modifier_10";
                    case 11 -> "cfg_lucky_modifier_11";
                    case 12 -> "cfg_lucky_modifier_12";
                    default -> throw failure("Unknown LuckyWars modifier category " + values.config.luckyModifier);
                };
            }
            case 0 -> setting = values.config.pvpMode;
            case 1 -> setting = values.config.day;
            case 2 -> setting = values.config.gameMode;
            case 3 -> setting = values.config.vanillaKits ? 1 : 0;
            case 4 -> setting = values.config.vanillaSkills ? 1 : 0;
            case 5 -> setting = values.config.stageOneEvent;
            case 6 -> setting = values.config.stageTwoEvent;
            case 12 -> setting = values.config.tntProtection;
            case 13 -> setting = values.config.lbcKits ? 1 : 0;
            case 14 -> setting = values.config.lbcSkills ? 1 : 0;
            case 15 -> setting = values.config.lbcLevelups ? 1 : 0;
            case 16 -> setting = values.config.lbcLevelupLimit;
            case 17 -> setting = values.config.lbcTrinketLimit;
            case 22 -> setting = values.config.lbcChallenges ? 1 : 0;
            case 24 -> {
                int[] presets = {2, 10, 20, 40, 60};
                int index = java.util.Arrays.binarySearch(presets, values.config.health);
                if (index < 0) return sourceDefaultVariant(path);
                setting = index;
            }
            default -> { return sourceDefaultVariant(path); }
        }
        String selected = path + "-" + setting;
        return design().getAsJsonObject("localizedStorageComponents").has(selected)
                ? selected : sourceDefaultVariant(path);
    }

    private static String sourceDefaultVariant(String path) {
        JsonObject components = design().getAsJsonObject("localizedStorageComponents");
        String prefix = path + "-";
        int smallest = Integer.MAX_VALUE;
        for (String candidate : components.keySet()) {
            if (!candidate.startsWith(prefix)) continue;
            try { smallest = Math.min(smallest, Integer.parseInt(candidate.substring(prefix.length()))); }
            catch (NumberFormatException ignored) { }
        }
        if (smallest != Integer.MAX_VALUE) return prefix + smallest;
        if (components.has(path)) return path;
        throw failure("Missing source default variant for " + path);
    }

    private static JsonElement applyWrapperStyle(JsonElement source, JsonObject binding, String path, RuntimeValues values) {
        JsonElement result = source.deepCopy();
        JsonObject target;
        if (result.isJsonObject()) {
            target = result.getAsJsonObject();
        } else {
            target = new JsonObject();
            JsonArray extra = new JsonArray();
            extra.add(result);
            target.add("text", new com.google.gson.JsonPrimitive(""));
            target.add("extra", extra);
            result = target;
        }
        for (String key : List.of("color", "bold", "italic", "underlined", "strikethrough", "obfuscated", "font")) {
            if (binding.has(key)) target.add(key, binding.get(key).deepCopy());
        }
        if (binding.has("extra")) {
            JsonArray extra = target.has("extra") && target.get("extra").isJsonArray()
                    ? target.getAsJsonArray("extra") : new JsonArray();
            for (JsonElement child : binding.getAsJsonArray("extra")) extra.add(resolveDynamic(child, values, "extra for " + path));
            target.add("extra", extra);
        }
        return result;
    }

    private static JsonElement resolveDynamic(JsonElement source, RuntimeValues values, String description) {
        if (source == null || source.isJsonNull() || source.isJsonPrimitive()) return source == null ? null : source.deepCopy();
        if (source.isJsonArray()) {
            JsonArray result = new JsonArray();
            for (JsonElement child : source.getAsJsonArray()) result.add(resolveDynamic(child, values, description));
            return result;
        }
        JsonObject object = source.getAsJsonObject();
        if (object.has("selector") && object.get("selector").isJsonPrimitive()) {
            if (values == null || values.player == null) throw failure("Missing player for source selector at " + description);
            JsonObject literal = object.deepCopy();
            literal.remove("selector");
            literal.remove("type");
            literal.addProperty("text", values.player.getName().getString());
            return resolveDynamic(literal, values, description);
        }
        if (object.has("score") && object.get("score").isJsonObject()) {
            if (values == null) throw failure("Missing in-memory profile/config for source score at " + description);
            JsonObject score = object.getAsJsonObject("score");
            String name = score.get("name").getAsString();
            String objective = score.get("objective").getAsString();
            JsonObject literal = object.deepCopy();
            literal.remove("score");
            literal.remove("type");
            literal.addProperty("text", scoreValue(name, objective, values));
            return resolveDynamic(literal, values, description);
        }
        JsonObject result = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            result.add(entry.getKey(), resolveDynamic(entry.getValue(), values, description));
        }
        return result;
    }

    private static String scoreValue(String name, String objective, RuntimeValues values) {
        PlayerStore.Profile profile = values.profile;
        if (name.equals("@s") && profile == null) throw failure("Missing player profile for score " + objective);
        if (profile == null && objective.contains("stat_")) throw failure("Missing player profile for score " + objective);
        if ("swrg.stat_games".equals(objective)) return Integer.toString(profile.games);
        if ("swrg.coins".equals(objective)) return Integer.toString(profile.coins);
        if ("swrg.memcoins".equals(objective)) return Integer.toString(profile.memcoins);
        if (objective.matches("swrg\\.keys[0-4]")) return Integer.toString(profile.keys[objective.charAt(objective.length() - 1) - '0']);
        if ("challenge_score".equals(objective)) return Integer.toString(profile.lbcChallengeProgress);
        if ("swrg.wr".equals(objective)) {
            return Integer.toString(profile.games == 0 ? 0 : profile.wins * 100 / profile.games);
        }
        if ("swrg.kd1..".equals(objective)) {
            return Integer.toString(profile.deaths == 0 ? profile.kills : profile.kills / profile.deaths);
        }
        if ("swrg.kd..1".equals(objective)) {
            return Integer.toString(profile.deaths == 0 ? 0 : (profile.kills * 100 / profile.deaths) % 100);
        }
        if ("swrg.math".equals(objective) && name.equals("#lbctrinket")) {
            if (profile == null) throw failure("Missing player profile for LBC trinket score");
            return Integer.toString(values.config == null ? 9 : values.config.lbcTrinketLimit);
        }
        if ("swrg.math".equals(objective) && name.equals("#lbclevelupmax")) {
            if (profile == null) throw failure("Missing player profile for LBC level score");
            return Integer.toString(values.config == null ? 4 : values.config.lbcLevelupLimit);
        }
        if ("swrg.math".equals(objective) && name.equals("#gamemode")) {
            return Integer.toString(values.config == null ? 1 : values.config.gameMode);
        }
        throw failure("No in-memory mapping for source score " + name + " / " + objective);
    }

    private static Component decodeComponent(JsonElement json, String context) {
        return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow(error -> failure("Invalid component " + context + ": " + error));
    }

    private static JsonArray requiredArray(JsonObject object, String key, String context) {
        if (!object.has(key) || !object.get(key).isJsonArray()) throw failure("Missing array " + key + " in " + context);
        return object.getAsJsonArray(key);
    }

    private static IllegalStateException failure(String message) {
        LOGGER.error(message);
        return new IllegalStateException(message);
    }

    private record RuntimeValues(PlayerStore.Profile profile, GameConfig config, ServerPlayer player) {}
}
