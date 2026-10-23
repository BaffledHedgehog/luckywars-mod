package me.skywars2.game;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Global source-space hazards; independent of Antigravity storm position, age or existence. */
public final class LbcSkyHazards {
    public static final String COSMOS="skywars2.cosmos";
    private final GameController game;
    private final Set<ServerPlayer> notified=new LinkedHashSet<>();
    private int ticks;
    public LbcSkyHazards(GameController game) { this.game=game; }
    public void tick() {
        if(++ticks%10!=0) return;
        for(var player:List.copyOf(notified))
            if(player.isRemoved() || !player.isAlive() || !game.canUseGameplayItems(player)) clear(player);
        for(var player:game.server().getPlayerList().getPlayers()) pulse(player);
    }
    void pulse(ServerPlayer player) {
        String dimension=player.level().dimension().identifier().toString();
        if(!game.playing() || game.stage()==GameController.Stage.ENDING
                || !game.items().wands().validTarget(player)
                || dimension.equals("minecraft:nexus") || dimension.equals("minecraft:imprinted")) {
            clear(player); return;
        }
        if(game.items().timeStop().stopped(player.level())) return;
        // Legacy stores floor(Pos[1]*100) in y; retain its inclusive score bounds exactly.
        double yScore=Math.floor(player.getY()*100);
        if(yScore<150000 || yScore>14499000) { clear(player); return; }
        if(!player.entityTags().contains("drowning"))
            player.sendSystemMessage(Component.translatable("damage_from_no_air"));
        player.entityTags().add("drowning"); notified.add(player);
        var position=player.position(); var level=player.level();
        var author=game.items().indirectKills().altitudeAuthor(player);
        game.items().indirectKills().altitudeDamage(player,()->player.hurtServer(level,player.damageSources().drown(),4));
        if(yScore>=1500000)
            game.items().exasperato().queueCosmos(level,position,author,game.items().epoch());
    }
    public boolean wild(Entity entity) { return entity!=null && entity.entityTags().contains(COSMOS); }
    public int pendingCosmosCount() { return game.items().exasperato().pendingCosmos(); }
    public void clear(ServerPlayer player) {
        notified.remove(player);
        player.entityTags().remove("drowning");
    }
    public void stop() {
        for(var player:List.copyOf(notified)) clear(player);
        ticks=0;
    }
}
