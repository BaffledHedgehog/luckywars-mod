package me.skywars2.game;

import net.minecraft.world.entity.Entity;

/** Exact native factories and live logical state for the remaining gameplay carriers. */
final class CloneExtraAbilityStates {
    private CloneExtraAbilityStates(){}
    static boolean recognizes(Entity source){return LbcBabylon.cloneRecognizes(source)||LbcStand.cloneRecognizes(source)||LbcSheep.cloneRecognizes(source)||LbcGodHedgehog.cloneActorRecognizes(source);}
    static boolean recognizesGameplay(Entity source){return recognizes(source);}
    static boolean protectedActor(Entity source){return false;}
    static LbcCloneSkills.ActorImage capture(GameController game,Entity source){
        if(LbcBabylon.cloneRecognizes(source))return game.items().babylon().cloneImage(source);
        if(LbcStand.cloneRecognizes(source))return game.items().stand().cloneImage(source);
        if(LbcSheep.cloneRecognizes(source))return game.items().sheep().cloneImage(source);
        if(LbcGodHedgehog.cloneActorRecognizes(source))return game.items().godHedgehog().cloneActorImage(source);
        return null;
    }
}
