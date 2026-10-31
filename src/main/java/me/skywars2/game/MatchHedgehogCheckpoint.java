package me.skywars2.game;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;

/** Native encounter registry and finite attack cadence; bodies and terrain remain canonical. */
final class MatchHedgehogCheckpoint {
    private final GameController game;
    private final String epoch;
    private final MatchSpellCheckpoint.Links links;
    private final LbcGodHedgehog.Checkpoint boss;
    private final LbcHedgehog.Checkpoint helpers;
    private boolean prepared,installed,published;

    MatchHedgehogCheckpoint(GameController game,MatchDamageSources sources){
        this.game=game;epoch=game.items().epoch();stable(false);links=new MatchSpellCheckpoint.Links(sources);
        boss=game.items().godHedgehog().checkpoint(links);helpers=game.items().hedgehog().checkpoint(links);
    }
    private void stable(boolean frozen){
        if(!game.server().isSameThread()||!game.playing()||game.maps().busy()||!game.items().currentEpoch(epoch)
                ||frozen&&!game.server().tickRateManager().isFrozen())
            throw new IllegalStateException("Restore Hedgehog encounters on the same stable frozen match thread");
        game.items().godHedgehog().checkpointBoundary();game.items().hedgehog().checkpointBoundary();
        game.items().spellSources().checkpointBoundary();
    }
    /** Before native body rebuilding: release future registries and warnings without retiring actors. */
    void prepareFrozen(){
        stable(true);game.items().godHedgehog().prepareFrozen();game.items().hedgehog().prepareFrozen();
        prepared=true;installed=false;published=false;
    }
    Set<UUID> restoreFrozen(Function<UUID,Entity> lookup){
        stable(true);if(!prepared)prepareFrozen();var pending=links.resolve(lookup);if(!pending.isEmpty())return pending;
        var encounter=boss.prepare(links);var helpersPlan=helpers.prepare(links);
        encounter.run();helpersPlan.run();installed=true;published=false;return Set.of();
    }
    Set<UUID> publishFrozen(){
        stable(true);if(published)return Set.of();if(!installed)throw new IllegalStateException("Install Hedgehog encounters before publication");
        game.items().godHedgehog().publishFrozen();game.items().hedgehog().publishFrozen();
        prepared=false;published=true;return Set.of();
    }
    void cancelFrozen(){
        if(!game.server().isSameThread()||!game.server().tickRateManager().isFrozen())
            throw new IllegalStateException("Cancel Hedgehog replay on the frozen native thread");
        game.items().godHedgehog().cancelFrozen();game.items().hedgehog().cancelFrozen();
        prepared=false;installed=false;published=false;
    }
}
