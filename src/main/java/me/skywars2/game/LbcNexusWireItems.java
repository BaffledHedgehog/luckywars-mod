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

/** Native identities and original presentations for the crimson and onyx Nexus wires. */
public final class LbcNexusWireItems {
    private static final CompoundTag RED_TRIGGER = trigger("nexus_wire_red");
    private static final CompoundTag BLACK_TRIGGER = trigger("nexus_wire_black");
    public static final Item RED = wire("nexus_wire_red", "lbc:nexus_wire_red", ChatFormatting.RED);
    public static final Item BLACK = wire("nexus_wire_black", "lbc:nexus_wire_onyx", ChatFormatting.GRAY);

    private LbcNexusWireItems() { }

    public static void register() { }

    private static CompoundTag trigger(String name) {
        var tag = new CompoundTag();
        tag.putInt(name, 1);
        return tag;
    }

    private static Item wire(String name, String modelName, ChatFormatting color) {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse("skywars2:" + name));
        var model = Identifier.parse(modelName);
        var flags = trigger(name);
        flags.putInt("instantitem", 1);
        var properties = new Item.Properties() {
            @Override public Identifier effectiveModel() { return model; }
        }.setId(key).overrideDescription(name)
                .component(DataComponents.ITEM_MODEL, model)
                .component(DataComponents.TOOLTIP_STYLE, Identifier.parse("lbc:tier_q"))
                .component(DataComponents.CUSTOM_DATA, CustomData.of(flags))
                .component(DataComponents.CUSTOM_NAME, Component.translatable(name)
                        .withStyle(style -> style.withColor(color).withItalic(false)))
                .component(DataComponents.LORE, new ItemLore(List.of(
                        Component.translatable(name + ".lore.1")
                                .withStyle(style -> style.withColor(color).withItalic(false)),
                        Component.translatable(name + ".lore.2")
                                .withStyle(style -> style.withColor(color).withItalic(false)),
                        Component.translatable("nexus_wire.lore")
                                .withStyle(style -> style.withColor(ChatFormatting.GOLD).withItalic(false)))));
        return Registry.register(BuiltInRegistries.ITEM, key,
                new SimplePolymerItem(properties, Items.REPEATING_COMMAND_BLOCK, true));
    }

    public static boolean isRed(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(RED)
                || stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).matchedBy(RED_TRIGGER));
    }

    public static boolean isBlack(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(BLACK)
                || stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).matchedBy(BLACK_TRIGGER));
    }
}
