package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Wall-clock credit for vanilla client actions; never changes synchronized attributes. */
public final class TpsCompensation {
    private TpsCompensation() { }
    private static final double TICK_NANOS = 50_000_000d;
    private static final long MAX_INTERVAL = 250_000_000L;
    private static MinecraftServer server;
    private static int generation;
    private static boolean running;

    public static void beginTick(MinecraftServer current) {
        boolean enabled = !current.tickRateManager().isFrozen()
                && current.tickRateManager().tickrate() == 20;
        if (server != current || enabled != running) generation++;
        server = current;
        running = enabled;
        // Player.doTick can be cancelled by Time Stop before Player.tick is reached.
        for (var player : current.getPlayerList().getPlayers()) {
            ((Tracked)player).skywars2$compensationTick();
            ((Tracked)player.gameMode).skywars2$compensationTick();
        }
    }

    static double interval(long nanos) {
        return Math.max(0, Math.min(MAX_INTERVAL, nanos)) / TICK_NANOS;
    }

    public interface Tracked { void skywars2$compensationTick(); }
    public interface PlayerClock extends Tracked { int skywars2$compensationEpoch(); }

    public static boolean enabled(ServerPlayer player) {
        if (server == null || player.level().getServer() != server || !running
                || server.tickRateManager().isFrozen() || server.tickRateManager().tickrate() != 20
                || !player.isAlive() || player.isRemoved() || player.isSpectator()) return false;
        var game = SkyWars2.game();
        return game == null || !game.items().timeStop().stopped(player.level());
    }

    /** max(ticks, elapsed time), not their sum: catch-up ticks cannot count lag twice. */
    public static final class Clock {
        private long previous;
        private double wallTicks;
        private double baseline;
        private int epoch = -1;
        private int playerEpoch = -1;

        public void reset(double nativeTicks) {
            previous = System.nanoTime();
            wallTicks = 0;
            baseline = nativeTicks;
            epoch = generation;
        }

        public double elapsed(ServerPlayer player, double nativeTicks) {
            int currentEpoch = ((PlayerClock)player).skywars2$compensationEpoch();
            if (epoch != generation || playerEpoch != currentEpoch || !enabled(player)) reset(nativeTicks);
            playerEpoch = currentEpoch;
            long now = System.nanoTime();
            wallTicks += interval(now - previous);
            previous = now;
            return enabled(player) ? Math.max(nativeTicks, baseline + wallTicks) : nativeTicks;
        }

        public void copyFrom(Clock other) {
            previous = other.previous;
            wallTicks = other.wallTicks;
            baseline = other.baseline;
            epoch = other.epoch;
            playerEpoch = other.playerEpoch;
        }
    }
}
