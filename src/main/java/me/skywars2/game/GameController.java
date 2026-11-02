package me.skywars2.game;

import static me.skywars2.SkyWars2.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import me.skywars2.SkyWars2;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.BossEvent;

/** One owner of the match. All methods are called on the server thread. */
public final class GameController {
    public static final int MENU_COMPASS_SLOT = 4;
    public enum Stage { LOBBY, LOADING, COUNTDOWN, PLAYING, PHASE_ONE, PHASE_TWO, ENDING }

    private static final class Participant {
        MapManager.Spawn spawn;
        int lives;
        boolean awaitingRespawn;
        boolean disconnected;
        Participant(MapManager.Spawn spawn, int lives) { this.spawn = spawn; this.lives = lives; }
    }

    /** Core values only; native bodies, entities and ability queues have their own restore phase. */
    static final class Checkpoint {
        private final GameController owner;
        private final String epoch;
        private final Map<UUID, Participant> members = new LinkedHashMap<>();
        private final Set<UUID> pending;
        private final List<Entity> unloads;
        private final Stage stage;
        private final UUID host,pendingStarter,victoryWinner;
        private final int countdownTicks,gameTicks,finishTicks,lastAliveTicks,lifetimeTicks,lastCountdownTitleSeconds;
        private final boolean lobbyReady,stopped,fallEnabled,readyBroken,preparingSelection,startAfterSelection;
        private final Component mapFailure;
        private final String readyMap;
        private final long selectionVersion;
        private final net.minecraft.nbt.Tag barName;
        private final BossEvent.BossBarColor color;
        private final BossEvent.BossBarOverlay overlay;
        private final float progress;
        private final boolean visible,darken,music,fog;
        private final List<UUID> barMembers;

        private Checkpoint(GameController g) {
            owner=g;epoch=g.items.epoch();
            g.participants.forEach((id,p)->members.put(id,copy(p)));
            pending=Set.copyOf(g.pendingLobby);unloads=List.copyOf(g.nativeEntityUnloads);
            stage=g.stage;host=g.host;pendingStarter=g.pendingStarter;victoryWinner=g.victoryWinner;
            countdownTicks=g.countdownTicks;gameTicks=g.gameTicks;finishTicks=g.finishTicks;lastAliveTicks=g.lastAliveTicks;
            lifetimeTicks=g.lifetimeTicks;lastCountdownTitleSeconds=g.lastCountdownTitleSeconds;
            lobbyReady=g.lobbyReady;stopped=g.stopped;fallEnabled=g.fallEnabled;readyBroken=g.readyBroken;
            preparingSelection=g.preparingSelection;startAfterSelection=g.startAfterSelection;
            mapFailure=g.mapFailure.copy();readyMap=g.readyMap;selectionVersion=g.selectionVersion;
            var ops=g.server.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
            barName=net.minecraft.network.chat.ComponentSerialization.CODEC.encodeStart(ops,g.bar.getName()).getOrThrow().copy();
            color=g.bar.getColor();overlay=g.bar.getOverlay();progress=g.bar.getProgress();visible=g.bar.isVisible();
            darken=g.bar.shouldDarkenScreen();music=g.bar.shouldPlayBossMusic();fog=g.bar.shouldCreateWorldFog();
            barMembers=g.bar.getPlayers().stream().map(ServerPlayer::getUUID).toList();
        }
        private static Participant copy(Participant source) {
            var result=new Participant(source.spawn,source.lives);
            result.awaitingRespawn=source.awaitingRespawn;result.disconnected=source.disconnected;return result;
        }
    }

    Checkpoint checkpointCore() {
        if(!server.isSameThread()||!playing()||stopped||maps.busy())throw new IllegalStateException("Capture a stable active match on its server thread");
        return new Checkpoint(this);
    }

    void validateCheckpoint(Checkpoint image) {
        if(!server.isSameThread()||!server.tickRateManager().isFrozen()||image.owner!=this||!items.currentEpoch(image.epoch)
                ||stopped||maps.busy())throw new IllegalStateException("Restore the original active match on a frozen server thread");
    }

    void restoreCore(Checkpoint image) {
        validateCheckpoint(image);
        // Unsupported short restart intent is cancelled by rewind, as other transient actions are.
        runeRestartPending=false;
        items.containedSkills().stop();
        items.combatRewards().clear();items.machinegunSkill().clearTransient();items.zeusSkill().clearTransient();
        participants.clear();image.members.forEach((id,p)->participants.put(id,Checkpoint.copy(p)));
        pendingLobby.clear();pendingLobby.addAll(image.pending);nativeEntityUnloads.clear();nativeEntityUnloads.addAll(image.unloads);
        stage=image.stage;host=image.host;pendingStarter=image.pendingStarter;victoryWinner=image.victoryWinner;
        countdownTicks=image.countdownTicks;gameTicks=image.gameTicks;finishTicks=image.finishTicks;lastAliveTicks=image.lastAliveTicks;
        lifetimeTicks=image.lifetimeTicks;lastCountdownTitleSeconds=image.lastCountdownTitleSeconds;
        lobbyReady=image.lobbyReady;stopped=image.stopped;fallEnabled=image.fallEnabled;readyBroken=image.readyBroken;
        preparingSelection=image.preparingSelection;startAfterSelection=image.startAfterSelection;
        mapFailure=image.mapFailure.copy();readyMap=image.readyMap;selectionVersion=image.selectionVersion;
        var ops=server.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        bar.removeAllPlayers();bar.setName(net.minecraft.network.chat.ComponentSerialization.CODEC.parse(ops,image.barName.copy()).getOrThrow());
        bar.setColor(image.color);bar.setOverlay(image.overlay);bar.setProgress(image.progress);bar.setVisible(image.visible);
        bar.setDarkenScreen(image.darken);bar.setPlayBossMusic(image.music);bar.setCreateWorldFog(image.fog);
        for(var id:image.barMembers){var player=server.getPlayerList().getPlayer(id);if(player!=null)bar.addPlayer(player);}
    }

    private final MinecraftServer server;
    private final GameConfig config;
    private final PlayerStore players;
    private final MapManager maps;
    private final MatchEvents events;
    private final LbcLoadouts lbc;
    private final LbcItems items;
    private final LbcVanityRollback vanity=new LbcVanityRollback(this);
    public LbcVanityRollback vanity(){return vanity;}
    void reconcileRollbackParticipants(Set<UUID> absent,Set<UUID> captured){
        for(var entry:participants.entrySet())if(captured.contains(entry.getKey()))
            entry.getValue().disconnected=absent.contains(entry.getKey());
        for(var player:server.getPlayerList().getPlayers())if(!captured.contains(player.getUUID())){
            player.getInventory().clearContent();player.removeAllEffects();spectators.enter(player);
        }
    }
    void rejoinRollbackParticipant(ServerPlayer player,boolean observer){
        var member=participants.get(player.getUUID());
        if(member!=null)member.disconnected=false;
        if(observer||member==null||member.lives<=0)spectators.enter(player);
        else {bar.addPlayer(player);teamColors.rejoin(player);}
        items.witchClaims().reconcile(player);
    }
    private final GameLuckyModifiers luckyModifiers;
    private final Spectators spectators;
    private final AttributeHud attributeHud;
    private final DeveloperTools developerTools;
    private final ZoneBarrier zoneBarrier;
    private final LbcTeamColors teamColors;
    private final ManualVisualTracking visualTracking=new ManualVisualTracking(this);
    private final ItemStack lobbyCompass = menuCompass();
    private final Map<UUID, Participant> participants = new LinkedHashMap<>();
    private final Set<UUID> pendingLobby = new java.util.HashSet<>();
    private final Set<Entity> nativeEntityUnloads = Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private final ServerBossEvent bar = new ServerBossEvent(UUID.randomUUID(), text("game.title"),
            BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
    private GameMenus menus;
    private Stage stage = Stage.LOADING;
    private UUID host;
    private int countdownTicks;
    private int gameTicks;
    private int finishTicks;
    private int lastAliveTicks = 5;
    private int lifetimeTicks;
    private int lastCountdownTitleSeconds = -1;
    private boolean lobbyReady;
    private boolean stopped;
    private boolean fallEnabled;
    private Component mapFailure = Component.empty();
    private String readyMap;
    private boolean runeRestartPending;
    private boolean readyBroken;
    private long selectionVersion;
    private boolean preparingSelection;
    private boolean startAfterSelection;
    private UUID pendingStarter;
    private UUID victoryWinner;

    public GameController(MinecraftServer server, GameConfig config, PlayerStore players, MapManager maps) {
        this.server = server; this.config = config; this.players = players; this.maps = maps;
        this.teamColors = new LbcTeamColors(server.getScoreboard());
        this.zoneBarrier = new ZoneBarrier(this);
        this.events = new MatchEvents(this, maps, config, players);
        this.lbc = new LbcLoadouts(this, config, players);
        this.items = new LbcItems(this);
        this.luckyModifiers = new GameLuckyModifiers(this);
        this.lbc.syncRanked();
        this.spectators = new Spectators(this);
        this.attributeHud = new AttributeHud(this);
        this.developerTools = new DeveloperTools(this);
    }

    public void menus(GameMenus menus) { this.menus = menus; }
    public MinecraftServer server() { return server; }
    public ManualVisualTracking visualTracking() { return visualTracking; }
    public Stage stage() { return stage; }
    public UUID host() { return host; }
    public boolean inLobby() { return stage == Stage.LOBBY && lobbyReady; }
    public boolean canUseMenus() { return !stopped && (inLobby() || stage == Stage.LOADING); }
    public boolean canUseMenus(ServerPlayer player) { return canUseMenus() || !stopped && (isObserver(player)
            || menus != null && menus.debugAccess(player)); }
    public static boolean operator(ServerPlayer player) {
        return net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS)
                .test(player.createCommandSourceStack());
    }
    public boolean debugPerks(ServerPlayer player) { return operator(player) && player.entityTags().contains("skywars2.debug_perks"); }
    public DeveloperTools developerTools() { return developerTools; }
    public boolean developerMode() { return config.developerMode; }
    public void developerMode(ServerPlayer player, boolean enabled) {
        if (!operator(player)) return;
        config.developerMode = enabled; config.save(); lastAliveTicks = 5;
        if (enabled) {
            player.entityTags().add("developer");
            if (playing() && isObserver(player)) returnFromGameModeCommand(player, GameType.CREATIVE);
            player.setGameMode(GameType.CREATIVE);
            for (var member : participants.values()) if (!member.disconnected) member.lives = Math.max(1, member.lives);
        } else player.entityTags().remove("developer");
        player.sendSystemMessage(Component.literal("Developer mode: " + (enabled ? "ON" : "OFF")));
    }
    public boolean isDeveloper(ServerPlayer player) { return player.entityTags().contains("developer"); }
    public boolean isObserver(ServerPlayer player) { return spectators.contains(player); }
    public Spectators spectators() { return spectators; }
    public boolean inventoryLocked() { return canUseMenus(); }
    public boolean inventoryLocked(ServerPlayer player) {
        return isObserver(player) || inventoryLocked() && !isDeveloper(player);
    }
    /** Item abilities can be tested by tagged developers in the lobby, but observer inventory stays powerless. */
    public boolean canUseGameplayItems(ServerPlayer player) {
        return !isObserver(player) && (allowDamage(player) || isDeveloper(player) && !stopped
                && (inLobby() || stage == Stage.LOADING));
    }
    public boolean selectedMapReady() { return readyMap != null && readyMap.equals(config.mapId) && readyBroken == config.broken && !maps.busy(); }
    public boolean playing() { return stage == Stage.PLAYING || stage == Stage.PHASE_ONE || stage == Stage.PHASE_TWO; }
    public int matchTicks() { return gameTicks; }
    public ZoneBarrier zoneBarrier() { return zoneBarrier; }
    public int lives(UUID id) { Participant p = participants.get(id); return p == null ? 0 : p.lives; }
    public boolean awaitingRespawn(UUID id) { Participant p = participants.get(id); return p != null && p.awaitingRespawn; }
    public MapManager maps() { return maps; }
    public GameConfig config() { return config; }
    public PlayerStore players() { return players; }
    public GameMenus menus() { return menus; }
    public LbcLoadouts lbc() { return lbc; }
    public LbcItems items() { return items; }
    public GameLuckyModifiers luckyModifiers() { return luckyModifiers; }
    MatchEvents events() { return events; }
    public AttributeHud attributeHud() { return attributeHud; }
    LbcTeamColors teamColors() { return teamColors; }

    public Component status() {
        return text("game.status", text("stage." + stage.name().toLowerCase(java.util.Locale.ROOT)),
                maps.info(config.mapId) == null ? text("game.map_unknown", config.mapId) : maps.info(config.mapId).displayName(),
                text(maps.busy() ? "common.enabled" : "common.disabled"), alivePlayers().size(),
                participants.size(), gameTicks, text(lobbyReady ? "common.enabled" : "common.disabled"), mapFailure);
    }

    public void startServer() {
        teamColors.ensurePalette();
        if (maps.level() == null) {
            mapFailure = text("map.error.dimension");
            SkyWars2.LOGGER.error("{}", mapFailure.getString()); return;
        }
        var rules = maps.level().getGameRules();
        rules.set(GameRules.IMMEDIATE_RESPAWN, true, server);
        rules.set(GameRules.SPAWN_MOBS, false, server);
        rules.set(GameRules.SPAWN_MONSTERS, false, server);
        rules.set(GameRules.ADVANCE_TIME, false, server);
        rules.set(GameRules.ADVANCE_WEATHER, false, server);
        rules.set(GameRules.RESPAWN_RADIUS, 0, server);
        maps.level().setRespawnData(LevelData.RespawnData.of(maps.level().dimension(),
                net.minecraft.core.BlockPos.containing(maps.lobbyPosition()), 0, 0));
        restoreLobby();
    }

    public void join(ServerPlayer player) {
        if(vanity.joined(player))return;
        items.witchClaims().reconcile(player);
        items.rifleZoom().joined(player);
        items.fpvDrone().forget(player);
        players.get(player.getUUID()).username = player.getScoreboardName();
        if (host == null || server.getPlayerList().getPlayer(host) == null) host = player.getUUID();
        // Joining during a match never silently enrolls a new participant.
        if (stage == Stage.LOBBY && lobbyReady) lobbyPlayer(player);
        else if (stage == Stage.LOADING) {
            pendingLobby.add(player.getUUID());
            loadingPlayer(player);
        }
        else {
            spectators.enter(player);
            pendingLobby.add(player.getUUID());
            if (maps.level() != null) teleport(player, maps.look(), 0, 0);
        }
        zoneBarrier.playerJoined(player);
        lbc.joined(player);
        items.stand().joined(player);
    }

    public void leave(ServerPlayer player) {
        vanity.leaving(player);
        if(vanity.busy())return;
        items.witchClaims().leavingOrDying(player);
        // При отключении снимаем временные бонусы, отмечаем участника выбывшим и сохраняем профиль.
        attributeHud.clear(player);
        teamColors.leave(player);
        items.forget(player);
        lbc.clear(player);
        lbc.left(player);
        events.removePlayerSkill(player);
        spectators.remove(player);
        pendingLobby.remove(player.getUUID());
        Participant p = participants.get(player.getUUID());
        if (p != null) { p.lives = 0; p.disconnected = true; p.awaitingRespawn = false; }
        bar.removePlayer(player);
        if (player.getUUID().equals(host)) host = server.getPlayerList().getPlayers().stream()
                .filter(other -> !other.getUUID().equals(player.getUUID())).map(ServerPlayer::getUUID).findFirst().orElse(null);
        players.save();
    }

    public boolean canStart(ServerPlayer player) {
        if (startFailure(player) != null || !selectedMapReady()) return false;
        MapManager.MapInfo map = maps.info(config.mapId);
        return map != null && map.available() && map.playerCapacity() >= online().size();
    }

    /** The legacy admin tag reserves shared controls only while its holder is online; OP is unrelated. */
    public boolean mayControlMatch(ServerPlayer player) {
        if (player == null) return true; // Console and internal server actions.
        if (isObserver(player)) return false;
        if (player.entityTags().contains("admin")) return true;
        return server.getPlayerList().getPlayers().stream().noneMatch(p -> p.entityTags().contains("admin"));
    }

    /** Recheck at the actual action: tags or online players may change while a menu stays open. */
    public boolean allowMatchControl(ServerPlayer player) {
        if (mayControlMatch(player)) return true;
        tell(player, text("game.admin_only"));
        return false;
    }

    /** Separate unsupported modes from map preparation, so their rejection is actionable. */
    private Component startFailure(ServerPlayer player) {
        if (stopped || !inLobby() || developerTools.authoring().busy() || maps.busy() && !preparingSelection) return text("game.start_unavailable");
        if (!mayControlMatch(player)) return text("game.admin_only");
        if (config.gameMode != 1) return Component.translatable("skywars2.game.mode_unavailable", config.gameMode);
        if (online().size() < (developerMode() ? 1 : 2)) return text("game.start_requirements");
        return null;
    }

    public boolean selectMap(ServerPlayer player, String id, boolean broken) {
        if (!allowMatchControl(player)) return false;
        return selectMapAuthorized(player, id, broken);
    }

    private boolean selectMapAuthorized(ServerPlayer player, String id, boolean broken) {
        if (!inLobby() || developerTools.authoring().busy()) return false;
        MapManager.MapInfo map = maps.info(id);
        if (map == null || !map.available()) {
            tell(player, map == null ? text("game.map_unknown", id) : map.unavailableReason()); return false;
        }
        if (broken && !maps.brokenAvailable(id)) {
            tell(player, text("game.no_broken", map.displayName())); return false;
        }
        startAfterSelection = false;
        pendingStarter = null;
        // Repeated GUI clicks on the current selection must not restart an expensive paste.
        if (config.mapId.equals(id) && config.broken == broken && (selectedMapReady() || preparingSelection)) return true;
        config.mapId = id; config.broken = broken; config.save();
        announce(text("game.map_selected", map.displayName(), text(broken ? "common.broken" : "common.normal")));
        readyMap = null;
        selectionVersion++;
        prepareSelectedMap();
        return true;
    }

    private void prepareSelectedMap() {
        if (stopped || !inLobby() || maps.busy() || developerTools.authoring().busy()) return;
        String id = config.mapId;
        boolean broken = config.broken;
        long version = selectionVersion;
        preparingSelection = true;
        // Кэшированный оверлей сохраняет лобби при вставке карты; устаревший результат не разрешает старт.
        maps.load(id, broken, result -> {
            if (stopped || !inLobby()) return;
            preparingSelection = false;
            if (version != selectionVersion) { prepareSelectedMap(); return; }
            if (!result.success()) {
                startAfterSelection = false;
                mapFailure = result.message(); announce(text("game.load_failed", result.message())); return;
            }
            readyMap = id;
            readyBroken = broken;
            mapFailure = Component.empty();
            SkyWars2.LOGGER.info("Selected map ready: {} broken={}", id, broken);
            if (startAfterSelection) {
                startAfterSelection = false;
                ServerPlayer starter = pendingStarter == null ? null : server.getPlayerList().getPlayer(pendingStarter);
                // Loading is asynchronous: a disconnected requester or a newly online admin revokes a queued start.
                if (pendingStarter == null || starter != null) requestStart(starter, false);
                pendingStarter = null;
            }
        });
    }

    /** Operator command configuration is validated and persisted at every match stage. */
    public Component configureLobbySetting(ServerPlayer player, String parameter, String value) {
        if (stopped) throw new GameConfig.SettingError(text("message.starting"));
        String key = parameter == null ? "" : parameter.trim().toLowerCase(java.util.Locale.ROOT);
        if (key.equals("map") || key.equals("mapid")) {
            MapManager.MapInfo map = maps.info(value);
            if (map == null) throw new GameConfig.SettingError(text("game.map_unknown", value));
            if (!map.available()) throw new GameConfig.SettingError(text("game.map_unavailable", map.displayName(), map.unavailableReason()));
            if (!configureSelectedMap(player, map.id(), config.broken)) throw new GameConfig.SettingError(text("settings.map_failed"));
            return text("game.map_selected", map.displayName(), text(config.broken ? "common.broken" : "common.normal"));
        }
        if (key.equals("broken")) {
            String normalized = value == null ? "" : value.trim();
            if (!normalized.equalsIgnoreCase("true") && !normalized.equalsIgnoreCase("false"))
                throw new GameConfig.SettingError(text("settings.boolean", "broken"));
            MapManager.MapInfo map = maps.info(config.mapId);
            if (map == null || !map.available())
                throw new GameConfig.SettingError(text("settings.map_first"));
            boolean brokenVariant = Boolean.parseBoolean(normalized);
            if (!configureSelectedMap(player, config.mapId, brokenVariant)) throw new GameConfig.SettingError(text("settings.map_failed"));
            return text("settings.broken", text(brokenVariant ? "common.enabled" : "common.disabled"));
        }
        var result = config.setSetting(parameter, value);
        luckyModifiers.syncConfig();
        lbc.syncRanked();
        lbc.refreshArtifactViews();
        if (key.equals("ranked")) menus.refreshOpen();
        return result;
    }

    /** During a match, changing selection must not paste over the active arena. */
    private boolean configureSelectedMap(ServerPlayer player, String id, boolean broken) {
        if (inLobby()) return selectMapAuthorized(player, id, broken);
        if (broken && !maps.brokenAvailable(id)) return false;
        config.mapId = id;
        config.broken = broken;
        config.save();
        readyMap = null;
        selectionVersion++;
        return true;
    }

    /** Service event flags are persistent configuration; their seasonal mechanics are a later port. */
    public Component configureLobbyEvent(String event, String value) {
        if (stopped) throw new GameConfig.SettingError(text("message.starting"));
        return config.setEvent(event, value);
    }

    public Component settingsSummary() {
        var summary = text("settings.header");
        for (Object[] row : new Object[][] {
                {"lives", config.lives},
                {"health", config.health},
                {"countdownSeconds", config.countdownSeconds},
                {"stageOneSeconds", config.stageOneSeconds},
                {"stageTwoSeconds", config.stageTwoSeconds},
                {"endingSeconds", config.endingSeconds},
                {"stageOneEvent", config.stageOneEvent},
                {"stageTwoEvent", config.stageTwoEvent},
                {"pvpMode", config.pvpMode},
                {"day", config.day},
                {"ranked", config.ranked},
                {"vanillaKits", config.vanillaKits},
                {"vanillaSkills", config.vanillaSkills},
                {"noBorders", config.noBorders},
                {"map", config.mapId}, {"broken", config.broken}
        }) {
            Object value = row[1] instanceof Boolean enabled ? text(enabled ? "common.enabled" : "common.disabled") : row[1];
            summary.append(text("settings.line", text("settings.label." + row[0]), value));
        }
        for (Object[] event : new Object[][] {{"halloween", config.eventHalloween},
                {"november", config.eventNovember}, {"tournament", config.eventTournament}}) {
            summary.append(text("settings.line", Component.literal("event " + event[0]),
                    text((boolean) event[1] ? "common.enabled" : "common.disabled")));
        }
        return summary.append(text("settings.help"));
    }

    public void requestStart(ServerPlayer player, boolean quick) {
        Component failure = startFailure(player);
        if (failure != null) { tell(player, failure); return; }
        if (quick) {
            int count = online().size();
            List<MapManager.MapInfo> available = maps.quickStartCandidates(count, config.broken);
            if (available.isEmpty()) { tell(player, text("game.quick_missing", count)); return; }
            String chosen = available.get(ThreadLocalRandom.current().nextInt(available.size())).id();
            if (!chosen.equals(readyMap) || readyBroken != config.broken) {
                if (!selectMap(player, chosen, config.broken)) return;
            }
        }
        MapManager.MapInfo map = maps.info(config.mapId);
        if (map == null || !map.available() || map.playerCapacity() < online().size()) {
            tell(player, text("game.start_requirements")); return;
        }
        if (!selectedMapReady()) {
            // A valid start waits for the latest selected map; GUI remains usable throughout.
            startAfterSelection = true;
            pendingStarter = player == null ? null : player.getUUID();
            prepareSelectedMap();
            tell(player, text("game.wait_loading"));
            return;
        }
        if (!canStart(player)) { tell(player, text("game.start_requirements")); return; }
        begin();
    }

    private void begin() {
        maps.removeLobbyOverlay();
        items.terra().resetCharges();
        // Выбранная карта уже размещена; старт только фиксирует состав и распределяет игроков по клеткам.
        transition(Stage.LOADING); lobbyReady = false; mapFailure = Component.empty();
        var enrolled = new ArrayList<>(online().stream().map(ServerPlayer::getUUID).toList());
        for (ServerPlayer player : online()) loadingPlayer(player);
        announce(text("game.loading", maps.info(config.mapId).displayName()));
        java.util.function.Consumer<MapManager.Result> enterCountdown = result -> {
            if (stopped) return;
            if (!result.success()) {
                mapFailure = result.message(); announce(text("game.load_failed", result.message())); restoreLobby(); return;
            }
            List<ServerPlayer> ready = enrolled.stream().map(id -> server.getPlayerList().getPlayer(id)).filter(p -> p != null).toList();
            if (ready.size() < (developerMode() ? 1 : 2) || maps.spawns().size() < ready.size()) {
                announce(text("game.cancelled_players")); restoreLobby(); return;
            }
            participants.clear();
            for (ServerPlayer player : online()) {
                player.closeContainer();
                if (!ready.contains(player)) spectators.enter(player);
            }
            List<MapManager.Spawn> spawns = new ArrayList<>(maps.spawns());
            Collections.shuffle(spawns);
            if (config.gameMode == 1) teamColors.begin(ready);
            for (int i = 0; i < ready.size(); i++) {
                ServerPlayer player = ready.get(i);
                Participant member = new Participant(spawns.get(i), Math.max(1, config.lives));
                participants.put(player.getUUID(), member);
                player.setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(
                        maps.level().dimension(), net.minecraft.core.BlockPos.containing(member.spawn.position()),
                        member.spawn.yaw(), member.spawn.pitch()), true), false);
                resetPlayer(player, config.health, true);
                player.setGameMode(GameType.ADVENTURE);
                GameType.ADVENTURE.updatePlayerAbilities(player.getAbilities());
                player.onUpdateAbilities();
                teleport(player, member.spawn.position(), member.spawn.yaw(), member.spawn.pitch());
                var profile = players.get(player.getUUID()); profile.games++; profile.coins += 10;
                player.sendSystemMessage(rewardText("swrg.reward.start_game"));
            }
            players.save(); maps.cages(true);
            countdownTicks = Math.max(1, config.countdownSeconds) * 20;
            gameTicks = 0; lastAliveTicks = 5; fallEnabled = false;
            maps.level().getGameRules().set(GameRules.FALL_DAMAGE, false, server);
            maps.level().getGameRules().set(GameRules.BLOCK_DROPS, false, server);
            maps.level().getGameRules().set(GameRules.ENTITY_DROPS, false, server);
            maps.level().getGameRules().set(GameRules.MOB_DROPS, false, server);
            command("time set " + (config.day == 1 ? 1000 : config.day == 2 ? 13000 : 18000));
            maps.level().getGameRules().set(GameRules.PVP, true, server);
            bar.removeAllPlayers(); for (ServerPlayer player : ready) bar.addPlayer(player);
            bar.setColor(BossEvent.BossBarColor.GREEN); bar.setVisible(false);
            lastCountdownTitleSeconds = -1;
            transition(Stage.COUNTDOWN);
        };
        enterCountdown.accept(new MapManager.Result(true, Component.empty()));
    }

    public void tick() {
        visualTracking.tick();
        // Tracking callbacks run inside native section iterators. Pair teardown may remove siblings.
        // Drain outside those iterators, including while game time is frozen.
        var unloaded=List.copyOf(nativeEntityUnloads);nativeEntityUnloads.clear();
        for(var entity:unloaded) entityUnloaded(entity);
        if (stopped || vanity.busy() || !server.tickRateManager().runsNormally()) return;
        lifetimeTicks++;
        if (inventoryLocked()) for (ServerPlayer player : online()) enforceLobbyInventory(player);
        maps.tick();
        developerTools.tick();
        spectators.tick();
        attributeHud.tick();
        lbc.tick();
        if(runeRestartPending&&stage==Stage.LOBBY&&lobbyReady&&!maps.busy()){
            runeRestartPending=false;requestStart(null,true);
        }
        // В лобби предметы доступны только developer; основной игровой tick items уже вызывается ниже.
        if (!playing()) items.tick();
        if (stage == Stage.LOBBY && lobbyReady) {
            if (!preparingSelection) maps.cleanupLobbyEntities();
            for (ServerPlayer player : online()) {
                if (isDeveloper(player)) continue;
                if (player.position().y < 0 || player.level() != maps.level()) lobbyPlayer(player);
                player.setRemainingFireTicks(0);
                if (lifetimeTicks % 20 == 0) {
                    player.setGameMode(GameType.ADVENTURE); player.setHealth(player.getMaxHealth());
                    player.getFoodData().setFoodLevel(20);
                    player.getFoodData().setSaturation(5);
                    player.removeAllEffects();
                    player.setExperienceLevels(0); player.setExperiencePoints(0);
                }
            }
        } else if (stage == Stage.COUNTDOWN) {
            for (ServerPlayer player : alivePlayers()) {
                Participant p = participants.get(player.getUUID());
                if (player.position().distanceToSqr(p.spawn.position()) > 4) teleport(player, p.spawn.position(), p.spawn.yaw(), p.spawn.pitch());
            }
            int seconds = (countdownTicks + 19) / 20;
            if (seconds != lastCountdownTitleSeconds) {
                showCountdownTitle(seconds);
                lastCountdownTitleSeconds = seconds;
            }
            if (--countdownTicks <= 0) startPlaying();
        } else if (playing()) {
            gameTicks++;
            if (!fallEnabled && gameTicks >= 100) {
                fallEnabled = true; maps.level().getGameRules().set(GameRules.FALL_DAMAGE, true, server);
            }
            if (stage == Stage.PLAYING && gameTicks >= Math.max(1, config.stageOneSeconds) * 20) {
                transition(Stage.PHASE_ONE);
                if (config.stageOneEvent != 0) rewardAlive(20, "swrg.reward.stage1");
                events.stageOne(config.stageOneEvent); bar.setColor(BossEvent.BossBarColor.RED);
            }
            if ((stage == Stage.PLAYING || stage == Stage.PHASE_ONE) && config.stageTwoEvent != 0
                    && gameTicks >= Math.max(config.stageOneSeconds + 1, config.stageTwoSeconds) * 20) {
                transition(Stage.PHASE_TWO); rewardAlive(40, "swrg.reward.stage2"); events.stageTwo(config.stageTwoEvent);
            }
            events.tick();
            items.tick();
            if (gameTicks % 20 == 0) {
                updateBar();
                updateTrackers();
                for (ServerPlayer player : alivePlayers()) {
                    // Void deaths use the real damage/death event and life accounting.
                    if (!config.noBorders && (Math.abs(player.getX()) > 500 || Math.abs(player.getZ()) > 500)) {
                        int x = Math.max(0, ((int) Math.floor(Math.abs(player.getX())) - 500) / 10);
                        int z = Math.max(0, ((int) Math.floor(Math.abs(player.getZ())) - 500) / 10);
                        float amount = (float) Math.floor(config.health * .1) + x + z;
                        player.hurtServer(player.level(), player.damageSources().generic(), Math.max(1, amount));
                    }
                }
            }
            // A player awaiting vanilla respawn still owns remaining lives.
            long eligible = participants.entrySet().stream().filter(e -> e.getValue().lives > 0
                    && !e.getValue().disconnected && server.getPlayerList().getPlayer(e.getKey()) != null
                    && !isObserver(server.getPlayerList().getPlayer(e.getKey()))).count();
            boolean waiting = participants.values().stream().anyMatch(p -> p.lives > 0 && p.awaitingRespawn && !p.disconnected);
            if (developerMode() || eligible >= 2 || waiting) lastAliveTicks = 5;
            else if (--lastAliveTicks <= 0) finish();
        } else if (stage == Stage.ENDING) {
            if (victoryWinner != null) {
                ServerPlayer winner = server.getPlayerList().getPlayer(victoryWinner);
                if (winner != null && finishTicks > 1) events.victoryTick(winner, finishTicks - 1);
            }
            if (--finishTicks <= 0) restoreLobby();
        }
        if (lifetimeTicks % 600 == 0) players.save();
    }

    private void startPlaying() {
        // После открытия клеток переключаем участников в выживание и выдаём ванильные и LBC-наборы.
        if (alivePlayers().size() < (developerMode() ? 1 : 2)) { restoreLobby(); return; }
        LbcPhdCrafting.matchStarted(this);
        luckyModifiers.startMatch();
        maps.cages(false); maps.captureRespawnIslands(); maps.fillChests(false);
        transition(Stage.PLAYING); gameTicks = 0;
        events.start();
        for (ServerPlayer player : alivePlayers()) {
            player.setGameMode(GameType.SURVIVAL); player.setHealth(player.getMaxHealth());
            if (config.vanillaKits) events.giveKit(player);
            if (config.vanillaSkills) events.giveSkill(player);
            items.magic().colors().resetResources(player);
            lbc.apply(player);
            items.stand().matchStarted(player);
        }
        maps.level().getGameRules().set(GameRules.BLOCK_DROPS, true, server);
        maps.level().getGameRules().set(GameRules.ENTITY_DROPS, true, server);
        maps.level().getGameRules().set(GameRules.MOB_DROPS, true, server);
        for (ServerPlayer player : alivePlayers()) {
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(" ")));
        }
        updateBar();
    }

    public List<ServerPlayer> alivePlayers() {
        return participants.entrySet().stream().filter(e -> e.getValue().lives > 0 && !e.getValue().disconnected
                        && !e.getValue().awaitingRespawn)
                .map(e -> server.getPlayerList().getPlayer(e.getKey())).filter(p -> p != null && !isObserver(p)).toList();
    }

    public boolean allowDamage(ServerPlayer player) {
        Participant p = participants.get(player.getUUID());
        return playing() && !isObserver(player) && p != null && p.lives > 0 && !p.disconnected && !p.awaitingRespawn;
    }

    public void died(ServerPlayer player, DamageSource damage) {
        if(vanity.death(player))return;
        items.oreshek().died(player,damage);
        if (!playing()) return;
        Participant p = participants.get(player.getUUID());
        if (p == null || p.lives <= 0 || p.awaitingRespawn) return;
        // Compare maximum HP before ordinary death cleanup removes event skill modifiers.
        boolean vipRespawn=items.containedSkills().protectsDeath(player,damage);
        items.machinegunSkill().clear(player);items.zeusSkill().clear(player);
        items.witchClaims().leavingOrDying(player);
        events.removePlayerSkill(player);
        items.stand().died(player);
        items.mobgun().forget(player);
        if (developerMode()) p.lives = Math.max(1, p.lives);
        else if(!vipRespawn&&!items.indolenceFingers().prepareDeath(player))p.lives--;
        p.awaitingRespawn = true;
        attributeHud.clear(player);
        items.resetRocket(player);
        items.electro().reset(player);
        items.mana().overload().reset(player);
        items.indirectKills().clear(player);
        items.clear(player);
        lbc.clear(player);
        if (!developerMode()) players.get(player.getUUID()).deaths++;
        Entity attacker = damage.getEntity()!=null ? damage.getEntity() : player.getKillCredit();
        if (!developerMode() && attacker instanceof ServerPlayer killer && !killer.getUUID().equals(player.getUUID())
                && participants.containsKey(killer.getUUID())) {
            players.get(killer.getUUID()).kills++;
            players.get(killer.getUUID()).coins += 50;
            command("execute at " + killer.getScoreboardName()
                    + " run playsound minecraft:entity.player.levelup master "
                    + killer.getScoreboardName() + " ~ ~ ~ 1 2");
            killer.sendSystemMessage(rewardText("swrg.reward.kill"));
        }
        players.save();
    }

    public void respawned(ServerPlayer player) {
        if(MatchPlayerCheckpoint.restoring(player)||vanity.busy())return;
        items.witchClaims().reconcile(player);
        attributeHud.clear(player);
        Participant p = participants.get(player.getUUID());
        if (playing() && p != null && !p.disconnected && p.lives > 0) {
            // Полная нормализация относится к границам матча; новую жизнь готовим по прежним правилам.
            p.awaitingRespawn = false; resetPlayer(player, config.health, false);
            boolean fingerSpawn=items.indolenceFingers().fingerSpawn(player);
            if(!fingerSpawn)maps.restoreRespawnIsland(p.spawn);
            player.setGameMode(GameType.SURVIVAL); if(!fingerSpawn)teleport(player, p.spawn.position(), p.spawn.yaw(), p.spawn.pitch());
            if (config.vanillaKits) events.giveKit(player);
            if (config.vanillaSkills) events.giveSkill(player);
            lbc.apply(player, false);
            items.stand().respawned(player);
            items.indolenceFingers().respawned(player);
        } else if (stage == Stage.LOBBY && lobbyReady) lobbyPlayer(player);
        else if (stage == Stage.LOADING) loadingPlayer(player);
        else { if (p != null) p.awaitingRespawn = false; spectators.enter(player); teleport(player, maps.look(), 0, 0); }
    }

    /** Only the authorized /gamemode command calls this; internal mode changes do not enroll observers. */
    public boolean returnFromGameModeCommand(ServerPlayer player, GameType requested) {
        if (stopped || !playing() || requested == GameType.SPECTATOR || !player.isAlive()) return false;
        Participant member = participants.get(player.getUUID());
        if (!isObserver(player) && member != null && member.lives > 0
                && !member.disconnected && !member.awaitingRespawn) return false;
        if (member == null) {
            var spawn = maps.spawns().stream().min(java.util.Comparator.comparingDouble(s ->
                    s.position().distanceToSqr(player.position()))).orElse(null);
            if (spawn == null) return false;
            member = new Participant(spawn, 1);
            participants.put(player.getUUID(), member);
        }
        spectators.remove(player);
        member.lives = Math.max(1, member.lives);
        member.disconnected = false;
        member.awaitingRespawn = false;
        pendingLobby.remove(player.getUUID());
        resetPlayer(player, config.health, false);
        player.setCamera(player);
        player.setInvisible(false);
        player.noPhysics = false;
        player.setNoGravity(false);
        // Adventure may already be the viewer's native mode: setGameMode then returns false.
        requested.updatePlayerAbilities(player.getAbilities());
        player.onUpdateAbilities();
        teamColors.rejoin(player);
        player.setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(
                maps.level().dimension(), net.minecraft.core.BlockPos.containing(member.spawn.position()),
                member.spawn.yaw(), member.spawn.pitch()), true), false);
        teleport(player, member.spawn.position(), member.spawn.yaw(), member.spawn.pitch());
        if (config.vanillaKits) events.giveKit(player);
        if (config.vanillaSkills) events.giveSkill(player);
        lbc.apply(player, false);
        items.stand().respawned(player);
        bar.addPlayer(player);
        lastAliveTicks = 5;
        player.inventoryMenu.sendAllDataToRemote();
        return true;
    }

    /** Nautiz replaces the round through the same map-ready/lobby/new-match transitions as normal play. */
    void restartFromRune(ServerPlayer author){
        if(!playing()||maps.busy()||stopped)return;
        stopMatch();runeRestartPending=true;
    }
    void addRuneLives(ServerPlayer author,int amount){
        var member=participants.get(author.getUUID());if(member==null)return;
        member.lives=(int)Math.clamp((long)member.lives+amount,0L,Integer.MAX_VALUE);
        if(playing())updateBar();
    }
    void rotateMapSpawns(net.minecraft.core.BlockPos origin){
        for(var member:participants.values())if(member.spawn!=null)member.spawn=MapManager.rotatedSpawn(member.spawn,origin);
    }
    public void stopMatch() {
        if (developerTools.authoring().busy()) { announce(text("game.wait_loading")); return; }
        if (maps.busy()) { announce(text("game.wait_loading")); return; }
        announce(text("game.cancelled")); restoreLobby();
    }

    public void damaged(ServerPlayer victim, DamageSource source, float amount) {
        if (!playing() || !allowDamage(victim)) return;
        events.onPlayerDamaged(victim, source, amount);
        if (source.getEntity() instanceof ServerPlayer attacker && allowDamage(attacker))
            events.onPlayerAttack(attacker, victim, source, amount);
    }

    public void landed(ServerPlayer player, double distance) { events.onLanding(player, distance); }
    public void consumed(ServerPlayer player, ItemStack stack) {
        items.foodLifecycle().consumed(player,stack);
        items.kfcFood().consumed(player,stack);
        items.aimbobRewards().consumed(player,stack);
        items.attributeFood().consumed(player,stack);items.pills().consumed(player,stack);
        items.edibleSpells().consumed(player,stack);
        items.namedFoods().consumed(player,stack);items.oreshek().consumed(player,stack);
        events.onConsumed(player, stack);
    }
    public void hurtForSkill(ServerPlayer player) { events.onPlayerHurt(player); }
    public void entityUnloaded(Entity entity) { events.entityUnloaded(entity); items.entityUnloaded(entity); }
    public void queueEntityUnload(Entity entity) { if(!stopped) nativeEntityUnloads.add(entity); }
    public void cancelQueuedEntityUnload(Entity entity) { nativeEntityUnloads.remove(entity); }

    private void finish() {
        vanity.stop();
        luckyModifiers.endMatch();
        items.stop();
        events.stop(); maps.cages(false); bar.setVisible(false); bar.removeAllPlayers();
        List<ServerPlayer> alive = participants.entrySet().stream().filter(e -> e.getValue().lives > 0
                && !e.getValue().disconnected).map(e -> server.getPlayerList().getPlayer(e.getKey())).filter(p -> p != null && !isObserver(p)).toList();
        ServerPlayer winner = alive.size() == 1 ? alive.getFirst() : null;
        victoryWinner = winner == null ? null : winner.getUUID();
        if (winner != null) {
            var profile = players.get(winner.getUUID()); profile.coins += 100; profile.wins++;
            profile.keys[ThreadLocalRandom.current().nextInt(4)]++;
            announce(Component.translatable("swrg.title.player_win")
                    .withStyle(ChatFormatting.DARK_GREEN)
                    .append(Component.literal(" "))
                    .append(winner.getDisplayName().copy().withStyle(style -> style
                            .withColor(ChatFormatting.WHITE).withBold(true)))
                    .append(Component.literal(" "))
                    .append(Component.translatable("swrg.title.player_win2")));
            winner.sendSystemMessage(rewardText("swrg.reward.win"));
        } else announce(Component.translatable("swrg.title.draw")
                .withStyle(style -> style.withColor(ChatFormatting.GREEN)));
        for (ServerPlayer player : online()) {
            player.closeContainer(); lbc.clear(player);
            player.getInventory().clearContent(); player.removeAllEffects();
            resetAttributeValues(player);
            if (player == winner) player.setGameMode(GameType.CREATIVE);
            else spectators.enter(player);
            teleport(player, maps.look(), 0, 0);
        }
        maps.level().getGameRules().set(GameRules.BLOCK_DROPS, false, server);
        maps.level().getGameRules().set(GameRules.ENTITY_DROPS, false, server);
        maps.level().getGameRules().set(GameRules.MOB_DROPS, false, server);
        config.gamesPlayed++; config.save(); players.save();
        transition(Stage.ENDING); finishTicks = Math.max(1, config.endingSeconds) * 20;
        // В исходном конце игры первая четвёрка ракет появляется сразу, затем раз в секунду.
        if (winner != null) events.victoryTick(winner, finishTicks);
    }

    private void restoreLobby() {
        runeRestartPending=false;
        teamColors.stop();
        luckyModifiers.endMatch();
        // Общая точка завершения/отмены матча: снимает временные состояния, готовит арену и ждёт callback лобби.
        boolean preserveFinishedMap = playing() || stage == Stage.ENDING;
        attributeHud.stop();
        items.stop();
        spectators.clear();
        selectionVersion++;
        readyMap = null;
        preparingSelection = false;
        startAfterSelection = false;
        victoryWinner = null;
        for (ServerPlayer player : online()) lbc.clear(player);
        events.stop(); participants.clear(); gameTicks = 0; lastAliveTicks = 5;
        lobbyReady = false; bar.setVisible(false); bar.removeAllPlayers(); transition(Stage.LOADING);
        for (ServerPlayer player : online()) {
            loadingPlayer(player);
            player.removeAllEffects();
            resetAttributeValues(player);
        }
        maps.clearTransientEntities();
        if (maps.level() != null) {
            maps.level().getWorldBorder().setCenter(0, 0); maps.level().getWorldBorder().setSize(59_999_968);
            maps.level().getGameRules().set(GameRules.FALL_DAMAGE, false, server);
            maps.level().getGameRules().set(GameRules.KEEP_INVENTORY, false, server);
        }
        maps.lobby(result -> {
            if (stopped) return;
            if (!result.success()) { mapFailure = result.message(); SkyWars2.LOGGER.error("Lobby: {}", mapFailure.getString()); return; }
            lobbyReady = true; transition(Stage.LOBBY);
            for (ServerPlayer player : online()) lobbyPlayer(player);
            pendingLobby.clear();
            // После матча оставляем его результат для осмотра. Следующий выбор/start подготовит новую карту.
            if (!preserveFinishedMap) prepareSelectedMap();
        });
    }

    private void lobbyPlayer(ServerPlayer player) {
        // При готовом лобби игрок получает приключение, базовые характеристики и закреплённый компас меню.
        if (isDeveloper(player)) return;
        spectators.remove(player);
        player.closeContainer(); resetPlayer(player, 20, true); player.setGameMode(GameType.ADVENTURE);
        GameType.ADVENTURE.updatePlayerAbilities(player.getAbilities());
        player.onUpdateAbilities();
        teleport(player, maps.lobbyPosition(), 0, 0);
        giveLobbyInventory(player);
    }

    /** Adventure permits vanilla use-item packets; temporary flight keeps players above the moving arena. */
    private void loadingPlayer(ServerPlayer player) {
        if (isDeveloper(player)) return;
        player.setGameMode(GameType.ADVENTURE);
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.getAbilities().invulnerable = true;
        player.onUpdateAbilities();
        if (maps.level() != null) teleport(player, new Vec3(.5, maps.level().getMaxY() - 16, .5), 0, 0);
        giveLobbyInventory(player);
    }

    private void giveLobbyInventory(ServerPlayer player) {
        enforceLobbyInventory(player);
        player.getInventory().setSelectedSlot(MENU_COMPASS_SLOT);
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket(MENU_COMPASS_SLOT));
    }

    /** Fixed lobby contents, also repairs server-side grants without allowing client mutations. */
    public void enforceLobbyInventory(ServerPlayer player) {
        if (isObserver(player)) return;
        if (!inventoryLocked(player)) return;
        var inventory = player.getInventory();
        boolean changed = false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack current = inventory.getItem(slot);
            if (slot == MENU_COMPASS_SLOT) {
                if (current.getCount() != 1 || !ItemStack.isSameItemSameComponents(current, lobbyCompass)) {
                    inventory.setItem(slot, lobbyCompass.copy()); changed = true;
                }
            } else if (!current.isEmpty()) {
                inventory.setItem(slot, ItemStack.EMPTY); changed = true;
            }
        }
        // The personal crafting grid and cursor are outside Inventory.
        for (int slot = 0; slot <= 4; slot++) {
            if (!player.inventoryMenu.getSlot(slot).getItem().isEmpty()) {
                player.inventoryMenu.getSlot(slot).set(ItemStack.EMPTY); changed = true;
            }
        }
        if (!player.containerMenu.getCarried().isEmpty()) {
            player.containerMenu.setCarried(ItemStack.EMPTY); changed = true;
        }
        if (changed) {
            inventory.setChanged();
            player.inventoryMenu.broadcastChanges();
            if (player.containerMenu != player.inventoryMenu) player.containerMenu.broadcastChanges();
        }
    }

    private static ItemStack menuCompass() {
        ItemStack compass = new ItemStack(Items.COMPASS);
        compass.set(DataComponents.CUSTOM_NAME, Component.translatable("menu")
                .withStyle(style -> style.withColor(ChatFormatting.YELLOW).withItalic(false)));
        return compass;
    }

    private void resetPlayer(ServerPlayer player, int health, boolean resetAllAttributes) {
        items.oreshek().resetBoundary(player);
        // Общая подготовка к лобби, старту и возрождению: снимаем бонусы, очищаем инвентарь, восстанавливаем здоровье и еду.
        items.resetAttributes(player);
        items.resetRocket(player);
        items.indirectKills().clear(player);
        items.clear(player);
        lbc.clear(player);
        player.getInventory().clearContent(); player.removeAllEffects();
        // Авторство предыдущей жизни/катки не переносится на новую экологическую смерть.
        player.setLastHurtByPlayer((net.minecraft.world.entity.player.Player)null,0);
        player.setLastHurtByMob(null);
        // Сначала забываем прежние ability snapshots/бонусы, затем базу; конфиг и новые перки идут после.
        if (resetAllAttributes) resetAttributeValues(player);
        var max = player.getAttribute(Attributes.MAX_HEALTH);
        if (max != null) max.setBaseValue(Math.max(1, Math.min(1000, health)));
        var speed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (speed != null) speed.setBaseValue(config.pvpMode == 1 && stage != Stage.LOBBY ? 100000 : 4);
        player.setHealth(player.getMaxHealth()); player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5);
        player.setExperienceLevels(0); player.setExperiencePoints(0);
        player.setRemainingFireTicks(0); player.fallDistance = 0;
    }

    /** Все зарегистрированные атрибуты, включая новые: оба вида modifiers снимаются только на границах матча. */
    private void resetAttributeValues(ServerPlayer player) {
        // Infinity traits share the full match reset; ordinary cleansing/respawn does not consume them.
        player.entityTags().removeAll(List.of("infinite_resistance","infinite_heal","infinite_saturation"));
        var attributes = player.getAttributes();
        for (var attribute : BuiltInRegistries.ATTRIBUTE) {
            var holder = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute);
            var instance = attributes.getInstance(holder);
            if (instance == null) continue;
            instance.removeModifiers();
            // Нативный supplier знает именно player defaults (например, attack_damage=1).
            // Fallback покрывает дополнительные materialized instances вне его supplier.
            if (!attributes.resetBaseValue(holder)) instance.setBaseValue(attribute.getDefaultValue());
        }
        attributeHud.clear(player);
    }

    private void teleport(ServerPlayer player, Vec3 position, float yaw, float pitch) {
        if (maps.level() != null) player.teleportTo(maps.level(), position.x, position.y, position.z, Set.of(), yaw, pitch, true);
    }

    private void rewardAlive(int coins, String translationKey) {
        for (ServerPlayer player : alivePlayers()) {
            players.get(player.getUUID()).coins += coins;
            player.sendSystemMessage(rewardText(translationKey));
        }
        players.save();
    }
    private static Component rewardText(String translationKey) {
        return Component.translatable(translationKey)
                .withStyle(style -> style.withColor(ChatFormatting.GOLD).withItalic(false));
    }
    private List<ServerPlayer> online() { return List.copyOf(server.getPlayerList().getPlayers()); }
    private void tell(ServerPlayer player, Component message) { if (player != null) player.sendSystemMessage(message); else SkyWars2.LOGGER.info("{}", message.getString()); }
    public void announce(Component message) { server.getPlayerList().broadcastSystemMessage(message, false); SkyWars2.LOGGER.info("[game] {}", message.getString()); }
    private void transition(Stage next) {
        SkyWars2.LOGGER.info("[game] {} -> {}", stage, next);
        if (stage == Stage.LOBBY || next == Stage.LOBBY) maps.resetLobbyEntityCleanup();
        // Lobby entry clears weather left by the match and parks all native weather timers.
        if (next == Stage.LOBBY) server.setWeatherParameters(Integer.MAX_VALUE, Integer.MAX_VALUE, false, false);
        stage = next;
    }

    /** Use the native dispatcher; malformed migrated commands are explicit failures. */
    public boolean command(String command) {
        try {
            boolean[] success = {false};
            var source = server.createCommandSourceStack().withLevel(maps.level()).withPosition(Vec3.ZERO)
                    .withSuppressedOutput().withCallback((ok, result) -> success[0] |= ok);
            var parsed = server.getCommands().getDispatcher().parse(command, source);
            if (parsed.getReader().canRead()) {
                SkyWars2.LOGGER.error("Invalid gameplay command: {} ({})", command, parsed.getExceptions()); return false;
            }
            server.getCommands().performCommand(parsed, command); return success[0];
        } catch (Exception exception) { SkyWars2.LOGGER.error("Gameplay command failed: {}", command, exception); return false; }
    }

    private void updateBar() {
        int stageOneAt = Math.max(1, config.stageOneSeconds);
        int stageTwoAt = Math.max(config.stageOneSeconds + 1, config.stageTwoSeconds);
        boolean hasStageOne = config.stageOneEvent != 0;
        boolean hasStageTwo = config.stageTwoEvent != 0;
        if (stage == Stage.PHASE_TWO || (!hasStageOne && !hasStageTwo)
                || (stage == Stage.PHASE_ONE && !hasStageTwo)) {
            bar.setVisible(false);
            return;
        }
        int next;
        int remaining;
        if (stage == Stage.PHASE_ONE) {
            next = stageTwoAt - stageOneAt;
            remaining = stageTwoAt - gameTicks / 20;
            bar.setColor(BossEvent.BossBarColor.RED);
        } else if (hasStageOne) {
            next = stageOneAt;
            remaining = next - gameTicks / 20;
            bar.setColor(BossEvent.BossBarColor.GREEN);
        } else {
            next = stageTwoAt;
            remaining = next - gameTicks / 20;
            bar.setColor(BossEvent.BossBarColor.RED);
        }
        bar.setVisible(true);
        remaining = Math.max(0, remaining);
        bar.setName(Component.translatable("swrg.title.gametimer").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("["))
                .append(Component.literal(Integer.toString(remaining)).withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.literal("]").withStyle(ChatFormatting.GOLD)));
        bar.setProgress(Math.max(0, Math.min(1, (float) remaining / Math.max(1, next))));
    }

    private void showCountdownTitle(int seconds) {
        Component subtitle = seconds > 0
                ? Component.literal(Integer.toString(seconds)).withStyle(ChatFormatting.GREEN)
                : Component.literal(" ").withStyle(ChatFormatting.GREEN);
        for (ServerPlayer player : alivePlayers()) {
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(" ")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        }
    }

    private void updateTrackers() {
        List<ServerPlayer> alive = alivePlayers();
        for (ServerPlayer player : alive) {
            ServerPlayer target = alive.stream().filter(other -> other != player)
                    .min(java.util.Comparator.comparingDouble(other -> other.distanceToSqr(player))).orElse(null);
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack compass = player.getInventory().getItem(slot);
                if (!compass.is(Items.COMPASS)) continue;
                if (target == null) compass.remove(DataComponents.LODESTONE_TRACKER);
                else compass.set(DataComponents.LODESTONE_TRACKER, new net.minecraft.world.item.component.LodestoneTracker(
                        java.util.Optional.of(net.minecraft.core.GlobalPos.of(maps.level().dimension(), target.blockPosition())), false));
            }
        }
    }

    public void shutdown() {
        developerTools.close();
        vanity.stop();
        visualTracking.stop();
        teamColors.stop();
        luckyModifiers.endMatch();
        attributeHud.stop();
        items.stop();
        spectators.clear();
        for (ServerPlayer player : online()) lbc.clear(player);
        stopped = true; nativeEntityUnloads.clear(); events.stop(); maps.shutdown(); bar.removeAllPlayers(); players.save(); config.save();
    }
}
