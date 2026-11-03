package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

/** Native Nexus tools retain the source loot presentation and Paper compass identity. */
public final class LbcNexusToolItems {
    private static final CompoundTag COMPASS_TRIGGER = trigger("nexus_compass");
    private static final CompoundTag ROADGUN_TRIGGER = trigger("nexus_roadgun");
    private static final CompoundTag CORRUPTER_TRIGGER = trigger("nexus_corrupter");
    private static final CompoundTag COMPASS_LWS_TRIGGER = paperCompass("lws:item", "lws:type");
    private static final CompoundTag COMPASS_LEGACY_TRIGGER = paperCompass("lucky_wars:item", "lucky_wars:type");

    public static final Item COMPASS = tool("skywars2:nexus_compass", "lbc:nexus_compass", "lbc:tier_x",
            "nexus_compass", false, List.of(
                    lore("nexus_compass.lore.1"), lore("nexus_compass.lore.2"), lore("nexus_compass.lore.3")));
    public static final Item ROADGUN = tool("skywars2:nexus_roadgun", "lbc:nexus_roadgun", "lbc:tier_s",
            "nexus_roadgun", true, List.of(
                    lore("nexus_roadgun.lore.1"), lore("nexus_roadgun.lore.2"), lore("nexus_roadgun.lore.3")));
    public static final Item CORRUPTER = tool("skywars2:nexus_corrupter", "lbc:nexus_corrupter", "lbc:tier_a",
            "nexus_corrupter", true, List.of(
                    lore("nexus_corrupter.lore.1"), lore("nexus_corrupter.lore.2"),
                    lore("nexus_corrupter.lore.3"), lore("nexus_corrupter.lore.4")));

    private LbcNexusToolItems() { }

    public static void register() { }

    private static CompoundTag trigger(String name) {
        var tag = new CompoundTag();
        tag.putInt(name, 1);
        return tag;
    }

    private static CompoundTag paperCompass(String itemKey, String typeKey) {
        var type = new CompoundTag();
        type.putString(typeKey, "nexus_compass");
        var values = new CompoundTag();
        values.put(itemKey, type);
        var tag = new CompoundTag();
        tag.put("PublicBukkitValues", values);
        return tag;
    }

    private static Component lore(String key) {
        return Component.translatable(key)
                .withStyle(style -> style.withColor(ChatFormatting.WHITE).withItalic(false));
    }

    private static Item tool(String id, String modelName, String tooltipStyle, String name,
                             boolean spellCooldown, List<Component> lore) {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse(id));
        var model = Identifier.parse(modelName);
        var flags = trigger(name);
        flags.putInt("magicwand", 1);
        flags.putInt("spellwithcooldown_n", 1);
        if (spellCooldown) {
            flags.putInt("spellwithcooldown", 1);
        } else {
            flags.merge(COMPASS_LWS_TRIGGER);
        }
        var properties = new Item.Properties() {
            @Override public Identifier effectiveModel() { return model; }
        }.setId(key).durability(25).overrideDescription(name)
                .component(DataComponents.ITEM_MODEL, model)
                .component(DataComponents.TOOLTIP_STYLE, Identifier.parse(tooltipStyle))
                .component(DataComponents.CUSTOM_DATA, CustomData.of(flags))
                .component(DataComponents.CUSTOM_NAME, Component.translatable(name)
                        .withStyle(style -> style.withItalic(false)))
                .component(DataComponents.LORE, new ItemLore(lore));
        return Registry.register(BuiltInRegistries.ITEM, key,
                new SimplePolymerItem(properties, Items.CARROT_ON_A_STICK, true));
    }

    public static boolean isCompass(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(COMPASS)) return true;
        // Fixed partial NBT predicates accept both historical Paper namespaces without copying stack tags.
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.matchedBy(COMPASS_TRIGGER) || data.matchedBy(COMPASS_LWS_TRIGGER)
                || data.matchedBy(COMPASS_LEGACY_TRIGGER);
    }

    public static boolean isRoadgun(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(ROADGUN)
                || stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).matchedBy(ROADGUN_TRIGGER));
    }

    public static boolean isCorrupter(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(CORRUPTER)
                || stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).matchedBy(CORRUPTER_TRIGGER));
    }
}
