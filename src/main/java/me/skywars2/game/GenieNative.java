package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

/** Narrow bridge to existing package-owned policies, keeping the new feature in game.genie. */
public final class GenieNative {
    private GenieNative(){}
    public static boolean stopped(GameController game,ServerPlayer player){return game.items().wands().inputTimeStopped(player);}
    public static boolean enemy(GameController game,ServerPlayer player,Entity entity){return game.items().wands().projectileTarget(player,entity,true);}
    public static void mark(GameController game,Entity entity){game.items().markCurrentItemEpoch(entity);}
    public static boolean write(GameController game,ServerPlayer player,ServerLevel level,BlockPos pos,BlockState state){
        return game.items().writeOwnedTerrain(level,pos,state,game.items().indirectKills().coldCause(player,false),true);
    }
}
