package me.skywars2.game;

import java.util.*;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.vehicle.minecart.MinecartSpawner;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.*;

/** Ten finite old scroll payloads use the common fair, rewindable native ray queue. */
final class LbcBalanceRays {
    enum Kind {
        LIGHTNING("lightning_old"),TNT("tnts_old"),CREEPERS("creeperz_old"),WITHER("wither1_old"),HEAL("heal1_old"),
        FIRESTORM("arrows1_old"),LAVA("lava1_old"),ANVILS("anvils_old"),SKELETONS("skeletons_old"),DESTROY("trap2_old");
        final String flag;Kind(String flag){this.flag=flag;}
    }
    private static final TagKey<net.minecraft.world.level.block.Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private final GameController game;
    private final Predicate<ServerPlayer> admitted;
    private final Set<LbcExasperato.RayTicket> pending=new LinkedHashSet<>();
    private final Map<Entity,UUID> actors=new LinkedHashMap<>();
    private final List<Emitter> emitters=new ArrayList<>();
    private final Set<UUID> retiredProducers=new LinkedHashSet<>();
    private int callbacks;
    private static final class Emitter {
        final UUID owner;ServerPlayer author;MinecartSpawner body;final ServerLevel level;final String epoch;final Kind kind;final Vec3 at;int age,born;
        Emitter(ServerPlayer p,Kind kind,Vec3 at,String epoch,MinecartSpawner body){owner=p.getUUID();author=p;level=(ServerLevel)body.level();this.kind=kind;this.at=at;this.epoch=epoch;this.body=body;born=p.level().getServer().getTickCount();}
    }
    LbcBalanceRays(GameController game,Predicate<ServerPlayer> admitted){this.game=game;this.admitted=admitted;}
    static Kind kind(ItemStack stack){if(!LbcBalanceItems.flag(stack,"raycastspell")||!LbcBalanceItems.flag(stack,"spell"))return null;for(var kind:Kind.values())if(LbcBalanceItems.flag(stack,kind.flag))return kind;return null;}
    boolean use(ServerPlayer p,ItemStack stack){var kind=kind(stack);if(kind==null)return false;
        game.items().exasperato().replayableSamples(p,p.getEyePosition(),p.getLookAngle(),100,new Ray(p,kind,stack.getItem()));return true;}
    private final class Ray implements LbcExasperato.ReplayableRay {
        final UUID owner;final ServerLevel level;final String epoch;final Kind kind;final Item used;final Vec3 direction;
        LbcExasperato.RayTicket ticket;
        Ray(ServerPlayer p,Kind kind,Item used){owner=p.getUUID();level=p.level();epoch=game.items().epoch();this.kind=kind;this.used=used;direction=p.getLookAngle();}
        public void submitted(LbcExasperato.RayTicket ticket){this.ticket=ticket;pending.add(ticket);}
        public boolean block(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){return !state.is(AIRS);}
        public boolean target(Entity entity){return entity instanceof LivingEntity&&!entity.getUUID().equals(owner)&&entity.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().validRoleTarget(entity,false);}
        public double sampleStep(){return 1;}
        public HitResult sampleAt(Vec3 point){
            level.sendParticles(ParticleTypes.END_ROD,true,false,point.x,point.y,point.z,1,0,0,0,.01);
            if(!level.getBlockState(BlockPos.containing(point)).is(AIRS))return new BlockHitResult(point.subtract(direction),net.minecraft.core.Direction.UP,BlockPos.containing(point),false);
            var found=level.getEntities((Entity)null,new AABB(point.x,point.y,point.z,point.x+1,point.y+1,point.z+1),this::target);return found.isEmpty()?null:new EntityHitResult(found.getFirst(),point);
        }
        public void trail(Vec3 from,Vec3 to){level.sendParticles(ParticleTypes.END_ROD,true,false,to.x,to.y,to.z,1,0,0,0,.01);}
        public void complete(HitResult hit){callbacks++;try{
            if(!pending.remove(ticket)||hit==null)return;var p=game.server().getPlayerList().getPlayer(owner);
            if(p==null||p.level()!=level||!game.items().currentEpoch(epoch)||!admitted.test(p))return;
            var stack=p.getMainHandItem();if(stack.getItem()!=used||kind(stack)!=kind)return;
            impact(p,kind,hit.getLocation());stack.shrink(1);p.getInventory().setChanged();
        }finally{callbacks--;}}
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){return current->()->{};}
    }
    private boolean target(ServerPlayer p,LivingEntity e){return game.items().wands().validRoleTarget(e,false)&&!LbcGreed.active(e);}
    private void impact(ServerPlayer p,Kind kind,Vec3 at){switch(kind){
        case LIGHTNING->{for(var e:p.level().getEntities((Entity)null,new AABB(at,at).inflate(5),e->e.position().distanceToSqr(at)<=25&&e.getType()!=EntityTypes.LIGHTNING_BOLT&&game.items().wands().validRoleTarget(e,false))){bolt(p,e.position());}bolt(p,at);}
        case TNT->{for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){if(x==0&&y==0&&z==0)continue;var tnt=new PrimedTnt(p.level(),at.x,at.y+.5,at.z,p);tnt.setFuse(56);tnt.setDeltaMovement(x*.4,y*.4,z*.4);insert(p,tnt);}}
        case CREEPERS->{for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var creeper=EntityTypes.CREEPER.create(p.level(),EntitySpawnReason.TRIGGERED);if(creeper!=null){load(creeper,"{ExplosionRadius:2b,Fuse:60}");creeper.setPos(at.add(x,1,z));game.items().creeperDrops().adoptEgg(creeper,p);insert(p,creeper);}}}
        case WITHER,HEAL->{var cause=game.items().indirectKills().wildCause(p);for(var e:p.level().getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(10),e->e.position().distanceToSqr(at)<=100&&target(p,e))){
            if(kind==Kind.WITHER)game.items().indirectKills().wildEffect(e,ItemEffectLevels.create(MobEffects.WITHER,1200,0,false,false),cause);
            else{game.items().indirectKills().withCause(e,cause,()->MobEffects.INSTANT_HEALTH.value().applyInstantaneousEffect(p.level(),p,p,e,2,1));game.items().indirectKills().wildEffect(e,ItemEffectLevels.create(MobEffects.INSTANT_HEALTH,14,2,false,false),cause);}
        }}
        case LAVA->{var min=BlockPos.containing(at.add(-.5,-.5,-.5));var max=BlockPos.containing(at.add(.5,.5,.5));for(var pos:BlockPos.betweenClosed(min,max))if(p.level().hasChunkAt(pos)&&p.level().getBlockState(pos).is(Blocks.AIR))p.level().setBlock(pos,Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL,1),3);}
        case DESTROY->{var origin=BlockPos.containing(at);game.items().indirectKills().trapChanges(p,()->{for(var pos:BlockPos.betweenClosed(origin.offset(-2,-2,-2),origin.offset(2,2,2)))if(p.level().hasChunkAt(pos))p.level().setBlock(pos,Blocks.AIR.defaultBlockState(),2|16|32);});}
        case SKELETONS->{int[][] offsets={{0,0},{0,1},{0,-1},{-1,0},{1,0}};for(int i=0;i<offsets.length;i++){var skeleton=EntityTypes.SKELETON.create(p.level(),EntitySpawnReason.TRIGGERED);if(skeleton==null)continue;boolean leader=i==0;
            load(skeleton,"{Glowing:"+(leader?"1b":"0b")+",equipment:{mainhand:{id:'minecraft:bow',count:1,components:{'minecraft:enchantments':{'minecraft:power':"+(leader?2:1)+",'minecraft:punch':1}}},head:{id:'minecraft:iron_helmet',count:1,components:{'minecraft:unbreakable':{}}}},Health:"+(leader?30:10)+"f,attributes:[{id:'minecraft:max_health',base:"+(leader?30:10)+"}],drop_chances:{head:0f,mainhand:0f}}");skeleton.setPos(at.add(offsets[i][0],1,offsets[i][1]));game.items().creatureDrops().adoptEggSkeleton(skeleton,p);insert(p,skeleton);}}
        case FIRESTORM,ANVILS->rain(p,kind,at.add(0,50,0));
    }}
    private void rain(ServerPlayer p,Kind kind,Vec3 at){
        var body=EntityTypes.SPAWNER_MINECART.create(p.level(),EntitySpawnReason.TRIGGERED);if(body==null)return;
        String data=kind==Kind.ANVILS
            ?"{NoGravity:1b,Silent:1b,Invulnerable:1b,SpawnCount:50s,SpawnRange:8s,Delay:10s,MinSpawnDelay:19980s,MaxSpawnDelay:19980s,MaxNearbyEntities:999s,RequiredPlayerRange:999s,Tags:['killedanvils','skywars2.balance.old_rain'],SpawnData:{entity:{id:'minecraft:falling_block',BlockState:{id:'minecraft:anvil'},Time:1,DropItem:0b,HurtEntities:1b,FallHurtMax:19,FallHurtAmount:19f,Motion:[0d,-2d,0d]}}}"
            :"{Invulnerable:1b,NoGravity:1b,Tags:['killedcart','skywars2.balance.old_rain'],SpawnCount:1s,SpawnRange:10s,Delay:0s,MinSpawnDelay:2s,MaxSpawnDelay:2s,MaxNearbyEntities:1000s,RequiredPlayerRange:100s,SpawnData:{entity:{id:'minecraft:small_fireball',Tags:['killedarrow'],Motion:[0d,-2d,0d]}}}";
        load(body,data);body.setPos(at);insert(p,body);if(body.isRemoved())return;
        // Source schedule replaces one cleanup function for every same-kind producer in the level.
        for(var emitter:emitters)if(emitter.kind==kind&&emitter.level==p.level()){emitter.age=0;emitter.born=game.server().getTickCount();}
        emitters.add(new Emitter(p,kind,at,game.items().epoch(),body));
    }
    private void bolt(ServerPlayer p,Vec3 at){var bolt=EntityTypes.LIGHTNING_BOLT.create(p.level(),EntitySpawnReason.TRIGGERED);if(bolt!=null){bolt.setPos(at);bolt.setCause(p);insert(p,bolt);}}
    private void load(Entity entity,String data){try{entity.load(TagValueInput.create(ProblemReporter.DISCARDING,entity.registryAccess(),TagParser.parseCompoundFully(data)));}catch(Exception e){throw new IllegalStateException("Invalid fixed old scroll actor preset",e);}}
    private void insert(ServerPlayer p,Entity body){game.items().summons().track(p,body);body.entityTags().add("skywars2.balance.old_scroll_actor");game.items().spellSources().bind(body,new LbcSpellSource.Source(body.getUUID(),p.getUUID(),game.items().epoch(),game.items().indirectKills().wildCause(p),Set.of(),true));game.items().timeStop().prepareFrozen(body);actors.put(body,p.getUUID());if(!((ServerLevel)body.level()).addFreshEntity(body)){actors.remove(body);body.discard();}}
    void tick(){callbacks++;try{
        actors.keySet().removeIf(body->body.isRemoved()&&body.getRemovalReason()!=Entity.RemovalReason.UNLOADED_TO_CHUNK);
        for(var emitter:List.copyOf(emitters)){
            if(emitter.body.isRemoved()&&emitter.body.getRemovalReason()!=Entity.RemovalReason.UNLOADED_TO_CHUNK){emitters.remove(emitter);continue;}
            if(!game.items().currentEpoch(emitter.epoch)){emitter.body.discard();emitters.remove(emitter);continue;}
            if(emitter.body.getRemovalReason()==Entity.RemovalReason.UNLOADED_TO_CHUNK||game.items().timeStop().stopped(emitter.level)||game.items().timeStop().frozen(emitter.body))continue;
            if(emitter.born!=game.server().getTickCount())emitter.age++;
            if(emitter.age>=(emitter.kind==Kind.ANVILS?20:500)){retiredProducers.add(emitter.body.getUUID());emitter.body.discard();actors.remove(emitter.body);emitters.remove(emitter);}
        }
    }finally{callbacks--;}}
    static boolean managedRain(Entity body){return body instanceof MinecartSpawner&&body.entityTags().contains("skywars2.balance.old_rain");}
    LbcCloneSkills.ActorImage cloneRain(Entity body){
        var saved=emitters.stream().filter(e->e.body==body).findFirst().orElse(null);if(saved==null)return null;
        int age=saved.age,born=saved.born;
        return CloneComplexProjectileStates.image(clone->{
            var next=new Emitter(saved.author,saved.kind,clone.position(),saved.epoch,(MinecartSpawner)clone);next.age=age;next.born=born;
            actors.put(clone,next.owner);emitters.add(next);
        },clone->{actors.remove(clone);emitters.removeIf(e->e.body==clone);});
    }
    void loaded(Entity body){
        if(MatchActorCheckpoint.restoring(body))return;
        if(!body.entityTags().contains("skywars2.balance.old_rain")||!(body instanceof MinecartSpawner cart))return;
        if(retiredProducers.contains(body.getUUID())||!game.playing()||!game.items().currentEpoch(body)){body.discard();return;}
        for(var emitter:emitters)if(emitter.body.getUUID().equals(body.getUUID())){actors.remove(emitter.body);emitter.body=cart;actors.put(cart,emitter.owner);break;}
    }
    void unloaded(Entity body){}
    void forget(ServerPlayer p){/* Published descendants keep their accepted UUID authority across disconnect. */}
    void stop(){for(var ticket:List.copyOf(pending))ticket.cancel();pending.clear();emitters.clear();retiredProducers.clear();var bodies=List.copyOf(actors.keySet());actors.clear();bodies.forEach(Entity::discard);}
    MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){
        if(callbacks!=0)throw new IllegalStateException("Finish Balance callbacks before checkpointing");
        var savedTickets=List.copyOf(pending);var savedActors=new LinkedHashMap<MatchDamageSources.Ref,UUID>();actors.forEach((body,owner)->savedActors.put(links.key(body),owner));
        var savedEmitters=List.copyOf(emitters);var ages=savedEmitters.stream().mapToInt(e->e.age).toArray();var born=savedEmitters.stream().mapToInt(e->game.server().getTickCount()-e.born).toArray();var authors=savedEmitters.stream().map(e->links.key(e.author)).toList();var carts=savedEmitters.stream().map(e->links.key(e.body)).toList();var retired=Set.copyOf(retiredProducers);
        return current->{var bodies=new LinkedHashMap<Entity,UUID>();savedActors.forEach((body,owner)->bodies.put(current.get(body),owner));var resolved=authors.stream().map(ref->(ServerPlayer)current.get(ref)).toList();var producers=carts.stream().map(ref->(MinecartSpawner)current.get(ref)).toList();return ()->{pending.clear();pending.addAll(savedTickets);actors.clear();actors.putAll(bodies);emitters.clear();emitters.addAll(savedEmitters);retiredProducers.clear();retiredProducers.addAll(retired);for(int i=0;i<ages.length;i++){var emitter=savedEmitters.get(i);emitter.age=ages[i];emitter.author=resolved.get(i);emitter.body=producers.get(i);emitter.born=game.server().getTickCount()-born[i];}};};
    }
}
