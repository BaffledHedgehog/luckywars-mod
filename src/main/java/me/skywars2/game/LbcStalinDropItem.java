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

/** The normal loli acquisition; no broad pool or variant is enabled here. */
public final class LbcStalinDropItem {
    private static final CompoundTag FLAGS=flags();
    public static final Item ITEM=create();
    private LbcStalinDropItem() { }
    private static CompoundTag flags() {
        var data=new CompoundTag();data.putInt("stalin",1);data.putInt("drop",1);return data;
    }
    private static Item create() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:stalin_drop"));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(
                new Item.Properties().setId(key).overrideDescription("drop_stalin")
                        .component(DataComponents.CUSTOM_DATA,CustomData.of(FLAGS))
                        .component(DataComponents.TOOLTIP_STYLE,Identifier.parse("lbc:tier_ff"))
                        .component(DataComponents.CUSTOM_NAME,Component.translatable("drop_stalin")
                                .withStyle(style->style.withColor(ChatFormatting.YELLOW).withItalic(false)))
                        .component(DataComponents.LORE,new ItemLore(List.of(Component.translatable("trap_desc")
                                .withStyle(style->style.withColor(ChatFormatting.WHITE).withItalic(false))))),
                Items.REPEATING_COMMAND_BLOCK,true));
    }
    public static void register() {
        DefaultItemComponentEvents.MODIFY.register(context->context.modify(ITEM,
                builder->builder.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:drop_boss"))));
    }
    public static boolean isTrigger(ItemStack stack) {
        return !stack.isEmpty() && stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).matchedBy(FLAGS);
    }
}
