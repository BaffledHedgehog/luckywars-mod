package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.Entity;

/** Operation-local target status overlays, installed after native effect values. */
final class CloneStatusStates {
    private CloneStatusStates(){}
    static LbcCloneSkills.ActorImage capture(GameController game,Entity source){
        var items=game.items();var all=new ArrayList<LbcCloneSkills.ActorImage>();
        add(all,items.ownedEffects().cloneImage(source));
        add(all,items.indirectKills().cloneImage(source));
        add(all,items.lifetimes().cloneImage(source));
        add(all,items.electro().cloneImage(source));
        add(all,items.shooters().cloneStatusImage(source));
        add(all,items.homing().cloneImage(source));
        add(all,items.frost().cloneImage(source));
        add(all,items.babylonEffects().cloneImage(source));
        add(all,SignedEnvironmentalEffects.cloneImage(source));
        if(all.isEmpty())return null;var images=List.copyOf(all);
        return new LbcCloneSkills.ActorImage(){
            public void install(Entity original,Entity clone,Map<Entity,Entity> family){for(var image:images)image.install(original,clone,family);}
            public void rollback(Entity clone){for(int i=images.size()-1;i>=0;i--)images.get(i).rollback(clone);}
        };
    }
    private static void add(List<LbcCloneSkills.ActorImage> images,LbcCloneSkills.ActorImage image){if(image!=null)images.add(image);}
}
