package me.skywars2.game;

import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attribute;

/** Explicit vanilla-attribute endpoint policy; unrelated and modded attributes retain their own contract. */
public final class AttributePolicy {
    private AttributePolicy(){}
    private static final Set<String> UPPER_ONLY=Set.of("movement_speed","flying_speed","jump_strength","step_height",
            "sneaking_speed","friction_modifier","air_drag_modifier","bounciness","max_health","max_absorption",
            "burning_time","name_tag_distance","waypoint_transmit_range","waypoint_receive_range");
    private static final Set<String> BOTH=Set.of("gravity","movement_efficiency","armor","armor_toughness",
            "attack_damage","attack_speed","attack_knockback","knockback_resistance","sweeping_damage_ratio","fall_damage_multiplier",
            "safe_fall_distance","oxygen_bonus","block_break_speed","mining_efficiency","submerged_mining_speed","luck","camera_distance");
    public record Bounds(double minimum,double maximum){}
    public static Bounds bounds(Attribute attribute,double nativeMinimum,double nativeMaximum){
        if(!VanillaExpansionRules.enabled())return new Bounds(nativeMinimum,nativeMaximum);
        var id=BuiltInRegistries.ATTRIBUTE.getKey(attribute);
        if(id==null||!id.getNamespace().equals("minecraft"))return new Bounds(nativeMinimum,nativeMaximum);
        String path=id.getPath();
        if(UPPER_ONLY.contains(path))return new Bounds(nativeMinimum,Double.POSITIVE_INFINITY);
        if(BOTH.contains(path))return new Bounds(Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY);
        if(path.equals("explosion_knockback_resistance"))return new Bounds(Double.NEGATIVE_INFINITY,1);
        return new Bounds(nativeMinimum,nativeMaximum);
    }
}
