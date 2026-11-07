package me.skywars2.game;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Registered Paper landing physics and the separate source Metal Divider continuation. */
public final class LbcMetalPipe {
    public static final String THROWN="skywars2.thrown_metal_pipe";
    private static final String LEFT_OWNER=THROWN+".left_owner", CONTACT=THROWN+".contact";
    private static boolean visualsRegistered;
    private static synchronized void registerVisuals(){
        if(visualsRegistered)return;visualsRegistered=true;
        // Preserve native item IDs and ordinary item rendering; only each tagged thrown instance is hidden.
        eu.pb4.polymer.core.api.entity.PolymerEntityUtils.registerPolymerEntityConstructor(
                net.minecraft.world.entity.EntityTypes.ITEM,item->new eu.pb4.polymer.core.api.entity.PolymerEntity(){
                    public net.minecraft.world.entity.EntityType<?> getPolymerEntityType(net.fabricmc.fabric.api.networking.v1.context.PacketContext context){
                        return item.entityTags().contains(THROWN)?net.minecraft.world.entity.EntityTypes.MARKER:net.minecraft.world.entity.EntityTypes.ITEM;
                    }
                    public void modifyRawTrackedData(List<net.minecraft.network.syncher.SynchedEntityData.DataValue<?>> data,ServerPlayer viewer,boolean initial){
                        if(item.entityTags().contains(THROWN))data.clear();
                    }
                });
    }
    private static final TagKey<DamageType> ARROWS=TagKey.create(Registries.DAMAGE_TYPE,Identifier.parse("minecraft:arrow_only"));
    private static final String[] KEYS={"luckywars","lws","lucky_wars"};
    private final GameController game;
    private final Set<ItemEntity> items=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<ItemEntity> craftPipes=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<ItemEntity,State> pipes=new IdentityHashMap<>();
    private final Map<ServerPlayer,Integer> timers=new IdentityHashMap<>();
    private int phase;
    private static final class State {
        boolean grounded;final String epoch;UUID producer;net.minecraft.world.phys.Vec3 previous;
        eu.pb4.polymer.virtualentity.api.ElementHolder holder;
        eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement display;
        void close(){if(holder!=null){holder.destroy();holder=null;display=null;}}
        State(String epoch,UUID producer){this.epoch=epoch;this.producer=producer;}
    }
    public LbcMetalPipe(GameController game){this.game=game;registerVisuals();}
    public static boolean matches(ItemStack stack){
        if(stack.isEmpty())return false;var data=stack.get(DataComponents.CUSTOM_DATA);if(data==null)return false;
        var pdc=data.copyTag().getCompoundOrEmpty("PublicBukkitValues");
        for(var key:KEYS)if(pdc.getCompoundOrEmpty(key+":item").getStringOr(key+":type","").equals("metal_pipe"))return true;
        return false;
    }
    public static boolean divider(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("divider_by_metal_pipe",0)==1;}
    private boolean active(ServerLevel level){return game.playing()&&game.stage()!=GameController.Stage.ENDING&&level==game.maps().level()&&LbcDimensionItems.ordinary(level);}
    private boolean admitted(ServerPlayer player){return player.isAlive()&&!player.isRemoved()&&active(player.level())
            &&game.canUseGameplayItems(player)&&!game.isObserver(player)&&game.items().wands().validRoleTarget(player,false);}
    private UUID producer(ItemEntity item){
        var original=game.items().inventoryHazards().producerIdOf(item.getItem());
        return original!=null?original:item.getOwner() instanceof LivingEntity owner?owner.getUUID():null;
    }
    private LivingEntity resolve(UUID id){
        if(id==null)return null;var player=game.server().getPlayerList().getPlayer(id);if(player!=null)return player;
        var entity=game.maps().level().getEntity(id);return entity instanceof LivingEntity living?living:null;
    }
    private void track(ItemEntity item){
        if(pipes.containsKey(item)||item.isRemoved()||item.getAge()>=5999||!matches(item.getItem())||!active((ServerLevel)item.level())||game.items().hasStaleItemEpoch(item))return;
        var author=producer(item);if(author!=null)LbcInventoryHazards.recordProducer(item.getItem(),author,game.items().epoch());
        game.items().markCurrentItemEpoch(item);item.setPickUpDelay(32767);var state=new State(game.items().epoch(),author);state.previous=item.position();pipes.put(item,state);
        if(item.entityTags().contains(THROWN)){
            state.display=new eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement(item.getItem().copy());
            state.display.setItemDisplayContext(net.minecraft.world.item.ItemDisplayContext.NONE);
            state.display.setTeleportDuration(1);state.display.setInterpolationDuration(1);
            state.holder=new eu.pb4.polymer.virtualentity.api.ElementHolder();state.holder.addElement(state.display);
            align(item,state);new eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment(state.holder,item,false);
        }
    }
    public void loaded(Entity entity){
        if(entity instanceof ServerPlayer player){if(game.items().mana().score(player,"metal_divider_cd").get()>0)timers.putIfAbsent(player,0);return;}
        if(entity instanceof ItemEntity item&&entity.level()==game.maps().level()){items.add(item);if(LbcFartus.data(item.getItem()).getIntOr("metal_pipe",0)==1)craftPipes.add(item);track(item);}
    }
    public boolean canMerge(ItemEntity first,ItemEntity second){return first.entityTags().contains(THROWN)==second.entityTags().contains(THROWN)&&Objects.equals(producer(first),producer(second));}
    public void unloaded(Entity entity){items.remove(entity);craftPipes.remove(entity);var state=pipes.remove(entity);if(state!=null)state.close();}
    private void landing(ItemEntity item,State state){
        item.setPickUpDelay(20);
        var level=(ServerLevel)item.level();var at=item.position();
        LbcOverload.sound(level,at,"minecraft:lbcsounds.metal_pipe",4,1,0,0);
        var cold=state.producer==null?null:game.items().indirectKills().restoreColdCause(state.producer,0,false);
        var cause=cold==null?null:new LbcIndirectKills.Owner(cold.player(),cold.order(),true,false);
        // Keep the actual pipe as direct source, so landing cannot masquerade as a real held-weapon hit.
        // DamageSource position is the impact, while the captured cause retains kill attribution.
        var raw=new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.FALLING_ANVIL),item,item){
            @Override public net.minecraft.world.phys.Vec3 getSourcePosition(){return at;}
            @Override public net.minecraft.world.phys.Vec3 sourcePositionRaw(){return at;}
        };
        var damage=game.items().indirectKills().captureDamage(raw,cause);
        for(var victim:level.getEntitiesOfClass(LivingEntity.class,item.getBoundingBox().inflate(2.5),target->game.items().wands().validRoleTarget(target,true))){
            victim.hurtServer(level,damage,25);
        }
    }
    private void align(ItemEntity item,State state){
        if(state.display==null)return;var velocity=item.getDeltaMovement();
        if(velocity.lengthSqr()>1e-8){
            // The pipe model's long local Y axis follows the actual velocity, including native bounces.
            state.display.setLeftRotation(new org.joml.Quaternionf().rotationTo(new org.joml.Vector3f(0,1,0),
                    new org.joml.Vector3f((float)velocity.x,(float)velocity.y,(float)velocity.z).normalize()));
        }
        state.holder.tick();
    }
    private boolean contact(ItemEntity item,State state){
        if(!item.entityTags().contains(THROWN))return false;
        var from=state.previous==null?item.position():state.previous;state.previous=item.position();
        var bounds=new net.minecraft.world.phys.AABB(from,item.position()).inflate(.125);
        var owner=resolve(state.producer);boolean ownerArmed=item.entityTags().contains(LEFT_OWNER);
        // The launch segment still starts inside its thrower. Arming its endpoint must not retroactively admit that segment.
        if(!ownerArmed&&(owner==null||!owner.getBoundingBox().intersects(
                item.getBoundingBox().expandTowards(item.getDeltaMovement()).inflate(1))))item.entityTags().add(LEFT_OWNER);
        LivingEntity nearest=null;net.minecraft.world.phys.Vec3 impact=null;double distance=Double.MAX_VALUE;
        for(var target:((ServerLevel)item.level()).getEntitiesOfClass(LivingEntity.class,bounds.inflate(1),
                target->game.items().wands().validRoleTarget(target,false)&&!game.items().timeStop().frozen(target)
                        &&(target!=owner||ownerArmed))){
            var box=target.getBoundingBox().inflate(.125);
            var hit=box.contains(from)?java.util.Optional.of(from):box.clip(from,item.position());
            if(hit.isEmpty())continue;double next=hit.get().distanceToSqr(from);
            if(next<distance){distance=next;nearest=target;impact=hit.get();}
        }
        if(nearest==null){item.entityTags().removeIf(tag->tag.startsWith(CONTACT+"."));return false;}
        var contactTag=CONTACT+"."+nearest.getUUID();if(item.entityTags().contains(contactTag))return false;
        item.entityTags().removeIf(tag->tag.startsWith(CONTACT+"."));item.entityTags().add(contactTag);
        item.setPos(impact);state.previous=impact;landing(item,state);
        var away=item.position().subtract(nearest.position()).multiply(1,0,1).normalize();
        if(away.lengthSqr()<1e-8)away=item.getDeltaMovement().multiply(-1,0,-1).normalize();
        item.setDeltaMovement(away.scale(Math.max(.25,item.getDeltaMovement().horizontalDistance()*.5)).add(0,.25,0));
        item.needsSync=true;item.syncVelocity=true;return true;
    }
    /** Source accepted non-arrow damage while the pipe flag is in the main hand. No magic gate. */
    public void damaged(LivingEntity victim,DamageSource source){
        if(source.getEntity() instanceof ServerPlayer owner&&LbcDamage.isMeleeAttack(source,owner)&&admitted(owner)&&!source.is(ARROWS)
                &&game.items().wands().validRoleTarget(victim,true)&&!game.items().actionsBlocked(owner)
                &&!game.items().wands().inputTimeStopped(owner)&&!game.items().timeStop().stopped(owner.level())
                &&LbcFartus.data(owner.getMainHandItem()).getIntOr("metal_pipe",0)==1)
            LbcOverload.sound(owner,owner.position().add(owner.getLookAngle()),"minecraft:lbcsounds.metal_pipe",2,2,0);
    }
    private ItemEntity output(ServerLevel level,net.minecraft.world.phys.Vec3 at,ItemStack stack,UUID author){
        if(author!=null)LbcInventoryHazards.recordProducer(stack,author,game.items().epoch());
        var result=new ItemEntity(level,at.x,at.y,at.z,stack);result.setDefaultPickUpDelay();
        var living=resolve(author);if(living!=null)result.setThrower(living);
        game.items().markCurrentItemEpoch(result);level.addFreshEntity(result);return result;
    }
    public boolean use(ServerPlayer player,InteractionHand hand){
        if(!divider(player.getItemInHand(hand))||hand==InteractionHand.OFF_HAND&&!game.config().offhandWands
                ||!admitted(player)||game.items().actionsBlocked(player)||game.items().wands().inputTimeStopped(player)
                ||game.items().timeStop().stopped(player.level())||game.items().magic().suppressed(player)
                ||game.items().magic().suppressedAt(player.level(),player.getEyePosition())
                ||game.items().mana().score(player,"metal_divider_cd").get()>0)return false;
        // Snapshot only the existing loaded actors, never the outputs created by this cast.
        for(var item:List.copyOf(items))if(!item.isRemoved()&&item.getAge()<5999&&item.level()==player.level()
                &&!item.entityTags().contains("metal_pipe")&&!game.items().hasStaleItemEpoch(item)){
            var at=item.position();var loot=game.lbc().rollTable(player.level(),at,player,"lbc:metal_pipe");
            item.discard();for(var stack:loot)if(!stack.isEmpty())output(player.level(),at,stack,player.getUUID());
        }
        game.items().mana().score(player,"metal_divider_cd").set(15);timers.put(player,0);return true;
    }
    public int cooldownTicks(ServerPlayer player){return (int)Math.clamp((long)game.items().mana().score(player,"metal_divider_cd").get()*20-timers.getOrDefault(player,0),0,Integer.MAX_VALUE);}
    public void crafted(ServerLevel level,net.minecraft.world.phys.Vec3 at){
        level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(0xc5c5c5,2),true,false,at.x,at.y+1,at.z,100,1,1,1,1);
        LbcOverload.sound(level,at,"minecraft:block.enchantment_table.use",1,1,0,0);
    }
    private void groundCraft(){
        craftPipes.removeIf(item->item.isRemoved()||LbcFartus.data(item.getItem()).getIntOr("metal_pipe",0)!=1);
        for(var item:List.copyOf(craftPipes))if(!item.isRemoved()&&!game.items().hasStaleItemEpoch(item)&&!item.entityTags().contains("time_stopped")&&LbcFartus.data(item.getItem()).getIntOr("metal_pipe",0)==1){
            var author=producer(item);if(resolve(author) instanceof ServerPlayer player&&!admitted(player))continue;
            for(var other:((ServerLevel)item.level()).getEntitiesOfClass(ItemEntity.class,item.getBoundingBox().inflate(1),candidate->candidate!=item&&!candidate.isRemoved()&&!game.items().hasStaleItemEpoch(candidate)&&!candidate.entityTags().contains("time_stopped")
                    &&candidate.position().distanceToSqr(item.position())>=.000001&&candidate.position().distanceToSqr(item.position())<=1
                    &&LbcWands.isDivider(candidate.getItem()))){
                var at=item.position();var level=(ServerLevel)item.level();var loot=game.lbc().rollTable(level,at,resolve(author),"lbc:ported/metal_pipe_divider");
                if(loot.isEmpty())break;
                // Source ground recipe consumes both whole entities; inventory crafting consumes one per slot.
                item.discard();other.discard();for(var stack:loot)if(!stack.isEmpty())output(level,at,stack,author);crafted(level,at);break;
            }
        }
    }
    public void tick(){
        var level=game.maps().level();if(level==null)return;
        for(var item:List.copyOf(pipes.keySet())){
            var state=pipes.get(item);
            if(item.isRemoved()){pipes.remove(item);state.close();continue;}
            if(!active((ServerLevel)item.level())||!game.items().currentEpoch(state.epoch)||game.items().hasStaleItemEpoch(item)){pipes.remove(item);state.close();item.discard();continue;}
            if(item.getAge()>=5999||!matches(item.getItem())){pipes.remove(item);state.close();item.setDefaultPickUpDelay();continue;}
            if(game.items().timeStop().stopped((ServerLevel)item.level())||item.entityTags().contains("time_stopped"))continue;
            if(state.producer==null)state.producer=producer(item);
            boolean impacted=contact(item,state);boolean grounded=item.onGround();if(state.grounded&&!grounded)item.setPickUpDelay(32767);
            else if(!state.grounded&&grounded&&!impacted)landing(item,state);
            state.grounded=grounded;align(item,state);
        }
        if(!active(level)||game.items().timeStop().stopped(level)||game.items().wands().timeStopped())return;
        for(var player:List.copyOf(timers.keySet())){
            if(player.isRemoved()||game.items().mana().score(player,"metal_divider_cd").get()<=0){timers.remove(player);continue;}
            if(!admitted(player))continue;
            if(game.items().wands().inputTimeStopped(player))continue;
            int ticks=timers.get(player)+1;if(ticks==20){game.items().mana().score(player,"metal_divider_cd").add(-1);ticks=0;}timers.put(player,ticks);
        }
        phase=(phase+1)%5;if(phase==0)groundCraft();
    }
    public void forget(ServerPlayer player){timers.remove(player);game.items().mana().score(player,"metal_divider_cd").set(0);}
    public void stop(){for(var entry:List.copyOf(pipes.entrySet())){entry.getValue().close();entry.getKey().discard();}pipes.clear();craftPipes.removeIf(Entity::isRemoved);items.removeIf(Entity::isRemoved);for(var player:List.copyOf(timers.keySet()))forget(player);phase=0;}
    public int trackedCount(){return (int)pipes.keySet().stream().filter(item->!item.isRemoved()).count();}
}
