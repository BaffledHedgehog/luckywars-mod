package me.skywars2.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.skywars2.SkyWars2;
import me.skywars2.game.match.checkpoints.MatchPlayerCheckpoint;
import me.skywars2.game.match.checkpoints.MatchScoreboardCheckpoint;
import me.skywars2.game.match.checkpoints.MatchValueSnapshot;
import me.skywars2.mixin.players.PlayerCheckpointAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;

/** Native living End-return replacement; restoration resolves the current body by its stable UUID. */
public final class DimensionReturnChecks {
    private static Scene running;
    private DimensionReturnChecks() {}
    private static void require(boolean ok, String why) { if (!ok) throw new IllegalStateException("Living End return: " + why); }
    private static void flatten(Packet<?> packet, List<Packet<?>> into) {
        if (packet instanceof ClientboundBundlePacket bundle) for (var child : bundle.subPackets()) flatten(child, into);
        else into.add(packet);
    }
    private static List<Packet<?>> flatPackets(List<? extends Packet<?>> packets) {
        var result = new ArrayList<Packet<?>>(); packets.forEach(packet -> flatten(packet, result)); return result;
    }
    private static final class Scene implements AutoCloseable {
        final GameController game;
        final UUID id;
        final ServerPlayer retired;
        final ServerGamePacketListenerImpl previous;
        final RegenerationBeltChecks.ScheduledClient wire;
        final MatchPlayerCheckpoint saved;
        final MatchValueSnapshot profile;
        final MatchScoreboardCheckpoint scoreboard;
        final GameController.Checkpoint core;
        final boolean frozen;
        final ServerLevel end;
        final int started;
        final List<ItemStack> inventory = new ArrayList<>();
        java.util.Map<Stat<?>, Integer> stats;
        String selection;
        float health, saturation;
        int hunger, lives, phase, beforeReturn;
        CompletableFuture<?> ready;
        ServerPlayer replacement;
        boolean closed;
        Scene(GameController game) {
            this.game = game;
            retired = Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            require(game.playing() && game.allowDamage(retired) && retired.getClass() == ServerPlayer.class
                    && !retired.isUsingItem(), "active vanilla native player without an in-flight item use");
            id = retired.getUUID(); previous = retired.connection;
            saved = new MatchPlayerCheckpoint(game, retired);
            profile = new MatchValueSnapshot(game.players().get(id));
            scoreboard = new MatchScoreboardCheckpoint(game.server()); core = game.checkpointCore();
            frozen = game.server().tickRateManager().isFrozen();
            end = Objects.requireNonNull(game.server().getLevel(Level.END)); started = game.server().getTickCount();
            wire = new RegenerationBeltChecks.ScheduledClient(game, retired);
        }
        ServerPlayer current() { return Objects.requireNonNull(game.server().getPlayerList().getPlayer(id), "current native UUID body"); }
        void acknowledge() {
            var player = current(); var listener = player.connection;
            listener.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            var teleport = flatPackets(wire.packets()).stream().filter(ClientboundPlayerPositionPacket.class::isInstance)
                    .map(ClientboundPlayerPositionPacket.class::cast).reduce((a,b)->b).orElse(null);
            if (teleport != null) listener.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(
                    teleport.id(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
        }
        void initialize() {
            retired.closeContainer(); retired.stopRiding(); retired.setGameMode(GameType.SURVIVAL);
            retired.setNoGravity(true); retired.getInventory().clearContent();
            for (int slot = 0; slot < 36; slot++) {
                var item = new ItemStack(slot % 2 == 0 ? Items.DIAMOND : Items.EMERALD, slot % 23 + 1);
                item.set(DataComponents.CUSTOM_NAME, Component.literal("End return slot " + slot));
                retired.getInventory().setItem(slot, item);
            }
            retired.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
            retired.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            retired.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.CHAINMAIL_LEGGINGS));
            retired.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
            retired.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            retired.experienceLevel = 27; retired.experienceProgress = .375f; retired.totalExperience = 1234;
            retired.getFoodData().setFoodLevel(9); retired.getFoodData().setSaturation(3.25f);
            retired.setHealth(Math.max(1, retired.getMaxHealth() / 2));
            retired.getStats().setValue(retired, Stats.CUSTOM.get(Stats.EAT_CAKE_SLICE), 37);
            retired.getStats().setValue(retired, Stats.ITEM_USED.get(Items.BREAD), 29);
            game.players().get(id).kit = 1; game.players().get(id).lbcKit = 1;
            // The actual End-return API chooses a respawn destination; null selects Overworld.
            retired.setRespawnPosition(null, false);
            ready = game.maps().prepareChunk(end, new ChunkPos(0, 0));
        }
        boolean step() {
            acknowledge();
            require(game.server().getTickCount() - started < 80 && game.playing(), "bounded active native End-return scene");
            if (phase == 0) {
                if (!ready.isDone()) return false; ready.join();
                require(retired.teleportTo(end, .5, 200, .5, java.util.Set.of(), 17, -9, true), "real native End entry");
                acknowledge();
                for (int slot = 0; slot < retired.getInventory().getContainerSize(); slot++) inventory.add(retired.getInventory().getItem(slot).copy());
                stats = new HashMap<>(((PlayerCheckpointAccess.Stats)retired.getStats()).skywars2$stats());
                selection = new com.google.gson.Gson().toJson(game.players().get(id));
                health = retired.getHealth(); hunger = retired.getFoodData().getFoodLevel(); saturation = retired.getFoodData().getSaturationLevel();
                lives = game.lives(id); beforeReturn = wire.packets().size();
                replacement = game.server().getPlayerList().respawn(retired, true, Entity.RemovalReason.CHANGED_DIMENSION);
                require(replacement != retired && replacement == current() && replacement.getUUID().equals(id)
                        && replacement.getId() == retired.getId(), "real native same-UUID player-body replacement");
                require(replacement.connection.player == replacement, "actual AFTER_RESPAWN callback rebound native listener");
                acknowledge(); assertKept();
                replacement.setNoGravity(true); phase = 1; return false;
            }
            require(current() == replacement && replacement.level() == game.server().overworld()
                    && replacement.isAlive() && game.allowDamage(replacement) && !game.isObserver(replacement)
                    && !game.awaitingRespawn(id), "living End return remains an active participant on the next native tick");
            var transition = flatPackets(wire.packets().subList(beforeReturn, wire.packets().size()));
            require(transition.stream().anyMatch(ClientboundRespawnPacket.class::isInstance),
                    "native respawn transition (including native bundles) published by next tick: "
                            + transition.stream().map(packet -> packet.getClass().getSimpleName()).toList());
            for (int slot = 0; slot < inventory.size(); slot++) require(ItemStack.matches(inventory.get(slot), replacement.getInventory().getItem(slot)),
                    "next native tick retains inventory slot " + slot);
            return true;
        }
        void assertKept() {
            require(replacement.level() == game.server().overworld() && replacement.isAlive(), "native living End return reaches Overworld");
            for (int slot = 0; slot < inventory.size(); slot++) require(ItemStack.matches(inventory.get(slot), replacement.getInventory().getItem(slot)),
                    "all inventory/armor/offhand counts and components retained at slot " + slot);
            require(replacement.experienceLevel == 27 && replacement.experienceProgress == .375f && replacement.totalExperience == 1234,
                    "experience survives without new-life reset");
            require(stats.equals(new HashMap<>(((PlayerCheckpointAccess.Stats)replacement.getStats()).skywars2$stats()))
                    && selection.equals(new com.google.gson.Gson().toJson(game.players().get(id))) && game.lives(id) == lives,
                    "native statistics, complete perk/profile selection and remaining lives are unchanged");
            require(replacement.getHealth() == health && replacement.getFoodData().getFoodLevel() == hunger
                    && replacement.getFoodData().getSaturationLevel() == saturation, "native health and hunger remain living-transfer state");
        }
        public void close() {
            if (closed) return; closed = true;
            try {
                game.server().tickRateManager().setFrozen(true);
                game.restoreCore(core); profile.restore(); scoreboard.restore(game.server());
                var restored = saved.restoreFrozen();
                require(restored == current(), "saved native data restored to current UUID replacement");
                acknowledge();
            } finally {
                try {
                    wire.close();
                    var live = game.server().getPlayerList().getPlayer(id);
                    if (live != null) {
                        live.connection = previous; previous.player = live; previous.resetPosition();
                        ((MatchPlayerCheckpoint.SimulatedAcknowledgement)previous).skywars2$acknowledgeRestore();
                    }
                } finally { game.server().tickRateManager().setFrozen(frozen); }
            }
        }
    }
    public static void run(GameController game) {
        require(running == null, "exclusive scene"); running = new Scene(game);
        try { running.initialize(); } catch (Throwable error) { running.close(); running = null; throw error; }
        SkyWars2.LOGGER.info("[check] START dimension-return");
    }
    public static void tick(GameController game) {
        if (running == null) return;
        try {
            if (!running.step()) return;
            running.close(); running = null;
            SkyWars2.LOGGER.info("[check] PASS dimension-return (actual End->PlayerList.respawn alive=true->Overworld, UUID replacement, inventory/armor/offhand, XP/stats/profile/lives and native restoration)");
        } catch (Throwable error) {
            try { running.close(); } catch (Throwable cleanup) { error.addSuppressed(cleanup); }
            running = null; SkyWars2.LOGGER.error("[check] FAIL dimension-return", error);
        }
    }
}
