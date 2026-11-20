package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.GameType;

/** Actual native lethal damage, spectator entry and replacement body/listener lifecycle. */
public final class DeathBodyChecks {
    private DeathBodyChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Death body: "+why);}
    public static void run(GameController game){
        ServerPlayer victim=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
        require(victim!=null&&victim.getClass()==ServerPlayer.class&&game.allowDamage(victim),"actual vanilla participant");
        var id=victim.getUUID();var connection=victim.connection;
        victim.setGameMode(GameType.SURVIVAL);victim.getAbilities().invulnerable=false;
        victim.removeAllEffects();victim.setAbsorptionAmount(0);victim.setHealth(1);victim.setInvulnerableTime(0);victim.damageCooldownTime=0;
        require(victim.hurtServer(victim.level(),victim.damageSources().fellOutOfWorld(),100000),"accepted lethal native damage");
        require(victim.getHealth()==0&&((MatchPlayerCheckpoint.SimulatedAcknowledgement)connection).skywars2$waitingForRespawn(),"native death owns the listener");
        game.spectators().enter(victim);
        require(victim.getHealth()==0&&!connection.hasClientLoaded(),"spectator preparation does not heal the dead body");
        connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
        var replacement=game.server().getPlayerList().getPlayer(id);
        require(replacement!=victim&&connection.player==replacement&&replacement.isAlive(),"native client respawn creates and binds a living replacement");
        ((MatchPlayerCheckpoint.SimulatedAcknowledgement)connection).skywars2$acknowledgeRestore();
        int deaths=replacement.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS));
        // Reproduce the earlier corrupt combination without adding another death/life debit.
        connection.markClientUnloadedAfterDeath();replacement.setHealth(20);replacement.deathTime=5;
        require(!connection.hasClientLoaded()&&replacement.getHealth()>0,"positive HP does not release the native death gate");
        game.repairHealedDeaths();
        var repaired=game.server().getPlayerList().getPlayer(id);
        require(repaired!=replacement&&connection.player==repaired&&repaired.isAlive()&&repaired.deathTime==0,"repair replaces the ghost instead of merely healing it");
        require(!((MatchPlayerCheckpoint.SimulatedAcknowledgement)connection).skywars2$waitingForRespawn(),"native respawn handshake released");
        ((MatchPlayerCheckpoint.SimulatedAcknowledgement)connection).skywars2$acknowledgeRestore();
        connection.handlePlayerInput(new ServerboundPlayerInputPacket(new Input(false,false,false,false,false,true,false)));
        require(connection.hasClientLoaded()&&repaired.isShiftKeyDown(),"actual input packet accepted on the replacement");
        require(repaired.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS))==deaths,"repair does not count a second death");
        checkCarpetDeath(game,repaired);
        SkyWars2.LOGGER.info("[check] PASS death-body (real client healed-ghost recovery, native input and no duplicate death, Carpet owns its own death)");
    }

    private static void checkCarpetDeath(GameController game,ServerPlayer reference){
        var player=CarpetBotChecks.connect(game,"TestDelta",reference.level(),reference.clientInformation());
        require(game.returnFromGameModeCommand(player,GameType.SURVIVAL),"ordinary Carpet fake participates in the round");
        player.setGameMode(GameType.SURVIVAL);player.getAbilities().invulnerable=false;
        player.hasChangedDimension();
        player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        require(player.connection.hasClientLoaded()&&game.allowDamage(player),"Carpet actor is ready for gameplay damage");
        player.removeAllEffects();player.setAbsorptionAmount(0);player.setHealth(1);
        player.setInvulnerableTime(0);player.damageCooldownTime=0;
        require(player.hurtServer(player.level(),player.damageSources().fellOutOfWorld(),100000),"accepted native Carpet lethal hit");
        var connection=(MatchPlayerCheckpoint.SimulatedAcknowledgement)player.connection;
        require(connection.skywars2$waitingForRespawn()&&player.getHealth()==20,"Carpet schedules its own disconnect with positive HP");
        game.repairHealedDeaths();
        require(game.server().getPlayerList().getPlayer(player.getUUID())==player&&player.connection.player==player,
                "real-player health repair leaves the registered Carpet body and its listener to the queued disconnect");
    }
}
