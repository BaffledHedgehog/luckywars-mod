package me.skywars2.game;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Shared source player counters, straight Triple copies and independent Blazefire continuation. */
public final class LbcChargedSwords {
    public static final Item TRIPLE=registerItem("triple_golden_sword",900,"triple_golden_sword");
    public static final Item BLAZE=registerItem("blazefire_sword",600,"lbckit.18");
    public static final EntityType<ThrownSword> TYPE=registerType();
    private static final String TRIPLE_CHARGE="swordcharged8",BLAZE_CHARGE="swordcharged9",FIRING="firefarting";
    private static final TagKey<DamageType> ARROW_ONLY=TagKey.create(Registries.DAMAGE_TYPE,Identifier.parse("minecraft:arrow_only"));
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private final GameController game;
    private final Set<ThrownSword> swords=new LinkedHashSet<>();
    private final Map<ServerPlayer,String> firing=new IdentityHashMap<>();
    private final Set<Pulse> pulses=new LinkedHashSet<>();
    private int clock,visualClock;
    private record FinishingModel(ElementHolder holder,ItemDisplayElement display,ServerPlayer owner,String epoch,ServerLevel level,Vec3 position,int removeAt){}
    private final List<FinishingModel> finishing=new ArrayList<>();
    record FinishingView(ElementHolder holder,ItemDisplayElement display,ServerPlayer owner,String epoch,Vec3 position,int remainingTicks){}
    int finishingModels(){return finishing.size();}
    List<FinishingView> finishingSnapshot(){return finishing.stream().map(m->new FinishingView(m.holder,m.display,m.owner,m.epoch,m.position,m.removeAt-visualClock)).toList();}
    public LbcChargedSwords(GameController game){this.game=game;}
    public static void register(){}
    private static Item registerItem(String id,int durability,String description){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:"+id));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(new Item.Properties(){
            @Override public Identifier effectiveModel(){return Identifier.parse("lbc:"+id);}
        }.setId(key).durability(durability).overrideDescription(description),Items.CARROT_ON_A_STICK,true){
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
                var controller=me.skywars2.SkyWars2.game();
                return controller!=null&&player instanceof ServerPlayer owner&&controller.items().wands().use(owner,hand)
                        ?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        });
    }
    private static EntityType<ThrownSword> registerType(){
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Identifier.parse("skywars2:triple_golden_copy"));
        var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<ThrownSword>of(ThrownSword::new,MobCategory.MISC)
                .sized(.1f,.1f).clientTrackingRange(8).updateInterval(1).build(key));
        PolymerEntityUtils.registerType(type);return type;
    }
    public static boolean isTriple(ItemStack stack){return !stack.isEmpty()&&(stack.is(TRIPLE)||LbcFartus.data(stack).getIntOr("triple_golden_sword",0)==1);}
    public static boolean isBlaze(ItemStack stack){return !stack.isEmpty()&&(stack.is(BLAZE)||LbcFartus.data(stack).getIntOr("blazefire_sword",0)==1);}
    public static boolean matches(ItemStack stack){return isTriple(stack)||isBlaze(stack);}
    public int tripleCharges(ServerPlayer owner){return game.items().mana().score(owner,TRIPLE_CHARGE).get();}
    public int blazeCharges(ServerPlayer owner){return game.items().mana().score(owner,BLAZE_CHARGE).get();}
    boolean activeBlaze(ServerPlayer owner){return firing.containsKey(owner);}
    int blazePulses(ServerPlayer owner){return (int)pulses.stream().filter(p->p.owner==owner).count();}
    int count(){return swords.size();}
    record View(ThrownSword entity,Vec3 position,int age,Vec3 direction,boolean grounded,ServerPlayer owner){}
    List<View> snapshot(){return swords.stream().map(s->new View(s,s.position(),s.age,s.getDeltaMovement().normalize(),s.grounded,
            s.getOwner() instanceof ServerPlayer p?p:null)).toList();}
    record PulseView(ServerPlayer owner,String epoch,Vec3 origin,Vec3 direction,int nextSample,Set<UUID> hit){}
    List<PulseView> pulseSnapshot(){return pulses.stream().map(p->new PulseView(p.owner,p.epoch,p.origin,p.direction,p.nextSample,Set.copyOf(p.hit))).toList();}
    private boolean valid(ServerPlayer owner){return owner!=null&&owner.isAlive()&&!owner.isRemoved()&&owner.level()==game.maps().level()
            &&game.stage()!=GameController.Stage.ENDING&&game.canUseGameplayItems(owner)&&!game.isObserver(owner);}
    private boolean paused(ServerPlayer owner){return game.items().wands().inputTimeStopped(owner)||game.items().timeStop().stopped(owner.level());}
    private boolean suppressed(ServerPlayer owner){return game.items().magic().suppressed(owner)
            ||game.items().magic().suppressedAt(owner.level(),owner.getEyePosition());}
    private boolean admitted(ServerPlayer owner){return valid(owner)&&!game.items().actionsBlocked(owner)&&!paused(owner)&&!suppressed(owner)
            &&owner.level().hasChunkAt(BlockPos.containing(owner.getEyePosition()));}
    /** Called only for accepted, unblocked positive damage, including the native fatal-damage hook. */
    public void damaged(LivingEntity victim,DamageSource source){
        if(!(source.getEntity() instanceof ServerPlayer owner)||!admitted(owner)||source.is(ARROW_ONLY)
                ||!game.items().wands().validRoleTarget(victim,true))return;
        var dimension=owner.level().dimension().identifier();
        if(dimension.equals(Identifier.parse("minecraft:nexus"))||dimension.equals(Identifier.parse("minecraft:imprinted")))return;
        var held=owner.getMainHandItem();
        if(isTriple(held)&&tripleCharges(owner)<=5)game.items().mana().score(owner,TRIPLE_CHARGE).add(1);
        if(isBlaze(held))game.items().mana().score(owner,BLAZE_CHARGE).set((int)Math.min(Integer.MAX_VALUE,(long)blazeCharges(owner)+1));
    }
    public boolean use(ServerPlayer owner,InteractionHand hand){
        if(!admitted(owner)||hand==InteractionHand.OFF_HAND&&!game.config().offhandWands)return false;
        var held=owner.getItemInHand(hand);
        if(isTriple(held)){
            if(tripleCharges(owner)<1)return false;
            var origin=owner.getEyePosition().add(0,-.5,0);
            if(!owner.level().hasChunkAt(BlockPos.containing(origin)))return false;
            for(int yaw:new int[]{10,0,-10})spawn(owner,origin,owner.getYRot()+yaw,owner.getXRot());
            game.items().mana().score(owner,TRIPLE_CHARGE).add(-1);return true;
        }
        if(isBlaze(held)&&blazeCharges(owner)>=5){firing.put(owner,game.items().epoch());owner.entityTags().add(FIRING);return true;}
        return false;
    }
    public static final class ThrownSword extends Projectile implements PolymerEntity {
        int age;boolean grounded;ElementHolder holder;ItemDisplayElement display;BlockPos embeddedBlock;
        ThrownSword(EntityType<? extends ThrownSword> type,Level level){super(type,level);}
        @Override protected void defineSynchedData(SynchedEntityData.Builder builder){}
        @Override public EntityType<?> getPolymerEntityType(PacketContext context){return EntityTypes.MARKER;}
        @Override public boolean shouldBeSaved(){return false;}
        @Override public boolean isPushedByFluid(){return false;}
        ItemDisplayElement display(){return display;}
        private void attach(){
            var item=new ItemStack(Items.CARROT_ON_A_STICK);item.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:triple_golden_sword"));
            display=new ItemDisplayElement(item);display.setItemDisplayContext(ItemDisplayContext.FIXED);
            display.setTransformation(new Matrix4f().setRow(0,new Vector4f(0,0,2.5f,0))
                    .setRow(1,new Vector4f(1.7678f,-1.7678f,0,0)).setRow(2,new Vector4f(1.7678f,1.7678f,0,0)).setRow(3,new Vector4f(0,0,0,3)));
            display.setInterpolationDuration(1);display.setTeleportDuration(1);display.setShadowRadius(0);display.setShadowStrength(0);
            holder=new ElementHolder();holder.addElement(display);sync(position());new EntityAttachment(holder,this,false);
        }
        private void sync(Vec3 visual){
            if(holder==null)return;
            var v=getDeltaMovement();setYRot((float)Math.toDegrees(Math.atan2(-v.x,v.z)));setXRot((float)-Math.toDegrees(Math.atan2(v.y,v.horizontalDistance())));
            display.setYaw(getYRot());display.setPitch(getXRot());
            boolean stepped=((ServerLevel)level()).getServer().tickRateManager().isFrozen();
            display.setTeleportDuration(stepped?0:1);display.setAlwaysSyncAbsolutePosition(stepped);display.setOffset(visual.subtract(position()));holder.tick();
        }
        private void destroy(){if(holder!=null){holder.destroy();holder=null;}}
    }
    private void spawn(ServerPlayer owner,Vec3 origin,float yaw,float pitch){
        var sword=new ThrownSword(TYPE,owner.level());sword.setOwner(owner);sword.setPos(origin);sword.setNoGravity(true);
        sword.setYRot(yaw);sword.setXRot(pitch);sword.setDeltaMovement(Vec3.directionFromRotation(pitch,yaw).normalize().scale(1.2));
        sword.entityTags().addAll(List.of("thromn_sword","slowraycast","player","magic"));game.items().markCurrentItemEpoch(sword);
        if(owner.level().addFreshEntity(sword)){swords.add(sword);if(sword.holder==null)sword.attach();}else sword.discard();
    }
    public void loaded(Entity entity){if(entity instanceof ThrownSword sword){
        if(!game.items().currentEpoch(sword)||sword.level()!=game.maps().level()||!(sword.getOwner() instanceof ServerPlayer owner)||!valid(owner)){remove(sword);return;}
        swords.add(sword);if(sword.holder==null)sword.attach();
    }}
    public void unloaded(Entity entity){if(entity instanceof ThrownSword sword)remove(sword);}
    private void remove(ThrownSword sword){swords.remove(sword);sword.destroy();if(!sword.isRemoved())sword.discard();}
    /** Gameplay ends immediately; only the fixed final display survives interpolation and one contact frame. */
    private void finishModel(ThrownSword sword,ServerPlayer owner){
        if(sword.holder==null)return;
        sword.sync(sword.position());
        var holder=sword.holder;var previous=holder.getAttachment();var impact=sword.position();
        game.visualTracking().attach(holder,(ServerLevel)sword.level(),()->impact);
        if(previous!=null)previous.destroy();
        sword.holder=null;
        finishing.add(new FinishingModel(holder,sword.display,owner,game.items().epoch(),(ServerLevel)sword.level(),impact,visualClock+2));
    }
    private void clearFinishing(ServerPlayer owner){
        for(var model:List.copyOf(finishing))if(owner==null||model.owner==owner){model.holder.destroy();finishing.remove(model);}
    }
    private void breakSword(ThrownSword sword,ServerPlayer owner){
        var at=sword.position();LbcOverload.sound(owner,at,"minecraft:entity.item.break",.5f,1,0);
        owner.level().sendParticles(new ItemParticleOption(ParticleTypes.ITEM,Items.GOLDEN_SWORD),at.x,at.y,at.z,10,.2,.2,.2,0);
        finishModel(sword,owner);remove(sword);
    }
    /** Platform adaptation: resident native block-shape DDA and swept entity boxes replace source point samples/stalls. */
    private BlockHitResult wall(ThrownSword sword,Vec3 from,Vec3 to){
        var level=(ServerLevel)sword.level();
        return BlockGetter.traverseBlocks(from,to,level,(world,pos)->{
            if(!world.hasChunkAt(pos))return Shapes.block().clip(from,to,pos);
            return world.getBlockState(pos).getCollisionShape(world,pos,CollisionContext.of(sword)).clip(from,to,pos);
        },world->null);
    }
    private void step(ThrownSword sword,ServerPlayer owner){
        var level=owner.level();var from=sword.position();var v=sword.getDeltaMovement();var forward=v.normalize();
        if(!level.hasChunkAt(BlockPos.containing(from))||game.items().magic().suppressedAt(level,from)){remove(sword);return;}
        ++sword.age;
        if(sword.grounded){
            var ahead=BlockPos.containing(from.add(forward.scale(.5)));
            if(!level.hasChunkAt(ahead)){remove(sword);return;}
            if(level.getBlockState(ahead).is(AIRS)||owner.getRandom().nextInt(100)==0||sword.age>=100)breakSword(sword,owner);
            else sword.sync(from);
            return;
        }
        var to=from.add(v);var block=wall(sword,from,to);var end=block==null?to:block.getLocation();
        double suppression=game.items().magic().suppressionFraction(level,from,end);
        if(Double.isFinite(suppression))end=from.lerp(end,suppression);
        var hit=ProjectileUtil.getEntityHitResult(level,sword,from,end,new AABB(from,end).inflate(1),
                e->e.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().projectileTarget(owner,e),0);
        var greed=LbcGreed.firstHit(level,from,end,0);
        if(greed!=null&&(greed.getEntity()!=sword.getOwner()||!greed.getEntity().getBoundingBox().inflate(.2).contains(from))
                &&(hit==null||from.distanceToSqr(greed.getLocation())<=from.distanceToSqr(hit.getLocation()))){
            sword.setPos(greed.getLocation());LbcGreed.reflect(sword,greed.getEntity());sword.sync(sword.position());return;
        }
        if(hit!=null){
            sword.setPos(hit.getLocation());var cause=game.items().indirectKills().coldCause(owner,true);
            var damage=new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.ARROW),sword,owner);
            hit.getEntity().hurtServer(level,game.items().indirectKills().captureDamage(damage,cause),4);
            if(!swords.contains(sword)||sword.isRemoved())return;
            var at=sword.position();level.sendParticles(ParticleTypes.CRIT,at.x,at.y,at.z,8,.1,.1,.1,.5);
            LbcOverload.sound(owner,at,"minecraft:entity.player.attack.strong",1,1,0);finishModel(sword,owner);remove(sword);return;
        }
        if(Double.isFinite(suppression)){remove(sword);return;}
        if(block!=null){
            if(!level.hasChunkAt(block.getBlockPos())){remove(sword);return;}
            sword.embeddedBlock=block.getBlockPos().immutable();sword.grounded=true;
            // Source backs out by .1 before moving .5 into the obstruction.
            sword.setPos(end.add(forward.scale(.4)));sword.sync(sword.position());
            LbcOverload.sound(owner,end,"minecraft:block.copper.break",1.5f,1,0);
        }else {
            sword.setPos(to);
            // Source teleport/interpolation targets the completed step; prediction could cross a future entity or suppression contact.
            sword.sync(to);
        }
        if(sword.age>=100)breakSword(sword,owner);
    }
    private static final class Pulse {
        final ServerPlayer owner;final String epoch;final ServerLevel level;final Vec3 origin,direction;
        final Set<UUID> hit=new HashSet<>();final LbcIndirectKills.Owner cause;int nextSample;boolean cancelled;
        CompletableFuture<HitResult> future;
        Pulse(ServerPlayer owner,String epoch,LbcIndirectKills.Owner cause){this.owner=owner;this.epoch=epoch;level=owner.level();origin=owner.getEyePosition();direction=owner.getLookAngle().normalize();this.cause=cause;}
    }
    private boolean live(Pulse pulse){return !pulse.cancelled&&pulses.contains(pulse)&&valid(pulse.owner)&&pulse.owner.level()==pulse.level
            &&game.items().currentEpoch(pulse.epoch)&&!suppressed(pulse.owner);}
    private void sample(Pulse pulse,Vec3 at){
        if(!live(pulse)||!pulse.level.hasChunkAt(BlockPos.containing(at)))return;
        pulse.level.sendParticles(ParticleTypes.FLAME,at.x,at.y,at.z,10,.8,.8,.8,.01);
        var center=at.add(0,-1,0);
        for(var target:List.copyOf(pulse.level.getEntities(pulse.owner,new AABB(center,center).inflate(2),
                e->!pulse.hit.contains(e.getUUID())&&e.position().distanceToSqr(center)<=4&&game.items().wands().projectileTarget(pulse.owner,e)))){
            if(!live(pulse))return;
            pulse.hit.add(target.getUUID());
            var types=pulse.level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
            target.hurtServer(pulse.level,game.items().indirectKills().captureDamage(new DamageSource(types.getOrThrow(DamageTypes.IN_FIRE),pulse.owner,pulse.owner),pulse.cause),1);
            if(!live(pulse))return;
            target.hurtServer(pulse.level,game.items().indirectKills().captureDamage(new DamageSource(types.getOrThrow(DamageTypes.ARROW),pulse.owner,pulse.owner),pulse.cause),.1f);
        }
    }
    private void pulse(ServerPlayer owner){
        var pulse=new Pulse(owner,game.items().epoch(),game.items().indirectKills().coldCause(owner,true));pulses.add(pulse);
        LbcOverload.sound(owner,owner.position(),"minecraft:item.firecharge.use",1,1,16,.2f);
        sample(pulse,pulse.origin);pulse.nextSample=1;
        if(!live(pulse))return;
        // Source always samples the origin, even inside a block; only the next one-block point must be air.
        var next=pulse.origin.add(pulse.direction);
        var nextBlock=BlockPos.containing(next);
        if(!pulse.level.hasChunkAt(nextBlock)||!pulse.level.getBlockState(nextBlock).is(AIRS)){finishPulse(pulse);return;}
        pulse.future=game.items().exasperato().searchSpell(owner,next,pulse.direction,true,(pos,state)->!state.is(AIRS),e->false,(start,end)->{
            if(!live(pulse))return;
            double distance=end.subtract(pulse.origin).dot(pulse.direction);
            while(pulse.nextSample<=distance+1e-7){
                var point=pulse.origin.add(pulse.direction.scale(pulse.nextSample++));
                if(!pulse.level.hasChunkAt(BlockPos.containing(point))||!pulse.level.getBlockState(BlockPos.containing(point)).is(AIRS))break;
                sample(pulse,point);if(!live(pulse))return;
            }
        });
        pulse.future.whenComplete((hit,error)->finishPulse(pulse));
    }
    private void finishPulse(Pulse pulse){
        boolean settle=live(pulse);pulses.remove(pulse);
        if(settle){var owner=pulse.owner;var score=game.items().mana().score(owner,BLAZE_CHARGE);score.set(Math.max(0,score.get()-3));
            if(score.get()==0){firing.remove(owner);owner.entityTags().remove(FIRING);}}
    }
    public void tick(){
        ++visualClock;
        for(var model:List.copyOf(finishing))if(visualClock>=model.removeAt||!valid(model.owner)
                ||model.owner.level()!=model.level||!game.items().currentEpoch(model.epoch)
                ||!model.level.hasChunkAt(BlockPos.containing(model.position))||suppressed(model.owner)
                ||game.items().magic().suppressedAt(model.level,model.position)){
            model.holder.destroy();finishing.remove(model);
        }
        for(var sword:List.copyOf(swords)){
            if(sword.isRemoved()||!(sword.getOwner() instanceof ServerPlayer owner)||!valid(owner)
                    ||owner.level()!=sword.level()||!game.items().currentEpoch(sword)){remove(sword);continue;}
            if(paused(owner)||game.items().timeStop().frozen(sword)||sword.entityTags().contains("time_stopped")){sword.sync(sword.position());continue;}
            if(suppressed(owner)){remove(sword);continue;}step(sword,owner);
        }
        for(var owner:List.copyOf(firing.keySet()))if(!valid(owner)||!game.items().currentEpoch(firing.get(owner))||suppressed(owner))cancel(owner);
        for(var pulse:List.copyOf(pulses))if(!live(pulse))cancelPulse(pulse);
        if(game.items().wands().timeStopped())return;
        if(++clock%2!=0)return;
        for(var owner:List.copyOf(firing.keySet()))if(admitted(owner)&&blazePulses(owner)==0)pulse(owner);
    }
    private void cancelPulse(Pulse pulse){pulse.cancelled=true;pulses.remove(pulse);if(pulse.future!=null)pulse.future.cancel(false);}
    private void cancel(ServerPlayer owner){firing.remove(owner);owner.entityTags().remove(FIRING);for(var pulse:List.copyOf(pulses))if(pulse.owner==owner)cancelPulse(pulse);}
    /** Source purify resets the counters but lets the already active fire settle on its next pulse. */
    public void clear(ServerPlayer owner){game.items().mana().score(owner,TRIPLE_CHARGE).set(0);game.items().mana().score(owner,BLAZE_CHARGE).set(0);}
    public void forget(ServerPlayer owner){clear(owner);cancel(owner);clearFinishing(owner);for(var sword:List.copyOf(swords))if(sword.getOwner()==owner)remove(sword);}
    public void stop(){
        for(var owner:List.copyOf(firing.keySet()))cancel(owner);for(var pulse:List.copyOf(pulses))cancelPulse(pulse);
        for(var sword:List.copyOf(swords))remove(sword);clearFinishing(null);
        for(var owner:game.server().getPlayerList().getPlayers()){clear(owner);owner.entityTags().remove(FIRING);}clock=0;visualClock=0;
    }
}
