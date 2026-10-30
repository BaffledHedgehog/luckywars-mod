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
    private MatchPlayerLinks playerLinks;
    private MatchCombatCheckpoint combat;

    private MatchActorCheckpoint(GameController game) {this.game=game;epoch=game.items().epoch();}
    static MatchActorCheckpoint captureRegistered(GameController game) {
        if(!game.server().isSameThread()||!game.playing()||game.maps().busy())throw new IllegalStateException("Capture a stable native match on its server thread");
        var result=new MatchActorCheckpoint(game);
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
        result.combat=new MatchCombatCheckpoint(game,result.images.values().stream().map(Image::actor).toList());
        return result;
    }
    int size(){return images.size();}
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
    private void validate() {
        if(!game.server().isSameThread()||!game.server().tickRateManager().isFrozen()||!game.playing()
                ||!game.items().currentEpoch(epoch)||game.maps().busy())throw new IllegalStateException("Restore the original match actors on a frozen server thread");
        for(var image:images.values()) {
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
    void restoreBodiesFrozen() {
        validate();bodiesRestored=false;
        var current=registered();
        scoped(()->{
            playerLinks.clear();
            for(var actor:current)detachLinks(actor);
            for(var image:images.values())detachLinks(image.actor);
            for(var actor:current) {
                // Final setRemoved avoids LivingEntity.remove's death-effect spawning/brain clearing.
                // CHANGED_DIMENSION retains restored scoreboard entries for original instances.
                // A native dimension replacement shares the saved UUID. Destroying that alias
                // would also erase the already-restored scoreboard data for the original body.
                boolean savedIdentity=images.containsKey(actor.getUUID());
                actor.setRemoved(savedIdentity?Entity.RemovalReason.CHANGED_DIMENSION:Entity.RemovalReason.DISCARDED);
                game.cancelQueuedEntityUnload(actor);
            }
            for(var image:images.values()) {
                var actor=image.actor;var access=(ActorCheckpointAccess.Body)actor;
                access.skywars2$level(image.state.level);access.skywars2$revive();
                image.state.load(actor);
                if(!image.state.level.addFreshEntity(actor)||actor.isRemoved())throw new IllegalStateException("Native actor registration rejected checkpoint body "+image.uuid);
                game.cancelQueuedEntityUnload(actor);
            }
            bodiesRestored=true;
        });
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
    private Entity resolve(UUID id) {
        if(id==null)return null;
        var player=game.server().getPlayerList().getPlayer(id);if(player!=null)return player;
        var image=images.get(id);return image!=null&&!image.actor.isRemoved()?image.actor:null;
    }
    /** Player bodies may be restored between phases; missing/offline UUIDs remain explicit pending links. */
    Set<UUID> restoreLinksFrozen() {
        validate();if(!bodiesRestored)throw new IllegalStateException("Restore native actor bodies before links");
        var current=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Entity,Boolean>());current.addAll(registered());
        for(var image:images.values())if(!current.contains(image.actor)||image.actor.level()!=image.state.level)throw new IllegalStateException("Actor body left its restored native registry before links");
        var pending=new LinkedHashSet<UUID>();
        scoped(()->{
            playerLinks.clear();
            for(var image:images.values())detachLinks(image.actor);
            for(var image:images.values()) {
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
                            if(holder==null||holder.level()!=actor.level())pending.add(holderId);else leash.setLeashHolder(holder);
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
            playerLinks.restore(this::resolve,pending);
            combat.restore(this::resolve,pending);
        });
        return Set.copyOf(pending);
    }
}
