package me.skywars2.game;

import java.util.*;
import java.util.function.Function;
import me.skywars2.mixin.CreatureCheckpointAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Llama;

/** Native bidirectional caravan edges. Replay never joins/leaves through gameplay callbacks. */
public final class MatchLlamaCaravans {
    private record Image(Llama llama,MatchDamageSources.Ref head,MatchDamageSources.Ref tail){}
    private final List<Image> images=new ArrayList<>();
    public MatchLlamaCaravans(Collection<? extends Entity> actors,MatchDamageSources sources){
        for(var actor:actors)if(actor instanceof Llama llama)images.add(new Image(llama,sources.dependencyRef(llama.getCaravanHead()),sources.dependencyRef(access(llama).skywars2$caravanTail())));
    }
    public void restore(Function<UUID,Entity> lookup,Set<UUID> pending){
        for (var image : images) {
            var llama = access(image.llama);
            llama.skywars2$caravanHead(resolve(image.head,lookup,pending));
            llama.skywars2$caravanTail(resolve(image.tail,lookup,pending));
        }
    }
    static void leave(Llama llama){
        var head = llama.getCaravanHead();
        if (head != null) access(head).skywars2$caravanTail(null);
        access(llama).skywars2$caravanHead(null);
    }
    private static CreatureCheckpointAccess.Caravan access(Llama llama) {
        return (CreatureCheckpointAccess.Caravan) llama;
    }
    private static Llama resolve(MatchDamageSources.Ref ref,Function<UUID,Entity> lookup,Set<UUID> pending){
        var value=MatchDamageSources.resolve(ref,lookup,pending);
        if (value != null && !(value instanceof Llama)) throw new IllegalStateException("Native caravan edge resolved to a non-llama body");
        return (Llama) value;
    }
}
