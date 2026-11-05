package me.skywars2.game;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Final reviewed logical slow actors and attachment-owned native gameplay actors. */
public final class CloneFinalAbilityStates {
    private CloneFinalAbilityStates() { }
    public static boolean recognizes(Entity e) {
        return LbcHomingArrows.cloneRecognizes(e)||LbcKapusta.cloneRecognizes(e)||LbcBouncingPoop.cloneRecognizes(e)||LbcSauvojen.cloneProtected(e)
                ||e.entityTags().contains(SauvojenMeditation.TAG)||e.entityTags().contains("banhammer3d");
    }
    /** Modern attachment/native bodies need not retain the old datapack slowraycast spelling. */
    public static boolean recognizesGameplay(Entity e){return LbcHomingArrows.cloneRecognizes(e)||LbcKapusta.cloneRecognizes(e)||LbcBouncingPoop.cloneRecognizes(e);}
    public static boolean protectedActor(Entity e) {
        return LbcSauvojen.cloneProtected(e)||e.entityTags().contains(SauvojenMeditation.TAG);
    }
    public static LbcCloneSkills.ActorImage captureNative(GameController game,Entity e) {
        return LbcHomingArrows.cloneRecognizes(e)?game.items().homingArrows().cloneImage(e):LbcKapusta.cloneRecognizes(e)?game.items().kapusta().cloneImage(e):LbcBouncingPoop.cloneRecognizes(e)?game.items().bouncingPoop().cloneImage(e):null;
    }
    public static int duplicateNear(GameController game,ServerPlayer holder,int skill,int budget,long deadlineNanos) {
        if(skill!=15&&skill!=17||budget<CloneVirtualAbilityStates.count(skill))return 0;
        // Erecto Sphere is deliberately dormant: its source has no slowraycast tag.
        int made=game.items().sauvojen().cloneNearby(holder,skill,budget,deadlineNanos);
        if(System.nanoTime()<deadlineNanos)made+=game.items().specialTools().cloneNearby(holder,skill,budget-made,deadlineNanos);
        if(System.nanoTime()<deadlineNanos)made+=game.items().pumpkinator().cloneNearby(holder,skill,64,budget-made,deadlineNanos).bodies();
        return made;
    }
    /** Root composes this narrow long-actor checkpoint into its existing canonical Links graph. */
    static final class Checkpoint {
        private final LbcHomingArrows.Checkpoint homingArrows;
        private final LbcKapusta.Checkpoint kapusta;
        private final MatchQueuedWorkCheckpoint.Image pumpkinator;
        private final LbcErecto.SphereCheckpoint spheres;
        private final LbcSauvojen.DroneCheckpoint drones;
        private final LbcSpecialTools.HammerCheckpoint hammers;
        Checkpoint(GameController game,MatchSpellCheckpoint.Links links) {
            homingArrows=game.items().homingArrows().checkpoint(links);kapusta=game.items().kapusta().checkpoint(links);pumpkinator=game.items().pumpkinator().checkpoint(links);
            spheres=game.items().erecto().checkpointSpheres(links);
            drones=game.items().sauvojen().checkpointDrones(links);
            hammers=game.items().specialTools().checkpointHammers(links);
        }
        Runnable prepare(MatchSpellCheckpoint.Links links) {
            var sphere=spheres.prepare(links);var drone=drones.prepare(links);var hammer=hammers.prepare(links);
            var arrows=homingArrows.prepare(links);var cabbage=kapusta.prepare(links);var pumpkin=pumpkinator.prepare(links);
            return ()->{arrows.run();sphere.run();drone.run();hammer.run();cabbage.run();pumpkin.run();};
        }
    }
    static void publishCheckpoint(GameController game) {
        game.items().kapusta().publishFrozen();game.items().pumpkinator().publishFrozen();
        game.items().sauvojen().publishCloneDrones();game.items().specialTools().publishCloneHammers();
    }
}
