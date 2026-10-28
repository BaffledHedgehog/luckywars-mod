package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/** Checks the live native registry and native AttributeInstance calculation, without a replacement registry. */
public final class AttributePolicyChecks {
    private AttributePolicyChecks(){}
    private static final Set<String> UPPER=Set.of("movement_speed","flying_speed","jump_strength","step_height","sneaking_speed","friction_modifier","air_drag_modifier","bounciness","max_health","max_absorption","burning_time","name_tag_distance","waypoint_transmit_range","waypoint_receive_range");
    private static final Set<String> BOTH=Set.of("gravity","scale","movement_efficiency","armor","armor_toughness","attack_damage","attack_speed","attack_knockback","knockback_resistance","sweeping_damage_ratio","fall_damage_multiplier","safe_fall_distance","oxygen_bonus","block_break_speed","mining_efficiency","submerged_mining_speed","luck","camera_distance");
    private static final Set<String> NATIVE=Set.of("water_movement_efficiency","spawn_reinforcements","block_interaction_range","entity_interaction_range","follow_range","tempt_range","below_name_distance");
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Attribute policy: "+why);}
    public static void run(GameController game){
        int checked=0;
        try{
            var min=RangedAttribute.class.getDeclaredField("minValue");min.setAccessible(true);var max=RangedAttribute.class.getDeclaredField("maxValue");max.setAccessible(true);
            for(var attribute:BuiltInRegistries.ATTRIBUTE){
                if(!(attribute instanceof RangedAttribute ranged))continue;var id=BuiltInRegistries.ATTRIBUTE.getKey(attribute);double lo=min.getDouble(ranged),hi=max.getDouble(ranged);
                String name=id.getPath();boolean vanilla=id.getNamespace().equals("minecraft");double expectedLo=lo,expectedHi=hi;
                if(vanilla&&UPPER.contains(name)){expectedHi=Double.POSITIVE_INFINITY;checked++;}
                else if(vanilla&&BOTH.contains(name)){expectedLo=Double.NEGATIVE_INFINITY;expectedHi=Double.POSITIVE_INFINITY;checked++;}
                else if(vanilla&&name.equals("explosion_knockback_resistance")){expectedLo=Double.NEGATIVE_INFINITY;expectedHi=1;checked++;}
                else if(vanilla&&NATIVE.contains(name))checked++;
                require(ranged.getMinValue()==expectedLo&&ranged.getMaxValue()==expectedHi,"exact registry bounds "+id);
                for(double value:new double[]{-2_000_000_000d,-1,0,1,2_000_000_000d,Double.MAX_VALUE,-Double.MAX_VALUE})
                    require(ranged.sanitizeValue(value)==Math.max(expectedLo,Math.min(expectedHi,value)),"finite sanitize "+id+" value="+value);
                require(ranged.sanitizeValue(Double.NaN)==lo&&ranged.sanitizeValue(Double.POSITIVE_INFINITY)==hi&&ranged.sanitizeValue(Double.NEGATIVE_INFINITY)==lo,"native nonfinite fallback "+id);
            }
        }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        require(checked==40,"all40 explicitly classified native attributes checked: "+checked);
        var unknown=new RangedAttribute("attribute.name.movement_speed",1,-3,7);
        require(unknown.getMinValue()==-3&&unknown.getMaxValue()==7&&unknown.sanitizeValue(-100)==-3&&unknown.sanitizeValue(100)==7,"unregistered/description-spoofed attribute retains native contract");
        var cow=new net.minecraft.world.entity.animal.cow.Cow(net.minecraft.world.entity.EntityTypes.COW,game.maps().level());
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2_000_000_000d);cow.setHealth(2_000_000_000f);
        require(cow.getMaxHealth()==2_000_000_000f&&cow.getHealth()==2_000_000_000f,"native source2b HP remains uncapped");
        cow.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(-1);require(cow.getAttributeValue(Attributes.MOVEMENT_SPEED)==0,"native speed minimum remains zero");
        cow.getAttribute(Attributes.GRAVITY).setBaseValue(-2);require(cow.getAttributeValue(Attributes.GRAVITY)==-2,"negative gravity remains unbounded");
        cow.getAttribute(Attributes.SCALE).setBaseValue(-2);require(cow.getAttributeValue(Attributes.SCALE)==-2,"negative scale remains unbounded");
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(owner!=null,"native player attack speed probe");
        var speed=owner.getAttribute(Attributes.ATTACK_SPEED);double saved=speed.getBaseValue();var modifiers=List.copyOf(speed.getModifiers());var permanent=Set.copyOf(speed.getPermanentModifiers());
        try{speed.removeModifiers();speed.setBaseValue(0);require(owner.getCurrentItemAttackStrengthDelay()==Float.POSITIVE_INFINITY,"zero attack speed uses native floating division");speed.setBaseValue(-4);require(owner.getCurrentItemAttackStrengthDelay()==-5,"negative attack speed remains finite without integer division failure");}
        finally{speed.removeModifiers();speed.setBaseValue(saved);for(var modifier:modifiers)if(permanent.contains(modifier))speed.addPermanentModifier(modifier);else speed.addTransientModifier(modifier);}
        SkyWars2.LOGGER.info("[check] PASS attribute-policy");
    }
}
