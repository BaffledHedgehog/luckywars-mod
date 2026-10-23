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

/** Separate source presentations share the native drone producer. */
public final class LbcDroneDropItem {
    private static final CompoundTag TRIGGER = trigger();
    private static final CompoundTag NORMAL_TRIGGER = normalTrigger();
    public static final Item ITEM = create();
    public static final Item NORMAL_ITEM = createNormal();
    private LbcDroneDropItem() { }

    private static CompoundTag trigger() {
        var data = new CompoundTag();
        data.putInt("droppertnt_old", 1);
        data.putInt("drop", 1);
        return data;
    }

    private static Item create() {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse("skywars2:old_tnt_drone_drop"));
        return Registry.register(BuiltInRegistries.ITEM, key, new SimplePolymerItem(
                new Item.Properties().setId(key).overrideDescription("block.minecraft.barrier")
                        .component(DataComponents.CUSTOM_DATA, CustomData.of(TRIGGER)), Items.BARRIER, true));
    }

    private static CompoundTag normalTrigger() {
        var data = new CompoundTag();
        data.putInt("droppertnt", 1);
        data.putInt("drop", 1);
        return data;
    }

    private static Item createNormal() {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse("skywars2:tnt_drone_drop"));
        return Registry.register(BuiltInRegistries.ITEM, key, new SimplePolymerItem(
                new Item.Properties().setId(key).overrideDescription("trap_hohol")
                        .component(DataComponents.CUSTOM_DATA, CustomData.of(NORMAL_TRIGGER))
                        .component(DataComponents.TOOLTIP_STYLE, Identifier.parse("lbc:tier_ff"))
                        .component(DataComponents.CUSTOM_NAME, Component.translatable("trap_hohol")
                                .withStyle(style -> style.withColor(ChatFormatting.YELLOW).withItalic(false)))
                        .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("trap_desc")
                                .withStyle(style -> style.withColor(ChatFormatting.WHITE).withItalic(false))))),
                Items.REPEATING_COMMAND_BLOCK, true));
    }

    public static void register() {
        DefaultItemComponentEvents.MODIFY.register(context -> {
            context.modify(ITEM, builder -> builder.set(DataComponents.ITEM_MODEL, Identifier.parse("minecraft:barrier")));
            context.modify(NORMAL_ITEM, builder -> builder.set(DataComponents.ITEM_MODEL, Identifier.parse("lbc:drop_boss")));
        });
    }

    public static boolean matches(ItemStack stack) {
        return isNormal(stack) || !stack.isEmpty()
                && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).matchedBy(TRIGGER);
    }

    public static boolean isNormal(ItemStack stack) {
        return !stack.isEmpty() && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).matchedBy(NORMAL_TRIGGER);
    }
}
