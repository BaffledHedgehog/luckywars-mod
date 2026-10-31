package me.skywars2.game;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;

/** Essential long-lived hole/academy-sheep state; native bodies, inventory and policies stay canonical. */
final class MatchBlackHoleCheckpoint {
    private final GameController game;
    private final String epoch;
    private final MatchSpellCheckpoint.Links links;
    private final LbcVoidArcanum.Checkpoint arcanum;
    private final LbcSheep.Checkpoint sheep;
    private boolean prepared,installed,published;

    MatchBlackHoleCheckpoint(GameController game,MatchDamageSources sources){
        this.game=game;epoch=game.items().epoch();stable(false);
        links=new MatchSpellCheckpoint.Links(sources);
        arcanum=game.items().voidArcanum().checkpoint(links);
        sheep=game.items().sheep().checkpoint(links);
    }
    private void stable(boolean frozen){
        if(!game.server().isSameThread()||!game.playing()||game.maps().busy()||!game.items().currentEpoch(epoch)
                ||frozen&&!game.server().tickRateManager().isFrozen())
            throw new IllegalStateException("Restore black holes on the same stable frozen match thread");
        game.items().voidArcanum().checkpointBoundary();game.items().sheep().checkpointBoundary();game.items().spellSources().checkpointBoundary();
    }
    /** Detach only these producers' future visuals before canonical native body replacement. */
    void prepareFrozen(){
        stable(true);game.items().voidArcanum().prepareFrozen();game.items().sheep().prepareFrozen();
        prepared=true;installed=false;published=false;
    }
    Set<UUID> restoreFrozen(Function<UUID,Entity> lookup){
        stable(true);if(!prepared)prepareFrozen();
        var pending=links.resolve(lookup);if(!pending.isEmpty())return pending;
        var voidPlan=arcanum.prepare(links);var sheepPlan=sheep.prepare(links);
        voidPlan.run();sheepPlan.run();installed=true;published=false;return Set.of();
    }
    /** No source selection RNG, factory, physics, erosion or conversion callback runs on publication. */
    Set<UUID> publishFrozen(){
        stable(true);if(published)return Set.of();if(!installed)throw new IllegalStateException("Install black-hole state before publication");
        var pending=new LinkedHashSet<UUID>();
        for(var player:game.server().getPlayerList().getPlayers())
            if(player.isChangingDimension()||!player.connection.hasClientLoaded())pending.add(player.getUUID());
        if(!pending.isEmpty())return Set.copyOf(pending);
        game.items().voidArcanum().publishFrozen();game.items().sheep().publishFrozen();game.items().blackHoleVisuals().publishFrozen();
        prepared=false;published=true;return Set.of();
    }
    void cancelFrozen(){
        if(!game.server().isSameThread()||!game.server().tickRateManager().isFrozen())
            throw new IllegalStateException("Cancel black-hole replay on its frozen native thread");
        game.items().voidArcanum().cancelFrozen();game.items().sheep().cancelFrozen();
        prepared=false;installed=false;published=false;
    }
}
