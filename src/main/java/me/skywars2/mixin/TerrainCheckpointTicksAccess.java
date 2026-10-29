package me.skywars2.mixin;

import java.util.List;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.SavedTick;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read pending, not-yet-unpacked ticks without changing the live scheduler. */
@Mixin(LevelChunkTicks.class)
public interface TerrainCheckpointTicksAccess<T> {
    @Accessor("pendingTicks")
    List<SavedTick<T>> skywars2$pendingTicks();
}
