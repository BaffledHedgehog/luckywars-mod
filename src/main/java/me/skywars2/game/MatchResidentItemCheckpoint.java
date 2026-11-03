package me.skywars2.game;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;

/** Persistent item authorities; native player/actor images own all effects, values and equipment. */
final class MatchResidentItemCheckpoint {
    private final GameController game;
    private final String epoch;
    private final MatchSpellCheckpoint.Links links;
    private final LbcItems.ResidentCheckpoint items;
    private final LbcTechnoFlight.ResidentCheckpoint flight;
    private final LbcKfcFood.ResidentCheckpoint food;
    private final LbcBabylonEffects.ResidentCheckpoint babylon;
    private final LbcStand.ResidentCheckpoint stand;
    private final LbcEternalWealth.Checkpoint wealth;
    private boolean prepared,installed,published;
    MatchResidentItemCheckpoint(GameController game,MatchDamageSources sources){
        this.game=game;epoch=game.items().epoch();stable(false);links=new MatchSpellCheckpoint.Links(sources);
        items=game.items().residentCheckpoint(links);flight=game.items().armorSets().techno().flight().residentCheckpoint(links);
        food=game.items().kfcFood().residentCheckpoint(links);babylon=game.items().babylonEffects().residentCheckpoint(links);stand=game.items().stand().residentCheckpoint(links);
        wealth=game.items().eternalWealth().checkpoint();
    }
    private void stable(boolean frozen){
        if(!game.server().isSameThread()||!game.playing()||game.maps().busy()||!game.items().currentEpoch(epoch)
                ||frozen&&!game.server().tickRateManager().isFrozen())throw new IllegalStateException("Restore resident item state on the stable frozen match thread");
    }
    void prepareFrozen(){
        stable(true);game.items().prepareResidentFrozen();game.items().armorSets().techno().flight().prepareResidentFrozen();
        game.items().kfcFood().prepareResidentFrozen();game.items().babylonEffects().prepareResidentFrozen();game.items().stand().prepareResidentFrozen();
        game.items().eternalWealth().prepareFrozen();
        prepared=true;installed=false;published=false;
    }
    Set<UUID> restoreFrozen(Function<UUID,Entity> lookup){
        stable(true);if(!prepared)prepareFrozen();var missing=links.resolve(lookup);if(!missing.isEmpty())return missing;
        var estate=wealth.prepare(lookup);if(!estate.pending().isEmpty())return estate.pending();
        var i=items.prepare(links);var f=flight.prepare(links);var k=food.prepare(links);var b=babylon.prepare(links);var s=stand.prepare(links);
        i.run();f.run();k.run();b.run();s.run();estate.install().run();installed=true;published=false;return Set.of();
    }
    Set<UUID> publishFrozen(){
        stable(true);if(published)return Set.of();if(!installed)throw new IllegalStateException("Install resident item state before publication");
        game.items().publishResidentFrozen();game.items().eternalWealth().publishFrozen();published=true;prepared=false;return Set.of();
    }
}
