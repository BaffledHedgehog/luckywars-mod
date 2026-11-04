package me.skywars2.game;

import java.lang.reflect.Field;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;

/** Actual native life loss/replacement, rune conversion and whole-coordinator rollback. Requires three ordinary participants. */
public final class PhoenixSkillChecks {
    private PhoenixSkillChecks(){}
    private static Fixture running;
    private static void require(boolean condition,String why){if(!condition)throw new IllegalStateException("Phoenix: "+why);}
    private static void near(double actual,double expected,String why){require(Math.abs(actual-expected)<1e-5,why+" actual="+actual+" expected="+expected);}
    private static void set(Object target,String name,Object value){
        for(Class<?> type=target.getClass();type!=null;type=type.getSuperclass())try{Field f=type.getDeclaredField(name);f.setAccessible(true);f.set(target,value);return;}catch(NoSuchFieldException ignored){}catch(IllegalAccessException error){throw new IllegalStateException(error);}
        throw new IllegalStateException("Missing native fixture field "+name);
    }
    public static void run(GameController game){
        require(running==null,"exclusive fixture");var fixture=new Fixture(game);running=fixture;
        try{fixture.begin();}catch(Throwable error){running=null;try{fixture.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}
    }
    public static void tick(GameController game){
        if(running==null)return;var fixture=running;
        try{require(fixture.game==game,"same controller");if(!fixture.step())return;running=null;fixture.close();SkyWars2.LOGGER.info("[check] PASS phoenix-native (actual lethal damage/native replacement, base half, terminal1HP, rune conversion and whole-coordinator replay)");}
        catch(Throwable error){running=null;try{fixture.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL phoenix-native",error);}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final LbcPhoenixSkill service;ServerPlayer owner;final ServerPlayer killer;
        final RegenerationBeltChecks.Snapshot saved,killerSaved;RegenerationBeltChecks.ScheduledClient client;final RegenerationBeltChecks.ScheduledClient audience;
        final MatchCoreCheckpoint core;final MatchWorldCheckpoint world;final MatchWorldRandomCheckpoint random;
        final LbcPhoenixSkill.Checkpoint originalService;final boolean frozen;final int started;
        final Set<UUID> originalEntities=new HashSet<>();final UUID id;
        int stage,when;double expected;boolean closed,paused,restoring;
        Fixture(GameController game){
            this.game=game;var alive=game.alivePlayers();require(game.playing()&&!game.bedwars().mode()&&!game.developerMode()&&alive.size()>=3,"three ordinary actual participants, so terminal death cannot end the fixture match");
            owner=alive.getFirst();killer=alive.get(1);id=owner.getUUID();service=game.items().phoenix();
            require(!service.active(id)&&!game.vanity().busy()&&!game.vanity().hasSnapshot()&&!game.items().timeStop().stopped(owner.level()),"fresh skill and exclusive unpaused coordinator");
            require(game.items().stonekivi().queuedSnapshot().isEmpty(),"no unrelated accepted shared work");
            frozen=game.server().tickRateManager().isFrozen();core=new MatchCoreCheckpoint(game);world=new MatchWorldCheckpoint(game);random=new MatchWorldRandomCheckpoint(game);originalService=service.checkpoint(null);
            saved=new RegenerationBeltChecks.Snapshot(game,owner);killerSaved=new RegenerationBeltChecks.Snapshot(game,killer);started=game.server().getTickCount();
            owner.level().getAllEntities().forEach(e->originalEntities.add(e.getUUID()));client=new RegenerationBeltChecks.ScheduledClient(game,owner);audience=new RegenerationBeltChecks.ScheduledClient(game,killer);
        }
        double base(){return owner.getAttribute(Attributes.MAX_HEALTH).getBaseValue();}
        void next(int value){stage=value;when=game.server().getTickCount();}
        void begin(){
            game.server().tickRateManager().setFrozen(false);game.config().ranked=true;game.config().lbcSkills=true;game.config().lbcKits=false;game.config().lbcTrinketLimit=0;game.config().lbcLevelups=false;game.config().vanillaKits=false;game.config().vanillaSkills=false;
            for(var player:List.of(owner,killer)){
                game.lbc().clear(player);player.closeContainer();player.getInventory().clearContent();player.removeAllEffects();player.entityTags().removeAll(Set.of("skywars2.debug_perks","sin_vanity"));player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);player.setAbsorptionAmount(0);player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);player.setHealth(20);
            }
            game.players().get(id).lbcSkill=31;game.players().get(killer.getUUID()).lbcSkill=0;
            game.addRuneLives(owner,3-game.lives(id));require(game.lives(id)==3,"actual canonical initial lives owner");
            int remaining=service.start(owner,game.lives(id));require(remaining==1,"initial extra lives consumed once");game.addRuneLives(owner,remaining-game.lives(id));
            near(base(),30,"two initial extra lives become10 base HP");require(game.lives(id)==1,"only one ordinary canonical life remains");
            game.addRuneLives(owner,2);near(base(),40,"actual later rune owner grants10 base HP");require(game.lives(id)==1,"later rune grant never queues ordinary lives");
            expected=40;owner.setHealth(owner.getMaxHealth());pause();require(game.vanity().requestCapture(owner,false),"actual whole coordinator captures enrolled Phoenix and native base40");next(0);
        }
        void pause(){game.items().timeStop().begin(owner.level(),10000);paused=true;}
        void thaw(){game.items().timeStop().end(owner.level());paused=false;}
        void lethal(){
            owner.setInvulnerableTime(0);owner.damageCooldownTime=0;owner.getAbilities().invulnerable=false;owner.setAbsorptionAmount(0);
            require(owner.hurtServer(owner.level(),owner.damageSources().playerAttack(killer),10000)&&owner.isDeadOrDying(),"actual native lethal player attack");
        }
        void respawn(){
            var retired=owner;client.close();owner=game.server().getPlayerList().respawn(retired,false,Entity.RemovalReason.KILLED);require(owner!=retired&&owner.isAlive(),"genuine native replacement player");
            owner.connection.resetPosition();var acknowledgement=(MatchPlayerCheckpoint.SimulatedAcknowledgement)owner.connection;acknowledgement.skywars2$beginRespawn();acknowledgement.skywars2$acknowledgeRestore();
            set(saved,"player",owner);set(saved.state,"player",owner);client=new RegenerationBeltChecks.ScheduledClient(game,owner);owner.setNoGravity(true);owner.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        }
        boolean step(){
            if(owner.isAlive())client.acknowledge();audience.acknowledge();require(game.server().getTickCount()-started<1600,"bounded native coordinator fixture stage="+stage);
            if(restoring&&game.vanity().busy())for(var p:game.server().getPlayerList().getPlayers())if(p.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();
            if(stage==0){if(game.vanity().busy()||!game.vanity().hasSnapshot())return false;thaw();lethal();require(game.lives(id)==1&&game.awaitingRespawn(id),"Phoenix survives native death through canonical pending respawn");near(service.pendingBaseHealth(id),20,"native death stages exactly half BASE");next(1);return false;}
            if(stage==1){respawn();near(base(),20,"new native body reapplies half after loadout reset");near(owner.getHealth(),owner.getMaxHealth(),"new native body has its full reduced health");game.addRuneLives(owner,1);near(base(),25,"post-respawn positive rune grants5 base HP");require(game.lives(id)==1,"post-respawn rune remains HP");pause();require(game.vanity().requestRestoreForCheck(),"actual whole-coordinator rewind of future native death and rune");restoring=true;next(2);return false;}
            if(stage==2){
                if(game.vanity().busy())return false;
                var current=game.server().getPlayerList().getPlayer(id);require(current==owner,"whole owner retained current native replacement identity");near(base(),40,"whole native attribute image restores pre-death40 BASE");near(service.pendingBaseHealth(id),40,"whole coordinator restores canonical Phoenix state");require(game.lives(id)==1&&service.active(id),"whole coordinator restores one canonical life and enrollment");game.vanity().stop();thaw();restoring=false;expected=40;next(3);return false;
            }
            if(stage==3){
                double half=expected*.5;lethal();
                if(half<=1){require(game.lives(id)==0&&!game.canUseGameplayItems(owner)&&game.stage()==GameController.Stage.PLAYING,"<=1 BASE ends actual participant despite Phoenix; other two preserve active match");return true;}
                require(game.lives(id)==1&&game.awaitingRespawn(id),"every actual lethal event retains one canonical life while half>1");near(service.pendingBaseHealth(id),half,"each native death halves exactly once");expected=half;next(4);return false;
            }
            respawn();near(base(),expected,"fractional half survives native replacement");near(owner.getHealth(),owner.getMaxHealth(),"fractional base body heals normally");next(3);return false;
        }
        @Override public void close(){
            if(closed)return;closed=true;Throwable failure=null;
            for(Runnable action:List.<Runnable>of(
                    ()->{game.vanity().stop();if(paused)thaw();},()->{if(!owner.isAlive())respawn();},
                    ()->{game.server().tickRateManager().setFrozen(true);core.restoreFrozen();world.restoreFrozen();random.restoreFrozen();originalService.prepare(null).run();},
                    ()->{saved.restoreAttributes();killerSaved.restoreAttributes();saved.restore(game);killerSaved.restore(game);saved.restoreUse();killerSaved.restoreUse();},
                    ()->{for(var entity:owner.level().getAllEntities())if(!originalEntities.contains(entity.getUUID())&&!(entity instanceof ServerPlayer))entity.discard();game.players().save();},
                    ()->client.close(),audience::close,()->game.server().tickRateManager().setFrozen(frozen)))
                try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("Phoenix scoped native restoration",failure);
        }
    }
}
