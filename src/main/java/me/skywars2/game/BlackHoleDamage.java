package me.skywars2.game;

import me.skywars2.mixin.BlackHoleDeathProtectionAccess;
import me.skywars2.mixin.CombatCheckpointAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Shared hole contact: native body geometry and HP loss without a hurt packet or hit cooldown. */
final class BlackHoleDamage {
    private BlackHoleDamage() { }
    static double distanceSquared(Entity target,Vec3 center) {
        return target.getBoundingBox().distanceToSqr(center);
    }
    static boolean apply(GameController game,LivingEntity target,DamageSource original,float amount) {
        if(!(amount>0)||!Float.isFinite(amount)||!target.isAlive()||target.isRemoved()
                ||!(target.level() instanceof ServerLevel level)||game.vanity().busy()
                ||game.items().timeStop().frozen(target))return false;
        var source=game.items().indirectKills().inheritDamage(target,original);
        if(target.isInvulnerableTo(level,source))return false;
        if(target instanceof ServerPlayer player){
            if(game.vanity().blocked(player))return false;
            boolean creativeBorder=player.isCreative()&&source.is(DamageTypes.OUTSIDE_BORDER);
            if(player.getAbilities().invulnerable&&!creativeBorder&&!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))return false;
            if(source.getEntity() instanceof Player attacker&&!player.canHarmPlayer(attacker)
                    &&!LbcDamage.allowsTrapFriendlyFire(source)&&!LbcDamage.allowsElectricFriendlyFire(source)
                    &&!LbcDamage.allowsKfcFriendlyFire(source))return false;
        }
        if(!net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.invoker()
                .allowDamage(target,source,amount))return false;
        return target instanceof ServerPlayer player
                ?game.items().withIncomingDamageAttempt(player,()->accepted(game,target,source,amount,level))
                :accepted(game,target,source,amount,level);
    }
    private static boolean accepted(GameController game,LivingEntity target,DamageSource source,float amount,ServerLevel level){
        ServerPlayer recipient=target instanceof ServerPlayer player?player:game.items().polymorph().playerOfBody(target);
        // Bypass mitigation/physical hit response, while publishing the ordinary damage events.
        // Native criteria/attempt owners remain shared with hurtServer, including fatal hits.
        game.items().indirectKills().damaged(target,source);
        if(source.getEntity() instanceof LivingEntity attacker&&attacker!=target){
            target.setLastHurtByMob(attacker);
            if(attacker instanceof ServerPlayer player)target.setLastHurtByPlayer(player,100);
        }
        var combat=(CombatCheckpointAccess.Living)target;
        combat.skywars2$damage(source);combat.skywars2$damageTime(level.getGameTime());
        target.getCombatTracker().recordDamage(source,amount);
        // Chef's existing setHealth override converts accepted HP loss into knock progress.
        target.setHealth(Math.max(0,target.getHealth()-amount));
        if(target.getHealth()<=0&&net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DEATH.invoker()
                .allowDeath(target,source,amount)&&!((BlackHoleDeathProtectionAccess)target).skywars2$protectFromHole(source)){
            target.die(source);
            game.items().polymorph().finishDirectDamage(target);
        }
        // Match native ordering: death/totem, effect reactions, criteria, surviving-hit event.
        // Death dispatches its own listeners; never synthesize another AFTER_DEATH.
        for(var effect:java.util.List.copyOf(target.getActiveEffects()))effect.onMobHurt(level,target,source,amount);
        if(target instanceof ServerPlayer player)
            net.minecraft.advancements.triggers.CriteriaTriggers.ENTITY_HURT_PLAYER.trigger(player,source,amount,amount,false);
        if(source.getEntity() instanceof ServerPlayer attacker)
            net.minecraft.advancements.triggers.CriteriaTriggers.PLAYER_HURT_ENTITY.trigger(attacker,target,source,amount,amount,false);
        if(!target.isDeadOrDying())net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DAMAGE.invoker()
                .afterDamage(target,source,amount,amount,false);
        if(recipient!=null&&recipient.isAlive()){
            // LocalPlayer.hurtTo also starts hurtTime from an ordinary health packet.
            // Publish native health metadata first so its later health/food packet has
            // no negative delta. Other players still receive the usual tracker update.
            recipient.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(recipient.getId(),
                    java.util.List.of(net.minecraft.network.syncher.SynchedEntityData.DataValue.create(
                            BlackHoleDeathProtectionAccess.skywars2$healthData(),recipient.getHealth()))));
        }
        return true;
    }
}
