package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/** Original scheduler on actual server ticks, including a genuine creator logout. */
public final class SauvojenLifecycleChecks {
    private SauvojenLifecycleChecks(){}
    private static Fixture running;
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException("Sauvojen lifecycle: "+detail);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final SauvojenBossChecks.Fixture base;
        final ServerPlayer creator;
        final ServerLevel level;
        final Vec3 ownerPoint,betaPoint,bossPoint;
        final long initialSelections;
        int ticks,phase,lastTick,lastSelectionTick=-1,pauseTick,logoutTick;
        long selections,pauseSelections,pauseEmissions,logoutSelections,logoutEmissions;
        int pauseClouds;
        boolean stopped;
        Fixture(GameController game){
            this.game=game;level=game.maps().level();
            creator=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestGamma")));
            require(game.canUseGameplayItems(creator)&&!game.items().timeStop().stopped(game.maps().level()),"third real participant and no borrowed time stop");
            base=new SauvojenBossChecks.Fixture(game);
            try{base.activation(creator);
            ownerPoint=base.owner.position();betaPoint=base.beta.position();bossPoint=base.boss.position();
            for(var player:List.of(base.owner,base.beta)){
                player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);player.setHealth(player.getMaxHealth());
                player.getInventory().clearContent();
            }
            initialSelections=selections=game.items().sauvojenBoss().selections();lastTick=game.server().getTickCount();
            }catch(Throwable error){try{base.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}
        }
        boolean step(){
            int now=game.server().getTickCount();if(now==lastTick)return false;lastTick=now;
            require(++ticks<=700&&game.playing(),"bounded actual match, phase="+phase);
            var service=game.items().sauvojenBoss();
            require(!base.boss.isRemoved()&&base.boss.isAlive()&&service.count()==1,"independent native boss remains alive");
            // Keep the test targets alive and resident; do not drive the spell or boss clocks.
            maintain(base.owner,ownerPoint);maintain(base.beta,betaPoint);
            base.boss.setPos(bossPoint);base.boss.setDeltaMovement(Vec3.ZERO);
            if(phase==0){
                long current=service.selections();
                if(current!=selections){
                    require(current==selections+1,"one selection per scheduler boundary");
                    if(lastSelectionTick>=0)require(now-lastSelectionTick==40,"original 40-tick selection cadence");
                    lastSelectionTick=now;selections=current;
                }
                if(current-initialSelections<2||service.cloudCount()==0||!service.captureReady())return false;
                replayClouds();
                pauseSelections=service.selections();pauseEmissions=service.emissions();pauseClouds=service.cloudCount();pauseTick=now;
                game.items().timeStop().begin(level,200);stopped=true;phase=1;return false;
            }
            if(phase==1){
                require(service.selections()==pauseSelections&&service.emissions()==pauseEmissions&&service.cloudCount()==pauseClouds,
                        "TimeStop pauses actual selection, emission and cloud lifetime");
                if(now-pauseTick<20)return false;
                game.items().timeStop().end(level);stopped=false;
                creator.connection.onDisconnect(new DisconnectionDetails(Component.literal("Sauvojen creator lifecycle check")));
                require(game.server().getPlayerList().getPlayer(creator.getUUID())==null&&service.owner(base.boss)==null,"real native creator departure");
                require(service.cloudCount()==pauseClouds&&!base.boss.isRemoved(),"logout leaves boss and active clouds intact");
                logoutSelections=service.selections();logoutEmissions=service.emissions();logoutTick=now;phase=2;return false;
            }
            if(now-logoutTick<80||!service.captureReady())return false;
            require(service.selections()>=logoutSelections+2&&service.emissions()>logoutEmissions,"real scheduler and cloud attacks continue without creator");
            require(game.items().spellSources().source(base.boss).author()==null,"no player authority is recaptured after logout");
            replayClouds();
            return true;
        }
        void maintain(ServerPlayer player,Vec3 point){
            require(player.isAlive()&&game.canUseGameplayItems(player),"actual remaining participant stays eligible");
            player.setHealth(player.getMaxHealth());player.setDeltaMovement(Vec3.ZERO);player.setNoGravity(true);
            if(player.level()!=level||player.position().distanceToSqr(point)>1e-6)
                player.teleportTo(level,point.x,point.y,point.z,Set.of(),0,0,true);
        }
        void replayClouds(){
            var service=game.items().sauvojenBoss();
            require(service.captureReady()&&service.cloudCount()>0,"live cloud and drained native terrain work at capture boundary");
            var entities=new ArrayList<Entity>();level.getAllEntities().forEach(entities::add);
            var links=new MatchSpellCheckpoint.Links(new MatchDamageSources(game,entities));
            var picture=service.checkpoint(links);int count=service.cloudCount();long shots=service.emissions(),choices=service.selections();
            require(links.resolve(id->{
                var player=game.server().getPlayerList().getPlayer(id);if(player!=null)return player;
                for(var world:game.server().getAllLevels()){var entity=world.getEntity(id);if(entity!=null)return entity;}
                return null;
            }).isEmpty(),"native cloud links resolve without borrowed creator");
            service.prepareFrozen();picture.prepare(links).run();service.publishFrozen();
            require(service.cloudCount()==count&&service.emissions()==shots&&service.selections()==choices,
                    "typed cloud checkpoint republishes nullable-owner visuals and exact counters");
        }
        @Override public void close(){if(stopped)game.items().timeStop().end(level);base.close();}
    }
    public static void run(GameController game){
        require(running==null,"exclusive fixture");
        running=new Fixture(game);
    }
    public static void tick(GameController game){
        if(running==null)return;
        try{if(!running.step())return;running.close();running=null;
            SkyWars2.LOGGER.info("[check] PASS sauvojen-lifecycle (real 40-tick scheduler, TimeStop, native creator disconnect, continued NPC attacks, typed nullable-owner cloud replay before and after logout)");
        }catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;
            SkyWars2.LOGGER.error("[check] FAIL sauvojen-lifecycle",error);}
    }
}
