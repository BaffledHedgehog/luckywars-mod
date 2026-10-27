package me.skywars2.game;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** Quiver reward: two independent uniform draws from the source's 21 attribute bonuses. */
final class LbcRandomStatLevelup {
    private static final String[] ATTRIBUTES={"armor","armor_toughness","attack_damage","attack_knockback",
            "attack_speed","burning_time","explosion_knockback_resistance","fall_damage_multiplier",
            "jump_strength","knockback_resistance","luck","max_health","movement_efficiency",
            "movement_speed","safe_fall_distance","scale","step_height","block_break_speed",
            "block_interaction_range","entity_interaction_range","sneaking_speed"};
    private static final double[] AMOUNTS={5,3,1.5,.5,1,-.5,.1,-.2,.01,.2,1,.4,.5,.025,4,.5,1,1,2,2,.3};
    private static Identifier id(int draw){return Identifier.parse("minecraft:levelup_random_statup_"+draw);}
    static void apply(ServerPlayer player){for(int draw=1;draw<=2;draw++)applyDraw(player,draw,player.getRandom().nextInt(21));}
    static void applyDraw(ServerPlayer player,int draw,int choice){
        var attribute=player.getAttribute(BuiltInRegistries.ATTRIBUTE.get(Identifier.parse("minecraft:"+ATTRIBUTES[choice])).orElseThrow());
        if(attribute==null)return;
        attribute.removeModifier(id(draw));
        attribute.addTransientModifier(new AttributeModifier(id(draw),AMOUNTS[choice],choice==11
                ?AttributeModifier.Operation.ADD_MULTIPLIED_BASE:AttributeModifier.Operation.ADD_VALUE));
    }
    static void clear(ServerPlayer player){for(var name:ATTRIBUTES){
        var attribute=player.getAttribute(BuiltInRegistries.ATTRIBUTE.get(Identifier.parse("minecraft:"+name)).orElseThrow());
        if(attribute!=null)for(int draw=1;draw<=2;draw++)attribute.removeModifier(id(draw));
    }}
}
