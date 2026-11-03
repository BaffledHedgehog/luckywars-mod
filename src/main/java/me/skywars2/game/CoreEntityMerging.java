package me.skywars2.game;

import com.google.gson.GsonBuilder;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import java.nio.file.Files;
import java.nio.file.Path;

/** Local entity-index queries, independent of match state. No global entity scans. */
public final class CoreEntityMerging {
    private CoreEntityMerging() { }
    public static final class Settings {
        public double itemMergeRadius = 2.5;
        public double experienceMergeRadius = 3.0;
    }
    private static Settings settings = new Settings();
    public static Settings settings() { return settings; }

    public static void load() {
        var path = Path.of("config", "luckywars", "core.json");
        var gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            if (Files.isRegularFile(path)) {
                try (var reader = Files.newBufferedReader(path)) {
                    var loaded = gson.fromJson(reader, Settings.class);
                    if (loaded == null) throw new IllegalArgumentException("Expected a settings object");
                    loaded.itemMergeRadius = radius(loaded.itemMergeRadius, 2.5);
                    loaded.experienceMergeRadius = radius(loaded.experienceMergeRadius, 3.0);
                    settings = loaded;
                }
            } else {
                Files.createDirectories(path.getParent());
                Files.writeString(path, gson.toJson(settings) + "\n");
            }
        } catch (Exception error) {
            SkyWars2.LOGGER.error("Cannot read {}; using core entity merge defaults", path, error);
            settings = new Settings();
        }
    }

    private static double radius(double value, double fallback) {
        return Double.isFinite(value) ? Math.clamp(value, 0, 16) : fallback;
    }

    /** Implemented on the actual orb; preserves its complete native Count * Value. */
    public interface Orb {
        long skywars2$totalExperience();
        int skywars2$experienceAge();
        boolean skywars2$absorbExperience(ExperienceOrb other);
        void skywars2$normalizeExperience();
    }

    public static boolean enabled(ServerLevel level) {
        var game = SkyWars2.game();
        return settings.experienceMergeRadius > 0
                && (game == null || !game.items().timeStop().stopped(level)&&!game.items().nexusWires().nexusExperience(level));
    }

    /** Called before publication, so absorbed orbs never get their own entity tracker. */
    public static boolean mergeOnSpawn(ServerLevel level, ExperienceOrb incoming) {
        var game=SkyWars2.game();
        if(game!=null&&game.items().nexusWires().convert(incoming))return true;
        if (!enabled(level) || incoming.isRemoved()) return false;
        for (var existing : level.getEntitiesOfClass(ExperienceOrb.class,
                incoming.getBoundingBox().inflate(settings.experienceMergeRadius), other -> !other.isRemoved())) {
            if (((Orb) existing).skywars2$absorbExperience(incoming)) return true;
        }
        ((Orb) incoming).skywars2$normalizeExperience();
        return false;
    }

    /** Existing / loaded orbs keep the native periodic nearby-merge schedule. */
    public static void mergeNearby(ExperienceOrb orb) {
        if (orb.isRemoved()) return;
        ((Orb) orb).skywars2$normalizeExperience();
        for (var other : orb.level().getEntitiesOfClass(ExperienceOrb.class,
                orb.getBoundingBox().inflate(settings.experienceMergeRadius), other -> other != orb && !other.isRemoved())) {
            ((Orb) orb).skywars2$absorbExperience(other);
        }
    }
}
