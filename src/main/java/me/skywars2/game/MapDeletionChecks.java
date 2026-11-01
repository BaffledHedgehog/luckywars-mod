package me.skywars2.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.skywars2.SkyWars2;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Real test-server authorization, atomic tombstoning, restart visibility and ID reuse checks. */
public final class MapDeletionChecks {
    private MapDeletionChecks() { }
    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException("Map deletion: " + message);
    }
    public static void run(GameController game) {
        ServerPlayer owner = game.server().getPlayerList().getPlayers().stream()
                .filter(p -> p.getGameProfile().name().equals("BaffledHedgehog")).findFirst().orElse(null);
        require(owner != null && MapEditing.allowedDeletion(owner, game.server()),
                "requires the actual authorized BaffledHedgehog operator on the isolated test server");
        ServerPlayer other = game.server().getPlayerList().getPlayers().stream()
                .filter(p -> !p.getGameProfile().name().equals("BaffledHedgehog") && GameController.operator(p))
                .findFirst().orElse(null);
        require(other != null, "requires a second actual operator to test exact-name denial");
        require(!game.maps().busy() && !game.developerTools().authoring().busy() && game.inLobby(), "idle lobby fixture");
        require(!MapEditing.allowedDirectory(game.maps().localMapRoot()), "non-allowlisted nested server directory is denied");
        String originalSelection = game.config().mapId;
        var spawns = game.maps().spawns();
        var forced = new it.unimi.dsi.fastutil.longs.LongOpenHashSet(game.maps().level().getForceLoadedChunks());
        String prefix = "_check_delete_" + UUID.randomUUID();
        List<String> owned = new ArrayList<>();
        try {
            for (boolean bedwars : new boolean[]{false, true}) {
                String id = game.maps().nextLocalId(bedwars);
                owned.add(id);
                JsonObject manifest = createFixture(game, id, prefix, bedwars);
                Path directory = game.maps().localMapRoot().resolve(id);
                require(game.maps().localIds().contains(id), "fixture entered local catalog");
                require(!MapEditing.allowedDeletion(other, game.server()), "other actual operator is denied");
                require(MapDeletion.delete(game, other, id) == 0 && Files.isDirectory(directory), "unauthorized delete leaves files intact");
                require(MapDeletion.delete(game, owner, "../" + id) == 0, "path-like input is denied");
                require(MapDeletion.delete(game, owner, "1") == 0 && game.maps().info("1") != null, "bundled map stays read-only");
                game.config().mapId = id;
                require(MapDeletion.delete(game, owner, id) == 0 && Files.isDirectory(directory), "selected map cannot be deleted");
                game.config().mapId = originalSelection;
                JsonObject tampered = manifest.deepCopy(); tampered.addProperty("id", "999999");
                Files.writeString(directory.resolve("manifest.json"), tampered.toString(), StandardCharsets.UTF_8);
                require(MapDeletion.delete(game, owner, id) == 0 && game.maps().info(id) != null, "manifest mismatch leaves catalog and files intact");
                Files.writeString(directory.resolve("manifest.json"), manifest.toString(), StandardCharsets.UTF_8);
                require(MapDeletion.delete(game, owner, id) == 1, "authorized atomic deletion succeeds");
                require(!Files.exists(directory) && game.maps().info(id) == null && !game.maps().localIds().contains(id), "deleted map disappears immediately");
                require(game.maps().nextLocalId(bedwars).equals(id), "next save reuses the vacant slot");
                Path trash = game.maps().localMapRoot().resolve(".deleted");
                try (var entries = Files.list(trash)) {
                    require(entries.anyMatch(p -> p.getFileName().toString().startsWith(id + "_")
                            && Files.isRegularFile(p.resolve("map.nbt")) && Files.isRegularFile(p.resolve("manifest.json"))), "complete recovery copy retained");
                }
                var restarted = new MapManager(game.server());
                try { require(restarted.info(id) == null && restarted.nextLocalId(bedwars).equals(id), "restart preserves deletion and free slot"); }
                finally { restarted.shutdown(); }
                createFixture(game, id, prefix + " reuse", bedwars);
                require(MapDeletion.delete(game, owner, id) == 1, "reused slot can be independently deleted without replacing old backup");
            }
            require(game.maps().spawns().equals(spawns) && forced.equals(game.maps().level().getForceLoadedChunks()), "deletion preserves live spawn state and forced chunks");
            SkyWars2.LOGGER.info("[check] PASS map-deletion prefix={} ids={} root={}", prefix, owned, game.maps().localMapRoot());
        } catch (java.io.IOException error) { throw new IllegalStateException(error); }
        finally {
            game.config().mapId = originalSelection;
            for (String id : owned) {
                var info = game.maps().info(id);
                if (info != null && info.name().startsWith(prefix)) MapDeletion.delete(game, owner, id);
            }
        }
    }
    private static JsonObject createFixture(GameController game, String id, String name, boolean bedwars) throws java.io.IOException {
        Path directory = game.maps().localMapRoot().resolve(id);
        Files.createDirectories(game.maps().localMapRoot());
        Files.createDirectory(directory);
        CompoundTag nbt = new CompoundTag();
        ListTag size = new ListTag(); for (int i = 0; i < 3; i++) size.add(IntTag.valueOf(1));
        nbt.put("size", size); nbt.put("palette", new ListTag()); nbt.put("blocks", new ListTag()); nbt.put("entities", new ListTag());
        NbtIo.writeCompressed(NbtUtils.addCurrentDataVersion(nbt), directory.resolve("map.nbt"));
        JsonObject manifest = new JsonObject(); manifest.addProperty("formatVersion", 1); manifest.addProperty("id", id);
        manifest.addProperty("name", name); manifest.addProperty("mode", bedwars ? "bedwars" : "skywars");
        manifest.addProperty("playerCapacity", 1); manifest.addProperty("available", true); manifest.addProperty("reason", "");
        JsonObject placement = new JsonObject(); placement.addProperty("template", "luckywars:local/" + id);
        JsonArray origin = new JsonArray(); origin.add(0); origin.add(128); origin.add(0); placement.add("origin", origin);
        placement.addProperty("integrity", 1); placement.addProperty("seed", 0);
        JsonArray placements = new JsonArray(); placements.add(placement); manifest.add("placements", placements);
        Files.writeString(directory.resolve("manifest.json"), manifest.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        game.maps().registerLocalMap(manifest, directory);
        return manifest;
    }
}
