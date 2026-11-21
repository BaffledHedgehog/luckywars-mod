package me.skywars2.game;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import me.skywars2.SkyWars2;
import me.skywars2.game.items.wands.LbcBlaze;
import me.skywars2.game.items.wands.LbcFartus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;

final class HomingColdChecks {
    private static final Vec3 ORIGIN = new Vec3(224.5, 260, 224.5);
    private static final int[][] OFFSETS = {{0, 0}, {50, 0}, {0, 50}, {-50, 0}, {0, -50}};
    private static HomingColdChecks running;

    private final ElementalGrimoireChecks.Fixture fixture;
    private final Map<LargeFireball, Vec3> shots = new IdentityHashMap<>();
    private int startTick;

    private HomingColdChecks(GameController game) {
        fixture = new ElementalGrimoireChecks.Fixture(game);
    }

    private void cast() {
        fixture.initialize();
        var game = fixture.game;
        var owner = fixture.owner;
        var level = owner.level();
        level.getChunk(BlockPos.containing(ORIGIN));
        owner.teleportTo(level, ORIGIN.x, ORIGIN.y, ORIGIN.z, Set.of(), 0, 90, true);

        int cold = 0;
        for (int[] offset : OFFSETS) {
            if (!level.hasChunkAt(BlockPos.containing(ORIGIN.add(offset[0], 0, offset[1])))) cold++;
        }
        require(cold > 0, "at least one formation column must be cold before the cast");

        var wand = game.lbc().rollTable(level, owner.position(), owner, "lbc:ported/exahoming_wand").getFirst();
        var flags = wand.get(DataComponents.CUSTOM_DATA).copyTag();
        flags.putInt("upgradable", 1);
        wand.set(DataComponents.CUSTOM_DATA, CustomData.of(flags));
        owner.setItemInHand(InteractionHand.MAIN_HAND, wand);
        owner.setShiftKeyDown(true);
        int cost = fixture.mana.score(ScoreHolder.forNameOnly("exahoming_wand_mana_use"), "lbc.math").get();
        fixture.mana.set(owner, cost);
        require(fixture.input(InteractionHand.MAIN_HAND) && fixture.mana.get(owner) == 0
                && LbcFartus.data(wand).getIntOr("upgrade_available", 0) == 1, "one paid cast marks the trial ready");

        for (var shot : game.items().homing().snapshot()) {
            if (shot.entityTags().contains(LbcBlaze.TRIAL_PROJECTILE)) {
                require(shot.getOwner() == owner && game.items().currentEpoch(shot), "native attribution");
                shots.put(shot, shot.position());
            }
        }
        require(shots.size() == 5, "expected five native trial shots, got " + shots.size() + " with " + cold + " cold columns");
        startTick = game.server().getTickCount();
    }

    private boolean advanced() {
        require(fixture.game.server().getTickCount() - startTick <= 160, "cold trial bodies must begin native flight");
        var level = fixture.owner.level();
        for (var entry : shots.entrySet()) {
            var shot = entry.getKey();
            require(!shot.isRemoved(), "trial survives admission");
            if (level.getEntity(shot.getUUID()) != shot || shot.tickCount < 5 || shot.getY() >= entry.getValue().y) return false;
        }
        return true;
    }

    private void close() {
        try {
            fixture.game.items().homing().stop();
            fixture.game.items().blaze().tickTrialFlights();
        } finally {
            fixture.close();
        }
    }

    static void run(GameController game) {
        require(running == null, "exclusive fixture");
        var scene = new HomingColdChecks(game);
        try {
            scene.cast();
            running = scene;
        } catch (RuntimeException | Error failure) {
            scene.close();
            throw failure;
        }
    }

    static void tick() {
        var scene = running;
        if (scene == null) return;
        try {
            if (!scene.advanced()) return;
            running = null;
            scene.close();
            SkyWars2.LOGGER.info("[check] PASS exahoming-cold");
        } catch (RuntimeException | Error failure) {
            running = null;
            scene.close();
            SkyWars2.LOGGER.error("[check] FAIL exahoming-cold", failure);
        }
    }

    private static void require(boolean ok, String detail) {
        if (!ok) throw new IllegalStateException("Exahoming cold: " + detail);
    }
}
