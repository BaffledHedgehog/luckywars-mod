package me.skywars2.game;

import java.util.List;

import me.skywars2.game.world.maps.MapManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;

/** An occupied native cell with a distinguishable inventory that belongs to one check. */
final class ContainerReplacementChecks implements AutoCloseable {
    private final GameController game;
    private final ServerLevel level;
    private final BlockPos position;
    private final MapManager.BlockSnapshot before;
    private final ChestBlockEntity chest;
    private final ItemStack expected;

    ContainerReplacementChecks(GameController game, ServerLevel level, BlockPos position, String label) {
        this.game = game;
        this.level = level;
        this.position = position.immutable();
        before = game.maps().snapshot(level, position);
        level.setBlock(position, Blocks.CHEST.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
        if (!(level.getBlockEntity(position) instanceof ChestBlockEntity nativeChest))
            throw new IllegalStateException("No native chest for " + label + " at " + position);
        chest = nativeChest;
        expected = new ItemStack(Items.PRISMARINE_CRYSTALS, 13);
        expected.set(DataComponents.CUSTOM_NAME, Component.literal(label));
        chest.setItem(0, expected.copy());
    }

    private List<ItemEntity> released() {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(position).inflate(1),
                item -> ItemStack.isSameItemSameComponents(item.getItem(), expected));
    }

    void requireReleased() {
        var drops = released();
        int count = drops.stream().mapToInt(item -> item.getItem().getCount()).sum();
        if (!chest.isRemoved() || drops.size() != 1 || count != expected.getCount())
            throw new IllegalStateException("Container inventory after " + expected.getHoverName().getString()
                    + " at " + position + " (removed=" + chest.isRemoved() + ", bodies=" + drops.size() + ", count=" + count + ")");
    }

    @Override
    public void close() {
        released().forEach(Entity::discard);
        game.maps().restoreSnapshot(level, position, before);
    }
}
