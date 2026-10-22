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
import java.util.UUID;

/** Transferable mirrors bind to a home entity, never to the player carrying them. */
public final class LbcHomeItems {
    private static final CompoundTag DROP_FLAGS = dropFlags();
    private static final CompoundTag MIRROR_FLAGS = mirrorFlags();
    public static final Item DROP = createDrop();
    public static final Item MIRROR = createMirror();

    private LbcHomeItems() { }

    private static CompoundTag dropFlags() {
        var data = new CompoundTag();
        data.putInt("sethome", 1);
        data.putInt("drop", 1);
        return data;
    }

    private static CompoundTag mirrorFlags() {
        var data = new CompoundTag();
        data.putInt("magicwand", 1);
        data.putInt("supersethomer", 1);
        data.putInt("tpid", 0);
        return data;
    }

    private static Component text(String key, ChatFormatting color) {
        return Component.translatable(key).withStyle(style -> style.withColor(color).withItalic(false));
    }

    private static Item createDrop() {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse("skywars2:home_drop"));
        var properties = new Item.Properties().setId(key).overrideDescription("drop_sethome")
                .component(DataComponents.CUSTOM_DATA, CustomData.of(DROP_FLAGS))
                .component(DataComponents.CUSTOM_NAME, text("drop_sethome", ChatFormatting.YELLOW))
                .component(DataComponents.LORE, new ItemLore(List.of(text("trap_desc", ChatFormatting.WHITE))))
                .component(DataComponents.TOOLTIP_STYLE, Identifier.parse("lbc:tier_b"));
        return Registry.register(BuiltInRegistries.ITEM, key,
                new SimplePolymerItem(properties, Items.REPEATING_COMMAND_BLOCK, true));
    }

    private static Item createMirror() {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse("skywars2:magic_mirror"));
        var properties = new Item.Properties().setId(key).overrideDescription("magic_mirror").durability(25)
                .component(DataComponents.CUSTOM_DATA, CustomData.of(MIRROR_FLAGS))
                .component(DataComponents.CUSTOM_NAME, text("magic_mirror", ChatFormatting.AQUA))
                .component(DataComponents.LORE, new ItemLore(List.of(text("magic_mirror.lore.1", ChatFormatting.WHITE),
                        text("magic_mirror.lore.2", ChatFormatting.WHITE))))
                .component(DataComponents.TOOLTIP_STYLE, Identifier.parse("lbc:tier_d"));
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
            context.modify(DROP, builder -> builder.set(DataComponents.ITEM_MODEL, Identifier.parse("lbc:drop_not_trap")));
            context.modify(MIRROR, builder -> builder.set(DataComponents.ITEM_MODEL, Identifier.parse("lbc:sethomer")));
        });
    }

    public static boolean matchesDrop(ItemStack stack) {
        return !stack.isEmpty() && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).matchedBy(DROP_FLAGS);
    }

    public static boolean isMirror(ItemStack stack) {
        // tpid is a binding value in old stacks, not a required constant in the dispatch predicate.
        return !stack.isEmpty() && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getIntOr("supersethomer", 0) == 1;
    }

    public static ItemStack mirror(UUID markerId) {
        var stack = new ItemStack(MIRROR);
        var data = MIRROR_FLAGS.copy();
        data.putString("skywars2_home", markerId.toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return stack;
    }
}
