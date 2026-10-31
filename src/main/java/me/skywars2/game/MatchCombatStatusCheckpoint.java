package me.skywars2.game;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;

/** Finite custom status authority. Native effects/attributes, player data and terrain stay canonical. */
final class MatchCombatStatusCheckpoint {
    private final GameController game;
    private final String epoch;
    private final MatchSpellCheckpoint.Links links;
    private final LbcFrost.Checkpoint frost;
    private final LbcShooters.StatusCheckpoint stuns;
    private final LbcElectro.Checkpoint electro;
    private boolean prepared,installed,published;

    MatchCombatStatusCheckpoint(GameController game,MatchDamageSources sources){
        this.game=game;epoch=game.items().epoch();stable(false);links=new MatchSpellCheckpoint.Links(sources);
        frost=game.items().frost().checkpoint(links);stuns=game.items().shooters().checkpointStatuses(links);electro=game.items().electro().checkpoint(links);
    }
    private void stable(boolean frozen){
        if(!game.server().isSameThread()||!game.playing()||game.maps().busy()||!game.items().currentEpoch(epoch)
                ||frozen&&!game.server().tickRateManager().isFrozen())throw new IllegalStateException("Restore custom combat statuses on the same stable frozen match thread");
        game.items().frost().checkpointBoundary();game.items().shooters().checkpointStatusBoundary();game.items().electro().checkpointBoundary();
        game.items().spellSources().checkpointBoundary();
    }
    /** Before canonical body rebuilding: forget future bookkeeping without thaw/dispel/attribute writes. */
    void prepareFrozen(){
        stable(true);game.items().frost().prepareFrozen();game.items().shooters().prepareStatusFrozen();game.items().electro().prepareFrozen();
        prepared=true;installed=false;published=false;
    }
    Set<UUID> restoreFrozen(Function<UUID,Entity> lookup){
        stable(true);if(!prepared)prepareFrozen();var pending=links.resolve(lookup);if(!pending.isEmpty())return pending;
        var cold=frost.prepare(links);var stun=stuns.prepare(links);var electric=electro.prepare(links);
        cold.run();stun.run();electric.run();installed=true;published=false;return Set.of();
    }
    /** Rebase private timers only. Native canonical owners already published effects, motion and ice. */
    Set<UUID> publishFrozen(){
        stable(true);if(published)return Set.of();if(!installed)throw new IllegalStateException("Install custom combat statuses before publication");
        game.items().frost().publishFrozen();game.items().shooters().publishStatusFrozen();game.items().electro().publishFrozen();
        prepared=false;published=true;return Set.of();
    }
    void cancelFrozen(){
        if(!game.server().isSameThread()||!game.server().tickRateManager().isFrozen())throw new IllegalStateException("Cancel custom status replay on the frozen native thread");
        game.items().frost().cancelFrozen();game.items().shooters().cancelStatusFrozen();game.items().electro().cancelFrozen();
        prepared=false;installed=false;published=false;
    }
}
