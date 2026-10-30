package me.skywars2.game;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import me.skywars2.mixin.CombatCheckpointAccess;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;

/** One accepted-source graph for native combat and private deferred hits. Never reapply damage. */
final class MatchDamageSources {
    record Ref(UUID id,Entity historical) { }
    record Image(DamageSource original,Ref direct,Ref cause,Image wrapped) { }
    private final Map<UUID,Entity> registered=new LinkedHashMap<>();
    private final Map<DamageSource,Image> images=new IdentityHashMap<>();
    MatchDamageSources(GameController game,Collection<? extends Entity> actors){
        for(var actor:actors)registered.put(actor.getUUID(),actor);
        for(var player:game.server().getPlayerList().getPlayers())registered.put(player.getUUID(),player);
    }
    Ref ref(Entity entity){return entity==null?null:new Ref(entity.getUUID(),(entity instanceof ServerPlayer?registered.get(entity.getUUID()) instanceof ServerPlayer:registered.get(entity.getUUID())==entity)?null:entity);}
    Image capture(DamageSource value){
        if(value==null)return null;var image=images.get(value);if(image!=null)return image;
        image=new Image(value,ref(value.getDirectEntity()),ref(value.getEntity()),capture(LbcIndirectKills.checkpointWrappedSource(value)));images.put(value,image);return image;
    }
    static Entity resolve(Ref ref,Function<UUID,Entity> lookup,Set<UUID> pending){
        if(ref==null)return null;if(ref.historical!=null)return ref.historical;var current=lookup.apply(ref.id);if(current==null)pending.add(ref.id);return current;
    }
    void missing(Function<UUID,Entity> lookup,Set<UUID> pending){for(var image:images.values()){resolve(image.direct,lookup,pending);resolve(image.cause,lookup,pending);}}
    void restore(Function<UUID,Entity> lookup,Set<UUID> pending){
        for(var image:images.values()){var access=(CombatCheckpointAccess.Source)image.original;access.skywars2$direct(resolve(image.direct,lookup,pending));access.skywars2$cause(resolve(image.cause,lookup,pending));}
    }
    static DamageSource value(Image image){return image==null?null:image.original;}
}
