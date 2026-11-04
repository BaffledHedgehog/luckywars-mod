package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import java.util.ArrayList;
import java.util.Map;

/** Narrow per-body owner images; published queues and player controller authority stay with their owners. */
final class CloneOtherActorStates {
    private CloneOtherActorStates() { }
    static boolean recognizes(Entity actor) {
        var game=SkyWars2.game();
        return LbcOreshnik.managed(actor)||LbcBridgeBuilder.managed(actor)||LbcBadApple.managed(actor)||actor instanceof LbcWtf.Chef || actor instanceof LbcFpvDrone.Drone
                || LbcTransformator.managed(actor) || LbcChaosTrapActors.managed(actor)
                || LbcAntDrops.managed(actor) || LbcDroneDrops.managed(actor)
                || game!=null&&game.items().libraryRandom().cloneManaged(actor);
    }
    static boolean protectedActor(Entity actor) {
        var game=SkyWars2.game();
        if(game!=null&&game.items().bridgeBuilder().protectedCloneActor(actor))return true;
        return LbcBadApple.managed(actor)&&actor instanceof net.minecraft.world.entity.Display||LbcFpvDrone.cloneProtected(actor)||LbcTransformator.cloneProtected(actor)
                ||actor.entityTags().contains("skywars2.label_chef_display");
    }
    static LbcCloneSkills.ActorImage capture(GameController game,Entity actor) {
        if(protectedActor(actor))return null;
        var images=new ArrayList<LbcCloneSkills.ActorImage>();
        if(LbcOreshnik.managed(actor)){var image=game.items().cinema().rockets().cloneImage(actor);if(image==null)return null;images.add(image);}
        if(game.items().bridgeBuilder().cloneRecognizes(actor)){var image=game.items().bridgeBuilder().cloneImage(actor);if(image==null)return null;images.add(image);}
        if(game.items().badApple().cloneRecognizes(actor)){var image=game.items().badApple().cloneImage(actor);if(image==null)return null;images.add(image);}
        if(actor instanceof LbcWtf.Chef chef){var image=game.items().wtf().cloneImage(chef);if(image==null)return null;images.add(image);}
        if(actor instanceof LbcFpvDrone.Drone drone){var image=game.items().fpvDrone().cloneImage(drone);if(image==null)return null;images.add(image);}
        if(LbcTransformator.managed(actor)){var image=game.items().transformator().cloneImage(actor);if(image==null)return null;images.add(image);}
        if(LbcChaosTrapActors.managed(actor))images.add(game.items().chaosTrapActors().cloneImage(actor));
        if(LbcAntDrops.managed(actor))images.add(game.items().antDrops().cloneImage(actor));
        if(LbcDroneDrops.managed(actor))images.add(game.items().droneDrops().cloneImage(actor));
        if(game.items().libraryRandom().cloneManaged(actor))images.add(game.items().libraryRandom().cloneImage(actor));
        if(images.isEmpty())return null;
        var saved=java.util.List.copyOf(images);
        return new LbcCloneSkills.ActorImage() {
            public Entity create(Entity original,ServerLevel level){return saved.getFirst().create(original,level);}
            public void install(Entity original,Entity clone,Map<Entity,Entity> family){
                for(var image:saved)image.install(original,clone,family);
            }
            public void rollback(Entity clone){
                RuntimeException failure=null;
                for(int i=saved.size()-1;i>=0;i--)try{saved.get(i).rollback(clone);}catch(RuntimeException error){
                    if(failure==null)failure=error;else failure.addSuppressed(error);
                }
                if(failure!=null)throw failure;
            }
        };
    }
}
