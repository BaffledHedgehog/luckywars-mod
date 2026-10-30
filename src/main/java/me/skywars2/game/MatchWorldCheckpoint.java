package me.skywars2.game;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.skywars2.mixin.WorldCheckpointAccess;
import net.minecraft.core.Holder;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.*;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.gamerules.*;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.PrimaryLevelData;

/** GLOBAL native metadata/timeline owner. Compose before terrain, bodies and final world-RNG replay.
 * Does not replace saved-data services, dimensions, listeners or resource registries. Not the full rewind. */
final class MatchWorldCheckpoint {
    // The native BorderExtent interface is private. One exact reviewed field retains its original
    // object/immutable lerp anchors; the mutable primitives use typed mixins. No codec/restarted lerp.
    private static final Field EXTENT=extentField();
    private static Field extentField(){try{var f=WorldBorder.class.getDeclaredField("extent");f.setAccessible(true);return f;}catch(ReflectiveOperationException e){throw new ExceptionInInitializerError(e);}}
    static Object extent(WorldBorder border){try{return EXTENT.get(border);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static void extent(WorldBorder border,Object value){try{EXTENT.set(border,value);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private record Border(WorldBorder owner,WorldBorder.Settings settings,int maximum,boolean initialized,Object original,long remaining,double size,double previous) {
        Border(WorldBorder border){this(border,new WorldBorder.Settings(border),border.getAbsoluteMaxSize(),((WorldCheckpointAccess.Border)border).skywars2$initialized(),extent(border),
                extent(border) instanceof WorldCheckpointAccess.Moving m?m.skywars2$remaining():0,
                extent(border) instanceof WorldCheckpointAccess.Moving m?m.skywars2$size():0,
                extent(border) instanceof WorldCheckpointAccess.Moving m?m.skywars2$previous():0);
            if(!(original instanceof WorldCheckpointAccess.Moving)&&!(original instanceof WorldCheckpointAccess.Static))throw new IllegalStateException("Unknown native border extent");}
        void restore(){var a=(WorldCheckpointAccess.Border)owner;a.skywars2$x(settings.centerX());a.skywars2$z(settings.centerZ());a.skywars2$damage(settings.damagePerBlock());a.skywars2$safe(settings.safeZone());a.skywars2$blocks(settings.warningBlocks());a.skywars2$time(settings.warningTime());a.skywars2$max(maximum);a.skywars2$initialized(initialized);
            extent(owner,original);if(original instanceof WorldCheckpointAccess.Moving m){m.skywars2$remaining(remaining);m.skywars2$size(size);m.skywars2$previous(previous);}else ((WorldCheckpointAccess.Static)original).skywars2$rebuild();owner.setDirty();}
    }
    private record World(ServerLevel level,Border border,float oldRain,float rain,float oldThunder,float thunder,int sky) {
        World(ServerLevel level){this(level,new Border(level.getWorldBorder()),((WorldCheckpointAccess.Weather)level).skywars2$oldRain(),((WorldCheckpointAccess.Weather)level).skywars2$rain(),
                ((WorldCheckpointAccess.Weather)level).skywars2$oldThunder(),((WorldCheckpointAccess.Weather)level).skywars2$thunder(),((WorldCheckpointAccess.Weather)level).skywars2$sky());}
        void validate(MinecraftServer server){if(server.getLevel(level.dimension())!=level||level.getWorldBorder()!=border.owner)throw new IllegalStateException("Reconcile native dimension/border owners before metadata replay");}
        void restore(){border.restore();var a=(WorldCheckpointAccess.Weather)level;a.skywars2$oldRain(oldRain);a.skywars2$rain(rain);a.skywars2$oldThunder(oldThunder);a.skywars2$thunder(thunder);a.skywars2$sky(sky);level.environmentAttributes().invalidateTickCache();}
    }
    private record Clock(ServerClockManager.ServerClockInstance original,ClockState state,Map<ResourceKey<ClockTimeMarker>,ClockTimeMarker> markers) {
        Clock(ServerClockManager.ServerClockInstance clock){this(clock,clock.packState(),Map.copyOf(((WorldCheckpointAccess.Clock)clock).skywars2$markers()));}
        void restore(){original.loadFrom(state);var map=((WorldCheckpointAccess.Clock)original).skywars2$markers();map.clear();map.putAll(markers);}
    }
    private record Weather(WeatherData original,int clear,int rain,int thunder,boolean raining,boolean thundering) {
        Weather(WeatherData owner){this(owner,owner.getClearWeatherTime(),owner.getRainTime(),owner.getThunderTime(),owner.isRaining(),owner.isThundering());}
        void restore(){original.setClearWeatherTime(clear);original.setRainTime(rain);original.setThunderTime(thunder);original.setRaining(raining);original.setThundering(thundering);}
    }
    private final GameController game;
    private final String epoch;
    private final Object resources,registries,worldData;
    private final PrimaryLevelData primary;
    private final LevelSettings settings;
    private final LevelData.RespawnData spawn,effectiveSpawn;
    private final long time;
    private final boolean initialized;
    private final List<World> worlds;
    private final ServerClockManager clocks;
    private final Map<Holder<WorldClock>,ServerClockManager.ServerClockInstance> clockMap;
    private final Map<Holder<WorldClock>,Clock> clockImages=new LinkedHashMap<>();
    private final Weather weather;
    private final GameRules rules;
    private final GameRuleMap ruleMap;
    private final Map<GameRule<?>,Object> nativeRules,values;
    MatchWorldCheckpoint(GameController game){
        this.game=game;var server=game.server();if(!server.isSameThread()||!game.playing()||game.maps().busy())throw new IllegalStateException("Capture metadata on the stable native match thread");epoch=game.items().epoch();
        resources=server.getResourceManager();registries=server.reloadableRegistries();worldData=server.getWorldData();
        if(!(server.getWorldData().overworldData() instanceof PrimaryLevelData p))throw new IllegalStateException("Unsupported primary level-data owner");primary=p;
        settings=((WorldCheckpointAccess.Primary)primary).skywars2$settings();time=primary.getGameTime();initialized=primary.isInitialized();spawn=copy(primary.getRespawnData());effectiveSpawn=copy(server.getRespawnData());
        var all=new ArrayList<World>();for(var world:server.getAllLevels())all.add(new World(world));worlds=List.copyOf(all);
        clocks=server.clockManager();clockMap=((WorldCheckpointAccess.Clocks)clocks).skywars2$clocks();clockMap.forEach((k,v)->clockImages.put(k,new Clock(v)));weather=new Weather(server.getWeatherData());
        rules=server.getGameRules();ruleMap=((WorldCheckpointAccess.Rules)rules).skywars2$rules();nativeRules=((WorldCheckpointAccess.RuleValues)(Object)ruleMap).skywars2$values();
        for(var value:nativeRules.values())if(!(value instanceof Boolean)&&!(value instanceof Integer))throw new IllegalStateException("Unsupported mutable native rule value");values=Map.copyOf(nativeRules);

    }
    private static LevelData.RespawnData copy(LevelData.RespawnData s){return new LevelData.RespawnData(GlobalPos.of(s.dimension(),s.pos().immutable()),s.yaw(),s.pitch());}

    void restoreFrozen(){
        var server=game.server();if(!server.isSameThread()||!server.tickRateManager().isFrozen()||!game.playing()||game.maps().busy()||!game.items().currentEpoch(epoch))throw new IllegalStateException("Restore metadata in the same frozen native match");
        if(server.getWorldData()!=worldData||server.getWorldData().overworldData()!=primary||server.getResourceManager()!=resources||server.reloadableRegistries()!=registries||!((WorldCheckpointAccess.Primary)primary).skywars2$settings().dataConfiguration().equals(settings.dataConfiguration()))throw new IllegalStateException("Reconcile native resources/world data before metadata replay");
        if(server.clockManager()!=clocks||((WorldCheckpointAccess.Clocks)clocks).skywars2$clocks()!=clockMap||server.getWeatherData()!=weather.original||server.getGameRules()!=rules||((WorldCheckpointAccess.Rules)rules).skywars2$rules()!=ruleMap||((WorldCheckpointAccess.RuleValues)(Object)ruleMap).skywars2$values()!=nativeRules)throw new IllegalStateException("Native metadata service identity changed");
        int count=0;for(var world:server.getAllLevels())count++;if(count!=worlds.size())throw new IllegalStateException("Reconcile native dimensions before metadata replay");for(var world:worlds)world.validate(server);
        // All preflight checks finish before any mutation. Saved immutable records cannot be consumed.
        ((WorldCheckpointAccess.Primary)primary).skywars2$settings(settings);primary.setGameTime(time);primary.setInitialized(initialized);primary.setSpawn(spawn);((WorldCheckpointAccess.Server)server).skywars2$effectiveSpawn(effectiveSpawn);
        nativeRules.clear();nativeRules.putAll(values);ruleMap.setDirty();weather.restore();
        clockMap.clear();clockImages.forEach((key,image)->{image.restore();clockMap.put(key,image.original);});clocks.setDirty();
        for(var world:worlds)world.restore();
        // Run native derived-rule notifications against the restored values, retaining current transports.
        values.forEach((key,value)->notifyRule(server,key,value));server.updateMobSpawningFlags();
        publish();
    }
    @SuppressWarnings({"unchecked","rawtypes"}) private static void notifyRule(MinecraftServer server,GameRule<?> rule,Object value){server.onGameRuleChanged((GameRule)rule,value);}
    void publish(){
        var server=game.server();if(!server.isSameThread()||!server.tickRateManager().isFrozen())throw new IllegalStateException("Publish metadata on frozen native thread");
        for(var player:server.getPlayerList().getPlayers()){
            var level=player.level();player.connection.send(server.clockManager().createFullSyncPacket());player.connection.send(new ClientboundInitializeBorderPacket(level.getWorldBorder()));
            player.connection.send(new ClientboundSetDefaultSpawnPositionPacket(level.getRespawnData()));player.connection.send(new ClientboundChangeDifficultyPacket(level.getLevelData().getDifficulty(),level.getLevelData().isDifficultyLocked()));
            player.connection.send(new ClientboundGameEventPacket(level.isRaining()?ClientboundGameEventPacket.START_RAINING:ClientboundGameEventPacket.STOP_RAINING,0));
            var w=(WorldCheckpointAccess.Weather)level;player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,w.skywars2$rain()));player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE,w.skywars2$thunder()));
        }
    }
}
