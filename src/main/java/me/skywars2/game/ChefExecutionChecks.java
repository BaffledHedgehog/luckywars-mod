package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Approved execution policy on actual native Chefs; natural ticks own delayed-death and Time Stop. */
public final class ChefExecutionChecks {
    private ChefExecutionChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Chef execution: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ChefChecks.Fixture base;final ServerLevel level;final ShishEncounterChecks.Client client;
        LbcWtf.Chef delayed;boolean stopped,closed;int stage,at,start;
        Fixture(GameController game){this.game=game;base=new ChefChecks.Fixture(game);level=base.owner.level();client=new ShishEncounterChecks.Client(game,base.owner);}
        void initialize(){base.init();game.items().mana().noMagic(base.owner,0);
            var target=base.cow(base.at.add(0,0,6));var chef=base.spawn(base.owner,target);
            require(chef.hurtServer(level,chef.damageSources().generic(),7)&&Math.abs(base.service.damageCount(chef)-7)<.001,"seven real accepted native damage before sacrifice");
            base.owner.removeEffect(MobEffects.RESISTANCE);game.items().acidCorruption().sacrifice(base.owner);
            var resistance=base.owner.getEffect(MobEffects.RESISTANCE);
            require(!chef.isRemoved()&&base.service.stasis(chef)==200&&resistance!=null&&resistance.getDuration()==20&&resistance.getAmplifier()==4,"sacrifice knocks owned Chef and counts remaining13, not full20/infinite damage");
            base.owner.removeEffect(MobEffects.RESISTANCE);game.items().acidCorruption().sacrifice(base.owner);require(!base.owner.hasEffect(MobEffects.RESISTANCE)&&!chef.isRemoved(),"already-knocked Chef gives no repeated donation");base.clear();
            // These are the exact existing generic-kill boundaries retained by the nuclear cleanup and lethal Stand roll.
            var nuclear=base.spawn(base.other,target);require(game.items().indirectKills().inheritedKill(nuclear,game.items().indirectKills().wildCause(base.owner))&&!nuclear.isRemoved()&&base.service.stasis(nuclear)==200,"nuclear generic-kill boundary only knocks Chef");base.clear();
            var stand=base.spawn(base.other,target);require(game.items().stand().resolveArrowHit(stand,base.owner,1,false)&&!stand.isRemoved()&&base.service.stasis(stand)==200,"actual lethal Stand Arrow roll only knocks Chef");base.clear();
            require(game.items().apocalypseEvents().pendingSnapshot().isEmpty()&&game.items().apocalypseActors().count()==0,"no borrowed apocalypse schedule/actors");
            var apocalypse=base.spawn(base.owner,target);try{
                var method=LbcApocalypseEvents.class.getDeclaredMethod("phase",LbcApocalypseEvents.Authority.class,LbcApocalypseEvents.Kind.class,int.class,net.minecraft.commands.CommandSourceStack.class);method.setAccessible(true);
                var a=new LbcApocalypseEvents.Authority(base.owner.getUUID(),game.items().indirectKills().wildCause(base.owner),game.items().epoch());
                method.invoke(game.items().apocalypseEvents(),a,LbcApocalypseEvents.Kind.END,54,game.server().createCommandSourceStack().withLevel(level).withPosition(base.at));
                require(apocalypse.isRemoved()&&base.service.count()==0&&base.service.pinnedChunks()==0,"actual End54 random native selector includes and erases Chef");
            }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}finally{game.items().apocalypseEvents().stop();game.items().apocalypseActors().stop();}
            target=base.cow(base.at.add(0,0,6));delayed=base.spawn(base.owner,target);delayed.receive(20);game.items().acidCorruption().applyDeath(base.owner,delayed,1);
            require(game.items().acidCorruption().deathRemaining(delayed)==1,"actual shared delayed-death status attached to knocked Chef");
            game.items().timeStop().begin(level,100);stopped=true;at=start=game.server().getTickCount();
        }
        boolean step(){client.ack();int now=game.server().getTickCount();require(now-start<60,"bounded natural delayed-death fixture");
            if(stage==0){require(!delayed.isRemoved()&&game.items().acidCorruption().deathRemaining(delayed)==1,"real Time Stop pauses delayed execution");if(now-at<5)return false;game.items().timeStop().end(level);stopped=false;stage=1;at=now;return false;}
            if(!delayed.isRemoved()){require(now-at<25,"next real source-second execution");return false;}
            require(base.service.count()==0&&base.service.pinnedChunks()==0,"delayed execution retires native Chef and tickets");return true;
        }
        public void close(){if(closed)return;closed=true;try{if(stopped)game.items().timeStop().end(level);base.close();}finally{client.close();}}
    }
    private static Fixture running;
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.initialize();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}throw error;}SkyWars2.LOGGER.info("[check] START chef-execution");}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS chef-execution (native remaining13 sacrifice/zero repeated donation, nuclear generic-kill survival boundary, real lethal Stand roll, actual End54 source selector/execution and native delayed-death/TimeStop; scoped snapshot restoration)");}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL chef-execution",error);}}
}
