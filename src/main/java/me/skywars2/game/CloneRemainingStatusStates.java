package me.skywars2.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.Entity;

/** Remaining explicit target images and real native gameplay producer indexes. */
final class CloneRemainingStatusStates {
    private CloneRemainingStatusStates(){}
    static boolean recognizes(Entity source){return LbcPendingCombat.cloneRecognizes(source)||LbcQuiver.cloneRecognizes(source)||LbcIncomingArtifacts.managed(source);}
    static boolean recognizesGameplay(Entity source){return recognizes(source);}
    static boolean protectedActor(Entity source){return source.entityTags().contains("stopper_magic")||source.entityTags().contains("stopper");}
    static LbcCloneSkills.ActorImage capture(GameController game,Entity source){
        if(protectedActor(source))return null;
        var all=new ArrayList<LbcCloneSkills.ActorImage>();var items=game.items();
        add(all,items.pendingCombat().cloneImage(source));
        add(all,items.mana().cloneImage(source));
        add(all,items.quiver().cloneImage(source));
        add(all,items.kiuas().cloneImage(source));
        add(all,items.magic().colors().cloneImage(source));
        add(all,items.incomingArtifacts().cloneImage(source));
        add(all,items.reactiveArtifacts().cloneImage(source));
        if(all.isEmpty())return null;var images=List.copyOf(all);
        return new LbcCloneSkills.ActorImage(){
            public void install(Entity original,Entity clone,Map<Entity,Entity> family){for(var image:images)image.install(original,clone,family);}
            public void rollback(Entity clone){for(int i=images.size()-1;i>=0;i--)images.get(i).rollback(clone);}
        };
    }
    private static void add(List<LbcCloneSkills.ActorImage> images,LbcCloneSkills.ActorImage image){if(image!=null)images.add(image);}
    /** Read existing owner-defined clocks only; never materialize source score entries. */
    static Map<String,Integer> scores(GameController game,Entity source,List<String> names){
        var result=new LinkedHashMap<String,Integer>();var board=game.server().getScoreboard();
        for(var name:names){var objective=board.getObjective(name);var value=objective==null?null:board.getPlayerScoreInfo(source,objective);if(value!=null)result.put(name,value.value());}
        return Map.copyOf(result);
    }
    static void installScores(GameController game,Entity target,Map<String,Integer> values){values.forEach((name,value)->game.items().mana().score(target,name).set(value));}
}
