package me.skywars2.game;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.skywars2.SkyWars2;
import net.minecraft.nbt.CompoundTag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
            weightedTrinkets(fixture.resolve("trinkets.json"));
            batchedPicks(fixture.resolve("batch.json"));
            deferredPicks(fixture.resolve("deferred.json"));
            playerAutosave(fixture.resolve("players.json"));
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

    private static void diskTask(Runnable work){
        var thread=new Thread(work,"owned-perk-disk-check");thread.setDaemon(true);thread.start();
        try{thread.join(10000);}catch(InterruptedException error){Thread.currentThread().interrupt();throw new IllegalStateException(error);}
        require(!thread.isAlive(),"owned worker finished before file assertions and cleanup");
    }
    private static void playerAutosave(Path file) throws IOException {
        var tasks = new ArrayDeque<Runnable>();
        var store = new PlayerStore(file, tasks::addLast);
        for (int i = 0; i < 1000; i++) store.get(new UUID(0, i)).wins = i;
        UUID player = new UUID(0, 999);
        var profile = store.get(player);
        require(profile.wins == 999, "profile indexed by UUID");

        store.saveDeferred();
        profile.wins++;
        profile.trinkets[0] = 4;
        String unlock = "lbc:true_advancements/storage_probe";
        profile.lbcUnlocks.add(unlock);
        store.saveDeferred();
        require(!Files.exists(file) && tasks.size() == 1, "periodic save leaves all I/O to a coalesced worker");

        profile.trinkets[0] = 5;
        profile.lbcUnlocks.clear();
        diskTask(tasks.removeFirst());
        JsonObject root = read(file);
        JsonObject saved = root.getAsJsonObject(player.toString());
        require(root.size() == 1000 && saved.get("wins").getAsInt() == 1000,
                "latest scheduled save persists all profiles");
        require(saved.getAsJsonArray("trinkets").get(0).getAsInt() == 4
                && saved.getAsJsonArray("lbcUnlocks").get(0).getAsString().equals(unlock),
                "autosave isolates mutable inventories and unlocks from later edits");

        store.saveDeferred();
        profile.wins++;
        store.saveAtomic();
        diskTask(tasks.removeFirst());
        saved = read(file).getAsJsonObject(player.toString());
        require(saved.get("wins").getAsInt() == 1001 && saved.getAsJsonArray("lbcUnlocks").isEmpty(),
                "an older autosave cannot replace a completed atomic transaction");

        profile.wins++;
        store.saveDeferred();
        diskTask(tasks.removeFirst());
        require(read(file).getAsJsonObject(player.toString()).get("wins").getAsInt() == 1002,
                "autosave worker can be scheduled again after a transaction");
    }

    private static void deferredPicks(Path file) throws IOException {
        var tasks=new java.util.ArrayList<Runnable>();var store=new LbcPerkStatistics(file,tasks::add);
        store.migrateLegacy(new CompoundTag(),()->{});var before=Files.readAllBytes(file);
        try(var batch=store.batchDeferred()){store.recordPicks("deferred/A","AsyncA",Map.of("kit",List.of(1)));}
        require(java.util.Arrays.equals(before,Files.readAllBytes(file))&&store.value("AsyncA","pick_count_kit[0]").equals("1"),
                "native counters visible immediately while deferred boundary performs no disk write");
        try(var batch=store.batchDeferred()){store.recordPicks("deferred/B","AsyncB",Map.of("kit",List.of(1)));}
        require(tasks.size()==1&&java.util.Arrays.equals(before,Files.readAllBytes(file)),"two pending full images share one queued worker");
        diskTask(tasks.removeFirst());store.flush();var loaded=new LbcPerkStatistics(file);
        require(loaded.value("AsyncA","pick_count_kit[0]").equals("1")&&loaded.value("AsyncB","pick_count_kit[0]").equals("1"),
                "latest coalesced snapshot durably retains every accepted event");
        Files.delete(file);Files.createDirectory(file);var sentinel=file.resolve("preserve.txt");Files.writeString(sentinel,"keep");
        try(var batch=store.batchDeferred()){store.recordPicks("deferred/C","AsyncC",Map.of("kit",List.of(1)));}
        require(tasks.size()==1,"one owned failed-write task");diskTask(tasks.removeFirst());store.flush();
        require(Files.readString(sentinel).equals("keep")&&store.value("AsyncC","pick_count_kit[0]").equals("1"),
                "failed async and synchronous retry preserve accepted memory counters and unrelated path contents");
        Files.delete(sentinel);Files.delete(file);store.flush();
        require(new LbcPerkStatistics(file).value("AsyncC","pick_count_kit[0]").equals("1"),"shutdown-style fence retries the failed latest snapshot");
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
        require(reloaded.value("Offline_A", "win_count_trinket[1]").equals("1"), "one win with two identical trinkets remains one personal win after reload");
        var raw = read(file).getAsJsonObject("counters");
        require(!raw.has("-1_levelup_pick"), "unset zero ID produces no counter");
        require(read(file).getAsJsonArray("receipts").size() == 3, "receipts grow with accepted match events only");
    }

    private static void weightedTrinkets(Path file)throws IOException{
        var store=new LbcPerkStatistics(file);store.migrateLegacy(new CompoundTag(),()->{});
        var choices=Map.of("trinket",List.of(1,1,1,1,2,2,2,0,0,0,0,0));
        store.recordPicks("fraction-0","Weighted",choices);store.recordWins("fraction-0","Weighted",choices);
        require(store.value("Weighted","win_count_trinket[0]").equals("0.57142857"),"four of seven shares, ignoring empty slots");
        require(store.value("Weighted","win_count_trinket[1]").equals("0.42857143"),"three of seven shares");
        require(store.value("Weighted","pick_count_trinket[0]").equals("4")&&store.value("Weighted","win_rate_trinket[0]").equals("100"),"picks and global percentages keep source counting");
        for(int i=1;i<7;i++)store.recordWins("fraction-"+i,"Weighted",choices);
        store=new LbcPerkStatistics(file);
        require(store.value("Weighted","win_count_trinket[0]").equals("4")&&store.value("Weighted","win_count_trinket[1]").equals("3"),"seven fractional wins add to exact integers across reload");
        store.recordWins("single","Solo",Map.of("trinket",List.of(0,0,3,0,0,0,0,0,0,0,0,0)));
        require(store.value("Solo","win_count_trinket[2]").equals("1"),"one actual trinket among empty slots earns a whole win");
        store.recordWins("nine","Nine",Map.of("trinket",java.util.Collections.nCopies(9,4)));
        require(store.value("Nine","win_count_trinket[3]").equals("1"),"nine identical selections earn one personal win");
        var all=Map.of("trinket",java.util.stream.IntStream.rangeClosed(1,12).boxed().toList());
        for(int i=0;i<12;i++)store.recordWins("twelfths-"+i,"Twelve",all);
        for(int i=0;i<12;i++)require(store.value("Twelve","win_count_trinket["+i+"]").equals("1"),"all twelve denominators remain exact");
        var before=Files.readAllBytes(file);boolean rejected=false;
        try{store.recordWins("invalid","Weighted",Map.of("trinket",java.util.Collections.nCopies(13,1)));}catch(IllegalStateException expected){rejected=true;}
        require(rejected&&Arrays.equals(before,Files.readAllBytes(file)),"invalid selection cannot partially alter counters or receipts");
        var legacy=new CompoundTag();legacy.putInt("Imported_0_trinket_win",7);
        var imported=new LbcPerkStatistics(file.resolveSibling("old-trinkets.json"));imported.migrateLegacy(legacy,()->{});
        imported.recordWins("new-win","Imported",Map.of("trinket",List.of(1,2)));
        require(new LbcPerkStatistics(file.resolveSibling("old-trinkets.json")).value("Imported","win_count_trinket[0]").equals("7.5"),"legacy personal totals are preserved with new exact shares");
    }

    private static void batchedPicks(Path file)throws IOException{
        var store=new LbcPerkStatistics(file);store.migrateLegacy(new CompoundTag(),()->{});var before=Files.readAllBytes(file);
        try(var batch=store.batch()){
            for(int i=0;i<24;i++)store.recordPicks("round/"+i,"Player"+i,Map.of("kit",List.of(1),"trinket",List.of(1,1)));
            try(var nested=store.batch()){store.recordWins("round/0","Player0",Map.of("kit",List.of(1)));}
            require(Arrays.equals(before,Files.readAllBytes(file)),"24 start receipts and nested work defer synchronous disk replacement until the outer boundary");
            require(store.value("Player23","pick_count_trinket[0]").equals("2"),"accepted in-memory state is immediately visible within the batch");
        }
        var restored=new LbcPerkStatistics(file);require(read(file).getAsJsonArray("receipts").size()==25,"all batched events durable together");
        require(restored.value("Player23","pick_count_trinket[0]").equals("2")&&restored.value("Player0","win_count_kit[0]").equals("1"),"the single durable write retains every actual event");
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
