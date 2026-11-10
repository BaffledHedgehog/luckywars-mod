package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Observe actual scheduled arcs and emitted native packets, not a visual-count estimate. */
public final class TeslaTrafficChecks {
    private TeslaTrafficChecks() { }
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Tesla traffic: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;int began;
        Fixture(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);}
        void begin(){f.prepareParticipants();require(game.items().wands().electroCount()==0,"exclusive arc fixture");
            f.move(f.owner,f.home.add(0,60,0));f.move(f.other,f.owner.position().add(10,0,0));
            f.owner.setYRot(0);f.owner.setXRot(0);
            game.items().wands().teslaVolley(f.owner,InteractionHand.MAIN_HAND,new ItemStack(LbcWands.TESLA),false);
            f.client.packets().clear();began=game.server().getTickCount();
        }
        boolean step(){f.client.acknowledge();f.audience.acknowledge();
            var arcs=game.items().wands().electroSnapshot();require(arcs.size()==4,"same four physical source arcs");
            int age=arcs.getFirst().age;if(age==0)return false;
            require(age<4&&arcs.stream().allMatch(t->t.age==age),"bounded equally scheduled source traces");
            int dust=0,sparks=0,particlePackets=0,sounds=0;
            for(var packet:f.client.packets()){
                if(packet instanceof ClientboundLevelParticlesPacket visual){
                    if(visual.particle().getType()==ParticleTypes.DUST_COLOR_TRANSITION){dust+=visual.count();particlePackets++;}
                    if(visual.particle()==ParticleTypes.ELECTRIC_SPARK){sparks+=visual.count();particlePackets++;}
                }
                if(packet instanceof ClientboundSoundPacket)sounds++;
            }
            require(dust==44*age&&sparks==44*age,"all source particles retained: dust="+dust+" sparks="+sparks+" age="+age);
            require(particlePackets<=32*age&&sounds<=4*age,"adjacent particles and repeated sample sounds consolidated: particles="+particlePackets+" sounds="+sounds);
            SkyWars2.LOGGER.info("[check] PASS tesla-traffic ({} real ticks, {} dust + {} sparks in {} particle packets and {} sound packets; old source {} packets)",age,dust,sparks,particlePackets,sounds,132*age);
            return true;
        }
        public void close(){game.items().wands().stop();f.close();}
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.begin();}catch(Throwable e){running.close();running=null;throw e;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;}catch(Throwable e){try{running.close();}catch(Throwable cleanup){e.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL tesla-traffic",e);}}
}
