package me.skywars2.game;

import java.nio.file.Files;
import java.nio.file.Path;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;

/** Two separate fixture boots: old-height native save, offline converter, then new-height native reload. */
public final class NativeOverworldHeightChecks {
    private static final BlockPos CHEST = new BlockPos(5_000_008, 96, 5_000_008);
    private static final BlockPos NEGATIVE = new BlockPos(5_000_008, -16, 5_000_008);
    private static final BlockPos ROOF = new BlockPos(5_000_008, 319, 5_000_008);
    private NativeOverworldHeightChecks() {}
    private static void require(boolean ok, String why) { if (!ok) throw new IllegalStateException("Overworld height: " + why); }
    private static Path receipt() {
        var configured = System.getProperty("skywars2.check.height.receipt");
        require(configured != null && !configured.isBlank(), "explicit receipt path for the isolated two-boot fixture");
        return Path.of(configured).toAbsolutePath().normalize();
    }

    /** Only for a disposable COPY booted with the old -64 dimension definition. */
    public static void prepare(GameController game) throws Exception {
        require(Boolean.getBoolean("skywars2.check.height.prepare"), "explicit isolated-fixture preparation flag");
        var level = game.server().overworld();
        require(level.getMinY() == -64 && level.getMaxY() == 319, "base boot really uses the original native -64..319 height");
        var target = receipt(); require(!Files.exists(target), "preparation owns a new expected-NBT receipt");
        level.getChunk(CHEST.getX() >> 4, CHEST.getZ() >> 4);
        require(level.setBlock(CHEST, Blocks.CHEST.defaultBlockState(), 2 | 16 | 32)
                && level.getBlockEntity(CHEST) instanceof ChestBlockEntity, "real positive-height native chest");
        var chest = (ChestBlockEntity)level.getBlockEntity(CHEST);
        chest.clearContent();
        var diamonds = new ItemStack(Items.DIAMOND, 13);
        diamonds.set(DataComponents.CUSTOM_NAME, Component.literal("Keep this exact component"));
        chest.setItem(4, diamonds); chest.setItem(22, new ItemStack(Items.EMERALD, 29)); chest.setChanged();
        require(level.setBlock(NEGATIVE, Blocks.CHEST.defaultBlockState(), 2 | 16 | 32), "real below-zero native container");
        var negative = (ChestBlockEntity)level.getBlockEntity(NEGATIVE);
        negative.setItem(0, new ItemStack(Items.NETHERITE_INGOT, 7)); negative.setChanged();
        level.setBlock(ROOF, Blocks.GOLD_BLOCK.defaultBlockState(), 2 | 16 | 32);
        require(level.getHeight(Heightmap.Types.WORLD_SURFACE, ROOF.getX(), ROOF.getZ()) == 320,
                "native old-height map records the positive roof's first-air height");
        Files.createDirectories(target.getParent());
        NbtIo.writeCompressed(chest.saveWithFullMetadata(level.registryAccess()), target);
        level.save(null, true, false);
        SkyWars2.LOGGER.info("[check] PASS overworld-height-prepare (actual -64 native world, positive chest/components and roof319, negative chest; flushed native region)");
    }

    /** Read-only against the same fixture copy AFTER offline crop and new-height boot. */
    public static void run(GameController game) throws Exception {
        var level = game.server().overworld();
        require(level.getMinY() == 0 && level.getMaxY() == 319 && level.getHeight() == 320
                && level.getMinSectionY() == 0 && level.getMaxSectionY() == 19,
                "actual native Overworld build bounds and section indices are0..319");
        require(level.isOutsideBuildHeight(-1) && level.isInsideBuildHeight(0)
                && level.isInsideBuildHeight(319) && level.isOutsideBuildHeight(320), "native floor/ceiling predicates use cropped height");
        var expected = NbtIo.readCompressed(receipt(), NbtAccounter.unlimitedHeap());
        level.getChunk(CHEST.getX() >> 4, CHEST.getZ() >> 4);
        require(level.getBlockState(CHEST).is(Blocks.CHEST) && level.getBlockEntity(CHEST) instanceof ChestBlockEntity,
                "converter output really loads the persisted positive native chest, without constructing it in this check");
        var chest = (ChestBlockEntity)level.getBlockEntity(CHEST);
        require(expected.equals(chest.saveWithFullMetadata(level.registryAccess())),
                "full native container NBT equals the pre-conversion native receipt after actual region reload");
        require(chest.getItem(4).is(Items.DIAMOND) && chest.getItem(4).getCount() == 13
                && chest.getItem(22).is(Items.EMERALD) && chest.getItem(22).getCount() == 29
                && chest.getItem(4).getHoverName().getString().equals("Keep this exact component"),
                "real positive container item counts and custom components survive");
        require(level.getBlockState(ROOF).is(Blocks.GOLD_BLOCK)
                && level.getHeight(Heightmap.Types.WORLD_SURFACE, ROOF.getX(), ROOF.getZ()) == 320,
                "native top positive section and repacked heightmap survive conversion");
        require(level.isOutsideBuildHeight(NEGATIVE) && level.getBlockEntity(NEGATIVE) == null,
                "negative container is outside the native cropped world");
        SkyWars2.LOGGER.info("[check] PASS overworld-height (actual min0/max319, offline-converted native region, complete positive chest NBT/items/components, top section and heightmap)");
    }
}
