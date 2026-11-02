package me.skywars2.game;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.function.BooleanSupplier;
import me.skywars2.mixin.WandProjectileMixin;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Actual-health mana recovery and finite, resident-column Teleportit evasion. */
public final class LbcReactiveSkills {
    private static final TagKey<EntityType<?>> PROJECTILES=TagKey.create(Registries.ENTITY_TYPE,
            Identifier.parse("minecraft:raycasters_projectiles"));
    private final GameController game;
    private final Deque<Attempt> attempts=new ArrayDeque<>();
    private long payouts,damageRolls,damageTeleports,projectileTeleports,projectileQueries;
    private static final class Attempt {
        final ServerPlayer player;
        final float health;
        final boolean admitted;
        final int regeneration;
        double nestedLoss;
        Attempt(ServerPlayer player,boolean admitted,int regeneration){
            this.player=player;this.health=player.getHealth();this.admitted=admitted;this.regeneration=regeneration;
        }
    }
    public LbcReactiveSkills(GameController game){this.game=game;}

    private boolean admitted(ServerPlayer player,int skill){
        return game.lbc().hasSkill(player,skill)
                &&game.server().tickRateManager().runsNormally()&&!game.server().tickRateManager().isFrozen()
                &&!game.items().wands().inputTimeStopped(player)
                &&!game.items().timeStop().stopped(player.level())
                &&!game.items().magic().suppressed(player);
    }
    /** Each nested attempt owns its actual HP loss, so an outer callback cannot pay it twice. */
    public boolean withIncomingAttempt(ServerPlayer player,BooleanSupplier action){
        boolean allowed=admitted(player,6);
        if(!allowed&&attempts.stream().noneMatch(outer->outer.player==player))return action.getAsBoolean();
        var attempt=new Attempt(player,allowed,allowed?game.items().mana().baseRegen(player):0);
        attempts.push(attempt);boolean accepted=false,completed=false;
        try{accepted=action.getAsBoolean();completed=true;return accepted;}
        finally{
            attempts.pop();
            double loss=Math.max(0,(double)attempt.health-player.getHealth());
            for(var outer:attempts)if(outer.player==player){outer.nestedLoss+=loss;break;}
            double ownLoss=Math.max(0,loss-attempt.nestedLoss);
            if(completed&&accepted&&attempt.admitted&&ownLoss>0&&player.isAlive()&&!player.isRemoved()){
                var mana=game.items().mana();
                int next=manaAfterLoss(mana.get(player),ownLoss,attempt.regeneration);
                if(next!=mana.get(player)){mana.set(player,next);game.items().indirectKills().manaRecovered(player);payouts++;}
            }
        }
    }
    /** Source additive payout, positive for either regeneration sign; no maximum-mana cap. */
    static int manaAfterLoss(int mana,double lostHp,int baseRegeneration){
        if(!(lostHp>0)||baseRegeneration==0)return mana;
        double gain=lostHp*100*Math.abs((long)baseRegeneration);
        // Compare before conversion: even MIN_VALUE regeneration and enormous native HP cannot wrap.
        if(gain>=(long)Integer.MAX_VALUE-mana)return Integer.MAX_VALUE;
        return (int)((long)mana+(long)gain);
    }

    /** Called only for accepted native/direct-HP damage; it does not alter hit policy. */
    public void damaged(LivingEntity victim,DamageSource source){
        if(!(victim instanceof ServerPlayer player)||!admitted(player,10))return;
        damageRolls++;
        if(player.getRandom().nextInt(5)==0&&teleport(player,3))damageTeleports++;
    }
    /** Iterate selected live owners only; no inventory scans or world-wide projectile enumeration. */
    public void tick(){
        for(var player:game.server().getPlayerList().getPlayers()){
            if(!admitted(player,10))continue;
            projectileQueries++;
            boolean danger=!player.level().getEntities((Entity)null,
                    new AABB(player.position(),player.position()).inflate(4),
                    entity->airborneEnemyProjectile(player,entity)).isEmpty();
            if(!danger)danger=game.items().exasperato().hostileRayNear(player,4);
            if(danger&&teleport(player,5))projectileTeleports++;
        }
    }
    boolean airborneEnemyProjectile(ServerPlayer observer,Entity entity){
        if(entity==observer||entity.isRemoved()||!entity.isAlive()||entity.position().distanceToSqr(observer.position())>16
                ||!(entity instanceof Projectile||entity.getType().builtInRegistryHolder().is(PROJECTILES))
                ||entity.entityTags().contains("time_stopped")||game.items().timeStop().frozen(entity)
                ||entity instanceof AbstractArrow arrow&&((WandProjectileMixin.ArrowAccess)(Object)arrow).skywars2$isInGround())return false;
        var owner=LbcIndirectKills.playerSource(entity);
        if(owner!=null)return owner!=observer&&!owner.isAlliedTo(observer);
        // Natural mob shots have native ownership without player IDs or legacy scoreboard metadata.
        // Resolve attributed summons above first, so their player's alliance remains authoritative.
        if(entity instanceof Projectile projectile&&projectile.getOwner()!=null){
            var nativeOwner=projectile.getOwner();
            return nativeOwner!=observer&&!nativeOwner.isAlliedTo(observer);
        }
        // Resident source actors can retain only legacy scoreboard ownership. Read without creating scores.
        Integer foreign=score(entity,"lbcID2"),own=score(observer,"lbcID2");
        if(foreign==null||own==null||foreign.equals(own))return false;
        Integer foreignTeam=score(entity,"team_number"),ownTeam=score(observer,"team_number");
        return foreignTeam==null||ownTeam==null||!foreignTeam.equals(ownTeam);
    }
    private Integer score(Entity entity,String name){
        var board=game.server().getScoreboard();var objective=board.getObjective(name);
        var score=objective==null?null:board.getPlayerScoreInfo(entity,objective);
        return score==null?null:score.value();
    }
    boolean teleport(ServerPlayer player,int radius){
        int ceiling=player.level().dimension().equals(Level.NETHER)?120:player.level().getMaxY()+1;
        var destination=LbcSurfaceTeleport.destination(player.level(),player.position(),radius,ceiling,player.getRandom());
        if(destination==null)return false;
        var velocity=player.getDeltaMovement();var known=player.getKnownMovement();
        float yaw=player.getYRot(),pitch=player.getXRot();
        try{return player.teleportTo(player.level(),destination.x,destination.y,destination.z,
                TeleportMomentum.positionFlags(Set.of()),yaw,pitch,true);}
        finally{player.setDeltaMovement(velocity);player.setKnownMovement(known);}
    }
    public long payoutCount(){return payouts;}
    public long damageRollCount(){return damageRolls;}
    public long damageTeleportCount(){return damageTeleports;}
    public long projectileTeleportCount(){return projectileTeleports;}
    public long projectileQueryCount(){return projectileQueries;}
}
