package me.skywars2.game.world.terrain;

import net.minecraft.world.entity.Relative;
import java.util.EnumSet;
import java.util.Set;

/** Shared wire flags for coordinate edits, independent of the client's local velocity. */
public final class TeleportMomentum {
    private TeleportMomentum() { }
    private static final ThreadLocal<net.minecraft.server.level.ServerPlayer> STOPPED=new ThreadLocal<>();
    public static boolean stopping(net.minecraft.server.level.ServerPlayer player){return STOPPED.get()==player;}
    public static Set<Relative> positionFlags(Set<Relative> relatives) {
        var flags=EnumSet.noneOf(Relative.class);flags.addAll(relatives);
        flags.add(Relative.DELTA_X);flags.add(Relative.DELTA_Y);flags.add(Relative.DELTA_Z);
        flags.remove(Relative.ROTATE_DELTA);return flags;
    }
    /** An explicit capture stops motion; ordinary coordinate-only teleports keep their existing policy. */
    public static void teleportStopped(net.minecraft.world.entity.Entity target,net.minecraft.world.phys.Vec3 at) {
        target.stopRiding();target.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);target.fallDistance=0;
        if(target instanceof net.minecraft.server.level.ServerPlayer player) {
            player.setKnownMovement(net.minecraft.world.phys.Vec3.ZERO);
            var previous=STOPPED.get();STOPPED.set(player);
            try{
                // One authoritative position AND zero-velocity transition. A later motion
                // packet leaves a window in which the client resumes its pre-capture fall.
                player.connection.teleport(new net.minecraft.world.entity.PositionMoveRotation(at,
                        net.minecraft.world.phys.Vec3.ZERO,player.getYRot(),player.getXRot()),Set.of());
            }finally{if(previous==null)STOPPED.remove();else STOPPED.set(previous);}
        }else target.teleportTo(at.x,at.y,at.z);
        target.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    }
    /** Transfer server-driven riding to the client's native vehicle physics without losing inertia. */
    public static void handoffVehicle(net.minecraft.server.level.ServerPlayer rider,net.minecraft.world.entity.Entity vehicle,net.minecraft.world.phys.Vec3 motion){
        if(rider.getVehicle()!=vehicle)throw new IllegalArgumentException("Vehicle handoff requires its actual passenger");
        vehicle.setDeltaMovement(motion);vehicle.needsSync=true;
        // The local controller must change first, otherwise the client zeros server-owned motion.
        rider.connection.send(new net.minecraft.network.protocol.game.ClientboundSetPassengersPacket(vehicle));
        rider.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(vehicle));
    }
}
