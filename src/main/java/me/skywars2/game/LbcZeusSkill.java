package me.skywars2.game;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Instantaneous, once-per-body Zeus chains; admission and the twenty-tick clock belong to the caller. */
public final class LbcZeusSkill {
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,
            Identifier.parse("minecraft:mobs"));
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,
            Identifier.parse("minecraft:airs"));
    private static final double TARGET_RADIUS_SQUARED=36,STEP=.25;
    private final GameController game;
    private final java.util.Map<ServerPlayer,Integer> clocked=new IdentityHashMap<>();
    private boolean pulsing;

    public LbcZeusSkill(GameController game){this.game=game;}

    int phase(ServerPlayer owner){return game.items().mana().score(owner,"zeus_skill_phase").get();}
    /** Native ticks only; the canonical scoreboard phase is included in full match rewind. */
    public void tick(ServerPlayer owner){
        if(!game.lbc().hasSkill(owner,22))return;
        int now=game.server().getTickCount();
        if(java.util.Objects.equals(clocked.get(owner),now)||game.server().tickRateManager().isFrozen()
                ||!game.server().tickRateManager().runsNormally()||game.items().wands().timeStopped()
                ||game.items().wands().inputTimeStopped(owner)||game.items().timeStop().stopped(owner.level()))return;
        clocked.put(owner,now);
        int next=Math.floorMod(phase(owner),20)+1;
        game.items().mana().score(owner,"zeus_skill_phase").set(next==20?0:next);
        // MAG owns only admission; released segments use TECH, never a target MAG check.
        if(next==20&&!game.items().magic().suppressed(owner))pulse(owner);
    }
    public void tick(){for(var owner:game.server().getPlayerList().getPlayers())tick(owner);}
    public void clear(ServerPlayer owner){
        if(MatchPlayerCheckpoint.restoring(owner))return;
        boolean tracked=clocked.remove(owner)!=null;
        if(tracked||game.players().get(owner.getUUID()).lbcSkill==22)
            game.items().mana().score(owner,"zeus_skill_phase").set(0);
    }
    public void stop(){for(var owner:game.server().getPlayerList().getPlayers())clear(owner);clocked.clear();}
    public void clearTransient(){clocked.clear();}

    /** One independent source50% trial. Caller owns skill/unlock, pause and suppression policy. */
    public void pulse(ServerPlayer owner){
        if(pulsing||owner.getRandom().nextInt(2)!=0)return;
        fire(owner);
    }

    /** Deterministic accepted pulse for native qualification; never performs its own admission. */
    void fire(ServerPlayer owner){
        if(pulsing)return;
        pulsing=true;
        try{
            var level=owner.level();
            var epoch=game.items().epoch();
            Vec3 origin=owner.position().add(0,1,0);
            // Empty local aura needs neither the resident baseline walk nor a damage-source allocation.
            if(nearest(owner,Collections.emptySet(),origin,Integer.MAX_VALUE)==null)return;
            // Native IDs increase on creation. The cached resident baseline bounds this pulse without
            // copying every body or allowing damage-created children to grow the chain indefinitely.
            int highestBodyId=owner.getId();
            for(var target:game.items().lifetimes().loadedTargets())
                if(target.level()==level)highestBodyId=Math.max(highestBodyId,target.getId());
            Set<LivingEntity> visited=Collections.newSetFromMap(new IdentityHashMap<>());
            var damage=new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                    .getOrThrow(LbcDamage.LIGHTNING),owner);
            while(owner.isAlive()&&!owner.isRemoved()&&owner.level()==level&&game.playing()
                    &&game.stage()!=GameController.Stage.ENDING&&game.items().currentEpoch(epoch)){
                var aim=nearest(owner,visited,origin,highestBodyId);
                if(aim==null)return;
                Vec3 direction=aim.getEyePosition().subtract(origin).normalize();
                var hit=leg(owner,level,visited,origin,direction,highestBodyId);
                if(hit==null)return;
                // Accepted/rejected native damage both consume this body's one attempt, as in source.
                visited.add(hit.target);
                hit.target.hurtServer(level,damage,4);
                LbcOverload.sound(level,hit.sample,"minecraft:entity.lightning_bolt.impact",1,2,0,0);
                origin=hit.sample;
            }
        }finally{pulsing=false;}
    }

    private boolean eligible(ServerPlayer owner,LivingEntity target){
        return target.level()==owner.level()&&target.getType().builtInRegistryHolder().is(MOBS)
                &&game.items().wands().validRoleTarget(target,false)
                &&game.items().wands().target(owner,target);
    }

    private LivingEntity nearest(ServerPlayer owner,Set<LivingEntity> visited,Vec3 origin,int highestBodyId){
        LivingEntity nearest=null;double best=TARGET_RADIUS_SQUARED;
        for(var target:owner.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(origin,origin).inflate(6.000001),
                body->body.getId()<=highestBodyId&&!visited.contains(body)&&eligible(owner,body))){
            double distance=target.position().distanceToSqr(origin);
            if(distance<=best&&(nearest==null||distance<best)){
                best=distance;nearest=target;
            }
        }
        return nearest;
    }

    private record Hit(LivingEntity target,Vec3 sample){}

    private Hit leg(ServerPlayer owner,ServerLevel level,Set<LivingEntity> visited,
            Vec3 origin,Vec3 direction,int highestBodyId){
        Vec3 end=origin.add(direction.scale(25*STEP));
        LivingEntity first=null;Vec3 firstEntry=null;double best=Double.POSITIVE_INFINITY;
        // Broadphase may be widened for query precision; the shared exact hitbox test is never widened.
        for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(origin,end).inflate(.000001),
                body->body.getId()<=highestBodyId&&!visited.contains(body)&&eligible(owner,body))){
            var box=target.getBoundingBox();
            if(!LbcBeamGeometry.intersects(box,origin,end))continue;
            Vec3 entry=box.contains(origin)?origin:box.clip(origin,end).orElse(end);
            double distance=entry.distanceToSqr(origin);
            if(distance<best){best=distance;first=target;firstEntry=entry;}
        }
        Vec3 previous=origin;
        // Source starts at0 and takes25 further quarter-block steps, including endpoint6.25.
        for(int sample=0;sample<=25;sample++){
            Vec3 point=origin.add(direction.scale(sample*STEP));
            double boundary=game.items().magic().mechanismSuppressionFraction(level,previous,point);
            boolean blocked=Double.isFinite(boundary);
            if(blocked){
                if(boundary<=0)return null;
                point=previous.lerp(point,boundary);
            }
            var block=BlockPos.containing(point);
            // getChunkNow is resident-only; hasChunkAt may admit a not-yet-ready chunk.
            if(level.getChunkSource().getChunkNow(block.getX()>>4,block.getZ()>>4)==null)return null;
            if(sample!=0&&!level.getBlockState(block).is(AIRS))return null;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,point.x,point.y,point.z,1,.1,.1,.1,1);
            LbcOverload.sound(level,point,"minecraft:entity.firework_rocket.twinkle",.03f,2,0,0);
            double reached=point.distanceToSqr(origin);
            // A body exactly at the TECH boundary cannot receive damage through that field.
            if(first!=null&&(blocked?best<reached:best<=reached))
                return new Hit(first,blocked?firstEntry:point);
            if(blocked)return null;
            previous=point;
        }
        return null;
    }
}
