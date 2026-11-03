package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Fevgalea's three detached helpers and their full particle-projectile closure. */
final class SauvojenStars {
    static final String TAG="skywars2.sauvojen.fevgalea";
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final TagKey<net.minecraft.world.entity.EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final DustParticleOptions[] DUST={new DustParticleOptions(rgb(.945f,.494f,.071f),1),new DustParticleOptions(rgb(.714f,.945f,.071f),1),new DustParticleOptions(rgb(.071f,.698f,.945f),1)};
    private static final DustParticleOptions DROP_DUST=new DustParticleOptions(rgb(.796f,1,.984f),1);
    private static final double[][] TRAIL={{0,0},{.2,-.07},{.4,-.15},{.6,-.2},{.75,-.25},{-.2,-.07},{-.4,-.15},{-.6,-.2},{-.75,-.25}};
    private static int rgb(float r,float g,float b){return net.minecraft.util.ARGB.colorFromFloat(1,r,g,b)&0xffffff;}
    enum Kind { CHARGE,RAIN,DROP }
    private static final class Actor {
        final ServerPlayer owner;
        final InteractionHand hand;
        final ItemStack stack;
        final String epoch;
        final Marker carrier;
        final Kind kind;
        final int flavour,lifetime;
        Vec3 direction,left;
        final LbcIndirectKills.Owner cause;
        int age,cloneMask;
        Actor(ServerPlayer owner,InteractionHand hand,ItemStack stack,String epoch,Marker carrier,Kind kind,int flavour,Vec3 direction,LbcIndirectKills.Owner cause){
            this.owner=owner;this.hand=hand;this.stack=stack.copy();this.epoch=epoch;this.carrier=carrier;this.kind=kind;this.flavour=flavour;
            this.direction=direction.normalize();left=left(this.direction);this.cause=cause;
            lifetime=kind==Kind.CHARGE?40:kind==Kind.RAIN?100:25;
        }
    }
    record View(ServerPlayer owner,String epoch,Kind kind,int flavour,Vec3 position,Vec3 direction,int age,int lifetime,Marker carrier){}
    private final GameController game;
    private final Map<Marker,Actor> actors=new LinkedHashMap<>();
    SauvojenStars(GameController game){this.game=game;}
    int count(){return actors.size();}
    List<View> snapshot(){return actors.values().stream().map(a->new View(a.owner,a.epoch,a.kind,a.flavour,a.carrier.position(),a.direction,a.age,a.lifetime,a.carrier)).toList();}
    /** Modes are the Academy terra row numbers, including the three identical random hit_shoot rows. */
    void castAt(ServerPlayer owner,InteractionHand hand,ItemStack stack,int mode,Vec3 origin,Vec3 direction){
        if(!validOwner(owner)||!resident(owner.level(),origin)||game.items().magic().suppressedAt(owner.level(),origin))return;
        Vec3 aim=direction.normalize();
        if(mode==5)beam(owner,origin,aim);
        else if(mode==6){
            // anchored eyes on a source marker has eye height zero; ^15 is LOCAL up, not world Y.
            Vec3 up=aim.cross(left(aim));
            spawn(owner,hand,stack,Kind.RAIN,0,origin.add(up.scale(15)),aim);
        }else if(mode>=7&&mode<=9)spawn(owner,hand,stack,Kind.CHARGE,owner.getRandom().nextInt(3)+1,origin,aim);
        else throw new IllegalArgumentException("Fevgalea helper mode "+mode);
    }
    private void spawn(ServerPlayer owner,InteractionHand hand,ItemStack stack,Kind kind,int flavour,Vec3 at,Vec3 direction){
        if(!resident(owner.level(),at)||game.items().magic().suppressedAt(owner.level(),at))return;
        var marker=new Marker(EntityTypes.MARKER,owner.level());marker.setPos(at);
        marker.entityTags().addAll(List.of(TAG,"magic","player","slowraycast","shield_blockable",game.items().epoch()));
        marker.entityTags().add(kind==Kind.CHARGE?"star_charge":kind==Kind.RAIN?"star_rain":"star_raindrop");
        if(flavour>0)marker.entityTags().add("star_"+flavour);
        marker.setYRot((float)Math.toDegrees(Math.atan2(-direction.x,direction.z)));
        marker.setXRot((float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance())));
        var actor=new Actor(owner,hand,stack,game.items().epoch(),marker,kind,flavour,direction,game.items().indirectKills().coldCause(owner,true));
        actors.put(marker,actor);game.items().timeStop().prepareFrozen(marker);
        if(!owner.level().addFreshEntity(marker))remove(actor);
    }
    private static Vec3 left(Vec3 aim){double yaw=Math.atan2(-aim.x,aim.z);return new Vec3(Math.cos(yaw),0,Math.sin(yaw));}
    private static boolean resident(ServerLevel level,Vec3 at){return level.hasChunkAt(BlockPos.containing(at));}
    private boolean validOwner(ServerPlayer owner){return owner.isAlive()&&!owner.isRemoved()&&owner.level()==game.maps().level()&&game.canUseGameplayItems(owner);}
    private boolean target(ServerPlayer owner,LivingEntity target){return target.getType().builtInRegistryHolder().is(MOBS)
            &&game.items().wands().projectileTarget(owner,target)&&!game.items().magic().suppressedAt(owner.level(),target.position());}
    /** Missing Health on the source item_display stores result zero, as established by native command callbacks. */
    private void beam(ServerPlayer owner,Vec3 origin,Vec3 direction){beam(owner,origin,direction,false);}
    /** The standalone sword executes as its player; the Academy display deliberately retains zero Health. */
    void playerBeam(ServerPlayer owner,Vec3 origin,Vec3 direction){
        if(validOwner(owner)&&resident(owner.level(),origin)&&!game.items().magic().suppressedAt(owner.level(),origin))
            beam(owner,origin,direction,true);
    }
    private void beam(ServerPlayer owner,Vec3 origin,Vec3 direction,boolean playerHealth){
        var level=owner.level();Vec3 previous=origin;
        var focus=playerHealth?LbcFevgaleaStar.focus(owner):null;
        var cause=playerHealth?game.items().indirectKills().coldCause(owner,true):null;
        for(int sample=0;sample<=120;sample++){
            Vec3 at=origin.add(direction.scale(sample));
            if(!resident(level,at)||game.items().magic().suppressedAt(level,at))return;
            if(sample>0&&game.items().wands().shieldHit(owner,previous,at)!=null)return;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,true,false,at.x,at.y,at.z,10,1.5,1.5,1.5,2);
            if(owner.getRandom().nextFloat()<.15f)LbcOverload.sound(owner,at,"minecraft:block.amethyst_cluster.fall",1,2,0);
            // Source stores caster Health once per contact sample, then uses that same integer for all its targets.
            if(playerHealth){
                float amount=Math.max(0,Mth.floor(owner.getHealth()));
                if(amount>0)for(var victim:level.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(4),e->target(owner,e)))
                    if(victim.position().distanceToSqr(at)<=16){
                        var source=new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                                .getOrThrow(DamageTypes.OUTSIDE_BORDER),focus,owner);
                        victim.hurtServer(level,game.items().indirectKills().captureDamage(source,cause),amount);
                    }
            }
            if(!level.getBlockState(BlockPos.containing(at)).is(AIRS))return;
            previous=at;
        }
    }
    void tick(){
        for(var actor:List.copyOf(actors.values())){
            var owner=actor.owner;var marker=actor.carrier;var level=owner.level();
            if(marker.isRemoved()||!validOwner(owner)||marker.level()!=level||!game.items().currentEpoch(actor.epoch)
                    ||!resident(level,marker.position())||game.items().magic().suppressedAt(level,marker.position())){remove(actor);continue;}
            if(game.items().timeStop().stopped(level)||owner.entityTags().contains("time_stopped")||game.items().timeStop().frozen(marker))continue;
            if(actor.kind==Kind.RAIN)rain(actor);else flight(actor);
            if(!actors.containsKey(marker))continue;
            if(++actor.age>=actor.lifetime)remove(actor);
        }
    }
    private void rain(Actor actor){
        Vec3 at=actor.carrier.position();
        if(actor.owner.getRandom().nextBoolean()){
            float yaw=(actor.owner.getRandom().nextInt(360001)-180000)*.001f;
            float pitch=(70000+actor.owner.getRandom().nextInt(20001))*.001f;
            spawn(actor.owner,actor.hand,actor.stack,Kind.DROP,0,at,Vec3.directionFromRotation(pitch,yaw));
        }
        Vec3 next=at.add(actor.direction);
        if(!resident(actor.owner.level(),next)||game.items().magic().suppressedAt(actor.owner.level(),next)
                ||game.items().wands().shieldHit(actor.owner,at,next)!=null){remove(actor);return;}
        // Source rain has no block terminal; only its falling children collide with terrain.
        actor.carrier.setPos(next);
    }
    private void flight(Actor actor){
        Vec3 origin=actor.carrier.position(),previous=origin;
        int samples=actor.kind==Kind.CHARGE?10:11;
        double spacing=actor.kind==Kind.CHARGE?.2:.33;
        for(int sample=0;sample<samples;sample++){
            Vec3 at=origin.add(actor.direction.scale(sample*spacing));
            var level=actor.owner.level();
            if(!resident(level,at)||game.items().magic().suppressedAt(level,at)
                    ||sample>0&&game.items().wands().shieldHit(actor.owner,previous,at)!=null){remove(actor);return;}
            if(actor.kind==Kind.CHARGE)chargeSample(actor,at);else dropSample(actor,at);
            if(!level.getBlockState(BlockPos.containing(at)).is(AIRS)){remove(actor);return;}
            previous=at;
        }
        actor.carrier.setPos(previous);
    }
    private void chargeSample(Actor actor,Vec3 at){
        for(double lateral:new double[]{0,.6,-.6}){
            Vec3 point=at.add(actor.left.scale(lateral)).add(actor.direction.scale(lateral==0?0:-.25));
            int hits=0;
            for(var target:actor.owner.level().getEntitiesOfClass(LivingEntity.class,new AABB(point,point.add(1.3,1,1.3)),e->target(actor.owner,e))){
                if(hits++>=3)break;
                if(game.items().mana().hudValue(target,"void_resistance")>0)continue;
                hurt(actor,target,chargeDamage(target,actor.flavour),DamageTypes.FELL_OUT_OF_WORLD);
            }
        }
        for(var point:TRAIL){Vec3 p=at.add(actor.left.scale(point[0])).add(actor.direction.scale(point[1]));
            actor.owner.level().sendParticles(DUST[actor.flavour-1],p.x,p.y,p.z,1,0,0,0,0);}
    }
    static float chargeDamage(LivingEntity target,int flavour){
        return switch(flavour){
            case 1 -> Mth.floor(target.getAttributeValue(Attributes.MAX_HEALTH))*.15f;
            case 2 -> target.getAttribute(Attributes.ATTACK_DAMAGE)==null?0:Mth.floor(target.getAttributeValue(Attributes.ATTACK_DAMAGE));
            case 3 -> Mth.floor(target.getAttributeValue(Attributes.MOVEMENT_SPEED)*50);
            default -> throw new IllegalArgumentException("Fevgalea flavour "+flavour);
        };
    }
    private void dropSample(Actor actor,Vec3 at){
        var level=actor.owner.level();
        level.sendParticles(ParticleTypes.END_ROD,at.x,at.y,at.z,1,0,0,0,.25);
        level.sendParticles(DROP_DUST,true,false,at.x,at.y,at.z,1,0,0,0,0);
        if(actor.owner.getRandom().nextFloat()<.05f)LbcOverload.sound(actor.owner,at,"minecraft:entity.firework_rocket.launch",.2f,2,0);
        int hits=0;
        for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(at,at.add(1,1,1)),e->target(actor.owner,e))){
            if(hits++>=2)break;
            // The imported arrow damage tag bypasses hurt cooldown natively; no manual iframe reset is needed.
            hurt(actor,target,3,DamageTypes.ARROW);
        }
    }
    private void hurt(Actor actor,LivingEntity target,float amount,net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> type){
        if(amount<=0)return;
        var damage=new DamageSource(actor.owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type),actor.carrier,actor.owner);
        target.hurtServer(actor.owner.level(),game.items().indirectKills().captureDamage(damage,actor.cause),amount);
    }
    void transformOrigins(ServerLevel level,java.util.function.UnaryOperator<Vec3> transform){
        for(var actor:List.copyOf(actors.values()))if(actor.carrier.level()==level){
            Vec3 next=transform.apply(actor.carrier.position());if(next==null)remove(actor);else actor.carrier.setPos(next);
        }
    }
    void loaded(Entity entity){if(entity.entityTags().contains(TAG)&&(!(entity instanceof Marker marker)||!actors.containsKey(marker)))entity.discard();}
    void unloaded(Entity entity){if(entity instanceof Marker marker)actors.remove(marker);}
    void forget(ServerPlayer owner){for(var actor:List.copyOf(actors.values()))if(actor.owner==owner)remove(actor);}
    void stop(){for(var actor:List.copyOf(actors.values()))remove(actor);}
    private void remove(Actor actor){if(actors.remove(actor.carrier)!=null&&!actor.carrier.isRemoved())actor.carrier.discard();}

    int cloneNearby(ServerPlayer holder,int skill,int budget,long deadline) {
        int made=0,count=CloneVirtualAbilityStates.count(skill),bit=CloneVirtualAbilityStates.bit(skill);
        for(var original:List.copyOf(actors.values())){
            if(budget-made<count||System.nanoTime()>=deadline)break;
            var level=(ServerLevel)original.carrier.level();
            if(original.carrier.isRemoved()||original.age>=original.lifetime||!game.items().currentEpoch(original.epoch)
                    ||LbcAcademy.trial(original.stack)||game.items().timeStop().frozen(original.carrier)
                    ||original.owner.entityTags().contains("time_stopped")
                    ||!CloneVirtualAbilityStates.near(game,holder,level,original.carrier.position(),original.cloneMask,skill))continue;
            var points=CloneVirtualAbilityStates.positions(holder,level,original.carrier.position(),skill);if(points==null)continue;
            var created=new java.util.ArrayList<Actor>();
            try{
                for(var point:points){var carrier=new Marker(EntityTypes.MARKER,level);carrier.setPos(point);
                    carrier.entityTags().addAll(original.carrier.entityTags());game.items().markCurrentItemEpoch(carrier);
                    carrier.setYRot(original.carrier.getYRot());carrier.setXRot(original.carrier.getXRot());
                    var direction=skill==15?original.direction:LbcCloneSkills.spreadMotion(original.direction,holder,1000,1500);
                    var copy=new Actor(original.owner,original.hand,original.stack,original.epoch,carrier,original.kind,original.flavour,direction,original.cause);
                    copy.direction=direction;copy.left=left(direction);copy.age=original.age;copy.cloneMask=original.cloneMask|bit;created.add(copy);actors.put(carrier,copy);
                    game.items().spellSources().inherit(original.carrier,carrier);game.items().timeStop().prepareFrozen(carrier);
                    if(!level.addFreshEntity(carrier)||carrier.isRemoved()||!actors.containsKey(carrier))throw new IllegalStateException("Star clone rejected");
                }
                original.cloneMask|=bit;made+=count;
                if(skill==17){original.direction=LbcCloneSkills.spreadMotion(original.direction,holder,10,10);original.left=left(original.direction);}
            }catch(RuntimeException failure){for(var copy:created){game.items().spellSources().reflected(copy.carrier);remove(copy);}throw failure;}
        }
        return made;
    }


    void planetoid(ServerPlayer holder) {
        for(var actor:actors.values())if(!actor.carrier.isRemoved()&&game.items().currentEpoch(actor.epoch)
                &&!game.items().timeStop().frozen(actor.carrier)&&!actor.carrier.entityTags().contains("time_stopped")
                &&LbcPlanetoidSkill.near(game,holder,(ServerLevel)actor.carrier.level(),actor.carrier.position())){
            actor.direction=LbcPlanetoidSkill.direction(holder,actor.carrier.position(),82);
            actor.left=left(actor.direction);LbcPlanetoidSkill.orient(actor.carrier,actor.direction);
        }
    }

}
