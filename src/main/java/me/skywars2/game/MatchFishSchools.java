package me.skywars2.game;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.fish.AbstractSchoolingFish;

/** Native school links and counters form one group; replay must not call join/leave callbacks. */
final class MatchFishSchools {
    private static final Field LEADER=field("leader"), SIZE=field("schoolSize");
    private record Image(AbstractSchoolingFish fish,int size,MatchDamageSources.Ref leader){}
    private final Map<UUID,Image> images=new LinkedHashMap<>();

    MatchFishSchools(Collection<? extends Entity> actors,MatchDamageSources sources){
        for(var actor:actors)if(actor instanceof AbstractSchoolingFish fish)
            images.put(fish.getUUID(),new Image(fish,(Integer)read(SIZE,fish),sources.dependencyRef(leader(fish))));
    }
    static AbstractSchoolingFish leader(AbstractSchoolingFish fish){return (AbstractSchoolingFish)read(LEADER,fish);}
    void restore(Function<UUID,Entity> lookup,Set<UUID> pending,Set<UUID> omitted){
        // Start every link replay from the saved values, including historical omitted bodies.
        // This makes omission accounting idempotent rather than decrementing live counters twice.
        for(var image:images.values())write(SIZE,image.fish,image.size);
        for(var image:images.values()){
            var resolved=MatchDamageSources.resolve(image.leader,lookup,pending);
            if(resolved!=null&&!(resolved instanceof AbstractSchoolingFish))throw new IllegalStateException("Native school leader resolved to a non-schooling body");
            write(LEADER,image.fish,resolved);
            if(omitted.contains(image.fish.getUUID())&&image.leader!=null&&image.leader.historical()==null){
                var parent=images.get(image.leader.id());
                if(parent!=null&&!omitted.contains(parent.fish.getUUID())){
                    int remaining=(Integer)read(SIZE,parent.fish);
                    write(SIZE,parent.fish,Math.max(1,remaining-1));
                }
            }
        }
    }
    private static Field field(String name){
        try{var field=AbstractSchoolingFish.class.getDeclaredField(name);field.setAccessible(true);return field;}
        catch(ReflectiveOperationException e){throw new IllegalStateException("Review native school field "+name,e);}
    }
    private static Object read(Field field,Object body){try{return field.get(body);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static void write(Field field,Object body,Object value){try{field.set(body,value);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
}
