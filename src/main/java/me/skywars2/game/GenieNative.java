package me.skywars2.game;

import me.skywars2.SkyWars2;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.skywars2.game.genie.GenieRuntime;
import me.skywars2.game.genie.GenieScriptItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Narrow bridge to existing package-owned policies, keeping the new feature in game.genie. */
public final class GenieNative {
    private GenieNative(){}
    public static void acceptedThrow(LivingEntity thrower, InteractionHand hand, ItemStack physical, ItemStack projectileStack,
                                     Projectile projectile) {
        if(!GenieRuntime.matches(projectileStack)||!(thrower instanceof ServerPlayer player))return;
        var game=SkyWars2.game();
        if(game==null||projectile.getOwner()!=player||player.level().getEntity(projectile.getUUID())!=projectile)return;
        GenieCombat.launched(game,player,hand,physical,projectileStack,List.of(projectile));
        if(!GenieScriptItem.matches(projectileStack))return;
        var runtime=game.items().genie().runtime().scripts();
        var accepted=runtime.captureThrow(player,hand,physical,projectileStack,projectile);
        LbcTmtShotCompletion.afterNativeAction(accepted);
    }
    public static boolean stopped(GameController game,ServerPlayer player){return game.items().wands().inputTimeStopped(player);}
    public static boolean enemy(GameController game,ServerPlayer player,Entity entity){return game.items().wands().projectileTarget(player,entity,true);}
    public static java.util.Map<Identifier,Integer> grimoires(){return LbcLibrarySpells.scriptSpells();}
    public static boolean grimoire(GameController game,ServerPlayer player,Identifier id,InteractionHand hand){
        Integer spell=grimoires().get(id);
        if(spell==null)throw new IllegalArgumentException("unknown native grimoire");
        return LbcLibrarySpells.admitted(game,player,hand)&&LbcLibrarySpells.cast(game,player,hand,spell);
    }
    private enum Cast { ELECTRO_ARC, ELECTRO_VOLLEY, ELECTRO_RAY, TERRA_CHARGE, ACADEMY_TERRA_CHARGE }
    private static final Map<Identifier, Cast> CASTS = Map.of(
        Identifier.parse("skywars2:electro_arc"), Cast.ELECTRO_ARC,
        Identifier.parse("skywars2:electro_volley"), Cast.ELECTRO_VOLLEY,
        Identifier.parse("skywars2:electro_ray"), Cast.ELECTRO_RAY,
        Identifier.parse("skywars2:terra_charge"), Cast.TERRA_CHARGE,
        Identifier.parse("skywars2:academy_terra_charge"), Cast.ACADEMY_TERRA_CHARGE
    );

    public static Set<Identifier> casts() {
        return CASTS.keySet();
    }

    /** Directed spell effects, independent of the actual item occupying either hand. */
    public static boolean cast(GameController game, ServerPlayer player, ItemStack weapon, Identifier id,
                               Vec3 origin, Vec3 direction) {
        var spell = CASTS.get(id);
        if (spell == null) throw new IllegalArgumentException("unknown directed LuckyWars spell");
        double length = direction.length();
        if (!Double.isFinite(length) || length < 1e-4) throw new IllegalArgumentException("finite nonzero aim required");

        var level = player.level();
        if (Math.abs(origin.x) >= 30_000_000 || Math.abs(origin.z) >= 30_000_000
                || origin.y < level.getMinY() || origin.y > level.getMaxY()) return false;
        var pos = BlockPos.containing(origin);
        if (!LbcLibrarySpells.admitted(game, player, InteractionHand.MAIN_HAND)
                || !level.isInWorldBounds(pos) || !level.hasChunkAt(pos)
                || !level.getWorldBorder().isWithinBounds(pos)
                || game.items().magic().suppressedAt(level, origin)) return false;

        Vec3 aim = direction.scale(1 / length);
        try (var borrowed = WandCastOrigin.scope(player, true)) {
            return switch (spell) {
                case ELECTRO_ARC, ELECTRO_VOLLEY, ELECTRO_RAY -> {
                    if (game.items().magic().mechanismSuppressedAt(level, origin)) yield false;
                    var cause = game.items().indirectKills().coldCause(player, true);
                    var wands = game.items().wands();
                    if (spell == Cast.ELECTRO_RAY) wands.electroRayAt(player, origin, aim, cause);
                    else wands.directedElectro(player, origin, aim, spell == Cast.ELECTRO_ARC ? 1 : 3, cause);
                    yield true;
                }
                case TERRA_CHARGE, ACADEMY_TERRA_CHARGE -> {
                    var kind = spell == Cast.TERRA_CHARGE ? LbcTerra.Kind.REGULAR : LbcTerra.Kind.ACADEMY;
                    var body = game.items().terra().castAt(player, origin, aim, kind, 0);
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
