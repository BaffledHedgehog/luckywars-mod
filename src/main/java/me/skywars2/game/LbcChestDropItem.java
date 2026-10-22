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
import net.minecraft.world.level.storage.loot.LootTable;
import java.util.List;

/** Native dropped tokens; PAPER is only the Polymer carrier for the source model. */
public final class LbcChestDropItem {
    private LbcChestDropItem() { }

    /** Declaration order is the legacy specialdrops dispatch order. */
    public enum Kind {
        BASTION("skywars2:chest_drop_bastion", "bastion", "drop_bastion", "minecraft:chests/bastion_treasure"),
        END_CITY("skywars2:chest_drop_end_city", "end_city", "drop_end_city", "minecraft:chests/end_city_treasure"),
        TREASURE("skywars2:chest_drop_treasure", "treasure", "drop_treasure", "minecraft:chests/buried_treasure"),
        VILLAGE("skywars2:chest_drop_village", "village", "drop_village", "minecraft:chests/village/village_weaponsmith"),
        BONUS("skywars2:chest_drop_bonus", "bonus_chest", "drop_bonus", "minecraft:chests/spawn_bonus_chest");

        private final Identifier id;
        private final String flag, nameKey;
        private final ResourceKey<LootTable> table;
        private final CompoundTag trigger;
        private final Item item;

        Kind(String id, String flag, String nameKey, String table) {
            this.id = Identifier.parse(id);
            this.flag = flag;
            this.nameKey = nameKey;
            this.table = ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(table));
            this.trigger = new CompoundTag();
            trigger.putInt(flag, 1);
            trigger.putInt("drop", 1);
            var key = ResourceKey.create(Registries.ITEM, this.id);
            var properties = new Item.Properties().setId(key).overrideDescription(nameKey)
                    .component(DataComponents.CUSTOM_DATA, CustomData.of(trigger))
                    .component(DataComponents.TOOLTIP_STYLE, Identifier.parse("lbc:tier_d"))
                    .component(DataComponents.CUSTOM_NAME, Component.translatable(nameKey)
                            .withStyle(style -> style.withColor(ChatFormatting.YELLOW).withItalic(false)))
                    .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("trap_desc")
                            .withStyle(style -> style.withColor(ChatFormatting.WHITE).withItalic(false)))));
            this.item = Registry.register(BuiltInRegistries.ITEM, key, new SimplePolymerItem(properties, Items.PAPER, true));
        }

        public Item item() { return item; }
        public String flag() { return flag; }
        public String nameKey() { return nameKey; }
        public Identifier id() { return id; }
        public ResourceKey<LootTable> table() { return table; }
    }

    public static void register() {
        // Item properties finalize ITEM_MODEL after construction; apply the source model afterwards.
        var kinds = Kind.values();
        DefaultItemComponentEvents.MODIFY.register(context -> {
            for (var kind : kinds) context.modify(kind.item(),
                    builder -> builder.set(DataComponents.ITEM_MODEL, Identifier.parse("lbc:drop_not_trap")));
        });
    }

    static boolean matches(ItemStack stack, Kind kind) {
        return !stack.isEmpty() && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).matchedBy(kind.trigger);
    }

    static Kind kind(ItemStack stack) {
        for (var kind : Kind.values()) if (matches(stack, kind)) return kind;
        return null;
    }
}
