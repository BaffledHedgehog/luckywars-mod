package me.skywars2.game;

import java.util.*;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Finite ray visuals. Hit detection and damage remain with the ability that requested the ray. */
final class LbcFadingBeams {
    record Image(ServerLevel level,Vec3 from,Vec3 to,boolean sniper,int age,String epoch){}
    private record Beam(Image initial,ElementHolder holder,ItemDisplayElement display,int born,int[] age){}
    private final GameController game;
    private final List<Beam> beams=new ArrayList<>();
    // Laser Minigun's six-frame width envelope is 6*.04. Keep the same model geometry.
    static final float LASER_WIDTH=.24f;
    LbcFadingBeams(GameController game){this.game=game;}
    void show(ServerLevel level,Vec3 from,Vec3 to,boolean sniper){spawn(new Image(level,from,to,sniper,0,game.items().epoch()));}
    private static int duration(Image image){return image.sniper?20:10;}
    private static float width(Image image,int age){return LASER_WIDTH*(image.sniper?1.25f:1.5f)*Math.max(0,1-age/(float)duration(image));}
    private void spawn(Image image){
        var delta=image.to.subtract(image.from);if(delta.lengthSqr()<.000001)return;
        var stack=new ItemStack(Items.REPEATING_COMMAND_BLOCK);
        stack.set(DataComponents.ITEM_MODEL,Identifier.parse(image.sniper?"lbc:laser_minigun_ray":"lbc:hedgehog_ray"));
        var display=new ItemDisplayElement(stack);display.setItemDisplayContext(ItemDisplayContext.NONE);
        display.setYaw((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));
        display.setPitch((float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance())));
        display.setShadowRadius(0);display.setShadowStrength(0);display.setInterpolationDuration(0);
        display.setScale(new Vector3f(width(image,image.age),width(image,image.age),(float)delta.length()*2));
        var holder=new ElementHolder();holder.addElement(display);
        // Native ItemDisplayRenderer adds Y180: model Z[-.5,0] therefore extends forward from the muzzle.
        game.visualTracking().attach(holder,image.level,()->image.from);
        holder.tick();
        var beam=new Beam(image,holder,display,game.server().getTickCount(),new int[]{image.age});
        viewers(beam);holder.tick();beams.add(beam);
    }
    private void viewers(Beam beam){
        var image=beam.initial;var players=game.server().getPlayerList().getPlayers();var axis=image.to.subtract(image.from);double length=axis.lengthSqr();
        for(var viewer:List.copyOf(beam.holder.getWatchingPlayers()))if(!players.contains(viewer.player)||viewer.player.level()!=image.level)beam.holder.stopWatching(viewer);
        for(var p:players){
            if(p.level()!=image.level)continue;
            double t=net.minecraft.util.Mth.clamp(p.getEyePosition().subtract(image.from).dot(axis)/length,0,1);
            boolean near=p.getEyePosition().distanceToSqr(image.from.add(axis.scale(t)))<=64*64;
            if(near&&!beam.holder.getWatchingPlayers().contains(p.connection))beam.holder.startWatching(p.connection);
            else if(!near&&beam.holder.getWatchingPlayers().contains(p.connection))beam.holder.stopWatching(p.connection);
        }
    }
    void tick(){
        for(var iterator=beams.iterator();iterator.hasNext();){var beam=iterator.next();var image=beam.initial;
            if(!game.playing()||!game.items().currentEpoch(image.epoch)||beam.age[0]>duration(image)){
                beam.holder.destroy();iterator.remove();continue;
            }
            viewers(beam);
            if(beam.born==game.server().getTickCount()||!game.server().tickRateManager().runsNormally()||game.items().timeStop().stopped(image.level))continue;
            beam.age[0]++;beam.display.setInterpolationDuration(1);
            beam.display.setScale(new Vector3f(width(image,beam.age[0]),width(image,beam.age[0]),(float)image.from.distanceTo(image.to)*2));
            beam.display.startInterpolation();beam.holder.tick();
        }
    }
    List<Image> checkpoint(){return beams.stream().map(b->new Image(b.initial.level,b.initial.from,b.initial.to,b.initial.sniper,b.age[0],b.initial.epoch)).toList();}
    void restore(List<Image> images){stop();for(var image:images)spawn(image);}
    int count(){return beams.size();}
    void stop(){for(var beam:beams)beam.holder.destroy();beams.clear();}
}
