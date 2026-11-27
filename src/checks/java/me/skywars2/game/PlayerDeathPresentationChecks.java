package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.function.Predicate;
import eu.pb4.polymer.core.api.entity.PolymerServerEntitySynchronizer;
import me.skywars2.game.presentation.PlayerDeathPresentation;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.UpdateInterval;

/** Native tracker and recipient packets; no entities are removed from tracking sections. */
public final class PlayerDeathPresentationChecks {
    private PlayerDeathPresentationChecks() {}
    private static void require(boolean value, String why) { if (!value) throw new IllegalStateException("Player death presentation: " + why); }
    private record Audience(List<ServerPlayer> players) implements ServerEntity.Synchronizer {
        public void sendToTrackingPlayers(Packet<? super ClientGamePacketListener> packet) { for (var p : players) p.connection.send(packet); }
        public void sendToTrackingPlayersAndSelf(Packet<? super ClientGamePacketListener> packet) { sendToTrackingPlayers(packet); }
        public void sendToTrackingPlayersFiltered(Packet<? super ClientGamePacketListener> packet, Predicate<ServerPlayer> filter) {
            for (var p : players) if (filter.test(p)) p.connection.send(packet);
        }
    }
    public static void run(GameController game) {
        var player = game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        var viewer = game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
        require(player != null && viewer != null && player.isAlive() && viewer.isAlive(), "two native living players");
        var saved = new RegenerationBeltChecks.Snapshot(game, player);
        try (var self = new RegenerationBeltChecks.ScheduledClient(game, player);
             var remote = new RegenerationBeltChecks.ScheduledClient(game, viewer)) {
            var listeners = new LinkedHashSet<net.minecraft.server.network.ServerPlayerConnection>();
            listeners.add(player.connection); listeners.add(viewer.connection);
            // The real ChunkMap constructor uses this exact Polymer API to wrap TrackedEntity.
            // A bare fixture Audience bypassed that production path and hid the wrapper cast failure.
            var synchronizer = PolymerServerEntitySynchronizer.of(new Audience(List.of(player, viewer)), () -> listeners, player);
            var tracker = new ServerEntity(player.level(), player, UpdateInterval.periodic(1), true, synchronizer);
            player.setHealth(0); tracker.sendChanges();
            int healthId = me.skywars2.mixin.combat.BlackHoleDeathProtectionAccess.skywars2$healthData().id();
            var deadPairing = new ArrayList<Packet<ClientGamePacketListener>>();tracker.sendPairingData(viewer, deadPairing::add);
            require(deadPairing.stream().filter(ClientboundSetEntityDataPacket.class::isInstance)
                    .map(ClientboundSetEntityDataPacket.class::cast).flatMap(packet -> packet.packedItems().stream())
                    .anyMatch(value -> value.id() == healthId && value.value().equals(0f)),
                    "controlled native tracker pairing starts with an actual cached zero-health value");
            self.packets().clear(); remote.packets().clear();
            player.setHealth(player.getMaxHealth());
            var immediatePairing = new ArrayList<Packet<ClientGamePacketListener>>();tracker.sendPairingData(viewer, immediatePairing::add);
            require(immediatePairing.stream().filter(ClientboundSetEntityDataPacket.class::isInstance)
                    .map(ClientboundSetEntityDataPacket.class::cast).flatMap(packet -> packet.packedItems().stream())
                    .anyMatch(value -> value.id() == healthId && value.value().equals(player.getHealth())),
                    "new viewer pairing before the next sendChanges contains current positive HP");
            require(immediatePairing.stream().noneMatch(ClientboundRemoveEntitiesPacket.class::isInstance)
                    && self.packets().isEmpty() && remote.packets().isEmpty(),
                    "fresh viewer pairing refreshes its snapshot without a global resend or death-pose repair broadcast");
            tracker.sendChanges();
            var bundles = remote.packets().stream().filter(ClientboundBundlePacket.class::isInstance).map(ClientboundBundlePacket.class::cast).toList();
            require(bundles.size() == 1, "one re-pair after actual same-body zero-to-positive health");
            var packets = new ArrayList<Packet<? super ClientGamePacketListener>>(); bundles.getFirst().subPackets().forEach(packets::add);
            require(packets.getFirst() instanceof ClientboundRemoveEntitiesPacket remove && remove.entityIds().contains(player.getId())
                    && packets.stream().anyMatch(p -> p instanceof ClientboundAddEntityPacket spawn && spawn.getId() == player.getId() && spawn.getUUID().equals(player.getUUID()))
                    && packets.stream().anyMatch(ClientboundSetEntityDataPacket.class::isInstance)
                    && packets.stream().anyMatch(ClientboundUpdateAttributesPacket.class::isInstance),
                    "bundle replaces only the client entity with identical id/profile and complete native state");
            require(packets.stream().filter(ClientboundSetEntityDataPacket.class::isInstance)
                    .map(ClientboundSetEntityDataPacket.class::cast).flatMap(packet -> packet.packedItems().stream())
                    .anyMatch(value -> value.id() == healthId && value.value().equals(player.getHealth())),
                    "re-pair metadata contains current positive HP rather than the tracker's former zero-health cache");
            require(self.packets().stream().noneMatch(ClientboundBundlePacket.class::isInstance)
                    && player.level().getEntityOrPart(player.getId()) == player
                    && game.server().getPlayerList().getPlayer(player.getUUID()) == player,
                    "self and native world/player-list membership are unchanged");
            remote.packets().clear(); tracker.sendChanges();
            require(remote.packets().stream().noneMatch(ClientboundBundlePacket.class::isInstance), "healthy unchanged body does not repeatedly re-pair");
            listeners.remove(viewer.connection);
            PlayerDeathPresentation.joined(player); tracker.sendChanges();
            require(remote.packets().stream().noneMatch(ClientboundBundlePacket.class::isInstance),
                    "current Polymer tracking audience excludes a removed viewer");
            listeners.add(viewer.connection);
            PlayerDeathPresentation.joined(player); tracker.sendChanges();
            require(remote.packets().stream().filter(ClientboundBundlePacket.class::isInstance).count() == 1,
                    "completed join requests one full-state re-pair even when old client death state is unknown");
        } finally { saved.restore(game); }
        me.skywars2.SkyWars2.LOGGER.info("[check] PASS player-death-presentation (real Polymer audience, same native body/client re-pair, positive metadata, self exclusion and live watcher changes)");
    }
}
