package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/** Native statistic events feed the original mutable objectives; lifetime totals are never polled back in. */
public final class LbcWitchStatistics {
    private LbcWitchStatistics(){}
    public static ObjectiveCriteria criterion(String name){return switch(name){
        case "dmg_dealt"->Stats.CUSTOM.get(Stats.DAMAGE_DEALT);
        case "lolidefecation"->Stats.BLOCK_MINED.get(Blocks.MUSHROOM_STEM);
        default->ObjectiveCriteria.DUMMY;
    };}
    private static String name(Stat<?> stat){
        if(stat==Stats.CUSTOM.get(Stats.DAMAGE_DEALT))return "dmg_dealt";
        if(stat==Stats.BLOCK_MINED.get(Blocks.MUSHROOM_STEM))return "lolidefecation";
        return null;
    }
    /** Create before vanilla forAllObjectives, so the first native award is counted too. */
    public static void prepare(ServerPlayer player,Stat<?> stat){
        var game=SkyWars2.game();var name=name(stat);if(game!=null&&name!=null)game.items().mana().score(player,name);
    }
    /** Worlds created by older mod versions have dummy objectives. Preserve them and add only this event. */
    public static void awarded(ServerPlayer player,Stat<?> stat,int count){
        var game=SkyWars2.game();var name=name(stat);if(game==null||name==null)return;
        var objective=player.level().getScoreboard().getObjective(name);
        if(objective!=null&&objective.getCriteria()==ObjectiveCriteria.DUMMY)game.items().mana().score(player,name).add(count);
    }
    public static void reset(ServerPlayer player,Stat<?> stat){
        var game=SkyWars2.game();var name=name(stat);if(game==null||name==null)return;
        var objective=player.level().getScoreboard().getObjective(name);
        if(objective!=null&&objective.getCriteria()==ObjectiveCriteria.DUMMY)game.items().mana().score(player,name).set(0);
    }
}
