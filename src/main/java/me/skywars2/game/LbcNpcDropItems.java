package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
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

import java.util.List;

/** Source presentation for the normal Transformator and Magic Academy acquisitions. */
public final class LbcNpcDropItems {
    private static final CompoundTag TRANSFORMATOR_DATA = flags("transformator");
    private static final CompoundTag ACADEMY_DATA = flags("magic_academy");

    public static final Item TRANSFORMATOR = create("transformator_drop", "drop_transformator",
            "lbc:tier_ff", TRANSFORMATOR_DATA);
    public static final Item ACADEMY = create("magic_academy_drop", "drop_academy",
            "lbc:tier_q", ACADEMY_DATA);

    private LbcNpcDropItems() { }

    private static CompoundTag flags(String flag) {
        var data = new CompoundTag();
        data.putInt(flag, 1);
        data.putInt("drop", 1);
        return data;
    }

    private static Item create(String id, String name, String tier, CompoundTag data) {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse("skywars2:" + id));
        var properties = new Item.Properties().setId(key).overrideDescription(name)
                .component(DataComponents.CUSTOM_DATA, CustomData.of(data))
                .component(DataComponents.TOOLTIP_STYLE, Identifier.parse(tier))
                .component(DataComponents.CUSTOM_NAME, Component.translatable(name)
                        .withStyle(style -> style.withColor(ChatFormatting.YELLOW).withItalic(false)))
                .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("trap_desc")
                        .withStyle(style -> style.withColor(ChatFormatting.WHITE).withItalic(false)))));
        return Registry.register(BuiltInRegistries.ITEM, key,
                new SimplePolymerItem(properties, Items.REPEATING_COMMAND_BLOCK, true));
    }

    public static void register() {
        DefaultItemComponentEvents.MODIFY.register(context -> {
            context.modify(TRANSFORMATOR, builder -> builder.set(DataComponents.ITEM_MODEL,
                    Identifier.parse("lbc:drop_trap")));
            context.modify(ACADEMY, builder -> builder.set(DataComponents.ITEM_MODEL,
                    Identifier.parse("lbc:drop_not_trap")));
        });
    }

    public static boolean isTransformator(ItemStack stack) {
        return matches(stack, TRANSFORMATOR_DATA);
    }

    public static boolean isAcademy(ItemStack stack) {
        return matches(stack, ACADEMY_DATA);
    }

    private static boolean matches(ItemStack stack, CompoundTag data) {
        return !stack.isEmpty() && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .matchedBy(data);
    }
}
