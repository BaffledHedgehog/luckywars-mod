package me.skywars2.validation.mixin;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.TicketStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

public final class NativeFixtureAccess {
    private NativeFixtureAccess() {}

    @Mixin(Connection.class)
    public interface ConnectionBinding {
        @Accessor("packetListener") void validation$packetListener(PacketListener listener);
    }

    @Mixin(ServerChunkCache.class)
    public interface Chunks {
        @Accessor("ticketStorage") TicketStorage skywars2$validationTicketStorage();
    }

    @Mixin(targets="eu.pb4.polymer.core.impl.PolymerImplUtils",remap=false)
    public interface Polymer {
        @Accessor("alreadySentMissingChunkPacketContextWarning")
        static long skywars2$validationChunkPacketWarning() { throw new AssertionError(); }

        @Accessor("alreadySentMissingChunkPacketContextWarning")
        static void skywars2$validationChunkPacketWarning(long previous) { throw new AssertionError(); }
    }
}
