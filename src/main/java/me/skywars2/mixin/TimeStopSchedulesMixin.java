package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.Queue;
import java.util.function.BiConsumer;
import me.skywars2.game.LbcTimeStopSchedules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelTicks.class)
public abstract class TimeStopSchedulesMixin<T> implements LbcTimeStopSchedules.Access<T> {
    @Shadow @Final private Long2ObjectMap<LevelChunkTicks<T>> allContainers;
    @Shadow @Final private Long2LongMap nextTickForContainer;
    @Shadow @Final private Queue<LevelChunkTicks<T>> containersToTick;

    @Override public Long2ObjectMap<LevelChunkTicks<T>> skywars2$containers() { return allContainers; }
    @Override public Long2LongMap skywars2$nextTicks() { return nextTickForContainer; }
    @Override public Queue<LevelChunkTicks<T>> skywars2$drainingContainers() { return containersToTick; }

    @SuppressWarnings("unchecked")
    private LevelTicks<T> skywars2$self() { return (LevelTicks<T>) (Object) this; }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void skywars2$pause(long currentTick, int maxTicks, BiConsumer<BlockPos, T> output, CallbackInfo ci) {
        var game = me.skywars2.SkyWars2.game();
        var arena = game == null ? null : game.maps().level();
        // Skip the scheduler itself so pending water/lava ticks survive map replacement.
        if (arena != null && (Object) this == arena.getFluidTicks() && game.maps().suspendsFluidTicks()
                || LbcTimeStopSchedules.stopped(skywars2$self())) ci.cancel();
    }

    @Inject(method = "addContainer", at = @At("HEAD"))
    private void skywars2$attach(ChunkPos pos, LevelChunkTicks<T> container, CallbackInfo ci) {
        LbcTimeStopSchedules.added(skywars2$self(), container);
    }

    @Inject(method = "removeContainer", at = @At("HEAD"))
    private void skywars2$detach(ChunkPos pos, CallbackInfo ci) {
        LbcTimeStopSchedules.removing(skywars2$self(), pos.pack());
    }

    // /clone copies absolute queue timestamps. Convert the source anchor back to the
    // real clock before the destination chunk's schedule hook applies its own anchor.
    @WrapMethod(method = "copyAreaFrom")
    private void skywars2$copyClock(LevelTicks<T> source, BoundingBox area, Vec3i offset, Operation<Void> original) {
        LbcTimeStopSchedules.copyArea(source, () -> original.call(source, area, offset));
    }

    @Mixin(LevelChunkTicks.class)
    public abstract static class ChunkTicks<T> {
        @SuppressWarnings("unchecked")
        private LevelChunkTicks<T> skywars2$self() { return (LevelChunkTicks<T>) (Object) this; }

        @ModifyVariable(method = "schedule", at = @At("HEAD"), argsOnly = true)
        private ScheduledTick<T> skywars2$scheduleClock(ScheduledTick<T> tick) {
            return LbcTimeStopSchedules.scheduling(skywars2$self(), tick);
        }

        @ModifyVariable(method = "pack", at = @At("HEAD"), argsOnly = true)
        private long skywars2$saveClock(long currentTick) {
            return LbcTimeStopSchedules.containerClock(skywars2$self(), currentTick);
        }

        @ModifyVariable(method = "unpack", at = @At("HEAD"), argsOnly = true)
        private long skywars2$loadClock(long currentTick) {
            return LbcTimeStopSchedules.containerClock(skywars2$self(), currentTick);
        }
    }
}
