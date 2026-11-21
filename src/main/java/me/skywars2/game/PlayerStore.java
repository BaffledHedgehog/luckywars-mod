package me.skywars2.game;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** Persistent per-player selections and SkyWars statistics. */
public final class PlayerStore {
    private static final Path FILE = Path.of("config", "luckywars", "players.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Executor AUTOSAVE_WRITER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "lbc-player-save");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });

    private final Map<UUID, Profile> profiles = new LinkedHashMap<>();
    private final Path file;
    private final Executor writer;
    private final Object queueLock = new Object();
    private final Object diskLock = new Object();
    private long revision;
    private long writtenRevision;
    private SaveSnapshot pendingSave;
    private boolean writerScheduled;

    private record SaveSnapshot(long revision, Map<String, Profile> profiles) {}

    public PlayerStore(Path file, Executor writer) {
        this.file = file;
        this.writer = writer;
    }

    public static PlayerStore load() {
        PlayerStore store = new PlayerStore(FILE, AUTOSAVE_WRITER);
        if (!Files.isRegularFile(FILE)) return store;

        try (var reader = Files.newBufferedReader(FILE)) {
            var json = JsonParser.parseReader(reader);
            if (!json.isJsonObject()) throw new JsonParseException("Expected a JSON object");
            var root = json.getAsJsonObject();
            for (var entry : root.entrySet()) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(entry.getKey());
                } catch (IllegalArgumentException ignored) {
                    // Skip malformed UUID keys while preserving all valid profiles.
                    continue;
                }
                Profile profile = GSON.fromJson(entry.getValue(), Profile.class);
                if (profile == null) throw new JsonParseException("Profile " + entry.getKey() + " is not an object");
                // Старый scalar сохраняется для совместимости; только старые JSON мигрируют в первый слот выбора.
                if (!entry.getValue().getAsJsonObject().has("trinkets") && profile.lbcTrinket > 0) {
                    profile.trinkets[0] = profile.lbcTrinket;
                    profile.lbcTrinketSlot = 1;
                }
                profile.normalize();
                store.profiles.put(uuid, profile);
            }
        } catch (IOException e) {
            System.err.println("Could not read " + FILE + ": " + e.getMessage());
            return store;
        } catch (RuntimeException e) {
            backupCorruptFile();
            store.profiles.clear();
            System.err.println("Invalid JSON in " + FILE + "; restored empty player store: " + e.getMessage());
            store.save();
        }
        return store;
    }

    public Profile get(UUID uuid) {
        return profiles.computeIfAbsent(uuid, ignored -> new Profile());
    }

    /** Enumerate already known players without creating profiles for command input. */
    public Map<UUID, Profile> knownProfiles() {
        return java.util.Collections.unmodifiableMap(profiles);
    }

    public Map<UUID, MatchValueSnapshot> checkpointProfiles() {
        var result = new LinkedHashMap<UUID, MatchValueSnapshot>();
        profiles.forEach((id, profile) -> result.put(id, new MatchValueSnapshot(profile)));
        return java.util.Collections.unmodifiableMap(result);
    }

    public void restoreProfiles(Map<UUID, MatchValueSnapshot> image) {
        profiles.clear();
        image.forEach((id, saved) -> profiles.put(id, (Profile) saved.restore()));
    }

    public Profile get(UUID uuid, String username) {
        Profile profile = get(uuid);
        if (username != null && !username.isBlank() && !username.equals(profile.username)) {
            profile.username = username;
            saveDeferred();
        }
        return profile;
    }

    public void save() {
        try {
            write(snapshot(false), false);
        } catch (IOException error) {
            logWriteFailure(error);
        }
    }

    /** Called on the server thread. At most one newer snapshot waits behind the active write. */
    public void saveDeferred() {
        SaveSnapshot snapshot = snapshot(true);
        synchronized (queueLock) {
            pendingSave = snapshot;
            scheduleWriter();
        }
    }

    /** Checked durable save for transactions such as key debit and case payout. */
    public void saveAtomic() throws IOException {
        write(snapshot(false), true);
    }

    private SaveSnapshot snapshot(boolean copy) {
        var serialized = new LinkedHashMap<String, Profile>();
        profiles.forEach((uuid, profile) -> {
            if (copy) {
                serialized.put(uuid.toString(), profile.storageCopy());
            } else {
                profile.normalize();
                serialized.put(uuid.toString(), profile);
            }
        });
        return new SaveSnapshot(++revision, serialized);
    }

    // Called with queueLock held. The writer never holds that lock during normalization or I/O.
    private void scheduleWriter() {
        if (writerScheduled || pendingSave == null) return;
        writerScheduled = true;
        writer.execute(this::drainSaves);
    }

    private void drainSaves() {
        try {
            while (true) {
                SaveSnapshot snapshot;
                synchronized (queueLock) {
                    snapshot = pendingSave;
                    pendingSave = null;
                }
                if (snapshot == null) return;

                try {
                    snapshot.profiles().values().forEach(Profile::normalize);
                    write(snapshot, false);
                } catch (IOException error) {
                    logWriteFailure(error);
                }
            }
        } finally {
            synchronized (queueLock) {
                writerScheduled = false;
                scheduleWriter();
            }
        }
    }

    private void write(SaveSnapshot snapshot, boolean durable) throws IOException {
        synchronized (diskLock) {
            // A foreground save can overtake a queued autosave. Never replace a newer file.
            if (snapshot.revision() <= writtenRevision) return;

            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            try {
                Files.createDirectories(file.getParent());
                String json = GSON.toJson(snapshot.profiles());
                if (durable) {
                    try (var channel = FileChannel.open(temporary, StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                        var buffer = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));
                        while (buffer.hasRemaining()) channel.write(buffer);
                        channel.force(true);
                    }
                } else {
                    Files.writeString(temporary, json);
                }
                try {
                    Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException error) {
                    if (durable) throw error;
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
                writtenRevision = snapshot.revision();
            } finally {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    private void logWriteFailure(IOException error) {
        System.err.println("Could not write " + file + ": " + error.getMessage());
    }

    public static final class Profile implements Cloneable {
        public String username = "";
        public int coins;
        public int memcoins;
        public int games;
        public int wins;
        public int kills;
        public int deaths;
        public boolean legacyScoreboardImported;
        public int[] keys = new int[5];
        public int kit;
        public int skill;
        public int killSound;
        public int suffix;
        public int winEffect;
        public int cage;
        public int killEffect;
        public int fovEffects = 5;
        public boolean modeScroll = true;
        // Exact SWRG tags/advancements, separate from LBC achievement unlocks.
        public Set<String> legacyUnlocks = new HashSet<>();
        // Prevent stale offline tags/impossible-advancement projections from undoing a manual revoke.
        public Set<String> revokedLegacyUnlocks = new HashSet<>();
        // Explicit administrator decisions override stale offline advancement files on login.
        public Set<String> dropRequestOverrides = new HashSet<>();
        // Immutable typed LbcCases.Pending serialized as one scalar. MatchValueSnapshot
        // already captures Strings exactly, including offline profiles; no ItemStack/NBT here.
        public String casePending = "";
        // Immutable serialized personal presets; parsed only during build-manager actions.
        // A scalar also participates in the existing exact match-profile checkpoint.
        public String savedBuilds = "";
        public int lbcKit;
        public int lbcSkill;
        public int lbcTrinket;
        public int[] trinkets = new int[12];
        public int lbcTrinketSlot;
        public int lbcChallenge;
        public int lbcChallengeProgress;
        public int lbcChallengeKit;
        // -1 means legacy scoreboard state has not been migrated yet.
        public int standOverheavenMatches = -1;
        // Migration: old four-slot profiles keep their first four choices; new slots append to 12.
        public int[] levelups = new int[12];
        // Persist the next cyclic slot (0..11) so expanding the array does not reset selection order.
        public int lbcLevelupSlot = 0;
        public Set<String> lbcUnlocks = new HashSet<>();

        private Profile storageCopy() {
            try {
                Profile copy = (Profile) super.clone();
                copy.keys = keys == null ? null : keys.clone();
                copy.trinkets = trinkets == null ? null : trinkets.clone();
                copy.levelups = levelups == null ? null : levelups.clone();
                copy.legacyUnlocks = copyUnlocks(legacyUnlocks);
                copy.revokedLegacyUnlocks = copyUnlocks(revokedLegacyUnlocks);
                copy.dropRequestOverrides = copyUnlocks(dropRequestOverrides);
                copy.lbcUnlocks = copyUnlocks(lbcUnlocks);
                return copy;
            } catch (CloneNotSupportedException impossible) {
                throw new AssertionError(impossible);
            }
        }

        private static Set<String> copyUnlocks(Set<String> source) {
            return source == null ? null : new HashSet<>(source);
        }

        private void normalize() {
            if (username == null) username = "";
            fovEffects = Math.clamp(fovEffects, 0, 100);
            coins = Math.max(0, coins);
            memcoins = Math.max(0, memcoins);
            games = Math.max(0, games);
            wins = Math.max(0, wins);
            kills = Math.max(0, kills);
            deaths = Math.max(0, deaths);
            kit = Math.max(0, kit);
            skill = Math.max(0, skill);
            killSound = Math.max(0, killSound);
            suffix = Math.max(0, suffix);
            winEffect = Math.max(0, winEffect);
            cage = Math.max(0, cage);
            killEffect = Math.clamp(killEffect, 0, 3);
            if (casePending == null) casePending = "";
            if (savedBuilds == null) savedBuilds = "";
            Set<String> normalizedLegacy = new HashSet<>();
            if (legacyUnlocks != null) for (String unlock : legacyUnlocks)
                if (LbcSourceUnlocks.valid(unlock)) normalizedLegacy.add(unlock);
            legacyUnlocks = normalizedLegacy;
            Set<String> normalizedRevokes = new HashSet<>();
            if(revokedLegacyUnlocks!=null)for(String id:revokedLegacyUnlocks)
                if(LbcSourceUnlocks.administrativeIds().contains(id))normalizedRevokes.add(id);
            revokedLegacyUnlocks=normalizedRevokes;legacyUnlocks.removeAll(revokedLegacyUnlocks);
            Set<String> normalizedRequests = new HashSet<>();
            if (dropRequestOverrides != null) for (String id : dropRequestOverrides)
                if (id != null && id.matches("[1-3]")) normalizedRequests.add(id);
            dropRequestOverrides = normalizedRequests;
            lbcKit = Math.max(0, lbcKit);
            lbcSkill = Math.max(0, lbcSkill);
            lbcTrinket = Math.max(0, lbcTrinket);
            lbcChallenge = Math.max(0, lbcChallenge);
            lbcChallengeProgress = Math.clamp(lbcChallengeProgress, 0, 19);
            lbcChallengeKit = Math.clamp(lbcChallengeKit, 0, 1);
            standOverheavenMatches = Math.max(-1, standOverheavenMatches);
            keys = normalizeArray(keys, 5);
            levelups = normalizeArray(levelups, 12);
            Set<Integer> uniqueLevelups = new HashSet<>();
            for (int i = 0; i < levelups.length; i++)
                if (levelups[i] != 0 && !uniqueLevelups.add(levelups[i])) levelups[i] = 0;
            trinkets = normalizeArray(trinkets, 12);
            lbcTrinketSlot = Math.floorMod(lbcTrinketSlot, 12);
            lbcLevelupSlot = Math.floorMod(lbcLevelupSlot, 12);
            Set<String> normalizedUnlocks = new HashSet<>();
            if (lbcUnlocks != null) {
                for (String unlock : lbcUnlocks) {
                    if (unlock != null && unlock.matches("lbc:true_advancements/[a-z0-9._-]+(?:/[a-z0-9._-]+)*")) {
                        normalizedUnlocks.add(unlock);
                    }
                }
            }
            lbcUnlocks = normalizedUnlocks;
        }

        private static int[] normalizeArray(int[] source, int length) {
            int[] normalized = new int[length];
            if (source != null) {
                System.arraycopy(source, 0, normalized, 0, Math.min(source.length, length));
            }
            for (int i = 0; i < normalized.length; i++) normalized[i] = Math.max(0, normalized[i]);
            return normalized;
        }
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
