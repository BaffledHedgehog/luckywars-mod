package me.skywars2.game;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3f;
import java.util.*;

/** Derived, recipient-only aiming telemetry; weapon state and RNG stay with their original owners. */
public final class WeaponSpreadVisuals {
    static final int MARKER=0xD57600, CONE=1, EULER=2;
    record Aim(float degrees,int mode,InteractionHand hand) { }
    private record View(ServerPlayer player,ServerLevel level,ElementHolder holder,ItemDisplayElement display) { }
    private static final Map<ServerGamePacketListenerImpl,View> VIEWERS=new IdentityHashMap<>();
    private WeaponSpreadVisuals() { }

    static Aim sample(GameController game,ServerPlayer player) {
        if(player.isRemoved()||!player.isAlive()||player.isSpectator()
                ||game.items().polymorph().masked(player)||game.items().fpvDrone().controlling(player)
                ||game.items().rifleZoom().scopeVisible(player))return null;
        Aim main=held(game,player,InteractionHand.MAIN_HAND);
        return main!=null?main:game.config().offhandWands?held(game,player,InteractionHand.OFF_HAND):null;
    }
    private static Aim held(GameController game,ServerPlayer player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(hand==InteractionHand.MAIN_HAND&&(LbcMagnum.isMagnum(stack)||LbcShooters.isSlr(stack)))
            return new Aim((float)LbcGunAccuracy.rifleDegrees(game,player),CONE,hand);
        if(LbcAk47.isAk47(stack))return new Aim((float)LbcGunAccuracy.ak47Degrees(player),CONE,hand);
        if(LbcLaserMinigun.isMinigun(stack))
            return new Aim((float)LbcGunAccuracy.minigunNextShotDegrees(game,player),EULER,hand);
        return null;
    }
    public static void synchronize(GameController game,ServerPlayer player) {
        if(player.connection==null)return;
        var aim=sample(game,player);
        if(aim==null){forget(player);return;}
        var view=VIEWERS.get(player.connection);
        if(view!=null&&(view.player!=player||view.level!=player.level())){
            forget(player);view=null;
        }
        if(view==null){
            var item=new ItemStack(Items.PAPER);
            // Reuse the invisible four-reference-point model; only the private outline marker is new.
            item.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:black_hole_small"));
            var display=new ItemDisplayElement(item);
            display.setItemDisplayContext(ItemDisplayContext.NONE);
            display.setBillboardMode(Display.BillboardConstraints.FIXED);
            display.setGlowing(true);display.setGlowColorOverride(MARKER);
            display.setDisplaySize(0,0);display.setViewRange(64);
            // Telemetry is current, not a second interpolation of an already changing accuracy value.
            display.setInterpolationDuration(0);display.setTeleportDuration(0);
            var holder=new ElementHolder();holder.addElement(display);
            game.visualTracking().attach(holder,player.level(),
                    ()->player.getEyePosition().add(player.getLookAngle().scale(.5)));
            view=new View(player,player.level(),holder,display);VIEWERS.put(player.connection,view);
            display.setScale(new Vector3f(aim.degrees,aim.mode,1));
            holder.startWatching(player);
        }else view.display.setScale(new Vector3f(aim.degrees,aim.mode,1));
        view.holder.tick();
    }
    public static void tick(MinecraftServer server) {
        var game=SkyWars2.game();if(game==null){stop();return;}
        var connected=Collections.newSetFromMap(new IdentityHashMap<ServerGamePacketListenerImpl,Boolean>());
        for(var player:server.getPlayerList().getPlayers())connected.add(player.connection);
        VIEWERS.entrySet().removeIf(entry->{if(connected.contains(entry.getKey()))return false;entry.getValue().holder.destroy();return true;});
        for(var player:server.getPlayerList().getPlayers())synchronize(game,player);
    }
    public static void forget(ServerPlayer player) {
        var old=VIEWERS.remove(player.connection);if(old!=null)old.holder.destroy();
    }
    public static void stop() {
        for(var view:VIEWERS.values())view.holder.destroy();VIEWERS.clear();
    }
    static List<ItemDisplayElement> displays(ServerPlayer player) {
        var view=VIEWERS.get(player.connection);return view==null?List.of():List.of(view.display);
    }
}
