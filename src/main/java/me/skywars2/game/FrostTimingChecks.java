package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.phys.Vec3;

/** Real global-second pulses and actual Time Stop entry path; no artificial second() calls. */
public final class FrostTimingChecks {
    private FrostTimingChecks(){}
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Frost timing: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final LbcFrost frost;final ServerPlayer owner;final List<Cow> cows=new ArrayList<>();
        Cow sword,snow,ice;int born,pulses,lastPulse=-1,previousFrozen=10,pauseAt=-1;boolean paused,closed;
        Fixture(GameController game){this.game=game;frost=game.items().frost();owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
            require(game.playing()&&owner!=null&&game.canUseGameplayItems(owner)&&!game.items().timeStop().stopped(owner.level()),"active eligible owner outside Time Stop");
            require(frost.trackedCount()==0,"exclusive empty Frost owner");born=game.server().getTickCount();}
        Cow cow(int offset){var at=new Vec3(owner.getX()+offset,Math.min(owner.level().getMaxY()-8,owner.getY()+16),owner.getZ());
            require(owner.level().getChunkSource().getChunkNow(BlockPos.containing(at).getX()>>4,BlockPos.containing(at).getZ()>>4)!=null,"resident fixture chunk");
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(at.add(-1,0,-1)),BlockPos.containing(at.add(1,3,1))))require(owner.level().getBlockState(pos).isAir(),"isolated native freeze footprint");
            var cow=new Cow(EntityTypes.COW,owner.level());cow.setPos(at);cow.setNoAi(true);cow.setNoGravity(true);
            // Native26.3 ice suffocates a frozen10HP cow before ten seconds elapse.
            // Preserve real damage, but keep this clock subject alive for the full trial.
            cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);cow.setHealth(1000);
            require(owner.level().addFreshEntity(cow),"native cow publication");cows.add(cow);return cow;}
        void init(){
            var multiplier=cow(0);multiplier.addEffect(ItemEffectLevels.create(MobEffects.SLOWNESS,200,2),owner);frost.add(multiplier,null);
            require(frost.cold(multiplier)==30&&frost.cooldown(multiplier)==5,"first source cold applies existing Slowness III multiplier");
            frost.add(multiplier,null);require(frost.cold(multiplier)==40,"later source cold adds base ten without multiplying again");frost.clear(multiplier);multiplier.discard();
            sword=cow(0);snow=cow(3);ice=cow(6);frost.add(sword,null);for(int i=0;i<5;i++)require(frost.snow(snow,owner),"snow source cold admitted");
            for(int i=0;i<10;i++)frost.add(ice,null);
            require(frost.cold(sword)==10&&frost.cooldown(sword)==5&&frost.cold(snow)==10&&frost.cooldown(snow)==2,"source sword five-second and cloud two-second cooldown units");
            require(frost.frozenSeconds(ice)==10&&frost.cold(ice)==0&&frost.cooldown(ice)==0,"100 cold freezes for ten global-second counters");
        }
        String observed(){return " tick="+game.server().getTickCount()+" born="+born+" pulses="+pulses+" lastPulse="+lastPulse+" previousFrozen="+previousFrozen+" currentFrozen="+frost.frozenSeconds(ice)+" iceHP="+ice.getHealth()+" iceAlive="+ice.isAlive()+" removed="+ice.isRemoved()+" inWall="+ice.isInWall()+" sword="+frost.cold(sword)+"/"+frost.cooldown(sword)+" snow="+frost.cold(snow)+"/"+frost.cooldown(snow);}
        boolean tick(){int now=game.server().getTickCount();require(now-born<280,"bounded eleven-second native timing fixture"+observed());
            require(sword.isAlive()&&!sword.isRemoved()&&snow.isAlive()&&!snow.isRemoved()&&ice.isAlive()&&!ice.isRemoved(),"native clock subjects remain alive and loaded throughout trial"+observed());
            if(paused){require(frost.frozenSeconds(ice)==previousFrozen&&frost.cold(sword)==10&&frost.cooldown(sword)==4,"shared Time Stop entry pauses cold and frozen counters"+observed());
                if(now-pauseAt<25)return false;game.items().timeStop().end(owner.level());paused=false;lastPulse=-1;return false;}
            int current=frost.frozenSeconds(ice);if(current==previousFrozen)return false;
            require(current==previousFrozen-1,"native global pulse subtracts exactly one frozen second"+observed());
            if(lastPulse>=0)require(now-lastPulse==20,"consecutive native second pulses are exactly twenty server ticks apart"+observed());lastPulse=now;previousFrozen=current;pulses++;
            int expectedSword=10-Math.max(0,pulses-4),expectedSnow=10-Math.max(0,pulses-1);
            require(frost.cooldown(sword)==Math.max(0,5-pulses)&&frost.cold(sword)==expectedSword,"five-second cooldown decrements first, cold begins decaying on fifth pulse");
            require(frost.cooldown(snow)==Math.max(0,2-pulses)&&frost.cold(snow)==expectedSnow,"two-second cooldown begins cold decay on second pulse");
            if(pulses==1){require(now-born<=20,"first global pulse uses existing phase rather than adding a new per-target second");game.items().timeStop().begin(owner.level(),200);paused=true;pauseAt=now;return false;}
            if(pulses<10)return false;
            require(!frost.isFrozen(ice)&&frost.iceBlockCount()==0,"tenth global-second pulse thaws native body and restores owned ice footprint");return true;
        }
        public void close(){if(closed)return;closed=true;if(paused)game.items().timeStop().end(owner.level());for(var cow:cows){frost.clear(cow);cow.discard();}}
    }
    public static void start(GameController game){require(running==null,"exclusive timing fixture");running=new Fixture(game);try{running.init();}catch(Throwable failure){running.close();running=null;throw failure;}SkyWars2.LOGGER.info("[check] START frost-timing");}
    public static void tick(GameController game){if(running==null)return;try{if(!running.tick())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS frost-timing (actual twenty-tick global pulses, cooldown5/2, same-pulse decay, 100 cold/frozen10, first Slowness multiplier, native ice thaw and shared Time Stop pause)");}
        catch(Throwable failure){try{running.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL frost-timing",failure);}}
}
