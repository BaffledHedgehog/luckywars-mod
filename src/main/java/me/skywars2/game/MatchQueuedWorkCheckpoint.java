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
    private boolean restoring;
    MatchQueuedWorkCheckpoint(GameController game,MatchDamageSources sources){
        this.game=game;epoch=game.items().epoch();stable(false);graph=new Graph(sources);
        gluttony=game.items().witchGluttony().checkpoint(graph);terrain=game.items().stonekivi().checkpoint(graph);
        searches=game.items().exasperato().checkpointSearches(graph.links);
    }
    private void stable(boolean frozen){if(!game.server().isSameThread()||!game.playing()||game.maps().busy()||!game.items().currentEpoch(epoch)||frozen&&!game.server().tickRateManager().isFrozen())throw new IllegalStateException("Restore shared jobs on a stable frozen match thread");
        game.items().stonekivi().checkpointBoundary();game.items().exasperato().searchBoundary();game.items().witchGluttony().checkpointBoundary();game.items().spellSources().checkpointBoundary();}
    void prepareFrozen(){stable(true);game.items().stonekivi().prepareCheckpoint();game.items().exasperato().prepareSearchCheckpoint();restoring=true;}
    Set<UUID> finishFrozen(Function<UUID,Entity> lookup){
        stable(true);if(!restoring)throw new IllegalStateException("Detach future shared jobs first");var pending=graph.links.resolve(lookup);if(!pending.isEmpty())return pending;
        var jobs=graph.prepare();var actors=gluttony.prepare(graph.links);var rays=searches.prepare(graph.links);var queue=terrain.prepare();
        jobs.run();actors.run();rays.run();queue.run();restoring=false;return Set.of();
    }
    void cancelFrozen(){if(!game.server().isSameThread()||!game.server().tickRateManager().isFrozen())throw new IllegalStateException("Cancel frozen job replay on its native thread");game.items().stonekivi().cancelCheckpoint();game.items().exasperato().cancelSearchCheckpoint();restoring=false;}
}
