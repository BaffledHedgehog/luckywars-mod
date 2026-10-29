package me.skywars2.game;

import net.minecraft.server.level.ServerPlayer;

/** Legacy score comparisons over the current native owners. No independent timers or charge ledger. */
final class LbcSourceCounters {
    private final GameController game;
    LbcSourceCounters(GameController game){this.game=game;}
    Integer read(ServerPlayer p,String name){var items=game.items();return switch(name){
        case "swordcharged1"->items.electro().charges(p);
        case "swordcharging1"->items.electro().charging(p);
        case "swordcharged2"->items.terra().charges(p);
        case "swordcharged5"->items.electro().yellowCharges(p);
        case "swordcharging5"->items.electro().yellowCharging(p);
        case "scared","lookback","twitching"->items.psycho().hudValue(p,name);
        case "shock"->items.electro().shocks(p);
        case "coldown1"->(int)(((long)items.timeStop().cooldownTicks(p)+19)/20);
        case "coldown16"->items.mobgun().cooldown(p);
        case "coldownshish"->(int)(((long)items.reactive().cooldownTicks(p)+4)/5);
        case "coldown13","coldown17","coldown18","coldown18b","coldown25","coldownMagnum"->items.shooters().sourceCounter(p,name);
        default->{var objective=game.server().getScoreboard().getObjective(name);
            var score=objective==null?null:game.server().getScoreboard().getPlayerScoreInfo(p,objective);
            yield score==null?null:score.value();}
    };}
    /** Envy only improves the existing value. Missing genuine scoreboard operands do not compare. */
    boolean copyBetter(ServerPlayer target,ServerPlayer donor,String name,boolean maximum){
        Integer before=read(target,name),next=read(donor,name);
        if(before==null||next==null||(maximum?before>=next:before<=next))return false;
        var items=game.items();switch(name){
            case "swordcharged1","swordcharging1","swordcharged5","swordcharging5"->
                items.electro().sourceCharge(target,name.endsWith("5"),name.startsWith("swordcharging"),next);
            case "swordcharged2"->items.terra().sourceCharges(target,next);
            case "scared","lookback","twitching"->items.psycho().reduceSourceStatus(target,name,next);
            case "shock"->items.electro().sourceShock(target,next);
            case "coldown1"->items.timeStop().reduceSourceCooldown(target,next);
            case "coldown16"->items.mobgun().sourceCooldown(target,next);
            case "coldownshish"->items.reactive().reduceSourceCooldown(target,next);
            case "coldown13","coldown17","coldown18","coldown18b","coldown25","coldownMagnum"->items.shooters().reduceSourceCounter(target,name,next);
            default->{ }
        }
        items.mana().score(target,name).set(next);
        // This source counter is already scoreboard-owned, but a newly positive one needs its native consumer.
        if(name.equals("divider_cd"))items.duplicating().addPlayerCounters(target);
        return true;
    }
}
