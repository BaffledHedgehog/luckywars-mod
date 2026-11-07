package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import me.skywars2.SkyWars2;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Actual native crystal chain and publication, including a victim outside the initial blast. */
public final class TestReportCoreChecks {
    private TestReportCoreChecks(){}
    private static void require(boolean test,String why){if(!test)throw new IllegalStateException("Test report core: "+why);}
    public static void run(GameController game){
        try(var base=new ChefChecks.Fixture(game)){
            base.init();var level=base.owner.level();var at=base.at.add(0,3,0);
            var crystals=new ArrayList<EndCrystal>();
            try(var client=new RegenerationBeltChecks.ScheduledClient(game,base.owner)){
                for(int i=0;i<3;i++){var c=new EndCrystal(EntityTypes.END_CRYSTAL,level);c.setPos(at.add(i,0,0));require(level.addFreshEntity(c),"actual crystal");crystals.add(c);}
                var witness=base.cow(at.add(12.5,0,0));witness.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);witness.setHealth(1000);
                require(crystals.getFirst().hurtServer(level,level.damageSources().playerAttack(base.owner),1),"accepted first crystal hit");
                require(crystals.stream().allMatch(EndCrystal::isRemoved),"whole native chain removed");
                require(witness.getHealth()<1000,"later crystal explodes rather than silently disappearing");
                var field=((Object)client.connection).getClass().getDeclaredField("packets");field.setAccessible(true);
                var packets=(List<?>)field.get(client.connection);
                require(packets.stream().filter(ClientboundExplodePacket.class::isInstance).count()==3,"one publication per crystal");
                int before=packets.size();
                level.explode(null,at.x,at.y,at.z,1,Level.ExplosionInteraction.MOB);
                var flash=packets.subList(before,packets.size()).stream().filter(ClientboundExplodePacket.class::isInstance).map(ClientboundExplodePacket.class::cast).findFirst().orElseThrow();
                require(flash.explosionParticle()==ParticleTypes.EXPLOSION&&flash.blockParticles().isEmpty(),"legacy creeper single flash without fragment burst");
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            finally{for(var c:crystals)if(!c.isRemoved())c.discard();}
        }
        SkyWars2.LOGGER.info("[check] PASS test-report-core (native crystal chain damage/publication and creeper single flash)");
    }
}
