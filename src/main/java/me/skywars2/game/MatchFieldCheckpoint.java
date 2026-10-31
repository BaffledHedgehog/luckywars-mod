package me.skywars2.game;

import java.util.*;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;

/** Essential Soulless/Metasola and moving Antigravity fields; native state has its existing canonical owner. */
final class MatchFieldCheckpoint {
    record Plan(Set<UUID> pending,Runnable install) { }
    private final GameController game;
    private final String epoch;
    private final LbcSoulless.Checkpoint souls;
    private final LbcAntigravity.Checkpoint gravity;
    private Set<UUID> pending=Set.of();
    private boolean prepared;
    MatchFieldCheckpoint(GameController game,MatchDamageSources sources){
        this.game=game;epoch=game.items().epoch();stable(false);souls=game.items().soulless().checkpoint(sources);gravity=game.items().antigravity().checkpoint(sources);prepared=true;
    }
    private MatchFieldCheckpoint(GameController game,String epoch,LbcSoulless.Checkpoint souls,LbcAntigravity.Checkpoint gravity){this.game=game;this.epoch=epoch;this.souls=souls;this.gravity=gravity;stable(false);}
    private void stable(boolean frozen){
        if(!game.server().isSameThread()||!game.playing()||game.maps().busy()||!game.items().currentEpoch(epoch)||frozen&&!game.server().tickRateManager().isFrozen())throw new IllegalStateException("Restore long-lived fields on the same stable frozen match thread");
        game.items().soulless().checkpointBoundary();game.items().antigravity().checkpointBoundary();
    }
    static boolean captureReady(GameController game){return game.items().soulless().captureReady()&&game.items().antigravity().captureReady();}
    Set<UUID> actorIds(){var all=new LinkedHashSet<UUID>();all.addAll(souls.actorIds());if(gravity!=null)all.addAll(gravity.actorIds());return Set.copyOf(all);}
    void prepareFrozen(){stable(true);game.items().soulless().prepareFrozen();if(gravity!=null)game.items().antigravity().prepareFrozen();souls.reset();if(gravity!=null)gravity.reset();prepared=true;pending=Set.of();}
    Set<UUID> finishFrozen(Function<UUID,Entity> lookup){
        stable(true);if(!prepared)throw new IllegalStateException("Prepare long-lived fields before native links");var missing=new LinkedHashSet<UUID>();
        var anti=gravity==null?new Plan(Set.of(),()->{}):gravity.prepareAvailable(lookup);var soul=souls.prepareAvailable(lookup);
        missing.addAll(anti.pending());missing.addAll(soul.pending());anti.install().run();soul.install().run();pending=Set.copyOf(missing);return pending;
    }
    /** Existing native actor pairing owns all displays; no launch, carry impulse, damage, healing or deployment runs. */
    void publishFrozen(){stable(true);game.items().soulless().publishFrozen();if(gravity!=null)game.items().antigravity().publishFrozen();}
    boolean hasPending(){return souls.hasPending()||gravity!=null&&gravity.hasPending();}
    Set<UUID> pending(){return pending;}
    MatchFieldCheckpoint pendingOnly(){
        stable(false);var result=new MatchFieldCheckpoint(game,epoch,souls.pendingOnly(),gravity!=null&&gravity.hasPending()?gravity:null);
        result.prepared=true;result.pending=pending;return result;
    }
    MatchFieldCheckpoint withPending(MatchFieldCheckpoint previous){
        stable(false);if(previous.pending.isEmpty())return this;
        var result=new MatchFieldCheckpoint(game,epoch,souls.withPending(previous.souls),gravity);result.pending=previous.pending;result.prepared=true;return result;
    }
    void cancelFrozen(){stable(true);game.items().soulless().cancelFrozen();if(gravity!=null)game.items().antigravity().cancelFrozen();prepared=false;pending=Set.of();}
}
