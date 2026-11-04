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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

/** A distinct native token, with the source prototype decoded once during registry bootstrap. */
public final class LbcMellstroyMaskItem {
    private static final String DEFINITION =
            "/data/lbc/loot_table/item_defs/lbc/mellstroy_mask__fc2cbe716754.json";
    public static final Item ITEM = registerAuthored();

    private LbcMellstroyMaskItem() { }
    public static void register() { }

    private static Item registerAuthored() {
        try (var input = LbcMellstroyMaskItem.class.getResourceAsStream(DEFINITION)) {
            if (input == null) throw new IllegalStateException("Missing Mellstroy Mask item definition");
            var source = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("pools").get(0).getAsJsonObject()
                    .getAsJsonArray("entries").get(0).getAsJsonObject();
            if (!source.get("name").getAsString().equals("minecraft:carrot_on_a_stick"))
                throw new IllegalStateException("Unexpected Mellstroy Mask source carrier");
            var rows = source.getAsJsonArray("functions");
            Identifier authoredModel = null;
            for (var value : rows) {
                var row = value.getAsJsonObject();
                if (row.get("function").getAsString().equals("minecraft:set_components")) {
                    var components = row.getAsJsonObject("components");
                    if (components.has("minecraft:item_model"))
                        authoredModel = Identifier.parse(components.get("minecraft:item_model").getAsString());
                }
            }
            if (authoredModel == null) throw new IllegalStateException("Missing Mellstroy Mask source model");
            final Identifier model = authoredModel;
            var key = ResourceKey.create(Registries.ITEM, Identifier.parse("skywars2:mellstroy_mask"));
            var properties = new Item.Properties() {
                @Override public Identifier effectiveModel() { return model; }
            }.setId(key).durability(5000).overrideDescription("mellstroy_mask");
            for (var value : rows) {
                var row = value.getAsJsonObject();
                switch (row.get("function").getAsString()) {
                    case "minecraft:set_count" -> {
                        if (row.get("count").getAsInt() != 1)
                            throw new IllegalStateException("Mellstroy Mask source count requires loot update");
                    }
                    case "minecraft:set_components" -> {
                        for (var component : row.getAsJsonObject("components").entrySet()) {
                            var type = BuiltInRegistries.DATA_COMPONENT_TYPE
                                    .getValue(Identifier.parse(component.getKey()));
                            if (type == null) throw new IllegalStateException("Unknown Mellstroy Mask source component");
                            component(properties, type, component.getValue());
                        }
                    }
                    case "minecraft:set_name" -> properties.component(DataComponents.CUSTOM_NAME,
                            ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, row.get("name")).getOrThrow());
                    case "minecraft:set_custom_data" -> properties.component(DataComponents.CUSTOM_DATA,
                            CustomData.of(TagParser.parseCompoundFully(row.get("tag").getAsString())));
                    case "minecraft:set_lore" -> {
                        if (!row.get("mode").getAsString().equals("replace_all"))
                            throw new IllegalStateException("Unexpected Mellstroy Mask source lore operation");
                        var lines = new ArrayList<Component>();
                        for (var line : row.getAsJsonArray("lore"))
                            lines.add(ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, line).getOrThrow());
                        properties.component(DataComponents.LORE, new ItemLore(lines));
                    }
                    default -> throw new IllegalStateException("Unsupported Mellstroy Mask source modifier");
                }
            }
            return Registry.register(BuiltInRegistries.ITEM, key,
                    new SimplePolymerItem(properties, Items.CARROT_ON_A_STICK, true));
        } catch (Exception failure) {
            throw new IllegalStateException("Invalid Mellstroy Mask source item definition", failure);
        }
    }

    public static boolean isMask(ItemStack stack) {
        return !stack.isEmpty() && LbcFartus.data(stack).getIntOr("mellstroy_mask", 0) == 1
                && Identifier.parse("lbc:mellstroy_mask").equals(stack.get(DataComponents.ITEM_MODEL));
    }

    public static ItemStack normalize(ItemStack source) {
        if (!isMask(source) || source.is(ITEM)) return source;
        var result = source.transmuteCopy(ITEM);
        result.applyComponents(source.getComponents());
        return result;
    }

    private static <T> void component(Item.Properties properties, DataComponentType<T> type, JsonElement value) {
        // Registry-backed enchantments resolve with the native default-component lifecycle, never per stack/tick.
        var frozen = value.deepCopy();
        properties.delayedComponent(type, lookup ->
                type.codec().parse(lookup.createSerializationContext(JsonOps.INSTANCE), frozen).getOrThrow());
    }
}
