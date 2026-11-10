package me.skywars2.game;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.entity.Entity;

/** Explicitly omitted short spell actors; long producers and all checkpointed owners stay separate. */
final class MatchTransientSpells {
    private MatchTransientSpells(){}

    /** Unsaved long rooms, trials and producers must finish before a coherent point can be recorded. */
    static boolean captureReady(GameController game){
        return game.items().bedrock().transientCaptureReady()&&game.items().gaycasterAcademy().transientCaptureReady()
                &&game.items().blind().transientCaptureReady()&&game.items().teleport().cloudCount()==0
                &&game.items().erecto().sphereCount()==0;
    }
    static Set<UUID> captureOmitted(GameController game){
        if(!game.server().isSameThread())throw new IllegalStateException("Inspect transient spell actors on their native match thread");
        var ids=new LinkedHashSet<UUID>();var items=game.items();
        add(ids,items.ruby().snapshot());add(ids,items.air().snapshot());add(ids,items.stone().snapshot());
        add(ids,items.blind().snapshot());add(ids,items.teleport().snapshot());add(ids,items.prism().transientSnapshot());
        add(ids,items.electro().transientSnapshot());add(ids,items.babylon().snapshot());add(ids,items.trinkets().transientSnapshot());
        add(ids,items.fartus().snapshot());for(var shot:items.fartus().snapshot())if(shot.display!=null)ids.add(shot.display.getUUID());
        add(ids,items.evokerBass().snapshot());
        return Set.copyOf(ids);
    }
    private static void add(Set<UUID> ids,Iterable<? extends Entity> actors){for(var actor:actors)ids.add(actor.getUUID());}

    /** Before native rollback. Unsaved long families are absent at capture; only abandoned future instances are stopped. */
    static void cancelFuture(GameController game){
        if(!game.server().isSameThread()||!game.server().tickRateManager().isFrozen())
            throw new IllegalStateException("Cancel abandoned short spells on the frozen native match thread");
        var items=game.items();items.wands().stop();items.electro().cancelTransient();
        items.ruby().stop();items.air().stop();items.stone().stop();items.prism().cancelTransient();
        items.blind().stop();items.teleport().stop();items.erecto().stop();items.bedrock().stop();items.gaycasterAcademy().stop();
        items.babylon().stop();items.trinkets().stop();
        items.fartus().cancelTransient();items.evokerBass().cancelTransient();
    }
}
