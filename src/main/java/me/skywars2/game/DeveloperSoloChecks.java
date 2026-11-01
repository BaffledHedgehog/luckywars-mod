package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import java.util.UUID;

/** Dedicated child-only fixture: actual one-player start, lethal native damage and replacement body. */
public final class DeveloperSoloChecks {
    private static Fixture prepared;
    private static Fixture running;
    private DeveloperSoloChecks(){}
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException("Developer solo: "+message);}

    /** The caller starts the match normally after this lobby preparation succeeds. */
    public static void prepare(GameController game){
        require(prepared==null&&running==null,"exclusive dedicated solo fixture");
        var alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        var beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
        require(!game.playing()&&alpha!=null&&GameController.operator(alpha),"operator Alpha in lobby before real start");
        var fixture=new Fixture(game,alpha);prepared=fixture;
        try{
            if(beta!=null)game.server().getPlayerList().remove(beta);
            require(game.server().getPlayerList().getPlayers().size()==1&&game.server().getPlayerList().getPlayer(alpha.getUUID())==alpha,"Beta left through actual PlayerList removal");
            game.developerMode(alpha,true);
            require(game.developerMode()&&GameConfig.load().developerMode,"solo practice enabled and persisted before normal start");
            SkyWars2.LOGGER.info("[check] PASS developer-solo-menu");
        }catch(RuntimeException|Error failure){prepared=null;fixture.close();throw failure;}
    }

    public static void run(GameController game){
        require(running==null&&prepared!=null&&prepared.game==game,"dedicated prepared solo start");
        var fixture=prepared;prepared=null;running=fixture;
        try{fixture.kill();}catch(RuntimeException|Error failure){running=null;fixture.close();throw failure;}
    }

    public static void tick(GameController game){
        if(running==null)return;
        var fixture=running;
        try{if(fixture.step()){running=null;fixture.close();SkyWars2.LOGGER.info("[check] PASS developer-solo");}}
        catch(RuntimeException|Error failure){running=null;fixture.close();throw failure;}
    }

    private static final class Fixture {
        final GameController game;
        final UUID id;
        final boolean originalMode,originalTag,frozen;
        ServerPlayer retired,replacement;
        int lives,deaths,clock,ticks,phase;
        Fixture(GameController game,ServerPlayer alpha){this.game=game;id=alpha.getUUID();originalMode=game.developerMode();originalTag=alpha.entityTags().contains("developer");frozen=game.server().tickRateManager().isFrozen();}
        void kill(){
            retired=game.server().getPlayerList().getPlayer(id);
            require(game.playing()&&game.developerMode()&&game.server().getPlayerList().getPlayers().size()==1,"normal start reached an actual solo playing match");
            require(game.alivePlayers().size()==1&&game.alivePlayers().getFirst()==retired&&game.allowDamage(retired),"sole participant is genuinely active");
            lives=game.lives(id);deaths=game.players().get(id).deaths;
            require(lives>0,"solo participant has native lives");
            retired.setGameMode(GameType.SURVIVAL);retired.setInvulnerableTime(0);
            require(retired.hurtServer(retired.level(),retired.damageSources().genericKill(),10000),"actual lethal native damage accepted");
            require(retired.isDeadOrDying()&&game.awaitingRespawn(id),"actual death hook entered participant respawn boundary");
            require(game.lives(id)==lives&&game.players().get(id).deaths==deaths,"actual practice death preserves lives and persistent death count");
            game.server().tickRateManager().setFrozen(false);
        }
        boolean step(){
            require(++ticks<200,"bounded native solo death/respawn test");require(game.playing()&&game.developerMode(),"solo match remains active across death and respawn");
            if(phase==0){
                replacement=game.server().getPlayerList().respawn(retired,false,Entity.RemovalReason.KILLED);
                require(replacement!=retired&&replacement==game.server().getPlayerList().getPlayer(id),"PlayerList registered a genuinely new native player body");
                require(replacement.getUUID().equals(id)&&replacement.isAlive()&&!replacement.isDeadOrDying(),"replacement keeps identity and returns alive");
                replacement.connection.resetPosition();
                // Socketless fixture acknowledges native respawn/teleport packets; lifecycle hooks are untouched.
                var acknowledgement=(MatchPlayerCheckpoint.SimulatedAcknowledgement)replacement.connection;
                acknowledgement.skywars2$beginRespawn();acknowledgement.skywars2$acknowledgeRestore();
                require(!game.awaitingRespawn(id)&&game.allowDamage(replacement)&&!game.isObserver(replacement),"AFTER_RESPAWN restored the real participant without manual callbacks");
                require(replacement.gameMode()==GameType.SURVIVAL&&game.lives(id)==lives,"native respawn resets game mode and keeps practice lives");
                require(game.players().get(id).deaths==deaths,"replacement respawn does not alter persistent death accounting");
                clock=game.matchTicks();phase=1;return false;
            }
            require(game.server().getPlayerList().getPlayer(id)==replacement&&game.alivePlayers().size()==1&&game.allowDamage(replacement),"same replacement is the sole active participant");
            if(game.matchTicks()-clock<20)return false;
            require(game.lives(id)==lives&&replacement.isAlive(),"twenty natural solo match ticks retain live participant and lives");
            SkyWars2.LOGGER.info("[check] developer-solo native replacement oldEntity={} newEntity={} lives={} naturalTicks={}",retired.getId(),replacement.getId(),lives,game.matchTicks()-clock);return true;
        }
        void close(){
            game.config().developerMode=originalMode;game.config().save();
            var current=game.server().getPlayerList().getPlayer(id);if(current!=null){if(originalTag)current.entityTags().add("developer");else current.entityTags().remove("developer");}
            game.server().tickRateManager().setFrozen(frozen);
        }
    }
}
