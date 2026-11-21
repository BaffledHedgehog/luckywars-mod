package me.skywars2.game;

import eu.pb4.sgui.api.SguiUtils;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import java.util.*;

/** Actual practice death/respawn, debug selection and twenty naturally advancing match ticks. */
public final class DeveloperModeChecks {
    private static Fixture running;
    private DeveloperModeChecks(){}
    private static void require(boolean ok,String text){if(!ok)throw new IllegalStateException("Developer mode: "+text);}
    public static void run(GameController game){require(running==null,"exclusive fixture");var f=new Fixture(game);running=f;try{f.start();}catch(RuntimeException|Error e){running=null;f.close();throw e;}}
    public static void tick(GameController game){if(running==null)return;var f=running;try{if(f.tick()){running=null;f.close();SkyWars2.LOGGER.info("[check] PASS developer-mode");}}catch(RuntimeException|Error e){running=null;f.close();throw e;}}
    private static final class Fixture {
        final GameController game;final ServerPlayer alpha,beta;final GameChecks.WandThirdPlayerState a,b;
        final GameController.Checkpoint core;final boolean enabled,frozen;final int kit,skill,deaths,kills,coins;
        int clock,ticks;
        Fixture(GameController game){
            this.game=game;alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing()&&alpha!=null&&beta!=null&&GameController.operator(alpha)&&!GameController.operator(beta),"active operator and nonoperator fixture participants");
            require(!game.isObserver(alpha)&&!game.isObserver(beta),"both fixtures initially participate");
            a=new GameChecks.WandThirdPlayerState(alpha,game.items().mana());b=new GameChecks.WandThirdPlayerState(beta,game.items().mana());core=game.checkpointCore();
            enabled=game.developerMode();frozen=game.server().tickRateManager().isFrozen();var profile=game.players().get(alpha.getUUID());kit=profile.kit;skill=profile.skill;deaths=profile.deaths;kills=profile.kills;coins=profile.coins;
        }
        void start(){
            game.developerMode(alpha,true);require(game.developerMode()&&GameConfig.load().developerMode,"practice switch survives config reload");
            game.developerMode(beta,false);require(game.developerMode(),"nonoperator cannot disable global practice");
            alpha.setGameMode(GameType.SURVIVAL);int lives=game.lives(alpha.getUUID());
            game.died(alpha,alpha.damageSources().generic());require(game.lives(alpha.getUUID())==lives&&!game.allowDamage(alpha),"practice death keeps lives and enters respawn boundary");
            require(game.players().get(alpha.getUUID()).deaths==deaths,"practice death never farms persistent deaths");
            game.respawned(alpha);require(game.allowDamage(alpha)&&alpha.gameMode()==GameType.SURVIVAL&&game.lives(alpha.getUUID())==lives,"native respawn returns same participant with unchanged lives");
            var betaMenu=beta.containerMenu;game.menus().openDebug(beta);require(beta.containerMenu==betaMenu&&!game.debugPerks(beta),"debug menu remains operator-only");
            game.menus().openDebug(alpha);require(game.debugPerks(alpha)&&game.menus().debugAccess(alpha),"actual debug context opens during match");
            click(alpha,12);click(alpha,0);require(game.players().get(alpha.getUUID()).kit==1,"real debug kit button changes current profile");
            require(!alpha.getInventory().isEmpty(),"real debug kit selection grants native kit immediately");alpha.closeContainer();
            game.spectators().enter(beta);for(var other:game.alivePlayers())require(other==alpha,"fixture leaves exactly one eligible player");
            clock=game.matchTicks();game.server().tickRateManager().setFrozen(false);
        }
        boolean tick(){require(game.playing(),"global practice prevents automatic last-player completion");if(++ticks>200)throw new IllegalStateException("Developer mode: natural tick timeout");if(game.matchTicks()-clock<20)return false;require(game.allowDamage(alpha)&&game.isObserver(beta),"single eligible participant remains active after twenty match ticks");return true;}
        void close(){
            game.server().tickRateManager().setFrozen(true);
            try{game.spectators().remove(beta);game.restoreCore(core);game.config().developerMode=enabled;game.config().save();var p=game.players().get(alpha.getUUID());p.kit=kit;p.skill=skill;p.deaths=deaths;p.kills=kills;p.coins=coins;a.restore(game);b.restore(game);game.players().save();}
            finally{game.server().tickRateManager().setFrozen(frozen);}
        }
        static void click(ServerPlayer player,int slot){require(SguiUtils.getCurrentGui(player) instanceof SimpleGui,"native menu before click");var gui=(SimpleGui)SguiUtils.getCurrentGui(player);var button=gui.getGuiElement(slot);require(button!=null,"native menu slot "+slot);button.getGuiCallback().click(slot,eu.pb4.sgui.api.ClickType.MOUSE_LEFT,net.minecraft.world.inventory.ContainerInput.PICKUP,gui);}
    }
}
