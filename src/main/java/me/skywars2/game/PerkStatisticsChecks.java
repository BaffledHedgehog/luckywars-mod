package me.skywars2.game;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.skywars2.SkyWars2;
import net.minecraft.nbt.CompoundTag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Isolated real-file qualification. Never opens the live store or world command storage. */
public final class PerkStatisticsChecks {
    private PerkStatisticsChecks() {}

    public static void run(GameController ignored) { run(); }

    public static void run() {
        Path fixture = null;
        try {
            fixture = Files.createTempDirectory("lbc-perk-statistics-check-").toAbsolutePath().normalize();
            counters(fixture.resolve("counters.json"));
            migration(fixture.resolve("migration.json"));
            rejectedImport(fixture.resolve("blocked.json"));
            malformedStore(fixture.resolve("malformed.json"));
            SkyWars2.LOGGER.info("[check] PASS perk-statistics (isolated durable files, zero/overflow-safe rates, native IDs, global/name counters, source retention, checked import failure/retry, reload and receipt idempotence)");
        } catch (IOException error) {
            throw new IllegalStateException("Perk statistics fixture I/O", error);
        } finally {
            if (fixture != null) cleanup(fixture);
        }
    }

    private static void counters(Path file) throws IOException {
        var store = new LbcPerkStatistics(file);
        var cleanup = new AtomicInteger();
        store.migrateLegacy(new CompoundTag(), cleanup::incrementAndGet);
        require(cleanup.get() == 0, "empty source needs no world cleanup");
        require(store.value("Offline_A", "pick_rate_kit[0]").equals("0"), "empty pick percentage");
        require(store.value("Offline_A", "win_rate_kit[0]").equals("0"), "empty win percentage");
        require(store.value("Offline_A", "win_count_kit[0]").equals("0"), "empty personal count");
        Map<String, List<Integer>> choices = Map.of(
                "kit", List.of(1), "skill", List.of(2), "trinket", List.of(2, 2),
                "levelup", List.of(14, 0), "x_kit", List.of(1), "swrg_kit", List.of(24),
                "swrg_skill", List.of(13), "challenge", List.of(1));
        store.recordPicks("match-a", "Offline_A", choices);
        require(store.value("Offline_A", "pick_count_kit[0]").equals("1"), "native ID1 maps to source index0");
        require(store.value("Offline_A", "pick_count_kit[1]").equals("0"), "native ID1 never increments index1");
        require(store.value("Offline_A", "pick_count_skill[1]").equals("1"), "native skill ID2 maps to index1");
        require(store.value("Offline_A", "pick_count_trinket[1]").equals("2"), "two granted accessory slots count twice");
        require(store.value("Offline_A", "pick_count_levelup[13]").equals("1"), "random-upgrade choice retains its own identity");
        require(store.value("Offline_A", "pick_count_swrg_kit[23]").equals("1"), "base kit category");
        require(store.value("Offline_A", "pick_count_swrg_skill[12]").equals("1"), "base skill category");
        require(store.value("Offline_A", "pick_count_challenge[0]").equals("1"), "active challenge category");
        require(store.value("Offline_A", "pick_count_x_kit[0]").equals("1"), "challenge reward category");
        store.recordPicks("match-b", "Offline_B", Map.of("kit", List.of(2)));
        require(store.value("Offline_A", "pick_rate_kit[0]").equals("50"), "percentage uses global category total");
        require(store.value("Offline_B", "pick_count_kit[0]").equals("0"), "personal counts never cross player names");
        store.recordWins("match-a", "Offline_A", choices);
        require(store.value("Offline_A", "win_count_kit[0]").equals("1"), "offline winner has a personal win without a player entity");
        require(store.value("Offline_B", "win_rate_kit[0]").equals("100"), "win percentage is global wins per same-perk picks");
        var before = Files.readAllBytes(file);
        var reloaded = new LbcPerkStatistics(file);
        reloaded.recordPicks("match-a", "Offline_A", choices);
        reloaded.recordWins("match-a", "Offline_A", choices);
        require(Arrays.equals(before, Files.readAllBytes(file)), "reloaded receipts reject duplicate picks and wins without rewriting");
        require(reloaded.value("Offline_A", "win_count_trinket[1]").equals("2"), "reloaded duplicate-slot counts remain exact");
        var raw = read(file).getAsJsonObject("counters");
        require(!raw.has("-1_levelup_pick"), "unset zero ID produces no counter");
        require(read(file).getAsJsonArray("receipts").size() == 3, "receipts grow with accepted match events only");
    }

    private static void migration(Path file) throws IOException {
        var legacy = new CompoundTag();
        legacy.putInt("0_kit_pick", 3);
        legacy.putInt("1_kit_pick", 1);
        legacy.putInt("kit_picks", 4);
        legacy.putInt("0_kit_win", 2);
        legacy.putInt("Offline_Name_0_kit_pick", 2);
        legacy.putInt("Offline_Name_0_kit_win", 1);
        legacy.putInt("1_skill_win", 7);
        legacy.putLong("0_levelup_pick", Long.MAX_VALUE);
        legacy.putLong("levelup_picks", Long.MAX_VALUE);
        legacy.putString("unknown_author_field", "retained before cleanup");
        legacy.putString("pick_rate_kit", "stale derived cache");
        var storage = new AtomicReference<>(legacy.copy());
        String sourceImage = storage.get().toString();
        var store = new LbcPerkStatistics(file);
        store.migrateLegacy(storage.get(), () -> {
            var durable = read(file);
            require(durable.get("legacyImported").getAsBoolean(), "import marker is durable before source removal");
            require(durable.get("legacySnapshot").getAsString().equals(sourceImage), "entire legacy source retained before cleanup");
            require(durable.getAsJsonObject("counters").get("Offline_Name_0_kit_pick").getAsLong() == 2,
                    "offline personal counters durable before source removal");
            storage.set(new CompoundTag());
        });
        require(storage.get().isEmpty(), "successful import clears synthetic storage");
        require(store.value("Offline_Name", "pick_count_kit[0]").equals("2"), "name with underscore survives migration");
        require(store.value("Offline_Name", "pick_rate_kit[0]").equals("75"), "legacy global picks retained");
        require(store.value("Offline_Name", "win_rate_kit[0]").equals("66.66"), "percentage floors to source precision");
        require(store.value("Offline_Name", "win_rate_skill[1]").equals("0"), "missing denominator overrides nonzero numerator");
        require(store.value("Offline_Name", "pick_rate_levelup[0]").equals("100"), "long counters do not overflow percentage arithmetic");
        var before = Files.readAllBytes(file);
        var again = new LbcPerkStatistics(file);
        var retriedCleanup = new AtomicInteger();
        again.migrateLegacy(legacy, retriedCleanup::incrementAndGet);
        require(retriedCleanup.get() == 1, "restart retries interrupted source cleanup");
        require(Arrays.equals(before, Files.readAllBytes(file)), "durable import marker prevents source reimport on restart");
        require(again.value("Offline_Name", "pick_count_kit[0]").equals("2"), "repeated cleanup never doubles offline counters");
    }

    private static void rejectedImport(Path blocked) throws IOException {
        Files.createDirectory(blocked);
        Path sentinel = blocked.resolve("existing-data.txt");
        Files.writeString(sentinel, "keep", StandardCharsets.UTF_8);
        var legacy = new CompoundTag();
        legacy.putInt("Offline_Name_0_kit_pick", 9);
        var storage = new AtomicReference<>(legacy.copy());
        var store = new LbcPerkStatistics(blocked);
        boolean rejected = false;
        try {
            store.migrateLegacy(storage.get(), () -> storage.set(new CompoundTag()));
        } catch (IOException expected) { rejected = true; }
        require(rejected, "atomic replacement of a nonempty directory fails explicitly");
        require(storage.get().equals(legacy), "failed durable import leaves source storage intact");
        require(Files.readString(sentinel).equals("keep"), "failed replacement preserves existing target contents");
        require(!Files.exists(blocked.resolveSibling(blocked.getFileName() + ".tmp")), "failed import removes only its temporary file");
        require(store.value("Offline_Name", "pick_count_kit[0]").equals("0"), "failed import rolls back in-memory acceptance");
        Files.delete(sentinel);
        Files.delete(blocked);
        store.migrateLegacy(storage.get(), () -> storage.set(new CompoundTag()));
        require(storage.get().isEmpty(), "same instance can retry after the atomic-write obstruction is removed");
        require(store.value("Offline_Name", "pick_count_kit[0]").equals("9"), "retry imports the source exactly once");
    }

    private static void malformedStore(Path file) throws IOException {
        Files.writeString(file, "{broken", StandardCharsets.UTF_8);
        boolean rejected = false;
        try { new LbcPerkStatistics(file); }
        catch (RuntimeException expected) { rejected = true; }
        require(rejected && Files.readString(file).equals("{broken"), "corrupt durable store is never silently replaced with empty data");
    }

    private static JsonObject read(Path file) {
        try { return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject(); }
        catch (IOException error) { throw new IllegalStateException("Cannot read isolated statistics fixture", error); }
    }

    private static void cleanup(Path fixture) {
        require(fixture.isAbsolute() && fixture.getFileName().toString().startsWith("lbc-perk-statistics-check-"), "cleanup stays in its generated fixture directory");
        try (var paths = Files.walk(fixture)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                require(path.toAbsolutePath().normalize().startsWith(fixture), "cleanup path remains inside fixture");
                Files.deleteIfExists(path);
            }
        } catch (IOException error) { throw new IllegalStateException("Cannot clean isolated statistics fixture", error); }
    }

    private static void require(boolean condition, String why) {
        if (!condition) throw new IllegalStateException("Perk statistics: " + why);
    }
}
