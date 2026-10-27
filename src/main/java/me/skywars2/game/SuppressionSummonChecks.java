package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Real beacon pulses qualify source-assigned Vex/cultists independently of their caster's position. */
public final class SuppressionSummonChecks {
    private SuppressionSummonChecks() { }
    private static final List<String> KINDS = List.of(
            "vex", "strangeness", "chaos", "luck", "omnipresence", "obsession", "hypocrisy");
    private static final int FLAGS = 2 | 16 | 32 | 256;
    private static Fixture running;

    private static void require(boolean value, String why) {
        if (!value) throw new IllegalStateException("Summon suppression: " + why);
    }

    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final AllGrimoireChecks.Fixture base;
        final BlockPos beacon;
        final Vec3 center;
        int warmTicks;
        boolean closed;

        Fixture(GameController game) {
            this.game = game;
            base = new AllGrimoireChecks.Fixture(game);
            base.reset();
            base.base.owner.getInventory().clearContent();
            beacon = BlockPos.containing(base.at).below(3);
            center = Vec3.atCenterOf(beacon);
        }

        boolean ready() {
            require(++warmTicks <= 200, "resident entity sections become visible");
            var at = BlockPos.containing(base.at);
            for (int x = (at.getX() >> 4) - 2; x <= (at.getX() >> 4) + 2; x++)
                for (int z = (at.getZ() >> 4) - 2; z <= (at.getZ() >> 4) + 2; z++)
                    if (!base.level.areEntitiesLoaded(ChunkPos.pack(x, z))) return false;
            return game.items().summons().ready();
        }

        void zone(Block block) {
            var state = block.defaultBlockState();
            require(base.level.setBlock(beacon, state, FLAGS), "actual beacon block replacement " + block);
            game.items().magic().blockChanged(beacon, state);
            require(block == LbcMagic.MAGIC_BEACON_BLOCK
                            ? game.items().magic().beaconSuppressed(center)
                            : game.items().magic().mechanismSuppressedAt(base.level, center),
                    "actual shared beacon index " + block);
        }

        void removeZone() {
            base.level.setBlock(beacon, Blocks.AIR.defaultBlockState(), FLAGS);
            game.items().magic().blockChanged(beacon, Blocks.AIR.defaultBlockState());
        }

        List<Mob> group(Vec3 at) {
            var actors = new ArrayList<Mob>();
            for (var kind : KINDS) {
                var mob = game.items().summons().spawnPreset(base.base.owner, kind, at);
                require(mob != null && mob.isAlive(), "native preset admission " + kind);
                mob.setNoAi(true);
                mob.setNoGravity(true);
                mob.setDeltaMovement(Vec3.ZERO);
                require(game.items().summons().owner(mob) == base.base.owner
                                && game.items().currentEpoch(mob), "native owner/epoch " + kind);
                // Positive query visibility prevents a cold entity section from masquerading as survival.
                require(base.level.getEntities((Entity)null, new AABB(at, at).inflate(2), e -> e == mob).contains(mob),
                        "published actor visible to the beacon's native spatial query " + kind);
                actors.add(mob);
            }
            return actors;
        }

        void checks() {
            var summons = game.items().summons();
            base.base.owner.setPos(base.at.add(25, 0, 0));
            var inside = group(center.add(2, 3, 0));
            var outside = group(center.add(20.01, 0, 0));
            var physical = new Cow(EntityTypes.COW, base.level);
            physical.setPos(center.add(4, 3, 0));
            physical.setNoAi(true);
            physical.setNoGravity(true);
            require(base.level.addFreshEntity(physical), "untagged biological control");

            zone(LbcMagic.MECHANISM_BEACON_BLOCK);
            game.items().magic().pulse();
            require(inside.stream().allMatch(Entity::isAlive) && outside.stream().allMatch(Entity::isAlive)
                            && physical.isAlive(), "TECH does not acquire biological summon classification");
            removeZone();

            zone(LbcMagic.MAGIC_BEACON_BLOCK);
            require(!game.items().magic().suppressed(base.base.owner), "caster stands outside MAG sphere");
            game.items().magic().pulse();
            require(inside.stream().noneMatch(Entity::isAlive),
                    "actual central MAG pulse kills Vex and all six cultists independently of caster");
            require(outside.stream().allMatch(Entity::isAlive) && physical.isAlive(),
                    "R20 boundary and untagged physical control survive MAG purge");
            summons.tick();
            require(inside.stream().allMatch(Entity::isRemoved) && summons.count() == KINDS.size(),
                    "normal summon lifecycle removes killed actors from goals and owner tracking");

            removeZone();
            // The surviving actors enter a newly placed field: released summons must not merely pause.
            outside.forEach(m -> m.setPos(center.add(3, 3, 0)));
            zone(LbcMagic.MAGIC_BEACON_BLOCK);
            game.items().magic().pulse();
            summons.tick();
            require(outside.stream().allMatch(Entity::isRemoved) && summons.count() == 0 && physical.isAlive(),
                    "later MAG placement permanently purges released actors without killing unrelated mobs");

            base.base.owner.setPos(base.at);
            for (var item : List.of(LbcWands.SUMMONER_VEXES, LbcWands.PSYCHOVEX)) {
                var stack = new ItemStack(item);
                base.base.owner.setItemInHand(InteractionHand.MAIN_HAND, stack);
                base.base.mana.set(base.base.owner, 100_000);
                require(!base.base.input(InteractionHand.MAIN_HAND) && summons.count() == 0
                                && base.base.mana.get(base.base.owner) == 100_000 && stack.getCount() == 1,
                        "existing actual MAG use gate preserves mana/item " + item);
            }
            removeZone();
            require(summons.count() == 0, "removing beacon does not resurrect purged summons");
        }

        @Override public void close() {
            if (closed) return;
            closed = true;
            try { removeZone(); }
            finally { base.close(); }
        }
    }

    public static void run(GameController game) {
        require(running == null, "exclusive fixture");
        running = new Fixture(game);
    }

    public static void tick(GameController game) {
        if (running == null) return;
        var fixture = running;
        try {
            require(fixture.game == game && game.playing(), "same active match");
            if (!fixture.ready()) return;
            fixture.checks();
            fixture.close();
            running = null;
            SkyWars2.LOGGER.info("[check] PASS suppression-summons (actual MAG/TECH blocks and central pulses; Vex/all six cultists; actor-position R20; physical control; native input; terminal lifecycle)");
        } catch (Throwable error) {
            try { fixture.close(); } catch (Throwable cleanup) { error.addSuppressed(cleanup); }
            finally { running = null; }
            SkyWars2.LOGGER.error("[check] FAIL suppression-summons", error);
        }
    }
}
