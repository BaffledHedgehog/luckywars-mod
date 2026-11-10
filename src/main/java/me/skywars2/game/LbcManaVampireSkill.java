package me.skywars2.game;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;

/** Accepted native hits only. No nearby HurtTime scan and no independent damage wrapper. */
public final class LbcManaVampireSkill {
    private final GameController game;
    private final Predicate<ServerPlayer> ownerAdmission;
    private final BiPredicate<ServerPlayer,LivingEntity> targetAdmission;
    private boolean damaging;

    public LbcManaVampireSkill(GameController game,Predicate<ServerPlayer> ownerAdmission,
            BiPredicate<ServerPlayer,LivingEntity> targetAdmission){
        this.game=game;this.ownerAdmission=Objects.requireNonNull(ownerAdmission);
        this.targetAdmission=Objects.requireNonNull(targetAdmission);
    }

    private boolean admitted(ServerPlayer owner){
        return game.lbc().hasSkill(owner,29)&&ownerAdmission.test(owner)
                &&game.server().tickRateManager().runsNormally()&&!game.server().tickRateManager().isFrozen()
                &&!game.items().wands().inputTimeStopped(owner)&&!game.items().timeStop().frozen(owner)
                &&!game.items().timeStop().stopped(owner.level());
    }

    /** Actual current owner, never stale kill credit from a previous attack. */
    static ServerPlayer owner(GameController game,DamageSource source){
        var cause=game.items().indirectKills().babylonCause(source);
        if(cause!=null)return game.server().getPlayerList().getPlayer(cause.player());
        var owner=LbcIndirectKills.playerSource(source.getEntity());
        return owner!=null?owner:LbcIndirectKills.playerSource(source.getDirectEntity());
    }

    public void damaged(LivingEntity victim,DamageSource source){
        if(damaging)return;
        var owner=owner(game,source);
        if(owner==null||owner==victim||!eligibleSource(game,source,owner)||!admitted(owner)||owner.level()!=victim.level()
                ||!LbcDimensionItems.ordinary(victim.level())||!targetAdmission.test(owner,victim))return;
        if(victim instanceof ServerPlayer player){
            var mana=game.items().mana();
            // Source checks the pre-reward value. The final +1000 may overshoot by999.
            boolean heal=(long)mana.get(owner)>=(long)mana.maximum(owner)+5000;
            mana.set(player,(int)Math.max(Integer.MIN_VALUE,(long)mana.get(player)-1000));
            if(heal)owner.addEffect(ItemEffectLevels.create(MobEffects.INSTANT_HEALTH,1,0),owner);
            else mana.set(owner,(int)Math.min(Integer.MAX_VALUE,(long)mana.get(owner)+1000));
        }else if(game.items().magic().magicalMob(victim)&&victim.isAlive()&&!victim.isRemoved()){
            damaging=true;
            try{victim.hurtServer(owner.level(),owner.damageSources().indirectMagic(owner,owner),15);}
            finally{damaging=false;}
        }
    }
    static boolean eligibleSource(GameController game,DamageSource source,ServerPlayer owner){
        return LbcDamage.isMeleeAttack(source,owner)||LbcTerra.swordProjectile(source.getDirectEntity(),owner)
                ||LbcChargedSwords.swordProjectile(source.getDirectEntity(),owner)
                ||game.items().pendingCombat().swordProjectile(source.getDirectEntity(),owner);
    }
}
