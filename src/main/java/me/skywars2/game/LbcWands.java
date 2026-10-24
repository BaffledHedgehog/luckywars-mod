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
    public static final Item TINY_ELECTRO=register("skywars2:tiny_electro_wand","wand_electro_tiny","lbc:st/electro_tiny");
    public static final Item ELECTRO=register("skywars2:electro_wand","sauvojen_wand_electro","lbc:st/electro");
    public static final Item ACADEMY_ELECTRO=register("skywars2:academy_electro_wand","sauvojen_wand_electro","lbc:st/electro");
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
        REGULAR_ACID(LbcWands.ACID_WAND,"stacid","stacid_wand_mana_use",250),
        ELECTRO(TINY_ELECTRO,"stelectro_tiny","tiny_electro_wand_mana_use",100),
        ACADEMY_ELECTRO(LbcWands.ACADEMY_ELECTRO,"stelectro","stelectro_wand_mana_use",400),
        REGULAR_ELECTRO(LbcWands.ELECTRO,"stelectro","stelectro_wand_mana_use",400),
        ACADEMY_EXASPERATO(LbcWands.ACADEMY_EXASPERATO,"exasperato_wand","exasperato_wand_mana_use",1500),
        ACADEMY_EXOPERSO(LbcWands.ACADEMY_EXOPERSO,"exoperso","megumin_staff_mana_use",5000),
        REGULAR_EXOPERSO(LbcWands.EXOPERSO,"exoperso","megumin_staff_mana_use",5000),
        ACADEMY_BEDROCK(LbcWands.ACADEMY_BEDROCK,"bedrock_wand","bedrock_wand_mana_use",2500),
        REGULAR_BEDROCK(LbcWands.BEDROCK,"bedrock_wand","bedrock_wand_mana_use",2500),
        ACADEMY_WALL(LbcWands.ACADEMY_WALL,"wall","wall_wand_mana_use",500),
        REGULAR_WALL(LbcWands.WALL,"wall","wall_wand_mana_use",1500),
        ACADEMY_STONEKIVI(LbcWands.ACADEMY_STONEKIVI,"stonekivi_wand","stonekivi_wand_mana_use",2500),
        REGULAR_STONEKIVI(LbcWands.STONEKIVI,"stonekivi_wand","stonekivi_wand_mana_use",2500),
        EXASPERATO(LbcWands.EXASPERATO,"exasperato_wand","exasperato_wand_mana_use",1500);
        final Item item;
        final CompoundTag flag;
        final ScoreHolder cost;
        final int defaultCost;
        Spell(Item item,String flag,String cost,int defaultCost) {
            this.item=item; this.flag=flag(flag); this.cost=ScoreHolder.forNameOnly(cost); this.defaultCost=defaultCost;
            if(item==LbcWands.ACADEMY_ICE||item==LbcWands.ACADEMY_CORRUPTION||item==LbcWands.ACADEMY_ACID_CORRUPTION||item==LbcWands.ACADEMY_ERECTO||item==LbcWands.ACADEMY_TESLA||item==LbcWands.ACADEMY_PRISM||item==LbcWands.ACADEMY_GAYCASTER||item==LbcWands.ACADEMY_AWAKENED_ACID) this.flag.putInt("upgraded",1);
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
            if(item==LbcWands.ACADEMY_EXOPERSO || item==LbcWands.ACADEMY_RUBY || item==LbcWands.ACADEMY_TELEPORT || item==LbcWands.ACADEMY_AIR || item==LbcWands.ACADEMY_BLIND || item==LbcWands.ACADEMY_STONE || item==LbcWands.ACADEMY_FIRE || item==LbcWands.ACADEMY_TERRA || item==LbcWands.ACADEMY_ELECTRO || item==LbcWands.ACADEMY_EXASPERATO)
                this.flag.putInt("upgraded",1);
        }
    }
    private static final Spell[] SPELLS=Spell.values();
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
        final ServerLevel level;
        final String epoch;
        final int lifetime;
        final boolean meditation;
        boolean mechanism,teslaChain;
        LbcErecto.Path erecto;
        ServerPlayer trialOwner;
        InteractionHand trialHand;
        ItemStack trialStack;
        Vec3 position;
        float yaw,pitch;
        int age;
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
        LivingEntity source() { return owner==null?npcSource:owner; }
    }
    private final GameController game;
    private final LbcMana mana;
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
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(properties,Items.CARROT_ON_A_STICK,true) {
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
                var game=SkyWars2.game();
                return game!=null && player instanceof ServerPlayer p && game.items().wands().use(p,hand)
                        ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            }
        });
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
        this.game=game; this.mana=mana;
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
    private boolean cast(ServerPlayer player,InteractionHand hand) {
        if(game.items().actionsBlocked(player)) return false;
        var stack=player.getItemInHand(hand);
        if(stack.is(FEATHER_GREED)||stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr("feather_greed",0)==1) return game.items().neutral().feather(player);
        if (LbcHomeItems.isMirror(stack)) return game.items().homes().use(player, hand);
        // Общий dispatch двух рук: хвоемёт платит боеприпасами и не проходит проверку маны жезлов.
        if (LbcShooters.isPine(stack) || LbcShooters.isSlr(stack) || LbcShooters.isKfcGun(stack)
                || LbcShooters.isBossHelicopter(stack) || LbcShooters.isPhantomKnives(stack) || LbcShooters.isWoodificator(stack) || LbcShooters.isHelicopter(stack))
            return game.items().shooters().use(player, hand);
        if (LbcGovnomet.isGovnomet(stack)) return game.items().govnomet().use(player,hand);
        if (LbcMagnum.isMagnum(stack)) return game.items().magnum().use(player,hand);
        if (LbcAk47.isAk47(stack)) return game.items().ak47().use(player,hand);
        if (LbcLaserMinigun.isMinigun(stack)) return game.items().laserMinigun().use(player,hand);
        if (LbcHohlopitek.isItem(stack)) return game.items().hohlopitek().use(player,hand);
        if (LbcStand.isOraora(stack) || LbcStand.isClocks(stack) || LbcStand.isHomingLightning(stack))
            return game.items().stand().use(player,hand);
        if (LbcMobgun.isMobgun(stack)) return game.items().mobgun().use(player,hand);
        if(LbcReactiveItems.isReactive(stack) || LbcReactiveItems.isWall(stack)) return game.items().reactive().use(player,hand);
        if (LbcKfcRewards.isCultus(stack)) return game.items().kfcRewards().useCultus(player);
        if (LbcElectro.isAnyKatana(stack)) return game.items().electro().use(player,hand);
        if (LbcVexSword.isSword(stack)) return game.items().vexSword().use(player,hand);
        var scrollData=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if(isDivider(stack)) return game.items().duplicating().useDivider(player,hand,stack);
        if(stack.is(DESTROYER_SPELL) || scrollData.getIntOr("trap2",0)==1)
            return game.items().consuming().useScroll(player,hand,stack,true);
        if(stack.is(CREEPER_ARMY_SPELL) || scrollData.getIntOr("creeperz",0)==1)
            return game.items().consuming().useScroll(player,hand,stack,false);
        Spell spell=null;
        for(var candidate:SPELLS) if(matches(stack,candidate.item,candidate.flag)) { spell=candidate; break; }
        if(spell==null) return false;
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
            int mode=Math.floorMod(data.getIntOr("mode",1),4)+1;
            data.putInt("mode",mode); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            player.sendSystemMessage(Component.translatable(switch(mode) {
                case 2 -> "mode_earth";
                case 3 -> "mode_sun";
                case 4 -> "mode_comets";
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
            int mode=Math.floorMod(data.getIntOr("mode",1),3)+1;
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
            int mode=Math.floorMod(data.getIntOr("mode",1),3)+1;
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
            int mode=Math.floorMod(data.getIntOr("mode",1),3)+1;
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
            int mode=Math.floorMod(data.getIntOr("mode",1),4)+1;
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
            int mode=Math.floorMod(data.getIntOr("mode",1),4)+1;
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
            int mode=Math.floorMod(data.getIntOr("mode",1),3)+1;
            data.putInt("mode",mode); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            player.sendSystemMessage(Component.translatable(mode==1 ? "mode_default" : mode==2 ? "mode_big" : "mode_concentrated"));
            return true;
        }
        if(spell==Spell.ACADEMY_EXASPERATO && player.isShiftKeyDown()) {
            if(!mana.visible(player) || player.entityTags().contains("time_stopped")
                    || game.items().magic().suppressed(player) || game.items().frost().isFrozen(player)) return false;
            var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
            int mode=Math.floorMod(data.getIntOr("mode",1),4)+1;
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
            int mode=data.getIntOr("mode",1)==2 ? 1 : 2;
            data.putInt("mode",mode);
            stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            player.sendSystemMessage(Component.translatable(mode==1 ? "mode_default" : "mode_ray"));
            return true;
        }
        // Улучшенный Wall в исходнике имеет собственную фиксированную цену 500.
        int requiredMana=spell==Spell.ACADEMY_WALL?500:mana.score(spell.cost,"lbc.math").get();
        int spentMana=spell==Spell.REGULAR_EXOPERSO?25000:spell==Spell.ACADEMY_EXOPERSO?10000:requiredMana;
        if(!mana.trySpendWand(player,requiredMana,spentMana)) return false;
        switch(spell) {
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
    void wandCasted(ServerPlayer player) {
        if(mana.mageFullSet(player)) mana.maximum(player,mana.maximum(player)+mana.score(MAGE_BONUS,"lbc.math").get());
        game.items().trinkets().castWand(player);
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
    private void fireVolley(ServerPlayer player) {
        smallFire(player,0); smallFire(player,.15); smallFire(player,-.15);
        LbcOverload.sound(player,player.getEyePosition(),"minecraft:item.firecharge.use",1,1,0);
    }
    private void bigFire(ServerPlayer player) {
        var projectile=new LargeFireball(player.level(),player,player.getLookAngle(),2);
        launchFire(player,projectile,player.getLookAngle().scale(2.5));
        LbcOverload.sound(player,player.getEyePosition(),"minecraft:item.firecharge.use",1,.5f,0);
    }
    private void launchFire(ServerPlayer player,Projectile projectile,Vec3 velocity) {
        launchFire(player,projectile,player.getEyePosition().add(player.getLookAngle()),velocity);
    }
    /** Общий native spawn для Fire и Blaze: owner, guards, эпоха и cleanup одинаковы. */
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
        var wall=LbcTerra.wallHit(owner.level(),from,to);
        Vec3 end=wall==null ? to : wall.getLocation();
        var entity=ProjectileUtil.getEntityHitResult(owner.level(),owner,from,end,new AABB(from,end).inflate(1),
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
    private void concentratedFire(ServerPlayer player) {
        Vec3 from=player.getEyePosition(),to=from.add(player.getLookAngle().scale(100));
        var level=player.level();
        var hit=firstHit(player,from,to);
        boolean terminal=hit!=null && (!(hit instanceof net.minecraft.world.phys.BlockHitResult wall) || level.hasChunkAt(wall.getBlockPos()));
        if(hit!=null) to=hit.getLocation();
        double distance=from.distanceTo(to);
        for(double step=0;step<=distance;step+=.5) {
            var point=from.add(player.getLookAngle().scale(step));
            level.sendParticles(ParticleTypes.FALLING_LAVA,true,false,point.x,point.y,point.z,1,.05,.05,.05,0);
            LbcOverload.sound(player,point,"minecraft:block.lava.ambient",1,.1f,0);
        }
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
        BlockPos previous=null;
        for(int i=0;i<=100;i++) {
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
        var level=player.level();
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
        BlockPos wall=null;
        for(int i=0;i<=100;i++) {
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
        for(var hit:hits) if(projectileTarget(player,hit.getEntity())) {
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
        for(int i=0;i<=100;i++) {
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
        if(hit!=null || wall) {
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
        electroTraces.add(new ElectroTrace(source,origin,direction,game.items().epoch()));
    }
    /** Виртуальные разряды участвуют в том же отражении, что нативные снаряды. Возраст сохраняется. */
    void reflectElectro(ServerPlayer player) {
        for(var trace:electroTraces) if(trace.level==player.level() && trace.position.distanceToSqr(player.position())<=49) {
            trace.owner=player; trace.yaw+=180; trace.pitch=-trace.pitch;
            trace.level.sendParticles(ParticleTypes.ELECTRIC_SPARK,trace.position.x,trace.position.y,trace.position.z,5,0,0,0,.3);
        }
    }
    /** Аксессуар присваивает только вражеские/нейтральные разряды; свои и союзные не разворачивает. */
    void reflectElectroblocker(ServerPlayer player) {
        for(var trace:electroTraces) if(trace.level==player.level() && trace.owner!=player
                && !player.isAlliedTo(trace.source()) && trace.position.distanceToSqr(player.position())<=196) {
            trace.owner=player; trace.yaw+=180; trace.pitch=-trace.pitch;
            trace.level.sendParticles(ParticleTypes.ELECTRIC_SPARK,trace.position.x,trace.position.y,trace.position.z,10,0,0,0,.3);
        }
    }
    /** Три разряда расходятся случайным поворотом при полёте; сроки исходника различаются. */
    private void electroVolley(ServerPlayer owner) {
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
    /** The same source four short arcs also belong to a magic staff, without mechanism suppression. */
    void shortElectro(ServerPlayer owner,Vec3 origin,Vec3 target,boolean mechanism) {
        Vec3 direction=target.subtract(origin);
        float yaw=direction.horizontalDistanceSqr()<1e-12 ? owner.getYRot()
                : (float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
        float pitch=direction.lengthSqr()<1e-12 ? owner.getXRot()
                : (float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance()));
        for(int i=0;i<4;i++) {
            var trace=new ElectroTrace(owner,origin,game.items().epoch(),5+i*2);
            trace.yaw=yaw; trace.pitch=pitch; trace.mechanism=mechanism;
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
    private void electroRay(ServerPlayer owner) {
        var level=owner.level();
        Vec3 from=owner.getEyePosition(),direction=owner.getLookAngle(),end=from.add(direction.scale(100));
        var wall=LbcTerra.wallHit(level,from,end);
        if(wall!=null) end=wall.getLocation();
        var shield=shieldHit(owner,from,end);
        if(shield!=null) {
            end=shield.getLocation();
            LbcOverload.sound(owner,shield.getEntity().position(),"minecraft:item.shield.block",1,1,0);
        }
        double distance=from.distanceTo(end);
        for(int i=0;i<=1000 && i*.1<=distance;i++) if(owner.getRandom().nextFloat()<.35f) {
            Vec3 point=from.add(direction.scale(i*.1));
            level.sendParticles(ELECTRO_PARTICLE,true,false,point.x,point.y,point.z,1,.1,.1,.1,0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,point.x,point.y,point.z,1,.1,.1,.1,1);
            LbcOverload.sound(owner,point,"minecraft:entity.firework_rocket.twinkle",.03f,2,0);
        }
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

    void higherElectro(ElectroTrace trace){electroTraces.add(trace);}
    java.util.List<ElectroTrace> electroSnapshot() { return java.util.List.copyOf(electroTraces); }
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
    private boolean legacyTimeStopped() {
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
                var owner=trace.owner;
                if(trace.age>=trace.lifetime || !game.items().currentEpoch(trace.epoch) || trace.level!=game.maps().level()
                        || trace.source()==null || trace.source().level()!=trace.level
                        || owner!=null && (!owner.isAlive() || owner.isRemoved() || !game.canUseGameplayItems(owner))
                        || game.stage()==GameController.Stage.ENDING) {
                    electroTraces.remove(trace); continue;
                }
                if(timeStopped || owner!=null && owner.entityTags().contains("time_stopped")) continue;
                // (user) Мана/антимагия проверяются при использовании жезла; выпущенная дуга немагическая.
                if(!trace.level.hasChunkAt(BlockPos.containing(trace.position))
                        || (trace.erecto!=null ? !game.items().erecto().step(trace) : trace.teslaChain&&trace.age%5!=0 ? false : !electroStep(trace)) || ++trace.age>=trace.lifetime) electroTraces.remove(trace);
            }
        } finally { electroBatch.clear(); }
    }
    private boolean electroStep(ElectroTrace trace) {
        var level=trace.level;
        Vec3 from=trace.position,direction=Vec3.directionFromRotation(trace.pitch,trace.yaw);
        int samples=trace.teslaChain?20:trace.meditation || trace.npcSource!=null ? 18 : 10;
        var shield=shieldHit(trace.source(),from,from.add(direction.scale(samples*.2)),trace.meditation);
        double shieldDistance=shield==null ? Double.POSITIVE_INFINITY : from.distanceTo(shield.getLocation());
        double mechanismFraction=trace.mechanism ? game.items().magic().mechanismSuppressionFraction(
                level,from,from.add(direction.scale(samples*.2))) : Double.POSITIVE_INFINITY;
        double mechanismDistance=mechanismFraction*samples*.2;
        double stopDistance=Math.min(shieldDistance,mechanismDistance);
        Vec3 end=from,conductor=null;
        boolean clear=true;
        for(int i=0;i<=samples && i*.2<stopDistance-1e-7;i++) {
            Vec3 point=from.add(direction.scale(i*.2));
            var pos=BlockPos.containing(point);
            if(!level.hasChunkAt(pos)) { clear=false; break; }
            end=point;
            level.sendParticles(ELECTRO_PARTICLE,true,false,point.x,point.y,point.z,1,.1,.1,.1,0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,point.x,point.y,point.z,1,.1,.1,.1,1);
            electroSound(trace,point,"minecraft:entity.firework_rocket.twinkle",.03f,2);
            var block=level.getBlockState(pos);
            if(block.is(CONDUCTIVE)) {
                if(conductor==null) conductor=point;
                Vec3 sparks=point.subtract(direction.scale(.5));
                level.sendParticles(ParticleTypes.END_ROD,sparks.x,sparks.y,sparks.z,5,.1,.1,.1,.1);
            } else if(!block.is(AIRS)) {
                end=new AABB(pos).clip(from,point).orElse(from); clear=false; break;
            }
        }
        // (user) Щит обрезает путь на границе сферы, не отменяя эффекты уже пройденной части.
        if(clear && Double.isFinite(mechanismDistance) && mechanismDistance<=shieldDistance) {
            end=from.add(direction.scale(mechanismDistance)); clear=false;
        } else if(clear && shield!=null) {
            end=shield.getLocation(); clear=false;
            electroSound(trace,shield.getEntity().position(),"minecraft:item.shield.block",1,1);
        }
        if(from.distanceToSqr(end)>1e-12) electroHits(trace,from,end);
        if(conductor!=null) electroConduction(trace,conductor);
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
        if(trace.owner!=null) LbcOverload.sound(trace.owner,at,id,volume,pitch,0);
        else trace.level.playSound(null,at.x,at.y,at.z,net.minecraft.sounds.SoundEvent.createVariableRangeEvent(
                Identifier.parse(id)),net.minecraft.sounds.SoundSource.MASTER,volume,pitch);
    }
    private EntityHitResult shieldHit(LivingEntity owner,Vec3 from,Vec3 to,boolean meditation) {
        Vec3 delta=to.subtract(from);
        double lengthSquared=delta.lengthSqr();
        EntityHitResult nearest=null;
        double nearestT=1;
        for(var player:((ServerLevel)owner.level()).getEntitiesOfClass(ServerPlayer.class,new AABB(from,to).inflate(4),
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
        electroHits(trace,from,to,.2,.5f,trace.meditation || trace.npcSource!=null ? Integer.MAX_VALUE : 2);
    }
    private void electroHits(ElectroTrace trace,Vec3 from,Vec3 to,double spacing,float amount,int targetLimit) {
        var damage=new DamageSource(trace.level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(trace.meditation ? LbcDamage.ELECTRO_MEDITATION : LbcDamage.ELECTRO_BOLT),trace.source());
        LbcDamage.hurtSegment(trace.level,damage,from,to,Vec3.ZERO,spacing,amount,targetLimit,
                entity->entity.getType().builtInRegistryHolder().is(MOBS)
                        && electroTarget(trace.source(),entity,trace.meditation),trace.meditation);
    }
    private boolean electroTarget(LivingEntity owner,Entity entity,boolean meditation) {
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
        var damage=new DamageSource(trace.level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(LbcDamage.ELECTRO_CONDUCTION),trace.source());
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
        var candidates=player.level().getEntities((Entity)null,rayBounds(origin,step),e->projectileTarget(player,e));
        ParticleOptions particle=switch(spell) {
            case BLIND -> ParticleTypes.SQUID_INK;
            case AIR -> ParticleTypes.FIREWORK;
            case STONE -> new BlockParticleOption(ParticleTypes.BLOCK,Blocks.STONE.defaultBlockState());
            case TELEPORT -> ParticleTypes.CLOUD;
            case ACID -> ParticleTypes.SNEEZE;
            default -> throw new IllegalArgumentException("Not a ray spell: "+spell);
        };
        for(int i=0;i<=100;i++) {
            Vec3 point=origin.add(step.scale(i));
            if(spell!=Spell.BLIND || player.getRandom().nextFloat()<.3f)
                player.level().sendParticles(particle,true,false,point.x,point.y,point.z,1,0,0,0,0);
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
                        if(entity.addEffect(new MobEffectInstance(MobEffects.LEVITATION,20,30),player))
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
            if(hit instanceof LivingEntity living) living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,80,0));
            hit.hurtServer(player.level(),new DamageSource(player.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                    .getOrThrow(DamageTypes.ARROW),player),.25f);
        }
        LbcOverload.sound(player,point,"minecraft:entity.squid.squirt",1,2,0);
    }
    /** Projectile impacts protect allied players; friendly mobs remain physical damage targets. */
    public boolean projectileTarget(ServerPlayer player,Entity entity) { return projectileTarget(player,entity,false); }
    /** Accepted lethal hits still trigger their sword; role/team exclusions remain authoritative. */
    boolean projectileTarget(ServerPlayer player,Entity entity,boolean allowDying) {
        return entity!=player && validTarget(entity,allowDying)
                && (!(entity instanceof ServerPlayer) || !player.isAlliedTo(entity));
    }
    public boolean target(ServerPlayer player,Entity entity) {
        return target(player,entity,false);
    }
    /** Узкое исключение притягивания Ruby: роли остаются защищены, союзников можно включить отдельно. */
    boolean target(ServerPlayer player,Entity entity,boolean includeAllies) {
        return entity!=player && validTarget(entity)
                && (includeAllies || !player.isAlliedTo(entity) && !game.items().blaze().friendly(player,entity) && !game.items().summons().friendly(player,entity));
    }
    boolean validTarget(Entity entity) { return validTarget(entity,false); }
    boolean validTarget(Entity entity,boolean allowDying) {
        return (allowDying || entity.isAlive()) && !entity.isRemoved() && !entity.isSpectator() && !entity.entityTags().contains("spectator")
                && !entity.entityTags().contains("rcdenyalways") && !entity.entityTags().contains("sauvojen_tuntija")
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
            return owner==null || nativeSelfTarget(projectile,target)
                    || game.items().consuming().owner(projectile)==target || projectileTarget(owner,target);
        }
        return projectile.getOwner() instanceof ServerPlayer owner && !game.isObserver(owner)
                && (projectile.entityTags().contains(LbcBlaze.TRIAL_PROJECTILE)
                    ? target==owner || target(owner,target,true) : projectileTarget(owner,target));
    }
    /** Радиус взрыва native firework обходит canHitEntity; союзников защищаем и в финальном уроне. */
    public boolean allowDamage(Entity victim,DamageSource source) {
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
            return owner==null || nativeSelfTarget(projectile,victim)
                    || game.items().consuming().owner(projectile)==victim || projectileTarget(owner,victim);
        }
        return projectile.getOwner() instanceof ServerPlayer owner && !game.isObserver(owner)
                && (victim==owner || (projectile.entityTags().contains(LbcBlaze.TRIAL_PROJECTILE)
                    ? target(owner,victim,true) : projectileTarget(owner,victim)));
    }
}
