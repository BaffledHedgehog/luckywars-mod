package me.skywars2.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;

/** Check-only view of item definitions; never changes the runtime loot graph. */
public final class LootDefinitionChecks {
    private LootDefinitionChecks() { }
    private static final Set<String> SELECTION = Set.of("weight", "quality", "condition", "conditions", "functions");
    private static final Set<String> FUNCTIONS = Set.of("set_components", "set_count", "set_name", "set_lore",
            "set_custom_data", "set_enchantments", "set_attributes", "enchant_with_levels", "toggle_tooltips",
            "set_damage", "explosion_decay", "copy_components", "fill_player_head");
    private static final String MELLSTROY_PROTOTYPE = "lbc:item_defs/lbc/mellstroy_mask__fc2cbe716754";
    // Exact reused targets introduced by canonicalization, independent of unreleased source pools.
    private static final String[] REUSED = {
        "lbc:lightning_bolt_egg",
        "lbc:ported/acid_burning_grimoire",
        "lbc:ported/acid_burst_drop",
        "lbc:ported/acid_corruption_death_grimoire",
        "lbc:ported/acid_corruption_grimoire",
        "lbc:ported/acid_corruption_mana_grimoire",
        "lbc:ported/acid_corruption_reverse_grimoire",
        "lbc:ported/acid_grimoire",
        "lbc:ported/advanced_ender_clip_grimoire",
        "lbc:ported/advanced_ender_grimoire",
        "lbc:ported/advanced_ender_safe_grimoire",
        "lbc:ported/advanced_ender_top_grimoire",
        "lbc:ported/air_burst_grimoire",
        "lbc:ported/air_fast_grimoire",
        "lbc:ported/air_grimoire",
        "lbc:ported/air_homing_grimoire",
        "lbc:ported/ak47",
        "lbc:ported/artifact_jump_lift",
        "lbc:ported/awakened_acid_caust_burning_grimoire",
        "lbc:ported/awakened_acid_caust_grimoire",
        "lbc:ported/awakened_acid_gatling_burning_grimoire",
        "lbc:ported/awakened_acid_gatling_grimoire",
        "lbc:ported/awakened_acid_grimoire",
        "lbc:ported/awakened_acid_rifle_grimoire",
        "lbc:ported/badwiki_01_grimoire",
        "lbc:ported/badwiki_02_grimoire",
        "lbc:ported/badwiki_03_grimoire",
        "lbc:ported/badwiki_04_grimoire",
        "lbc:ported/badwiki_05_grimoire",
        "lbc:ported/badwiki_06_grimoire",
        "lbc:ported/badwiki_07_grimoire",
        "lbc:ported/badwiki_08_grimoire",
        "lbc:ported/badwiki_09_grimoire",
        "lbc:ported/badwiki_10_grimoire",
        "lbc:ported/badwiki_11_grimoire",
        "lbc:ported/badwiki_12_grimoire",
        "lbc:ported/badwiki_13_grimoire",
        "lbc:ported/badwiki_14_grimoire",
        "lbc:ported/badwiki_15_grimoire",
        "lbc:ported/badwiki_16_grimoire",
        "lbc:ported/badwiki_17_grimoire",
        "lbc:ported/badwiki_18_grimoire",
        "lbc:ported/badwiki_19_grimoire",
        "lbc:ported/badwiki_20_grimoire",
        "lbc:ported/badwiki_21_grimoire",
        "lbc:ported/badwiki_22_grimoire",
        "lbc:ported/badwiki_23_grimoire",
        "lbc:ported/badwiki_24_grimoire",
        "lbc:ported/badwiki_25_grimoire",
        "lbc:ported/badwiki_26_grimoire",
        "lbc:ported/badwiki_27_grimoire",
        "lbc:ported/badwiki_28_grimoire",
        "lbc:ported/badwiki_29_grimoire",
        "lbc:ported/badwiki_30_grimoire",
        "lbc:ported/badwiki_31_grimoire",
        "lbc:ported/badwiki_32_grimoire",
        "lbc:ported/badwiki_33_grimoire",
        "lbc:ported/badwiki_34_grimoire",
        "lbc:ported/badwiki_35_grimoire",
        "lbc:ported/badwiki_36_grimoire",
        "lbc:ported/badwiki_37_grimoire",
        "lbc:ported/badwiki_38_grimoire",
        "lbc:ported/badwiki_39_grimoire",
        "lbc:ported/badwiki_40_grimoire",
        "lbc:ported/badwiki_41_grimoire",
        "lbc:ported/badwiki_42_grimoire",
        "lbc:ported/badwiki_43_grimoire",
        "lbc:ported/badwiki_44_grimoire",
        "lbc:ported/badwiki_45_grimoire",
        "lbc:ported/badwiki_46_grimoire",
        "lbc:ported/badwiki_47_grimoire",
        "lbc:ported/badwiki_48_grimoire",
        "lbc:ported/badwiki_49_grimoire",
        "lbc:ported/badwiki_50_grimoire",
        "lbc:ported/badwiki_51_grimoire",
        "lbc:ported/badwiki_52_grimoire",
        "lbc:ported/badwiki_53_grimoire",
        "lbc:ported/badwiki_54_grimoire",
        "lbc:ported/badwiki_55_grimoire",
        "lbc:ported/badwiki_56_grimoire",
        "lbc:ported/badwiki_57_grimoire",
        "lbc:ported/badwiki_58_grimoire",
        "lbc:ported/badwiki_59_grimoire",
        "lbc:ported/badwiki_60_grimoire",
        "lbc:ported/badwiki_61_grimoire",
        "lbc:ported/badwiki_62_grimoire",
        "lbc:ported/badwiki_63_grimoire",
        "lbc:ported/badwiki_64_grimoire",
        "lbc:ported/badwiki_65_grimoire",
        "lbc:ported/badwiki_66_grimoire",
        "lbc:ported/badwiki_67_grimoire",
        "lbc:ported/badwiki_68_grimoire",
        "lbc:ported/badwiki_69_grimoire",
        "lbc:ported/badwiki_70_grimoire",
        "lbc:ported/badwiki_71_grimoire",
        "lbc:ported/badwiki_72_grimoire",
        "lbc:ported/badwiki_73_grimoire",
        "lbc:ported/badwiki_74_grimoire",
        "lbc:ported/badwiki_75_grimoire",
        "lbc:ported/badwiki_76_grimoire",
        "lbc:ported/badwiki_77_grimoire",
        "lbc:ported/badwiki_78_grimoire",
        "lbc:ported/badwiki_79_grimoire",
        "lbc:ported/badwiki_80_grimoire",
        "lbc:ported/badwiki_81_grimoire",
        "lbc:ported/badwiki_82_grimoire",
        "lbc:ported/badwiki_83_grimoire",
        "lbc:ported/badwiki_84_grimoire",
        "lbc:ported/badwiki_grimoire",
        "lbc:ported/banhammer",
        "lbc:ported/bass_blindness_grimoire",
        "lbc:ported/bass_fatigue_grimoire",
        "lbc:ported/bass_grimoire",
        "lbc:ported/bass_slowness_grimoire",
        "lbc:ported/bass_weakness_grimoire",
        "lbc:ported/bedrock_clear_cubes_grimoire",
        "lbc:ported/bedrock_cube_grimoire",
        "lbc:ported/bedrock_grimoire",
        "lbc:ported/bedrock_regen_grimoire",
        "lbc:ported/bedrock_remove_grimoire",
        "lbc:ported/blaze_grimoire",
        "lbc:ported/blaze_head_grimoire",
        "lbc:ported/blazefire_sword",
        "lbc:ported/blind_gatling_grimoire",
        "lbc:ported/blind_grimoire",
        "lbc:ported/blind_phantom_grimoire",
        "lbc:ported/blind_rain_grimoire",
        "lbc:ported/blind_spike",
        "lbc:ported/blood_grimoire",
        "lbc:ported/blood_safe_grimoire",
        "lbc:ported/boltbolt_grimoire",
        "lbc:ported/boltbolt_meteor_grimoire",
        "lbc:ported/boltbolt_racer_grimoire",
        "lbc:ported/boltbolt_watering_grimoire",
        "lbc:ported/caustic_acid_burning_grimoire",
        "lbc:ported/caustic_acid_grimoire",
        "lbc:ported/chaos_sword",
        "lbc:ported/confuse_grimoire",
        "lbc:ported/confuse_invisibility_grimoire",
        "lbc:ported/confuse_levitation_grimoire",
        "lbc:ported/confuse_speed_grimoire",
        "lbc:ported/confuse_teleport_grimoire",
        "lbc:ported/consuming_avarice_grimoire",
        "lbc:ported/consuming_end_grimoire",
        "lbc:ported/consuming_grimoire",
        "lbc:ported/consuming_silence_grimoire",
        "lbc:ported/corruption_damage_grimoire",
        "lbc:ported/corruption_grimoire",
        "lbc:ported/corruption_hunger_grimoire",
        "lbc:ported/corruption_poison_grimoire",
        "lbc:ported/corruption_wither_grimoire",
        "lbc:ported/danmaku_grimoire",
        "lbc:ported/death_scythe",
        "lbc:ported/duplicating_divide_grimoire",
        "lbc:ported/duplicating_expand_grimoire",
        "lbc:ported/duplicating_grimoire",
        "lbc:ported/duplicating_hole_grimoire",
        "lbc:ported/efficient_shovel",
        "lbc:ported/electro_grimoire",
        "lbc:ported/electro_ray_grimoire",
        "lbc:ported/electro_shield",
        "lbc:ported/ender_grimoire",
        "lbc:ported/ender_safe_grimoire",
        "lbc:ported/ender_spike",
        "lbc:ported/enhanced_flint",
        "lbc:ported/erecto_grimoire",
        "lbc:ported/erecto_ray_grimoire",
        "lbc:ported/erecto_sniper_grimoire",
        "lbc:ported/erecto_sphere_grimoire",
        "lbc:ported/evoker_fan_grimoire",
        "lbc:ported/evoker_grimoire",
        "lbc:ported/evoker_ring_grimoire",
        "lbc:ported/evoker_spiral_grimoire",
        "lbc:ported/exahoming_curse_grimoire",
        "lbc:ported/exahoming_destiny_grimoire",
        "lbc:ported/exahoming_grimoire",
        "lbc:ported/exahoming_sisyphus_grimoire",
        "lbc:ported/exasperato_chaos_grimoire",
        "lbc:ported/exasperato_grimoire",
        "lbc:ported/exasperato_nuke_grimoire",
        "lbc:ported/exasperato_ray_grimoire",
        "lbc:ported/exasperato_triple_grimoire",
        "lbc:ported/exoperso_awakened_grimoire",
        "lbc:ported/exoperso_grimoire",
        "lbc:ported/explosive_spike",
        "lbc:ported/falling_stars_big_grimoire",
        "lbc:ported/falling_stars_grimoire",
        "lbc:ported/falling_stars_rain_grimoire",
        "lbc:ported/fartus_grimoire",
        "lbc:ported/fire_big_grimoire",
        "lbc:ported/fire_grimoire",
        "lbc:ported/fire_ray_grimoire",
        "lbc:ported/fireblaze_grimoire",
        "lbc:ported/fireblaze_kiuas_grimoire",
        "lbc:ported/fireblaze_mega_grimoire",
        "lbc:ported/fireblaze_pillars_grimoire",
        "lbc:ported/fireblaze_world_grimoire",
        "lbc:ported/gaycaster_blue_grimoire",
        "lbc:ported/gaycaster_cyan_grimoire",
        "lbc:ported/gaycaster_green_grimoire",
        "lbc:ported/gaycaster_grimoire",
        "lbc:ported/gaycaster_orange_grimoire",
        "lbc:ported/gaycaster_purple_grimoire",
        "lbc:ported/gaycaster_red_grimoire",
        "lbc:ported/gaycaster_yellow_grimoire",
        "lbc:ported/god",
        "lbc:ported/govnomet",
        "lbc:ported/halloween_mace",
        "lbc:ported/halloween_wind_charges_grimoire",
        "lbc:ported/halloween_wind_grimoire",
        "lbc:ported/halloween_wind_others_grimoire",
        "lbc:ported/halloween_wind_self_grimoire",
        "lbc:ported/hammer",
        "lbc:ported/harpoon",
        "lbc:ported/hedgehog_grimoire",
        "lbc:ported/hedgehog_healer_grimoire",
        "lbc:ported/hedgehog_laser_grimoire",
        "lbc:ported/hedgehog_sniper_grimoire",
        "lbc:ported/hedgehog_tank_grimoire",
        "lbc:ported/helicopter_boss",
        "lbc:ported/ice_grimoire",
        "lbc:ported/impulse_spike",
        "lbc:ported/infinite_totem",
        "lbc:ported/kfc_chick",
        "lbc:ported/kfc_gun",
        "lbc:ported/legacy_artifact_jump_levitation",
        "lbc:ported/legacy_devil_sword",
        "lbc:ported/legacy_ender_sword",
        "lbc:ported/legacy_epic_sword",
        "lbc:ported/legacy_feeding_sword",
        "lbc:ported/legacy_magnet",
        "lbc:ported/legacy_reactive_artifact_4",
        "lbc:ported/legacy_sand_amulet",
        "lbc:ported/legacy_save_platform",
        "lbc:ported/legacy_vampire_fang",
        "lbc:ported/lifestealer_charge_grimoire",
        "lbc:ported/lifestealer_grimoire",
        "lbc:ported/lifestealer_strong_grimoire",
        "lbc:ported/lightning_chain_grimoire",
        "lbc:ported/lightning_grimoire",
        "lbc:ported/lightning_reforge_grimoire",
        "lbc:ported/magnet",
        "lbc:ported/magnum",
        "lbc:ported/metal_pipe_divider",
        "lbc:ported/metasola_1",
        "lbc:ported/metasola_2",
        "lbc:ported/metasola_3",
        "lbc:ported/metasola_4",
        "lbc:ported/metasola_5",
        "lbc:ported/metasola_6",
        "lbc:ported/mobgun",
        "lbc:ported/moisei_boots",
        "lbc:ported/mortis",
        "lbc:ported/neutral_big_fireball_grimoire",
        "lbc:ported/neutral_fireballs_grimoire",
        "lbc:ported/neutral_fireworks_grimoire",
        "lbc:ported/neutral_grimoire",
        "lbc:ported/neutral_potion_grimoire",
        "lbc:ported/neutral_skulls_grimoire",
        "lbc:ported/omega_grimoire",
        "lbc:ported/omega_safe_grimoire",
        "lbc:ported/piston_sword",
        "lbc:ported/poison_spike",
        "lbc:ported/poleaxe",
        "lbc:ported/prism_awakened_grimoire",
        "lbc:ported/prism_grimoire",
        "lbc:ported/psycho_grimoire",
        "lbc:ported/psycho_look_back_grimoire",
        "lbc:ported/psycho_scare_grimoire",
        "lbc:ported/psycho_screamer_grimoire",
        "lbc:ported/psycho_sound_grimoire",
        "lbc:ported/psychoblind_grimoire",
        "lbc:ported/psychoblind_invisible_grimoire",
        "lbc:ported/psychoblind_ray_grimoire",
        "lbc:ported/psychoblind_spray_grimoire",
        "lbc:ported/psychovex_wand",
        "lbc:ported/random_all_grimoire",
        "lbc:ported/random_enchanted_shovel",
        "lbc:ported/random_grimoire",
        "lbc:ported/reactive_artifact_4",
        "lbc:ported/reroll_machine",
        "lbc:ported/ruby_grimoire",
        "lbc:ported/ruby_hole_grimoire",
        "lbc:ported/ruby_ray_grimoire",
        "lbc:ported/sheep_awakened_grimoire",
        "lbc:ported/sheep_grimoire",
        "lbc:ported/shish_wall_grimoire",
        "lbc:ported/sickle",
        "lbc:ported/siggewinne_syringe",
        "lbc:ported/slr",
        "lbc:ported/snow_academy_grimoire",
        "lbc:ported/snow_grimoire",
        "lbc:ported/soulhunter_mace",
        "lbc:ported/soulless_grimoire",
        "lbc:ported/soulless_metasola_grimoire",
        "lbc:ported/spike",
        "lbc:ported/stone_bridge_grimoire",
        "lbc:ported/stone_grimoire",
        "lbc:ported/stone_ray_grimoire",
        "lbc:ported/stone_shield_grimoire",
        "lbc:ported/stone_spikes_grimoire",
        "lbc:ported/stonekivi_comet_grimoire",
        "lbc:ported/stonekivi_earth_grimoire",
        "lbc:ported/stonekivi_grimoire",
        "lbc:ported/stonekivi_sun_grimoire",
        "lbc:ported/summoner_blazes",
        "lbc:ported/summoner_elite_skeletons",
        "lbc:ported/summoner_ghasts",
        "lbc:ported/summoner_ghost_rider",
        "lbc:ported/summoner_guardians",
        "lbc:ported/summoner_ravager",
        "lbc:ported/summoner_skeletons",
        "lbc:ported/teleport_cloud_grimoire",
        "lbc:ported/teleport_grimoire",
        "lbc:ported/teleport_inverted_grimoire",
        "lbc:ported/terra_fast_grimoire",
        "lbc:ported/terra_grimoire",
        "lbc:ported/terra_razorpine",
        "lbc:ported/tesla_chain_grimoire",
        "lbc:ported/tesla_grimoire",
        "lbc:ported/tesla_instant_grimoire",
        "lbc:ported/tiny_acid_grimoire",
        "lbc:ported/tiny_air_grimoire",
        "lbc:ported/tiny_blind_grimoire",
        "lbc:ported/tiny_electro_grimoire",
        "lbc:ported/tiny_fire_grimoire",
        "lbc:ported/tiny_ruby_grimoire",
        "lbc:ported/tiny_stone_grimoire",
        "lbc:ported/tiny_teleport_grimoire",
        "lbc:ported/tiny_terra_grimoire",
        "lbc:ported/tomahawk",
        "lbc:ported/triple_golden_sword",
        "lbc:ported/wall_break_grimoire",
        "lbc:ported/wall_grimoire",
        "lbc:ported/wall_horizontal_grimoire",
        "lbc:ported/wall_stair_grimoire",
        "lbc:ported/wither_spike",
        "lbc:uncrafted/acacia_planks",
        "lbc:uncrafted/allium",
        "lbc:uncrafted/ancient_debris",
        "lbc:uncrafted/andesite",
        "lbc:uncrafted/bamboo_planks",
        "lbc:uncrafted/basalt",
        "lbc:uncrafted/beef",
        "lbc:uncrafted/beetroot_seeds",
        "lbc:uncrafted/birch_planks",
        "lbc:uncrafted/blackstone",
        "lbc:uncrafted/blue_orchid",
        "lbc:uncrafted/bone_meal",
        "lbc:uncrafted/bricks",
        "lbc:uncrafted/cactus",
        "lbc:uncrafted/cherry_planks",
        "lbc:uncrafted/chicken",
        "lbc:uncrafted/chorus_fruit",
        "lbc:uncrafted/clay_ball",
        "lbc:uncrafted/closed_eyeblossom",
        "lbc:uncrafted/cobbled_deepslate",
        "lbc:uncrafted/cobblestone",
        "lbc:uncrafted/cocoa_beans",
        "lbc:uncrafted/cod",
        "lbc:uncrafted/copper_block",
        "lbc:uncrafted/crimson_planks",
        "lbc:uncrafted/dandelion",
        "lbc:uncrafted/dark_oak_planks",
        "lbc:uncrafted/deepslate_bricks",
        "lbc:uncrafted/diorite",
        "lbc:uncrafted/end_stone",
        "lbc:uncrafted/end_stone_bricks",
        "lbc:uncrafted/exposed_copper",
        "lbc:uncrafted/glass",
        "lbc:uncrafted/granite",
        "lbc:uncrafted/gravel",
        "lbc:uncrafted/honey_bottle",
        "lbc:uncrafted/ink_sac",
        "lbc:uncrafted/jungle_planks",
        "lbc:uncrafted/kelp",
        "lbc:uncrafted/lapis_lazuli",
        "lbc:uncrafted/mangrove_planks",
        "lbc:uncrafted/melon_slice",
        "lbc:uncrafted/mossy_cobblestone",
        "lbc:uncrafted/mud_bricks",
        "lbc:uncrafted/mutton",
        "lbc:uncrafted/nether_bricks",
        "lbc:uncrafted/netherrack",
        "lbc:uncrafted/oak_planks",
        "lbc:uncrafted/orange_tulip",
        "lbc:uncrafted/oxeye_daisy",
        "lbc:uncrafted/oxidized_copper",
        "lbc:uncrafted/packed_mud",
        "lbc:uncrafted/pale_oak_planks",
        "lbc:uncrafted/pink_petals",
        "lbc:uncrafted/pitcher_plant",
        "lbc:uncrafted/polished_blackstone",
        "lbc:uncrafted/polished_blackstone_bricks",
        "lbc:uncrafted/polished_deepslate",
        "lbc:uncrafted/polished_tuff",
        "lbc:uncrafted/popped_chorus_fruit",
        "lbc:uncrafted/poppy",
        "lbc:uncrafted/porkchop",
        "lbc:uncrafted/prismarine",
        "lbc:uncrafted/quartz_block",
        "lbc:uncrafted/rabbit",
        "lbc:uncrafted/raw_copper",
        "lbc:uncrafted/red_nether_bricks",
        "lbc:uncrafted/red_sandstone",
        "lbc:uncrafted/resin_bricks",
        "lbc:uncrafted/resin_clump",
        "lbc:uncrafted/salmon",
        "lbc:uncrafted/sandstone",
        "lbc:uncrafted/spruce_planks",
        "lbc:uncrafted/stone",
        "lbc:uncrafted/stone_bricks",
        "lbc:uncrafted/stripped_acacia_log",
        "lbc:uncrafted/stripped_bamboo_log",
        "lbc:uncrafted/stripped_birch_log",
        "lbc:uncrafted/stripped_cherry_log",
        "lbc:uncrafted/stripped_crimson_log",
        "lbc:uncrafted/stripped_dark_oak_log",
        "lbc:uncrafted/stripped_jungle_log",
        "lbc:uncrafted/stripped_mangrove_log",
        "lbc:uncrafted/stripped_oak_log",
        "lbc:uncrafted/stripped_pale_oak_log",
        "lbc:uncrafted/stripped_spruce_log",
        "lbc:uncrafted/stripped_warped_log",
        "lbc:uncrafted/sugar_cane",
        "lbc:uncrafted/tuff",
        "lbc:uncrafted/tuff_bricks",
        "lbc:uncrafted/warped_planks",
        "lbc:uncrafted/waxed_copper",
        "lbc:uncrafted/waxed_exposed_copper",
        "lbc:uncrafted/waxed_oxidized_copper",
        "lbc:uncrafted/waxed_weathered_copper",
        "lbc:uncrafted/weathered_copper",
        "lbc:uncrafted/whead_seeds",
        "lbc:wind_charge",
    };

    private static void require(boolean condition, String why) {
        if (!condition) throw new IllegalStateException("Canonical loot: " + why);
    }
    private static String kind(JsonObject object) {
        return object.has("type") && object.get("type").isJsonPrimitive()
                ? object.get("type").getAsString().replaceFirst("^minecraft:", "") : "";
    }
    private static JsonObject resource(String id) {
        int colon = id.indexOf(':');
        String path = "data/" + id.substring(0, colon) + "/loot_table/" + id.substring(colon + 1) + ".json";
        try (var input = LootDefinitionChecks.class.getClassLoader().getResourceAsStream(path)) {
            if (input == null) return null;
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException error) { throw new IllegalStateException(path, error); }
    }
    private static JsonArray modifiers(JsonObject object) {
        var result = new JsonArray();
        if (object.has("modifier")) {
            var value = object.get("modifier");
            if (value.isJsonArray()) value.getAsJsonArray().forEach(child -> result.add(child.deepCopy()));
            else result.add(value.deepCopy());
        }
        return result;
    }
    private static boolean safeFunctions(JsonObject item) {
        for (var modifier : modifiers(item)) {
            if (!modifier.isJsonObject() || !FUNCTIONS.contains(kind(modifier.getAsJsonObject()))) return false;
            if (!safeNested(modifier)) return false;
        }
        return true;
    }
    private static boolean safeNested(JsonElement value) {
        if (value.isJsonArray()) {
            for (var child : value.getAsJsonArray()) if (!safeNested(child)) return false;
        } else if (value.isJsonObject()) {
            var object = value.getAsJsonObject();
            if (Set.of("loot_table", "set_contents", "reference", "filtered", "sequence").contains(kind(object))) return false;
            for (var field : object.entrySet()) {
                if (field.getKey().equals("modifier") || field.getKey().equals("functions")) return false;
                if (!safeNested(field.getValue())) return false;
            }
        }
        return true;
    }
    private static boolean emptyGate(JsonObject object, String field) {
        return !object.has(field) || (object.get(field).isJsonArray() && object.getAsJsonArray(field).isEmpty());
    }
    private static boolean singleSelection(JsonObject entry) {
        return emptyGate(entry, "conditions") && emptyGate(entry, "condition")
                && (!entry.has("quality") || entry.get("quality").getAsInt() == 0)
                && (!entry.has("weight") || entry.get("weight").getAsInt() > 0);
    }
    private static JsonObject definition(String id, Set<String> active) {
        if (!active.add(id)) return null;
        try {
            var table = resource(id);
            if (table == null || !Set.of("type", "pools").containsAll(table.keySet()) || !table.has("pools")) return null;
            var pools = table.getAsJsonArray("pools");
            if (pools.size() != 1) return null;
            var pool = pools.get(0).getAsJsonObject();
            if (!Set.of("rolls", "entries").containsAll(pool.keySet()) || !pool.has("rolls")
                    || !pool.get("rolls").isJsonPrimitive() || !pool.get("rolls").getAsJsonPrimitive().isNumber()
                    || pool.get("rolls").getAsDouble() != 1 || !pool.has("entries")) return null;
            var entries = pool.getAsJsonArray("entries");
            if (entries.size() != 1) return null;
            var entry = entries.get(0).getAsJsonObject();
            if (!singleSelection(entry)) return null;
            JsonObject payload;
            if (kind(entry).equals("item")) {
                // Legacy functions is opaque metadata: only modifier is executed by the native codec.
                if (!Set.of("type", "name", "modifier", "weight", "quality", "conditions", "functions").containsAll(entry.keySet())
                        || !entry.has("name") || !safeFunctions(entry)) return null;
                payload = entry.deepCopy();
            } else if (kind(entry).equals("loot_table") && entry.has("value") && entry.get("value").isJsonPrimitive()) {
                if (!Set.of("type", "value", "modifier", "weight", "quality", "conditions", "functions").containsAll(entry.keySet())) return null;
                payload = definition(entry.get("value").getAsString(), active);
                if (payload == null || !safeFunctions(entry)) return null;
                var functions = modifiers(payload);
                modifiers(entry).forEach(functions::add);
                if (!functions.isEmpty()) payload.add("modifier", functions);
            } else return null;
            SELECTION.forEach(payload::remove);
            return payload;
        } finally { active.remove(id); }
    }

    /** Expand only proven singleton item definitions. Preserve all caller selection metadata. */
    public static JsonObject expanded(JsonObject original) {
        return expand(original).getAsJsonObject();
    }
    private static JsonElement expand(JsonElement original) {
        if (original.isJsonArray()) {
            var array = new JsonArray();
            original.getAsJsonArray().forEach(child -> array.add(expand(child)));
            return array;
        }
        if (!original.isJsonObject()) return original.deepCopy();
        var object = original.getAsJsonObject();
        if (kind(object).equals("loot_table") && object.has("value") && object.get("value").isJsonPrimitive()
                && Set.of("type", "value", "modifier", "weight", "quality", "condition", "conditions", "functions").containsAll(object.keySet())) {
            String id = object.get("value").getAsString();
            var payload = definition(id, new HashSet<>());
            require(payload != null || !id.startsWith("lbc:item_defs/"), "unsafe or missing generated definition " + id);
            if (payload != null) {
                for (String field : SELECTION) if (object.has(field)) payload.add(field, object.get(field).deepCopy());
                var functions = modifiers(payload);
                modifiers(object).forEach(functions::add);
                if (!functions.isEmpty()) payload.add("modifier", functions);
                return payload;
            }
        }
        var result = object.deepCopy();
        // Never descend into components, predicates or modifier-owned nested loot.
        for (String field : Set.of("pools", "entries", "children", "value"))
            if (object.has(field)) result.add(field, expand(object.get(field)));
        if (result.has("modifier")) result.add("modifier", modifiers(result));
        return result;
    }

    public static void run(GameController game) {
        var mellstroy = definition(MELLSTROY_PROTOTYPE, new HashSet<>());
        require(mellstroy != null && mellstroy.get("name").getAsString().equals("minecraft:carrot_on_a_stick")
                && !mellstroy.has("functions") && !mellstroy.has("modifier"),
                "native Mellstroy prototype keeps legacy functions opaque to loot expansion");
        var jungleSlab = definition("lbc:item_defs/minecraft/jungle_slab__1da05219a901", new HashSet<>());
        require(jungleSlab != null && jungleSlab.get("name").getAsString().equals("skywars2:cunny_jungle_slab")
                && !jungleSlab.has("functions") && !jungleSlab.has("modifier"),
                "native jungle slab keeps legacy count functions opaque to loot expansion");
        var wrapper = JsonParser.parseString("{\"type\":\"minecraft:loot_table\",\"value\":\"lbc:item_defs/lbc/popit__1655a8bd833f\","
                + "\"weight\":7,\"quality\":-2,\"condition\":{\"type\":\"minecraft:random_chance\",\"chance\":0.25},"
                + "\"modifier\":[{\"type\":\"minecraft:set_count\",\"count\":3}]}").getAsJsonObject();
        var semantic = expanded(wrapper);
        require(kind(semantic).equals("item") && semantic.get("weight").getAsInt() == 7
                && semantic.get("quality").getAsInt() == -2 && semantic.get("condition").equals(wrapper.get("condition")),
                "check expander retains caller weights, quality and conditional gate");
        var sequence = semantic.getAsJsonArray("modifier");
        require(sequence.get(0).getAsJsonObject().get("count").getAsInt() == 1
                && sequence.get(sequence.size() - 1).getAsJsonObject().get("count").getAsInt() == 3,
                "check expander keeps definition prefix before caller count suffix");
        var resources = game.server().getResourceManager().listResources("loot_table/item_defs",
                id -> id.getNamespace().equals("lbc") && id.getPath().endsWith(".json"));
        require(resources.size() == 3627, "all 3627 packaged canonical resources, actual=" + resources.size());
        for(var helper:java.util.List.of("bucks","academy_token","blood_anal_boom","mana_gem"))
            require(resources.containsKey(Identifier.parse("lbc:loot_table/item_defs/skywars2/"+helper+".json")),"dedicated native helper resource "+helper);
        for (var resource : resources.keySet()) {
            String path = resource.getPath();
            String id = resource.getNamespace() + ":" + path.substring("loot_table/".length(), path.length() - 5);
            require(game.server().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                    Identifier.parse(id))) != LootTable.EMPTY, "registered canonical definition " + id);
            require(definition(id, new HashSet<>()) != null, "nonrecursive safe singleton " + id);
        }
        for (String id : REUSED) {
            require(game.server().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                    Identifier.parse(id))) != LootTable.EMPTY, "introduced reused target resident " + id);
            require(definition(id, new HashSet<>()) != null, "introduced reused target remains a safe singleton " + id);
        }
        var player = game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(player != null, "requires TestAlpha");
        var boat = game.lbc().rollTable(player.level(), player.position(), player,
                "lbc:item_defs/minecraft/acacia_boat__d551bf610b21");
        require(boat.size() == 1 && boat.getFirst().is(Items.ACACIA_BOAT) && boat.getFirst().getCount() == 1,
                "canonical vanilla count/carrier");
        var popit = game.lbc().rollTable(player.level(), player.position(), player,
                "lbc:item_defs/lbc/popit__1655a8bd833f");
        require(popit.size() == 1 && popit.getFirst().is(Items.CARROT_ON_A_STICK) && popit.getFirst().getCount() == 1
                && Identifier.parse("lbc:popit").equals(popit.getFirst().get(DataComponents.ITEM_MODEL))
                && popit.getFirst().has(DataComponents.CUSTOM_NAME) && popit.getFirst().has(DataComponents.LORE)
                && LbcFartus.data(popit.getFirst()).getIntOr("popit", 0) == 1, "canonical full custom identity/components");
        var sword = game.lbc().rollTable(player.level(), player.position(), player, "lbc:ported/antimagic_sword");
        require(sword.size() == 1 && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(sword.getFirst().getItem()).equals(Identifier.parse("skywars2:antimagic_sword")) && sword.getFirst().getCount() == 1
                && sword.getFirst().has(DataComponents.CUSTOM_NAME)
                && Identifier.parse("lbc:tier_s").equals(sword.getFirst().get(DataComponents.TOOLTIP_STYLE)), "existing native ported canonical leaf");
        nativeOpaqueCodec(game, player);
        SkyWars2.LOGGER.info("[check] PASS canonical-loot (3627 canonical definitions, {} reused targets, native representative rolls and opaque codec witnesses)", REUSED.length);
    }

    private static void nativeOpaqueCodec(GameController game, net.minecraft.server.level.ServerPlayer player) {
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(player.level())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, player.position())
                .withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, player)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.COMMAND);
        var ops = game.server().reloadableRegistries().lookup().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        for (int chance : new int[]{0, 1}) {
            var before = JsonParser.parseString("{\"type\":\"minecraft:item\",\"name\":\"minecraft:acacia_boat\","
                    + "\"weight\":7,\"quality\":-2,\"condition\":{\"type\":\"minecraft:random_chance\",\"chance\":" + chance + "},"
                    + "\"functions\":[{\"function\":\"minecraft:set_count\",\"count\":99}],"
                    + "\"modifier\":[{\"type\":\"minecraft:set_count\",\"count\":3}]}").getAsJsonObject();
            var after = before.deepCopy();
            after.addProperty("type", "minecraft:loot_table");
            after.remove("name");
            after.addProperty("value", "lbc:item_defs/minecraft/acacia_boat__d551bf610b21");
            require(expanded(after).equals(before), "opaque functions and active condition round-trip unchanged");
            var source = LootTable.DIRECT_CODEC.parse(ops, singleton(before)).getOrThrow();
            var canonical = LootTable.DIRECT_CODEC.parse(ops, singleton(after)).getOrThrow();
            var expected = source.getRandomItems(params, 17);
            var actual = canonical.getRandomItems(params, 17);
            require(expected.size() == actual.size() && (chance == 0 ? actual.isEmpty() : !actual.isEmpty()),
                    "native singular condition stays active before/after factoring, chance=" + chance);
            if (chance == 1) {
                int total = actual.stream().mapToInt(net.minecraft.world.item.ItemStack::getCount).sum();
                require(total == 3 && actual.stream().allMatch(stack -> stack.is(Items.ACACIA_BOAT)),
                        "native modifier count3 applies while legacy functions count99 remains ignored");
                require(expected.size() == actual.size(), "same native split stack count");
                for (int index = 0; index < actual.size(); index++)
                    require(net.minecraft.world.item.ItemStack.matches(expected.get(index), actual.get(index)),
                            "independent native item/ref codec witness components " + index);
            }
        }
    }

    private static JsonObject singleton(JsonObject entry) {
        var entries = new JsonArray(); entries.add(entry.deepCopy());
        var pool = new JsonObject(); pool.addProperty("rolls", 1); pool.add("entries", entries);
        var pools = new JsonArray(); pools.add(pool);
        var root = new JsonObject(); root.add("pools", pools);
        return root;
    }
}
