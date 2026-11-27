package me.skywars2.game.items.food;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Wall time supplies food progress; every actual consume still belongs to native completeUsingItem. */
public final class FoodUseClock {
    public static final long TICK_NANOS = 50_000_000L;
    private static final ThreadLocal<Long> INPUT = new ThreadLocal<>();
    private FoodUseClock() {}

    public interface Arrival {
        long skywars2$foodArrival();
        void skywars2$recordFoodArrival();
    }
    public interface Control {
        void skywars2$pauseFood(long now);
        void skywars2$resumeFood(long now);
        void skywars2$rebaseFood(long now);
    }

    public static boolean eligible(ServerPlayer player, ItemStack item) {
        return player.isAlive() && !player.isSpectator() && item.has(DataComponents.FOOD)
                && item.has(DataComponents.CONSUMABLE) && !item.useOnRelease() && item.getUseDuration(player) > 0;
    }
    public static long now() { var received = INPUT.get(); return received == null ? System.nanoTime() : received; }
    public static Long enter(long received) { var previous = INPUT.get(); INPUT.set(received); return previous; }
    public static void leave(Long previous) { if (previous == null) INPUT.remove(); else INPUT.set(previous); }
    public static int remaining(int ticks, long started, long now) {
        long elapsed = Math.max(0, now - started);
        return (int)Math.max(0L, ticks - elapsed / TICK_NANOS);
    }
}
