package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.ScoreHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Derived native ownership index; no periodic scoreboard/entity scan. */
@Mixin(ServerScoreboard.class)
public abstract class SymbioteScoreMixin {
    @Inject(method="onScoreChanged",at=@At("RETURN"))
    private void changed(ScoreHolder holder,Objective objective,Score score,CallbackInfo ci){
        var game=SkyWars2.game();
        if(game!=null&&game.items()!=null&&game.items().communism()!=null&&objective.getName().equals("lbcID2"))
            game.items().communism().ownershipScoreChanged(holder.getScoreboardName(),score.value());
    }
    @Inject(method="onPlayerScoreRemoved",at=@At("RETURN"))
    private void removed(ScoreHolder holder,Objective objective,CallbackInfo ci){
        var game=SkyWars2.game();
        if(game!=null&&game.items()!=null&&game.items().communism()!=null&&objective.getName().equals("lbcID2"))
            game.items().communism().ownershipScoreChanged(holder.getScoreboardName(),null);
    }
    @Inject(method="onPlayerRemoved",at=@At("RETURN"))
    private void removedAll(ScoreHolder holder,CallbackInfo ci){
        var game=SkyWars2.game();
        if(game!=null&&game.items()!=null&&game.items().communism()!=null)game.items().communism().ownershipScoreChanged(holder.getScoreboardName(),null);
    }
    @Inject(method="onObjectiveRemoved",at=@At("RETURN"))
    private void removedObjective(Objective objective,CallbackInfo ci){
        var game=SkyWars2.game();
        if(game!=null&&game.items()!=null&&game.items().communism()!=null&&objective.getName().equals("lbcID2"))game.items().communism().ownershipObjectiveRemoved();
    }
}
