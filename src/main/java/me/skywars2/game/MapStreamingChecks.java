package me.skywars2.game;

import me.skywars2.SkyWars2;
import me.skywars2.mixin.ArenaChunkAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Actual 60 -> 61 placement and native initial chunk delivery, driven only by real server ticks. */
public final class MapStreamingChecks {
    private static Fixture active;
    private MapStreamingChecks() { }

    private static void require(boolean condition, String why) {
        if (!condition) throw new IllegalStateException("Map streaming: " + why);
    }

    private static Field field(Object value, String name) throws NoSuchFieldException {
        for (Class<?> type = value.getClass(); type != null; type = type.getSuperclass()) {
            try { var result = type.getDeclaredField(name); result.setAccessible(true); return result; }
            catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }

    public static void run(GameController game) {
        require(active == null && game.inLobby() && !game.maps().busy(), "exclusive ready lobby");
        var fixture = new Fixture(game);
        active = fixture;
        try { fixture.prepare(); }
        catch (RuntimeException failure) { fixture.close(); active = null; throw failure; }
    }

    public static void tick(GameController game) {
        if (active == null) return;
        try {
            if (active.tick()) {
                active.close(); active = null;
                SkyWars2.LOGGER.info("[check] PASS map-streaming (actual maps60->61; native initial chunk packets during placement; no manual ticks)");
            }
        } catch (RuntimeException failure) {
            try { active.close(); } catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); }
            active = null;
            SkyWars2.LOGGER.error("[check] FAIL map-streaming: {}", failure.getMessage(), failure);
        }
    }

    private static final class Recording extends Connection {
        final Fixture fixture;
        ClientboundPlayerPositionPacket position;
        int batches;
        int fullPackets;
        Recording(Fixture fixture) { super(PacketFlow.SERVERBOUND); this.fixture = fixture; }
        @Override public boolean isConnecting() { return false; }
        @Override public boolean isConnected() { return true; }
        @Override public boolean isMemoryConnection() { return true; }
        @Override public void flushChannel() { }
        @Override public void send(Packet<?> packet, io.netty.channel.ChannelFutureListener listener, boolean flush) {
            if (packet instanceof ClientboundPlayerPositionPacket teleport) position = teleport;
            if (packet instanceof ClientboundChunkBatchFinishedPacket) batches++;
            if (packet instanceof ClientboundLevelChunkWithLightPacket chunk) {
                fullPackets++;
                long key = net.minecraft.world.level.ChunkPos.pack(chunk.x(), chunk.z());
                // Record busy at packet emission, since the next check may follow completion.
                if (fixture.game.maps().busy() && fixture.gated.contains(key)
                        && fixture.game.maps().streamingChangedChunks().contains(key))
                    fixture.deliveredDuringWork.add(key);
            }
        }
    }

    private static final class Viewer implements AutoCloseable {
        final Fixture fixture;
        final ServerPlayer player;
        final ServerGamePacketListenerImpl previous, listener;
        final ChunkTrackingView tracking;
        final Vec3 position, velocity;
        final float yaw, pitch;
        final boolean gravity;
        final Recording recording;
        final Field ticks;
        final Set<Long> queued = new HashSet<>();
        int acknowledged;

        Viewer(Fixture fixture, ServerPlayer player) {
            this.fixture = fixture; this.player = player;
            previous = player.connection; tracking = player.getChunkTrackingView();
            position = player.position(); velocity = player.getDeltaMovement();
            yaw = player.getYRot(); pitch = player.getXRot(); gravity = player.isNoGravity();
            recording = new Recording(fixture);
            try {
                listener = new ServerGamePacketListenerImpl(fixture.game.server(), recording, player,
                        new CommonListenerCookie(player.getGameProfile(), 0, player.clientInformation(), false));
                player.connection = listener;
                // A channel-less scoped connection needs its actual native dispatch field bound.
                field(recording, "packetListener").set(recording, listener);
                ticks = field(listener, "tickCount");
                listener.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
                listener.teleport(player.getX(), player.getY(), player.getZ(), yaw, pitch);
                acknowledge();
                fixture.game.server().getConnection().getConnections().add(recording);
                player.setNoGravity(true); player.setDeltaMovement(Vec3.ZERO);
            } catch (ReflectiveOperationException | RuntimeException failure) {
                fixture.game.server().getConnection().getConnections().remove(recording);
                player.connection = previous;
                throw new IllegalStateException("Map streaming: register native viewer", failure);
            }
        }

        void acknowledge() {
            listener.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            try {
                if (field(listener, "awaitingPositionFromClient").get(listener) != null) {
                    var teleport = recording.position;
                    require(teleport != null, "acknowledge an actual emitted teleport");
                    listener.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(teleport.id(),
                            player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
                }
            } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
            while (acknowledged < recording.batches) {
                listener.handleChunkBatchReceived(new ServerboundChunkBatchReceivedPacket(64)); acknowledged++;
            }
        }

        int updates() {
            try { return ticks.getInt(listener); }
            catch (IllegalAccessException failure) { throw new IllegalStateException(failure); }
        }

        void pending(long key) {
            if (!queued.add(key)) return;
            var pos = net.minecraft.world.level.ChunkPos.unpack(key);
            var chunk = fixture.level.getChunkSource().getChunkNow(pos.x(), pos.z());
            if (chunk != null) {
                // Represent a new viewer's initial request using the actual native sender.
                // MinecraftServer owns sendNextChunks; this fixture never calls a tick or send loop.
                listener.chunkSender.markChunkPendingToSend(chunk);
            }
        }

        @Override public void close() {
            fixture.game.server().getConnection().getConnections().remove(recording);
            player.teleportTo(fixture.level, position.x, position.y, position.z, Set.of(), yaw, pitch, true);
            player.setNoGravity(gravity); player.setDeltaMovement(velocity);
            player.connection = previous; previous.resetPosition();
            player.setChunkTrackingView(tracking);
        }
    }

    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final ServerLevel level;
        final List<Viewer> viewers = new ArrayList<>();
        final Set<Long> gated = new HashSet<>(), deliveredDuringWork = new HashSet<>();
        MapManager.Result result;
        int phase, started, settling;
        int firstDelivered;

        Fixture(GameController game) { this.game = game; level = game.maps().level(); }

        void prepare() {
            for (String name : List.of("TestAlpha", "TestBeta")) {
                var player = game.server().getPlayerList().getPlayer(GameChecks.offlineUuid(name));
                require(player != null && player.level() == level, "two actual registered Overworld viewers: " + name);
                viewers.add(new Viewer(this, player));
            }
            for (String id : List.of("60", "61", "1"))
                require(game.maps().info(id) != null && game.maps().info(id).available(), "available native map " + id);
            begin();
        }

        void begin() {
            result = null; gated.clear(); deliveredDuringWork.clear(); settling = 0;
            viewers.forEach(viewer -> viewer.queued.clear());
            started = game.server().getTickCount();
            game.maps().load(mapId(), false, value -> result = value);
            require(game.maps().busy(), "real asynchronous map request entered busy state");
        }

        boolean tick() {
            int elapsed = game.server().getTickCount() - started;
            require(elapsed < 2400, "map " + mapId() + " completes within2400 real ticks; gated="
                    + gated.size() + " delivered=" + deliveredDuringWork.size() + " batches=" + game.maps().streamingPendingBatches());
            viewers.forEach(Viewer::acknowledge);
            var chunks = level.getChunkSource().chunkMap;
            if (game.maps().busy()) {
                for (long key : game.maps().streamingChangedChunks()) {
                    if (chunks.getChunkToSend(key) == null) {
                        gated.add(key);
                        for (var viewer : viewers) viewer.pending(key);
                    }
                }
                return false;
            }
            require(result != null && result.success(), "actual map callback succeeded: " + result);
            require(!game.maps().spawns().isEmpty(), "loaded map has native spawn markers");
            require(!game.maps().lightingPending() && game.maps().streamingPendingBatches() == 0,
                    "completion waits for every sealed lighting batch");
            require(phase == 2 || (!gated.isEmpty() && !deliveredDuringWork.isEmpty()),
                    "native initial full chunk packet for an actually changed, previously gated chunk before map completion; map="
                            + mapId() + " gated=" + gated.size());
            // A merely FULL chunk need not be ticking; inspect its actual dependency future,
            // and require getChunkToSend only where the native ticking prerequisite is met.
            for (long key : game.maps().streamingChangedChunks()) {
                var pos = net.minecraft.world.level.ChunkPos.unpack(key);
                var holder = ((ArenaChunkAccess) chunks).skywars2$visibleChunks().get(key);
                if (holder != null) {
                    require(holder.getSendSyncFuture().isDone(), "no residual native send dependency after completion: " + pos);
                    if (holder.getTickingChunk() != null)
                        require(chunks.getChunkToSend(key) != null, "native ticking chunk is sendable after completion: " + pos);
                }
            }
            if (++settling < 3) return false;
            for (var viewer : viewers)
                require(viewer.updates() >= 2, "actual registered native connection scheduler advanced");
            SkyWars2.LOGGER.info("[check] map-streaming map={} ticks={} changed={} gated={} fullDuringWork={} completedBatches={}",
                    mapId(), elapsed, game.maps().streamingChangedChunks().size(), gated.size(),
                    deliveredDuringWork.size(), game.maps().streamingCompletedBatches());
            if (phase == 0) firstDelivered = deliveredDuringWork.size();
            if (phase++ < 2) { begin(); return false; }
            require(firstDelivered > 0, "both native map operations streamed progressively");
            return true;
        }

        String mapId() { return phase == 0 ? "60" : phase == 1 ? "61" : "1"; }

        @Override public void close() {
            RuntimeException failure = null;
            for (var viewer : viewers) try { viewer.close(); }
            catch (RuntimeException error) { if (failure == null) failure = error; else failure.addSuppressed(error); }
            if (failure != null) throw failure;
        }
    }
}
