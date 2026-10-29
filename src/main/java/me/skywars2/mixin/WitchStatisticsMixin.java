package me.skywars2.mixin;

import me.skywars2.game.LbcWitchStatistics;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native awards/reset remain the only event authority, including Carpet and ordinary players. */
@Mixin(ServerPlayer.class)
public abstract class WitchStatisticsMixin {
    /** Retain old worlds' native criterion and every score/display setting, but stop its broad auto-award. */
    @WrapOperation(method="awardStat",at=@At(value="INVOKE",target="Lnet/minecraft/server/ServerScoreboard;forAllObjectives(Lnet/minecraft/world/scores/criteria/ObjectiveCriteria;Lnet/minecraft/world/scores/ScoreHolder;Ljava/util/function/Consumer;)V"))
    private void skywars2$meleeOnly(net.minecraft.server.ServerScoreboard board,
            net.minecraft.world.scores.criteria.ObjectiveCriteria criteria,net.minecraft.world.scores.ScoreHolder holder,
            java.util.function.Consumer<net.minecraft.world.scores.ScoreAccess> action,Operation<Void> original){
        if(criteria!=net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.DAMAGE_DEALT)){
            original.call(board,criteria,holder,action);return;
        }
        for(var objective:board.getObjectives())if(objective.getCriteria()==criteria&&!objective.getName().equals("dmg_dealt"))
            action.accept(board.getOrCreatePlayerScore(holder,objective,true));
    }
    @Inject(method="awardStat",at=@At("HEAD"))
    private void skywars2$prepare(Stat<?> stat,int count,CallbackInfo ci){LbcWitchStatistics.prepare((ServerPlayer)(Object)this,stat);}
    @Inject(method="awardStat",at=@At("TAIL"))
    private void skywars2$awarded(Stat<?> stat,int count,CallbackInfo ci){LbcWitchStatistics.awarded((ServerPlayer)(Object)this,stat,count);}
    @Inject(method="resetStat",at=@At("HEAD"))
    private void skywars2$prepareReset(Stat<?> stat,CallbackInfo ci){LbcWitchStatistics.prepare((ServerPlayer)(Object)this,stat);}
    @Inject(method="resetStat",at=@At("TAIL"))
    private void skywars2$reset(Stat<?> stat,CallbackInfo ci){LbcWitchStatistics.reset((ServerPlayer)(Object)this,stat);}
}
