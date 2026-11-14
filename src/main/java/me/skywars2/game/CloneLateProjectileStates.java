package me.skywars2.game;

import net.minecraft.world.entity.Entity;

/** Explicit factories for projectile mechanics added after the original cloning catalogue. */
final class CloneLateProjectileStates {
    private CloneLateProjectileStates(){}
    static boolean recognizes(Entity entity){return LbcHedgehogSpikes.cloneRecognizes(entity)||LbcPendingSwords.cloneRecognizes(entity)
            ||LbcShooters.cloneRecognizes(entity)||LbcVexSword.cloneRecognizes(entity)||LbcFoodLifecycle.cloneRecognizes(entity)||LbcWitchIndolence.cloneRecognizes(entity);}
    static LbcCloneSkills.ActorImage capture(GameController game,Entity entity){
        if(LbcHedgehogSpikes.cloneRecognizes(entity))return game.items().hedgehogSpikes().cloneImage(entity);
        if(LbcPendingSwords.cloneRecognizes(entity))return game.items().pendingSwords().cloneImage(entity);
        if(LbcShooters.cloneRecognizes(entity))return game.items().shooters().cloneImage(entity);
        if(LbcVexSword.cloneRecognizes(entity))return game.items().vexSword().cloneImage(entity);
        if(LbcFoodLifecycle.cloneRecognizes(entity))return game.items().foodLifecycle().cloneImage(entity);
        if(LbcWitchIndolence.cloneRecognizes(entity))return game.items().witchIndolence().cloneImage(entity);
        return null;
    }
}
