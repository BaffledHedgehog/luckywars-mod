package me.skywars2.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.SavedTick;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;

/** Real native palettes, block entities, schedulers, lighting executor and viewer packets. */
public final class VanityTerrainChecks {
    private VanityTerrainChecks() { }
    private static Fixture running;
    private static void require(boolean value, String why) {
        if (!value) throw new IllegalStateException("Vanity terrain: " + why);
    }

    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final ServerLevel level;
        final BlockPos origin, chest, removedChest, addedChest, light, fluid, fire, cave, blockTick, fluidTick, futureTick, longTick, sign;
        final BoundingBox box;
        final List<LevelChunk> chunks = new ArrayList<>();
        final List<ChunkPos> forced = new ArrayList<>();
        final Map<BlockPos, MapManager.BlockSnapshot> originals = new LinkedHashMap<>();
        final List<ScheduledTick<Block>> oldBlocks = new ArrayList<>();
        final List<ScheduledTick<Fluid>> oldFluids = new ArrayList<>();
        final Map<LevelChunk, LevelChunkSection[]> oldSections = new LinkedHashMap<>();
        final boolean frozen;
        final net.minecraft.server.level.ServerPlayer viewer;
        final ServerGamePacketListenerImpl connection;
        final ChunkTrackingView tracking;
        final ElementalGrimoireChecks.Recording recording;
        LbcIndirectKills.TerrainProvenance provenance;
        MatchTerrainCheckpoint checkpoint;
        MatchTerrainCheckpoint.Restore restore;
        int phase, ticks, verifyAfterTick = -1;
        long clock, firstVisited;
        boolean initialized, closed;

        Fixture(GameController game) {
            this.game = game; level = game.maps().level();
            require(game.playing() && !game.maps().busy(), "actual ready playing match");
            viewer = game.server().getPlayerList().getPlayers().getFirst();
            require(viewer.level() == level, "real viewer in map world");
            origin = new BlockPos((viewer.chunkPosition().x() << 4) + 15, level.getMaxY() - 24,
                    (viewer.chunkPosition().z() << 4) + 6);
            chest = origin; removedChest = origin.south(2); addedChest = origin.south(4);
            light = origin.east(); fluid = origin.east(2); fire = origin.east(3); cave = origin.east(4);
            blockTick = origin.south(6); fluidTick = origin.east(2).south(6); futureTick = origin.east(4).south(6);
            longTick = origin.south(7);
            sign = origin.east(6);
            var first = ChunkPos.containing(origin); var second = ChunkPos.containing(origin.east());
            box = new BoundingBox(first.x() << 4, level.getMinY(), first.z() << 4,
                    (second.x() << 4) + 15, level.getMaxY(), (second.z() << 4) + 15);
            frozen = game.server().tickRateManager().isFrozen();
            connection = viewer.connection; tracking = viewer.getChunkTrackingView();
            recording = new ElementalGrimoireChecks.Recording(viewer);
        }

        void init() {
            require(!frozen, "fixture owns only its native freeze");
            game.server().tickRateManager().setFrozen(true);
            for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) {
                var chunk = level.getChunk(x, box.minZ() >> 4); chunks.add(chunk);
                if (level.setChunkForced(x, box.minZ() >> 4, true)) forced.add(chunk.getPos());
                var sections = new LevelChunkSection[chunk.getSectionsCount()];
                for (int i = 0; i < sections.length; i++) sections[i] = chunk.getSection(i).copy();
                oldSections.put(chunk, sections);
                oldBlocks.addAll(((LevelChunkTicks<Block>) chunk.getBlockTicks()).getAll().toList());
                oldFluids.addAll(((LevelChunkTicks<Fluid>) chunk.getFluidTicks()).getAll().toList());
            }
            for (var pos : List.of(chest, removedChest, addedChest, light, fluid, fire, cave, blockTick, fluidTick, futureTick, longTick, sign))
                originals.put(pos, game.maps().snapshot(level, pos));
            provenance = game.items().indirectKills().captureTerrain(level, originals.keySet());
            initialized = true;
            viewer.connection = new ServerGamePacketListenerImpl(game.server(), recording, viewer,
                    new CommonListenerCookie(viewer.getGameProfile(), 0, viewer.clientInformation(), false));
            viewer.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            viewer.setChunkTrackingView(ChunkTrackingView.of(viewer.chunkPosition(), 8));
            require(chunks.stream().allMatch(c -> level.getChunkSource().chunkMap.getPlayers(c.getPos(), false).contains(viewer)),
                    "native ChunkMap selects actual connected viewer for both edited chunks");
            level.getBlockTicks().clearArea(box); level.getFluidTicks().clearArea(box);
            write(chest, Blocks.CHEST); write(removedChest, Blocks.CHEST); write(addedChest, Blocks.AIR);
            write(light, Blocks.GLOWSTONE); write(fluid, Blocks.LAVA); write(fire, Blocks.FIRE); write(cave, Blocks.CAVE_AIR);
            write(blockTick, Blocks.STONE); write(fluidTick, Blocks.WATER); write(futureTick, Blocks.STONE);
            write(longTick, Blocks.STONE);
            write(sign, Blocks.OAK_SIGN); sign("Saved text");
            var item = new ItemStack(Items.DIAMOND, 3);
            item.set(DataComponents.CUSTOM_NAME, Component.literal("Checkpoint payload"));
            chest(chest).setItem(7, item); chest(removedChest).setItem(11, new ItemStack(Items.EMERALD, 9));
            clock = level.getGameTime();
            level.getBlockTicks().schedule(new ScheduledTick<>(Blocks.STONE, blockTick, clock + 137, TickPriority.HIGH, 774411));
            level.getFluidTicks().schedule(new ScheduledTick<>(Fluids.WATER, fluidTick, clock + 83, TickPriority.LOW, 774412));
            level.getBlockTicks().schedule(new ScheduledTick<>(Blocks.STONE, longTick, clock + (1L << 33), TickPriority.VERY_LOW, -774414));
            checkpoint = new MatchTerrainCheckpoint(game.server());
            chunks.forEach(checkpoint::include); checkpoint.include(chunks.getFirst());
            require(checkpoint.chunks() == 2, "duplicate chunk admission keeps the FIRST immutable image");
            var pending = new LevelChunkTicks<Block>(List.of(new SavedTick<>(Blocks.STONE, blockTick, 61, TickPriority.VERY_HIGH)));
            var captured = MatchTerrainCheckpoint.captureTicks(pending, clock);
            require(pending.getAll().findAny().isEmpty() && pending.count() == 1 && captured.size() == 1
                    && captured.getFirst().triggerTick() == clock + 61 && captured.getFirst().subTickOrder() == -1,
                    "pending native queues captured without unpacking/mutating the source");
            mutate();
            boolean rejected = false;
            game.server().tickRateManager().setFrozen(false);
            try { checkpoint.restore(game); } catch (IllegalStateException expected) { rejected = true; }
            finally { game.server().tickRateManager().setFrozen(true); }
            require(rejected && chest(chest).getItem(7).is(Items.DIRT), "live restore rejected before ANY writes");
            restore = checkpoint.restore(game);
            rejected = false;
            try { checkpoint.restore(game); } catch (IllegalStateException expected) { rejected = true; }
            require(rejected, "overlapping restore rejected");
            require(!restore.step(1) && restore.visited() <= 1, "one-cell budget also bounds unchanged-cell examination");
            recording.packets.clear();
        }

        ChestBlockEntity chest(BlockPos pos) { return (ChestBlockEntity) level.getBlockEntity(pos); }
        void sign(String text) {
            ((SignBlockEntity) level.getBlockEntity(sign)).setText(SignText.EMPTY.asMutable()
                    .setLine(0, Component.literal(text)).asImmutable(), SignTextSlot.FRONT);
        }
        void write(BlockPos pos, Block block) {
            game.maps().restoreSnapshot(level, pos, new MapManager.BlockSnapshot(block.defaultBlockState(), null),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS
                            | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS | Block.UPDATE_SKIP_ON_PLACE);
        }
        void mutate() {
            chest(chest).setItem(7, new ItemStack(Items.DIRT, 64));
            write(removedChest, Blocks.STONE); write(addedChest, Blocks.CHEST);
            chest(addedChest).setItem(1, new ItemStack(Items.NETHER_STAR));
            write(light, Blocks.STONE); write(fluid, Blocks.AIR); write(fire, Blocks.AIR); write(cave, Blocks.AIR);
            sign("Future text");
            level.getBlockTicks().clearArea(box); level.getFluidTicks().clearArea(box);
            level.getBlockTicks().schedule(new ScheduledTick<>(Blocks.STONE, futureTick, clock + 500, TickPriority.NORMAL, 774413));
            var desert = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.DESERT);
            var chunk = chunks.getLast(); int i = chunk.getSectionIndex(origin.getY());
            chunk.getSection(i).fillBiomesFromNoise((x, y, z) -> desert, chunk.getPos().x() * 4,
                    chunk.getSectionYFromSectionIndex(i) * 4, chunk.getPos().z() * 4);
        }

        void verify() {
            require(chest(chest).getItem(7).is(Items.DIAMOND) && chest(chest).getItem(7).getCount() == 3
                    && chest(chest).getItem(7).getHoverName().getString().equals("Checkpoint payload"),
                    "same-state chest restores exact contents and components");
            require(chest(removedChest).getItem(11).is(Items.EMERALD) && chest(removedChest).getItem(11).getCount() == 9,
                    "destroyed native chest recreated with original payload");
            require(level.getBlockState(addedChest).isAir() && level.getBlockEntity(addedChest) == null,
                    "future chest and its contents removed without leaving a block entity");
            require(level.getBlockState(fluid).is(Blocks.LAVA) && level.getBlockState(fire).is(Blocks.FIRE)
                    && level.getBlockState(cave).is(Blocks.CAVE_AIR), "unsupported lava/fire and exact air variant survive neighbour-free restore");
            var bt = ((LevelChunkTicks<Block>) chunks.getFirst().getBlockTicks()).getAll().filter(t -> t.pos().equals(blockTick)).findFirst().orElseThrow();
            var ft = ((LevelChunkTicks<Fluid>) chunks.getLast().getFluidTicks()).getAll().filter(t -> t.pos().equals(fluidTick)).findFirst().orElseThrow();
            require(bt.triggerTick() == level.getGameTime() + 137 && bt.priority() == TickPriority.HIGH && bt.subTickOrder() == 774411
                    && ft.triggerTick() == level.getGameTime() + 83 && ft.priority() == TickPriority.LOW && ft.subTickOrder() == 774412,
                    "native block/fluid delays, priority and exact sub-tick ordering restored");
            require(!level.getBlockTicks().hasScheduledTick(futureTick, Blocks.STONE), "future scheduler work erased");
            var lt = ((LevelChunkTicks<Block>) chunks.getFirst().getBlockTicks()).getAll().filter(t -> t.pos().equals(longTick)).findFirst().orElseThrow();
            require(lt.triggerTick() == level.getGameTime() + (1L << 33) && lt.subTickOrder() == -774414
                    && lt.priority() == TickPriority.VERY_LOW, "long scheduler delays never truncate through int SavedTick serialization");
            for (var chunk : chunks) {
                int i = chunk.getSectionIndex(origin.getY());
                var original = oldSections.get(chunk)[i];
                require(chunk.getSection(i).getNoiseBiome(0, 0, 0).equals(original.getNoiseBiome(0, 0, 0)), "native biome palette restored");
            }
            require(level.getBrightness(LightLayer.BLOCK, light) == 15 && recording.packets.stream().anyMatch(ClientboundLightUpdatePacket.class::isInstance),
                    "native lighting propagation completes before final viewer packet publication");
            require(((SignBlockEntity) level.getBlockEntity(sign)).getText(SignTextSlot.FRONT).getMessages(false).getFirst().getString().equals("Saved text")
                    && recording.packets.stream().anyMatch(p -> p instanceof ClientboundBlockEntityDataPacket data
                    && data.getPos().equals(sign) && data.getTag().toString().contains("Saved text")),
                    "same-state sign text restored on server AND published by native block-entity packet");
            require(restore.changed() >= 6 && restore.visited() > 1, "real cross-chunk write and scan work occurred");
        }

        boolean step() {
            require(++ticks < 240, "bounded native restore, phase=" + phase);
            if (phase == 2) {
                if (level.getGameTime() - clock < 4) return false;
                game.server().tickRateManager().setFrozen(true);
                mutate(); restore = checkpoint.restore(game); recording.packets.clear(); phase = 3;
            }
            if (!restore.step(16384)) return false;
            // ChunkHolder broadcasts queued same-state block-entity changes on the
            // next native frame; do not replace that shared publisher in production.
            if (verifyAfterTick < 0) { verifyAfterTick = game.server().getTickCount(); return false; }
            if (verifyAfterTick == game.server().getTickCount()) return false;
            verifyAfterTick = -1;
            verify();
            if (phase == 0) {
                firstVisited = restore.visited(); restore.close(); mutate();
                restore = checkpoint.restore(game); recording.packets.clear(); phase = 1; return false;
            }
            require(restore.visited() == firstVisited, "second rewind reads the same immutable native palettes");
            if (phase == 1) {
                restore.close(); game.server().tickRateManager().setFrozen(false); phase = 2; return false;
            }
            require(level.getGameTime() - clock >= 4, "third rewind rebases delays after actual native world ticks, not a mocked clock");
            return true;
        }

        @Override public void close() {
            if (closed) return; closed = true;
            try {
                if (restore != null) restore.close();
                if (initialized) {
                    for (var entry : originals.entrySet()) game.maps().restoreSnapshot(level, entry.getKey(), entry.getValue());
                    level.getBlockTicks().clearArea(box); level.getFluidTicks().clearArea(box);
                    oldBlocks.forEach(level.getBlockTicks()::schedule); oldFluids.forEach(level.getFluidTicks()::schedule);
                    for (var chunk : chunks) {
                        var sections = oldSections.get(chunk);
                        for (int i = 0; i < sections.length; i++) {
                            var saved = sections[i];
                            chunk.getSection(i).fillBiomesFromNoise((x, y, z) -> saved.getNoiseBiome(x & 3, y & 3, z & 3),
                                    chunk.getPos().x() * 4, chunk.getSectionYFromSectionIndex(i) * 4, chunk.getPos().z() * 4);
                        }
                    }
                    game.items().indirectKills().restoreTerrain(provenance);
                }
            } finally {
                viewer.connection = connection; viewer.setChunkTrackingView(tracking);
                for (var pos : forced) level.setChunkForced(pos.x(), pos.z(), false);
                game.server().tickRateManager().setFrozen(frozen);
            }
        }
    }

    public static void run(GameController game) {
        require(running == null, "exclusive fixture"); running = new Fixture(game);
        try { running.init(); } catch (Throwable error) { try { running.close(); } catch (Throwable cleanup) { error.addSuppressed(cleanup); } running = null; throw error; }
        SkyWars2.LOGGER.info("[check] START vanity-terrain");
    }
    public static void tick(GameController game) {
        if (running == null) return;
        try {
            if (!running.step()) return;
            running.close(); running = null;
            SkyWars2.LOGGER.info("[check] PASS vanity-terrain (immutable cross-chunk palettes/biomes, same-state and destroyed container NBT/components, unsupported fluids/fire, pending/queued long scheduler clocks/order, bounded scan/native freeze/overlap guards, three rewinds across real world ticks, native relight and viewer packet)");
        } catch (Throwable error) {
            try { running.close(); } catch (Throwable cleanup) { error.addSuppressed(cleanup); }
            running = null; SkyWars2.LOGGER.error("[check] FAIL vanity-terrain", error);
        }
    }
}
