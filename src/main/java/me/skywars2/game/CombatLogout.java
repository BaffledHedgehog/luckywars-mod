package me.skywars2.game;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/** Synchronous native death at the actual player-leave boundary, before player data is saved. */
public final class CombatLogout {
    private static final Map<ServerPlayer,UUID> active=new IdentityHashMap<>();
    private CombatLogout() { }
    public static boolean active(ServerPlayer player){return active.containsKey(player);}
    static UUID killer(ServerPlayer player){return active.get(player);}
    static void forfeit(GameController game,ServerPlayer player){
        if(active(player)||!game.allowDamage(player)||!player.isAlive()||player.isRemoved())return;
        var hit=game.items().indirectKills().logoutHit(player);if(hit==null)return;
        active.put(player,hit.player());
        try{
            game.items().indirectKills().settleLogoutHit(player,hit);
            // Return the cursor/crafting inputs before the ordinary native inventory drop.
            player.closeContainer();
            player.setHealth(0);
            player.die(hit.source());
        }finally{active.remove(player);}
    }
}
