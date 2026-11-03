package me.skywars2.game;

import java.util.Set;
import net.minecraft.server.level.ServerPlayer;

/** Source initial resource bonuses; three scoreboard deltas own reversible debug selection changes. */
public final class LbcManaLevelups {
    private LbcManaLevelups(){}
    public record Values(int mana,int maximum,int regeneration){}
    /** Author rule: Nulla clears all three resources after the other initial mana bonuses. */
    public static Values resolve(Values original,Set<Integer> selected){
        int mana=original.mana(),maximum=original.maximum(),regeneration=original.regeneration();
        if(selected.contains(2))regeneration+=5;
        if(selected.contains(11)){regeneration+=20;maximum-=2500;mana-=2500;}
        if(selected.contains(17)){maximum+=1500;regeneration+=4;}
        if(selected.contains(31)){maximum+=5000;regeneration+=20;}
        if(selected.contains(32)){mana=0;maximum=0;regeneration=0;}
        return new Values(mana,maximum,regeneration);
    }
    static void apply(GameController game,ServerPlayer player,Set<Integer> selected){
        var mana=game.items().mana();var before=new Values(mana.get(player),mana.maximum(player),mana.baseRegen(player));
        var after=resolve(before,selected);
        if(after.equals(before))return;
        mana.set(player,after.mana());mana.maximum(player,after.maximum());mana.baseRegen(player,after.regeneration());
        mana.score(player,"levelup_mana_delta").set(after.mana()-before.mana());
        mana.score(player,"levelup_manamax_delta").set(after.maximum()-before.maximum());
        mana.score(player,"levelup_manaregen_delta").set(after.regeneration()-before.regeneration());
    }
    static void clear(GameController game,ServerPlayer player){
        var mana=game.items().mana();int amount=mana.hudValue(player,"levelup_mana_delta");
        int maximum=mana.hudValue(player,"levelup_manamax_delta"),regeneration=mana.hudValue(player,"levelup_manaregen_delta");
        if(amount!=0)mana.set(player,mana.get(player)-amount);
        if(maximum!=0)mana.maximum(player,mana.maximum(player)-maximum);
        if(regeneration!=0)mana.baseRegen(player,mana.baseRegen(player)-regeneration);
        resetLedger(game,player);
    }
    /** An explicit full resource reset drops the old layer without debiting the new base. */
    static void resetLedger(GameController game,ServerPlayer player){
        var mana=game.items().mana();
        if(mana.hudValue(player,"levelup_mana_delta")!=0)mana.score(player,"levelup_mana_delta").set(0);
        if(mana.hudValue(player,"levelup_manamax_delta")!=0)mana.score(player,"levelup_manamax_delta").set(0);
        if(mana.hudValue(player,"levelup_manaregen_delta")!=0)mana.score(player,"levelup_manaregen_delta").set(0);
    }
}
