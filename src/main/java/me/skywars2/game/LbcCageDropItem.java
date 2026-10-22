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

/** Five source cage flags, in specialdrops dispatch order. */
public final class LbcCageDropItem {
    private LbcCageDropItem() { }

    public enum Kind {
        ANVIL("skywars2:cage_drop_anvil", "lookupanvil", "trap_anvil"),
        FIREBALL("skywars2:cage_drop_fireball", "lookupfireball", "trap_fireball"),
        POTIONS("skywars2:cage_drop_potions", "lookuppotions", "trap_potion"),
        LAVA("skywars2:cage_drop_lava", "lookuplava", "trap_lava"),
        OBSIDIAN("skywars2:cage_drop_obsidian", "obsidiantrap", "trap_obsidian");

        private final Identifier id;
        private final String flag, nameKey;
        private final CompoundTag trigger;
        private final Item item;

        Kind(String id, String flag, String nameKey) {
            this.id = Identifier.parse(id);
            this.flag = flag;
            this.nameKey = nameKey;
            trigger = new CompoundTag();
            trigger.putInt(flag, 1);
            trigger.putInt("drop", 1);
            var key = ResourceKey.create(Registries.ITEM, this.id);
            var properties = new Item.Properties().setId(key).overrideDescription(nameKey)
                    .component(DataComponents.CUSTOM_DATA, CustomData.of(trigger))
                    .component(DataComponents.TOOLTIP_STYLE, Identifier.parse("lbc:tier_ff"))
                    .component(DataComponents.CUSTOM_NAME, Component.translatable(nameKey)
                            .withStyle(style -> style.withColor(ChatFormatting.YELLOW).withItalic(false)))
                    .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("trap_desc")
                            .withStyle(style -> style.withColor(ChatFormatting.WHITE).withItalic(false)))));
            item = Registry.register(BuiltInRegistries.ITEM, key,
                    new SimplePolymerItem(properties, Items.REPEATING_COMMAND_BLOCK, true));
        }

        public Item item() { return item; }
        public Identifier id() { return id; }
        public String flag() { return flag; }
        public String nameKey() { return nameKey; }
    }

    public static void register() {
        var kinds = Kind.values();
        DefaultItemComponentEvents.MODIFY.register(context -> {
            for (var kind : kinds) context.modify(kind.item(),
                    builder -> builder.set(DataComponents.ITEM_MODEL, Identifier.parse("lbc:drop_trap")));
        });
    }

    public static boolean matches(ItemStack stack, Kind kind) {
        return !stack.isEmpty() && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).matchedBy(kind.trigger);
    }

    public static Kind kind(ItemStack stack) {
        for (var kind : Kind.values()) if (matches(stack, kind)) return kind;
        return null;
    }
}
