package me.skywars2.game;

import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.levelgen.FlatLevelSource;

/** Checks the actual native world used by lobby, match and respawn transitions. */
public final class MapWorldChecks {
    private MapWorldChecks() { }

    private static void require(boolean value, String reason) {
        if (!value) throw new IllegalStateException("Map world: " + reason);
    }

    private static void world(GameController game) {
        var overworld = game.server().overworld();
        require(overworld != null && game.maps().level() == overworld,
                "map manager uses the actual server overworld instance");
        require(overworld.dimension().equals(Level.OVERWORLD), "native dimension is minecraft:overworld");
        // Inspect the persisted journal consumed by the real map manager, not a
        // synthetic JSON value. The old dimensionless journal is never opened.
        var journal = game.server().getWorldPath(LevelResource.ROOT)
                .resolve("data/luckywars/overworld-bounds.json");
        try {
            var root = JsonParser.parseString(Files.readString(journal, StandardCharsets.UTF_8)).getAsJsonObject();
            require(root.get("version").getAsInt() == 2, "dimension-bound journal version");
            require(root.get("dimension").getAsString().equals(Level.OVERWORLD.identifier().toString()),
                    "persisted cleanup bounds belong to minecraft:overworld");
            require(!root.getAsJsonArray("bounds").isEmpty(), "native map placement persisted cleanup bounds");
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Map world: cannot read native cleanup journal", error);
        }
    }

    public static void run(GameController game) {
        world(game);
        var level = game.server().overworld();
        // Saved WorldGenSettings can still name the old noise preset. Check the
        // actual baked generator used by ServerChunkCache, then request FULL
        // chunks far outside the imported maps so old terrain cannot mask it.
        var generator = level.getChunkSource().getGenerator();
        require(generator instanceof FlatLevelSource, "actual Overworld generator must be void-flat: "
                + generator.getClass().getSimpleName());
        var settings = ((FlatLevelSource) generator).settings();
        require(settings.getLayers().stream().allMatch(state -> state == null || state.isAir()),
                "Overworld flat layers contain no solid blocks or fluids");
        require(settings.structureOverrides().isPresent()
                && settings.structureOverrides().get().size() == 0, "no generated structures");
        for (int offset = 0; offset < 2; offset++) {
            var chunk = level.getChunk(75000 + offset, -75001);
            for (var section : chunk.getSections())
                require(section.hasOnlyAir(), "fresh FULL Overworld chunk contains terrain/fluids/features");
        }
        if (game.inLobby()) {
            var players = game.server().getPlayerList().getPlayers();
            require(!players.isEmpty(), "actual connected lobby players");
            for (var player : players)
                require(player.level() == game.server().overworld(), "lobby player remains in overworld");
            SkyWars2.LOGGER.info("[check] PASS map-world (native lobby)");
        } else {
            require(game.playing(), "ready lobby or actual match");
            var players = game.alivePlayers();
            require(players.size() >= 2, "actual match participants");
            require(!game.maps().spawns().isEmpty(), "native map spawn markers are available");
            for (var player : players)
                require(player.level() == game.server().overworld(), "match participant remains in overworld");
            SkyWars2.LOGGER.info("[check] PASS map-world (native match)");
        }
    }

    /** Invoke after an actual remaining-life native respawn, using the replacement player. */
    public static void respawn(GameController game, ServerPlayer player) {
        world(game);
        require(game.playing() && game.lives(player.getUUID()) > 0, "remaining-life match respawn");
        require(!game.awaitingRespawn(player.getUUID()) && player.isAlive(), "native respawn completed");
        require(player.level() == game.server().overworld(), "replacement player respawned in overworld");
        require(game.maps().spawns().stream().anyMatch(spawn ->
                spawn.position().distanceToSqr(player.position()) < 16), "respawn uses an imported map spawn");
        SkyWars2.LOGGER.info("[check] PASS map-world-respawn");
    }
}
