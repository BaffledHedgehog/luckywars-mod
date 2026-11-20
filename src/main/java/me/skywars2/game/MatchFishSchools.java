package me.skywars2.game;

import java.util.*;
import java.util.function.Function;
import me.skywars2.mixin.CreatureCheckpointAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.fish.AbstractSchoolingFish;

/** Native school links and counters form one group; replay must not call join/leave callbacks. */
final class MatchFishSchools {
    private record Image(AbstractSchoolingFish fish,int size,MatchDamageSources.Ref leader){}
    private final Map<UUID,Image> images=new LinkedHashMap<>();

    MatchFishSchools(Collection<? extends Entity> actors,MatchDamageSources sources){
        for(var actor:actors)if(actor instanceof AbstractSchoolingFish fish)
            images.put(fish.getUUID(),new Image(fish,access(fish).skywars2$schoolSize(),sources.dependencyRef(leader(fish))));
    }
    private static CreatureCheckpointAccess.School access(AbstractSchoolingFish fish) {
        return (CreatureCheckpointAccess.School) fish;
    }
    static AbstractSchoolingFish leader(AbstractSchoolingFish fish){return access(fish).skywars2$leader();}
    void restore(Function<UUID,Entity> lookup,Set<UUID> pending,Set<UUID> omitted){
        // Start every link replay from the saved values, including historical omitted bodies.
        // This makes omission accounting idempotent rather than decrementing live counters twice.
        for(var image:images.values())access(image.fish).skywars2$schoolSize(image.size);
        for(var image:images.values()){
            var resolved=MatchDamageSources.resolve(image.leader,lookup,pending);
            if(resolved!=null&&!(resolved instanceof AbstractSchoolingFish))throw new IllegalStateException("Native school leader resolved to a non-schooling body");
            access(image.fish).skywars2$leader((AbstractSchoolingFish) resolved);
            if(omitted.contains(image.fish.getUUID())&&image.leader!=null&&image.leader.historical()==null){
                var parent=images.get(image.leader.id());
                if(parent!=null&&!omitted.contains(parent.fish.getUUID())){
                    var parentState = access(parent.fish);
                    parentState.skywars2$schoolSize(Math.max(1,parentState.skywars2$schoolSize()-1));
                }
            }
        }
    }
}
