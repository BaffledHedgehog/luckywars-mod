package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;

/** Complete the accepted movement before jump abilities may teleport or change velocity. */
public final class GroundJumpEvents {
    private GroundJumpEvents(){}
    private static final class Pending {
        final ServerPlayer player;boolean jumped;
        Pending(ServerPlayer player){this.player=player;}
    }
    private static final ThreadLocal<Pending> movement=new ThreadLocal<>();
    public static void afterMovement(ServerPlayer player,Runnable action){
        var previous=movement.get();var pending=new Pending(player);movement.set(pending);
        try{action.run();}finally{if(previous==null)movement.remove();else movement.set(previous);}
        if(pending.jumped)dispatch(player);
    }
    public static void jumped(ServerPlayer player){
        var pending=movement.get();
        if(pending!=null&&pending.player==player)pending.jumped=true;
        else dispatch(player);
    }
    private static void dispatch(ServerPlayer player){
        effects(player);LbcTmtTriggers.input(player,LbcTmtTriggers.Input.JUMP);
    }
    static void effects(ServerPlayer player){
        var game=SkyWars2.game();if(game==null)return;
        game.items().halloweenWind().jump(player);
        game.items().soulhunter().jump(player);
        game.items().mana().overload().armor().jump(player);
        game.items().jumpArtifacts().jump(player);
        game.items().shulkerArmor().jump(player);
        game.items().armorSets().techno().jump(player);
        game.items().armorSets().chaos().jump(player);
    }
}
