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

/** Native token preserving the source Lucky pigs drop presentation. */
public final class LbcLuckyPigDropItem {
    private static final CompoundTag TRIGGER = trigger();
    public static final Item ITEM = create();

    private LbcLuckyPigDropItem() { }

    public static void register() {
        DefaultItemComponentEvents.MODIFY.register(context -> context.modify(ITEM,
                builder -> builder.set(DataComponents.ITEM_MODEL, Identifier.parse("lbc:drop_not_trap"))));
    }

    private static CompoundTag trigger() {
        var flags = new CompoundTag();
        flags.putInt("applepig", 1);
        flags.putInt("drop", 1);
        return flags;
    }

    private static Item create() {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse("skywars2:lucky_pigs_drop"));
        var properties = new Item.Properties().setId(key).overrideDescription("drop_apple_pigs")
                .component(DataComponents.CUSTOM_DATA, CustomData.of(TRIGGER))
                .component(DataComponents.TOOLTIP_STYLE, Identifier.parse("lbc:tier_c"))
                .component(DataComponents.CUSTOM_NAME, Component.translatable("drop_apple_pigs")
                        .withStyle(style -> style.withColor(ChatFormatting.YELLOW).withItalic(false)))
                .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("trap_desc")
                        .withStyle(style -> style.withColor(ChatFormatting.WHITE).withItalic(false)))));
        return Registry.register(BuiltInRegistries.ITEM, key,
                new SimplePolymerItem(properties, Items.REPEATING_COMMAND_BLOCK, true));
    }

    public static boolean matches(ItemStack stack) {
        return !stack.isEmpty() && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).matchedBy(TRIGGER);
    }
}
