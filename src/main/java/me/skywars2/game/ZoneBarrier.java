package me.skywars2.game;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ManualAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import com.mojang.math.Transformation;

import java.util.List;
import java.util.HashSet;

/** zone_border: один клиентский Polymer display, аналитический радиус и HP-пульс в секунду. */
public final class ZoneBarrier {
    private final GameController game;
    private ElementHolder holder;
    private ItemDisplayElement display;
    private Vec3 center;
    private double startRadius, endRadius;
    private int startedAt, durationTicks, nextPulse, lastVisualTick;

    ZoneBarrier(GameController game) { this.game = game; }

    /** Размер scale X/Z у модели lbc:zone_border совпадает с радиусом в блоках. */
    public void start(double x, double z, double fromRadius, double toRadius, int ticks) {
        if (!Double.isFinite(x) || !Double.isFinite(z) || !Double.isFinite(fromRadius)
                || !Double.isFinite(toRadius) || fromRadius <= 0 || toRadius <= 0 || ticks <= 0)
            throw new IllegalArgumentException("Zone center, radii and duration must be finite and positive");
        stop();
        center = new Vec3(x, -60, z);
        startRadius = fromRadius; endRadius = toRadius; durationTicks = ticks;
        startedAt = game.matchTicks(); nextPulse = startedAt + 20; lastVisualTick = startedAt;
        var stack = new ItemStack(Items.PAPER);
        stack.set(DataComponents.ITEM_MODEL, Identifier.parse("lbc:zone_border"));
        display = new ItemDisplayElement(stack);
        display.setItemDisplayContext(ItemDisplayContext.NONE);
        display.setBillboardMode(Display.BillboardConstraints.FIXED);
        display.setGlowing(true);
        display.setGlowColorOverride(16588098);
        display.setViewRange(64);
        display.setDisplaySize(0, 0);
        display.setTransformation(new Transformation(new Vector3f(0, 450, 0), new Quaternionf(),
                new Vector3f((float) fromRadius, 1100, (float) fromRadius), new Quaternionf()));
        display.setInterpolationDuration(1);
        holder = new ElementHolder();
        holder.addElement(display);
        // ManualAttachment не зависит от center chunk: барьер виден и далеко за островами.
        new ManualAttachment(holder, game.maps().level(), () -> center);
        refreshViewers();
        holder.tick();
    }

    public boolean active() { return holder != null; }
    public Vec3 center() { return center; }

    public double radius() { return active() ? radiusAt(game.matchTicks()) : 0; }

    double radiusAt(int tick) {
        double progress = Math.clamp((tick - (double) startedAt) / durationTicks, 0, 1);
        return startRadius + (endRadius - startRadius) * progress;
    }

    /** Столкновение с зоной двумерное: высота и границы структуры не ограничивают бой. */
    public double excessDistance(Vec3 position) {
        if (!active()) return 0;
        return Math.max(0, Math.hypot(position.x - center.x, position.z - center.z) - radius());
    }

    public void tick() {
        if (!active()) return;
        int now = game.matchTicks();
        if (lastVisualTick != now && lastVisualTick < startedAt + durationTicks) {
            double radius = radius();
            display.setScale(new Vector3f((float) radius, 1100, (float) radius));
            display.startInterpolation();
            lastVisualTick = now;
            holder.tick();
        }
        if (now < nextPulse) return;
        nextPulse = now + 20;
        refreshViewers();
        double radius = radius();
        for (ServerPlayer player : game.alivePlayers()) {
            if (!game.allowDamage(player) || player.level() != game.maps().level() || !player.isAlive()) continue;
            double dx = player.getX() - center.x, dz = player.getZ() - center.z;
            // sqrt нужен только для находящихся снаружи игроков, один раз на секундный пульс.
            if (dx * dx + dz * dz <= radius * radius) continue;
            LbcDamage.removeHealth(player, (float) (Math.hypot(dx, dz) - radius), player.damageSources().outOfBorder());
        }
    }

    public void playerJoined(ServerPlayer player) {
        if (active() && player.level() == game.maps().level()) holder.startWatching(player);
    }

    private void refreshViewers() {
        var connected = new HashSet<>(game.server().getPlayerList().getPlayers());
        for (var connection : List.copyOf(holder.getWatchingPlayers())) {
            var player = connection.player;
            if (!connected.contains(player) || player.level() != game.maps().level()) holder.stopWatching(connection);
        }
        for (var player : connected) playerJoined(player);
    }

    public void stop() {
        if (holder != null) holder.destroy();
        holder = null; display = null; center = null;
    }

    // Проверки читают те же metadata, которые Polymer отправляет настоящему клиенту.
    ItemDisplayElement display() { return display; }
    int viewerCount() { return holder == null ? 0 : holder.getWatchingPlayers().size(); }
}
