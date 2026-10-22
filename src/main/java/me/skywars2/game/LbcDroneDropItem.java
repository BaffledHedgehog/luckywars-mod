package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/** Legacy drone token has only its source flags and ordinary barrier presentation. */
public final class LbcDroneDropItem {
    private static final CompoundTag TRIGGER = trigger();
    public static final Item ITEM = create();
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

    public static void register() {
        DefaultItemComponentEvents.MODIFY.register(context -> context.modify(ITEM,
                builder -> builder.set(DataComponents.ITEM_MODEL, Identifier.parse("minecraft:barrier"))));
    }

    public static boolean matches(ItemStack stack) {
        return !stack.isEmpty() && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).matchedBy(TRIGGER);
    }
}
