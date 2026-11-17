package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

/** Narrow bridge to existing package-owned policies, keeping the new feature in game.genie. */
public final class GenieNative {
    private GenieNative(){}
    public static boolean stopped(GameController game,ServerPlayer player){return game.items().wands().inputTimeStopped(player);}
    public static boolean enemy(GameController game,ServerPlayer player,Entity entity){return game.items().wands().projectileTarget(player,entity,true);}
    public static java.util.Map<Identifier,Integer> grimoires(){return LbcLibrarySpells.scriptSpells();}
    public static boolean grimoire(GameController game,ServerPlayer player,Identifier id,InteractionHand hand){
        Integer spell=grimoires().get(id);
        if(spell==null)throw new IllegalArgumentException("unknown native grimoire");
        return LbcLibrarySpells.admitted(game,player,hand)&&LbcLibrarySpells.cast(game,player,hand,spell);
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
