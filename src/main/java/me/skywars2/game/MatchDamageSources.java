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
    /** Native-link policy only; private spell/job Links deliberately resolve outside this scope. */
    private static final ThreadLocal<Set<UUID>> ABSENT=new ThreadLocal<>();
    static void withAbsent(Set<UUID> absent,Runnable action){
        var previous=ABSENT.get();ABSENT.set(Set.copyOf(absent));
        try{action.run();}finally{if(previous==null)ABSENT.remove();else ABSENT.set(previous);}
    }
    static boolean intentionallyAbsent(UUID id){var absent=ABSENT.get();return absent!=null&&absent.contains(id);}

    record Ref(UUID id,Entity historical) { }
    record Image(DamageSource original,Ref direct,Ref cause,Image wrapped) { }
    private final Map<UUID,Entity> registered=new LinkedHashMap<>();
    private final Map<DamageSource,Image> images=new IdentityHashMap<>();
    private final Set<UUID> omitted;
    MatchDamageSources(GameController game,Collection<? extends Entity> actors){this(game,actors,Set.of());}
    MatchDamageSources(GameController game,Collection<? extends Entity> actors,Set<UUID> omitted){
        this.omitted=Set.copyOf(omitted);
        for(var actor:actors)if(!omitted.contains(actor.getUUID()))registered.put(actor.getUUID(),actor);
        for(var player:game.server().getPlayerList().getPlayers())registered.put(player.getUUID(),player);
    }
    boolean omitted(Entity entity){return entity!=null&&omitted.contains(entity.getUUID());}
    Ref ref(Entity entity){return entity==null?null:new Ref(entity.getUUID(),((entity instanceof ServerPlayer?registered.get(entity.getUUID()) instanceof ServerPlayer:registered.get(entity.getUUID())==entity)||MatchOfflinePlayers.retained(entity))?null:entity);}
    /** Ledger/victim keys follow a captured body; an older body with the same UUID stays historical. */
    Ref bodyRef(Entity entity){return entity==null?null:new Ref(entity.getUUID(),(registered.get(entity.getUUID())==entity||MatchOfflinePlayers.alias(entity)||MatchOfflinePlayers.retained(entity))?null:entity);}
    Image capture(DamageSource value){
        if(value==null)return null;var image=images.get(value);if(image!=null)return image;
        image=new Image(value,ref(value.getDirectEntity()),ref(value.getEntity()),capture(parent(value)));images.put(value,image);return image;
    }
    private static DamageSource parent(DamageSource source){var accepted=LbcIndirectKills.checkpointWrappedSource(source);return accepted!=null?accepted:LbcWitchStatistics.checkpointWrappedSource(source);}
    static Entity resolve(Ref ref,Function<UUID,Entity> lookup,Set<UUID> pending){
        if(ref==null)return null;if(ref.historical!=null)return ref.historical;var current=lookup.apply(ref.id);if(current==null&&!intentionallyAbsent(ref.id)){current=MatchOfflinePlayers.find(ref.id);if(current==null)pending.add(ref.id);}return current;
    }
    /** Native EntityReference may already contain only an unresolved UUID at the save point. */
    static Entity resolveNative(UUID id,boolean resolvedAtCapture,Function<UUID,Entity> lookup,Set<UUID> pending){
        if(id==null)return null;var current=lookup.apply(id);
        if(current==null&&resolvedAtCapture&&!intentionallyAbsent(id))pending.add(id);
        return current;
    }
    void missing(Function<UUID,Entity> lookup,Set<UUID> pending){for(var image:images.values()){resolve(image.direct,lookup,pending);resolve(image.cause,lookup,pending);}}
    void restore(Function<UUID,Entity> lookup,Set<UUID> pending){
        for(var image:images.values()){var access=(CombatCheckpointAccess.Source)image.original;access.skywars2$direct(resolve(image.direct,lookup,pending));access.skywars2$cause(resolve(image.cause,lookup,pending));}
    }
    /** Real-login body replacement only; never rewind other actors or replay accepted damage. */
    void rebindPlayers(Map<UUID,ServerPlayer> players){
        for(var image:images.values()){
            var access=(CombatCheckpointAccess.Source)image.original;
            if(image.direct!=null&&image.direct.historical==null&&players.containsKey(image.direct.id))access.skywars2$direct(players.get(image.direct.id));
            if(image.cause!=null&&image.cause.historical==null&&players.containsKey(image.cause.id))access.skywars2$cause(players.get(image.cause.id));
        }
    }
    static DamageSource value(Image image){return image==null?null:image.original;}
}
