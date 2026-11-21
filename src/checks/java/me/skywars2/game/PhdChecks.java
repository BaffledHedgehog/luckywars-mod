package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.phys.Vec3;

/** Independent original reward anchors and actual native consumption, before long consequences tick. */
public final class PhdChecks {
    private PhdChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("PhD: "+why);}
    private static void near(double actual,double expected,String why){require(Math.abs(actual-expected)<.000001,why+" actual="+actual+" expected="+expected);}
    private static boolean translated(Component text,String key){return text!=null&&(text.getContents() instanceof TranslatableContents t&&t.getKey().equals(key)||text.getSiblings().stream().anyMatch(c->translated(c,key)));}
    private record Source(int id,String key,String attribute,int scale,int step,double output,List<String> actions,String sound){}
    // Original advancement IDs and reward functions, independently of LbcPills.Definition.
    private static final List<Source> SOURCE=List.of(
        new Source(0,"max_health_down","max_health",1,-1,1.0,List.of(),"minecraft:lbcsounds.pill_bad"),
        new Source(1,"attack_damage_down","attack_damage",1,-1,1.0,List.of(),"minecraft:lbcsounds.pill_bad"),
        new Source(2,"armor_down","armor",1,-1,1.0,List.of(),"minecraft:lbcsounds.pill_bad"),
        new Source(3,"armor_toughness_down","armor_toughness",1,-1,1.0,List.of(),"minecraft:lbcsounds.pill_bad"),
        new Source(4,"attack_speed_down","attack_speed",5,-1,0.2,List.of(),"minecraft:lbcsounds.pill_bad"),
        new Source(5,"knockback_resistance_down","knockback_resistance",10,-10,0.1,List.of(),"minecraft:lbcsounds.pill_bad"),
        new Source(6,"luck_down","luck",1,-1,1.0,List.of(),"minecraft:lbcsounds.pill_bad"),
        new Source(7,"movement_speed_down","movement_speed",100000,-2000,1e-05,List.of(),"minecraft:lbcsounds.pill_bad"),
        new Source(8,"attack_knockback_down","attack_knockback",10,-1,0.1,List.of(),"minecraft:lbcsounds.pill_bad"),
        new Source(9,"burning_time_down","burning_time",10,-1,0.1,List.of(),"minecraft:lbcsounds.pill_good"),
        new Source(10,"explosion_knockback_resistance_down","explosion_knockback_resistance",10,-1,0.1,List.of(),"minecraft:lbcsounds.pill_bad"),
        new Source(11,"fall_damage_multiplier_down","fall_damage_multiplier",10,-1,0.1,List.of(),"minecraft:lbcsounds.pill_good"),
        new Source(12,"gravity_down","gravity",100,-1,0.01,List.of(),"minecraft:lbcsounds.pill_good"),
        new Source(13,"jump_strength_down","jump_strength",10,-1,0.1,List.of(),"minecraft:lbcsounds.pill_bad"),
        new Source(14,"movement_efficiency_down","movement_efficiency",10,-1,0.1,List.of(),"minecraft:lbcsounds.pill_bad"),
        new Source(15,"oxygen_bonus_down","oxygen_bonus",10,-1,0.1,List.of(),"minecraft:lbcsounds.pill_bad"),
        new Source(16,"safe_fall_distance_down","safe_fall_distance",5,-1,0.2,List.of(),"minecraft:lbcsounds.pill_bad"),
        new Source(17,"scale_down","scale",10,-1,0.1,List.of(),"minecraft:lbcsounds.pill_good"),
        new Source(18,"step_height_down","step_height",10,-1,0.1,List.of(),"minecraft:lbcsounds.pill_bad"),
        new Source(19,"water_movement_efficiency_down","water_movement_efficiency",10,-1,0.1,List.of(),"minecraft:lbcsounds.pill_bad"),
        new Source(20,"block_break_speed_down","block_break_speed",10,-1,0.1,List.of(),"minecraft:lbcsounds.pill_bad"),
        new Source(21,"block_interaction_range_down","block_interaction_range",5,-1,0.2,List.of(),"minecraft:lbcsounds.pill_bad"),
        new Source(22,"entity_interaction_range_down","entity_interaction_range",5,-1,0.2,List.of(),"minecraft:lbcsounds.pill_bad"),
        new Source(23,"mining_efficiency_down","mining_efficiency",3,-1,0.333333333,List.of(),"minecraft:lbcsounds.pill_bad"),
        new Source(24,"sneaking_speed_down","sneaking_speed",10,-1,0.1,List.of(),"minecraft:lbcsounds.pill_bad"),
        new Source(25,"submerged_mining_speed_down","submerged_mining_speed",20,-1,0.05,List.of(),"minecraft:lbcsounds.pill_bad"),
        new Source(26,"sweeping_damage_ratio_down","sweeping_damage_ratio",20,-1,0.05,List.of(),"minecraft:lbcsounds.pill_bad"),
        new Source(27,"max_health_up","max_health",1,2,1.0,List.of(),"minecraft:lbcsounds.pill_good"),
        new Source(28,"attack_damage_up","attack_damage",1,1,1.0,List.of(),"minecraft:lbcsounds.pill_good"),
        new Source(29,"armor_up","armor",1,2,1.0,List.of(),"minecraft:lbcsounds.pill_good"),
        new Source(30,"armor_toughness_up","armor_toughness",1,1,1.0,List.of(),"minecraft:lbcsounds.pill_good"),
        new Source(31,"attack_speed_up","attack_speed",5,1,0.2,List.of(),"minecraft:lbcsounds.pill_good"),
        new Source(32,"knockback_resistance_up","knockback_resistance",10,10,0.1,List.of(),"minecraft:lbcsounds.pill_good"),
        new Source(33,"luck_up","luck",1,1,1.0,List.of(),"minecraft:lbcsounds.pill_good"),
        new Source(34,"movement_speed_up","movement_speed",100000,2000,1e-05,List.of(),"minecraft:lbcsounds.pill_good"),
        new Source(35,"attack_knockback_up","attack_knockback",10,1,0.1,List.of(),"minecraft:lbcsounds.pill_good"),
        new Source(36,"burning_time_up","burning_time",10,1,0.1,List.of(),"minecraft:lbcsounds.pill_bad"),
        new Source(37,"explosion_knockback_resistance_up","explosion_knockback_resistance",10,1,0.1,List.of(),"minecraft:lbcsounds.pill_good"),
        new Source(38,"fall_damage_multiplier_up","fall_damage_multiplier",10,1,0.1,List.of(),"minecraft:lbcsounds.pill_bad"),
        new Source(39,"gravity_up","gravity",100,1,0.01,List.of(),"minecraft:lbcsounds.pill_bad"),
        new Source(40,"jump_strength_up","jump_strength",10,1,0.1,List.of(),"minecraft:lbcsounds.pill_good"),
        new Source(41,"movement_efficiency_up","movement_efficiency",10,1,0.1,List.of(),"minecraft:lbcsounds.pill_good"),
        new Source(42,"oxygen_bonus_up","oxygen_bonus",10,1,0.1,List.of(),"minecraft:lbcsounds.pill_good"),
        new Source(43,"safe_fall_distance_up","safe_fall_distance",5,1,0.2,List.of(),"minecraft:lbcsounds.pill_good"),
        new Source(44,"scale_up","scale",10,1,0.1,List.of(),"minecraft:lbcsounds.pill_bad"),
        new Source(45,"step_height_up","step_height",10,1,0.1,List.of(),"minecraft:lbcsounds.pill_good"),
        new Source(46,"water_movement_efficiency_up","water_movement_efficiency",10,1,0.1,List.of(),"minecraft:lbcsounds.pill_good"),
        new Source(47,"block_break_speed_up","block_break_speed",10,1,0.1,List.of(),"minecraft:lbcsounds.pill_good"),
        new Source(48,"block_interaction_range_up","block_interaction_range",5,1,0.2,List.of(),"minecraft:lbcsounds.pill_good"),
        new Source(49,"entity_interaction_range_up","entity_interaction_range",5,1,0.2,List.of(),"minecraft:lbcsounds.pill_good"),
        new Source(50,"mining_efficiency_up","mining_efficiency",3,1,0.333333333,List.of(),"minecraft:lbcsounds.pill_good"),
        new Source(51,"sneaking_speed_up","sneaking_speed",10,1,0.1,List.of(),"minecraft:lbcsounds.pill_good"),
        new Source(52,"submerged_mining_speed_up","submerged_mining_speed",20,1,0.05,List.of(),"minecraft:lbcsounds.pill_good"),
        new Source(53,"sweeping_damage_ratio_up","sweeping_damage_ratio",20,1,0.05,List.of(),"minecraft:lbcsounds.pill_good"),
        new Source(54,"absorption","",0,0,0,List.of("effect give @s minecraft:absorption 60 1"),"minecraft:lbcsounds.pill_good"),
        new Source(55,"absorption_99","",0,0,0,List.of("effect give @s minecraft:absorption 5 127"),"minecraft:lbcsounds.pill_good"),
        new Source(56,"blindness","",0,0,0,List.of("effect give @s minecraft:blindness 60 0"),"minecraft:lbcsounds.pill_bad"),
        new Source(57,"conduit_power_1","",0,0,0,List.of("effect give @s minecraft:conduit_power 30 1"),"minecraft:lbcsounds.pill_good"),
        new Source(58,"conduit_power_2","",0,0,0,List.of("effect give @s minecraft:conduit_power 15 2"),"minecraft:lbcsounds.pill_good"),
        new Source(59,"darkness","",0,0,0,List.of("effect give @s minecraft:darkness 120 0"),"minecraft:lbcsounds.pill_bad"),
        new Source(60,"dolphins_grace","",0,0,0,List.of("effect give @s minecraft:dolphins_grace 60 0"),"minecraft:lbcsounds.pill_good"),
        new Source(61,"fire_resistance","",0,0,0,List.of("effect give @s minecraft:fire_resistance 60 0"),"minecraft:lbcsounds.pill_good"),
        new Source(62,"fire_resistance_1","",0,0,0,List.of("effect give @s minecraft:fire_resistance 30 1"),"minecraft:lbcsounds.pill_good"),
        new Source(63,"fire_resistance_2","",0,0,0,List.of("effect give @s minecraft:fire_resistance 15 2"),"minecraft:lbcsounds.pill_good"),
        new Source(64,"glowing","",0,0,0,List.of("effect give @s minecraft:glowing 60 0"),"minecraft:lbcsounds.pill_bad"),
        new Source(65,"haste","",0,0,0,List.of("effect give @s minecraft:haste 60 1"),"minecraft:lbcsounds.pill_good"),
        new Source(66,"haste_99","",0,0,0,List.of("effect give @s minecraft:haste 10 99"),"minecraft:lbcsounds.pill_good"),
        new Source(67,"health_boost","",0,0,0,List.of("effect give @s minecraft:health_boost 60 4"),"minecraft:lbcsounds.pill_good"),
        new Source(68,"hero_of_the_village","",0,0,0,List.of("effect give @s minecraft:hero_of_the_village 120 127"),"minecraft:lbcsounds.pill_good"),
        new Source(69,"hunger","",0,0,0,List.of("effect give @s minecraft:hunger 100 1"),"minecraft:lbcsounds.pill_bad"),
        new Source(70,"hunger_99","",0,0,0,List.of("effect give @s minecraft:hunger 4 127"),"minecraft:lbcsounds.pill_bad"),
        new Source(71,"infested","",0,0,0,List.of("effect give @s minecraft:infested infinite 0"),"minecraft:lbcsounds.pill_bad"),
        new Source(72,"instant_damage","",0,0,0,List.of("effect give @s minecraft:instant_damage 1 0"),"minecraft:lbcsounds.pill_bad"),
        new Source(73,"instant_damage_2","",0,0,0,List.of("effect give @s minecraft:instant_damage 1 2"),"minecraft:lbcsounds.pill_bad"),
        new Source(74,"instant_health","",0,0,0,List.of("effect give @s minecraft:instant_health 1 99"),"minecraft:lbcsounds.pill_good"),
        new Source(75,"invisibility","",0,0,0,List.of("effect give @s minecraft:invisibility 60 0"),"minecraft:lbcsounds.pill_good"),
        new Source(76,"invisibility_1","",0,0,0,List.of("effect give @s minecraft:invisibility 30 1 true"),"minecraft:lbcsounds.pill_good"),
        new Source(77,"jump_boost","",0,0,0,List.of("effect give @s minecraft:jump_boost 60 2"),"minecraft:lbcsounds.pill_good"),
        new Source(78,"jump_boost_99","",0,0,0,List.of("effect give @s minecraft:jump_boost 10 127"),"minecraft:lbcsounds.pill_bad"),
        new Source(79,"levitation","",0,0,0,List.of("effect give @s minecraft:levitation 20 0"),"minecraft:lbcsounds.pill_bad"),
        new Source(80,"levitation_99","",0,0,0,List.of("effect give @s minecraft:levitation 1 127"),"minecraft:lbcsounds.pill_bad"),
        new Source(81,"luck","",0,0,0,List.of("effect give @s minecraft:luck 30 0"),"minecraft:lbcsounds.pill_good"),
        new Source(82,"luck_99","",0,0,0,List.of("effect give @s minecraft:luck 2 255"),"minecraft:lbcsounds.pill_good"),
        new Source(83,"mining_fatigue","",0,0,0,List.of("effect give @s minecraft:mining_fatigue 60 1"),"minecraft:lbcsounds.pill_bad"),
        new Source(84,"mining_fatigue_99","",0,0,0,List.of("effect give @s minecraft:mining_fatigue 20 99"),"minecraft:lbcsounds.pill_bad"),
        new Source(85,"nausea","",0,0,0,List.of("effect give @s minecraft:nausea 100 0"),"minecraft:lbcsounds.pill_bad"),
        new Source(86,"night_vision","",0,0,0,List.of("effect give @s minecraft:night_vision 120 0"),"minecraft:lbcsounds.pill_good"),
        new Source(87,"oozing","",0,0,0,List.of("effect give @s minecraft:oozing infinite 0"),"minecraft:lbcsounds.pill_bad"),
        new Source(88,"poison","",0,0,0,List.of("effect give @s minecraft:poison 30 0"),"minecraft:lbcsounds.pill_bad"),
        new Source(89,"raid_omen_16","",0,0,0,List.of("effect give @s minecraft:raid_omen 60 16"),"minecraft:lbcsounds.pill_good"),
        new Source(90,"raid_omen_17","",0,0,0,List.of("scoreboard players add @s mana 5000"),"minecraft:lbcsounds.pill_good"),
        new Source(91,"raid_omen_18","",0,0,0,List.of("scoreboard players add @s manamax 1000"),"minecraft:lbcsounds.pill_good"),
        new Source(92,"raid_omen_19","",0,0,0,List.of("scoreboard players add @s manaregen 3"),"minecraft:lbcsounds.pill_good"),
        new Source(93,"raid_omen_20","",0,0,0,List.of("effect give @s minecraft:raid_omen 1 20"),"minecraft:lbcsounds.pill_bad"),
        new Source(94,"raid_omen_74","",0,0,0,List.of("scoreboard players add @s effect_snus 1","tag @s add have_lucky_effects"),"minecraft:lbcsounds.pill_bad"),
        new Source(95,"raid_omen_40","",0,0,0,List.of("effect give @s minecraft:raid_omen 10 40"),"minecraft:lbcsounds.pill_bad"),
        new Source(96,"raid_omen_41","",0,0,0,List.of("effect give @s minecraft:raid_omen 10 41"),"minecraft:lbcsounds.pill_bad"),
        new Source(97,"raid_omen_42","",0,0,0,List.of("effect give @s minecraft:raid_omen 10 42"),"minecraft:lbcsounds.pill_bad"),
        new Source(98,"raid_omen_101","",0,0,0,List.of("scoreboard players add @s effect_small_explosion 1","tag @s add have_lucky_effects"),"minecraft:lbcsounds.pill_bad"),
        new Source(99,"raid_omen_102","",0,0,0,List.of("scoreboard players add @s effect_evoker 1","tag @s add have_lucky_effects"),"minecraft:lbcsounds.pill_bad"),
        new Source(100,"raid_omen_103","",0,0,0,List.of("scoreboard players add @s effect_shock 1","tag @s add have_lucky_effects","tag @s add have_lucky_effects_1s","tag @s add have_lucky_effects_5t"),"minecraft:lbcsounds.pill_bad"),
        new Source(101,"raid_omen_104","",0,0,0,List.of("scoreboard players add @s effect_explosion 1","tag @s add have_lucky_effects"),"minecraft:lbcsounds.pill_bad"),
        new Source(102,"raid_omen_105","",0,0,0,List.of("effect give @s minecraft:raid_omen 10 105"),"minecraft:lbcsounds.pill_bad"),
        new Source(103,"raid_omen_106","",0,0,0,List.of("effect give @s minecraft:raid_omen 1 106"),"minecraft:lbcsounds.pill_bad"),
        new Source(104,"raid_omen_107","",0,0,0,List.of("scoreboard players add @s effect_big_explosion 1","tag @s add have_lucky_effects"),"minecraft:lbcsounds.pill_bad"),
        new Source(105,"raid_omen_108","",0,0,0,List.of("scoreboard players add @s effect_traps 1","tag @s add have_lucky_effects"),"minecraft:lbcsounds.pill_bad"),
        new Source(106,"raid_omen_109","",0,0,0,List.of("scoreboard players add @s effect_lightning_bolt 1","tag @s add have_lucky_effects"),"minecraft:lbcsounds.pill_bad"),
        new Source(107,"raid_omen_110","",0,0,0,List.of("effect give @s minecraft:raid_omen 1 110"),"minecraft:lbcsounds.pill_bad"),
        new Source(108,"raid_omen_111","",0,0,0,List.of("effect give @s minecraft:raid_omen 15 111"),"minecraft:lbcsounds.pill_bad"),
        new Source(109,"raid_omen_112","",0,0,0,List.of("effect give @s minecraft:raid_omen 30 112"),"minecraft:lbcsounds.pill_bad"),
        new Source(110,"raid_omen_113","",0,0,0,List.of("effect give @s minecraft:raid_omen 30 113"),"minecraft:lbcsounds.pill_bad"),
        new Source(111,"raid_omen_114","",0,0,0,List.of("effect give @s minecraft:raid_omen 1 114"),"minecraft:lbcsounds.pill_bad"),
        new Source(112,"raid_omen_115","",0,0,0,List.of("effect give @s minecraft:raid_omen 1 115"),"minecraft:lbcsounds.pill_bad"),
        new Source(113,"raid_omen_116","",0,0,0,List.of("effect give @s minecraft:raid_omen 30 116"),"minecraft:lbcsounds.pill_bad"),
        new Source(114,"raid_omen_117","",0,0,0,List.of("effect give @s minecraft:raid_omen 1 117"),"minecraft:lbcsounds.pill_bad"),
        new Source(115,"raid_omen_118","",0,0,0,List.of("scoreboard players add @s effect_hedgehog_boom 1","tag @s add have_lucky_effects"),"minecraft:lbcsounds.pill_bad"),
        new Source(116,"raid_omen_119","",0,0,0,List.of("scoreboard players add @s effect_stand_arrow_hit 1","tag @s add have_lucky_effects"),"minecraft:lbcsounds.pill_bad"),
        new Source(117,"raid_omen_120","",0,0,0,List.of("effect give @s minecraft:raid_omen 120 120"),"minecraft:lbcsounds.pill_bad"),
        new Source(118,"raid_omen_121","",0,0,0,List.of("scoreboard players add @s effect_nuking 1","tag @s add have_lucky_effects"),"minecraft:lbcsounds.pill_bad"),
        new Source(119,"raid_omen_122","",0,0,0,List.of("scoreboard players add @s effect_vacuum 1","tag @s add have_lucky_effects"),"minecraft:lbcsounds.pill_bad"),
        new Source(120,"raid_omen_123","",0,0,0,List.of("scoreboard players add @s effect_timestop 1","tag @s add have_lucky_effects"),"minecraft:lbcsounds.pill_bad"),
        new Source(121,"raid_omen_124","",0,0,0,List.of("scoreboard players add @s effect_chain_lightning 1","tag @s add have_lucky_effects"),"minecraft:lbcsounds.pill_good"),
        new Source(122,"regeneration","",0,0,0,List.of("effect give @s minecraft:regeneration 60 1"),"minecraft:lbcsounds.pill_good"),
        new Source(123,"resistance","",0,0,0,List.of("effect give @s minecraft:resistance 60 1"),"minecraft:lbcsounds.pill_good"),
        new Source(124,"resistance_5","",0,0,0,List.of("effect give @s minecraft:resistance 10 4"),"minecraft:lbcsounds.pill_good"),
        new Source(125,"saturation","",0,0,0,List.of("effect give @s minecraft:saturation 1 127"),"minecraft:lbcsounds.pill_good"),
        new Source(126,"slow_falling","",0,0,0,List.of("effect give @s minecraft:slow_falling 60 0"),"minecraft:lbcsounds.pill_good"),
        new Source(127,"slowness","",0,0,0,List.of("effect give @s minecraft:slowness 60 2"),"minecraft:lbcsounds.pill_bad"),
        new Source(128,"speed","",0,0,0,List.of("effect give @s minecraft:speed 60 2"),"minecraft:lbcsounds.pill_good"),
        new Source(129,"strength","",0,0,0,List.of("effect give @s minecraft:strength 40 0"),"minecraft:lbcsounds.pill_good"),
        new Source(130,"strength_99","",0,0,0,List.of("effect give @s minecraft:strength 2 99"),"minecraft:lbcsounds.pill_good"),
        new Source(131,"unluck","",0,0,0,List.of("effect give @s minecraft:unluck 120 0"),"minecraft:lbcsounds.pill_bad"),
        new Source(132,"unluck_99","",0,0,0,List.of("effect give @s minecraft:unluck 60 99"),"minecraft:lbcsounds.pill_bad"),
        new Source(133,"water_breathing","",0,0,0,List.of("effect give @s minecraft:water_breathing 120"),"minecraft:lbcsounds.pill_good"),
        new Source(134,"water_breathing_2","",0,0,0,List.of("effect give @s minecraft:water_breathing 60 2"),"minecraft:lbcsounds.pill_good"),
        new Source(135,"weakness","",0,0,0,List.of("effect give @s minecraft:weakness 120 0"),"minecraft:lbcsounds.pill_bad"),
        new Source(136,"weakness_99","",0,0,0,List.of("effect give @s minecraft:weakness 30 99"),"minecraft:lbcsounds.pill_bad"),
        new Source(137,"weaving","",0,0,0,List.of("effect give @s minecraft:weaving infinite 0"),"minecraft:lbcsounds.pill_bad"),
        new Source(138,"wind_charged","",0,0,0,List.of("effect give @s minecraft:wind_charged infinite 0"),"minecraft:lbcsounds.pill_bad"),
        new Source(139,"wither","",0,0,0,List.of("effect give @s minecraft:wither 60 0"),"minecraft:lbcsounds.pill_bad"),
        new Source(140,"wither_99","",0,0,0,List.of("effect give @s minecraft:wither 10 99"),"minecraft:lbcsounds.pill_bad"),
        new Source(141,"paralysis","",0,0,0,List.of("effect give @s minecraft:slowness 10 127"),"minecraft:lbcsounds.pill_bad"),
        new Source(142,"excited","",0,0,0,List.of("effect give @s minecraft:speed 30 4","effect give @s minecraft:haste 30 4","effect give @s minecraft:jump_boost 30 0"),"minecraft:lbcsounds.pill_good"),
        new Source(143,"drowsy","",0,0,0,List.of("effect give @s minecraft:slowness 30 4","effect give @s minecraft:mining_fatigue 30 0","effect give @s minecraft:weakness 30 0"),"minecraft:lbcsounds.pill_bad"),
        new Source(144,"i_found_pills","",0,0,0,List.of(),"minecraft:lbcsounds.pill_bad"));

    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;
        final RegenerationBeltChecks.Snapshot saved;final ServerPlayer p;
        final Map<net.minecraft.stats.Stat<?>,Integer> stats=new LinkedHashMap<>();
        final Map<Marker,MapManager.BlockSnapshot> fieldBlocks=new IdentityHashMap<>();final Set<String> counters=new LinkedHashSet<>();final Vec3 origin;boolean closed;
        Fixture(GameController game){
            this.game=game;p=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            require(p!=null&&game.playing()&&!p.isUsingItem(),"active idle native consumer");saved=new RegenerationBeltChecks.Snapshot(game,p);
            for(var s:SOURCE)for(var action:s.actions())if(action.startsWith("scoreboard players add"))counters.add(action.split(" ")[4]);
            for(var key:counters)if(!List.of("mana","manamax","manaregen").contains(key)){var objective=game.server().getScoreboard().getObjective(key);var value=objective==null?null:game.server().getScoreboard().getPlayerScoreInfo(p,objective);require(value==null||value.value()==0,"exclusive empty source counter "+key);}
            for(var item:List.of(LbcPills.PILL,Items.COOKIE))for(var type:List.of(Stats.ITEM_USED,Stats.ITEM_CRAFTED)){var stat=type.get(item);stats.put(stat,p.getStats().getValue(stat));}
            base=new ElementalGrimoireChecks.Fixture(game);base.initialize();origin=p.position();reset();
        }
        ItemStack loot(String name){var out=game.lbc().rollTable(p.level(),p.position(),p,name);require(out.size()==1,"singleton canonical source leaf "+name);return out.getFirst();}
        ItemStack pill(int id){return loot("lbc:ported/pills/pill_"+id);}
        void reset(){
            game.items().clear(p);p.removeAllEffects();for(var counter:counters)game.items().mana().score(p,counter).set(0);
            p.entityTags().removeAll(List.of("have_lucky_effects","have_lucky_effects_1s","have_lucky_effects_5t","spectator","strict_map_object","rcdenyalways","time_stopped","nomagic_active"));
            p.getAttributes().assignAllValues(saved.attributes);p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(500);p.setAbsorptionAmount(0);p.setInvulnerableTime(0);p.damageCooldownTime=0;p.hurtTime=0;
            p.getInventory().clearContent();p.inventoryMenu.getCraftSlots().clearContent();p.containerMenu.setCarried(ItemStack.EMPTY);p.getInventory().setSelectedSlot(0);
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getAbilities().invulnerable=false;p.getAbilities().instabuild=false;p.setNoGravity(true);p.setSprinting(false);p.setShiftKeyDown(false);p.setDeltaMovement(Vec3.ZERO);p.setKnownMovement(Vec3.ZERO);
            p.teleportTo(p.level(),origin.x,origin.y,origin.z,Set.of(),0,0,true);p.getFoodData().setFoodLevel(1);p.getFoodData().setSaturation(0);
            var profile=game.players().get(p.getUUID());profile.lbcKit=0;profile.lbcChallengeKit=0;profile.lbcSkill=0;profile.lbcChallenge=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);Arrays.fill(profile.levelups,0);
            game.items().mana().set(p,100);game.items().mana().maximum(p,10000);game.items().mana().baseRegen(p,0);game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();base.recording.packets.clear();
        }
        int command(String text){try{return game.server().getCommands().getDispatcher().execute(text,game.server().createCommandSourceStack().withEntity(p).withLevel(p.level()).withPosition(p.position()).withSuppressedOutput());}catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException("native source oracle "+text,error);}}
        List<String> effects(){var values=new ArrayList<String>();for(var effect:p.getActiveEffects())for(var layer=effect;layer!=null;layer=((EffectLayers.Access)layer).skywars2$layerHidden())values.add(BuiltInRegistries.MOB_EFFECT.getKey(layer.getEffect().value())+":"+layer.getDuration()+":"+layer.getAmplifier()+":"+layer.isVisible()+":"+layer.showIcon());Collections.sort(values);return values;}
        void presentation(){
            require(SOURCE.size()==145,"144 source outputs and raw144 reward");
            for(int id=0;id<144;id++){
                var item=pill(id);require(item.is(LbcPills.PILL)&&item.getCount()==1&&item.getMaxStackSize()==64&&LbcPills.pillId(item)==id,"canonical native pill identity/count/unchanged max stack "+id);
                require(Identifier.parse("lbc:pills/pill_"+id).equals(item.get(DataComponents.ITEM_MODEL)),"source model "+id);
                var tag=new CompoundTag();tag.putInt("pill",id);require(LbcFartus.data(item).equals(tag),"exact source pill metadata including valid zero "+id);
                require(translated(item.getHoverName(),"pill")&&!item.getHoverName().getStyle().isItalic()&&item.getHoverName().getStyle().getColor().getValue()==0xffffff,"source styled pill name "+id);
                var lore=item.get(DataComponents.LORE).lines();require(lore.size()==1&&translated(lore.getFirst(),"pill.lore")&&!lore.getFirst().getStyle().isItalic(),"source lore "+id);
                var food=item.get(DataComponents.FOOD);var consume=item.get(DataComponents.CONSUMABLE);
                require(food!=null&&food.nutrition()==1&&food.saturation()==1&&food.canAlwaysEat()&&consume!=null&&consume.consumeSeconds()==.2f,"exact source food/always eat/0.2second native duration "+id);
            }
            require(LbcPills.pillId(new ItemStack(Items.COOKIE))==-1&&LbcPills.pillId(ItemStack.EMPTY)==-1,"missing custom_data cannot trigger valid pill zero");
            require(LbcPills.pillId(raw(Items.COOKIE,"unrelated",1))==-1,"unrelated custom_data cannot default to valid zero");
            for(int id:new int[]{-1,145,Integer.MAX_VALUE})require(LbcPills.pillId(raw(Items.COOKIE,"pill",id))==-1,"invalid raw id "+id);
            require(LbcPills.pillId(raw(Items.COOKIE,"pill",0))==0,"raw imported zero is valid");
            var certificate=loot("lbc:ported/phd_certificate");require(certificate.is(LbcPills.CERTIFICATE)&&LbcPills.certificate(certificate)&&certificate.getCount()==1&&Identifier.parse("lbc:phd").equals(certificate.get(DataComponents.ITEM_MODEL)),"canonical native reusable certificate");
            var lore=certificate.get(DataComponents.LORE).lines();require(lore.size()==4&&lore.getLast().getStyle().isItalic(),"original certificate4 rows and final italic");
        }
        void feedback(Source source){
            var titles=base.recording.packets.stream().filter(ClientboundSetTitleTextPacket.class::isInstance).map(ClientboundSetTitleTextPacket.class::cast).toList();
            var subtitles=base.recording.packets.stream().filter(ClientboundSetSubtitleTextPacket.class::isInstance).map(ClientboundSetSubtitleTextPacket.class::cast).toList();
            var sounds=base.recording.packets.stream().filter(ClientboundSoundPacket.class::isInstance).map(ClientboundSoundPacket.class::cast).filter(packet->packet.getSound().value().location().toString().equals(source.sound())).toList();
            require(titles.size()==1&&titles.getFirst().text().getString().equals(" ")&&subtitles.size()==1&&translated(subtitles.getFirst().text(),"pill_"+source.key()),"exact once source title/subtitle "+source.id());
            require(sounds.size()==1&&sounds.getFirst().getVolume()==1&&sounds.getFirst().getPitch()==1,"exact once source sound "+source.id());
        }
        void eat(ItemStack item){p.setItemInHand(InteractionHand.MAIN_HAND,item.copyWithCount(2));var result=p.getMainHandItem().finishUsingItem(p.level(),p);require(result.getCount()==1&&ItemStack.isSameItemSameComponents(result,item),"actual native survival consume retains components and debits exactlyone");p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);}
        void consumption(){
            for(var source:SOURCE){
                if(source.id()==144)continue;
                reset();Double expectedAttribute=null;
                if(!source.attribute().isEmpty())expectedAttribute=(command("attribute @s minecraft:"+source.attribute()+" base get "+source.scale())+source.step())*source.output();
                for(var action:source.actions())if(action.startsWith("effect give")){
                    var words=action.split(" ");if(words.length>5){int cap=switch(words[3]){case "minecraft:regeneration","minecraft:wither"->5;case "minecraft:poison","minecraft:resistance"->4;case "minecraft:blindness","minecraft:darkness"->2;default->Integer.MAX_VALUE;};words[5]=Integer.toString(Math.min(cap,Integer.parseInt(words[5])));}command(String.join(" ",words));
                }
                // /effect stores an instant instance for the next native effect tick; pills intentionally apply its payload immediately.
                for(var effect:List.copyOf(p.getActiveEffects()))if(effect.getEffect().value().isInstantaneous()){
                    effect.getEffect().value().applyInstantaneousEffect(p.level(),null,null,p,effect.getAmplifier(),1.0);p.removeEffect(effect.getEffect());
                }
                var expectedEffects=effects();float expectedHealth=p.getHealth();reset();
                var counterBefore=new LinkedHashMap<String,Integer>();for(var counter:counters)counterBefore.put(counter,game.items().mana().score(p,counter).get());
                var before=p.position();var velocity=new Vec3(.125,.0625,-.25);p.setDeltaMovement(velocity);p.setKnownMovement(velocity);eat(pill(source.id()));
                require(effects().equals(expectedEffects),"independent native effect-command duration/cap/hidden-layer oracle "+source.id()+" expected="+expectedEffects+" actual="+effects());
                if(expectedAttribute!=null){var key=BuiltInRegistries.ATTRIBUTE.get(Identifier.parse("minecraft:"+source.attribute())).orElseThrow();near(p.getAttribute(key).getBaseValue(),expectedAttribute,"independent source-scaled native attribute "+source.id());}
                else near(p.getHealth(),expectedHealth,"source instant native health consequence "+source.id());
                for(var action:source.actions()){
                    var words=action.split(" ");if(action.startsWith("scoreboard players add"))require(game.items().mana().score(p,words[4]).get()==counterBefore.get(words[4])+Integer.parseInt(words[5]),"exact one source raw counter contribution "+source.id()+"/"+words[4]);
                    if(action.startsWith("tag @s add"))require(p.entityTags().contains(words[3]),"original source scheduler tag "+source.id()+"/"+words[3]);
                }
                if(source.key().equals("paralysis")){near(p.getY(),before.y-2,"native source paralysis relative Y");near(p.getXRot(),-90,"source wrapped relative pitch clamp");require(p.getDeltaMovement().distanceToSqr(velocity)<.0000001,"shared paralysis teleport preserves native velocity");}
                feedback(source);require(p.isAlive(),"bounded source consumption keeps fixture alive "+source.id());
            }
            reset();eat(raw(Items.COOKIE,"pill",144));feedback(SOURCE.get(144));require(effects().isEmpty(),"raw source144 has feedback and no invented payload");
            reset();eat(new ItemStack(Items.COOKIE));require(effects().isEmpty()&&base.recording.packets.stream().noneMatch(ClientboundSetSubtitleTextPacket.class::isInstance),"ordinary cookie never triggers pill zero");
            for(int policy=0;policy<3;policy++){reset();var field=policy(policy);try{near(p.getAttribute(Attributes.MAX_HEALTH).getBaseValue(),1000,"neutral attribute control");eat(policy==2?pill(27).transmuteCopy(Items.COOKIE):pill(27));near(p.getAttribute(Attributes.MAX_HEALTH).getBaseValue(),1002,"physical permanent pill intake under MAG/TECH/strong artifact "+policy);feedback(SOURCE.get(27));}finally{remove(field);}}
        }
        Marker policy(int policy){
            if(policy==2){p.getInventory().setItem(9,new ItemStack(LbcMagic.MYSTICAL_ARTIFACT));game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();require(game.items().magic().suppressed(p),"real strong artifact positive control");return null;}
            var marker=new Marker(EntityTypes.MARKER,p.level());marker.setPos(p.position());marker.entityTags().add(policy==0?"stopper_magic":"stopper");fieldBlocks.put(marker,game.maps().snapshot(p.level(),marker.blockPosition()));require(p.level().addFreshEntity(marker),"real suppression field");game.items().magic().loaded(marker);
            require(policy==0?game.items().magic().suppressed(p):game.items().magic().mechanismSuppressed(p),"real MAG/TECH positive control "+policy);return marker;
        }
        void remove(Marker marker){if(marker!=null){game.items().magic().unloaded(marker);marker.discard();var old=fieldBlocks.remove(marker);if(old!=null&&p.level().getBlockState(marker.blockPosition()).is(LbcMagic.MECHANISM_BEACON_BLOCK))game.maps().restoreSnapshot(p.level(),marker.blockPosition(),old);game.items().magic().blockChanged(marker.blockPosition(),p.level().getBlockState(marker.blockPosition()));}p.getInventory().setItem(9,ItemStack.EMPTY);game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();}
        void flow(boolean imported,int catalyst,boolean shift,int policy){
            reset();var field=policy<0?null:policy(policy);try{
                require(p.containerMenu==p.inventoryMenu,"actual native survival inventory2x2");var menu=(AbstractCraftingMenu)p.containerMenu;var slots=menu.getInputGridSlots();
                var certificate=imported?raw(Items.REPEATING_COMMAND_BLOCK,"phd",1):loot("lbc:ported/phd_certificate");certificate.setCount(2);certificate.set(DataComponents.CUSTOM_NAME,Component.literal("retained metadata"));var expectedCertificate=certificate.copy();
                int n=0;var donors=List.of(new ItemStack(Items.BONE_MEAL,shift?3:2),new ItemStack(Items.APPLE,shift?3:2),new ItemStack(Items.STICK,shift?3:2));
                for(int i=0;i<4;i++){var stack=i==catalyst?certificate:donors.get(n++);if(i!=catalyst)stack.set(DataComponents.CUSTOM_NAME,Component.literal("original donor "+i));slots.get(i).setByPlayer(stack);}
                var preview=menu.getResultSlot().getItem().copy();require(preview.is(LbcPills.PILL)&&preview.getCount()==1,"actual native PHD result slot");
                for(int repeat=0;repeat<3;repeat++)menu.slotsChanged(p.getInventory());require(ItemStack.matches(slots.get(catalyst).getItem(),expectedCertificate)&&donors.stream().allMatch(d->d.getCount()==(shift?3:2)),"actual preview keeps catalyst and donors untouched");
                int before=p.getStats().getValue(Stats.ITEM_CRAFTED.get(LbcPills.PILL));
                if(shift&&policy<0){
                    for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));GameChecks.inventoryClick(p,0,0,ContainerInput.QUICK_MOVE);
                    require(ItemStack.matches(menu.getResultSlot().getItem(),preview)&&ItemStack.matches(slots.get(catalyst).getItem(),expectedCertificate)&&donors.stream().allMatch(d->d.getCount()==3)&&p.getStats().getValue(Stats.ITEM_CRAFTED.get(LbcPills.PILL))==before,"full inventory shift has no donor/catalyst debit or native crafted stat");p.getInventory().setItem(9,ItemStack.EMPTY);
                }
                GameChecks.inventoryClick(p,0,0,shift?ContainerInput.QUICK_MOVE:ContainerInput.PICKUP);
                require(ItemStack.matches(slots.get(catalyst).getItem(),expectedCertificate),"native repeated result take retains exact reusable catalyst count/name/components");
                if(shift){int count=0;for(int i=0;i<36;i++){var s=p.getInventory().getItem(i);if(s.is(LbcPills.PILL)){require(ItemStack.isSameItemSameComponents(s,preview),"shift canonical output components");count+=s.getCount();}}require(count==3&&menu.getCarried().isEmpty(),"shift crafts exactlythree into actual inventory");for(int i=0;i<4;i++)if(i!=catalyst)require(slots.get(i).getItem().isEmpty(),"shift consumes exactlythree original donors per slot");}
                else{require(ItemStack.matches(menu.getCarried(),preview),"actual pickup moves one exact canonical output");for(int i=0;i<4;i++)if(i!=catalyst)require(slots.get(i).getItem().getCount()==1&&slots.get(i).getItem().getHoverName().getString().equals("original donor "+i),"pickup debits exactlyone retaining donor components");}
                require(p.getStats().getValue(Stats.ITEM_CRAFTED.get(LbcPills.PILL))==before+(shift?3:1),"native crafted stat exactlyonce per output");
                require(base.recording.packets.stream().noneMatch(ClientboundSetSubtitleTextPacket.class::isInstance),"crafting never invokes pill consumption reward");
            }finally{remove(field);}
        }
        public void close(){if(closed)return;closed=true;for(var marker:List.copyOf(fieldBlocks.keySet()))remove(marker);try{reset();}finally{try{base.close();}finally{saved.restore(game);saved.restoreAttributes();saved.restoreUse();stats.forEach((stat,value)->p.getStats().setValue(p,stat,value));}}}
    }
    private static ItemStack raw(Item item,String key,int value){var stack=new ItemStack(item);var data=new CompoundTag();data.putInt(key,value);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return stack;}
    public static void menu(GameController game){
        require(game.inLobby(),"actual lobby catalogue selection");var p=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(p!=null,"native menu participant");
        var saved=new RegenerationBeltChecks.Snapshot(game,p);boolean ranked=game.config().ranked,kits=game.config().lbcKits;
        String gate="lbc:true_advancements/legends/academy/badwiki";var advancement=game.server().getAdvancements().get(Identifier.parse(gate));require(advancement!=null,"native original BadWiki gate");
        var criteria=new LinkedHashMap<String,Boolean>();var progress=p.getAdvancements().getOrStartProgress(advancement);for(var key:advancement.value().criteria().keySet())criteria.put(key,progress.getCriterion(key).isDone());
        try{
            game.config().ranked=false;game.config().lbcKits=true;game.lbc().syncRanked();var profile=game.players().get(p.getUUID());profile.lbcKit=0;profile.lbcChallengeKit=0;profile.lbcSkill=0;profile.lbcChallenge=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);Arrays.fill(profile.levelups,0);
            for(var criterion:criteria.keySet())p.getAdvancements().revoke(advancement,criterion);profile.lbcUnlocks.remove(gate);
            require(!game.lbc().select(p,LbcLoadouts.Kind.KITS,9000,17)&&profile.lbcKit==0,"locked original slot17 refuses PhD kit18");
            for(var criterion:criteria.keySet())p.getAdvancements().award(advancement,criterion);
            require(game.lbc().select(p,LbcLoadouts.Kind.KITS,9000,17)&&profile.lbcKit==18&&game.lbc().selected(profile,LbcLoadouts.Kind.KITS,9000,17),"actual original menu selects kit18 through BadWiki gate");
            p.getInventory().clearContent();game.lbc().apply(p,false);var contents=new ArrayList<ItemStack>();for(int i=0;i<p.getInventory().getContainerSize();i++)if(!p.getInventory().getItem(i).isEmpty())contents.add(p.getInventory().getItem(i));
            require(contents.stream().filter(LbcPills::certificate).mapToInt(ItemStack::getCount).sum()==1&&contents.stream().filter(s->s.is(Items.BONE_MEAL)).mapToInt(ItemStack::getCount).sum()==3,"normal selected-kit grant one certificate plus three compost");
            require(contents.stream().allMatch(s->LbcPills.certificate(s)||s.is(Items.BONE_MEAL)),"no invented kit grants");
            for(var compost:contents)if(compost.is(Items.BONE_MEAL))require(translated(compost.getHoverName(),"compost")&&compost.get(DataComponents.LORE).lines().size()==1&&translated(compost.get(DataComponents.LORE).lines().getFirst(),"compost.lore"),"three authored named/lore compost");
        }finally{game.config().ranked=ranked;game.config().lbcKits=kits;game.lbc().syncRanked();for(var c:criteria.entrySet())if(c.getValue())p.getAdvancements().award(advancement,c.getKey());else p.getAdvancements().revoke(advancement,c.getKey());saved.restore(game);saved.restoreAttributes();saved.restoreUse();saved.restoreProfile(game);game.players().save();}
        SkyWars2.LOGGER.info("[check] PASS phd-kit-menu (actual lobby original page9000/slot17/BadWiki gate locked and unlocked, selected kit18 normal certificate plus three source compost)");
    }
    private static int sourceHash(int seed,List<ItemStack> donors){
        int hash=seed;for(var donor:donors){String id=BuiltInRegistries.ITEM.getKey(donor.getItem()).toString();for(int i=10;i<Math.min(id.length(),37);i++){char c=id.charAt(i);if(c>='a'&&c<='z')hash+=c-'a'+1;else if(c=='_')hash+=27;}}
        return Math.floorMod(hash,144);
    }
    public static void crafting(GameController game){try(var fixture=new Fixture(game)){
        var scoreboard=game.server().getScoreboard();var objective=scoreboard.getObjective("lbc.math");var sourceSeed=objective==null?null:scoreboard.getPlayerScoreInfo(ScoreHolder.forNameOnly("#game_seed"),objective);
        require(sourceSeed!=null&&sourceSeed.value()>=1&&sourceSeed.value()<=10000000,"actual source once-per-match seed1..10000000");int seed=sourceSeed.value();
        for(boolean imported:new boolean[]{false,true})for(int catalyst=0;catalyst<4;catalyst++){
            var donors=List.of(new ItemStack(Items.BONE_MEAL,4),new ItemStack(Items.APPLE,3),new ItemStack(Items.STICK,2));
            var certificate=imported?raw(Items.REPEATING_COMMAND_BLOCK,"phd",1):fixture.loot("lbc:ported/phd_certificate");certificate.setCount(2);certificate.set(DataComponents.CUSTOM_NAME,Component.literal("retained original certificate"));
            var grid=new ArrayList<ItemStack>();int index=0;for(int slot=0;slot<4;slot++)grid.add(slot==catalyst?certificate:donors.get(index++));var input=CraftingInput.of(2,2,grid);
            require(LbcPhdCrafting.matches(input),"source complete2x2 recipe, certificate position "+catalyst+" imported="+imported);
            var expected=fixture.pill(sourceHash(seed,donors));for(int repeat=0;repeat<3;repeat++)require(ItemStack.matches(LbcPhdCrafting.result(game,input),expected),"deterministic actual source hash and canonical144 output leaf");
            require(grid.get(catalyst).getCount()==2&&donors.getFirst().getCount()==4&&donors.get(1).getCount()==3&&donors.getLast().getCount()==2,"repeated pure result preview has no debit");
            require(fixture.base.recording.packets.stream().noneMatch(ClientboundSetSubtitleTextPacket.class::isInstance),"recipe preview has no consumption feedback");
        }
        var rawDonors=List.of(new ItemStack(Items.COOKIE),new ItemStack(Items.APPLE),new ItemStack(Items.STICK));
        var aliasInput=CraftingInput.of(2,2,List.of(fixture.loot("lbc:ported/phd_certificate"),fixture.pill(0),rawDonors.get(1),rawDonors.get(2)));
        require(LbcPhdCrafting.matches(aliasInput)&&ItemStack.matches(LbcPhdCrafting.result(game,aliasInput),fixture.pill(sourceHash(seed,rawDonors))),"native Polymer biological donor uses exact original COOKIE tag and hash identity");
        var bucketCertificate=fixture.loot("lbc:ported/phd_certificate");bucketCertificate.set(DataComponents.CUSTOM_NAME,Component.literal("original catalyst remainder"));
        var bucketInput=CraftingInput.of(2,2,List.of(new ItemStack(Items.COD_BUCKET),new ItemStack(Items.APPLE),bucketCertificate,new ItemStack(Items.STICK)));
        require(LbcCraftingRecipe.craftingKind(bucketInput)==LbcCraftingRecipe.Kind.PHD,"source biological fish bucket accepts native PHD recipe");var remainders=LbcCraftingRecipe.Kind.PHD.recipe.getRemainingItems(bucketInput);
        for(int i=0;i<4;i++)require(i==2?ItemStack.matches(remainders.get(i),bucketCertificate.copyWithCount(1)):remainders.get(i).isEmpty(),"only original certificate remainder; biological fish consumes its source bucket too");
        var certificate=fixture.loot("lbc:ported/phd_certificate");
        for(var invalid:List.of(
                CraftingInput.of(2,2,List.of(new ItemStack(Items.APPLE),new ItemStack(Items.STICK),new ItemStack(Items.BONE_MEAL),new ItemStack(Items.WHEAT))),
                CraftingInput.of(2,2,List.of(certificate,new ItemStack(Items.APPLE),new ItemStack(Items.STICK),new ItemStack(Items.IRON_BLOCK))),
                CraftingInput.of(2,2,List.of(certificate,new ItemStack(Items.APPLE),new ItemStack(Items.STICK),ItemStack.EMPTY)),
                CraftingInput.of(2,2,List.of(raw(Items.PAPER,"phd",2),new ItemStack(Items.APPLE),new ItemStack(Items.STICK),new ItemStack(Items.BONE_MEAL))),
                CraftingInput.of(1,4,List.of(certificate,new ItemStack(Items.APPLE),new ItemStack(Items.STICK),new ItemStack(Items.BONE_MEAL)))))
            require(!LbcPhdCrafting.matches(invalid)&&LbcPhdCrafting.result(game,invalid).isEmpty(),"wrong catalyst/nonbio/missing fourth/not2x2 cannot create pill");
        require(sourceSeed.value()==seed,"preview leaves actual match seed intact");
        for(boolean imported:new boolean[]{false,true})for(int catalyst=0;catalyst<4;catalyst++)for(boolean shift:new boolean[]{false,true})fixture.flow(imported,catalyst,shift,-1);
        for(int policy=0;policy<3;policy++)fixture.flow(policy==2,policy,false,policy);
    }SkyWars2.LOGGER.info("[check] PASS phd-crafting (actual once-per-match source seed and independent exact character hash, deterministic canonical144 output, native2x2 pickup/shift/full inventory, raw/native catalyst permutations and exact retained components, one donor debit, preview purity, invalid grids, MAG/TECH/artifact ungated)");}
    public static void consumption(GameController game){try(var fixture=new Fixture(game)){fixture.presentation();fixture.consumption();}SkyWars2.LOGGER.info("[check] PASS phd-pills (144 canonical native source leaves,144 actual native survival consumes, independent command effect/attribute anchors, exact raw counters/tags and once-only title/sound/debit, raw144 feedback-only, missing-zero negative, bounded synchronous cleanup)");}

    /** Two real raw consumers across native ticks, with long global payloads excluded. */
    private static final class Consequences implements AutoCloseable {
        final GameController game;final Fixture fixture;final ServerPlayer p;final MatchEntityState body;
        final RegenerationBeltChecks.ScheduledClient client;final SauvojenMeditationChecks.Attribution attribution;
        final LbcLegacyEffects legacy;final LbcPersonalStasis stasis;final Vec3 at,momentum=new Vec3(.125,.0625,-.25);
        final List<net.minecraft.world.entity.Entity> actors=new ArrayList<>();final List<Marker> fields=new ArrayList<>();
        final Map<java.lang.reflect.Field,Object> clocks=new LinkedHashMap<>();
        Marker movable,strict,inside,outside;net.minecraft.world.entity.animal.cow.Cow observer;
        int phase,started,phaseStarted,observerTicks;long actions;boolean ownsStop,closed;
        Vec3 moved,anchor;float yaw,pitch;
        Consequences(GameController game){
            this.game=game;p=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(p!=null&&game.server().tickRateManager().runsNormally(),"native running scheduled consequence fixture");
            body=new MatchEntityState(game,p);fixture=new Fixture(game);legacy=game.items().legacyEffects();stasis=legacy.personalStasis();at=p.position();actions=legacy.actions();
            for(var name:List.of("lastTick","actions"))try{var field=legacy.getClass().getDeclaredField(name);field.setAccessible(true);clocks.put(field,field.get(legacy));}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            attribution=new SauvojenMeditationChecks.Attribution(game,List.of(p));client=new RegenerationBeltChecks.ScheduledClient(game,p);
        }
        Marker marker(Vec3 position,String tag){var marker=new Marker(EntityTypes.MARKER,p.level());marker.setPos(position);if(tag!=null)marker.entityTags().add(tag);require(p.level().addFreshEntity(marker),"resident native consequence marker");actors.add(marker);return marker;}
        int score(String key){return game.items().mana().score(p,key).get();}
        void begin(){
            require(!stasis.active(p)&&!game.items().timeStop().stopped(p.level()),"no borrowed stasis or world stop");
            for(var entity:p.level().getAllEntities())if(!(entity instanceof net.minecraft.world.entity.player.Player)&&!entity.entityTags().contains("strict_map_object"))require(entity.position().distanceToSqr(at)>10000,"exclusive high-altitude vacuum sphere without borrowed entities");
            p.noPhysics=true;p.setLastClientInput(new net.minecraft.world.entity.player.Input(false,false,false,false,false,false,false));
            movable=marker(at.add(12,0,0),null);movable.setDeltaMovement(momentum);strict=marker(at.add(15,0,0),"strict_map_object");inside=marker(at.add(0,99,0),null);outside=marker(at.add(0,101,0),null);
            observer=new net.minecraft.world.entity.animal.cow.Cow(EntityTypes.COW,p.level());observer.setPos(at.add(5,0,0));observer.setNoAi(true);observer.setNoGravity(true);observer.noPhysics=true;require(p.level().addFreshEntity(observer),"actual native ticking world observer");actors.add(observer);
            for(int policy=0;policy<2;policy++){var field=fixture.policy(policy);fields.add(field);actors.add(field);}fixture.policy(2);
            require(game.items().magic().suppressed(p)&&game.items().magic().mechanismSuppressed(p),"real MAG/TECH/strong artifact positive controls coexist");
            p.entityTags().add("sin_greed_active");fixture.eat(fixture.pill(119));require(score("effect_vacuum")==1,"actual native pill119 adopts one shared vacuum contribution");
            game.items().timeStop().begin(p.level(),100);ownsStop=true;started=phaseStarted=game.server().getTickCount();phase=0;
        }
        void next(int value){phase=value;phaseStarted=game.server().getTickCount();}
        void clearFields(){for(var field:List.copyOf(fields)){fixture.remove(field);actors.remove(field);}fields.clear();fixture.remove(null);}
        boolean step(){
            client.acknowledge();int now=game.server().getTickCount();require(now-started<35,"bounded real scheduled consequences");int elapsed=now-phaseStarted;if(elapsed<1)return false;
            switch(phase){
                case 0->{
                    require(score("effect_vacuum")==1&&movable.position().distanceToSqr(at.add(12,0,0))<.0000001&&legacy.actions()==actions,"native Time Stop preserves raw vacuum queue and prevents real action");
                    if(elapsed<2)return false;game.items().timeStop().end(p.level());ownsStop=false;next(1);
                }
                case 1->{
                    require(score("effect_vacuum")==0&&legacy.actions()==actions+1,"real resumed scheduler drains exactlyone vacuum contribution");
                    require(movable.position().distanceToSqr(at)<.0000001&&inside.position().distanceToSqr(at)<.0000001,"source all-nonplayer vacuum includes marker and distance99 sphere target under Greed/MAG/TECH/artifact");
                    require(strict.position().distanceToSqr(at.add(15,0,0))<.0000001&&outside.position().distanceToSqr(at.add(0,101,0))<.0000001,"source strict-map and distance101 exclusions");
                    require(movable.getDeltaMovement().distanceToSqr(momentum)<.0000001,"shared native vacuum teleport preserves independent momentum");
                    near(movable.getYRot(),p.getYRot(),"source vacuum wearer yaw");near(movable.getXRot(),p.getXRot(),"source vacuum wearer pitch");
                    moved=at.add(9,2,0);movable.setPos(moved);fixture.eat(fixture.pill(120));require(score("effect_timestop")==1,"actual native pill120 adopts one shared personal-stasis contribution");next(2);
                }
                case 2->{
                    require(movable.position().distanceToSqr(moved)<.0000001&&legacy.actions()==actions+1,"completed vacuum is never replayed by later native ticks");
                    require(!stasis.active(p)&&score("effect_timestop")==1,"source Greed excludes personal stasis without spending queued contribution");
                    if(elapsed<2)return false;p.entityTags().remove("sin_greed_active");anchor=p.position();yaw=p.getYRot();pitch=p.getXRot();next(3);
                }
                case 3->{
                    require(stasis.active(p)&&score("effect_timestop")==0&&legacy.actions()==actions+2,"real scheduler admits stasis under MAG/TECH/artifact exactlyonce after Greed ends");
                    require(stasis.remaining(p)==LbcPersonalStasis.DEFAULT_DURATION_TICKS&&!game.items().timeStop().stopped(p.level()),"source personal anchor duration and no world Time Stop");
                    clearFields();
                    var image=new MatchEntityState(game,p);int remaining=stasis.remaining(p);boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
                    try{stasis.clear(p);require(!stasis.active(p),"native anchor clear before image replay");image.load(p);require(stasis.active(p)&&stasis.remaining(p)==remaining,"positive persistent native attachment image restores the active anchor");}finally{game.server().tickRateManager().setFrozen(frozen);}client.acknowledge();
                    observerTicks=observer.tickCount;p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING,100,0));
                    require(p.teleportTo(p.level(),anchor.x+2,anchor.y+1,anchor.z-1,Set.of(),yaw+37,pitch+18,true),"actual native attempted pose/camera movement");client.acknowledge();next(4);
                }
                case 4->{
                    require(stasis.active(p)&&p.position().distanceToSqr(anchor)<.0000001,"actual recurring native tick holds personal xyz");near(p.getYRot(),yaw,"source anchored yaw");near(p.getXRot(),pitch,"source anchored pitch");
                    require(!p.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING)&&p.getActiveEffects().size()==3,"each recurring frame clears existing native effects and leaves source three");
                    for(var type:List.of(net.minecraft.world.effect.MobEffects.RESISTANCE,net.minecraft.world.effect.MobEffects.WEAKNESS,net.minecraft.world.effect.MobEffects.SLOWNESS)){
                        int amplifier=type.equals(net.minecraft.world.effect.MobEffects.RESISTANCE)?4:127;var expected=ItemEffectLevels.create(type,20,amplifier,false,false);var actual=p.getEffect(type);
                        require(actual!=null&&actual.getAmplifier()==expected.getAmplifier()&&actual.getDuration()>=19&&actual.getDuration()<=20&&!actual.isVisible()&&!actual.showIcon(),"source hidden one-second recurring stasis effect with shared native cap "+type);
                    }
                    require(observer.tickCount>observerTicks&&!game.items().timeStop().stopped(p.level()),"independent native world observer ticks while only wearer is anchored");
                    require(score("effect_timestop")==0&&legacy.actions()==actions+2,"recurring anchor is not a repeated raw source action");
                    require(p.teleportTo(p.level(),anchor.x+3,anchor.y,anchor.z,Set.of(),yaw+11,pitch,true),"move pose before actual world stop captures its own frozen pose");client.acknowledge();moved=p.position();game.items().timeStop().begin(p.level(),100);ownsStop=true;next(5);
                }
                case 5->{
                    require(stasis.active(p)&&p.position().distanceToSqr(moved)<.0000001,"native world Time Stop pauses personal anchor correction");
                    game.items().timeStop().end(p.level());ownsStop=false;next(6);
                }
                case 6->{
                    require(stasis.active(p)&&p.position().distanceToSqr(anchor)<.0000001&&legacy.actions()==actions+2,"resumed personal anchor corrects pose without replaying intake");
                    p.setItemInHand(InteractionHand.MAIN_HAND,fixture.pill(27).copyWithCount(2));
                    require(p.gameMode.useItem(p,p.level(),p.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction()&&p.isUsingItem(),"personal anchor adds no extra native item-action ban");next(7);
                }
                case 7->{
                    if(p.isUsingItem()){require(elapsed<=6,"bounded actual native four-tick pill use while personally anchored");return false;}
                    require(elapsed>=3&&p.getMainHandItem().getCount()==1,"actual scheduled native food completion debits once while anchor stays active");
                    near(p.getAttribute(Attributes.MAX_HEALTH).getBaseValue(),1002,"actual scheduled native pill reward remains usable under personal anchor");
                    require(stasis.active(p)&&legacy.actions()==actions+2,"ordinary item completion neither removes anchor nor replays raw source actions");return true;
                }
                default->throw new IllegalStateException("PhD scheduled phase "+phase);
            }return false;
        }
        public void close(){
            if(closed)return;closed=true;try{if(ownsStop){game.items().timeStop().end(p.level());ownsStop=false;}clearFields();stasis.clear(p);p.entityTags().remove("sin_greed_active");for(var actor:actors){game.items().entityUnloaded(actor);actor.discard();}actors.clear();}
            finally{try{client.close();fixture.close();attribution.close();}finally{boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);try{body.load(p);for(var entry:clocks.entrySet())entry.getKey().set(legacy,entry.getValue());}catch(IllegalAccessException error){throw new IllegalStateException(error);}finally{game.server().tickRateManager().setFrozen(frozen);}}}
        }
    }
    private static Consequences consequences;
    public static void startConsequences(GameController game){require(consequences==null,"exclusive scheduled PhD fixture");var state=new Consequences(game);try{state.begin();consequences=state;SkyWars2.LOGGER.info("[check] START phd-consequences");}catch(Throwable error){try{state.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    public static void tick(GameController game){if(consequences==null)return;try{if(!consequences.step())return;consequences.close();consequences=null;SkyWars2.LOGGER.info("[check] PASS phd-consequences (native119/120 intake and actual shared scheduler actions once, Time Stop queue/anchor pauses and resume, Greed vacuum inclusion/stasis exclusion, MAG/TECH/artifact controls, source radius100/strict-map/pose/momentum, personal recurring effects and independent world ticks, canonical native attachment restore)");}catch(Throwable error){try{consequences.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{consequences=null;}SkyWars2.LOGGER.error("[check] FAIL phd-consequences",error);}}
}
