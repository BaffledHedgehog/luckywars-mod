package me.skywars2.game;

import eu.pb4.polymer.virtualentity.api.data.DisplayEntityData;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Actual outgoing Polymer packets, source model geometry, viewer enrollment and beam lifetime. */
public final class MinigunVisualChecks {
    private MinigunVisualChecks() { }
    private static void require(boolean value,String message) {
        if(!value) throw new IllegalStateException("Minigun visual: "+message);
    }
    private static final class Recording extends Connection {
        final ServerPlayer player;
        final List<Packet<?>> packets=new ArrayList<>();
        Recording(ServerPlayer player) { super(PacketFlow.SERVERBOUND); this.player=player; }
        @Override public boolean isMemoryConnection() { return true; }
        @Override public boolean isConnected() { return true; }
        @Override public PacketListener getPacketListener() { return player.connection; }
        @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush) {
            append(packet);
        }
        private void append(Packet<?> packet) {
            if(packet instanceof ClientboundBundlePacket bundle) bundle.subPackets().forEach(this::append);
            else packets.add(packet);
        }
        @Override public void flushChannel() { }
    }
    private static final class Swap implements AutoCloseable {
        final ServerPlayer player;
        final ServerGamePacketListenerImpl previous;
        final Recording recording;
        Swap(GameController game,ServerPlayer player) {
            this.player=player; previous=player.connection; recording=new Recording(player);
            player.connection=new ServerGamePacketListenerImpl(game.server(),recording,player,
                    new CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));
            player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        }
        @Override public void close() { player.connection=previous; previous.resetPosition(); }
    }
    private static ClientboundAddEntityPacket spawn(Recording recording) {
        return recording.packets.stream().filter(ClientboundAddEntityPacket.class::isInstance)
                .map(ClientboundAddEntityPacket.class::cast).filter(p->p.getType()==EntityTypes.ITEM_DISPLAY)
                .reduce((first,last)->last).orElseThrow(()->new IllegalStateException("Missing actual beam spawn packet"));
    }
    private static <T> T data(Recording recording,int entityId,EntityDataAccessor<T> accessor,T fallback) {
        T result=fallback;
        for(var packet:recording.packets)
            if(packet instanceof ClientboundSetEntityDataPacket metadata && metadata.id()==entityId)
                for(var value:metadata.packedItems()) if(value.id()==accessor.id()) {
                    @SuppressWarnings("unchecked") T typed=(T)value.value(); result=typed;
                }
        return result;
    }
    private static boolean removed(Recording recording,int id) {
        return recording.packets.stream().anyMatch(p->p instanceof ClientboundRemoveEntitiesPacket remove
                && remove.entityIds().contains(id));
    }
    private static void scale(Recording recording,int id,float width,float length) {
        var actual=data(recording,id,DisplayEntityData.SCALE,null);
        require(actual!=null && Math.abs(actual.x()-width)<.00001 && Math.abs(actual.y()-width)<.00001
                && Math.abs(actual.z()-length*2)<.0001,"outgoing width/length matches six-tick source fade");
    }
    private static void geometry(ClientboundAddEntityPacket spawn,Vector3fc scale,double length) {
        // RP bounds [7,7,0]..[9,9,8], vanilla centered item model and native ItemDisplayRenderer's Y pi.
        var pose=new Matrix4f().rotateYXZ((float)Math.toRadians(-spawn.getYRot()),
                (float)Math.toRadians(spawn.getXRot()),0).scale(scale).rotateY((float)Math.PI)
                .translate(-.5f,-.5f,-.5f);
        var start=pose.transformPosition(new Vector3f(.5f,.5f,.5f));
        var end=pose.transformPosition(new Vector3f(.5f,.5f,0));
        // DisplayRenderer uses JOML rotationYXZ, not Vec3.directionFromRotation's quantized Mth SIN table.
        // Compare against the analytic render axis; lookup-table error grows with the stretched beam length.
        double yaw=Math.toRadians(spawn.getYRot()),pitch=Math.toRadians(spawn.getXRot());
        var expected=new Vector3f((float)(-Math.sin(yaw)*Math.cos(pitch)*length),
                (float)(-Math.sin(pitch)*length),(float)(Math.cos(yaw)*Math.cos(pitch)*length));
        require(start.length()<.0001 && end.distance(expected)<.0001,
                "exact RP half-block geometry reaches forward from muzzle with native display rotations");
        require(Math.abs(pose.transformDirection(new Vector3f(.125f,0,0)).length()-scale.x()*.125f)<.00001,
                "RP two-pixel beam width remains source scale/8");
    }
    public static void run(GameController game) {
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        var viewer=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
        require(game.playing() && owner!=null && viewer!=null && game.allowDamage(owner),"requires active fixture players");
        var savedOwner=new GameChecks.WandThirdPlayerState(owner,game.items().mana());
        var savedViewer=new GameChecks.WandThirdPlayerState(viewer,game.items().mana());
        var gun=game.items().laserMinigun();
        try(var a=new Swap(game,owner); var b=new Swap(game,viewer)) {
            try {
                gun.forget(owner); owner.closeContainer(); owner.getInventory().clearContent();
                owner.getInventory().setSelectedSlot(0); owner.removeAllEffects(); owner.setGameMode(GameType.SURVIVAL);
                owner.entityTags().removeAll(Set.of("developer","time_stopped","have_stand","stand_clocks_active"));
                owner.teleportTo(game.maps().level(),.5,281,70.5,Set.of(),37,-70,true);
                owner.setNoGravity(true); owner.setDeltaMovement(Vec3.ZERO);
                owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcLaserMinigun.ITEM));
                game.items().mana().inventoryChanged(owner); game.items().mana().noMagic(owner,0);
                game.items().magic().inventoryChanged(owner); game.items().magic().flushCleansing();
                game.items().mana().set(owner,100);
                viewer.teleportTo(owner.level(),210,281,70.5,Set.of(),0,0,true);
                a.recording.packets.clear(); b.recording.packets.clear();
                for(int tick=0;tick<30;tick++) {
                    owner.connection.handleClientTickEnd(ServerboundClientTickEndPacket.INSTANCE);
                    owner.connection.handleUseItem(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND,tick,
                            owner.getYRot(),owner.getXRot()));
                    gun.tick();
                }
                require(gun.shots(owner)==1 && gun.beamSnapshot().size()==1,"real paid native input emits exactly one beam");
                var beam=gun.beamSnapshot().getFirst(); var initial=spawn(a.recording); int id=initial.getId();
                require(new Vec3(initial.getX(),initial.getY(),initial.getZ()).distanceTo(beam.origin())<.00001,
                        "actual outgoing spawn starts at exact muzzle");
                require(Math.abs(net.minecraft.util.Mth.wrapDegrees(initial.getYRot()-beam.yaw()))<=360f/256
                        && Math.abs(net.minecraft.util.Mth.wrapDegrees(initial.getXRot()-beam.pitch()))<=360f/256,
                        "actual spawn carries authored yaw/pitch within native packet angle quantization");
                var item=data(a.recording,id,DisplayEntityData.Item.ITEM,ItemStack.EMPTY);
                require(item.is(Items.REPEATING_COMMAND_BLOCK) && Identifier.parse("lbc:laser_minigun_ray")
                        .equals(item.get(DataComponents.ITEM_MODEL)),"outgoing native item/model retains shader-marked RP texture");
                require(data(a.recording,id,DisplayEntityData.Item.ITEM_DISPLAY,(byte)0)==0
                        && data(a.recording,id,DisplayEntityData.WIDTH,0f)==0
                        && data(a.recording,id,DisplayEntityData.HEIGHT,0f)==0
                        && data(a.recording,id,DisplayEntityData.VIEW_RANGE,1f)==1
                        && data(a.recording,id,DisplayEntityData.INTERPOLATION_DURATION,0)==0,
                        "native NONE context, uncullable zero dimensions, view range and immediate transforms retained");
                scale(a.recording,id,0,(float)beam.length());
                require(b.recording.packets.stream().noneMatch(ClientboundAddEntityPacket.class::isInstance),
                        "outside native item-display tracking range receives no beam");
                gun.tick(); scale(a.recording,id,.2f,(float)beam.length());
                geometry(initial,data(a.recording,id,DisplayEntityData.SCALE,null),beam.length());
                viewer.teleportTo(owner.level(),owner.getX()+2,owner.getY(),owner.getZ(),Set.of(),0,0,true);
                owner.entityTags().add("time_stopped"); gun.tick();
                require(spawn(b.recording).getId()==id,"late nearby viewer receives actual spawn during time stop");
                scale(b.recording,id,.2f,(float)beam.length());
                require(gun.beamSnapshot().getFirst().life()==5,"viewer refresh does not age frozen beams");
                viewer.setPos(owner.getX()+210,owner.getY(),owner.getZ()); gun.tick();
                require(removed(b.recording,id),"leaving tracking range removes frozen display");
                owner.entityTags().remove("time_stopped");
                for(int life=4;life>=1;life--) { gun.tick(); scale(a.recording,id,life*.04f,(float)beam.length()); }
                require(!removed(a.recording,id),"beam survives every positive lifetime");
                gun.tick(); require(removed(a.recording,id) && gun.beamSnapshot().isEmpty(),
                        "sixth active tick sends actual native removal and releases beam");
                SkyWars2.LOGGER.info("[check] PASS minigun-visual");
            } finally {
                gun.forget(owner); savedViewer.restore(game); savedOwner.restore(game);
            }
        }
    }
}
