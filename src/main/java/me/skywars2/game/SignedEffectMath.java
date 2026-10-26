package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** Signed levels are widened before addition; native low levels keep their tick cadence. */
public final class SignedEffectMath {
    private SignedEffectMath() { }
    public static long level(int amplifier) { return (long)amplifier + 1; }
    public static long level(LivingEntity entity, Holder<MobEffect> effect) {
        var instance=entity.getEffect(effect); return instance==null?0:level(instance.getAmplifier());
    }
    public static float finite(double value) {
        if(Double.isNaN(value))return 0;
        return (float)Math.max(-Float.MAX_VALUE,Math.min(Float.MAX_VALUE,value));
    }
    public static double fatigue(int amplifier) {
        // exp is bounded before conversion; enormous positive fatigue reaches zero.
        return Math.exp(Math.max(-745,Math.min(80,Math.log(.3)*level(amplifier))));
    }
    public static int interval(int amplifier,int base) {
        long magnitude=Math.abs(level(amplifier));
        if(magnitude==0)return Integer.MAX_VALUE;
        return magnitude>31?1:Math.max(1,base>>(int)(magnitude-1));
    }
    public static float periodicAmount(int amplifier,int base) {
        int firstEveryTick=32-Integer.numberOfLeadingZeros(base);
        return finite(Math.max(1,(double)Math.abs(level(amplifier))/firstEveryTick));
    }
    public static boolean due(int ticks,int amplifier,int base) {
        return level(amplifier)!=0 && ticks%interval(amplifier,base)==0;
    }
    public static boolean damage(ServerLevel world,LivingEntity target,Holder<MobEffect> effect,DamageSource source,float amount) {
        if(!(amount>0)||LbcGreed.active(target))return false;
        var game=SkyWars2.game();
        return game==null?target.hurtServer(world,source,amount):game.items().indirectKills().statusDamage(target,effect,
                ()->target.hurtServer(world,source,amount));
    }
    public static boolean periodic(ServerLevel world,LivingEntity target,Holder<MobEffect> effect,int amplifier) {
        long signed=level(amplifier);if(signed==0||LbcGreed.active(target))return true;
        boolean regeneration=effect.is(MobEffects.REGENERATION);
        int base=regeneration?50:effect.is(MobEffects.POISON)?25:40;
        float amount=periodicAmount(amplifier,base);
        boolean healing=regeneration?signed>0:signed<0;
        if(healing) { if(ShadowAbsorption.needsHealing(target))target.heal(amount); }
        else {
            if(effect.is(MobEffects.POISON))amount=Math.min(amount,Math.max(0,target.getHealth()-1));
            damage(world,target,effect,effect.is(MobEffects.WITHER)?target.damageSources().wither():target.damageSources().magic(),amount);
        }
        return true;
    }
    public static void instant(ServerLevel world,Entity source,Entity owner,LivingEntity target,int amplifier,double scale,boolean harm) {
        long signed=level(amplifier);
        if(signed==0||!(scale>0)||LbcGreed.active(target)&&owner!=target&&source!=target)return;
        boolean healing=(harm==target.isInvertedHealAndHarm())==(signed>0);
        long exponent=Math.abs(signed)-1;
        double base=healing?4:6;
        float amount=finite(scale*base*Math.scalb(1, (int)Math.min(128,exponent)));
        // Splash effects historically round to integer HP; direct /effect does not.
        if(source!=null)amount=finite(Math.floor((double)amount+.5));
        if(healing)target.heal(amount);
        else damage(world,target,harm?MobEffects.INSTANT_DAMAGE:MobEffects.INSTANT_HEALTH,
                source==null?target.damageSources().magic():target.damageSources().indirectMagic(source,owner),amount);
    }
    public static void hunger(LivingEntity entity,int amplifier) {
        if(!(entity instanceof Player player)||LbcGreed.active(entity))return;
        float rate=finite(.005*level(amplifier));
        if(rate>=0)player.causeFoodExhaustion(rate);
        else ((SignedFoodAccess)player.getFoodData()).skywars2$restoreExhaustion(-rate);
    }
    public static void saturation(LivingEntity entity,int amplifier) {
        if(!(entity instanceof Player player)||LbcGreed.active(entity))return;
        int amount=(int)Math.max(-20,Math.min(20,level(amplifier)));
        var food=player.getFoodData();
        food.setFoodLevel(Math.max(0,Math.min(20,food.getFoodLevel()+amount)));
        food.setSaturation(Math.max(0,Math.min(food.getFoodLevel(),food.getSaturationLevel()+2*amount)));
    }
    /** Closed-form height for v[n+1]=(v[n]-g)*drag, avoiding a loop over huge levels. */
    public static double jumpHeight(double velocity,double gravity,double drag) {
        if(!(velocity>0))return 0;
        drag=Math.max(0,Math.min(1,drag));
        if(drag==0)return velocity;
        if(gravity==0&&drag<1)return Math.min(Float.MAX_VALUE,velocity/(1-drag));
        if(!(gravity>0))return Float.MAX_VALUE;
        if(drag>=1) {
            double ticks=Math.ceil(velocity/gravity);
            return Math.min(Float.MAX_VALUE,ticks*velocity-gravity*ticks*(ticks-1)/2);
        }
        double terminal=gravity*drag/(1-drag);
        double ticks=Math.max(1,Math.ceil(Math.log(terminal/(velocity+terminal))/Math.log(drag)));
        double geometric=-Math.expm1(ticks*Math.log(drag))/(1-drag);
        return Math.max(0,Math.min(Float.MAX_VALUE,(velocity+terminal)*geometric-terminal*ticks));
    }
    public static float safeJumpFallDistance(LivingEntity entity,float nativeDistance) {
        var jump=entity.getEffect(MobEffects.JUMP_BOOST);if(jump==null||level(jump.getAmplifier())<=0)return nativeDistance;
        double l=level(jump.getAmplifier());
        double velocity=Math.max(0,entity.getAttributeValue(Attributes.JUMP_STRENGTH)+.1*l);
        double drag=Math.max(0,Math.min(1,1.0f-(1.0f-.98f)*(float)entity.getAttributeValue(Attributes.AIR_DRAG_MODIFIER)));
        double nativeEffectBonus=l;
        // Keep the ordinary safe-drop allowance, then cover the actual jump plus one block below takeoff.
        return finite(Math.max(nativeDistance,nativeDistance-nativeEffectBonus+jumpHeight(velocity,entity.getAttributeValue(Attributes.GRAVITY),drag)+1));
    }
    public interface SignedFoodAccess { void skywars2$restoreExhaustion(float amount); }

    private static final net.minecraft.resources.Identifier DOLPHIN_SPEED=net.minecraft.resources.Identifier.fromNamespaceAndPath("skywars2","dolphin_speed");
    private static final net.minecraft.resources.Identifier DOLPHIN_WATER=net.minecraft.resources.Identifier.fromNamespaceAndPath("skywars2","dolphin_water");
    private static final net.minecraft.resources.Identifier NEGATIVE_JUMP=net.minecraft.resources.Identifier.fromNamespaceAndPath("skywars2","negative_jump");
    public static void dolphin(LivingEntity entity) {
        if(entity.level().isClientSide())return;
        long jump=level(entity,MobEffects.JUMP_BOOST);
        dolphinModifier(entity,Attributes.JUMP_STRENGTH,NEGATIVE_JUMP,entity instanceof Player&&jump<0?.1*jump:0,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE);
        long level=entity.isInWater()?level(entity,MobEffects.DOLPHINS_GRACE):0;
        // Native attributes are simulated by the unmodified client too, so no velocity correction is needed.
        dolphinModifier(entity,Attributes.MOVEMENT_SPEED,DOLPHIN_SPEED,level>1?.2*(level-1):0,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        dolphinModifier(entity,Attributes.WATER_MOVEMENT_EFFICIENCY,DOLPHIN_WATER,level>1?1-1.0/level:0,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE);
    }
    private static void dolphinModifier(LivingEntity entity,Holder<net.minecraft.world.entity.ai.attributes.Attribute> type,
            net.minecraft.resources.Identifier id,double amount,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation operation) {
        var attribute=entity.getAttribute(type);if(attribute==null)return;
        var old=attribute.getModifier(id);
        if(amount==0){if(old!=null)attribute.removeModifier(id);return;}
        if(old!=null&&old.amount()==amount)return;
        if(old!=null)attribute.removeModifier(id);
        attribute.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(id,amount,operation));
    }
}
