package me.skywars2.game;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;

/** Chef macrostate only; native bodies, equipment, terrain and sources remain canonical. */
final class MatchChefCheckpoint {
    private final GameController game;
    private final String epoch;
    private final MatchSpellCheckpoint.Links links;
    private final LbcWtf.Checkpoint chefs;
    private boolean prepared,installed,published;

    MatchChefCheckpoint(GameController game,MatchDamageSources sources){
        this.game=game;epoch=game.items().epoch();stable(false);
        links=new MatchSpellCheckpoint.Links(sources);chefs=game.items().wtf().checkpoint(links);
    }
    private void stable(boolean frozen){
        if(!game.server().isSameThread()||!game.playing()||game.maps().busy()||!game.items().currentEpoch(epoch)
                ||frozen&&!game.server().tickRateManager().isFrozen())
            throw new IllegalStateException("Restore Chef macrostate on the same stable frozen match thread");
        game.items().wtf().checkpointBoundary();game.items().spellSources().checkpointBoundary();
    }
    /** Release future ticket ownership before native body reconstruction, without retiring actors. */
    void prepareFrozen(){
        stable(true);game.items().wtf().prepareFrozen();prepared=true;installed=false;published=false;
    }
    Set<UUID> restoreFrozen(Function<UUID,Entity> lookup){
        stable(true);if(!prepared)prepareFrozen();var pending=links.resolve(lookup);if(!pending.isEmpty())return pending;
        var plan=chefs.prepare(links);plan.run();installed=true;published=false;return Set.of();
    }
    Set<UUID> publishFrozen(){
        stable(true);if(published)return Set.of();if(!installed)throw new IllegalStateException("Install Chef macrostate before publication");
        game.items().wtf().publishFrozen();prepared=false;published=true;return Set.of();
    }
    void cancelFrozen(){
        if(!game.server().isSameThread()||!game.server().tickRateManager().isFrozen())
            throw new IllegalStateException("Cancel Chef replay on the frozen native thread");
        game.items().wtf().cancelFrozen();prepared=false;installed=false;published=false;
    }
}
