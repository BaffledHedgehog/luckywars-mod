package me.skywars2.game;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import java.util.List;

/** Visual-only telegraphs: geometry follows the actual hazard, never substitutes for damage queries. */
final class TrialWarningVisual implements AutoCloseable {
    enum Kind { BEAM,WALL,CUBE,SPHERE,RING }
    private final GameController game;
    private final ServerLevel level;
    private final String epoch;
    private final ElementHolder holder=new ElementHolder();
    private final ItemDisplayElement display;
    private final ItemStack stack;
    private final Kind kind;
    private final Identifier model;
    private final int lifetime;
    private final double radius;
    private Vec3 origin,end;
    private int age,opacity=255;
    private boolean closed;

    private TrialWarningVisual(GameController game,ServerLevel level,String epoch,Kind kind,String suffix,
                               Vec3 origin,Vec3 end,int lifetime,double radius) {
        this.game=game;this.level=level;this.epoch=epoch;this.kind=kind;this.origin=origin;this.end=end;
        this.lifetime=Math.max(1,lifetime);this.radius=radius;
        model=Identifier.parse("lbc:meditation_warning_"+suffix);
        stack=new ItemStack(Items.PAPER);stack.set(DataComponents.ITEM_MODEL,model);alpha(255);
        display=new ItemDisplayElement(stack.copy());display.setItemDisplayContext(ItemDisplayContext.NONE);
        display.setBillboardMode(Display.BillboardConstraints.FIXED);
        display.setShadowRadius(0);display.setShadowStrength(0);display.setDisplaySize(0,0);
        display.setViewRange(64);display.setTeleportDuration(1);display.setInterpolationDuration(1);
        holder.addElement(display);game.visualTracking().attach(holder,level,()->this.origin);
    }
    private void spawn(){refreshViewers();holder.tick();}
    static TrialWarningVisual beam(GameController game,ServerLevel level,String epoch,Vec3 from,Vec3 to,boolean green,int life){
        var v=new TrialWarningVisual(game,level,epoch,Kind.BEAM,green?"beam_green":"beam_red",from,to,life,0);
        Vec3 delta=to.subtract(from);double length=delta.length();
        v.display.setYaw((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));
        v.display.setPitch((float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance())));
        v.display.setScale(new Vector3f(1,1,(float)Math.max(.001,length)));v.spawn();return v;
    }
    static TrialWarningVisual wall(GameController game,ServerLevel level,String epoch,Vec3 center,Vec3 direction,double width,double height,int life){
        var v=new TrialWarningVisual(game,level,epoch,Kind.WALL,"wall_red",center,center,life,0);
        v.display.setYaw((float)Math.toDegrees(Math.atan2(-direction.x,direction.z)));
        v.display.setScale(new Vector3f((float)width,(float)height,1));v.spawn();return v;
    }
    static TrialWarningVisual cube(GameController game,ServerLevel level,String epoch,Vec3 min,Vec3 max,int life){
        var v=new TrialWarningVisual(game,level,epoch,Kind.CUBE,"cube_blue",min.add(max).scale(.5),max,life,0);
        v.cubeBounds(min,max);v.spawn();return v;
    }
    static TrialWarningVisual sphere(GameController game,ServerLevel level,String epoch,Vec3 center,double radius,int life){
        var v=new TrialWarningVisual(game,level,epoch,Kind.SPHERE,"sphere_orange",center,center,life,radius);
        v.display.setScale(new Vector3f(0));v.spawn();return v;
    }
    static TrialWarningVisual ring(GameController game,ServerLevel level,String epoch,Vec3 center,double outerRadius,int life){
        var v=new TrialWarningVisual(game,level,epoch,Kind.RING,"ring_green",center,center,life,outerRadius);
        v.display.setScale(new Vector3f((float)(outerRadius*2),1,(float)(outerRadius*2)));v.spawn();return v;
    }
    /** One tick ahead offsets the client's one-tick display-position interpolation, not the hit test. */
    void moveTo(Vec3 at){if(closed)return;Vec3 delta=at.subtract(origin);origin=at;end=end.add(delta);}
    void cubeBounds(Vec3 min,Vec3 max){if(closed)return;origin=min.add(max).scale(.5);end=max;
        display.setScale(new Vector3f((float)(max.x-min.x),(float)(max.y-min.y),(float)(max.z-min.z)));}
    private void alpha(int amount){opacity=amount;int rgb=amount*0x010101;
        stack.set(DataComponents.CUSTOM_MODEL_DATA,new CustomModelData(List.of(),List.of(),List.of(),List.of(rgb)));}
    boolean tick(){
        if(closed)return true;
        if(!game.items().currentEpoch(epoch)||level!=game.maps().level()){close();return true;}
        refreshViewers();if(game.items().timeStop().stopped(level))return false;
        if(++age>=lifetime){close();return true;}
        if(kind==Kind.SPHERE)display.setScale(new Vector3f((float)(2*radius*age/Math.max(1,lifetime-1))));
        // Alpha is an unlit marker tint, decoded by the template shader. It never darkens bloom RGB.
        int fade=Math.max(1,Math.min(10,lifetime/3));
        int next=kind==Kind.SPHERE?255:(int)Math.round(255*Math.min(1,(lifetime-age)/(double)fade));
        if(next!=opacity){alpha(next);display.setItem(stack.copy());}
        display.startInterpolation();holder.tick();return false;
    }
    private void refreshViewers(){
        var players=game.server().getPlayerList().getPlayers();double range=256+origin.distanceTo(end);
        double squared=range*range;
        for(var c:List.copyOf(holder.getWatchingPlayers()))if(c.player.connection!=c||!players.contains(c.player)
                ||c.player.level()!=level||c.player.position().distanceToSqr(origin)>squared)holder.stopWatching(c);
        for(var p:players)if(p.level()==level&&p.position().distanceToSqr(origin)<=squared)holder.startWatching(p);
    }
    @Override public void close(){if(!closed){closed=true;holder.destroy();}}
    Vec3 origin(){return origin;} Vec3 end(){return end;}
    Identifier model(){return model;} boolean closed(){return closed;}
    int age(){return age;} int lifetime(){return lifetime;} double radius(){return radius;}
    Kind kind(){return kind;} ItemDisplayElement display(){return display;}
}
