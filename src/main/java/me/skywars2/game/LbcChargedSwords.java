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
import eu.pb4.polymer.common.impl.entity.InternalEntityHelpers;
import eu.pb4.polymer.virtualentity.api.data.SynchedEntityDataLike;
import net.minecraft.network.syncher.EntityDataAccessor;

/** Shared source player counters, straight Triple copies and independent Blazefire continuation. */
public final class LbcChargedSwords {
    public static final Item TRIPLE=registerItem("triple_golden_sword",900,"triple_golden_sword");
    public static final Item BLAZE=registerItem("blazefire_sword",600,"lbckit.18");
    public static final EntityType<ThrownSword> TYPE=registerType();
    private static final String HOSTILE="skywars2.dungeon_sword",BONE="bone";
    private static final String TRIPLE_CHARGE="swordcharged8",BLAZE_CHARGE="swordcharged9",FIRING="firefarting";
    private static final TagKey<DamageType> ARROW_ONLY=TagKey.create(Registries.DAMAGE_TYPE,Identifier.parse("minecraft:arrow_only"));
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private final GameController game;
    private final Set<ThrownSword> swords=new LinkedHashSet<>();
    private IdentityHashMap<ServerPlayer,String> firing=new IdentityHashMap<>();
    private final Set<Pulse> pulses=new LinkedHashSet<>();
    private int clock,visualClock;
    private int callbackDepth;
    private boolean rewinding,presentationReady;
    private final IdentityHashMap<ElementHolder,List<ServerPlayer>> presentationWatchers=new IdentityHashMap<>();
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
        callbackDepth++;try{
        if(!(source.getEntity() instanceof ServerPlayer owner)||!LbcDamage.isMeleeAttack(source,owner)||!admitted(owner)||source.is(ARROW_ONLY)
                ||!game.items().wands().validRoleTarget(victim,true))return;
        var dimension=owner.level().dimension().identifier();
        if(dimension.equals(Identifier.parse("minecraft:nexus"))||dimension.equals(Identifier.parse("minecraft:imprinted")))return;
        var held=owner.getMainHandItem();
        if(isTriple(held)&&tripleCharges(owner)<=5)game.items().mana().score(owner,TRIPLE_CHARGE).add(1);
        if(isBlaze(held))game.items().mana().score(owner,BLAZE_CHARGE).set((int)Math.min(Integer.MAX_VALUE,(long)blazeCharges(owner)+1));

        }finally{callbackDepth--;}
    }
    public boolean use(ServerPlayer owner,InteractionHand hand){
        callbackDepth++;try{
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

        }finally{callbackDepth--;}
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
            var item=new ItemStack(entityTags().contains(BONE)?Items.GOLDEN_SWORD:Items.CARROT_ON_A_STICK);item.set(DataComponents.ITEM_MODEL,Identifier.parse(entityTags().contains(BONE)?"lbc:tiny_bone":"lbc:triple_golden_sword"));
            display=new SwordDisplay(item);display.setItemDisplayContext(ItemDisplayContext.FIXED);
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
    /** Source evil Triple copies and tiny bones use the same native flight/contact/visual owner. */
    public void spawnHostile(Entity source,Vec3 origin,float yaw,float pitch,boolean bone){
        if(source==null||!(source.level() instanceof ServerLevel level)||!game.items().currentEpoch(source)
                ||!level.hasChunkAt(BlockPos.containing(origin)))return;
        var sword=new ThrownSword(TYPE,level);sword.setOwner(source);sword.setPos(origin);sword.setNoGravity(true);
        sword.setYRot(yaw);sword.setXRot(pitch);sword.setDeltaMovement(Vec3.directionFromRotation(pitch,yaw).normalize().scale(1.2));
        sword.entityTags().addAll(List.of(HOSTILE,"thromn_sword","slowraycast","player","evil"));
        sword.entityTags().add(bone?BONE:"magic");game.items().markCurrentItemEpoch(sword);
        game.items().spellSources().inherit(source,sword);
        if(level.addFreshEntity(sword)){swords.add(sword);if(sword.holder==null)sword.attach();}else sword.discard();
    }
    public void loaded(Entity entity){
        callbackDepth++;try{if(rewinding)return;if(entity instanceof ThrownSword sword){
        if(!game.items().currentEpoch(sword)||sword.level()!=game.maps().level()||!sword.entityTags().contains(HOSTILE)&&(!(sword.getOwner() instanceof ServerPlayer owner)||!valid(owner))){remove(sword);return;}
        swords.add(sword);if(sword.holder==null)sword.attach();
    }
        }finally{callbackDepth--;}
    }
    public void unloaded(Entity entity){
        callbackDepth++;try{if(rewinding)return;if(entity instanceof ThrownSword sword)remove(sword);
        }finally{callbackDepth--;}
    }
    private void remove(ThrownSword sword){swords.remove(sword);sword.destroy();if(!sword.isRemoved())sword.discard();}
    /** Gameplay ends immediately; only the fixed final display survives interpolation and one contact frame. */
    private void finishModel(ThrownSword sword,ServerPlayer owner){
        callbackDepth++;try{
        if(sword.holder==null)return;
        sword.sync(sword.position());
        var holder=sword.holder;var previous=holder.getAttachment();var impact=sword.position();
        game.visualTracking().attach(holder,(ServerLevel)sword.level(),()->impact);
        if(previous!=null)previous.destroy();
        sword.holder=null;
        finishing.add(new FinishingModel(holder,sword.display,owner,game.items().epoch(),(ServerLevel)sword.level(),impact,visualClock+2));

        }finally{callbackDepth--;}
    }
    private void clearFinishing(ServerPlayer owner){
        for(var model:List.copyOf(finishing))if(owner==null||model.owner==owner){model.holder.destroy();finishing.remove(model);}
    }
    private void breakSword(ThrownSword sword,ServerPlayer owner){
        var at=sword.position();LbcOverload.sound((ServerLevel)sword.level(),at,"minecraft:entity.item.break",.5f,1,0,0);
        ((ServerLevel)sword.level()).sendParticles(new ItemParticleOption(ParticleTypes.ITEM,sword.entityTags().contains(BONE)?Items.BONE:Items.GOLDEN_SWORD),at.x,at.y,at.z,10,.2,.2,.2,0);
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
        var level=(ServerLevel)sword.level();var from=sword.position();var v=sword.getDeltaMovement();var forward=v.normalize();
        boolean hostile=sword.entityTags().contains(HOSTILE),magic=!sword.entityTags().contains(BONE);
        if(!level.hasChunkAt(BlockPos.containing(from))||magic&&game.items().magic().suppressedAt(level,from)){remove(sword);return;}
        ++sword.age;
        if(sword.grounded){
            var ahead=BlockPos.containing(from.add(forward.scale(.5)));
            if(!level.hasChunkAt(ahead)){remove(sword);return;}
            if(level.getBlockState(ahead).is(AIRS)||(hostile?level.getRandom():owner.getRandom()).nextInt(100)==0||sword.age>=100)breakSword(sword,owner);
            else sword.sync(from);
            return;
        }
        var to=from.add(v);var block=wall(sword,from,to);var end=block==null?to:block.getLocation();
        double suppression=magic?game.items().magic().suppressionFraction(level,from,end):Double.POSITIVE_INFINITY;
        if(Double.isFinite(suppression))end=from.lerp(end,suppression);
        var hit=ProjectileUtil.getEntityHitResult(level,sword,from,end,new AABB(from,end).inflate(1),
                e->e.getType().builtInRegistryHolder().is(MOBS)&&(hostile?
                        !e.entityTags().contains("dungeon_mob")&&game.items().wands().validRoleTarget(e,false):game.items().wands().projectileTarget(owner,e)),0);
        var greed=LbcGreed.firstHit(level,from,end,0);
        if(greed!=null&&(greed.getEntity()!=sword.getOwner()||!greed.getEntity().getBoundingBox().inflate(.2).contains(from))
                &&(hit==null||from.distanceToSqr(greed.getLocation())<=from.distanceToSqr(hit.getLocation()))){
            sword.setPos(greed.getLocation());LbcGreed.reflect(sword,greed.getEntity());sword.entityTags().remove(HOSTILE);sword.sync(sword.position());return;
        }
        if(hit!=null){
            sword.setPos(hit.getLocation());var policy=game.items().spellSources().source(sword);
            var cause=hostile?(policy==null?null:policy.cause()):game.items().indirectKills().coldCause(owner,true);
            var damage=new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.ARROW),sword,hostile?sword.getOwner():owner);
            hit.getEntity().hurtServer(level,game.items().indirectKills().captureDamage(damage,cause),4);
            if(!swords.contains(sword)||sword.isRemoved())return;
            var at=sword.position();level.sendParticles(ParticleTypes.CRIT,at.x,at.y,at.z,8,.1,.1,.1,.5);
            LbcOverload.sound(level,at,"minecraft:entity.player.attack.strong",1,1,0,0);finishModel(sword,owner);remove(sword);return;
        }
        if(Double.isFinite(suppression)){remove(sword);return;}
        if(block!=null){
            if(!level.hasChunkAt(block.getBlockPos())){remove(sword);return;}
            sword.embeddedBlock=block.getBlockPos().immutable();sword.grounded=true;
            // Source backs out by .1 before moving .5 into the obstruction.
            sword.setPos(end.add(forward.scale(.4)));sword.sync(sword.position());
            LbcOverload.sound(level,end,"minecraft:block.copper.break",1.5f,1,0,0);
        }else {
            sword.setPos(to);
            // Source teleport/interpolation targets the completed step; prediction could cross a future entity or suppression contact.
            sword.sync(to);
        }
        if(sword.age>=100)breakSword(sword,owner);
    }
    private final class Pulse implements LbcExasperato.ReplayableRay {
        ServerPlayer owner;final String epoch;final ServerLevel level;final Vec3 origin,direction;
        final Set<UUID> hit=new HashSet<>();final LbcIndirectKills.Owner cause;int nextSample;boolean cancelled;
        LbcExasperato.RayTicket ticket;
        Pulse(ServerPlayer owner,String epoch,LbcIndirectKills.Owner cause){this.owner=owner;this.epoch=epoch;level=owner.level();origin=owner.getEyePosition();direction=owner.getLookAngle().normalize();this.cause=cause;}
        @Override public boolean block(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){return !state.is(AIRS);}
        @Override public boolean target(Entity entity){return false;}
        @Override public void submitted(LbcExasperato.RayTicket ticket){this.ticket=ticket;}
        @Override public void trail(Vec3 start,Vec3 end){
            callbackDepth++;try{
                if(!live(this))return;
                double distance=end.subtract(origin).dot(direction);
                while(nextSample<=distance+1e-7){
                    var point=origin.add(direction.scale(nextSample++));
                    if(!level.hasChunkAt(BlockPos.containing(point))||!level.getBlockState(BlockPos.containing(point)).is(AIRS))break;
                    sample(this,point);if(!live(this))return;
                }
            }finally{callbackDepth--;}
        }
        @Override public void complete(HitResult hit){finishPulse(this);}
        @Override public MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){return new PulseImage(this,links);}
    }
    private boolean live(Pulse pulse){return !pulse.cancelled&&pulses.contains(pulse)&&valid(pulse.owner)&&pulse.owner.level()==pulse.level
            &&game.items().currentEpoch(pulse.epoch)&&!suppressed(pulse.owner);}
    private void sample(Pulse pulse,Vec3 at){
        callbackDepth++;try{
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

        }finally{callbackDepth--;}
    }
    private void pulse(ServerPlayer owner){
        callbackDepth++;try{
        var pulse=new Pulse(owner,game.items().epoch(),game.items().indirectKills().coldCause(owner,true));pulses.add(pulse);
        LbcOverload.sound(owner,owner.position(),"minecraft:item.firecharge.use",1,1,16,.2f);
        sample(pulse,pulse.origin);pulse.nextSample=1;
        if(!live(pulse))return;
        // Source always samples the origin, even inside a block; only the next one-block point must be air.
        var next=pulse.origin.add(pulse.direction);
        var nextBlock=BlockPos.containing(next);
        if(!pulse.level.hasChunkAt(nextBlock)||!pulse.level.getBlockState(nextBlock).is(AIRS)){finishPulse(pulse);return;}
        game.items().exasperato().replayableTool(owner,next,pulse.direction,Double.POSITIVE_INFINITY,true,pulse,false);

        }finally{callbackDepth--;}
    }
    private void finishPulse(Pulse pulse){
        callbackDepth++;try{
        boolean settle=live(pulse);pulses.remove(pulse);
        if(settle){var owner=pulse.owner;var score=game.items().mana().score(owner,BLAZE_CHARGE);score.set(Math.max(0,score.get()-3));
            if(score.get()==0){firing.remove(owner);owner.entityTags().remove(FIRING);}}

        }finally{callbackDepth--;}
    }
    public void tick(){
        callbackDepth++;try{
        if(rewinding)return;
        ++visualClock;
        for(var model:List.copyOf(finishing))if(visualClock>=model.removeAt||model.owner!=null&&(!valid(model.owner)
                ||model.owner.level()!=model.level)||!game.items().currentEpoch(model.epoch)
                ||!model.level.hasChunkAt(BlockPos.containing(model.position))||model.owner!=null&&suppressed(model.owner)
                ||game.items().magic().suppressedAt(model.level,model.position)){
            model.holder.destroy();finishing.remove(model);
        }
        for(var sword:List.copyOf(swords)){
            boolean hostile=sword.entityTags().contains(HOSTILE);
            var owner=sword.getOwner() instanceof ServerPlayer player?player:null;
            if(sword.isRemoved()||!hostile&&(!valid(owner)||owner.level()!=sword.level())
                    ||!game.items().currentEpoch(sword)){remove(sword);continue;}
            if((hostile?game.items().timeStop().stopped((ServerLevel)sword.level()):paused(owner))||game.items().timeStop().frozen(sword)||sword.entityTags().contains("time_stopped")){sword.sync(sword.position());continue;}
            if(!hostile&&suppressed(owner)){remove(sword);continue;}step(sword,hostile?null:owner);
        }
        for(var owner:List.copyOf(firing.keySet()))if(!valid(owner)||!game.items().currentEpoch(firing.get(owner))||suppressed(owner))cancel(owner);
        for(var pulse:List.copyOf(pulses))if(!live(pulse))cancelPulse(pulse);
        if(game.items().wands().timeStopped())return;
        if(++clock%2!=0)return;
        for(var owner:List.copyOf(firing.keySet()))if(admitted(owner)&&blazePulses(owner)==0)pulse(owner);

        }finally{callbackDepth--;}
    }
    private void cancelPulse(Pulse pulse){pulse.cancelled=true;pulses.remove(pulse);if(pulse.ticket!=null)pulse.ticket.cancel();}
    private void cancel(ServerPlayer owner){firing.remove(owner);owner.entityTags().remove(FIRING);for(var pulse:List.copyOf(pulses))if(pulse.owner==owner)cancelPulse(pulse);}
    /** Source purify resets the counters but lets the already active fire settle on its next pulse. */
    public void clear(ServerPlayer owner){
        callbackDepth++;try{game.items().mana().score(owner,TRIPLE_CHARGE).set(0);game.items().mana().score(owner,BLAZE_CHARGE).set(0);
        }finally{callbackDepth--;}
    }
    public void forget(ServerPlayer owner){
        callbackDepth++;try{clear(owner);cancel(owner);clearFinishing(owner);for(var sword:List.copyOf(swords))if(sword.getOwner()==owner)remove(sword);
        }finally{callbackDepth--;}
    }
    public void stop(){
        callbackDepth++;try{
        for(var owner:List.copyOf(firing.keySet()))cancel(owner);for(var pulse:List.copyOf(pulses))cancelPulse(pulse);
        for(var sword:List.copyOf(swords))remove(sword);clearFinishing(null);
        for(var owner:game.server().getPlayerList().getPlayers()){clear(owner);owner.entityTags().remove(FIRING);}clock=0;visualClock=0;presentationWatchers.clear();rewinding=false;presentationReady=false;

        }finally{callbackDepth--;}
    }
    void checkpointBoundary(){if(callbackDepth!=0)throw new IllegalStateException("Finish charged-sword tick/hit/ray callbacks before checkpointing");}
    private void frozenBoundary(){checkpointBoundary();if(!game.server().isSameThread()||!game.server().tickRateManager().isFrozen())throw new IllegalStateException("Restore charged-sword presentation on its frozen native thread");}
    /** Retire only the abandoned LIVE holders; saved images contain values, never reusable destroyed holders. */
    void prepareFrozen(){
        frozenBoundary();rewinding=true;presentationReady=false;presentationWatchers.clear();
        for(var sword:swords)sword.destroy();for(var model:finishing)model.holder.destroy();
    }
    /** Call after all native bodies and clients have been paired; no clocks or gameplay callbacks run. */
    Set<UUID> publishFrozen(){
        frozenBoundary();if(!rewinding||!presentationReady)throw new IllegalStateException("Install charged-sword state before presentation");
        // Retained offline references are source data, never client connections to pair or watch.
        presentationWatchers.replaceAll((holder,players)->players.stream().filter(player->!MatchOfflinePlayers.retained(player)).toList());
        var pending=new LinkedHashSet<UUID>();
        for(var entry:presentationWatchers.entrySet())for(var player:entry.getValue()){
            if(game.server().getPlayerList().getPlayer(player.getUUID())!=player)throw new IllegalStateException("Charged-sword viewer body changed before publication");
            if(player.isChangingDimension()||!player.connection.hasClientLoaded())pending.add(player.getUUID());
        }
        for(var sword:swords)if(sword.holder!=null)for(var player:presentationWatchers.get(sword.holder))
            if(!net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(sword).contains(player))pending.add(player.getUUID());
        if(!pending.isEmpty())return Set.copyOf(pending);
        for(var sword:swords)if(sword.holder!=null&&sword.holder.getAttachment()==null)new EntityAttachment(sword.holder,sword,false);
        for(var model:finishing)if(model.holder.getAttachment()==null)game.visualTracking().attach(model.holder,model.level,()->model.position);
        presentationWatchers.forEach((holder,players)->players.forEach(holder::startWatching));
        presentationWatchers.clear();rewinding=false;presentationReady=false;return Set.of();
    }
    void cancelFrozen(){frozenBoundary();for(var sword:swords)sword.destroy();for(var model:finishing)model.holder.destroy();finishing.clear();presentationWatchers.clear();rewinding=false;presentationReady=false;}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){return new Checkpoint(links);}
    private final class PulseImage implements MatchQueuedWorkCheckpoint.Image {
        private final Pulse pulse;
        private final MatchDamageSources.Ref owner;
        private final Set<UUID> hit;
        private final int next;
        private final boolean cancelled;
        private final LbcExasperato.RayTicket ticket;
        PulseImage(Pulse pulse,MatchSpellCheckpoint.Links links){
            checkpointBoundary();this.pulse=pulse;owner=links.key(pulse.owner);hit=Set.copyOf(pulse.hit);next=pulse.nextSample;
            cancelled=pulse.cancelled;ticket=pulse.ticket;
            // coldCause is the immutable UUID/order/friendlyFire/projectile record; no native player is hidden in it.
            if(pulse.cause==null||!pulse.cause.player().equals(pulse.owner.getUUID()))throw new IllegalStateException("Charged-sword pulse has an unreviewed cause");
        }
        @Override public Runnable prepare(MatchSpellCheckpoint.Links links){
            var current=(ServerPlayer)links.get(owner);
            return ()->{pulse.owner=current;pulse.hit.clear();pulse.hit.addAll(hit);pulse.nextSample=next;pulse.cancelled=cancelled;pulse.ticket=ticket;};
        }
    }
    final class Checkpoint {
        private record SwordImage(MatchDamageSources.Ref actor,int age,boolean grounded,BlockPos embedded,ModelImage model) { }
        private record FinishImage(MatchDamageSources.Ref owner,String epoch,ServerLevel level,Vec3 position,int removeAt,ModelImage model) { }
        private final List<SwordImage> actors=new ArrayList<>();
        private final List<FinishImage> models=new ArrayList<>();
        private final List<PulseImage> rays=new ArrayList<>();
        private final MatchSpellCheckpoint.IdentityImage<ServerPlayer,String> firingImage;
        private final int savedClock=clock,savedVisualClock=visualClock;
        Checkpoint(MatchSpellCheckpoint.Links links){
            checkpointBoundary();if(rewinding)throw new IllegalStateException("Publish charged-sword presentation before capture");
            firingImage=new MatchSpellCheckpoint.IdentityImage<>(firing,links);
            for(var sword:swords)actors.add(new SwordImage(links.key(sword),sword.age,sword.grounded,sword.embeddedBlock,
                    sword.holder==null?null:new ModelImage(sword.display,links)));
            for(var model:finishing)models.add(new FinishImage(links.key(model.owner),model.epoch,model.level,model.position,model.removeAt,new ModelImage(model.display,links)));
            for(var pulse:pulses)rays.add(new PulseImage(pulse,links));
        }
        Runnable prepare(MatchSpellCheckpoint.Links links){
            checkpointBoundary();var restoredFiring=firingImage.restore(links);
            var currentActors=new ArrayList<ThrownSword>();for(var image:actors)currentActors.add((ThrownSword)links.get(image.actor));
            var currentOwners=new ArrayList<ServerPlayer>();for(var image:models)currentOwners.add((ServerPlayer)links.get(image.owner));
            var actorModels=new ArrayList<PreparedModel>();for(var image:actors)actorModels.add(image.model==null?null:image.model.prepare(links));
            var finishModels=models.stream().map(image->image.model.prepare(links)).toList();
            var pulsePlans=rays.stream().map(image->image.prepare(links)).toList();
            return ()->{
                frozenBoundary();if(!rewinding)throw new IllegalStateException("Detach future charged-sword presentation before private restore");
                for(var sword:swords)sword.destroy();for(var model:finishing)model.holder.destroy();
                swords.clear();finishing.clear();presentationWatchers.clear();firing=restoredFiring;pulses.clear();
                clock=savedClock;visualClock=savedVisualClock;
                for(int i=0;i<actors.size();i++){
                    var image=actors.get(i);var sword=currentActors.get(i);sword.age=image.age;sword.grounded=image.grounded;sword.embeddedBlock=image.embedded;
                    sword.destroy();sword.display=null;
                    if(image.model!=null){var model=actorModels.get(i);sword.display=model.image.create();sword.holder=new ElementHolder();sword.holder.addElement(sword.display);presentationWatchers.put(sword.holder,model.watchers);}
                    swords.add(sword);
                }
                for(int i=0;i<models.size();i++){
                    var image=models.get(i);var model=finishModels.get(i);var display=model.image.create();var holder=new ElementHolder();holder.addElement(display);presentationWatchers.put(holder,model.watchers);
                    finishing.add(new FinishingModel(holder,display,currentOwners.get(i),image.epoch,image.level,image.position,image.removeAt));
                }
                pulsePlans.forEach(Runnable::run);for(var image:rays)pulses.add(image.pulse);presentationReady=true;
            };
        }
    }
    /** Pinned Polymer tracker schema supplies canonical accessor identities; detach mutable tracker values. */
    private record TrackerImage<T>(EntityDataAccessor<T> accessor,T value,boolean dirty) {
        @SuppressWarnings("unchecked") private static <T> T copy(EntityDataAccessor<T> key,T value){
            // Native value-type serializers intentionally retain JOML interfaces; their backing objects are mutable.
            if(value instanceof org.joml.Vector3fc vector)return (T)new org.joml.Vector3f(vector);
            if(value instanceof org.joml.Quaternionfc rotation)return (T)new org.joml.Quaternionf(rotation);
            if(value instanceof net.minecraft.network.chat.Component text)return (T)text.copy();
            if(value instanceof Optional<?> optional&&optional.orElse(null) instanceof net.minecraft.network.chat.Component text)
                return (T)Optional.of(text.copy());
            return key.serializer().copy(value);
        }
        static <T> TrackerImage<T> capture(EntityDataAccessor<T> key,SynchedEntityDataLike data){return new TrackerImage<>(key,copy(key,data.get(key)),data.isDirty(key));}
        void install(SynchedEntityDataLike data){data.set(accessor,copy(accessor,value),true);}
        void dirty(SynchedEntityDataLike data){data.setDirty(accessor,dirty);}
    }
    private static final class SwordDisplay extends ItemDisplayElement {
        SwordDisplay(ItemStack item){super(item);}
        void savedPosition(Vec3 position){lastSyncedPos=position;}
    }
    private record PreparedModel(ModelImage image,List<ServerPlayer> watchers) { }
    private static final class ModelImage {
        private final List<TrackerImage<?>> data=new ArrayList<>();
        private final List<MatchDamageSources.Ref> watchers;
        private final float yaw,pitch;
        private final Vec3 offset,override,lastPosition;
        private final boolean absolute,sendPosition,rotationDirty;
        ModelImage(ItemDisplayElement display,MatchSpellCheckpoint.Links links){
            if(display==null||display.getHolder()==null)throw new IllegalStateException("Charged-sword holder lacks its display");
            watchers=display.getHolder().getWatchingPlayers().stream().map(connection->links.key(connection.player)).toList();
            for(var item:InternalEntityHelpers.getExampleTrackedDataOfEntityType(EntityTypes.ITEM_DISPLAY))data.add(TrackerImage.capture(item.getAccessor(),display.getSyncedData()));
            yaw=display.getYaw();pitch=display.getPitch();offset=display.getOffset();override=display.getOverridePos();lastPosition=display.getLastSyncedPos();
            absolute=display.isAlwaysSyncingAbsolutePosition();sendPosition=display.isSendingPositionUpdates();rotationDirty=display.isRotationDirty();
        }
        PreparedModel prepare(MatchSpellCheckpoint.Links links){return new PreparedModel(this,watchers.stream().map(ref->(ServerPlayer)links.get(ref)).filter(player->!MatchOfflinePlayers.retained(player)).toList());}
        ItemDisplayElement create(){
            var display=new SwordDisplay(ItemStack.EMPTY);display.setYaw(yaw==0?1:0);display.setYaw(yaw);display.setPitch(pitch);
            // Clear construction-only rotation state while detached; then install the saved tracker/dirty image.
            if(!rotationDirty)display.tick();data.forEach(image->image.install(display.getSyncedData()));display.getSyncedData().getDirtyEntries();
            data.forEach(image->image.dirty(display.getSyncedData()));
            display.setOffset(offset);display.setOverridePos(override);display.savedPosition(lastPosition);
            display.setAlwaysSyncAbsolutePosition(absolute);display.setSendPositionUpdates(sendPosition);return display;
        }
    }

}
