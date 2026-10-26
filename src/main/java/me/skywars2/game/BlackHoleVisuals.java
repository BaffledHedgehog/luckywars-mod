package me.skywars2.game;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Shared black-hole renderer; gameplay and lifetime belong to the originating spell. */
public final class BlackHoleVisuals {
    private static final int SLOTS=16, COLOR=11618816, TAIL=4;
    private final GameController game;
    private final List<Handle> holes=new ArrayList<>();
    private final Map<ServerGamePacketListenerImpl,Slot[]> viewers=new IdentityHashMap<>();
    private int clock;
    private boolean dirty;

    public static final class Handle {
        ServerPlayer owner;
        final ServerLevel level;
        final String epoch;
        float radius;
        boolean disc;
        Vec3 point;
        final Quaternionf tilt=new Quaternionf();
        private Handle(ServerPlayer owner,Vec3 point,float radius,String epoch) {
            this.owner=owner; level=owner.level(); this.point=point;
            this.radius=radius; this.epoch=epoch;
        }
    }
    private static final class Slot {
        Handle hole;
        ElementHolder holder;
        ItemDisplayElement display;
        int availableAt;
    }
    BlackHoleVisuals(GameController game) { this.game=game; }
    public Handle create(ServerPlayer owner,Vec3 point,float radius) {
        return create(owner,point,radius,false);
    }
    public Handle create(ServerPlayer owner,Vec3 point,float radius,boolean disc) {
        if(!Float.isFinite(radius) || radius<=0 || !finite(point))
            throw new IllegalArgumentException("Black-hole position/radius must be finite");
        var hole=new Handle(owner,point,radius,game.items().epoch());
        hole.disc=disc;
        holes.add(hole); refresh(); return hole;
    }
    public void update(Handle hole,Vec3 point,float radius,boolean disc) {
        if(!finite(point)||!Float.isFinite(radius)||radius<=0) throw new IllegalArgumentException("Invalid black-hole geometry");
        boolean changed=hole.radius!=radius||hole.disc!=disc;
        hole.point=point;hole.radius=radius;hole.disc=disc;
        if(changed) for(var slots:viewers.values()) for(var slot:slots) if(slot.hole==hole) {
            slot.display.setItem(model(hole));slot.display.setScale(new Vector3f(radius));
        }
    }
    public void rebind(Handle hole,ServerPlayer owner) {
        if(owner.level()!=hole.level) throw new IllegalArgumentException("Black-hole owner changed dimension");
        hole.owner=owner;
    }
    public void tilt(Handle hole,Quaternionfc rotation) {
        if(!rotation.isFinite())throw new IllegalArgumentException("Black-hole rotation must be finite");
        hole.tilt.set(rotation);
        for(var slots:viewers.values())for(var slot:slots)if(slot.hole==hole)slot.display.setLeftRotation(new Quaternionf(hole.tilt));
    }
    private static ItemStack model(Handle hole) {
        var item=new ItemStack(Items.PAPER);
        item.set(DataComponents.ITEM_MODEL,Identifier.parse(hole.disc?"lbc:black_hole_large":"lbc:black_hole_small"));
        return item;
    }
    public void move(Handle hole,Vec3 point) {
        if(!finite(point)) throw new IllegalArgumentException("Black-hole position must be finite");
        hole.point=point;
    }
    private static boolean finite(Vec3 point) {
        return point!=null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }
    public void remove(Handle hole) {
        if(!holes.remove(hole)) return;
        // Release visible holders immediately; choose replacements once next tick.
        // A mass spell cleanup must not repeatedly sort every player's candidate set.
        for(var slots:viewers.values()) for(var slot:slots) if(slot.hole==hole) release(slot);
        dirty=true;
    }
    private boolean valid(Handle hole) {
        // Producers own gameplay lifetime and remove their handles explicitly. A UUID-based
        // producer can survive player replacement or pause before rebinding during Time Stop.
        // Retiring its still-live handle using the old ServerPlayer makes it invisible forever.
        return game.items().currentEpoch(hole.epoch);
    }
    public void tick() {
        if(!game.server().tickRateManager().runsNormally()) return;
        clock++;
        boolean removed=holes.removeIf(hole->!valid(hole));
        if(removed || dirty || holes.isEmpty() || clock%10==0) refresh();
        // Manual attachments follow the spell's current center without loading chunks.
        for(var slots:viewers.values()) for(var slot:slots) if(slot.hole!=null) slot.holder.tick();
    }
    private List<Handle> nearest(ServerPlayer viewer) {
        var result=new ArrayList<Handle>(SLOTS);
        for(var hole:holes) {
            if(hole.level!=viewer.level()) continue;
            double distance=viewer.position().distanceToSqr(hole.point); int at=0;
            while(at<result.size() && viewer.position().distanceToSqr(result.get(at).point)<=distance) at++;
            if(at<SLOTS) { result.add(at,hole); if(result.size()>SLOTS) result.removeLast(); }
        }
        return result;
    }
    private void refresh() {
        dirty=false;
        var players=game.server().getPlayerList().getPlayers(); var connected=new HashSet<>(players.stream().map(player->player.connection).toList());
        for(var iterator=viewers.entrySet().iterator();iterator.hasNext();) {
            var entry=iterator.next();
            if(!connected.contains(entry.getKey())) {
                for(var slot:entry.getValue()) release(slot);
                iterator.remove();
            }
        }
        for(var player:players) {
            var wanted=nearest(player); var slots=viewers.get(player.connection);
            if(slots==null) {
                if(wanted.isEmpty()) continue;
                slots=new Slot[SLOTS]; java.util.Arrays.setAll(slots,index->new Slot()); viewers.put(player.connection,slots);
            }
            for(var slot:slots) if(slot.hole!=null && !wanted.contains(slot.hole)) release(slot);
            for(var hole:wanted) {
                boolean present=false;
                for(var slot:slots) if(slot.hole==hole) { present=true; break; }
                if(present) continue;
                for(int index=0;index<SLOTS;index++) if(slots[index].hole==null && clock>=slots[index].availableAt) {
                    attach(player,slots[index],hole,index); break;
                }
            }
        }
        viewers.entrySet().removeIf(entry->{
            for(var slot:entry.getValue()) if(slot.hole!=null || clock<slot.availableAt) return false;
            return true;
        });
    }
    private void attach(ServerPlayer viewer,Slot slot,Handle hole,int index) {
        var display=new ItemDisplayElement(model(hole));
        display.setItemDisplayContext(ItemDisplayContext.NONE);
        display.setBillboardMode(Display.BillboardConstraints.FIXED);
        display.setGlowing(true); display.setGlowColorOverride(COLOR+index);
        display.setDisplaySize(0,0); display.setViewRange(64);
        display.setScale(new Vector3f(hole.radius));
        display.setLeftRotation(new Quaternionf(hole.tilt));
        display.setTeleportDuration(1); display.setInterpolationDuration(1);
        var holder=new ElementHolder(); holder.addElement(display);
        game.visualTracking().attach(holder,hole.level,()->hole.point);
        slot.hole=hole; slot.holder=holder; slot.display=display;
        holder.startWatching(viewer); holder.tick();
    }
    private void release(Slot slot) {
        if(slot.holder==null) return;
        slot.holder.destroy(); slot.holder=null; slot.display=null; slot.hole=null;
        // The resource-pack shader retains a 0.2-second collapse in this slot.
        slot.availableAt=clock+TAIL;
    }
    public void forget(ServerPlayer owner) { if(holes.removeIf(hole->hole.owner.getUUID().equals(owner.getUUID()))) refresh(); }
    public void stop() {
        holes.clear();
        for(var slots:viewers.values()) for(var slot:slots) release(slot);
    }
    int count() { return holes.size(); }
    List<ItemDisplayElement> displays(ServerPlayer viewer) {
        var result=new ArrayList<ItemDisplayElement>(); var slots=viewers.get(viewer.connection);
        if(slots!=null) for(var slot:slots) if(slot.display!=null) result.add(slot.display);
        return List.copyOf(result);
    }
    List<Vec3> origins(ServerPlayer viewer) {
        var result=new ArrayList<Vec3>(); var slots=viewers.get(viewer.connection);
        if(slots!=null) for(var slot:slots) if(slot.hole!=null) result.add(slot.hole.point);
        return List.copyOf(result);
    }
}
