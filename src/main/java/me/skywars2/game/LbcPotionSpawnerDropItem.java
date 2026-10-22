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

/** The normal potion-spawner drop keeps its authored frame; the legacy token remains a plain barrier. */
public final class LbcPotionSpawnerDropItem {
    private LbcPotionSpawnerDropItem() { }

    public enum Kind {
        NORMAL("skywars2:potion_spawner_drop", "badspawner"),
        OLD("skywars2:potion_spawner_drop_old", "badspawner_old");

        private final Item item;
        private final String flag;
        private final CompoundTag trigger = new CompoundTag();

        Kind(String id, String flag) {
            this.flag = flag;
            trigger.putInt(flag, 1);
            trigger.putInt("drop", 1);
            var key = ResourceKey.create(Registries.ITEM, Identifier.parse(id));
            var properties = new Item.Properties().setId(key)
                    .overrideDescription(flag.equals("badspawner") ? "trap_potions" : "block.minecraft.barrier")
                    .component(DataComponents.CUSTOM_DATA, CustomData.of(trigger));
            if (flag.equals("badspawner")) {
                properties.component(DataComponents.TOOLTIP_STYLE, Identifier.parse("lbc:tier_ff"))
                        .component(DataComponents.CUSTOM_NAME, Component.translatable("trap_potions")
                                .withStyle(style -> style.withColor(ChatFormatting.YELLOW).withItalic(false)))
                        .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("trap_desc")
                                .withStyle(style -> style.withColor(ChatFormatting.WHITE).withItalic(false)))));
            }
            item = Registry.register(BuiltInRegistries.ITEM, key,
                    new SimplePolymerItem(properties, flag.equals("badspawner") ? Items.REPEATING_COMMAND_BLOCK : Items.BARRIER, true));
        }

        public Item item() { return item; }
        public String flag() { return flag; }
    }

    public static void register() {
        var kinds = Kind.values();
        DefaultItemComponentEvents.MODIFY.register(context -> {
            for (var kind : kinds) context.modify(kind.item(), builder -> builder.set(DataComponents.ITEM_MODEL,
                    Identifier.parse(kind == Kind.NORMAL ? "lbc:drop_trap" : "minecraft:barrier")));
        });
    }

    public static boolean matches(ItemStack stack, Kind kind) {
        return !stack.isEmpty() && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).matchedBy(kind.trigger);
    }

    public static boolean matches(ItemStack stack) {
        return kind(stack) != null;
    }

    public static Kind kind(ItemStack stack) {
        for (var kind : Kind.values()) if (matches(stack, kind)) return kind;
        return null;
    }
}
