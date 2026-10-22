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

/** Source ant and acid-burst tokens retain their distinct authored item models. */
public final class LbcAntDropItem {
    private LbcAntDropItem() { }

    public enum Kind {
        ACID_BURST("skywars2:acid_burst_drop", "acidboom"),
        ANTS("skywars2:ant_swarm_drop", "muravei");

        private final Item item;
        private final String flag;
        private final CompoundTag trigger = new CompoundTag();

        Kind(String id, String flag) {
            this.flag = flag;
            trigger.putInt(flag, 1);
            trigger.putInt("drop", 1);
            var key = ResourceKey.create(Registries.ITEM, Identifier.parse(id));
            var properties = new Item.Properties().setId(key)
                    .overrideDescription(flag.equals("acidboom") ? "drop_acid" : "drop_muravei")
                    .component(DataComponents.CUSTOM_DATA, CustomData.of(trigger));
            {
                properties.component(DataComponents.CUSTOM_NAME, Component.translatable(flag.equals("acidboom") ? "drop_acid" : "drop_muravei")
                                .withStyle(style -> style.withColor(ChatFormatting.YELLOW).withItalic(false)))
                        .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("trap_desc")
                                .withStyle(style -> style.withColor(ChatFormatting.WHITE).withItalic(false)))));
            }
            properties.component(DataComponents.TOOLTIP_STYLE, Identifier.parse("lbc:tier_ff"));
            item = Registry.register(BuiltInRegistries.ITEM, key,
                    new SimplePolymerItem(properties, Items.REPEATING_COMMAND_BLOCK, true));
        }

        public Item item() { return item; }
        public String flag() { return flag; }
    }

    public static void register() {
        var kinds = Kind.values();
        DefaultItemComponentEvents.MODIFY.register(context -> {
            for (var kind : kinds) context.modify(kind.item(), builder -> builder.set(DataComponents.ITEM_MODEL,
                    Identifier.parse(kind == Kind.ACID_BURST ? "lbc:muravei" : "lbc:drop_boss")));
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
