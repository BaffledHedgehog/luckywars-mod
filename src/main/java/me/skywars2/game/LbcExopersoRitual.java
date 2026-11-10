package me.skywars2.game;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import com.mojang.math.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Fixed-position wand ritual. External Exoperso rockets continue using the projectile service. */
final class LbcExopersoRitual {
    static final String TAG="skywars2.exoperso_ritual";
    private final GameController game;
    private final LbcExoperso spell;
    private final Set<State> states=new LinkedHashSet<>();
    private final Map<Entity,State> bodies=new IdentityHashMap<>();
    private final Set<Entity> spawning=Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean restoring;

    static final class State {
        final UUID owner;final String epoch;final ServerLevel level;final Vec3 target;
        final boolean upgraded;final Marker anchor;final Display.TextDisplay timer;
        final List<Display.ItemDisplay> rings;
        final float[] scales,angles,speeds;
        CompletableFuture<NuclearTerrain.Geometry> preparation;
        NuclearTerrain.WandPreparation terrain;
        int age;
        State(UUID owner,String epoch,ServerLevel level,Vec3 target,boolean upgraded,Marker anchor,Display.TextDisplay timer,
                List<Display.ItemDisplay> rings,float[] scales,float[] angles,float[] speeds){
            this.owner=owner;this.epoch=epoch;this.level=level;this.target=target;this.upgraded=upgraded;
            this.anchor=anchor;this.timer=timer;this.rings=List.copyOf(rings);this.scales=scales;this.angles=angles;this.speeds=speeds;
        }
        int duration(){return upgraded?160:320;}
        int collapse(){return 1;}
        float scale(int index){
            int growth=upgraded?1:2,birth=index*growth;
            if(age>=duration()-collapse())return 0;
            return scales[index]*Math.clamp((age-birth)/(float)growth,0,1);
        }
    }
    LbcExopersoRitual(GameController game,LbcExoperso spell){this.game=game;this.spell=spell;}
    int count(){return states.size();}
    List<State> snapshot(){return List.copyOf(states);}

    boolean cast(ServerPlayer owner,boolean upgraded){
        var level=owner.level();var from=owner.getEyePosition();var end=from.add(owner.getLookAngle().scale(128));
        var hit=LbcTerra.wallHit(level,from,end);var target=hit==null?end:hit.getLocation();
        // The ray's first missing chunk is a boundary, not a reason to reject an otherwise valid cast.
        if(!level.hasChunkAt(BlockPos.containing(target)))target=target.subtract(owner.getLookAngle().scale(.5));
        if(!level.hasChunkAt(BlockPos.containing(target))||game.items().magic().suppressedAt(level,target))return false;
        var anchor=EntityTypes.MARKER.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if(anchor==null)return false;anchor.setPos(target);
        var timer=new Display.TextDisplay(EntityTypes.TEXT_DISPLAY,level);timer.setPos(target.add(0,1,0));
        timer.setBillboardConstraints(Display.BillboardConstraints.CENTER);timer.setViewRange(8);
        var rings=new ArrayList<Display.ItemDisplay>();var random=level.getRandom();
        float[] scales=new float[30],angles=new float[30],speeds=new float[30];
        for(int i=0;i<30;i++){
            var ring=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,level);
            var item=new ItemStack(Items.STICK);item.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:exoperso/rune_"+String.format(java.util.Locale.ROOT,"%02d",i%10+1)));
            ring.setItemStack(item);ring.setPos(target.add(0,10+(i+random.nextFloat())*(40.0/30),0));
            ring.setViewRange(8);ring.setPosRotInterpolationDuration(1);ring.setTransformationInterpolationDuration(1);
            // Private outline mask feeds rune bloom; ordinary warm scene colors do not.
            ring.setGlowingTag(true);ring.setGlowColorOverride(0xE75313);
            scales[i]=5*(1+random.nextFloat()*19);angles[i]=random.nextFloat()*360;
            speeds[i]=(random.nextBoolean()?1:-1)*(3.6f+random.nextFloat()*32.4f);
            rings.add(ring);
        }
        var state=new State(owner.getUUID(),game.items().epoch(),level,target,upgraded,anchor,timer,rings,scales,angles,speeds);
        paint(state);var all=new ArrayList<Entity>();all.add(anchor);all.add(timer);all.addAll(rings);
        states.add(state);for(var actor:all)bodies.put(actor,state);
        for(var actor:all){
            actor.entityTags().addAll(Set.of(TAG,"strict_map_object"));game.items().markCurrentItemEpoch(actor);
            spawning.add(actor);try{if(!level.addFreshEntity(actor)||actor.isRemoved()){remove(state);return false;}}
            finally{spawning.remove(actor);}
        }
        prepare(state);
        LbcOverload.sound(owner,owner.position().add(0,20,0),"minecraft:lbcsounds.exoperso",15,upgraded?2:1,0);
        return true;
    }
    private void prepare(State state){
        var shape=NuclearTerrain.wand(state.target);
        state.preparation=game.maps().preparePure(()->NuclearTerrain.Geometry.prepare(shape));
    }
    private void paint(State state){
        state.timer.setText(LbcTntTimer.label(Math.max(0,state.duration()-state.age)));
        for(int i=0;i<state.rings.size();i++){
            var ring=state.rings.get(i);float scale=state.scale(i);
            ring.setTransformationInterpolationDelay(0);
            ring.setTransformation(new Transformation(new Vector3f(),new Quaternionf().rotationY((float)Math.toRadians(state.angles[i]+state.speeds[i]*state.age)),
                    new Vector3f(scale,1,scale),new Quaternionf()));
        }
    }
    void tick(){
        if(restoring)return;
        for(var state:List.copyOf(states)){
            var owner=game.server().getPlayerList().getPlayer(state.owner);
            if(!game.items().currentEpoch(state.epoch)||!spell.validOwner(owner)||owner.level()!=state.level
                    ||state.anchor.isRemoved()||!state.level.hasChunkAt(BlockPos.containing(state.target))
                    ||game.items().magic().suppressedAt(state.level,state.target)){remove(state);continue;}
            if(game.items().timeStop().stopped(state.level)||game.items().wands().inputTimeStopped(owner))continue;
            ++state.age;
            if(state.age>=state.duration()-40){
                if(state.terrain==null)state.terrain=new NuclearTerrain.WandPreparation(state.level,state.target);
                state.terrain.tick(game,state.level);
            }
            if(state.age<state.duration()){paint(state);continue;}
            NuclearTerrain.Geometry geometry=null;
            if(state.terrain!=null)geometry=state.terrain.finish(state.level);
            // No join/world access on the worker. A busy decoder cannot postpone the accepted deadline.
            if(geometry==null)geometry=NuclearTerrain.Geometry.prepare(NuclearTerrain.wand(state.target));
            remove(state);spell.ritualImpact(owner,state.target,geometry);
        }
    }
    void loaded(Entity actor){
        if(restoring||spawning.contains(actor)||!actor.entityTags().contains(TAG))return;
        if(!bodies.containsKey(actor)||!game.items().currentEpoch(actor))actor.discard();
    }
    void unloaded(Entity actor){if(restoring||MatchActorCheckpoint.restoring(actor))return;var state=bodies.get(actor);if(state!=null)remove(state);}
    private void remove(State state){
        if(!states.remove(state))return;if(state.preparation!=null)state.preparation.cancel(false);if(state.terrain!=null)state.terrain.cancel();
        bodies.remove(state.anchor);bodies.remove(state.timer);state.rings.forEach(bodies::remove);
        state.anchor.discard();state.timer.discard();state.rings.forEach(Entity::discard);
    }
    void stop(){for(var state:List.copyOf(states))remove(state);restoring=false;}
    void prepareFrozen(){restoring=true;}
    void cancelFrozen(){restoring=false;}
    private record Image(UUID owner,String epoch,ServerLevel level,Vec3 target,boolean upgraded,int age,
            MatchDamageSources.Ref anchor,MatchDamageSources.Ref timer,List<MatchDamageSources.Ref> rings,
            float[] scales,float[] angles,float[] speeds){}
    final class Checkpoint {
        private final List<Image> images;
        Checkpoint(MatchSpellCheckpoint.Links links){images=states.stream().map(s->new Image(s.owner,s.epoch,s.level,s.target,s.upgraded,s.age,
                links.key(s.anchor),links.key(s.timer),s.rings.stream().map(links::key).toList(),s.scales.clone(),s.angles.clone(),s.speeds.clone())).toList();}
        Runnable prepare(MatchSpellCheckpoint.Links links){
            var restored=new ArrayList<State>();
            for(var image:images){MatchAuthorityCheckpoint.world(game,image.level);
                if(!(links.get(image.anchor) instanceof Marker anchor)||!(links.get(image.timer) instanceof Display.TextDisplay timer))
                    throw new IllegalStateException("Restore native Exoperso ritual bodies first");
                var rings=new ArrayList<Display.ItemDisplay>();for(var ref:image.rings){if(!(links.get(ref) instanceof Display.ItemDisplay ring))throw new IllegalStateException("Restore native rune first");rings.add(ring);}
                var state=new State(image.owner,image.epoch,image.level,image.target,image.upgraded,anchor,timer,rings,image.scales.clone(),image.angles.clone(),image.speeds.clone());state.age=image.age;restored.add(state);
            }
            return ()->{for(var state:states){if(state.preparation!=null)state.preparation.cancel(false);if(state.terrain!=null)state.terrain.cancel();}states.clear();bodies.clear();
                for(var state:restored){states.add(state);bodies.put(state.anchor,state);bodies.put(state.timer,state);state.rings.forEach(actor->bodies.put(actor,state));LbcExopersoRitual.this.prepare(state);paint(state);}restoring=false;};
        }
    }
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){return new Checkpoint(links);}
}
