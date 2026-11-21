package me.skywars2.game.players;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Load the same UUID file as the native player list, with the old world path as a fallback. */
public record SavedAdvancements(Path current, Path legacy) {
    public static SavedAdvancements of(MinecraftServer server) {
        return new SavedAdvancements(server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR),
                server.getWorldPath(LevelResource.ROOT).resolve("advancements"));
    }

    public Entry read(UUID uuid) throws IOException {
        String name = uuid + ".json";
        var file = current.resolve(name);
        var original = readBytes(file);
        byte[] source = original != null || current.equals(legacy) ? original : readBytes(legacy.resolve(name));
        if (source == null) {
            var data = new JsonObject();
            data.addProperty("DataVersion", SharedConstants.getCurrentVersion().dataVersion().version());
            return new Entry(file, original, data);
        }
        try {
            var data = JsonParser.parseString(new String(source, StandardCharsets.UTF_8));
            if (data.isJsonObject()) return new Entry(file, original, data.getAsJsonObject());
            throw new JsonParseException("Expected an advancement progress object");
        } catch (JsonParseException invalid) {
            throw new IOException("Invalid advancement data for " + uuid, invalid);
        }
    }

    private static byte[] readBytes(Path file) throws IOException {
        try {
            return Files.readAllBytes(file);
        } catch (NoSuchFileException missing) {
            return null;
        }
    }

    public record Entry(Path file, byte[] original, JsonObject data) {
        private static final com.google.gson.Gson GSON = new GsonBuilder().setPrettyPrinting().create();

        public AdvancementProgress progress(String id, AdvancementRequirements requirements) throws IOException {
            var raw = data.get(id);
            var progress = raw == null ? new AdvancementProgress() : AdvancementProgress.CODEC.parse(JsonOps.INSTANCE, raw)
                    .result().orElseThrow(() -> new IOException("Invalid saved advancement " + id));
            progress.update(requirements);
            return progress;
        }

        public void put(String id, AdvancementProgress progress) {
            if (progress.hasProgress()) data.add(id, AdvancementProgress.CODEC.encodeStart(JsonOps.INSTANCE, progress).getOrThrow());
            else data.remove(id);
        }

        public void save(PlayerStore profiles, boolean profileChanged) throws IOException {
            write((GSON.toJson(data) + "\n").getBytes(StandardCharsets.UTF_8));
            if (!profileChanged) return;
            try {
                profiles.saveAtomic();
            } catch (IOException | RuntimeException failure) {
                try { restore(); } catch (IOException rollback) { failure.addSuppressed(rollback); }
                throw failure;
            }
        }

        private void restore() throws IOException {
            if (original == null) Files.deleteIfExists(file);
            else write(original);
        }

        private void write(byte[] contents) throws IOException {
            Files.createDirectories(file.getParent());
            var temporary = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
            try {
                Files.write(temporary, contents);
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
    }
}
