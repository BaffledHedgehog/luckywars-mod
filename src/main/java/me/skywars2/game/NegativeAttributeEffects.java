package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** Explicit user semantics for signed combat/fall/oxygen attributes; never fabricates damage. */
public final class NegativeAttributeEffects {
    private NegativeAttributeEffects(){}
    private static boolean active(LivingEntity entity){
        var game=SkyWars2.game();
        return game!=null&&game.playing()&&entity.level()==game.maps().level()
                &&game.items().wands().validRoleTarget(entity,false)
                &&(!(entity instanceof ServerPlayer player)||game.allowDamage(player));
    }
    private static boolean paused(LivingEntity entity){
        var game=SkyWars2.game();
        return game==null||!(entity.level() instanceof ServerLevel level)||game.items().timeStop().stopped(level)
                ||game.items().timeStop().frozen(entity)||entity.entityTags().contains("time_stopped")
                ||game.items().frost().isFrozen(entity);
    }
    private static boolean canSupport(LivingEntity attacker,LivingEntity target){
        var game=SkyWars2.game();
        if(!active(attacker)||!active(target)||attacker==target||attacker.level()!=target.level()
                ||paused(attacker)||paused(target)||LbcGreed.active(target)||game.items().actionsBlocked(attacker)
                ||!target.isAttackable()||!attacker.level().getWorldBorder().isWithinBounds(target.blockPosition()))return false;
        // The real native attack selected this target. Retain native reach and LOS,
        // including direct server callers, without filtering allies from support.
        if(attacker instanceof Player player)return player.isWithinAttackRange(player.getMainHandItem(),target.getBoundingBox(),0)
                &&!player.cannotAttackWithItem(player.getMainHandItem(),0)&&player.hasLineOfSight(target);
        return attacker instanceof Mob mob&&mob.isWithinMeleeAttackRange(target)&&mob.hasLineOfSight(target);
    }
    private static boolean heal(LivingEntity target,double amount){
        double missing=(double)target.getMaxHealth()-target.getHealth();
        if(!(amount>0)||Double.isNaN(amount)||!(missing>0))return false;
        float before=target.getHealth();
        target.heal((float)Math.min(Math.min(amount,missing),Float.MAX_VALUE));
        return target.getHealth()>before;
    }
    public static boolean negativeAttack(LivingEntity attacker,float amount){
        return attacker.getAttributeValue(Attributes.ATTACK_DAMAGE)<0&&(amount<0||Float.isNaN(amount));
    }
    /** Called after native/Fabric admission, before native onAttack resets strength. */
    public static boolean playerAttack(Player player,Entity entity,float scaledBase,float magicBoost,float strength){
        if(!(player instanceof ServerPlayer)||!(entity instanceof LivingEntity target)
                ||!(player.getAttributeValue(Attributes.ATTACK_DAMAGE)<0))return false;
        var weapon=player.getWeaponItem();var source=weapon.getDamageSource(player);
        double base=scaledBase;
        if(base==Double.NEGATIVE_INFINITY&&Float.isNaN(magicBoost))magicBoost=0;
        base+=weapon.getItem().getAttackDamageBonus(entity,scaledBase,source);
        if(strength>.9f&&player.fallDistance>0&&!player.onGround()&&!player.onClimbable()&&!player.isInWater()
                &&!player.isMobilityRestricted()&&!player.isPassenger()&&!player.isSprinting())base*=1.5;
        double amount=base+magicBoost;
        if(!(amount<0))return false;
        // No hurt(), damage callbacks, armor wear, hostile credit or knockback.
        boolean accepted=canSupport(player,target);
        player.onAttack();
        if(accepted)heal(target,-amount);
        player.postPiercingAttack();
        return true;
    }
    public static boolean mobAttack(Mob attacker,Entity entity,float amount){
        double healing=Float.isNaN(amount)?-attacker.getAttributeValue(Attributes.ATTACK_DAMAGE):-(double)amount;
        return entity instanceof LivingEntity target&&canSupport(attacker,target)&&heal(target,healing);
    }
    /** Fallback only for an actual signed, direct native melee call, never projectiles/effects. */
    public static boolean directMelee(LivingEntity target,DamageSource source,float amount){
        if(!(amount<0)||!(source.getEntity() instanceof LivingEntity attacker)
                ||source.getDirectEntity()!=attacker||attacker.getAttribute(Attributes.ATTACK_DAMAGE)==null
                ||!(attacker.getAttributeValue(Attributes.ATTACK_DAMAGE)<0)
                ||!(source.is(DamageTypes.PLAYER_ATTACK)||source.is(DamageTypes.MOB_ATTACK)
                    ||source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)||source.is(DamageTypes.STING)||source.is(DamageTypes.SPEAR)))return false;
        if(canSupport(attacker,target))heal(target,-(double)amount);
        // Claim even an inadmissible/full-health signed attempt: no fake hurt,
        // hostile credit, on-hurt statuses or native post-hit consequences.
        return true;
    }
    /** Native effective fall distance is supplied after impulse/grace adjustment. */
    public static int fallDamage(LivingEntity target,double distance,float modifier){
        if(target.is(EntityTypeTags.FALL_DAMAGE_IMMUNE))return 0;
        double power=distance+1e-6-SignedEffectMath.safeJumpFallDistance(target,SignedEffectMath.finite(target.getAttributeValue(Attributes.SAFE_FALL_DISTANCE)));
        if(!(power>0)||!(modifier>0))return 0;
        double signed=Math.floor(power*modifier*target.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER));
        if(signed<0&&active(target)&&!paused(target)&&!LbcGreed.active(target))heal(target,-signed);
        return 0;
    }
    /** Bernoulli remainder preserves fractional negative bonuses without integer overflow. */
    public static int oxygen(LivingEntity entity,int current,double bonus){
        double loss=1-bonus;double integer=Math.floor(loss);
        if(entity.getRandom().nextDouble()<loss-integer)integer++;
        return (int)Math.max(Integer.MIN_VALUE,Math.min(Integer.MAX_VALUE,(double)current-integer));
    }
}
