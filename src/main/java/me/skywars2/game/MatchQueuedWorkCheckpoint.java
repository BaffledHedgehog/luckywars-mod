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
    private final LbcHoming.Checkpoint homing;
    private final LbcDroneDrops.Checkpoint drones;
    private final LbcAdvancedSnow.Checkpoint snow;private final LbcLibraryRandom.Checkpoint library;private final LbcChargedSwords.Checkpoint charged;
    private boolean presentationPending;
    private boolean restoring;
    MatchQueuedWorkCheckpoint(GameController game,MatchDamageSources sources){
        this.game=game;epoch=game.items().epoch();stable(false);graph=new Graph(sources);
        trinkets=game.items().reactiveTrinkets().offense().checkpoint(graph);
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
        game.items().pozdmanBot().checkpointBoundary();game.items().stonekivi().checkpointBoundary();game.items().exasperato().searchBoundary();game.items().witchGluttony().checkpointBoundary();game.items().spellSources().checkpointBoundary();
        game.items().hohlopitek().checkpointBoundary();game.items().reactive().checkpointBoundary();game.items().homing().checkpointBoundary();game.items().droneDrops().checkpointBoundary();game.items().advancedSnow().checkpointBoundary();game.items().libraryRandom().checkpointBoundary();game.items().chargedSwords().checkpointBoundary();game.items().bolts().checkpointBoundary();game.items().consuming().checkpointBoundary();game.items().corrosionDrops().checkpointBoundary();game.items().magicBean().checkpointBoundary();game.items().totems().checkpointBoundary();}
    void prepareFrozen(){stable(true);game.items().reactiveTrinkets().offense().prepareFrozen();game.items().rasengan().stop();game.items().chargedBows().prepareFrozen();game.items().balance().stop();game.items().pozdmanBot().prepareFrozen();game.items().stonekivi().prepareCheckpoint();game.items().exasperato().prepareSearchCheckpoint();game.items().chargedSwords().prepareFrozen();game.items().bolts().prepareFrozen();game.items().consuming().prepareFrozen();game.items().magicBean().prepareFrozen();game.items().totems().prepareFrozen();presentationPending=true;restoring=true;}
    Set<UUID> finishFrozen(Function<UUID,Entity> lookup){
        stable(true);if(!restoring)throw new IllegalStateException("Detach future shared jobs first");var pending=graph.links.resolve(lookup);if(!pending.isEmpty())return pending;
        var trinketPlan=trinkets.prepare(graph.links);
        var consumePlan=consuming.prepare(graph.links);var corrosionPlan=corrosion.prepare(graph.links);var beanPlan=beans.prepare(graph.links);var totemPlan=totems.prepare(graph.links);
        var boltPlan=bolts.prepare(graph.links);var jobs=graph.prepare();var actors=gluttony.prepare(graph.links);var rays=searches.prepare(graph.links);var queue=terrain.prepare();
        var bomberPlan=bombers.prepare(graph.links);var wallPlan=reactive.prepare(graph.links);var homingPlan=homing.prepare(graph.links);var dronePlan=drones.prepare(graph.links);
        var pozdmanPlan=pozdman.prepare(graph.links);var balancePlan=balance.prepare(graph.links);var snowPlan=snow.prepare(graph.links);var libraryPlan=library.prepare(graph.links);var chargedPlan=charged.prepare(graph.links);
        jobs.run();trinketPlan.run();balancePlan.run();pozdmanPlan.run();consumePlan.run();corrosionPlan.run();beanPlan.run();totemPlan.run();boltPlan.run();actors.run();snowPlan.run();libraryPlan.run();chargedPlan.run();bomberPlan.run();wallPlan.run();homingPlan.run();dronePlan.run();rays.run();queue.run();restoring=false;return Set.of();
    }
    Set<UUID> publishFrozen(){stable(true);if(!presentationPending)return Set.of();var pending=game.items().chargedSwords().publishFrozen();if(pending.isEmpty()){game.items().bolts().publishFrozen();game.items().consuming().publishFrozen();game.items().magicBean().publishFrozen();game.items().totems().publishFrozen();game.items().pozdmanBot().publishFrozen();drones.rebaseFrozen();snow.rebaseFrozen();game.items().reactiveTrinkets().offense().publishFrozen();presentationPending=false;}return pending;}
    void cancelFrozen(){if(!game.server().isSameThread()||!game.server().tickRateManager().isFrozen())throw new IllegalStateException("Cancel frozen job replay on its native thread");game.items().reactiveTrinkets().offense().cancelFrozen();game.items().stonekivi().cancelCheckpoint();game.items().exasperato().cancelSearchCheckpoint();game.items().chargedSwords().cancelFrozen();game.items().bolts().cancelFrozen();game.items().consuming().cancelFrozen();game.items().magicBean().cancelFrozen();game.items().totems().cancelFrozen();game.items().pozdmanBot().publishFrozen();presentationPending=false;restoring=false;}
}
