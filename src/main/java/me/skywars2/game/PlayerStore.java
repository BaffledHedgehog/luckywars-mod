package me.skywars2.game;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Persistent per-player selections and SkyWars statistics. */
public final class PlayerStore {
    private static final Path FILE = Path.of("config", "luckywars", "players.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Map<UUID, Profile> profiles = new LinkedHashMap<>();

    private PlayerStore() {}

    public static PlayerStore load() {
        PlayerStore store = new PlayerStore();
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
    Map<UUID, Profile> knownProfiles() {
        return java.util.Collections.unmodifiableMap(profiles);
    }

    Map<UUID, MatchValueSnapshot> checkpointProfiles() {
        var result = new LinkedHashMap<UUID, MatchValueSnapshot>();
        profiles.forEach((id, profile) -> result.put(id, new MatchValueSnapshot(profile)));
        return java.util.Collections.unmodifiableMap(result);
    }

    void restoreProfiles(Map<UUID, MatchValueSnapshot> image) {
        profiles.clear();
        image.forEach((id, saved) -> profiles.put(id, (Profile) saved.restore()));
    }

    public Profile get(UUID uuid, String username) {
        Profile profile = get(uuid);
        if (username != null && !username.isBlank() && !username.equals(profile.username)) {
            profile.username = username;
            save();
        }
        return profile;
    }

    public void save() {
        // Профили хранятся по UUID; сначала записываем временный JSON, затем заменяем файл (атомарно, если ФС поддерживает).
        Path temporary = FILE.resolveSibling(FILE.getFileName() + ".tmp");
        try {
            Files.createDirectories(FILE.getParent());
            Map<String, Profile> serialized = new LinkedHashMap<>();
            profiles.forEach((uuid, profile) -> {
                profile.normalize();
                serialized.put(uuid.toString(), profile);
            });
            Files.writeString(temporary, GSON.toJson(serialized));
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

    /** Checked canonical save for case debit and payout; ordinary autosave is unchanged. */
    void saveAtomic() throws IOException {
        Path temporary = FILE.resolveSibling(FILE.getFileName() + ".tmp");
        try {
            Files.createDirectories(FILE.getParent());
            Map<String, Profile> serialized = new LinkedHashMap<>();
            profiles.forEach((uuid, profile) -> {
                profile.normalize();
                serialized.put(uuid.toString(), profile);
            });
            byte[] payload = GSON.toJson(serialized).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            try (var channel = java.nio.channels.FileChannel.open(temporary,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                    java.nio.file.StandardOpenOption.WRITE)) {
                var buffer = java.nio.ByteBuffer.wrap(payload);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            // Unsupported atomic replacement fails acceptance; never partially accept the key debit.
            Files.move(temporary, FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
        }
    }

    public static final class Profile {
        public String username = "";
        public int coins;
        public int memcoins;
        public int games;
        public int wins;
        public int kills;
        public int deaths;
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
