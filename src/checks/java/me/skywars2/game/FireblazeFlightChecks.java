package me.skywars2.game;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import me.skywars2.SkyWars2;
import me.skywars2.game.combat.MatchDamageSources;
import me.skywars2.game.items.wands.LbcBlaze;
import me.skywars2.game.items.wands.LbcFartus;
import me.skywars2.game.items.wands.LbcWands;
import me.skywars2.game.match.checkpoints.MatchActorCheckpoint;
import me.skywars2.game.match.checkpoints.MatchAuraCheckpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.phys.Vec3;

/** A native cast with the last fireball beyond the simulation distance of every player. */
final class FireblazeFlightChecks {
    private static Scene running;

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Fireblaze flight: " + message);
    }

    private static final class Scene implements AutoCloseable {
        final ElementalGrimoireChecks.Fixture fixture;
        final TreeMap<Integer, LargeFireball> shots = new TreeMap<>();
        final Vec3 origin = new Vec3(224.5, 260, 224.5);
        final Vec3 farOrigin = origin.add(0, 0, 150);
        final boolean invulnerable;
        final int useStat;
        final Set<ChunkPos> visited = new HashSet<>();
        int started;
        boolean arrived;

        Scene(GameController game) {
            fixture = new ElementalGrimoireChecks.Fixture(game);
            invulnerable = fixture.owner.getAbilities().invulnerable;
            useStat = fixture.owner.getStats().getValue(Stats.ITEM_USED.get(LbcWands.FIREBLAZE));
        }

        void cast() {
            fixture.initialize();
            var owner = fixture.owner;
            var level = owner.level();
            level.getChunk(BlockPos.containing(origin));
            owner.teleportTo(level, origin.x, origin.y, origin.z, Set.of(), 0, 0, true);
            require(!level.isPositionEntityTicking(BlockPos.containing(farOrigin)),
                    "native +150 origin must start outside entity simulation");
            owner.getAbilities().invulnerable = true;
            owner.setShiftKeyDown(true);
            var wand = new ItemStack(LbcWands.FIREBLAZE);
            var flags = LbcFartus.data(wand);
            flags.putInt("upgradable", 1);
            wand.set(DataComponents.CUSTOM_DATA, CustomData.of(flags));
            owner.setItemInHand(InteractionHand.MAIN_HAND, wand);
            int cost = fixture.mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("fireblaze_wand_mana_use"), "lbc.math").get();
            fixture.mana.set(owner, cost);
            require(fixture.input(InteractionHand.MAIN_HAND) && fixture.mana.get(owner) == 0 &&
                    LbcFartus.data(wand).getIntOr("upgrade_available", 0) == 1,
                    "real paid shift cast completes the original academy trial");
            started = fixture.game.server().getTickCount();
            for (var actor : MatchActorCheckpoint.currentBodies(fixture.game).values()) {
                if (actor instanceof LargeFireball ball && ball.getOwner() == owner &&
                        ball.entityTags().contains(LbcBlaze.TRIAL_PROJECTILE)) {
                    require(shots.put(GameChecks.fireballPower(ball), ball) == null, "one trial body per power");
                }
            }
            require(shots.size() == 14 && shots.firstKey() == 1 && shots.lastKey() == 14,
                    "all fourteen native bodies in entity sections: " + shots.keySet());
            require(shots.get(14).position().equals(farOrigin), "power fourteen starts exactly +150 on Z");
            SkyWars2.LOGGER.info("[check] Fireblaze far origin resident={} ticking={} observable={}",
                    level.hasChunkAt(BlockPos.containing(farOrigin)), level.isPositionEntityTicking(BlockPos.containing(farOrigin)),
                    level.getEntity(shots.get(14).getUUID()) == shots.get(14));
        }

        private long tickets(ChunkPos pos) {
            try {
                var source = fixture.owner.level().getChunkSource();
                var field = source.getClass().getDeclaredField("ticketStorage");
                field.setAccessible(true);
                var storage = (TicketStorage) field.get(source);
                var id = Identifier.parse("skywars2:fireblaze_trial");
                return storage.getTickets(pos.pack()).stream()
                        .filter(ticket -> id.equals(BuiltInRegistries.TICKET_TYPE.getKey(ticket.getType()))).count();
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException(error);
            }
        }

        private void replayAuraCheckpoint() {
            var game = fixture.game;
            var bodies = MatchActorCheckpoint.currentBodies(game);
            var sources = new MatchDamageSources(game, bodies.values());
            var checkpoint = new MatchAuraCheckpoint(game, sources);
            var tickRate = game.server().tickRateManager();
            boolean wasFrozen = tickRate.isFrozen();
            tickRate.setFrozen(true);
            try {
                checkpoint.prepareFrozen();
                require(checkpoint.restoreFrozen(bodies::get).isEmpty(), "native Fireblaze aura references are rebound");
                checkpoint.publishFrozen();
            } finally {
                tickRate.setFrozen(wasFrozen);
            }
        }

        boolean tick() {
            if (arrived) {
                require(visited.stream().noneMatch(pos -> tickets(pos) != 0),
                        "all transient trial tickets are released after native removal");
                return true;
            }
            shots.values().forEach(shot -> visited.add(shot.chunkPosition()));
            var far = shots.get(14);
            int elapsed = fixture.game.server().getTickCount() - started;
            require(elapsed <= 160, "power 14 remained at " + far.position() + " after " + far.tickCount +
                    " native ticks, active destination=" + fixture.owner.level().isPositionEntityTicking(far.blockPosition()));
            require(!far.isRemoved(), "power 14 survives open air at the edge of simulation");
            if (farOrigin.z - far.getZ() < 20) return false;
            require(far.tickCount > 0 && far.getOwner() == fixture.owner && far.getX() == farOrigin.x &&
                    far.getY() == farOrigin.y && fixture.game.items().currentEpoch(far),
                    "power 14 advances across a chunk boundary with native physics and original attribution");
            require(tickets(ChunkPos.containing(BlockPos.containing(farOrigin))) == 0 &&
                    tickets(far.chunkPosition()) == 1, "one shared native ticket follows the active fireball");
            replayAuraCheckpoint();
            require(tickets(far.chunkPosition()) == 1, "checkpoint republishes exactly one ticket on the restored body");
            SkyWars2.LOGGER.info("[check] Fireblaze power 14 advanced {} blocks in {} native ticks",
                    farOrigin.z - far.getZ(), far.tickCount);
            shots.values().forEach(LargeFireball::discard);
            arrived = true;
            return false;
        }

        @Override
        public void close() {
            try {
                shots.values().forEach(LargeFireball::discard);
                fixture.owner.getAbilities().invulnerable = invulnerable;
                fixture.owner.getStats().setValue(fixture.owner, Stats.ITEM_USED.get(LbcWands.FIREBLAZE), useStat);
            } finally {
                fixture.close();
            }
        }
    }

    static void run(GameController game) {
        require(running == null, "exclusive fixture");
        running = new Scene(game);
        try {
            running.cast();
        } catch (RuntimeException | Error failure) {
            try { running.close(); } catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); }
            running = null;
            throw failure;
        }
    }

    static void tick(GameController game) {
        if (running == null) return;
        try {
            if (!running.tick()) return;
            running.close();
            running = null;
            SkyWars2.LOGGER.info("[check] PASS fireblaze-flight");
        } catch (RuntimeException | Error failure) {
            try { running.close(); } catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); }
            running = null;
            SkyWars2.LOGGER.error("[check] FAIL fireblaze-flight", failure);
        }
    }
}
