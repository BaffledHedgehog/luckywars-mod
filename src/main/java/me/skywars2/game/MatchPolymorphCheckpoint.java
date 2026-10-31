package me.skywars2.game;

import java.util.*;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;

/** Essential reversible forms; separate per-form links let offline humans wait without stopping other players. */
final class MatchPolymorphCheckpoint {
    private final GameController game;
    private final LbcPolymorph service;
    private final String epoch;
    private final List<LbcPolymorph.FormCheckpoint> forms;
    private final Set<UUID> transientActors;
    private final Set<LbcPolymorph.FormCheckpoint> installed=Collections.newSetFromMap(new IdentityHashMap<>());
    private Set<UUID> pending=Set.of();
    private boolean prepared;
    MatchPolymorphCheckpoint(GameController game,MatchDamageSources sources){
        this.game=game;service=game.items().polymorph();epoch=game.items().epoch();stable(false);
        forms=service.checkpointForms(sources);transientActors=service.transientCheckpointActors();installed.addAll(forms);prepared=true;
    }
    private MatchPolymorphCheckpoint(GameController game,String epoch,List<LbcPolymorph.FormCheckpoint> forms){
        this.game=game;this.service=game.items().polymorph();this.epoch=epoch;this.forms=List.copyOf(forms);transientActors=Set.of();stable(false);
    }
    /** Carry unresolved human forms into a replacement snapshot without replaying already-live old forms. */
    MatchPolymorphCheckpoint pendingOnly(){
        stable(false);var result=new MatchPolymorphCheckpoint(game,epoch,forms.stream().filter(form->!installed.contains(form)).toList());
        result.prepared=true;result.pending=pending;return result;
    }
    MatchPolymorphCheckpoint withPending(MatchPolymorphCheckpoint previous){
        stable(false);if(previous.pending.isEmpty())return this;
        var combined=new ArrayList<>(forms);
        for(var form:previous.forms)if(!previous.installed.contains(form))combined.add(form);
        var result=new MatchPolymorphCheckpoint(game,epoch,combined);result.installed.addAll(installed);result.pending=previous.pending;result.prepared=true;return result;
    }
    private void stable(boolean frozen){
        if(!game.server().isSameThread()||!game.playing()||game.maps().busy()||!game.items().currentEpoch(epoch)||frozen&&!game.server().tickRateManager().isFrozen())
            throw new IllegalStateException("Restore polymorph on the same stable frozen match thread");
        service.checkpointBoundary();
    }
    /** Exclude these native originals/sheep from the coordinator's transient-actor cancellation. */
    Set<UUID> actorIds(){var ids=new LinkedHashSet<UUID>();for(var form:forms)ids.addAll(form.actors());return Set.copyOf(ids);}
    /** Orb emission is intentionally transient and may be omitted by the native body restore. */
    Set<UUID> omittedActorIds(){return transientActors;}
    void prepareFrozen(){stable(true);service.prepareCheckpointFrozen();installed.clear();pending=Set.of();prepared=true;}
    /** First restore saved native/player images. Later calls install only still-pending forms, never reset live timers. */
    Set<UUID> finishFrozen(Function<UUID,Entity> lookup){
        stable(true);if(!prepared)throw new IllegalStateException("Prepare polymorph before installing forms");
        var missing=new LinkedHashSet<UUID>();var waiting=new ArrayList<LbcPolymorph.FormCheckpoint>();
        var ready=new ArrayList<LbcPolymorph.FormCheckpoint>();var plans=new ArrayList<Runnable>();
        for(var form:forms)if(!installed.contains(form)){
            var absent=form.resolve(lookup);
            if(!absent.isEmpty()){missing.addAll(absent);waiting.add(form);}else{ready.add(form);plans.add(form.prepare());}
        }
        for(var form:waiting)form.hold(lookup);
        plans.forEach(Runnable::run);installed.addAll(ready);pending=Set.copyOf(missing);return pending;
    }
    /** Ready forms publish even if another human remains offline. No camera/body teleport or transform grant runs. */
    void publishFrozen(){stable(true);if(!prepared)throw new IllegalStateException("Install polymorph before publication");service.publishCheckpointFrozen();}
    Set<UUID> pending(){return pending;}
    boolean hasPending(){return installed.size()!=forms.size();}
    /** Partial cancellation still requires the coordinator's trusted baseline restore before normal gameplay. */
    void cancelFrozen(){stable(true);service.cancelCheckpointFrozen();installed.clear();pending=Set.of();prepared=false;}
}
