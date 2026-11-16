package me.skywars2.game;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Llama;

/** Native bidirectional caravan edges. Replay never joins/leaves through gameplay callbacks. */
final class MatchLlamaCaravans {
    private static final Field HEAD=field("caravanHead"),TAIL=field("caravanTail");
    private record Image(Llama llama,MatchDamageSources.Ref head,MatchDamageSources.Ref tail){}
    private final List<Image> images=new ArrayList<>();
    MatchLlamaCaravans(Collection<? extends Entity> actors,MatchDamageSources sources){
        for(var actor:actors)if(actor instanceof Llama llama)images.add(new Image(llama,sources.dependencyRef(llama.getCaravanHead()),sources.dependencyRef((Llama)read(TAIL,llama))));
    }
    void restore(Function<UUID,Entity> lookup,Set<UUID> pending){
        for(var image:images){write(HEAD,image.llama,resolve(image.head,lookup,pending));write(TAIL,image.llama,resolve(image.tail,lookup,pending));}
    }
    static void leave(Llama llama){
        var head=llama.getCaravanHead();if(head!=null)write(TAIL,head,null);write(HEAD,llama,null);
    }
    private static Entity resolve(MatchDamageSources.Ref ref,Function<UUID,Entity> lookup,Set<UUID> pending){
        var value=MatchDamageSources.resolve(ref,lookup,pending);
        if(value!=null&&!(value instanceof Llama))throw new IllegalStateException("Native caravan edge resolved to a non-llama body");return value;
    }
    private static Field field(String name){try{var value=Llama.class.getDeclaredField(name);value.setAccessible(true);return value;}catch(ReflectiveOperationException e){throw new IllegalStateException("Review native caravan field "+name,e);}}
    private static Object read(Field field,Object owner){try{return field.get(owner);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static void write(Field field,Object owner,Object value){try{field.set(owner,value);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
}
