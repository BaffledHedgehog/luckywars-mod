package me.skywars2.game;

import java.util.*;
import me.skywars2.mixin.StaticChargeCloudAccess;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.*;

/** Tranquillizer126: persistent charge, refreshed by its cloud, discharged on an accepted hit. */
public final class LbcStaticCharge {
    static final String COUNTER="effect_shock_potion";
    private static final String BEAM="skywars2.static_charge_visual";
    private static final DustColorTransitionOptions DUST=new DustColorTransitionOptions(0xff00ccff,0xff003333,.9f);
    private static final net.minecraft.nbt.CompoundTag BEAM_DATA=beamData();
    private static net.minecraft.nbt.CompoundTag beamData(){
        try{return net.minecraft.nbt.TagParser.parseCompoundFully("{shadow_radius:0f,shadow_strength:0f,item:{id:'minecraft:repeating_command_block',count:1,components:{'minecraft:item_model':'lbc:lightning_bolt_linear'}},transformation:{scale:[3f,3f,-1f],translation:[0f,0f,0f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]}}");}
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new ExceptionInInitializerError(error);}
    }
    private final GameController game;
    private Set<LivingEntity> charged=new LinkedHashSet<>(),pending=new LinkedHashSet<>();
    private Map<Display.ItemDisplay,Integer> beams=new LinkedHashMap<>();
    private Set<AreaEffectCloud> clouds=Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean frozen;private int callbacks,discharging;
    LbcStaticCharge(GameController game){this.game=game;}
    public static boolean marker(MobEffectInstance effect){return effect.is(MobEffects.RAID_OMEN)&&effect.getAmplifier()==126;}
    private int amount(LivingEntity target){return game.items().mana().hudValue(target,COUNTER);}
    private void write(LivingEntity target,int value){game.items().mana().score(target,COUNTER).set(value);if(value>0)charged.add(target);else charged.remove(target);}
    private boolean valid(LivingEntity target){return target.level() instanceof ServerLevel&&LbcDimensionItems.ordinary(target.level())&&game.playing()
            &&game.stage()!=GameController.Stage.ENDING&&game.items().wands().validTarget(target);}
    private boolean paused(Entity target){return !game.server().tickRateManager().runsNormally()||game.items().timeStop().stopped((ServerLevel)target.level())
            ||game.items().timeStop().frozen(target)||target instanceof LivingEntity living&&game.items().frost().isFrozen(living);}
    public boolean accept(LivingEntity target,MobEffectInstance effect){
        if(!marker(effect)||frozen||!valid(target)||LbcParasite.active(target)
                ||target instanceof ServerPlayer player&&game.items().magic().rejectCustomDebuff(player))return false;
        callbacks++;try{
            int before=amount(target);
            // Source get_effect discharges an existing charge before adding ten; no tick timer is drained.
            if(before>0)discharge(target);
            int remaining=amount(target);if(remaining<40)write(target,remaining+10);
            return true;
        }finally{callbacks--;}
    }
    public void damaged(LivingEntity target){if(!frozen&&discharging==0&&amount(target)>0&&valid(target))pending.add(target);}
    public void loaded(Entity entity){
        if(frozen)return;
        if(entity instanceof LivingEntity living&&amount(living)>0&&valid(living))charged.add(living);
        if(entity instanceof AreaEffectCloud cloud){
            boolean matches=false;for(var effect:((StaticChargeCloudAccess)cloud).skywars2$staticPotion().getAllEffects())if(marker(effect)){matches=true;break;}
            if(matches){cloud.setRadius(5);cloud.setRadiusOnUse(0);cloud.setRadiusPerTick(0);cloud.setWaitTime(0);
                if(!cloud.entityTags().contains("skywars2.static_cloud"))cloud.setDuration(300);
                cloud.entityTags().add("skywars2.static_cloud");cloud.setPotionDurationScale(1);cloud.setCustomParticle(DUST);
                ((StaticChargeCloudAccess)cloud).skywars2$staticReapplication(10);clouds.add(cloud);}
        }
    }
    public void unloaded(Entity entity){if(frozen)return;charged.remove(entity);pending.remove(entity);clouds.remove(entity);beams.remove(entity);}
    public void tick(){
        if(frozen)return;callbacks++;try{
            for(var target:List.copyOf(charged)){
                if(!valid(target)||LbcParasite.active(target)||target instanceof ServerPlayer p&&game.items().magic().rejectCustomDebuff(p)){clear(target);continue;}
                if(amount(target)<=0){charged.remove(target);continue;}if(paused(target))continue;
                var level=(ServerLevel)target.level();var at=target.getEyePosition().add(0,-.8,0);
                level.sendParticles(DUST,at.x,at.y,at.z,2,.3,.3,.3,0);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,at.x,at.y,at.z,1,.1,.1,.1,0);
            }
            var accepted=List.copyOf(pending);pending.clear();
            for(var target:accepted){if(paused(target)){pending.add(target);continue;}if(valid(target)&&amount(target)>0)discharge(target);}
            for(var it=beams.entrySet().iterator();it.hasNext();){var beam=it.next();var entity=beam.getKey();
                if(entity.isRemoved()){it.remove();continue;}if(paused(entity))continue;
                if(beam.getValue()<=1){entity.discard();it.remove();}else beam.setValue(beam.getValue()-1);}
            clouds.removeIf(Entity::isRemoved);for(var cloud:clouds)if(!paused(cloud)){
                var random=cloud.getRandom();double angle=random.nextDouble()*Math.PI*2,r=Math.sqrt(random.nextDouble())*cloud.getRadius();
                ((ServerLevel)cloud.level()).sendParticles(ParticleTypes.ELECTRIC_SPARK,cloud.getX()+Math.cos(angle)*r,cloud.getY()+.1,cloud.getZ()+Math.sin(angle)*r,1,.1,.1,.1,0);}
        }finally{callbacks--;}
    }
    private void discharge(LivingEntity emitter){
        int charge=amount(emitter);if(charge<=0||!valid(emitter))return;write(emitter,0);pending.remove(emitter);
        var level=(ServerLevel)emitter.level();var visited=Collections.newSetFromMap(new IdentityHashMap<LivingEntity,Boolean>());visited.add(emitter);
        Vec3 from=emitter.getEyePosition();
        for(int link=0;link<charge;link++){
            final Vec3 origin=from;double radius=link==0?10:20;
            var target=level.getEntitiesOfClass(LivingEntity.class,new AABB(origin,origin).inflate(radius),e->!visited.contains(e)&&valid(e)
                    &&!e.isAlliedTo(emitter)&&e.getEyePosition().distanceToSqr(origin)<=radius*radius)
                    .stream().min(Comparator.comparingDouble(e->e.getEyePosition().distanceToSqr(origin))).orElse(null);
            if(target==null||game.items().magic().mechanismSuppressedAt(level,from))break;
            // Source ray50 quarter-block steps; a wall terminates the chain.
            var delta=target.getEyePosition().subtract(from);var end=from.add(delta.normalize().scale(Math.min(12.5,delta.length())));
            var wall=LbcTerra.wallHit(level,from,end);
            if(wall!=null){beam(level,from,wall.getLocation());break;}
            boolean stopped=false;int steps=(int)Math.ceil(from.distanceTo(end)*4);
            for(int step=1;step<=steps;step++)if(game.items().magic().mechanismSuppressedAt(level,from.lerp(end,step/(double)steps))){stopped=true;break;}
            if(stopped)break;
            if(target.getBoundingBox().clip(from,end).isEmpty()&&!target.getBoundingBox().contains(end)){beam(level,from,end);break;}
            if(game.items().magic().mechanismSuppressedAt(level,target.position()))break;
            beam(level,from,target.getEyePosition());visited.add(target);write(target,Math.max(0,charge-1));
            if(!game.items().fpvDrone().tryShortCircuit(target)){
                // A transferred charge stays available for the next external hit.
                // Damage from this discharge must not immediately start an echo chain.
                discharging++;try{target.hurtServer(level,new DamageSource(level.registryAccess()
                        .lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.ARROW),emitter),6);}
                finally{discharging--;}
            }
            from=target.getEyePosition();
        }
    }
    private void beam(ServerLevel level,Vec3 from,Vec3 to){
        var entity=new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY,level);
        var direction=to.subtract(from);
        var data=BEAM_DATA.copy();data.getCompoundOrEmpty("transformation").getListOrEmpty("scale").set(2,net.minecraft.nbt.FloatTag.valueOf((float)-direction.length()));
        entity.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),data));
        entity.setPos(from);
        entity.setYRot((float)-Math.toDegrees(Math.atan2(direction.x,direction.z)));
        entity.setXRot((float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance())));
        entity.entityTags().add(BEAM);game.items().markCurrentItemEpoch(entity);
        if(level.addFreshEntity(entity))beams.put(entity,8);
    }
    public void clear(LivingEntity target){pending.remove(target);write(target,0);}
    public void stop(){for(var target:List.copyOf(charged))clear(target);for(var beam:beams.keySet())beam.discard();beams.clear();pending.clear();clouds.clear();frozen=false;}
    public static boolean visual(Entity entity){return entity!=null&&entity.entityTags().contains(BEAM);}
    void boundary(){if(callbacks!=0)throw new IllegalStateException("Finish static charge callbacks before checkpointing");}
    void frozen(boolean value){boundary();frozen=value;}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){return new Checkpoint(links);}
    final class Checkpoint {
        final List<MatchDamageSources.Ref> live,waiting,fields;
        final Map<MatchDamageSources.Ref,Integer> rays=new LinkedHashMap<>();
        Checkpoint(MatchSpellCheckpoint.Links links){boundary();live=charged.stream().map(links::key).toList();waiting=pending.stream().map(links::key).toList();fields=clouds.stream().map(links::key).toList();beams.forEach((e,ttl)->rays.put(links.key(e),ttl));}
        Runnable prepare(MatchSpellCheckpoint.Links links){
            var c=new LinkedHashSet<LivingEntity>();for(var ref:live)c.add((LivingEntity)links.get(ref));
            var p=new LinkedHashSet<LivingEntity>();for(var ref:waiting)p.add((LivingEntity)links.get(ref));
            var a=Collections.newSetFromMap(new IdentityHashMap<AreaEffectCloud,Boolean>());for(var ref:fields)a.add((AreaEffectCloud)links.get(ref));
            var b=new LinkedHashMap<Display.ItemDisplay,Integer>();rays.forEach((ref,ttl)->b.put((Display.ItemDisplay)links.get(ref),ttl));
            return ()->{charged=c;pending=p;clouds=a;beams=b;};
        }
    }
}
