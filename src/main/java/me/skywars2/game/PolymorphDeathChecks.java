package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;

/** A real remaining-life death/respawn scenario, not a scoped fake death. */
public final class PolymorphDeathChecks {
    private PolymorphDeathChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Polymorph death: "+why);}
    public static void run(GameController game){
        var players=game.alivePlayers();require(game.playing()&&players.size()==2,"two live native match participants");
        var author=players.getFirst();var player=players.getLast();var service=game.items().polymorph();
        require(game.allowDamage(author)&&game.allowDamage(player)&&game.lives(player.getUUID())>=2&&service.count()==0,"clean ordinary remaining-life scenario");
        int lives=game.lives(player.getUUID()),deaths=game.players().get(player.getUUID()).deaths,kills=game.players().get(author.getUUID()).kills;
        var body=service.transform(player,game.items().indirectKills().wildCause(author));
        try{
            require(body!=null&&player.getHealth()==8&&player.getMaxHealth()==8,"native participant starts with actual sheep HP");
            require(body.hurtServer(player.level(),player.damageSources().generic(),2)&&body.getHealth()==6&&player.getHealth()==6,"accepted body damage is immediately visible on the human HP bar");
            body.setInvulnerableTime(0);body.damageCooldownTime=0;
            require(body.hurtServer(player.level(),player.damageSources().playerAttack(author),100),"native lethal body damage accepted");
            require(body.isRemoved()&&!service.active(player)&&player.isDeadOrDying()&&player.getHealth()==0
                    &&game.awaitingRespawn(player.getUUID())&&game.lives(player.getUUID())==lives-1
                    &&game.players().get(player.getUUID()).deaths==deaths+1&&game.players().get(author.getUUID()).kills==kills+1,
                    "one sheep death spends exactly one native life, records one attributed kill and leaves zero player HP");
            player.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
            player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            var replacement=game.server().getPlayerList().getPlayer(player.getUUID());
            require(replacement!=null&&replacement!=player&&replacement.isAlive()&&game.allowDamage(replacement)
                    &&!game.awaitingRespawn(player.getUUID())&&game.lives(player.getUUID())==lives-1&&!service.active(replacement)
                    &&game.alivePlayers().size()==2&&game.playing()&&game.stage()!=GameController.Stage.ENDING,
                    "ordinary replacement-player respawn retains the participant and continuing two-player match");
        }finally{service.forget(player);}
        SkyWars2.LOGGER.info("[check] PASS polymorph-death (immediate sheep/player HP, one native attributed death/life, actual PERFORM_RESPAWN replacement and continuing match)");
    }
}
