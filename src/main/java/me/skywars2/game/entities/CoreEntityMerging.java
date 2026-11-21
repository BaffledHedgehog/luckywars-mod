package me.skywars2.game.entities;

import com.google.gson.GsonBuilder;
import me.skywars2.SkyWars2;
import me.skywars2.game.items.drops.LbcSpecialDrops;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import java.nio.file.Files;
import java.nio.file.Path;
import me.skywars2.game.GameController;
import me.skywars2.game.items.weapons.melee.LbcMetalPipe;

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

    /** Native physical drops; the stack's component maximum is never changed. */
    public interface Items {
        boolean skywars2$mergeableItem();
        boolean skywars2$absorbItem(ItemEntity incoming);
        int skywars2$itemPickupDelay();
        java.util.UUID skywars2$itemPickupTarget();
    }

    private static boolean bookkeeping(String tag) {
        if (tag.startsWith("skywars2.item_order.")) return true;
        // The dispatcher resolves the current causal author below, independently
        // of an older persisted fallback author tag.
        if (tag.startsWith("skywars2.special_drop_owner.")) return true;
        var game = SkyWars2.game();
        return game != null && game.items().currentEpoch(tag);
    }

    /** Actor tags and causal trap authors are not interchangeable item quantities. */
    public static boolean sameDrop(ItemEntity a, ItemEntity b) {
        if (!ItemStack.isSameItemSameComponents(a.getItem(), b.getItem())
                || a.isNoGravity() != b.isNoGravity()
                || !java.util.Objects.equals(a.getCustomName(), b.getCustomName())) return false;
        for (var tag : a.entityTags()) if (!bookkeeping(tag) && !b.entityTags().contains(tag)) return false;
        for (var tag : b.entityTags()) if (!bookkeeping(tag) && !a.entityTags().contains(tag)) return false;
        var game = SkyWars2.game();
        if (game == null) return true;
        if (LbcMetalPipe.matches(a.getItem())) return game.items().metalPipe().canMerge(a, b);
        return !LbcSpecialDrops.nativeRecognized(a.getItem()) || game.items().specialDrops().canMerge(a, b);
    }

    /** Absorb into a resident body so the incoming drop never gets an entity tracker. */
    public static boolean mergeOnSpawn(ServerLevel level, ItemEntity incoming) {
        if (!((Items) incoming).skywars2$mergeableItem()) return false;
        // These are activation requests, admitted once per body by ENTITY_LOAD.
        // Combining them before admission changes trap timing and loses requests.
        // Their later native merge still checks the actual causal author.
        if (LbcSpecialDrops.nativeRecognized(incoming.getItem()) || LbcMetalPipe.matches(incoming.getItem())) return false;
        var game = SkyWars2.game();
        if (game != null && game.items().timeStop().stopped(level)) return false;
        double radius = settings.itemMergeRadius;
        var box = incoming.getBoundingBox().inflate(radius > 0 ? radius : .5, radius > 0 ? radius : 0, radius > 0 ? radius : .5);
        for (var resident : level.getEntitiesOfClass(ItemEntity.class, box,
                item -> item != incoming && ((Items) item).skywars2$mergeableItem())) {
            if (((Items) resident).skywars2$absorbItem(incoming)) return true;
        }
        return false;
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
