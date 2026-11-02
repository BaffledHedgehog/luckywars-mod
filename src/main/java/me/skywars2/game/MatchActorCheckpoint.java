package me.skywars2.game;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.skywars2.mixin.ActorCheckpointAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;

/** Registered native actor bodies and relationships, including hidden sections. Private queues remain separate. */
public final class MatchActorCheckpoint {
    private static final ThreadLocal<GameController> RESTORING=new ThreadLocal<>();
    public static boolean restoring(Entity entity) {
        var game=RESTORING.get();return game!=null&&!(entity instanceof ServerPlayer)&&entity.level().getServer()==game.server();
    }
    private record Link(UUID id,net.minecraft.world.entity.Pose pose) { }
    private record Image(Entity actor,int id,UUID uuid,MatchEntityState state,List<Link> passengers,
                         UUID target,UUID owner,UUID hooked,Tag leash,UUID leashHolder,double angularMomentum) { }
    private final GameController game;
    private final String epoch;
    private final Map<UUID,Image> images=new LinkedHashMap<>();
    private boolean bodiesRestored;
    private Set<UUID> omitted=Set.of();
    private final Set<UUID> capturedPlayers=new LinkedHashSet<>();
    private record PlayerLedger(Set<UUID> pearls,UUID fishing) { }
    private final Map<UUID,PlayerLedger> playerLedgers=new LinkedHashMap<>();
    private MatchPlayerLinks playerLinks;
    private MatchCombatCheckpoint combat;
    private MatchDamageSources damageSources;
    private MatchPlayerViews views;

    private MatchActorCheckpoint(GameController game) {this.game=game;epoch=game.items().epoch();}
    static MatchActorCheckpoint captureRegistered(GameController game) {return captureRegistered(game,Set.of());}
    static MatchActorCheckpoint captureRegistered(GameController game,Set<UUID> omitted) {
        if(!game.server().isSameThread()||!game.playing()||game.maps().busy())throw new IllegalStateException("Capture a stable native match on its server thread");
        var result=new MatchActorCheckpoint(game);result.omitted=Set.copyOf(omitted);
        for(var actor:result.registered()) {
            var passengers=actor.getPassengers().stream().map(e->new Link(e.getUUID(),e.getPose())).toList();
            var target=actor instanceof Mob mob?mob.getTargetUnchecked():null;
            var owner=actor instanceof Projectile shot?shot.getOwner():null;
            var state=new MatchEntityState(game,actor);
            var ownerId=actor instanceof Projectile?state.nbt.read("Owner",net.minecraft.core.UUIDUtil.CODEC).orElse(owner==null?null:owner.getUUID()):null;
            Tag leash=null;UUID leashHolder=null;double angular=0;
            if(actor instanceof Leashable leashed&&leashed.getLeashData()!=null) {
                leash=Leashable.LeashData.CODEC.encodeStart(NbtOps.INSTANCE,leashed.getLeashData()).getOrThrow().copy();
                if(leashed.getLeashData().leashHolder!=null)leashHolder=leashed.getLeashData().leashHolder.getUUID();
                angular=leashed.getLeashData().angularMomentum;
            }
            var image=new Image(actor,actor.getId(),actor.getUUID(),state,passengers,
                    target==null?null:target.getUUID(),ownerId,
                    actor instanceof net.minecraft.world.entity.projectile.FishingHook hook&&hook.getHookedIn()!=null?hook.getHookedIn().getUUID():null,
                    leash,leashHolder,angular);
            if(result.images.put(image.uuid,image)!=null)throw new IllegalStateException("Duplicate native actor UUID at capture");
        }
        result.playerLinks=new MatchPlayerLinks(game,id->{var image=result.images.get(id);return image==null?null:image.actor;});
        for(var player:game.server().getPlayerList().getPlayers()){
            result.capturedPlayers.add(player.getUUID());var pearls=new LinkedHashSet<UUID>();
            for(var pearl:player.getEnderPearls())pearls.add(pearl.getUUID());
            result.playerLedgers.put(player.getUUID(),new PlayerLedger(Set.copyOf(pearls),player.fishing==null?null:player.fishing.getUUID()));
        }
        var actors=result.images.values().stream().map(Image::actor).toList();
        result.damageSources=new MatchDamageSources(game,actors,omitted);
        result.combat=new MatchCombatCheckpoint(game,actors,result.damageSources);
        result.views=new MatchPlayerViews(game);
        return result;
    }
    int size(){return images.size();}
    static Map<UUID,Entity> currentBodies(GameController game){
        var result=new LinkedHashMap<UUID,Entity>();
        for(var actor:new MatchActorCheckpoint(game).registered())result.put(actor.getUUID(),actor);
        for(var player:game.server().getPlayerList().getPlayers())result.put(player.getUUID(),player);
        return result;
    }
    /** Native section storage also contains actors whose destination chunk is not currently visible. */
    static boolean prepareResident(GameController game){
        boolean ready=true;
        for(var body:currentBodies(game).values()){
            if(body.isRemoved()||!(body.level() instanceof net.minecraft.server.level.ServerLevel level))continue;
            var pos=BlockPos.containing(body.position());var future=game.maps().prepareChunk(level,new net.minecraft.world.level.ChunkPos(pos.getX() >> 4,pos.getZ() >> 4));
            if(!future.isDone()){ready=false;continue;}
            var result=future.join();
            if(result instanceof net.minecraft.server.level.ChunkResult<?> chunk&&!chunk.isSuccess())
                throw new IllegalStateException("Could not prepare saved actor chunk "+level.dimension()+" "+pos);
            if(!level.hasChunkAt(pos))ready=false;
        }
        return ready;
    }
    private List<Entity> registered() {
        var actors=new ArrayList<Entity>();for(var level:game.server().getAllLevels()) {
            var manager=((ActorCheckpointAccess.LevelEntities)level).skywars2$manager();
            var storage=((ActorCheckpointAccess.Sections)manager).skywars2$sections();
            for(long chunk:storage.getAllChunksWithExistingSections().toLongArray())
                storage.getExistingSectionsInChunk(chunk).forEach(section->section.getEntities().forEach(entity->{
                    if(!(entity instanceof ServerPlayer)&&!entity.isRemoved())actors.add(entity);
                }));
        }
        return actors;
    }
    private void validate(){validate(omitted);}
    private void validate(Set<UUID> excluded) {
        if(!game.server().isSameThread()||!game.server().tickRateManager().isFrozen()||!game.playing()
                ||!game.items().currentEpoch(epoch)||game.maps().busy())throw new IllegalStateException("Restore the original match actors on a frozen server thread");
        for(var image:images.values()) {
            if(excluded.contains(image.uuid))continue;
            if(image.actor.getId()!=image.id||!image.actor.getUUID().equals(image.uuid))throw new IllegalStateException("Native actor identity changed outside the entity manager");
            if(!image.state.level.hasChunkAt(BlockPos.containing(image.state.position)))throw new IllegalStateException("Saved actor destination is not resident");
            if(image.actor.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK)throw new IllegalStateException("Full owner must reconcile actor region storage before an unloaded actor restore");
        }
    }
    private void scoped(Runnable action) {
        var previous=RESTORING.get();RESTORING.set(game);
        try{action.run();}finally{if(previous==null)RESTORING.remove();else RESTORING.set(previous);}
    }
    /** No death/drop/status triggers. Native managers remove/register the same original instances. */
    void prepareViewsFrozen() {validate();views.prepareFrozen();}
    void restoreBodiesFrozen(){restoreBodiesFrozen(Set.of());}
    /** Omitted saved transient bodies stay retired; no native load/AI/spawn callback recreates them. */
    void restoreBodiesFrozen(Set<UUID> omittedTransientActors) {
        var next=Set.copyOf(omittedTransientActors);
        if(!images.keySet().containsAll(next))throw new IllegalArgumentException("Only captured native actors may be omitted");
        validate(next);views.prepareFrozen();omitted=next;bodiesRestored=false;
        var current=registered();
        scoped(()->{
            playerLinks.clear();
            for(var actor:current)detachLinks(actor);
            for(var image:images.values())if(!omitted.contains(image.uuid))detachLinks(image.actor);
            for(var actor:current) {
                // Final setRemoved avoids LivingEntity.remove's death-effect spawning/brain clearing.
                // CHANGED_DIMENSION retains restored scoreboard entries for original instances.
                // A native dimension replacement shares the saved UUID. Destroying that alias
                // would also erase the already-restored scoreboard data for the original body.
                boolean savedIdentity=images.containsKey(actor.getUUID())&&!omitted.contains(actor.getUUID());
                actor.setRemoved(savedIdentity?Entity.RemovalReason.CHANGED_DIMENSION:Entity.RemovalReason.DISCARDED);
                game.cancelQueuedEntityUnload(actor);
            }
            for(var image:images.values()) {
                if(omitted.contains(image.uuid)){
                    // Accepted sources may retain this historical object. Restore its data only;
                    // never revive/register it or let native physics simulate the cancelled spell.
                    if(!image.actor.isRemoved())image.actor.setRemoved(Entity.RemovalReason.DISCARDED);
                    image.state.load(image.actor);image.state.finishRandom(image.actor);
                    if(!image.actor.isRemoved())throw new IllegalStateException("Historical source data revived an omitted actor");
                    continue;
                }
                var actor=image.actor;var access=(ActorCheckpointAccess.Body)actor;
                access.skywars2$level(image.state.level);access.skywars2$revive();
                image.state.load(actor);
                if(!image.state.level.addFreshEntity(actor)||actor.isRemoved())throw new IllegalStateException("Native actor registration rejected checkpoint body "+image.uuid);
                image.state.finishRandom(actor);
                game.cancelQueuedEntityUnload(actor);
            }
            bodiesRestored=true;
        });
    }
    /** Canonical saved bodies only; no world enumeration, chunk request or load callback replay. */
    List<Entity> restoredBodies(){
        if(!bodiesRestored)throw new IllegalStateException("Restore native actor bodies before derived indexes");
        return images.values().stream().filter(image->!omitted.contains(image.uuid)&&!image.actor.isRemoved())
                .map(Image::actor).toList();
    }
    private void detachLinks(Entity actor) {
        var vehicle=actor.getVehicle();
        if(vehicle!=null) {
            ((ActorCheckpointAccess.Body)vehicle).skywars2$passengers(ImmutableList.copyOf(vehicle.getPassengers().stream().filter(e->e!=actor).toList()));
            ((ActorCheckpointAccess.Body)actor).skywars2$vehicle(null);
            ((net.minecraft.server.level.ServerLevel)vehicle.level()).getChunkSource().sendToTrackingPlayers(vehicle,new ClientboundSetPassengersPacket(vehicle));
        }
        for(var passenger:actor.getPassengers())((ActorCheckpointAccess.Body)passenger).skywars2$vehicle(null);
        ((ActorCheckpointAccess.Body)actor).skywars2$passengers(ImmutableList.of());
    }
    MatchDamageSources damageSources(){return damageSources;}
    /** Canonical current body lookup for other checkpoint owners; no gameplay visibility selector. */
    Entity resolve(UUID id) {
        if(!bodiesRestored)throw new IllegalStateException("Restore native bodies before resolving checkpoint references");
        if(id==null||omitted.contains(id))return null;
        var player=game.server().getPlayerList().getPlayer(id);if(player!=null)return player;
        var image=images.get(id);return image!=null&&!image.actor.isRemoved()?image.actor:null;
    }
    /** Finish camera links only after the native post-respawn chunk/entity pairing. */
    Set<UUID> finishViewsFrozen(){return finishViewsFrozen(Set.of());}
    Set<UUID> finishViewsFrozen(Set<UUID> intentionallyAbsent) {
        validate();var absent=absentPolicy(intentionallyAbsent);if(!bodiesRestored)throw new IllegalStateException("Restore native actor bodies before views");
        var pending=pendingExcept(absent);MatchDamageSources.withAbsent(absent,()->{views.finishBoundViews(this::resolve,pending);views.restoreCameras(this::resolve,pending);});return Set.copyOf(pending);
    }
    /** Player bodies may be restored between phases; missing/offline UUIDs remain explicit pending links. */
    Set<UUID> restoreLinksFrozen(){return restoreLinksFrozen(Set.of());}
    /** Only offline captured players and this restore's omitted actors may remain intentionally absent. */
    Set<UUID> restoreLinksFrozen(Set<UUID> intentionallyAbsent) {
        validate();var absent=absentPolicy(intentionallyAbsent);if(!bodiesRestored)throw new IllegalStateException("Restore native actor bodies before links");
        var current=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Entity,Boolean>());current.addAll(registered());
        for(var image:images.values())if(!omitted.contains(image.uuid)&&(!current.contains(image.actor)||image.actor.level()!=image.state.level))throw new IllegalStateException("Actor body left its restored native registry before links");
        var pending=pendingExcept(absent);
        MatchDamageSources.withAbsent(absent,()->scoped(()->{
            playerLinks.clear();
            for(var image:images.values())if(!omitted.contains(image.uuid))detachLinks(image.actor);
            for(var image:images.values()) {
                if(omitted.contains(image.uuid))continue;
                var actor=image.actor;
                if(actor instanceof Mob) {
                    var target=resolve(image.target);if(image.target!=null&&target==null)pending.add(image.target);
                    ((ActorCheckpointAccess.Target)actor).skywars2$target(target instanceof LivingEntity living?living:null);
                }
                if(actor instanceof Projectile shot) {
                    var owner=resolve(image.owner);
                    if(image.owner!=null&&owner==null)pending.add(image.owner);else shot.setOwner(owner);
                }
                if(actor instanceof net.minecraft.world.entity.projectile.FishingHook hook) {
                    var hooked=resolve(image.hooked);
                    if(image.hooked!=null&&(hooked==null||hooked.level()!=hook.level())){pending.add(image.hooked);hooked=null;}
                    ((ActorCheckpointAccess.Fishing)hook).skywars2$hooked(hooked);
                    // The hook's spawn packet embeds a native player ID, which changes on respawn.
                    // Re-pair existing viewers after UUID resolution, with full metadata and motion.
                    var owner=hook.getPlayerOwner();
                    if(owner!=null) {
                        var level=image.state.level;var position=hook.position();
                        level.getChunkSource().sendToTrackingPlayers(hook,new net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket(hook.getId()));
                        level.getChunkSource().sendToTrackingPlayers(hook,new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(
                                hook.getId(),hook.getUUID(),position.x,position.y,position.z,hook.getXRot(),hook.getYRot(),hook.getType(),owner.getId(),hook.getDeltaMovement(),hook.getYHeadRot()));
                        var data=new ArrayList<net.minecraft.network.syncher.SynchedEntityData.DataValue<?>>();
                        for(var item:((me.skywars2.mixin.PlayerCheckpointAccess.Metadata)hook.getEntityData()).skywars2$items())data.add(item.value());
                        level.getChunkSource().sendToTrackingPlayers(hook,new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(hook.getId(),data));
                    }
                }
                if(actor instanceof Leashable leashed) {
                    var leash=image.leash==null?null:Leashable.LeashData.CODEC.parse(NbtOps.INSTANCE,image.leash.copy()).getOrThrow();
                    if(leash!=null) {
                        leash.angularMomentum=image.angularMomentum;
                        var holderId=image.leashHolder;
                        if(holderId==null&&leash.delayedLeashInfo!=null)holderId=leash.delayedLeashInfo.left().orElse(null);
                        if(holderId!=null) {
                            var holder=resolve(holderId);
                            if(holder==null||holder.level()!=actor.level()){pending.add(holderId);if(absent.contains(holderId))leash=null;}else leash.setLeashHolder(holder);
                        }
                    }
                    leashed.setLeashData(leash);
                    image.state.level.getChunkSource().sendToTrackingPlayers(actor,new ClientboundSetEntityLinkPacket(actor,leash==null?null:leash.leashHolder));
                }
                for(var link:image.passengers) {
                    var passenger=resolve(link.id);
                    if(passenger==null||passenger.level()!=actor.level()){pending.add(link.id);continue;}
                    if(!passenger.startRiding(actor,true,false))throw new IllegalStateException("Native passenger graph rejected checkpoint relationship");
                    passenger.setPose(link.pose);
                }
                image.state.level.getChunkSource().sendToTrackingPlayers(actor,new ClientboundSetPassengersPacket(actor));
            }
            if(omitted.isEmpty())playerLinks.restore(this::resolve,pending);else restorePlayerLedgers(pending);
            combat.restore(this::resolve,pending);
            views.restore(this::resolve,pending);
        }));
        return Set.copyOf(pending);
    }
    private Set<UUID> absentPolicy(Set<UUID> requested){
        var result=new LinkedHashSet<UUID>(omitted);
        for(var id:requested){
            if(omitted.contains(id)){result.add(id);continue;}
            if(!capturedPlayers.contains(id))throw new IllegalArgumentException("Intentional absence requires a captured offline player or omitted actor: "+id);
            if(game.server().getPlayerList().getPlayer(id)!=null)throw new IllegalArgumentException("An online captured player cannot be treated as absent: "+id);
            result.add(id);
        }
        return Set.copyOf(result);
    }
    private static Set<UUID> pendingExcept(Set<UUID> absent){return new LinkedHashSet<>(){
        @Override public boolean add(UUID id){return !absent.contains(id)&&super.add(id);}
    };}
    /** The normal native ledger path throws for an omitted pearl/hook; rebuild only retained ledger entries. */
    private void restorePlayerLedgers(Set<UUID> pending){
        playerLinks.clear();
        for(var entry:playerLedgers.entrySet()){
            var player=game.server().getPlayerList().getPlayer(entry.getKey());var saved=entry.getValue();
            if(player==null){if(!saved.pearls.isEmpty()||saved.fishing!=null)pending.add(entry.getKey());continue;}
            for(var id:saved.pearls){
                if(omitted.contains(id))continue;var body=resolve(id);
                if(!(body instanceof net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl pearl)||pearl.getOwner()!=player)
                    throw new IllegalStateException("Restore retained native pearl bodies and owners before player ledgers");
                player.registerEnderPearl(pearl);
            }
            if(saved.fishing!=null&&!omitted.contains(saved.fishing)){
                var body=resolve(saved.fishing);
                if(!(body instanceof net.minecraft.world.entity.projectile.FishingHook hook)||hook.getPlayerOwner()!=player)
                    throw new IllegalStateException("Restore retained native fishing body and owner before player ledger");
                player.fishing=hook;
            }
        }
    }

}
