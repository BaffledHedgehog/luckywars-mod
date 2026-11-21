package me.skywars2.game.gui.loadouts;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
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
public final class LbcPerkStatistics {
    private static final Path FILE = Path.of("config", "luckywars", "perk-statistics.json");
    private static final Identifier LEGACY = Identifier.parse("stats:stats");
    private static final Map<MinecraftServer, LbcPerkStatistics> STORES = new WeakHashMap<>();
    private static final Pattern CATEGORY = Pattern.compile("[a-z][a-z0-9_]*");
    private static final Pattern PATH = Pattern.compile("(pick|win)_(count|rate)_([a-z][a-z0-9_]*)\\[(\\d+)]");
    private static final Pattern RAW = Pattern.compile("(?:.*_)?\\d+_(?:kit|x_kit|skill|trinket|levelup)_(?:pick|win)|(?:kit|x_kit|skill|trinket|levelup)_(?:picks|wins)");
    private final Map<String, Long> counters = new LinkedHashMap<>();
    // LCM(1..12): every supported trinket selection has an exact integer share.
    private static final BigInteger TRINKET_UNITS=BigInteger.valueOf(27720);
    private final Map<String,BigInteger> personalTrinketWins=new LinkedHashMap<>();
    private int batchDepth;
    private static final java.util.concurrent.Executor WRITER=java.util.concurrent.Executors.newSingleThreadExecutor(r->{
        var thread=new Thread(r,"skywars2-perk-statistics-io");thread.setDaemon(true);return thread;
    });
    private final java.util.concurrent.Executor writer;
    private final Object diskLock=new Object();
    private record Snapshot(boolean imported, String legacySnapshot, Set<String> archivedLegacySources, List<String> receipts,
                            Map<String, Long> counters, Map<String, BigInteger> trinketWins) {}

    private Snapshot queuedSnapshot;
    private boolean writing;
    private java.util.concurrent.CompletableFuture<Void> pending=java.util.concurrent.CompletableFuture.completedFuture(null);
    private final Set<String> receipts = new LinkedHashSet<>();
    private final Path file;
    private boolean imported;
    private boolean dirty;
    // Retain the entire source, including unfamiliar keys, before removing command storage.
    private String legacySnapshot = "";
    private final Set<String> archivedLegacySources = new LinkedHashSet<>();

    /** Isolated fixture stores use the same persistence and migration code as production. */
    LbcPerkStatistics(Path file) throws IOException {this(file,WRITER);}
    LbcPerkStatistics(Path file,java.util.concurrent.Executor writer) throws IOException {
        this.file = file.toAbsolutePath().normalize();this.writer=writer;
        load();
    }

    /** Call during server startup. Any failed read/write leaves command storage intact. */
    public static void initialize(MinecraftServer server) {
        get(server);
    }

    public static LbcPerkStatistics get(MinecraftServer server) {
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

    public static boolean importCurrentStorage(MinecraftServer server) throws IOException {
        if(server.getCommandStorage().get(LEGACY).isEmpty())return false;
        var store=get(server);
        store.migrateLegacy(server.getCommandStorage().get(LEGACY),
                () -> server.getCommandStorage().set(LEGACY,new CompoundTag()));
        return true;
    }

    /** Cleanup is invoked only after the import marker and full source image are durable. */
    void migrateLegacy(CompoundTag legacy, Runnable cleanup) throws IOException {
        String source=legacy.toString();
        if (!imported || !legacy.isEmpty()&&!legacySnapshot.equals(source)&&!archivedLegacySources.contains(source)) {
            var before = new LinkedHashMap<>(counters);
            String previousSnapshot = legacySnapshot;
            boolean alreadyImported=imported;
            boolean archived=false;
            try {
                for (String key : legacy.keySet()) {
                    if (RAW.matcher(key).matches() && legacy.get(key) instanceof NumericTag number)
                        counters.merge(key, Math.max(0L, number.longValue()), Math::max);
                }
                if(!imported||legacySnapshot.equals("{}"))legacySnapshot=source;
                else archived=archivedLegacySources.add(source);
                imported = true;
                save();
            } catch (IOException | RuntimeException error) {
                counters.clear();
                counters.putAll(before);
                legacySnapshot = previousSnapshot;
                imported = alreadyImported;
                if(archived)archivedLegacySources.remove(source);
                throw error;
            }
        }
        // A crash before the world saves this removal retries cleanup, never the import.
        if (!legacy.isEmpty()) cleanup.run();
    }

    /** IDs are native one-based selections. Pass accepted choices once per match start. */
    public void recordPicks(String receipt, String playerName, Map<String, List<Integer>> choices) {
        record(receipt, playerName, choices, "pick");
    }

    /** Use the same match-start choices, including for offline winners. */
    public void recordWins(String receipt, String playerName, Map<String, List<Integer>> choices) {
        record(receipt, playerName, choices, "win");
    }

    private void record(String receipt, String playerName, Map<String, List<Integer>> choices, String kind) {
        if (receipt == null || receipt.isBlank()) throw new IllegalArgumentException("Missing perk-statistics event receipt");
        String event = receipt + "/" + kind;
        if (receipts.contains(event)) { flush(); return; }
        if (playerName == null || playerName.isBlank()) throw new IllegalArgumentException("Missing perk-statistics player name");
        var increments = new LinkedHashMap<String, Long>();
        var trinketIncrements = new LinkedHashMap<String, BigInteger>();
        try {
            for (var entry : choices.entrySet()) {
                String category = entry.getKey();
                if (!CATEGORY.matcher(category).matches()) throw new IllegalArgumentException("Invalid perk-statistics category " + category);
                boolean weighted=kind.equals("win")&&category.equals("trinket");
                long selected=entry.getValue().stream().filter(id->id>0).count();
                if(weighted&&selected>12)throw new IllegalArgumentException("More than twelve selected trinkets");
                var share=weighted&&selected>0?TRINKET_UNITS.divide(BigInteger.valueOf(selected)):BigInteger.ZERO;
                for (int nativeId : entry.getValue()) {
                    if (nativeId <= 0) continue;
                    String suffix = (nativeId - 1) + "_" + category + "_" + kind;
                    increments.merge(suffix, 1L, Long::sum);
                    if (weighted) trinketIncrements.merge(playerName + "_" + suffix, share, BigInteger::add);
                    else increments.merge(playerName + "_" + suffix, 1L, Long::sum);
                    increments.merge(category + "_" + kind + "s", 1L, Long::sum);
                }
            }
        } catch (RuntimeException error) {
            throw new IllegalStateException("Invalid perk-statistics event", error);
        }
        increments.forEach((key, amount) -> counters.merge(key, amount,
                (previous, added) -> previous > Long.MAX_VALUE - added ? Long.MAX_VALUE : previous + added));
        trinketIncrements.forEach((key, amount) -> personalTrinketWins.merge(key, amount, BigInteger::add));
        receipts.add(event);
        dirty = true;
        flush();
    }

    /** Runtime disk failures retain pending counters for the next accepted event or shutdown. */
    public void flush() {
        if(batchDepth>0)return;
        awaitWrites();if(!dirty)return;
        try {
            save();
            dirty = false;
        } catch (IOException error) {
            me.skywars2.SkyWars2.LOGGER.error("Could not save perk statistics; pending counters retained in memory", error);
        }
    }

    /** One durable write for the accepted players of the same match, including exceptional exits. */
    Batch batch(){batchDepth++;return new Batch(false);}
    /** Match-start counters are immediately visible; only the immutable disk image is deferred. */
    public Batch batchDeferred(){batchDepth++;return new Batch(true);}
    public final class Batch implements AutoCloseable {
        private boolean closed;private final boolean deferred;
        Batch(boolean deferred){this.deferred=deferred;}
        @Override public void close(){if(!closed){closed=true;if(--batchDepth==0){if(deferred)flushDeferred();else flush();}}}
    }
    private void flushDeferred(){
        if(batchDepth>0||!dirty)return;
        var snapshot = snapshot();
        synchronized(diskLock){
            queuedSnapshot = snapshot;
            if(!writing){
                writing=true;
                pending=java.util.concurrent.CompletableFuture.runAsync(this::drain,writer).whenComplete((unused,error)->{
                    if(error!=null)me.skywars2.SkyWars2.LOGGER.error("Deferred perk-statistics write failed; counters retained for retry",error);
                });
            }
        }
        dirty=false;
    }
    /** One writer plus one latest snapshot: rapid restarts cannot accumulate old JSON images. */
    private void drain(){
        for(;;){
            Snapshot next;
            synchronized(diskLock){next=queuedSnapshot;queuedSnapshot=null;if(next==null){writing=false;return;}}
            try{write(payload(next));}catch(IOException error){synchronized(diskLock){writing=false;}throw new java.util.concurrent.CompletionException(error);}
        }
    }
    /** Synchronous/import/shutdown boundaries fence older images before replacing the file. */
    private void awaitWrites(){
        try{pending.join();}catch(java.util.concurrent.CompletionException error){dirty=true;}
        synchronized(diskLock){queuedSnapshot=null;pending=java.util.concurrent.CompletableFuture.completedFuture(null);}
    }

    public static String value(MinecraftServer server, String playerName, String path) {
        return get(server).value(playerName, path);
    }

    String value(String playerName, String path) {
        var match = PATH.matcher(path);
        if (!match.matches()) throw new IllegalArgumentException("Unknown perk-statistics path " + path);
        String kind = match.group(1), category = match.group(3), id = match.group(4);
        String suffix = id + "_" + category + "_" + kind;
        if (match.group(2).equals("count")) {
            String key=playerName+"_"+suffix;
            if(kind.equals("win")&&category.equals("trinket"))return BigDecimal.valueOf(count(key))
                    .add(new BigDecimal(personalTrinketWins.getOrDefault(key,BigInteger.ZERO))
                            .divide(new BigDecimal(TRINKET_UNITS),8,RoundingMode.HALF_UP)).stripTrailingZeros().toPlainString();
            return Long.toString(count(key));
        }
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
            if(root.has("archivedLegacySources"))for(var source:root.getAsJsonArray("archivedLegacySources"))
                archivedLegacySources.add(source.getAsString());
            if (root.has("receipts")) for (var receipt : root.getAsJsonArray("receipts")) receipts.add(receipt.getAsString());
            for (var entry : root.getAsJsonObject("counters").entrySet()) {
                long value = entry.getValue().getAsBigDecimal().longValueExact();
                if (value < 0) throw new IOException("Negative perk-statistics counter " + entry.getKey());
                counters.put(entry.getKey(), value);
            }
            if(root.has("personalTrinketWinUnits"))for(var entry:root.getAsJsonObject("personalTrinketWinUnits").entrySet()){
                var value=entry.getValue().getAsBigDecimal().toBigIntegerExact();
                if(value.signum()<0)throw new IOException("Negative personal trinket wins "+entry.getKey());
                personalTrinketWins.put(entry.getKey(),value);
            }
        }
    }

    private void save() throws IOException {awaitWrites();write(payload(snapshot()));}

    private Snapshot snapshot() {
        return new Snapshot(imported, legacySnapshot, new LinkedHashSet<>(archivedLegacySources), List.copyOf(receipts),
                new LinkedHashMap<>(counters), new LinkedHashMap<>(personalTrinketWins));
    }

    private static byte[] payload(Snapshot snapshot) {
        var root = new JsonObject();
        root.addProperty("version", 1);
        root.addProperty("legacyImported", snapshot.imported());
        root.addProperty("legacySnapshot", snapshot.legacySnapshot());
        if(!snapshot.archivedLegacySources().isEmpty()){
            var archived=new com.google.gson.JsonArray();
            snapshot.archivedLegacySources().forEach(archived::add);
            root.add("archivedLegacySources",archived);
        }
        var accepted = new com.google.gson.JsonArray();
        snapshot.receipts().forEach(accepted::add);
        root.add("receipts", accepted);
        var values = new JsonObject();
        snapshot.counters().forEach(values::addProperty);
        root.add("counters", values);
        var trinkets = new JsonObject();
        snapshot.trinketWins().forEach(trinkets::addProperty);
        root.add("personalTrinketWinUnits", trinkets);
        return new GsonBuilder().setPrettyPrinting().create().toJson(root).getBytes(StandardCharsets.UTF_8);
    }
    private void write(byte[] payload) throws IOException {
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
