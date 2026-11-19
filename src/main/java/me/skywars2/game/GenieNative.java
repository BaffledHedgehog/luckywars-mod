package me.skywars2.game;

import me.skywars2.SkyWars2;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.skywars2.game.genie.GenieRuntime;
import me.skywars2.game.genie.GenieScriptItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Narrow bridge to existing package-owned policies, keeping the new feature in game.genie. */
public final class GenieNative {
    private GenieNative(){}
    public static void acceptedThrow(LivingEntity thrower, InteractionHand hand, ItemStack physical, ItemStack projectileStack,
                                     Projectile projectile) {
        if(!GenieRuntime.matches(projectileStack)||!(thrower instanceof ServerPlayer player))return;
        var game=SkyWars2.game();
        if (game == null) return;
        var accepted = capturePublishedThrow(game, player, hand, physical, projectileStack, List.of(projectile));
        if (accepted != null) LbcTmtShotCompletion.afterNativeAction(accepted);
    }

    /** Capture the final admitted bodies before debit. Dispatch the returned work after the use completes. */
    static Runnable capturePublishedThrow(GameController game, ServerPlayer owner, InteractionHand hand,
                                          ItemStack physical, ItemStack source, List<? extends Entity> bodies) {
        if (!GenieRuntime.matches(source)) return null;
        var published = bodies.stream().filter(body -> !body.isRemoved() && GenieCombat.ownedLaunch(body, owner)
                && owner.level().getEntity(body.getUUID()) == body).toList();
        if (published.isEmpty()) return null;

        GenieCombat.launched(game, owner, hand, physical, source, published);
        if (!GenieScriptItem.matches(source)) return null;
        return game.items().genie().runtime().scripts().captureThrow(owner, hand, physical, source, published);
    }
    public static boolean stopped(GameController game,ServerPlayer player){return game.items().wands().inputTimeStopped(player);}
    public static boolean enemy(GameController game,ServerPlayer player,Entity entity){return game.items().wands().projectileTarget(player,entity,true);}
    public static java.util.Map<Identifier,Integer> grimoires(){return LbcLibrarySpells.scriptSpells();}
    public static boolean grimoire(GameController game,ServerPlayer player,Identifier id,InteractionHand hand){
        Integer spell=grimoires().get(id);
        if(spell==null)throw new IllegalArgumentException("unknown native grimoire");
        return LbcLibrarySpells.admitted(game,player,hand)&&LbcLibrarySpells.cast(game,player,hand,spell);
    }
    private enum Cast {
        ELECTRO_ARC, ELECTRO_VOLLEY, ELECTRO_RAY, DANMAKU_CARD, DANMAKU_HOMING_CARD,
        DANMAKU_VOLLEY, DANMAKU_OFFHAND_VOLLEY, ACADEMY_DANMAKU_VOLLEY, ACADEMY_DANMAKU_OFFHAND_VOLLEY,
        POLYMORPH_RAY, ACADEMY_POLYMORPH_RAY, UNSTABLE_PRISM_CHARGE, ACADEMY_PRISM_RAY,
        FALLING_STARS, ACADEMY_FALLING_STARS, FALLING_STARS_BIG, FALLING_STARS_FIRE_RAIN,
        WALL, ACADEMY_WALL, WALL_HORIZONTAL, WALL_DISMANTLE, WALL_STAIRS,
        ADVANCED_ENDER, ACADEMY_ADVANCED_ENDER, ADVANCED_ENDER_TOP, ADVANCED_ENDER_CLIP,
        SNOW_WAND, ACADEMY_SNOW_WAND, ICE_WAND, ACADEMY_ICE_WAND,
        MUSIC_STAFF, ACADEMY_MUSIC_STAFF,
        RUBY_WAND, ACADEMY_RUBY_WAND, RUBY_COMPRESSION, RUBY_CUTTING_RAY,
        STONE_WAND, ACADEMY_STONE_WAND, STONE_SHIELD, STONE_BRIDGE, STONE_SPIKES,
        FARTUS_WAND, FARTUS_BANANA_VOLLEY, FARTUS_LABEL_VOLLEY, ACADEMY_FARTUS_WAND,
        AWAKENED_ACID_WAND(1,false), AWAKENED_ACID_WAND_IGNITED(1,true),
        AWAKENED_ACID_CAUSTIC(2,false), AWAKENED_ACID_CAUSTIC_IGNITED(2,true),
        AWAKENED_ACID_GATLING(3,false), AWAKENED_ACID_GATLING_IGNITED(3,true),
        AWAKENED_ACID_RIFLE(4,false), AWAKENED_ACID_RIFLE_IGNITED(4,true),
        TERRA_CHARGE(LbcTerra.Kind.REGULAR), ACADEMY_TERRA_CHARGE(LbcTerra.Kind.ACADEMY),
        TERRA_SWORD_CHARGE(LbcTerra.Kind.SWORD), TRUE_TERRA_CHARGE(LbcTerra.Kind.TRUE_CHARGE),
        TRUE_TERRA_BEAM(LbcTerra.Kind.TRUE_BEAM), TERRA_RAZOR(LbcTerra.Kind.RAZOR);

        final LbcTerra.Kind terra;
        final int acidMode;
        final boolean ignited;

        Cast() { this(null); }
        Cast(LbcTerra.Kind terra) { this(terra,0,false); }
        Cast(int acidMode,boolean ignited) { this(null,acidMode,ignited); }
        Cast(LbcTerra.Kind terra,int acidMode,boolean ignited) {
            this.terra=terra;this.acidMode=acidMode;this.ignited=ignited;
        }
    }
    private static final Map<Identifier, Cast> CASTS = java.util.Arrays.stream(Cast.values())
            .collect(java.util.stream.Collectors.toUnmodifiableMap(
                    spell -> Identifier.parse("skywars2:" + spell.name().toLowerCase(java.util.Locale.ROOT)), spell -> spell));
    public enum TargetedCast {
        ORESHNIK("oreshnik", "Absolute Cinema: Putin / Oreshnik", "Абсолютное кино: Путин / Орешник",
                "Launches the original rocket and its cluster at target."),
        EXOPERSO("exoperso", "Crimson Demon Clan Staff", "Жезл клана Алых Мазоку",
                "Locks the target for the original Exoperso ritual, thirty runes and a crater after 340 ticks."),
        ACADEMY_EXOPERSO("academy_exoperso", "Academy Crimson Demon Clan Staff", "Улучшенный жезл клана Алых Мазоку",
                "Locks the target for the faster Academy Exoperso ritual and a crater after 180 ticks."),
        SUCCUBUS_BLACK_HOLE("succubus_black_hole", "Polymorph: Succubus Black Hole", "Полиморф: Чёрная дыра суккуба",
                "Creates the original traveling Polymorph black hole at the target."),
        VOID_ARCANUM("void_arcanum", 1),
        VOID_ARCANUM_LEVEL_2("void_arcanum_level_2", 2),
        VOID_ARCANUM_LEVEL_3("void_arcanum_level_3", 3),
        VOID_ARCANUM_LEVEL_4("void_arcanum_level_4", 4),
        VOID_ARCANUM_LEVEL_5("void_arcanum_level_5", 5),
        PSYCHO_AREA("psycho_area", "Psycho Wand", "Психожезл",
                "Runs the original random Psycho Wand effect within 30 blocks."),
        PSYCHO_SOUND("psycho_sound", "Psycho Wand: Sound", "Психожезл: Звук",
                "Plays the native phantom sounds to nearby enemy players."),
        PSYCHO_SCREAMER("psycho_screamer", "Psycho Wand: Screamer", "Психожезл: Скример",
                "Applies the native screamer to nearby enemies."),
        PSYCHO_LOOK_BACK("psycho_look_back", "Psycho Wand: Look Back", "Психожезл: Обернись",
                "Applies the native nine-tick compulsive turn to nearby enemies."),
        PSYCHO_SCARE("psycho_scare", "Psycho Wand: Scare", "Психожезл: Испуг",
                "Applies the original five-second fear to nearby enemies."),
        HEDGEHOG("hedgehog", "Ordinary Hedgehog", "Обычный ёж", LbcHedgehog.Kind.ORDINARY),
        HEDGEHOG_STANDARD("hedgehog_standard", "Standard Hedgehog", "Стандартный ёж", LbcHedgehog.Kind.DEFAULT),
        HEDGEHOG_HEALER("hedgehog_healer", "Healing Hedgehog", "Исцеляющий ёж", LbcHedgehog.Kind.HEALER),
        HEDGEHOG_TANK("hedgehog_tank", "Heavy Hedgehog", "Тяжёлый ёж", LbcHedgehog.Kind.TANK),
        HEDGEHOG_SNIPER("hedgehog_sniper", "Sniper Hedgehog", "Ёж-снайпер", LbcHedgehog.Kind.SNIPER),
        HEDGEHOG_LASER("hedgehog_laser", "Laser Hedgehog", "Лазерный ёж", LbcHedgehog.Kind.LASER),
        HEDGEHOG_WALL("hedgehog_wall", "Hedgehog Wall", "Ежовая стена",
                "Summons the original Hedgehog Wall towers, oriented along the player's aim."),
        GOHEI_SPELLCARD("gohei_spellcard", "Spirit Sign: Fantasy Seal", "Знак духа: Печать фантазии",
                "Expanding red damaging sphere, equivalent to 300 Gohei charge."),
        GOHEI_SPELLCARD_FULL("gohei_spellcard_full", "Fully charged Spirit Sign: Fantasy Seal", "Знак духа: Печать фантазии, полный заряд",
                "Expanding red damaging sphere, equivalent to 1500 Gohei charge."),
        ICE_WAND_DETONATE("ice_wand_detonate", "Permafrost: Detonate Crystals", "Вечная Мерзлота: Подрыв кристаллов",
                "Detonates your Permafrost crystals within 50 blocks of the target."),
        ACADEMY_ICE_WAND_SHATTER("academy_ice_wand_shatter", "Academy Permafrost: Shatter", "Вечная Мерзлота (Академия): Раскалывание",
                "Detonates your crystals and shatters frozen entities within 50 blocks. Shatter can hurt allies."),
        FIREBLAZE_WAND("fireblaze_wand", "Fireblaze Staff", "Акфер Уасан Сахриатан Била'умен Фи Лалами",
                "Sends a native small fireball toward each enemy within 30 blocks of the target position."),
        ACADEMY_FIREBLAZE_WORLD("academy_fireblaze_world", "Fireblaze: Infinite Range", "Акфер Уасан Сахриатан: Бесконечная дальность",
                "Sends a small fireball toward each eligible enemy in the arena."),
        ACADEMY_FIREBLAZE_MEGA("academy_fireblaze_mega", "Fireblaze: Powerful Explosions", "Акфер Уасан Сахриатан: Мощные взрывы",
                "Sends an explosive fireball of power five toward each enemy within 100 blocks of the target position."),
        ACADEMY_FIREBLAZE_PILLARS("academy_fireblaze_pillars", "Fireblaze: Lava Pillars", "Акфер Уасан Сахриатан: Лавовые колонны",
                "Creates the original lava columns near enemies in the arena and gives the caster fire resistance."),
        ACADEMY_FIREBLAZE_KIUAS("academy_fireblaze_kiuas", "Fireblaze: World Inferno", "Акфер Уасан Сахриатан: Всемирный Холокост",
                "Ignites the original regions around enemies and places lava. Gives the caster fire resistance."),
        HAKUREI_BOMB("hakurei_bomb", "Hakurei Shrine: Fantasy Seal bomb", "Храм Хакурей: бомба Печать фантазии",
                "Original faster yellow Fantasy Seal bomb with its normal 300 charge and lingering shell.");

        public final Identifier id;
        public final List<String> names;
        public final String effect;
        private final LbcHedgehog.Kind helper;
        private final int arcanumLevel;

        TargetedCast(String path, String english, String russian, String effect) {
            this(path, english, russian, effect, null, 0);
        }

        TargetedCast(String path, int level) {
            this(path, "Eye of Eternal Darkness, level " + level, "Око вечной тьмы, уровень " + level,
                    "Creates a native Void Arcanum black hole of level " + level
                            + ". The optional third argument is a positive lifetime in active ticks (default 200).",
                    null, level);
        }

        TargetedCast(String path, String english, String russian, LbcHedgehog.Kind helper) {
            this(path, english, russian, "Summons one native allied hedgehog. Target is its unobstructed feet position.", helper, 0);
        }

        TargetedCast(String path, String english, String russian, String effect, LbcHedgehog.Kind helper, int arcanumLevel) {
            this.id = Identifier.parse("skywars2:" + path);
            this.names = List.of(english, russian);
            this.effect = effect;
            this.helper = helper;
            this.arcanumLevel = arcanumLevel;
        }
    }

    public static final Identifier ORESHNIK = TargetedCast.ORESHNIK.id;
    private static final Map<Identifier, TargetedCast> TARGETED_CASTS = java.util.Arrays.stream(TargetedCast.values())
            .collect(java.util.stream.Collectors.toUnmodifiableMap(spell -> spell.id, spell -> spell));

    private static final Map<Identifier, LbcSauvojen.Payload> SAUVOJEN_CASTS = LbcSauvojen.payloads().stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(LbcSauvojen.Payload::id, payload -> payload));
    private static final Set<Identifier> DIRECTED_CAST_IDS = java.util.stream.Stream
            .concat(CASTS.keySet().stream(), SAUVOJEN_CASTS.keySet().stream())
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

    public static Set<Identifier> casts() {
        return DIRECTED_CAST_IDS;
    }

    public static boolean hitscan(GameController game, ServerPlayer player, Vec3 from, Vec3 direction,
                                  double range, double damage) {
        double length = direction.length();
        if (!(Double.isFinite(length) && length >= 1e-4 && range > 0 && range <= 1024
                && damage > 0 && damage <= Float.MAX_VALUE))
            throw new IllegalArgumentException("invalid hitscan direction, range or damage");
        var level = player.level();
        if (!(Math.abs(from.x) < 30_000_000 && Math.abs(from.z) < 30_000_000
                && from.y >= level.getMinY() && from.y < level.getMaxY() + 1.0)) return false;
        var origin = BlockPos.containing(from);
        if (!game.items().genie().runtime().admitted(player) || game.stage() == GameController.Stage.ENDING
                || !level.isInWorldBounds(origin) || !level.hasChunkAt(origin) || !level.getWorldBorder().isWithinBounds(origin)
                || game.items().magic().mechanismSuppressed(player)) return false;

        var end = from.add(direction.scale(range / length));
        double suppression = game.items().magic().mechanismSuppressionFraction(level, from, end);
        if (suppression <= 0) return false;
        if (suppression <= 1) end = from.lerp(end, suppression);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1, 1);
        if (!(game.items().wands().firstHit(level, player, from, end, entity -> true, false) instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity target)) return false;
        var type = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.ARROW);
        var ledger = game.items().indirectKills();
        var source = ledger.captureDamage(new DamageSource(type, player), ledger.coldCause(player, true));
        boolean accepted = target.hurtServer(level, source, (float) damage);
        if (accepted) LbcAcceptedHits.projectileFeedback(game, player);
        return accepted;
    }

    public static List<java.util.UUID> summon(GameController game, ServerPlayer player, ItemStack weapon, Identifier id, Vec3 center) {
        try (var scope = WandCastOrigin.scope(player, true)) {
            return game.items().summons().scriptSummon(player, weapon, id, center);
        }
    }

    /** Point-targeted abilities keep their own permissions, effects and native attribution. */
    public static boolean castAt(GameController game, ServerPlayer player, ItemStack weapon, Identifier id, Vec3 target,
                                 Integer duration) {
        var spell = TARGETED_CASTS.get(id);
        if (spell == null) throw new IllegalArgumentException("unknown targeted LuckyWars ability");
        if (duration != null && (spell.arcanumLevel == 0 || duration < 1))
            throw new IllegalArgumentException("Only Void Arcanum accepts a positive lifetime");
        var level = player.level();
        if (!(Math.abs(target.x) < 30_000_000 && Math.abs(target.z) < 30_000_000
                && target.y >= level.getMinY() && target.y < level.getMaxY() + 1.0)) return false;
        var position = BlockPos.containing(target);
        if (!player.isAlive() || player.isRemoved() || level != game.maps().level()
                || !game.canUseGameplayItems(player) || game.items().actionsBlocked(player) || stopped(game, player)
                || !level.isInWorldBounds(position) || !level.hasChunkAt(position)
                || !level.getWorldBorder().isWithinBounds(position)) return false;
        if (spell.arcanumLevel > 0)
            return game.items().voidArcanum().scriptSpawn(player, weapon, target, spell.arcanumLevel,
                    duration == null ? 200 : duration);
        return switch (spell) {
            case ORESHNIK -> game.items().cinema().rockets().launch(player, target);
            case EXOPERSO, ACADEMY_EXOPERSO ->
                    game.items().exoperso().scriptRitual(player, target, spell == TargetedCast.ACADEMY_EXOPERSO);
            case ICE_WAND_DETONATE, ACADEMY_ICE_WAND_SHATTER ->
                    game.items().iceWand().scriptDetonate(player, weapon, target, spell == TargetedCast.ACADEMY_ICE_WAND_SHATTER);
            case GOHEI_SPELLCARD -> game.items().danmaku().scriptBomb(player, target, 300, false);
            case GOHEI_SPELLCARD_FULL -> game.items().danmaku().scriptBomb(player, target, 1500, false);
            case HAKUREI_BOMB -> game.items().danmaku().scriptBomb(player, target, 300, true);
            case FIREBLAZE_WAND, ACADEMY_FIREBLAZE_WORLD, ACADEMY_FIREBLAZE_MEGA,
                    ACADEMY_FIREBLAZE_PILLARS, ACADEMY_FIREBLAZE_KIUAS ->
                    game.items().blaze().scriptFireblaze(player, target, switch (spell) {
                        case ACADEMY_FIREBLAZE_WORLD -> 1;
                        case ACADEMY_FIREBLAZE_MEGA -> 2;
                        case ACADEMY_FIREBLAZE_PILLARS -> 3;
                        case ACADEMY_FIREBLAZE_KIUAS -> 4;
                        default -> 0;
                    }, weapon);
            case PSYCHO_AREA, PSYCHO_SOUND, PSYCHO_SCREAMER, PSYCHO_LOOK_BACK, PSYCHO_SCARE ->
                    game.items().psycho().scriptArea(player, target, switch (spell) {
                        case PSYCHO_SOUND -> 1;
                        case PSYCHO_SCREAMER -> 2;
                        case PSYCHO_LOOK_BACK -> 3;
                        case PSYCHO_SCARE -> 4;
                        default -> 0;
                    });
            default -> {
                var magic = game.items().magic();
                if (magic.suppressed(player) || magic.suppressedAt(level, player.getEyePosition())) yield false;
                if (spell == TargetedCast.SUCCUBUS_BLACK_HOLE)
                    yield game.items().sheep().spawnHole(player, target) != null;
                var hedgehog = game.items().hedgehog();
                yield spell == TargetedCast.HEDGEHOG_WALL
                        ? hedgehog.spawnWall(level, target, player) > 0
                        : hedgehog.spawnHelper(player, level, target, spell.helper) != null;
            }
        };
    }

    /** Directed spell effects, independent of the actual item occupying either hand. */
    public static boolean cast(GameController game, ServerPlayer player, InteractionHand hand, ItemStack weapon,
                               Identifier id, Vec3 origin, Vec3 direction) {
        var spell = CASTS.get(id);
        var payload = SAUVOJEN_CASTS.get(id);
        if (spell == null && payload == null) throw new IllegalArgumentException("unknown directed LuckyWars spell");
        double length = direction.length();
        if (!Double.isFinite(length) || length < 1e-4) throw new IllegalArgumentException("finite nonzero aim required");

        var level = player.level();
        if (Math.abs(origin.x) >= 30_000_000 || Math.abs(origin.z) >= 30_000_000
                || origin.y < level.getMinY() || origin.y > level.getMaxY()) return false;
        var pos = BlockPos.containing(origin);
        if (!LbcLibrarySpells.admitted(game, player, hand) || game.isObserver(player)
                || !level.isInWorldBounds(pos) || !level.hasChunkAt(pos)
                || !level.getWorldBorder().isWithinBounds(pos)
                || game.items().magic().suppressedAt(level, origin)) return false;

        Vec3 aim = direction.scale(1 / length);
        try (var borrowed = WandCastOrigin.scope(player, true)) {
            if (payload != null) {
                game.items().sauvojen().emitPayload(player, hand, weapon, payload, origin, aim);
                return true;
            }
            return switch (spell) {
                case ELECTRO_ARC, ELECTRO_VOLLEY, ELECTRO_RAY -> {
                    if (game.items().magic().mechanismSuppressedAt(level, origin)) yield false;
                    var cause = game.items().indirectKills().coldCause(player, true);
                    var wands = game.items().wands();
                    if (spell == Cast.ELECTRO_RAY) wands.electroRayAt(player, origin, aim, cause);
                    else wands.directedElectro(player, origin, aim, spell == Cast.ELECTRO_ARC ? 1 : 3, cause);
                    yield true;
                }
                case AWAKENED_ACID_WAND, AWAKENED_ACID_WAND_IGNITED, AWAKENED_ACID_CAUSTIC, AWAKENED_ACID_CAUSTIC_IGNITED,
                        AWAKENED_ACID_GATLING, AWAKENED_ACID_GATLING_IGNITED, AWAKENED_ACID_RIFLE, AWAKENED_ACID_RIFLE_IGNITED ->
                        game.items().awakenedAcid().scriptCast(player,weapon,origin,aim,spell.acidMode,spell.ignited);
                case FARTUS_WAND, FARTUS_BANANA_VOLLEY, FARTUS_LABEL_VOLLEY, ACADEMY_FARTUS_WAND ->
                        game.items().fartus().scriptVolley(player, weapon, origin, aim, switch (spell) {
                            case FARTUS_BANANA_VOLLEY -> LbcFartus.Kind.BANANA;
                            case FARTUS_LABEL_VOLLEY -> LbcFartus.Kind.LABEL;
                            case ACADEMY_FARTUS_WAND -> LbcFartus.Kind.HOMING;
                            default -> null;
                        });
                case ICE_WAND, ACADEMY_ICE_WAND -> game.items().iceWand().scriptShot(player, hand, weapon, origin, aim);
                case MUSIC_STAFF, ACADEMY_MUSIC_STAFF ->
                        game.items().musicStaff().scriptShot(player, weapon, origin, aim, spell == Cast.ACADEMY_MUSIC_STAFF);
                case RUBY_WAND, ACADEMY_RUBY_WAND -> game.items().ruby().scriptDrill(player, origin, aim);
                case RUBY_COMPRESSION, RUBY_CUTTING_RAY -> {
                    try (var enchanted = WeaponEnchantments.using(player, weapon)) {
                        yield game.items().ruby().scriptRay(player, origin, aim, spell == Cast.RUBY_COMPRESSION);
                    }
                }
                case STONE_WAND, ACADEMY_STONE_WAND, STONE_SHIELD, STONE_BRIDGE, STONE_SPIKES -> {
                    int mode = switch (spell) {
                        case ACADEMY_STONE_WAND -> 1;
                        case STONE_SHIELD -> 2;
                        case STONE_BRIDGE -> 3;
                        case STONE_SPIKES -> 4;
                        default -> 0;
                    };
                    try (var enchanted = WeaponEnchantments.using(player, weapon)) {
                        yield game.items().stone().scriptCast(player, origin, aim, mode);
                    }
                }
                case DANMAKU_CARD, DANMAKU_HOMING_CARD -> game.items().danmaku()
                        .scriptCard(player, origin, aim, spell == Cast.DANMAKU_HOMING_CARD);
                case DANMAKU_VOLLEY, DANMAKU_OFFHAND_VOLLEY, ACADEMY_DANMAKU_VOLLEY, ACADEMY_DANMAKU_OFFHAND_VOLLEY ->
                        game.items().danmaku().scriptVolley(player, origin, aim, switch (spell) {
                            case DANMAKU_VOLLEY -> LbcDanmaku.Volley.STANDARD;
                            case DANMAKU_OFFHAND_VOLLEY -> LbcDanmaku.Volley.STANDARD_OFFHAND;
                            case ACADEMY_DANMAKU_VOLLEY -> LbcDanmaku.Volley.ACADEMY;
                            default -> LbcDanmaku.Volley.ACADEMY_OFFHAND;
                        });
                case POLYMORPH_RAY, ACADEMY_POLYMORPH_RAY -> game.items().sheep()
                        .scriptRay(player, hand, spell == Cast.ACADEMY_POLYMORPH_RAY, origin, aim);
                case UNSTABLE_PRISM_CHARGE, ACADEMY_PRISM_RAY -> game.items().prism()
                        .scriptCast(player, hand, weapon, origin, aim, spell == Cast.ACADEMY_PRISM_RAY);
                case FALLING_STARS, ACADEMY_FALLING_STARS, FALLING_STARS_BIG, FALLING_STARS_FIRE_RAIN -> {
                    int mode = switch (spell) {
                        case FALLING_STARS_BIG -> 2;
                        case FALLING_STARS_FIRE_RAIN -> 3;
                        default -> 1;
                    };
                    game.items().homing().scriptStars(player, origin, aim, spell != Cast.FALLING_STARS, mode);
                    yield true;
                }
                case WALL, ACADEMY_WALL, WALL_HORIZONTAL, WALL_DISMANTLE, WALL_STAIRS -> {
                    int mode = switch (spell) {
                        case WALL_HORIZONTAL -> 2;
                        case WALL_DISMANTLE -> 3;
                        case WALL_STAIRS -> 4;
                        default -> 1;
                    };
                    LbcWall.scriptCast(game, player, origin, aim, spell != Cast.WALL, mode);
                    yield true;
                }
                case ADVANCED_ENDER, ACADEMY_ADVANCED_ENDER, ADVANCED_ENDER_TOP, ADVANCED_ENDER_CLIP,
                        SNOW_WAND, ACADEMY_SNOW_WAND -> {
                    boolean snow = spell == Cast.SNOW_WAND || spell == Cast.ACADEMY_SNOW_WAND;
                    boolean academy = spell != Cast.ADVANCED_ENDER && spell != Cast.SNOW_WAND;
                    int mode = switch (spell) {
                        case ADVANCED_ENDER_TOP -> 2;
                        case ADVANCED_ENDER_CLIP -> 3;
                        default -> 1;
                    };
                    game.items().advancedSnow().scriptCast(player, hand, origin, aim, snow, academy, mode);
                    yield true;
                }
                default -> {
                    var body = game.items().terra().castAt(player, origin, aim, spell.terra, 0);
                    if (body == null || body.getOwner() != player || level.getEntity(body.getUUID()) != body) yield false;
                    GenieCombat.castProjectile(game, player, body, weapon);
                    yield true;
                }
            };
        }
    }

    public static net.minecraft.world.phys.BlockHitResult wall(ServerLevel level,net.minecraft.world.phys.Vec3 from,net.minecraft.world.phys.Vec3 to){
        return LbcTerra.shapeHit(level,from,to,(pos,state)->state.getCollisionShape(level,pos));
    }
    public static void mark(GameController game,Entity entity){game.items().markCurrentItemEpoch(entity);}
    public static void credit(GameController game,Entity entity,ServerPlayer author){
        var cause=game.items().indirectKills().coldCause(author,entity instanceof net.minecraft.world.entity.projectile.Projectile);
        game.items().spellSources().bind(entity,new LbcSpellSource.Source(entity.getUUID(),author.getUUID(),game.items().epoch(),cause,java.util.Set.of()));
    }
    public static void initialize(GameController game,net.minecraft.world.entity.Mob mob){
        if(mob.level() instanceof ServerLevel level)try(var scope=game.items().spellSources().scope(mob)){
            mob.finalizeSpawn(level,level.getCurrentDifficultyAt(mob.blockPosition()),net.minecraft.world.entity.EntitySpawnReason.TRIGGERED,null);
        }
    }
    public static void rejected(GameController game,Entity entity){
        entity.discard();game.items().spellSources().unloaded(entity);
    }
    public static boolean write(GameController game,ServerPlayer player,ServerLevel level,BlockPos pos,BlockState state){
        return game.items().writeOwnedTerrain(level,pos,state,game.items().indirectKills().coldCause(player,false),true);
    }
}
