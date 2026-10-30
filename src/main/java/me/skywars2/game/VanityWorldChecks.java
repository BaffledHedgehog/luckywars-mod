package me.skywars2.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.WorldCheckpointAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.Difficulty;
import net.minecraft.world.clock.ClockState;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.PrimaryLevelData;

/** Native dimension metadata, fractional clocks and exact continuation across real world ticks. */
public final class VanityWorldChecks {
    private VanityWorldChecks() { }
    private static Fixture running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity world: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final MatchCoreCheckpoint originalCore;final MatchWorldCheckpoint original;
        final MatchWorldRandomCheckpoint originalRandom;final ChefChecks.Fixture base;final boolean frozen;
        final Map<ServerPlayer,ServerGamePacketListenerImpl> connections=new LinkedHashMap<>();
        final Map<ServerPlayer,ElementalGrimoireChecks.Recording> recordings=new LinkedHashMap<>();
        MatchWorldCheckpoint image;List<Object> expected,afterFour;List<WorldBorder> borders;List<ServerClockManager.ServerClockInstance> borrowed;
        Object clockMap,rulesMap;final ServerLevel otherLevel;long time;int tick,phase=-1,start;boolean closed;
        Fixture(GameController game){this.game=game;require(game.playing()&&!game.maps().busy(),"stable actual match");frozen=game.server().tickRateManager().isFrozen();require(!frozen,"owned native freeze");
            originalCore=new MatchCoreCheckpoint(game);original=new MatchWorldCheckpoint(game);originalRandom=new MatchWorldRandomCheckpoint(game);base=new ChefChecks.Fixture(game);otherLevel=base.other.level();}
        void init(){base.init();for(var p:List.of(base.owner,base.other)){connections.put(p,p.connection);var recording=new ElementalGrimoireChecks.Recording(p);recordings.put(p,recording);
            p.connection=new ServerGamePacketListenerImpl(game.server(),recording,p,new CommonListenerCookie(p.getGameProfile(),0,p.clientInformation(),false));p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());}
            var nether=game.server().getLevel(Level.NETHER);require(nether!=null,"native second dimension available");base.other.teleportTo(nether,base.at.x,base.at.y,base.at.z,java.util.Set.of(),0,0,true);base.other.setNoGravity(true);ack(base.other);SkyWars2.LOGGER.info("[check] START vanity-world");}
        void ack(ServerPlayer player){player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            var teleports=recordings.get(player).packets.stream().filter(ClientboundPlayerPositionPacket.class::isInstance).map(ClientboundPlayerPositionPacket.class::cast).toList();require(!teleports.isEmpty(),"native dimension transfer emitted a real teleport ID");
            player.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(teleports.getLast().id(),player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot()));require(!player.isChangingDimension()&&player.connection.hasClientLoaded(),"native teleport/load acknowledgements end dimension-transfer immunity");}
        PrimaryLevelData primary(){return (PrimaryLevelData)game.server().getWorldData().overworldData();}
        WorldCheckpointAccess.Clocks clocks(){return (WorldCheckpointAccess.Clocks)game.server().clockManager();}
        List<Object> trajectory(){var out=new ArrayList<Object>();for(var c:clocks().skywars2$clocks().values())out.add(c.packState());for(var b:borders){out.add(new WorldBorder.Settings(b));out.add(b.getLerpSpeed());out.add(b.getMinX(0));out.add(b.getMaxX(.37f));out.add(b.getMinZ(.85f));out.add(b.getStatus());}return List.copyOf(out);}
        List<Object> probe(){var out=new ArrayList<Object>();out.add(primary().getGameTime());out.add(((WorldCheckpointAccess.Primary)primary()).skywars2$settings());out.add(primary().getRespawnData());out.add(game.server().getRespawnData());out.add(primary().isInitialized());
            var rules=((WorldCheckpointAccess.Rules)game.server().getGameRules()).skywars2$rules();out.add(Map.copyOf(((WorldCheckpointAccess.RuleValues)(Object)rules).skywars2$values()));
            var weather=game.server().getWeatherData();out.add(List.of(weather.getClearWeatherTime(),weather.getRainTime(),weather.getThunderTime(),weather.isRaining(),weather.isThundering()));out.add(trajectory());
            for(var level:game.server().getAllLevels()){var w=(WorldCheckpointAccess.Weather)level;out.add(List.of(level.dimension(),w.skywars2$oldRain(),w.skywars2$rain(),w.skywars2$oldThunder(),w.skywars2$thunder(),w.skywars2$sky(),level.getRainLevel(.3f),level.getThunderLevel(.6f),level.getWorldBorder().getAbsoluteMaxSize()));
                var markers=new ArrayList<Object>();for(var c:clocks().skywars2$clocks().values())markers.add(Map.copyOf(((WorldCheckpointAccess.Clock)c).skywars2$markers()));out.add(markers);}
            return List.copyOf(out);}

        void setup(){
            game.server().tickRateManager().setFrozen(true);var server=game.server();
            var rules=server.getGameRules();rules.set(GameRules.ADVANCE_TIME,true,server);rules.set(GameRules.ADVANCE_WEATHER,false,server);rules.set(GameRules.SPAWN_MONSTERS,false,server);rules.set(GameRules.RANDOM_TICK_SPEED,0,server);rules.set(GameRules.REDUCED_DEBUG_INFO,true,server);rules.set(GameRules.LIMITED_CRAFTING,true,server);rules.set(GameRules.IMMEDIATE_RESPAWN,false,server);
            var settings=((WorldCheckpointAccess.Primary)primary()).skywars2$settings();((WorldCheckpointAccess.Primary)primary()).skywars2$settings(settings.withGameType(GameType.ADVENTURE).withDifficulty(Difficulty.HARD).withDifficultyLock(true).withAllowCommands(false));
            server.setRespawnData(LevelData.RespawnData.of(Level.OVERWORLD,BlockPos.containing(base.at),31,-17));primary().setInitialized(true);time=primary().getGameTime();
            int i=0;borrowed=new ArrayList<>();for(var c:clocks().skywars2$clocks().values()){c.loadFrom(new ClockState(9000+i,.625f,i%2==0?.375f:2.25f,i%2!=0));borrowed.add(c);i++;}
            var weather=server.getWeatherData();weather.setClearWeatherTime(71);weather.setRainTime(93);weather.setThunderTime(117);weather.setRaining(true);weather.setThundering(true);
            borders=new ArrayList<>();i=0;for(var level:server.getAllLevels()){var b=level.getWorldBorder();b.setCenter(base.at.x+i*3,base.at.z-i*2);b.setAbsoluteMaxSize(100000+i);b.setDamagePerBlock(.11+i*.01);b.setSafeZone(2.25+i);b.setWarningBlocks(4+i);b.setWarningTime(6+i);
                if(i%2==0){b.lerpSizeBetween(50000+i*100,(i%3==2?51000:49000)+i*100,60,time);for(int n=0;n<7;n++)b.tick();}else b.setSize(45000+i);borders.add(b);
                var w=(WorldCheckpointAccess.Weather)level;w.skywars2$oldRain(.11f+i*.01f);w.skywars2$rain(.28f+i*.01f);w.skywars2$oldThunder(.32f+i*.01f);w.skywars2$thunder(.43f+i*.01f);w.skywars2$sky(4+i);i++;}

            clockMap=clocks().skywars2$clocks();rulesMap=((WorldCheckpointAccess.RuleValues)(Object)((WorldCheckpointAccess.Rules)rules).skywars2$rules()).skywars2$values();
            image=new MatchWorldCheckpoint(game);expected=probe();for(int n=0;n<4;n++){server.clockManager().tick();borders.forEach(WorldBorder::tick);}afterFour=trajectory();image.restoreFrozen();verify();
            // A live restore must reject before altering even the game-time field.
            server.tickRateManager().setFrozen(false);boolean rejected=false;try{image.restoreFrozen();}catch(IllegalStateException wanted){rejected=true;}server.tickRateManager().setFrozen(true);require(rejected&&probe().equals(expected),"live restore rejected before mutation");
            mutate();image.restoreFrozen();verify();start=game.matchTicks();server.tickRateManager().setFrozen(false);phase=0;
        }
        void mutate(){var server=game.server();primary().setGameTime(time+123);primary().setInitialized(false);((WorldCheckpointAccess.Primary)primary()).skywars2$settings(((WorldCheckpointAccess.Primary)primary()).skywars2$settings().withGameType(GameType.CREATIVE).withDifficulty(Difficulty.PEACEFUL).withDifficultyLock(false).withAllowCommands(true));server.setRespawnData(LevelData.RespawnData.of(Level.NETHER,new BlockPos(7,90,11),-15,45));
            var rules=((WorldCheckpointAccess.Rules)server.getGameRules()).skywars2$rules();rules.remove(GameRules.RANDOM_TICK_SPEED);rules.set(GameRules.REDUCED_DEBUG_INFO,false);rules.set(GameRules.ADVANCE_TIME,false);rules.set(GameRules.LIMITED_CRAFTING,false);rules.set(GameRules.IMMEDIATE_RESPAWN,true);
            var weather=server.getWeatherData();weather.setClearWeatherTime(999);weather.setRainTime(998);weather.setThunderTime(997);weather.setRaining(false);weather.setThundering(false);
            for(var c:borrowed){c.loadFrom(new ClockState(123,.125f,3,false));((WorldCheckpointAccess.Clock)c).skywars2$markers().clear();}var keys=List.copyOf(clocks().skywars2$clocks().keySet());clocks().skywars2$clocks().clear();clocks().skywars2$clocks().put(keys.getFirst(),new ServerClockManager.ServerClockInstance());
            for(var level:server.getAllLevels()){var b=level.getWorldBorder();b.setCenter(-200,300);b.setSize(65000);b.setAbsoluteMaxSize(999999);b.setWarningBlocks(90);b.setWarningTime(99);b.setSafeZone(123);b.setDamagePerBlock(77);level.setRainLevel(.95f);level.setThunderLevel(.92f);((WorldCheckpointAccess.Weather)level).skywars2$sky(15);}
            recordings.values().forEach(r->r.packets.clear());}

        void verify(){require(probe().equals(expected),"exact independent image: all native settings/spawn/rules/clocks/weather/interpolation/borders");require(clocks().skywars2$clocks()==clockMap&&borrowed.equals(new ArrayList<>(clocks().skywars2$clocks().values()))&&((WorldCheckpointAccess.RuleValues)(Object)((WorldCheckpointAccess.Rules)game.server().getGameRules()).skywars2$rules()).skywars2$values()==rulesMap,"borrowed native owners/clock instances and original containers survive replay");
            for(var entry:recordings.entrySet()){var p=entry.getKey();var packets=entry.getValue().packets;var border=packets.stream().filter(x->x instanceof ClientboundInitializeBorderPacket).map(x->(ClientboundInitializeBorderPacket)x).reduce((a,b)->b).orElseThrow();require(border.getNewCenterX()==p.level().getWorldBorder().getCenterX()&&border.getOldSize()==p.level().getWorldBorder().getSize()&&border.getLerpTime()==p.level().getWorldBorder().getLerpTime(),"native border publication uses the CURRENT player's own dimension");
                var clocks=packets.stream().filter(x->x instanceof ClientboundSetTimePacket).map(x->(ClientboundSetTimePacket)x).reduce((a,b)->b).orElseThrow();require(clocks.gameTime()==time&&clocks.clockUpdates().equals(game.server().clockManager().createFullSyncPacket().clockUpdates()),"full fractional clock packet and game time");
                require(packets.stream().anyMatch(x->x instanceof ClientboundChangeDifficultyPacket)&&packets.stream().anyMatch(x->x instanceof ClientboundSetDefaultSpawnPositionPacket)&&packets.stream().filter(x->x instanceof ClientboundGameEventPacket).count()>=5,"native weather, spawn, difficulty and rule packets sent");}
        }
        boolean step(){require(++tick<120,"bounded native metadata fixture");if(phase==-1){setup();return false;}if(game.matchTicks()-start<4)return false;
            game.server().tickRateManager().setFrozen(true);require(primary().getGameTime()==time+4&&trajectory().equals(afterFour),"native clock/border continuation matches the original four-tick trajectory, including paused clocks and previous border size");
            mutate();image.restoreFrozen();verify();if(phase++==0){start=game.matchTicks();game.server().tickRateManager().setFrozen(false);return false;}
            var c=borrowed.getFirst();c.loadFrom(new ClockState(731,.875f,2.25f,true));var paused=new MatchWorldCheckpoint(game);var state=c.packState();c.loadFrom(new ClockState(999,0,3,false));paused.restoreFrozen();game.server().clockManager().tick();require(c.packState().equals(state),"paused native clock retains fractional state and rate through restore and actual manager tick");image.restoreFrozen();verify();return true;
        }
        @Override public void close(){if(closed)return;closed=true;game.server().tickRateManager().setFrozen(true);try{original.restoreFrozen();if(base.other.level()!=otherLevel){base.other.teleportTo(otherLevel,base.at.x,base.at.y,base.at.z,java.util.Set.of(),0,0,true);ack(base.other);}for(var p:connections.keySet())p.connection=connections.get(p);base.close();require(base.owner.level()==game.maps().level()&&base.other.level()==otherLevel&&!base.other.isChangingDimension()&&base.other.connection.hasClientLoaded(),"cross-dimension fixture returns both borrowed players to their original worlds");originalCore.restoreFrozen();originalRandom.restoreFrozen();}finally{game.server().tickRateManager().setFrozen(frozen);}}
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.init();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS vanity-world (native global fractional/paused clocks, saved weather and per-dimension interpolation/borders with original lerp trajectory, immutable world settings/spawn and raw rules; current cross-dimension transports; two exact replays across eight real world ticks and explicit paused-clock continuation; both borrowed players return to their original worlds; full coordinator remains gated)");}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL vanity-world",error);}}
}
