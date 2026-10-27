package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Complete source Epic/Fragment variants, accepted-target Ender, and shared status inputs. */
public final class LbcPendingSwords {
    private static final TagKey<DamageType> ARROW_ONLY=TagKey.create(Registries.DAMAGE_TYPE,Identifier.parse("minecraft:arrow_only"));
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final String ARROW="skywars2.epic_arrow";
    private final GameController game;
    private final List<Cast> casts=new ArrayList<>();
    private final Set<Arrow> arrows=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<ServerPlayer,Attack> attacks=new IdentityHashMap<>();
    private boolean producing;private int ticks;
    private static final String FANG="skywars2.epic_fang";
    private static final class Cast {final ServerPlayer owner;final String epoch;int due,stage=2;Cast(ServerPlayer p,String e){owner=p;epoch=e;due=p.level().getServer().getTickCount()+5;}}
    private static final class Attack {final Vec3 origin;int depth=1;final Set<LivingEntity> victims=Collections.newSetFromMap(new IdentityHashMap<>());Attack(ServerPlayer p){origin=p.position();}}
    public LbcPendingSwords(GameController game){this.game=game;}
    private boolean valid(ServerPlayer p){return p!=null&&p.isAlive()&&!p.isRemoved()&&p.level()==game.maps().level()&&game.canUseGameplayItems(p)&&!game.isObserver(p)&&game.stage()!=GameController.Stage.ENDING;}
    private boolean paused(ServerPlayer p){return game.items().wands().inputTimeStopped(p)||game.items().timeStop().stopped(p.level());}
    private boolean admitted(ServerPlayer p){return valid(p)&&game.items().wands().validRoleTarget(p,false)&&!game.items().actionsBlocked(p)&&!paused(p);}
    private boolean magic(ServerPlayer p,double radius){return !game.items().magic().beaconSuppressed(p.position())&&p.level().players().stream().noneMatch(v->!v.isSpectator()&&v.entityTags().contains("nomagic_active")&&v.distanceToSqr(p)<=radius*radius);}
    public void beginAttack(ServerPlayer owner){var current=attacks.get(owner);if(current!=null)current.depth++;else attacks.put(owner,new Attack(owner));}
    public void endAttack(ServerPlayer owner){var attack=attacks.get(owner);if(attack==null||--attack.depth>0)return;attacks.remove(owner);if(!attack.victims.isEmpty()&&valid(owner))ender(owner,attack.origin,attack.victims);}
    /** Called by the common accepted-positive-damage hook, including fatal and indirect events. */
    public void damaged(LivingEntity victim,DamageSource source){
        if(producing||source.getDirectEntity()!=null&&source.getDirectEntity().entityTags().contains(FANG)||!(source.getEntity() instanceof ServerPlayer owner)||!admitted(owner)||source.is(ARROW_ONLY)||!game.items().wands().projectileTarget(owner,victim,true))return;
        var dim=owner.level().dimension().identifier();if(dim.equals(Identifier.parse("minecraft:nexus"))||dim.equals(Identifier.parse("minecraft:imprinted")))return;
        var data=LbcFartus.data(owner.getMainHandItem());
        if(data.getIntOr("ender",0)==1&&magic(owner,2)){var attack=attacks.get(owner);if(attack!=null)attack.victims.add(victim);else ender(owner,owner.position(),List.of(victim));}
        if(data.getIntOr("antimagic",0)==1&&victim instanceof ServerPlayer target)game.items().mana().applyNoMagic(target,15);
        if(data.getIntOr("holocaust",0)==1&&victim.getType().builtInRegistryHolder().is(MOBS))victim.addEffect(ItemEffectLevels.create(MobEffects.RAID_OMEN,400,111),owner);
        boolean old=data.getIntOr("cool_old",0)==1||data.getIntOr("fragmentsword_old",0)==1;
        boolean random=data.getIntOr(old?"cool_old":"cool",0)==1;
        if(!old&&data.getIntOr("cool",0)!=1&&data.getIntOr("fragmentsword",0)!=1)return;
        if(!old&&!magic(owner,20))return;
        int type=random?owner.getRandom().nextInt(17)+(old?0:1):data.getIntOr("cooltype",-1);
        if(random)type=old?type:type==17?0:type;
        if(type<0||type>16)return;
        producing=true;try{branch(owner,type,old,random);}finally{producing=false;}
    }
    private void ender(ServerPlayer owner,Vec3 origin,Collection<LivingEntity> victims){
        sound(owner,origin,"entity.enderman.teleport",10,1,10);
        particles(owner,ParticleTypes.PORTAL,origin.add(0,0,1),100,.2,.5,.2,.5,true,null);
        for(var victim:victims){sound(owner,victim.position(),"entity.enderman.teleport",10,1,10);particles(owner,ParticleTypes.PORTAL,victim.position().add(0,0,1),100,.2,.5,.2,.5,true,null);}
        for(var victim:victims)if(!victim.isRemoved()){teleport(owner,victim,origin,10,true);particles(owner,ParticleTypes.REVERSE_PORTAL,victim.position().add(0,0,1),100,.2,.5,.2,.5,true,null);}
        teleport(owner,owner,origin,10,true);
        particles(owner,ParticleTypes.REVERSE_PORTAL,origin.add(0,0,1),100,.2,.5,.2,.5,true,null);
    }
    private void teleport(ServerPlayer owner,Entity victim,Vec3 center,int radius,boolean netherCeiling){
        int ceiling=netherCeiling&&owner.level().dimension()==Level.NETHER?120:owner.level().getMaxY()+1;
        var destination=LbcSurfaceTeleport.destination(owner.level(),center,radius,ceiling,owner.getRandom());
        if(destination==null)return;
        if(victim.teleportTo(owner.level(),destination.x,destination.y,destination.z,Set.of(),victim.getYRot(),victim.getXRot(),true)&&victim instanceof LivingEntity living)game.items().indirectKills().teleported(living,owner,false);
    }
    private List<LivingEntity> targets(ServerPlayer p,double minimum,double radius,boolean players,boolean mobs){
        java.util.function.Predicate<LivingEntity> admitted=e->e.distanceToSqr(p)>=minimum*minimum&&e.distanceToSqr(p)<=radius*radius&&(!mobs||e.getType().builtInRegistryHolder().is(MOBS))&&game.items().wands().projectileTarget(p,e);
        // Source @a uses ServerLevel's player list, independent of ticking entity sections.
        if(players)return p.level().players().stream().filter(admitted).map(e->(LivingEntity)e).toList();
        return p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(radius),admitted);
    }
    private List<Entity> teleportTargets(ServerPlayer p,double minimum,double radius){return p.level().getEntities((Entity)null,p.getBoundingBox().inflate(radius),e->e.distanceToSqr(p)>=minimum*minimum&&e.distanceToSqr(p)<=radius*radius&&game.items().wands().projectileTarget(p,e));}
    private void effect(ServerPlayer owner,LivingEntity target,Holder<MobEffect> type,int ticks,int amp,boolean hidden){target.addEffect(ItemEffectLevels.create(type,ticks,amp,false,!hidden),owner);}
    private void self(ServerPlayer p,Holder<MobEffect> type,int ticks,int amp,boolean hidden){effect(p,p,type,ticks,amp,hidden);}
    private void message(Entity target,String key,int color){if(target instanceof ServerPlayer p)p.sendSystemMessage(Component.translatable(key).withColor(color));}
    private void sound(ServerPlayer p,Vec3 at,String id,float volume,float pitch,double radius){LbcOverload.sound(p,at,"minecraft:"+id,volume,pitch,radius);}
    private void sound(ServerPlayer p,String id,float pitch,boolean old){sound(p,p.position(),id,old?10:1,pitch,old?20:0);}
    private void particles(ServerPlayer owner,ParticleOptions type,Vec3 at,int count,double x,double y,double z,double speed,boolean force,ServerPlayer recipient){
        if(recipient!=null){recipient.connection.send(new ClientboundLevelParticlesPacket(type,force,false,at.x,at.y,at.z,(float)x,(float)y,(float)z,(float)speed,count));return;}
        owner.level().sendParticles(type,force,false,at.x,at.y,at.z,count,x,y,z,speed);
    }
    private void particle(ServerPlayer p,ParticleOptions type,double dy,int count,double x,double y,double z,double speed,boolean force){particles(p,type,p.position().add(0,dy,0),count,x,y,z,speed,force,null);}
    private static final int[] OLD_TO_CURRENT={11,7,5,13,3,9,1,2,4,6,14,12,10,16,15,0,8};
    private static final int[] OLD_RANDOM_TO_CURRENT={11,7,13,3,9,1,2,4,6,14,12,10,16,15,0,8,5};
    private void branch(ServerPlayer p,int sourceType,boolean old,boolean random){
        int type=old?(random?OLD_RANDOM_TO_CURRENT[sourceType]:OLD_TO_CURRENT[sourceType]):sourceType;
        switch(type){
            case 0->{self(p,MobEffects.ABSORPTION,600,1,false);particle(p,ParticleTypes.FALLING_HONEY,1,150,.3,1,.3,0,old);sound(p,"entity.player.burp",1,old);}
            case 1->{for(int[] axis:new int[][]{{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}}){var arrow=new EpicArrow(p.level());arrow.setOwner(p);arrow.pickup=AbstractArrow.Pickup.DISALLOWED;arrow.setPos(p.position().add(axis[0],1.62,axis[1]));arrow.setDeltaMovement(axis[0]*8,0,axis[1]*8);((me.skywars2.mixin.WandProjectileMixin.ArrowAccess)(Object)arrow).skywars2$setPierceLevel((byte)10);arrow.entityTags().addAll(List.of(ARROW,"killedarrow"));game.items().markCurrentItemEpoch(arrow);LbcShooters.orient(arrow);if(p.level().addFreshEntity(arrow))arrows.add(arrow);}particle(p,ParticleTypes.END_ROD,1,40,.3,1,.3,.1,old);sound(p,"entity.skeleton.shoot",1,old);}
            case 2->{self(p,MobEffects.JUMP_BOOST,200,2,false);for(var t:targets(p,old?.1:.01,old?4:6,false,false)){effect(p,t,MobEffects.LEVITATION,20,7,false);if(!old)message(t,"player_pumped_up",0x55ff55);}particle(p,ParticleTypes.SNEEZE,.1,140,3,.1,3,0,old);sound(p,"entity.iron_golem.damage",0,old);}
            case 3->{fang(p,1);casts.add(new Cast(p,game.items().epoch()));particle(p,ParticleTypes.WITCH,2,35,.3,0,.3,0,true);sound(p,"entity.evoker.cast_spell",2,old);}
            case 4->{self(p,MobEffects.RESISTANCE,20,4,true);if(!old)self(p,MobEffects.SLOW_FALLING,80,0,true);Vec3 at=old?p.position().add(p.getLookAngle().scale(-.5)):p.position();var cause=game.items().indirectKills().coldCause(p,false);var damage=game.items().indirectKills().captureDamage(p.damageSources().explosion(p,p),cause);game.items().indirectKills().trapChanges(p.level(),cause,()->p.level().explode(null,damage,null,at,old?1:2,false,Level.ExplosionInteraction.MOB));sound(p,"block.fire.extinguish",1,old);}
            case 5->{self(p,MobEffects.FIRE_RESISTANCE,old?320:500,0,false);if(!old)self(p,MobEffects.FIRE_RESISTANCE,160,1,false);particle(p,ParticleTypes.FLAME,1,45,.3,1,.3,0,old);sound(p,"entity.blaze.shoot",1,old);}
            case 6->{self(p,MobEffects.GLOWING,old?60:100,1,false);for(var t:targets(p,old?.1:.01,old?4:6,!old,false)){effect(p,t,MobEffects.BLINDNESS,old?60:100,1,false);if(!old)message(t,"player_blinded",0xaaaaaa);}if(old)particle(p,ParticleTypes.SMOKE,1,200,3,.3,3,0,true);else for(var t:targets(p,.01,75,true,false))particles(p,new BlockParticleOption(ParticleTypes.BLOCK_MARKER,Blocks.RED_MUSHROOM_BLOCK.defaultBlockState()),p.position().add(0,1,0),600,6,3,6,0,true,(ServerPlayer)t);sound(p,"entity.illusioner.prepare_blindness",2,old);}
            case 7->{self(p,old?MobEffects.REGENERATION:MobEffects.INSTANT_HEALTH,old?20:1,old?99:1,false);particle(p,ParticleTypes.HEART,1,25,.3,1,.3,0,old);sound(p,"entity.player.levelup",1.5f,old);}
            case 8->{for(var t:targets(p,old?.5:.01,10,false,!old)){if(old)effect(p,t,MobEffects.INSTANT_DAMAGE,1,0,false);else{var damage=new DamageSource(p.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.ARROW),p,p);t.hurtServer(p.level(),damage,8);message(t,"player_crippled",0xff5555);}}particle(p,ParticleTypes.DAMAGE_INDICATOR,2,400,10,5,10,0,old);sound(p,"entity.player.death",1,old);}
            case 9->{self(p,MobEffects.INVISIBILITY,400,1,true);if(old)particle(p,ParticleTypes.CAMPFIRE_COSY_SMOKE,1,70,.3,1,.3,.4,true);else particle(p,new BlockParticleOption(ParticleTypes.BLOCK_MARKER,Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE).defaultBlockState()),1,50,6,3,6,0,true);sound(p,"entity.evoker.prepare_wololo",1,old);}
            case 10->{var origin=p.position();for(var t:teleportTargets(p,old?.1:.01,old?5:6)){teleport(p,t,origin,20,!old);sound(p,old?origin:t.position(),"entity.enderman.teleport",1,0,old?15:0);particles(p,ParticleTypes.PORTAL,t.position().add(0,1,0),150,.3,1,.3,2,false,null);if(!old)message(t,"player_teleported",0xff55ff);}}
            case 11->{self(p,MobEffects.RESISTANCE,100,1,false);particle(p,new BlockParticleOption(ParticleTypes.BLOCK,Blocks.IRON_BLOCK.defaultBlockState()),1,45,.3,1,.3,0,old);sound(p,"block.anvil.break",1,old);}
            case 12->{if(!old)sound(p,p.position(),"entity.ghast.hurt",1,1,0);for(var t:targets(p,.1,15,true,false)){var target=(ServerPlayer)t;target.connection.send(new ClientboundSoundPacket(Holder.direct(SoundEvent.createVariableRangeEvent(Identifier.parse("minecraft:entity.ghast.hurt"))),SoundSource.MASTER,p.getX(),p.getY(),p.getZ(),old?1000000:10,0,p.getRandom().nextLong()));particles(p,ParticleTypes.ELDER_GUARDIAN,old?t.position():p.position(),old?1:10,0,0,0,1,true,target);}}
            case 13->{self(p,MobEffects.SPEED,40,2,false);particle(p,ColorParticleOption.create(ParticleTypes.FLASH,old?0xffffffff:0xff00ffff),1,100,10,10,10,0,old);sound(p,"entity.firework_rocket.large_blast",1,old);}
            case 14->{self(p,MobEffects.STRENGTH,100,0,false);particle(p,ParticleTypes.SWEEP_ATTACK,1,40,.3,1,.3,0,old);sound(p,"entity.ender_dragon.growl",2,old);}
            case 15->{self(p,MobEffects.CONDUIT_POWER,old?(random?60:80):200,old&&random?2:1,false);sound(p,"entity.dolphin.attack",0,old);}
            case 16->{for(var t:targets(p,old?.1:.01,old?5:7,false,false)){effect(p,t,MobEffects.WEAKNESS,120,0,false);if(!old)message(t,"player_weak",0xaaaaaa);}particle(p,ParticleTypes.FALLING_NECTAR,2,350,4,.1,4,.1,old);sound(p,"entity.elder_guardian.curse",1,old);}
            default->throw new IllegalArgumentException("Epic branch "+type);
        }
    }
    private static final class EpicArrow extends Arrow {
        EpicArrow(net.minecraft.server.level.ServerLevel level){super(EntityTypes.ARROW,level);}
        boolean grounded(){return isInGround();}
        @Override public boolean shouldBeSaved(){return false;}
    }
    private void fang(ServerPlayer owner,int stage){var forward=owner.getLookAngle();var up=Vec3.directionFromRotation(owner.getXRot()-90,owner.getYRot());var fang=game.items().evokerBass().spawnOwned(owner,owner.position().add(up.scale(.5)).add(forward.scale(stage*1.5)));if(fang!=null)fang.entityTags().add(FANG);}
    int queuedCasts(){return casts.size();}
    List<Arrow> arrowSnapshot(){return List.copyOf(arrows);}
    private void antimagicTask(ServerPlayer owner){
        if(!valid(owner)||paused(owner)||LbcFartus.data(owner.getMainHandItem()).getIntOr("antimagic",0)!=1)return;
        var stand=owner.level().getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class,owner.getBoundingBox().inflate(4),e->e.entityTags().contains("mom")&&e.distanceToSqr(owner)<=16).stream().findFirst().orElse(null);
        if(stand==null)return;
        var board=game.server().getScoreboard();var objective=board.getObjective("lbcID2");
        var theirs=objective==null?null:board.getPlayerScoreInfo(stand,objective);var ours=objective==null?null:board.getPlayerScoreInfo(owner,objective);
        if(theirs==null||ours==null||theirs.value()!=ours.value())game.items().mana().award(owner,"lbc:true_advancements/swords/antimagic");
    }
    public void tick(){
        if(++ticks%20==0)for(var owner:game.server().getPlayerList().getPlayers())antimagicTask(owner);
        for(var cast:List.copyOf(casts)){if(!valid(cast.owner)||!cast.epoch.equals(game.items().epoch())){casts.remove(cast);continue;}if(paused(cast.owner)){cast.due++;continue;}if(game.server().getTickCount()>=cast.due){fang(cast.owner,cast.stage++);cast.due+=5;if(cast.stage>3)casts.remove(cast);}}
        for(var arrow:List.copyOf(arrows))if(arrow.isRemoved()||!game.items().currentEpoch(arrow)||!(arrow.getOwner() instanceof ServerPlayer owner)||!valid(owner)||arrow instanceof EpicArrow epic&&epic.grounded()||arrow.tickCount>=1200)unloaded(arrow);
    }
    public void loaded(Entity entity){if(entity instanceof Arrow arrow&&entity.entityTags().contains(ARROW)){if(!(arrow instanceof EpicArrow)||!game.items().currentEpoch(arrow)||!(arrow.getOwner() instanceof ServerPlayer owner)||!valid(owner))arrow.discard();else arrows.add(arrow);}}
    public void unloaded(Entity entity){if(entity instanceof Arrow arrow&&arrows.remove(arrow)&&!arrow.isRemoved())arrow.discard();}
    public void forget(ServerPlayer owner){casts.removeIf(c->c.owner==owner);attacks.remove(owner);for(var arrow:List.copyOf(arrows))if(arrow.getOwner()==owner)unloaded(arrow);}
    public void stop(){casts.clear();attacks.clear();for(var arrow:List.copyOf(arrows))unloaded(arrow);producing=false;ticks=0;}
}
