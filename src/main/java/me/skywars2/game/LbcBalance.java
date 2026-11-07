package me.skywars2.game;

import java.util.*;
import java.util.function.*;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Balance is a controller-owned mode overlay. This service owns only its old equipment clocks. */
public final class LbcBalance {
    public interface WoolPolicy {
        default boolean allowsLaunch(ServerPlayer owner,InteractionHand hand){return true;}
        default boolean allowsField(ServerLevel level,Vec3 position){return true;}
    }
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private final GameController game;
    private final Consumer<ServerPlayer> activate;
    private final BooleanSupplier legacy;
    private final Map<UUID,int[]> cooldowns=new LinkedHashMap<>();
    private final LbcBalancePassive passive;
    private final LbcBalanceRays rays;
    private final LbcBalanceExtras extras;
    private final LbcBalanceExplosiveBow explosive;
    private final LbcBalanceEggActors eggs;
    private final Map<UUID,Integer> clocks=new LinkedHashMap<>();
    public LbcBalance(GameController game,Consumer<ServerPlayer> activate,BooleanSupplier legacy){
        this.game=game;this.activate=activate;this.legacy=legacy;passive=new LbcBalancePassive(game);rays=new LbcBalanceRays(game,this::admitted);extras=new LbcBalanceExtras(game);explosive=new LbcBalanceExplosiveBow(game);eggs=new LbcBalanceEggActors(game);
    }
    boolean admitted(ServerPlayer p){return game.playing()&&game.canUseGameplayItems(p)&&p.isAlive()&&!p.isRemoved()
        &&!game.isObserver(p)&&!p.isSpectator()&&!game.items().actionsBlocked(p)
        &&!game.items().wands().inputTimeStopped(p)&&!game.items().timeStop().stopped(p.level())
        &&game.items().wands().validRoleTarget(p,false)&&!p.entityTags().contains("nomagic_active")
        &&!game.items().magic().artifactSuppressedAt(p.level(),p.position())
        &&p.level().players().stream().noneMatch(other->!other.isSpectator()&&other.entityTags().contains("nomagic_active")&&other.distanceToSqr(p)<=400);}
    public boolean use(ServerPlayer p,InteractionHand hand){
        if(hand!=InteractionHand.MAIN_HAND||!admitted(p))return false;
        var stack=p.getMainHandItem();
        if(LbcBalanceItems.isBalance(stack)){activate.accept(p);return true;}
        var wand=LbcBalanceItems.wand(stack);
        if(wand==null)return rays.use(p,stack);
        var scores=cooldowns.computeIfAbsent(p.getUUID(),ignored->new int[8]);
        if(scores[wand.ordinal()]>=wand.threshold)return false;
        cast(p,wand);scores[wand.ordinal()]=1000;
        game.items().wands().wandCasted(p);return true;
    }
    public void inventoryChanged(ServerPlayer p){passive.changed(p);}
    public void loaded(Entity body){extras.loaded(body);explosive.loaded(body);rays.loaded(body);eggs.loaded(body);}
    public void unloaded(Entity body){extras.unloaded(body);explosive.unloaded(body);rays.unloaded(body);eggs.unloaded(body);}
    public void adoptEgg(Entity body,LivingEntity user){eggs.adopt(body,user);}
    public static boolean isDrop(ItemStack stack){return LbcBalanceExtras.isDrop(stack);}
    public boolean drop(net.minecraft.world.entity.item.ItemEntity token){return extras.drop(token);}
    public boolean canDrop(net.minecraft.world.entity.item.ItemEntity token){return extras.canDrop(token);}
    public void damaged(ServerPlayer owner){extras.damaged(owner);}
    public void woolPolicy(WoolPolicy policy){extras.woolPolicy(policy);}
    public boolean woolAllowed(ServerPlayer owner,InteractionHand hand){return extras.woolAllowed(owner,hand);}
    public List<net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball> trailActors(){return extras.trailActors();}
    public net.minecraft.world.item.DyeColor trailColor(Entity actor){return extras.trailColor(actor);}
    public LbcIndirectKills.Owner trailCause(Entity actor){return extras.trailCause(actor);}
    public boolean explosiveBow(ServerPlayer owner,ItemStack weapon,List<net.minecraft.world.entity.projectile.Projectile> family){return explosive.convert(owner,weapon,family);}
    public boolean beforeTnt(net.minecraft.world.entity.item.PrimedTnt body){return explosive.beforeTick(body);}
    public void exploding(net.minecraft.world.entity.item.PrimedTnt body){explosive.exploding(body);}
    public static boolean managedTnt(Entity body){return LbcBalanceExplosiveBow.managed(body);}
    public ServerPlayer tntOwner(Entity body){return explosive.owner(body);}
    public LbcCloneSkills.ActorImage cloneTnt(Entity body){return explosive.cloneImage(body);}
    public static boolean managedRain(Entity body){return LbcBalanceRays.managedRain(body);}
    public LbcCloneSkills.ActorImage cloneRain(Entity body){return rays.cloneRain(body);}
    public List<net.minecraft.world.entity.projectile.Projectile> augment(ServerPlayer p,ItemStack weapon,List<net.minecraft.world.entity.projectile.Projectile> family){return extras.augment(p,weapon,family);}
    public void begin(){clocks.clear();cooldowns.clear();passive.clear();for(var p:game.server().getPlayerList().getPlayers())passive.changed(p);tick();}
    public void tick(){
        rays.tick();extras.tick();explosive.tick();eggs.tick();
        if(!legacy.getAsBoolean())return;
        for(var p:game.server().getPlayerList().getPlayers()){
            if(game.items().timeStop().stopped(p.level())||game.items().wands().inputTimeStopped(p))continue;
            int clock=clocks.getOrDefault(p.getUUID(),0);clocks.put(p.getUUID(),clock+1);
            if(clock%20==0)passive.portals(p);
            if(!p.isAlive()||p.isRemoved()||p.isSpectator()||game.isObserver(p))continue;
            passive.tick(p,clock);
            if(clock%5==0){var scores=cooldowns.computeIfAbsent(p.getUUID(),ignored->new int[8]);int bonus=passive.cooldownBonus(p);
                for(int i=0;i<scores.length;i++)scores[i]-=5+(i==LbcBalanceItems.Wand.WIKI.ordinal()?0:bonus);}
        }
    }
    /** Disconnect drops derived body caches; legacy scoreboard values survive reconnect. */
    public void forget(ServerPlayer p){passive.forget(p);rays.forget(p);}
    public void stop(){cooldowns.clear();passive.clear();rays.stop();extras.stop();explosive.stop();eggs.stop();clocks.clear();}
    public int cooldown(ServerPlayer p,LbcBalanceItems.Wand wand){var values=cooldowns.get(p.getUUID());return values==null?0:values[wand.ordinal()];}
    private List<LivingEntity> enemies(ServerPlayer p,double radius){return p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(radius),e->e!=p&&e.distanceToSqr(p)<=radius*radius&&game.items().wands().projectileTarget(p,e));}
    private List<LivingEntity> wild(ServerPlayer p,double radius){return p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(radius),e->e.distanceToSqr(p)<=radius*radius&&game.items().wands().validRoleTarget(e,false)&&!LbcGreed.active(e));}
    private void effect(ServerPlayer p,LivingEntity target,Holder<MobEffect> type,int seconds,int amp){effect(p,target,type,seconds,amp,false);}
    private void effect(ServerPlayer p,LivingEntity target,Holder<MobEffect> type,int seconds,int amp,boolean wild){
        var cause=wild?game.items().indirectKills().wildCause(p):game.items().indirectKills().coldCause(p,false);
        if(type.value().isInstantaneous()){game.items().indirectKills().withCause(target,cause,()->type.value().applyInstantaneousEffect(p.level(),p,p,target,amp,1));return;}
        var value=ItemEffectLevels.create(type,seconds*20,amp,false,false);
        if(wild)game.items().indirectKills().wildEffect(target,value,cause);else game.items().indirectKills().withCause(target,cause,()->target.addEffect(value,p));
    }
    private void spread(ServerPlayer p,int radius){var at=LbcSurfaceTeleport.destination(p.level(),p.position(),radius,p.level().getMaxY()+1,p.getRandom());if(at!=null)p.teleportTo(p.level(),at.x,at.y+.5,at.z,Set.of(),p.getYRot(),p.getXRot(),true);}
    private void sound(ServerPlayer p,String id,float volume,float pitch){LbcOverload.sound(p,p.position(),id,volume,pitch,0);}
    private void particle(ServerPlayer p,ParticleOptions kind,Vec3 at,int count,double x,double y,double z,double speed){p.level().sendParticles(kind,true,false,at.x,at.y,at.z,count,x,y,z,speed);}
    private void cast(ServerPlayer p,LbcBalanceItems.Wand wand){switch(wand){
        case EVOKER->evoker(p,p.getRandom().nextInt(3)+1);
        case BASS->{int roll=p.getRandom().nextInt(4);var type=switch(roll){case 0->MobEffects.WEAKNESS;case 1->MobEffects.SLOWNESS;case 2->MobEffects.BLINDNESS;default->MobEffects.MINING_FATIGUE;};
            for(var e:wild(p,25))if(e.distanceToSqr(p)>=1)effect(p,e,type,8,roll==1?7:0,true);
            sound(p,"minecraft:entity.firework_rocket.large_blast",2,0);sound(p,"minecraft:entity.firework_rocket.blast",2,0);particle(p,net.minecraft.core.particles.ColorParticleOption.create(ParticleTypes.FLASH,0xffffffff),p.position(),100,17,17,17,1);}
        case ENDER->{var origin=p.position().add(0,0,1);sound(p,"minecraft:entity.enderman.teleport",2,1);particle(p,ParticleTypes.PORTAL,origin,100,.2,.5,.2,.5);spread(p,60);particle(p,ParticleTypes.REVERSE_PORTAL,origin,100,.2,.5,.2,.5);sound(p,"minecraft:entity.enderman.teleport",2,1);}
        case CORRUPTER->{int roll=p.getRandom().nextInt(4);var type=switch(roll){case 0->MobEffects.POISON;case 1->MobEffects.WITHER;case 2->MobEffects.HUNGER;default->MobEffects.INSTANT_DAMAGE;};
            for(var e:enemies(p,15))if(e.distanceToSqr(p)>=.01)effect(p,e,type,new int[]{10,5,2,1}[roll],new int[]{1,1,255,0}[roll]);sound(p,"minecraft:entity.wither.ambient",2,1.2f);particle(p,ParticleTypes.ASH,p.position().add(0,1,0),1500,10,10,10,0);particle(p,ParticleTypes.ASH,p.position().add(0,1,0),300,1,1,1,1);}
        case VAMPIRE->{int n=0;for(var e:enemies(p,20))if(e.getType().builtInRegistryHolder().is(MOBS)&&e.distanceToSqr(p)>=.0001&&!e.entityTags().contains("nomagic_active")){effect(p,e,MobEffects.WITHER,1,1);if(e.distanceToSqr(p)>=.01)n++;}
            if(n>=13)effect(p,p,MobEffects.INSTANT_HEALTH,1,4);else if(n>0)effect(p,p,MobEffects.REGENERATION,1,n>=8?4:n>=4?3:2);sound(p,"minecraft:entity.cat.hiss",1.5f,2);particle(p,ParticleTypes.DAMAGE_INDICATOR,p.position().add(0,1,0),200,12,12,12,1);}
        case FOXUS->{int roll=p.getRandom().nextInt(4)+1;if(roll==1)effect(p,p,MobEffects.INVISIBILITY,10,0);
            if(roll==2){for(var e:enemies(p,10))if(e.distanceToSqr(p)>=.01)effect(p,e,MobEffects.SLOWNESS,3,50);spread(p,10);}
            if(roll==3){effect(p,p,MobEffects.SPEED,10,3);effect(p,p,MobEffects.JUMP_BOOST,10,3);}
            if(roll==4){for(var e:enemies(p,10))if(e.distanceToSqr(p)>=.01)effect(p,e,MobEffects.BLINDNESS,6,0);effect(p,p,MobEffects.LEVITATION,1,10);effect(p,p,MobEffects.SLOW_FALLING,8,0);}
            sound(p,switch(roll){case 1->"minecraft:entity.illusioner.prepare_mirror";case 2->"minecraft:entity.illusioner.mirror_move";case 3->"minecraft:entity.illusioner.cast_spell";default->"minecraft:entity.illusioner.prepare_blindness";},1,roll==2||roll==3?0:1);particle(p,switch(roll){case 1->ParticleTypes.CAMPFIRE_SIGNAL_SMOKE;case 2->ParticleTypes.LARGE_SMOKE;case 3->ParticleTypes.END_ROD;default->ParticleTypes.POOF;},p.position().add(0,1,0),roll==4?1000:roll==3?30:roll==2?80:100,roll==4?5:1,1,roll==4?5:1,roll==3?.05:roll==4?0:.075);}
        case BLOOD->{for(var e:enemies(p,10))if(e instanceof ServerPlayer&&e.distanceToSqr(p)>=.01){effect(p,e,MobEffects.INSTANT_DAMAGE,1,1);effect(p,e,MobEffects.HUNGER,60,255);effect(p,e,MobEffects.WITHER,25,1);}
            effect(p,p,MobEffects.INSTANT_DAMAGE,1,0);effect(p,p,MobEffects.WITHER,15,1);effect(p,p,MobEffects.POISON,15,1);sound(p,"minecraft:entity.ender_dragon.growl",1,2);particle(p,new BlockParticleOption(ParticleTypes.BLOCK,Blocks.REDSTONE_BLOCK.defaultBlockState()),p.getEyePosition().add(p.getLookAngle().scale(3)),1500,8,8,8,1);}
        case WIKI->{var value=LbcBalanceWiki.EFFECTS[p.getRandom().nextInt(81)];var type=BuiltInRegistries.MOB_EFFECT.get(Identifier.withDefaultNamespace(value.id())).orElseThrow();effect(p,p,type,value.seconds(),value.amplifier());sound(p,"minecraft:item.book.page_turn",10,1);}
    }}
    private void evoker(ServerPlayer p,int formation){
        if(formation==1)for(var target:wild(p,17)){if(target.distanceToSqr(p)<2.25||target instanceof net.minecraft.world.entity.decoration.ArmorStand)continue;var at=target.position();for(var offset:List.of(Vec3.ZERO,new Vec3(1.2,0,0),new Vec3(-1.2,0,0),new Vec3(0,0,1.2),new Vec3(0,0,-1.2)))game.items().evokerBass().spawnLegacyOwned(p,at.add(offset));}
        else if(formation==2)for(int y=0;y<=4;y+=2)for(int[] offset:new int[][]{{3,0},{-3,0},{0,3},{0,-3},{1,2},{2,1},{-1,-2},{-2,-1},{1,-2},{-1,2},{2,-1},{-2,1}})game.items().evokerBass().spawnLegacyOwned(p,p.position().add(offset[0],y,offset[1]));
        else{var forward=p.getLookAngle();var left=Vec3.directionFromRotation(0,p.getYRot()-90);for(int z=2;z<=20;z++)game.items().evokerBass().spawnLegacyOwned(p,p.position().add(forward.scale(z)));for(int z=2;z<=17;z++)for(int side:new int[]{-1,1})game.items().evokerBass().spawnLegacyOwned(p,p.position().add(forward.scale(z)).add(left.scale(side*(z-1)*.5)));}
        sound(p,"minecraft:entity.evoker.prepare_attack",1.5f,2);
    }
    /** Cooldowns and cached equipment are values; entities remain with the shared summon/ray owners. */
    MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){
        var savedClocks=new LinkedHashMap<>(clocks);var saved=new LinkedHashMap<UUID,int[]>();cooldowns.forEach((id,values)->saved.put(id,values.clone()));var equipment=passive.image();var rayImage=rays.checkpoint(links);var extraImage=extras.checkpoint(links);var explosiveImage=explosive.checkpoint(links);var eggImage=eggs.checkpoint(links);var pigImage=game.items().luckyPigs().fakeCheckpoint(links);
        return current->{var rayPlan=rayImage.prepare(current);var extraPlan=extraImage.prepare(current);var explosivePlan=explosiveImage.prepare(current);var eggPlan=eggImage.prepare(current);var pigPlan=pigImage.prepare(current);return ()->{clocks.clear();clocks.putAll(savedClocks);cooldowns.clear();saved.forEach((id,values)->cooldowns.put(id,values.clone()));passive.restore(equipment);rayPlan.run();extraPlan.run();explosivePlan.run();eggPlan.run();pigPlan.run();};};
    }
}
