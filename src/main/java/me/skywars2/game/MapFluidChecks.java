package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Native lifecycle fixtures: deliberately use Level writes, never the arena tracking API. */
public final class MapFluidChecks {
    private static final List<BlockPos> sources = new ArrayList<>();
    private static final List<BlockPos> flows = new ArrayList<>();
    private static final Map<BlockPos, MapManager.BlockSnapshot> loadingCells = new LinkedHashMap<>();
    private static final List<BlockPos> forced = new ArrayList<>();
    private static BlockPos loading = new BlockPos(300, 300, 300);
    private static int loadingGeneration;
    private static int waitTicks;
    private static String waiting;
    private static int previousClear;
    private static boolean loadingWatch;
    private static boolean loadingPlanted;
    private static int suspendedTicks;
    private static int resumeTicks;
    private static String oldMap;
    private static boolean watchStop;

    private MapFluidChecks() { }

    private static void require(boolean ok, String message) {
        if (!ok) throw new IllegalStateException("Map fluids: " + message);
    }

    private static void force(GameController game, BlockPos pos) {
        var level = game.maps().level();
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        if (level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true)) forced.add(pos);
    }

    // A closed 3x4x3 chamber leaves exactly one cell below the source for native downflow.
    private static void chamber(GameController game, BlockPos base, boolean lava, boolean direct) {
        force(game, base);
        var level = game.maps().level();
        for (var mutable : BlockPos.betweenClosed(base.offset(-1, 0, -1), base.offset(1, 3, 1))) {
            var p = mutable.immutable();
            var state = p.getX() == base.getX() && p.getZ() == base.getZ()
                    && (p.getY() == base.getY() + 2 || !direct && p.getY() == base.getY() + 1)
                    ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState();
            if (direct) level.getChunk(p.getX() >> 4, p.getZ() >> 4).setBlockState(p, state, 2);
            else level.setBlock(p, state, 2);
        }
        var source = base.above(2);
        var state = (lava ? Blocks.LAVA : Blocks.WATER).defaultBlockState();
        if (direct) level.getChunk(source.getX() >> 4, source.getZ() >> 4).setBlockState(source, state, 2);
        else level.setBlock(source, state, 3);
        if (!direct) level.scheduleTick(source, lava ? Fluids.LAVA : Fluids.WATER, 1);
        sources.add(source);
        if (!direct) flows.add(base.above());
    }

    private static void plant(GameController game, int x) {
        for (int y : new int[]{300, -30}) {
            chamber(game, new BlockPos(x, y, 220), false, false);
            chamber(game, new BlockPos(x + 5, y, 220), true, false);
        }
        // A reload-style direct chunk write bypasses Level.setBlock's mutation callback.
        // Every chamber write also bypasses Level tracking, in a separate section
        // from ordinary probes, so resident palette recovery must find the source.
        chamber(game, new BlockPos(x + 50, -48, 272), false, true);
        var waterlogged = new BlockPos(x + 10, 302, 220);
        force(game, waterlogged);
        for (var mutable : BlockPos.betweenClosed(waterlogged.offset(-1, -1, -1), waterlogged.offset(1, 1, 1)))
            game.maps().level().setBlock(mutable, Blocks.STONE.defaultBlockState(), 2);
        game.maps().level().setBlock(waterlogged, Blocks.OAK_SLAB.defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, true), 3);
        sources.add(waterlogged);
    }

    private static void weather(GameController game) {
        var data = game.server().getWeatherData();
        require(!data.isRaining() && !data.isThundering(), "lobby clears native rain and thunder flags");
        require(data.getClearWeatherTime() >= Integer.MAX_VALUE - 400,
                "lobby sets near-maximum clear timer: " + data.getClearWeatherTime());
        // Native clear-weather ticking sets rain/thunder timers to zero on its next tick.
        require(data.getRainTime() == Integer.MAX_VALUE || data.getRainTime() <= 1,
                "rain timer is reset by transition/native clear-weather ticking");
        require(data.getThunderTime() == Integer.MAX_VALUE || data.getThunderTime() <= 1,
                "thunder timer is reset by transition/native clear-weather ticking");
    }

    public static void run(GameController game, String suite) {
        switch (suite) {
            case "map-fluids-playing" -> {
                require(game.playing(), "fixture requires a real match");
                require(sources.isEmpty(), "previous match probes were verified and cleared");
                plant(game, 220);
                game.server().setWeatherParameters(0, 6000, true, true);
                waiting = "storm"; waitTicks = 40; watchStop = true;
            }
            case "map-fluids-lobby" -> {
                require(game.inLobby() && !game.maps().busy(), "stop completed into ready lobby");
                weather(game);
                for (var p : sources) require(!game.maps().level().getFluidState(p).isEmpty(),
                        "stop preserves post-match fluid at " + p);
                plant(game, 250);
                previousClear = game.server().getWeatherData().getClearWeatherTime();
                waiting = "lobby-flow"; waitTicks = 80;
            }
            case "map-fluids-arm-loading" -> {
                require(game.inLobby() && !game.maps().busy(), "arm before actual map selection");
                oldMap = game.config().mapId;
                // Each selection uses a fresh chamber outside every previous mutation bound.
                // Reusing a restored chamber would correctly place it in the next cleanup.
                int loadingCoordinate = 300 + 80 * ++loadingGeneration;
                loading = new BlockPos(loadingCoordinate, 300, loadingCoordinate);
                loadingWatch = true; loadingPlanted = false; suspendedTicks = 0; resumeTicks = 0;
            }
            case "map-fluids-cleared" -> {
                require(game.inLobby() && game.selectedMapReady(), "actual map callback finished");
                require(!game.config().mapId.equals(oldMap), "selected a different actual map");
                require(!loadingWatch && suspendedTicks >= 8, "observed native loading pause for >=8 ticks");
                for (var p : sources) require(game.maps().level().getFluidState(p).isEmpty(),
                        "replacement clears source/waterlogged fluid at " + p);
                for (var p : flows) require(game.maps().level().getFluidState(p).isEmpty(),
                        "replacement clears actual native downflow at " + p);
                sources.clear(); flows.clear();
                for (var p : forced) game.maps().level().setChunkForced(p.getX() >> 4, p.getZ() >> 4, false);
                forced.clear();
                weather(game);
            }
            default -> throw new IllegalArgumentException(suite);
        }
    }

    public static void tick(GameController game) {
        if (waiting == null && !loadingWatch && !watchStop) return;
        try {
            if (watchStop && game.inLobby()) {
                var data = game.server().getWeatherData();
                require(!data.isRaining() && !data.isThundering()
                        && data.getClearWeatherTime() == Integer.MAX_VALUE
                        && data.getRainTime() == Integer.MAX_VALUE
                        && data.getThunderTime() == Integer.MAX_VALUE,
                        "LOBBY transition sets exact maximum weather timers and clear flags");
                watchStop = false;
                SkyWars2.LOGGER.info("[check] PASS map-fluids-weather-transition-native");
            }
            if (waiting != null && --waitTicks == 0) {
                String finished = waiting; waiting = null;
                if (finished.equals("storm")) {
                    var data = game.server().getWeatherData();
                    require(game.playing() && data.isRaining() && data.isThundering(),
                            "native storm persists for 40 match ticks");
                } else {
                    weather(game);
                    boolean advances = game.server().overworld().getGameRules().get(
                            net.minecraft.world.level.gamerules.GameRules.ADVANCE_WEATHER);
                    int clear = game.server().getWeatherData().getClearWeatherTime();
                    require(advances ? clear < previousClear - 40 : clear == previousClear,
                            "native clear timer obeys ADVANCE_WEATHER: " + advances + "/" + clear);
                    for (var p : flows) require(!game.maps().level().getFluidState(p).isEmpty(),
                            "scheduled native fluid tick spreads in lobby at " + p);
                }
                SkyWars2.LOGGER.info("[check] PASS map-fluids-{}-native", finished);
            }
            if (loadingWatch && game.maps().suspendsFluidTicks()) {
                if (!loadingPlanted) {
                    force(game, loading);
                    for (var p : BlockPos.betweenClosed(loading.offset(-1, 0, -1), loading.offset(1, 3, 1)))
                        loadingCells.put(p.immutable(), game.maps().snapshot(p));
                    int sourceCount = sources.size(), flowCount = flows.size();
                    chamber(game, loading, false, false);
                    sources.subList(sourceCount, sources.size()).clear();
                    flows.subList(flowCount, flows.size()).clear();
                    loadingPlanted = true;
                }
                require(game.maps().level().getFluidState(loading.above()).isEmpty(),
                        "scheduled native downflow remains paused during map replacement");
                suspendedTicks++;
            } else if (loadingWatch && loadingPlanted && !game.maps().busy()) {
                require(suspendedTicks >= 8, "map replacement had enough native scheduled ticks to prove suspension");
                if (++resumeTicks < 20) return;
                require(!game.maps().level().getFluidState(loading.above()).isEmpty(),
                        "queued native fluid tick resumes after map loading finishes at " + loading
                                + ": source=" + game.maps().level().getBlockState(loading.above(2))
                                + ", target=" + game.maps().level().getBlockState(loading.above())
                                + ", resident=" + (game.maps().level().getChunkSource().getChunkNow(
                                        loading.getX() >> 4, loading.getZ() >> 4) != null));
                for (var entry : loadingCells.entrySet())
                    game.maps().restoreSnapshot(game.maps().level(), entry.getKey(), entry.getValue(), 2);
                loadingCells.clear(); loadingWatch = false;
                SkyWars2.LOGGER.info("[check] PASS map-fluids-loading-native ticks={}", suspendedTicks);
            }
        } catch (RuntimeException failure) {
            waiting = null; loadingWatch = false; watchStop = false;
            SkyWars2.LOGGER.error("[check] FAIL map-fluids-native: {}", failure.getMessage(), failure);
        }
    }
}
