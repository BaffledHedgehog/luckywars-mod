package me.skywars2.game.presentation.projectiles;

import java.util.*;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import me.skywars2.game.GameController;
import me.skywars2.game.entities.mobs.hedgehogs.LbcHedgehog;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Fading shots and one refreshed display per continuous laser. The ability owns damage and collision. */
public final class LbcFadingBeams {
    public record Image(ServerLevel level,Vec3 from,Vec3 to,LbcHedgehog.Kind kind,UUID source,int age,String epoch){}
    private static final class Beam {
        Image initial;final ElementHolder holder;final ItemDisplayElement display;int born,age;
        Beam(Image image,ElementHolder holder,ItemDisplayElement display,int tick){initial=image;this.holder=holder;this.display=display;born=tick;age=image.age;}
    }
    private final GameController game;
    private final List<Beam> beams=new ArrayList<>();
    private final Map<UUID,Beam> sustained=new HashMap<>();
    // Laser Minigun's six-frame width envelope is 6*.04. Keep the same model geometry.
    public static final float LASER_WIDTH=.24f;
    public LbcFadingBeams(GameController game){this.game=game;}
    public void show(ServerLevel level,Vec3 from,Vec3 to,LbcHedgehog.Kind kind,UUID source){
        if(from.distanceToSqr(to)<.000001)return;
        var image=new Image(level,from,to,kind,source,0,game.items().epoch());var old=kind==LbcHedgehog.Kind.LASER?sustained.get(source):null;
        if(old==null){spawn(image);return;}
        old.initial=image;old.born=game.server().getTickCount();old.display.setInterpolationDuration(1);
        pose(old);old.display.startInterpolation();old.holder.tick();
    }
    private static int duration(Image image){return image.kind==LbcHedgehog.Kind.SNIPER?20:10;}
    private static float width(Image image,int age){return LASER_WIDTH*(image.kind==LbcHedgehog.Kind.SNIPER?5f:1.5f)*(image.kind==LbcHedgehog.Kind.LASER?1:Math.max(0,1-age/(float)duration(image)));}
    private static void pose(Beam beam){
        var image=beam.initial;var delta=image.to.subtract(image.from);
        beam.display.setYaw((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));
        beam.display.setPitch((float)-Math.toDegrees(Math.atan2(delta.y,delta.horizontalDistance())));
        beam.display.setScale(new Vector3f(width(image,beam.age),width(image,beam.age),(float)delta.length()*2));
    }
    private void spawn(Image image){
        var delta=image.to.subtract(image.from);if(delta.lengthSqr()<.000001)return;
        var stack=new ItemStack(Items.REPEATING_COMMAND_BLOCK);
        stack.set(DataComponents.ITEM_MODEL,Identifier.parse(switch(image.kind){
            case SNIPER->"lbc:hedgehog_sniper_ray";case LASER->"lbc:hedgehog_laser_ray";
            case HEALER->"lbc:hedgehog_healer_ray";default->"lbc:hedgehog_ray";}));
        var display=new ItemDisplayElement(stack);display.setItemDisplayContext(ItemDisplayContext.NONE);
        display.setShadowRadius(0);display.setShadowStrength(0);display.setInterpolationDuration(0);display.setTeleportDuration(1);
        display.setBrightness(new net.minecraft.util.Brightness(15,15));
        var holder=new ElementHolder();holder.addElement(display);
        var beam=new Beam(image,holder,display,game.server().getTickCount());pose(beam);
        // Native ItemDisplayRenderer adds Y180: model Z[-.5,0] therefore extends forward from the muzzle.
        game.visualTracking().attach(holder,image.level,()->beam.initial.from);
        holder.tick();
        viewers(beam);holder.tick();beams.add(beam);if(image.kind==LbcHedgehog.Kind.LASER)sustained.put(image.source,beam);
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
    public void tick(){
        for(var iterator=beams.iterator();iterator.hasNext();){var beam=iterator.next();var image=beam.initial;
            boolean laser=image.kind==LbcHedgehog.Kind.LASER;
            var source=laser?image.level.getEntity(image.source):null;
            boolean paused=!game.server().tickRateManager().runsNormally()||game.items().timeStop().stopped(image.level)
                    ||source!=null&&(game.items().timeStop().frozen(source)||source.entityTags().contains("time_stopped")
                            ||source instanceof net.minecraft.world.entity.LivingEntity living&&game.items().frost().isFrozen(living));
            if(!game.playing()||!game.items().currentEpoch(image.epoch)||(!laser&&beam.age>duration(image))
                    ||laser&&(source==null||!source.isAlive()||!paused&&beam.born!=game.server().getTickCount())){
                beam.holder.destroy();iterator.remove();if(laser)sustained.remove(image.source,beam);continue;
            }
            viewers(beam);
            if(laser||beam.born==game.server().getTickCount()||paused)continue;
            beam.age++;beam.display.setInterpolationDuration(1);
            beam.display.setScale(new Vector3f(width(image,beam.age),width(image,beam.age),(float)image.from.distanceTo(image.to)*2));
            beam.display.startInterpolation();beam.holder.tick();
        }
    }
    public List<Image> checkpoint(){return beams.stream().map(b->new Image(b.initial.level,b.initial.from,b.initial.to,b.initial.kind,b.initial.source,b.age,b.initial.epoch)).toList();}
    public void restore(List<Image> images){stop();for(var image:images)spawn(image);}
    public int count(){return beams.size();}
    public void stop(){for(var beam:beams)beam.holder.destroy();beams.clear();sustained.clear();}
}
