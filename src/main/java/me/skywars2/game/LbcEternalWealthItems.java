package me.skywars2.game;

import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

/** Distinct server identities keep raw map mushrooms outside the Greedstone mechanic. */
public final class LbcEternalWealthItems {
    private static final CompoundTag DROP_TRIGGER=trigger("greedcurse",true),STONE_TRIGGER=trigger("greedstone",false);
    public static final Item DROP = drop(false);
    public static final Item IMBA_DROP = drop(true);
    public static final Item STONE = stone();
    private static final BlockState LEGACY_CURSED_STATE = Blocks.RED_MUSHROOM_BLOCK.defaultBlockState()
            .setValue(HugeMushroomBlock.NORTH, false).setValue(HugeMushroomBlock.SOUTH, false)
            .setValue(HugeMushroomBlock.EAST, false).setValue(HugeMushroomBlock.WEST, false)
            .setValue(HugeMushroomBlock.UP, false).setValue(HugeMushroomBlock.DOWN, false);
    public static final Block CURSED_BLOCK = cursedBlock();

    private LbcEternalWealthItems() { }
    public static void register() { }
    private static CompoundTag trigger(String key,boolean drop){var tag=new CompoundTag();tag.putInt(key,1);if(drop)tag.putInt("drop",1);return tag;}

    private static Item.Properties properties(String model) {
        return new Item.Properties() {
            @Override public Identifier effectiveModel() { return Identifier.parse(model); }
        }.component(DataComponents.ITEM_MODEL, Identifier.parse(model));
    }

    private static Item drop(boolean imba) {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse("skywars2:greedcurse_drop" + (imba ? "_imba" : "")));
        var flags = new CompoundTag();
        flags.putInt("greedcurse", 1);
        flags.putInt("drop", 1);
        if (imba) flags.putInt("imba", 1);
        var properties = properties(imba ? "minecraft:repeating_command_block" : "lbc:drop_not_trap")
                .setId(key).component(DataComponents.CUSTOM_DATA, CustomData.of(flags));
        // The original imba entry deliberately supplies no name, lore, tooltip style or custom model.
        if (!imba) properties.overrideDescription("drop_greed_curse")
                .component(DataComponents.CUSTOM_NAME, Component.translatable("drop_greed_curse")
                        .withStyle(style -> style.withColor(ChatFormatting.YELLOW).withItalic(false)))
                .component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("trap_desc")
                        .withStyle(style -> style.withColor(ChatFormatting.WHITE).withItalic(false)))))
                .component(DataComponents.TOOLTIP_STYLE, Identifier.parse("lbc:tier_q"));
        else properties.overrideDescription("block.minecraft.repeating_command_block");
        return Registry.register(BuiltInRegistries.ITEM, key,
                new SimplePolymerItem(properties, Items.REPEATING_COMMAND_BLOCK, true));
    }

    private static Item stone() {
        var key = ResourceKey.create(Registries.ITEM, Identifier.parse("skywars2:greedstone"));
        var flags = new CompoundTag();
        flags.putInt("greedstone", 1);
        flags.putInt("instantitem", 1);
        return Registry.register(BuiltInRegistries.ITEM, key, new SimplePolymerItem(
                properties("minecraft:nether_star").setId(key).overrideDescription("eternal_wealth")
                        .component(DataComponents.CUSTOM_DATA, CustomData.of(flags))
                        .component(DataComponents.CUSTOM_NAME, Component.translatable("eternal_wealth")
                                .withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(true).withItalic(false))),
                Items.NETHER_STAR, true));
    }

    public static boolean isDrop(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(DROP)||stack.is(IMBA_DROP)||stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).matchedBy(DROP_TRIGGER);
    }

    public static boolean isStone(ItemStack stack) {
        return !stack.isEmpty()&&(stack.is(STONE)||stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).matchedBy(STONE_TRIGGER));
    }

    private static Block cursedBlock() {
        var key = ResourceKey.create(Registries.BLOCK, Identifier.parse("skywars2:greedstone_cursed_block"));
        return Registry.register(BuiltInRegistries.BLOCK, key, new CursedBlock(
                BlockBehaviour.Properties.ofFullCopy(Blocks.RED_MUSHROOM_BLOCK).setId(key)));
    }

    private static final class CursedBlock extends Block implements PolymerBlock {
        CursedBlock(Properties properties) { super(properties); }
        @Override public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
            return LEGACY_CURSED_STATE;
        }
        @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            return LEGACY_CURSED_STATE.getDrops(params);
        }
        @Override protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
            return LEGACY_CURSED_STATE.getCloneItemStack(level, pos, includeData);
        }
    }
}
