package me.skywars2.game.match.events;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.skywars2.mixin.terrain.TerrainCheckpointEventsAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.ChunkPos;

/** Native ordered events for the terrain owner's included chunks. No event callbacks during replay. */
public final class MatchBlockEvents {
    private record Image(List<BlockEventData> queue,List<BlockEventData> deferred,Set<Long> chunks,java.util.function.Predicate<net.minecraft.core.BlockPos> admitted) { }
    private final Map<ServerLevel,Image> images=new LinkedHashMap<>();
    private static BlockEventData copy(BlockEventData e){return new BlockEventData(e.pos().immutable(),e.block(),e.paramA(),e.paramB());}
    public void include(ServerLevel level,ChunkPos chunk) {
        include(level,chunk,p->true);
    }
    public void include(ServerLevel level,ChunkPos chunk,java.util.function.Predicate<net.minecraft.core.BlockPos> admitted) {
        var image=images.computeIfAbsent(level,l->{var access=(TerrainCheckpointEventsAccess)l;
            return new Image(access.skywars2$events().stream().filter(e->admitted.test(e.pos())).map(MatchBlockEvents::copy).toList(),
                    access.skywars2$deferred().stream().filter(e->admitted.test(e.pos())).map(MatchBlockEvents::copy).toList(),new LinkedHashSet<>(),admitted);});
        image.chunks.add(chunk.pack());
    }
    private static boolean owned(Image image,BlockEventData event){return image.admitted.test(event.pos())&&image.chunks.contains(ChunkPos.pack(event.pos()));}
    public void restore() {
        for(var entry:images.entrySet()) {
            var access=(TerrainCheckpointEventsAccess)entry.getKey();var image=entry.getValue();
            // Unowned events retain their current relative order; included events
            // regain their saved cross-chunk order. When all events are owned the
            // entire native queue is exact. Full world coverage is a caller contract.
            access.skywars2$events().removeIf(e->owned(image,e));
            for(var event:image.queue)if(owned(image,event))access.skywars2$events().add(copy(event));
            access.skywars2$deferred().removeIf(e->owned(image,e));
            for(var event:image.deferred)if(owned(image,event))access.skywars2$deferred().add(copy(event));
        }
    }
}
