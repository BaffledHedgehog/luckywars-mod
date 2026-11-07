package me.skywars2.game;

import java.util.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.Display.BlockDisplay;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Source cabbage: eight samples per dispatch, inbound children and native damaging fireworks. */
public final class LbcKapusta {
    private static final String TAG="skywars2.kapusta",ROCKET="skywars2.kapusta.firework";
    private static final Codec<Flight> FLIGHT_CODEC=RecordCodecBuilder.create(instance->instance.group(
            net.minecraft.core.UUIDUtil.CODEC.fieldOf("author").forGetter(Flight::author),
            Codec.LONG.fieldOf("order").forGetter(s->s.cause().order()),
            Codec.BOOL.fieldOf("projectile").forGetter(s->s.cause().projectile()),
            Codec.STRING.fieldOf("epoch").forGetter(Flight::epoch),Codec.BOOL.fieldOf("primary").forGetter(Flight::primary),
            Codec.INT.fieldOf("age").forGetter(Flight::age),Codec.LONG.fieldOf("rng").forGetter(Flight::rng),
            Codec.BOOL.optionalFieldOf("jack",false).forGetter(Flight::jack))
            .apply(instance,(author,order,projectile,epoch,primary,age,rng,jack)->new Flight(author,
                    new LbcIndirectKills.Owner(author,order,true,projectile),epoch,primary,age,rng,jack)));
    private static final AttachmentType<Flight> DATA=AttachmentRegistry.create(
            Identifier.parse("skywars2:kapusta"),builder->builder.persistent(FLIGHT_CODEC));
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final CompoundTag FIREWORK=fireworkPreset(false),JACK_FIREWORK=fireworkPreset(true);
    private static CompoundTag fireworkPreset(boolean jack){
        try{return TagParser.parseCompoundFully("{Life:1,LifeTime:1,ShotAtAngle:1b,FireworksItem:{id:'minecraft:firework_rocket',components:{'minecraft:fireworks':{explosions:[{shape:'large_ball',colors:["+(jack?16732416:5111552)+"],fade_colors:["+(jack?16730112:2606080)+"],has_trail:1b,has_twinkle:1b}]}},count:1}}");}
        catch(Exception error){throw new ExceptionInInitializerError(error);}
    }
    // This bounds work, never the number of accepted actors or offspring.
    private static final int DISPATCH_BUDGET=256;
    public record Flight(UUID author,LbcIndirectKills.Owner cause,String epoch,boolean primary,int age,long rng,boolean jack){
        public Flight(UUID author,LbcIndirectKills.Owner cause,String epoch,boolean primary,int age,long rng){this(author,cause,epoch,primary,age,rng,false);}
    }
    public record Spawn(ServerLevel level,Vec3 at,float yaw,float pitch,Flight state){}
    private final GameController game;
    private final Map<Entity,Flight> flights=new LinkedHashMap<>();
    private final Deque<BlockDisplay> ready=new ArrayDeque<>();
    private final Deque<Spawn> pending=new ArrayDeque<>();
    private int lastTick=Integer.MIN_VALUE;
    private boolean restoring;
    public LbcKapusta(GameController game){this.game=game;}
    public static void register(){LbcKapustaItem.register();}
    public static boolean jack(ItemStack stack){return !stack.isEmpty()&&ItemData.intValue(stack,"jack_o_lantern",0)==1;}
    public static boolean matches(ItemStack stack){return !stack.isEmpty()&&(ItemData.intValue(stack,"kapusta",0)==1||jack(stack));}
    public static boolean isManaged(Entity body){return body!=null&&body.entityTags().contains(TAG);}
    public static boolean wild(Entity body){return isManaged(body);}
    static boolean cloneRecognizes(Entity body){return isManaged(body);}
    public LbcIndirectKills.Owner cause(Entity body){var state=flights.get(body);return state==null?null:state.cause();}
    public Flight state(Entity body){return flights.get(body);}
    public List<BlockDisplay> actors(){return flights.keySet().stream().filter(BlockDisplay.class::isInstance).map(BlockDisplay.class::cast).toList();}
    public List<Entity> nativeActors(){return List.copyOf(flights.keySet());}
    public int count(){return ready.size();}
    public int pendingCount(){return pending.size();}
    public List<Spawn> pendingViews(){return List.copyOf(pending);}
    void transformOrigins(ServerLevel level,java.util.function.UnaryOperator<Vec3> transform){
        var transformed=new ArrayDeque<Spawn>();
        for(var value:pending){
            if(value.level()!=level){transformed.addLast(value);continue;}
            var at=transform.apply(value.at());if(at==null)continue;
            var oldDirection=Vec3.directionFromRotation(value.pitch(),value.yaw());
            var tip=transform.apply(value.at().add(oldDirection));
            var direction=tip==null?oldDirection:tip.subtract(at);
            float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
            float pitch=(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));
            transformed.addLast(new Spawn(level,at,yaw,pitch,value.state()));
        }
        pending.clear();pending.addAll(transformed);
    }
    private boolean active(ServerLevel level){return game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&!game.maps().busy()&&level==game.maps().level()&&LbcDimensionItems.ordinary(level)
            &&LbcBedwarsRoleGuards.publication(level);}
    private boolean admitted(ServerPlayer owner){return active(owner.level())&&owner.isAlive()&&!owner.isRemoved()
            &&!owner.isSpectator()&&!game.isObserver(owner)&&!game.awaitingRespawn(owner.getUUID())
            &&game.canUseGameplayItems(owner)&&game.items().wands().validRoleTarget(owner,false)
            &&!game.items().actionsBlocked(owner)&&!game.items().wands().inputTimeStopped(owner)
            &&!game.items().timeStop().stopped(owner.level())&&!game.server().tickRateManager().isFrozen()
            &&game.server().tickRateManager().runsNormally();}
    public boolean use(ServerPlayer owner,InteractionHand hand){
        var held=owner.getItemInHand(hand);if(!matches(held)||!admitted(owner))return false;
        var state=new Flight(owner.getUUID(),game.items().indirectKills().wildCause(owner),game.items().epoch(),!jack(held),0,owner.getRandom().nextLong(),jack(held));
        if(spawn(owner.level(),owner.getEyePosition(),owner.getYRot(),owner.getXRot(),state)==null)return false;
        LbcOverload.sound(owner,owner.position(),"minecraft:entity.snowball.throw",.5f,.4f/(owner.getRandom().nextFloat()*.4f+.8f),0);
        owner.awardStat(Stats.ITEM_USED.get(held.getItem()));held.consume(1,owner);return true;
    }
    BlockDisplay launch(ServerPlayer owner,Vec3 at,float yaw,float pitch,boolean primary){
        return spawn(owner.level(),at,yaw,pitch,new Flight(owner.getUUID(),game.items().indirectKills().wildCause(owner),
                game.items().epoch(),primary,0,owner.getRandom().nextLong()));
    }
    private BlockDisplay spawn(ServerLevel level,Vec3 at,float yaw,float pitch,Flight state){
        if(!level.hasChunkAt(BlockPos.containing(at)))return null;
        var body=new BlockDisplay(EntityTypes.BLOCK_DISPLAY,level);body.setPos(at);body.setYRot(yaw);body.setXRot(pitch);
        body.setBlockState(state.jack()?Blocks.JACK_O_LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.CarvedPumpkinBlock.FACING,net.minecraft.core.Direction.WEST):Blocks.MELON.defaultBlockState());body.setViewRange(10);
        body.setPosRotInterpolationDuration(state.jack()?1:5);body.setTransformationInterpolationDuration(state.jack()?1:5);
        if(state.jack())body.setTransformation(new com.mojang.math.Transformation(new org.joml.Vector3f(-.5f,-.5f,-.5f),new org.joml.Quaternionf(),new org.joml.Vector3f(1,1,1),new org.joml.Quaternionf()));
        body.entityTags().add("slowraycast");body.entityTags().add("rare");body.entityTags().add(state.jack()?"jack_o_lantern":"kapusta");body.entityTags().add("shield_blockable");
        if(state.primary())body.entityTags().add("ass");install(body,state);
        if(!level.addFreshEntity(body)){unloaded(body);body.discard();return null;}return body;
    }
    private void install(Entity body,Flight state){
        body.entityTags().add(TAG);game.items().markCurrentItemEpoch(body);write(body,state);var previous=flights.put(body,state);
        if(previous==null&&body instanceof BlockDisplay display)ready.addLast(display);
    }
    private boolean paused(Entity body){return game.items().timeStop().frozen(body)
            ||game.items().timeStop().stopped((ServerLevel)body.level())||body.entityTags().contains("time_stopped");}
    /** A round-robin resident index owns dispatch; newborn children start with the next round. */
    public void tick(){
        if(restoring||lastTick==game.server().getTickCount())return;lastTick=game.server().getTickCount();
        if(game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally())return;
        int publications=Math.min(DISPATCH_BUDGET,pending.size());
        while(publications-->0){var next=pending.removeFirst();
            if(!active(next.level())||!game.items().currentEpoch(next.state().epoch()))continue;
            if(game.items().timeStop().stopped(next.level())||!next.level().hasChunkAt(BlockPos.containing(next.at()))){pending.addLast(next);continue;}
            if(spawn(next.level(),next.at(),next.yaw(),next.pitch(),next.state())==null)pending.addLast(next);
        }
        int remaining=Math.min(DISPATCH_BUDGET,ready.size());
        while(remaining-->0){var body=ready.removeFirst();var state=flights.get(body);if(state==null)continue;
            if(body.isRemoved()){unloaded(body);continue;}
            var level=(ServerLevel)body.level();
            if(!active(level)||!game.items().currentEpoch(state.epoch())){retire(body);continue;}
            if(paused(body)||!level.hasChunkAt(body.blockPosition())){ready.addLast(body);continue;}
            var at=body.position();var step=body.getLookAngle().scale(.2);boolean waiting=false;
            for(int sample=0;sample<8;sample++){
                if(!level.hasChunkAt(BlockPos.containing(at))){waiting=true;break;}
                if(state.jack())level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,at.x,at.y,at.z,1,0,0,0,0);
                if(collides(level,at,state.author(),state.jack())||!level.getBlockState(BlockPos.containing(at)).is(AIRS)){
                    explode(body,state,at);break;
                }
                if(sample<7)at=at.add(step);
            }
            if(!flights.containsKey(body))continue;
            // Loaded samples remain committed if the next cell is not resident; never sample through absent terrain.
            body.setPos(at);if(state.jack()&&!waiting){softHome(body,state);LbcOverload.sound(level,at,"minecraft:block.cherry_leaves.step",.5f,1,0,0);}
            var next=new Flight(state.author(),state.cause(),state.epoch(),state.primary(),state.age()+(waiting?0:1),state.rng(),state.jack());
            flights.put(body,next);write(body,next);ready.addLast(body);
        }
    }
    private void softHome(BlockDisplay body,Flight state){
        var level=(ServerLevel)body.level();var owner=game.server().getPlayerList().getPlayer(state.author());var at=body.position();var look=body.getLookAngle();var ahead=at.add(look.scale(30));
        var target=level.getEntitiesOfClass(LivingEntity.class,new AABB(ahead,ahead).inflate(30),
                e->!e.getUUID().equals(state.author())&&e.getType().builtInRegistryHolder().is(MOBS)&&e.position().distanceToSqr(at)<=3600&&e.position().distanceToSqr(ahead)<=900
                        &&game.items().wands().validRoleTarget(e,false)&&(owner==null||game.items().wands().projectileTarget(owner,e)))
                .stream().min(Comparator.comparingDouble(e->e.position().distanceToSqr(at))).orElse(null);
        if(target==null)return;var direction=look.add(target.getEyePosition().subtract(at).normalize().scale(.1)).normalize();
        body.setYRot((float)Math.toDegrees(Math.atan2(-direction.x,direction.z)));body.setXRot((float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance())));
    }
    private boolean collides(ServerLevel level,Vec3 at,UUID owner,boolean jack){
        return !level.getEntitiesOfClass(LivingEntity.class,new AABB(at,at.add(1,1,1)),
                mob->!mob.getUUID().equals(owner)&&mob.isAlive()&&!mob.isSpectator()&&!mob.entityTags().contains("spectator")
                        &&mob.getType().builtInRegistryHolder().is(MOBS)&&mob.position().distanceToSqr(at)<=(jack?64:25)
                        &&game.items().wands().validRoleTarget(mob,false)).isEmpty();
    }
    private void explode(BlockDisplay body,Flight state,Vec3 at){
        var level=(ServerLevel)body.level();retire(body); // Claim first: damage callbacks cannot explode this source twice.
        var coverage=LbcExplosionSuppression.coverage(game,level);if(coverage.contains(at))return;
        firework(level,at,state);
        var owner=game.server().getPlayerList().getPlayer(state.author());
        var type=state.jack()||owner!=null&&owner.level()==level?DamageTypes.ARROW:DamageTypes.GENERIC;
        var damage=new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type),body,
                owner!=null&&owner.level()==level?owner:null);
        var captured=game.items().indirectKills().captureDamage(damage,state.cause());
        for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(3.5),
                e->e.isAlive()&&!e.isSpectator()&&e.getType().builtInRegistryHolder().is(MOBS)
                        &&e.position().distanceToSqr(at)<=3.5*3.5&&game.items().wands().validRoleTarget(e,false)
                        &&!game.items().timeStop().frozen(e)&&!coverage.targets(e)))target.hurtServer(level,captured,6);
        if(state.jack())return;
        var random=new Rolls(state.rng());
        if(state.primary())for(int i=0;i<8;i++)child(level,at,body.getEyePosition(),state,random);
        for(int i=0;i<5;i++)if(random.integer(100)<20)child(level,at,body.getEyePosition(),state,random);
    }
    private void child(ServerLevel level,Vec3 original,Vec3 facing,Flight parent,Rolls random){
        float outwardYaw=random.integer(361)-180,outwardPitch=random.integer(71)-90;
        var at=original.add(Vec3.directionFromRotation(outwardPitch,outwardYaw).scale(60));
        // Source `facing entity @s eyes` uses the actor's dispatch origin, even after a later ray sample hits.
        var direction=facing.subtract(at);double horizontal=Math.sqrt(direction.x*direction.x+direction.z*direction.z);
        float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z))+(random.integer(1001)-500)*.01f;
        float pitch=(float)-Math.toDegrees(Math.atan2(direction.y,horizontal))+(random.integer(1001)-500)*.01f;
        var state=new Flight(parent.author(),parent.cause(),parent.epoch(),false,0,random.next());
        if(spawn(level,at,yaw,pitch,state)==null)pending.addLast(new Spawn(level,at,yaw,pitch,state));
    }
    private void firework(ServerLevel level,Vec3 at,Flight state){
        var rocket=new FireworkRocketEntity(EntityTypes.FIREWORK_ROCKET,level);
        rocket.load(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),state.jack()?JACK_FIREWORK:FIREWORK));
        rocket.setPos(at);var owner=game.server().getPlayerList().getPlayer(state.author());if(owner!=null)rocket.setOwner(owner);
        rocket.entityTags().add(ROCKET);install(rocket,new Flight(state.author(),state.cause(),state.epoch(),false,0,0,state.jack()));
        if(!level.addFreshEntity(rocket)){unloaded(rocket);rocket.discard();}
    }
    /** Actor-local deterministic stream belongs to the typed checkpoint, including the five independent chances. */
    private static final class Rolls {
        long value;Rolls(long value){this.value=value;}
        long next(){long z=(value+=0x9e3779b97f4a7c15L);z=(z^(z>>>30))*0xbf58476d1ce4e5b9L;z=(z^(z>>>27))*0x94d049bb133111ebL;return z^(z>>>31);}
        int integer(int bound){long bits=next()>>>1;long result=bits%bound;while(bits-result+(bound-1)<0){bits=next()>>>1;result=bits%bound;}return (int)result;}
    }
    private static void write(Entity body,Flight state){
        body.setAttached(DATA,state);
    }
    public void loaded(Entity body){rebind(body);}
    public void rebind(Entity body){
        if(restoring||!isManaged(body)||flights.containsKey(body))return;var state=body.getAttached(DATA);if(state==null)return;
        if(!game.items().currentEpoch(state.epoch())){body.discard();return;}install(body,state);
    }
    public void unloaded(Entity body){flights.remove(body);ready.remove(body);}
    private void retire(Entity body){unloaded(body);body.discard();}
    public void reindexRestored(Iterable<Entity> restored){flights.clear();ready.clear();for(var body:restored)rebind(body);}
    public void stop(){for(var body:List.copyOf(flights.keySet()))body.discard();flights.clear();ready.clear();pending.clear();restoring=false;}
    void prepareFrozen(){flights.clear();ready.clear();pending.clear();restoring=true;}
    void publishFrozen(){restoring=false;lastTick=game.server().getTickCount();}
    LbcCloneSkills.ActorImage cloneImage(Entity original){
        var saved=flights.get(original);if(saved==null)return null;
        return CloneComplexProjectileStates.image(clone->{
            // Source clones draw their own future random choices. A fresh UUID forks that stream once;
            // replay then preserves the resulting stream exactly in the ordinary typed actor state.
            long fork=saved.rng()^clone.getUUID().getMostSignificantBits()^clone.getUUID().getLeastSignificantBits();
            install(clone,new Flight(saved.author(),saved.cause(),saved.epoch(),saved.primary(),saved.age(),fork,saved.jack()));
        },this::unloaded);
    }
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){return new Checkpoint(links);}
    final class Checkpoint {
        private record Image(MatchDamageSources.Ref body,Flight state){}
        private final List<Image> images;
        private final List<MatchDamageSources.Ref> order;
        private final List<Spawn> queued;
        Checkpoint(MatchSpellCheckpoint.Links links){images=flights.entrySet().stream().map(e->new Image(links.key(e.getKey()),e.getValue())).toList();
            order=ready.stream().map(links::key).toList();queued=List.copyOf(pending);}
        Runnable prepare(MatchSpellCheckpoint.Links links){var restored=new LinkedHashMap<Entity,Flight>();var queue=new ArrayDeque<BlockDisplay>();
            for(var spawn:queued)MatchAuthorityCheckpoint.world(game,spawn.level());
            for(var image:images)restored.put(links.get(image.body()),image.state());
            for(var ref:order)queue.add((BlockDisplay)links.get(ref));
            return ()->{flights.clear();flights.putAll(restored);ready.clear();ready.addAll(queue);pending.clear();pending.addAll(queued);};
        }
    }
}
