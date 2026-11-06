package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.*;
import java.util.*;

/** Whole current and legacy mystical2 inventory family, with owned temporary WILD smoke. */
public final class LbcReactiveArtifacts {
    private static final TagKey<DamageType> ARROWS=TagKey.create(Registries.DAMAGE_TYPE,Identifier.parse("minecraft:arrow_only"));
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final Identifier RANGE=Identifier.parse("skywars2:reactive_smoke");
    private final GameController game;
    private final Map<ServerPlayer,Window> attacks=new IdentityHashMap<>();
    private final List<Cloud> clouds=new ArrayList<>();
    private final Map<LivingEntity,Blindness> blind=new IdentityHashMap<>();
    private final Set<LivingEntity> reduced=Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean applying,producing;
    private static final class Window {int depth=1;final List<Runnable> moves=new ArrayList<>();}
    private static final class Cloud {
        final ServerPlayer owner;final ServerLevel level;final String epoch;final Vec3 center;final double radius;final int amplifier,visual;int expires;
        Cloud(ServerPlayer p,String e){this(p,e,p.position(),7,0,100,0);}
        Cloud(ServerPlayer p,String e,Vec3 center,double radius,int amplifier,int life,int visual){owner=p;level=p.level();epoch=e;this.center=center;this.radius=radius;this.amplifier=amplifier;this.visual=visual;expires=p.level().getServer().getTickCount()+life;}
    }
    private static final class Blindness {MobEffectInstance baseline;final int amplifier;int lastTick;
        Blindness(MobEffectInstance original,int tick,int amplifier){this.amplifier=amplifier;baseline=smokeLayer(original,amplifier);lastTick=tick;}}
    public LbcReactiveArtifacts(GameController game){this.game=game;}
    private boolean valid(ServerPlayer p){return p!=null&&p.isAlive()&&!p.isRemoved()&&p.level()==game.maps().level()&&game.canUseGameplayItems(p)&&!game.isObserver(p)&&game.stage()!=GameController.Stage.ENDING;}
    private boolean paused(ServerPlayer p){return game.items().wands().inputTimeStopped(p)||game.items().timeStop().stopped(p.level());}
    private boolean admitted(ServerPlayer p){return valid(p)&&!paused(p)&&!game.items().actionsBlocked(p)&&game.items().wands().validRoleTarget(p,false);}
    private boolean currentMagic(ServerPlayer p){return !game.items().magic().beaconSuppressed(p.position())&&p.level().players().stream().noneMatch(v->!v.isSpectator()&&v.entityTags().contains("nomagic_active")&&v.distanceToSqr(p)<=400);}
    private boolean wildTarget(Entity e){return game.items().wands().validRoleTarget(e,false)&&!LbcGreed.active(e);}
    public void beginAttack(ServerPlayer owner){var w=attacks.get(owner);if(w==null)attacks.put(owner,new Window());else w.depth++;}
    public void endAttack(ServerPlayer owner){var w=attacks.get(owner);if(w==null||--w.depth>0)return;attacks.remove(owner);if(valid(owner))for(var move:w.moves)move.run();}
    private void moveAfterAttack(ServerPlayer owner,Runnable action){var window=attacks.get(owner);if(window==null)action.run();else window.moves.add(action);}
    public void damaged(LivingEntity victim,DamageSource source){
        if(producing||!(source.getEntity() instanceof ServerPlayer owner)||!admitted(owner)||source.is(ARROWS)||!game.items().wands().projectileTarget(owner,victim,true))return;
        var dimension=owner.level().dimension().identifier();if(dimension.equals(Identifier.parse("minecraft:nexus"))||dimension.equals(Identifier.parse("minecraft:imprinted")))return;
        int current=0,old=0;
        for(int slot=0;slot<=40;slot++){var stack=owner.getInventory().getItem(slot);if(stack.isEmpty())continue;var data=LbcFartus.data(stack);if(data.getIntOr("tmt_item",0)==1){int masks=LbcTmtInventory.mysticalTypes(data,2);current|=masks&0x7e;old|=(masks>>>16)&0x7e;}else{int type=data.getIntOr("type",0);if(type<1||type>6)continue;if(data.getIntOr("mystical",0)==2)current|=1<<type;if(data.getIntOr("mystical_old",0)==2)old|=1<<type;}}
        int currentFlags=currentMagic(owner)?current:0,oldFlags=old;
        // Replay each accepted event in source type order after native sweep collection.
        // This also keeps old4 movement before the following old5/6 area queries.
        moveAfterAttack(owner,()->{
            if(!valid(owner))return;producing=true;try{
                for(int type=1;type<=6;type++)if((currentFlags&(1<<type))!=0)current(owner,victim,type);
                for(int type=1;type<=6;type++)if((oldFlags&(1<<type))!=0)old(owner,type);
            }finally{producing=false;}
        });
    }
    private void effect(ServerPlayer owner,LivingEntity target,Holder<MobEffect> kind,int ticks,int amplifier,boolean hidden,boolean wild){var value=ItemEffectLevels.create(kind,ticks,amplifier,false,!hidden);if(wild)game.items().indirectKills().wildEffect(target,value,game.items().indirectKills().wildCause(owner));else target.addEffect(value,owner);}
    private void particle(ServerPlayer p,net.minecraft.core.particles.ParticleOptions kind,Vec3 at,int count,double x,double y,double z,double speed,boolean force){p.level().sendParticles(kind,force,false,at.x,at.y,at.z,count,x,y,z,speed);}
    private void current(ServerPlayer owner,LivingEntity victim,int type){switch(type){
        case 1->{effect(owner,owner,MobEffects.REGENERATION,40,2,true,false);effect(owner,owner,MobEffects.SATURATION,2,4,true,false);effect(owner,victim,MobEffects.WITHER,40,4,true,false);effect(owner,victim,MobEffects.HUNGER,40,99,true,false);game.items().mana().score(owner,"food_count").add(1);particle(owner,ParticleTypes.HEART,owner.position().add(0,1,0),5,.5,1,.5,0,false);LbcOverload.sound(owner,"minecraft:entity.cat.hiss",1,2,0);}
        case 2->effect(owner,victim,MobEffects.POISON,400,4,false,false);
        case 3->smoke(owner);
        case 4->{moveAfterAttack(owner,()->{forward(owner,victim,4,false,false,true);forward(owner,owner,4,false,false,true);});}
        case 5->moveAfterAttack(owner,()->teleport(owner,victim,victim.position(),victim.getYRot()+180,victim.getXRot(),false));
        case 6->{game.items().mana().score(owner,"mana").add(1000);LbcOverload.sound(owner,"minecraft:entity.witch.ambient",1,1,0);particle(owner,ParticleTypes.WITCH,owner.position().add(0,1,0),20,.5,1,.5,.01,false);}
        default->throw new IllegalArgumentException("reactive type "+type);
    }}
    private List<LivingEntity> area(ServerPlayer owner,double radius,boolean wild){return owner.level().getEntitiesOfClass(LivingEntity.class,owner.getBoundingBox().inflate(radius),e->e.distanceToSqr(owner)<=radius*radius&&(wild?wildTarget(e):e==owner?wildTarget(e):game.items().wands().projectileTarget(owner,e)));}
    private void old(ServerPlayer owner,int type){switch(type){
        case 1->{effect(owner,owner,MobEffects.REGENERATION,20,1,true,false);effect(owner,owner,MobEffects.SATURATION,1,1,true,false);}
        case 2->effect(owner,owner,MobEffects.STRENGTH,40,0,true,false);
        case 3->{var at=owner.position().add(0,1,0);for(var viewer:owner.level().players())if(viewer.distanceToSqr(owner)>=4)owner.level().sendParticles(viewer,ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,true,false,at.x,at.y,at.z,200,.3,.3,.3,.2);}
        case 4->moveAfterAttack(owner,()->{forward(owner,owner,3,true,true,false);for(var target:owner.level().getEntities((Entity)null,owner.getBoundingBox().inflate(8),e->!(e instanceof ServerPlayer)&&!(e instanceof ArmorStand)&&!(e instanceof AreaEffectCloud)&&e.distanceToSqr(owner)<=64&&wildTarget(e)))forward(owner,target,3,false,true,false);});
        case 5->{for(var target:area(owner,8,true))effect(owner,target,MobEffects.WITHER,80,10,true,true);}
        case 6->{for(var target:area(owner,8,false))effect(owner,target,MobEffects.REGENERATION,80,2,true,false);}
        default->throw new IllegalArgumentException("legacy reactive type "+type);
    }}
    private void forward(ServerPlayer owner,Entity target,int distance,boolean eyes,boolean wild,boolean flat){
        if(target.isRemoved()||!wildTarget(target))return;var direction=flat?Vec3.directionFromRotation(0,target.getYRot()):target.getLookAngle();var at=eyes?target.getEyePosition():target.position();
        if(flat)for(int z=0;z<=3;z++)particle(owner,ParticleTypes.FIREWORK,at.add(direction.scale(z)).add(0,1,0),5,.5,.7,.5,.02,false);
        teleport(owner,target,at.add(direction.scale(distance)),target.getYRot(),target.getXRot(),wild);
    }
    private void teleport(ServerPlayer owner,Entity target,Vec3 destination,float yaw,float pitch,boolean wild){if(!valid(owner)||target.isRemoved()||!wildTarget(target)||!owner.level().hasChunkAt(BlockPos.containing(destination)))return;if(target.teleportTo(owner.level(),destination.x,destination.y,destination.z,Set.of(),yaw,pitch,true)&&target instanceof LivingEntity living)game.items().indirectKills().teleported(living,owner,wild);}
    private void smoke(ServerPlayer owner){
        var at=owner.position().add(0,1,0);for(var viewer:owner.level().players())if(viewer.distanceToSqr(owner)>=.0001)owner.level().sendParticles(viewer,ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,false,false,at.x,at.y,at.z,500,5,5,5,.1);
        owner.level().sendParticles(owner,ParticleTypes.SMOKE,false,false,at.x,at.y,at.z,10,.3,.3,.3,.05);
        clouds.add(new Cloud(owner,game.items().epoch()));updateSmoke(false);
    }
    void grenadeSmoke(ServerPlayer owner,Vec3 at,boolean old){
        if(!valid(owner))return;
        clouds.add(new Cloud(owner,game.items().epoch(),at,5,1,1200,old?2:1));updateSmoke(false);
    }
    private static MobEffectInstance smokeLayer(MobEffectInstance value,int amplifier){for(var node=value;node!=null;node=((EffectLayers.Access)node).skywars2$layerHidden())if(node.getAmplifier()==amplifier){var copy=new MobEffectInstance(node);((EffectLayers.Access)copy).skywars2$layerHidden(null);return copy;}return null;}
    /** Native addEffect observer retains unrelated same-amplifier layers while smoke owns its short layer. */
    public void externalBlindness(LivingEntity target,MobEffectInstance grant){if(applying||!grant.is(MobEffects.BLINDNESS)||!target.canBeAffected(grant))return;var state=blind.get(target);if(state==null||grant.getAmplifier()!=state.amplifier)return;if(state.baseline==null)state.baseline=smokeLayer(grant,state.amplifier);else EffectLayers.merge(state.baseline,grant);}
    public void externalBlindnessRemoved(LivingEntity target){if(applying)return;var state=blind.get(target);if(state!=null)state.baseline=null;}
    private void restoreBlindness(LivingEntity target,Blindness state){
        var retained=new ArrayList<MobEffectInstance>();for(var node=target.getEffect(MobEffects.BLINDNESS);node!=null;node=((EffectLayers.Access)node).skywars2$layerHidden())if(node.getAmplifier()!=state.amplifier){var copy=new MobEffectInstance(node);((EffectLayers.Access)copy).skywars2$layerHidden(null);retained.add(copy);}
        applying=true;try{target.removeEffect(MobEffects.BLINDNESS);for(var layer:retained)target.addEffect(layer);if(state.baseline!=null&&EffectLayers.live(state.baseline))target.addEffect(state.baseline);}finally{applying=false;}
    }
    private void reduce(LivingEntity target){var attribute=target.getAttribute(Attributes.FOLLOW_RANGE);if(attribute==null)return;attribute.removeModifier(RANGE);double additive=attribute.getBaseValue(),base=1,total=1;for(var modifier:attribute.getModifiers())switch(modifier.operation()){case ADD_VALUE->additive+=modifier.amount();case ADD_MULTIPLIED_BASE->base+=modifier.amount();case ADD_MULTIPLIED_TOTAL->total*=1+modifier.amount();}double factor=base*total;if(Double.isFinite(factor)&&Math.abs(factor)>1e-12)attribute.addTransientModifier(new AttributeModifier(RANGE,3/factor-additive,AttributeModifier.Operation.ADD_VALUE));reduced.add(target);if(target instanceof Mob mob&&mob.getTarget()!=null&&mob.distanceToSqr(mob.getTarget())>9){mob.setTarget(null);mob.getNavigation().stop();}}
    private void updateSmoke(boolean advance){
        var players=new IdentityHashMap<LivingEntity,Cloud>();var mobs=Collections.newSetFromMap(new IdentityHashMap<LivingEntity,Boolean>());
        for(var cloud:List.copyOf(clouds)){
            boolean validSource=cloud.visual==0?valid(cloud.owner):game.playing()&&game.stage()!=GameController.Stage.ENDING&&cloud.level==game.maps().level();
            if(!validSource||!cloud.epoch.equals(game.items().epoch())||game.server().getTickCount()>=cloud.expires){clouds.remove(cloud);continue;}
            if(cloud.visual==0?paused(cloud.owner):game.items().timeStop().stopped(cloud.level)){if(advance)cloud.expires++;continue;}
            for(var target:cloud.level.getEntitiesOfClass(LivingEntity.class,new AABB(cloud.center,cloud.center).inflate(cloud.radius),e->e.position().distanceToSqr(cloud.center)<=cloud.radius*cloud.radius&&wildTarget(e))){
                if(target instanceof ServerPlayer){var previous=players.get(target);if(previous==null||cloud.amplifier>previous.amplifier)players.put(target,cloud);}
                else if(target.getType().builtInRegistryHolder().is(MOBS)&&target.getAttribute(Attributes.FOLLOW_RANGE)!=null)mobs.add(target);
            }
            if(advance&&cloud.visual!=0){var at=cloud.center;
                if(cloud.visual==2)cloud.level.sendParticles(ParticleTypes.POOF,true,false,at.x,at.y+1,at.z,70,2,.5,2,.1);
                else cloud.level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK_MARKER,
                        net.minecraft.world.level.block.Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.GRAY).defaultBlockState()),true,false,at.x,at.y+2.5,at.z,100,7,4,7,0);
            }

        }
        for(var target:List.copyOf(blind.keySet()))if(!players.containsKey(target)){restoreBlindness(target,blind.remove(target));}
        for(var target:List.copyOf(reduced))if(!mobs.contains(target)){var range=target.getAttribute(Attributes.FOLLOW_RANGE);if(range!=null)range.removeModifier(RANGE);reduced.remove(target);}
        for(var target:players.keySet()){
            var cloud=players.get(target);var state=blind.get(target);
            if(state!=null&&state.amplifier!=cloud.amplifier){restoreBlindness(target,state);blind.remove(target);state=null;}
            int now=game.server().getTickCount();
            if(state==null){state=new Blindness(target.getEffect(MobEffects.BLINDNESS),now,cloud.amplifier);blind.put(target,state);}
            if(state.baseline!=null&&!state.baseline.isInfiniteDuration()&&now>state.lastTick){int remaining=state.baseline.getDuration()-(now-state.lastTick);state.baseline=remaining<=0?null:ItemEffectLevels.create(MobEffects.BLINDNESS,remaining,state.amplifier,state.baseline.isAmbient(),state.baseline.isVisible(),state.baseline.showIcon());}
            state.lastTick=now;applying=true;try{target.addEffect(ItemEffectLevels.create(MobEffects.BLINDNESS,2,state.amplifier,false,false),cloud.owner);}finally{applying=false;}
        }
        for(var mob:mobs)reduce(mob);
    }
    public boolean canTarget(Mob mob,LivingEntity target){return target==null||!reduced.contains(mob)||mob.distanceToSqr(target)<=9;}
    int cloudCount(){return clouds.size();}
    public void tick(){if(!game.server().tickRateManager().runsNormally()||game.items().timeStop().stopped(game.maps().level())){for(var state:blind.values())state.lastTick=game.server().getTickCount();for(var cloud:clouds)cloud.expires++;return;}updateSmoke(true);}
    public void loaded(Entity entity){if(entity instanceof LivingEntity target&&!reduced.contains(target)){var range=target.getAttribute(Attributes.FOLLOW_RANGE);if(range!=null)range.removeModifier(RANGE);}}
    public void unloaded(Entity entity){if(entity instanceof LivingEntity target){var state=blind.remove(target);if(state!=null)restoreBlindness(target,state);var range=target.getAttribute(Attributes.FOLLOW_RANGE);if(reduced.remove(target)&&range!=null)range.removeModifier(RANGE);}}
    public void clearEffects(ServerPlayer player){clouds.removeIf(c->c.owner==player);updateSmoke(false);blind.remove(player);var range=player.getAttribute(Attributes.FOLLOW_RANGE);if(reduced.remove(player)&&range!=null)range.removeModifier(RANGE);}
    public void forget(ServerPlayer owner){attacks.remove(owner);clouds.removeIf(c->c.owner==owner);updateSmoke(false);}
    public void stop(){clouds.clear();attacks.clear();for(var entry:List.copyOf(blind.entrySet()))restoreBlindness(entry.getKey(),entry.getValue());blind.clear();for(var mob:reduced){var range=mob.getAttribute(Attributes.FOLLOW_RANGE);if(range!=null)range.removeModifier(RANGE);}reduced.clear();producing=false;}
    LbcCloneSkills.ActorImage cloneImage(Entity source){
        if(!(source instanceof LivingEntity target))return null;var state=blind.get(target);boolean range=reduced.contains(target);
        if(state==null&&!range)return null;var baseline=state==null||state.baseline==null?null:EffectLayers.copy(state.baseline);
        int amp=state==null?0:state.amplifier,last=state==null?0:state.lastTick;
        return new LbcCloneSkills.ActorImage(){
            public void install(Entity original,Entity clone,Map<Entity,Entity> family){
                if(state!=null){var b=new Blindness(null,last,amp);b.baseline=baseline==null?null:EffectLayers.copy(baseline);blind.put((LivingEntity)clone,b);}
                if(range)reduced.add((LivingEntity)clone);
            }
            public void rollback(Entity clone){blind.remove(clone);reduced.remove(clone);}
        };
    }

}
