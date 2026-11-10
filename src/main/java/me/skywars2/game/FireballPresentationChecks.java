package me.skywars2.game;

import io.netty.buffer.Unpooled;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.UpdateInterval;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.phys.Vec3;

/** Actual native pairing/update sends and motion codec; server projectile physics stay untouched. */
public final class FireballPresentationChecks {
    private FireballPresentationChecks(){}
    private static void require(boolean value,String reason){if(!value)throw new IllegalStateException("Fireball presentation: "+reason);}
    private static List<Packet<?>> flat(Iterable<? extends Packet<?>> packets){
        var result=new ArrayList<Packet<?>>();for(var packet:packets){
            if(packet instanceof ClientboundBundlePacket bundle)result.addAll(flat(bundle.subPackets()));else result.add(packet);
        }return result;
    }
    private static final class Wire implements ServerEntity.Synchronizer {
        final ServerPlayer viewer;Wire(ServerPlayer viewer){this.viewer=viewer;}
        public void sendToTrackingPlayers(Packet<? super ClientGamePacketListener> packet){viewer.connection.send(packet);}
        public void sendToTrackingPlayersAndSelf(Packet<? super ClientGamePacketListener> packet){viewer.connection.send(packet);}
        public void sendToTrackingPlayersFiltered(Packet<? super ClientGamePacketListener> packet,Predicate<ServerPlayer> filter){if(filter.test(viewer))viewer.connection.send(packet);}
    }
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();var at=f.home.add(0,12,0);var velocity=new Vec3(5.25,-.375,-2.625);
            var donor=new Arrow(EntityTypes.ARROW,f.level);donor.setOwner(f.owner);
            var body=game.items().specialFireballs().spawnRangedFire(f.owner,at,velocity,donor);
            require(body instanceof SmallFireball&&!body.isRemoved(),"canonical small-fireball factory publishes native body");f.owned.add(body.getUUID());
            f.client.packets().clear();var tracker=new ServerEntity(f.level,body,UpdateInterval.periodic(1),true,new Wire(f.owner));
            tracker.sendPairingData(f.owner,f.owner.connection::send);
            var packets=flat(f.client.packets());var spawn=packets.stream().filter(ClientboundAddEntityPacket.class::isInstance)
                    .map(ClientboundAddEntityPacket.class::cast).filter(p->p.getId()==body.getId()).findFirst().orElseThrow();
            require(spawn.getType()==EntityTypes.SMALL_FIREBALL&&spawn.getMovement().equals(Vec3.ZERO)
                    &&new Vec3(spawn.getX(),spawn.getY(),spawn.getZ()).equals(at),"real initial pairing retains native identity/exact position and disables visual extrapolation");
            require(body.getDeltaMovement().equals(velocity),"pairing does not clamp, round or zero the real server velocity");
            f.client.packets().clear();f.owner.connection.send(new ClientboundBundlePacket(List.of(new ClientboundSetEntityMotionPacket(body),
                    new ClientboundProjectilePowerPacket(body.getId(),.1))));
            var motion=flat(f.client.packets()).stream().filter(ClientboundSetEntityMotionPacket.class::isInstance)
                    .map(ClientboundSetEntityMotionPacket.class::cast).findFirst().orElseThrow();
            var bytes=Unpooled.buffer();try{
                ClientboundSetEntityMotionPacket.STREAM_CODEC.encode(bytes,motion);
                require(ClientboundSetEntityMotionPacket.STREAM_CODEC.decode(bytes).movement().equals(Vec3.ZERO),"native bundled motion wire codec cannot reintroduce a divergent client velocity");
            }finally{bytes.release();}
            f.client.packets().clear();body.setPos(at.add(velocity));body.needsSync=true;body.syncPosition=true;tracker.sendChanges();
            require(flat(f.client.packets()).stream().anyMatch(p->p instanceof ClientboundMoveEntityPacket||p instanceof ClientboundEntityPositionSyncPacket),"actual native tracker still publishes authoritative movement");
            require(body.getDeltaMovement().equals(velocity),"position/motion publication leaves authoritative flight unchanged");
            var vanilla=new SmallFireball(EntityTypes.SMALL_FIREBALL,f.level);vanilla.setPos(at.add(0,4,0));vanilla.setDeltaMovement(velocity);
            require(f.level.addFreshEntity(vanilla),"unmanaged native control exists");f.owned.add(vanilla.getUUID());f.client.packets().clear();
            f.owner.connection.send(new ClientboundSetEntityMotionPacket(vanilla));
            require(flat(f.client.packets()).stream().filter(ClientboundSetEntityMotionPacket.class::isInstance)
                    .map(ClientboundSetEntityMotionPacket.class::cast).anyMatch(p->p.id()==vanilla.getId()&&p.movement().equals(velocity)),"ordinary unmanaged fireball keeps native client motion");
        }
        me.skywars2.SkyWars2.LOGGER.info("[check] PASS fireball-presentation (real native pairing, exact initial position, bundled motion codec, native tracker updates, true server velocity and unmanaged control)");
    }
}
