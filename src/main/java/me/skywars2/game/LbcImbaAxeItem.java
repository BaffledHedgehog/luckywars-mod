package me.skywars2.game;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

/** A distinct native token, with the source prototype decoded once during registry bootstrap. */
public final class LbcImbaAxeItem {
    private static final String DEFINITION =
            "/data/lbc/loot_table/item_defs/minecraft/golden_axe__9834f449f80c.json";
    public static final Item TOKEN = registerAuthored();

    private LbcImbaAxeItem() { }
    public static void register() { }

    private static Item registerAuthored() {
        try (var input = LbcImbaAxeItem.class.getResourceAsStream(DEFINITION)) {
            if (input == null) throw new IllegalStateException("Missing IMBA Axe item definition");
            var source = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("pools").get(0).getAsJsonObject()
                    .getAsJsonArray("entries").get(0).getAsJsonObject();
            if (!source.get("name").getAsString().equals("minecraft:netherite_axe"))
                throw new IllegalStateException("Unexpected IMBA Axe source carrier");
            var rows = source.getAsJsonArray("modifier");
            Identifier authoredModel = null;
            for (var value : rows) {
                var row = value.getAsJsonObject();
                if (row.get("type").getAsString().equals("minecraft:set_components")) {
                    var components = row.getAsJsonObject("components");
                    if (components.has("minecraft:item_model"))
                        authoredModel = Identifier.parse(components.get("minecraft:item_model").getAsString());
                }
            }
            if (authoredModel == null) throw new IllegalStateException("Missing IMBA Axe source model");
            final Identifier model = authoredModel;
            var key = ResourceKey.create(Registries.ITEM, Identifier.parse("skywars2:imba_axe"));
            var properties = new Item.Properties() {
                @Override public Identifier effectiveModel() { return model; }
            }.setId(key).axe(ToolMaterial.NETHERITE, 5, -3).fireResistant().overrideDescription("imba_axe");
            for (var value : rows) {
                var row = value.getAsJsonObject();
                switch (row.get("type").getAsString()) {
                    case "minecraft:set_count" -> {
                        if (row.get("count").getAsInt() != 1)
                            throw new IllegalStateException("IMBA Axe source count requires loot update");
                    }
                    case "minecraft:set_components" -> {
                        for (var component : row.getAsJsonObject("components").entrySet()) {
                            var type = BuiltInRegistries.DATA_COMPONENT_TYPE
                                    .getValue(Identifier.parse(component.getKey()));
                            if (type == null) throw new IllegalStateException("Unknown IMBA Axe source component");
                            component(properties, type, component.getValue());
                        }
                    }
                    case "minecraft:set_name" -> properties.component(DataComponents.CUSTOM_NAME,
                            ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, row.get("name")).getOrThrow());
                    case "minecraft:set_custom_data" -> properties.component(DataComponents.CUSTOM_DATA,
                            CustomData.of(TagParser.parseCompoundFully(row.get("tag").getAsString())));
                    case "minecraft:set_lore" -> {
                        if (!row.get("mode").getAsString().equals("replace_all"))
                            throw new IllegalStateException("Unexpected IMBA Axe source lore operation");
                        var lines = new ArrayList<Component>();
                        for (var line : row.getAsJsonArray("lore"))
                            lines.add(ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, line).getOrThrow());
                        properties.component(DataComponents.LORE, new ItemLore(lines));
                    }
                    default -> throw new IllegalStateException("Unsupported IMBA Axe source modifier");
                }
            }
            return Registry.register(BuiltInRegistries.ITEM, key,
                    new SimplePolymerItem(properties, Items.NETHERITE_AXE, true));
        } catch (Exception failure) {
            throw new IllegalStateException("Invalid IMBA Axe source item definition", failure);
        }
    }

    private static <T> void component(Item.Properties properties, DataComponentType<T> type, JsonElement value) {
        // Registry-backed enchantments resolve with the native default-component lifecycle, never per stack/tick.
        var frozen = value.deepCopy();
        properties.delayedComponent(type, lookup ->
                type.codec().parse(lookup.createSerializationContext(JsonOps.INSTANCE), frozen).getOrThrow());
    }
}
