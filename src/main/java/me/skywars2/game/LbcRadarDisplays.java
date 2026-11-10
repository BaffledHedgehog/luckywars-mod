package me.skywars2.game;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** One private mark per viewer/target, shared by active radar producers in the current frame. */
public final class LbcRadarDisplays {
    private record Key(ServerPlayer viewer,LivingEntity target){}
    private static final class Mark {
        final ElementHolder holder=new ElementHolder();final ItemDisplayElement display;Vec3 at;long frame;
        Mark(ItemDisplayElement display){this.display=display;holder.addElement(display);}
    }
    private final GameController game;
    private final Map<Key,Mark> marks=new HashMap<>();
    private long frame;
    LbcRadarDisplays(GameController game){this.game=game;}
    void begin(){frame++;}
    void mark(ServerPlayer viewer,LivingEntity target){
        if(viewer==target||viewer.isRemoved()||target.isRemoved()||!target.isAlive()||viewer.level()!=target.level())return;
        var key=new Key(viewer,target);var mark=marks.get(key);
        if(mark==null){
            var stack=new ItemStack(Items.PAPER);stack.set(DataComponents.ITEM_MODEL,
                    Identifier.parse(target instanceof ServerPlayer?"lbc:radar_player":"lbc:radar_mob"));
            var display=new ItemDisplayElement(stack);display.setItemDisplayContext(ItemDisplayContext.NONE);
            display.setBillboardMode(Display.BillboardConstraints.CENTER);display.setScale(new Vector3f(.3f));
            display.setTeleportDuration(1);display.setInterpolationDuration(1);display.setDisplaySize(0,0);
            display.setShadowRadius(0);display.setShadowStrength(0);display.setViewRange(1024);
            mark=new Mark(display);mark.at=position(target);var owned=mark;
            game.visualTracking().attach(mark.holder,viewer.level(),()->owned.at);
            mark.holder.startWatching(viewer);marks.put(key,mark);
        }
        mark.frame=frame;mark.at=position(target);
    }
    private static Vec3 position(LivingEntity target){return new Vec3(target.getX(),target.getBoundingBox().maxY+.3,target.getZ());}
    void end(){
        for(var it=marks.entrySet().iterator();it.hasNext();){var entry=it.next();var key=entry.getKey();var mark=entry.getValue();
            if(mark.frame!=frame||key.viewer.level()!=key.target.level()||key.viewer.connection==null
                    ||game.server().getPlayerList().getPlayer(key.viewer.getUUID())!=key.viewer){mark.holder.destroy();it.remove();continue;}
            // Only position changes: teleport_duration handles movement. Restarting
            // transform interpolation would emit metadata even for stationary targets.
            mark.holder.tick();
        }
    }
    int count(){return marks.size();}
    void stop(){for(var mark:marks.values())mark.holder.destroy();marks.clear();}
}
