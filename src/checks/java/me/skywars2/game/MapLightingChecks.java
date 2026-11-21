package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tick-driven native light propagation and packet regression; no unfinished future is joined. */
public final class MapLightingChecks {
    private static Fixture active;
    private MapLightingChecks() { }

    private static void require(boolean value, String why) {
        if (!value) throw new IllegalStateException("Map lighting: " + why);
    }

    public static void run(GameController game) {
        require(active == null && !game.maps().busy(), "exclusive ready map fixture");
        active = new Fixture(game);
        try { active.prepare(); }
        catch (RuntimeException failure) { active.close(); active = null; throw failure; }
    }

    public static void tick(GameController game) {
        if (active == null) return;
        try {
            if (active.tick()) {
                active.close(); active = null;
                SkyWars2.LOGGER.info("[check] PASS map-lighting");
            }
        } catch (RuntimeException failure) {
            try { active.close(); } catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); }
            active = null;
            SkyWars2.LOGGER.error("[check] FAIL map-lighting: {}", failure.getMessage(), failure);
        }
    }

    private static final class Viewer {
        final ServerPlayer player;
        final ServerGamePacketListenerImpl connection;
        final ChunkTrackingView tracking;
        final ElementalGrimoireChecks.Recording recording;
        Viewer(GameController game, ServerPlayer player) {
            this.player = player; connection = player.connection; tracking = player.getChunkTrackingView();
            recording = new ElementalGrimoireChecks.Recording(player);
            player.connection = new ServerGamePacketListenerImpl(game.server(), recording, player,
                    new CommonListenerCookie(player.getGameProfile(), 0, player.clientInformation(), false));
            player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        }
        void restore() { player.connection = connection; player.setChunkTrackingView(tracking); }
    }

    private static final class Fixture {
        final GameController game;
        final ServerLevel level;
        final Map<BlockPos, MapManager.BlockSnapshot> saved = new LinkedHashMap<>();
        final List<ChunkPos> forced = new ArrayList<>();
        final Viewer interior, excluded;
        final BlockPos center, source, probe;
        MapLighting.Update update;
        int phase, ticks, hold;
        boolean restoring;

        Fixture(GameController game) {
            this.game = game; level = game.maps().level();
            var players = game.server().getPlayerList().getPlayers();
            require(players.size() >= 2, "two actual connected viewers");
            var owner = players.getFirst();
            require(owner.level() == level && players.get(1).level() == level, "viewers in arena dimension");
            var chunk = owner.chunkPosition();
            center = new BlockPos((chunk.x() << 4) + 15, level.getMaxY() - 25, (chunk.z() << 4) + 8);
            source = center; probe = center.east();
            interior = new Viewer(game, owner); excluded = new Viewer(game, players.get(1));
        }

        void prepare() {
            // Load only the deliberately edited volume plus its propagation neighbors.
            var chunk = ChunkPos.containing(center);
            for (int x = chunk.x() - 1; x <= chunk.x() + 2; x++)
                for (int z = chunk.z() - 1; z <= chunk.z() + 1; z++) {
                    level.getChunk(x, z);
                    if (level.setChunkForced(x, z, true)) forced.add(new ChunkPos(x, z));
                }
            for (var pos : BlockPos.betweenClosed(center.offset(-3, -1, -3), center.offset(3, 3, 3)))
                saved.put(pos.immutable(), game.maps().snapshot(pos));
            update = new MapLighting.Update(level);
            for (var pos : saved.keySet()) {
                boolean wall = Math.abs(pos.getX() - center.getX()) == 3
                        || Math.abs(pos.getZ() - center.getZ()) == 3 || pos.getY() == center.getY() - 1
                        || pos.getY() == center.getY() + 3;
                write(pos, wall ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
            write(source, Blocks.GLOWSTONE.defaultBlockState());
            hold = 3;
        }

        void write(BlockPos pos, BlockState state) {
            if (level.getBlockState(pos).equals(state)) return;
            update.changed(pos); level.setBlock(pos, state, 2);
        }

        boolean tick() {
            require(++ticks < 800, "native asynchronous fixture completed within 800 ticks; phase=" + phase);
            var map = level.getChunkSource().chunkMap;
            // This is a real member of ChunkMap's player map with an empty native tracking view.
            // No player movement, team, inventory or game mode is changed by the fixture.
            excluded.player.setChunkTrackingView(ChunkTrackingView.EMPTY);
            require(!map.getPlayers(ChunkPos.containing(probe), false).contains(excluded.player), "untracked viewer excluded natively");
            if (hold > 0) {
                require(!update.completion().isDone(), "write phase cannot complete the lighting transaction");
                require(map.getChunkToSend(ChunkPos.containing(source).pack()) == null, "native initial chunk send is gated during writes");
                require(!update.tick(), "tick cannot finish before finishWrites");
                if (--hold == 0) {
                    require(map.getPlayers(ChunkPos.containing(probe), false).contains(interior.player), "actual tracked interior viewer");
                    require(!map.getPlayers(ChunkPos.containing(probe), true).contains(interior.player), "interior viewer is excluded from native border-only light broadcast");
                    interior.recording.packets.clear(); excluded.recording.packets.clear();
                    update.finishWrites();
                }
                return false;
            }
            if (!update.tick()) return false;
            require(update.completion().isDone() && !update.completion().isCompletedExceptionally(), "native barrier and resend completed");
            if (restoring) return true;
            boolean closed = phase % 2 == 0;
            int block = closed ? 14 : 0, sky = closed ? 0 : 15;
            require(level.getBrightness(LightLayer.BLOCK, probe) == block,
                    "actual block light after roof/source transition " + phase + ": " + level.getBrightness(LightLayer.BLOCK, probe));
            require(level.getBrightness(LightLayer.SKY, probe) == sky,
                    "actual skylight after roof transition " + phase + ": " + level.getBrightness(LightLayer.SKY, probe));
            verifyPacket(interior, probe, block, sky);
            require(excluded.recording.packets.stream().noneMatch(p -> p instanceof ClientboundLightUpdatePacket light
                    && light.x() == probe.getX() >> 4 && light.z() == probe.getZ() >> 4), "no light resend to untracked viewer");
            require(update.sentPackets() > 0, "production resend reached tracked viewer");
            if (++phase == 4) {
                // Cancellation must release the same native send gate even before finishWrites.
                var canceled = new MapLighting.Update(level); canceled.changed(probe);
                require(map.getChunkToSend(ChunkPos.containing(probe).pack()) == null, "cancel probe acquired native gate");
                canceled.cancel();
                require(map.getChunkToSend(ChunkPos.containing(probe).pack()) != null, "cancel releases native gate");
                var remote = new BlockPos(19000000, center.getY(), 19000000);
                require(level.getChunkSource().getChunkNow(remote.getX() >> 4, remote.getZ() >> 4) == null, "remote chunk initially unloaded");
                var empty = new MapLighting.Update(level); empty.changed(remote); empty.finishWrites(); empty.tick();
                require(level.getChunkSource().getChunkNow(remote.getX() >> 4, remote.getZ() >> 4) == null, "lighting resend never loads an absent chunk");
                empty.cancel();
                update = new MapLighting.Update(level); restoring = true;
                for (var entry : saved.entrySet()) {
                    update.changed(entry.getKey());
                    game.maps().restoreSnapshot(level, entry.getKey(), entry.getValue(), 2);
                }
                update.finishWrites(); return false;
            }
            update = new MapLighting.Update(level);
            boolean nextClosed = phase % 2 == 0;
            write(source, nextClosed ? Blocks.GLOWSTONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            for (var pos : BlockPos.betweenClosed(center.offset(-2, 3, -2), center.offset(2, 3, 2)))
                write(pos, nextClosed ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            interior.recording.packets.clear(); excluded.recording.packets.clear();
            update.finishWrites(); return false;
        }

        void verifyPacket(Viewer viewer, BlockPos pos, int block, int sky) {
            ClientboundLightUpdatePacket found = null;
            for (var packet : viewer.recording.packets)
                if (packet instanceof ClientboundLightUpdatePacket light && light.x() == pos.getX() >> 4 && light.z() == pos.getZ() >> 4)
                    found = light;
            require(found != null, "authoritative native light packet reaches interior viewer after phase " + phase);
            var data = found.lightData();
            require(data.blockYMask().cardinality() == data.blockUpdates().size()
                    && data.skyYMask().cardinality() == data.skyUpdates().size(), "packet masks index actual data arrays");
            require(packetValue(data.blockYMask(), data.emptyBlockYMask(), data.blockUpdates(), pos) == block,
                    "packet block nibble matches settled engine, phase=" + phase);
            require(packetValue(data.skyYMask(), data.emptySkyYMask(), data.skyUpdates(), pos) == sky,
                    "packet sky nibble matches settled engine, phase=" + phase);
        }

        int packetValue(BitSet mask, BitSet empty, List<byte[]> arrays, BlockPos pos) {
            int section = SectionPos.blockToSectionCoord(pos.getY()) - level.getLightEngine().getMinLightSection();
            if (empty.get(section)) return 0;
            require(mask.get(section), "full packet covers probe section including empty mask");
            int index = mask.get(0, section).cardinality();
            return new DataLayer(arrays.get(index)).get(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
        }

        void close() {
            if (update != null) update.cancel();
            if (!restoring) for (var entry : saved.entrySet())
                game.maps().restoreSnapshot(level, entry.getKey(), entry.getValue(), 2);
            interior.restore(); excluded.restore();
            for (var chunk : forced) level.setChunkForced(chunk.x(), chunk.z(), false);
        }
    }
}
