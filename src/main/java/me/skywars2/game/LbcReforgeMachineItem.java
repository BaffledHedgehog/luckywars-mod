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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;

/** A retained crafting tool; the old command block was only its datapack carrier. */
public final class LbcReforgeMachineItem {
    public static final Item MACHINE = create();

    private LbcReforgeMachineItem() {}
    static void register() {
        // Native property finalization replaces ITEM_MODEL with the registry ID; apply the source model afterwards.
        DefaultItemComponentEvents.MODIFY.register(context -> context.modify(MACHINE,
                builder -> builder.set(DataComponents.ITEM_MODEL, Identifier.parse("lbc:reroll_machine"))));
    }

    private static Item create() {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse("skywars2:reroll_machine"));
        var flags = new CompoundTag();
        flags.putInt("reroll_machine", 1);
        flags.putInt("craftusable", 1);
        var properties = new Item.Properties().setId(key).overrideDescription("lbckit.24")
                .component(DataComponents.CUSTOM_DATA, CustomData.of(flags))
                .component(DataComponents.CUSTOM_NAME, Component.translatable("lbckit.24")
                        .withStyle(style -> style.withColor(ChatFormatting.WHITE).withItalic(false)))
                .component(DataComponents.LORE, new ItemLore(List.of(
                        Component.translatable("lbckit.24.lore.1")
                                .withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false)),
                        Component.translatable("lbckit.24.lore.2")
                                .withStyle(style -> style.withColor(ChatFormatting.GRAY).withItalic(false)))));
        // The existing item model supplies geometry; a paper proxy avoids block placement prediction.
        return Registry.register(BuiltInRegistries.ITEM, key, new SimplePolymerItem(properties, Items.PAPER, true));
    }
}
