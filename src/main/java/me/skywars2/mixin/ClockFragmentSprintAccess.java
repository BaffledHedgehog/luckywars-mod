package me.skywars2.mixin;

import net.minecraft.server.ServerTickRateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Public stopSprinting deliberately cannot settle the native final remaining0 state. */
@Mixin(ServerTickRateManager.class)
public interface ClockFragmentSprintAccess {
    @Invoker("finishTickSprint") void skywars2$finishClockSprint();
}
