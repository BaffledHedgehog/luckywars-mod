package me.skywars2.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import me.skywars2.SkyWars2;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.player.Player;
import me.skywars2.mixin.ActorCheckpointAccess;
import me.skywars2.mixin.PlayerCheckpointAccess;
import me.skywars2.mixin.WandProjectileMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Proximity cloning, with native value copies and explicit service-owned actor reconstruction. */
public final class LbcCloneSkills {
    private static final TagKey<EntityType<?>> PROJECTILES=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:projectiles_double"));
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final TagKey<EntityType<?>> RAYCASTERS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:raycasters"));
    private static final String[] PROJECTILE_SCORES={"lbcID2","team_number","Distance","Lifetime"};
    private static final String[] MOB_SCORES={"lbcID2","team_number","Distance","Lifetime","cyborgBossFly","delayed_death","lookback","shock","scared","piss","target"};
    private static final int SCAN_BUDGET=256, BODY_BUDGET=48;
    private static final long SOFT_NANOS=4_000_000L;

    /**
     * The integration owner must enumerate every admitted Java actor/state owner before returning
     * completeCoverage=true. Returning null from capture is a visible unsupported operation, never
     * permission to create an inert native shell. Child displays/hitboxes are excluded here and are
     * reconstructed once by their main actor's image. No ownership or team filter belongs here.
     */
    public interface ActorAdapter {
        boolean completeCoverage();
        boolean protectedActor(Entity source);
        ActorImage capture(Entity source);
        default boolean gameplayActor(Entity source){return false;}
        default boolean combatBody(Entity source){return false;}
    }
    /** One immutable, operation-local image. Mutable queues/collections must be copied per install. */
    public interface ActorImage {
        /** Custom native subclasses and fishing hooks need their owner's actual factory. */
        default Entity create(Entity original,ServerLevel level){return original.getType().create(level,EntitySpawnReason.COMMAND);}
        /** Install links, owned effects, lifetime/AI/control state and regenerated visual children. */
        void install(Entity original,Entity clone,Map<Entity,Entity> family);
        /** Called after every attempted install, including partially failed publication. */
        void rollback(Entity clone);
    }
    private final GameController game;
    private ActorAdapter adapter;
    private int ownerCursor;
    private boolean cloning;
    private String epoch;
    private long scanned,operations,bodies,unsupported,failed;

    public LbcCloneSkills(GameController game){this.game=game;}
    public void installAdapter(ActorAdapter adapter){if(cloning)throw new IllegalStateException("Clone operation in progress");this.adapter=adapter;}
    public boolean ready(){return adapter!=null&&adapter.completeCoverage();}
    public void stop(){checkpointBoundary();ownerCursor=0;epoch=null;}
    public void clearPending(){checkpointBoundary();}
    public void checkpointBoundary(){if(cloning)throw new IllegalStateException("Finish cloning before checkpoint publication");}

    private boolean admitted(ServerPlayer player,int skill){
        return game.lbc().hasSkill(player,skill)&&!game.items().wands().inputTimeStopped(player)
                &&!game.items().timeStop().stopped(player.level());
    }
    /** Rotate admitted holders; native resident spatial queries never request or enumerate world chunks. */
    public void tick(){
        if(!ready()||!game.playing()||game.maps().busy()||!game.server().tickRateManager().runsNormally()
                ||game.server().tickRateManager().isFrozen())return;
        if(!game.items().epoch().equals(epoch))epoch=game.items().epoch();
        var owners=new ArrayList<ServerPlayer>();
        for(var player:game.server().getPlayerList().getPlayers()){
            if(admitted(player,15)||admitted(player,17))owners.add(player);
        }
        if(owners.isEmpty())return;
        var owner=owners.get(Math.floorMod(ownerCursor++,owners.size()));int skill=admitted(owner,15)?15:17;
        int radius=skill==15?10:16;String marker=skill==15?"doubled":"quadried";
        var candidates=new ArrayList<Entity>();
        owner.level().getEntities(EntityTypeTest.forClass(Entity.class),new AABB(owner.position(),owner.position()).inflate(radius+1.0e-6),
                actor->baseEligible(actor)&&!actor.entityTags().contains(marker)&&actor.position().distanceToSqr(owner.position())<=radius*radius
                        &&(projectile(actor)||skill==17&&mob(actor)),candidates,SCAN_BUDGET);
        int remaining=BODY_BUDGET;long start=System.nanoTime();
        for(var actor:candidates){
            if(remaining<(skill==15?1:3))break;
            scanned++;long before=bodies;
            if(baseEligible(actor)&&!actor.entityTags().contains(marker)&&actor.position().distanceToSqr(owner.position())<=radius*radius
                    &&cloneActor(actor,skill,skill==17&&mob(actor),remaining))remaining-=(int)(bodies-before);
            if(System.nanoTime()-start>=SOFT_NANOS)break;
        }
        if(remaining>0&&System.nanoTime()-start<SOFT_NANOS){
            int count=CloneVirtualAbilityStates.duplicateNear(game,owner,skill,remaining,start+SOFT_NANOS);
            remaining-=count;bodies+=count;
            if(remaining>0&&System.nanoTime()-start<SOFT_NANOS)bodies+=CloneFinalAbilityStates.duplicateNear(game,owner,skill,remaining,start+SOFT_NANOS);
        }
    }
    /** Bound helper actors share their canonical gameplay family; never target their visual alone. */
    public boolean protectedFamilyActor(Entity actor){return adapter.protectedActor(actor);}
    private boolean mob(Entity actor){return actor.getType().builtInRegistryHolder().is(MOBS)||adapter.combatBody(actor);}
    private boolean baseEligible(Entity actor){
        return !(actor instanceof ServerPlayer)&&!actor.isRemoved()&&actor.level() instanceof ServerLevel level
                &&level.hasChunkAt(actor.blockPosition())&&!game.items().timeStop().stopped(level)
                &&!game.items().timeStop().frozen(actor)&&!actor.entityTags().contains("time_stopped")
                &&!actor.isSpectator()&&!actor.entityTags().contains("spectator")&&!actor.entityTags().contains("magic_academy")
                &&(game.items().wands().validRoleTarget(actor,true,true)||adapter.combatBody(actor))&&!adapter.protectedActor(actor);
    }
    private boolean projectile(Entity actor){
        if(actor instanceof AbstractArrow arrow&&((WandProjectileMixin.ArrowAccess)(Object)arrow).skywars2$isInGround())return false;
        if(adapter.gameplayActor(actor)||actor instanceof Projectile
                ||actor instanceof net.minecraft.world.entity.vehicle.boat.AbstractBoat
                ||actor instanceof net.minecraft.world.entity.vehicle.minecart.AbstractMinecart
                ||actor instanceof net.minecraft.world.entity.AreaEffectCloud)return true;
        boolean ray=actor.getType().builtInRegistryHolder().is(RAYCASTERS);
        return ray?actor.entityTags().contains("slowraycast"):actor.getType().builtInRegistryHolder().is(PROJECTILES);
    }
    /** Package-only fixture entry, using the installed registry and ordinary eligibility. */
    boolean copyNow(Entity original,int skill){
        if(skill!=15&&skill!=17||!ready()||!baseEligible(original)||original.entityTags().contains(skill==15?"doubled":"quadried"))return false;
        boolean mob=skill==17&&mob(original);
        return (projectile(original)||mob)&&cloneActor(original,skill,mob,BODY_BUDGET);
    }
    private record Node(Entity source,NativeImage nativeImage,ActorImage owner,Entity parent){}
    private List<Node> snapshot(Entity root){
        var result=new ArrayList<Node>();var seen=new IdentityHashMap<Entity,Boolean>();
        snapshot(root,null,result,seen);return List.copyOf(result);
    }
    private void snapshot(Entity source,Entity parent,List<Node> nodes,IdentityHashMap<Entity,Boolean> seen){
        if(seen.put(source,true)!=null)return;
        var image=adapter.capture(source);
        if(image==null)throw new UnsupportedOperationException("No reviewed actor image: "+source.getClass().getName());
        nodes.add(new Node(source,new NativeImage(source),image,parent));
        for(var child:source.getPassengers())if(!(child instanceof Player)&&baseEligible(child))snapshot(child,source,nodes,seen);
    }
    /** An auxiliary hidden gameplay body belongs to its form, never to the proximity scanner. */
    final class Auxiliary {
        private final List<Node> nodes;private final Map<Entity,Entity> family;private boolean retired;
        Auxiliary(List<Node> nodes,Map<Entity,Entity> family){this.nodes=nodes;this.family=family;}
        Entity root(){return family.get(nodes.getFirst().source);}
        void rollback(){if(!retired){retired=true;discard(nodes,family);}}
    }
    Auxiliary copyAuxiliary(Entity original,Consumer<Entity> beforePublish){
        if(!cloning)throw new IllegalStateException("Auxiliary clone outside actor transaction");
        var nodes=snapshot(original);var family=construct(nodes,Vec3.ZERO,null,0);
        var handle=new Auxiliary(nodes,family);
        try{beforePublish.accept(handle.root());install(nodes,family);publish(nodes,family);return handle;}
        catch(RuntimeException failure){try{handle.rollback();}catch(RuntimeException cleanup){failure.addSuppressed(cleanup);}throw failure;}
    }
    private Map<Entity,Entity> construct(List<Node> nodes,Vec3 offset,String marker,int skill){
        var family=new IdentityHashMap<Entity,Entity>();
        try{
            for(var node:nodes){
                var source=node.source;var level=(ServerLevel)source.level();var clone=node.owner.create(source,level);
                if(clone==null||clone==source||clone.getType()!=source.getType()||clone.getClass()!=source.getClass()
                        ||clone.getId()==source.getId()||clone.getUUID().equals(source.getUUID())
                        ||family.values().stream().anyMatch(e->e==clone||e.getId()==clone.getId()||e.getUUID().equals(clone.getUUID())))
                    throw new UnsupportedOperationException("No fresh matching factory: "+source.getClass().getName());
                family.put(source,clone);node.nativeImage.load(clone);
                if(marker!=null)clone.entityTags().add(marker);
                clone.entityTags().remove("doubledcurr");clone.entityTags().remove("quadriedcurr");
                clone.setPos(node.nativeImage.position.add(offset));
                clone.setDeltaMovement(skill==17?spreadMotion(node.nativeImage.motion,source,mob(source)?10:1000,mob(source)?10:1500):node.nativeImage.motion);
                if(!level.hasChunkAt(clone.blockPosition()))throw new IllegalStateException("Clone destination is not resident");
                game.items().spellSources().inherit(source,clone);
            }
            return family;
        }catch(RuntimeException failure){try{discard(nodes,family);}catch(RuntimeException cleanup){failure.addSuppressed(cleanup);}throw failure;}
    }
    private void install(List<Node> nodes,Map<Entity,Entity> family){
        var links=Collections.unmodifiableMap(family);
        for(var node:nodes)node.owner.install(node.source,family.get(node.source),links);
        for(var node:nodes){
            var clone=family.get(node.source);
            if(node.parent!=null&&!clone.startRiding(family.get(node.parent),true,false))throw new IllegalStateException("Clone passenger link rejected");
            node.nativeImage.links(clone,links);
        }
    }
    private void publish(List<Node> nodes,Map<Entity,Entity> family){
        for(var node:nodes){
            var clone=family.get(node.source);copyScores(node.source,clone,mob(node.source)?MOB_SCORES:PROJECTILE_SCORES);
            if(node.source.getTeam()!=null)game.server().getScoreboard().addPlayerToTeam(clone.getStringUUID(),node.source.getTeam());
            if(!((ServerLevel)clone.level()).addFreshEntity(clone)||clone.isRemoved())throw new IllegalStateException("Clone native publication rejected");
        }
    }
    private void discard(List<Node> nodes,Map<Entity,Entity> family){
        RuntimeException failure=null;
        for(int i=nodes.size()-1;i>=0;i--){var node=nodes.get(i);var clone=family.get(node.source);if(clone==null)continue;
            try{node.owner.rollback(clone);}catch(RuntimeException error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            game.items().spellSources().reflected(clone);var board=game.server().getScoreboard();board.resetAllPlayerScores(clone);
            if(clone.getTeam()!=null)board.removePlayerFromTeam(clone.getStringUUID(),clone.getTeam());clone.stopRiding();clone.discard();
        }
        if(failure!=null)throw failure;
    }
    private boolean cloneActor(Entity original,int skill,boolean mob,int budget){
        List<Node> nodes;
        try{nodes=snapshot(original);}catch(UnsupportedOperationException unavailable){unsupported++;return false;}
        var level=(ServerLevel)original.level();var origin=original.position();int count=skill==15?1:3;
        if((long)nodes.size()*count>budget)return false;
        var destinations=new ArrayList<Vec3>();
        for(int i=0;i<count;i++){var destination=mob?origin:spreadPosition(origin,original);
            if(!level.hasChunkAt(BlockPos.containing(destination)))return false;destinations.add(destination);}
        var made=new ArrayList<Map<Entity,Entity>>();String marker=skill==15?"doubled":"quadried";
        cloning=true;
        try{
            for(var destination:destinations){var family=construct(nodes,destination.subtract(origin),marker,skill);made.add(family);install(nodes,family);}
            for(var family:made)publish(nodes,family);
            for(var node:nodes){node.source.entityTags().add(marker);
                if(skill==17){node.source.setDeltaMovement(spreadMotion(node.nativeImage.motion,node.source,10,10));node.source.needsSync=true;}}
            operations++;bodies+=(long)nodes.size()*count;return true;
        }catch(RuntimeException failure){
            for(var family:made)try{discard(nodes,family);}catch(RuntimeException cleanup){failure.addSuppressed(cleanup);}
            if(failure instanceof UnsupportedOperationException)unsupported++;else failed++;
            if(failed+unsupported<=5)SkyWars2.LOGGER.warn("Clone rejected for {}",original.getClass().getName(),failure);
            return false;
        }finally{cloning=false;}
    }
    private void copyScores(Entity source,Entity target,String[] names){
        var board=game.server().getScoreboard();for(var name:names){var objective=board.getObjective(name);
            var score=objective==null?null:board.getPlayerScoreInfo(source,objective);
            if(score!=null)board.getOrCreatePlayerScore(target,objective).set(score.value());}
    }
    static Vec3 spreadPosition(Vec3 p,Entity random){return new Vec3(
            (Math.floor(p.x*100)+random.getRandom().nextInt(401)-200)*.01,
            (Math.floor(p.y*100)+random.getRandom().nextInt(201))*.01,
            (Math.floor(p.z*100)+random.getRandom().nextInt(401)-200)*.01);}
    static Vec3 spreadMotion(Vec3 v,Entity random,int scale,int spread){return new Vec3(
            (Math.floor(v.x*scale)+random.getRandom().nextInt(spread*2+1)-spread)/scale,
            (Math.floor(v.y*scale)+random.getRandom().nextInt(spread*2+1)-spread)/scale,
            (Math.floor(v.z*scale)+random.getRandom().nextInt(spread*2+1)-spread)/scale);}

    /** No MatchEntityState.load: it restores old UUIDs and forgets original owned effects. */
    private static final class NativeImage {
        private final CompoundTag nbt;
        private final Vec3 position,motion;
        private final List<net.minecraft.world.entity.ai.attributes.AttributeInstance> attributes;
        private final List<SynchedEntityData.DataValue<?>> metadata=new ArrayList<>();
        private final MatchEntityRuntime runtime;
        private final Entity projectileOwner;
        private final LivingEntity target;
        private final Entity leashHolder,hooked;
        NativeImage(Entity source){
            var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,source.registryAccess());source.saveWithoutId(out);
            nbt=out.buildResult().copy();nbt.remove("UUID");nbt.remove("uuid");nbt.remove("Passengers");
            position=source.position();motion=source.getDeltaMovement();runtime=new MatchEntityRuntime(source);
            projectileOwner=source instanceof Projectile shot?shot.getOwner():null;target=source instanceof Mob mob?mob.getTargetUnchecked():null;
            leashHolder=source instanceof Leashable leash?leash.getLeashHolder():null;hooked=source instanceof FishingHook hook?hook.getHookedIn():null;
            var values=new ArrayList<net.minecraft.world.entity.ai.attributes.AttributeInstance>();
            if(source instanceof LivingEntity living)BuiltInRegistries.ATTRIBUTE.listElements().forEach(type->{
                if(living.getAttributes().hasAttribute(type)){var saved=new net.minecraft.world.entity.ai.attributes.AttributeInstance(type,ignored->{});
                    saved.replaceFrom(living.getAttribute(type));values.add(saved);}
            });
            attributes=List.copyOf(values);
            for(var value:((PlayerCheckpointAccess.Metadata)source.getEntityData()).skywars2$items())metadata.add(copy(value.value()));
        }
        private static <T> SynchedEntityData.DataValue<T> copy(SynchedEntityData.DataValue<T> v){return new SynchedEntityData.DataValue<>(v.id(),v.serializer(),v.serializer().copy(v.value()));}
        void load(Entity clone){
            UUID uuid=clone.getUUID();int id=clone.getId();
            if(clone instanceof FishingHook)clone.entityTags().add("skywars2.cloned_fishing");
            var player=projectileOwner instanceof Player p?p:null;var primary=player==null?null:player.fishing;
            try{clone.load(TagValueInput.create(ProblemReporter.DISCARDING,clone.registryAccess(),nbt.copy()));}
            finally{if(player!=null)player.fishing=primary;}
            if(clone instanceof FishingHook)clone.entityTags().add("skywars2.cloned_fishing");
            clone.setUUID(uuid);if(clone.getId()!=id)throw new IllegalStateException("Native load changed clone body ID");
            if(clone instanceof LivingEntity living)for(var saved:attributes){
                var map=living.getAttributes();var target=map.getInstance(saved.getAttribute());
                if(target==null){target=new net.minecraft.world.entity.ai.attributes.AttributeInstance(saved.getAttribute(),changed->{
                    map.getAttributesToUpdate().add(changed);if(changed.getAttribute().value().isClientSyncable())map.getAttributesToSync().add(changed);
                });((me.skywars2.mixin.GlyphAiMixin.AttributeAccess)map).skywars2$glyphAttributes().put(saved.getAttribute(),target);}
                target.replaceFrom(saved);
            }
            var values=new ArrayList<SynchedEntityData.DataValue<?>>();for(var value:metadata)values.add(copy(value));
            clone.getEntityData().assignValues(values);runtime.restore(clone);
            if(clone instanceof Projectile shot)shot.setOwner(projectileOwner);
            if(clone instanceof Mob)((ActorCheckpointAccess.Target)clone).skywars2$target(target);
            clone.setPos(position);clone.setDeltaMovement(motion);clone.needsSync=true;
        }
        void links(Entity clone,Map<Entity,Entity> family){
            if(clone instanceof Projectile shot)shot.setOwner(family.getOrDefault(projectileOwner,projectileOwner));
            if(clone instanceof Mob)((ActorCheckpointAccess.Target)clone).skywars2$target((LivingEntity)family.getOrDefault(target,target));
            if(clone instanceof Leashable leash&&leashHolder!=null)leash.setLeashedTo(family.getOrDefault(leashHolder,leashHolder),false);
            if(clone instanceof FishingHook)((ActorCheckpointAccess.Fishing)clone).skywars2$hooked(family.getOrDefault(hooked,hooked));
        }
    }
    public long scannedCount(){return scanned;}
    public long operationCount(){return operations;}
    public long bodyCount(){return bodies;}
    public long unsupportedCount(){return unsupported;}
    public long failureCount(){return failed;}
}
