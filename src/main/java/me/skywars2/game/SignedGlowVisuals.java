package me.skywars2.game;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import me.skywars2.SkyWars2;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import java.util.*;

/** Per-viewer invisible native displays. No real entity, scoreboard or team is replaced. */
public final class SignedGlowVisuals {
    private static final int SLOTS = 8, MARKER = 0xD57500;
    private static final Map<Integer,LivingEntity> TARGETS = new HashMap<>();
    private static final Map<ServerGamePacketListenerImpl,Slot[]> VIEWERS = new IdentityHashMap<>();
    private static final class Slot {
        LivingEntity target; ElementHolder holder; ItemDisplayElement display;
    }
    private SignedGlowVisuals() { }
    public static void changed(LivingEntity target) {
        if (target.level().isClientSide()) return;
        if (SignedEffectVisuals.level(target, MobEffects.GLOWING) >= 2) TARGETS.put(target.getId(), target);
        else TARGETS.remove(target.getId());
    }
    public static void removed(LivingEntity target) { TARGETS.remove(target.getId()); }
    public static void forget(ServerPlayer player) {
        var slots = VIEWERS.remove(player.connection);
        if (slots != null) for (var slot : slots) release(slot);
    }
    static List<ItemDisplayElement> displays(ServerPlayer viewer) {
        var slots=VIEWERS.get(viewer.connection);
        if(slots==null)return List.of();
        var result=new ArrayList<ItemDisplayElement>();
        for(var slot:slots)if(slot.display!=null)result.add(slot.display);
        return List.copyOf(result);
    }
    public static int carrierCount() {
        int count=0;
        for(var slots:VIEWERS.values())for(var slot:slots)if(slot.holder!=null)count++;
        return count;
    }
    public static void stop() {
        for (var slots : VIEWERS.values()) for (var slot : slots) release(slot);
        VIEWERS.clear(); TARGETS.clear();
    }
    public static void tick(MinecraftServer server) {
        var game = SkyWars2.game();
        if (game == null) return;
        TARGETS.values().removeIf(t -> t.isRemoved() || !t.isAlive()
                || SignedEffectVisuals.level(t, MobEffects.GLOWING) < 2);
        var connected = Collections.newSetFromMap(new IdentityHashMap<ServerGamePacketListenerImpl,Boolean>());
        for (var player : server.getPlayerList().getPlayers()) connected.add(player.connection);
        VIEWERS.entrySet().removeIf(entry -> {
            if (connected.contains(entry.getKey())) return false;
            for (var slot : entry.getValue()) release(slot);
            return true;
        });
        for (var viewer : server.getPlayerList().getPlayers()) {
            var wanted = TARGETS.values().stream()
                    .filter(t -> t.level() == viewer.level() && (t == viewer || PlayerLookup.tracking(t).contains(viewer))
                            && !FullInvisibility.proxyFor(t.getId(),viewer))
                    .sorted(Comparator.comparingDouble(t -> t.distanceToSqr(viewer))).limit(SLOTS).toList();
            var slots = VIEWERS.get(viewer.connection);
            if (slots == null) {
                if (wanted.isEmpty()) continue;
                slots = new Slot[SLOTS]; Arrays.setAll(slots,i -> new Slot()); VIEWERS.put(viewer.connection,slots);
            }
            for (var slot : slots) if (slot.target != null && !wanted.contains(slot.target)) release(slot);
            for (var target : wanted) {
                boolean found = false;
                for (var slot : slots) if (slot.target == target) { found = true; break; }
                if (!found) for (int index=0; index<SLOTS; index++) if (slots[index].target == null) {
                    var slot=slots[index]; var item=new ItemStack(Items.PAPER);
                    item.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:black_hole_small"));
                    slot.target=target; slot.display=new ItemDisplayElement(item);
                    slot.display.setItemDisplayContext(ItemDisplayContext.NONE);
                    slot.display.setBillboardMode(Display.BillboardConstraints.FIXED);
                    slot.display.setGlowing(true); slot.display.setGlowColorOverride(MARKER+index);
                    slot.display.setDisplaySize(0,0); slot.display.setViewRange(64);
                    slot.display.setTeleportDuration(1); slot.display.setInterpolationDuration(1);
                    slot.holder=new ElementHolder(); slot.holder.addElement(slot.display);
                    game.visualTracking().attach(slot.holder,viewer.level(),
                            () -> slot.target.position().add(0,slot.target.getBbHeight()*0.5,0));
                    update(slot); slot.holder.startWatching(viewer); slot.holder.tick(); break;
                }
            }
            for (var slot : slots) if (slot.target != null) { update(slot); slot.holder.tick(); }
        }
    }
    private static void update(Slot slot) {
        // The helper's four reference points carry center, signed magnitude, actor height and RGB.
        int rgb=slot.target.getTeamColor(), color=15;
        for (var candidate : net.minecraft.world.scores.TeamColor.VALUES)
            if (candidate.rgb()==rgb) { color=candidate.ordinal(); break; }
        float magnitude=(float)(Math.log(SignedEffectVisuals.level(slot.target,MobEffects.GLOWING))/Math.log(2));
        slot.display.setScale(new Vector3f(magnitude,slot.target.getBbHeight(),color+1));
    }
    private static void release(Slot slot) {
        if (slot.holder != null) slot.holder.destroy();
        slot.target=null; slot.holder=null; slot.display=null;
    }
}
