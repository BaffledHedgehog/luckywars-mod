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
    private final LbcExasperato.SearchCheckpoint searches;
    private final LbcWitchGluttony.Checkpoint gluttony;
    private final LbcHohlopitek.Checkpoint bombers;
    private final LbcReactive.Checkpoint reactive;
    private final LbcHoming.Checkpoint homing;
    private final LbcDroneDrops.Checkpoint drones;
    private boolean restoring;
    MatchQueuedWorkCheckpoint(GameController game,MatchDamageSources sources){
        this.game=game;epoch=game.items().epoch();stable(false);graph=new Graph(sources);
        gluttony=game.items().witchGluttony().checkpoint(graph);terrain=game.items().stonekivi().checkpoint(graph);
        bombers=game.items().hohlopitek().checkpoint(graph.links);reactive=game.items().reactive().checkpoint(graph.links);
        homing=game.items().homing().checkpoint(graph.links);drones=game.items().droneDrops().checkpoint(graph.links);
        searches=game.items().exasperato().checkpointSearches(graph.links);
    }
    private void stable(boolean frozen){if(!game.server().isSameThread()||!game.playing()||game.maps().busy()||!game.items().currentEpoch(epoch)||frozen&&!game.server().tickRateManager().isFrozen())throw new IllegalStateException("Restore shared jobs on a stable frozen match thread");
        game.items().stonekivi().checkpointBoundary();game.items().exasperato().searchBoundary();game.items().witchGluttony().checkpointBoundary();game.items().spellSources().checkpointBoundary();
        game.items().hohlopitek().checkpointBoundary();game.items().reactive().checkpointBoundary();game.items().homing().checkpointBoundary();game.items().droneDrops().checkpointBoundary();}
    void prepareFrozen(){stable(true);game.items().stonekivi().prepareCheckpoint();game.items().exasperato().prepareSearchCheckpoint();restoring=true;}
    Set<UUID> finishFrozen(Function<UUID,Entity> lookup){
        stable(true);if(!restoring)throw new IllegalStateException("Detach future shared jobs first");var pending=graph.links.resolve(lookup);if(!pending.isEmpty())return pending;
        var jobs=graph.prepare();var actors=gluttony.prepare(graph.links);var rays=searches.prepare(graph.links);var queue=terrain.prepare();
        var bomberPlan=bombers.prepare(graph.links);var wallPlan=reactive.prepare(graph.links);var homingPlan=homing.prepare(graph.links);var dronePlan=drones.prepare(graph.links);
        jobs.run();actors.run();bomberPlan.run();wallPlan.run();homingPlan.run();dronePlan.run();rays.run();queue.run();restoring=false;return Set.of();
    }
    void cancelFrozen(){if(!game.server().isSameThread()||!game.server().tickRateManager().isFrozen())throw new IllegalStateException("Cancel frozen job replay on its native thread");game.items().stonekivi().cancelCheckpoint();game.items().exasperato().cancelSearchCheckpoint();restoring=false;}
}
