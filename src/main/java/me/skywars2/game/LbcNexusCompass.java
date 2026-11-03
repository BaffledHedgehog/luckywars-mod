package me.skywars2.game;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import me.skywars2.SkyWars2;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Saved Nexus island destinations; scoreboard time remains authoritative across player rebind/rewind. */
public final class LbcNexusCompass {
    private static final Path OVERRIDE = Path.of("config", "luckywars", "nexus_coords.json");
    private static final String COOLDOWN = "nexus_compass_cd";
    private static final String PHASE = "nexus_compass_phase";
    private final GameController game;
    private final Predicate<ServerPlayer> suppressed;
    // Derived lookup only: neither remaining time nor inventory contents live in this index.
    private final Set<UUID> active = new HashSet<>();
    private List<Vec3> bundled = List.of(), destinations = List.of();
    private int lastTick = Integer.MIN_VALUE;
    private final java.util.concurrent.atomic.AtomicReference<List<Vec3>> pendingWrite=new java.util.concurrent.atomic.AtomicReference<>();
    private final java.util.concurrent.atomic.AtomicBoolean writing=new java.util.concurrent.atomic.AtomicBoolean();
    /** Called once after a complete native chunk move, never during a cell write. */
    void appendedIsland(int x,int y,int z){
        var updated=new ArrayList<>(destinations);updated.add(new Vec3(x+5,y+321,z+5));
        destinations=List.copyOf(updated);persistDestinations();
    }
    List<Vec3> checkpointDestinations(){return destinations;}
    void restoreDestinations(List<Vec3> saved){destinations=List.copyOf(saved);persistDestinations();}
    private void persistDestinations(){pendingWrite.set(destinations);startWriter();}
    private void startWriter(){
        if(!writing.compareAndSet(false,true))return;
        java.util.concurrent.CompletableFuture.runAsync(()->{
            try{
                for(var next=pendingWrite.getAndSet(null);next!=null;next=pendingWrite.getAndSet(null)){
                    var json=new com.google.gson.JsonArray();for(var point:next){var row=new com.google.gson.JsonObject();row.addProperty("x",point.x);row.addProperty("y",point.y);row.addProperty("z",point.z);json.add(row);}
                    Files.createDirectories(OVERRIDE.getParent());var temporary=OVERRIDE.resolveSibling("nexus_coords.json.writing");
                    Files.writeString(temporary,json.toString(),StandardCharsets.UTF_8);
                    try{Files.move(temporary,OVERRIDE,java.nio.file.StandardCopyOption.ATOMIC_MOVE,java.nio.file.StandardCopyOption.REPLACE_EXISTING);}
                    catch(java.nio.file.AtomicMoveNotSupportedException ignored){Files.move(temporary,OVERRIDE,java.nio.file.StandardCopyOption.REPLACE_EXISTING);}
                }
            }catch(IOException failure){SkyWars2.LOGGER.error("Cannot persist completed Nexus island coordinates",failure);}
            finally{writing.set(false);if(pendingWrite.get()!=null)startWriter();}
        });
    }

    public LbcNexusCompass(GameController game, Predicate<ServerPlayer> suppressed) {
        this.game = game;
        this.suppressed = suppressed;
    }

    /** Startup/reload only. Never writes an override or reads files during use/tick. */
    public void reload() {
        if (bundled.isEmpty()) {
            try (var input = LbcNexusCompass.class.getResourceAsStream("/skywars2/nexus_coords.json")) {
                if (input == null) throw new IOException("Missing bundled Nexus coordinates");
                bundled = read(new InputStreamReader(input, StandardCharsets.UTF_8));
            } catch (IOException | RuntimeException error) {
                SkyWars2.LOGGER.error("Cannot load bundled Nexus compass coordinates", error);
            }
        }
        if (Files.exists(OVERRIDE)) {
            try (var reader = Files.newBufferedReader(OVERRIDE, StandardCharsets.UTF_8)) {
                destinations = read(reader);
            } catch (IOException | RuntimeException error) {
                // Keep the last valid table when a live author override is incomplete or malformed.
                if (destinations.isEmpty()) destinations = bundled;
                SkyWars2.LOGGER.warn("Cannot reload {}; keeping valid Nexus compass coordinates", OVERRIDE, error);
            }
        } else destinations = bundled;
        publishFrozen();
    }

    private static List<Vec3> read(Reader reader) throws IOException {
        var document = JsonParser.parseReader(reader);
        if (!document.isJsonArray()) throw new IOException("Nexus coordinates must be an array");
        var points = new ArrayList<Vec3>();
        for (var value : document.getAsJsonArray()) {
            if (!value.isJsonObject()) throw new IOException("Nexus coordinate must be an object");
            var row = value.getAsJsonObject();
            double[] xyz = new double[3];
            String[] axes = {"x", "y", "z"};
            for (int i = 0; i < axes.length; i++) {
                var coordinate = row.get(axes[i]);
                if (coordinate == null || !coordinate.isJsonPrimitive() || !coordinate.getAsJsonPrimitive().isNumber())
                    throw new IOException("Missing numeric Nexus coordinate " + axes[i]);
                xyz[i] = coordinate.getAsDouble();
                if (!Double.isFinite(xyz[i])) throw new IOException("Nonfinite Nexus coordinate");
            }
            var point = new Vec3(xyz[0], xyz[1], xyz[2]);
            if (!Level.isInSpawnableBounds(BlockPos.containing(point))) throw new IOException("Nexus coordinate outside native bounds");
            points.add(point);
        }
        if (points.isEmpty()) throw new IOException("Nexus coordinate array is empty");
        return List.copyOf(points);
    }

    /** Called only by right-click dispatch. A matched mainhand item consumes the interaction. */
    public boolean use(ServerPlayer player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !LbcNexusToolItems.isCompass(player.getItemInHand(hand))) return false;
        inventoryChanged(player);
        if (!game.canUseGameplayItems(player) || !player.isAlive() || player.isSpectator()
                || player.level().dimension().equals(LbcHedgehogDimensions.IMPRINTED)
                || game.items().wands().inputTimeStopped(player) || paused(player)
                || suppressed.test(player) || cooldownSeconds(player) > 0 || destinations.isEmpty()) return false;
        var nexus = game.server().getLevel(LbcHedgehogDimensions.NEXUS);
        if (nexus == null || game.items().timeStop().stopped(nexus)) return false;
        var point = destinations.get(player.getRandom().nextInt(destinations.size()));
        // Native position teleport retains the exact authored XYZ and independent camera/momentum.
        // This is voluntary self travel: no indirect damage ownership is added.
        if (player.teleportTo(nexus, point.x, point.y, point.z, TeleportMomentum.positionFlags(Set.of()),
                player.getYRot(), player.getXRot(), true)) {
            game.items().mana().score(player, COOLDOWN).set(30);
            game.items().mana().score(player, PHASE).set(0);
            active.add(player.getUUID());
            return true;
        }
        return false;
    }

    private boolean paused(ServerPlayer player) {
        return !game.server().tickRateManager().runsNormally() || game.items().timeStop().stopped(player.level())
                || game.items().timeStop().frozen(player) || player.entityTags().contains("time_stopped");
    }

    public int cooldownSeconds(ServerPlayer player) {
        return Math.max(0, game.items().mana().hudValue(player, COOLDOWN));
    }

    public boolean isHolding(ServerPlayer player) {
        return LbcNexusToolItems.isCompass(player.getMainHandItem()) || LbcNexusToolItems.isCompass(player.getOffhandItem());
    }

    public MutableComponent hud(ServerPlayer player) {
        if (!isHolding(player)) return Component.empty();
        int seconds = cooldownSeconds(player);
        return seconds > 0 ? Component.literal("    \uE105 ").append(Component.literal(Integer.toString(seconds)).withStyle(ChatFormatting.WHITE))
                : Component.literal("    \uE104 ").append(Component.translatable("ready").withStyle(ChatFormatting.GREEN));
    }

    /** Inventory/load notifications rediscover a restored score without creating one. */
    public void inventoryChanged(ServerPlayer player) {
        if (cooldownSeconds(player) > 0) active.add(player.getUUID()); else active.remove(player.getUUID());
    }

    public void tick() {
        int tick = game.server().getTickCount();
        if (tick == lastTick) return;
        lastTick = tick;
        var mana = game.items().mana();
        for (var iterator = active.iterator(); iterator.hasNext();) {
            var player = game.server().getPlayerList().getPlayer(iterator.next());
            if (player == null) { iterator.remove(); continue; }
            int seconds = cooldownSeconds(player);
            if (seconds <= 0) { iterator.remove(); continue; }
            if (paused(player)) continue;
            int phase = Math.max(0, Math.min(19, mana.hudValue(player, PHASE))) + 1;
            if (phase == 20) {
                mana.score(player, COOLDOWN).set(--seconds);
                phase = 0;
                if (seconds == 0) iterator.remove();
            }
            mana.score(player, PHASE).set(phase);
        }
    }

    /** Rebuild after publishing restored scoreboard state; no private timer checkpoint is needed. */
    public void publishFrozen() {
        active.clear();
        lastTick = Integer.MIN_VALUE;
        for (var player : game.server().getPlayerList().getPlayers()) inventoryChanged(player);
    }

    public void forget(ServerPlayer player) { active.remove(player.getUUID()); }

    /** Match boundary cleanup includes disconnected score holders, without creating new scores. */
    public void stop() {
        active.clear();
        lastTick = Integer.MIN_VALUE;
        var board = game.server().getScoreboard();
        for (String name : List.of(COOLDOWN, PHASE)) {
            var objective = board.getObjective(name);
            if (objective == null) continue;
            for (var holder : List.copyOf(board.getTrackedPlayers()))
                if (board.getPlayerScoreInfo(holder, objective) != null) board.getOrCreatePlayerScore(holder, objective).set(0);
        }
    }
}
