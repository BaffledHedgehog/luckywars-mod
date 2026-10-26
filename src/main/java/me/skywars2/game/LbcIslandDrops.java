package me.skywars2.game;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Native clone, strict clear and native teleport remain separate source commands. */
public final class LbcIslandDrops {
    private final GameController game;
    public LbcIslandDrops(GameController game) { this.game=game; }

    public ServerPlayer target(ItemEntity token) { return game.items().cageDrops().target(token); }

    private boolean admitted(ItemEntity token) {
        if(token.isRemoved() || token.getAge()>=5999 || !LbcIslandDropItem.matches(token.getItem())
                || !game.playing() || game.stage()==GameController.Stage.ENDING
                || token.level()!=game.maps().level() || !(token.level() instanceof ServerLevel level)
                || game.items().timeStop().stopped(level))return false;
        for(var tag:token.entityTags())if(tag.startsWith("skywars2.items.") && !game.items().currentEpoch(tag))return false;
        return true;
    }

    /** Preflight the complete ordered pair before the dispatcher performs either branch. */
    public boolean ready(ItemEntity token) {
        if(!admitted(token))return false;
        var level=(ServerLevel)token.level();var raise=target(token);
        boolean raising=LbcIslandDropItem.matches(token.getItem(),LbcIslandDropItem.Kind.RAISE);
        if(raising && raise!=null && !resident(level,raise.position(),LbcIslandDropItem.Kind.RAISE))return false;
        if(!LbcIslandDropItem.matches(token.getItem(),LbcIslandDropItem.Kind.LOWER_OLD))return true;
        // The next source selector runs again after normal TP, including the token's possible move.
        Vec3 tokenPos=projected(token,raising?raise:null);
        ServerPlayer lower=null;double distance=Double.POSITIVE_INFINITY;
        for(var player:level.players()) {
            if(!game.items().wands().validTarget(player))continue;
            double candidate=projected(player,raising?raise:null).distanceToSqr(tokenPos);
            if(candidate<distance) { lower=player;distance=candidate; }
        }
        return lower==null || resident(level,projected(lower,raising?raise:null),LbcIslandDropItem.Kind.LOWER_OLD);
    }

    /** Current-branch admission: spawn reevaluates its actual nearest player after earlier branches. */
    public boolean ready(ItemEntity token,LbcIslandDropItem.Kind kind) {
        if(!admitted(token) || !LbcIslandDropItem.matches(token.getItem(),kind))return false;
        var target=target(token);
        return target==null || resident((ServerLevel)token.level(),target.position(),kind);
    }

    private Vec3 projected(Entity actor,ServerPlayer raise) {
        if(raise==null || !eligible(actor,LbcIslandDropItem.Kind.RAISE)
                || actor.position().distanceToSqr(raise.position())>256)return actor.position();
        var destination=actor.position().add(0,46,0);
        return Level.isInSpawnableBounds(BlockPos.containing(destination))?destination:actor.position();
    }

    private static boolean resident(ServerLevel level,Vec3 origin,LbcIslandDropItem.Kind kind) {
        int radius=kind==LbcIslandDropItem.Kind.RAISE?16:9;
        // Source/destination share XZ. One neighbour around their 31/17-wide block footprint
        // also contains every entity destination and native block-update boundary.
        var center=BlockPos.containing(origin);
        for(int x=(center.getX()-radius)>>4;x<=(center.getX()+radius)>>4;x++)
            for(int z=(center.getZ()-radius)>>4;z<=(center.getZ()+radius)>>4;z++)
                if(level.getChunkSource().getChunkNow(x,z)==null)return false;
        return true;
    }

    public boolean spawn(ItemEntity token,LbcIslandDropItem.Kind kind) {
        if(!ready(token,kind))return false;
        var target=target(token);if(target==null)return true;
        var level=(ServerLevel)token.level();var origin=target.position();
        var source=source(level,target,origin);var cause=game.items().specialDrops().owner(token);
        var author=LbcIndirectKills.playerSource(cause);
        // User override: a failed copy leaves terrain intact and runs only the teleport sequence.
        var copied=new AtomicBoolean();
        Runnable geometry=()->game.items().indirectKills().withSupportCandidates(level,region(origin,kind),()-> {
            // Native hasChunksAt permits a region straddling build height and clone can partially
            // place it. The user requires an impossible full copy to leave both islands intact.
            if(!copyFits(level,origin,kind))return;
            if(kind==LbcIslandDropItem.Kind.RAISE) {
                copied.set(execute(source,"clone ~15 ~15 ~15 ~-15 ~-15 ~-15 ~-15 ~31 ~-15")>0);
                if(copied.get())execute(source,"fill ~15 ~15 ~15 ~-15 ~-15 ~-15 minecraft:air strict");
            } else {
                copied.set(execute(source,"clone ~8 ~ ~8 ~-8 ~-12 ~-8 ~-8 ~-25 ~-8")>0);
                if(copied.get())execute(source,"fill ~8 ~ ~8 ~-8 ~-12 ~-8 minecraft:air strict");
            }
        });
        if(author!=null)game.items().indirectKills().trapChanges(level,author,true,geometry);
        else game.items().indirectKills().worldChanges(level,cause,true,geometry);

        int radius=kind==LbcIslandDropItem.Kind.RAISE?16:9;
        int dy=kind==LbcIslandDropItem.Kind.RAISE?46:-13;
        // Native @e selection is after both terrain commands and is based on each entity's position.
        var selected=level.getEntities((Entity)null,new AABB(origin,origin).inflate(radius),
                actor->eligible(actor,kind) && actor.position().distanceToSqr(origin)<=radius*radius);
        for(var actor:List.copyOf(selected)) {
            if(actor.isRemoved())continue;
            var before=actor.position();var destination=before.add(0,dy,0);
            if(execute(source(level,actor,before),"tp @s ~ ~"+dy+" ~")>0
                    && actor.position().distanceToSqr(destination)<1.0e-12
                    && actor instanceof LivingEntity living && author!=null)
                game.items().indirectKills().teleported(living,author,true);
        }
        if(kind==LbcIslandDropItem.Kind.LOWER_OLD && copied.get())
            target.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,40,4,false,false,false),cause);
        return true;
    }

    private boolean eligible(Entity actor,LbcIslandDropItem.Kind kind) {
        return !actor.entityTags().contains("strict_map_object") && game.items().wands().validTarget(actor)
                && (kind!=LbcIslandDropItem.Kind.LOWER_OLD || actor.getType()!=EntityTypes.PLAYER);
    }
    private CommandSourceStack source(ServerLevel level,Entity actor,Vec3 at) {
        return game.server().createCommandSourceStack().withSource(CommandSource.NULL).withLevel(level)
                .withEntity(actor).withPosition(at).withRotation(actor.getRotationVector())
                .withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput();
    }
    private int execute(CommandSourceStack source,String command) {
        try {
            // Brigadier's simple native endpoints execute synchronously. The higher-level Commands
            // queue can defer nested commands beyond the terrain attribution scope.
            return game.server().getCommands().getDispatcher().execute(command,source);
        } catch(CommandSyntaxException expectedFailure) {
            // Keep native failure as a result for the copy-success gate and following teleports.
            return 0;
        }
    }
    private static AABB region(Vec3 origin,LbcIslandDropItem.Kind kind) {
        var center=BlockPos.containing(origin);int radius=kind==LbcIslandDropItem.Kind.RAISE?15:8;
        int low=kind==LbcIslandDropItem.Kind.RAISE?-15:-25;
        int high=kind==LbcIslandDropItem.Kind.RAISE?61:0;
        return AABB.encapsulatingFullBlocks(center.offset(-radius,low,-radius),center.offset(radius,high,radius));
    }
    private static boolean copyFits(ServerLevel level,Vec3 origin,LbcIslandDropItem.Kind kind) {
        int y=BlockPos.containing(origin).getY();
        int low=kind==LbcIslandDropItem.Kind.RAISE?-15:-25;
        int high=kind==LbcIslandDropItem.Kind.RAISE?61:0;
        return y+low>=level.getMinY() && y+high<level.getMaxY();
    }
}
