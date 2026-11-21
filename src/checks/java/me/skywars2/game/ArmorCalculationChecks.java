package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;

/** Native CombatRules and LivingEntity armor pipeline, including high armor and singular toughness. */
public final class ArmorCalculationChecks {
    private ArmorCalculationChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Armor calculation: "+why);}
    private static final class Probe extends Cow {
        Probe(net.minecraft.server.level.ServerLevel level){super(EntityTypes.COW,level);}
        float absorb(DamageSource source,float amount){return super.getDamageAfterArmorAbsorb(source,amount);}
    }
    private static void near(float actual,float expected,String why){require(Float.isFinite(actual)&&Math.abs(actual-expected)<=Math.max(1e-5,Math.abs(expected)*1e-5),why+" actual="+actual+" expected="+expected);}
    public static void run(GameController game){
        var victim=new Probe(game.maps().level());var source=victim.damageSources().generic();
        float base=CombatRules.getDamageAfterAbsorb(victim,10,source,40,0);near(base,2,"native40 armor baseline");
        near(CombatRules.getDamageAfterAbsorb(victim,10,source,80,0),base*.5f,"80 armor halves remaining damage");
        near(CombatRules.getDamageAfterAbsorb(victim,10,source,160,0),base*.25f,"160 armor quarters remaining damage");
        float previous=Float.POSITIVE_INFINITY;for(float armor:new float[]{40,80,160,1000,2_000_000_000f}){
            float after=CombatRules.getDamageAfterAbsorb(victim,10,source,armor,0);require(after>=0&&after<previous,"continued finite positive mitigation above40");previous=after;
        }
        for(float armor:new float[]{-100,-1,0,20,40,80})for(float toughness:new float[]{-100,-8.0001f,-8,-7.9999f,0,20})for(float damage:new float[]{0,1,20,1000}){
            float after=CombatRules.getDamageAfterAbsorb(victim,damage,source,armor,toughness);require(Float.isFinite(after)&&after>=0,"defense never creates nonfinite/negative damage: armor="+armor+" toughness="+toughness+" damage="+damage+" result="+after);
        }
        require(CombatRules.getDamageAfterAbsorb(victim,10,source,-10,0)>10,"negative armor amplifies native unarmed damage");
        for(float value:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY}){
            require(Float.isFinite(CombatRules.getDamageAfterAbsorb(victim,10,source,value,-8)),"nonfinite armor cannot corrupt damage");
            require(Float.isFinite(CombatRules.getDamageAfterAbsorb(victim,value,source,80,-8)),"nonfinite incoming value cannot corrupt armor result");
            require(Float.isFinite(CombatRules.getDamageAfterAbsorb(victim,10,source,80,value)),"nonfinite toughness cannot corrupt damage");
        }
        victim.getAttribute(Attributes.ARMOR).setBaseValue(80);victim.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(-8);
        // Native generic is explicitly in bypasses_armor; use a real mob-attack source for this pipeline.
        var attacker=new Cow(EntityTypes.COW,game.maps().level());var armoredSource=victim.damageSources().mobAttack(attacker);
        require(!armoredSource.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR),"native mob attack must enter the armor branch");
        near(victim.absorb(armoredSource,10),CombatRules.getDamageAfterAbsorb(victim,10,armoredSource,80,-8),"real LivingEntity armor attribute pipeline");
        victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);victim.setHealth(1000);
        require(victim.hurtServer(game.maps().level(),armoredSource,10)&&Float.isFinite(victim.getHealth())&&victim.getHealth()<1000,"native accepted high armor/negative toughness hurt keeps finite HP");
        victim.setInvulnerableTime(0);victim.damageCooldownTime=0;victim.hurtServer(game.maps().level(),armoredSource,Float.NaN);require(Float.isFinite(victim.getHealth()),"native NaN armor path leaves finite HP");
        game.items().indirectKills().clear(victim);victim.discard();attacker.discard();
        SkyWars2.LOGGER.info("[check] PASS armor-calculation");
    }
}
