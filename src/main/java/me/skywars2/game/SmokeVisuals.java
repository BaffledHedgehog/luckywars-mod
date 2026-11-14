package me.skywars2.game;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Derived per-viewer shader carriers; the grenade retains all gameplay/lifetime state. */
final class SmokeVisuals {
    static final int SLOTS = 16, MARKER = 0xD57700;
    interface Volume { ServerLevel level(); Vec3 center(); double radius(); double density(); }
    private static final class Slot {
        final Volume volume; final ElementHolder holder; final ItemDisplayElement display;
        Vec3 sentCenter; double sentRadius, sentDensity;
        Slot(Volume volume, ElementHolder holder, ItemDisplayElement display) {
            this.volume=volume;this.holder=holder;this.display=display;
            sentCenter=volume.center();sentRadius=volume.radius();sentDensity=volume.density();
        }
        ItemDisplayElement display(){return display;}
    }
    private static final class View {
        final ServerPlayer player;
        final ServerLevel level;
        final Slot[] slots = new Slot[SLOTS];
        View(ServerPlayer p) { player = p; level = p.level(); }
    }
    private final GameController game;
    private final Map<ServerGamePacketListenerImpl, View> views = new IdentityHashMap<>();
    SmokeVisuals(GameController game) { this.game = game; }

    void synchronize(List<? extends Volume> volumes) {
        if (volumes.isEmpty()) { stop(); return; }
        var players = game.server().getPlayerList().getPlayers();
        var connected = Collections.newSetFromMap(new IdentityHashMap<ServerGamePacketListenerImpl, Boolean>());
        for (var player : players) connected.add(player.connection);
        views.entrySet().removeIf(entry -> {
            if (connected.contains(entry.getKey())) return false;
            release(entry.getValue()); return true;
        });
        for (var player : players) {
            if (player.connection == null) continue;
            Vec3 camera = LbcFpvDrone.viewpoint(player);
            var wanted = volumes.stream().filter(v -> v.level() == player.level()
                    && camera.distanceToSqr(v.center()) <= Math.pow(512 + v.radius(), 2))
                .sorted(Comparator.comparingDouble(v -> Math.max(0, camera.distanceTo(v.center()) - v.radius())))
                .limit(SLOTS).toList();
            var view = views.get(player.connection);
            if (view != null && (view.player != player || view.level != player.level())) {
                release(view); views.remove(player.connection); view = null;
            }
            if (view == null) {
                if (wanted.isEmpty()) continue;
                view = new View(player); views.put(player.connection, view);
            }
            for (int i = 0; i < SLOTS; i++) {
                var slot = view.slots[i];
                if (slot != null && !wanted.contains(slot.volume)) {
                    slot.holder.destroy(); view.slots[i] = null;
                }
            }
            for (var volume : wanted) {
                boolean present = false;
                for (var slot : view.slots) if (slot != null && slot.volume == volume) { present = true; break; }
                if (present) continue;
                for (int i = 0; i < SLOTS; i++) if (view.slots[i] == null) {
                    view.slots[i] = attach(player, volume, i); break;
                }
            }
            for(var slot:view.slots)if(slot!=null)refresh(slot);
            if (wanted.isEmpty()) views.remove(player.connection);
        }
    }

    private Slot attach(ServerPlayer viewer, Volume volume, int index) {
        var item = new ItemStack(Items.PAPER);
        // Existing invisible four-reference-point mesh. Outline color selects the new rows.
        item.set(DataComponents.ITEM_MODEL, Identifier.parse("lbc:black_hole_small"));
        var display = new ItemDisplayElement(item);
        display.setItemDisplayContext(ItemDisplayContext.NONE);
        display.setBillboardMode(Display.BillboardConstraints.FIXED);
        display.setGlowing(true); display.setGlowColorOverride(MARKER + index);
        display.setDisplaySize(0, 0); display.setViewRange(64);
        display.setScale(new Vector3f((float)volume.radius(),(float)(volume.radius()*volume.density()),(float)volume.radius()));
        display.setInterpolationDuration(2); display.setTeleportDuration(2);
        var holder = new ElementHolder(); holder.addElement(display);
        game.visualTracking().attach(holder, volume.level(), volume::center);
        holder.startWatching(viewer); holder.tick();
        // Settled geometry and client-side camera matrices need no periodic packets.
        return new Slot(volume, holder, display);
    }

    private void refresh(Slot slot){
        var volume=slot.volume;
        boolean moved=slot.sentCenter.distanceToSqr(volume.center())>1e-8;
        boolean resized=game.server().getTickCount()%2==0 &&
            (Math.abs(slot.sentRadius-volume.radius())>.0001||Math.abs(slot.sentDensity-volume.density())>.0001);
        if(resized){
            slot.display.setScale(new Vector3f((float)volume.radius(),(float)(volume.radius()*volume.density()),(float)volume.radius()));
            slot.display.startInterpolation();slot.sentRadius=volume.radius();slot.sentDensity=volume.density();
        }
        if(moved||resized){slot.holder.tick();slot.sentCenter=volume.center();}
    }

    private static void release(View view) {
        for (var slot : view.slots) if (slot != null) slot.holder.destroy();
        Arrays.fill(view.slots, null);
    }
    void stop() { for (var view : views.values()) release(view); views.clear(); }
    List<ItemDisplayElement> displays(ServerPlayer player) {
        var view = views.get(player.connection);
        if (view == null) return List.of();
        return Arrays.stream(view.slots).filter(Objects::nonNull).map(Slot::display).toList();
    }
}
