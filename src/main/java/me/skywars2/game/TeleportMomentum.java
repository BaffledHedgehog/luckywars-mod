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
}
