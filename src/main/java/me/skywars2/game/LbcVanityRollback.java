package me.skywars2.game;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import me.skywars2.SkyWars2;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Match-scoped save point, driven after normal simulation, never recursively from a death callback. */
public final class LbcVanityRollback {
    private enum Phase { IDLE, REQUESTED, TERRAIN, BODIES, PUBLICATION, LOGIN, REBINDING, FAILED }
    private final GameController game;
    private final Map<UUID,MatchPlayerCheckpoint> pendingPlayers=new LinkedHashMap<>();
    private final Set<UUID> restoringLogins=new LinkedHashSet<>();
    private final Map<UUID,ServerPlayer> retainedBodies=new LinkedHashMap<>();
    private final Map<UUID,ServerPlayer> logoutBodies=new LinkedHashMap<>();
    private final Set<UUID> preparedOffline=new LinkedHashSet<>();
    private final Set<UUID> awaitingRebind=new LinkedHashSet<>();
    private MatchRollbackSnapshot image;
    private MatchRollbackSnapshot captureFrom;
    private MatchTerrainCheckpoint.Restore terrain;
    private MatchRuntimeRebind rebind;
    private UUID requested;
    private boolean claim,granted,previousFreeze,privateInstalled;
    private Phase phase=Phase.IDLE;
    private int lastFrame=Integer.MIN_VALUE,nextCaptureAttempt;
    private String waitReason="";
    private long restoreStarted;

    LbcVanityRollback(GameController game){this.game=game;}
    public boolean busy(){return phase!=Phase.IDLE&&phase!=Phase.REQUESTED;}
    public boolean blocked(ServerPlayer player){return busy()||restoringLogins.contains(player.getUUID())||awaitingRebind.contains(player.getUUID())
            ||phase==Phase.REQUESTED&&granted&&player.getUUID().equals(requested);}
    public boolean hasSnapshot(){return image!=null&&game.items().currentEpoch(image.epoch);}
    public String status(){return phase+(!waitReason.isEmpty()?": "+waitReason:"");}
    boolean requestCapture(ServerPlayer holder,boolean grant){
        if(phase!=Phase.IDLE||!game.playing()||game.maps().busy())return false;
        requested=holder.getUUID();claim=grant;granted=false;nextCaptureAttempt=0;captureFrom=image;phase=Phase.REQUESTED;
        game.items().duplicating().beginCheckpointDrain();return true;
    }
    boolean death(ServerPlayer holder){
        if(!hasSnapshot()||!holder.entityTags().contains("sin_vanity")||busy())return false;
        requested=null;claim=false;granted=false;captureFrom=null;
        phase=Phase.BODIES;restoreStarted=System.nanoTime();previousFreeze=game.server().tickRateManager().isFrozen();
        // Only request here. Native death/drop iteration finishes before any world is replaced.
        return true;
    }
    boolean requestRestoreForCheck(){
        if(!hasSnapshot()||phase!=Phase.IDLE)return false;
        phase=Phase.BODIES;restoreStarted=System.nanoTime();previousFreeze=game.server().tickRateManager().isFrozen();return true;
    }
    private boolean independentTerrainReady(){
        var items=game.items();
        return !game.maps().busy()&&SignedEnvironmentalEffects.queuedTerrain()==0
                &&items.blaze().terrainCount()==0&&items.kiuas().queuedBlockCount()==0
                &&items.tanner().queuedBlockCount()==0&&items.portalDrops().idle()
                &&MatchEventActorCheckpoint.captureReady(game)&&MatchFieldCheckpoint.captureReady(game)
                &&MatchTransientSpells.captureReady(game)&&MatchAcademyCheckpoint.captureReady(game)
                &&MatchEncounterCheckpoint.captureReady(game)&&items.sauvojen().meditation().count()==0
                &&items.duplicating().captureReady()&&items.stonekivi().captureSettled();
    }
    /** Called once at END_SERVER_TICK, including ticks whose native simulation is frozen. */
    public void tick(){
        int frame=game.server().getTickCount();if(frame==lastFrame)return;lastFrame=frame;
        if(!game.playing()||image!=null&&!game.items().currentEpoch(image.epoch)){stop();return;}
        if(phase==Phase.FAILED)return;
        try{
            if(phase==Phase.REQUESTED){if(frame>=nextCaptureAttempt)capture();return;}
            if(phase==Phase.BODIES&&terrain==null){beginRestore();return;}
            if(phase==Phase.TERRAIN){
                // A sizeable palette scan per frame avoids a many-second fixed-cell trickle.
                long deadline=System.nanoTime()+12_000_000L;
                boolean done;
                do{done=terrain.step(131072);}while(!done&&System.nanoTime()<deadline);
                if(!done)return;
                terrain.close();terrain=null;restoreBodies();phase=Phase.PUBLICATION;
            }
            if(phase==Phase.PUBLICATION)publish();
            if((phase==Phase.IDLE||phase==Phase.LOGIN)&&!restoringLogins.isEmpty())restoreLogins();
            if(phase==Phase.LOGIN||phase==Phase.IDLE&&!awaitingRebind.isEmpty())tryRebind();
            if(phase==Phase.REBINDING&&privateData(rebind::publishFrozen)){
                rebind=null;awaitingRebind.clear();phase=Phase.IDLE;game.server().tickRateManager().setFrozen(previousFreeze);
            }
        }catch(Throwable failure){
            if(phase==Phase.REQUESTED){
                // Unreviewed terrain callbacks drain naturally, outside the freeze. Retry at the
                // next stable boundary, rather than keeping half a structure or copying a Future.
                String reason=failure.getMessage()==null?failure.getClass().getSimpleName():failure.getMessage();
                if(!reason.equals(waitReason))SkyWars2.LOGGER.warn("[vanity] Save point waiting: {}",reason);
                waitReason=reason;nextCaptureAttempt=frame+20;
                return;
            }
            phase=Phase.FAILED;waitReason=failure.toString();game.server().tickRateManager().setFrozen(true);
            SkyWars2.LOGGER.error("[vanity] Rewind remains frozen after a restore failure; save point retained",failure);
        }
    }
    private void capture(){
        var player=game.server().getPlayerList().getPlayer(requested);
        if(player==null||!player.isAlive()||game.isObserver(player)){
            requested=null;phase=Phase.IDLE;game.items().duplicating().endCheckpointDrain();return;
        }
        // Complete accepted construction with the existing bounded dispatcher before saving.
        game.items().stonekivi().tick();
        if(!independentTerrainReady()){waitReason="waiting for structure writes to finish";return;}
        if(!MatchActorCheckpoint.prepareResident(game)){waitReason="waiting for native actor chunks";return;}
        // First establish a usable save point; granting below is a separate entry boundary.
        if(claim&&!granted){
            // Admission does not copy the terrain. The point is captured once, after the
            // one successful grant; retrying a newly queued bonus never consumes another token.
            var admissionActors=MatchActorCheckpoint.captureRegistered(game);
            new MatchQueuedWorkCheckpoint(game,admissionActors.damageSources());
            if(!game.items().witchClaims().finishVanityClaim(player)){phase=Phase.IDLE;game.items().duplicating().endCheckpointDrain();return;}
            granted=true;image=null;
        }
        var candidate=privateData(()->new MatchRollbackSnapshot(game,captureFrom));
        pendingPlayers.forEach(candidate.players::putIfAbsent);
        image=candidate;captureFrom=null;image.terrain.retain(game);
        restoringLogins.clear();requested=null;claim=false;granted=false;waitReason="";phase=Phase.IDLE;
        game.items().duplicating().endCheckpointDrain();
    }
    private void beginRestore(){
        if(!hasSnapshot())throw new IllegalStateException("Save point no longer belongs to this match");
        game.server().tickRateManager().setFrozen(true);
        if(!image.terrain.ensureResident(game))return;
        image.world.restoreFrozen();image.prepareFrozen(game);
        game.items().duplicating().prepareCheckpointRestore();
        privateInstalled=false;preparedOffline.clear();
        terrain=image.terrain.restore(game);phase=Phase.TERRAIN;
    }
    private Set<UUID> offline(){
        var result=new LinkedHashSet<UUID>();for(var id:image.players.keySet())
            if(game.server().getPlayerList().getPlayer(id)==null)result.add(id);return result;
    }
    private static void complete(Set<UUID> pending,String owner){
        if(!pending.isEmpty())throw new IllegalStateException(owner+" awaits saved players "+pending);
    }
    private <T>T privateData(Supplier<T> action){return MatchOfflinePlayers.using(game,retainedBodies,action);}
    private void restoreBodies(){
        image.core.restoreFrozen(image.players);image.actors.restoreBodiesFrozen(image.omitted);
        image.restoreOnlinePlayers(game,pendingPlayers);
        var absent=offline();game.reconcileRollbackParticipants(absent,image.players.keySet());
        prepareOfflineData(absent);
        complete(image.actors.restoreLinksFrozen(absent),"native relationships");
        privateData(()->{var pending=new LinkedHashSet<UUID>();image.actors.damageSources().restore(image.actors::resolve,pending);complete(pending,"accepted sources");return null;});
        complete(privateData(()->image.spells.restoreFrozen(image.actors::resolve)),"spell owners");
        complete(privateData(()->image.authorities.restoreFrozen(image.actors::resolve)),"authority owners");
        complete(privateData(()->image.attribution.restoreFrozen(image.actors::resolve)),"attribution");
        complete(privateData(()->image.fpv.finishFrozen(image.actors::resolve)),"FPV metadata");
        complete(privateData(image::restoreDuplicatingFrozen),"duplicating owners");
        complete(privateData(()->image.holes.restoreFrozen(image.actors::resolve)),"black holes");
        complete(privateData(()->image.events.restoreFrozen(image.actors::resolve)),"event actors");
        complete(privateData(()->image.statuses.restoreFrozen(image.actors::resolve)),"custom statuses");
        complete(privateData(()->image.academy.restoreFrozen(image.actors::resolve)),"academy ownership");
        complete(privateData(()->image.chefs.restoreFrozen(image.actors::resolve)),"Chefs");
        complete(privateData(()->image.hedgehogs.restoreFrozen(image.actors::resolve)),"Hedgehog encounters");
        complete(privateData(()->image.encounters.restoreFrozen(image.actors::resolve)),"native encounters");
        complete(privateData(()->image.residents.restoreFrozen(image.actors::resolve)),"resident item state");
        image.auras.restoreFrozen(image.actors::resolve);image.fields.finishFrozen(image.actors::resolve);
        image.polymorph.finishFrozen(image.actors::resolve);image.aim.finishFrozen(image.actors::resolve);
    }
    private boolean waiting(Set<UUID> players,String owner){
        if(players.isEmpty())return false;waitReason=owner+": "+players;return true;
    }
    private void publish(){
        prepareOfflineData(offline());
        if(!restoringLogins.isEmpty())restoreLogins();
        for(var p:game.server().getPlayerList().getPlayers())
            if(image.players.containsKey(p.getUUID())&&(!p.connection.hasClientLoaded()||p.isChangingDimension())){waitReason="client "+p.getScoreboardName()+" loaded="+p.connection.hasClientLoaded()+" changingDimension="+p.isChangingDimension();return;}
        if(waiting(image.actors.finishViewsFrozen(offline()),"native views")||waiting(privateData(image.fpv::publishFrozen),"FPV views"))return;
        if(!privateInstalled){
            complete(privateData(()->image.queued.finishFrozen(image.actors::resolve)),"queued work");
            complete(privateData(()->image.blasts.finishFrozen(image.actors::resolve)),"explosive state");
            complete(privateData(()->image.timeStop.finishFrozen(image.actors::resolve)),"time stop");
            image.authorities.publishFrozen();privateInstalled=true;
        }
        if(waiting(privateData(image.queued::publishFrozen),"queued presentation")||waiting(image.holes.publishFrozen(),"black-hole presentation")
                ||waiting(image.events.publishFrozen(),"event presentation")||waiting(privateData(image.encounters::publishFrozen),"encounter presentation"))return;
        complete(privateData(image.statuses::publishFrozen),"custom status publication");image.academy.publishFrozen();
        complete(privateData(image.chefs::publishFrozen),"Chef publication");complete(privateData(image.hedgehogs::publishFrozen),"Hedgehog publication");
        complete(privateData(image.residents::publishFrozen),"resident item publication");
        image.auras.publishFrozen();image.fields.publishFrozen();image.polymorph.publishFrozen();image.aim.publishFrozen();image.random.restoreFrozen();
        for(var player:game.server().getPlayerList().getPlayers())
            if(!game.isObserver(player)&&player.getCamera() instanceof LbcFpvDrone.Drone&&!game.items().fpvDrone().controlling(player))player.setCamera(player);
        game.items().duplicating().endCheckpointDrain();game.players().save();game.config().save();
        phase=Phase.IDLE;waitReason="";game.server().tickRateManager().setFrozen(previousFreeze);
        SkyWars2.LOGGER.info("[vanity] Rewind finished in {} ms; {} offline player images retained",
                (System.nanoTime()-restoreStarted)/1_000_000.0,pendingPlayers.size());
    }
    /** Real login is restored at an end-tick boundary, before ordinary spectator initialization. */
    boolean joined(ServerPlayer player){
        if(!hasSnapshot()||!pendingPlayers.containsKey(player.getUUID()))return false;
        if(phase==Phase.REQUESTED&&!granted){
            requested=null;claim=false;captureFrom=null;waitReason="";phase=Phase.IDLE;game.items().duplicating().endCheckpointDrain();
        }
        if(phase==Phase.IDLE){previousFreeze=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);phase=Phase.LOGIN;}
        restoringLogins.add(player.getUUID());return true;
    }
    void leaving(ServerPlayer player){if(hasSnapshot()&&image.players.containsKey(player.getUUID()))logoutBodies.put(player.getUUID(),player);}
    private void prepareOfflineData(Set<UUID> absent){
        for(var id:absent){
            var data=image.players.get(id);pendingPlayers.putIfAbsent(id,data);
            var body=logoutBodies.getOrDefault(id,data.retainedBody());
            if(preparedOffline.add(id))data.restoreRetainedData(body);retainedBodies.put(id,body);
        }
    }
    /** Chunks first seen after acquisition retain their unmodified loaded baseline too. */
    public void chunkLoaded(net.minecraft.world.level.chunk.LevelChunk chunk){
        if(hasSnapshot()&&game.playing()&&!busy())image.terrain.include(chunk);
    }
    private void restoreLogins(){
        boolean wasFrozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
        try{
            for(var id:Set.copyOf(restoringLogins)){
                var player=game.server().getPlayerList().getPlayer(id);
                if(player==null){restoringLogins.remove(id);continue;}
                var data=pendingPlayers.get(id);
                player=privateData(data::restoreFrozen);game.rejoinRollbackParticipant(player,data.observer());
                image.actors.damageSources().rebindPlayers(Map.of(id,player));
                image.attribution.restorePlayerFrozen(player);
                game.items().duplicating().rebindRetained(player);
                game.items().sauvojenBoss().rebindRetained(player);
                awaitingRebind.add(id);
                pendingPlayers.remove(id);restoringLogins.remove(id);
            }
            var current=MatchActorCheckpoint.currentBodies(game);
            image.polymorph.finishFrozen(current::get);image.polymorph.publishFrozen();
            image.fields.finishFrozen(current::get);image.fields.publishFrozen();
            image.auras.restoreFrozen(current::get);image.auras.publishFrozen();
            image.aim.finishFrozen(current::get);image.aim.publishFrozen();
        }finally{game.server().tickRateManager().setFrozen(wasFrozen);}
    }
    private void tryRebind(){
        for(var id:Set.copyOf(awaitingRebind)){
            var player=game.server().getPlayerList().getPlayer(id);
            if(player==null){awaitingRebind.remove(id);continue;}
            if(!player.connection.hasClientLoaded()||player.isChangingDimension())return;
        }
        if(awaitingRebind.isEmpty()){
            if(phase==Phase.LOGIN){phase=Phase.IDLE;game.server().tickRateManager().setFrozen(previousFreeze);}return;
        }
        var online=new LinkedHashMap<UUID,ServerPlayer>();
        for(var id:retainedBodies.keySet()){var player=game.server().getPlayerList().getPlayer(id);if(player!=null)online.put(id,player);}
        image.actors.damageSources().rebindPlayers(online);
        final MatchRuntimeRebind candidate;
        candidate=privateData(()->new MatchRuntimeRebind(game));
        if(phase!=Phase.LOGIN)previousFreeze=game.server().tickRateManager().isFrozen();
        game.server().tickRateManager().setFrozen(true);
        rebind=candidate;phase=Phase.REBINDING;privateData(()->{rebind.prepareFrozen();return null;});
    }
    void stop(){
        if(busy()&&image!=null&&game.playing()&&!game.maps().busy()&&game.items().currentEpoch(image.epoch)){
            game.server().tickRateManager().setFrozen(true);
            // An interrupted publication must not leave private owners staged into the next match.
            for(Runnable cancel:List.<Runnable>of(image.timeStop::cancelFrozen,image.queued::cancelFrozen,image.blasts::cancelFrozen,image.fpv::cancelFrozen,
                    image.holes::cancelFrozen,image.polymorph::cancelFrozen,image.auras::cancelFrozen,image.fields::cancelFrozen,
                    image.events::cancelFrozen,image.statuses::cancelFrozen,image.academy::cancelFrozen,
                    image.chefs::cancelFrozen,image.hedgehogs::cancelFrozen,image.encounters::cancelFrozen,image.aim::cancelFrozen)){
                try{privateData(()->{cancel.run();return null;});}catch(RuntimeException failure){SkyWars2.LOGGER.error("[vanity] Private cleanup failed while stopping the point",failure);}
            }
        }
        if(terrain!=null){terrain.close();terrain=null;}
        image=null;captureFrom=null;requested=null;claim=false;granted=false;privateInstalled=false;pendingPlayers.clear();restoringLogins.clear();waitReason="";
        retainedBodies.clear();logoutBodies.clear();awaitingRebind.clear();rebind=null;preparedOffline.clear();
        game.items().duplicating().endCheckpointDrain();
        if(busy())game.server().tickRateManager().setFrozen(previousFreeze);phase=Phase.IDLE;
    }
}
