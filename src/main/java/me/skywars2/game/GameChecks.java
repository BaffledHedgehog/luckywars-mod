package me.skywars2.game;

import eu.pb4.sgui.api.ClickType;
import eu.pb4.sgui.api.SguiUtils;
import eu.pb4.sgui.api.gui.SlotBasedGui;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.skywars2.SkyWars2;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.contents.TranslatableContents;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Explicit operator checks against live server state, never run automatically. */
public final class GameChecks {
    private static final Map<UUID, int[]> baseline = new LinkedHashMap<>();
    private static SlotBasedGui staleMenu;
    private static ServerPlayer stalePlayer;
    private static GameConfigSnapshot fastConfigSnapshot;
    private static WatchState watcher;
    private static LivesState livesState;
    private static Runnable deferredCheck;
    private static int deferredTicks;
    private static final String[] BOT_NAMES = {"TestAlpha", "TestBeta"};
    private GameChecks() {}

    private record GameConfigSnapshot(int lives, int countdownSeconds, int stageOneSeconds,
                                      int stageTwoSeconds, int endingSeconds, int stageOneEvent,
                                      int stageTwoEvent) {
        static GameConfigSnapshot from(GameConfig c) {
            return new GameConfigSnapshot(c.lives, c.countdownSeconds, c.stageOneSeconds,
                    c.stageTwoSeconds, c.endingSeconds, c.stageOneEvent, c.stageTwoEvent);
        }
        void apply(GameConfig c) {
            c.lives = lives; c.countdownSeconds = countdownSeconds;
            c.stageOneSeconds = stageOneSeconds; c.stageTwoSeconds = stageTwoSeconds;
            c.endingSeconds = endingSeconds; c.stageOneEvent = stageOneEvent;
            c.stageTwoEvent = stageTwoEvent;
        }
    }

    private static final class WatchState {
        GameController.Stage lastStage;
        boolean countdown, playing, phaseOne, phaseTwo, ending;
        boolean trackerChecked, failed;
        int playingTicks;
        int loadingMenuChecks, preparationMenuChecks, placementMenuChecks;
        long maxLoadingMenuNanos;
        WatchState(GameController.Stage stage) { lastStage = stage; }
    }

    private enum LivesStep { WAIT_PLAYING, WAIT_FIRST_RESPAWN, WAIT_SECOND_DEATH, WAIT_ELIMINATION }
    private static final class LivesState {
        UUID victim;
        LivesStep step = LivesStep.WAIT_PLAYING;
        int waitTicks;
        LivesState(UUID victim) { this.victim = victim; }
    }

    public static void run(GameController game, String suite) {
        try {
            switch (suite) {
                case "lobby" -> {
                    require(game.inLobby(), "Lobby is ready");
                    require(game.maps().level() != null, "Arena dimension exists");
                    require(game.maps().catalog().stream().filter(MapManager.MapInfo::available).count() == 42, "42 complete maps");
                    for (var player : game.server().getPlayerList().getPlayers()) {
                        require(player.level() == game.maps().level(), "Player in arena");
                        require(player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE, "Lobby adventure mode");
                        require(player.getInventory().getItem(GameController.MENU_COMPASS_SLOT).is(Items.COMPASS), "Lobby compass in center hotbar slot");
                    }
                }
                case "snapshot" -> {
                    snapshot(game);
                }
                case "menus" -> checkMenus(game);
                case "design-export" -> exportMenuDesign(game);
                case "lobby-inventory" -> checkLobbyInventory(game);
                case "inventory-unlocked" -> checkUnlockedInventory(game);
                case "kill-reward" -> {
                    require(game.playing(), "Kill reward requires an active match");
                    var victim = game.server().getPlayerList().getPlayer(offlineUuid("TestAlpha"));
                    var killer = game.server().getPlayerList().getPlayer(offlineUuid("TestBeta"));
                    require(victim != null && killer != null && game.allowDamage(victim) && game.allowDamage(killer), "Both test participants are alive");
                    var profile = game.players().get(killer.getUUID());
                    int coins = profile.coins, kills = profile.kills, lives = game.lives(victim.getUUID());
                    victim.hurtServer(victim.level(), victim.damageSources().playerAttack(killer), 10000);
                    require(game.lives(victim.getUUID()) == lives - 1 && game.awaitingRespawn(victim.getUUID()), "Native PvP death consumed one life");
                    require(profile.coins == coins + 50 && profile.kills == kills + 1, "Kill announcement corresponds to exactly 50 awarded coins");
                }
                case "lobby-rules" -> { checkLobbyRules(game); return; }
                case "stale-menu" -> checkStaleMenu(game);
                case "countdown" -> {
                    require(game.stage() == GameController.Stage.COUNTDOWN, "Countdown stage");
                    require(game.maps().spawns().size() >= 2, "NBT spawn markers recovered");
                    require(game.alivePlayers().size() >= 2, "Participants enrolled");
                    for (var p : game.alivePlayers()) require(p.gameMode.getGameModeForPlayer() == GameType.ADVENTURE, "Caged adventure player");
                    require(cageBarriers(game) > 0, "Countdown has actual barrier cages");
                }
                case "playing" -> {
                    require(game.playing(), "Match active");
                    require(game.alivePlayers().size() >= 2, "Two live participants");
                    require(cageBarriers(game) == 0, "Cage walls and roofs opened");
                    for (var p : game.alivePlayers()) {
                        require(p.gameMode.getGameModeForPlayer() == GameType.SURVIVAL, "Survival player");
                        require(p.getHealth() > 0, "Alive health");
                        var prior = baseline.get(p.getUUID());
                        if (prior != null) require(game.players().get(p.getUUID()).games == prior[1] + 1, "Game count credited once");
                    }
                }
                case "ending" -> {
                    require(game.stage() == GameController.Stage.ENDING, "Ending stage");
                    int winners = 0;
                    for (var p : game.server().getPlayerList().getPlayers()) {
                        var prior = baseline.get(p.getUUID());
                        if (prior == null) continue;
                        var now = game.players().get(p.getUUID());
                        if (now.wins == prior[2] + 1) {
                            winners++;
                            require(now.coins >= prior[0] + 110, "Start and victory coins");
                            require(java.util.Arrays.stream(now.keys).sum() == prior[4] + 1, "One victory key");
                            require(p.gameMode.getGameModeForPlayer() == GameType.CREATIVE, "Winner creative mode");
                        }
                    }
                    require(winners == 1, "One credited winner");
                }
                case "tracker" -> {
                    require(game.playing(), "Match active");
                    for (var p : game.alivePlayers()) {
                        boolean found = false;
                        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                            var stack = p.getInventory().getItem(i);
                            if (stack.is(Items.COMPASS) && stack.has(DataComponents.LODESTONE_TRACKER)) found = true;
                        }
                        require(found, "Compass points to a live opponent");
                    }
                }
                case "phases" -> {
                    require(game.stage() == GameController.Stage.PHASE_TWO, "Both stages reached");
                    require(game.config().stageOneEvent == 2 && game.config().stageTwoEvent == 6, "Fixed smoke-test events");
                    for (var p : game.alivePlayers()) {
                        var speed = p.getEffect(net.minecraft.world.effect.MobEffects.SPEED);
                        require(speed != null && speed.getAmplifier() == 1, "Stage one applies Speed II");
                        var prior = baseline.get(p.getUUID());
                        require(prior != null && game.players().get(p.getUUID()).coins == prior[0] + 70,
                                "Exactly 10 start + 20 stage one + 40 stage two coins");
                    }
                    double border = game.maps().level().getWorldBorder().getSize();
                    require(border > 4 && border <= 384, "Stage two actively shrinks border");
                }
                case "bots" -> createTestBots(game);
                case "one-life" -> { require(game.inLobby(), "Lobby required"); game.config().lives = 1; game.config().save(); }
                case "draw" -> {
                    require(game.stage() == GameController.Stage.ENDING, "Ending after simultaneous eliminations");
                    require(game.server().getPlayerList().getPlayers().stream().noneMatch(p ->
                            p.gameMode.getGameModeForPlayer() == GameType.CREATIVE), "Draw has no creative winner");
                    for (var p : game.server().getPlayerList().getPlayers()) {
                        var prior = baseline.get(p.getUUID());
                        if (prior != null) require(game.players().get(p.getUUID()).wins == prior[2], "Draw credits no wins");
                    }
                }
                case "late" -> {
                    require(game.playing(), "Join during an active match");
                    ServerPlayer late = createBot(game, "TestLate");
                    require(late.gameMode.getGameModeForPlayer() == GameType.SPECTATOR, "Late join is spectator");
                    require(game.lives(late.getUUID()) == 0 && !game.alivePlayers().contains(late), "Late join not enrolled");
                    // FakeClientConnection has no real channel to deliver the disconnect callback.
                    late.connection.onDisconnect(new net.minecraft.network.DisconnectionDetails(
                            me.skywars2.SkyWars2.text("test.disconnect")));
                    require(game.server().getPlayerList().getPlayer(late.getUUID()) == null, "Late-join probe disconnected before the next match");
                }
                case "chests" -> checkChests(game);
                case "bridge" -> {
                    require(game.playing(), "Place a bridge probe during the match");
                    var pos = new net.minecraft.core.BlockPos(250, 70, 250);
                    game.maps().level().setBlock(pos, net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
                    require(game.maps().level().getBlockState(pos).is(net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK), "Bridge probe placed outside template bounds");
                }
                case "cleanup" -> {
                    require(game.inLobby(), "Cleanup finished in lobby");
                    require(game.maps().level().getBlockState(new net.minecraft.core.BlockPos(250, 70, 250))
                            .isAir(), "Bridge probe removed outside original template bounds");
                }
                case "respawn" -> {
                    for (String name : BOT_NAMES) {
                        var bot = game.server().getPlayerList().getPlayer(offlineUuid(name));
                        if (bot != null && bot.isDeadOrDying()) sendRespawnPacket(bot);
                    }
                }
                case "lives" -> startLivesTest(game);
                case "fast" -> setFastConfig(game);
                case "restore" -> restoreFastConfig(game);
                case "watch" -> startWatch(game);
                default -> throw new IllegalArgumentException("Unknown check: " + suite);
            }
            SkyWars2.LOGGER.info("[check] PASS {}", suite);
        } catch (RuntimeException failure) {
            SkyWars2.LOGGER.error("[check] FAIL {}: {}", suite, failure.getMessage());
        }
    }

    /** Called once per server tick after the controller tick. */
    public static void tick(GameController game) {
        if (game == null) return;
        if (deferredCheck != null && --deferredTicks <= 0) {
            Runnable check = deferredCheck;
            deferredCheck = null;
            try { check.run(); }
            catch (RuntimeException failure) { SkyWars2.LOGGER.error("[check] FAIL lobby-rules: {}", failure.getMessage()); }
        }
        if (livesState != null) {
            try { tickLives(game); }
            catch (RuntimeException failure) { failLives(failure.getMessage()); }
        }
        if (watcher != null) {
            try { tickWatch(game); }
            catch (RuntimeException failure) { watchFailure(failure.getMessage()); }
        }
    }

    private static void checkLobbyRules(GameController game) {
        require(game.inLobby(), "Lobby rules require ready lobby");
        var player = game.server().getPlayerList().getPlayers().getFirst();
        player.setRemainingFireTicks(100);
        var arena = game.maps().level();
        var kept = new net.minecraft.world.entity.decoration.ArmorStand(arena, 2, 128, 2);
        kept.entityTags().add("map_object"); arena.addFreshEntity(kept);
        var removed = new net.minecraft.world.entity.decoration.ArmorStand(arena, 3, 128, 2);
        arena.addFreshEntity(removed);
        var overworld = game.server().overworld();
        boolean alreadyForced = overworld.getForceLoadedChunks().contains(net.minecraft.world.level.ChunkPos.pack(0, 0));
        overworld.setChunkForced(0, 0, true);
        overworld.getChunkAt(new net.minecraft.core.BlockPos(0, 128, 0));
        var other = new net.minecraft.world.entity.decoration.ArmorStand(overworld, 0, 128, 0);
        other.entityTags().add("strict_map_object");
        require(overworld.addFreshEntity(other), "Other-dimension probe added");
        // Forced chunks become visible asynchronously, especially on a fresh world.
        deferredTicks = 100;
        deferredCheck = () -> {
            int visibleOther = 0;
            for (var entity : overworld.getAllEntities()) visibleOther++;
            SkyWars2.LOGGER.info("[check] lobby-rules otherWorld={} forced={} visible={} probeLookup={} removed={}",
                    other.level().dimension().identifier(), overworld.getForceLoadedChunks().contains(net.minecraft.world.level.ChunkPos.pack(0, 0)),
                    visibleOther, overworld.getEntity(other.getUUID()) != null, other.isRemoved());
            require(!kept.isRemoved(), "Arena map_object preserved");
            require(removed.isRemoved(), "Untagged arena entity removed");
            require(other.isRemoved(), "Tags do not protect entities in other dimensions");
            require(player.getRemainingFireTicks() <= 0, "Lobby fire extinguished without powder snow");
            kept.discard();
            if (!alreadyForced) overworld.setChunkForced(0, 0, false);
            SkyWars2.LOGGER.info("[check] PASS lobby-rules");
        };
    }

    private static void snapshot(GameController game) {
        baseline.clear();
        for (var player : game.server().getPlayerList().getPlayers()) {
            var p = game.players().get(player.getUUID());
            baseline.put(player.getUUID(), new int[] {p.coins, p.games, p.wins, p.deaths,
                    java.util.Arrays.stream(p.keys).sum()});
        }
    }

    private static int cageBarriers(GameController game) {
        int barriers = 0;
        for (var spawn : game.maps().spawns()) {
            var feet = net.minecraft.core.BlockPos.containing(spawn.position());
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) for (int y = -1; y <= 4; y++)
                if (game.maps().level().getBlockState(feet.offset(x, y, z)).is(net.minecraft.world.level.block.Blocks.BARRIER)) barriers++;
        }
        return barriers;
    }

    private static void createTestBots(GameController game) {
        require(game.inLobby(), "Create vanilla test players in the ready lobby");
        for (String name : BOT_NAMES) {
            ServerPlayer existing = createBot(game, name);
            require(existing != null && existing.getClass() == ServerPlayer.class,
                    name + " must be a vanilla ServerPlayer, not Carpet EntityPlayerMPFake");
        }
        SkyWars2.LOGGER.info("[check] Created TestAlpha/TestBeta as vanilla ServerPlayer instances");
    }

    private static ServerPlayer createBot(GameController game, String name) {
        UUID uuid = offlineUuid(name);
        ServerPlayer existing = game.server().getPlayerList().getPlayer(uuid);
        if (existing != null) return existing;
        GameProfile profile = new GameProfile(uuid, name);
        ServerPlayer player = new ServerPlayer(game.server(), game.maps().level(), profile, ClientInformation.createDefault());
        game.server().getPlayerList().placeNewPlayer(carpetConnection(), player,
                new CommonListenerCookie(profile, 0, player.clientInformation(), false));
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        return game.server().getPlayerList().getPlayer(uuid);
    }

    private static void checkChests(GameController game) {
        require(game.playing(), "Match chests generated");
        int chestCount = 0, stackCount = 0, nonempty = 0;
        for (var entity : game.maps().level().getAllEntities()) {
            if (entity.entityTags().stream().noneMatch(t -> t.equals("swrg.default") || t.equals("swrg.midle") || t.equals("swrg.center"))) continue;
            var block = game.maps().level().getBlockEntity(net.minecraft.core.BlockPos.containing(entity.position()));
            require(block instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity, "Chest marker has native container");
            var container = (net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity) block;
            chestCount++;
            for (int i = 0; i < container.getContainerSize(); i++) {
                var stack = container.getItem(i);
                if (stack.isEmpty()) continue;
                nonempty++;
                if (stack.getCount() > 1) stackCount++;
                var mods = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
                if (mods != null) require(mods.modifiers().stream().noneMatch(m -> m.modifier().id().toString().equals("minecraft:count")), "Synthetic count modifier removed");
            }
        }
        SkyWars2.LOGGER.info("[check] Generated {} chests, {} occupied slots, {} multi-item stacks", chestCount, nonempty, stackCount);
        require(chestCount > 0 && nonempty > 0 && stackCount > 0, "Real loot generated with multiple-item stacks");
    }

    private static UUID offlineUuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    private static Connection carpetConnection() {
        try {
            Class<?> fakeConnection = Class.forName("carpet.patches.FakeClientConnection");
            Constructor<?> constructor = fakeConnection.getDeclaredConstructor(PacketFlow.class);
            constructor.setAccessible(true);
            return (Connection) constructor.newInstance(PacketFlow.SERVERBOUND);
        } catch (ClassNotFoundException missingCarpet) {
            throw new IllegalStateException("Install Carpet to run /sw check bots; it supplies FakeClientConnection.", missingCarpet);
        } catch (ReflectiveOperationException | ClassCastException failure) {
            throw new IllegalStateException("Could not create Carpet FakeClientConnection (PacketFlow.SERVERBOUND).", failure);
        }
    }

    private static void setFastConfig(GameController game) {
        require(game.inLobby(), "Fast test timings can only be set in lobby");
        if (fastConfigSnapshot == null) fastConfigSnapshot = GameConfigSnapshot.from(game.config());
        GameConfig config = game.config();
        config.lives = 2;
        config.countdownSeconds = 5;
        config.stageOneSeconds = 3;
        config.stageTwoSeconds = 6;
        config.endingSeconds = 15;
        config.stageOneEvent = 2; // selection 2 = first phase-one event (speed)
        config.stageTwoEvent = 6; // selection 6 = phase-two shrinking border
        config.save();
        SkyWars2.LOGGER.info("[check] Fast fixture saved: lives=2 countdown=5 phase1=3 phase2=6 ending=15 events=2,6");
    }

    private static void restoreFastConfig(GameController game) {
        require(game.inLobby(), "Restore test timings after match returns to lobby");
        require(fastConfigSnapshot != null, "Run /sw check fast before restore");
        fastConfigSnapshot.apply(game.config());
        game.config().save();
        fastConfigSnapshot = null;
        SkyWars2.LOGGER.info("[check] PASS restore");
    }

    private static void startWatch(GameController game) {
        require(game.inLobby(), "Start the asynchronous watch from lobby before /sw start");
        require(watcher == null, "A watch is already running");
        snapshot(game);
        watcher = new WatchState(game.stage());
        SkyWars2.LOGGER.info("[check] watch armed; start a match now");
    }

    private static void startLivesTest(GameController game) {
        require(game.inLobby(), "Run /sw check lives from lobby to start the extra-lives scenario");
        require(game.config().lives >= 2, "Set /sw check fast first (lives=2)");
        for (String name : BOT_NAMES) {
            ServerPlayer bot = game.server().getPlayerList().getPlayer(offlineUuid(name));
            require(bot != null && bot.getClass() == ServerPlayer.class, "Run /sw check bots first");
        }
        require(game.canStart(null), "Current map must have enough spawn points for all online players");
        watcher = null;
        snapshot(game);
        livesState = new LivesState(offlineUuid(BOT_NAMES[0]));
        game.requestStart(null, false);
        SkyWars2.LOGGER.info("[check] lives scenario armed; waiting for the real match start");
    }

    private static void tickLives(GameController game) {
        LivesState state = livesState;
        if (state == null) return;
        if (++state.waitTicks > 20 * 60) {
            failLives("Timed out in step " + state.step);
            return;
        }
        ServerPlayer player = game.server().getPlayerList().getPlayer(state.victim);
        if (state.step == LivesStep.WAIT_PLAYING) {
            if (!game.playing()) return;
            require(game.lives(state.victim) == 2, "TestAlpha starts with two lives");
            damageByCommand(game, player);
            require(game.lives(state.victim) == 1 && game.awaitingRespawn(state.victim),
                    "First real damage-command death decrements life and awaits respawn");
            state.step = LivesStep.WAIT_FIRST_RESPAWN;
            state.waitTicks = 0;
            return;
        }
        if (player == null) {
            failLives("TestAlpha disconnected during " + state.step);
            return;
        }
        if (state.step == LivesStep.WAIT_FIRST_RESPAWN) {
            if (!game.awaitingRespawn(state.victim) || !player.isDeadOrDying()) return;
            sendRespawnPacket(player);
            ServerPlayer respawned = game.server().getPlayerList().getPlayer(state.victim);
            require(respawned != null && !respawned.isDeadOrDying(), "Serverbound PERFORM_RESPAWN creates the replacement player");
            require(game.lives(state.victim) == 1 && !game.awaitingRespawn(state.victim), "One life remains after real respawn callback");
            require(respawned.gameMode.getGameModeForPlayer() == GameType.SURVIVAL, "Respawned player returns to survival");
            state.step = LivesStep.WAIT_SECOND_DEATH;
            state.waitTicks = 0;
            return;
        }
        if (state.step == LivesStep.WAIT_SECOND_DEATH) {
            if (game.awaitingRespawn(state.victim) || player.isDeadOrDying()) return;
            damageByCommand(game, player);
            require(game.lives(state.victim) == 0 && game.awaitingRespawn(state.victim),
                    "Second real damage-command death eliminates the participant");
            state.step = LivesStep.WAIT_ELIMINATION;
            state.waitTicks = 0;
            return;
        }
        if (state.step == LivesStep.WAIT_ELIMINATION && player.isDeadOrDying()) {
            sendRespawnPacket(player);
            ServerPlayer eliminated = game.server().getPlayerList().getPlayer(state.victim);
            require(eliminated != null && eliminated.gameMode.getGameModeForPlayer() == GameType.SPECTATOR,
                    "Zero-life respawn packet is converted to spectator");
            require(game.lives(state.victim) == 0, "Elimination preserves zero lives");
            livesState = null;
            SkyWars2.LOGGER.info("[check] PASS lives: vanilla packet respawn with one remaining life, then elimination");
        }
    }

    private static void damageByCommand(GameController game, ServerPlayer player) {
        require(player != null, "Damage-test player is online");
        require(game.command("damage " + player.getScoreboardName() + " 100 minecraft:generic_kill"),
                "Vanilla damage command parsed and executed");
    }

    private static void sendRespawnPacket(ServerPlayer deadPlayer) {
        deadPlayer.connection.handleClientCommand(new ServerboundClientCommandPacket(
                ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
        // Vanilla 26.3 waits for this acknowledgement before accepting damage again.
        deadPlayer.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        require(deadPlayer.connection.hasClientLoaded(), "Client load acknowledgement accepted after respawn");
    }

    private static void tickWatch(GameController game) {
        WatchState state = watcher;
        if (state == null) return;
        GameController.Stage current = game.stage();
        if (current == GameController.Stage.LOADING && !state.countdown && !state.ending && game.maps().busy()) {
            if (state.loadingMenuChecks == 0) runWatchCheck("loading-actions", () -> checkLoadingActions(game));
            long started = System.nanoTime();
            if (state.loadingMenuChecks % 10 == 0) {
                runWatchCheck("loading-navigation", () -> {
                    ServerPlayer player = game.server().getPlayerList().getPlayers().getFirst();
                    game.menus().open(player, GameMenus.Page.HUB);
                    click(currentGui(player), 6);
                    require(currentGui(player).getGuiElement(0).getItemStack().is(Items.TRIAL_KEY),
                            "Shop navigation works while map is busy");
                    click(currentGui(player), 18);
                    require(currentGui(player).getGuiElement(0).getItemStack().is(Items.COMPARATOR),
                            "Back navigation works while map is busy");
                });
                state.maxLoadingMenuNanos = Math.max(state.maxLoadingMenuNanos, System.nanoTime() - started);
            }
            state.loadingMenuChecks++;
            if (game.maps().preparing()) state.preparationMenuChecks++;
            else state.placementMenuChecks++;
        }
        if (current != state.lastStage) {
            switch (current) {
                case COUNTDOWN -> {
                    require(!state.countdown && state.lastStage == GameController.Stage.LOADING, "One transition into countdown from loading");
                    runWatchCheck("countdown", () -> {
                        require(state.loadingMenuChecks > 0 && state.placementMenuChecks > 0,
                                "Menus remained available during incremental world placement");
                        require(game.maps().loadingChunkCount() == 0, "Temporary chunk tickets released before countdown");
                        for (ServerPlayer player : game.alivePlayers()) {
                            require(!player.getAbilities().mayfly && !player.getAbilities().flying
                                    && !player.getAbilities().invulnerable, "Temporary loading abilities removed before countdown");
                        }
                        SkyWars2.LOGGER.info("[check] Loading menu ticks={} preparing={} placing={} maxNavigationMs={}",
                                state.loadingMenuChecks, state.preparationMenuChecks, state.placementMenuChecks,
                                state.maxLoadingMenuNanos / 1_000_000.0);
                        require(game.alivePlayers().size() >= 2, "Countdown has enrolled participants");
                        require(cageBarriers(game) > 0, "Countdown has actual barrier cages");
                    });
                    state.countdown = true;
                    if (staleMenu != null) runWatchCheck("stale-menu", () -> checkStaleMenu(game));
                }
                case PLAYING -> {
                    require(state.countdown && !state.playing, "Playing follows countdown exactly once");
                    runWatchCheck("playing", () -> {
                        require(game.alivePlayers().size() >= 2, "Two live participants");
                        for (ServerPlayer p : game.alivePlayers()) {
                            require(p.gameMode.getGameModeForPlayer() == GameType.SURVIVAL, "Survival after countdown");
                            require(p.getHealth() > 0, "Positive health at match start");
                        }
                    });
                    state.playing = true;
                }
                case PHASE_ONE -> {
                    require(state.playing && !state.phaseOne, "Phase one follows play exactly once");
                    state.phaseOne = true;
                }
                case PHASE_TWO -> {
                    require(state.phaseOne && !state.phaseTwo, "Phase two follows phase one exactly once");
                    state.phaseTwo = true;
                }
                case ENDING -> {
                    require(state.playing && !state.ending, "Ending follows the match");
                    require(state.phaseOne && (game.config().stageTwoEvent == 0 || state.phaseTwo), "Timed stages were observed before ending");
                    runWatchCheck("ending", () -> {
                        int winners = 0;
                        for (var p : game.server().getPlayerList().getPlayers()) {
                            var prior = baseline.get(p.getUUID());
                            var now = game.players().get(p.getUUID());
                            if (prior == null || now.wins != prior[2] + 1) continue;
                            winners++;
                            require(now.coins >= prior[0] + 110, "Start and victory coins");
                            require(java.util.Arrays.stream(now.keys).sum() == prior[4] + 1, "One victory key");
                            require(p.gameMode.getGameModeForPlayer() == GameType.CREATIVE, "Winner creative mode");
                        }
                        require(winners == 1, "Exactly one winner credited");
                    });
                    state.ending = true;
                }
                case LOBBY -> {
                    if (state.ending) {
                        runWatchCheck("lobby", () -> require(game.inLobby(), "Return to ready lobby"));
                        if (!state.failed) SkyWars2.LOGGER.info("[check] PASS watch: countdown -> playing -> phases -> ending -> lobby");
                        else SkyWars2.LOGGER.error("[check] FAIL watch: one or more asynchronous assertions failed");
                        watcher = null;
                        return;
                    }
                    if (state.countdown || state.playing) watchFailure("Match returned to lobby before ending");
                }
                default -> { }
            }
            state.lastStage = current;
        }
        if (state.playing && !state.trackerChecked && game.playing()) {
            state.playingTicks++;
            if (game.matchTicks() >= 20) {
                runWatchCheck("tracker", () -> {
                    for (ServerPlayer p : game.alivePlayers()) {
                        boolean found = false;
                        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                            var stack = p.getInventory().getItem(i);
                            if (stack.is(Items.COMPASS) && stack.has(DataComponents.LODESTONE_TRACKER)) found = true;
                        }
                        require(found, "Compass points to a live opponent after one second");
                    }
                });
                state.trackerChecked = true;
            }
        }
    }

    private static void runWatchCheck(String name, Runnable check) {
        try { check.run(); SkyWars2.LOGGER.info("[check] PASS watch/{}", name); }
        catch (RuntimeException failure) { watchFailure(name + ": " + failure.getMessage()); }
    }

    private static void watchFailure(String message) {
        if (watcher != null) watcher.failed = true;
        SkyWars2.LOGGER.error("[check] FAIL watch: {}", message);
    }

    private static void failLives(String message) {
        livesState = null;
        SkyWars2.LOGGER.error("[check] FAIL lives: {}", message);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void checkMenus(GameController game) {
        require(game.inLobby(), "Menu checks require the ready lobby");
        require(game.server().getPlayerList().getPlayers().size() >= 2, "Menu checks require two online players");
        ServerPlayer player = game.server().getPlayerList().getPlayers().getFirst();
        PlayerStore.Profile profile = game.players().get(player.getUUID());

        for (GameMenus.Page page : GameMenus.Page.values()) {
            game.menus().open(player, page);
            SlotBasedGui gui = currentGui(player);
            require(gui instanceof SimpleGui simple && simple.getVirtualSize() == 27, "SGui 9x3 wrapper opened for " + page);
            require(gui.getLockPlayerInventory(), "Player inventory locked on " + page);
            require(gui.isOpen(), "SGui reports open for " + page);
            checkClientTranslation(((SimpleGui) gui).getTitle());
            for (int slot = 0; slot < 27; slot++) {
                var element = gui.getGuiElement(slot);
                if (element == null) continue;
                checkClientTranslation(element.getItemStack().getHoverName());
                var lore = element.getItemStack().get(DataComponents.LORE);
                if (lore != null) for (Component line : lore.lines()) checkClientTranslation(line);
            }
        }
        checkClientTranslation(game.status());
        checkClientTranslation(game.settingsSummary());
        checkClientTranslation(SkyWars2.text("bossbar.countdown", 5));

        game.menus().open(player, GameMenus.Page.HUB);
        SlotBasedGui hub = currentGui(player);
        requireItem(hub, 0, Items.COMPARATOR, "Original settings comparator at slot 0");
        click(hub, 0);
        SlotBasedGui settings = currentGui(player);
        requireItem(settings, 24, Items.COOKED_PORKCHOP, "Original health setting at slot 24");
        int previousHealth = game.config().health;
        int expectedHealth = previousHealth < 2 ? 2 : previousHealth < 10 ? 10 : previousHealth < 20 ? 20 : previousHealth < 40 ? 40 : previousHealth < 60 ? 60 : 2;
        try {
            click(settings, 24);
            require(game.config().health == expectedHealth, "Health setting callback updates current config");
            require(GameConfig.load().health == expectedHealth, "Health setting callback persists config");
        } finally {
            game.config().health = previousHealth;
            game.config().save();
        }

        game.menus().open(player, GameMenus.Page.VANILLA_KITS);
        var kitOne = currentGui(player).getGuiElement(0).getItemStack();
        Component kitName = kitOne.getHoverName();
        translation(kitName, "swrg.kit.a1");
        requireLegacyStyle(kitName, net.minecraft.ChatFormatting.AQUA, "Legacy kit name style");
        var kitLore = kitOne.get(DataComponents.LORE);
        require(kitLore != null && kitLore.lines().size() == 3, "Legacy kit lore has exactly three original rows");
        String[] kitLoreKeys = {"swrg.lore.kit.a1.0", "swrg.lore.kit.a1.1", "swrg.lore.kit.a1.2"};
        for (int row = 0; row < kitLoreKeys.length; row++) {
            Component line = kitLore.lines().get(row);
            translation(line, kitLoreKeys[row]);
            requireLegacyStyle(line, net.minecraft.ChatFormatting.GRAY, "Legacy kit lore row " + row);
        }

        game.menus().open(player, GameMenus.Page.VANILLA_SKILLS);
        Component spiderSkill = currentGui(player).getGuiElement(0).getItemStack().getHoverName();
        translation(spiderSkill, "swrg.skill.a1");
        requireLegacyStyle(spiderSkill, net.minecraft.ChatFormatting.AQUA, "Legacy skill name style");
        var spiderLore = currentGui(player).getGuiElement(0).getItemStack().get(DataComponents.LORE);
        require(spiderLore != null && spiderLore.lines().size() == 1, "Legacy skill lore has exactly its original description row");
        translation(spiderLore.lines().getFirst(), "swrg.lore.skill.a1.0");
        requireLegacyStyle(spiderLore.lines().getFirst(), net.minecraft.ChatFormatting.GRAY, "Legacy skill lore style");

        var windSkill = currentGui(player).getGuiElement(11).getItemStack();
        Component windName = windSkill.getHoverName();
        translation(windName, "wind_runner_skill");
        requireLegacyStyle(windName, net.minecraft.ChatFormatting.WHITE, "Legacy Wind Wings name style");
        var windLore = windSkill.get(DataComponents.LORE);
        require(windLore != null && windLore.lines().size() == 1, "Legacy Wind Wings lore has exactly its original description row");
        translation(windLore.lines().getFirst(), "wind_runner_skill.lore.1");
        requireLegacyStyle(windLore.lines().getFirst(), net.minecraft.ChatFormatting.GRAY, "Legacy Wind Wings lore style");

        var dualist = currentGui(player).getGuiElement(12).getItemStack();
        translation(dualist.getHoverName(), "dualist_skill");
        requireLegacyStyle(dualist.getHoverName(), net.minecraft.ChatFormatting.WHITE, "Legacy Vex skill name style");
        var dualistLore = dualist.get(DataComponents.LORE);
        require(dualistLore != null && dualistLore.lines().size() == 1, "Legacy Vex skill lore has exactly its original description row");
        translation(dualistLore.lines().getFirst(), "dualist_skill.lore");
        requireLegacyStyle(dualistLore.lines().getFirst(), net.minecraft.ChatFormatting.GRAY, "Legacy Vex skill lore style");

        int previousCoins = profile.coins;
        int[] previousKeys = profile.keys.clone();
        try {
            profile.coins = 1000;
            game.players().save();
            game.menus().open(player, GameMenus.Page.KEY_SHOP);
            SlotBasedGui shop = currentGui(player);
            int[][] costs = {{100, 400, 700}, {150, 600, 1050}, {150, 600, 1050}, {200, 800, 1400}, {100, 400, 750}};
            for (int key = 0; key < 5; key++) {
                for (int tier = 0; tier < 3; tier++) {
                    int slot = key * 3 + tier;
                    var stack = shop.getGuiElement(slot).getItemStack();
                    require(stack.is(key == 4 ? Items.GOLD_NUGGET : Items.TRIAL_KEY), "Original key shop material at slot " + slot);
                    var lore = stack.get(DataComponents.LORE);
                    require(lore != null && lore.lines().size() == 3, "Original quantity, price and balance lore at slot " + slot);
                    checkClientTranslation(stack.getHoverName());
                    for (var line : lore.lines()) checkClientTranslation(line);
                    int beforeCoins = profile.coins, beforeMemcoins = profile.memcoins, beforeKeys = profile.keys[key];
                    profile.coins = 10000; profile.memcoins = 10000;
                    game.menus().open(player, GameMenus.Page.KEY_SHOP);
                    click(currentGui(player), slot);
                    require((key == 4 ? profile.memcoins : profile.coins) == 10000 - costs[key][tier], "Source key price at slot " + slot);
                    require(profile.keys[key] == beforeKeys + (tier == 0 ? 1 : tier == 1 ? 5 : 10), "Source key quantity at slot " + slot);
                    profile.coins = beforeCoins; profile.memcoins = beforeMemcoins; profile.keys[key] = beforeKeys;
                }
            }
            game.menus().open(player, GameMenus.Page.KEY_SHOP);
            shop = currentGui(player);

            click(shop, 0);
            require(profile.coins == 900, "One key click charges exactly 100 coins");
            require(profile.keys[0] == previousKeys[0] + 1, "One key click grants exactly one key");
            PlayerStore.Profile persisted = PlayerStore.load().get(player.getUUID());
            require(persisted.coins == 900 && persisted.keys[0] == previousKeys[0] + 1, "Key purchase is persisted");

            stalePlayer = player;
            staleMenu = currentGui(player);
            require(staleMenu.getGuiElement(0) != null, "Keep real keyshop callback for stale-menu suite");
        } finally {
            profile.coins = previousCoins;
            System.arraycopy(previousKeys, 0, profile.keys, 0, profile.keys.length);
            game.players().save();
        }
    }

    private static void checkStaleMenu(GameController game) {
        require(game.stage() == GameController.Stage.COUNTDOWN, "Stale menu check requires countdown stage");
        require(staleMenu != null && stalePlayer != null, "Run /skywars check menus in the lobby first");
        require(game.server().getPlayerList().getPlayer(stalePlayer.getUUID()) != null, "Stale-menu test player remains online");
        PlayerStore.Profile profile = game.players().get(stalePlayer.getUUID());
        int coins = profile.coins;
        int[] keys = profile.keys.clone();
        click(staleMenu, 0);
        require(profile.coins == coins, "Stale keyshop callback cannot charge during countdown");
        require(java.util.Arrays.equals(profile.keys, keys), "Stale keyshop callback cannot grant keys during countdown");
        staleMenu = null;
        stalePlayer = null;
    }

    private static void checkLoadingActions(GameController game) {
        require(game.stage() == GameController.Stage.LOADING && game.canUseMenus(), "Loading permits menus");
        ServerPlayer player = game.server().getPlayerList().getPlayers().getFirst();
        require(player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE,
                "Vanilla client can send compass use-item packets during loading");
        require(player.getAbilities().flying && player.getAbilities().invulnerable,
                "Player protected above the arena during loading");
        if (staleMenu != null) {
            require(currentGui(stalePlayer) == staleMenu && staleMenu.isOpen(), "Starting loading keeps the existing menu open");
        }
        for (GameMenus.Page page : GameMenus.Page.values()) {
            game.menus().open(player, page);
            require(currentGui(player).isOpen(), "Menu page opens during loading: " + page);
        }
        int health = game.config().health;
        String mapId = game.config().mapId;
        game.menus().open(player, GameMenus.Page.SETTINGS);
        click(currentGui(player), 24);
        require(game.config().health == health, "Settings cannot mutate during loading");
        click(currentGui(player), 26);
        require(game.config().health == health && game.config().mapId.equals(mapId), "Settings reset cannot mutate during loading");
        require(!game.selectMap(player, "3", false) && game.config().mapId.equals(mapId), "Changing map is rejected while loading");
        game.requestStart(player, false);
        require(game.stage() == GameController.Stage.LOADING && game.maps().busy(), "Duplicate start does not replace the loading job");
        var profile = game.players().get(player.getUUID());
        int coins = profile.coins;
        int[] keys = profile.keys.clone();
        try {
            profile.coins = 1000;
            game.menus().open(player, GameMenus.Page.KEY_SHOP);
            click(currentGui(player), 0);
            require(profile.coins == 900 && profile.keys[0] == keys[0] + 1, "Purchase callback works while loading");
        } finally {
            profile.coins = coins;
            System.arraycopy(keys, 0, profile.keys, 0, keys.length);
            game.players().save();
        }
        player.getInventory().setSelectedSlot(GameController.MENU_COMPASS_SLOT);
        require(player.getMainHandItem().is(Items.COMPASS), "Loading keeps the menu compass");
        player.connection.handleUseItem(new net.minecraft.network.protocol.game.ServerboundUseItemPacket(
                net.minecraft.world.InteractionHand.MAIN_HAND, 0, player.getYRot(), player.getXRot()));
        require(currentGui(player).getGuiElement(0).getItemStack().is(Items.COMPARATOR),
                "Native compass packet opens the main menu during loading");
    }

    private static void inventoryClick(ServerPlayer player, int slot, int button, ContainerInput input) {
        var menu = player.containerMenu;
        player.connection.handleContainerClick(new net.minecraft.network.protocol.game.ServerboundContainerClickPacket(
                menu.containerId, menu.getStateId(), (short) slot, (byte) button, input,
                new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>(), net.minecraft.network.HashedStack.EMPTY));
    }

    private static void exportMenuDesign(GameController game) {
        require(game.canUseMenus(), "Menu design export requires lobby/loading");
        var player = game.server().getPlayerList().getPlayers().getFirst();
        var output = new com.google.gson.JsonObject();
        var pages = new com.google.gson.JsonArray();
        var ops = player.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        for (var page : GameMenus.Page.values()) {
            game.menus().open(player, page);
            var gui = (SimpleGui) currentGui(player);
            var entry = new com.google.gson.JsonObject();
            entry.addProperty("page", page.name());
            entry.addProperty("size", gui.getVirtualSize());
            entry.add("title", ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, gui.getTitle()).getOrThrow());
            var slots = new com.google.gson.JsonArray();
            for (int slot = 0; slot < gui.getVirtualSize(); slot++) {
                var element = gui.getGuiElement(slot);
                if (element == null || element.getItemStack().isEmpty()) continue;
                var button = new com.google.gson.JsonObject();
                button.addProperty("slot", slot);
                button.add("item", net.minecraft.world.item.ItemStack.CODEC.encodeStart(ops, element.getItemStack()).getOrThrow());
                slots.add(button);
            }
            entry.add("slots", slots); pages.add(entry);
        }
        output.add("pages", pages);
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("menu-design-snapshot.json"),
                    new com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(output),
                    StandardCharsets.UTF_8);
        } catch (java.io.IOException error) { throw new IllegalStateException("Cannot export menu design", error); }
        game.menus().open(player);
    }

    private static void requireLobbyInventory(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            require(slot == GameController.MENU_COMPASS_SLOT ? stack.is(Items.COMPASS) && stack.getCount() == 1 : stack.isEmpty(),
                    "Fixed lobby inventory slot " + slot);
        }
        require(player.containerMenu.getCarried().isEmpty(), "Lobby cursor remains empty");
    }

    private static void checkLobbyInventory(GameController game) {
        require(game.inventoryLocked(), "Lobby/loading inventory lock is active");
        ServerPlayer player = game.server().getPlayerList().getPlayer(offlineUuid("TestAlpha"));
        require(player != null, "Create bots first");
        player.closeContainer();
        player.getInventory().setSelectedSlot(GameController.MENU_COMPASS_SLOT);
        requireLobbyInventory(player);
        int compassSlot = 36 + GameController.MENU_COMPASS_SLOT;
        for (ContainerInput input : ContainerInput.values()) {
            inventoryClick(player, compassSlot, 0, input);
            requireLobbyInventory(player);
        }
        for (var action : new net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action[] {
                net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.DROP_ITEM,
                net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS,
                net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND}) {
            player.connection.handlePlayerAction(new net.minecraft.network.protocol.game.ServerboundPlayerActionPacket(
                    action, net.minecraft.core.BlockPos.ZERO, net.minecraft.core.Direction.DOWN));
            requireLobbyInventory(player);
        }
        var item = new net.minecraft.world.entity.item.ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(),
                new net.minecraft.world.item.ItemStack(Items.DIAMOND));
        item.setNoPickUpDelay(); item.playerTouch(player);
        require(item.getItem().getCount() == 1 && !item.isRemoved(), "Dropped item is not picked up in lobby/loading");
        requireLobbyInventory(player); item.discard();

        GameType mode = player.gameMode.getGameModeForPlayer();
        try {
            player.setGameMode(GameType.CREATIVE);
            player.connection.handleSetCreativeModeSlot(new net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket(
                    compassSlot, new net.minecraft.world.item.ItemStack(Items.DIAMOND)));
            requireLobbyInventory(player);
        } finally { player.setGameMode(mode); }

        // A real vanilla container must not become a loophole for moving items.
        var storage = new net.minecraft.world.SimpleContainer(27);
        storage.setItem(0, new net.minecraft.world.item.ItemStack(Items.DIAMOND));
        player.containerMenu = net.minecraft.world.inventory.ChestMenu.threeRows(99, player.getInventory(), storage);
        try {
            inventoryClick(player, 0, 0, ContainerInput.QUICK_MOVE);
            require(storage.getItem(0).is(Items.DIAMOND), "Vanilla chest transfer is blocked");
            requireLobbyInventory(player);
        } finally { player.containerMenu = player.inventoryMenu; }

        var profile = game.players().get(player.getUUID());
        int coins = profile.coins; int[] keys = profile.keys.clone();
        try {
            profile.coins = 1000;
            game.menus().open(player, GameMenus.Page.KEY_SHOP);
            inventoryClick(player, currentGui(player).getSize() + 27 + GameController.MENU_COMPASS_SLOT, 0, ContainerInput.QUICK_MOVE);
            requireLobbyInventory(player);
            inventoryClick(player, 0, 0, ContainerInput.PICKUP);
            require(profile.coins == 900 && profile.keys[0] == keys[0] + 1, "Native SGui purchase packet still works");
            requireLobbyInventory(player);
        } finally {
            profile.coins = coins; System.arraycopy(keys, 0, profile.keys, 0, keys.length);
            game.players().save(); player.closeContainer();
        }
    }

    private static void checkUnlockedInventory(GameController game) {
        require(game.playing() && !game.inventoryLocked(), "Inventory lock released for the match");
        ServerPlayer player = game.alivePlayers().getFirst();
        player.closeContainer();
        var saved = player.getInventory().getItem(0).copy();
        try {
            player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(Items.DIAMOND));
            inventoryClick(player, 36, 0, ContainerInput.PICKUP);
            require(player.getInventory().getItem(0).isEmpty() && player.containerMenu.getCarried().is(Items.DIAMOND),
                    "Native inventory move works during a match");
            player.containerMenu.setCarried(net.minecraft.world.item.ItemStack.EMPTY);
            var item = new net.minecraft.world.entity.item.ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(),
                    new net.minecraft.world.item.ItemStack(Items.DIAMOND));
            item.setNoPickUpDelay(); item.playerTouch(player);
            require(item.isRemoved() && player.getInventory().getItem(0).is(Items.DIAMOND), "Item pickup works during a match");
        } finally {
            player.containerMenu.setCarried(net.minecraft.world.item.ItemStack.EMPTY);
            player.getInventory().setItem(0, saved); player.inventoryMenu.sendAllDataToRemote();
        }
    }

    private static SlotBasedGui currentGui(ServerPlayer player) {
        var gui = SguiUtils.getCurrentGui(player);
        require(gui instanceof SlotBasedGui, "Current container is a real SGui slot wrapper");
        return (SlotBasedGui) gui;
    }

    private static TranslatableContents translation(Component component, String key) {
        require(component.getContents() instanceof TranslatableContents, "Client receives a translation component: " + key);
        var contents = (TranslatableContents) component.getContents();
        require(contents.getKey().equals(key), "Translation key preserved: " + key);
        return contents;
    }

    private static void requireLegacyStyle(
            Component component, net.minecraft.ChatFormatting color, String message) {
        var style = component.getStyle();
        require(
                java.util.Objects.equals(
                        style.getColor(), net.minecraft.network.chat.TextColor.fromLegacyFormat(color)),
                message + " color");
        require(Boolean.FALSE.equals(style.isItalic()), message + " italic=false");
    }

    private static void checkClientTranslation(Component component) {
        require(containsTranslation(component) || component.getString().matches("[\\s\\d\\p{Punct}]*"),
                "Custom text retains client translations: " + component);
        var encoded = ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, component).getOrThrow();
        Component decoded = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        var encodedAgain = ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, decoded).getOrThrow();
        // The decoder normalizes Java numeric argument types; compare their wire representation.
        require(encodedAgain.equals(encoded), "Component codec preserves translation keys, arguments and styles: " + encoded);
    }

    private static boolean containsTranslation(Component component) {
        if (component.getContents() instanceof TranslatableContents) return true;
        return component.getSiblings().stream().anyMatch(GameChecks::containsTranslation);
    }

    private static void requireItem(SlotBasedGui gui, int slot, net.minecraft.world.item.Item item, String message) {
        var element = gui.getGuiElement(slot);
        require(element != null && element.getItemStack().is(item), message);
    }

    private static void click(SlotBasedGui gui, int slot) {
        gui.click(slot, ClickType.MOUSE_LEFT, ContainerInput.PICKUP);
    }
}
