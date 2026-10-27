package me.skywars2.game;

import net.minecraft.commands.arguments.coordinates.LocalCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Source carried hazards: live physical inventory predicates, native damage and native consequence actors. */
public final class LbcInventoryHazards {
    /** Opaque native provenance is written only by an actual tracked producer's successful pickup path. */
    static final String ORIGIN="skywars2_inventory_hazard_origin";
    public static final String MANAGED="skywars2_inventory_hazard_actor";
    private static final CompoundTag LIQUID=tag("liquidvoid",1),INSTANT=tag("instantitem",1),BOMB=tag("penis_bomb",1);
    private static final CompoundTag[] VARIANTS={null,tag("penis_num",1),tag("penis_num",2),tag("penis_num",3),tag("penis_num",4),tag("penis_num",5),tag("penis_num",6)};
    private final GameController game;
    private int phase;
    private record Origin(UUID producer,String epoch){}
    private record Actor(UUID holder,Origin origin,LbcIndirectKills.Owner cause,String epoch,int variant){}
    private final Map<Entity,Actor> actors=new IdentityHashMap<>();
    private final Set<Entity> ticking=java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<LbcStonekivi.Work,UUID> multicasts=new IdentityHashMap<>();
    public LbcInventoryHazards(GameController game){this.game=game;}
    private static CompoundTag tag(String key,int value){var result=new CompoundTag();result.putInt(key,value);return result;}
    private static boolean matches(ItemStack stack,CompoundTag predicate){var data=stack.get(DataComponents.CUSTOM_DATA);return !stack.isEmpty()&&data!=null&&data.matchedBy(predicate);}
    static void recordProducer(ItemStack stack,UUID producer,String epoch){var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();var origin=new CompoundTag();origin.putString("producer",producer.toString());origin.putString("epoch",epoch);data.put(ORIGIN,origin);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));}
    private Origin origin(ItemStack stack){var data=stack.get(DataComponents.CUSTOM_DATA);if(data==null)return null;var value=data.copyTag().getCompound(ORIGIN).orElse(null);if(value==null)return null;try{var origin=new Origin(UUID.fromString(value.getStringOr("producer","")),value.getStringOr("epoch",""));return game.items().currentEpoch(origin.epoch)?origin:null;}catch(IllegalArgumentException ignored){return null;}}
    private LivingEntity producer(Origin origin){if(origin==null||!game.items().currentEpoch(origin.epoch))return null;var player=game.server().getPlayerList().getPlayer(origin.producer);if(player!=null)return player;var level=game.maps().level();var entity=level==null?null:level.getEntity(origin.producer);return entity instanceof LivingEntity living?living:null;}
    private LbcIndirectKills.Owner cause(Origin origin,boolean wild){return producer(origin) instanceof ServerPlayer player?(wild?game.items().indirectKills().wildCause(player):game.items().indirectKills().coldCause(player,false)):null;}
    /** Reflection and native TNT chains can transfer authority after the carried payload was emitted. */
    private Actor authority(Entity actor){
        var state=actors.get(actor);if(state==null||!game.items().currentEpoch(state.epoch))return null;
        LivingEntity author=game.items().electro().reflectedOwner(actor);
        if(author==null&&actor instanceof PrimedTnt tnt)author=tnt.getOwner();
        if(author!=null&&(state.origin==null||!state.origin.producer.equals(author.getUUID()))){
            var origin=new Origin(author.getUUID(),state.epoch);
            state=new Actor(state.holder,origin,cause(origin,state.variant==4),state.epoch,state.variant);
            actors.put(actor,state);
        }
        return state;
    }
    public LivingEntity owner(Entity actor){var state=authority(actor);return state==null?null:producer(state.origin);}
    public LbcIndirectKills.Owner cause(Entity actor){var state=authority(actor);return state==null?null:state.cause;}
    /** Native callers retain direct/causing entities and damage type; only a genuinely known origin adds credit. */
    public DamageSource inheritSource(DamageSource source){var actor=source.getDirectEntity();var cause=cause(actor);return cause==null?source:game.items().indirectKills().captureDamage(source,cause);}
    public boolean allowDamage(LivingEntity victim,DamageSource source){if(!actors.containsKey(source.getDirectEntity())||LbcDamage.isNativeLightning(source))return true;return payloadTarget(owner(source.getDirectEntity()),victim);}
    /** Filter before native explosion damage AND impulse; raw environmental actors do not invent a friend owner. */
    public boolean explosionTarget(Entity source,Entity target){if(!actors.containsKey(source)||!(target instanceof LivingEntity living))return true;return game.items().wands().validTarget(living)&&payloadTarget(owner(source),living);}
    /** Native block-chain TNT retains the actual source cast, including an intentionally unknown producer. */
    public void inheritChild(Entity source,Entity spawned){var state=authority(source);if(state==null||!(spawned instanceof PrimedTnt))return;if(!game.items().currentEpoch(state.epoch)||spawned.level()!=game.maps().level()||game.stage()==GameController.Stage.ENDING){spawned.discard();return;}spawned.entityTags().add(MANAGED);game.items().markCurrentItemEpoch(spawned);actors.put(spawned,state);}
    private boolean payloadTarget(LivingEntity producer,LivingEntity victim){
        if(producer==null||producer==victim)return game.items().wands().validRoleTarget(victim,false);
        return producer instanceof ServerPlayer player?game.items().wands().projectileTarget(player,victim)
                :game.items().wands().validRoleTarget(victim,false)&&!producer.isAlliedTo(victim);
    }
    private boolean admitted(ServerPlayer player){return player!=null&&player.isAlive()&&!player.isRemoved()&&player.level()==game.maps().level()
            &&game.canUseGameplayItems(player)&&game.stage()!=GameController.Stage.ENDING&&!game.isObserver(player)
            &&game.items().wands().validRoleTarget(player,false)&&!game.items().wands().inputTimeStopped(player)
            &&!game.items().timeStop().stopped(player.level())
            &&!player.level().dimension().identifier().equals(Identifier.parse("minecraft:nexus"))
            &&!player.level().dimension().identifier().equals(Identifier.parse("minecraft:imprinted"));}
    private static boolean container(ServerPlayer player,CompoundTag predicate){for(int i=0;i<36;i++)if(matches(player.getInventory().getItem(i),predicate))return true;return false;}
    private static boolean hands(ServerPlayer player,CompoundTag predicate){return matches(player.getMainHandItem(),predicate)||matches(player.getOffhandItem(),predicate);}
    private static boolean inventory(ServerPlayer player,CompoundTag predicate){for(int i=0;i<=40;i++)if(matches(player.getInventory().getItem(i),predicate))return true;return false;}
    private boolean armored(ServerPlayer player){return game.config().lbcSkills&&game.players().get(player.getUUID()).lbcSkill==2;}
    /** Only read the source mode score when both objective and entry already exist; never create/reset global mode. */
    private boolean legacy(){var board=game.server().getScoreboard();var objective=board.getObjective("lbc.math");if(objective==null)return false;var value=board.getPlayerScoreInfo(net.minecraft.world.scores.ScoreHolder.forNameOnly("legacy"),objective);return value!=null&&value.value()==1;}
    public void tick(){
        for(var actor:List.copyOf(actors.keySet())){if(actor.isRemoved()){if(!ticking.contains(actor))actors.remove(actor);}else if(!activeActor(actor)){actors.remove(actor);actor.discard();}}
        if(game.maps().level()==null||game.stage()==GameController.Stage.ENDING||game.items().wands().timeStopped())return;
        phase=(phase+1)%40;
        for(var player:game.server().getPlayerList().getPlayers()){
            if(!admitted(player))continue;
            // Source tick function dispatches instants before the independent scheduled two-second callback.
            if(!armored(player)&&inventory(player,INSTANT)&&inventory(player,BOMB))bombs(player);
            if(phase==0&&admitted(player)){
                // Source players_2s consumes cans before either Liquid Abyss probe.
                game.items().cannedFood().pulse(player);
                if(admitted(player)&&container(player,LIQUID))liquid(player);
                if(admitted(player)&&hands(player,LIQUID))liquid(player);
            }
        }
        actors.keySet().removeIf(Entity::isRemoved);
    }
    private void liquid(ServerPlayer player){
        if(!legacy()&&armored(player)&&!hands(player,LIQUID))return;
        int count=player.getInventory().clearOrCountMatchingItems(stack->matches(stack,LIQUID),true,0,player.inventoryMenu.getCraftSlots());
        // ExecuteCommand's int store scales in double then saturates; int multiplication would wrap.
        int amount=(int)(count*4d);if(amount<=0||game.items().mana().hudValue(player,"void_resistance")>=1)return;
        player.hurtServer(player.level(),new DamageSource(player.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.OUTSIDE_BORDER)),(float)amount);
    }
    private Origin consumedOrigin(ServerPlayer player,CompoundTag predicate){for(int i=0;i<player.getInventory().getContainerSize();i++){var stack=player.getInventory().getItem(i);if(matches(stack,predicate))return origin(stack);}var craft=player.inventoryMenu.getCraftSlots();for(int i=0;i<craft.getContainerSize();i++)if(matches(craft.getItem(i),predicate))return origin(craft.getItem(i));var cursor=player.containerMenu.getCarried();return matches(cursor,predicate)?origin(cursor):null;}
    private void bombs(ServerPlayer player){
        // Function source position/rotation is immutable across the six nested helpers, even after horse teleport.
        var level=player.level();var at=player.position();var rotation=new Vec2(player.getXRot(),player.getYRot());
        for(int variant=1;variant<=6;variant++){
            if(!admitted(player))return;
            if(!inventory(player,VARIANTS[variant]))continue;
            game.items().kfcRewards().announcement(player,variant,true);
            var origin=consumedOrigin(player,VARIANTS[variant]);final var predicate=VARIANTS[variant];
            player.getInventory().clearOrCountMatchingItems(stack->matches(stack,predicate),false,1,player.inventoryMenu.getCraftSlots());
            player.getInventory().setChanged();player.containerMenu.broadcastChanges();player.inventoryMenu.slotsChanged(player.getInventory());
            payload(player,level,at,rotation,variant,origin);
        }
    }
    private void payload(ServerPlayer holder,ServerLevel level,Vec3 at,Vec2 rotation,int variant,Origin origin){
        var producer=producer(origin);var cause=cause(origin,variant==4);boolean allowed=variant==4||payloadTarget(producer,holder);
        if(allowed)switch(variant){
            case 1 -> arrow(holder,6,cause);
            case 2 -> {effects(producer,holder,variant);arrow(holder,4,cause);}
            case 3 -> add(new CarriedTnt(level,at),holder,origin,cause,variant);
            case 4 -> {if(payloadTarget(producer,holder))effects(producer,holder,variant);var bolt=new CarriedLightning(level);bolt.setPos(at);add(bolt,holder,origin,cause,variant);}
            case 5 -> {effects(producer,holder,variant);arrow(holder,10,cause);horse(holder,level,at,rotation,cause);}
            case 6 -> multicast(holder,level,origin,cause);
        }
        // Explicit source feedback still follows native clear, regardless of damage/protection acceptance.
        LbcKfcRewards.bombSound(holder,level,at,variant);LbcKfcRewards.bombParticles(level,at,variant);
    }
    private static void effects(LivingEntity producer,ServerPlayer holder,int variant){
        // A picked-up hazard is external even when its creator is also its holder.
        if(!LbcGreed.active(holder))LbcKfcRewards.bombEffects(producer,holder,variant);
    }
    private void arrow(ServerPlayer victim,float amount,LbcIndirectKills.Owner cause){var raw=new DamageSource(victim.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.ARROW));victim.hurtServer(victim.level(),game.items().indirectKills().captureDamage(raw,cause),amount);}
    private void horse(ServerPlayer holder,ServerLevel level,Vec3 origin,Vec2 rotation,LbcIndirectKills.Owner cause){
        if(!holder.isAlive()||holder.isRemoved()||LbcGreed.active(holder))return;
        var target=new LocalCoordinates(0,0,-5).apply(origin,rotation).add(0,5,0);if(!Level.isInSpawnableBounds(BlockPos.containing(target)))return;
        if(cause!=null)game.items().indirectKills().displaced(holder,cause);
        var relatives=Relative.union(Relative.position(true,true,true),Relative.direction(true,true,true),Relative.ROTATION);
        if(holder.teleportTo(level,target.x-holder.getX(),target.y-holder.getY(),target.z-holder.getZ(),relatives,0,0,true)&&!holder.isFallFlying()){holder.setDeltaMovement(holder.getDeltaMovement().multiply(1,0,1));holder.setOnGround(true);}
    }
    private void multicast(ServerPlayer holder,ServerLevel level,Origin origin,LbcIndirectKills.Owner cause){
        int count=0;for(var entity:level.getAllEntities())if(!entity.entityTags().contains("spectator"))count++;
        final int repetitions=count;final String epoch=game.items().epoch();
        var work=new LbcStonekivi.Work(){int remaining=repetitions;
            public boolean step(){if(!multicasts.containsKey(this)||!game.items().currentEpoch(epoch)||!holder.isAlive()||holder.isRemoved()||holder.level()!=level||level!=game.maps().level()||game.stage()==GameController.Stage.ENDING){cancel();return true;}if(game.items().wands().timeStopped()||game.items().timeStop().stopped(level)||holder.entityTags().contains("time_stopped"))return false;if(!payloadTarget(producer(origin),holder)){cancel();return true;}if(remaining>0){arrow(holder,.1f,cause);remaining--;}if(remaining==0){cancel();return true;}return false;}
            public void cancel(){multicasts.remove(this);}
        };multicasts.put(work,holder.getUUID());game.items().stonekivi().queuedWork(work);
    }
    private void add(Entity entity,ServerPlayer holder,Origin origin,LbcIndirectKills.Owner cause,int variant){entity.entityTags().add(MANAGED);game.items().markCurrentItemEpoch(entity);actors.put(entity,new Actor(holder.getUUID(),origin,cause,game.items().epoch(),variant));if(!entity.level().addFreshEntity(entity)){actors.remove(entity);entity.discard();}}
    private boolean activeActor(Entity entity){var state=actors.get(entity);return state!=null&&game.items().currentEpoch(state.epoch)&&entity.level()==game.maps().level()&&game.stage()!=GameController.Stage.ENDING;}
    private boolean paused(Entity entity){return game.items().wands().timeStopped()||game.items().timeStop().stopped((ServerLevel)entity.level())||entity.entityTags().contains("time_stopped");}
    private void actorTick(Entity entity,Runnable tick){if(!activeActor(entity)){actors.remove(entity);entity.discard();return;}if(paused(entity))return;var level=(ServerLevel)entity.level();var cause=cause(entity);ticking.add(entity);try{if(cause==null)tick.run();else game.items().indirectKills().capturedWorldChanges(level,cause,tick);}finally{ticking.remove(entity);if(entity.isRemoved())actors.remove(entity);}}
    private final class CarriedTnt extends PrimedTnt{CarriedTnt(ServerLevel level,Vec3 at){super(EntityTypes.TNT,level);setPos(at);setDeltaMovement(Vec3.ZERO);setFuse(2);}@Override public boolean shouldBeSaved(){return false;}@Override public void tick(){actorTick(this,()->super.tick());}}
    private final class CarriedLightning extends LightningBolt{CarriedLightning(ServerLevel level){super(EntityTypes.LIGHTNING_BOLT,level);}@Override public boolean shouldBeSaved(){return false;}@Override public void tick(){actorTick(this,()->super.tick());}}
    public void forget(ServerPlayer holder){for(var entry:List.copyOf(actors.entrySet()))if(entry.getValue().holder.equals(holder.getUUID())){if(!ticking.contains(entry.getKey()))actors.remove(entry.getKey());entry.getKey().discard();}for(var entry:List.copyOf(multicasts.entrySet()))if(entry.getValue().equals(holder.getUUID()))entry.getKey().cancel();}
    /** Vanilla TNT discards before exploding, including un-subclassed block-chain children. */
    public void unloaded(Entity entity){if(ticking.contains(entity)||entity instanceof PrimedTnt&&entity.isRemoved())return;if(actors.remove(entity)!=null&&!entity.isRemoved())entity.discard();}
    /** Native chain children serialize vanilla state; unsupported reload must not resurrect a detached cast. */
    public void loaded(Entity entity){if((entity instanceof PrimedTnt||entity instanceof LightningBolt)&&entity.entityTags().contains(MANAGED)&&!actors.containsKey(entity))entity.discard();}
    public void stop(){phase=0;for(var actor:List.copyOf(actors.keySet()))actor.discard();actors.clear();for(var work:List.copyOf(multicasts.keySet()))work.cancel();}
    int count(){return actors.size();}
    List<Entity> snapshot(){return List.copyOf(actors.keySet());}
    int pendingMulticastCount(){return multicasts.size();}
}
