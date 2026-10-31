package me.skywars2.game;

import java.util.*;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;

/** Reusable scope macro-state; only the actual login body and its restored native hand can resume a row. */
final class MatchAimCheckpoint {
    private final GameController game;
    private final String epoch;
    private final LbcRifleZoom service;
    private final List<LbcRifleZoom.ScopeCheckpoint> rows;
    private final Set<LbcRifleZoom.ScopeCheckpoint> installed=Collections.newSetFromMap(new IdentityHashMap<>());
    private Set<UUID> pending=Set.of();
    private boolean prepared;
    MatchAimCheckpoint(GameController game,MatchDamageSources sources){this.game=game;epoch=game.items().epoch();service=game.items().rifleZoom();stable(false);rows=service.checkpointScopes(sources);installed.addAll(rows);prepared=true;}
    private MatchAimCheckpoint(GameController game,String epoch,List<LbcRifleZoom.ScopeCheckpoint> rows){this.game=game;this.epoch=epoch;service=game.items().rifleZoom();this.rows=List.copyOf(rows);stable(false);}
    private void stable(boolean frozen){
        if(!game.server().isSameThread()||!game.playing()||game.maps().busy()||!game.items().currentEpoch(epoch)||frozen&&!game.server().tickRateManager().isFrozen())throw new IllegalStateException("Restore scope state on the same stable frozen match thread");
        service.checkpointBoundary();
    }
    void prepareFrozen(){stable(true);service.prepareCheckpointFrozen();installed.clear();pending=Set.of();prepared=true;}
    Set<UUID> finishFrozen(Function<UUID,Entity> lookup){
        stable(true);if(!prepared)throw new IllegalStateException("Prepare scope state before native links");var missing=new LinkedHashSet<UUID>();
        var ready=new ArrayList<LbcRifleZoom.ScopeCheckpoint>();var plans=new ArrayList<Runnable>();
        for(var row:rows)if(!installed.contains(row)){var absent=row.resolve(lookup);if(absent.isEmpty()){ready.add(row);plans.add(row.prepare());}else missing.addAll(absent);}
        plans.forEach(Runnable::run);installed.addAll(ready);pending=Set.copyOf(missing);return pending;
    }
    void publishFrozen(){stable(true);if(!prepared)throw new IllegalStateException("Install scope state before publication");service.publishCheckpointFrozen();}
    Set<UUID> pending(){return pending;}
    boolean hasPending(){return installed.size()!=rows.size();}
    MatchAimCheckpoint pendingOnly(){stable(false);var image=new MatchAimCheckpoint(game,epoch,rows.stream().filter(row->!installed.contains(row)).toList());image.prepared=prepared;image.pending=pending;return image;}
    MatchAimCheckpoint withPending(MatchAimCheckpoint previous){
        stable(false);if(previous.pending.isEmpty())return this;
        var combined=new ArrayList<>(rows);for(var row:previous.rows)if(!previous.installed.contains(row))combined.add(row);
        var result=new MatchAimCheckpoint(game,epoch,combined);result.installed.addAll(installed);result.pending=previous.pending;result.prepared=true;return result;
    }
    void cancelFrozen(){stable(true);service.cancelCheckpointFrozen();installed.clear();pending=Set.of();prepared=false;}
}
