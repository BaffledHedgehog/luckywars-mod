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
import net.minecraft.server.level.ServerLevel;
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
    private static final boolean BEDWARS_READY = true; // Ordinary native chain; Lucky modes retain their separate gate.
    public enum Stage { LOBBY, LOADING, COUNTDOWN, PLAYING, PHASE_ONE, PHASE_TWO, ENDING }

    private static final class Participant {
        MapManager.Spawn spawn;
        int lives;
        boolean awaitingRespawn;
        boolean disconnected;
        boolean renewalRespawn;
        Participant(MapManager.Spawn spawn, int lives) { this.spawn = spawn; this.lives = lives; }
    }

    private enum RenewalPhase { SETTLING, HOLDING, LOADING, READY, FAILED }
    /** Immutable intent belongs to the match checkpoint; asynchronous work itself is never copied. */
    private record SupernovaMap(UUID event, UUID owner, LbcIndirectKills.Owner cause, String map,
                                boolean broken, RenewalPhase phase, boolean requested) {
        SupernovaMap phase(RenewalPhase next) { return new SupernovaMap(event,owner,cause,map,broken,next,requested); }
        SupernovaMap request() { return new SupernovaMap(event,owner,cause,map,broken,phase,true); }
    }
    private record BalanceMap(UUID event, RenewalPhase phase) {
        BalanceMap phase(RenewalPhase next) { return new BalanceMap(event,next); }
    }

    /** Core values only; native bodies, entities and ability queues have their own restore phase. */
    static final class Checkpoint {
        private final GameController owner;
        private final String epoch;
        private final LbcBedwars.Checkpoint bedwars;
        private final SupernovaMap renewal;
        private final BalanceMap balanceRenewal;
        private final boolean legacy;
        private final Set<UUID> balanceResets;
        private final Map<UUID,Integer> balanceRewards;
        private final Map<UUID, Participant> members = new LinkedHashMap<>();
        private final Set<UUID> pending;
        private final List<Entity> unloads;
        private final Stage stage;
        private final UUID host,pendingStarter,victoryWinner;
        private final int countdownTicks,gameTicks,finishTicks,lastAliveTicks,lifetimeTicks,lastCountdownTitleSeconds;
        private final boolean lobbyReady,stopped,fallEnabled,readyBroken,preparingSelection,startAfterSelection;
        private final Component mapFailure;
        private final String readyMap;
        private final int activeGameMode;
        private final long selectionVersion;
        private final net.minecraft.nbt.Tag barName;
        private final BossEvent.BossBarColor color;
        private final BossEvent.BossBarOverlay overlay;
        private final float progress;
        private final boolean visible,darken,music,fog;
        private final List<UUID> barMembers;

        private Checkpoint(GameController g) {
            owner=g;epoch=g.items.epoch();bedwars=g.bedwars.checkpoint();
            renewal=g.supernovaMap;
            balanceRenewal=g.balanceMap;legacy=g.balanceLegacy;balanceResets=Set.copyOf(g.balancePendingReset);
            balanceRewards=Map.copyOf(g.balancePendingRewards);
            g.participants.forEach((id,p)->members.put(id,copy(p)));
            pending=Set.copyOf(g.pendingLobby);unloads=List.copyOf(g.nativeEntityUnloads);
            stage=g.stage;host=g.host;pendingStarter=g.pendingStarter;victoryWinner=g.victoryWinner;
            activeGameMode=g.activeGameMode;
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
            result.awaitingRespawn=source.awaitingRespawn;result.disconnected=source.disconnected;
            result.renewalRespawn=source.renewalRespawn;return result;
        }
    }

    Checkpoint checkpointCore() {
        if(!server.isSameThread()||!playing()||stopped||maps.busy()
                ||supernovaMap!=null&&(supernovaMap.phase()==RenewalPhase.FAILED||!supernovaTerrainSettled())
                ||balanceMap!=null)throw new IllegalStateException("Capture a stable active match on its server thread");
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
        cunnyRestart=null;
        activeGameMode=image.activeGameMode;
        supernovaMap=image.renewal;
        balanceMap=image.balanceRenewal;balanceLegacy(image.legacy);
        balancePendingReset.clear();balancePendingReset.addAll(image.balanceResets);
        balancePendingRewards.clear();balancePendingRewards.putAll(image.balanceRewards);
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
        bedwars.restore(image.bedwars);
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
    private final LbcBedwars bedwars;
    private final LbcCases cases;
    LbcCases cases() { return cases; }
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
    private int activeGameMode;
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
    private enum CunnyRestartPhase { DRAINING, PREPARING }
    private CunnyRestartPhase cunnyRestart;
    private SupernovaMap supernovaMap;
    private boolean supernovaNativeTick;
    private BalanceMap balanceMap;
    private boolean balanceLegacy;
    private final Set<UUID> balancePendingReset=new java.util.HashSet<>();
    private final Map<UUID,Integer> balancePendingRewards=new LinkedHashMap<>();
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
        this.bedwars = new LbcBedwars(this);
        this.cases = new LbcCases(this);
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
        var member=participants.get(player.getUUID());
        boolean retained=supernovaNativeTick && playing() && member!=null && member.lives>0
                && !member.disconnected && !member.awaitingRespawn;
        return (!supernovaMapTransition() || supernovaNativeTick || !renewalPlayer(player)) && !isObserver(player)
                && (allowDamage(player) || retained || isDeveloper(player) && !stopped
                && (inLobby() || stage == Stage.LOADING));
    }
    public boolean selectedMapReady() { return readyMap != null && readyMap.equals(config.mapId) && readyBroken == config.broken && !maps.busy() && mapMatchesMode(config.mapId); }

    /** Configuration selects the next match; every current gameplay owner reads this latched value. */
    public int gameMode() { return activeGameMode==0?config.gameMode:activeGameMode; }
    void beginMatchMode(int mode) {
        if(mode<1||mode>4)throw new IllegalArgumentException("Match mode must be1..4");
        activeGameMode=mode;
    }
    void prepareSoloTeams(List<ServerPlayer> ready) {
        if(gameMode()==1||gameMode()==2)teamColors.begin(ready);
    }
    void grantStartExperience(ServerPlayer player) {
        // Source lbc:start grants20 levels before kits, only in Lucky SkyWars; Stand still requires entitlement.
        if(gameMode()==2)player.giveExperienceLevels(20);
    }
    boolean mapMatchesMode(String id) {
        return config.gameMode>=1&&config.gameMode<=4&&maps.info(id)!=null&&maps.bedwars(id)==(config.gameMode>=3);
    }
    private Component mapModeMismatch() {
        return Component.translatableWithFallback("skywars2.game.map_mode_mismatch","Карта не подходит для режима %s.",config.gameMode);
    }
    public boolean playing() { return stage == Stage.PLAYING || stage == Stage.PHASE_ONE || stage == Stage.PHASE_TWO; }
    public int matchTicks() { return gameTicks; }
    public boolean supernovaMapTransition() { return supernovaMap != null || balanceMap != null; }
    boolean cunnyRestartPending() { return cunnyRestart != null; }
    public boolean balanceLegacy() { return balanceLegacy; }
    private void balanceLegacy(boolean active){
        balanceLegacy=active;
        items.mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("legacy"),"lbc.math").set(active?1:0);
    }
    public void activateBalance(ServerPlayer player) {
        if (!playing() || stopped || cunnyRestart != null || supernovaMapTransition() || maps.busy()
                || vanity.busy() || vanity.capturePreparing() || items.supernova().phase()!=LbcSupernova.Phase.IDLE
                || !canUseGameplayItems(player)) return;
        balanceMap=new BalanceMap(UUID.randomUUID(),RenewalPhase.SETTLING);
        items.duplicating().beginCheckpointDrain();
    }

    private int renewalCapacity() {
        if(bedwars.mode())return Math.max(1,(int)activeMembersForBedwars().stream().map(server.getPlayerList()::getPlayer).filter(java.util.Objects::nonNull).map(bedwars::team).filter(t->t>0).distinct().count());
        return Math.max(1,(int)participants.values().stream().filter(p->p.lives>0&&!p.disconnected).count());
    }
    public boolean canBeginSupernova(ServerLevel level, UUID owner) {
        return server.isSameThread() && level == maps.level() && playing() && !stopped && cunnyRestart == null && !maps.busy()
                && !supernovaMapTransition() && !vanity.busy() && !vanity.capturePreparing()
                && !maps.renewalCandidates(renewalCapacity(),config.broken,maps.bedwars(config.mapId)).isEmpty();
    }
    /** Called once at DELAY admission. Neither the match nor any item epoch is restarted. */
    public void prepareSupernovaMap(UUID event, UUID owner, LbcIndirectKills.Owner cause) {
        if (supernovaMap != null || !playing() || stopped) return;
        var pool=maps.renewalCandidates(renewalCapacity(),config.broken,maps.bedwars(config.mapId));
        String selected=pool.isEmpty()?null:pool.get(maps.level().getRandom().nextInt(pool.size())).id();
        supernovaMap=new SupernovaMap(event,owner,cause,selected,config.broken,RenewalPhase.SETTLING,false);
        items.duplicating().beginCheckpointDrain();
    }
    public void replaceSupernovaMap(UUID event, UUID owner, LbcIndirectKills.Owner cause) {
        if (supernovaMap == null || !supernovaMap.event().equals(event)) return;
        supernovaMap=supernovaMap.request();
    }
    public void finishSupernovaMap(UUID event, UUID owner, boolean success) {
        if (supernovaMap == null || !supernovaMap.event().equals(event)) return;
        // A failed paste retains its safety hold and the old public checkpoint for explicit rewind.
        if (!success) { supernovaMap=supernovaMap.phase(RenewalPhase.FAILED); return; }
        balancePendingRewards.merge(supernovaMap.owner(),1,Integer::sum);
        var recipient=server.getPlayerList().getPlayer(supernovaMap.owner());
        supernovaMap=null;
        items.duplicating().endCheckpointDrain();
        if(recipient!=null)givePendingBalanceRewards(recipient);
    }
    private void givePendingBalanceRewards(ServerPlayer player) {
        var count=balancePendingRewards.remove(player.getUUID());if(count==null)return;
        for(int i=0;i<count;i++){
            var rolled=lbc.rollTable(player.level(),player.position(),player,"lbc:balance");
            var reward=rolled.isEmpty()?new ItemStack(LbcBalanceItems.BALANCE):rolled.getFirst().copyWithCount(1);
            player.getInventory().add(reward);
            if(!reward.isEmpty())player.drop(reward,false,net.minecraft.util.Prediction.SERVER_ONLY);
        }
    }
    private boolean supernovaPaused() {
        return items.wands().legacyTimeStopped() || items.timeStop().stopped(maps.level());
    }
    private boolean supernovaTerrainSettled() {
        return !maps.busy() && SignedEnvironmentalEffects.queuedTerrain()==0
                && items.blaze().terrainCount()==0 && items.kiuas().queuedBlockCount()==0
                && items.tanner().queuedBlockCount()==0 && items.portalDrops().idle()
                && MatchEventActorCheckpoint.captureReady(this) && MatchFieldCheckpoint.captureReady(this)
                && MatchTransientSpells.captureReady(this) && MatchAcademyCheckpoint.captureReady(this)
                && MatchEncounterCheckpoint.captureReady(this) && items.sauvojen().meditation().count()==0
                && items.duplicating().pendingCheckpointWrites()==0 && items.stonekivi().captureSettled()
                && items.eternalWealth().captureReady() && items.nexusWires().captureReady() && items.imbaTerrainUtilities().restartSettled();
    }
    private boolean renewalPlayer(ServerPlayer player) {
        return balanceMap!=null || !player.level().dimension().equals(LbcHedgehogDimensions.IMPRINTED);
    }
    private void holdSupernovaPlayers() {
        for (var player:online()) {
            if (!renewalPlayer(player) || !player.isAlive()) continue;
            player.closeContainer(); player.stopRiding();
            teleport(player,new Vec3(0,1000,0),player.getYRot(),player.getXRot());
            player.setDeltaMovement(Vec3.ZERO); player.fallDistance=0;
            items.ownedEffects().grant(player,"supernova.map.hold",new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.RESISTANCE,100,3,false,false),null);
        }
    }
    private void tickSupernovaMap() {
        if (supernovaMap==null || supernovaPaused()) return;
        if (supernovaMap.phase()==RenewalPhase.SETTLING) {
            // Drain every accepted native structure rather than abandoning a half-built footprint.
            if (!supernovaTerrainSettled()) return;
            holdSupernovaPlayers(); maps.purgeRenewalEntities();
            supernovaMap=supernovaMap.phase(RenewalPhase.HOLDING);
        }
        if (supernovaMap.phase()!=RenewalPhase.SETTLING && lifetimeTicks%20==0) holdSupernovaPlayers();
        if (supernovaMap.phase()==RenewalPhase.HOLDING && supernovaMap.requested()) {
            if (maps.busy() || !supernovaTerrainSettled()) return;
            var intent=supernovaMap;
            if (intent.map()==null) { failSupernovaMap(Component.literal("No eligible replacement map")); return; }
            maps.purgeRenewalEntities();
            supernovaMap=intent.phase(RenewalPhase.LOADING);
            maps.load(intent.map(),intent.broken(),result->{
                if (supernovaMap==null || !supernovaMap.event().equals(intent.event()) || stopped) return;
                if (!result.success()) { failSupernovaMap(result.message()); return; }
                // Completion can arrive during TimeStop; player placement waits for the active clock.
                supernovaMap=supernovaMap.phase(RenewalPhase.READY);
            });
        }
        if (supernovaMap!=null && supernovaMap.phase()==RenewalPhase.READY) completeSupernovaMap();
    }
    private void failSupernovaMap(Component reason) {
        var intent=supernovaMap;
        mapFailure=reason.copy(); supernovaMap=intent.phase(RenewalPhase.FAILED);
        announce(text("game.load_failed",reason));
        items.supernova().completed(intent.event(),false);
    }
    private void completeSupernovaMap() {
        var intent=supernovaMap;
        var retained=participants.entrySet().stream().filter(e->e.getValue().lives>0&&!e.getValue().disconnected).toList();
        boolean bedwarsRenewal=bedwars.mode();
        Map<UUID,MapManager.Spawn> teamSpawns=Map.of();
        var spawns=new ArrayList<>(maps.spawns());
        if(bedwarsRenewal){
            try{
                teamSpawns=bedwars.prepareRenewal(retained.stream().map(Map.Entry::getKey).toList());
                bedwars.tick();
                if(!bedwars.startReady())return;
            }catch(RuntimeException error){
                SkyWars2.LOGGER.error("BedWars replacement setup rejected",error);
                failSupernovaMap(Component.translatable("skywars2.map.error.spawns_missing",intent.map()));return;
            }
        }else{
            Collections.shuffle(spawns);
            if (spawns.size()<retained.size()) { failSupernovaMap(Component.literal("Replacement map has insufficient spawns")); return; }
        }
        for (int i=0;i<retained.size();i++) {
            var entry=retained.get(i); var member=entry.getValue();
            member.spawn=bedwarsRenewal?teamSpawns.get(entry.getKey()):spawns.get(i);
            var player=server.getPlayerList().getPlayer(entry.getKey());
            if (player==null || !renewalPlayer(player)) continue;
            player.setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(
                    maps.level().dimension(),net.minecraft.core.BlockPos.containing(member.spawn.position()),
                    member.spawn.yaw(),member.spawn.pitch()),true),false);
            if (member.awaitingRespawn || !player.isAlive() || isObserver(player)) continue;
            teleport(player,member.spawn.position(),member.spawn.yaw(),member.spawn.pitch());
            player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;
            items.ownedEffects().grant(player,"supernova.map.arrival",new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.SLOW_FALLING,200,0,false,false),null);
        }
        for (var player:online()) {
            if (!renewalPlayer(player)) continue;
            items.ownedEffects().withdraw(player,"supernova.map.hold");
            var member=participants.get(player.getUUID());
            if (player.isAlive() && (member==null || member.lives<=0 || member.disconnected || isObserver(player)))
                teleport(player,maps.look(),0,0);
        }
        config.mapId=intent.map(); config.broken=intent.broken();
        readyMap=intent.map();readyBroken=intent.broken();mapFailure=Component.empty();
        if(!bedwars.mode()){maps.cages(false);maps.captureRespawnIslands();maps.fillChests(false);}
        // Restart only stage scheduling. Skill state, inventory, resources and lives retain their owners.
        zoneBarrier.stop(); events.cancelPendingHitReactions();events.restoreTimeline(new MatchEvents.Timeline(true,-1,-1));
        transition(Stage.PLAYING);gameTicks=0;lastAliveTicks=5;fallEnabled=false;
        maps.level().getGameRules().set(GameRules.FALL_DAMAGE,false,server);
        maps.level().getGameRules().set(GameRules.BLOCK_DROPS,true,server);
        maps.level().getGameRules().set(GameRules.ENTITY_DROPS,true,server);
        maps.level().getGameRules().set(GameRules.MOB_DROPS,true,server);
        bar.setColor(BossEvent.BossBarColor.GREEN);updateBar();
        for(var player:online())if(renewalPlayer(player)){
            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(" ")));
            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("end_respawn")));
            if(!player.isSpectator())items.mana().award(player,"lbc:true_advancements/legends/supernova");
        }
        if(bedwarsRenewal)bedwars.renewalCommitted();
        items.supernova().completed(intent.event(),true);
        for (var entry:participants.entrySet()) {
            var member=entry.getValue();
            if (!member.renewalRespawn) continue;
            member.renewalRespawn=false;
            var player=server.getPlayerList().getPlayer(entry.getKey());
            if (player!=null) respawned(player);
        }
    }

    private void resetBalancePlayer(ServerPlayer player) {
        lbc.resolvedLevelups().forget(player.getUUID());
        int level=player.experienceLevel,total=player.totalExperience;float progress=player.experienceProgress;
        resetPlayer(player,20,true);
        // The source clears inventory/perks, but never debits existing XP.
        player.experienceLevel=level;player.totalExperience=total;player.experienceProgress=progress;
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetExperiencePacket(progress,total,level));
        player.entityTags().removeAll(List.of("levelups","gamehealthmodifier","darkness_player","abyss","powerfall","trollhunter"));
        var modifier=player.getAttribute(Attributes.MAX_HEALTH);
        if(modifier!=null)modifier.setBaseValue(20);
        player.setHealth(player.getMaxHealth());
    }
    private void tickBalanceMap() {
        if(balanceMap==null || supernovaPaused())return;
        if(balanceMap.phase()==RenewalPhase.SETTLING){
            if(!supernovaTerrainSettled())return;
            var intent=balanceMap;
            holdSupernovaPlayers();
            // This is the explicit full old-mode boundary, distinct from same-match Supernova.
            vanity.stop();lbc.resetLevelupMatch();events.stop();zoneBarrier.stop();luckyModifiers.endMatch();attributeHud.stop();
            items.stop();
            balanceMap=intent.phase(RenewalPhase.LOADING);
            balanceLegacy(false);
            for(var entry:participants.entrySet()){
                if(entry.getValue().lives>0&&!entry.getValue().disconnected)entry.getValue().lives=1;
                if(server.getPlayerList().getPlayer(entry.getKey())==null)balancePendingReset.add(entry.getKey());
            }
            for(var player:online()){
                if(player.isAlive())resetBalancePlayer(player);else balancePendingReset.add(player.getUUID());
            }
            for(var dimension:List.of(net.minecraft.world.level.Level.OVERWORLD,net.minecraft.world.level.Level.NETHER,
                    net.minecraft.world.level.Level.END,LbcHedgehogDimensions.IMPRINTED)){
                var level=server.getLevel(dimension);if(level==null)continue;
                // These are source game rules, never a legacy-function execution bridge.
                var rules=level.getGameRules();
                rules.set(GameRules.WATER_SOURCE_CONVERSION,true,server);
                rules.set(GameRules.RANDOM_TICK_SPEED,3,server);
                rules.set(GameRules.NATURAL_HEALTH_REGENERATION,true,server);
                rules.set(GameRules.LIMITED_CRAFTING,false,server);
                rules.set(GameRules.MAX_BLOCK_MODIFICATIONS,32777000,server);
                rules.set(GameRules.ADVANCE_TIME,true,server);
                rules.set(GameRules.IMMEDIATE_RESPAWN,true,server);
                rules.set(GameRules.BLOCK_DROPS,true,server);
                rules.set(GameRules.ENTITY_DROPS,true,server);
                rules.set(GameRules.MOB_DROPS,true,server);
                if(!dimension.equals(LbcHedgehogDimensions.IMPRINTED))
                    for(long chunk:level.getForceLoadedChunks().toLongArray())
                        level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(chunk),net.minecraft.world.level.ChunkPos.getZ(chunk),false);
            }
            maps.removeLobbyOverlay();maps.purgeRenewalEntities();
            maps.level().getGameRules().set(GameRules.BLOCK_DROPS,false,server);
            maps.level().getGameRules().set(GameRules.ENTITY_DROPS,false,server);
            maps.level().getGameRules().set(GameRules.MOB_DROPS,false,server);
            maps.level().getWorldBorder().setCenter(0,0);maps.level().getWorldBorder().setSize(59_999_968);
            server.setWeatherParameters(200000000,0,false,false);
            maps.loadBalance(result->{
                if(balanceMap==null||!balanceMap.event().equals(intent.event())||stopped)return;
                if(!result.success()){
                    mapFailure=result.message();balanceMap=balanceMap.phase(RenewalPhase.FAILED);
                    announce(text("game.load_failed",result.message()));return;
                }
                balanceMap=balanceMap.phase(RenewalPhase.READY);
            });
        }
        if(balanceMap==null)return;
        if(lifetimeTicks%20==0)holdSupernovaPlayers();
        if(balanceMap.phase()!=RenewalPhase.READY)return;
        var retained=participants.entrySet().stream().filter(e->e.getValue().lives>0&&!e.getValue().disconnected).toList();
        var spawns=new ArrayList<>(maps.spawns());Collections.shuffle(spawns);
        if(spawns.isEmpty()){
            mapFailure=Component.literal("Balance map has no spawns");balanceMap=balanceMap.phase(RenewalPhase.FAILED);return;
        }
        // Source has twelve real spawn markers. Excess players remain on its staging platform.
        for(int i=0;i<retained.size();i++){
            var entry=retained.get(i);var member=entry.getValue();
            var spawn=i<spawns.size()?spawns.get(i):new MapManager.Spawn(new Vec3(0,128,0),0,0);
            member.spawn=spawn;var player=server.getPlayerList().getPlayer(entry.getKey());
            if(player==null)continue;
            player.setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(maps.level().dimension(),
                    net.minecraft.core.BlockPos.containing(spawn.position()),spawn.yaw(),spawn.pitch()),true),false);
            if(player.isAlive()&&!member.awaitingRespawn&&!isObserver(player))teleport(player,spawn.position(),spawn.yaw(),spawn.pitch());
        }
        for(var player:online()){
            items.ownedEffects().withdraw(player,"supernova.map.hold");
            var member=participants.get(player.getUUID());
            if(player.isAlive()&&(member==null||member.lives<=0||member.disconnected||isObserver(player)))teleport(player,maps.look(),0,0);
        }
        balanceLegacy(true);balanceMap=null;readyMap=null;mapFailure=Component.empty();
        if(!bedwars.mode()){maps.cages(false);maps.captureRespawnIslands();maps.fillChests(false);}
        gameTicks=0;lastAliveTicks=5;fallEnabled=false;transition(Stage.PLAYING);
        events.restoreTimeline(new MatchEvents.Timeline(true,-1,-1));
        maps.level().getGameRules().set(GameRules.FALL_DAMAGE,false,server);
        maps.level().getGameRules().set(GameRules.BLOCK_DROPS,true,server);
        maps.level().getGameRules().set(GameRules.ENTITY_DROPS,true,server);
        maps.level().getGameRules().set(GameRules.MOB_DROPS,true,server);
        items.balance().begin();
        for(var player:online()){
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.RESISTANCE,100,4,false,false),player);
            net.minecraft.world.effect.MobEffects.INSTANT_HEALTH.value().applyInstantaneousEffect(player.level(),player,player,player,4,1);
            net.minecraft.world.effect.MobEffects.SATURATION.value().applyInstantaneousEffect(player.level(),player,player,player,40,1);
            LbcOverload.sound(player,player.position(),"minecraft:lbcsounds.burning_balance",1,1,1);
            LbcOverload.sound(player,player.position(),"minecraft:lbcsounds.burning_balance",1,1,1);
            if(!player.isSpectator())items.mana().award(player,"lbc:true_advancements/legends/new_world");
        }
        bar.setColor(BossEvent.BossBarColor.GREEN);updateBar();
        for(var entry:participants.entrySet())if(entry.getValue().renewalRespawn){
            entry.getValue().renewalRespawn=false;var player=server.getPlayerList().getPlayer(entry.getKey());
            if(player!=null)respawned(player);
        }
    }
    public ZoneBarrier zoneBarrier() { return zoneBarrier; }
    public int lives(UUID id) { Participant p = participants.get(id); return p == null ? 0 : p.lives; }
    public boolean awaitingRespawn(UUID id) { Participant p = participants.get(id); return p != null && p.awaitingRespawn; }
    public MapManager maps() { return maps; }
    public GameConfig config() { return config; }
    public PlayerStore players() { return players; }
    public GameMenus menus() { return menus; }
    public LbcLoadouts lbc() { return lbc; }
    public LbcItems items() { return items; }
    public LbcBedwars bedwars() { return bedwars; }
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
        if(balancePendingReset.remove(player.getUUID()))resetBalancePlayer(player);
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
        givePendingBalanceRewards(player);
        cases.onJoin(player);
        items.cosmetics().refreshSuffixes(List.of(player));
    }

    public void leave(ServerPlayer player) {
        items.cosmetics().nameplates().forget(player);
        cases.onLeave(player);
        vanity.leaving(player);
        if(vanity.busy())return;
        items.witchClaims().leavingOrDying(player);
        // При отключении снимаем временные бонусы, отмечаем участника выбывшим и сохраняем профиль.
        attributeHud.clear(player);
        teamColors.leave(player);
        items.forget(player);
        items.cinema().forget(player);
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
        return mapCapacity(map,online().size());
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
        if (config.gameMode != 1 && !(config.gameMode == 3 && BEDWARS_READY))
            return Component.translatable("skywars2.game.mode_unavailable", config.gameMode);
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
        if(!mapMatchesMode(id)){tell(player,mapModeMismatch());return false;}
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
        if(!mapMatchesMode(id)){readyMap=null;startAfterSelection=false;pendingStarter=null;mapFailure=mapModeMismatch();return;}
        boolean broken = config.broken;
        long version = selectionVersion;
        preparingSelection = true;
        // Кэшированный оверлей сохраняет лобби при вставке карты; устаревший результат не разрешает старт.
        developerTools.gallery().clearForMatchStart();
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
        if(!mapMatchesMode(id))return false;
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

    boolean mapCapacity(MapManager.MapInfo map,int playersCount) {
        if(map==null||!map.available()||maps.bedwars(map.id())!=(config.gameMode>=3))return false;
        return map.playerCapacity()>=(config.gameMode>=3?(developerMode()?1:2):playersCount);
    }

    public void requestStart(ServerPlayer player, boolean quick) {
        Component failure = startFailure(player);
        if (failure != null) { tell(player, failure); return; }
        if (quick) {
            int count = online().size();
            List<MapManager.MapInfo> available = maps.quickStartCandidates(config.gameMode>=3?(developerMode()?1:2):count,config.broken,config.gameMode>=3);
            if (available.isEmpty()) { tell(player, text("game.quick_missing", count)); return; }
            String chosen = available.get(ThreadLocalRandom.current().nextInt(available.size())).id();
            if (!chosen.equals(readyMap) || readyBroken != config.broken) {
                if (!selectMap(player, chosen, config.broken)) return;
            }
        }
        MapManager.MapInfo map = maps.info(config.mapId);
        if (!mapCapacity(map,online().size())) {
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
        beginMatchMode(config.gameMode);
        developerTools.gallery().clearForMatchStart();
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
            if (ready.size() < (developerMode() ? 1 : 2) || !bedwars.mode() && maps.spawns().size() < ready.size()) {
                announce(text("game.cancelled_players")); restoreLobby(); return;
            }
            participants.clear();
            for (ServerPlayer player : online()) {
                player.closeContainer();
                if (!ready.contains(player)) spectators.enter(player);
            }
            List<MapManager.Spawn> spawns = new ArrayList<>(maps.spawns());
            Map<UUID,MapManager.Spawn> teamSpawns=Map.of();
            if(bedwars.mode()){
                try{
                    if(!maps.bedwars(config.mapId))throw new IllegalStateException("BedWars requires its map family");
                    teamSpawns=bedwars.prepare(ready);
                    if(!teamSpawns.keySet().containsAll(ready.stream().map(ServerPlayer::getUUID).toList()))
                        throw new IllegalStateException("Incomplete BedWars spawn assignment");
                }catch(RuntimeException error){
                    SkyWars2.LOGGER.error("BedWars start topology rejected",error);
                    announce(text("game.cancelled_players"));restoreLobby();return;
                }
            }else Collections.shuffle(spawns);
            prepareSoloTeams(ready);
            for (int i = 0; i < ready.size(); i++) {
                ServerPlayer player = ready.get(i);
                Participant member = new Participant(bedwars.mode()?teamSpawns.get(player.getUUID()):spawns.get(i), Math.max(1, config.lives));
                participants.put(player.getUUID(), member);
                player.setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(
                        maps.level().dimension(), net.minecraft.core.BlockPos.containing(member.spawn.position()),
                        member.spawn.yaw(), member.spawn.pitch()), true), false);
                resetPlayer(player, config.health, true);
                grantStartExperience(player);
                player.setGameMode(GameType.ADVENTURE);
                GameType.ADVENTURE.updatePlayerAbilities(player.getAbilities());
                player.onUpdateAbilities();
                teleport(player, member.spawn.position(), member.spawn.yaw(), member.spawn.pitch());
                var profile = players.get(player.getUUID()); profile.games++; profile.coins += 10;
                player.sendSystemMessage(rewardText("swrg.reward.start_game"));
            }
            players.save(); if(!bedwars.mode()){
                maps.cages(true);
                for(var player:ready){var member=participants.get(player.getUUID());items.cosmetics().cage(player,maps.level(),net.minecraft.core.BlockPos.containing(member.spawn.position()),gameMode());}
            }
            items.cosmetics().startSuffixes(ready,maps.level().getRandom().nextLong());
            countdownTicks = bedwars.mode()?60:Math.max(1, config.countdownSeconds) * 20;
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
        // Accepted case sessions also advance in lobby/postgame; menu refresh never ticks them.
        cases.tick();
        if (inventoryLocked()) for (ServerPlayer player : online()) enforceLobbyInventory(player);
        if (!supernovaMapTransition() || !supernovaPaused()) maps.tick();
        developerTools.tick();
        spectators.tick();
        attributeHud.tick();
        lbc.tick();
        if (cunnyRestart != null) { tickCunnyRestart(); return; }
        if (supernovaMapTransition()) {
            // The item owner advances TimeStop, accepted construction and the Supernova delay.
            // Match stages, win detection and the border remain paused throughout replacement.
            supernovaNativeTick=true;
            try { items.tick(); } finally { supernovaNativeTick=false; }
            tickSupernovaMap(); tickBalanceMap(); return;
        }
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
            if(bedwars.mode()){
                if(items.timeStop().stopped(maps.level()))return;
                bedwars.tick();
                if(bedwars.startReady()&&--countdownTicks<=0)startPlaying();
                return;
            }
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
            if(bedwars.mode()&&items.timeStop().stopped(maps.level())){items.tick();return;}
            gameTicks++;
            if (!fallEnabled && gameTicks >= 100) {
                fallEnabled = true; maps.level().getGameRules().set(GameRules.FALL_DAMAGE, true, server);
            }
            if (!bedwars.mode() && stage == Stage.PLAYING && gameTicks >= Math.max(1, config.stageOneSeconds) * 20) {
                transition(Stage.PHASE_ONE);
                if (config.stageOneEvent != 0) rewardAlive(20, "swrg.reward.stage1");
                events.stageOne(config.stageOneEvent); bar.setColor(BossEvent.BossBarColor.RED);
            }
            if (!bedwars.mode() && (stage == Stage.PLAYING || stage == Stage.PHASE_ONE) && config.stageTwoEvent != 0
                    && gameTicks >= Math.max(config.stageOneSeconds + 1, config.stageTwoSeconds) * 20) {
                transition(Stage.PHASE_TWO); rewardAlive(40, "swrg.reward.stage2"); events.stageTwo(config.stageTwoEvent);
            }
            events.tick();
            supernovaNativeTick=true;
            try { items.tick(); } finally { supernovaNativeTick=false; }
            if (supernovaMapTransition() || cunnyRestart != null) return;
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
            if(bedwars.mode()){
                if(!items.timeStop().stopped(maps.level()))bedwars.tick();
                int team=bedwars.winnerTeam();
                if(developerMode()||team<0||items.timeStop().stopped(maps.level()))lastAliveTicks=5;
                else if(--lastAliveTicks<=0)finish();
                if(lifetimeTicks%600==0)players.save();
                return;
            }
            // A player awaiting vanilla respawn still owns remaining lives.
            long eligible = participants.entrySet().stream().filter(e -> e.getValue().lives > 0
                    && !e.getValue().disconnected && server.getPlayerList().getPlayer(e.getKey()) != null
                    && !isObserver(server.getPlayerList().getPlayer(e.getKey()))).count();
            boolean waiting = participants.values().stream().anyMatch(p -> p.lives > 0 && p.awaitingRespawn && !p.disconnected);
            if (developerMode() || eligible >= 2 || waiting) lastAliveTicks = 5;
            else if (--lastAliveTicks <= 0) finish();
        } else if (stage == Stage.ENDING) {
            items.stonekivi().tick();
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
        lbc.resetLevelupMatch();
        LbcPhdCrafting.matchStarted(this);
        luckyModifiers.startMatch();
        if(!bedwars.mode()){maps.cages(false); maps.captureRespawnIslands(); maps.fillChests(false);}
        transition(Stage.PLAYING); gameTicks = 0;
        events.start();
        for (ServerPlayer player : alivePlayers()) {
            player.setGameMode(GameType.SURVIVAL); player.setHealth(player.getMaxHealth());
            if (config.vanillaKits) events.giveKit(player);
            if (config.vanillaSkills) events.giveSkill(player);
            items.magic().colors().resetResources(player);
            lbc.apply(player);
            var member=participants.get(player.getUUID());
            member.lives=items.phoenix().start(player,member.lives);player.setHealth(player.getMaxHealth());
            items.cinema().start(player,maps.centerPositions());
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

    /** Match membership, including real pending vanilla/BedWars respawns; no second lives registry. */
    List<UUID> activeMembersForBedwars(){
        return participants.entrySet().stream().filter(e->e.getValue().lives>0&&!e.getValue().disconnected)
                .map(Map.Entry::getKey).toList();
    }
    /** Called by the BedWars clock only after the replacement native player body actually exists. */
    boolean bedwarsRespawnReady(UUID id){
        var member=participants.get(id);var player=server.getPlayerList().getPlayer(id);
        if(!bedwars.mode()||!bedwars.active()||!playing()||member==null||member.disconnected||member.lives<=0
                ||!member.awaitingRespawn||player==null||!player.isAlive()||!bedwars.hasBed(player))return false;
        int level=player.experienceLevel,total=player.totalExperience;float progress=player.experienceProgress;
        spectators.remove(player);member.awaitingRespawn=false;
        resetPlayer(player,config.health,false);
        player.experienceLevel=level;player.totalExperience=total;player.experienceProgress=progress;
        player.setCamera(player);player.setInvisible(false);player.noPhysics=false;player.setNoGravity(false);
        player.setGameMode(GameType.SURVIVAL);teamColors.rejoin(player);
        player.setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(
                maps.level().dimension(),net.minecraft.core.BlockPos.containing(member.spawn.position()),
                member.spawn.yaw(),member.spawn.pitch()),true),false);
        teleport(player,member.spawn.position(),member.spawn.yaw(),member.spawn.pitch());
        player.giveExperienceLevels(30);lbc.applyBedwarsRespawn(player);
        items.cinema().bodyReady(player);
        items.phoenix().bodyReady(player);
        player.connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.empty()));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetExperiencePacket(
                player.experienceProgress,player.totalExperience,player.experienceLevel));
        bar.addPlayer(player);player.inventoryMenu.sendAllDataToRemote();return true;
    }
    /** Bed loss during the pending spectator interval cancels only this registered participant. */
    void bedwarsCancelRespawn(UUID id){
        var member=participants.get(id);if(member==null||member.disconnected)return;
        member.awaitingRespawn=false;var player=server.getPlayerList().getPlayer(id);
        if(developerMode()){
            member.lives=Math.max(1,member.lives);
            if(player!=null&&player.isAlive())respawned(player);
            return;
        }
        member.lives=0;
        if(player!=null&&player.isAlive()){
            teamColors.leave(player);spectators.enter(player);teleport(player,maps.look(),0,0);
        }
    }

    public List<ServerPlayer> alivePlayers() {
        return participants.entrySet().stream().filter(e -> e.getValue().lives > 0 && !e.getValue().disconnected
                        && !e.getValue().awaitingRespawn)
                .map(e -> server.getPlayerList().getPlayer(e.getKey())).filter(p -> p != null && !isObserver(p)).toList();
    }

    public boolean allowDamage(ServerPlayer player) {
        Participant p = participants.get(player.getUUID());
        return (!supernovaMapTransition() || !renewalPlayer(player)) && playing() && !isObserver(player)
                && p != null && p.lives > 0 && !p.disconnected && !p.awaitingRespawn;
    }

    public void died(ServerPlayer player, DamageSource damage) {
        if(vanity.death(player))return;
        items.projectileLevelups().clear(player.getUUID());
        items.cosmetics().deathSound(player);items.rainbowAura().clear(player);items.psychblindSkill().leaveOrDeath(player);
        items.oreshek().died(player,damage);
        if (!playing()) return;
        Participant p = participants.get(player.getUUID());
        if (p == null || p.lives <= 0 || p.awaitingRespawn) return;
        // Compare maximum HP before ordinary death cleanup removes event skill modifiers.
        Entity seasonalKiller=damage.getEntity()!=null?damage.getEntity():player.getKillCredit();
        if(seasonalKiller instanceof ServerPlayer killer)items.lightsabers().onKill(killer,player);
        boolean vipRespawn=items.containedSkills().protectsDeath(player,damage);
        items.machinegunSkill().clear(player);items.zeusSkill().clear(player);
        items.witchClaims().leavingOrDying(player);
        events.removePlayerSkill(player);
        items.stand().died(player);
        items.mobgun().forget(player);
        boolean bedDeath=bedwars.mode()&&bedwars.active()&&bedwars.hasBed(player)&&!vipRespawn&&!developerMode();
        boolean phoenixSelected=items.phoenix().active(player.getUUID());
        boolean phoenixRespawn=phoenixSelected&&items.phoenix().beforeDeath(player);
        if(phoenixSelected&&!phoenixRespawn){bedDeath=false;p.lives=Math.min(p.lives,1);}
        if(!bedDeath&&!vipRespawn&&!developerMode()&&items.cinema().beforeLastLifeLost(player,p.lives,false))p.lives++;
        if (developerMode()) p.lives = Math.max(1, p.lives);
        else if(!bedDeath&&!vipRespawn&&!phoenixRespawn&&!items.indolenceFingers().prepareDeath(player))p.lives--;
        p.awaitingRespawn = true;
        if(bedDeath)bedwars.death(player);
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
        // Balance resets every actual player body, including eliminated spectators.
        if(balancePendingReset.remove(player.getUUID()))resetBalancePlayer(player);
        if (supernovaMapTransition() && renewalPlayer(player)) {
            var member=participants.get(player.getUUID());
            if (member!=null && member.lives>0 && !member.disconnected) member.renewalRespawn=true;
            else spectators.enter(player);
            teleport(player,new Vec3(0,1000,0),0,0);
            return;
        }
        items.witchClaims().reconcile(player);
        attributeHud.clear(player);
        Participant p = participants.get(player.getUUID());
        if (playing() && p != null && !p.disconnected && p.lives > 0) {
            if(bedwars.mode()&&bedwars.pending(player)){
                player.setGameMode(GameType.SPECTATOR);player.setCamera(player);
                teleport(player,p.spawn.position(),p.spawn.yaw(),p.spawn.pitch());return;
            }
            if(balanceLegacy){
                p.awaitingRespawn=false;balancePendingReset.remove(player.getUUID());resetBalancePlayer(player);
                player.setGameMode(GameType.SURVIVAL);maps.restoreRespawnIsland(p.spawn);
                teleport(player,p.spawn.position(),p.spawn.yaw(),p.spawn.pitch());return;
            }
            // Полная нормализация относится к границам матча; новую жизнь готовим по прежним правилам.
            p.awaitingRespawn = false; resetPlayer(player, config.health, false);
            boolean fingerSpawn=items.indolenceFingers().fingerSpawn(player);
            if(!fingerSpawn&&!bedwars.mode())maps.restoreRespawnIsland(p.spawn);
            player.setGameMode(GameType.SURVIVAL); if(!fingerSpawn)teleport(player, p.spawn.position(), p.spawn.yaw(), p.spawn.pitch());
            if (config.vanillaKits) events.giveKit(player);
            if (config.vanillaSkills) events.giveSkill(player);
            lbc.apply(player, false);
            items.cinema().bodyReady(player);
            items.phoenix().bodyReady(player);
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
        if(bedwars.mode()&&(member==null||bedwars.team(player)<1||bedwars.pending(player)))return false;
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
        items.phoenix().bodyReady(player);items.cinema().bodyReady(player);
        items.stand().respawned(player);
        bar.addPlayer(player);
        lastAliveTicks = 5;
        player.inventoryMenu.sendAllDataToRemote();
        return true;
    }

    /** Accept once; the caller debits the old used stack before the next shared tick boundary. */
    public boolean restartForCunny(ServerPlayer owner) {
        if (!server.isSameThread() || owner == null || cunnyRestart != null || stopped || !playing()
                || supernovaMapTransition() || maps.busy() || developerTools.authoring().busy()
                || vanity.busy() || vanity.capturePreparing()
                || items.supernova().phase() != LbcSupernova.Phase.IDLE
                || config.gameMode != 1 && !(config.gameMode == 3 && BEDWARS_READY)
                || !allowDamage(owner) || !items.wands().validRoleTarget(owner, false)
                || !LbcDimensionItems.ordinary(owner.level())
                || online().size() < (developerMode() ? 1 : 2)
                || !mapCapacity(maps.info(config.mapId), online().size())) return false;
        cunnyRestart = CunnyRestartPhase.DRAINING;
        return true;
    }

    private void tickCunnyRestart() {
        if (cunnyRestart == CunnyRestartPhase.DRAINING) {
            // Producer admission changes and teardown both happen outside item/entity callbacks.
            items.duplicating().beginCheckpointDrain();
            items.cunnyTerrain().prepareRestart();
            items.tick();
            if (supernovaPaused() || !supernovaTerrainSettled() || !items.cunnyTerrain().restartSettled()) return;
            restoreLobby();
            cunnyRestart = CunnyRestartPhase.PREPARING;
            return;
        }
        if (!inLobby() || maps.busy() || preparingSelection) return;
        // This is an already accepted item action, so lobby admin ownership cannot revoke it.
        if (config.gameMode != 1 && !(config.gameMode == 3 && BEDWARS_READY)
                || online().size() < (developerMode() ? 1 : 2)
                || !mapCapacity(maps.info(config.mapId), online().size())) {
            cunnyRestart = null; announce(text("game.cancelled_players")); return;
        }
        if (!selectedMapReady()) {
            if (!mapFailure.getString().isEmpty()) { cunnyRestart = null; return; }
            prepareSelectedMap(); return;
        }
        cunnyRestart = null;
        // begin owns the sole durable +10 start award and the normal teams/lives/loadout lifecycle.
        begin();
    }

    /** Nautiz replaces the round through the same map-ready/lobby/new-match transitions as normal play. */
    void restartFromRune(ServerPlayer author){
        if(!playing()||maps.busy()||stopped||cunnyRestart!=null)return;
        stopMatch();runeRestartPending=true;
    }
    void addRuneLives(ServerPlayer author,int amount){
        var member=participants.get(author.getUUID());if(member==null)return;
        if(amount>0&&items.phoenix().convertLives(author,amount)==amount)return;
        member.lives=(int)Math.clamp((long)member.lives+amount,0L,Integer.MAX_VALUE);
        if(playing())updateBar();
    }

    void selectedSkillChanged(ServerPlayer player){
        var member=participants.get(player.getUUID());
        if(!playing()||member==null||member.lives<=0||!player.isAlive()||!canUseGameplayItems(player))return;
        member.lives=items.phoenix().start(player,member.lives);
        items.cinema().start(player,maps.centerPositions());
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
        if(amount>0)items.balance().damaged(victim);
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

    private Component bedwarsTeamName(int team){
        String key=switch(team){
            case 1->"team_aqua";case 2->"team_black";case 3->"team_blue";case 4->"team_dark_aqua";
            case 5->"team_dark_blue";case 6->"team_dark_gray";case 7->"team_dark_green";case 8->"team_dark_purple";
            case 9->"team_dark_red";case 10->"team_gold";case 11->"team_gray";
            case 12->"team_green";case 13->"team_light_purple";case 14->"team_red";case 15->"team_white";
            case 16->"team_yellow";default->throw new IllegalArgumentException("Invalid source BedWars team");
        };
        return Component.translatable(key).withStyle(style->style.withColor(ChatFormatting.WHITE).withBold(true));
    }

    private void finish() {
        boolean bedwarsResult=bedwars.mode();
        int bedwarsWinner=bedwarsResult?bedwars.winnerTeam():-1;
        List<ServerPlayer> bedwarsLiving=bedwarsResult?alivePlayers().stream()
                .filter(player->player.isAlive()&&!player.isSpectator()&&bedwars.team(player)==bedwarsWinner).toList():List.of();
        List<UUID> bedwarsRewarded=bedwarsResult&&bedwarsWinner>0?participants.keySet().stream()
                .filter(id->bedwars.assignedTeam(id)==bedwarsWinner).toList():List.of();
        bedwars.stop();
        balanceLegacy(false);balanceMap=null;balancePendingReset.clear();balancePendingRewards.clear();
        vanity.stop();
        luckyModifiers.endMatch();
        lbc.resetLevelupMatch();
        items.stop();
        events.stop(); if(!bedwarsResult)maps.cages(false); bar.setVisible(false); bar.removeAllPlayers();
        List<ServerPlayer> alive = participants.entrySet().stream().filter(e -> e.getValue().lives > 0
                && !e.getValue().disconnected).map(e -> server.getPlayerList().getPlayer(e.getKey())).filter(p -> p != null && !isObserver(p)).toList();
        ServerPlayer winner = bedwarsResult?bedwarsLiving.stream()
                .min(java.util.Comparator.comparingDouble(player->player.position().distanceToSqr(Vec3.ZERO))).orElse(null)
                :alive.size() == 1 ? alive.getFirst() : null;
        victoryWinner = winner == null ? null : winner.getUUID();
        // Team victory belongs to every enrolled teammate, including dead/offline teammates.
        List<UUID> rewarded=bedwarsResult?bedwarsRewarded:winner==null?List.of():List.of(winner.getUUID());
        for(UUID id:rewarded){var profile=players.get(id);profile.coins+=100;profile.wins++;
            profile.keys[ThreadLocalRandom.current().nextInt(4)]++;
            var onlineWinner=server.getPlayerList().getPlayer(id);
            if(onlineWinner!=null)onlineWinner.sendSystemMessage(rewardText("swrg.reward.win"));}
        if (winner != null) {
            announce(Component.translatable("swrg.title.player_win")
                    .withStyle(ChatFormatting.DARK_GREEN)
                    .append(Component.literal(" "))
                    .append(winner.getDisplayName().copy().withStyle(style -> style
                            .withColor(ChatFormatting.WHITE).withBold(true)))
                    .append(Component.literal(" "))
                    .append(Component.translatable("swrg.title.player_win2")));
        } else if(bedwarsResult&&bedwarsWinner>0){
            // A bed-entitled team can win while every member is waiting for its actual native body.
            announce(Component.translatable("swrg.title.player_win").withStyle(ChatFormatting.DARK_GREEN)
                    .append(Component.literal(" ")).append(bedwarsTeamName(bedwarsWinner))
                    .append(Component.literal(" ")).append(Component.translatable("swrg.title.player_win2")));
        } else announce(Component.translatable("swrg.title.draw")
                .withStyle(style -> style.withColor(ChatFormatting.GREEN)));
        for (ServerPlayer player : online()) {
            player.closeContainer(); lbc.clear(player);
            player.getInventory().clearContent(); player.removeAllEffects();
            resetAttributeValues(player);
            if (player == winner || bedwarsResult&&bedwarsLiving.contains(player)) player.setGameMode(GameType.CREATIVE);
            else spectators.enter(player);
            teleport(player, maps.look(), 0, 0);
        }
        maps.level().getGameRules().set(GameRules.BLOCK_DROPS, false, server);
        maps.level().getGameRules().set(GameRules.ENTITY_DROPS, false, server);
        maps.level().getGameRules().set(GameRules.MOB_DROPS, false, server);
        config.gamesPlayed++; config.save(); players.save();
        transition(Stage.ENDING); finishTicks = Math.max(1, config.endingSeconds) * 20;
        // В исходном конце игры первая четвёрка ракет появляется сразу, затем раз в секунду.
        if (winner != null) {items.cosmetics().winner(winner,gameTicks);events.victoryTick(winner, finishTicks);}
    }

    private void restoreLobby() {
        cunnyRestart=null;
        vanity.stop();
        lbc.resetLevelupMatch();
        bedwars.stop();
        runeRestartPending=false;
        supernovaMap=null;
        balanceMap=null;balanceLegacy(false);balancePendingReset.clear();balancePendingRewards.clear();
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
        activeGameMode=0;
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
        if(bedwars.mode()){bar.setVisible(false);return;}
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
        bedwars.stop();
        developerTools.close();
        vanity.stop();
        lbc.resetLevelupMatch();
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
