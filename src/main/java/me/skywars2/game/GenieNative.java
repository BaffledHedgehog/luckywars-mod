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
        TERRA_CHARGE(LbcTerra.Kind.REGULAR), ACADEMY_TERRA_CHARGE(LbcTerra.Kind.ACADEMY),
        TERRA_SWORD_CHARGE(LbcTerra.Kind.SWORD), TRUE_TERRA_CHARGE(LbcTerra.Kind.TRUE_CHARGE),
        TRUE_TERRA_BEAM(LbcTerra.Kind.TRUE_BEAM), TERRA_RAZOR(LbcTerra.Kind.RAZOR);

        final LbcTerra.Kind terra;

        Cast() { this(null); }
        Cast(LbcTerra.Kind terra) { this.terra = terra; }
    }
    private static final Map<Identifier, Cast> CASTS = java.util.Arrays.stream(Cast.values())
            .collect(java.util.stream.Collectors.toUnmodifiableMap(
                    spell -> Identifier.parse("skywars2:" + spell.name().toLowerCase(java.util.Locale.ROOT)), spell -> spell));
    public enum TargetedCast {
        ORESHNIK("oreshnik", "Absolute Cinema: Putin / Oreshnik", "Абсолютное кино: Путин / Орешник",
                "Launches the original rocket and its cluster at target."),
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
        HAKUREI_BOMB("hakurei_bomb", "Hakurei Shrine: Fantasy Seal bomb", "Храм Хакурей: бомба Печать фантазии",
                "Original faster yellow Fantasy Seal bomb with its normal 300 charge and lingering shell.");

        public final Identifier id;
        public final List<String> names;
        public final String effect;
        private final LbcHedgehog.Kind helper;

        TargetedCast(String path, String english, String russian, String effect) {
            this(path, english, russian, effect, null);
        }

        TargetedCast(String path, String english, String russian, LbcHedgehog.Kind helper) {
            this(path, english, russian, "Summons one native allied hedgehog. Target is its unobstructed feet position.", helper);
        }

        TargetedCast(String path, String english, String russian, String effect, LbcHedgehog.Kind helper) {
            this.id = Identifier.parse("skywars2:" + path);
            this.names = List.of(english, russian);
            this.effect = effect;
            this.helper = helper;
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

    public static List<java.util.UUID> summon(GameController game, ServerPlayer player, Identifier id, Vec3 center) {
        try (var scope = WandCastOrigin.scope(player, true)) {
            return game.items().summons().scriptSummon(player, id, center);
        }
    }

    /** Point-targeted abilities keep their own permissions, effects and native attribution. */
    public static boolean castAt(GameController game, ServerPlayer player, Identifier id, Vec3 target) {
        var spell = TARGETED_CASTS.get(id);
        if (spell == null) throw new IllegalArgumentException("unknown targeted LuckyWars ability");
        var level = player.level();
        if (!(Math.abs(target.x) < 30_000_000 && Math.abs(target.z) < 30_000_000
                && target.y >= level.getMinY() && target.y < level.getMaxY() + 1.0)) return false;
        var position = BlockPos.containing(target);
        if (!player.isAlive() || player.isRemoved() || level != game.maps().level()
                || !game.canUseGameplayItems(player) || game.items().actionsBlocked(player) || stopped(game, player)
                || !level.isInWorldBounds(position) || !level.hasChunkAt(position)
                || !level.getWorldBorder().isWithinBounds(position)) return false;
        return switch (spell) {
            case ORESHNIK -> game.items().cinema().rockets().launch(player, target);
            case GOHEI_SPELLCARD -> game.items().danmaku().scriptBomb(player, target, 300, false);
            case GOHEI_SPELLCARD_FULL -> game.items().danmaku().scriptBomb(player, target, 1500, false);
            case HAKUREI_BOMB -> game.items().danmaku().scriptBomb(player, target, 300, true);
            default -> {
                var magic = game.items().magic();
                if (magic.suppressed(player) || magic.suppressedAt(level, player.getEyePosition())) yield false;
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
                case DANMAKU_CARD, DANMAKU_HOMING_CARD -> game.items().danmaku()
                        .scriptCard(player, origin, aim, spell == Cast.DANMAKU_HOMING_CARD);
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
