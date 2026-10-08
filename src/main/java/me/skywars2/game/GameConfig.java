package me.skywars2.game;

import net.minecraft.network.chat.Component;
import static me.skywars2.SkyWars2.text;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;

/** Persistent, server-wide settings for the next SkyWars match. */
public final class GameConfig {
    private static final Path FILE = Path.of("config", "luckywars", "game.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public int lives = 1;
    public int health = 20;
    public int countdownSeconds = 5;
    public int stageOneSeconds = 120;
    public int stageTwoSeconds = 360;
    public int endingSeconds = 10;
    public int stageOneEvent = 1;
    public int stageTwoEvent = 1;
    public int pvpMode = 2;
    public int day = 1;
    public boolean vanillaKits = true;
    public boolean vanillaSkills = true;
    public boolean noBorders;
    public String mapId = "1";
    public boolean broken;
    public int gamesPlayed;

    public static GameConfig load() {
        GameConfig config = new GameConfig();
        if (!Files.isRegularFile(FILE)) {
            config.save();
            return config;
        }

        try (var reader = Files.newBufferedReader(FILE)) {
            var json = JsonParser.parseReader(reader);
            if (!json.isJsonObject()) throw new JsonParseException("Expected a JSON object");
            GameConfig loaded = GSON.fromJson(json, GameConfig.class);
            if (loaded == null) loaded = new GameConfig();
            loaded.clampTimings();
            if (!validMapId(loaded.mapId)) loaded.mapId = "1";
            return loaded;
        } catch (IOException e) {
            System.err.println("Could not read " + FILE + ": " + e.getMessage());
            return config;
        } catch (RuntimeException e) {
            backupCorruptFile();
            System.err.println("Invalid JSON in " + FILE + "; restored defaults: " + e.getMessage());
            config.save();
            return config;
        }
    }

    public void save() {
        clampTimings();
        Path temporary = FILE.resolveSibling(FILE.getFileName() + ".tmp");
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(temporary, GSON.toJson(this));
            try {
                Files.move(temporary, FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("Could not write " + FILE + ": " + e.getMessage());
        } finally {
            try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
        }
    }

    /** Fresh defaults; the caller copies settings into the existing config and saves once. */
    public static GameConfig resetDefaults() {
        return new GameConfig();
    }

    private void clampTimings() {
        lives = clamp(lives, 1, 5);
        health = clamp(health, 1, 60);
        countdownSeconds = clamp(countdownSeconds, 0, 60);
        stageOneSeconds = clamp(stageOneSeconds, 0, 3600);
        stageTwoSeconds = clamp(stageTwoSeconds, 0, 7200);
        endingSeconds = clamp(endingSeconds, 0, 120);
        stageOneEvent = clamp(stageOneEvent, 0, 9);
        stageTwoEvent = clamp(stageTwoEvent, 0, 8);
        pvpMode = clamp(pvpMode, 1, 2);
        day = clamp(day, 1, 3);
        gamesPlayed = Math.max(0, gamesPlayed);
    }

    /** Keeps translated validation errors intact until they reach the client. */
    public static final class SettingError extends IllegalArgumentException {
        private final Component component;
        public SettingError(Component component) { super(component.getString()); this.component = component; }
        public Component component() { return component; }
    }

    public Component setSetting(String parameter, String rawValue) {
        if (parameter == null || rawValue == null) throw new SettingError(text("settings.arguments"));
        String key = parameter.trim().toLowerCase(java.util.Locale.ROOT);
        String value = rawValue.trim();
        String canonical;
        Object configured;
        switch (key) {
            case "lives" -> { configured = lives = integer(key, value, 1, 5); canonical = "lives"; }
            case "health" -> { configured = health = integer(key, value, 1, 60); canonical = "health"; }
            case "countdownseconds", "countdown" -> { configured = countdownSeconds = integer(key, value, 0, 60); canonical = "countdownSeconds"; }
            case "stageoneseconds", "stage1seconds" -> { configured = stageOneSeconds = integer(key, value, 0, 3600); canonical = "stageOneSeconds"; }
            case "stagetwoseconds", "stage2seconds" -> { configured = stageTwoSeconds = integer(key, value, 0, 7200); canonical = "stageTwoSeconds"; }
            case "endingseconds", "ending" -> { configured = endingSeconds = integer(key, value, 0, 120); canonical = "endingSeconds"; }
            case "stageoneevent", "stage1event" -> { configured = stageOneEvent = integer(key, value, 0, 9); canonical = "stageOneEvent"; }
            case "stagetwoevent", "stage2event" -> { configured = stageTwoEvent = integer(key, value, 0, 8); canonical = "stageTwoEvent"; }
            case "pvpmode", "pvp" -> { configured = pvpMode = integer(key, value, 1, 2); canonical = "pvpMode"; }
            case "day" -> { configured = day = integer(key, value, 1, 3); canonical = "day"; }
            case "vanillakits" -> { configured = vanillaKits = bool(key, value); canonical = "vanillaKits"; }
            case "vanillaskills" -> { configured = vanillaSkills = bool(key, value); canonical = "vanillaSkills"; }
            case "noborders" -> { configured = noBorders = bool(key, value); canonical = "noBorders"; }
            default -> throw new SettingError(text("settings.unknown", parameter));
        }
        save();
        return text("settings.saved", canonical, configured instanceof Boolean enabled
                ? text(enabled ? "common.enabled" : "common.disabled") : configured);
    }

    private static int integer(String key, String value, int min, int max) {
        final int parsed;
        try { parsed = Integer.parseInt(value); }
        catch (NumberFormatException e) { throw new SettingError(text("settings.integer", key, min, max)); }
        if (parsed < min || parsed > max) throw new SettingError(text("settings.range", key, min, max));
        return parsed;
    }

    private static boolean bool(String key, String value) {
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        throw new SettingError(text("settings.boolean", key));
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    private static boolean validMapId(String id) {
        if (id == null || !id.matches("[0-9]{1,2}b?")) return false;
        boolean brokenVariant = id.endsWith("b");
        int number = Integer.parseInt(brokenVariant ? id.substring(0, id.length() - 1) : id);
        return number >= 1 && number <= (brokenVariant ? 25 : 64);
    }

    private static void backupCorruptFile() {
        try {
            if (!Files.isRegularFile(FILE)) return;
            Path backup = FILE.resolveSibling(FILE.getFileName() + ".corrupt-" + System.currentTimeMillis() + ".bak");
            Files.copy(FILE, backup, StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException e) {
            System.err.println("Could not back up malformed " + FILE + ": " + e.getMessage());
        }
    }
}
