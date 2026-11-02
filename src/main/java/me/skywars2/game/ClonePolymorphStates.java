package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.world.entity.Entity;

/** Gameplay orb and nonplayer reversible-form ownership; player form authority remains singular. */
final class ClonePolymorphStates {
    private ClonePolymorphStates() { }
    static boolean recognizes(Entity actor){var game=SkyWars2.game();return game!=null&&game.items().polymorph().cloneRecognizes(actor);}
    static boolean recognizesGameplay(Entity actor){var game=SkyWars2.game();return game!=null&&game.items().polymorph().cloneGameplay(actor);}
    static boolean protectedActor(Entity actor){var game=SkyWars2.game();return game!=null&&game.items().polymorph().cloneProtected(actor);}
    static LbcCloneSkills.ActorImage capture(GameController game,Entity actor){return game.items().polymorph().cloneImage(actor);}
}
