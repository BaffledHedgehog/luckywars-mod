package me.skywars2.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

/** Independent apocalypse actors: native worms, the exact Stevari variant, and nine hostile clouds. */
public final class LbcApocalypseActors {
    private static final String MANAGED="skywars2.apocalypse.source_actor";
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final Set<String> EXCLUDED=Set.of("rcdenyalways","friendly_tuntija","strict_map_object");
    private static final CompoundTag STEVARI=parse("""
{Tags:["fat_skeleton"],DeathLootTable:"lbc:fatskeleton",PersistenceRequired:1b,CustomNameVisible:1b,CustomName:{"italic":false,"translate":"fat_skeleton"},equipment:{mainhand:{id:"minecraft:bone",components:{"minecraft:enchantments":{"minecraft:knockback":1},"minecraft:custom_name":{"italic":false,"translate":"omega_bone"},"minecraft:attribute_modifiers":[{type:"minecraft:attack_damage",slot:"mainhand",id:"minecraft:base_attack_damage",amount:7,operation:"add_value"},{type:"minecraft:attack_speed",slot:"mainhand",id:"minecraft:base_attack_speed",amount:-3.5,operation:"add_value"},{type:"minecraft:knockback_resistance",slot:"mainhand",id:"sdiuirj4938uhf",amount:1,operation:"add_value"},{type:"minecraft:movement_speed",slot:"mainhand",id:"idjf9u8weu89",amount:-0.7,operation:"add_multiplied_base"}]},count:1},feet:{id:"minecraft:bone_meal",count:64},legs:{id:"minecraft:bone",count:32},chest:{id:"minecraft:iron_chestplate",count:1},head:{id:"minecraft:skeleton_skull",count:1}},drop_chances:{mainhand:1.0f,legs:1.0f,feet:1.0f,chest:0.0f,head:0.0f},attributes:[{id:"minecraft:movement_speed",base:0.6},{id:"minecraft:armor",base:20},{id:"minecraft:attack_knockback",base:100},{id:"minecraft:armor_toughness",base:5}]}
        """);
    private enum Type { WORM, STEVARI, CLOUD }
    private static final class Actor {
        final LbcApocalypseEvents.Authority authority;final LbcSpellSource.Source source;
        final Type type;final LbcSauvojen.Kind kind;int lifetime;
        Actor(LbcApocalypseEvents.Authority authority,LbcSpellSource.Source source,Type type,LbcSauvojen.Kind kind){this.authority=authority;this.source=source;this.type=type;this.kind=kind;}
    }
    private final GameController game;
    private IdentityHashMap<Entity,Actor> actors=new IdentityHashMap<>();
    private final Map<UUID,Actor> unloaded=new HashMap<>();
    private final Set<Terrain> terrain=Collections.newSetFromMap(new IdentityHashMap<>());
    private int phase;private int callbackDepth;private boolean rewinding;
    public LbcApocalypseActors(GameController game){this.game=game;}
    private static CompoundTag parse(String value){try{return TagParser.parseCompoundFully(value);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}}
    private static void load(Entity entity,CompoundTag tag){entity.load(TagValueInput.create(ProblemReporter.DISCARDING,entity.registryAccess(),SourceKnockbackBalance.entity(tag.copy())));}
    private boolean live(LbcApocalypseEvents.Authority authority,ServerLevel level){return authority!=null&&game.playing()&&game.stage()!=GameController.Stage.ENDING&&game.items().currentEpoch(authority.epoch())&&LbcDimensionItems.ordinary(level);}
    private boolean spawnable(LbcApocalypseEvents.Authority authority,ServerLevel level,Vec3 at){return live(authority,level)&&Level.isInSpawnableBounds(BlockPos.containing(at))&&level.hasChunkAt(BlockPos.containing(at));}
    private boolean admit(Entity entity,LbcApocalypseEvents.Authority authority,Type type,LbcSauvojen.Kind kind,Vec3 at){
        if(rewinding)return false;
        callbackDepth++;try{
        entity.setPos(at);entity.entityTags().add(MANAGED);game.items().markCurrentItemEpoch(entity);
        var exclusions=new HashSet<>(EXCLUDED);
        if(kind==LbcSauvojen.Kind.TERRA||kind==LbcSauvojen.Kind.ELECTRO)exclusions.add("summoned_cultist");
        if(kind==LbcSauvojen.Kind.ELECTRO){exclusions.add("transformator");exclusions.add("cyborg_boss");}
        var source=new LbcSpellSource.Source(entity.getUUID(),authority.author(),authority.epoch(),authority.cause(),exclusions,true);
        var state=new Actor(authority,source,type,kind);actors.put(entity,state);game.items().spellSources().bind(entity,source);game.items().timeStop().prepareFrozen(entity);
        if(((ServerLevel)entity.level()).addFreshEntity(entity))return true;
        actors.remove(entity);entity.discard();return false;

        }finally{callbackDepth--;}
    }
    /** Source NBT Size20 means native slime size21; native split descendants retain native vanilla sizes/AI. */
    public boolean spawnWorm(LbcApocalypseEvents.Authority authority,ServerLevel level,Vec3 at){
        if(!spawnable(authority,level,at))return false;
        var slime=EntityTypes.SLIME.create(level,EntitySpawnReason.COMMAND);if(slime==null)return false;
        load(slime,parse("{Size:20,Tags:[\"worm\",\"lucky_mob\"]}"));
        return admit(slime,authority,Type.WORM,null,at);
    }
    /** Caller retains source execute-at executor and performs its following kill only after the summon. */
    public boolean spawnStevari(LbcApocalypseEvents.Authority authority,ServerLevel level,Vec3 at){
        if(!spawnable(authority,level,at))return false;
        var skeleton=EntityTypes.SKELETON.create(level,EntitySpawnReason.COMMAND);if(skeleton==null)return false;
        load(skeleton,STEVARI);return admit(skeleton,authority,Type.STEVARI,null,at);
    }
    public boolean spawnCloud(LbcApocalypseEvents.Authority authority,ServerLevel level,Vec3 at){
        if(!spawnable(authority,level,at))return false;
        return spawnCloud(authority,level,at,LbcSauvojen.Kind.values()[level.getRandom().nextInt(9)]);
    }
    public boolean spawnCloud(LbcApocalypseEvents.Authority authority,ServerLevel level,Vec3 at,LbcSauvojen.Kind kind){
        if(kind==null||!spawnable(authority,level,at))return false;
        var display=EntityTypes.ITEM_DISPLAY.create(level,EntitySpawnReason.COMMAND);if(display==null)return false;
        var name=kind.name().toLowerCase(Locale.ROOT);
        String cadence=switch(kind){case RUBY,AIR,STONE->"area_3s";case TELEPORT->"area_4s";case FIRE->"rc_5t";case BLIND->"rc_10t";default->"area_1s\",\"rc_1sec";};
        load(display,parse("{interpolation_duration:1,teleport_duration:1,item_display:\"fixed\",transformation:[0.0f,0.0f,2.5f,0.0f,1.7678f,-1.7678f,0.0f,0.0f,1.7678f,1.7678f,0.0f,0.0f,0.0f,0.0f,0.0f,1.0f],item:{id:\"minecraft:carrot_on_a_stick\",components:{\"minecraft:item_model\":\"lbc:st/"+name+"\"},count:1},Tags:[\"sauvojen_"+name+"\",\""+cadence+"\",\"savoujen_cloud\",\"magic\"]}"));
        return admit(display,authority,Type.CLOUD,kind,at);
    }
    public boolean ruby(LbcApocalypseEvents.Authority authority,ServerLevel level,Vec3 at,Vec3 direction){
        if(!spawnable(authority,level,at))return false;
        var marker=new Marker(EntityTypes.MARKER,level);
        if(!admit(marker,authority,Type.STEVARI,null,at))return false;
        var source=actors.get(marker).source;
        var shot=game.items().ruby().flying(level,at,direction,source);
        actors.remove(marker);marker.discard();
        return shot!=null&&!shot.isRemoved();
    }
    /** Infinity selects every source cloud in range, including clouds produced by other events. */
    public int convertClouds(ServerLevel level,Vec3 at,double radius,ServerPlayer author){
        int converted=0;
        for(var entity:List.copyOf(actors.keySet())){
            var state=actors.get(entity);
            if(state==null||state.type!=Type.CLOUD||entity.isRemoved()||entity.level()!=level||entity.position().distanceToSqr(at)>radius*radius)continue;
            game.items().sauvojenBoss().dropCloudWand(level,entity.position(),state.kind,author);
            actors.remove(entity);entity.discard();converted++;
        }
        return converted;
    }
    public static boolean managed(Entity entity){return entity!=null&&entity.entityTags().contains(MANAGED);}
    public ServerPlayer owner(Entity entity){var state=actors.get(entity);return state==null?game.items().spellSources().owner(entity):game.server().getPlayerList().getPlayer(state.authority.author());}
    public boolean canAffect(Entity actor,Entity target){return game.items().spellSources().canAffect(actor,target);}
    public void targetChanged(Mob actor,LivingEntity target){if(managed(actor)&&target!=null&&!canAffect(actor,target))actor.setTarget(null);}
    public int count(){return actors.size();}
    List<Entity> snapshot(){return List.copyOf(actors.keySet());}
    private LivingEntity nearest(Entity cloud){
        LivingEntity nearest=null;double best=Double.POSITIVE_INFINITY;
        var targets=Collections.newSetFromMap(new IdentityHashMap<LivingEntity,Boolean>());targets.addAll(game.items().lifetimes().loadedTargets());targets.addAll(((ServerLevel)cloud.level()).players());
        for(var target:targets){if(target.level()!=cloud.level()||!target.getType().builtInRegistryHolder().is(MOBS)||!canAffect(cloud,target))continue;
            double distance=cloud.position().distanceToSqr(target.position());if(distance<best){best=distance;nearest=target;}}
        return nearest;
    }
    public void tick(){
        if(rewinding)return;
        callbackDepth++;try{
        if(!game.playing()||game.stage()==GameController.Stage.ENDING){stop();return;}

        phase=(phase+1)%240;
        for(var actor:List.copyOf(actors.keySet())){
            var state=actors.get(actor);if(state==null)continue;var level=(ServerLevel)actor.level();
            if(actor.isRemoved()){actors.remove(actor);continue;}
            if(!live(state.authority,(ServerLevel)actor.level())){actors.remove(actor);actor.discard();continue;}
            if(game.items().timeStop().stopped(level)||actor.entityTags().contains("time_stopped")||!level.hasChunkAt(actor.blockPosition()))continue;
            if(state.type==Type.WORM&&actor instanceof Slime slime&&slime.isAlive()&&phase%5==0){
                var center=BlockPos.containing(actor.getX(),actor.getY()+3,actor.getZ());var work=new Terrain(state.authority,level,center.offset(-5,-5,-5),center.offset(5,5,5));terrain.add(work);game.items().stonekivi().queuedWork(work);
            }else if(state.type==Type.CLOUD)tickCloud(actor,state,level);
        }

        }finally{callbackDepth--;}
    }
    private void tickCloud(Entity actor,Actor state,ServerLevel level){
        if(game.items().magic().suppressedAt(level,actor.position())){actors.remove(actor);actor.discard();return;}
        if(phase%state.kind.cadence!=0)return;
        var target=nearest(actor);
        boolean outerFacing=state.kind==LbcSauvojen.Kind.RUBY||state.kind==LbcSauvojen.Kind.AIR||state.kind==LbcSauvojen.Kind.STONE||state.kind==LbcSauvojen.Kind.TELEPORT;
        if(outerFacing&&target==null)return;
        state.lifetime+=state.kind.cadence;
        if(target==null)return;
        var direction=target.getEyePosition().subtract(actor.position()).normalize();
        actor.setYRot((float)Math.toDegrees(Math.atan2(-direction.x,direction.z)));actor.setXRot((float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance())));
        game.items().sauvojen().emitHostile(level,state.kind,actor.position(),direction,state.source);
        if(state.lifetime>=200){var at=actor.position();LbcOverload.sound(level,at,"minecraft:block.enchantment_table.use",2,1.3f,0,0);level.sendParticles(ParticleTypes.WITCH,at.x,at.y,at.z,30,.1,.1,.1,.5);actors.remove(actor);actor.discard();}
    }
    private static boolean resident(ServerLevel level,BlockPos from,BlockPos to){for(int x=from.getX()>>4;x<=to.getX()>>4;x++)for(int z=from.getZ()>>4;z<=to.getZ()>>4;z++)if(level.getChunkSource().getChunkNow(x,z)==null)return false;return true;}
    private final class Terrain implements LbcStonekivi.Work {
        final LbcApocalypseEvents.Authority authority;final ServerLevel level;final BlockPos from,to;boolean cancelled;
        Terrain(LbcApocalypseEvents.Authority authority,ServerLevel level,BlockPos from,BlockPos to){this.authority=authority;this.level=level;this.from=from;this.to=to;}
        @Override public boolean ready(){return cancelled||!live(authority,level)||!game.items().timeStop().stopped(level)&&resident(level,from,to);}
        @Override public boolean step(){
        callbackDepth++;try{if(!cancelled&&live(authority,level))game.items().indirectKills().trapChanges(level,authority.cause(),()->LbcDropBlocks.fill(level,from,to,Blocks.AIR,true));cancel();return true;
        }finally{callbackDepth--;}
    }
        @Override public void cancel(){
        callbackDepth++;try{cancelled=true;terrain.remove(this);
        }finally{callbackDepth--;}
    }
    }
    /** Legacy slime split copies native properties, but not worm behavior or this service's private identity. */
    public void splitChild(Mob parent,Mob child){
        var state=actors.get(parent);
        if(state==null||state.type!=Type.WORM||!(parent instanceof Slime)||!(child instanceof Slime))return;
        child.entityTags().removeAll(Set.of(MANAGED,"worm","lucky_mob"));
        game.items().spellSources().inherit(parent,child);
    }
    public void loaded(Entity entity){
        callbackDepth++;try{if(rewinding||MatchActorCheckpoint.restoring(entity))return;
        if(!managed(entity))return;
        var state=actors.get(entity);if(state==null)state=unloaded.remove(entity.getUUID());
        if(state==null||!(entity.level() instanceof ServerLevel level)||!live(state.authority,level)){entity.discard();return;}
        actors.put(entity,state);game.items().spellSources().bind(entity,state.source);game.items().timeStop().prepareFrozen(entity);

        }finally{callbackDepth--;}
    }
    public void unloaded(Entity entity){
        callbackDepth++;try{if(rewinding||MatchActorCheckpoint.restoring(entity))return;var state=actors.remove(entity);if(state!=null&&entity.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK)unloaded.put(entity.getUUID(),state);
        }finally{callbackDepth--;}
    }
    /** Event authority persists when its causal author disconnects; epoch/stop still terminate it. */
    public void forget(ServerPlayer author){}
    public void stop(){
        callbackDepth++;try{rewinding=false;for(var entity:List.copyOf(actors.keySet()))if(!entity.isRemoved())entity.discard();actors.clear();unloaded.clear();for(var work:List.copyOf(terrain))work.cancel();phase=0;
        }finally{callbackDepth--;}
    }

    boolean captureReady(){return callbackDepth==0&&terrain.isEmpty()&&!rewinding;}
    void checkpointBoundary(){if(callbackDepth!=0)throw new IllegalStateException("Finish apocalypse actor callbacks before checkpointing");}
    void cancelFutureTerrainForRollback(){checkpointBoundary();for(var work:List.copyOf(terrain))work.cancel();}
    void prepareFrozen(){checkpointBoundary();cancelFutureTerrainForRollback();rewinding=true;}
    void publishFrozen(){checkpointBoundary();rewinding=false;}
    void cancelFrozen(){checkpointBoundary();rewinding=false;}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){checkpointBoundary();if(!captureReady())throw new IllegalStateException("Complete apocalypse actor terrain before capture");return new Checkpoint(links);}
    final class Checkpoint {
        private record Age(Actor original,int lifetime){}
        private final MatchSpellCheckpoint.IdentityImage<Entity,Actor> registry;
        private final Map<UUID,Actor> dormant=new HashMap<>(unloaded);
        private final List<Age> ages=new ArrayList<>();private final int savedPhase=phase;
        Checkpoint(MatchSpellCheckpoint.Links links){
            for(var entity:actors.keySet())if(links.key(entity).historical()!=null)throw new IllegalStateException("Capture live event actor bodies");
            registry=new MatchSpellCheckpoint.IdentityImage<>(actors,links);var seen=Collections.newSetFromMap(new IdentityHashMap<Actor,Boolean>());
            for(var actor:actors.values())if(seen.add(actor))ages.add(new Age(actor,actor.lifetime));
            for(var actor:unloaded.values())if(seen.add(actor))ages.add(new Age(actor,actor.lifetime));
        }
        Runnable prepare(MatchSpellCheckpoint.Links links){checkpointBoundary();var restored=registry.restore(links);
            return ()->{for(var age:ages)age.original.lifetime=age.lifetime;actors=restored;unloaded.clear();unloaded.putAll(dormant);phase=savedPhase;};}
    }
}
