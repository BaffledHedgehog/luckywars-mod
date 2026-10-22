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

/** The encounter starts when this token is dropped; its command-block carrier was a datapack workaround. */
public final class LbcHelicopterTriggerItem {
    public static final Item TOKEN = create();
    private LbcHelicopterTriggerItem() { }

    static void register() {
        DefaultItemComponentEvents.MODIFY.register(context -> context.modify(TOKEN,
                builder -> builder.set(DataComponents.ITEM_MODEL, Identifier.parse("lbc:drop_boss"))));
    }

    private static Item create() {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse("skywars2:helicopter_encounter"));
        var flags = new CompoundTag();
        flags.putInt("helicopter_boss", 1);
        flags.putInt("drop", 1);
        var properties = new Item.Properties().setId(key).overrideDescription("drop_helicopter")
                .component(DataComponents.CUSTOM_DATA, CustomData.of(flags))
                .component(DataComponents.TOOLTIP_STYLE, Identifier.parse("lbc:tier_ff"))
                .component(DataComponents.CUSTOM_NAME, Component.translatable("drop_helicopter")
                        .withStyle(style -> style.withColor(ChatFormatting.YELLOW).withItalic(false)))
                .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("trap_desc")
                        .withStyle(style -> style.withColor(ChatFormatting.WHITE).withItalic(false)))));
        return Registry.register(BuiltInRegistries.ITEM, key, new SimplePolymerItem(properties, Items.PAPER, true));
    }
}
