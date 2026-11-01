package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;

/** Bounded native consumption rewards; source functions are represented as typed definitions. */
public final class LbcPills {
    public static final int OUTPUT_COUNT=144;
    public static final Item PILL=registerPill();
    public static final Item CERTIFICATE=registerCertificate();
    public record AttributeChange(Holder<Attribute> attribute,int inputScale,int step,double outputScale) {}
    public record Effect(Holder<MobEffect> type,int seconds,int amplifier,boolean hidden) {}
    public record Counter(String key,int amount) {}
    public record Definition(int id,String key,AttributeChange attribute,List<Effect> effects,
                             List<Counter> counters,List<String> tags,boolean paralysis,String subtitle,String sound) {}
    private static final List<Definition> DEFINITIONS=List.of(
        new Definition(0,"max_health_down",new AttributeChange(Attributes.MAX_HEALTH,1,-1,1.0),List.of(),List.of(),List.of(),false,"pill_max_health_down","minecraft:lbcsounds.pill_bad"),
        new Definition(1,"attack_damage_down",new AttributeChange(Attributes.ATTACK_DAMAGE,1,-1,1.0),List.of(),List.of(),List.of(),false,"pill_attack_damage_down","minecraft:lbcsounds.pill_bad"),
        new Definition(2,"armor_down",new AttributeChange(Attributes.ARMOR,1,-1,1.0),List.of(),List.of(),List.of(),false,"pill_armor_down","minecraft:lbcsounds.pill_bad"),
        new Definition(3,"armor_toughness_down",new AttributeChange(Attributes.ARMOR_TOUGHNESS,1,-1,1.0),List.of(),List.of(),List.of(),false,"pill_armor_toughness_down","minecraft:lbcsounds.pill_bad"),
        new Definition(4,"attack_speed_down",new AttributeChange(Attributes.ATTACK_SPEED,5,-1,0.2),List.of(),List.of(),List.of(),false,"pill_attack_speed_down","minecraft:lbcsounds.pill_bad"),
        new Definition(5,"knockback_resistance_down",new AttributeChange(Attributes.KNOCKBACK_RESISTANCE,10,-10,0.1),List.of(),List.of(),List.of(),false,"pill_knockback_resistance_down","minecraft:lbcsounds.pill_bad"),
        new Definition(6,"luck_down",new AttributeChange(Attributes.LUCK,1,-1,1.0),List.of(),List.of(),List.of(),false,"pill_luck_down","minecraft:lbcsounds.pill_bad"),
        new Definition(7,"movement_speed_down",new AttributeChange(Attributes.MOVEMENT_SPEED,100000,-2000,1e-05),List.of(),List.of(),List.of(),false,"pill_movement_speed_down","minecraft:lbcsounds.pill_bad"),
        new Definition(8,"attack_knockback_down",new AttributeChange(Attributes.ATTACK_KNOCKBACK,10,-1,0.1),List.of(),List.of(),List.of(),false,"pill_attack_knockback_down","minecraft:lbcsounds.pill_bad"),
        new Definition(9,"burning_time_down",new AttributeChange(Attributes.BURNING_TIME,10,-1,0.1),List.of(),List.of(),List.of(),false,"pill_burning_time_down","minecraft:lbcsounds.pill_good"),
        new Definition(10,"explosion_knockback_resistance_down",new AttributeChange(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE,10,-1,0.1),List.of(),List.of(),List.of(),false,"pill_explosion_knockback_resistance_down","minecraft:lbcsounds.pill_bad"),
        new Definition(11,"fall_damage_multiplier_down",new AttributeChange(Attributes.FALL_DAMAGE_MULTIPLIER,10,-1,0.1),List.of(),List.of(),List.of(),false,"pill_fall_damage_multiplier_down","minecraft:lbcsounds.pill_good"),
        new Definition(12,"gravity_down",new AttributeChange(Attributes.GRAVITY,100,-1,0.01),List.of(),List.of(),List.of(),false,"pill_gravity_down","minecraft:lbcsounds.pill_good"),
        new Definition(13,"jump_strength_down",new AttributeChange(Attributes.JUMP_STRENGTH,10,-1,0.1),List.of(),List.of(),List.of(),false,"pill_jump_strength_down","minecraft:lbcsounds.pill_bad"),
        new Definition(14,"movement_efficiency_down",new AttributeChange(Attributes.MOVEMENT_EFFICIENCY,10,-1,0.1),List.of(),List.of(),List.of(),false,"pill_movement_efficiency_down","minecraft:lbcsounds.pill_bad"),
        new Definition(15,"oxygen_bonus_down",new AttributeChange(Attributes.OXYGEN_BONUS,10,-1,0.1),List.of(),List.of(),List.of(),false,"pill_oxygen_bonus_down","minecraft:lbcsounds.pill_bad"),
        new Definition(16,"safe_fall_distance_down",new AttributeChange(Attributes.SAFE_FALL_DISTANCE,5,-1,0.2),List.of(),List.of(),List.of(),false,"pill_safe_fall_distance_down","minecraft:lbcsounds.pill_bad"),
        new Definition(17,"scale_down",new AttributeChange(Attributes.SCALE,10,-1,0.1),List.of(),List.of(),List.of(),false,"pill_scale_down","minecraft:lbcsounds.pill_good"),
        new Definition(18,"step_height_down",new AttributeChange(Attributes.STEP_HEIGHT,10,-1,0.1),List.of(),List.of(),List.of(),false,"pill_step_height_down","minecraft:lbcsounds.pill_bad"),
        new Definition(19,"water_movement_efficiency_down",new AttributeChange(Attributes.WATER_MOVEMENT_EFFICIENCY,10,-1,0.1),List.of(),List.of(),List.of(),false,"pill_water_movement_efficiency_down","minecraft:lbcsounds.pill_bad"),
        new Definition(20,"block_break_speed_down",new AttributeChange(Attributes.BLOCK_BREAK_SPEED,10,-1,0.1),List.of(),List.of(),List.of(),false,"pill_block_break_speed_down","minecraft:lbcsounds.pill_bad"),
        new Definition(21,"block_interaction_range_down",new AttributeChange(Attributes.BLOCK_INTERACTION_RANGE,5,-1,0.2),List.of(),List.of(),List.of(),false,"pill_block_interaction_range_down","minecraft:lbcsounds.pill_bad"),
        new Definition(22,"entity_interaction_range_down",new AttributeChange(Attributes.ENTITY_INTERACTION_RANGE,5,-1,0.2),List.of(),List.of(),List.of(),false,"pill_entity_interaction_range_down","minecraft:lbcsounds.pill_bad"),
        new Definition(23,"mining_efficiency_down",new AttributeChange(Attributes.MINING_EFFICIENCY,3,-1,0.333333333),List.of(),List.of(),List.of(),false,"pill_mining_efficiency_down","minecraft:lbcsounds.pill_bad"),
        new Definition(24,"sneaking_speed_down",new AttributeChange(Attributes.SNEAKING_SPEED,10,-1,0.1),List.of(),List.of(),List.of(),false,"pill_sneaking_speed_down","minecraft:lbcsounds.pill_bad"),
        new Definition(25,"submerged_mining_speed_down",new AttributeChange(Attributes.SUBMERGED_MINING_SPEED,20,-1,0.05),List.of(),List.of(),List.of(),false,"pill_submerged_mining_speed_down","minecraft:lbcsounds.pill_bad"),
        new Definition(26,"sweeping_damage_ratio_down",new AttributeChange(Attributes.SWEEPING_DAMAGE_RATIO,20,-1,0.05),List.of(),List.of(),List.of(),false,"pill_sweeping_damage_ratio_down","minecraft:lbcsounds.pill_bad"),
        new Definition(27,"max_health_up",new AttributeChange(Attributes.MAX_HEALTH,1,2,1.0),List.of(),List.of(),List.of(),false,"pill_max_health_up","minecraft:lbcsounds.pill_good"),
        new Definition(28,"attack_damage_up",new AttributeChange(Attributes.ATTACK_DAMAGE,1,1,1.0),List.of(),List.of(),List.of(),false,"pill_attack_damage_up","minecraft:lbcsounds.pill_good"),
        new Definition(29,"armor_up",new AttributeChange(Attributes.ARMOR,1,2,1.0),List.of(),List.of(),List.of(),false,"pill_armor_up","minecraft:lbcsounds.pill_good"),
        new Definition(30,"armor_toughness_up",new AttributeChange(Attributes.ARMOR_TOUGHNESS,1,1,1.0),List.of(),List.of(),List.of(),false,"pill_armor_toughness_up","minecraft:lbcsounds.pill_good"),
        new Definition(31,"attack_speed_up",new AttributeChange(Attributes.ATTACK_SPEED,5,1,0.2),List.of(),List.of(),List.of(),false,"pill_attack_speed_up","minecraft:lbcsounds.pill_good"),
        new Definition(32,"knockback_resistance_up",new AttributeChange(Attributes.KNOCKBACK_RESISTANCE,10,10,0.1),List.of(),List.of(),List.of(),false,"pill_knockback_resistance_up","minecraft:lbcsounds.pill_good"),
        new Definition(33,"luck_up",new AttributeChange(Attributes.LUCK,1,1,1.0),List.of(),List.of(),List.of(),false,"pill_luck_up","minecraft:lbcsounds.pill_good"),
        new Definition(34,"movement_speed_up",new AttributeChange(Attributes.MOVEMENT_SPEED,100000,2000,1e-05),List.of(),List.of(),List.of(),false,"pill_movement_speed_up","minecraft:lbcsounds.pill_good"),
        new Definition(35,"attack_knockback_up",new AttributeChange(Attributes.ATTACK_KNOCKBACK,10,1,0.1),List.of(),List.of(),List.of(),false,"pill_attack_knockback_up","minecraft:lbcsounds.pill_good"),
        new Definition(36,"burning_time_up",new AttributeChange(Attributes.BURNING_TIME,10,1,0.1),List.of(),List.of(),List.of(),false,"pill_burning_time_up","minecraft:lbcsounds.pill_bad"),
        new Definition(37,"explosion_knockback_resistance_up",new AttributeChange(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE,10,1,0.1),List.of(),List.of(),List.of(),false,"pill_explosion_knockback_resistance_up","minecraft:lbcsounds.pill_good"),
        new Definition(38,"fall_damage_multiplier_up",new AttributeChange(Attributes.FALL_DAMAGE_MULTIPLIER,10,1,0.1),List.of(),List.of(),List.of(),false,"pill_fall_damage_multiplier_up","minecraft:lbcsounds.pill_bad"),
        new Definition(39,"gravity_up",new AttributeChange(Attributes.GRAVITY,100,1,0.01),List.of(),List.of(),List.of(),false,"pill_gravity_up","minecraft:lbcsounds.pill_bad"),
        new Definition(40,"jump_strength_up",new AttributeChange(Attributes.JUMP_STRENGTH,10,1,0.1),List.of(),List.of(),List.of(),false,"pill_jump_strength_up","minecraft:lbcsounds.pill_good"),
        new Definition(41,"movement_efficiency_up",new AttributeChange(Attributes.MOVEMENT_EFFICIENCY,10,1,0.1),List.of(),List.of(),List.of(),false,"pill_movement_efficiency_up","minecraft:lbcsounds.pill_good"),
        new Definition(42,"oxygen_bonus_up",new AttributeChange(Attributes.OXYGEN_BONUS,10,1,0.1),List.of(),List.of(),List.of(),false,"pill_oxygen_bonus_up","minecraft:lbcsounds.pill_good"),
        new Definition(43,"safe_fall_distance_up",new AttributeChange(Attributes.SAFE_FALL_DISTANCE,5,1,0.2),List.of(),List.of(),List.of(),false,"pill_safe_fall_distance_up","minecraft:lbcsounds.pill_good"),
        new Definition(44,"scale_up",new AttributeChange(Attributes.SCALE,10,1,0.1),List.of(),List.of(),List.of(),false,"pill_scale_up","minecraft:lbcsounds.pill_bad"),
        new Definition(45,"step_height_up",new AttributeChange(Attributes.STEP_HEIGHT,10,1,0.1),List.of(),List.of(),List.of(),false,"pill_step_height_up","minecraft:lbcsounds.pill_good"),
        new Definition(46,"water_movement_efficiency_up",new AttributeChange(Attributes.WATER_MOVEMENT_EFFICIENCY,10,1,0.1),List.of(),List.of(),List.of(),false,"pill_water_movement_efficiency_up","minecraft:lbcsounds.pill_good"),
        new Definition(47,"block_break_speed_up",new AttributeChange(Attributes.BLOCK_BREAK_SPEED,10,1,0.1),List.of(),List.of(),List.of(),false,"pill_block_break_speed_up","minecraft:lbcsounds.pill_good"),
        new Definition(48,"block_interaction_range_up",new AttributeChange(Attributes.BLOCK_INTERACTION_RANGE,5,1,0.2),List.of(),List.of(),List.of(),false,"pill_block_interaction_range_up","minecraft:lbcsounds.pill_good"),
        new Definition(49,"entity_interaction_range_up",new AttributeChange(Attributes.ENTITY_INTERACTION_RANGE,5,1,0.2),List.of(),List.of(),List.of(),false,"pill_entity_interaction_range_up","minecraft:lbcsounds.pill_good"),
        new Definition(50,"mining_efficiency_up",new AttributeChange(Attributes.MINING_EFFICIENCY,3,1,0.333333333),List.of(),List.of(),List.of(),false,"pill_mining_efficiency_up","minecraft:lbcsounds.pill_good"),
        new Definition(51,"sneaking_speed_up",new AttributeChange(Attributes.SNEAKING_SPEED,10,1,0.1),List.of(),List.of(),List.of(),false,"pill_sneaking_speed_up","minecraft:lbcsounds.pill_good"),
        new Definition(52,"submerged_mining_speed_up",new AttributeChange(Attributes.SUBMERGED_MINING_SPEED,20,1,0.05),List.of(),List.of(),List.of(),false,"pill_submerged_mining_speed_up","minecraft:lbcsounds.pill_good"),
        new Definition(53,"sweeping_damage_ratio_up",new AttributeChange(Attributes.SWEEPING_DAMAGE_RATIO,20,1,0.05),List.of(),List.of(),List.of(),false,"pill_sweeping_damage_ratio_up","minecraft:lbcsounds.pill_good"),
        new Definition(54,"absorption",null,List.of(new Effect(MobEffects.ABSORPTION,60,1,false)),List.of(),List.of(),false,"pill_absorption","minecraft:lbcsounds.pill_good"),
        new Definition(55,"absorption_99",null,List.of(new Effect(MobEffects.ABSORPTION,5,127,false)),List.of(),List.of(),false,"pill_absorption_99","minecraft:lbcsounds.pill_good"),
        new Definition(56,"blindness",null,List.of(new Effect(MobEffects.BLINDNESS,60,0,false)),List.of(),List.of(),false,"pill_blindness","minecraft:lbcsounds.pill_bad"),
        new Definition(57,"conduit_power_1",null,List.of(new Effect(MobEffects.CONDUIT_POWER,30,1,false)),List.of(),List.of(),false,"pill_conduit_power_1","minecraft:lbcsounds.pill_good"),
        new Definition(58,"conduit_power_2",null,List.of(new Effect(MobEffects.CONDUIT_POWER,15,2,false)),List.of(),List.of(),false,"pill_conduit_power_2","minecraft:lbcsounds.pill_good"),
        new Definition(59,"darkness",null,List.of(new Effect(MobEffects.DARKNESS,120,0,false)),List.of(),List.of(),false,"pill_darkness","minecraft:lbcsounds.pill_bad"),
        new Definition(60,"dolphins_grace",null,List.of(new Effect(MobEffects.DOLPHINS_GRACE,60,0,false)),List.of(),List.of(),false,"pill_dolphins_grace","minecraft:lbcsounds.pill_good"),
        new Definition(61,"fire_resistance",null,List.of(new Effect(MobEffects.FIRE_RESISTANCE,60,0,false)),List.of(),List.of(),false,"pill_fire_resistance","minecraft:lbcsounds.pill_good"),
        new Definition(62,"fire_resistance_1",null,List.of(new Effect(MobEffects.FIRE_RESISTANCE,30,1,false)),List.of(),List.of(),false,"pill_fire_resistance_1","minecraft:lbcsounds.pill_good"),
        new Definition(63,"fire_resistance_2",null,List.of(new Effect(MobEffects.FIRE_RESISTANCE,15,2,false)),List.of(),List.of(),false,"pill_fire_resistance_2","minecraft:lbcsounds.pill_good"),
        new Definition(64,"glowing",null,List.of(new Effect(MobEffects.GLOWING,60,0,false)),List.of(),List.of(),false,"pill_glowing","minecraft:lbcsounds.pill_bad"),
        new Definition(65,"haste",null,List.of(new Effect(MobEffects.HASTE,60,1,false)),List.of(),List.of(),false,"pill_haste","minecraft:lbcsounds.pill_good"),
        new Definition(66,"haste_99",null,List.of(new Effect(MobEffects.HASTE,10,99,false)),List.of(),List.of(),false,"pill_haste_99","minecraft:lbcsounds.pill_good"),
        new Definition(67,"health_boost",null,List.of(new Effect(MobEffects.HEALTH_BOOST,60,4,false)),List.of(),List.of(),false,"pill_health_boost","minecraft:lbcsounds.pill_good"),
        new Definition(68,"hero_of_the_village",null,List.of(new Effect(MobEffects.HERO_OF_THE_VILLAGE,120,127,false)),List.of(),List.of(),false,"pill_hero_of_the_village","minecraft:lbcsounds.pill_good"),
        new Definition(69,"hunger",null,List.of(new Effect(MobEffects.HUNGER,100,1,false)),List.of(),List.of(),false,"pill_hunger","minecraft:lbcsounds.pill_bad"),
        new Definition(70,"hunger_99",null,List.of(new Effect(MobEffects.HUNGER,4,127,false)),List.of(),List.of(),false,"pill_hunger_99","minecraft:lbcsounds.pill_bad"),
        new Definition(71,"infested",null,List.of(new Effect(MobEffects.INFESTED,-1,0,false)),List.of(),List.of(),false,"pill_infested","minecraft:lbcsounds.pill_bad"),
        new Definition(72,"instant_damage",null,List.of(new Effect(MobEffects.INSTANT_DAMAGE,1,0,false)),List.of(),List.of(),false,"pill_instant_damage","minecraft:lbcsounds.pill_bad"),
        new Definition(73,"instant_damage_2",null,List.of(new Effect(MobEffects.INSTANT_DAMAGE,1,2,false)),List.of(),List.of(),false,"pill_instant_damage_2","minecraft:lbcsounds.pill_bad"),
        new Definition(74,"instant_health",null,List.of(new Effect(MobEffects.INSTANT_HEALTH,1,99,false)),List.of(),List.of(),false,"pill_instant_health","minecraft:lbcsounds.pill_good"),
        new Definition(75,"invisibility",null,List.of(new Effect(MobEffects.INVISIBILITY,60,0,false)),List.of(),List.of(),false,"pill_invisibility","minecraft:lbcsounds.pill_good"),
        new Definition(76,"invisibility_1",null,List.of(new Effect(MobEffects.INVISIBILITY,30,1,true)),List.of(),List.of(),false,"pill_invisibility_1","minecraft:lbcsounds.pill_good"),
        new Definition(77,"jump_boost",null,List.of(new Effect(MobEffects.JUMP_BOOST,60,2,false)),List.of(),List.of(),false,"pill_jump_boost","minecraft:lbcsounds.pill_good"),
        new Definition(78,"jump_boost_99",null,List.of(new Effect(MobEffects.JUMP_BOOST,10,127,false)),List.of(),List.of(),false,"pill_jump_boost_99","minecraft:lbcsounds.pill_bad"),
        new Definition(79,"levitation",null,List.of(new Effect(MobEffects.LEVITATION,20,0,false)),List.of(),List.of(),false,"pill_levitation","minecraft:lbcsounds.pill_bad"),
        new Definition(80,"levitation_99",null,List.of(new Effect(MobEffects.LEVITATION,1,127,false)),List.of(),List.of(),false,"pill_levitation_99","minecraft:lbcsounds.pill_bad"),
        new Definition(81,"luck",null,List.of(new Effect(MobEffects.LUCK,30,0,false)),List.of(),List.of(),false,"pill_luck","minecraft:lbcsounds.pill_good"),
        new Definition(82,"luck_99",null,List.of(new Effect(MobEffects.LUCK,2,255,false)),List.of(),List.of(),false,"pill_luck_99","minecraft:lbcsounds.pill_good"),
        new Definition(83,"mining_fatigue",null,List.of(new Effect(MobEffects.MINING_FATIGUE,60,1,false)),List.of(),List.of(),false,"pill_mining_fatigue","minecraft:lbcsounds.pill_bad"),
        new Definition(84,"mining_fatigue_99",null,List.of(new Effect(MobEffects.MINING_FATIGUE,20,99,false)),List.of(),List.of(),false,"pill_mining_fatigue_99","minecraft:lbcsounds.pill_bad"),
        new Definition(85,"nausea",null,List.of(new Effect(MobEffects.NAUSEA,100,0,false)),List.of(),List.of(),false,"pill_nausea","minecraft:lbcsounds.pill_bad"),
        new Definition(86,"night_vision",null,List.of(new Effect(MobEffects.NIGHT_VISION,120,0,false)),List.of(),List.of(),false,"pill_night_vision","minecraft:lbcsounds.pill_good"),
        new Definition(87,"oozing",null,List.of(new Effect(MobEffects.OOZING,-1,0,false)),List.of(),List.of(),false,"pill_oozing","minecraft:lbcsounds.pill_bad"),
        new Definition(88,"poison",null,List.of(new Effect(MobEffects.POISON,30,0,false)),List.of(),List.of(),false,"pill_poison","minecraft:lbcsounds.pill_bad"),
        new Definition(89,"raid_omen_16",null,List.of(new Effect(MobEffects.RAID_OMEN,60,16,false)),List.of(),List.of(),false,"pill_raid_omen_16","minecraft:lbcsounds.pill_good"),
        new Definition(90,"raid_omen_17",null,List.of(),List.of(new Counter("mana",5000)),List.of(),false,"pill_raid_omen_17","minecraft:lbcsounds.pill_good"),
        new Definition(91,"raid_omen_18",null,List.of(),List.of(new Counter("manamax",1000)),List.of(),false,"pill_raid_omen_18","minecraft:lbcsounds.pill_good"),
        new Definition(92,"raid_omen_19",null,List.of(),List.of(new Counter("manaregen",3)),List.of(),false,"pill_raid_omen_19","minecraft:lbcsounds.pill_good"),
        new Definition(93,"raid_omen_20",null,List.of(new Effect(MobEffects.RAID_OMEN,1,20,false)),List.of(),List.of(),false,"pill_raid_omen_20","minecraft:lbcsounds.pill_bad"),
        new Definition(94,"raid_omen_74",null,List.of(),List.of(new Counter("effect_snus",1)),List.of("have_lucky_effects"),false,"pill_raid_omen_74","minecraft:lbcsounds.pill_bad"),
        new Definition(95,"raid_omen_40",null,List.of(new Effect(MobEffects.RAID_OMEN,10,40,false)),List.of(),List.of(),false,"pill_raid_omen_40","minecraft:lbcsounds.pill_bad"),
        new Definition(96,"raid_omen_41",null,List.of(new Effect(MobEffects.RAID_OMEN,10,41,false)),List.of(),List.of(),false,"pill_raid_omen_41","minecraft:lbcsounds.pill_bad"),
        new Definition(97,"raid_omen_42",null,List.of(new Effect(MobEffects.RAID_OMEN,10,42,false)),List.of(),List.of(),false,"pill_raid_omen_42","minecraft:lbcsounds.pill_bad"),
        new Definition(98,"raid_omen_101",null,List.of(),List.of(new Counter("effect_small_explosion",1)),List.of("have_lucky_effects"),false,"pill_raid_omen_101","minecraft:lbcsounds.pill_bad"),
        new Definition(99,"raid_omen_102",null,List.of(),List.of(new Counter("effect_evoker",1)),List.of("have_lucky_effects"),false,"pill_raid_omen_102","minecraft:lbcsounds.pill_bad"),
        new Definition(100,"raid_omen_103",null,List.of(),List.of(new Counter("effect_shock",1)),List.of("have_lucky_effects","have_lucky_effects_1s","have_lucky_effects_5t"),false,"pill_raid_omen_103","minecraft:lbcsounds.pill_bad"),
        new Definition(101,"raid_omen_104",null,List.of(),List.of(new Counter("effect_explosion",1)),List.of("have_lucky_effects"),false,"pill_raid_omen_104","minecraft:lbcsounds.pill_bad"),
        new Definition(102,"raid_omen_105",null,List.of(new Effect(MobEffects.RAID_OMEN,10,105,false)),List.of(),List.of(),false,"pill_raid_omen_105","minecraft:lbcsounds.pill_bad"),
        new Definition(103,"raid_omen_106",null,List.of(new Effect(MobEffects.RAID_OMEN,1,106,false)),List.of(),List.of(),false,"pill_raid_omen_106","minecraft:lbcsounds.pill_bad"),
        new Definition(104,"raid_omen_107",null,List.of(),List.of(new Counter("effect_big_explosion",1)),List.of("have_lucky_effects"),false,"pill_raid_omen_107","minecraft:lbcsounds.pill_bad"),
        new Definition(105,"raid_omen_108",null,List.of(),List.of(new Counter("effect_traps",1)),List.of("have_lucky_effects"),false,"pill_raid_omen_108","minecraft:lbcsounds.pill_bad"),
        new Definition(106,"raid_omen_109",null,List.of(),List.of(new Counter("effect_lightning_bolt",1)),List.of("have_lucky_effects"),false,"pill_raid_omen_109","minecraft:lbcsounds.pill_bad"),
        new Definition(107,"raid_omen_110",null,List.of(new Effect(MobEffects.RAID_OMEN,1,110,false)),List.of(),List.of(),false,"pill_raid_omen_110","minecraft:lbcsounds.pill_bad"),
        new Definition(108,"raid_omen_111",null,List.of(new Effect(MobEffects.RAID_OMEN,15,111,false)),List.of(),List.of(),false,"pill_raid_omen_111","minecraft:lbcsounds.pill_bad"),
        new Definition(109,"raid_omen_112",null,List.of(new Effect(MobEffects.RAID_OMEN,30,112,false)),List.of(),List.of(),false,"pill_raid_omen_112","minecraft:lbcsounds.pill_bad"),
        new Definition(110,"raid_omen_113",null,List.of(new Effect(MobEffects.RAID_OMEN,30,113,false)),List.of(),List.of(),false,"pill_raid_omen_113","minecraft:lbcsounds.pill_bad"),
        new Definition(111,"raid_omen_114",null,List.of(new Effect(MobEffects.RAID_OMEN,1,114,false)),List.of(),List.of(),false,"pill_raid_omen_114","minecraft:lbcsounds.pill_bad"),
        new Definition(112,"raid_omen_115",null,List.of(new Effect(MobEffects.RAID_OMEN,1,115,false)),List.of(),List.of(),false,"pill_raid_omen_115","minecraft:lbcsounds.pill_bad"),
        new Definition(113,"raid_omen_116",null,List.of(new Effect(MobEffects.RAID_OMEN,30,116,false)),List.of(),List.of(),false,"pill_raid_omen_116","minecraft:lbcsounds.pill_bad"),
        new Definition(114,"raid_omen_117",null,List.of(new Effect(MobEffects.RAID_OMEN,1,117,false)),List.of(),List.of(),false,"pill_raid_omen_117","minecraft:lbcsounds.pill_bad"),
        new Definition(115,"raid_omen_118",null,List.of(),List.of(new Counter("effect_hedgehog_boom",1)),List.of("have_lucky_effects"),false,"pill_raid_omen_118","minecraft:lbcsounds.pill_bad"),
        new Definition(116,"raid_omen_119",null,List.of(),List.of(new Counter("effect_stand_arrow_hit",1)),List.of("have_lucky_effects"),false,"pill_raid_omen_119","minecraft:lbcsounds.pill_bad"),
        new Definition(117,"raid_omen_120",null,List.of(new Effect(MobEffects.RAID_OMEN,120,120,false)),List.of(),List.of(),false,"pill_raid_omen_120","minecraft:lbcsounds.pill_bad"),
        new Definition(118,"raid_omen_121",null,List.of(),List.of(new Counter("effect_nuking",1)),List.of("have_lucky_effects"),false,"pill_raid_omen_121","minecraft:lbcsounds.pill_bad"),
        new Definition(119,"raid_omen_122",null,List.of(),List.of(new Counter("effect_vacuum",1)),List.of("have_lucky_effects"),false,"pill_raid_omen_122","minecraft:lbcsounds.pill_bad"),
        new Definition(120,"raid_omen_123",null,List.of(),List.of(new Counter("effect_timestop",1)),List.of("have_lucky_effects"),false,"pill_raid_omen_123","minecraft:lbcsounds.pill_bad"),
        new Definition(121,"raid_omen_124",null,List.of(),List.of(new Counter("effect_chain_lightning",1)),List.of("have_lucky_effects"),false,"pill_raid_omen_124","minecraft:lbcsounds.pill_good"),
        new Definition(122,"regeneration",null,List.of(new Effect(MobEffects.REGENERATION,60,1,false)),List.of(),List.of(),false,"pill_regeneration","minecraft:lbcsounds.pill_good"),
        new Definition(123,"resistance",null,List.of(new Effect(MobEffects.RESISTANCE,60,1,false)),List.of(),List.of(),false,"pill_resistance","minecraft:lbcsounds.pill_good"),
        new Definition(124,"resistance_5",null,List.of(new Effect(MobEffects.RESISTANCE,10,4,false)),List.of(),List.of(),false,"pill_resistance_5","minecraft:lbcsounds.pill_good"),
        new Definition(125,"saturation",null,List.of(new Effect(MobEffects.SATURATION,1,127,false)),List.of(),List.of(),false,"pill_saturation","minecraft:lbcsounds.pill_good"),
        new Definition(126,"slow_falling",null,List.of(new Effect(MobEffects.SLOW_FALLING,60,0,false)),List.of(),List.of(),false,"pill_slow_falling","minecraft:lbcsounds.pill_good"),
        new Definition(127,"slowness",null,List.of(new Effect(MobEffects.SLOWNESS,60,2,false)),List.of(),List.of(),false,"pill_slowness","minecraft:lbcsounds.pill_bad"),
        new Definition(128,"speed",null,List.of(new Effect(MobEffects.SPEED,60,2,false)),List.of(),List.of(),false,"pill_speed","minecraft:lbcsounds.pill_good"),
        new Definition(129,"strength",null,List.of(new Effect(MobEffects.STRENGTH,40,0,false)),List.of(),List.of(),false,"pill_strength","minecraft:lbcsounds.pill_good"),
        new Definition(130,"strength_99",null,List.of(new Effect(MobEffects.STRENGTH,2,99,false)),List.of(),List.of(),false,"pill_strength_99","minecraft:lbcsounds.pill_good"),
        new Definition(131,"unluck",null,List.of(new Effect(MobEffects.UNLUCK,120,0,false)),List.of(),List.of(),false,"pill_unluck","minecraft:lbcsounds.pill_bad"),
        new Definition(132,"unluck_99",null,List.of(new Effect(MobEffects.UNLUCK,60,99,false)),List.of(),List.of(),false,"pill_unluck_99","minecraft:lbcsounds.pill_bad"),
        new Definition(133,"water_breathing",null,List.of(new Effect(MobEffects.WATER_BREATHING,120,0,false)),List.of(),List.of(),false,"pill_water_breathing","minecraft:lbcsounds.pill_good"),
        new Definition(134,"water_breathing_2",null,List.of(new Effect(MobEffects.WATER_BREATHING,60,2,false)),List.of(),List.of(),false,"pill_water_breathing_2","minecraft:lbcsounds.pill_good"),
        new Definition(135,"weakness",null,List.of(new Effect(MobEffects.WEAKNESS,120,0,false)),List.of(),List.of(),false,"pill_weakness","minecraft:lbcsounds.pill_bad"),
        new Definition(136,"weakness_99",null,List.of(new Effect(MobEffects.WEAKNESS,30,99,false)),List.of(),List.of(),false,"pill_weakness_99","minecraft:lbcsounds.pill_bad"),
        new Definition(137,"weaving",null,List.of(new Effect(MobEffects.WEAVING,-1,0,false)),List.of(),List.of(),false,"pill_weaving","minecraft:lbcsounds.pill_bad"),
        new Definition(138,"wind_charged",null,List.of(new Effect(MobEffects.WIND_CHARGED,-1,0,false)),List.of(),List.of(),false,"pill_wind_charged","minecraft:lbcsounds.pill_bad"),
        new Definition(139,"wither",null,List.of(new Effect(MobEffects.WITHER,60,0,false)),List.of(),List.of(),false,"pill_wither","minecraft:lbcsounds.pill_bad"),
        new Definition(140,"wither_99",null,List.of(new Effect(MobEffects.WITHER,10,99,false)),List.of(),List.of(),false,"pill_wither_99","minecraft:lbcsounds.pill_bad"),
        new Definition(141,"paralysis",null,List.of(new Effect(MobEffects.SLOWNESS,10,127,false)),List.of(),List.of(),true,"pill_paralysis","minecraft:lbcsounds.pill_bad"),
        new Definition(142,"excited",null,List.of(new Effect(MobEffects.SPEED,30,4,false),new Effect(MobEffects.HASTE,30,4,false),new Effect(MobEffects.JUMP_BOOST,30,0,false)),List.of(),List.of(),false,"pill_excited","minecraft:lbcsounds.pill_good"),
        new Definition(143,"drowsy",null,List.of(new Effect(MobEffects.SLOWNESS,30,4,false),new Effect(MobEffects.MINING_FATIGUE,30,0,false),new Effect(MobEffects.WEAKNESS,30,0,false)),List.of(),List.of(),false,"pill_drowsy","minecraft:lbcsounds.pill_bad"),
        new Definition(144,"i_found_pills",null,List.of(),List.of(),List.of(),false,"pill_i_found_pills","minecraft:lbcsounds.pill_bad"));
    private final GameController game;
    public LbcPills(GameController game){this.game=game;}
    public static void register() {}
    public static List<Definition> definitions(){return DEFINITIONS;}
    public static Definition definition(int id){return id>=0&&id<DEFINITIONS.size()?DEFINITIONS.get(id):null;}
    public static String table(int id){if(id<0||id>=OUTPUT_COUNT)throw new IllegalArgumentException("pill id");return "lbc:ported/pills/pill_"+id;}
    /** Pure canonical result prototype: constructing a recipe preview consumes no world RNG. */
    public static ItemStack stack(int id){
        if(id<0||id>=OUTPUT_COUNT)throw new IllegalArgumentException("pill id");
        var stack=new ItemStack(PILL);var data=new CompoundTag();data.putInt("pill",id);
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
        stack.set(DataComponents.ITEM_MODEL,Identifier.parse("lbc:pills/pill_"+id));
        stack.set(DataComponents.CUSTOM_NAME,Component.translatable("pill").withStyle(ChatFormatting.WHITE).withStyle(s->s.withItalic(false)));
        stack.set(DataComponents.LORE,new ItemLore(List.of(Component.translatable("pill.lore")
                .withStyle(ChatFormatting.WHITE).withStyle(s->s.withItalic(false)))));
        return stack;
    }
    public static int pillId(ItemStack stack){
        if(stack.isEmpty())return -1;var data=stack.get(DataComponents.CUSTOM_DATA);
        if(data==null)return -1;var tag=data.copyTag();
        // Missing pill must never be confused with the valid zero output.
        var id=tag.getInt("pill");if(id.isEmpty())return -1;int value=id.get();
        return value>=0&&value<=OUTPUT_COUNT?value:-1;
    }
    public static boolean certificate(ItemStack stack){
        return !stack.isEmpty()&&(stack.is(CERTIFICATE)||stack.is(Items.REPEATING_COMMAND_BLOCK))
                &&LbcFartus.data(stack).getIntOr("phd",0)==1;
    }
    private static Item.Properties properties(ResourceKey<Item> key,Item carrier){
        return new Item.Properties(){@Override public Identifier effectiveModel(){return BuiltInRegistries.ITEM.getKey(carrier);}}
                .setId(key);
    }
    private static Item registerPill(){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:phd_pill"));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(properties(key,Items.COOKIE)
                .overrideDescription("pill").food(new FoodProperties(1,1,true),Consumable.builder().consumeSeconds(.2f).build())
                .component(DataComponents.ITEM_MODEL,Identifier.parse("lbc:pills/pill_0")),Items.COOKIE,true));
    }
    private static Item registerCertificate(){
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:phd_certificate"));
        var flag=new CompoundTag();flag.putInt("phd",1);
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(properties(key,Items.REPEATING_COMMAND_BLOCK)
                .overrideDescription("phd").component(DataComponents.CUSTOM_DATA,CustomData.of(flag))
                .component(DataComponents.ITEM_MODEL,Identifier.parse("lbc:phd"))
                .component(DataComponents.CUSTOM_NAME,Component.translatable("phd").withStyle(s->s.withItalic(false)))
                .component(DataComponents.LORE,new ItemLore(List.of(
                    Component.translatable("phd.lore.1").withStyle(ChatFormatting.WHITE).withStyle(s->s.withItalic(false)),
                    Component.translatable("phd.lore.2").withStyle(ChatFormatting.WHITE).withStyle(s->s.withItalic(false)),
                    Component.translatable("phd.lore.3").withStyle(ChatFormatting.WHITE).withStyle(s->s.withItalic(false)),
                    Component.translatable("phd.lore.4").withStyle(ChatFormatting.WHITE).withStyle(s->s.withItalic(true)))))
                ,Items.REPEATING_COMMAND_BLOCK,true));
    }
    private boolean admitted(ServerPlayer p){
        return game.playing()&&game.stage()!=GameController.Stage.ENDING&&p.isAlive()&&!p.isRemoved()
                &&p.level()==game.maps().level()&&game.canUseGameplayItems(p)&&!game.isObserver(p)
                &&game.items().wands().validRoleTarget(p,false)&&LbcDimensionItems.ordinary(p)
                &&!game.items().timeStop().stopped(p.level())&&!game.items().wands().timeStopped()
                &&!game.items().wands().inputTimeStopped(p);
    }
    /** Native consumption owns nutrition, normal consumption events, and the sole item debit. */
    public void consumed(ServerPlayer p,ItemStack stack){
        int id=pillId(stack);if(id<0||!admitted(p))return;var pill=DEFINITIONS.get(id);
        if(pill.attribute()!=null){var a=pill.attribute();LbcAttributeChanges.change(p,a.attribute(),a.inputScale(),a.step(),a.outputScale());}
        for(var effect:pill.effects()){
            if(effect.type().value().isInstantaneous()){
                effect.type().value().applyInstantaneousEffect(p.level(),null,null,p,
                        ItemEffectLevels.amplifier(effect.type(),effect.amplifier()),1.0);
            }else{
                int ticks=effect.seconds()<0?-1:effect.seconds()*20;
                p.addEffect(ItemEffectLevels.create(effect.type(),ticks,effect.amplifier(),false,!effect.hidden()),p);
            }
        }
        for(var counter:pill.counters())game.items().mana().score(p,counter.key()).add(counter.amount());
        p.entityTags().addAll(pill.tags());
        if(!pill.counters().isEmpty())game.items().adoptRawFoodStatuses(p);
        if(pill.paralysis()){
            // /tp wraps the relative +200 pitch to -160 before adding the old pitch and clamping.
            // Shared native teleport mixin preserves server and client motion independently.
            if(p.teleportTo(p.level(),p.getX(),p.getY()-2,p.getZ(),Set.of(Relative.Y_ROT,Relative.X_ROT),0,
                    Mth.wrapDegrees(200f),true))
                game.items().indirectKills().teleported(p,p,true);
        }
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(" ")));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(pill.subtitle())));
        LbcOverload.sound(p,pill.sound(),1,1,0);
    }
}
