package me.skywars2.game;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;

/** Native player-side actor ledgers. Bodies and UUID relationships have separate owners. */
final class MatchPlayerLinks {
    private record Image(ServerPlayer original,Set<UUID> pearls,UUID fishing) { }
    private final GameController game;
    private final Map<UUID,Image> images=new LinkedHashMap<>();
    private final Set<ServerPlayer> bodies=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    MatchPlayerLinks(GameController game,Function<UUID,Entity> actors) {
        this.game=game;
        for(var player:game.server().getPlayerList().getPlayers()) {
            var pearls=new LinkedHashSet<UUID>();
            for(var pearl:player.getEnderPearls()) {
                require(actors.apply(pearl.getUUID())==pearl&&!pearl.isRemoved()&&pearl.getOwner()==player,
                        "Capture requires a registered pearl belonging to its native player ledger");
                pearls.add(pearl.getUUID());
            }
            var hook=player.fishing;
            if(hook!=null)require(actors.apply(hook.getUUID())==hook&&!hook.isRemoved()&&hook.getPlayerOwner()==player,
                    "Capture requires a registered native fishing hook");
            images.put(player.getUUID(),new Image(player,Set.copyOf(pearls),hook==null?null:hook.getUUID()));
            bodies.add(player);
        }
    }
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    /** Clear old removed player bodies as well as current sessions and later newcomers. */
    void clear() {
        bodies.addAll(game.server().getPlayerList().getPlayers());
        for(var player:bodies)clear(player);
    }
    private static void clear(ServerPlayer player){player.getEnderPearls().clear();player.fishing=null;}
    void restore(Function<UUID,Entity> actors,Set<UUID> pending) {
        clear();
        for(var entry:images.entrySet()) {
            var image=entry.getValue();var player=game.server().getPlayerList().getPlayer(entry.getKey());
            if(player==null){if(!image.pearls.isEmpty()||image.fishing!=null)pending.add(entry.getKey());continue;}
            for(var id:image.pearls) {
                var actor=actors.apply(id);
                require(actor instanceof ThrownEnderpearl,"Restore actor bodies before native pearl ledgers");
                var pearl=(ThrownEnderpearl)actor;
                require(pearl.getOwner()==player,"Restore pearl owners before native player ledgers");
                player.registerEnderPearl(pearl);
            }
            if(image.fishing!=null) {
                var actor=actors.apply(image.fishing);
                require(actor instanceof FishingHook&&((FishingHook)actor).getPlayerOwner()==player,
                        "Restore fishing owner before native player ledger");
                player.fishing=(FishingHook)actor;
            }
        }
    }
}
