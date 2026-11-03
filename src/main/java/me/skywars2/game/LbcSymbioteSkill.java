package me.skywars2.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Native skill20 acquisition and accepted owner damage distribution. */
public final class LbcSymbioteSkill {
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,
            Identifier.parse("minecraft:mobs"));
    private static final Set<String> SERVICE_TAGS=Set.of("spectator","strict_map_object","magic_academy");
    private final GameController game;
    private final Set<ServerPlayer> distributing=Collections.newSetFromMap(new IdentityHashMap<>());

    public LbcSymbioteSkill(GameController game){this.game=game;}

    private boolean paused(ServerPlayer owner){
        return game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally()
                ||game.items().wands().timeStopped()||game.items().wands().inputTimeStopped(owner)
                ||game.items().timeStop().stopped(owner.level());
    }

    private boolean combatMob(LivingEntity mob){
        return !(mob instanceof Player)&&mob.getType().builtInRegistryHolder().is(MOBS)
                // Its living Vex body replaces a source item_display projectile, not a subordinate mob.
                &&!(mob instanceof LbcFpvDrone.Drone)
                &&mob.isAlive()&&!mob.isRemoved()&&!mob.isSpectator()
                &&Collections.disjoint(mob.entityTags(),SERVICE_TAGS)
                &&!game.items().polymorph().concealed(mob);
    }

    /** Local native spatial query only. MAG, TECH, artifacts and Greed are not skill jammers. */
    public void tick(){
        for(var owner:game.server().getPlayerList().getPlayers()){
            if(!game.lbc().hasSkill(owner,20)||paused(owner))continue;
            for(var mob:owner.level().getEntitiesOfClass(LivingEntity.class,
                    owner.getBoundingBox().inflate(2),this::combatMob)){
                if(owner.distanceToSqr(mob)>4||game.items().communism().owner(mob)==owner)continue;
                game.items().communism().subjugate(owner,mob,true,true);
                LbcOverload.sound(owner.level(),mob.position(),"minecraft:entity.villager.trade",1,1,0,0);
            }
        }
    }

    /**
     * Shared native/raw-HP boundary: after ordinary mitigation, before finite armor reserve
     * and absorption/HP debit. Return zero without cancelling the enclosing native hit.
     * No distance check here; the entire current same-dimension cohort gets equal shares.
     */
    public float absorb(ServerPlayer owner,DamageSource source,float damage){
        if(!(damage>0)||!Float.isFinite(damage)
                ||MatchPlayerCheckpoint.restoring(owner)||!game.lbc().hasSkill(owner,20)||paused(owner))return damage;
        var cohort=new ArrayList<LivingEntity>();
        // Existing canonical owner authority and derived native score index; never a world scan.
        for(var actor:game.items().communism().controlledActors(owner.getUUID()))
            if(actor instanceof LivingEntity mob&&mob.level()==owner.level()&&combatMob(mob))cohort.add(mob);
        if(cohort.isEmpty())return damage;
        // Feedback from this in-flight distribution cannot hurt the protected owner or
        // start another redistribution chain. Recheck the live cohort before blocking it.
        if(distributing.contains(owner))return 0;
        float share=damage/cohort.size();
        var transfer=game.items().indirectKills().transferDamage(source);
        distributing.add(owner);
        try{
            // Capture cohort before dispatch: deaths, cloning and retargeting cannot change this divisor.
            // No minimum 1HP, integer flooring, artificial HP buffer or residual owner damage.
            for(var mob:cohort)if(mob.isAlive()&&!mob.isRemoved())
                BlackHoleDamage.transferAccepted(game,mob,transfer,share);
        }finally{distributing.remove(owner);}
        return 0;
    }

    public void clear(ServerPlayer owner){distributing.remove(owner);}
    public void stop(){distributing.clear();}
    void checkpointBoundary(){
        if(!distributing.isEmpty())throw new IllegalStateException("Cannot snapshot during Symbiote redistribution");
    }
}
