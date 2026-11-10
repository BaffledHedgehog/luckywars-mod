package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;

/** Source mobs_1s phase10->1, through actual native dragon ticks and TimeStop. */
public final class LegacyMobTickChecks {
    private LegacyMobTickChecks(){}
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Legacy mob ticks: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;
        EnderDragon dragon;int started,phase=-1,when;long chunk;boolean forced;
        Fixture(GameController game){this.game=game;base=new ElementalGrimoireChecks.Fixture(game,true);base.retain=true;}
        void initialize(){base.initialize();dragon=EntityTypes.ENDER_DRAGON.create(base.owner.level(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            require(dragon!=null,"native dragon factory");dragon.setPos(base.owner.position().add(0,70,0));dragon.setNoAi(true);dragon.setNoGravity(true);
            dragon.getPhaseManager().setPhase(EnderDragonPhase.HOVERING);require(base.owner.level().addFreshEntity(dragon),"native multipart dragon publication");
            chunk=net.minecraft.world.level.ChunkPos.pack(dragon.blockPosition().getX()>>4,dragon.blockPosition().getZ()>>4);
            if(!base.owner.level().getForceLoadedChunks().contains(chunk)){forced=true;base.owner.level().setChunkForced(net.minecraft.world.level.ChunkPos.getX(chunk),net.minecraft.world.level.ChunkPos.getZ(chunk),true);}
            started=when=game.server().getTickCount();}
        boolean step(){int now=game.server().getTickCount();require(now-started<200,"bounded actual three cadence boundaries");
            if(phase==-1){if(dragon.tickCount==0)return false;dragon.getPhaseManager().setPhase(EnderDragonPhase.HOVERING);game.items().timeStop().begin(base.owner.level(),100);when=now;phase=0;return false;}
            if(now==when||now%20!=0)return false;
            if(phase==0){require(dragon.getPhaseManager().getCurrentPhase().getPhase()==EnderDragonPhase.HOVERING,"TimeStop preserves source phase10 across actual second boundary");
                game.items().timeStop().end(base.owner.level());when=now;phase=1;return false;}
            if(phase==1){require(dragon.getPhaseManager().getCurrentPhase().getPhase()==EnderDragonPhase.STRAFE_PLAYER,"actual native dragon source phase10 becomes phase1 on next second; tick="+dragon.tickCount+",phase="+dragon.getPhaseManager().getCurrentPhase().getPhase()+",server="+now);
                dragon.getPhaseManager().setPhase(EnderDragonPhase.TAKEOFF);when=now;phase=2;return false;}
            require(dragon.getPhaseManager().getCurrentPhase().getPhase()==EnderDragonPhase.TAKEOFF,"source rule does not replace any non-hover phase");return true;
        }
        public void close(){game.items().timeStop().end(base.owner.level());if(dragon!=null)dragon.discard();if(forced)base.owner.level().setChunkForced(net.minecraft.world.level.ChunkPos.getX(chunk),net.minecraft.world.level.ChunkPos.getZ(chunk),false);base.close();}
    }
    public static void run(GameController game){require(running==null,"exclusive scene");var next=new Fixture(game);try{next.initialize();running=next;}catch(Throwable error){next.close();throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;
        SkyWars2.LOGGER.info("[check] PASS legacy-mob-ticks (actual native phase10->1 every20ticks; TimeStop pause and untouched non-hover phase)");
    }catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL legacy-mob-ticks",error);}}
}
