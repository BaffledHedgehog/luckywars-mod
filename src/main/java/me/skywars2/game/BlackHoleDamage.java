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

/** Raw hole HP loss retains native reactions and sound while the victim gets no physical hit response. */
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
    private static void sound(LivingEntity target,net.minecraft.sounds.SoundEvent sound){
        if(sound==null)return;
        if(target instanceof ServerPlayer){
            // Player.playSound normally omits self, relying on the client hit event.
            // Our hit event is intentionally absent; send this one native sound to everyone.
            target.level().playSound(null,target.getX(),target.getY(),target.getZ(),sound,target.getSoundSource(),
                    ((BlackHoleDeathProtectionAccess)target).skywars2$holeSoundVolume(),target.getVoicePitch());
        }else target.makeSound(sound);
    }
    private static boolean accepted(GameController game,LivingEntity target,DamageSource source,float amount,ServerLevel level){
        ServerPlayer recipient=target instanceof ServerPlayer player?player:game.items().polymorph().playerOfBody(target);
        // Bypass mitigation/physical hit response, while publishing the ordinary damage events.
        // Native criteria/attempt owners remain shared with hurtServer, including fatal hits.
        if(target.isSleeping())target.stopSleeping();
        target.setNoActionTime(0);
        game.items().indirectKills().damaged(target,source);
        var nativeHit=(BlackHoleDeathProtectionAccess)target;
        nativeHit.skywars2$holeMobCredit(source);nativeHit.skywars2$holePlayerCredit(source);
        var combat=(CombatCheckpointAccess.Living)target;
        combat.skywars2$damage(source);combat.skywars2$damageTime(level.getGameTime());
        target.getCombatTracker().recordDamage(source,amount);
        // Chef's existing setHealth override converts accepted HP loss into knock progress.
        target.setHealth(Math.max(0,target.getHealth()-amount));
        target.gameEvent(net.minecraft.world.level.gameevent.GameEvent.ENTITY_DAMAGE);
        if(target instanceof ServerPlayer player){
            player.causeFoodExhaustion(source.getFoodExhaustion());
            if(amount<3.4028235E37F)player.awardStat(net.minecraft.stats.Stats.DAMAGE_TAKEN,Math.round(amount*10));
        }
        // Other viewers retain the normal red hit response. Never send it to a client
        // whose camera is this victim: that packet itself starts client hurtTime/tilt.
        var response=new net.minecraft.network.protocol.game.ClientboundDamageEventPacket(target,source);
        for(var viewer:net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(target))
            if(viewer!=recipient&&viewer!=target&&viewer.getCamera()!=target)viewer.connection.send(response);
        boolean deathPath=target.getHealth()<=0&&net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DEATH.invoker()
                .allowDeath(target,source,amount);
        if(deathPath){
            if(!nativeHit.skywars2$protectFromHole(source)){
                sound(target,nativeHit.skywars2$holeDeathSound());nativeHit.skywars2$holeSecondarySound(source);
                target.die(source);game.items().polymorph().finishDirectDamage(target);
            }
        }else{
            if(target instanceof ServerPlayer)sound(target,nativeHit.skywars2$holeHurtSoundType(source));
            else nativeHit.skywars2$holeHurtSound(source);
            nativeHit.skywars2$holeSecondarySound(source);
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
