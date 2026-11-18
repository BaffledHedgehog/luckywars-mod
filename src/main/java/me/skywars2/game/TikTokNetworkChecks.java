package me.skywars2.game;

import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;
import me.skywars2.SkyWars2;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.item.*;

/** Offline media, actual native PLAY encoder/compression and a real loopback TCP recipient. */
public final class TikTokNetworkChecks {
    private static Scene running;
    private TikTokNetworkChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("TikTok network: "+why);}
    public static void run(GameController game){
        require(running==null,"exclusive scene");
        try{running=new Scene(game);}catch(Exception error){throw new IllegalStateException(error);}
    }
    public static void tick(GameController game){
        if(running==null)return;var scene=running;
        try{require(scene.game==game,"same controller");if(!scene.step())return;running=null;scene.close();SkyWars2.LOGGER.info("[check] PASS tiktok-network (native phone projection, framed compressed PLAY bytes, separate video/stereo audio totals, real TCP recipient and exact byte/CRC agreement)");}
        catch(Throwable failure){running=null;try{scene.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL tiktok-network",failure);}
    }
    private static final class Scene implements AutoCloseable {
        final GameController game;
        final WoolBucketChecks.Fixture f;
        final TikTokMedia media;
        final FileChannel spectral;
        final int spectralFrames;
        final Path output;
        final Socket sender,receiver;
        final CompletableFuture<Void> received=new CompletableFuture<>();
        final AtomicLong receivedBytes=new AtomicLong();
        final CRC32 sentCrc=new CRC32(),receivedCrc=new CRC32();
        final TikTokVoiceBudget voices=new TikTokVoiceBudget();
        final ItemStack phone;
        final int began;
        final long beganNanos=System.nanoTime();
        final List<Long> frameBytes=new ArrayList<>();
        final long[] videoBytes=new long[2],audioBytes=new long[2],protocolFrames=new long[2],soundEvents=new long[2];
        int profile;
        int frame;
        boolean sent,closed;
        @SuppressWarnings("unchecked") Scene(GameController game)throws Exception{
            this.game=game;
            var root=Path.of(Objects.requireNonNull(System.getProperty("skywars2.tiktok.network.reference")));
            var spectralPath=Path.of(Objects.requireNonNull(System.getProperty("skywars2.tiktok.network.spectral")));
            output=Path.of(Objects.requireNonNull(System.getProperty("skywars2.tiktok.network.output")));
            require(Files.isRegularFile(root.resolve("clip.tkp"))&&Files.isRegularFile(spectralPath),"offline sample files");
            f=new WoolBucketChecks.Fixture(game,false);f.prepareParticipants();
            media=new TikTokMedia("local",root);
            spectral=FileChannel.open(spectralPath,StandardOpenOption.READ);
            var header=ByteBuffer.allocate(16);while(header.hasRemaining())require(spectral.read(header)>=0,"spectral header");header.flip();
            require(header.getInt()==0x544b4131,"real spectral sidecar");spectralFrames=header.getInt();
            require(spectralFrames>0&&header.getInt()==20&&header.getInt()==88,"native spectral format");
            phone=new ItemStack(LbcSpecialArtifactItems.Kind.ADMIN_ZAIKA.item);f.owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,phone);
            try(var listener=new ServerSocket(0,1,InetAddress.getLoopbackAddress())){
                sender=new Socket(listener.getInetAddress(),listener.getLocalPort());receiver=listener.accept();
            }
            sender.setTcpNoDelay(true);receiver.setSoTimeout(30000);began=game.server().getTickCount();
            Thread.ofPlatform().daemon().name("skywars-media-network-recipient").start(()->{
                try{var input=receiver.getInputStream();byte[] buffer=new byte[65536];int n;
                    while((n=input.read(buffer))>=0){receivedCrc.update(buffer,0,n);receivedBytes.addAndGet(n);}received.complete(null);
                }catch(Throwable error){received.completeExceptionally(error);}
            });
        }
        byte[] spectral(int index)throws Exception{
            var data=ByteBuffer.allocate(TikTokMedia.SPECTRAL_BYTES);long pos=16L+(index%spectralFrames)*(long)TikTokMedia.SPECTRAL_BYTES;
            while(data.hasRemaining()){int n=spectral.read(data,pos+data.position());require(n>0,"spectral record");}return data.array();
        }
        void send(NetworkTransportChecks.Wire wire)throws Exception{
            sender.getOutputStream().write(wire.bytes());sentCrc.update(wire.bytes());protocolFrames[profile]+=wire.frames();
        }
        boolean step()throws Exception{
            f.client.acknowledge();f.audience.acknowledge();require(game.server().getTickCount()-began<2000,"bounded offline sample");
            if(frame<media.frames*2){
                profile=frame/media.frames;int index=frame%media.frames;
                var source=media.frame(index,false);
                var proxy = TikTokPlayback.frameStack(f.owner, phone, TikTokPixels.encode(source.map()));
                require(proxy.get(net.minecraft.core.component.DataComponents.ITEM_MODEL).equals(TikTokPlayback.VIDEO_MODEL),"actual vanilla phone factory");
                var video=NetworkTransportChecks.encode(f.owner,List.of(new ClientboundSetPlayerInventoryPacket(0,proxy)),true,256,false);
                long nanos=index*50_000_000L;int limit=profile==0?72:Math.min(72,voices.available(nanos,index));
                List<Packet<?>> tones=limit>0?new ArrayList<>(TikTokAudio.spectralPackets(f.owner,spectral(index),index,limit)):List.of();
                if(profile==1)voices.emitted(nanos,index,tones.size());soundEvents[profile]+=tones.size();
                var audio=NetworkTransportChecks.encode(f.owner,tones.isEmpty()?List.of():List.of(new ClientboundBundlePacket(new ArrayList<>(tones.stream().map(p->(Packet<? super net.minecraft.network.protocol.game.ClientGamePacketListener>)p).toList()))),true,256,false);
                videoBytes[profile]+=video.bytes().length;audioBytes[profile]+=audio.bytes().length;if(profile==0)frameBytes.add((long)video.bytes().length+audio.bytes().length);
                send(video);send(audio);frame++;return false;
            }
            if(!sent){sender.shutdownOutput();sent=true;return false;}
            if(!received.isDone())return false;received.join();
            require(receivedBytes.get()==videoBytes[0]+audioBytes[0]+videoBytes[1]+audioBytes[1]&&receivedCrc.getValue()==sentCrc.getValue(),"TCP recipient received exact native wire bytes");
            var bySecond=new ArrayList<Long>();for(int start=0;start<frameBytes.size();start+=20){long sum=0;for(int i=start;i<Math.min(start+20,frameBytes.size());i++)sum+=frameBytes.get(i);bySecond.add(sum);}
            require(videoBytes[0]==videoBytes[1],"both sound profiles use identical video wire cost");
            double seconds=media.frames/20.0;
            var result=new com.google.gson.JsonObject();result.addProperty("frames",media.frames);result.addProperty("sampleSeconds",seconds);result.addProperty("fps",20);result.addProperty("width",480);result.addProperty("height",854);result.addProperty("paletteEntries",248);result.addProperty("compressionThreshold",256);
            result.addProperty("videoBytes",videoBytes[0]);result.addProperty("audioBytes",audioBytes[0]);result.addProperty("totalBytes",videoBytes[0]+audioBytes[0]);result.addProperty("bytesPerSecond",(videoBytes[0]+audioBytes[0])/seconds);result.addProperty("megabitsPerSecond",(videoBytes[0]+audioBytes[0])*8/seconds/1_000_000);
            result.addProperty("patchedAudioBytesPerSecond",audioBytes[1]/seconds);result.addProperty("patchedTotalBytesPerSecond",(videoBytes[1]+audioBytes[1])/seconds);result.addProperty("patchedSoundEventsPerSecond",soundEvents[1]/seconds);
            result.addProperty("videoBytesPerSecond",videoBytes[0]/seconds);result.addProperty("audioBytesPerSecond",audioBytes[0]/seconds);result.addProperty("nativeProtocolFramesPerSecond",protocolFrames[0]/seconds);result.addProperty("soundEventsPerSecond",soundEvents[0]/seconds);result.addProperty("maxFullSecondBytes",bySecond.stream().limit(media.frames/20).mapToLong(Long::longValue).max().orElse(0));
            result.addProperty("tcpReceivedBytes",receivedBytes.get());result.addProperty("tcpCrcMatches",true);result.addProperty("measurementWallSeconds",(System.nanoTime()-beganNanos)/1e9);result.addProperty("scope","Two complete TCP passes: installed72-partial profile, then the new rolling192-voice budget. Real cached video plus separately sampled real spectral sidecar. Native Polymer projection, PLAY framing/zlib and exact loopback TCP payload. Normalized to20FPS; excludes TCP/IP headers, ACKs, retransmissions, ordinary game traffic and client render cost. No cookies or live feed.");
            Files.createDirectories(output.getParent());Files.writeString(output,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n");
            SkyWars2.LOGGER.info("[check] TikTok native wire sample: {} bytes/s, {} Mbit/s, {} video bytes/s, {} audio bytes/s, exact TCP recipient",(long)((videoBytes[0]+audioBytes[0])/seconds),(videoBytes[0]+audioBytes[0])*8/seconds/1_000_000,videoBytes[0]/seconds,audioBytes[0]/seconds);
            return true;
        }
        @Override public void close(){
            if(closed)return;closed=true;
            try{sender.close();receiver.close();spectral.close();media.close();}catch(Exception error){throw new IllegalStateException(error);}finally{f.close();}
        }
    }
}
