package me.skywars2.game;

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
import java.util.*;

/** Native old drone producer and its explicitly tagged lineage; no replacement AI or actor lifetime. */
public final class LbcDroneDrops {
    private static final String ACTOR="skywars2.drone_drop",OWNER="skywars2.drone_owner.";
    private static final String HOST="skywars2.drone_host.",CLOSED="skywars2.drone_closed";
    private static final CompoundTag PAYLOAD=parse("""
            {Silent:1b,CustomNameVisible:0b,PersistenceRequired:1b,Health:40.0f,Passengers:[{id:"minecraft:spawner_minecart",Tags:["hohlospawner"],CustomNameVisible:0b,SpawnCount:1,SpawnRange:0,Delay:20,MinSpawnDelay:20,MaxSpawnDelay:20,MaxNearbyEntities:1000,RequiredPlayerRange:50,Passengers:[{id:"minecraft:hoglin",CustomNameVisible:1b,PersistenceRequired:1b,Health:30.0f,IsImmuneToZombification:1b,CustomName:{"italic":false,"text":"український безпілотник"},active_effects:[{id:"minecraft:weakness",amplifier:99b,duration:2000000,show_particles:0b}],attributes:[{id:"minecraft:max_health",base:30}]}],CustomName:{"italic":false,"text":"український безпілотник"},SpawnData:{entity:{id:"minecraft:tnt",CustomNameVisible:0b,fuse:100,Tags:["groundbam"],Passengers:[{id:"minecraft:pig",CustomNameVisible:1b,DeathLootTable:"empty",Health:35.0f,CustomName:{"text":"Українська бiмба","italic":false},attributes:[{id:"max_health",base:35}]}],CustomName:{"text":"Українська бiмба","italic":false}}}}],CustomName:{"italic":false,"text":"український безпілотник"},active_effects:[{id:"minecraft:invisibility",amplifier:10b,duration:2000000,show_particles:0b}],attributes:[{id:"minecraft:max_health",base:40}]}
            """);
    private final GameController game;
    private final Set<Entity> tracked=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<BaseSpawner,MinecartSpawner> producers=new IdentityHashMap<>();
    private final Map<UUID,ServerPlayer> authors=new HashMap<>();
    private final Set<String> deadHosts=new HashSet<>();
    public LbcDroneDrops(GameController game){this.game=game;}
    private static CompoundTag parse(String text){try{return TagParser.parseCompoundFully(text);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new ExceptionInInitializerError(e);}}
    public static CompoundTag payload(){var tag=PAYLOAD.copy();tag.putString("id","minecraft:bat");return tag;}

    public boolean ready(ItemEntity token){
        if(token.isRemoved() || token.getAge()>=5999 || !LbcDroneDropItem.matches(token.getItem())
                || !game.playing() || game.stage()==GameController.Stage.ENDING
                || token.level()!=game.maps().level() || !(token.level() instanceof ServerLevel level)
                || game.items().timeStop().stopped(level))return false;
        for(var tag:token.entityTags())if(tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag))return false;
        return resident(level,new AABB(BlockPos.containing(token.position().add(0,20,0))));
    }
    public boolean spawn(ItemEntity token){
        if(!ready(token))return false;
        var level=(ServerLevel)token.level();
        level.sendParticles(ParticleTypes.EXPLOSION,token.getX(),token.getY()+10,token.getZ(),300,.3,8,.3,0);
        var at=token.position().add(0,20,0);
        if(!Level.isInSpawnableBounds(BlockPos.containing(at)))return true;
        var author=LbcIndirectKills.playerSource(game.items().specialDrops().owner(token));
        if(author!=null)authors.put(author.getUUID(),author);
        var data=payload();enrich(data,author);
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
        if(!managed(source) || !active(source) || !(child instanceof PrimedTnt))return;
        inherit(source,child);
    }
    public void inheritConversion(Entity source,Entity child){
        if(!managed(source) || !active(source) || !(child instanceof LivingEntity))return;
        inherit(source,child);
    }
    private void inherit(Entity source,Entity child){
        child.entityTags().add(ACTOR);
        for(var tag:source.entityTags())if(tag.startsWith(OWNER) || tag.startsWith(HOST) || tag.startsWith("skywars2.items."))child.entityTags().add(tag);
        var actual=LbcIndirectKills.playerSource(owner(source));
        if(actual!=null){child.entityTags().removeIf(tag->tag.startsWith(OWNER));child.entityTags().add(OWNER+actual.getUUID());authors.put(actual.getUUID(),actual);}
        loaded(child);
    }
    public void loaded(Entity actor){
        if(!managed(actor))return;
        if(!active(actor)){actor.discard();return;}
        tracked.add(actor);game.items().timeStop().prepareFrozen(actor);
        if(actor instanceof MinecartSpawner cart){
            if(deadHosts.contains(hostKey(cart)))cart.entityTags().add(CLOSED);
            producers.put(cart.getSpawner(),cart);
        }
    }
    public void unloaded(Entity actor){
        tracked.remove(actor);if(actor instanceof MinecartSpawner cart)producers.remove(cart.getSpawner());
    }
    public boolean canTick(MinecartSpawner cart){
        if(!managed(cart))return true;
        if(!active(cart)){unloaded(cart);cart.discard();return false;}
        producers.put(cart.getSpawner(),cart);
        if(deadHosts.contains(hostKey(cart)))cart.entityTags().add(CLOSED);
        return !isClosed(cart) && !game.items().timeStop().stopped((ServerLevel)cart.level());
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
        if(!managed(actor) || !active(actor))return;
        var key=hostKey(actor);if(key.isEmpty())return;
        deadHosts.add(key);
        for(var cart:List.copyOf(producers.values()))if(key.equals(hostKey(cart)))cart.entityTags().add(CLOSED);
    }
    public void groundbam(PrimedTnt tnt){
        if(managed(tnt) && active(tnt) && !game.items().timeStop().stopped((ServerLevel)tnt.level())
                && tnt.onGround() && tnt.entityTags().remove("groundbam"))tnt.setFuse(2);
    }
    public void tick(){
        for(var actor:List.copyOf(tracked)){
            if(actor.isRemoved()){unloaded(actor);continue;}
            if(!active(actor)){unloaded(actor);actor.discard();continue;}
            if(actor instanceof MinecartSpawner cart && game.server().getTickCount()%20==0
                    && !game.items().timeStop().stopped((ServerLevel)cart.level()) && !keepAlive(cart))cart.kill((ServerLevel)cart.level());
        }
    }
    public void stop(){var actors=List.copyOf(tracked);tracked.clear();producers.clear();
        for(var actor:actors)if(!actor.isRemoved())actor.discard();authors.clear();deadHosts.clear();}
    public List<Entity> snapshot(){return List.copyOf(tracked);}
    public int trackedCount(){return tracked.size();}
}
