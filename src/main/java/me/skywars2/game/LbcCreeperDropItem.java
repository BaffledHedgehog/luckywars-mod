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

/** The two independent creeper special-drop flags, in source dispatch order. */
public final class LbcCreeperDropItem {
    private LbcCreeperDropItem() { }

    public enum Kind {
        NORMAL("skywars2:creeper_drop", "creepers", "trap_creepers"),
        OLD("skywars2:creeper_drop_old", "creepers_old", "block.minecraft.barrier");

        private final Identifier id;
        private final String flag, nameKey;
        private final CompoundTag trigger;
        private final Item item;

        Kind(String id, String flag, String nameKey) {
            this.id = Identifier.parse(id);
            this.flag = flag;
            this.nameKey = nameKey;
            this.trigger = new CompoundTag();
            trigger.putInt(flag, 1);
            trigger.putInt("drop", 1);
            var key = ResourceKey.create(Registries.ITEM, this.id);
            var properties = new Item.Properties().setId(key).overrideDescription(nameKey)
                    .component(DataComponents.CUSTOM_DATA, CustomData.of(trigger));
            if (flag.equals("creepers")) {
                properties.component(DataComponents.TOOLTIP_STYLE, Identifier.parse("lbc:tier_ff"))
                        .component(DataComponents.CUSTOM_NAME, Component.translatable("trap_creepers")
                                .withStyle(style -> style.withColor(ChatFormatting.YELLOW).withItalic(false)))
                        .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("trap_desc")
                                .withStyle(style -> style.withColor(ChatFormatting.WHITE).withItalic(false)))));
            }
            // The old source is an undecorated barrier, including its ordinary item name/model.
            item = Registry.register(BuiltInRegistries.ITEM, key,
                    new SimplePolymerItem(properties, flag.equals("creepers") ? Items.PAPER : Items.BARRIER, true));
        }

        public Item item() { return item; }
        public String flag() { return flag; }
        public Identifier id() { return id; }
        public String nameKey() { return nameKey; }
    }

    public static void register() {
        var normal = Kind.NORMAL.item();
        var old = Kind.OLD.item();
        DefaultItemComponentEvents.MODIFY.register(context -> {
            context.modify(normal, builder -> builder.set(DataComponents.ITEM_MODEL, Identifier.parse("lbc:drop_boss")));
            context.modify(old, builder -> builder.set(DataComponents.ITEM_MODEL, Identifier.parse("minecraft:barrier")));
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
