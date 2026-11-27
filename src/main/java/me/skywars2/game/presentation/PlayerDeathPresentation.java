package me.skywars2.game.presentation;

import eu.pb4.polymer.core.api.entity.PolymerServerEntitySynchronizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;

/** A healed native player needs a fresh client entity: health updates do not reset client deathTime. */
public final class PlayerDeathPresentation {
    private static final Set<ServerPlayer> PENDING = Collections.newSetFromMap(new WeakHashMap<>());
    private PlayerDeathPresentation() {}

    public interface Pairing { void skywars2$refreshMetadata(); }

    private static List<ServerPlayer> viewers(ServerPlayer player, ServerEntity.Synchronizer synchronizer) {
        // Polymer wraps the native TrackedEntity synchronizer. Its public listener supplier is the
        // same live seenBy set, for both Direct and Polymeric wrappers, including real players.
        if (synchronizer instanceof PolymerServerEntitySynchronizer polymer)
            return List.copyOf(polymer.listeners()).stream().map(ServerPlayerConnection::getPlayer).toList();
        return List.copyOf(PlayerLookup.tracking(player));
    }

    /** Call after all join/reset state is installed, including legacy saved dead bodies. */
    public static void joined(ServerPlayer player) { request(player); }

    public static void request(ServerPlayer player) {
        if (player.connection != null && player.level().getServer().isSameThread()) PENDING.add(player);
    }

    /** Called after the native tracker publishes current metadata; never changes world tracking membership. */
    public static void synchronize(ServerPlayer player, ServerEntity tracker, ServerEntity.Synchronizer synchronizer) {
        if (!PENDING.remove(player) || player.isRemoved() || !player.isAlive()
                || player.getHealth() <= 0 || player.level().getServer().getPlayerList().getPlayer(player.getUUID()) != player) return;
        ((Pairing) tracker).skywars2$refreshMetadata();
        for (var viewer : viewers(player, synchronizer)) {
            if (viewer == player || viewer.connection == null || viewer.level() != player.level()) continue;
            viewer.connection.send(bundle(player, viewer, tracker));
        }
    }

    public static ClientboundBundlePacket bundle(ServerPlayer player, ServerPlayer viewer, ServerEntity tracker) {
        var packets = new ArrayList<Packet<? super ClientGamePacketListener>>();
        packets.add(new ClientboundRemoveEntitiesPacket(player.getId()));
        tracker.sendPairingData(viewer, packets::add);
        for (var effect : player.getActiveEffects()) packets.add(new ClientboundUpdateMobEffectPacket(player.getId(), effect, false));
        if (viewer.getCamera() == player) packets.add(new net.minecraft.network.protocol.game.ClientboundSetCameraPacket(player));
        return new ClientboundBundlePacket(packets);
    }
}
