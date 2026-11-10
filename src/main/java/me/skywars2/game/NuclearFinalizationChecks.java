package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Disposable last-life match: real fatal wave, delayed crater, then automatic victory. */
public final class NuclearFinalizationChecks {
    private NuclearFinalizationChecks(){}
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Nuclear finalization: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        final boolean developer,invulnerable;int started,thaw=-1;boolean closed;
        Fixture(GameController game){this.game=game;base=new ElementalGrimoireChecks.Fixture(game,true);base.retain=true;
            developer=game.developerMode();invulnerable=base.owner.getAbilities().invulnerable;}
        void initialize(){
            base.initialize();require(game.alivePlayers().size()==2,"two actual last-life participants");
            var victim=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(victim!=null&&game.lives(victim.getUUID())==1,"disposable actual final life");
            game.config().developerMode=false;base.owner.getAbilities().invulnerable=true;
            var at=base.owner.position().add(4,0,0);var center=BlockPos.containing(at);
            for(var p:BlockPos.betweenClosed(center.offset(-6,-6,-6),center.offset(6,6,6))){
                require(base.owner.level().hasChunkAt(p)&&base.owner.level().getBlockState(p).isAir(),"resident empty owned crater frame");
                blocks.put(p.immutable(),game.maps().snapshot(base.owner.level(),p));
                boolean cavity=Math.abs(p.getX()-center.getX())<=1&&Math.abs(p.getZ()-center.getZ())<=1&&p.getY()>=center.getY()&&p.getY()<=center.getY()+2;
                if(!cavity)LbcDropBlocks.setBlock(base.owner.level(),p,Blocks.STONE.defaultBlockState(),true);
            }
            victim.teleportTo(base.owner.level(),at.x,at.y,at.z,Set.of(),0,0,true);victim.setNoGravity(true);
            victim.getInventory().clearContent();victim.removeAllEffects();victim.getAbilities().invulnerable=false;
            victim.setAbsorptionAmount(0);victim.setHealth(1);victim.setInvulnerableTime(0);victim.damageCooldownTime=0;
            victim.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
            game.items().exoperso().edibleBurst(base.owner,at);
            require(game.lives(victim.getUUID())==0&&game.items().exasperato().pendingNuclearTerrain(),"first accepted wave consumes enemy final life before terrain work; lives="+game.lives(victim.getUUID())+", hp="+victim.getHealth());
            game.items().timeStop().begin(base.owner.level(),100);started=game.server().getTickCount();
        }
        boolean step(){
            int now=game.server().getTickCount();require(now-started<300,"accepted crater settles in bounded actual ticks");
            if(thaw<0){require(game.playing()&&game.stage()!=GameController.Stage.ENDING&&game.items().exasperato().pendingNuclearTerrain(),
                    "automatic victory must not cancel a stopped accepted crater after its old five-tick deadline");
                if(now-started<8)return false;game.items().timeStop().end(base.owner.level());thaw=now;return false;}
            if(game.items().exasperato().pendingNuclearTerrain()){
                require(game.playing()&&game.stage()!=GameController.Stage.ENDING,"match remains active throughout accepted terrain and remaining waves");return false;
            }
            int air=0;for(var p:blocks.keySet())if(base.owner.level().getBlockState(p).isAir())air++;
            require(air>blocks.size()*.9,"final-life nuclear crater reaches its dense geometry instead of stopping after five ticks");
            if(game.stage()!=GameController.Stage.ENDING)return false;
            require(now-thaw>=5,"normal victory countdown follows actual burst completion");return true;
        }
        public void close(){if(closed)return;closed=true;game.items().timeStop().end(base.owner.level());game.items().exoperso().stop();game.items().exasperato().stop();
            for(var e:blocks.entrySet())game.maps().restoreSnapshot(base.owner.level(),e.getKey(),e.getValue());
            game.config().developerMode=developer;base.owner.getAbilities().invulnerable=invulnerable;base.close();}
    }
    public static void run(GameController game){require(running==null,"exclusive scene");var next=new Fixture(game);try{next.initialize();running=next;}catch(Throwable error){next.close();throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;
        SkyWars2.LOGGER.info("[check] PASS nuclear-finalization (actual native final-life death,8tick stopped accepted crater survives old victory deadline, dense crater and all remaining waves finish before automatic ENDING)");
    }catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL nuclear-finalization",error);}}
}
