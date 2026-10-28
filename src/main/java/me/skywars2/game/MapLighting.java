package me.skywars2.game;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import me.skywars2.mixin.ArenaChunkAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** A server-thread bulk-write boundary; native lighting work stays on its own executor. */
final class MapLighting {
    private MapLighting() { }

    static final class Update {
        private static final int RESEND_CHUNKS_PER_TICK=16;
        private static final long RESEND_NANOS=1_500_000;
        private final ServerLevel level;
        private final LongOpenHashSet changed=new LongOpenHashSet();
        private final LongOpenHashSet affected=new LongOpenHashSet();
        private final Set<ChunkHolder> gated=new HashSet<>();
        private final ArrayDeque<ChunkPos> resends=new ArrayDeque<>();
        private final CompletableFuture<Void> completion=new CompletableFuture<>();
        private CompletableFuture<Void> propagation;
        private boolean finishing;
        private int sentPackets;

        Update(ServerLevel level) { this.level=level; }
        CompletableFuture<Void> completion() { return completion; }
        boolean finishing() { return finishing; }
        int chunkCount() { return affected.size(); }
        int sentPackets() { return sentPackets; }
        boolean empty() { return changed.isEmpty(); }

        /** Called before a write. The one-chunk margin covers light crossing a chunk edge. */
        void changed(BlockPos pos) {
            if(completion.isDone())return;
            // Sealed batches are immutable: later writes must have their own send dependency.
            if(finishing)throw new IllegalStateException("Lighting batch is already sealed");
            int x=pos.getX()>>4,z=pos.getZ()>>4;
            if(!changed.add(ChunkPos.pack(x,z)))return;
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++) {
                long key=ChunkPos.pack(x+dx,z+dz);
                if(affected.add(key))gate(ChunkPos.unpack(key));
            }
        }

        private void gate(ChunkPos pos) {
            var holder=((ArenaChunkAccess)level.getChunkSource().chunkMap).skywars2$visibleChunks().get(pos.pack());
            if(holder!=null&&gated.add(holder))holder.addSendDependency(completion);
        }

        void finishWrites() {
            if(completion.isDone())return;
            finishing=true;
            if(propagation==null)scheduleBarriers();
        }

        private void scheduleBarriers() {
            var engine=level.getChunkSource().getLightEngine();
            var waits=new java.util.ArrayList<CompletableFuture<?>>();
            for(long key:affected) {
                var pos=ChunkPos.unpack(key);gate(pos);
                // Query resident chunks only: a lighting repair must never generate terrain.
                if(level.getChunkSource().getChunkNow(pos.x(),pos.z())!=null) {
                    waits.add(engine.waitForPendingTasks(pos.x(),pos.z()));
                    // Pending/new viewers receive fresh light with their gated initial chunk packet.
                    if(!level.getChunkSource().chunkMap.getPlayers(pos,false).isEmpty())resends.add(pos);
                }
            }
            propagation=CompletableFuture.allOf(waits.toArray(CompletableFuture[]::new));
        }

        /** Returns after propagation/publication and all final packets; never waits on the server thread. */
        boolean tick() {
            return tick(System.nanoTime()+RESEND_NANOS);
        }

        boolean tick(long deadline) {
            if(completion.isDone())return true;
            if(!finishing)return false;
            if(propagation==null)scheduleBarriers();
            if(!propagation.isDone())return false;
            propagation.join(); // Completed only; propagate failures without a blocking wait.
            for(int count=0;count<RESEND_CHUNKS_PER_TICK&&!resends.isEmpty();count++) {
                if(System.nanoTime()>=deadline)return false;
                var pos=resends.removeFirst();
                if(level.getChunkSource().getChunkNow(pos.x(),pos.z())==null)continue;
                var viewers=level.getChunkSource().chunkMap.getPlayers(pos,false);
                if(viewers.isEmpty())continue;
                // Native delta broadcasts reach border viewers only. Interior viewers need this full snapshot.
                var packet=new ClientboundLightUpdatePacket(pos,level.getChunkSource().getLightEngine(),null,null);
                for(var viewer:viewers) { viewer.connection.send(packet);sentPackets++; }
            }
            if(!resends.isEmpty())return false;
            completion.complete(null);
            gated.clear();
            return true;
        }

        /** Unblock initial chunk sends on failed/cancelled operations; native work itself is not cancelled. */
        void cancel() {
            completion.complete(null);resends.clear();gated.clear();propagation=null;
        }
    }
}
