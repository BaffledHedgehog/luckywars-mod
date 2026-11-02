package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/** Explicit mob owner images. Capture on every admitted body: several owners are overlays. */
final class CloneMobStates {
    private CloneMobStates(){}
    static boolean recognizes(Entity e){return LbcSummons.cloneRecognizes(e)||LbcSoulless.cloneRecognizes(e)
            ||LbcKfcRewards.cloneRecognizes(e)||LbcIndolenceFingers.cloneRecognizes(e)
            ||LbcWitchLust.cloneRecognizes(e)||LbcFamilyDrops.managed(e)
            ||e.entityTags().contains(LbcConsuming.TRIAL_ACTOR);}
    static boolean protectedActor(Entity e){return LbcSoulless.cloneProtected(e)||LbcWitchLust.cloneProtected(e)
            ||LbcKfcRewards.cloneProtected(e)||e.entityTags().contains(LbcConsuming.TRIAL_ACTOR);}
    static LbcCloneSkills.ActorImage capture(GameController game,Entity source){
        if(protectedActor(source))return null;
        var images=new ArrayList<LbcCloneSkills.ActorImage>();
        var items=game.items();
        add(images,items.summons().cloneImage(source));
        add(images,items.communism().cloneImage(source));
        add(images,items.soulless().cloneImage(source));
        add(images,items.kfcRewards().cloneImage(source));
        add(images,items.indolenceFingers().cloneImage(source));
        add(images,items.witchLust().cloneImage(source));
        add(images,items.familyDrops().cloneImage(source));
        if(images.isEmpty())return null;
        var immutable=List.copyOf(images);
        return new LbcCloneSkills.ActorImage(){
            public Entity create(Entity original,ServerLevel level){
                // Native overlays use the ordinary factory. Only one owner supplies a custom class.
                for(var image:immutable){var body=image.create(original,level);if(body!=null&&body.getClass()==original.getClass())return body;}
                return null;
            }
            public void install(Entity original,Entity clone,Map<Entity,Entity> family){for(var image:immutable)image.install(original,clone,family);}
            public void rollback(Entity clone){for(int i=immutable.size()-1;i>=0;i--)immutable.get(i).rollback(clone);}
        };
    }
    private static void add(List<LbcCloneSkills.ActorImage> images,LbcCloneSkills.ActorImage image){if(image!=null)images.add(image);}
}
