package me.skywars2.game;

import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;

/** Test-jar-only map record deletion. It never removes terrain or edits bundled resources. */
public final class MapDeletion {
    private MapDeletion() { }

    public static int delete(GameController game, ServerPlayer player, String id) {
        if (!MapEditing.allowedDeletion(player, game.server())) {
            message(player, "Map deletion is restricted to the BaffledHedgehog operator on this test server.");
            return 0;
        }
        if (id == null || !id.matches("[1-9][0-9]*b?")) {
            message(player, "Use the map ID shown in the map menu, for example 65 or 26b."); return 0;
        }
        MapManager maps = game.maps();
        if (!maps.isLocal(id)) {
            message(player, maps.info(id) == null ? "Unknown local map: " + id
                    : "Bundled maps are read-only. Only locally saved maps can be deleted."); return 0;
        }
        if (!game.inLobby() || game.playing() || maps.busy() || maps.rotationBusy()
                || game.developerTools().authoring().busy()) {
            message(player, "Wait for the match, save or map operation to finish before deleting a map."); return 0;
        }
        if (id.equals(game.config().mapId) || maps.mapResident(id)) {
            message(player, "This map is selected or still loaded. Select and fully load another map before deleting it."); return 0;
        }
        Path source = maps.localMapDirectory(id);
        Path tombstone = null;
        try {
            Path root = checkedRoot(game);
            Path expected = root.resolve(id);
            if (source == null || !source.toAbsolutePath().normalize().equals(expected))
                throw new IOException("Catalog path does not match the local map ID");
            checkedDirectory(expected, root);
            Path manifestPath = expected.resolve("manifest.json");
            checkedFile(manifestPath, root);
            Path template = expected.resolve("map.nbt");
            if (Files.exists(template, LinkOption.NOFOLLOW_LINKS)) checkedFile(template, root);
            try (var entries = Files.list(expected)) {
                for (Path entry : entries.toList()) {
                    String name = entry.getFileName().toString();
                    if (!name.equals("map.nbt") && !name.equals("manifest.json"))
                        throw new IOException("Map directory contains unexpected files");
                }
            }
            try (var reader = Files.newBufferedReader(manifestPath, StandardCharsets.UTF_8)) {
                var manifest = JsonParser.parseReader(reader).getAsJsonObject();
                if (!id.equals(manifest.get("id").getAsString()))
                    throw new IOException("Manifest ID does not match the catalog");
            }
            Path trash = root.resolve(".deleted");
            if (!Files.exists(trash, LinkOption.NOFOLLOW_LINKS)) Files.createDirectory(trash);
            checkedDirectory(trash, root);
            tombstone = trash.resolve(id + "_" + UUID.randomUUID());
            Files.move(expected, tombstone, StandardCopyOption.ATOMIC_MOVE);
            if (!maps.unregisterMap(id)) {
                // Main-thread execution makes this unreachable unless a caller changed invariants.
                Files.move(tombstone, expected, StandardCopyOption.ATOMIC_MOVE);
                throw new IOException("Map became busy; deletion was rolled back");
            }
            message(player, "Deleted local map " + id + ". Its menu slot is free for the next save; a recovery copy was retained.");
            return 1;
        } catch (IOException | RuntimeException error) {
            message(player, "Could not delete local map " + id + ": " + error.getMessage());
            return 0;
        }
    }

    /** Resolve each existing ancestor independently; neither symlinks nor junction escapes are accepted. */
    private static Path checkedRoot(GameController game) throws IOException {
        Path world = game.server().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().toRealPath();
        Path current = world;
        for (String part : new String[]{"data", "luckywars", "maps"}) {
            current = current.resolve(part);
            checkedDirectory(current, world);
        }
        return current;
    }
    private static void checkedDirectory(Path path, Path boundary) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attrs.isDirectory() || attrs.isSymbolicLink() || attrs.isOther()
                || !path.toRealPath().equals(path.toAbsolutePath().normalize())
                || !path.toRealPath().startsWith(boundary)) throw new IOException("Unsafe map directory");
    }
    private static void checkedFile(Path path, Path boundary) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attrs.isRegularFile() || attrs.isSymbolicLink() || attrs.isOther()
                || !path.toRealPath().equals(path.toAbsolutePath().normalize())
                || !path.toRealPath().startsWith(boundary)) throw new IOException("Unsafe map file");
    }
    private static void message(ServerPlayer player, String text) {
        if (player != null) player.sendSystemMessage(Component.literal(text));
    }
}
