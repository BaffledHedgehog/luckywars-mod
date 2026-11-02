package me.skywars2.game;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;

/** Real reconnect: replace retired player references in CURRENT private state; never replay the old world image. */
final class MatchRuntimeRebind {
    private final Function<UUID,Entity> lookup;
    private final MatchDamageSources sources;
    private final LbcTimeStop.Checkpoint stop;
    private final LbcIndirectKills.Checkpoint attribution;
    private final MatchSpellCheckpoint spells;
    private final MatchAuthorityCheckpoint authorities;
    private final MatchQueuedWorkCheckpoint queued;
    private final MatchBlastCheckpoint blasts;
    private final LbcFpvDrone.Checkpoint fpv;
    private final MatchCombatStatusCheckpoint statuses;
    private final MatchChefCheckpoint chefs;
    private final MatchHedgehogCheckpoint hedgehogs;
    private final MatchEncounterCheckpoint encounters;
    private final MatchResidentItemCheckpoint residents;
    private boolean installed;
    MatchRuntimeRebind(GameController game){
        var bodies=MatchActorCheckpoint.currentBodies(game);lookup=bodies::get;
        sources=new MatchDamageSources(game,bodies.values());
        stop=game.items().timeStop().checkpoint(sources);attribution=game.items().indirectKills().checkpoint(sources);
        spells=new MatchSpellCheckpoint(game,sources);authorities=new MatchAuthorityCheckpoint(game,sources);
        MatchQueuedWorkCheckpoint work;
        try{work=new MatchQueuedWorkCheckpoint(game,sources);}
        catch(IllegalStateException pending){
            String reason=pending.getMessage();
            if(reason==null||!(reason.contains("shared-work checkpoint adapter")||reason.contains("Duplicating")||reason.contains("duplicating")))throw pending;
            // A current, unrelated construction continues after reconnect. Its private
            // continuation is not an old world image and must not be discarded or rewound.
            work=null;
        }
        queued=work;blasts=new MatchBlastCheckpoint(game,sources);
        fpv=game.items().fpvDrone().checkpoint(sources);
        statuses=new MatchCombatStatusCheckpoint(game,sources);chefs=new MatchChefCheckpoint(game,sources);
        hedgehogs=new MatchHedgehogCheckpoint(game,sources);encounters=new MatchEncounterCheckpoint(game,sources);residents=new MatchResidentItemCheckpoint(game,sources);
    }
    private static void ready(Set<UUID> pending){if(!pending.isEmpty())throw new IllegalStateException("Real-login references unavailable: "+pending);}
    void prepareFrozen(){
        stop.prepareFrozen();fpv.prepareFrozen();if(queued!=null)queued.prepareFrozen();blasts.prepareFrozen();
        statuses.prepareFrozen();chefs.prepareFrozen();hedgehogs.prepareFrozen();encounters.prepareFrozen();residents.prepareFrozen();
        var pending=new LinkedHashSet<UUID>();sources.restore(lookup,pending);ready(pending);
        ready(spells.restoreFrozen(lookup,false));ready(authorities.restoreFrozen(lookup));ready(attribution.restoreFrozen(lookup));
        ready(fpv.finishFrozen(lookup));
        ready(statuses.restoreFrozen(lookup));ready(chefs.restoreFrozen(lookup));ready(hedgehogs.restoreFrozen(lookup));ready(encounters.restoreFrozen(lookup));ready(residents.restoreFrozen(lookup));
    }
    boolean publishFrozen(){
        if(!fpv.publishFrozen().isEmpty())return false;
        if(!installed){if(queued!=null)ready(queued.finishFrozen(lookup));ready(blasts.finishFrozen(lookup));ready(stop.finishFrozen(lookup));installed=true;}
        if(queued!=null&&!queued.publishFrozen().isEmpty())return false;
        if(!encounters.publishFrozen().isEmpty())return false;
        spells.publishFrozen();authorities.publishFrozen();ready(statuses.publishFrozen());ready(chefs.publishFrozen());ready(hedgehogs.publishFrozen());ready(residents.publishFrozen());return true;
    }
}
