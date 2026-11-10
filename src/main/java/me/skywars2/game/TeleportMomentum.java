package me.skywars2.game;

import net.minecraft.world.entity.Relative;
import java.util.EnumSet;
import java.util.Set;

/** Shared wire flags for coordinate edits, independent of the client's local velocity. */
public final class TeleportMomentum {
    private TeleportMomentum() { }
    public static Set<Relative> positionFlags(Set<Relative> relatives) {
        var flags=EnumSet.noneOf(Relative.class);flags.addAll(relatives);
        flags.add(Relative.DELTA_X);flags.add(Relative.DELTA_Y);flags.add(Relative.DELTA_Z);
        flags.remove(Relative.ROTATE_DELTA);return flags;
    }
    /** An explicit capture stops motion; ordinary coordinate-only teleports keep their existing policy. */
    public static void teleportStopped(net.minecraft.world.entity.Entity target,net.minecraft.world.phys.Vec3 at) {
        target.stopRiding();target.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);target.fallDistance=0;
        target.teleportTo(at.x,at.y,at.z);
        target.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        if(target instanceof net.minecraft.server.level.ServerPlayer player) {
            player.setKnownMovement(net.minecraft.world.phys.Vec3.ZERO);
            // Position-only teleports intentionally preserve client-local motion. Override it explicitly here.
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
        }
    }
}
