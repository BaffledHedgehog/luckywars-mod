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

/** Native skill20 acquisition and bidirectional accepted damage transfer. */
public final class LbcSymbioteSkill {
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,
            Identifier.parse("minecraft:mobs"));
    private static final Set<String> SERVICE_TAGS=Set.of("spectator","strict_map_object","magic_academy");
    private final GameController game;
    private final Set<ServerPlayer> distributing=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<ServerPlayer> receiving=Collections.newSetFromMap(new IdentityHashMap<>());

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
    public float absorb(LivingEntity target,DamageSource source,float damage){
        if(!(damage>0)||!Float.isFinite(damage))return damage;
        if(!(target instanceof ServerPlayer owner)){
            var master=game.items().communism().owner(target);
            if(master==null||!master.isAlive()||master.isRemoved()||master.level()!=target.level()
                    ||!combatMob(target)||!game.lbc().hasSkill(master,20)||paused(master)
                    ||MatchActorCheckpoint.restoring(target)||distributing.contains(master))return damage;
            if(receiving.contains(master))return 0;
            receiving.add(master);
            try{BlackHoleDamage.transferAccepted(game,master,game.items().indirectKills().transferDamage(source),damage);}
            finally{receiving.remove(master);}
            return 0;
        }
        // This is the already accepted debit from a directly struck subordinate,
        // not another attack to distribute back into that same subordinate cohort.
        if(receiving.contains(owner))return damage;
        if(MatchPlayerCheckpoint.restoring(owner)||!game.lbc().hasSkill(owner,20)||paused(owner))return damage;
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

    public void clear(ServerPlayer owner){distributing.remove(owner);receiving.remove(owner);}
    public void stop(){distributing.clear();receiving.clear();}
    void checkpointBoundary(){
        if(!distributing.isEmpty()||!receiving.isEmpty())throw new IllegalStateException("Cannot snapshot during Symbiote redistribution");
    }
}
