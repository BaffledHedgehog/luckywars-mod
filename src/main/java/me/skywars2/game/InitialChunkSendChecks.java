package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.ArenaChunkAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;

/** Initial native view admission while a resident central chunk is gated; no manual sender queue repair. */
public final class InitialChunkSendChecks {
    private InitialChunkSendChecks(){}
    private static GameController requested;private static Scene running;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("Initial chunk send: "+why);}
    public static void run(GameController game){require(requested==null&&running==null,"exclusive scene");requested=game;}
    public static void tick(GameController game){
        if(requested!=null){require(requested==game,"queued controller");requested=null;var scene=new Scene(game);running=scene;try{scene.begin();}catch(Throwable e){failed(scene,e);}return;}
        var scene=running;if(scene==null)return;try{require(scene.game==game,"same controller");if(!scene.step())return;running=null;scene.close();SkyWars2.LOGGER.info("[check] PASS initial-chunk-send (real native view delta queues a gated resident central chunk; no full packet before lighting; actual native scheduler sends once after release without movement; leaving view cancels pending request; original views/terrain/player state restored)");}catch(Throwable e){failed(scene,e);}
    }
    private static void failed(Scene scene,Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL initial-chunk-send",error);}
    private static final class Scene implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final ChunkTrackingView originalView,initialView;
        final List<Packet<?>> packets;final ChunkPos target;final BlockPos probe;
        MapLighting.Update update;int phase,when,started,cursor,targetPackets,before;boolean closed;
        @SuppressWarnings("unchecked") Scene(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);originalView=f.owner.getChunkTrackingView();target=f.owner.chunkPosition();initialView=ChunkTrackingView.of(target,2);probe=BlockPos.containing(f.home).above(5);
            packets=(List<Packet<?>>)WoolBucketChecks.read(WoolBucketChecks.read(f.client,"connection"),"packets");}
        void begin(){f.prepareParticipants();started=game.server().getTickCount();require(initialView.contains(target.x(),target.z()),"actual initial view includes player's central chunk");}
        void collect(){f.client.acknowledge();f.audience.acknowledge();while(cursor<packets.size()){
            var packet=packets.get(cursor++);if(packet instanceof ClientboundChunkBatchFinishedPacket)f.client.listener.handleChunkBatchReceived(new ServerboundChunkBatchReceivedPacket(64));
            if(packet instanceof ClientboundLevelChunkWithLightPacket chunk&&chunk.x()==target.x()&&chunk.z()==target.z())targetPackets++;
        }}
        void enterView(){var access=(ArenaChunkAccess)f.level.getChunkSource().chunkMap;access.skywars2$applyChunkTrackingView(f.owner,ChunkTrackingView.EMPTY);access.skywars2$applyChunkTrackingView(f.owner,initialView);}
        boolean step(){collect();require(game.server().getTickCount()-started<160,"bounded real native scene phase="+phase);var map=f.level.getChunkSource().chunkMap;int elapsed=game.server().getTickCount()-when;
            if(phase==0){if(map.getChunkToSend(target.pack())==null)return false;
                update=new MapLighting.Update(f.level);update.changed(probe);f.set(probe,Blocks.GLOWSTONE.defaultBlockState());before=targetPackets;enterView();
                require(map.getChunkToSend(target.pack())==null,"actual send dependency blocks central chunk data");
                require(f.client.listener.chunkSender.isPending(target.pack()),"initial native view must retain a request when the ready chunk is temporarily gated");when=game.server().getTickCount();phase=1;return false;}
            if(phase==1){require(targetPackets==before&&f.client.listener.chunkSender.isPending(target.pack()),"native frames retain the request without sending unlit data");if(elapsed<3)return false;update.finishWrites();when=game.server().getTickCount();phase=2;return false;}
            if(phase==2){if(!update.tick())return false;if(targetPackets==before)return false;
                require(targetPackets==before+1&&!f.client.listener.chunkSender.isPending(target.pack()),"native scheduler delivers central chunk exactly once after release without teleport/view reset");
                require(map.getChunkToSend(target.pack())!=null,"released native dependency remains ready");
                update=new MapLighting.Update(f.level);update.changed(probe);before=targetPackets;enterView();require(f.client.listener.chunkSender.isPending(target.pack()),"second actual pending admission");
                ((ArenaChunkAccess)map).skywars2$applyChunkTrackingView(f.owner,ChunkTrackingView.EMPTY);
                require(!f.client.listener.chunkSender.isPending(target.pack()),"native view exit cancels the request");update.cancel();return true;}
            throw new IllegalStateException("phase "+phase);
        }
        public void close(){if(closed)return;closed=true;try{if(update!=null)update.cancel();f.owner.setChunkTrackingView(originalView);}finally{f.close();}}
    }
}
