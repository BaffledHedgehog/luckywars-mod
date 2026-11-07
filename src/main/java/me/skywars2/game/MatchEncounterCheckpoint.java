package me.skywars2.game;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;

/** Essential native Shish/supply and helicopter encounter authority, pairing and scheduled phases. */
final class MatchEncounterCheckpoint {
    private final GameController game;
    private final String epoch;
    private final MatchSpellCheckpoint.Links links;
    private final LbcShishEncounter.Checkpoint shish;
    private final LbcHelicopterEncounter.Checkpoint helicopter;
    private final LbcCrocodildo.Checkpoint crocodildo;
    private boolean prepared,installed,published;

    static boolean captureReady(GameController game){return game.items().shishEncounter().captureReady()&&game.items().helicopterEncounter().captureReady()&&game.items().crocodildo().captureReady();}
    MatchEncounterCheckpoint(GameController game,MatchDamageSources sources){
        this.game=game;epoch=game.items().epoch();stable(false);
        if(!captureReady(game))throw new IllegalStateException("Finish native encounter construction before capture");
        links=new MatchSpellCheckpoint.Links(sources);shish=game.items().shishEncounter().checkpoint(links);helicopter=game.items().helicopterEncounter().checkpoint(links);crocodildo=game.items().crocodildo().checkpoint(links);
    }
    private void stable(boolean frozen){
        if(!game.server().isSameThread()||!game.playing()||game.maps().busy()||!game.items().currentEpoch(epoch)
                ||frozen&&!game.server().tickRateManager().isFrozen())
            throw new IllegalStateException("Restore encounters on their stable frozen match thread");
        game.items().shishEncounter().checkpointBoundary();game.items().helicopterEncounter().checkpointBoundary();game.items().spellSources().checkpointBoundary();game.items().crocodildo().checkpointBoundary();
    }
    /** Before native body replacement; no native discard, attribute withdrawal, reward, attack or sound runs. */
    void prepareFrozen(){
        stable(true);game.items().shishEncounter().prepareFrozen();game.items().helicopterEncounter().prepareFrozen();game.items().crocodildo().prepareFrozen();prepared=true;installed=false;published=false;
    }
    Set<UUID> restoreFrozen(Function<UUID,Entity> lookup){
        stable(true);if(!prepared)prepareFrozen();var pending=links.resolve(lookup);if(!pending.isEmpty())return pending;
        var s=shish.prepare(links);var h=helicopter.prepare(links);var c=crocodildo.prepare(links);s.run();h.run();c.run();installed=true;published=false;return Set.of();
    }
    /** Canonical native displays carry visual state; only source frame dedup clocks rebase at publication. */
    Set<UUID> publishFrozen(){
        stable(true);if(published)return Set.of();if(!installed)throw new IllegalStateException("Install encounter state before publication");
        game.items().shishEncounter().publishFrozen();game.items().helicopterEncounter().publishFrozen();game.items().crocodildo().publishFrozen();prepared=false;published=true;return Set.of();
    }
    void cancelFrozen(){
        stable(true);game.items().shishEncounter().cancelFrozen();game.items().helicopterEncounter().cancelFrozen();game.items().crocodildo().cancelFrozen();prepared=false;installed=false;published=false;
    }
}
