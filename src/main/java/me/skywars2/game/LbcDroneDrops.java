package me.skywars2.game;

import me.skywars2.mixin.DroneSpawnerCheckpointAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.vehicle.minecart.MinecartSpawner;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Native drone variants and their explicitly tagged lineage; no replacement AI or actor lifetime. */
public final class LbcDroneDrops {
    private static final String ACTOR="skywars2.drone_drop",OWNER="skywars2.drone_owner.";
    private static final String HOST="skywars2.drone_host.",CLOSED="skywars2.drone_closed";
    private static final CompoundTag PAYLOAD=parse("""
            {Silent:1b,CustomNameVisible:0b,PersistenceRequired:1b,Health:40.0f,Passengers:[{id:"minecraft:spawner_minecart",Tags:["hohlospawner"],CustomNameVisible:0b,SpawnCount:1,SpawnRange:0,Delay:20,MinSpawnDelay:20,MaxSpawnDelay:20,MaxNearbyEntities:1000,RequiredPlayerRange:50,Passengers:[{id:"minecraft:hoglin",CustomNameVisible:1b,PersistenceRequired:1b,Health:30.0f,IsImmuneToZombification:1b,CustomName:{"italic":false,"text":"український безпілотник"},active_effects:[{id:"minecraft:weakness",amplifier:99b,duration:2000000,show_particles:0b}],attributes:[{id:"minecraft:max_health",base:30}]}],CustomName:{"italic":false,"text":"український безпілотник"},SpawnData:{entity:{id:"minecraft:tnt",CustomNameVisible:0b,fuse:100,Tags:["groundbam"],Passengers:[{id:"minecraft:pig",CustomNameVisible:1b,DeathLootTable:"empty",Health:35.0f,CustomName:{"text":"Українська бiмба","italic":false},attributes:[{id:"max_health",base:35}]}],CustomName:{"text":"Українська бiмба","italic":false}}}}],CustomName:{"italic":false,"text":"український безпілотник"},active_effects:[{id:"minecraft:invisibility",amplifier:10b,duration:2000000,show_particles:0b}],attributes:[{id:"minecraft:max_health",base:40}]}
            """);
    private static final CompoundTag BOMBER=parse("""
            {id:"minecraft:hoglin",Tags:["hohlopitek","lucky_mob","actived"],NoGravity:1b,IsImmuneToZombification:1b,active_effects:[{id:"minecraft:instant_damage",duration:2000000,show_particles:0b}],NoAI:1b,Invulnerable:0b,Health:150.0f,attributes:[{id:"minecraft:max_health",base:150}]}
            """);
    private static final CompoundTag BOMB=parse("""
            {id:"minecraft:tnt",CustomNameVisible:0b,fuse:100,Tags:["groundbam"],Passengers:[{id:"minecraft:pig",CustomNameVisible:1b,DeathLootTable:"empty",Health:35.0f,CustomName:{"italic":false,"text":"Українська бiмба"},attributes:[{id:"minecraft:max_health",base:35}]}],CustomName:{"italic":false,"text":"Українська бiмба"}}
            """);
    private final GameController game;
    // Stable identity-table slots preserve native traversal when a captured body is replaced.
    // Values always point at the current body; keys are private registry identity anchors only.
    private IdentityHashMap<Entity,Entity> tracked=new IdentityHashMap<>();
    // Reverse identity index keeps native load/unload O(1), including rebound bodies.
    private IdentityHashMap<Entity,Entity> slots=new IdentityHashMap<>();
    private int checkpointDepth;
    private int cadenceOffset;
    private Map<BaseSpawner,MinecartSpawner> producers=new IdentityHashMap<>();
    private final Map<UUID,ServerPlayer> authors=new HashMap<>();
    private final Set<String> deadHosts=new HashSet<>();
    public LbcDroneDrops(GameController game){this.game=game;}
    private static CompoundTag parse(String text){try{return TagParser.parseCompoundFully(text);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new ExceptionInInitializerError(e);}}
    public static CompoundTag payload(){return payload(false);}
    public static CompoundTag payload(boolean normal){
        var tag=PAYLOAD.copy();tag.putString("id","minecraft:bat");
        if(normal){
            var cart=(CompoundTag)tag.getListOrEmpty("Passengers").get(0);
            var host=(CompoundTag)cart.getListOrEmpty("Passengers").get(0);
            host.putString("DeathLootTable","lbc:hohlopitek");
        }
        return tag;
    }

    /** Source attack hoglin: its own target sets yaw, and native Instant Damage owns its lifespan. */
    public Hoglin spawnBomber(ServerPlayer owner,Vec3 target,float castYaw){
        checkpointDepth++;try{
        var level=owner.level();
        if(!game.playing() || game.stage()==GameController.Stage.ENDING || level!=game.maps().level())return null;
        double angle=Math.toRadians(castYaw+90);
        var at=target.add(-Math.sin(angle)*28,20,Math.cos(angle)*28);
        if(!Level.isInSpawnableBounds(BlockPos.containing(at)) || !resident(level,new AABB(BlockPos.containing(at)).inflate(1)))return null;
        var data=BOMBER.copy();enrich(data,owner);authors.put(owner.getUUID(),owner);
        float yaw=(float)Math.toDegrees(Math.atan2(-(target.x-at.x),target.z-at.z));
        var actor=EntityType.loadEntityRecursive(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),data),
                level,EntitySpawnReason.COMMAND,e->{e.snapTo(at.x,at.y,at.z,yaw,0);return e;});
        if(!(actor instanceof Hoglin hoglin))return null;
        if(!level.tryAddFreshEntityWithPassengers(hoglin)){
            hoglin.getSelfAndPassengers().toList().forEach(e->{unloaded(e);e.discard();});return null;
        }
        return hoglin;

        }finally{checkpointDepth--;}
    }

    private void bomberTick(Hoglin actor){
        if(!actor.isAlive() || !actor.entityTags().contains("hohlopitek"))return;
        var level=(ServerLevel)actor.level();
        if(game.items().timeStop().stopped(level) || game.items().magic().mechanismSuppressedAt(level,actor.position()))return;
        double angle=Math.toRadians(actor.getYRot());
        var forward=new Vec3(-Math.sin(angle),0,Math.cos(angle));
        var left=new Vec3(Math.cos(angle),0,Math.sin(angle));
        var next=actor.position().add(forward);
        var positions=List.of(actor.position().add(left.scale(2)),actor.position().add(left.scale(-2)),actor.position());
        if(!Level.isInSpawnableBounds(BlockPos.containing(next)) || !resident(level,actor.getBoundingBox().move(forward)))return;
        for(var at:positions)if(!Level.isInSpawnableBounds(BlockPos.containing(at))
                || !resident(level,new AABB(BlockPos.containing(at)).inflate(1)))return;
        var author=LbcIndirectKills.playerSource(owner(actor));
        for(var at:positions){
            var data=BOMB.copy();enrich(data,author);
            var bomb=EntityType.loadEntityRecursive(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),data),
                    level,EntitySpawnReason.COMMAND,e->{e.snapTo(at.x,at.y,at.z,e.getYRot(),e.getXRot());return e;});
            if(bomb!=null && !level.tryAddFreshEntityWithPassengers(bomb))
                bomb.getSelfAndPassengers().toList().forEach(e->{unloaded(e);e.discard();});
        }
        actor.snapTo(next.x,next.y,next.z,actor.getYRot(),actor.getXRot());
    }

    public boolean ready(ItemEntity token){
        if(token.isRemoved() || token.getAge()>=5999 || !LbcDroneDropItem.matches(token.getItem())
                || !game.playing() || game.stage()==GameController.Stage.ENDING
                || token.level()!=game.maps().level() || !(token.level() instanceof ServerLevel level)
                || game.items().timeStop().stopped(level))return false;
        for(var tag:token.entityTags())if(tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag))return false;
        return resident(level,new AABB(BlockPos.containing(token.position().add(0,20,0))));
    }
    public boolean spawn(ItemEntity token){
        checkpointDepth++;try{
        if(!ready(token))return false;
        var level=(ServerLevel)token.level();
        level.sendParticles(ParticleTypes.EXPLOSION,token.getX(),token.getY()+10,token.getZ(),300,.3,8,.3,0);
        var at=token.position().add(0,20,0);
        if(!Level.isInSpawnableBounds(BlockPos.containing(at)))return true;
        var author=LbcIndirectKills.playerSource(game.items().specialDrops().owner(token));
        if(author!=null)authors.put(author.getUUID(),author);
        var data=payload(LbcDroneDropItem.isNormal(token.getItem()));enrich(data,author);
        var actor=EntityType.loadEntityRecursive(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),data),
                level,EntitySpawnReason.COMMAND,e->{e.snapTo(at.x,at.y,at.z,e.getYRot(),e.getXRot());return e;});
        if(actor!=null){
            // Bind actual native passenger identity before loaded callbacks see either actor.
            actor.getSelfAndPassengers().filter(e->e instanceof MinecartSpawner).forEach(e->{
                var host=e.getFirstPassenger();
                if(host instanceof Hoglin){var key=HOST+host.getUUID();e.entityTags().add(key);host.entityTags().add(key);}
            });
            if(!level.tryAddFreshEntityWithPassengers(actor))
                actor.getSelfAndPassengers().toList().forEach(e->{unloaded(e);e.discard();});
        }
        return true;

        }finally{checkpointDepth--;}
    }
    private void enrich(CompoundTag entity,ServerPlayer author){
        var tags=entity.getListOrEmpty("Tags");tags.add(StringTag.valueOf(ACTOR));
        tags.add(StringTag.valueOf(game.items().epoch()));
        if(author!=null)tags.add(StringTag.valueOf(OWNER+author.getUUID()));entity.put("Tags",tags);
        for(var raw:entity.getListOrEmpty("Passengers"))if(raw instanceof CompoundTag passenger)enrich(passenger,author);
        if(entity.getStringOr("id","").equals("minecraft:spawner_minecart")){
            enrich(entity.getCompoundOrEmpty("SpawnData").getCompoundOrEmpty("entity"),author);
            for(var raw:entity.getListOrEmpty("SpawnPotentials"))if(raw instanceof CompoundTag entry)
                enrich(entry.getCompoundOrEmpty("data").getCompoundOrEmpty("entity"),author);
        }
    }
    public static boolean managed(Entity actor){
        return actor!=null && actor.entityTags().contains(ACTOR) && (actor instanceof Bat
                || actor instanceof MinecartSpawner || actor instanceof Hoglin || actor instanceof Zoglin
                || actor instanceof Pig || actor instanceof ZombifiedPiglin || actor instanceof PrimedTnt);
    }
    private boolean active(Entity actor){return game.playing() && game.stage()!=GameController.Stage.ENDING
            && actor.level()==game.maps().level() && game.items().currentEpoch(actor);}
    public LivingEntity retainedOwner(Entity actor){
        if(!managed(actor))return null;
        var reflected=game.items().electro().reflectedOwner(actor);if(reflected!=null)return reflected;
        for(var tag:actor.entityTags())if(tag.startsWith(OWNER))try{
            var id=UUID.fromString(tag.substring(OWNER.length()));var online=game.server().getPlayerList().getPlayer(id);
            return online==null?authors.get(id):online;
        }catch(IllegalArgumentException ignored){}
        return null;
    }
    public LivingEntity owner(Entity actor){
        if(actor instanceof ItemEntity token && LbcDroneDropItem.matches(token.getItem()))return game.items().specialDrops().owner(token);
        if(!managed(actor))return null;
        var reflected=game.items().electro().reflectedOwner(actor);if(reflected!=null)return reflected;
        if(actor instanceof PrimedTnt tnt && tnt.getOwner()!=null)return tnt.getOwner();
        return retainedOwner(actor);
    }
    public boolean wild(Entity actor){return managed(actor) || actor instanceof ItemEntity token && LbcDroneDropItem.matches(token.getItem());}
    public boolean canAffect(Entity source,Entity target){return !managed(source)
            || active(source) && target!=null && game.items().wands().validTarget(target);}
    public boolean canTarget(Entity source,LivingEntity target){return target==null || canAffect(source,target);}
    public void inheritChild(Entity source,Entity child){
        checkpointDepth++;try{
        if(!managed(source) || !active(source) || !(child instanceof PrimedTnt))return;
        inherit(source,child);

        }finally{checkpointDepth--;}
    }
    public void inheritConversion(Entity source,Entity child){
        checkpointDepth++;try{
        if(!managed(source) || !active(source) || !(child instanceof LivingEntity))return;
        inherit(source,child);

        }finally{checkpointDepth--;}
    }
    private void inherit(Entity source,Entity child){
        child.entityTags().add(ACTOR);
        for(var tag:source.entityTags())if(tag.startsWith(OWNER) || tag.startsWith(HOST) || tag.startsWith("skywars2.items."))child.entityTags().add(tag);
        var actual=LbcIndirectKills.playerSource(owner(source));
        if(actual!=null){child.entityTags().removeIf(tag->tag.startsWith(OWNER));child.entityTags().add(OWNER+actual.getUUID());authors.put(actual.getUUID(),actual);}
        loaded(child);
    }
    public void loaded(Entity actor){
        checkpointDepth++;try{
        if(!managed(actor))return;
        if(!active(actor)){actor.discard();return;}
        if(!slots.containsKey(actor)){tracked.put(actor,actor);slots.put(actor,actor);}game.items().timeStop().prepareFrozen(actor);
        if(actor instanceof MinecartSpawner cart){
            if(deadHosts.contains(hostKey(cart)))cart.entityTags().add(CLOSED);
            producers.put(cart.getSpawner(),cart);
        }

        }finally{checkpointDepth--;}
    }
    public void unloaded(Entity actor){
        checkpointDepth++;try{
        var slot=slots.remove(actor);if(slot!=null)tracked.remove(slot);if(actor instanceof MinecartSpawner cart)producers.remove(cart.getSpawner());

        }finally{checkpointDepth--;}
    }
    public boolean canTick(MinecartSpawner cart){
        checkpointDepth++;try{
        if(!managed(cart))return true;
        if(!active(cart)){unloaded(cart);cart.discard();return false;}
        producers.put(cart.getSpawner(),cart);
        if(deadHosts.contains(hostKey(cart)))cart.entityTags().add(CLOSED);
        return !isClosed(cart) && !game.items().timeStop().stopped((ServerLevel)cart.level());

        }finally{checkpointDepth--;}
    }
    public boolean canSpawnAt(BaseSpawner spawner,ServerLevel level,AABB bounds){
        return !producers.containsKey(spawner) || resident(level,bounds.inflate(1));
    }
    private static boolean resident(ServerLevel level,AABB bounds){
        for(int x=net.minecraft.util.Mth.floor(bounds.minX)>>4;x<=net.minecraft.util.Mth.floor(bounds.maxX)>>4;x++)
            for(int z=net.minecraft.util.Mth.floor(bounds.minZ)>>4;z<=net.minecraft.util.Mth.floor(bounds.maxZ)>>4;z++)
                if(level.getChunkSource().getChunkNow(x,z)==null)return false;
        return true;
    }
    private static String hostKey(Entity actor){
        for(var tag:actor.entityTags())if(tag.startsWith(HOST))return tag;
        return "";
    }
    public boolean isClosed(MinecartSpawner cart){return managed(cart) && cart.entityTags().contains(CLOSED);}
    /** User override: unrelated mobs, distance and chunk sleep cannot substitute for this host. */
    public boolean keepAlive(MinecartSpawner cart){return !isClosed(cart) && !deadHosts.contains(hostKey(cart));}
    public void hostDied(LivingEntity actor){
        checkpointDepth++;try{
        if(!managed(actor) || !active(actor))return;
        var key=hostKey(actor);if(key.isEmpty())return;
        deadHosts.add(key);
        for(var cart:List.copyOf(producers.values()))if(key.equals(hostKey(cart)))cart.entityTags().add(CLOSED);

        }finally{checkpointDepth--;}
    }
    public void groundbam(PrimedTnt tnt){
        checkpointDepth++;try{
        if(managed(tnt) && active(tnt) && !game.items().timeStop().stopped((ServerLevel)tnt.level())
                && tnt.onGround() && tnt.entityTags().remove("groundbam"))tnt.setFuse(2);

        }finally{checkpointDepth--;}
    }
    public void tick(){
        checkpointDepth++;try{
        for(var actor:snapshot()){
            if(actor.isRemoved()){unloaded(actor);continue;}
            if(!active(actor)){unloaded(actor);actor.discard();continue;}
            if(actor instanceof Hoglin hoglin && Math.floorMod(game.server().getTickCount()-cadenceOffset,5)==0)bomberTick(hoglin);
            if(actor instanceof MinecartSpawner cart && Math.floorMod(game.server().getTickCount()-cadenceOffset,20)==0
                    && !game.items().timeStop().stopped((ServerLevel)cart.level()) && !keepAlive(cart))cart.kill((ServerLevel)cart.level());
        }

        }finally{checkpointDepth--;}
    }
    public void stop(){
        checkpointDepth++;try{var actors=snapshot();tracked.clear();slots.clear();producers.clear();
        for(var actor:actors)if(!actor.isRemoved())actor.discard();authors.clear();deadHosts.clear();cadenceOffset=0;
        }finally{checkpointDepth--;}
    }
    public List<Entity> snapshot(){return List.copyOf(tracked.values());}

    /** Native bodies/AI, relationships and direct/reflected owners have separate canonical owners. */
    void checkpointBoundary(){
        if(checkpointDepth!=0)throw new IllegalStateException("Finish drone tick/spawn/host callback before checkpointing");
    }
    /** Called by the narrow native BaseSpawner wrapper, including callbacks during entity creation. */
    public void checkpointNativeEnter(BaseSpawner producer){checkpointDepth++;}
    public void checkpointNativeExit(BaseSpawner producer){checkpointDepth--;}
    public boolean checkpointNativeTracked(BaseSpawner producer){return producers.containsKey(producer);}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){return new Checkpoint(links);}
    final class Checkpoint {
        private final IdentityHashMap<Entity,Entity> actorTable;
        private final IdentityHashMap<Entity,MatchDamageSources.Ref> actors=new IdentityHashMap<>();
        private final List<ProducerImage> producerImages;
        private final Map<UUID,MatchDamageSources.Ref> authorImages=new HashMap<>();
        private final Set<String> dead;
        private final int phase;
        private record ProducerImage(MatchDamageSources.Ref cart,SpawnerImage runtime) { }
        @SuppressWarnings("unchecked") Checkpoint(MatchSpellCheckpoint.Links links){
            checkpointBoundary();actorTable=(IdentityHashMap<Entity,Entity>)tracked.clone();
            tracked.forEach((slot,actor)->actors.put(slot,links.key(actor)));
            producerImages=new ArrayList<>();
            var aliases=new IdentityHashMap<net.minecraft.world.level.SpawnData,net.minecraft.world.level.SpawnData>();
            producers.forEach((spawner,cart)->{
                if(cart.getSpawner()!=spawner)throw new IllegalStateException("Drone producer must belong to its native cart");
                producerImages.add(new ProducerImage(links.key(cart),new SpawnerImage(spawner,aliases)));
            });
            authors.forEach((id,author)->authorImages.put(id,links.key(author)));
            dead=Set.copyOf(deadHosts);phase=Math.floorMod(game.server().getTickCount()-cadenceOffset,20);
        }
        @SuppressWarnings("unchecked") Runnable prepare(MatchSpellCheckpoint.Links links){
            checkpointBoundary();
            var nextTracked=(IdentityHashMap<Entity,Entity>)actorTable.clone();
            var nextSlots=new IdentityHashMap<Entity,Entity>();
            actors.forEach((slot,ref)->{var actor=links.get(ref);nextTracked.put(slot,actor);nextSlots.put(actor,slot);});
            var nextProducers=new IdentityHashMap<BaseSpawner,MinecartSpawner>();var nativeInstalls=new ArrayList<Runnable>();
            var aliases=new IdentityHashMap<net.minecraft.world.level.SpawnData,net.minecraft.world.level.SpawnData>();
            for(var image:producerImages){var cart=(MinecartSpawner)links.get(image.cart);
                nextProducers.put(cart.getSpawner(),cart);nativeInstalls.add(image.runtime.prepare(cart.getSpawner(),aliases));}
            var nextAuthors=new HashMap<UUID,ServerPlayer>();
            authorImages.forEach((id,ref)->nextAuthors.put(id,(ServerPlayer)links.get(ref)));
            return ()->{
                checkpointBoundary();nativeInstalls.forEach(Runnable::run);
                tracked=nextTracked;slots=nextSlots;producers=nextProducers;
                authors.clear();authors.putAll(nextAuthors);deadHosts.clear();deadHosts.addAll(dead);
                cadenceOffset=game.server().getTickCount()-phase;
            };
        }
    }
    /** Runtime which native NBT omits or normalizes; preserve null data, exact ints and data aliases. */
    private static final class SpawnerImage {
        private final int delay,min,max,count,near,range,spawnRange;
        private final double spin,oldSpin;
        private final net.minecraft.world.level.SpawnData next;
        private final net.minecraft.util.random.WeightedList<net.minecraft.world.level.SpawnData> potentials;
        private static net.minecraft.world.level.SpawnData copy(net.minecraft.world.level.SpawnData value,
                IdentityHashMap<net.minecraft.world.level.SpawnData,net.minecraft.world.level.SpawnData> aliases){
            return value==null?null:aliases.computeIfAbsent(value,v->new net.minecraft.world.level.SpawnData(
                    v.getEntityToSpawn().copy(),v.getCustomSpawnRules(),v.getEquipment()));
        }
        SpawnerImage(BaseSpawner source,
                IdentityHashMap<net.minecraft.world.level.SpawnData,net.minecraft.world.level.SpawnData> aliases){
            var a=(DroneSpawnerCheckpointAccess)source;delay=a.skywars2$delay();min=a.skywars2$min();max=a.skywars2$max();
            count=a.skywars2$count();near=a.skywars2$near();range=a.skywars2$range();spawnRange=a.skywars2$spawnRange();
            spin=a.skywars2$spin();oldSpin=a.skywars2$oldSpin();
            if(a.skywars2$display()!=null)throw new IllegalStateException("Drone display cache requires a native display-state owner before checkpointing");
            next=copy(a.skywars2$next(),aliases);potentials=a.skywars2$potentials().map(v->copy(v,aliases));
        }
        Runnable prepare(BaseSpawner source,
                IdentityHashMap<net.minecraft.world.level.SpawnData,net.minecraft.world.level.SpawnData> aliases){
            var a=(DroneSpawnerCheckpointAccess)source;
            var nextData=copy(next,aliases);var entries=potentials.map(v->copy(v,aliases));
            return ()->{a.skywars2$delay(delay);a.skywars2$min(min);a.skywars2$max(max);a.skywars2$count(count);
                a.skywars2$near(near);a.skywars2$range(range);a.skywars2$spawnRange(spawnRange);
                a.skywars2$spin(spin);a.skywars2$oldSpin(oldSpin);a.skywars2$display(null);
                a.skywars2$next(nextData);a.skywars2$potentials(entries);};
        }
    }

    public int trackedCount(){return tracked.size();}
}
