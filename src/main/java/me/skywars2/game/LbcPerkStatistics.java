package me.skywars2.game;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

/** Native, durable counters; legacy name keys remain readable for offline players. */
final class LbcPerkStatistics {
    private static final Path FILE = Path.of("config", "luckywars", "perk-statistics.json");
    private static final Identifier LEGACY = Identifier.parse("stats:stats");
    private static final Map<MinecraftServer, LbcPerkStatistics> STORES = new WeakHashMap<>();
    private static final Pattern PATH = Pattern.compile("(pick|win)_(count|rate)_([a-z][a-z0-9_]*)\\[(\\d+)]");
    private static final Pattern RAW = Pattern.compile("(?:.*_)?\\d+_(?:kit|x_kit|skill|trinket|levelup)_(?:pick|win)|(?:kit|x_kit|skill|trinket|levelup)_(?:picks|wins)");
    private final Map<String, Long> counters = new LinkedHashMap<>();
    private final Set<String> receipts = new LinkedHashSet<>();
    private final Path file;
    private boolean imported;
    private boolean dirty;
    // Retain the entire source, including unfamiliar keys, before removing command storage.
    private String legacySnapshot = "";

    /** Isolated fixture stores use the same persistence and migration code as production. */
    LbcPerkStatistics(Path file) throws IOException {
        this.file = file.toAbsolutePath().normalize();
        load();
    }

    /** Call during server startup. Any failed read/write leaves command storage intact. */
    static void initialize(MinecraftServer server) {
        get(server);
    }

    static LbcPerkStatistics get(MinecraftServer server) {
        var existing = STORES.get(server);
        if (existing != null) return existing;
        try {
            var loaded = new LbcPerkStatistics(FILE);
            var legacy = server.getCommandStorage().get(LEGACY);
            loaded.migrateLegacy(legacy, () -> server.getCommandStorage().set(LEGACY, new CompoundTag()));
            STORES.put(server, loaded);
            return loaded;
        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("Could not initialize durable perk statistics; legacy storage retained", error);
        }
    }

    /** Cleanup is invoked only after the import marker and full source image are durable. */
    void migrateLegacy(CompoundTag legacy, Runnable cleanup) throws IOException {
        if (!imported) {
            var before = new LinkedHashMap<>(counters);
            String previousSnapshot = legacySnapshot;
            try {
                for (String key : legacy.keySet()) {
                    if (RAW.matcher(key).matches() && legacy.get(key) instanceof NumericTag number)
                        counters.merge(key, Math.max(0L, number.longValue()), Math::max);
                }
                legacySnapshot = legacy.toString();
                imported = true;
                save();
            } catch (IOException | RuntimeException error) {
                counters.clear();
                counters.putAll(before);
                legacySnapshot = previousSnapshot;
                imported = false;
                throw error;
            }
        }
        // A crash before the world saves this removal retries cleanup, never the import.
        if (!legacy.isEmpty()) cleanup.run();
    }

    /** IDs are native one-based selections. Pass accepted choices once per match start. */
    void recordPicks(String receipt, String playerName, Map<String, List<Integer>> choices) {
        record(receipt, playerName, choices, "pick");
    }

    /** Use the same match-start choices, including for offline winners. */
    void recordWins(String receipt, String playerName, Map<String, List<Integer>> choices) {
        record(receipt, playerName, choices, "win");
    }

    private void record(String receipt, String playerName, Map<String, List<Integer>> choices, String kind) {
        if (receipt == null || receipt.isBlank()) throw new IllegalArgumentException("Missing perk-statistics event receipt");
        String event = receipt + "/" + kind;
        if (receipts.contains(event)) { flush(); return; }
        if (playerName == null || playerName.isBlank()) throw new IllegalArgumentException("Missing perk-statistics player name");
        var before = new LinkedHashMap<>(counters);
        try {
            for (var entry : choices.entrySet()) {
                String category = entry.getKey();
                if (!category.matches("[a-z][a-z0-9_]*")) throw new IllegalArgumentException("Invalid perk-statistics category " + category);
                for (int nativeId : entry.getValue()) {
                    if (nativeId <= 0) continue;
                    String suffix = (nativeId - 1) + "_" + category + "_" + kind;
                    increment(suffix);
                    increment(playerName + "_" + suffix);
                    increment(category + "_" + kind + "s");
                }
            }
            receipts.add(event);
        } catch (RuntimeException error) {
            counters.clear();
            counters.putAll(before);
            receipts.remove(event);
            throw new IllegalStateException("Could not save perk statistics", error);
        }
        dirty = true;
        flush();
    }

    /** Runtime disk failures retain pending counters for the next accepted event or shutdown. */
    void flush() {
        if (!dirty) return;
        try {
            save();
            dirty = false;
        } catch (IOException error) {
            me.skywars2.SkyWars2.LOGGER.error("Could not save perk statistics; pending counters retained in memory", error);
        }
    }

    private void increment(String key) {
        counters.compute(key, (ignored, value) -> value == null ? 1L : value == Long.MAX_VALUE ? value : value + 1);
    }

    static String value(MinecraftServer server, String playerName, String path) {
        return get(server).value(playerName, path);
    }

    String value(String playerName, String path) {
        var match = PATH.matcher(path);
        if (!match.matches()) throw new IllegalArgumentException("Unknown perk-statistics path " + path);
        String kind = match.group(1), category = match.group(3), id = match.group(4);
        String suffix = id + "_" + category + "_" + kind;
        if (match.group(2).equals("count")) return Long.toString(count(playerName + "_" + suffix));
        long numerator = count(suffix);
        long denominator = count(kind.equals("pick") ? category + "_picks" : id + "_" + category + "_pick");
        if (denominator == 0 || numerator == 0) return "0";
        return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.DOWN).stripTrailingZeros().toPlainString();
    }

    private long count(String key) { return counters.getOrDefault(key, 0L); }

    private void load() throws IOException {
        if (!Files.isRegularFile(file)) return;
        try (var reader = Files.newBufferedReader(file)) {
            var root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.get("version").getAsInt() != 1) throw new IOException("Unsupported perk-statistics version");
            imported = root.get("legacyImported").getAsBoolean();
            legacySnapshot = root.get("legacySnapshot").getAsString();
            if (root.has("receipts")) for (var receipt : root.getAsJsonArray("receipts")) receipts.add(receipt.getAsString());
            for (var entry : root.getAsJsonObject("counters").entrySet()) {
                long value = entry.getValue().getAsBigDecimal().longValueExact();
                if (value < 0) throw new IOException("Negative perk-statistics counter " + entry.getKey());
                counters.put(entry.getKey(), value);
            }
        }
    }

    private void save() throws IOException {
        var root = new JsonObject();
        root.addProperty("version", 1);
        root.addProperty("legacyImported", imported);
        root.addProperty("legacySnapshot", legacySnapshot);
        var accepted = new com.google.gson.JsonArray();
        receipts.forEach(accepted::add);
        root.add("receipts", accepted);
        var values = new JsonObject();
        counters.forEach(values::addProperty);
        root.add("counters", values);
        byte[] payload = new GsonBuilder().setPrettyPrinting().create().toJson(root).getBytes(StandardCharsets.UTF_8);
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            try (var channel = FileChannel.open(temporary, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                var bytes = ByteBuffer.wrap(payload);
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
