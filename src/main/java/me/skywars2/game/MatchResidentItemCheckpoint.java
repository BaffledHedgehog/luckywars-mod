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
    private final LbcNexusWires.Checkpoint wires;
    private final LbcNexusTools.Checkpoint nexus;
    private final LbcNexusPulsar.Checkpoint pulsar;
    private final LbcNexusAcquisition.Checkpoint acquisition;
    private final LbcSupernova.Checkpoint supernova;
    private final LbcChargedBowCrafting.Checkpoint chargedCrafting;
    private final LbcCursedPumpkin.Checkpoint pumpkins;
    private final LbcGodClawSkill.Checkpoint godClaw;
    private final LbcStoneGolemSkill.Checkpoint golem;
    private final LbcExpansionSkill.Checkpoint expansion;
    private final LbcHakureiSkill.Checkpoint hakurei;
    private final LbcTouhouCollectibles.Checkpoint touhou;
    private final LbcNovemberLightsabers.Checkpoint lightsabers;
    private final java.util.List<net.minecraft.world.phys.Vec3> compassDestinations;
    private boolean prepared,installed,published;
    MatchResidentItemCheckpoint(GameController game,MatchDamageSources sources){
        this.game=game;epoch=game.items().epoch();stable(false);links=new MatchSpellCheckpoint.Links(sources);
        godClaw=game.items().godClaw().checkpoint();golem=game.items().stoneGolemSkill().checkpoint();
        expansion=game.items().expansionSkill().checkpoint();hakurei=game.items().hakureiSkill().checkpoint();touhou=game.items().touhouCollectibles().checkpoint();
        pumpkins=game.items().cursedPumpkin().checkpoint();lightsabers=game.items().lightsabers().checkpoint();
        items=game.items().residentCheckpoint(links);flight=game.items().armorSets().techno().flight().residentCheckpoint(links);
        food=game.items().kfcFood().residentCheckpoint(links);babylon=game.items().babylonEffects().residentCheckpoint(links);stand=game.items().stand().residentCheckpoint(links);
        wealth=game.items().eternalWealth().checkpoint();wires=game.items().nexusWires().checkpoint();nexus=game.items().nexusTools().checkpoint();pulsar=game.items().nexusCombat().pulsar().checkpoint();acquisition=game.items().nexusAcquisition().checkpoint();supernova=game.items().supernova().checkpoint();chargedCrafting=game.items().chargedBows().crafting().checkpoint();compassDestinations=game.items().nexusCompass().checkpointDestinations();
    }
    private void stable(boolean frozen){
        if(!game.server().isSameThread()||!game.playing()||game.maps().busy()||!game.items().currentEpoch(epoch)
                ||frozen&&!game.server().tickRateManager().isFrozen())throw new IllegalStateException("Restore resident item state on the stable frozen match thread");
    }
    void prepareFrozen(){
        stable(true);game.items().expansionSkill().prepareFrozen();game.items().hakureiSkill().prepareFrozen();game.items().touhouCollectibles().prepareFrozen();game.items().godClaw().prepareFrozen();game.items().stoneGolemSkill().prepareFrozen();game.items().cursedPumpkin().prepareFrozen();game.items().lightsabers().prepareFrozen();game.items().prepareResidentFrozen();game.items().armorSets().techno().flight().prepareResidentFrozen();
        game.items().kfcFood().prepareResidentFrozen();game.items().babylonEffects().prepareResidentFrozen();game.items().stand().prepareResidentFrozen();
        game.items().eternalWealth().prepareFrozen();game.items().nexusWires().prepareFrozen();game.items().nexusTools().prepareFrozen();game.items().nexusCombat().prepareFrozen();game.items().nexusAcquisition().prepareFrozen();game.items().supernova().prepareFrozen();
        prepared=true;installed=false;published=false;
    }
    Set<UUID> restoreFrozen(Function<UUID,Entity> lookup){
        stable(true);if(!prepared)prepareFrozen();var missing=links.resolve(lookup);if(!missing.isEmpty())return missing;
        var estate=wealth.prepare(lookup);if(!estate.pending().isEmpty())return estate.pending();
        var wireState=wires.prepare(lookup);if(!wireState.pending().isEmpty())return wireState.pending();
        var nx=nexus.prepare(lookup);if(!nx.pending().isEmpty())return nx.pending();
        var pulse=pulsar.prepare(lookup);if(!pulse.pending().isEmpty())return pulse.pending();
        var acquired=acquisition.prepare(lookup);if(!acquired.pending().isEmpty())return acquired.pending();
        var craft=chargedCrafting.prepare(lookup);if(!craft.pending().isEmpty())return craft.pending();
        var expansionState=expansion.prepare(lookup);if(!expansionState.pending().isEmpty())return expansionState.pending();
        var hakureiState=hakurei.prepare(lookup);if(!hakureiState.pending().isEmpty())return hakureiState.pending();
        var touhouState=touhou.prepare(lookup);if(!touhouState.pending().isEmpty())return touhouState.pending();
        var clawState=godClaw.prepare(lookup);var golemState=golem.prepare(lookup);
        var pumpkinState=pumpkins.prepare(lookup);if(!pumpkinState.pending().isEmpty())return pumpkinState.pending();
        var lightState=lightsabers.prepare(lookup);
        var nova=supernova.prepare(lookup);if(!nova.pending().isEmpty())return nova.pending();
        var i=items.prepare(links);var f=flight.prepare(links);var k=food.prepare(links);var b=babylon.prepare(links);var s=stand.prepare(links);
        i.run();f.run();k.run();b.run();s.run();estate.install().run();wireState.install().run();nx.install().run();pulse.install().run();acquired.install().run();nova.install().run();craft.install().run();pumpkinState.install().run();clawState.install().run();golemState.install().run();expansionState.install().run();hakureiState.install().run();touhouState.install().run();lightState.install().run();game.items().nexusCompass().restoreDestinations(compassDestinations);installed=true;published=false;return Set.of();
    }
    Set<UUID> publishFrozen(){
        stable(true);if(published)return Set.of();if(!installed)throw new IllegalStateException("Install resident item state before publication");
        game.items().publishResidentFrozen();game.items().eternalWealth().publishFrozen();game.items().nexusWires().publishFrozen();game.items().nexusTools().publishFrozen();game.items().nexusCompass().publishFrozen();game.items().nexusCombat().publishFrozen();game.items().nexusAcquisition().publishFrozen();game.items().supernova().publishFrozen();game.items().cursedPumpkin().publishFrozen();game.items().godClaw().publishFrozen();game.items().stoneGolemSkill().publishFrozen();game.items().expansionSkill().publishFrozen();game.items().hakureiSkill().publishFrozen();game.items().touhouCollectibles().publishFrozen();game.items().lightsabers().publishFrozen();published=true;prepared=false;return Set.of();
    }
}
