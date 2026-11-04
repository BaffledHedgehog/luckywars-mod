package me.skywars2.game;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Skill36: extract a soul once, gather/orbit it, then hand ONE shot to the ordinary Psycho owner. */
public final class LbcPsychblindSkill {
    public static final EntityType<Doppel> TYPE=registerType();
    public static final String CLAIM="doppeled";
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    /** Parent owns selection and role admission. Nondamaging extraction includes allies;
     * only the terminal target predicate excludes allies. Neither adds a MAG/TECH gate. */
    public interface Policy {
        boolean selected(ServerPlayer player);
        boolean acquirable(ServerPlayer player,LivingEntity candidate);
        boolean target(ServerPlayer player,LivingEntity candidate);
    }
    public static final class Doppel extends Projectile implements PolymerEntity {
        boolean gathering=true;
        int advancedTick=Integer.MIN_VALUE;
        Doppel(EntityType<? extends Doppel> type,Level level){super(type,level);}
        @Override protected void defineSynchedData(SynchedEntityData.Builder builder){}
        @Override public EntityType<?> getPolymerEntityType(PacketContext context){return EntityTypes.MARKER;}
        @Override public boolean shouldBeSaved(){return false;}
        @Override public boolean isPushedByFluid(){return false;}
    }
    private static EntityType<Doppel> registerType(){
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Identifier.parse("skywars2:psychblind_doppel"));
        var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,
                EntityType.Builder.<Doppel>of(Doppel::new,MobCategory.MISC)
                        .sized(.1f,.1f).clientTrackingRange(8).updateInterval(1).build(key));
        PolymerEntityUtils.registerType(type);return type;
    }
    static void register(){}
    private final GameController game;
    private final Policy policy;
    private final LinkedHashSet<UUID> owners=new LinkedHashSet<>();
    private final LinkedHashSet<Doppel> bodies=new LinkedHashSet<>();
    private Dispatcher queued;
    private boolean restoring;
    private int lastTick=Integer.MIN_VALUE;
    LbcPsychblindSkill(GameController game,Policy policy){this.game=game;this.policy=Objects.requireNonNull(policy);}

    private boolean admitted(ServerPlayer p){
        return p!=null&&p.isAlive()&&game.playing()&&!game.maps().busy()
                &&game.canUseGameplayItems(p)&&!game.isObserver(p)&&policy.selected(p);
    }
    private boolean paused(ServerPlayer p){
        return game.items().timeStop().stopped(p.level())||game.items().timeStop().frozen(p)
                ||game.items().wands().inputTimeStopped(p);
    }
    /** Selection/reconcile hooks enroll only selected owners; tick never scans the player roster. */
    public void reconcile(ServerPlayer p){if(admitted(p))owners.add(p.getUUID());else clear(p.getUUID());}
    public boolean enrolled(ServerPlayer p){return owners.contains(p.getUUID());}
    public void clear(UUID owner){
        owners.remove(owner);
        for(var d:List.copyOf(bodies))if(d.getOwner()!=null&&owner.equals(d.getOwner().getUUID()))remove(d);
        // A consumed global claim belongs to the original native actor, not this visual's lifetime.
    }
    /** Call for EVERY leaving/dying player, including a borrowed source who never selected skill36. */
    public void leaveOrDeath(ServerPlayer p){clear(p.getUUID());p.entityTags().remove(CLAIM);}
    public void stop(){
        if(queued!=null)queued.cancel();
        for(var d:List.copyOf(bodies))remove(d);
        owners.clear();lastTick=Integer.MIN_VALUE;
    }
    public void tick(){
        int tick=game.server().getTickCount();
        if(restoring||lastTick==tick||!game.playing()||game.maps().busy()
                ||!game.server().tickRateManager().runsNormally())return;
        lastTick=tick;
        for(var id:List.copyOf(owners)){
            var p=game.server().getPlayerList().getPlayer(id);
            if(!admitted(p))clear(id);
        }
        // A dispatcher may finish over several ticks under shared CPU pressure. Accepted jobs persist.
        if(queued==null&&(!owners.isEmpty()||!bodies.isEmpty())){
            queued=new Dispatcher();game.items().stonekivi().queuedWork(queued);
        }
    }
    private record Job(UUID owner,Doppel body,LivingEntity source){}
    private final class Dispatcher implements LbcStonekivi.Work {
        final ArrayDeque<Job> jobs=new ArrayDeque<>();
        boolean cancelled,stepping;
        Dispatcher(){
            for(var id:owners)jobs.add(new Job(id,null,null));
            for(var d:bodies)jobs.add(new Job(null,d,null));
        }
        @Override public boolean ready(){
            if(restoring)return false;
            if(cancelled||jobs.isEmpty())return true;
            // Rotate paused producers without spending the shared worker's write budget on them.
            for(var j:jobs){var p=player(j);
                if(p==null||!admitted(p)||j.body!=null&&!valid(p,j.body)||!held(p,j))return true;
            }
            return false;
        }
        private ServerPlayer player(Job j){
            if(j.body!=null)return j.body.getOwner() instanceof ServerPlayer p?p:null;
            return game.server().getPlayerList().getPlayer(j.owner);
        }
        private boolean held(ServerPlayer p,Job j){
            return paused(p)||j.body!=null&&(game.items().timeStop().frozen(j.body)
                    ||j.body.entityTags().contains("time_stopped"));
        }
        @Override public boolean step(){
            if(stepping)throw new IllegalStateException("Reentrant Psychblind work step");
            stepping=true;
            try{
                if(cancelled)return true;
                int remaining=jobs.size();
                while(remaining-->0){
                    var j=jobs.removeFirst();var p=player(j);
                    if(p!=null&&admitted(p)&&(j.body==null||valid(p,j.body))&&held(p,j)){jobs.addLast(j);continue;}
                    if(j.body==null){if(p!=null&&admitted(p)&&owners.contains(j.owner)){
                        if(j.source==null)discover(p);else extract(p,j.source);
                    }}
                    else advance(p,j.body);
                    break;
                }
                if(jobs.isEmpty()){if(queued==this)queued=null;return true;}
                return false;
            }finally{stepping=false;}
        }
        /** A held ability is capture-safe between callbacks; emptiness controls completion only. */
        @Override public boolean snapshotIdle(){return !stepping;}
        @Override public void cancel(){cancelled=true;if(queued==this)queued=null;}
        @Override public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            checkpointBoundary();
            boolean savedCancelled=cancelled;
            return captureJobs(savedCancelled,graph);
        }
        private record SavedJob(UUID owner,MatchDamageSources.Ref body,MatchDamageSources.Ref source){}
        private MatchQueuedWorkCheckpoint.Image captureJobs(boolean savedCancelled,MatchQueuedWorkCheckpoint.Graph graph){
            var saved=jobs.stream().map(j->new SavedJob(j.owner,graph.links.key(j.body),graph.links.key(j.source))).toList();
            return links->{
                var restored=new ArrayList<Job>();
                for(var j:saved)restored.add(new Job(j.owner,(Doppel)links.get(j.body),(LivingEntity)links.get(j.source)));
                return ()->{jobs.clear();jobs.addAll(restored);cancelled=savedCancelled;if(!cancelled)queued=this;};
            };
        }
    }

    private boolean candidate(ServerPlayer p,LivingEntity e){
        return e.getType().builtInRegistryHolder().is(MOBS)&&e!=p&&e.isAlive()&&!e.isRemoved()&&!e.entityTags().contains("spectator")
                &&e.position().distanceToSqr(p.position())<=24*24
                &&policy.acquirable(p,e);
    }
    private void discover(ServerPlayer p){
        var candidates=p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(24),
                e->candidate(p,e)&&!e.entityTags().contains(CLAIM));
        for(var source:candidates)queued.jobs.addLast(new Job(p.getUUID(),null,source));
    }
    private void extract(ServerPlayer p,LivingEntity source){
        // One publication per shared fair step, even for a densely populated 24-block source sphere.
        if(source.entityTags().contains(CLAIM)||!candidate(p,source)||source.level()!=p.level())return;
        Vec3 at=source.getEyePosition().add(0,1,0);
        if(!p.level().hasChunkAt(BlockPos.containing(at)))return;
        var d=new Doppel(TYPE,p.level());d.setOwner(p);d.setPos(at);d.setNoGravity(true);
        d.setYRot(yaw(p.getEyePosition().subtract(at)));d.setXRot(-90);
        d.entityTags().add("slowraycast");d.entityTags().add("doppel");
        game.items().markCurrentItemEpoch(d);
        if(!p.level().addFreshEntity(d))return;
        bodies.add(d);source.entityTags().add(CLAIM);
        LbcOverload.sound(p,at,"minecraft:particle.soul_escape",1,1,0);
    }

    private boolean valid(ServerPlayer p,Doppel d){
        return admitted(p)&&owners.contains(p.getUUID())&&p.level()==d.level()
                &&!d.isRemoved()&&game.items().currentEpoch(d)&&bodies.contains(d);
    }
    private static float yaw(Vec3 direction){return (float)Math.toDegrees(Math.atan2(-direction.x,direction.z));}
    private static void aim(Doppel d,Vec3 direction){
        d.setYRot(yaw(direction));d.setXRot((float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance())));
    }
    private void advance(ServerPlayer p,Doppel d){
        if(!valid(p,d)){remove(d);return;}
        int tick=game.server().getTickCount();
        if(d.advancedTick==tick)return;
        d.advancedTick=tick;
        if(!p.level().hasChunkAt(d.blockPosition()))return;
        if(d.gathering){
            // Source combines current facing with owner-facing once, then uses eight .25 samples.
            Vec3 heading=Vec3.directionFromRotation(d.getXRot(),d.getYRot());
            Vec3 delta=p.getEyePosition().subtract(d.position()).normalize().add(heading);
            if(delta.lengthSqr()<1.0e-12)delta=p.getEyePosition().subtract(d.position());
            Vec3 step=delta.normalize().scale(.25);aim(d,step);
            for(int i=0;i<8;i++){
                ink(p,d.position());
                if(d.position().distanceToSqr(p.position())<=25){d.gathering=false;return;}
                Vec3 next=d.position().add(step);
                if(!p.level().hasChunkAt(BlockPos.containing(next)))return;
                d.setPos(next);
            }
            return;
        }
        var targets=p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(10),
                e->e.getType().builtInRegistryHolder().is(MOBS)&&e!=p&&e.position().distanceToSqr(p.position())<=100&&policy.target(p,e)
                        &&game.items().psycho().doppelTarget(p,e));
        targets.sort(Comparator.comparingDouble(e->e.position().distanceToSqr(p.position())));
        if(!targets.isEmpty()){
            // Deliberate repair: lore says nearest; source copied stale entity Rotation over its facing.
            var direction=targets.getFirst().getEyePosition().subtract(d.position());
            if(game.items().psycho().emitDoppel(p,d.position(),direction)){remove(d);return;}
        }
        Vec3 radial=d.position().subtract(p.position());
        float angle=yaw(radial)-4;
        Vec3 at=p.position().add(0,3,0).add(Vec3.directionFromRotation(0,angle).scale(4));
        if(p.level().hasChunkAt(BlockPos.containing(at))){d.setPos(at);d.setYRot(angle);d.setXRot(0);}
        ink(p,d.position());
    }
    private void ink(ServerPlayer p,Vec3 at){
        p.level().sendParticles(ParticleTypes.SQUID_INK,at.x,at.y,at.z,1,0,0,0,0);
        p.level().sendParticles(ParticleTypes.GLOW_SQUID_INK,true,false,at.x,at.y,at.z,1,0,0,0,0);
    }
    private void remove(Doppel d){bodies.remove(d);if(!d.isRemoved())d.discard();}
    public void loaded(Entity e){if(e instanceof Doppel d)bodies.add(d);}
    public void unloaded(Entity e){if(e instanceof Doppel d)bodies.remove(d);}
    public int count(){return bodies.size();}
    static boolean cloneRecognizes(Entity e){return e instanceof Doppel;}
    LbcCloneSkills.ActorImage cloneImage(Entity original){
        if(!(original instanceof Doppel d)||!bodies.contains(d))return null;
        boolean gathering=d.gathering;
        return CloneComplexProjectileStates.image(clone->{var n=(Doppel)clone;
            n.gathering=gathering;n.advancedTick=game.server().getTickCount();bodies.add(n);
        },clone->bodies.remove(clone));
    }
    void checkpointBoundary(){
        if(!game.server().isSameThread()||queued!=null&&queued.stepping)
            throw new IllegalStateException("Capture Psychblind between native work callbacks");
    }
    public void prepareFrozen(){checkpointBoundary();restoring=true;if(queued!=null)queued.cancel();owners.clear();bodies.clear();}
    public void publishFrozen(){checkpointBoundary();restoring=false;lastTick=game.server().getTickCount();}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){checkpointBoundary();return new Checkpoint(links);}
    final class Checkpoint {
        private record Body(MatchDamageSources.Ref ref,boolean gathering){}
        private final List<UUID> selected=List.copyOf(owners);
        private final List<Body> actors;
        Checkpoint(MatchSpellCheckpoint.Links links){
            actors=bodies.stream().map(d->new Body(links.key(d),d.gathering)).toList();
        }
        Runnable prepare(MatchSpellCheckpoint.Links links){
            checkpointBoundary();
            var restored=new LinkedHashMap<Doppel,Boolean>();
            for(var image:actors)restored.put((Doppel)links.get(image.ref),image.gathering);
            return ()->{owners.clear();owners.addAll(selected);bodies.clear();
                restored.forEach((d,gathering)->{d.gathering=gathering;
                    d.advancedTick=game.server().getTickCount();bodies.add(d);});};
        }
    }
}
