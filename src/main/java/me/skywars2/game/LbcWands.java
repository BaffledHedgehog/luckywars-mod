package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;

/** Жезлы используют общий расход маны и каст-хуки; временные снаряды хранит существующий трекер. */
public final class LbcWands {
    public static final Item ICE=register("skywars2:ice_wand","ice_wand","lbc:ice_wand");
    public static final Item ACADEMY_ICE=register("skywars2:academy_ice_wand","ice_wand","lbc:ice_wand");
    public static final Item CORRUPTION=register("skywars2:corruption_wand","corruption_wand","lbc:corruptwand");
    public static final Item ACADEMY_CORRUPTION=register("skywars2:academy_corruption_wand","corruption_wand","lbc:corruptwand");
    public static final Item ACID_CORRUPTION=register("skywars2:acid_corruption_wand","acidcorrupt_wand","lbc:acidcorrupt");
    public static final Item ACADEMY_ACID_CORRUPTION=register("skywars2:academy_acid_corruption_wand","acidcorrupt_wand","lbc:acidcorrupt");
    public static final Item ERECTO=register("skywars2:erecto_wand","erecto_wand","lbc:erecto");
    public static final Item ACADEMY_ERECTO=register("skywars2:academy_erecto_wand","erecto_wand","lbc:erecto");
    public static final Item TESLA=register("skywars2:tesla_wand","lightning_wand","lbc:lightningwand");
    public static final Item ACADEMY_TESLA=register("skywars2:academy_tesla_wand","lightning_wand","lbc:lightningwand");
    public static final Item PRISM=register("skywars2:unstable_prism","unstable_prism","lbc:prism");
    public static final Item GAYCASTER=register("skywars2:gaycaster","gaycaster","lbc:gaycaster");
    public static final Item AWAKENED_ACID=register("skywars2:awakened_acid_wand","acidacid_wand","lbc:acidacidwand");
    public static final Item ACADEMY_AWAKENED_ACID=register("skywars2:academy_awakened_acid_wand","acidacid_wand","lbc:acidacidwand");
    public static final Item ACADEMY_GAYCASTER=register("skywars2:academy_gaycaster","gaycaster","lbc:gaycaster");
    public static final Item ACADEMY_PRISM=register("skywars2:academy_unstable_prism","unstable_prism","lbc:prism");
    public static final Item CAUSTIC=register("skywars2:caustic_wand","acid_wand","lbc:acidwand");
    public static final Item ACADEMY_CAUSTIC=register("skywars2:academy_caustic_wand","acid_wand","lbc:acidwand");
    public static final Item OMEGA=register("skywars2:omega_flag","omega_flag","lbc:omegaflag");
    public static final Item ACADEMY_OMEGA=register("skywars2:academy_omega_flag","omega_flag","lbc:omegaflag");
    public static final Item ADVANCED_ENDER=register("skywars2:advanced_ender_wand","advanced_ender_wand","lbc:advancedenderwand");
    public static final Item ACADEMY_ADVANCED_ENDER=register("skywars2:academy_advanced_ender_wand","advanced_ender_wand","lbc:advancedenderwand");
    public static final Item SNOW=register("skywars2:snow_wand","snow_wand","lbc:snow_wand");
    public static final Item ACADEMY_SNOW=register("skywars2:academy_snow_wand","snow_wand","lbc:snow_wand");
    public static final Item ENDER=register("skywars2:ender_wand","ender_wand","lbc:enderwand");
    public static final Item ACADEMY_ENDER=register("skywars2:academy_ender_wand","ender_wand","lbc:enderwand");
    public static final Item BAD_WIKI=register("skywars2:bad_wiki","bad_wiki","lbc:badwiki");
    public static final Item ACADEMY_BAD_WIKI=register("skywars2:academy_bad_wiki","bad_wiki","lbc:badwiki");
    public static final Item NEUTRAL=register("skywars2:neutral_wand","neutral_wand","lbc:neutralwand");
    public static final Item ACADEMY_NEUTRAL=register("skywars2:academy_neutral_wand","neutral_wand","lbc:neutralwand");
    public static final Item FEATHER_GREED=register("skywars2:feather_greed","feather_greed","minecraft:feather");
    public static final Item BLOOD=register("skywars2:blood_wand","blood_wand","lbc:killingwand");
    public static final Item ACADEMY_BLOOD=register("skywars2:academy_blood_wand","blood_wand","lbc:killingwand");
    public static final Item CONFUSE=register("skywars2:confuse_wand","confuse_wand","lbc:foxuswand");
    public static final Item ACADEMY_CONFUSE=register("skywars2:academy_confuse_wand","confuse_wand","lbc:foxuswand");
    public static final Item EVOKER=register("skywars2:evoker_wand","evoker_wand","lbc:evokerwand");
    public static final Item ACADEMY_EVOKER=register("skywars2:academy_evoker_wand","evoker_wand","lbc:evokerwand");
    public static final Item BASS=register("skywars2:bass_wand","bass_wand","lbc:basswand");
    public static final Item ACADEMY_BASS=register("skywars2:academy_bass_wand","bass_wand","lbc:basswand");
    public static final Item LIFESTEALER=register("skywars2:lifestealer_wand","vampire_wand","lbc:vampirewand");
    public static final Item ACADEMY_LIFESTEALER=register("skywars2:academy_lifestealer_wand","vampire_wand","lbc:vampirewand");
    public static final Item TINY_FIRE=register("skywars2:tiny_fire_wand","wand_fire_tiny","lbc:st/fire_tiny");
    public static final Item FIRE_WAND=register("skywars2:fire_wand","sauvojen_wand_fire","lbc:st/fire");
    public static final Item ACADEMY_FIRE=register("skywars2:academy_fire_wand","sauvojen_wand_fire","lbc:st/fire");
    public static final Item BLAZE_WAND=register("skywars2:blaze_wand","blaze_wand","lbc:blazewand");
    public static final Item ACADEMY_BLAZE=register("skywars2:academy_blaze_wand","blaze_wand","lbc:blazewand");
    public static final Item FIREBLAZE=register("skywars2:fireblaze_wand","fireblaze_wand","lbc:blazefire");
    public static final Item ACADEMY_FIREBLAZE=register("skywars2:academy_fireblaze_wand","fireblaze_wand","lbc:blazefire");
    public static final Item SUMMONER_VEXES=register("skywars2:summoner_vexes_wand","vex_soul","lbc:summonvex");
    public static final Item PSYCHOVEX=register("skywars2:psychovex_wand","psychovex_wand","lbc:blindvex");
    public static final Item FALLING_STARS_WAND=register("skywars2:falling_stars_wand","falling_stars_wand","lbc:starwand");
    public static final Item ACADEMY_FALLING_STARS=register("skywars2:academy_falling_stars_wand","falling_stars_wand","lbc:starwand");
    public static final Item BOLT_MATERIAL=registerBoltMaterial();
    public static final Item INFBOLT=register("skywars2:infbolt_wand","infbolt_wand","lbc:infbolt");
    public static final Item CONSUMING=register("skywars2:consuming_wand","consuming_wand","lbc:consuming_wand");
    public static final Item ACADEMY_CONSUMING=register("skywars2:academy_consuming_wand","consuming_wand","lbc:consuming_wand");
    public static final Item DUPLICATING=register("skywars2:duplicating_wand","dublicating_wand","lbc:dublicating_wand");
    public static final Item ACADEMY_DUPLICATING=register("skywars2:academy_duplicating_wand","dublicating_wand","lbc:dublicating_wand");
    public static final Item DIVIDER=register("skywars2:divider","divider","lbc:divider");
    public static final Item CREEPER_ARMY_SPELL=register("skywars2:creeper_army_spell","creeper_army_spell","lbc:creepers",0);
    public static final Item DESTROYER_SPELL=register("skywars2:destroyer_spell","destroyer_spell","lbc:breaker",0);
    public static final Item LIGHTNING_BOLTS_WAND=register("skywars2:lightning_bolts_wand","lightning_bolt_wand","lbc:lightningboltwand");
    public static final Item ACADEMY_LIGHTNING_BOLTS=register("skywars2:academy_lightning_bolts_wand","lightning_bolt_wand","lbc:lightningboltwand");
    public static final Item BOLTBOLT=register("skywars2:boltbolt_wand","boltbolt_wand","lbc:boltbolt_wand");
    public static final Item ACADEMY_BOLTBOLT=register("skywars2:academy_boltbolt_wand","boltbolt_wand","lbc:boltbolt_wand");
    public static final Item EXAHOMING=register("skywars2:exahoming_wand","exahoming_wand","lbc:homing");
    public static final Item ACADEMY_EXAHOMING=register("skywars2:academy_exahoming_wand","exahoming_wand","lbc:homing");
    public static final Item PSYCHOWAND=register("skywars2:psychowand","psycho_wand","lbc:psychowand");
    public static final Item ACADEMY_PSYCHOWAND=register("skywars2:academy_psychowand","psycho_wand","lbc:psychowand");
    public static final Item PSYCHOBLIND=register("skywars2:psychoblind_wand","psychoblind_wand","lbc:psychoblind");
    public static final Item ACADEMY_PSYCHOBLIND=register("skywars2:academy_psychoblind_wand","psychoblind_wand","lbc:psychoblind");
    public static final Item TINY_BLIND=register("skywars2:tiny_blind_wand","wand_blind_tiny","lbc:st/blind_tiny");
    public static final Item BLIND_WAND=register("skywars2:blind_wand","sauvojen_wand_blind","lbc:st/blind");
    public static final Item ACADEMY_BLIND=register("skywars2:academy_blind_wand","sauvojen_wand_blind","lbc:st/blind");
    public static final Item TINY_AIR=register("skywars2:tiny_air_wand","wand_air_tiny","lbc:st/air_tiny");
    public static final Item AIR_WAND=register("skywars2:air_wand","sauvojen_wand_air","lbc:st/air");
    public static final Item ACADEMY_AIR=register("skywars2:academy_air_wand","sauvojen_wand_air","lbc:st/air");
    public static final Item TINY_STONE=register("skywars2:tiny_stone_wand","wand_stone_tiny","lbc:st/stone_tiny");
    public static final Item STONE_WAND=register("skywars2:stone_wand","sauvojen_wand_stone","lbc:st/stone");
    public static final Item ACADEMY_STONE=register("skywars2:academy_stone_wand","sauvojen_wand_stone","lbc:st/stone");
    public static final Item TINY_TELEPORT=register("skywars2:tiny_teleport_wand","wand_teleport_tiny","lbc:st/teleport_tiny");
    public static final Item TELEPORT_WAND=register("skywars2:teleport_wand","sauvojen_wand_teleport","lbc:st/teleport");
    public static final Item ACADEMY_TELEPORT=register("skywars2:academy_teleport_wand","sauvojen_wand_teleport","lbc:st/teleport");
    public static final Item TINY_RUBY=register("skywars2:tiny_ruby_wand","wand_ruby_tiny","lbc:st/ruby_tiny");
    public static final Item RUBY_WAND=register("skywars2:ruby_wand","sauvojen_wand_ruby","lbc:st/ruby");
    public static final Item ACADEMY_RUBY=register("skywars2:academy_ruby_wand","sauvojen_wand_ruby","lbc:st/ruby");
    public static final Item TINY_TERRA=register("skywars2:tiny_terra_wand","wand_terra_tiny","lbc:st/terra_tiny");
    public static final Item TERRA=register("skywars2:terra_wand","sauvojen_wand_terra","lbc:st/terra");
    public static final Item ACADEMY_TERRA=register("skywars2:academy_terra_wand","sauvojen_wand_terra","lbc:st/terra");
    public static final Item TINY_ACID=register("skywars2:tiny_acid_wand","wand_acid_tiny","lbc:st/acid_tiny");
    public static final Item ACID_WAND=register("skywars2:acid_wand","sauvojen_wand_acid","lbc:st/acid");
    public static final Item ACADEMY_ACID_WAND=register("skywars2:academy_acid_wand","sauvojen_wand_acid","lbc:st/acid");
    public static final Item TINY_ELECTRO=register("skywars2:tiny_electro_wand","wand_electro_tiny","lbc:st/electro_tiny");
    public static final Item ELECTRO=register("skywars2:electro_wand","sauvojen_wand_electro","lbc:st/electro");
    public static final Item ACADEMY_ELECTRO=register("skywars2:academy_electro_wand","sauvojen_wand_electro","lbc:st/electro");
    public static final Item SAUVOJEN=register("skywars2:sauvojen_wand","sauvojen_wand","lbc:sauvojen_wand");
    public static final Item ACADEMY_SAUVOJEN=register("skywars2:academy_sauvojen_wand","sauvojen_wand","lbc:sauvojen_wand");
    public static final Item EXASPERATO=register("skywars2:exasperato_wand","exasperato_wand","lbc:exasperato");
    public static final Item EXOPERSO=register("skywars2:exoperso_wand","megumin_wand","lbc:exoperso");
    public static final Item BEDROCK=register("skywars2:bedrock_wand","bedrock_wand","lbc:bedrock_wand");
    public static final Item WALL=register("skywars2:wall_wand","wall_wand","lbc:wallwand");
    public static final Item STONEKIVI=register("skywars2:stonekivi_wand","stonekivi_wand","lbc:stonekivi");
    public static final Item ACADEMY_STONEKIVI=register("skywars2:academy_stonekivi_wand","stonekivi_wand","lbc:stonekivi");
    public static final Item ACADEMY_WALL=register("skywars2:academy_wall_wand","wall_wand","lbc:wallwand");
    public static final Item ACADEMY_BEDROCK=register("skywars2:academy_bedrock_wand","bedrock_wand","lbc:bedrock_wand");
    public static final Item ACADEMY_EXOPERSO=register("skywars2:academy_exoperso_wand","megumin_wand","lbc:exoperso");
    public static final Item ACADEMY_EXASPERATO=register("skywars2:academy_exasperato_wand","exasperato_wand","lbc:exasperato");
    private enum Spell {
        ACADEMY_ICE(LbcWands.ACADEMY_ICE,"ice_wand","ice_wand_mana_use",400),
        ICE(LbcWands.ICE,"ice_wand","ice_wand_mana_use",400),
        ACADEMY_CORRUPTION(LbcWands.ACADEMY_CORRUPTION,"corrupter","corruption_wand_mana_use",1800),
        CORRUPTION(LbcWands.CORRUPTION,"corrupter","corruption_wand_mana_use",1800),
        ACADEMY_ACID_CORRUPTION(LbcWands.ACADEMY_ACID_CORRUPTION,"acidcorrupt_wand","acidcorrupt_wand_mana_use",1210),
        ACID_CORRUPTION(LbcWands.ACID_CORRUPTION,"acidcorrupt_wand","acidcorrupt_wand_mana_use",1210),
        ACADEMY_ERECTO(LbcWands.ACADEMY_ERECTO,"erecto_wand","erecto_wand_mana_use",1100),
        ERECTO(LbcWands.ERECTO,"erecto_wand","erecto_wand_mana_use",1100),
        ACADEMY_TESLA(LbcWands.ACADEMY_TESLA,"lightningwand","lightning_wand_mana_use",1200),
        TESLA(LbcWands.TESLA,"lightningwand","lightning_wand_mana_use",1200),
        ACADEMY_AWAKENED_ACID(LbcWands.ACADEMY_AWAKENED_ACID,"acidacid_wand","acidacid_wand_mana_use",1450),
        AWAKENED_ACID(LbcWands.AWAKENED_ACID,"acidacid_wand","acidacid_wand_mana_use",1450),
        ACADEMY_PRISM(LbcWands.ACADEMY_PRISM,"prisma","unstable_prism_mana_use",1200),
        REGULAR_PRISM(LbcWands.PRISM,"prisma","unstable_prism_mana_use",1200),
        ACADEMY_GAYCASTER(LbcWands.ACADEMY_GAYCASTER,"gaycaster","gaycaster_mana_use",1500),
        GAYCASTER(LbcWands.GAYCASTER,"gaycaster","gaycaster_mana_use",1500),
        ACADEMY_CAUSTIC(LbcWands.ACADEMY_CAUSTIC,"acidwand","acid_wand_mana_use",1200),
        REGULAR_CAUSTIC(LbcWands.CAUSTIC,"acidwand","acid_wand_mana_use",1200),
        ACADEMY_OMEGA(LbcWands.ACADEMY_OMEGA,"omegaflag","absurd_singularity_mana_use",3000),
        REGULAR_OMEGA(LbcWands.OMEGA,"omegaflag","absurd_singularity_mana_use",3000),
        ACADEMY_ADVANCED_ENDER(LbcWands.ACADEMY_ADVANCED_ENDER,"advancedteleport","advanced_ender_wand_mana_use",3000),
        REGULAR_ADVANCED_ENDER(LbcWands.ADVANCED_ENDER,"advancedteleport","advanced_ender_wand_mana_use",3000),
        ACADEMY_SNOW(LbcWands.ACADEMY_SNOW,"snow_wand","snow_wand_mana_use",1500),
        REGULAR_SNOW(LbcWands.SNOW,"snow_wand","snow_wand_mana_use",1500),
        ACADEMY_ENDER(LbcWands.ACADEMY_ENDER,"enderwand","ender_wand_mana_use",2500),
        REGULAR_ENDER(LbcWands.ENDER,"enderwand","ender_wand_mana_use",2500),
        ACADEMY_BAD_WIKI(LbcWands.ACADEMY_BAD_WIKI,"badwiki","bad_wiki_mana_use",500),
        REGULAR_BAD_WIKI(LbcWands.BAD_WIKI,"badwiki","bad_wiki_mana_use",500),
        ACADEMY_NEUTRAL(LbcWands.ACADEMY_NEUTRAL,"neutralmage","neutral_wand_mana_use",1200),
        REGULAR_NEUTRAL(LbcWands.NEUTRAL,"neutralmage","neutral_wand_mana_use",1200),
        ACADEMY_BLOOD(LbcWands.ACADEMY_BLOOD,"bloodmagic","blood_wand_mana_use",5000),
        REGULAR_BLOOD(LbcWands.BLOOD,"bloodmagic","blood_wand_mana_use",5000),
        ACADEMY_CONFUSE(LbcWands.ACADEMY_CONFUSE,"foxusing","confuse_wand_mana_use",900),
        REGULAR_CONFUSE(LbcWands.CONFUSE,"foxusing","confuse_wand_mana_use",900),
        ACADEMY_EVOKER(LbcWands.ACADEMY_EVOKER,"evocator","evoker_wand_mana_use",700),
        REGULAR_EVOKER(LbcWands.EVOKER,"evocator","evoker_wand_mana_use",700),
        ACADEMY_BASS(LbcWands.ACADEMY_BASS,"bassboost","bass_wand_mana_use",800),
        REGULAR_BASS(LbcWands.BASS,"bassboost","bass_wand_mana_use",800),
        ACADEMY_LIFESTEALER(LbcWands.ACADEMY_LIFESTEALER,"lifestealer","vampire_wand_mana_use",250),
        REGULAR_LIFESTEALER(LbcWands.LIFESTEALER,"lifestealer","vampire_wand_mana_use",250),
        FIRE(TINY_FIRE,"stfire_tiny","tiny_fire_wand_mana_use",25),
        ACADEMY_FIRE(LbcWands.ACADEMY_FIRE,"stfire","stfire_wand_mana_use",100),
        REGULAR_FIRE(LbcWands.FIRE_WAND,"stfire","stfire_wand_mana_use",100),
        ACADEMY_BLAZE(LbcWands.ACADEMY_BLAZE,"fallingstars","blaze_wand_mana_use",2800),
        REGULAR_BLAZE(LbcWands.BLAZE_WAND,"fallingstars","blaze_wand_mana_use",2800),
        ACADEMY_FIREBLAZE(LbcWands.ACADEMY_FIREBLAZE,"fireblaze_wand","fireblaze_wand_mana_use",50),
        REGULAR_FIREBLAZE(LbcWands.FIREBLAZE,"fireblaze_wand","fireblaze_wand_mana_use",50),
        SUMMONER_VEXES(LbcWands.SUMMONER_VEXES,"summonervexes","vex_soul_mana_use",5000),
        PSYCHOVEX(LbcWands.PSYCHOVEX,"psychovex_wand","psychovex_wand_mana_use",1000),
        ACADEMY_FALLING_STARS(LbcWands.ACADEMY_FALLING_STARS,"truefallingstars","falling_stars_wand_mana_use",4000),
        REGULAR_FALLING_STARS(LbcWands.FALLING_STARS_WAND,"truefallingstars","falling_stars_wand_mana_use",4000),
        INFBOLT(LbcWands.INFBOLT,"infbolt_wand","infbolt_wand_mana_use",0),
        ACADEMY_CONSUMING(LbcWands.ACADEMY_CONSUMING,"consuming_wand","consuming_wand_mana_use",2000),
        REGULAR_CONSUMING(LbcWands.CONSUMING,"consuming_wand","consuming_wand_mana_use",2000),
        ACADEMY_DUPLICATING(LbcWands.ACADEMY_DUPLICATING,"dublicating_wand","dublicating_wand_mana_use",1000),
        REGULAR_DUPLICATING(LbcWands.DUPLICATING,"dublicating_wand","dublicating_wand_mana_use",1000),
        ACADEMY_LIGHTNING_BOLTS(LbcWands.ACADEMY_LIGHTNING_BOLTS,"lightning_bolts","lightning_bolt_wand_mana_use",3000),
        REGULAR_LIGHTNING_BOLTS(LbcWands.LIGHTNING_BOLTS_WAND,"lightning_bolts","lightning_bolt_wand_mana_use",3000),
        ACADEMY_BOLTBOLT(LbcWands.ACADEMY_BOLTBOLT,"boltbolt_wand","boltbolt_wand_mana_use",1830),
        REGULAR_BOLTBOLT(LbcWands.BOLTBOLT,"boltbolt_wand","boltbolt_wand_mana_use",1830),
        ACADEMY_EXAHOMING(LbcWands.ACADEMY_EXAHOMING,"exahoming_wand","exahoming_wand_mana_use",2000),
        REGULAR_EXAHOMING(LbcWands.EXAHOMING,"exahoming_wand","exahoming_wand_mana_use",2000),
        ACADEMY_PSYCHOWAND(LbcWands.ACADEMY_PSYCHOWAND,"psychowand","psycho_wand_mana_use",600),
        REGULAR_PSYCHOWAND(LbcWands.PSYCHOWAND,"psychowand","psycho_wand_mana_use",600),
        ACADEMY_PSYCHOBLIND(LbcWands.ACADEMY_PSYCHOBLIND,"psychoblind_wand","psychoblind_wand_mana_use",500),
        REGULAR_PSYCHOBLIND(LbcWands.PSYCHOBLIND,"psychoblind_wand","psychoblind_wand_mana_use",500),
        BLIND(TINY_BLIND,"stblind_tiny","tiny_blind_wand_mana_use",63),
        ACADEMY_BLIND(LbcWands.ACADEMY_BLIND,"stblind","stblind_wand_mana_use",250),
        REGULAR_BLIND(LbcWands.BLIND_WAND,"stblind","stblind_wand_mana_use",250),
        AIR(TINY_AIR,"stair_tiny","tiny_air_wand_mana_use",250),
        ACADEMY_AIR(LbcWands.ACADEMY_AIR,"stair","stair_wand_mana_use",1000),
        REGULAR_AIR(LbcWands.AIR_WAND,"stair","stair_wand_mana_use",1000),
        STONE(TINY_STONE,"ststone_tiny","tiny_stone_wand_mana_use",500),
        ACADEMY_STONE(LbcWands.ACADEMY_STONE,"ststone","ststone_wand_mana_use",2000),
        REGULAR_STONE(LbcWands.STONE_WAND,"ststone","ststone_wand_mana_use",2000),
        TELEPORT(TINY_TELEPORT,"stteleport_tiny","tiny_teleport_wand_mana_use",750),
        ACADEMY_TELEPORT(LbcWands.ACADEMY_TELEPORT,"stteleport","stteleport_wand_mana_use",3000),
        REGULAR_TELEPORT(LbcWands.TELEPORT_WAND,"stteleport","stteleport_wand_mana_use",3000),
        RUBY(TINY_RUBY,"struby_tiny","tiny_ruby_wand_mana_use",500),
        ACADEMY_RUBY(LbcWands.ACADEMY_RUBY,"struby","struby_wand_mana_use",2000),
        REGULAR_RUBY(LbcWands.RUBY_WAND,"struby","struby_wand_mana_use",2000),
        TERRA(TINY_TERRA,"stterra_tiny","tiny_terra_wand_mana_use",250),
        // Более точная Academy-маска обязана идти раньше общего stterra.
        ACADEMY_TERRA(LbcWands.ACADEMY_TERRA,"stterra","stterra_wand_mana_use",1000),
        REGULAR_TERRA(LbcWands.TERRA,"stterra","stterra_wand_mana_use",1000),
        TRUE_TERRA(LbcTerra.TRUE_SWORD,"true_terra","true_terra_mana_use",1000),
        ACID(TINY_ACID,"stacid_tiny","tiny_acid_wand_mana_use",63),
        ACADEMY_REGULAR_ACID(LbcWands.ACADEMY_ACID_WAND,"stacid","stacid_wand_mana_use",250),
        REGULAR_ACID(LbcWands.ACID_WAND,"stacid","stacid_wand_mana_use",250),
        ELECTRO(TINY_ELECTRO,"stelectro_tiny","tiny_electro_wand_mana_use",100),
        ACADEMY_ELECTRO(LbcWands.ACADEMY_ELECTRO,"stelectro","stelectro_wand_mana_use",400),
        REGULAR_ELECTRO(LbcWands.ELECTRO,"stelectro","stelectro_wand_mana_use",400),
        ACADEMY_EXASPERATO(LbcWands.ACADEMY_EXASPERATO,"exasperato_wand","exasperato_wand_mana_use",1500),
        ACADEMY_EXOPERSO(LbcWands.ACADEMY_EXOPERSO,"exoperso","megumin_staff_mana_use",5000),
        REGULAR_EXOPERSO(LbcWands.EXOPERSO,"exoperso","megumin_staff_mana_use",5000),
        ACADEMY_BEDROCK(LbcWands.ACADEMY_BEDROCK,"bedrock_wand","bedrock_wand_mana_use",2500),
        REGULAR_BEDROCK(LbcWands.BEDROCK,"bedrock_wand","bedrock_wand_mana_use",2500),
        ACADEMY_WALL(LbcWands.ACADEMY_WALL,"wall","academy_wall_wand_mana_use",500),
        REGULAR_WALL(LbcWands.WALL,"wall","wall_wand_mana_use",1500),
        ACADEMY_STONEKIVI(LbcWands.ACADEMY_STONEKIVI,"stonekivi_wand","stonekivi_wand_mana_use",2500),
        REGULAR_STONEKIVI(LbcWands.STONEKIVI,"stonekivi_wand","stonekivi_wand_mana_use",2500),
        SAUVOJEN(LbcWands.SAUVOJEN,"sauvojen_wand","sauvojen_wand_mana_use",2500),
        ACADEMY_SAUVOJEN(LbcWands.ACADEMY_SAUVOJEN,"sauvojen_wand","sauvojen_wand_mana_use",2500),
        EXASPERATO(LbcWands.EXASPERATO,"exasperato_wand","exasperato_wand_mana_use",1500);
        final Item item;
        final String family;
        final CompoundTag flag;
        final ScoreHolder cost;
        final int defaultCost;
        Spell(Item item,String flag,String cost,int defaultCost) {
            this.item=item; this.family=flag; this.flag=flag(flag); this.cost=ScoreHolder.forNameOnly(cost); this.defaultCost=defaultCost;
            if(item==LbcWands.ACADEMY_ICE||item==LbcWands.ACADEMY_CORRUPTION||item==LbcWands.ACADEMY_ACID_CORRUPTION||item==LbcWands.ACADEMY_ERECTO||item==LbcWands.ACADEMY_TESLA||item==LbcWands.ACADEMY_PRISM||item==LbcWands.ACADEMY_GAYCASTER||item==LbcWands.ACADEMY_AWAKENED_ACID) this.flag.putInt("upgraded",1);
            if(item==LbcWands.ACADEMY_ACID_WAND) this.flag.putInt("upgraded",1);
            if(item==LbcWands.ACADEMY_CAUSTIC || item==LbcWands.ACADEMY_OMEGA) this.flag.putInt("upgraded",1);
            if(item==LbcWands.ACADEMY_ADVANCED_ENDER || item==LbcWands.ACADEMY_SNOW) this.flag.putInt("upgraded",1);
            if(item==LbcWands.ACADEMY_ENDER || item==LbcWands.ACADEMY_BAD_WIKI) this.flag.putInt("upgraded",1);
            if(item==LbcWands.ACADEMY_NEUTRAL) this.flag.putInt("upgraded",1);
            if(item==LbcWands.ACADEMY_BLOOD || item==LbcWands.ACADEMY_CONFUSE) this.flag.putInt("upgraded",1);
            if(item==LbcWands.ACADEMY_EVOKER || item==LbcWands.ACADEMY_BASS) this.flag.putInt("upgraded",1);
            if(item==LbcWands.ACADEMY_LIFESTEALER) this.flag.putInt("upgraded",1);
            if(item==LbcWands.ACADEMY_CONSUMING) this.flag.putInt("upgraded",1);
            if(item==LbcWands.ACADEMY_DUPLICATING) this.flag.putInt("upgraded",1);
            if(item==LbcWands.ACADEMY_PSYCHOWAND || item==LbcWands.ACADEMY_PSYCHOBLIND) this.flag.putInt("upgraded",1);
            if(item==LbcWands.ACADEMY_BLAZE || item==LbcWands.ACADEMY_FIREBLAZE) this.flag.putInt("upgraded",1);
            if(item==LbcWands.ACADEMY_LIGHTNING_BOLTS || item==LbcWands.ACADEMY_BOLTBOLT) this.flag.putInt("upgraded",1);
            if(item==LbcWands.ACADEMY_FALLING_STARS || item==LbcWands.ACADEMY_EXAHOMING) this.flag.putInt("upgraded",1);
            if(item==LbcWands.ACADEMY_BEDROCK || item==LbcWands.ACADEMY_WALL || item==LbcWands.ACADEMY_STONEKIVI) this.flag.putInt("upgraded",1);
            if(item==LbcWands.ACADEMY_SAUVOJEN || item==LbcWands.ACADEMY_EXOPERSO || item==LbcWands.ACADEMY_RUBY || item==LbcWands.ACADEMY_TELEPORT || item==LbcWands.ACADEMY_AIR || item==LbcWands.ACADEMY_BLIND || item==LbcWands.ACADEMY_STONE || item==LbcWands.ACADEMY_FIRE || item==LbcWands.ACADEMY_TERRA || item==LbcWands.ACADEMY_ELECTRO || item==LbcWands.ACADEMY_EXASPERATO)
                this.flag.putInt("upgraded",1);
        }
    }
    private static final Spell[] SPELLS=Spell.values();
    // Literal legacy order: raycast, ordinary spell dispatch, then summoned souls.
    private static final java.util.List<String> TMT_ORDER=java.util.List.of(
            "wall","advancedteleport","fallingstars","truefallingstars","prisma","lightningwand","acidwand",
            "lightning_bolts","consuming_wand","exahoming_wand","acidcorrupt_wand","boltbolt_wand","infbolt_wand",
            "stonekivi_wand","evocator","bassboost","enderwand","corrupter","lifestealer","foxusing","bloodmagic",
            "badwiki","psychowand","omegaflag","neutralmage","exoperso","struby","stterra","stelectro","stair",
            "stacid","stteleport","ststone","stfire","stblind","dublicating_wand","true_terra","erecto_wand",
            "exasperato_wand","acidacid_wand","fireblaze_wand","psychoblind_wand","gaycaster","ice_wand","snow_wand",
            "bedrock_wand","struby_tiny","stterra_tiny","stelectro_tiny","stair_tiny","stacid_tiny","stteleport_tiny",
            "ststone_tiny","stfire_tiny","stblind_tiny","sauvojen_wand","summonervexes","psychovex_wand");
    private static final ScoreHolder MAGE_BONUS=ScoreHolder.forNameOnly("mage_set_bonus");
    private static final ScoreHolder TIME_STOPPED=ScoreHolder.forNameOnly("time_stopped");
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final TagKey<Block> CONDUCTIVE=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:electroprovod"));
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final String FIRE_PROJECTILE="skywars2.wand.fire";
    private static final String AIR_PROJECTILE="skywars2.wand.air";
    private static final CompoundTag AIR_ROCKET=airRocketPreset();
    private static final DustParticleOptions RUBY_PARTICLE=new DustParticleOptions(ARGB.colorFromFloat(1,.89f,0,.973f) & 0xFFFFFF,1);
    private static final DustColorTransitionOptions TERRA_PARTICLE=new DustColorTransitionOptions(
            ARGB.colorFromFloat(1,.01f,1,.134f) & 0xFFFFFF,ARGB.colorFromFloat(1,.004f,1,.137f) & 0xFFFFFF,1);
    private static final DustColorTransitionOptions ELECTRO_PARTICLE=new DustColorTransitionOptions(
            ARGB.colorFromFloat(1,0,.8f,1) & 0xFFFFFF,ARGB.colorFromFloat(1,0,.2f,.2f) & 0xFFFFFF,.8f);
    /** Отложенный извилистый луч: native geometry, без marker-сущности и scoreboard на каждый шаг. */
    static final class ElectroTrace {
        ServerPlayer owner;
        final LivingEntity npcSource;
        Entity explicitEmitter;
        final ServerLevel level;
        final String epoch;
        final int lifetime;
        final boolean meditation;
        boolean mechanism=true,teslaChain,statusWild;
        LbcIndirectKills.Owner cause;
        LbcSpellSource.Source sourcePolicy;
        boolean hostileSauvojen;
        LbcErecto.Path erecto;
        ServerPlayer trialOwner;
        InteractionHand trialHand;
        ItemStack trialStack;
        Vec3 position;
        float yaw,pitch;
        int age,cloneMask;
        ElectroTrace(ServerPlayer owner,Vec3 position,String epoch) {
            this(owner,position,epoch,40);
        }
        ElectroTrace(ServerPlayer owner,Vec3 position,String epoch,int lifetime) {
            this(owner,position,epoch,lifetime,false);
        }
        ElectroTrace(ServerPlayer owner,Vec3 position,String epoch,int lifetime,boolean meditation) {
            this.owner=owner; this.level=owner.level(); this.position=position; this.epoch=epoch;
            this.npcSource=null;
            this.lifetime=lifetime;
            this.meditation=meditation;
            yaw=owner.getYRot(); pitch=owner.getXRot();
        }
        ElectroTrace(LivingEntity source,Vec3 position,Vec3 direction,String epoch) {
            this.npcSource=source; this.level=(ServerLevel)source.level(); this.position=position; this.epoch=epoch;
            lifetime=30; meditation=false; mechanism=true;
            yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
            pitch=(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));
        }
        ElectroTrace(LivingEntity emitter,ServerPlayer owner,Vec3 position,String epoch,LbcIndirectKills.Owner cause){
            this((ServerLevel)emitter.level(),emitter,owner,position,epoch,cause);
        }
        ElectroTrace(ServerLevel level,LivingEntity emitter,ServerPlayer owner,Vec3 position,String epoch,LbcIndirectKills.Owner cause){
            npcSource=emitter;this.owner=owner;this.level=level;this.position=position;this.epoch=epoch;
            lifetime=10;meditation=false;statusWild=true;this.cause=cause;yaw=0;pitch=-90;
        }
        ElectroTrace(ServerLevel level,Vec3 position,Vec3 direction,LbcSpellSource.Source source,int lifetime){
            this.level=level;this.position=position;epoch=source.epoch();this.lifetime=lifetime;meditation=false;
            npcSource=null;sourcePolicy=source;cause=source.cause();explicitEmitter=level.getEntity(source.emitter());hostileSauvojen=true;
            yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
            pitch=(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));
        }

        ElectroTrace(ElectroTrace old) {
            owner=old.owner;npcSource=old.npcSource;explicitEmitter=old.explicitEmitter;level=old.level;epoch=old.epoch;
            lifetime=old.lifetime;meditation=old.meditation;mechanism=old.mechanism;teslaChain=old.teslaChain;
            statusWild=old.statusWild;cause=old.cause;sourcePolicy=old.sourcePolicy;hostileSauvojen=old.hostileSauvojen;
            trialOwner=old.trialOwner;trialHand=old.trialHand;trialStack=old.trialStack;
            position=old.position;yaw=old.yaw;pitch=old.pitch;age=old.age;cloneMask=old.cloneMask;
            if(old.erecto!=null){var path=old.erecto;
                erecto=new LbcErecto.Path(path.kind,path.origin,path.focus,path.focusSteps,path.side,path.caster,path.hand,path.stack.copy());
                erecto.terrainCause=path.terrainCause;
                erecto.ammunition=path.ammunition==null?null:path.ammunition.copy();
            }
        }
        LivingEntity source() { return owner==null?npcSource:owner; }
        Entity damageEmitter(){return explicitEmitter!=null?explicitEmitter:source();}
    }
    private final GameController game;
    private final LbcMana mana;
    private final java.util.List<NativeUse> nativeUses;
    private final java.util.Set<ElectroTrace> electroTraces=new java.util.LinkedHashSet<>();
    private final java.util.ArrayList<ElectroTrace> electroBatch=new java.util.ArrayList<>();

    /** Исходную лестницу Wall загружаем вместе с ресурсами, до первого боевого каста. */
    public void prepare() { game.server().getStructureTemplateManager().get(LbcWall.STAIR); }

    private static Item register(String id,String name,String model) {
        return register(id,name,model,25);
    }
    private static Item register(String id,String name,String model,int durability) {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse(id));
        var properties=new Item.Properties() {
            @Override public Identifier effectiveModel() { return Identifier.parse(model); }
        }.setId(key).overrideDescription(name);
        if(durability>0) properties.durability(durability);
        return Registry.register(BuiltInRegistries.ITEM,key,nativeTool(properties,Items.CARROT_ON_A_STICK));
    }
    /** Native authored tools share the same admission and ray input as existing wands. */
    static Item nativeTool(Item.Properties properties,Item proxy){
        return new SimplePolymerItem(properties,proxy,true){
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
                var game=SkyWars2.game();
                return game!=null&&player instanceof ServerPlayer owner&&game.items().wands().use(owner,hand)
                        ?InteractionResult.SUCCESS:InteractionResult.FAIL;
            }
        };
    }
    /** The donor keeps native egg placement and consumption; only its client representation is Polymer. */
    private static final class LightningEgg extends net.minecraft.world.item.SpawnEggItem implements eu.pb4.polymer.core.api.item.PolymerItem {
        LightningEgg(Item.Properties properties) { super(properties); }
        @Override public Item getPolymerItem(ItemStack stack,net.fabricmc.fabric.api.networking.v1.context.PacketContext context) { return Items.PANDA_SPAWN_EGG; }
        private boolean allowed(Player player) {
            var game=SkyWars2.game();
            return game!=null && player instanceof ServerPlayer p && game.canUseGameplayItems(p)
                    && !game.isObserver(p) && !game.items().frost().isFrozen(p);
        }
        @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
            return allowed(context.getPlayer()) ? super.useOn(context) : InteractionResult.FAIL;
        }
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
            return allowed(player) ? super.use(level,player,hand) : InteractionResult.FAIL;
        }
    }
    private static Item registerBoltMaterial() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:bolt_material"));
        var data=flag("bolt_material"); data.putInt("craftusable",1);
        return Registry.register(BuiltInRegistries.ITEM,key,new LightningEgg(new Item.Properties().setId(key)
                .overrideDescription("lightning_bolt.lbc").component(DataComponents.ITEM_MODEL,Identifier.parse("lbc:lightningegg"))
                .component(DataComponents.CUSTOM_DATA,CustomData.of(data))
                .component(DataComponents.ENTITY_DATA,net.minecraft.world.item.component.TypedEntityData.of(EntityTypes.LIGHTNING_BOLT,new CompoundTag()))));
    }
    public static boolean isBoltMaterial(ItemStack stack) {
        return stack.is(BOLT_MATERIAL) || stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("bolt_material",0)==1;
    }
    public static boolean isConsumingMaterial(ItemStack stack) {
        return stack.is(CREEPER_ARMY_SPELL) || stack.is(DESTROYER_SPELL)
                || stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("cons_material",0)==1;
    }
    public static boolean isDivider(ItemStack stack) {
        return stack.is(DIVIDER) || stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("greed_greed",0)==1;
    }
    static void register() { }
    public static boolean isLifestealer(ItemStack stack) {
        return stack.is(LIFESTEALER) || stack.is(ACADEMY_LIFESTEALER)
                || stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).matchedBy(Spell.REGULAR_LIFESTEALER.flag);
    }
    private static CompoundTag flag(String name) { var value=new CompoundTag(); value.putInt(name,1); return value; }
    private static boolean matches(ItemStack stack,Item nativeItem,CompoundTag flag) {
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(nativeItem) || data!=null && data.matchedBy(flag);
    }
    private static CompoundTag airRocketPreset() {
        try {
            return TagParser.parseCompoundFully("{Life:0,LifeTime:0,ShotAtAngle:1b,FireworksItem:{id:\"minecraft:firework_rocket\",count:1,components:{\"minecraft:fireworks\":{explosions:[{shape:\"burst\",colors:[16770831],fade_colors:[12422150],has_trail:1b,has_twinkle:1b}]}}}}");
        } catch(Exception failure) { throw new IllegalStateException("Invalid Tiny Air rocket preset",failure); }
    }
    LbcWands(GameController game,LbcMana mana) {
        this.game=game; this.mana=mana;this.nativeUses=nativeUses();
        for(var spell:SPELLS) mana.score(spell.cost,"lbc.math").set(spell.defaultCost);
        mana.score(MAGE_BONUS,"lbc.math").set(150);
    }
    /** Каждый жезл оплачивается отдельно; countdown nomagic приостанавливает regen, а не эти касты. */
    public boolean use(ServerPlayer player,InteractionHand hand) {
        if(hand==InteractionHand.OFF_HAND && !game.config().offhandWands) return false;
        boolean applied=cast(player,hand);
        // Старый items/spells последовательно обрабатывал две руки на один ПКМ, если lefthand разрешён.
        if(hand==InteractionHand.MAIN_HAND && game.config().offhandWands)
            applied=cast(player,InteractionHand.OFF_HAND) || applied;
        return applied;
    }
    record WandAbility(Identifier nativeItem,String family,CustomData flags) {
        LbcTmtAbilities.Ability inMode(int mode) {
            var data=flags.copyTag();data.putInt("mode",mode);
            return new LbcTmtAbilities.Ability("wand/"+family,CustomData.of(data));
        }
    }

    /** Actual common-wand properties, for the final meaningful TMT catalog. No item roll or registration. */
    static java.util.List<WandAbility> tmtAbilities() {
        return TmtAbilityDomain.VALUES;
    }
    private static final class TmtAbilityDomain {
        static final java.util.List<WandAbility> VALUES=java.util.Arrays.stream(SPELLS).map(LbcWands::ability).toList();
    }
    private static WandAbility ability(Spell spell) {
        return new WandAbility(BuiltInRegistries.ITEM.getKey(spell.item),spell.family,CustomData.of(spell.flag));
    }
    static java.util.List<WandAbility> tmtPlan(ItemStack stack) {
        return tmtSpells(stack).stream().map(call->ability(call.spell)).toList();
    }
    private record TmtSpell(String slot,Spell spell) {}
    private static java.util.List<TmtSpell> tmtSpells(ItemStack stack) {
        var selected=new java.util.ArrayList<TmtSpell>();
        for(var slot:LbcTmtAbilities.read(stack)) {
            if(!slot.id().startsWith("wand/"))continue;
            String family=slot.id().substring(5);Spell chosen=null;
            for(var spell:SPELLS) {
                if(!spell.family.equals(family)||!slot.data().matchedBy(spell.flag))continue;
                // A property's Academy and regular masks overlap. Select just its most specific match.
                if(chosen==null||spell.flag.size()>chosen.flag.size())chosen=spell;
            }
            if(chosen!=null)selected.add(new TmtSpell(slot.id(),chosen));
        }
        selected.sort(java.util.Comparator.comparingInt(call->{
            int index=TMT_ORDER.indexOf(call.spell.family);return index<0?Integer.MAX_VALUE:index;
        }));
        return java.util.List.copyOf(selected);
    }

    /** Exact used-hand continuation for the future TMT event owner; ordinary use keeps its existing policy. */
    int castTmtSpells(ServerPlayer player,InteractionHand hand) {
        if(hand==null||player==null||!LbcDimensionItems.ordinary(player)||hand==InteractionHand.OFF_HAND&&!game.config().offhandWands)return 0;
        var stack=player.getItemInHand(hand);
        if(ItemData.intValue(stack,"tmt_item",0)!=1||LbcTmtAbilities.active(stack))return 0;
        int completed=0;
        for(var call:tmtSpells(stack)) {
            if(player.getItemInHand(hand)!=stack||stack.isEmpty()||!mana.visible(player)||game.isObserver(player)
                    ||game.items().actionsBlocked(player)||inputTimeStopped(player))break;
            if(LbcTmtAbilities.invoke(stack,call.slot,()->castSpell(player,hand,stack,call.spell)))completed++;
        }
        return completed;
    }

    private interface NativeAction { boolean use(ServerPlayer player,InteractionHand hand,ItemStack stack); }
    private record NativeUse(java.util.function.Predicate<ItemStack> matches,NativeAction action) {}
    private java.util.List<NativeUse> nativeUses(){
        return java.util.List.of(
                new NativeUse(stack->LbcDeus.matches(stack), (player,hand,stack)->game.items().deus().use(player,hand)),
                new NativeUse(stack->LbcKitUtilities.matches(stack), (player,hand,stack)->LbcKitUtilities.use(game,player,hand)),
                new NativeUse(stack->LbcSteampunkHook.matches(stack), (player,hand,stack)->game.items().steampunkHook().use(player,hand)),
                new NativeUse(stack->LbcWitchLust.matches(stack), (player,hand,stack)->game.items().witchLust().use(player,hand)),
                new NativeUse(stack->LbcWitchClaims.matches(stack), (player,hand,stack)->game.items().witchClaims().use(player,hand)),
                new NativeUse(stack->LbcWitchGluttony.matches(stack), (player,hand,stack)->game.items().witchGluttony().use(player,hand)),
                new NativeUse(stack->LbcWitchIndolence.matches(stack), (player,hand,stack)->game.items().witchIndolence().use(player,hand)),
                new NativeUse(stack->LbcShishEncounter.matches(stack), (player,hand,stack)->game.items().shishEncounter().use(player,hand)),
                new NativeUse(stack->LbcSpecialTools.scythe(stack)||LbcSpecialTools.hammer(stack), (player,hand,stack)->game.items().specialTools().use(player,hand)),
                new NativeUse(stack->LbcFpvDrone.matches(stack), (player,hand,stack)->game.items().fpvDrone().use(player,hand)),
                new NativeUse(stack->LbcUniwand.matches(stack), (player,hand,stack)->LbcUniwand.use(game,player,hand)),
                new NativeUse(stack->LbcBoba.matches(stack), (player,hand,stack)->game.items().boba().use(player,hand)),
                new NativeUse(stack->LbcBigBob.matches(stack), (player,hand,stack)->LbcBigBob.use(game,player,hand)),
                new NativeUse(stack->LbcImbaRunes.matches(stack), (player,hand,stack)->game.items().imbaRunes().use(player,hand)),
                new NativeUse(stack->LbcChaosRewardItems.matches(stack), (player,hand,stack)->LbcChaosRewardItems.use(game,player,hand)),
                new NativeUse(stack->LbcRewardTools.matches(stack), (player,hand,stack)->LbcRewardTools.use(game,player,hand)),
                new NativeUse(stack->LbcLooseRewards.luckyWand(stack), (player,hand,stack)->game.items().looseRewards().useLucky(player,hand)),
                new NativeUse(stack->LbcCommunism.matches(stack), (player,hand,stack)->game.items().communism().use(player,hand)),
                new NativeUse(stack->LbcPopit.matches(stack), (player,hand,stack)->game.items().popit().use(player,hand)),
                new NativeUse(stack->LbcPendingCombat.isPiston(stack), (player,hand,stack)->game.items().pendingCombat().use(player,hand)),
                new NativeUse(stack->LbcSoulhunter.isMace(stack), (player,hand,stack)->game.items().soulhunter().use(player,hand)),
                new NativeUse(stack->LbcSoulless.isWand(stack)||LbcSoulless.material(stack)>0, (player,hand,stack)->game.items().soulless().use(player,hand)),
                new NativeUse(stack->LbcDanmaku.isGrimoire(stack), (player,hand,stack)->game.items().danmaku().useGrimoire(player,hand)),
                new NativeUse(stack->LbcElementalGrimoires.kind(stack)!=null, (player,hand,stack)->LbcElementalGrimoires.use(game,player,hand)),
                new NativeUse(stack->LbcLibrarySpells.kind(stack)!=null, (player,hand,stack)->LbcLibrarySpells.use(game,player,hand)),
                new NativeUse(stack->LbcLibrarySpells.matchesRawRaycast(stack), (player,hand,stack)->LbcLibrarySpells.useRawRaycast(game,player,hand)),
                new NativeUse(stack->LbcSummonerItems.kind(stack)!=null, (player,hand,stack)->game.items().summons().useOrdinary(player,hand,LbcSummonerItems.kind(stack))),
                new NativeUse(stack->LbcDanmaku.isItem(stack), (player,hand,stack)->game.items().danmaku().use(player,hand)),
                new NativeUse(stack->LbcSheep.isWand(stack), (player,hand,stack)->game.items().sheep().use(player,hand)),
                new NativeUse(stack->LbcHedgehogItems.isWand(stack), (player,hand,stack)->game.items().hedgehog().use(player,hand)),
                new NativeUse(stack->LbcHedgehogItems.spike(stack)!=null, (player,hand,stack)->game.items().hedgehogSpikes().use(player,hand)),
                new NativeUse(stack->LbcHalloweenWind.isWind(stack), (player,hand,stack)->game.items().halloweenWind().use(player,hand)),
                new NativeUse(stack->LbcFartus.isFartus(stack), (player,hand,stack)->game.items().fartus().use(player,hand)),
                new NativeUse(stack->stack.is(FEATHER_GREED)||ItemData.intValue(stack,"feather_greed",0)==1, (player,hand,stack)->game.items().neutral().feather(player)),
                new NativeUse(stack->LbcHomeItems.isMirror(stack), (player,hand,stack)->game.items().homes().use(player, hand)),
                new NativeUse(stack->LbcShooters.isPine(stack) || LbcShooters.isSlr(stack) || LbcShooters.isKfcGun(stack)
                || LbcShooters.isBossHelicopter(stack) || LbcShooters.isPhantomKnives(stack) || LbcShooters.isWoodificator(stack) || LbcShooters.isHelicopter(stack), (player,hand,stack)->game.items().shooters().use(player, hand)),
                new NativeUse(stack->LbcGovnomet.isGovnomet(stack), (player,hand,stack)->game.items().govnomet().use(player,hand)),
                new NativeUse(stack->LbcMagnum.isMagnum(stack), (player,hand,stack)->game.items().magnum().use(player,hand)),
                new NativeUse(stack->LbcAk47.isAk47(stack), (player,hand,stack)->game.items().ak47().use(player,hand)),
                new NativeUse(stack->LbcLaserMinigun.isMinigun(stack), (player,hand,stack)->game.items().laserMinigun().use(player,hand)),
                new NativeUse(stack->LbcHohlopitek.isItem(stack), (player,hand,stack)->game.items().hohlopitek().use(player,hand)),
                new NativeUse(stack->LbcStand.isOraora(stack) || LbcStand.isClocks(stack) || LbcStand.isHomingLightning(stack), (player,hand,stack)->game.items().stand().use(player,hand)),
                new NativeUse(stack->LbcMobgun.isMobgun(stack), (player,hand,stack)->game.items().mobgun().use(player,hand)),
                new NativeUse(stack->LbcReactiveItems.isReactive(stack) || LbcReactiveItems.isWall(stack), (player,hand,stack)->game.items().reactive().use(player,hand)),
                new NativeUse(stack->LbcKfcRewards.isCultus(stack), (player,hand,stack)->game.items().kfcRewards().useCultus(player)),
                new NativeUse(stack->LbcElectro.isAnyKatana(stack), (player,hand,stack)->game.items().electro().use(player,hand)),
                new NativeUse(stack->LbcVexSword.isSword(stack), (player,hand,stack)->game.items().vexSword().use(player,hand)),
                new NativeUse(stack->LbcFevgaleaStar.isSword(stack), (player,hand,stack)->game.items().fevgaleaStar().use(player,hand)),
                new NativeUse(stack->LbcChargedSwords.matches(stack), (player,hand,stack)->game.items().chargedSwords().use(player,hand)),
                new NativeUse(stack->LbcMetalPipe.divider(stack), (player,hand,stack)->game.items().metalPipe().use(player,hand)),
                new NativeUse(stack->isDivider(stack), (player,hand,stack)->game.items().duplicating().useDivider(player,hand,stack)),
                new NativeUse(stack->stack.is(DESTROYER_SPELL) || ItemData.intValue(stack,"trap2",0)==1, (player,hand,stack)->game.items().consuming().useScroll(player,hand,stack,true)),
                new NativeUse(stack->stack.is(CREEPER_ARMY_SPELL) || ItemData.intValue(stack,"creeperz",0)==1, (player,hand,stack)->game.items().consuming().useScroll(player,hand,stack,false)));
    }
    boolean tmtNativeUseCandidate(ItemStack stack){
        return !stack.isEmpty()&&nativeUses.stream().anyMatch(route->route.matches.test(stack));
    }
    int tmtNativeUseOrder(ItemStack stack){
        for(int i=0;i<nativeUses.size();i++)if(nativeUses.get(i).matches.test(stack))return i;
        return nativeUses.size();
    }
    /** Same native dispatch and same real hand; only this property's own data/classifier is projected. */
    boolean castTmtNative(ServerPlayer player,InteractionHand hand,String slot){
        if(player==null||hand==null||hand==InteractionHand.OFF_HAND&&!game.config().offhandWands||!game.items().tmt().admitted(player))return false;
        var stack=player.getItemInHand(hand);
        return LbcTmtAbilities.invoke(stack,slot,()->cast(player,hand));
    }

    boolean cast(ServerPlayer player,InteractionHand hand) {
        if(!LbcDimensionItems.ordinary(player))return false;
        if(game.items().actionsBlocked(player)) return false;
        var stack=player.getItemInHand(hand);
        for(var route:nativeUses)if(route.matches.test(stack))return route.action.use(player,hand,stack);
        Spell spell=null;
        for(var candidate:SPELLS) if(matches(stack,candidate.item,candidate.flag)) { spell=candidate; break; }
        return spell!=null && castSpell(player,hand,stack,spell);
    }
    boolean hasSelectableMode(ItemStack stack){
        for(var candidate:SPELLS)if(matches(stack,candidate.item,candidate.flag))return switch(candidate){
            case ACADEMY_CORRUPTION,ACADEMY_ACID_CORRUPTION,ACADEMY_ERECTO,ACADEMY_TESLA,
                    ACADEMY_AWAKENED_ACID,ACADEMY_GAYCASTER,ACADEMY_CAUSTIC,ACADEMY_ADVANCED_ENDER,
                    ACADEMY_BAD_WIKI,ACADEMY_NEUTRAL,ACADEMY_CONFUSE,ACADEMY_EVOKER,ACADEMY_BASS,
                    ACADEMY_LIFESTEALER,ACADEMY_FIRE,ACADEMY_BLAZE,ACADEMY_FIREBLAZE,
                    ACADEMY_FALLING_STARS,ACADEMY_CONSUMING,ACADEMY_DUPLICATING,
                    ACADEMY_LIGHTNING_BOLTS,ACADEMY_BOLTBOLT,ACADEMY_EXAHOMING,
                    ACADEMY_PSYCHOWAND,ACADEMY_PSYCHOBLIND,ACADEMY_BLIND,ACADEMY_AIR,
                    ACADEMY_STONE,ACADEMY_TELEPORT,ACADEMY_RUBY,ACADEMY_REGULAR_ACID,
                    ACADEMY_ELECTRO,ACADEMY_EXASPERATO,ACADEMY_BEDROCK,ACADEMY_WALL,ACADEMY_STONEKIVI->true;
            default->false;
        };return false;
    }
    private boolean castSpell(ServerPlayer player,InteractionHand hand,ItemStack stack,Spell spell) {
        // Detached producers and the trial share ordinary admission/payment, once per real hand.
        if(spell==Spell.SAUVOJEN || spell==Spell.ACADEMY_SAUVOJEN) {
            var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            if(timeStopped() || player.entityTags().contains("time_stopped")
                    || game.items().magic().suppressedAt(player.level(),player.getEyePosition())
                    || !player.level().hasChunkAt(BlockPos.containing(player.getEyePosition().add(player.getLookAngle().scale(2))))) return false;
        }
        if(spell==Spell.ICE||spell==Spell.ACADEMY_ICE)return game.items().iceWand().use(player,hand,stack,spell==Spell.ACADEMY_ICE);
        if(spell==Spell.CORRUPTION||spell==Spell.ACADEMY_CORRUPTION)return LbcCorruption.use(game,player,hand,stack,spell==Spell.ACADEMY_CORRUPTION);
        if(spell==Spell.ACID_CORRUPTION||spell==Spell.ACADEMY_ACID_CORRUPTION)return game.items().acidCorruption().use(player,hand,stack,spell==Spell.ACADEMY_ACID_CORRUPTION);
        if(spell==Spell.ERECTO||spell==Spell.ACADEMY_ERECTO)return game.items().erecto().use(player,hand,stack,spell==Spell.ACADEMY_ERECTO);
        if(spell==Spell.TESLA||spell==Spell.ACADEMY_TESLA)return LbcTesla.use(game,player,hand,stack,spell==Spell.ACADEMY_TESLA);
        if(spell==Spell.AWAKENED_ACID||spell==Spell.ACADEMY_AWAKENED_ACID)return game.items().awakenedAcid().use(player,hand,stack,spell==Spell.ACADEMY_AWAKENED_ACID);
        if(spell==Spell.GAYCASTER||spell==Spell.ACADEMY_GAYCASTER)return LbcGaycaster.use(game,player,hand,stack);
        if(spell==Spell.REGULAR_PRISM||spell==Spell.ACADEMY_PRISM)return game.items().prism().use(player,hand,stack,spell==Spell.ACADEMY_PRISM);
        if(spell==Spell.REGULAR_CAUSTIC || spell==Spell.ACADEMY_CAUSTIC || spell==Spell.REGULAR_OMEGA || spell==Spell.ACADEMY_OMEGA)
            return LbcCausticOmega.use(game,player,hand,stack,spell==Spell.REGULAR_CAUSTIC || spell==Spell.ACADEMY_CAUSTIC,
                    spell==Spell.ACADEMY_CAUSTIC || spell==Spell.ACADEMY_OMEGA);
        if(spell==Spell.REGULAR_ADVANCED_ENDER || spell==Spell.ACADEMY_ADVANCED_ENDER || spell==Spell.REGULAR_SNOW || spell==Spell.ACADEMY_SNOW)
            return game.items().advancedSnow().use(player,hand,stack,spell==Spell.REGULAR_SNOW || spell==Spell.ACADEMY_SNOW,
                    spell==Spell.ACADEMY_ADVANCED_ENDER || spell==Spell.ACADEMY_SNOW);
        if(spell==Spell.REGULAR_ENDER || spell==Spell.ACADEMY_ENDER || spell==Spell.REGULAR_BAD_WIKI || spell==Spell.ACADEMY_BAD_WIKI)
            return game.items().enderWiki().use(player,hand,stack,spell==Spell.REGULAR_BAD_WIKI || spell==Spell.ACADEMY_BAD_WIKI,
                    spell==Spell.ACADEMY_BAD_WIKI || spell==Spell.ACADEMY_ENDER);
        if(spell==Spell.REGULAR_NEUTRAL || spell==Spell.ACADEMY_NEUTRAL) return game.items().neutral().use(player,hand,stack,spell==Spell.ACADEMY_NEUTRAL);
        if(spell==Spell.REGULAR_BLOOD || spell==Spell.ACADEMY_BLOOD || spell==Spell.REGULAR_CONFUSE || spell==Spell.ACADEMY_CONFUSE)
            return game.items().bloodConfuse().use(player,hand,stack,spell==Spell.REGULAR_BLOOD || spell==Spell.ACADEMY_BLOOD,
                    spell==Spell.ACADEMY_BLOOD || spell==Spell.ACADEMY_CONFUSE);
        if(spell==Spell.REGULAR_EVOKER || spell==Spell.ACADEMY_EVOKER || spell==Spell.REGULAR_BASS || spell==Spell.ACADEMY_BASS)
            return game.items().evokerBass().use(player,hand,stack,spell==Spell.REGULAR_EVOKER || spell==Spell.ACADEMY_EVOKER,
                    spell==Spell.ACADEMY_EVOKER || spell==Spell.ACADEMY_BASS);
        if(spell==Spell.REGULAR_LIFESTEALER || spell==Spell.ACADEMY_LIFESTEALER)
            return game.items().lifestealer().use(player,hand,stack,spell==Spell.ACADEMY_LIFESTEALER);
        if(spell==Spell.SUMMONER_VEXES || spell==Spell.PSYCHOVEX)
            return game.items().summons().use(player,hand,stack,spell==Spell.PSYCHOVEX);
        if(spell==Spell.INFBOLT) return game.items().bolts().useInfbolt(player,hand,stack);
        if(spell==Spell.REGULAR_CONSUMING || spell==Spell.ACADEMY_CONSUMING)
            return game.items().consuming().use(player,hand,stack,spell==Spell.ACADEMY_CONSUMING);
        if(spell==Spell.REGULAR_DUPLICATING || spell==Spell.ACADEMY_DUPLICATING)
            return game.items().duplicating().use(player,hand,stack,spell==Spell.ACADEMY_DUPLICATING);
        if(spell==Spell.REGULAR_LIGHTNING_BOLTS || spell==Spell.ACADEMY_LIGHTNING_BOLTS
                || spell==Spell.REGULAR_BOLTBOLT || spell==Spell.ACADEMY_BOLTBOLT)
            return game.items().bolts().use(player,hand,stack,
                    spell==Spell.REGULAR_LIGHTNING_BOLTS || spell==Spell.ACADEMY_LIGHTNING_BOLTS,
                    spell==Spell.ACADEMY_LIGHTNING_BOLTS || spell==Spell.ACADEMY_BOLTBOLT);
        if(spell==Spell.REGULAR_FALLING_STARS || spell==Spell.ACADEMY_FALLING_STARS
                || spell==Spell.REGULAR_EXAHOMING || spell==Spell.ACADEMY_EXAHOMING)
            return game.items().homing().use(player,hand,stack,
                    spell==Spell.REGULAR_FALLING_STARS || spell==Spell.ACADEMY_FALLING_STARS,
                    spell==Spell.ACADEMY_FALLING_STARS || spell==Spell.ACADEMY_EXAHOMING);
        if(spell==Spell.REGULAR_PSYCHOWAND || spell==Spell.ACADEMY_PSYCHOWAND
                || spell==Spell.REGULAR_PSYCHOBLIND || spell==Spell.ACADEMY_PSYCHOBLIND)
            return game.items().psycho().use(player,hand,stack,
                    spell==Spell.REGULAR_PSYCHOWAND || spell==Spell.ACADEMY_PSYCHOWAND,
                    spell==Spell.ACADEMY_PSYCHOWAND || spell==Spell.ACADEMY_PSYCHOBLIND);
        if(spell==Spell.REGULAR_BLAZE || spell==Spell.ACADEMY_BLAZE
                || spell==Spell.REGULAR_FIREBLAZE || spell==Spell.ACADEMY_FIREBLAZE)
            return game.items().blaze().use(player,hand,stack,
                    spell==Spell.REGULAR_BLAZE || spell==Spell.ACADEMY_BLAZE,
                    spell==Spell.ACADEMY_BLAZE || spell==Spell.ACADEMY_FIREBLAZE);
        // У комнаты платное создание, но бесплатные отзыв/режимы; общий платёж ниже к ним неприменим.
        if(spell==Spell.REGULAR_BEDROCK || spell==Spell.ACADEMY_BEDROCK)
            return game.items().bedrock().use(player,hand,stack,spell==Spell.ACADEMY_BEDROCK);
        if((spell==Spell.REGULAR_WALL || spell==Spell.ACADEMY_WALL)
                && (timeStopped() || game.items().magic().suppressedAt(player.level(),player.getEyePosition()))) return false;
        if(spell==Spell.ACADEMY_WALL && player.isShiftKeyDown())
            return LbcWall.cycle(game,player,stack);
        if((spell==Spell.REGULAR_STONEKIVI || spell==Spell.ACADEMY_STONEKIVI)
                && (timeStopped() || !game.items().stonekivi().ready()
                    || game.items().magic().suppressedAt(player.level(),player.getEyePosition()))) return false;
        // (user) Смена режима — настройка предмета: не требует маны и не запускает бонусы каста.
        if(spell==Spell.ACADEMY_STONEKIVI && player.isShiftKeyDown()) {
            if(!mana.visible(player) || player.entityTags().contains("time_stopped") || game.items().magic().suppressed(player)
) return false;
            var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            int mode=LbcModeScroll.next(data.getIntOr("mode",1),5);
            data.putInt("mode",mode); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            player.sendSystemMessage(Component.translatable(switch(mode) {
                case 2 -> "mode_earth";
                case 3 -> "mode_sun";
                case 4 -> "mode_comets";
                case 5 -> "mode_stonekivi_sun";
                default -> "mode_default";
            }));
            return true;
        }
        if((spell==Spell.EXASPERATO || spell==Spell.ACADEMY_EXASPERATO) && timeStopped()) return false;
        boolean electric=spell==Spell.ELECTRO || spell==Spell.REGULAR_ELECTRO || spell==Spell.ACADEMY_ELECTRO;
        if(electric && timeStopped()) return false;
        if((spell==Spell.REGULAR_EXOPERSO || spell==Spell.ACADEMY_EXOPERSO)
                && (timeStopped() || game.items().magic().suppressedAt(player.level(),player.getEyePosition()))) return false;
        if((spell==Spell.REGULAR_RUBY || spell==Spell.ACADEMY_RUBY) && timeStopped()) return false;
        if(spell==Spell.ACADEMY_RUBY && player.isShiftKeyDown()) {
            if(!mana.visible(player) || player.entityTags().contains("time_stopped")
                    || game.items().magic().suppressed(player)
) return false;
            var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            int mode=LbcModeScroll.next(data.getIntOr("mode",1),3);
            data.putInt("mode",mode); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            player.sendSystemMessage(Component.translatable(switch(mode) {
                case 2 -> "mode_hole";
                case 3 -> "mode_ray";
                default -> "mode_charge";
            }));
            return true;
        }
        if((spell==Spell.REGULAR_TELEPORT || spell==Spell.ACADEMY_TELEPORT) && timeStopped()) return false;
        if(spell==Spell.ACADEMY_TELEPORT && player.isShiftKeyDown()) {
            if(!mana.visible(player) || player.entityTags().contains("time_stopped")
                    || game.items().magic().suppressed(player)
) return false;
            var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            int mode=LbcModeScroll.next(data.getIntOr("mode",1),3);
            data.putInt("mode",mode); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            player.sendSystemMessage(Component.translatable(switch(mode) {
                case 2 -> "mode_inverted";
                case 3 -> "mode_cloud";
                default -> "mode_default";
            }));
            return true;
        }
        if((spell==Spell.REGULAR_AIR || spell==Spell.ACADEMY_AIR) && timeStopped()) return false;
        if(spell==Spell.ACADEMY_AIR && player.isShiftKeyDown()) {
            if(!mana.visible(player) || player.entityTags().contains("time_stopped")
                    || game.items().magic().suppressed(player)
) return false;
            var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            int mode=LbcModeScroll.next(data.getIntOr("mode",1),3);
            data.putInt("mode",mode); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            player.sendSystemMessage(Component.translatable(switch(mode) {
                case 2 -> "mode_fast_rocket";
                case 3 -> "mode_homing";
                default -> "mode_burst";
            }));
            return true;
        }
        if((spell==Spell.REGULAR_BLIND || spell==Spell.ACADEMY_BLIND) && timeStopped()) return false;
        if(spell==Spell.ACADEMY_BLIND && player.isShiftKeyDown()) {
            if(!mana.visible(player) || player.entityTags().contains("time_stopped")
                    || game.items().magic().suppressed(player)
) return false;
            var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            int mode=LbcModeScroll.next(data.getIntOr("mode",1),4);
            data.putInt("mode",mode); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            player.sendSystemMessage(Component.translatable(switch(mode) {
                case 2 -> "mode_phantom";
                case 3 -> "mode_rain";
                case 4 -> "mode_gatlingator";
                default -> "mode_default";
            }));
            return true;
        }
        if(spell==Spell.ACADEMY_STONE && player.isShiftKeyDown()) {
            if(!mana.visible(player) || player.entityTags().contains("time_stopped")
                    || game.items().magic().suppressed(player)
) return false;
            var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            int mode=LbcModeScroll.next(data.getIntOr("mode",1),4);
            data.putInt("mode",mode); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            player.sendSystemMessage(Component.translatable(switch(mode) {
                case 2 -> "mode_shield";
                case 3 -> "mode_bridge";
                case 4 -> "mode_spike";
                default -> "mode_default";
            }));
            return true;
        }
        if(spell==Spell.ACADEMY_FIRE && player.isShiftKeyDown()) {
            if(!mana.visible(player) || player.entityTags().contains("time_stopped")
                    || game.items().magic().suppressed(player) || game.items().frost().isFrozen(player)) return false;
            var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            int mode=LbcModeScroll.next(data.getIntOr("mode",1),3);
            data.putInt("mode",mode); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            player.sendSystemMessage(Component.translatable(mode==1 ? "mode_default" : mode==2 ? "mode_big" : "mode_concentrated"));
            return true;
        }
        if(spell==Spell.ACADEMY_EXASPERATO && player.isShiftKeyDown()) {
            if(!mana.visible(player) || player.entityTags().contains("time_stopped")
                    || game.items().magic().suppressed(player) || game.items().frost().isFrozen(player)) return false;
            var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            int mode=LbcModeScroll.next(data.getIntOr("mode",1),4);
            data.putInt("mode",mode); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            player.sendSystemMessage(Component.translatable(switch(mode) {
                case 2 -> "mode_electro_ray";
                case 3 -> "mode_chaos_area";
                case 4 -> "mode_nuke";
                default -> "mode_default";
            }));
            return true;
        }
        if(spell==Spell.ACADEMY_ELECTRO && player.isShiftKeyDown()) {
            if(!mana.visible(player) || player.entityTags().contains("time_stopped")
                    || game.items().magic().suppressed(player)) return false;
            var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            int mode=LbcModeScroll.next(data.getIntOr("mode",1),2);
            data.putInt("mode",mode);
            stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            player.sendSystemMessage(Component.translatable(mode==1 ? "mode_default" : "mode_ray"));
            return true;
        }
        if(spell==Spell.REGULAR_ACID || spell==Spell.ACADEMY_REGULAR_ACID) {
            if(inputTimeStopped(player) || game.items().magic().suppressedAt(player.level(),player.getEyePosition())) return false;
            if(spell==Spell.ACADEMY_REGULAR_ACID && player.isShiftKeyDown()) {
                if(!mana.visible(player) || game.items().magic().suppressed(player)) return false;
                // Mode selection is independent per used hand and never enters the paid cast path.
                var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
                int mode=LbcModeScroll.next(data.getIntOr("mode",1),2);
                data.putInt("mode",mode);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));player.getInventory().setChanged();
                player.sendSystemMessage(Component.translatable(mode==1 ? "mode_default" : "mode_ignited"));
                return true;
            }
        }
        // Academy Wall keeps its original 500 default through its own reloadable operand.
        int requiredMana=mana.score(spell.cost,"lbc.math").get();
        int spentMana=spell==Spell.REGULAR_EXOPERSO?25000:spell==Spell.ACADEMY_EXOPERSO?10000:requiredMana;
        if(!mana.trySpendWand(player,requiredMana,spentMana)) return false;
        switch(spell) {
            case SAUVOJEN, ACADEMY_SAUVOJEN -> {
                if(!game.items().sauvojen().meditation().start(player,hand,stack))
                    game.items().sauvojen().cast(player,hand,stack);
            }
            case REGULAR_STONEKIVI -> game.items().stonekivi().cast(player,hand,stack,false);
            case ACADEMY_STONEKIVI -> game.items().stonekivi().cast(player,hand,stack,true);
            case REGULAR_WALL -> LbcWall.cast(game,player,hand,stack,false);
            case ACADEMY_WALL -> LbcWall.cast(game,player,hand,stack,true);
            case FIRE -> fire(player);
            case REGULAR_EXOPERSO -> {
                if(game.items().exoperso().cast(player,false)) game.items().academy().exopersoTrial(player,hand,stack);
            }
            case ACADEMY_EXOPERSO -> game.items().exoperso().cast(player,true);
            case REGULAR_RUBY -> {
                if(!game.items().academy().rubyTrial(player,hand,stack)) game.items().ruby().cast(player,0);
            }
            case ACADEMY_RUBY -> game.items().ruby().cast(player,
                    Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("mode",1),1,3));
            case REGULAR_TELEPORT -> {
                game.items().teleport().cast(player,hand,0,stack);
                game.items().academy().teleportTrial(player,hand,stack);
            }
            case ACADEMY_TELEPORT -> game.items().teleport().cast(player,hand,
                    Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("mode",1),1,3),stack);
            case REGULAR_AIR -> game.items().air().cast(player,hand,0,stack);
            case ACADEMY_AIR -> game.items().air().cast(player,hand,
                    Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("mode",1),1,3),stack);
            case REGULAR_BLIND -> game.items().blind().cast(player,hand,0,stack);
            case ACADEMY_BLIND -> game.items().blind().cast(player,hand,
                    Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("mode",1),1,4),stack);
            case REGULAR_STONE -> {
                game.items().stone().cast(player,0);
                game.items().academy().stoneTrial(player,hand,stack);
            }
            case ACADEMY_STONE -> game.items().stone().cast(player,
                    Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("mode",1),1,4));
            case REGULAR_FIRE -> {
                fireVolley(player);
                game.items().academy().fireTrial(player,hand,stack);
            }
            case ACADEMY_FIRE -> {
                int mode=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("mode",1);
                if(mode==2) bigFire(player);
                else if(mode==3) concentratedFire(player);
                else fireVolley(player);
            }
            case EXASPERATO -> game.items().exasperato().cast(player);
            case ACADEMY_EXASPERATO -> game.items().exasperato().cast(player,
                    Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("mode",1),1,4));
            case RUBY -> rubyRay(player);
            case TERRA -> terraRay(player);
            case REGULAR_TERRA -> {
                if(!game.items().academy().terraTrial(player,hand,stack)) game.items().terra().cast(player);
            }
            case ACADEMY_TERRA -> game.items().terra().cast(player,LbcTerra.Kind.ACADEMY);
            case TRUE_TERRA -> game.items().terra().cast(player,LbcTerra.Kind.TRUE_CHARGE);
            case REGULAR_ACID -> {
                game.items().clouds().ordinaryWandAcid(player);
                // The source trial checks burning at the cast, without replacing the normal shot.
                if(player.isOnFire()) game.items().academy().ready(player,hand,stack);
                LbcOverload.sound(player,player.getEyePosition(),"minecraft:entity.zombie_villager.cure",1,2,0);
            }
            case ACADEMY_REGULAR_ACID -> {
                boolean ignited=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("mode",1)==2;
                game.items().clouds().academyWandAcid(player,ignited);
                LbcOverload.sound(player,player.getEyePosition(),"minecraft:entity.zombie_villager.cure",1,2,0);
            }
            case ELECTRO -> electro(player);
            case REGULAR_ELECTRO -> {
                electroVolley(player);
                var data=stack.get(DataComponents.CUSTOM_DATA);
                if(player.isShiftKeyDown() && data!=null && data.copyTag().getIntOr("upgradable",0)==1) {
                    electroMeditation(player);
                    game.items().academy().electroTrial(player,hand,stack);
                }
            }
            case ACADEMY_ELECTRO -> {
                var data=stack.get(DataComponents.CUSTOM_DATA);
                if(data!=null && data.copyTag().getIntOr("mode",1)==2) electroRay(player);
                else electroVolley(player);
            }
            default -> ray(player,spell);
        }
        wandCasted(player);
        return true;
    }
    /** Общие бонусы только исполнившегося заклинания; бесплатный отзыв комнаты их не выдаёт. */
    void wandCasted(ServerPlayer player) { wandCasted(player,true); }
    void wandCasted(ServerPlayer player,boolean mageBonus) {
        if(mageBonus&&mana.mageFullSet(player)) mana.maximum(player,mana.maximum(player)+mana.score(MAGE_BONUS,"lbc.math").get());
        game.items().armorSets().nyauwu().wandCasted(player);
        game.items().trinkets().castWand(player);
    }
    void extraTiny(ServerPlayer p,int kind){
        switch(kind){
            case 0->fire(p);case 1->ray(p,Spell.BLIND);case 2->ray(p,Spell.AIR);
            case 3->ray(p,Spell.STONE);case 4->ray(p,Spell.TELEPORT);case 5->rubyRay(p);
            case 6->terraRay(p);case 7->ray(p,Spell.ACID);case 8->electro(p);
            default->throw new IllegalArgumentException("Unknown tiny spell "+kind);
        }
    }
    private void fire(ServerPlayer player) {
        smallFire(player,0);
        LbcOverload.sound(player,player.getEyePosition(),"minecraft:item.firecharge.use",1,2,0);
    }
    private void smallFire(ServerPlayer player,double side) {
        Vec3 direction=player.getLookAngle();
        var projectile=new SmallFireball(EntityTypes.SMALL_FIREBALL,player.level());
        Vec3 left=Vec3.directionFromRotation(0,player.getYRot()-90);
        launchFire(player,projectile,direction.scale(2.5).add(left.scale(side)));
    }
    /** Три исходных Motion-вектора; native fireball выполняет swept collision и затухание скорости. */
    void fireVolley(ServerPlayer player) {
        smallFire(player,0); smallFire(player,.15); smallFire(player,-.15);
        LbcOverload.sound(player,player.getEyePosition(),"minecraft:item.firecharge.use",1,1,0);
    }
    void bigFire(ServerPlayer player) {
        var projectile=new LargeFireball(player.level(),player,player.getLookAngle(),2);
        launchFire(player,projectile,player.getLookAngle().scale(2.5));
        LbcOverload.sound(player,player.getEyePosition(),"minecraft:item.firecharge.use",1,.5f,0);
    }
    private void launchFire(ServerPlayer player,Projectile projectile,Vec3 velocity) {
        launchFire(player,projectile,player.getEyePosition().add(player.getLookAngle()),velocity);
    }
    /** Общий native spawn для Fire и Blaze: owner, guards, эпоха и cleanup одинаковы. */
    void launchFire(ServerLevel level,SmallFireball projectile,Vec3 position,Vec3 velocity,LbcSpellSource.Source source){
        if(!LbcBedwarsRoleGuards.canEmit(source,level))return;
        try(var scope=game.items().spellSources().scope(source,level)){
            projectile.setPos(position);projectile.setDeltaMovement(velocity);LbcShooters.orient(projectile);
            projectile.setOwner(level.getEntity(source.emitter()));
            projectile.entityTags().addAll(java.util.List.of(FIRE_PROJECTILE,"stwandfireball","can_deleted"));
            game.items().markCurrentItemEpoch(projectile);game.items().spellSources().capture(projectile);
            game.items().trinkets().spawn(projectile);
        }
    }
    void launchFire(ServerPlayer player,Projectile projectile,Vec3 position,Vec3 velocity) {
        projectile.setPos(position);
        // Исходный marker в начале мира вычислял именно этот вектор, не мировую цель полёта.
        projectile.setDeltaMovement(velocity);
        LbcShooters.orient(projectile);
        adoptFire(player,projectile);
        game.items().trinkets().spawn(projectile);
    }
    /** Также подхватывает уже созданный native огненный шар призванного/подчинённого ифрита. */
    void adoptFire(ServerPlayer player,Projectile projectile) {
        projectile.setOwner(player);
        projectile.entityTags().add(FIRE_PROJECTILE);
        projectile.entityTags().add("stwandfireball");
        projectile.entityTags().add("can_deleted");
        game.items().markCurrentItemEpoch(projectile);
    }
    public boolean isFireProjectile(Entity entity) { return entity.entityTags().contains(FIRE_PROJECTILE); }
    /** Общая геометрия жезлов: первое пересечение хитбокса или блока, без загрузки новых чанков. */
    net.minecraft.world.phys.HitResult firstHit(ServerPlayer owner,Vec3 from,Vec3 to) {
        return firstHit(owner,from,to,entity->true);
    }
    net.minecraft.world.phys.HitResult firstHit(ServerPlayer owner,Vec3 from,Vec3 to,
            java.util.function.Predicate<Entity> extraFilter) {
        return firstHit(owner,from,to,extraFilter,false);
    }
    net.minecraft.world.phys.HitResult firstHit(ServerPlayer owner,Vec3 from,Vec3 to,
            java.util.function.Predicate<Entity> extraFilter,boolean includeAllies) {
        return firstHit(game.items().spellSources().level(owner),owner,from,to,extraFilter,includeAllies);
    }
    net.minecraft.world.phys.HitResult firstHit(ServerLevel level,ServerPlayer owner,Vec3 from,Vec3 to,
            java.util.function.Predicate<Entity> extraFilter,boolean includeAllies) {
        var wall=LbcTerra.wallHit(level,from,to);
        Vec3 end=wall==null ? to : wall.getLocation();
        var entity=ProjectileUtil.getEntityHitResult(level,game.items().spellSources().current()==null?owner:null,from,end,new AABB(from,end).inflate(1),
                target->(includeAllies ? target instanceof LivingEntity && target(owner,target,true)
                        && target.getType().builtInRegistryHolder().is(MOBS)
                        : game.items().lifetimes().target(owner,target)) && extraFilter.test(target),0);
        return entity==null ? wall : entity;
    }
    /** Source NPC guns share resident block geometry and native entity hitboxes. */
    net.minecraft.world.phys.HitResult firstHit(LivingEntity owner,Vec3 from,Vec3 to,
            java.util.function.Predicate<Entity> collision) {
        if(from.equals(to)) {
            var targets=owner.level().getEntities(owner,new AABB(from,to).inflate(1),
                    target->target!=owner && collision.test(target) && target.getBoundingBox().contains(from));
            if(!targets.isEmpty()) return new net.minecraft.world.phys.EntityHitResult(targets.getFirst(),from);
            var level=(ServerLevel)owner.level();var pos=BlockPos.containing(from);
            if(level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null) return null;
            if(!level.getBlockState(pos).is(AIRS))
                return new net.minecraft.world.phys.BlockHitResult(from,net.minecraft.core.Direction.UP,pos,true);
        }
        var wall=LbcTerra.wallHit((ServerLevel)owner.level(),from,to);
        var end=wall==null?to:wall.getLocation();
        var entity=ProjectileUtil.getEntityHitResult(owner.level(),owner,from,end,new AABB(from,end).inflate(1),
                target->target!=owner && collision.test(target),0);
        return entity==null?wall:entity;
    }
    /** Вместо рекурсивных 200 запросов: один native clip и один локальный поиск ближайшей цели. */
    void concentratedFire(ServerPlayer player) {concentratedFireAt(player,player.getEyePosition(),player.getLookAngle());}
    private void concentratedFireAt(ServerPlayer player,Vec3 from,Vec3 direction) {
        Vec3 to=from.add(direction.scale(100));
        var level=player.level();
        double boundary=game.items().magic().suppressionFraction(level,from,to);
        if(Double.isFinite(boundary))to=from.lerp(to,boundary);
        var hit=firstHit(player,from,to);
        boolean terminal=hit!=null && (!Double.isFinite(boundary)||from.distanceToSqr(hit.getLocation())<from.distanceToSqr(to)-1e-9)
                && (!(hit instanceof net.minecraft.world.phys.BlockHitResult wall) || level.hasChunkAt(wall.getBlockPos()));
        if(hit!=null) to=hit.getLocation();
        double distance=from.distanceTo(to);
        try(var fireVisual=new LbcParticleBatch.Group(level,ParticleTypes.FALLING_LAVA,.05,0,true)){
for(double step=0;step<=distance;step+=.5) {
            var point=from.add(direction.scale(step));
            fireVisual.add(point);

        }
}
        LbcOverload.sound(player,from,"minecraft:block.lava.ambient",1,.1f,0);
        if(terminal) {
            Vec3 end=to;
            game.items().indirectKills().worldChanges(level,player,()-> {
                for(var pos:BlockPos.betweenClosed(BlockPos.containing(end.add(-.3,-.3,-.3)),BlockPos.containing(end.add(.3,.3,.3))))
                    if(level.hasChunkAt(pos)) level.setBlockAndUpdate(pos,Blocks.LAVA.defaultBlockState());
            });
        }
    }
    /** (user) Ruby намеренно прорезает всю линию, включая неразрушимые обычным инструментом блоки. */
    private void rubyRay(ServerPlayer player) {
        var level=player.level();
        Vec3 origin=player.getEyePosition(),step=player.getLookAngle().scale(.35);
        double boundary=game.items().magic().suppressionFraction(level,origin,origin.add(step.scale(100)))*100;
        BlockPos previous=null;
        for(int i=0;i<=100 && i<boundary;i++) {
            Vec3 point=origin.add(step.scale(i));
            var pos=BlockPos.containing(point);
            if(!level.hasChunkAt(pos)) break;
            level.sendParticles(RUBY_PARTICLE,true,false,point.x,point.y,point.z,1,0,0,0,0);
            // Шаг меньше блока: соседние samples одной клетки не требуют повторного destroy/loot.
            if(pos.equals(previous)) continue;
            previous=pos;
            destroyRubyCell(player,pos);
        }
        game.items().indirectKills().finishSupportRemoval();
        LbcOverload.sound(player,origin,"minecraft:block.amethyst_cluster.break",1,2,0);
    }
    /** Общий destructive fill Ruby: одна клетка, один native loot и автор утраченной опоры. */
    boolean destroyRubyCell(ServerPlayer player,BlockPos pos) {
        var level=game.items().spellSources().level(player);
        if(level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) return false;
        var old=level.getBlockState(pos);
        if(old.isAir()) return false;
        game.items().indirectKills().supportRemoved(player,pos);
        level.destroyBlock(pos,true,player,Block.UPDATE_LIMIT);
        if(level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS))
            level.updateNeighboursOnBlockSet(pos,old);
        return true;
    }
    boolean destroyRubyCell(LivingEntity source,BlockPos pos) {
        if(source instanceof ServerPlayer player) return destroyRubyCell(player,pos);
        var level=(ServerLevel)source.level();
        if(level.isOutsideBuildHeight(pos) || level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null) return false;
        var old=level.getBlockState(pos);
        if(old.isAir()) return false;
        level.destroyBlock(pos,true,source,Block.UPDATE_LIMIT);
        if(level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS))
            level.updateNeighboursOnBlockSet(pos,old);
        return true;
    }
    /** (user) Terra пробивает существ, но каждая цель получает лишь одну попытку 5 cactus-урона за каст. */
    private void terraRay(ServerPlayer player) {
        var level=player.level();
        Vec3 origin=player.getEyePosition(),step=player.getLookAngle().scale(.35);
        Vec3 end=origin;
        double boundary=game.items().magic().suppressionFraction(level,origin,origin.add(step.scale(100)))*100;
        BlockPos wall=null;
        for(int i=0;i<=100;i++) {
            if(i>=boundary){end=origin.add(step.scale(boundary));break;}
            Vec3 point=origin.add(step.scale(i));
            var pos=BlockPos.containing(point);
            if(!level.hasChunkAt(pos)) break;
            end=point;
            if(player.getRandom().nextFloat()<.3f)
                level.sendParticles(TERRA_PARTICLE,true,false,point.x,point.y,point.z,1,0,0,0,0);
            LbcOverload.sound(player,point,"minecraft:block.amethyst_cluster.hit",.1f,2,0);
            if(!level.getBlockState(pos).is(AIRS)) {
                wall=pos;
                // Попадания заканчиваются на границе стены, а не внутри последнего visual sample.
                end=new AABB(pos).clip(origin,point).orElse(origin);
                break;
            }
        }
        // (user) Нативный отрезок против хитбоксов: один spatial query и одна проверка на кандидата.
        // 101 visual samples выше больше не умножают число проверок сущностей.
        var hits=ProjectileUtil.getManyEntityHitResult(level,player,origin,end,new AABB(origin,end).inflate(1),
                entity->projectileTarget(player,entity),0,ClipContext.Block.COLLIDER,true,false);
        var damage=new DamageSource(player.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.CACTUS),player);
        for(var hit:hits) if(projectileTarget(player,hit.getEntity())
                &&(!Double.isFinite(boundary)||origin.distanceToSqr(hit.getLocation())<step.lengthSqr()*boundary*boundary-1e-9)) {
            // Каждая цель встречается один раз; нативную защиту от повторного урона не сбрасываем.
            hit.getEntity().hurtServer(level,damage,5);
        }
        if(wall!=null) {
            // Source setblock сначала ставит траву над стеной, затем превращает стену в мох.
            placeTerraBlock(player,wall.above(),Blocks.SHORT_GRASS);
            placeTerraBlock(player,wall,Blocks.MOSS_BLOCK);
        }
    }
    private void placeTerraBlock(ServerPlayer player,BlockPos pos,Block block) {
        var level=player.level();
        if(!level.hasChunkAt(pos) || !level.isInValidBounds(pos)) return;
        var old=level.getBlockState(pos);
        // Нативные setblock flags 258, включая обновление соседей после успешной замены.
        if(level.setBlock(pos,block.defaultBlockState(),Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS))
            level.updateNeighboursOnBlockSet(pos,old);
    }
    /** От блока дуга уходит по наружной нормали грани; попадание в существо сохраняет направление выстрела. */
    private void electro(ServerPlayer owner) {
        var level=owner.level();
        Vec3 from=owner.getEyePosition(),direction=owner.getLookAngle(),end=from;
        boolean wall=false;
        Vec3 wallNormal=null;
        double boundary=game.items().magic().mechanismSuppressionFraction(level,from,from.add(direction.scale(35)))*35;
        for(int i=0;i<=100;i++) {
            if(i*.35>=boundary){end=from.add(direction.scale(boundary));break;}
            Vec3 point=from.add(direction.scale(i*.35));
            var pos=BlockPos.containing(point);
            if(!level.hasChunkAt(pos)) break;
            end=point;
            if(!level.getBlockState(pos).is(AIRS)) {
                // clip не принимает t=1: короткое продление сохраняет контакт точно на конце шага.
                Vec3 pastFace=point.add(direction.scale(1e-4));
                var contact=Shapes.block().clip(from,pastFace,pos);
                end=contact==null ? new AABB(pos).clip(from,pastFace).orElse(from) : contact.getLocation();
                // Native clip даёт именно задетую грань, в том числе при косом выстреле.
                wallNormal=(contact==null ? Direction.getApproximateNearest(direction.reverse())
                        : contact.getDirection()).getUnitVec3();
                wall=true; break;
            }
        }
        var hit=ProjectileUtil.getEntityHitResult(level,owner,from,end,new AABB(from,end).inflate(1),
                entity->projectileTarget(owner,entity) && !entity.entityTags().contains("raycaster"),0);
        if(hit!=null) end=hit.getLocation();
        double distance=from.distanceTo(end);
        for(int i=0;i<=100 && i*.35<=distance+1e-7;i++) {
            Vec3 point=from.add(direction.scale(i*.35));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,true,false,point.x,point.y,point.z,1,0,0,0,0);
        }
        if((hit!=null || wall)&&(!Double.isFinite(boundary)||from.distanceToSqr(end)<boundary*boundary-1e-9)) {
            boolean blockContact=hit==null && wall;
            Vec3 start=blockContact ? end.add(wallNormal) : end.subtract(direction);
            if(level.hasChunkAt(BlockPos.containing(start))) {
                var trace=new ElectroTrace(owner,start,game.items().epoch());
                if(blockContact) {
                    trace.yaw=(float)-Math.toDegrees(Math.atan2(wallNormal.x,wallNormal.z));
                    trace.pitch=(float)-Math.toDegrees(Math.atan2(wallNormal.y,wallNormal.horizontalDistance()));
                }
                electroTraces.add(trace);
                LbcOverload.sound(owner,start,"minecraft:entity.firework_rocket.twinkle",1,1,0);
            }
        }
        LbcOverload.sound(owner,from,"minecraft:entity.firework_rocket.twinkle",1,1,0);
    }
    public int electroCount() { return electroTraces.size(); }
    /** NPC electricity shares collision, conductivity and reflection; its original cause survives launch. */
    void mechanismElectro(LivingEntity source,Vec3 origin,Vec3 direction) {
        if(!LbcBedwarsRoleGuards.canEmitFrom(source))return;
        electroTraces.add(new ElectroTrace(source,origin,direction,game.items().epoch()));
    }
    /** Boss emitters reuse the same arc geometry; their causal player is not the emitter. */
    void hostileSauvojenElectro(ServerLevel level,Vec3 origin,Vec3 direction,LbcSpellSource.Source source){
        if(!LbcBedwarsRoleGuards.canEmit(source,level))return;
        for(int i=0;i<4;i++){var trace=new ElectroTrace(level,origin,direction,source,20);
            trace.owner=game.items().spellSources().authorPlayer(source);electroTraces.add(trace);}
        LbcOverload.sound(level,origin,"minecraft:entity.firework_rocket.twinkle",1,1,0,0);
    }
    void cyborgElectro(LivingEntity emitter,LbcSpellSource.Source source){
        if(!LbcBedwarsRoleGuards.canEmitFrom(emitter)||!LbcBedwarsRoleGuards.canEmit(source,(ServerLevel)emitter.level()))return;
        var direction=emitter.getLookAngle();var level=(ServerLevel)emitter.level();var at=emitter.getEyePosition();
        for(int i=0;i<4;i++){var trace=new ElectroTrace(level,at.subtract(direction.scale(i*.01)),direction,source,30+i*5);
            trace.owner=game.items().spellSources().authorPlayer(source);electroTraces.add(trace);}
        LbcOverload.sound(level,at,"minecraft:entity.firework_rocket.twinkle",1,1,0,0);
    }
    void hostileSauvojenElectro(ServerPlayer author,Vec3 origin,Vec3 direction,LbcSpellSource.Source source) {
        if(!LbcBedwarsRoleGuards.canEmit(source,author.level()))return;
        for(int i=0;i<4;i++){
            var trace=new ElectroTrace(author,origin,game.items().epoch(),20);
            trace.sourcePolicy=source;trace.cause=source.cause();trace.hostileSauvojen=true;
            trace.yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
            trace.pitch=(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));
            electroTraces.add(trace);
        }
        LbcOverload.sound(author,origin,"minecraft:entity.firework_rocket.twinkle",1,1,0);
    }
    void libraryFlyingElectro(ServerPlayer owner,Vec3 origin,Vec3 direction){
        for(int lifetime:new int[]{35,20,25,30}){
            var trace=new ElectroTrace(owner,origin,game.items().epoch(),lifetime);
            trace.yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
            trace.pitch=(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));electroTraces.add(trace);
        }
        LbcOverload.sound(owner,origin,"minecraft:entity.firework_rocket.twinkle",1,1,0);
    }
    void sauvojenElectro(ServerPlayer owner,Vec3 origin,Vec3 direction,boolean ray){
        if(ray){electroRayAt(owner,origin,direction);return;}
        float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
        float pitch=(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));
        for(int i=0;i<3;i++){
            var trace=new ElectroTrace(owner,origin.subtract(direction.scale(i*.01)),game.items().epoch(),40+i*5);
            trace.yaw=yaw;trace.pitch=pitch;electroTraces.add(trace);
        }
        LbcOverload.sound(owner,origin,"minecraft:entity.firework_rocket.twinkle",1,1,0);
    }
    void sauvojenFire(ServerPlayer owner,Vec3 origin,Vec3 direction,int mode){
        if(mode==2){concentratedFireAt(owner,origin,direction);return;}
        if(mode==1){
            var shot=new LargeFireball(owner.level(),owner,direction,2);
            launchFire(owner,shot,origin.add(direction),direction.scale(2.5));
        }else{
            Vec3 left=new Vec3(direction.z,0,-direction.x).normalize();
            for(double side:new double[]{0,.15,-.15}){
                var shot=new SmallFireball(EntityTypes.SMALL_FIREBALL,owner.level());
                launchFire(owner,shot,origin.add(direction),direction.scale(2.5).add(left.scale(side)));
            }
        }
        LbcOverload.sound(owner,origin,"minecraft:item.firecharge.use",1,mode==1?.5f:1,0);
    }
    /** Source Raid Omen124 emits three upward, ten-tick WILD arcs per chained contact. */
    void chainStatusElectro(LivingEntity emitter,ServerPlayer owner,Vec3 at,LbcIndirectKills.Owner cause){
        if(!LbcBedwarsRoleGuards.canEmitFrom(emitter))return;
        for(int i=0;i<3;i++)electroTraces.add(new ElectroTrace(emitter,owner,at,game.items().epoch(),cause));
    }
    /** Accepted projectile consequences retain the same ten-tick WILD arcs after their caster leaves. */
    void arrowChainElectro(Entity projectile,ServerPlayer owner,Vec3 at,LbcIndirectKills.Owner accepted){
        var wild=accepted==null?null:new LbcIndirectKills.Owner(accepted.player(),accepted.order(),true,true);
        var source=new LbcSpellSource.Source(projectile.getUUID(),accepted==null?null:accepted.player(),game.items().epoch(),wild,java.util.Set.of(),true);
        for(int i=0;i<3;i++){var trace=new ElectroTrace((ServerLevel)projectile.level(),owner,owner,at,source.epoch(),wild);trace.sourcePolicy=source;trace.explicitEmitter=projectile;electroTraces.add(trace);}
    }
    /** Виртуальные разряды участвуют в том же отражении, что нативные снаряды. Возраст сохраняется. */
    void reflectElectro(ServerPlayer player) {
        for(var trace:electroTraces) if(trace.level==player.level() && trace.position.distanceToSqr(player.position())<=49) {
            trace.owner=player; trace.yaw+=180; trace.pitch=-trace.pitch;
            trace.sourcePolicy=null;trace.cause=null;trace.explicitEmitter=null;
            trace.level.sendParticles(ParticleTypes.ELECTRIC_SPARK,trace.position.x,trace.position.y,trace.position.z,5,0,0,0,.3);
        }
    }
    /** Аксессуар присваивает только вражеские/нейтральные разряды; свои и союзные не разворачивает. */
    void reflectElectroblocker(ServerPlayer player) {
        for(var trace:electroTraces) if(trace.level==player.level() && trace.owner!=player
                && !player.isAlliedTo(trace.source()) && trace.position.distanceToSqr(player.position())<=196) {
            trace.owner=player; trace.yaw+=180; trace.pitch=-trace.pitch;
            trace.sourcePolicy=null;trace.cause=null;trace.explicitEmitter=null;
            trace.level.sendParticles(ParticleTypes.ELECTRIC_SPARK,trace.position.x,trace.position.y,trace.position.z,10,0,0,0,.3);
        }
    }
    /** Три разряда расходятся случайным поворотом при полёте; сроки исходника различаются. */
    /** Techno armor retaliates with the original three fifteen-tick mechanical arcs. */
    void technoRetaliation(ServerPlayer owner) {
        var direction=owner.getLookAngle();var origin=owner.getEyePosition();
        for(int i=0;i<3;i++){
            var trace=new ElectroTrace(owner,origin.subtract(direction.scale(i*.01)),game.items().epoch(),15);
            trace.mechanism=true;electroTraces.add(trace);
        }
        LbcOverload.sound(owner,origin,"minecraft:entity.firework_rocket.twinkle",1,1,0);
    }
    void electroVolley(ServerPlayer owner) {
        Vec3 origin=owner.getEyePosition(),direction=owner.getLookAngle();
        for(int i=0;i<3;i++)
            electroTraces.add(new ElectroTrace(owner,origin.subtract(direction.scale(i*.01)),game.items().epoch(),40+i*5));
        LbcOverload.sound(owner,origin,"minecraft:entity.firework_rocket.twinkle",1,1,0);
    }

    /** Tesla reuses Electro's native geometry, ownership, shield, conduction and lifecycle. */
    void teslaVolley(ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean chain) {
        var direction=owner.getLookAngle();var origin=owner.getEyePosition();
        boolean academy=stack.is(ACADEMY_TESLA)||stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("upgraded",0)==1;
        if(hand==InteractionHand.OFF_HAND&&!academy)origin=origin.add(direction.scale(.5));
        for(int i=0;i<(chain?1:4);i++){
            var trace=new ElectroTrace(owner,origin.subtract(direction.scale(i*.01)),game.items().epoch(),chain?150:30+i*5);
            trace.teslaChain=chain;
            if(!chain&&LbcAcademy.trial(stack)){trace.trialOwner=owner;trace.trialHand=hand;trace.trialStack=stack;}
            electroTraces.add(trace);
        }
        LbcOverload.sound(owner,origin,"minecraft:entity.firework_rocket.twinkle",1,1,0);
    }
    void teslaConduction(ElectroTrace trace,Vec3 point){electroConduction(trace,point);}
    private void teslaChainTurn(ElectroTrace trace){
        var point=trace.position;LivingEntity nearest=null;double nearestDistance=100;
        trace.level.sendParticles(ParticleTypes.END_ROD,point.x,point.y,point.z,5,0,0,0,.05);
        for(float volume:new float[]{1,1.5f,2})electroSound(trace,point,"minecraft:lbcsounds.laser_mg_shot",volume,1);
        for(var entity:trace.level.getEntitiesOfClass(LivingEntity.class,new AABB(point,point).inflate(10),
                entity->entity.getType().builtInRegistryHolder().is(MOBS)&&target(trace.owner,entity))){
            double distance=entity.position().distanceToSqr(point);
            if(distance<=25)return;
            if(distance<=nearestDistance){nearest=entity;nearestDistance=distance;}
        }
        if(nearest!=null){
            Vec3 direction=nearest.getEyePosition().subtract(point);
            trace.yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
            trace.pitch=(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));
        }
    }
    /** Культист использует те же разряды, срок и владельца, меняется только точка запуска. */
    void cultistElectro(ServerPlayer owner,Vec3 origin,float yaw,float pitch) {
        electroVolley(owner,origin,yaw,pitch,20,6);
    }
    void racerLightning(ServerPlayer owner,Vec3 origin) {
        electroVolley(owner,origin,0,90,30,4);
    }
    /** Magnum uses the same arcs, with its short lifetimes and mechanism-beacon policy. */
    void magnumElectro(ServerPlayer owner,Vec3 origin,Vec3 target) {
        shortElectro(owner,origin,target,true);
    }
    /** Released electricity is nonmagical; all producers share mechanism-beacon interception. */
    void shortElectro(ServerPlayer owner,Vec3 origin,Vec3 target,boolean mechanism) {
        Vec3 direction=target.subtract(origin);
        float yaw=direction.horizontalDistanceSqr()<1e-12 ? owner.getYRot()
                : (float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
        float pitch=direction.lengthSqr()<1e-12 ? owner.getXRot()
                : (float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));
        for(int i=0;i<4;i++) {
            var trace=new ElectroTrace(owner,origin,game.items().epoch(),5+i*2);
            trace.yaw=yaw; trace.pitch=pitch; trace.mechanism=true;
            electroTraces.add(trace);
        }
        LbcOverload.sound(owner,origin,"minecraft:entity.firework_rocket.twinkle",1,1,0);
    }
    private void electroVolley(ServerPlayer owner,Vec3 origin,float yaw,float pitch,int firstLifetime,int count) {
        for(int i=0;i<count;i++) {
            var trace=new ElectroTrace(owner,origin,game.items().epoch(),firstLifetime+i*5);
            trace.yaw=yaw; trace.pitch=pitch; electroTraces.add(trace);
        }
    }
    /** (user) Квестовая медитация: принадлежность не защищает никого, автор урона сохраняется. */
    private void electroMeditation(ServerPlayer owner) {
        for(int i=0;i<60;i++) {
            var trace=new ElectroTrace(owner,owner.getEyePosition(),game.items().epoch(),10,true);
            trace.yaw=owner.getRandom().nextInt(361)-180;
            trace.pitch=owner.getRandom().nextInt(181)-90;
            electroTraces.add(trace);
        }
    }
    /** Режим Academy: одна геометрическая проверка пути и хитбоксов вместо рекурсии на 1001 шаг. */
    void electroRay(ServerPlayer owner) {electroRayAt(owner,owner.getEyePosition(),owner.getLookAngle());}
    private void electroRayAt(ServerPlayer owner,Vec3 from,Vec3 direction) {
        var level=owner.level();
        Vec3 end=from.add(direction.scale(100));
        double boundary=game.items().magic().mechanismSuppressionFraction(level,from,end);
        if(Double.isFinite(boundary))end=from.lerp(end,Math.max(0,Math.nextDown(boundary)));
        var wall=LbcTerra.wallHit(level,from,end);
        if(wall!=null) end=wall.getLocation();
        var shield=shieldHit(owner,from,end);
        if(shield!=null) {
            end=shield.getLocation();
            LbcOverload.sound(owner,shield.getEntity().position(),"minecraft:item.shield.block",1,1,0);
        }
        double distance=from.distanceTo(end);
        try(var denseDust=new LbcParticleBatch.Group(level,ELECTRO_PARTICLE,.1,0,true);var denseSpark=new LbcParticleBatch.Group(level,ParticleTypes.ELECTRIC_SPARK,.1,1,false)){
for(int i=0;i<=1000 && i*.1<=distance;i++) if(owner.getRandom().nextFloat()<.35f) {
            Vec3 point=from.add(direction.scale(i*.1));
            denseDust.add(point);
            denseSpark.add(point);

        }
}
        if(distance>0)LbcOverload.sound(owner,from.lerp(end,.5),"minecraft:entity.firework_rocket.twinkle",.03f,2,0);
        // Проводники останавливают этот режим; отдельного импульса проводимости в нём нет.
        if(distance>1e-7) electroHits(new ElectroTrace(owner,from,game.items().epoch()),from,end,.1,.4f,1);
        LbcOverload.sound(owner,from,"minecraft:entity.firework_rocket.twinkle",1,1,0);
    }
    void transformOrigins(ServerLevel level,java.util.function.UnaryOperator<Vec3> transform) {
        electroTraces.removeIf(trace->{
            if(trace.level!=level) return false;
            Vec3 next=transform.apply(trace.position);
            if(next==null) return true;
            trace.position=next;
            if(trace.erecto!=null&&trace.erecto.focus!=null)trace.erecto.focus=transform.apply(trace.erecto.focus);
            return false;
        });
    }

    void higherElectro(ElectroTrace trace){if(LbcBedwarsRoleGuards.canEmitFrom(trace.npcSource)&&LbcBedwarsRoleGuards.canEmitFrom(trace.explicitEmitter)&&LbcBedwarsRoleGuards.canEmit(trace.sourcePolicy,trace.level))electroTraces.add(trace);}
    java.util.List<ElectroTrace> electroSnapshot() { return java.util.List.copyOf(electroTraces); }
    /** Danmaku clears moving electrical traces as well as their entity-backed counterparts. */
    int clearDanmakuRays(ServerPlayer caster,Vec3 center,double radius) {
        int before=electroTraces.size();double distance=radius*radius;
        electroTraces.removeIf(trace->{
            if(trace.level!=caster.level() || trace.position.distanceToSqr(center)>distance) return false;
            var source=trace.source();
            if(source!=null && !validRoleTarget(source,false)) return false;
            var owner=trace.cause==null ? LbcIndirectKills.playerSource(source)
                    : game.server().getPlayerList().getPlayer(trace.cause.player());
            return owner!=caster && (owner==null || owner.getTeam()==null || owner.getTeam()!=caster.getTeam());
        });
        return before-electroTraces.size();
    }
    /** Electro хранится записями, поэтому глобальный Curse использует тот же выбор цели без marker-сущностей. */
    void redirectCursedElectro() {
        for(var trace:electroTraces) game.items().homing().redirect(trace);
    }
    boolean timeStopped() {
        return game.maps().level()!=null && game.items().timeStop().stopped(game.maps().level())
                || legacyTimeStopped();
    }
    /** Stand/часы освобождают ввод владельца, но не таймеры и полёт всего измерения. */
    boolean inputTimeStopped(ServerPlayer player) {
        return game.items().timeStop().frozen(player) || player.entityTags().contains("time_stopped")
                || legacyTimeStopped();
    }
    boolean legacyTimeStopped() {
        var board=game.server().getScoreboard(); var objective=board.getObjective("lbc.math");
        var stopped=objective==null ? null : board.getPlayerScoreInfo(TIME_STOPPED,objective);
        return stopped!=null && stopped.value()>=1;
    }
    /** Вызывается общим item tick; snapshot устойчив к stop/death callbacks во время hurtServer. */
    void tick() {
        if(electroTraces.isEmpty()) return;
        boolean timeStopped=timeStopped();
        electroBatch.addAll(electroTraces);
        try {
            for(var trace:electroBatch) {
                if(!electroTraces.contains(trace)) continue;
                if(!LbcBedwarsRoleGuards.canEmitFrom(trace.npcSource)||!LbcBedwarsRoleGuards.canEmitFrom(trace.explicitEmitter)||!LbcBedwarsRoleGuards.canEmit(trace.sourcePolicy,trace.level)){electroTraces.remove(trace);continue;}
                var owner=trace.owner;
                boolean accepted=trace.sourcePolicy!=null&&trace.sourcePolicy.persistent();
                if(trace.age>=trace.lifetime || !game.items().currentEpoch(trace.epoch) || !accepted&&trace.level!=game.maps().level()
                        || !accepted&&(trace.source()==null || trace.source().level()!=trace.level)
                        || !accepted&&owner!=null && (!owner.isAlive() || owner.isRemoved() || !game.canUseGameplayItems(owner))
                        || game.stage()==GameController.Stage.ENDING) {
                    electroTraces.remove(trace); continue;
                }
                if(timeStopped || trace.sourcePolicy==null && owner!=null && owner.entityTags().contains("time_stopped")) continue;
                if(game.items().magic().mechanismSuppressedAt(trace.level,trace.position)){electroTraces.remove(trace);continue;}
                // (user) Мана/антимагия проверяются при использовании жезла; выпущенная дуга немагическая.
                try(var scope=game.items().spellSources().scope(trace.sourcePolicy,trace.level)){
                    var author=trace.cause==null?LbcIndirectKills.playerSource(trace.source()):game.server().getPlayerList().getPlayer(trace.cause.player());
                    var original=Vec3.directionFromRotation(trace.pitch,trace.yaw);
                    var direction=game.items().homing().skillDirection(author,trace.level,trace.position,original);
                    if(direction!=original){
                        trace.yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
                        trace.pitch=(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));
                    }
                    if(!trace.level.hasChunkAt(BlockPos.containing(trace.position))
                            || (trace.erecto!=null ? !game.items().erecto().step(trace) : trace.teslaChain&&trace.age%5!=0 ? false : !electroStep(trace)) || ++trace.age>=trace.lifetime) electroTraces.remove(trace);
                }
            }
        } finally { electroBatch.clear(); }
    }
    private boolean electroStep(ElectroTrace trace) {
        var level=trace.level;
        Vec3 from=trace.position,direction=Vec3.directionFromRotation(trace.pitch,trace.yaw);
        int samples=trace.teslaChain?20:trace.meditation || trace.npcSource!=null || trace.hostileSauvojen ? 18 : 10;
        double mechanismFraction=game.items().magic().mechanismSuppressionFraction(
                level,from,from.add(direction.scale(samples*.2)));
        double mechanismDistance=mechanismFraction*samples*.2;
        var greed=LbcGreed.firstHit(level,from,from.add(direction.scale(samples*.2)),.15);
        // TECH contact wins a tie and prevents reflection from a victim beyond the destroyed arc.
        if(greed!=null && from.distanceToSqr(greed.getLocation())<mechanismDistance*mechanismDistance-1e-9
                && greed.getEntity() instanceof ServerPlayer reflector && reflector!=trace.owner) {
            trace.owner=reflector; trace.yaw+=180; trace.pitch=-trace.pitch;
            trace.sourcePolicy=null;trace.cause=null;trace.explicitEmitter=null;
            return true;
        }
        var shield=shieldHit(trace.level,trace.source(),from,from.add(direction.scale(samples*.2)),trace.meditation||trace.statusWild);
        double shieldDistance=shield==null ? Double.POSITIVE_INFINITY : from.distanceTo(shield.getLocation());
        double stopDistance=Math.min(shieldDistance,mechanismDistance);
        Vec3 end=from,conductor=null;
        boolean clear=true;boolean emitted=false;
        var dust=new LbcParticleBatch.Group(level,ELECTRO_PARTICLE,.1,0,true);
        var sparksVisual=new LbcParticleBatch.Group(level,ParticleTypes.ELECTRIC_SPARK,.1,1,false);
        var rods=new LbcParticleBatch.Group(level,ParticleTypes.END_ROD,.1,.1,false);
        BlockPos previousPos=null;net.minecraft.world.level.block.state.BlockState previousBlock=null;
        for(int i=0;i<=samples && i*.2<stopDistance-1e-7;i++) {
            Vec3 point=from.add(direction.scale(i*.2));
            var pos=BlockPos.containing(point);
            if(!level.hasChunkAt(pos)) { clear=false; break; }
            end=point;
            dust.add(point);sparksVisual.add(point);emitted=true;
            var block=pos.equals(previousPos)?previousBlock:level.getBlockState(pos);previousPos=pos;previousBlock=block;
            if(block.is(CONDUCTIVE)) {
                if(conductor==null) conductor=point;
                Vec3 sparks=point.subtract(direction.scale(.5));
                rods.add(sparks,5);
            } else if(!block.is(AIRS)) {
                end=new AABB(pos).clip(from,point).orElse(from); clear=false; break;
            }
        }
        dust.flush();sparksVisual.flush();rods.flush();
        if(emitted)electroSound(trace,from.lerp(end,.5),"minecraft:entity.firework_rocket.twinkle",.03f,2);
        // (user) Щит обрезает путь на границе сферы, не отменяя эффекты уже пройденной части.
        if(clear && Double.isFinite(mechanismDistance) && mechanismDistance<=shieldDistance) {
            end=from.add(direction.scale(mechanismDistance)); clear=false;
        } else if(clear && shield!=null) {
            end=shield.getLocation(); clear=false;
            electroSound(trace,shield.getEntity().position(),"minecraft:item.shield.block",1,1);
        }
        if(from.distanceToSqr(end)>1e-12) electroHits(trace,from,end);
        if(conductor!=null)try(var scope=game.items().spellSources().scope((LbcSpellSource.Source)null)){electroConduction(trace,conductor);}
        trace.position=end;
        // В исходнике это случайное преломление каждые два блока, а не отражение от стены.
        if(trace.teslaChain){teslaChainTurn(trace);return clear;}
        trace.yaw=net.minecraft.util.Mth.floor(trace.yaw*100)*.01f+(level.getRandom().nextInt(1601)-800)*.01f;
        trace.pitch=net.minecraft.util.Mth.floor(trace.pitch*100)*.01f+(level.getRandom().nextInt(1601)-800)*.01f;
        return clear;
    }
    EntityHitResult shieldHit(ServerPlayer owner,Vec3 from,Vec3 to) {
        return shieldHit(owner,from,to,false);
    }
    private void electroSound(ElectroTrace trace,Vec3 at,String id,float volume,float pitch) {
        if(trace.owner!=null&&trace.owner.level()==trace.level) LbcOverload.sound(trace.owner,at,id,volume,pitch,0);
        else trace.level.playSound(null,at.x,at.y,at.z,net.minecraft.sounds.SoundEvent.createVariableRangeEvent(
                Identifier.parse(id)),net.minecraft.sounds.SoundSource.MASTER,volume,pitch);
    }
    EntityHitResult shieldHit(LivingEntity owner,Vec3 from,Vec3 to,boolean meditation) {
        var level=owner instanceof ServerPlayer player?game.items().spellSources().level(player)
                :owner==null?game.items().spellSources().level(null):(ServerLevel)owner.level();
        return level==null?null:shieldHit(level,owner,from,to,meditation);
    }
    private EntityHitResult shieldHit(ServerLevel level,LivingEntity owner,Vec3 from,Vec3 to,boolean meditation) {
        Vec3 delta=to.subtract(from);
        double lengthSquared=delta.lengthSqr();
        EntityHitResult nearest=null;
        double nearestT=1;
        for(var player:level.getEntitiesOfClass(ServerPlayer.class,new AABB(from,to).inflate(4),
                player->electroTarget(owner,player,meditation) && player.isBlocking())) {
            Vec3 center=player.position().add(player.getLookAngle());
            Vec3 offset=from.subtract(center);
            double c=offset.lengthSqr()-4,t=0;
            if(c>0) {
                double b=offset.dot(delta),discriminant=b*b-lengthSquared*c;
                if(lengthSquared<=1e-12 || b>=0 || discriminant<0) continue;
                t=(-b-Math.sqrt(discriminant))/lengthSquared;
            }
            if(t>=0 && t<=nearestT) {
                nearestT=t;
                nearest=new EntityHitResult(player,from.add(delta.scale(t)));
            }
        }
        return nearest;
    }
    /** (user) Урон зависит от длины контакта: сохраняем отдельные 0.5 hits, не объединяя их против брони. */
    private void electroHits(ElectroTrace trace,Vec3 from,Vec3 to) {
        electroHits(trace,from,to,.2,.5f,trace.meditation || trace.npcSource!=null || trace.hostileSauvojen ? Integer.MAX_VALUE : 2);
    }
    private void electroHits(ElectroTrace trace,Vec3 from,Vec3 to,double spacing,float amount,int targetLimit) {
        var damage=new DamageSource(trace.level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(trace.hostileSauvojen ? LbcDamage.ELECTRIC : trace.meditation ? LbcDamage.ELECTRO_MEDITATION : LbcDamage.ELECTRO_BOLT),trace.statusWild?trace.owner:trace.damageEmitter());
        LbcDamage.hurtSegment(trace.level,game.items().indirectKills().captureDamage(damage,traceCause(trace)),from,to,Vec3.ZERO,spacing,amount,targetLimit,
                entity->entity.getType().builtInRegistryHolder().is(MOBS)
                        && (trace.statusWild?validTarget(entity):electroTarget(trace.source(),entity,trace.meditation)),trace.meditation||trace.statusWild||trace.sourcePolicy!=null);
    }
    private LbcIndirectKills.Owner traceCause(ElectroTrace trace){
        if(trace.statusWild&&trace.owner!=null&&(trace.cause==null||!trace.cause.player().equals(trace.owner.getUUID())))
            trace.cause=game.items().indirectKills().wildCause(trace.owner);
        return trace.cause;
    }
    private boolean electroTarget(LivingEntity owner,Entity entity,boolean meditation) {
        var policy=game.items().spellSources().target(owner instanceof ServerPlayer player?player:null,entity);if(policy!=null)return policy;
        if(!meditation && owner instanceof ServerPlayer player) return projectileTarget(player,entity);
        if(!meditation && !validTarget(entity)) return false;
        var tags=entity.entityTags();
        return entity.isAlive() && !entity.isRemoved() && !entity.isSpectator() && !tags.contains("spectator")
                && !tags.contains("rcdenyalways") && !tags.contains("sauvojen_tuntija")
                && !tags.contains("transformator") && !tags.contains("cyborg_boss") && !tags.contains("summoned_cultist")
                && (!(entity instanceof ServerPlayer player) || game.canUseGameplayItems(player));
    }
    /** Проводник: один импульс за тик луча; только Electro-типы обходят cooldown, остальные не меняем. */
    private void electroConduction(ElectroTrace trace,Vec3 point) {
        int accepted=0;
        var damage=game.items().indirectKills().captureDamage(new DamageSource(trace.level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(LbcDamage.ELECTRO_CONDUCTION),trace.statusWild?trace.owner:trace.damageEmitter()),traceCause(trace));
        for(var entity:trace.level.getEntitiesOfClass(LivingEntity.class,new AABB(point,point).inflate(10),
                entity->validTarget(entity) && entity.getType().builtInRegistryHolder().is(MOBS)
                        && !entity.isSpectator() && !entity.entityTags().contains("spectator")
                        && !entity.entityTags().contains("rcdenyalways") && entity.position().distanceToSqr(point)<=100
                        && (!(entity instanceof ServerPlayer player) || game.canUseGameplayItems(player)))) {
            // (user) Проводящий разряд поражает и стрелка/союзников; прямой луч фильтруется отдельно.
            if(entity.hurtServer(trace.level,damage,1))accepted++;
            trace.level.sendParticles(ParticleTypes.FIREWORK,true,false,
                    entity.getX(),entity.getY()+1,entity.getZ(),4,.1,.1,.1,0);
        }
        if(accepted>=10&&trace.owner==trace.trialOwner&&trace.trialStack!=null)
            game.items().academy().ready(trace.trialOwner,trace.trialHand,trace.trialStack);
    }
    void stop() { electroTraces.clear(); }
    /** Малые жезлы делят мгновенный луч: кандидаты выбираются один раз на каст, без marker-тикеров. */
    void blindRay(ServerPlayer player) {
        ray(player,Spell.BLIND);
    }
    private static AABB rayBounds(Vec3 origin,Vec3 step) {
        // Попадание требует пересечения ячейки point..point+1: дальность selector 8 не расширяет её.
        // Включаем предыдущий шаг для wall-end у самых глаз; лишние секции вокруг луча не опрашиваются.
        return new AABB(origin.subtract(step),origin.add(step.scale(100))).expandTowards(1,1,1);
    }
    private void ray(ServerPlayer player,Spell spell) {
        Vec3 origin=player.getEyePosition(), step=player.getLookAngle().scale(.35);
        // (user) Tiny acid is already released physical acid; only its paid cast is magical.
        double boundary=spell==Spell.ACID?Double.POSITIVE_INFINITY:game.items().magic().suppressionFraction(player.level(),origin,origin.add(step.scale(100)))*100;
        var candidates=player.level().getEntities((Entity)null,rayBounds(origin,step),e->projectileTarget(player,e)
                &&(spell==Spell.ACID||!game.items().magic().suppressedAt(player.level(),e.position())));
        ParticleOptions particle=switch(spell) {
            case BLIND -> ParticleTypes.SQUID_INK;
            case AIR -> ParticleTypes.FIREWORK;
            case STONE -> new BlockParticleOption(ParticleTypes.BLOCK,Blocks.STONE.defaultBlockState());
            case TELEPORT -> ParticleTypes.CLOUD;
            case ACID -> ParticleTypes.SNEEZE;
            default -> throw new IllegalArgumentException("Not a ray spell: "+spell);
        };
        try(var rayVisual=new LbcParticleBatch.Group(player.level(),particle,0,0,true)){
for(int i=0;i<=100 && i<boundary;i++) {
            Vec3 point=origin.add(step.scale(i));
            if(spell!=Spell.BLIND || player.getRandom().nextFloat()<.3f)
                rayVisual.add(point);
            Entity hit=rayTarget(player,candidates,point);
            if(hit!=null) {
                // (user) Первый контакт завершает луч: исходный повтор при entity+wall был багом.
                rayEnd(player,spell,point,hit);
                break;
            }
            var pos=BlockPos.containing(point);
            if(!player.level().hasChunkAt(pos) || !player.level().getBlockState(pos).is(AIRS)) {
                // Стена оставляет terminal на предыдущей свободной точке, действие выполняется один раз.
                var previous=point.subtract(step);
                rayEnd(player,spell,previous,rayTarget(player,candidates,previous));
                break;
            }
        }
}
        raySound(player,spell,origin);
    }
    private void raySound(ServerPlayer player,Spell spell,Vec3 point) {
        String sound=switch(spell) {
            case BLIND -> "minecraft:entity.squid.squirt";
            case AIR -> "minecraft:entity.generic.explode";
            case STONE -> "minecraft:block.stone.place";
            case TELEPORT -> "minecraft:entity.lightning_bolt.thunder";
            case ACID -> "minecraft:entity.zombie_villager.cure";
            default -> throw new IllegalArgumentException("Not a ray spell: "+spell);
        };
        LbcOverload.sound(player,point,sound,1,2,0);
    }
    private void rayEnd(ServerPlayer player,Spell spell,Vec3 point,Entity hit) {
        switch(spell) {
            case BLIND -> { blindEnd(player,point,hit); return; }
            case AIR -> {
                var rocket=new FireworkRocketEntity(EntityTypes.FIREWORK_ROCKET,player.level());
                // Нативный load задаёт LifeTime=0 и source-палитру без reflection/accessor к private полям.
                rocket.load(TagValueInput.create(ProblemReporter.DISCARDING,player.registryAccess(),AIR_ROCKET.copy()));
                rocket.setOwner(player);
                rocket.setPos(point);
                rocket.entityTags().add(AIR_PROJECTILE);
                game.items().trinkets().spawn(rocket);
            }
            case STONE -> {
                game.items().indirectKills().fillTrap(player,BlockPos.containing(point.add(-1,-1,-1)),
                        BlockPos.containing(point.add(1,1,1)),Blocks.STONE.defaultBlockState());
            }
            case TELEPORT -> {
                var cell=new AABB(point,point.add(1,1,1));
                // Union исходных двух селекторов: одинаковый эффект не требует двух записей одной цели.
                for(var entity:player.level().getEntitiesOfClass(LivingEntity.class,new AABB(point,point).inflate(12),e->projectileTarget(player,e)))
                    if(entity.getType().builtInRegistryHolder().is(MOBS) && (entity.position().distanceToSqr(point)<=4
                            || entity.position().distanceToSqr(point)<=144 && entity.getBoundingBox().intersects(cell)))
                        if(entity.addEffect(ItemEffectLevels.create(MobEffects.LEVITATION,20,30),player))
                            game.items().indirectKills().levitated(entity,player);
            }
            case ACID -> {
                game.items().clouds().wandAcid(player,point);
                // Source preloop также играет cure, помимо terminal и wrapper.
                raySound(player,spell,point);
            }
            default -> throw new IllegalArgumentException("Not a ray spell: "+spell);
        }
        raySound(player,spell,point);
    }
    private Entity rayTarget(ServerPlayer player,java.util.List<Entity> candidates,Vec3 point) {
        var cell=new AABB(point,point.add(1,1,1));
        for(var entity:candidates) if(projectileTarget(player,entity) && entity.position().distanceToSqr(point)<=64
                && entity.getBoundingBox().intersects(cell)) return entity;
        return null;
    }
    private void blindEnd(ServerPlayer player,Vec3 point,Entity hit) {
        if(hit!=null) {
            if(hit instanceof LivingEntity living) living.addEffect(ItemEffectLevels.create(MobEffects.BLINDNESS,80,0));
            hit.hurtServer(player.level(),new DamageSource(player.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                    .getOrThrow(DamageTypes.ARROW),player),.25f);
        }
        LbcOverload.sound(player,point,"minecraft:entity.squid.squirt",1,2,0);
    }
    /** Projectile impacts protect allied players; friendly mobs remain physical damage targets. */
    public boolean projectileTarget(ServerPlayer player,Entity entity) { return projectileTarget(player,entity,false); }
    /** Physical missiles can return into their caster or cross a teammate; acquisition stays separate. */
    public static boolean physicalProjectile(Entity projectile) {
        return projectile instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow
                || projectile instanceof net.minecraft.world.entity.projectile.ShulkerBullet;
    }
    public boolean physicalProjectileTarget(Entity projectile,Entity target) {
        return physicalProjectile(projectile) && validTarget(target)&&LbcBedwarsHarm.projectileAllowed(projectile,target);
    }
    /** Accepted lethal hits still trigger their sword; role/team exclusions remain authoritative. */
    boolean projectileTarget(ServerPlayer player,Entity entity,boolean allowDying) {
        var policy=game.items().spellSources().target(player,entity);if(policy!=null)return policy;
        if(LbcBedwarsHarm.active()&&player!=entity&&player.isAlliedTo(entity))return false;
        return entity!=player && !LbcGreed.active(entity) && validTarget(entity,allowDying)
                && (!(entity instanceof ServerPlayer) || !player.isAlliedTo(entity));
    }
    public boolean target(ServerPlayer player,Entity entity) {
        return target(player,entity,false);
    }
    /** Узкое исключение притягивания Ruby: роли остаются защищены, союзников можно включить отдельно. */
    boolean target(ServerPlayer player,Entity entity,boolean includeAllies) {
        var policy=game.items().spellSources().target(player,entity);if(policy!=null)return policy;
        return entity!=player && !LbcGreed.active(entity) && validTarget(entity)
                && (includeAllies || !player.isAlliedTo(entity) && !game.items().blaze().friendly(player,entity) && !game.items().summons().friendly(player,entity));
    }
    boolean validTarget(Entity entity) { return validTarget(entity,false); }
    boolean validTarget(Entity entity,boolean allowDying) {
        return !game.items().polymorph().concealed(entity) && !LbcGreed.active(entity) && validRoleTarget(entity,allowDying);
    }
    boolean validRoleTarget(Entity entity,boolean allowDying) {
        return validRoleTarget(entity,allowDying,false);
    }
    boolean validRoleTarget(Entity entity,boolean allowDying,boolean allowSauvojen) {
        if(LbcBedwarsRoleGuards.shop(entity))return false;
        return (allowDying || entity.isAlive()) && !entity.isRemoved() && !entity.isSpectator() && !entity.entityTags().contains("spectator")
                && !entity.entityTags().contains("rcdenyalways") && (allowSauvojen||!entity.entityTags().contains("sauvojen_tuntija"))
                && !entity.entityTags().contains("strict_map_object")
                && !entity.entityTags().contains("magic_academy")
                && (!entity.entityTags().contains("transformator") || LbcTransformator.managed(entity))
                && (!(entity instanceof ServerPlayer p) || game.canUseGameplayItems(p));
    }
    /** Нативный canHitEntity пропускает союзников и наблюдателей до поджигания/урона small fireball. */
    private boolean nativeSelfTarget(Projectile projectile,Entity target) {
        return projectile.getOwner()==target && target instanceof ServerPlayer && validTarget(target)
                && !ProjectileLifetimes.protectedWandProjectile(projectile)
                && !(projectile instanceof LbcClouds.AcidProjectile)
                && game.items().shooters().canHit(projectile,target)
                && game.items().electro().canHit(projectile,target);
    }
    public boolean canHit(Projectile projectile,Entity target) {
        if(!LbcBedwarsHarm.projectileAllowed(projectile,target))return false;
        if(LbcSpecialFireballs.managed(projectile))return LbcGreed.active(target)&&validRoleTarget(target,false)||game.items().specialFireballs().canAffect(projectile,target);
        if(!game.items().chaosTrapActors().canAffect(projectile,target))return false;
        var source=game.items().spellSources().source(projectile);
        if(source!=null)return LbcGreed.active(target)&&validRoleTarget(target,false)
                ||game.items().spellSources().canAffect(projectile,target);
        if(LbcVexSword.wild(projectile))return validTarget(target)||LbcGreed.active(target)&&validRoleTarget(target,false);
        if(LbcGreed.active(target) && validRoleTarget(target,false)) return true;
        if(LbcHalloweenWind.managed(projectile))return game.items().halloweenWind().canAffect(projectile,target);
        if(LbcBabylon.managed(projectile)) return game.items().babylon().canAffect(projectile,target);
        if(LbcNeutral.managed(projectile)) return game.items().neutral().canAffect(projectile,target);
        if(LbcTransformator.managed(projectile)) return game.items().transformator().canAffect(projectile,target);
        if(LbcPotionSpawnerDrops.managed(projectile)) return game.items().potionSpawnerDrops().canAffect(projectile,target);
        if(LbcCreatureDrops.managed(projectile)) return game.items().creatureDrops().canAffect(projectile,target);
        if(LbcDroneDrops.managed(projectile)) return game.items().droneDrops().canAffect(projectile,target);
        if(LbcClouds.sourceAcidManaged(projectile)) return game.items().clouds().sourceAcidCanAffect(projectile,target);
        if(LbcCageDrops.managed(projectile)) return game.items().cageDrops().canAffect(projectile,target);
        if(LbcBlaze.isKfcSpirit(projectile)) return projectile.getOwner() instanceof ServerPlayer owner
                && !game.isObserver(owner) && validTarget(target);
        if(game.items().stand().isStandProjectile(projectile)) return game.items().stand().canHit(projectile,target);
        if(!game.items().consuming().canTrialProjectileTarget(projectile.getOwner(),target)) return false;
        var summoner=game.items().summons().owner(projectile.getOwner());
        if(summoner==null && projectile.entityTags().contains(LbcSummons.PROJECTILE)
                && projectile.getOwner() instanceof ServerPlayer player) summoner=player;
        if(summoner!=null && !projectileTarget(summoner,target)) return false;
        if(!game.items().teleport().canHit(projectile,target)) return false;
        if(!projectile.entityTags().contains(FIRE_PROJECTILE) && !projectile.entityTags().contains(AIR_PROJECTILE)) {
            var owner=LbcIndirectKills.playerSource(projectile);
            return owner==null || physicalProjectileTarget(projectile,target) || nativeSelfTarget(projectile,target)
                    || game.items().consuming().owner(projectile)==target || projectileTarget(owner,target);
        }
        return projectile.getOwner() instanceof ServerPlayer owner && !game.isObserver(owner)
                && (projectile.entityTags().contains(LbcBlaze.TRIAL_PROJECTILE)
                    ? target==owner || target(owner,target,true) : projectileTarget(owner,target));
    }
    /** Радиус взрыва native firework обходит canHitEntity; союзников защищаем и в финальном уроне. */
    public boolean allowDamage(Entity victim,DamageSource source) {
        if(!LbcBedwarsHarm.damageAllowed(victim,source))return false;
        if(LbcRasengan.blast(source))return validRoleTarget(victim,false)&&!LbcGreed.active(victim);
        if(LbcGreed.active(victim)) return false;
        if(LbcSpecialFireballs.managed(source.getDirectEntity()))return game.items().specialFireballs().canAffect(source.getDirectEntity(),victim);
        if(LbcThrownUtilities.isManaged(source.getDirectEntity()))return validRoleTarget(victim,false);
        if(LbcEdibleSpells.wild(source))return validRoleTarget(victim,false);
        if(LbcFoodLifecycle.managed(source.getDirectEntity()))return game.items().foodLifecycle().canAffect(source.getDirectEntity(),victim);
        if(LbcOreshek.managed(source.getDirectEntity()))return game.items().oreshek().canAffect(source.getDirectEntity(),victim);
        if(LbcNukeEvent.managed(source.getDirectEntity()))return LbcIndirectKills.isWildCaptured(source)
                ?validRoleTarget(victim,false):game.items().nukeEvent().canAffect(source.getDirectEntity(),victim);
        if(LbcChaosTrapActors.managed(source.getDirectEntity()))return game.items().chaosTrapActors().canAffect(source.getDirectEntity(),victim);
        if(game.items().spellSources().wild(source.getDirectEntity()))return game.items().spellSources().canAffect(source.getDirectEntity(),victim);
        var spellTarget=game.items().spellSources().target(LbcIndirectKills.playerSource(source.getEntity()),victim);
        if(spellTarget!=null)return spellTarget;
        if(game.items().ultraponos().isExplosion(source)) return validTarget(victim);
        if(LbcIncomingArtifacts.managed(source.getDirectEntity())) return game.items().incomingArtifacts().canAffect(source.getDirectEntity(),victim);
        if(SauvojenMeditation.managed(source.getDirectEntity())) return validTarget(victim);
        if(source instanceof LbcBabylonEffects.OwnerDamageSource){
            var cause=game.items().indirectKills().babylonCause(source);
            var owner=cause==null?null:game.server().getPlayerList().getPlayer(cause.player());
            return validTarget(victim) && (cause==null||cause.friendlyFire()||owner!=null&&(victim==owner||projectileTarget(owner,victim)));
        }
        if(game.items().babylonEffects().managedSecondary(source.getDirectEntity()))return game.items().babylonEffects().canAffect(source.getDirectEntity(),victim);
        if(LbcMoiseiBoots.managed(source.getDirectEntity()))return game.items().moiseiBoots().canAffect(source.getDirectEntity(),victim);
        if(game.items().quiver().managedSecondary(source.getDirectEntity()))return game.items().quiver().canAffect(source.getDirectEntity(),victim);
        if(LbcVexSword.wild(source.getDirectEntity())) return validTarget(victim);
        if(LbcGodHedgehog.managed(source.getDirectEntity())
                || (LbcHedgehog.managed(source.getDirectEntity()) || source.getDirectEntity() instanceof net.minecraft.world.entity.monster.Silverfish)
                    && LbcIndirectKills.isWildCaptured(source)) return validTarget(victim);
        if(LbcBabylon.managed(source.getDirectEntity())) return game.items().babylon().canAffect((Projectile)source.getDirectEntity(),victim);
        if(LbcNeutral.managed(source.getDirectEntity())) return game.items().neutral().canAffect((Projectile)source.getDirectEntity(),victim);
        if(LbcTransformator.managed(source.getDirectEntity())) return game.items().transformator().canAffect(source.getDirectEntity(),victim);
        if(LbcStalin.managed(source.getDirectEntity())) return game.items().stalin().canAffect(source.getDirectEntity(),victim);
        if(LbcPotionSpawnerDrops.managed(source.getDirectEntity())) return game.items().potionSpawnerDrops().canAffect(source.getDirectEntity(),victim);
        if(LbcCreatureDrops.managed(source.getDirectEntity())) return game.items().creatureDrops().canAffect(source.getDirectEntity(),victim);
        if(LbcDroneDrops.managed(source.getDirectEntity())) return game.items().droneDrops().canAffect(source.getDirectEntity(),victim);
        if(LbcAntDrops.managed(source.getDirectEntity())) return game.items().antDrops().canAffect(source.getDirectEntity(),victim);
        if(LbcClouds.sourceAcidManaged(source.getDirectEntity())) return game.items().clouds().sourceAcidCanAffect(source.getDirectEntity(),victim);
        if(LbcCageDrops.managed(source.getDirectEntity())) return game.items().cageDrops().canAffect(source.getDirectEntity(),victim);
        if(LbcZombieTntDrops.managed(source.getDirectEntity())) return game.items().zombieTntDrops().canAffect(source.getDirectEntity(),victim);
        if(LbcDamage.isNativeLightning(source)) return validTarget(victim);
        if(LbcBlaze.isKfcSpirit(source.getDirectEntity())) return canHit((Projectile)source.getDirectEntity(),victim);
        if(source.getDirectEntity() instanceof Projectile shot && game.items().stand().isStandProjectile(shot))
            return game.items().stand().canHit(shot,victim);
        if(!(source.getDirectEntity() instanceof Projectile
                ? game.items().consuming().canTrialProjectileTarget(source.getEntity(),victim)
                : game.items().consuming().canTrialTarget(source.getEntity(),victim))) return false;
        var friendlySummoner=game.items().summons().owner(source.getEntity());
        if(friendlySummoner==null && source.getDirectEntity()!=null
                && source.getDirectEntity().entityTags().contains(LbcSummons.PROJECTILE))
            friendlySummoner=LbcIndirectKills.playerSource(source.getEntity());
        if(friendlySummoner!=null) return source.getDirectEntity() instanceof Projectile
                ? projectileTarget(friendlySummoner,victim) : target(friendlySummoner,victim);
        var summonedOwner=game.items().blaze().owner(source.getEntity());
        if(summonedOwner!=null) return victim==summonedOwner && source.getEntity().entityTags().contains(LbcBlaze.TRIAL_PROJECTILE)
                || (source.getEntity().entityTags().contains(LbcBlaze.TRIAL_PROJECTILE)
                    ? target(summonedOwner,victim,true) : source.getDirectEntity() instanceof Projectile
                        ? projectileTarget(summonedOwner,victim) : target(summonedOwner,victim));
        if(!(source.getDirectEntity() instanceof Projectile projectile)) return true;
        if(!projectile.entityTags().contains(AIR_PROJECTILE) && !isFireProjectile(projectile)) {
            var owner=LbcIndirectKills.playerSource(projectile);
            return owner==null || physicalProjectileTarget(projectile,victim) || nativeSelfTarget(projectile,victim)
                    || game.items().consuming().owner(projectile)==victim || projectileTarget(owner,victim);
        }
        return projectile.getOwner() instanceof ServerPlayer owner && !game.isObserver(owner)
                && (victim==owner || (projectile.entityTags().contains(LbcBlaze.TRIAL_PROJECTILE)
                    ? target(owner,victim,true) : projectileTarget(owner,victim)));
    }

    int cloneNearby(ServerPlayer holder,int skill,int budget,long deadline) {
        int made=0,count=CloneVirtualAbilityStates.count(skill),bit=CloneVirtualAbilityStates.bit(skill);
        for(var trace:java.util.List.copyOf(electroTraces)){
            if(budget-made<count||System.nanoTime()>=deadline)break;
            if(trace.age>=trace.lifetime||!game.items().currentEpoch(trace.epoch)||trace.trialOwner!=null
                    ||trace.erecto!=null&&LbcAcademy.trial(trace.erecto.stack)
                    ||!CloneVirtualAbilityStates.near(game,holder,trace.level,trace.position,trace.cloneMask,skill)
                    ||timeStopped()||trace.sourcePolicy==null&&trace.owner!=null
                    &&(trace.owner.entityTags().contains("time_stopped")||game.items().timeStop().frozen(trace.owner)))continue;
            var points=CloneVirtualAbilityStates.positions(holder,trace.level,trace.position,skill);if(points==null)continue;
            var clones=new java.util.ArrayList<ElectroTrace>();
            for(var point:points){var copy=new ElectroTrace(trace);copy.position=point;copy.cloneMask|=bit;
                if(skill==17){var direction=LbcCloneSkills.spreadMotion(Vec3.directionFromRotation(copy.pitch,copy.yaw),holder,1000,1500);
                    copy.yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
                    copy.pitch=(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));}
                clones.add(copy);
            }
            electroTraces.addAll(clones);trace.cloneMask|=bit;made+=count;
            if(skill==17){var direction=LbcCloneSkills.spreadMotion(Vec3.directionFromRotation(trace.pitch,trace.yaw),holder,10,10);
                trace.yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
                trace.pitch=(float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));}
        }
        return made;
    }


    void planetoid(ServerPlayer holder) {
        for(var trace:electroTraces)if(trace.age<trace.lifetime&&game.items().currentEpoch(trace.epoch)
                &&LbcPlanetoidSkill.near(game,holder,trace.level,trace.position)){
            var aim=LbcPlanetoidSkill.direction(holder,trace.position,82);
            trace.yaw=(float)Math.toDegrees(Math.atan2(-aim.x,aim.z));
            trace.pitch=(float)-Math.toDegrees(Math.atan2(aim.y,aim.horizontalDistance()));
        }
    }

}
