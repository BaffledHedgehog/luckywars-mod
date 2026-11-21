package me.skywars2.game;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

/** Synchronous native death at the actual player-leave boundary, before player data is saved. */
public final class CombatLogout {
    private static final Map<ServerPlayer,UUID> active=new IdentityHashMap<>();
    private CombatLogout() { }
    public static boolean active(ServerPlayer player){return active.containsKey(player);}
    public static UUID killer(ServerPlayer player){return active.get(player);}
    public static void recordAbsorbedHit(ServerPlayer player, DamageSource source) {
        var game = SkyWars2.game();
        if (game != null && game.allowDamage(player)) {
            game.items().indirectKills().recordLogoutAbsorption(player, source);
        }
    }

    public static void forfeit(GameController game,ServerPlayer player){
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
