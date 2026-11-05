package me.skywars2.game;

import java.util.*;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;

/** Explicit job adapters, not reflection over arbitrary callbacks or completed CompletableFutures. */
final class MatchQueuedWorkCheckpoint {
    interface Image { Runnable prepare(MatchSpellCheckpoint.Links links); }
    static final class Graph {
        final MatchSpellCheckpoint.Links links;
        private final IdentityHashMap<LbcStonekivi.Work,Image> images=new IdentityHashMap<>();
        private final Set<LbcStonekivi.Work> visiting=Collections.newSetFromMap(new IdentityHashMap<>());
        private final List<Image> ordered=new ArrayList<>();
        Graph(MatchDamageSources sources){links=new MatchSpellCheckpoint.Links(sources);}
        void capture(LbcStonekivi.Work work){
            if(images.containsKey(work))return;if(!visiting.add(work))throw new IllegalStateException("Cyclic shared work graph");
            try{var image=Objects.requireNonNull(work.checkpoint(this));images.put(work,image);ordered.add(image);}finally{visiting.remove(work);}
        }
        Runnable prepare(){var values=ordered.stream().map(image->image.prepare(links)).toList();return ()->values.forEach(Runnable::run);}
    }
    private final GameController game;private final String epoch;private final Graph graph;
    private final LbcStonekivi.Checkpoint terrain;
    private final LbcBolts.Checkpoint bolts;
    private final LbcConsuming.Checkpoint consuming;private final LbcCorrosionDrops.Checkpoint corrosion;
    private final LbcMagicBean.Checkpoint beans;private final LbcTotems.Checkpoint totems;
    private final LbcExasperato.SearchCheckpoint searches;
    private final Image balance;
    private final LbcPozdmanBot.Checkpoint pozdman;
    private final LbcWitchGluttony.Checkpoint gluttony;
    private final LbcHohlopitek.Checkpoint bombers;
    private final LbcReactive.Checkpoint reactive;
    private final LbcReactiveOffense.Checkpoint trinkets;
    private final LbcPsychblindSkill.Checkpoint psychblind;
    private final LbcProjectileLevelups.Checkpoint projectileLevelups;
    private final LbcBadApple.Checkpoint badApple;
    private final LbcBridgeBuilder.Checkpoint bridgeBuilder;
    private final LbcImbaAxe.Checkpoint imbaAxe;
    private final LbcAbsoluteCinema.Checkpoint cinema;
    private final LbcPhoenixSkill.Checkpoint phoenix;
    private final LbcCosmetics.Checkpoint cosmetics;
    private final Image mellstroy;
    private final Image imbaHoe;
    private final Image diamondifier;
    private final Image funnyTnt,deltaTnt,specialFireballs;
    private final Image cunny,cunnyTerrain,cloneSelection,imbaTerrain,imbaTeleport,whiteArrows;
    private final LbcMellstroyCrafting.Snapshot mellstroyCrafting;
    private final LbcHoming.Checkpoint homing;
    private final LbcDroneDrops.Checkpoint drones;
    private final LbcAdvancedSnow.Checkpoint snow;private final LbcLibraryRandom.Checkpoint library;private final LbcChargedSwords.Checkpoint charged;
    private boolean presentationPending;
    private boolean restoring;
    MatchQueuedWorkCheckpoint(GameController game,MatchDamageSources sources){
        this.game=game;epoch=game.items().epoch();stable(false);graph=new Graph(sources);
        funnyTnt=game.items().funnyTnt().checkpoint(graph);deltaTnt=game.items().deltaTnt().checkpoint(graph);specialFireballs=game.items().specialFireballs().checkpoint(graph);diamondifier=game.items().diamondifier().checkpoint(graph);imbaHoe=game.items().imbaHoe().checkpoint(graph.links);
        imbaTerrain=game.items().imbaTerrainUtilities().checkpoint(graph);imbaTeleport=game.items().teleportGadgets().checkpoint(graph);whiteArrows=game.items().whiteArrows().checkpoint(graph.links);cosmetics=game.items().cosmetics().checkpoint(graph);mellstroy=game.items().mellstroy().checkpoint(graph.links);cunny=game.items().cunny().checkpoint(graph.links);cunnyTerrain=game.items().cunnyTerrain().checkpoint(graph);cloneSelection=game.items().cloneSkills().checkpoint();mellstroyCrafting=game.items().mellstroyCrafting().snapshot();
        imbaAxe=game.items().imbaAxe().checkpoint(graph);cinema=game.items().cinema().checkpoint(graph);phoenix=game.items().phoenix().checkpoint(graph);
        trinkets=game.items().reactiveTrinkets().offense().checkpoint(graph);psychblind=game.items().psychblindSkill().checkpoint(graph.links);projectileLevelups=game.items().projectileLevelups().checkpoint();badApple=game.items().badApple().checkpoint(graph);bridgeBuilder=game.items().bridgeBuilder().checkpoint(graph);
        consuming=game.items().consuming().checkpoint(graph);corrosion=game.items().corrosionDrops().checkpoint(graph);
        beans=game.items().magicBean().checkpoint(graph);totems=game.items().totems().checkpoint(graph);
        bolts=game.items().bolts().checkpoint(graph);gluttony=game.items().witchGluttony().checkpoint(graph);terrain=game.items().stonekivi().checkpoint(graph);
        bombers=game.items().hohlopitek().checkpoint(graph.links);reactive=game.items().reactive().checkpoint(graph.links);
        homing=game.items().homing().checkpoint(graph.links);drones=game.items().droneDrops().checkpoint(graph.links);
        snow=game.items().advancedSnow().checkpoint(graph.links);library=game.items().libraryRandom().checkpoint(graph.links);charged=game.items().chargedSwords().checkpoint(graph.links);
        balance=game.items().balance().checkpoint(graph.links);pozdman=game.items().pozdmanBot().checkpoint(graph.links);
        searches=game.items().exasperato().checkpointSearches(graph.links);
    }
    private void stable(boolean frozen){if(!game.server().isSameThread()||!game.playing()||game.maps().busy()||!game.items().currentEpoch(epoch)||frozen&&!game.server().tickRateManager().isFrozen())throw new IllegalStateException("Restore shared jobs on a stable frozen match thread");
        game.items().funnyTnt().checkpointBoundary();game.items().deltaTnt().checkpointBoundary();game.items().specialFireballs().checkpointBoundary();game.items().diamondifier().checkpointBoundary();game.items().imbaHoe().checkpointBoundary();game.items().imbaTerrainUtilities().checkpointBoundary();game.items().teleportGadgets().checkpointBoundary();game.items().whiteArrows().checkpointBoundary();game.items().cunny().checkpointBoundary();game.items().cunnyTerrain().checkpointBoundary();game.items().mellstroy().checkpointBoundary();game.items().cosmetics().boundary();game.items().badApple().checkpointBoundary();game.items().bridgeBuilder().checkpointBoundary();game.items().imbaAxe().checkpointBoundary();game.items().cinema().boundary();game.items().projectileLevelups().checkpointBoundary();game.items().pozdmanBot().checkpointBoundary();game.items().stonekivi().checkpointBoundary();game.items().exasperato().searchBoundary();game.items().witchGluttony().checkpointBoundary();game.items().spellSources().checkpointBoundary();
        game.items().hohlopitek().checkpointBoundary();game.items().reactive().checkpointBoundary();game.items().homing().checkpointBoundary();game.items().droneDrops().checkpointBoundary();game.items().advancedSnow().checkpointBoundary();game.items().libraryRandom().checkpointBoundary();game.items().chargedSwords().checkpointBoundary();game.items().bolts().checkpointBoundary();game.items().consuming().checkpointBoundary();game.items().corrosionDrops().checkpointBoundary();game.items().magicBean().checkpointBoundary();game.items().totems().checkpointBoundary();}
    void prepareFrozen(){stable(true);game.items().funnyTnt().prepareFrozen();game.items().deltaTnt().prepareFrozen();game.items().specialFireballs().prepareFrozen();game.items().diamondifier().prepareFrozen();game.items().imbaHoe().prepareFrozen();game.items().imbaTerrainUtilities().prepareFrozen();game.items().teleportGadgets().prepareFrozen();game.items().whiteArrows().prepareFrozen();game.items().cunny().prepareFrozen();game.items().cunnyTerrain().prepareFrozen();game.items().mellstroy().prepareFrozen();game.items().cosmetics().prepareFrozen();game.items().badApple().prepareFrozen();game.items().bridgeBuilder().prepareFrozen();game.items().imbaAxe().prepareFrozen();game.items().cinema().prepareFrozen();game.items().phoenix().prepareFrozen();game.items().projectileLevelups().prepareFrozen();game.items().psychblindSkill().prepareFrozen();game.items().reactiveTrinkets().offense().prepareFrozen();game.items().rasengan().stop();game.items().chargedBows().prepareFrozen();game.items().balance().stop();game.items().pozdmanBot().prepareFrozen();game.items().stonekivi().prepareCheckpoint();game.items().exasperato().prepareSearchCheckpoint();game.items().chargedSwords().prepareFrozen();game.items().bolts().prepareFrozen();game.items().consuming().prepareFrozen();game.items().magicBean().prepareFrozen();game.items().totems().prepareFrozen();presentationPending=true;restoring=true;}
    Set<UUID> finishFrozen(Function<UUID,Entity> lookup){
        stable(true);if(!restoring)throw new IllegalStateException("Detach future shared jobs first");var pending=graph.links.resolve(lookup);if(!pending.isEmpty())return pending;
        var funnyPlan=funnyTnt.prepare(graph.links);var deltaPlan=deltaTnt.prepare(graph.links);var specialPlan=specialFireballs.prepare(graph.links);var diamondPlan=diamondifier.prepare(graph.links);var imbaHoePlan=imbaHoe.prepare(graph.links);
        var imbaTerrainPlan=imbaTerrain.prepare(graph.links);var imbaTeleportPlan=imbaTeleport.prepare(graph.links);var whiteArrowPlan=whiteArrows.prepare(graph.links);var cunnyPlan=cunny.prepare(graph.links);var cunnyTerrainPlan=cunnyTerrain.prepare(graph.links);var clonePlan=cloneSelection.prepare(graph.links);var mellstroyPlan=mellstroy.prepare(graph.links);var cosmeticsPlan=cosmetics.prepare(graph.links);var badApplePlan=badApple.prepare(graph.links);var bridgePlan=bridgeBuilder.prepare(graph.links);var psychPlan=psychblind.prepare(graph.links);
        var trinketPlan=trinkets.prepare(graph.links);var axePlan=imbaAxe.prepare();var cinemaPlan=cinema.prepare(graph.links);var phoenixPlan=phoenix.prepare(graph.links);
        var consumePlan=consuming.prepare(graph.links);var corrosionPlan=corrosion.prepare(graph.links);var beanPlan=beans.prepare(graph.links);var totemPlan=totems.prepare(graph.links);
        var boltPlan=bolts.prepare(graph.links);var jobs=graph.prepare();var actors=gluttony.prepare(graph.links);var rays=searches.prepare(graph.links);var queue=terrain.prepare();
        var bomberPlan=bombers.prepare(graph.links);var wallPlan=reactive.prepare(graph.links);var homingPlan=homing.prepare(graph.links);var dronePlan=drones.prepare(graph.links);
        var pozdmanPlan=pozdman.prepare(graph.links);var balancePlan=balance.prepare(graph.links);var snowPlan=snow.prepare(graph.links);var libraryPlan=library.prepare(graph.links);var chargedPlan=charged.prepare(graph.links);
        jobs.run();funnyPlan.run();deltaPlan.run();specialPlan.run();diamondPlan.run();imbaHoePlan.run();imbaTerrainPlan.run();imbaTeleportPlan.run();whiteArrowPlan.run();axePlan.run();cinemaPlan.run();phoenixPlan.run();cosmeticsPlan.run();badApplePlan.run();bridgePlan.run();game.items().projectileLevelups().restoreFrozen(projectileLevelups);psychPlan.run();trinketPlan.run();balancePlan.run();pozdmanPlan.run();consumePlan.run();corrosionPlan.run();beanPlan.run();totemPlan.run();boltPlan.run();actors.run();snowPlan.run();libraryPlan.run();chargedPlan.run();bomberPlan.run();wallPlan.run();homingPlan.run();dronePlan.run();rays.run();queue.run();cunnyPlan.run();cunnyTerrainPlan.run();clonePlan.run();mellstroyPlan.run();game.items().mellstroyCrafting().restore(mellstroyCrafting);restoring=false;return Set.of();
    }
    Set<UUID> publishFrozen(){stable(true);if(!presentationPending)return Set.of();var pending=game.items().chargedSwords().publishFrozen();if(pending.isEmpty()){game.items().funnyTnt().publishFrozen();game.items().deltaTnt().publishFrozen();game.items().specialFireballs().publishFrozen();game.items().diamondifier().publishFrozen();game.items().imbaHoe().publishFrozen();game.items().imbaTerrainUtilities().publishFrozen();game.items().teleportGadgets().publishFrozen();game.items().whiteArrows().publishFrozen();game.items().cunny().publishFrozen();game.items().cunnyTerrain().publishFrozen();game.items().mellstroy().publishFrozen();game.items().cosmetics().publishFrozen();game.items().bolts().publishFrozen();game.items().consuming().publishFrozen();game.items().magicBean().publishFrozen();game.items().totems().publishFrozen();game.items().pozdmanBot().publishFrozen();drones.rebaseFrozen();snow.rebaseFrozen();game.items().psychblindSkill().publishFrozen();game.items().projectileLevelups().publishFrozen();game.items().badApple().publishFrozen();game.items().bridgeBuilder().publishFrozen();game.items().imbaAxe().publishFrozen();game.items().cinema().publishFrozen();game.items().phoenix().publishFrozen();game.items().reactiveTrinkets().offense().publishFrozen();presentationPending=false;}return pending;}
    void cancelFrozen(){if(!game.server().isSameThread()||!game.server().tickRateManager().isFrozen())throw new IllegalStateException("Cancel frozen job replay on its native thread");game.items().funnyTnt().cancelFrozen();game.items().deltaTnt().cancelFrozen();game.items().specialFireballs().cancelFrozen();game.items().diamondifier().cancelFrozen();game.items().imbaHoe().cancelFrozen();game.items().imbaTerrainUtilities().cancelFrozen();game.items().teleportGadgets().cancelFrozen();game.items().whiteArrows().cancelFrozen();game.items().cunny().cancelFrozen();game.items().cunny().stop();game.items().cunnyTerrain().cancelFrozen();game.items().mellstroy().cancelFrozen();game.items().cosmetics().cancelFrozen();game.items().badApple().stop();game.items().badApple().cancelFrozen();game.items().bridgeBuilder().stop();game.items().bridgeBuilder().cancelFrozen();game.items().imbaAxe().cancelFrozen();game.items().cinema().stop();game.items().cinema().cancelFrozen();game.items().phoenix().stop();game.items().phoenix().cancelFrozen();game.items().projectileLevelups().stop();game.items().projectileLevelups().publishFrozen();game.items().psychblindSkill().stop();game.items().psychblindSkill().publishFrozen();game.items().reactiveTrinkets().offense().cancelFrozen();game.items().stonekivi().cancelCheckpoint();game.items().exasperato().cancelSearchCheckpoint();game.items().chargedSwords().cancelFrozen();game.items().bolts().cancelFrozen();game.items().consuming().cancelFrozen();game.items().magicBean().cancelFrozen();game.items().totems().cancelFrozen();game.items().pozdmanBot().publishFrozen();presentationPending=false;restoring=false;}
}
