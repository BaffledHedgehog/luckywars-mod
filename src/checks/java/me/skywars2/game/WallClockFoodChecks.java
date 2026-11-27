package me.skywars2.game;

import me.skywars2.SkyWars2;
import me.skywars2.game.items.food.FoodUseClock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;

/** Actual 5TPS consumption and native release packets, including queued receipt times. */
public final class WallClockFoodChecks {
    private static Scene running;
    private static void require(boolean ok, String why) { if (!ok) throw new IllegalStateException("Wall-clock food: " + why); }
    private static final class Scene implements AutoCloseable {
        final GameController game;
        final ServerPlayer owner;
        final RegenerationBeltChecks.Snapshot saved;
        final RegenerationBeltChecks.ScheduledClient wire;
        final float tickrate;
        final boolean skills, vanillaSkills;
        final boolean frozen;
        final long began = System.nanoTime();
        long started;
        int phase, used, sequence;
        ServerboundUseItemPacket queued;
        me.skywars2.game.match.checkpoints.MatchPlayerCheckpoint checkpoint;
        boolean timeStopped;
        boolean closed;
        Scene(GameController game) {
            this.game = game;
            owner = game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            require(owner != null && game.allowDamage(owner) && !owner.isUsingItem(), "idle native participant");
            saved = new RegenerationBeltChecks.Snapshot(game, owner);
            wire = new RegenerationBeltChecks.ScheduledClient(game, owner);
            tickrate = game.server().tickRateManager().tickrate();
            frozen = game.server().tickRateManager().isFrozen();
            skills = game.config().lbcSkills; vanillaSkills = game.config().vanillaSkills;
        }
        int uses() { return owner.getStats().getValue(Stats.ITEM_USED.get(Items.BREAD)); }
        void food(float seconds) {
            owner.stopUsingItem(); owner.getFoodData().setFoodLevel(0);
            owner.getInventory().setSelectedSlot(5);
            var stack = new ItemStack(Items.BREAD, 3);
            stack.set(DataComponents.CONSUMABLE, Consumable.builder().consumeSeconds(seconds).build());
            owner.setItemInHand(InteractionHand.MAIN_HAND, stack);
            used = uses(); started = System.nanoTime();
        }
        void start() {
            owner.connection.handleUseItem(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, ++sequence, owner.getYRot(), owner.getXRot()));
            require(owner.isUsingItem(), "native use packet starts food");
        }
        void release() {
            owner.connection.handlePlayerAction(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
                    BlockPos.ZERO, Direction.DOWN));
        }
        void initialize() {
            require(!frozen && !game.items().timeStop().stopped(owner.level()), "unpaused native fixture");
            game.config().lbcSkills = false; game.config().vanillaSkills = false;
            game.server().tickRateManager().setTickRate(5);
            food(.2f); start();
        }
        boolean step() {
            wire.acknowledge();
            long now = System.nanoTime();
            require(now - began < 12_000_000_000L, "bounded real-time fixture");
            if (phase == 0) {
                if (uses() == used) { require(now - started < 800_000_000L, "four-tick food cannot stall for native4ticks at5TPS"); return false; }
                require(uses() == used + 1 && owner.getMainHandItem().getCount() == 2 && !owner.isUsingItem(), "one short native consume at5TPS");
                require(wire.packets().stream().filter(ClientboundEntityEventPacket.class::isInstance)
                        .map(ClientboundEntityEventPacket.class::cast).anyMatch(packet -> packet.getEventId() == 9), "native completion notification reaches client");
                release(); release();
                require(uses() == used + 1 && owner.getMainHandItem().getCount() == 2, "late duplicate client releases do not consume again");
                food(1.6f); start(); release();
                require(uses() == used && owner.getMainHandItem().getCount() == 3 && !owner.isUsingItem(), "genuine early release cancels native food");
                food(.2f);
                queued = new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, ++sequence, owner.getYRot(), owner.getXRot());
                ((FoodUseClock.Arrival)(Object)queued).skywars2$recordFoodArrival();
                started = now; phase = 1; return false;
            }
            if (phase == 1) {
                if (now - started < 210_000_000L) return false;
                owner.connection.handleUseItem(queued);
                require(owner.isUsingItem(), "queued packet preserves active native use before first item tick");
                release();
                require(!owner.isUsingItem() && uses() == used + 1 && owner.getMainHandItem().getCount() == 2,
                        "client release completes elapsed food before its first native item update; queue delay counts");
                release();
                require(uses() == used + 1 && owner.getMainHandItem().getCount() == 2, "completed queued use has one native debit");
                food(1.6f); start(); phase = 2; return false;
            }
            if (phase == 3) {
                require(uses() == used && owner.getMainHandItem().getCount() == 3 && owner.isUsingItem(),
                        "actual Time Stop holds the unfinished food beyond its wall deadline");
                if (now - started < 650_000_000L) return false;
                game.items().timeStop().end(owner.level()); timeStopped = false;
                started = System.nanoTime(); phase = 4; return false;
            }
            if (phase == 4) {
                if (uses() == used) { require(now - started < 1_000_000_000L, "food resumes after thaw"); return false; }
                require(now - started >= 300_000_000L && uses() == used + 1 && owner.getMainHandItem().getCount() == 2,
                        "frozen elapsed time is excluded instead of completing immediately on thaw");
                food(.8f); start();
                checkpoint = new me.skywars2.game.match.checkpoints.MatchPlayerCheckpoint(game, owner);
                owner.stopUsingItem(); started = System.nanoTime(); phase = 5; return false;
            }
            if (phase == 5) {
                if (now - started < 1_000_000_000L) return false;
                game.server().tickRateManager().setFrozen(true);
                try { require(checkpoint.restoreFrozen() == owner && owner.isUsingItem(), "native checkpoint restores active food use"); }
                finally { game.server().tickRateManager().setFrozen(frozen); }
                wire.acknowledge(); started = System.nanoTime(); phase = 6; return false;
            }
            if (phase == 6) {
                if (uses() == used) { require(now - started < 1_600_000_000L, "restored food completes normally"); return false; }
                require(now - started >= 650_000_000L && uses() == used + 1 && owner.getMainHandItem().getCount() == 2,
                        "rewound food clock rebases to restored native remaining time, excluding abandoned future time");
                return true;
            }
            if (uses() == used) {
                require(now - started < 2_600_000_000L, "ordinary32tick food completes by walltime instead of6.4seconds at5TPS");
                return false;
            }
            require(now - started >= 1_500_000_000L && uses() == used + 1 && owner.getMainHandItem().getCount() == 2,
                    "ordinary food respects authored duration and one complete native consume");
            food(.4f); start();
            game.items().timeStop().begin(owner.level(), 100); timeStopped = true;
            require(game.items().timeStop().frozen(owner), "actual native player is Time Stop frozen");
            started = System.nanoTime(); phase = 3; return false;
        }
        public void close() {
            if (closed) return; closed = true;
            try {
                if (timeStopped) game.items().timeStop().end(owner.level());
                game.server().tickRateManager().setFrozen(frozen);
                game.server().tickRateManager().setTickRate(tickrate);
                game.config().lbcSkills = skills; game.config().vanillaSkills = vanillaSkills;
                owner.stopUsingItem(); saved.restore(game);
            } finally { wire.close(); }
        }
    }
    public static void run(GameController game) {
        require(running == null, "exclusive scene"); running = new Scene(game);
        try { running.initialize(); } catch (Throwable error) { running.close(); running = null; throw error; }
        SkyWars2.LOGGER.info("[check] START wall-clock-food");
    }
    public static void tick(GameController game) {
        if (running == null) return;
        try {
            if (!running.step()) return;
            running.close(); running = null;
            SkyWars2.LOGGER.info("[check] PASS wall-clock-food (actual5TPS, short+ordinary durations, native completion notification, queued receipt, early/duplicate release, real Time Stop pause and native checkpoint rebase)");
        } catch (Throwable error) {
            try { running.close(); } catch (Throwable cleanup) { error.addSuppressed(cleanup); }
            running = null; SkyWars2.LOGGER.error("[check] FAIL wall-clock-food", error);
        }
    }
}
