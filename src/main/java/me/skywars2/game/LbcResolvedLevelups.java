package me.skywars2.game;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntPredicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/** Match choices and their active effects. Profile/menu IDs remain the player's original picks. */
final class LbcResolvedLevelups {
    /** Zero means not rolled; every nonzero outcome is a real, installed upgrade. */
    record Image(int randomOutcome, int firstStat, int secondStat, long effectiveMask) {
        Image {
            if (randomOutcome < 0 || randomOutcome > 44 || randomOutcome != 0 && !contains(REAL_MASK, randomOutcome)
                    || firstStat < -1 || firstStat >= 21
                    || secondStat < -1 || secondStat >= 21 || (firstStat == -1) != (secondStat == -1)
                    || (effectiveMask & ~REAL_MASK) != 0)
                throw new IllegalArgumentException("Invalid resolved source levelup image");
        }
    }
    private static final long REAL_MASK = ((1L << 45) - 2)
            & ~(1L << 14 | 1L << 19 | 1L << 26 | 1L << 27);
    private static final Image EMPTY = new Image(0, -1, -1, 0);
    private final GameController game;
    private final RandomSource random;
    private final Map<UUID, Image> images = new HashMap<>();
    private boolean restoring;

    LbcResolvedLevelups(GameController game) { this(game, RandomSource.create()); }
    LbcResolvedLevelups(GameController game, RandomSource random) {
        this.game = game;
        this.random = java.util.Objects.requireNonNull(random);
    }
    boolean rebuilding() { return restoring; }
    Image image(UUID player) { thread(); return images.getOrDefault(player, EMPTY); }

    private void thread() {
        if (!game.server().isSameThread()) throw new IllegalStateException("Resolve levelups on the server thread");
    }
    private void mutable() {
        thread();
        if (restoring) throw new IllegalStateException("Do not mutate choices while restoring canonical bodies");
    }
    static boolean contains(long mask, int id) { return id > 0 && id <= 44 && (mask & 1L << id) != 0; }

    /** Only raw picks pass the ordinary gate. A resolved Tatam result bypasses that result's gate. */
    long resolve(ServerPlayer player, int[] raw, int limit, boolean enabled, IntPredicate allowedRaw) {
        mutable();
        UUID id = player.getUUID();
        Image before = images.getOrDefault(id, EMPTY);
        int outcome = before.randomOutcome(), first = before.firstStat(), second = before.secondStat();
        long effective = 0;
        boolean selectedRandom = false;
        if (enabled) for (int slot = 0; slot < Math.min(Math.max(0, limit), raw.length); slot++) {
            int pick = raw[slot];
            if (pick <= 0 || pick > 44 || !allowedRaw.test(pick)) continue;
            if (pick == 14) selectedRandom = true;
            else effective |= 1L << pick & REAL_MASK;
        }
        if (selectedRandom) {
            // One uniform draw over distinct installed upgrades, independent of unlock gates.
            // A retained result never rerolls on respawn, removal/reselection or a later duplicate pick.
            if (outcome == 0) {
                long candidates = REAL_MASK & ~effective;
                int count = Long.bitCount(candidates);
                if (count != 0) {
                    int selected = random.nextInt(count);
                    for (int candidate = 1; candidate <= 44; candidate++) {
                        if (!contains(candidates, candidate)) continue;
                        if (selected-- == 0) { outcome = candidate; break; }
                    }
                }
            }
            if (outcome != 0) effective |= 1L << outcome;
        }
        if (contains(effective, 1) && first == -1) {
            first = random.nextInt(21);
            second = random.nextInt(21);
        }
        // Keep dormant choices after debug removal so selecting14/1 again cannot fish for a reroll.
        images.put(id, new Image(outcome, first, second, effective));
        return effective;
    }

    long mask(ServerPlayer player) {
        thread();
        return restoring ? 0 : images.getOrDefault(player.getUUID(), EMPTY).effectiveMask();
    }
    boolean has(ServerPlayer player, int id) { return contains(mask(player), id); }
    static Set<Integer> ids(long mask) {
        var result = new HashSet<Integer>();
        for (int id = 1; id <= 44; id++) if (contains(mask, id)) result.add(id);
        return Set.copyOf(result);
    }
    void applyRandomStats(ServerPlayer player) {
        mutable();
        var image = images.getOrDefault(player.getUUID(), EMPTY);
        if (!contains(image.effectiveMask(), 1)) return;
        if (image.firstStat() < 0) throw new IllegalStateException("Resolve random-stat choices before applying them");
        LbcRandomStatLevelup.applyDraw(player, 1, image.firstStat());
        LbcRandomStatLevelup.applyDraw(player, 2, image.secondStat());
    }

    /** Attribute/resource removal, death and role cleanup remove activity, never the match choices. */
    void deactivate(ServerPlayer player) {
        thread();
        if (restoring) return;
        images.computeIfPresent(player.getUUID(), (id, image) -> new Image(
                image.randomOutcome(), image.firstStat(), image.secondStat(), 0));
    }
    /** Actual full leave. Ordinary respawn/body replacement must never call this. */
    void forget(UUID player) { mutable(); images.remove(player); }
    /** Full match start/end/reset, including stop/cancel; never same-match map replacement. */
    void resetMatch() { mutable(); images.clear(); }
    /** Clone adapters copy scalars, never re-run source selection or consume RNG. */
    void copy(UUID source, UUID target) {
        mutable();
        var image = images.get(source);
        if (image == null) images.remove(target); else images.put(target, image);
    }

    Checkpoint checkpoint() { thread(); return new Checkpoint(); }
    final class Checkpoint {
        private final String epoch = game.items().epoch();
        private final Map<UUID, Image> saved;
        private final MatchRandomState rng;
        private boolean prepared, installed;
        private Checkpoint() {
            if (!game.playing() || restoring) throw new IllegalStateException("Capture stable match levelup choices");
            saved = Map.copyOf(images);
            rng = MatchRandomState.capture(random);
        }
        private Checkpoint(Map<UUID, Image> saved, MatchRandomState rng) {
            this.saved = Map.copyOf(saved);
            this.rng = rng;
        }
        /** Recaptured points inherit only actual pending offline rows, keeping the current RNG. */
        Checkpoint withPending(Checkpoint previous, Set<UUID> pending) {
            thread();
            var merged = new HashMap<>(saved);
            for (var id : pending) {
                var old = previous.saved.get(id);
                if (old != null) merged.putIfAbsent(id, old);
            }
            return new Checkpoint(merged, rng);
        }
        private void frozen() {
            thread();
            if (!game.playing() || !game.items().currentEpoch(epoch) || game.maps().busy()
                    || !game.server().tickRateManager().isFrozen())
                throw new IllegalStateException("Restore levelup choices in their stable frozen match");
        }
        /** Block loadout reapplication while native player bodies/modifiers are reconstructed. */
        void prepareFrozen() { frozen(); restoring = true; prepared = true; installed = false; }
        void restoreFrozen() {
            frozen();
            if (!prepared) throw new IllegalStateException("Prepare levelup choices before scalar restore");
            images.clear(); images.putAll(saved); rng.restore(random); installed = true;
        }
        /** A delayed login restores one saved row without rewinding other players or the RNG. */
        void restorePlayerFrozen(UUID id) {
            frozen();
            var image = saved.get(id);
            if (image == null) images.remove(id); else images.put(id, image);
        }
        /** Native body checkpoints already restored modifiers/resources; publication writes none. */
        void publishFrozen() {
            frozen();
            if (!installed) throw new IllegalStateException("Install levelup choices before publication");
            restoring = false; prepared = false;
        }
        // Stop may run after its match epoch was invalidated; releasing this scalar gate is safe.
        void cancelFrozen() { thread(); restoring = false; prepared = false; installed = false; }
    }
}
