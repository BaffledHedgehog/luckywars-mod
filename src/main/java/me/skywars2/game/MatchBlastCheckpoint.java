package me.skywars2.game;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;

/** Reviewed blast initializer/state graph; no introspection of arbitrary Consumer closures. */
final class MatchBlastCheckpoint {
    interface Binding extends java.util.function.Consumer<PrimedTnt> { void checkpoint(Graph graph); }
    static final class Graph {
        final MatchSpellCheckpoint.Links links;
        private final IdentityHashMap<Object,MatchQueuedWorkCheckpoint.Image> values=new IdentityHashMap<>();
        private final List<MatchQueuedWorkCheckpoint.Image> order=new ArrayList<>();
        Graph(MatchDamageSources sources){links=new MatchSpellCheckpoint.Links(sources);}
        void remember(Object key,Supplier<MatchQueuedWorkCheckpoint.Image> capture){if(values.containsKey(key))return;var image=Objects.requireNonNull(capture.get());values.put(key,image);order.add(image);}
        void binding(java.util.function.Consumer<PrimedTnt> value){if(value==null)return;if(!(value instanceof Binding typed))throw new IllegalStateException("Missing typed blast initializer checkpoint");typed.checkpoint(this);}
        void trial(LbcAcademy.BlastTrial trial){remember(trial,()->{var targets=Set.copyOf(trial.targets);boolean completed=trial.completed;return links->()->{trial.targets.clear();trial.targets.addAll(targets);trial.completed=completed;};});}
        Runnable prepare(){var plans=order.stream().map(image->image.prepare(links)).toList();return ()->plans.forEach(Runnable::run);}
    }
    private static final class AcademyBinding implements Binding {
        final GameController game;final LbcAcademy.BlastTrial trial;
        AcademyBinding(GameController game,LbcAcademy.BlastTrial trial){this.game=game;this.trial=trial;}
        public void accept(PrimedTnt source){game.items().academy().blastSource(source,trial);}
        public void checkpoint(Graph graph){graph.trial(trial);}
    }
    static java.util.function.Consumer<PrimedTnt> academyBinding(GameController game,LbcAcademy.BlastTrial trial){return trial==null?null:new AcademyBinding(game,trial);}
    private final class AcademyImage {
        private final MatchSpellCheckpoint.IdentityImage<Entity,LbcAcademy.BlastTrial> ledger;
        @SuppressWarnings("unchecked") AcademyImage(Graph graph){var current=((me.skywars2.mixin.AcademyBlastCheckpointAccess)(Object)game.items().academy()).skywars2$blastSources();if(!(current instanceof IdentityHashMap<?,?>))throw new IllegalStateException("Unexpected academy blast ledger implementation");var values=(IdentityHashMap<Entity,LbcAcademy.BlastTrial>)(Map<?,?>)current;values.values().forEach(graph::trial);ledger=new MatchSpellCheckpoint.IdentityImage<>(values,graph.links);}
        Runnable prepare(MatchSpellCheckpoint.Links links){var restored=ledger.restore(links);return ()->((me.skywars2.mixin.AcademyBlastCheckpointAccess)(Object)game.items().academy()).skywars2$blastSources(restored);}
    }
    private final GameController game;private final String epoch;private final Graph graph;
    private final LbcExasperato.BlastCheckpoint first;private final LbcExoperso.Checkpoint second;private final AcademyImage academy;
    private boolean restoring;
    MatchBlastCheckpoint(GameController game,MatchDamageSources sources){this.game=game;epoch=game.items().epoch();stable(false);graph=new Graph(sources);first=game.items().exasperato().checkpointBlasts(graph);second=game.items().exoperso().checkpoint(graph);academy=new AcademyImage(graph);}
    private void stable(boolean frozen){if(!game.server().isSameThread()||!game.playing()||game.maps().busy()||!game.items().currentEpoch(epoch)||frozen&&!game.server().tickRateManager().isFrozen())throw new IllegalStateException("Restore blast state on its stable frozen match thread");game.items().exasperato().blastBoundary();game.items().exoperso().checkpointBoundary();game.items().spellSources().checkpointBoundary();}
    void prepareFrozen(){stable(true);game.items().exasperato().prepareBlastCheckpoint();game.items().exoperso().prepareCheckpoint();restoring=true;}
    Set<UUID> finishFrozen(Function<UUID,Entity> lookup){stable(true);if(!restoring)throw new IllegalStateException("Detach future blast state first");var pending=graph.links.resolve(lookup);if(!pending.isEmpty())return pending;var values=graph.prepare();var a=first.prepare(graph.links);var b=second.prepare(graph.links);var c=academy.prepare(graph.links);values.run();a.run();b.run();c.run();restoring=false;return Set.of();}
    void cancelFrozen(){if(!game.server().isSameThread()||!game.server().tickRateManager().isFrozen())throw new IllegalStateException("Cancel blasts on their frozen native thread");game.items().exasperato().cancelBlastCheckpoint();game.items().exoperso().cancelCheckpoint();restoring=false;}
}
