package me.skywars2.game;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;

/** Essential event phases and long-lived actors. Source terrain must finish before capture. */
final class MatchEventActorCheckpoint {
    private final GameController game;
    private final String epoch;
    private final MatchSpellCheckpoint.Links links;
    private final LbcApocalypseEvents.Checkpoint events;
    private final LbcApocalypseActors.Checkpoint actors;
    private final LbcNukeEvent.Checkpoint nuke;
    private final LbcSauvojenBoss.Checkpoint boss;
    private boolean prepared,installed,published;

    static boolean captureReady(GameController game){
        return game.items().apocalypseEvents().captureReady()&&game.items().apocalypseActors().captureReady()
                &&game.items().nukeEvent().captureReady()&&game.items().sauvojenBoss().captureReady();
    }
    MatchEventActorCheckpoint(GameController game,MatchDamageSources sources){
        this.game=game;epoch=game.items().epoch();stable(false);
        if(!captureReady(game))throw new IllegalStateException("Complete event terrain and template preparation before capture");
        links=new MatchSpellCheckpoint.Links(sources);
        events=game.items().apocalypseEvents().checkpoint(links);actors=game.items().apocalypseActors().checkpoint(links);
        nuke=game.items().nukeEvent().checkpoint(links);boss=game.items().sauvojenBoss().checkpoint(links);
    }
    private void stable(boolean frozen){
        if(!game.server().isSameThread()||!game.playing()||game.maps().busy()||!game.items().currentEpoch(epoch)
                ||frozen&&!game.server().tickRateManager().isFrozen())
            throw new IllegalStateException("Restore event actors on the same stable frozen match thread");
        game.items().apocalypseEvents().checkpointBoundary();game.items().apocalypseActors().checkpointBoundary();
        game.items().nukeEvent().checkpointBoundary();game.items().sauvojenBoss().checkpointBoundary();game.items().spellSources().checkpointBoundary();
    }
    /** Remove abandoned future terrain; never use this to make an unfinished capture ready. */
    void cancelFutureTerrainForRollback(){
        stable(true);game.items().apocalypseEvents().cancelFutureTerrainForRollback();game.items().apocalypseActors().cancelFutureTerrainForRollback();
        game.items().nukeEvent().cancelFutureTerrainForRollback();game.items().sauvojenBoss().cancelFutureTerrainForRollback();
    }
    /** Before native body replacement: source callbacks pause and only boss cloud visuals detach. */
    void prepareFrozen(){
        stable(true);game.items().apocalypseEvents().prepareFrozen();game.items().apocalypseActors().prepareFrozen();
        game.items().nukeEvent().prepareFrozen();game.items().sauvojenBoss().prepareFrozen();
        prepared=true;installed=false;published=false;
    }
    Set<UUID> restoreFrozen(Function<UUID,Entity> lookup){
        stable(true);if(!prepared)prepareFrozen();var pending=links.resolve(lookup);if(!pending.isEmpty())return pending;
        var e=events.prepare(links);var a=actors.prepare(links);var n=nuke.prepare(links);var b=boss.prepare(links);
        e.run();a.run();n.run();b.run();installed=true;published=false;return Set.of();
    }
    Set<UUID> publishFrozen(){
        stable(true);if(published)return Set.of();if(!installed)throw new IllegalStateException("Install event actors before publication");
        var pending=new LinkedHashSet<UUID>();
        for(var player:game.server().getPlayerList().getPlayers())
            if(player.isChangingDimension()||!player.connection.hasClientLoaded())pending.add(player.getUUID());
        if(!pending.isEmpty())return Set.copyOf(pending);
        game.items().apocalypseEvents().publishFrozen();game.items().apocalypseActors().publishFrozen();
        game.items().nukeEvent().publishFrozen();game.items().sauvojenBoss().publishFrozen();prepared=false;published=true;return Set.of();
    }
    void cancelFrozen(){
        stable(true);game.items().apocalypseEvents().cancelFrozen();game.items().apocalypseActors().cancelFrozen();
        game.items().nukeEvent().cancelFrozen();game.items().sauvojenBoss().cancelFrozen();prepared=false;installed=false;published=false;
    }
}
