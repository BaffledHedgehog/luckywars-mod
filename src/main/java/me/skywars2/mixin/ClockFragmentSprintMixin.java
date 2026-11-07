package me.skywars2.mixin;

import me.skywars2.game.LbcClockFragments;
import net.minecraft.server.ServerTickRateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(ServerTickRateManager.class)
public abstract class ClockFragmentSprintMixin {
    @Inject(method="setFrozen",at=@At("HEAD"))
    private void skywars2$releaseBeforeFreeze(boolean frozen,CallbackInfo ci){
        LbcClockFragments.beforeFreeze((ServerTickRateManager)(Object)this,frozen);
    }
    @Inject(method="requestGameToSprint",at=@At("HEAD"))
    private void skywars2$releaseBeforeForeignSprint(int ticks,CallbackInfoReturnable<Boolean> cir){
        LbcClockFragments.beforeSprint((ServerTickRateManager)(Object)this);
    }
}
