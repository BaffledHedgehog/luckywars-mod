package me.skywars2.mixin;

import me.skywars2.game.LbcWitchStatistics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native awards/reset remain the only event authority, including Carpet and ordinary players. */
@Mixin(ServerPlayer.class)
public abstract class WitchStatisticsMixin {
    @Inject(method="awardStat",at=@At("HEAD"))
    private void skywars2$prepare(Stat<?> stat,int count,CallbackInfo ci){LbcWitchStatistics.prepare((ServerPlayer)(Object)this,stat);}
    @Inject(method="awardStat",at=@At("TAIL"))
    private void skywars2$awarded(Stat<?> stat,int count,CallbackInfo ci){LbcWitchStatistics.awarded((ServerPlayer)(Object)this,stat,count);}
    @Inject(method="resetStat",at=@At("HEAD"))
    private void skywars2$prepareReset(Stat<?> stat,CallbackInfo ci){LbcWitchStatistics.prepare((ServerPlayer)(Object)this,stat);}
    @Inject(method="resetStat",at=@At("TAIL"))
    private void skywars2$reset(Stat<?> stat,CallbackInfo ci){LbcWitchStatistics.reset((ServerPlayer)(Object)this,stat);}
}
