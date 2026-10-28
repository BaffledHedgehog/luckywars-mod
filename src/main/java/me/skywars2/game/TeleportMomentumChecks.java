package me.skywars2.game;

import io.netty.buffer.Unpooled;
import me.skywars2.SkyWars2;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Native position packets keep independently differing client and server movement. */
public final class TeleportMomentumChecks {
    private TeleportMomentumChecks() { }
    private static void require(boolean ok,String message) {
        if(!ok)throw new IllegalStateException("Teleport momentum: "+message);
    }
    private static void same(Vec3 actual,Vec3 expected,String message) {
        require(actual.distanceToSqr(expected)<1e-20,message+": "+actual+" expected "+expected);
    }
    private static final class Recording extends Connection {
        final ServerPlayer player;final List<Packet<?>> packets=new ArrayList<>();
        Recording(ServerPlayer p){super(PacketFlow.SERVERBOUND);player=p;}
        @Override public boolean isMemoryConnection(){return true;}
        @Override public boolean isConnected(){return true;}
        @Override public PacketListener getPacketListener(){return player.connection;}
        @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){packets.add(packet);}
        @Override public void flushChannel(){ }
        ClientboundPlayerPositionPacket position(){return packets.stream().filter(ClientboundPlayerPositionPacket.class::isInstance)
                .map(ClientboundPlayerPositionPacket.class::cast).reduce((a,b)->b).orElseThrow();}
    }
    private static Field knownField(){try{var f=ServerPlayer.class.getDeclaredField("lastKnownClientMovement");f.setAccessible(true);return f;}
        catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
    private static void known(ServerPlayer p,Vec3 value){try{knownField().set(p,value);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
    private static void packet(Recording wire,Vec3 clientMotion){
        var buffer=new FriendlyByteBuf(Unpooled.buffer());ClientboundPlayerPositionPacket decoded;
        try{ClientboundPlayerPositionPacket.STREAM_CODEC.encode(buffer,wire.position());decoded=ClientboundPlayerPositionPacket.STREAM_CODEC.decode(buffer);}
        finally{buffer.release();}
        require(decoded.change().deltaMovement().equals(Vec3.ZERO),"position-only wire has zero relative impulse");
        require(decoded.relatives().containsAll(Set.of(Relative.DELTA_X,Relative.DELTA_Y,Relative.DELTA_Z))
                &&!decoded.relatives().contains(Relative.ROTATE_DELTA),"wire keeps all velocity axes without rotating them");
        var local=new PositionMoveRotation(wire.player.position(),clientMotion,-73,37);
        same(PositionMoveRotation.calculateAbsolute(local,decoded.change(),decoded.relatives()).deltaMovement(),clientMotion,
                "native client packet application retains its own movement, not server/known movement");
    }
    private static void pose(ServerPlayer p,Recording wire,Vec3 serverMotion,Vec3 clientMotion,Runnable action,String label){
        p.setDeltaMovement(serverMotion);known(p,new Vec3(-.17,.09,.23));wire.packets.clear();action.run();
        same(p.getDeltaMovement(),serverMotion,label+" server momentum");packet(wire,clientMotion);
    }
    public static void run(GameController game){
        var p=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(game.playing()&&p!=null,"active native participant");
        var saved=new GameChecks.WandThirdPlayerState(p,game.items().mana());
        var previous=p.connection;var wire=new Recording(p);var known=p.getKnownMovement();boolean grounded=p.onGround();
        p.connection=new ServerGamePacketListenerImpl(game.server(),wire,p,
                new CommonListenerCookie(p.getGameProfile(),0,p.clientInformation(),false));
        p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        var serverMotion=new Vec3(.37,.28,-.46);var clientMotion=new Vec3(-.29,-.33,.54);
        Cow cow=null;
        try{
            pose(p,wire,serverMotion,clientMotion,()->p.teleportTo(p.level(),p.getX(),p.getY(),p.getZ(),Set.of(),121,-39,true),"full camera/position overload");
            pose(p,wire,serverMotion,clientMotion,()->p.teleportTo(p.getX()+.25,p.getY()+.1,p.getZ()-.25),"three-coordinate shift");
            pose(p,wire,serverMotion,clientMotion,()->p.teleportRelative(-.25,-.1,.25),"relative shift");
            pose(p,wire,serverMotion,clientMotion,()->p.connection.teleport(p.getX(),p.getY(),p.getZ(),-112,43),"connection coordinates");
            pose(p,wire,serverMotion,clientMotion,()->p.connection.teleport(
                    new PositionMoveRotation(p.position(),Vec3.ZERO,80,-25),Relative.ALL),"connection relative rotation");
            pose(p,wire,serverMotion,clientMotion,()->p.teleport(new TeleportTransition(p.level(),p.position(),Vec3.ZERO,-61,22,Set.of(),TeleportTransition.DO_NOTHING)),"zero-motion native transition");
            var last=wire.position();p.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(
                    last.id(),p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot()));
            same(p.getDeltaMovement(),serverMotion,"actual teleport acknowledgement keeps server movement");
            p.setOnGround(false);
            pose(p,wire,serverMotion,clientMotion,()->{
                try{game.server().getCommands().getDispatcher().execute("tp @s ~ ~ ~",game.server().createCommandSourceStack()
                        .withEntity(p).withLevel(p.level()).withPosition(p.position()).withSuppressedOutput());}
                catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException(error);}
            },"native /tp command");
            require(!p.onGround(),"/tp does not fake an airborne landing");
            var horse=LbcInventoryHazards.class.getDeclaredMethod("horse",ServerPlayer.class,net.minecraft.server.level.ServerLevel.class,
                    Vec3.class,net.minecraft.world.phys.Vec2.class,LbcIndirectKills.Owner.class);horse.setAccessible(true);
            pose(p,wire,serverMotion,clientMotion,()->{
                try{horse.invoke(game.items().inventoryHazards(),p,p.level(),p.position(),new net.minecraft.world.phys.Vec2(p.getXRot(),p.getYRot()),null);}
                catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            },"inventory horse displacement independently of preceding native hurt/knockback");
            var explicit=new Vec3(-.42,.18,.39);wire.packets.clear();
            p.teleport(new TeleportTransition(p.level(),p.position(),explicit,120,15,Set.of(),TeleportTransition.DO_NOTHING));
            same(p.getDeltaMovement(),explicit,"explicit transition movement is retained");
            same(wire.position().change().deltaMovement(),explicit,"explicit transition wire movement is retained");
            require(wire.position().relatives().isEmpty(),"explicit native transition flags remain unchanged");
            p.setDeltaMovement(serverMotion);var rotate=LbcPsycho.class.getDeclaredMethod("rotate",net.minecraft.world.entity.LivingEntity.class,float.class,float.class);
            rotate.setAccessible(true);wire.packets.clear();var position=p.position();
            for(int i=0;i<20;i++)rotate.invoke(game.items().psycho(),p,5f,-2f);
            same(p.getDeltaMovement(),serverMotion,"repeated real Psycho turns preserve momentum");same(p.position(),position,"camera-only turns never move position");
            require(wire.packets.stream().filter(ClientboundPlayerRotationPacket.class::isInstance).count()==20
                    &&wire.packets.stream().noneMatch(ClientboundPlayerPositionPacket.class::isInstance),"Psycho uses actual rotation-only packets without teleport ACKs");
            var recoil=LbcErecto.class.getDeclaredMethod("rotate",ServerPlayer.class,Vec3.class,float.class);recoil.setAccessible(true);
            wire.packets.clear();recoil.invoke(null,p,p.getLookAngle(),-15f);
            same(p.getDeltaMovement(),serverMotion,"real Erecto recoil keeps motion");
            require(wire.packets.stream().anyMatch(ClientboundPlayerRotationPacket.class::isInstance)
                    &&wire.packets.stream().noneMatch(ClientboundPlayerPositionPacket.class::isInstance),"Erecto recoil is rotation-only");
            var scare=LbcPsycho.class.getDeclaredMethod("scareTick",net.minecraft.world.entity.LivingEntity.class);scare.setAccessible(true);
            wire.packets.clear();p.getRandom().setSeed(0x51e4);
            for(int i=0;i<30;i++){scare.invoke(game.items().psycho(),p);same(p.getDeltaMovement(),serverMotion,"real repeated fear shift/turn "+i);}
            require(wire.packets.stream().anyMatch(ClientboundPlayerPositionPacket.class::isInstance),"deterministic fear actually shifted position");packet(wire,clientMotion);
            var impulse=new Vec3(-.8,.65,.3);p.setDeltaMovement(impulse);
            p.teleportTo(p.getX(),p.getY(),p.getZ());same(p.getDeltaMovement(),impulse,"explicit knockback before position edit survives");
            p.setDeltaMovement(impulse.scale(2));same(p.getDeltaMovement(),impulse.scale(2),"explicit dash after position edit remains effective");
            cow=new Cow(EntityTypes.COW,p.level());cow.setNoAi(true);cow.setNoGravity(true);cow.setPos(position);
            require(p.level().addFreshEntity(cow),"native mob publication");cow.setDeltaMovement(serverMotion);
            require(cow.teleportTo(p.level(),position.x+.25,position.y,position.z,Set.of(),131,-25,true),"native mob coordinate edit");
            same(cow.getDeltaMovement(),serverMotion,"mob coordinate/rotation edit also retains momentum");
        }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        finally{if(cow!=null)cow.discard();p.connection=previous;saved.restore(game);known(p,known);p.setOnGround(grounded);}
        SkyWars2.LOGGER.info("[check] PASS momentum-teleports (all position overloads, independent server/known/client velocity, native wire codec and client calculation, ACK, /tp, Psycho shifts/rotation-only packets, Erecto recoil, explicit transition/impulses and mob motion)");
    }
}
