package me.skywars2.game;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Private restore data only. Retained real logout bodies are never registered, revived or simulated. */
final class MatchOfflinePlayers {
    private record Scope(GameController game,Map<UUID,ServerPlayer> bodies){}
    private static final ThreadLocal<Scope> CURRENT=new ThreadLocal<>();
    private MatchOfflinePlayers(){}
    static <T>T using(GameController game,Map<UUID,ServerPlayer> bodies,Supplier<T> action){
        var old=CURRENT.get();CURRENT.set(new Scope(game,bodies));
        try{return action.get();}finally{if(old==null)CURRENT.remove();else CURRENT.set(old);}
    }
    static ServerPlayer find(UUID id){
        var scope=CURRENT.get();return scope==null?null:scope.bodies.get(id);
    }
    static boolean retained(Entity body){
        var scope=CURRENT.get();return body instanceof ServerPlayer player&&scope!=null
                &&scope.bodies.get(player.getUUID())==player&&scope.game.server().getPlayerList().getPlayer(player.getUUID())==null;
    }
    static boolean alias(Entity body){
        var scope=CURRENT.get();return body instanceof ServerPlayer player&&scope!=null
                &&scope.bodies.get(player.getUUID())==player&&scope.game.server().getPlayerList().getPlayer(player.getUUID())!=null
                &&scope.game.server().getPlayerList().getPlayer(player.getUUID())!=player;
    }
    static List<ServerPlayer> inventoryBodies(){
        var scope=CURRENT.get();return scope==null?List.of():scope.bodies.values().stream().filter(body->scope.game.server().getPlayerList().getPlayer(body.getUUID())!=body).toList();
    }
    static List<ServerPlayer> aliases(){
        var scope=CURRENT.get();return scope==null?List.of():scope.bodies.values().stream().filter(MatchOfflinePlayers::alias).toList();
    }
}
