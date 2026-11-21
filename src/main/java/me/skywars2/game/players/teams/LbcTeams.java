package me.skywars2.game.players.teams;

import me.skywars2.game.GameController;
import net.minecraft.world.entity.Entity;

/** Current native teams and physical summon controllers, independent of statistical kill credit. */
public final class LbcTeams {
    private LbcTeams(){}

    public static boolean allied(GameController game,Entity first,Entity second){
        if(first==null||second==null)return false;
        if(first==second||first.isAlliedTo(second))return true;
        var a=teamActor(game,first);var b=teamActor(game,second);
        return a==b||a.isAlliedTo(b)||first.isAlliedTo(b)||a.isAlliedTo(second);
    }

    private static Entity teamActor(GameController game,Entity entity){
        // Bound the walk even for a malformed native projectile-owner cycle.
        for(int depth=0;depth<8;depth++){
            var source=game.items().spellSources().source(entity);
            if(source!=null&&source.unownedMobs())return entity;
            var controller=game.items().spellSources().projectileController(entity);
            if(controller==null||controller==entity)controller=game.items().summons().owner(entity);
            if(controller==null||controller==entity)return entity;
            entity=controller;
        }
        return entity;
    }
}
