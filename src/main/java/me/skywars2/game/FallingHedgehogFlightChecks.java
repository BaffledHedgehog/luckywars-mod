package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.phys.Vec3;

/** A normally ticking 100-block fall of the actual skill carrier, not a synthetic OnGround change. */
final class FallingHedgehogFlightChecks {
    private static Scene running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Falling hedgehog flight: "+why);}
    private static final class Scene implements AutoCloseable {
        final GameController game;final ContainedSkillChecks.Fixture f;final Set<UUID> original=new HashSet<>();
        Silverfish carrier;int at,cursor,initialTicks;double initialY;boolean fell,tracked;int holes;
        Scene(GameController game){this.game=game;f=new ContainedSkillChecks.Fixture(game);}
        void setup(){
            require(game.items().fallingHedgehogs().trackedCount()==0,"exclusive empty source carrier service");
            for(var entity:f.level.getAllEntities())original.add(entity.getUUID());f.initialize();
            var center=BlockPos.containing(f.origin);
            for(var pos:BlockPos.betweenClosed(center.offset(-15,-15,-15),center.offset(15,15,15))){
                require(f.level.hasChunkAt(pos),"blast neighborhood resident before measuring");var key=pos.immutable();f.terrain.computeIfAbsent(key,ignored->game.maps().snapshot(f.level,key));
            }
            cursor=f.client.packets().size();carrier=f.spawn();
            // Keep the existing source spawn/eligibility, then arrange only its initial test column.
            carrier.setPos(f.origin.add(0,100,0));carrier.setDeltaMovement(Vec3.ZERO);carrier.setOnGround(false);
            require(carrier.isInvulnerable()&&!carrier.isNoGravity(),"actual invulnerable carrier retains native gravity");
            game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("#falling_hedgehogs"),"lbc.math").set(0);
            initialTicks=carrier.tickCount;initialY=carrier.getY();at=game.server().getTickCount();
        }
        void flatten(Packet<?> packet,List<Packet<?>> out){if(packet instanceof ClientboundBundlePacket bundle)for(var child:bundle.subPackets())flatten(child,out);else out.add(packet);}
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();int elapsed=game.server().getTickCount()-at;
            require(elapsed<95,"natural skill carrier must fall and detonate before the next five-second emission: pos="+carrier.position()+" delta="+carrier.getDeltaMovement()+" ticks="+carrier.tickCount);
            var packets=new ArrayList<Packet<?>>();for(var packet:f.client.packets().subList(cursor,f.client.packets().size()))flatten(packet,packets);cursor=f.client.packets().size();
            tracked|=packets.stream().anyMatch(packet->packet instanceof ClientboundAddEntityPacket add&&add.getId()==carrier.getId()&&add.getType()==EntityTypes.SILVERFISH);
            if(!carrier.isRemoved()){
                require(carrier.isInvulnerable()&&!carrier.isNoGravity(),"native falling body keeps its source traits");
                if(carrier.getY()<initialY-10){fell=true;require(carrier.tickCount>initialTicks+5&&carrier.getDeltaMovement().y<0,"native entity ticks and gravity drive the fall");}
                return false;
            }
            require(fell&&tracked,"source body really falls and its Silverfish spawn is transmitted to the native viewer");
            var floor=BlockPos.containing(f.origin).below();
            for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++)if(f.level.getBlockState(floor.offset(x,0,z)).isAir())holes++;
            require(holes>0&&game.items().fallingHedgehogs().trackedCount()==0,"natural solid landing removes the exact carrier and produces a destructive blast");
            SkyWars2.LOGGER.info("[check] Falling hedgehog natural100 fallTicks={} nativeEntityTicks={} floorHoles={} spawnPacket=true",elapsed,carrier.tickCount-initialTicks,holes);
            return true;
        }
        @Override public void close(){
            for(var entity:f.level.getAllEntities())if(!(entity instanceof net.minecraft.server.level.ServerPlayer)&&!original.contains(entity.getUUID())&&entity.position().distanceToSqr(f.origin)<120*120&&!f.actors.contains(entity))f.actors.add(entity);
            f.close();
        }
    }
    static void run(GameController game){require(running==null,"exclusive scene");running=new Scene(game);try{running.setup();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS falling-hedgehog-flight (native100block gravity, transmitted Silverfish, real solid landing and destructive single blast)");}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL falling-hedgehog-flight",error);}}
}
