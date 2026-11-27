package me.skywars2.validation.mixin;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.TicketStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

public final class NativeFixtureAccess {
    private NativeFixtureAccess() {}

    @Mixin(net.minecraft.world.level.entity.PersistentEntitySectionManager.class)
    public interface Sections {
        @Invoker("stopTracking") void skywars2$validationStopTracking(net.minecraft.world.level.entity.EntityAccess actor);
        @Invoker("startTracking") void skywars2$validationStartTracking(net.minecraft.world.level.entity.EntityAccess actor);
    }

    @Mixin(MinecraftServer.class)
    public interface Server {
        @Accessor("tickCount") int skywars2$validationTickCount();
        @Accessor("tickCount") void skywars2$validationTickCount(int value);
        @Accessor("aggregatedTickTimesNanos") long skywars2$validationAggregatedTickTime();
        @Accessor("aggregatedTickTimesNanos") void skywars2$validationAggregatedTickTime(long value);
    }

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
