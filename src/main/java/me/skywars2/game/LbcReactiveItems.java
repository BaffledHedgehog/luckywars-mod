package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import me.skywars2.SkyWars2;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;

import java.util.List;

/** The scoped Reactive Shish and automatic Shish Wall share normal wand admission. */
public final class LbcReactiveItems {
    public static final Item REACTIVE = create("skywars2:reactive_shish", "reactive_shish", "lbc:tier_c",
            flags("craftusable", "magicwand", "reactive_shish", "spellwithcooldown"), List.of(
                    text("reactive_shish.lore", ChatFormatting.GREEN),
                    text("reactive_shish.lore.1", ChatFormatting.GREEN),
                    text("craft_with_upgraded_wall_wand.1", ChatFormatting.GOLD),
                    text("craft_with_upgraded_wall_wand.2", ChatFormatting.GOLD)));
    public static final Item WALL = create("skywars2:shish_wall_wand", "shish_wall_wand", "lbc:tier_b",
            flags("craftusable", "transformable", "staff", "magicwand", "shish_wall_wand"), List.of(
                    text("type", ChatFormatting.AQUA).append(text("staff", ChatFormatting.AQUA)),
                    Component.literal("500").withStyle(style -> style.withColor(ChatFormatting.AQUA).withItalic(false))
                            .append(text("mana_on_activate", ChatFormatting.AQUA)),
                    text("shish_wall_wand.lore.1", ChatFormatting.WHITE),
                    text("shish_wall_wand.lore.2", ChatFormatting.WHITE),
                    text("shish_wall_wand.lore.3", ChatFormatting.WHITE),
                    text("shish_wall_wand.lore.4", ChatFormatting.WHITE),
                    text("shish_wall_wand.lore.5", ChatFormatting.WHITE),
                    text("shish_wall_wand.lore.6", ChatFormatting.GRAY)));

    private LbcReactiveItems() { }

    private static net.minecraft.network.chat.MutableComponent text(String key, ChatFormatting color) {
        return Component.translatable(key).withStyle(style -> style.withColor(color).withItalic(false));
    }

    private static CompoundTag flags(String... names) {
        var tag = new CompoundTag();
        for (var name : names) tag.putInt(name, 1);
        return tag;
    }

    private static Item create(String id, String name, String tier, CompoundTag flags, List<Component> lore) {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse(id));
        var properties = new Item.Properties().setId(key).durability(25).overrideDescription(name)
                .component(DataComponents.CUSTOM_DATA, CustomData.of(flags))
                .component(DataComponents.CUSTOM_NAME, text(name, ChatFormatting.DARK_GREEN))
                .component(DataComponents.LORE, new ItemLore(lore))
                .component(DataComponents.TOOLTIP_STYLE, Identifier.parse(tier));
        return Registry.register(BuiltInRegistries.ITEM, key,
                new SimplePolymerItem(properties, Items.CARROT_ON_A_STICK, true) {
                    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
                        var game = SkyWars2.game();
                        return game != null && player instanceof ServerPlayer owner && game.items().wands().use(owner, hand)
                                ? InteractionResult.SUCCESS : InteractionResult.FAIL;
                    }
                });
    }

    public static void register() {
        DefaultItemComponentEvents.MODIFY.register(context -> {
            context.modify(REACTIVE, builder -> builder.set(DataComponents.ITEM_MODEL, Identifier.parse("minecraft:spruce_sapling")));
            context.modify(WALL, builder -> builder.set(DataComponents.ITEM_MODEL, Identifier.parse("lbc:shish_wall_wand")));
        });
    }

    public static boolean isReactive(ItemStack stack) {
        return !stack.isEmpty() && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getIntOr("reactive_shish", 0) == 1;
    }

    public static boolean isWall(ItemStack stack) {
        return !stack.isEmpty() && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getIntOr("shish_wall_wand", 0) == 1;
    }
}
