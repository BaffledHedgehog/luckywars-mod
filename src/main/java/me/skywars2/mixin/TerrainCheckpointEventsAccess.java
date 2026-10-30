package me.skywars2.mixin;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockEventData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerLevel.class)
public interface TerrainCheckpointEventsAccess {
    @Accessor("blockEvents") ObjectLinkedOpenHashSet<BlockEventData> skywars2$events();
    @Accessor("blockEventsToReschedule") List<BlockEventData> skywars2$deferred();
}
