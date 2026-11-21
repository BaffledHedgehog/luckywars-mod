package me.skywars2.game.items.phone;

import me.skywars2.game.GameController;
import me.skywars2.game.NetworkTransportChecks;
import me.skywars2.game.SmokeVolumeChecks;
import me.skywars2.game.WoolBucketChecks;
import me.skywars2.game.effects.time.TimeStopView;
import me.skywars2.game.items.gadgets.LbcSpecialArtifactItems;
import me.skywars2.game.net.ServerNetworkTransport;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.LockSupport;
import me.skywars2.SkyWars2;
import me.skywars2.media.MediaClip;
import me.skywars2.media.MediaPackets;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;

/** Real native use/stream/cooldown, private projection, exact codecs and slow-channel admission. */
public final class TikTokPlaybackChecks {
    private TikTokPlaybackChecks(){}
    private static Fixture running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("TikTok playback: "+why);}
    private static List<ClientboundSetPlayerInventoryPacket> frames(List<Packet<?>> packets){
        return SmokeVolumeChecks.flatten(packets).stream().filter(ClientboundSetPlayerInventoryPacket.class::isInstance)
                .map(ClientboundSetPlayerInventoryPacket.class::cast)
                .filter(p->TikTokPlayback.VIDEO_MODEL.equals(p.contents().get(DataComponents.ITEM_MODEL))).toList();
    }
    private static <T> List<T> media(List<Packet<?>> packets,Class<T> payloadType){
        return SmokeVolumeChecks.flatten(packets).stream().filter(ClientboundCustomPayloadPacket.class::isInstance)
                .map(ClientboundCustomPayloadPacket.class::cast).map(ClientboundCustomPayloadPacket::payload)
                .filter(payloadType::isInstance).map(payloadType::cast).toList();
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;
        final Path directory;final TikTokCache cache;final TikTokPlayback playback,previous;
        final TikTokCache migrationCache;
        final java.lang.reflect.Field playbackField,channelField;final Object previousChannel;
        final EmbeddedChannel channel = new EmbeddedChannel();
        final ItemStack authored;
        final RelayFixture relay;
        volatile boolean echoMediaReceipts = true;
        final List<net.minecraft.world.entity.item.ItemEntity> dropped=new ArrayList<>();
        long started,windowStarted;int began,phase,when;long now,wireBytes;boolean closing,realReference,enhancedVerified;
        byte[] shapedPixels;
        int shapedPhase;
        long firstGameDelay, lastSlowReceipt;
        long pipelineStarted;int pipelineFrames,pipelineCursor;
        final Map<Integer,Long> pipelineReceipts=new LinkedHashMap<>();
        CompletableFuture<LinkSample> link;
        private record LinkSample(int videoBytes,long gameDelay) {}
        Fixture(GameController game)throws Exception{
            relay=RelayFixture.configured();
            this.game=game;f=new WoolBucketChecks.Fixture(game,false);f.prepareParticipants();
            directory=Files.createTempDirectory(game.server().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT),"tiktok-stream-");
            for(int i=0;i<2;i++){
                String id="1234567"+i;
                Path clipDirectory=directory.resolve("cache/videos").resolve(id);
                clip(clipDirectory,i);
                if (relay != null) {
                    relay.stageClip(clipDirectory, id);
                    if (i == 1) relay.stageSegmentVideo(clipDirectory);
                }
            }
            cache=new TikTokCache(directory.resolve("cache"),directory.resolve("no-cookies.json"),false);
            clip(directory.resolve("migration/videos/12345672"),2);
            migrationCache=new TikTokCache(directory.resolve("migration"),directory.resolve("no-migration-cookies.json"));
            playback=new TikTokPlayback(game.server(),cache);
            playbackField=TikTokAccounts.class.getDeclaredField("playback");playbackField.setAccessible(true);
            previous=(TikTokPlayback)playbackField.get(null);require(previous!=null,"registered real lifecycle service");playbackField.set(null,playback);
            channelField=net.minecraft.network.Connection.class.getDeclaredField("channel");channelField.setAccessible(true);
            previousChannel=channelField.get(f.client().connection());channelField.set(f.client().connection(),channel);
            var recorded=((Object)f.client().connection()).getClass().getDeclaredField("packets");recorded.setAccessible(true);
            channel.pipeline().addLast("packet_handler", new ChannelInboundHandlerAdapter());
            ServerNetworkTransport.configure(channel.pipeline(), PacketFlow.SERVERBOUND, true, ConnectionProtocol.PLAY);
            recorded.set(f.client().connection(), new java.util.concurrent.CopyOnWriteArrayList<Packet<?>>(f.client().packets()) {
                @Override public boolean add(Packet<?> packet) {
                    boolean added = super.add(packet);
                    if (echoMediaReceipts && packet instanceof ClientboundPingPacket ping) {
                        TikTokOutput.acknowledge(channel, ping.getId());
                    }
                    return added;
                }
            });
            authored=new ItemStack(LbcSpecialArtifactItems.Kind.ADMIN_ZAIKA.item);f.owner().getInventory().setSelectedSlot(0);f.owner().setItemInHand(InteractionHand.MAIN_HAND,authored.copy());
            began=when=f.level().getServer().getTickCount();
        }
        void use(){require(f.owner().gameMode.useItem(f.owner(),f.level(),f.owner().getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction(),"real native phone use accepted");}
        void next(int value){phase=value;when=game.server().getTickCount();windowStarted=System.nanoTime();f.client().packets().clear();f.audience().packets().clear();}
        void begin()throws Exception{
            pixels();audio();partialSoundReceipts();use();started=System.nanoTime();
            int amount=frames(f.client().packets()).size();use();
            require(frames(f.client().packets()).size()==amount,"hidden five-second cooldown does not restart video");
            var chats=SmokeVolumeChecks.flatten(f.client().packets()).stream().filter(ClientboundSystemChatPacket.class::isInstance).map(ClientboundSystemChatPacket.class::cast).toList();
            require(chats.stream().anyMatch(p->p.content().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                    &&t.getKey().equals("tiktok.loading")&&!p.content().getStyle().isItalic()&&p.content().getStyle().getColor().getValue()==0xff5555),"exact red nonitalic loading key");
            var clock=(TikTokSoundClock)TikTokCheckFields.read(playback,"audioClock");long slices=clock.emittedSlices.get();
            Thread.sleep(200);require(clock.emittedSlices.get()-slices>=7,"60Hz audio advances independently during an actual200ms fixture tick stall");
            next(0);
        }
        void partialSoundReceipts(){
            var output=TikTokOutput.forPlayer(f.owner());
            require(output!=null,"real PLAY output is available");
            byte[] bands=new byte[TikTokMedia.BANDS];
            Arrays.fill(bands,(byte)40);
            var slice=new ClientboundBundlePacket(TikTokAudio.packets(f.owner(),bands,1));
            echoMediaReceipts=false;
            f.client().packets().clear();
            try{
                long expires=System.nanoTime()+2_000_000_000L;
                for(int i=0;i<6;i++)require(output.offerSound(slice,expires,()->true),"initial audio credit "+i);
                require(!output.soundReady(),"six unacknowledged slices exhaust the audio window");
                int first=f.client().packets().stream().filter(ClientboundPingPacket.class::isInstance)
                        .map(ClientboundPingPacket.class::cast).findFirst().orElseThrow().getId();
                channel.writeInbound(new ServerboundPongPacket(first));
                require(channel.readInbound()==null&&output.soundReady(),"first real receipt opens one audio slot");
                require(output.offerSound(slice,expires,()->true)&&!output.soundReady(),
                        "a single Pong restores exactly one audio slot while the other five are unacknowledged");
                require(!output.offerSound(slice,expires,()->true),"no extra credit from the other unopened slices");
                var duplicate=new ServerboundPongPacket(first);
                channel.writeInbound(duplicate);
                require(channel.readInbound()==duplicate&&!output.soundReady(),"a repeated audio Pong never grants more credit");
                var stillPending=f.client().packets().stream().filter(ClientboundPingPacket.class::isInstance)
                        .map(ClientboundPingPacket.class::cast).map(ClientboundPingPacket::getId)
                        .filter(id->id!=first).toList();
                require(stillPending.size()==6,"one ordered receipt per outstanding slice");
                for(int id:stillPending.reversed())channel.writeInbound(new ServerboundPongPacket(id));
                require(channel.readInbound()==null&&output.soundReady()&&output.frameReady(),
                        "all outstanding receipts release audio without affecting independent video credit");
                SkyWars2.LOGGER.info("[check] TikTok sound window: six outstanding, one replacement per client receipt");
            }finally{
                echoMediaReceipts=true;
                f.client().packets().clear();
            }
        }
        void pixels()throws Exception{
            byte[] data=new byte[TikTokMedia.PIXELS];for(int i=0;i<data.length;i++)data[i]=(byte)(4+i%244);
            var model=TikTokPixels.encode(data);require(model.colors().size()==TikTokPixels.TINTS,"native triplet tint count");
            for(int y=0;y<TikTokMedia.HEIGHT;y++)for(int x=0;x<TikTokMedia.WIDTH;x++){
                int packed=model.colors().get(y*TikTokPixels.GROUPS_PER_ROW+x/3);int index=(packed>>(16-8*(x%3)))&255;
                require(index==Byte.toUnsignedInt(data[y*TikTokMedia.WIDTH+x]),"every native pixel decodes independently");
            }
            for(int step:new int[]{2,4,8,16}){
                var reduced=TikTokPixels.encode(data,step);
                require(reduced.colors().size()==TikTokPixels.TINTS,"slow link frames retain the native RP geometry");
                for(int y=0;y<TikTokMedia.HEIGHT;y++)for(int x=0;x<TikTokMedia.WIDTH;x++){
                    int packed=reduced.colors().get(y*TikTokPixels.GROUPS_PER_ROW+x/3);
                    int index=(packed>>(16-8*(x%3)))&255;
                    int sampleX=Math.min(x/step*step+step/2,TikTokMedia.WIDTH-1);
                    int sampleY=Math.min(y/step*step+step/2,TikTokMedia.HEIGHT-1);
                    require(index==Byte.toUnsignedInt(data[sampleY*TikTokMedia.WIDTH+sampleX]),"reduced frame preserves exact palette indices and edges");
                }
            }
            var raw=Unpooled.buffer();try{
                net.minecraft.world.item.component.CustomModelData.STREAM_CODEC.encode(raw,model);
                require(net.minecraft.world.item.component.CustomModelData.STREAM_CODEC.decode(raw).equals(model)&&!raw.isReadable(),"actual native custom-model-data codec accepts all136640 tints");
            }finally{raw.release();}
            String source=System.getProperty("skywars2.tiktok.reference");
            if(source!=null){
                var copy=directory.resolve("actual-for-you");Files.createDirectories(copy);
                for(String name:List.of("clip.tkp","clip.idx"))Files.copy(Path.of(source).resolve(name),copy.resolve(name));
                try(var actual=new TikTokMedia("12345678",copy)){
                    require(actual.frames>1,"real ForYou processed video has moving frames");
                    shapedPixels=actual.frame(0,false).map();
                    for(int frame:List.of(0,actual.frames/2,actual.frames-1)){
                        var payload=actual.frame(frame,true);var colours=TikTokPixels.encode(payload.map());
                        require(colours.colors().size()==TikTokPixels.TINTS&&payload.jpeg().length>4&&payload.jpeg()[0]==(byte)255&&payload.jpeg()[1]==(byte)216,"real worker binary/header/index/JPEG native reader compatibility");
                        require(payload.pcm().length==TikTokMedia.AUDIO_BYTES&&payload.bands().length==TikTokMedia.BAND_BYTES,"real worker audiovisual frame layout");
                    }
                }
                realReference=true;
            }
        }
        void beginLink(int step,int bytesPerSecond){
            var output=TikTokOutput.forPlayer(f.owner());
            require(output.vanillaPixelStep()==step,"expected link quality "+step+", actual "+output.vanillaPixelStep());
            echoMediaReceipts=false;
            f.client().packets().clear();
            var frame=new ClientboundSetPlayerInventoryPacket(1,
                    TikTokPlayback.frameStack(f.owner(),authored,TikTokPixels.encode(shapedPixels,step)));
            require(output.offerVanillaFrame(frame,System.nanoTime()+2_000_000_000L,()->true,step),"frame as real media output");
            var probes=f.client().packets().stream().filter(ClientboundPingPacket.class::isInstance)
                    .map(ClientboundPingPacket.class::cast).toList();
            require(probes.size()==2 && f.client().packets().indexOf(probes.getFirst())<f.client().packets().indexOf(frame)
                    && f.client().packets().indexOf(frame)<f.client().packets().indexOf(probes.getLast()),"two native Ping packets bracket the actual frame");
            var gamePacket=new ClientboundSystemChatPacket(Component.literal("gameplay after video"),false);
            f.owner().connection.send(gamePacket);
            require(f.client().packets().contains(gamePacket)&&output.outstandingFrames()==1,"gameplay is admitted while video is still unacknowledged");
            var encoded=NetworkTransportChecks.encode(f.owner(),List.of(probes.getFirst(),frame,probes.getLast(),gamePacket),true,256,false);
            require(encoded.frames()==4,"native compressed game and media frames");
            var pending=new CompletableFuture<LinkSample>();
            link=pending;
            Thread.ofVirtual().name("skywars-tiktok-shaped-link").start(()->{
                try(var acceptor=new ServerSocket(0,1,InetAddress.getLoopbackAddress());
                    var sender=new Socket(acceptor.getInetAddress(),acceptor.getLocalPort());
                    var receiver=acceptor.accept()){
                    receiver.setSoTimeout(4000);
                    var sending=CompletableFuture.runAsync(()->{
                        try{
                            sender.getOutputStream().write(encoded.bytes());
                            sender.shutdownOutput();
                        }catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}
                    });
                    var input=new DataInputStream(receiver.getInputStream());
                    long baseline=System.nanoTime()+180_000_000L;
                    int moved=0,fullFrame=0;
                    long first=0,gameDelay=0;
                    for(int index=0;index<4;index++){
                        int length=0,count=0,value;
                        do{
                            value=input.readUnsignedByte();
                            require(count<3,"native length prefix");
                            length|=(value&127)<<(7*count++);
                        }while((value&128)!=0);
                        require(input.readNBytes(length).length==length,"real TCP receives each entire PLAY packet");
                        moved+=count+length;
                        long due=baseline+1_000_000_000L*moved/bytesPerSecond;
                        long remaining;
                        while((remaining=due-System.nanoTime())>0)LockSupport.parkNanos(remaining);
                        long received=System.nanoTime();
                        if(index==0)first=received;
                        if(index==1)fullFrame=count+length;
                        if(index==3)gameDelay=received-first;
                        if(index==0||index==2){
                            int id=probes.get(index/2).getId();
                            game.server().execute(()->{if(channel.isActive())channel.writeInbound(new ServerboundPongPacket(id));});
                        }
                    }
                    sending.join();
                    require(input.read()==-1&&moved==encoded.bytes().length,"real loopback DTO exact length");
                    var result=new LinkSample(fullFrame,gameDelay);
                    game.server().execute(()->pending.complete(result));
                }catch(Throwable error){pending.completeExceptionally(error);}
            });
        }
        boolean shapedLink(){
            var output=TikTokOutput.forPlayer(f.owner());
            if(shapedPhase==0){
                beginLink(1,10_000_000);
                shapedPhase++;
                return false;
            }
            if(!link.isDone())return false;
            var sample=link.join();
            switch(shapedPhase){
                case 1->{
                    require(output.vanillaPixelStep()==1,"shared 180ms delay on fast link preserves native detail");
                    beginLink(1,125_000);
                }
                case 2->{
                    require(sample.videoBytes>50_000 && sample.gameDelay>350_000_000L && output.vanillaPixelStep()==4,
                            "1Mbit/s full frame delays the next gameplay packet, then drops wire cost");
                    firstGameDelay=sample.gameDelay;
                    beginLink(4,125_000);
                }
                case 3->{
                    require(sample.gameDelay<firstGameDelay/2 && output.vanillaPixelStep()==8,
                            "substantially smaller packets on the next stage of real shaped TCP");
                    beginLink(8,125_000);
                }
                case 4->{
                    require(sample.videoBytes<14_000 && sample.gameDelay<firstGameDelay/4 && output.vanillaPixelStep()==8,
                            "step-eight gameplay delivery is at least four times faster in the 1Mbit/s fixture");
                    SkyWars2.LOGGER.info("[check] TikTok shaped TCP: trailing gameplay delay {} -> {} ms at 1 Mbit/s, adapted frame {} bytes",
                            firstGameDelay/1_000_000,sample.gameDelay/1_000_000,sample.videoBytes);
                    beginLink(8,30_000);
                }
                case 5->{
                    require(output.vanillaPixelStep()==16,"lower bandwidth reduces quality to the minimum");
                    beginLink(16,30_000);
                }
                case 6->{
                    require(output.frameReady() && output.soundReady() && !output.vanillaFrameReady(),
                            "severe congestion backs off only vanilla video admission");
                    lastSlowReceipt=System.nanoTime();
                }
                case 7->{
                    if(System.nanoTime()-lastSlowReceipt<5_100_000_000L)return false;
                    echoMediaReceipts=true;
                    var frame=new ClientboundSetPlayerInventoryPacket(1,
                            TikTokPlayback.frameStack(f.owner(),authored,TikTokPixels.encode(shapedPixels,16)));
                    require(output.offerVanillaFrame(frame,System.nanoTime()+1_000_000_000L,()->true,16)
                            && output.vanillaPixelStep()==8,
                            "a fast receipt after cooldown restores quality one step at a time");
                    echoMediaReceipts=false;f.client().packets().clear();pipelineStarted=System.nanoTime();
                }
                case 8->{
                    long now=System.nanoTime();
                    var received=pipelineReceipts.entrySet().iterator();while(received.hasNext()){
                        var receipt=received.next();if(receipt.getValue()>now)continue;
                        channel.writeInbound(new ServerboundPongPacket(receipt.getKey()));received.remove();
                    }
                    var packets=f.client().packets();
                    while(pipelineCursor<packets.size()){
                        var packet=packets.get(pipelineCursor++);if(packet instanceof ClientboundPingPacket ping)pipelineReceipts.put(ping.getId(),now+180_000_000L);
                    }
                    if(now-pipelineStarted<3_000_000_000L){
                        if(output.vanillaFrameReady()){
                            var frame=new ClientboundSetPlayerInventoryPacket(1,TikTokPlayback.frameStack(f.owner(),authored,TikTokPixels.encode(shapedPixels,output.vanillaPixelStep())));
                            if(output.offerVanillaFrame(frame,now+500_000_000L,()->true,output.vanillaPixelStep()))pipelineFrames++;
                        }
                        require(output.outstandingFrames()<=4,"video backlog bounded during delayed client receipts");
                        return false;
                    }
                    if(output.outstandingFrames()>0)return false;
                    for(int receipt:pipelineReceipts.keySet())channel.writeInbound(new ServerboundPongPacket(receipt));pipelineReceipts.clear();
                    require(pipelineFrames>=30,"180ms client RTT must not impose stop-and-wait playback: frames="+pipelineFrames);
                    echoMediaReceipts=true;
                    SkyWars2.LOGGER.info("[check] TikTok pipeline: {} frames in3seconds at180ms receipt latency, at most4 outstanding",pipelineFrames);
                    return true;
                }
                default->throw new IllegalStateException("unexpected shaped link phase");
            }
            shapedPhase++;
            return false;
        }
        void audio(){
            TikTokVoiceBudget.verify();
            require(TikTokAudio.mixture(0,0,0)==null,"silence emits no audio");
            for(int a=0;a<=4;a++)for(int b=0;b<=4-a;b++){
                int c=4-a-b;var mix=TikTokAudio.mixture(a*63,b*63,c*63);
                require(mix.index()==a*(11-a)/2+b&&Float.isFinite(mix.volume())&&mix.volume()>0&&mix.volume()<=2,"every barycentric audio mixture native index/volume");
            }
            var packets=TikTokAudio.packets(f.owner(),new byte[TikTokMedia.BANDS],0);require(packets.isEmpty(),"silent frame no packets");
            byte[] bands=new byte[TikTokMedia.BANDS];Arrays.fill(bands,(byte)40);packets=TikTokAudio.packets(f.owner(),bands,1);
            require(packets.size()==16,"sixteen-sound limit per60Hz slice");
            for(var packet:packets){
                var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),f.owner().registryAccess());try{
                    ClientboundSoundEntityPacket.STREAM_CODEC.encode(buf,(ClientboundSoundEntityPacket)packet);
                    var decoded=ClientboundSoundEntityPacket.STREAM_CODEC.decode(buf);
                    require(decoded.getId()==f.owner().getId()&&decoded.getSound().value().location().getPath().startsWith("tiktok.v2.noise_"),"actual native direct custom sound event/entity codec");
                }finally{buf.release();}
            }
            for(int bin:new int[]{2,16,100,500,1149})for(int phase=0;phase<16;phase++)for(int side=0;side<2;side++){
                var record=ByteBuffer.allocate(TikTokMedia.SPECTRAL_BYTES);
                record.putShort((short)1).putShort((short)((bin<<5)|(phase<<1)|side)).putShort((short)32768);
                var tones=TikTokAudio.spectralPackets(f.owner(),record.array(),17);
                require(tones.size()==1,"one source coefficient is one native event");
                var tone=(ClientboundSoundEntityPacket)tones.getFirst();
                require(tone.getVolume()>0&&tone.getVolume()<=1&&tone.getPitch()>.97f&&tone.getPitch()<1.03f,"native gain/pitch clamps cannot change basis");
                require(tone.getSound().value().location().getPath().endsWith("_"+phase+"_"+side),"phase and signed stereo identity retained");
                var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),f.owner().registryAccess());try{
                    ClientboundSoundEntityPacket.STREAM_CODEC.encode(buf,tone);
                    var decoded=ClientboundSoundEntityPacket.STREAM_CODEC.decode(buf);
                    require(decoded.getId()==f.owner().getId()&&decoded.getSound().value().location().equals(tone.getSound().value().location())
                            &&decoded.getVolume()==tone.getVolume()&&decoded.getPitch()==tone.getPitch(),"actual native spectral event/volume/pitch codec");
                }finally{buf.release();}
            }
            require(TikTokAudio.spectralPackets(f.owner(),new byte[TikTokMedia.SPECTRAL_BYTES],0).isEmpty(),"spectral silence emits no events");
            var busy=ByteBuffer.allocate(TikTokMedia.SPECTRAL_BYTES).putShort((short)88);
            for(int i=0;i<88;i++)busy.putShort((short)(((2+i*13)<<5)|(i%2))).putShort((short)(200+i));
            var bounded=TikTokAudio.spectralPackets(f.owner(),busy.array(),0,72);
            require(bounded.size()==72&&bounded.stream().map(p->(ClientboundSoundEntityPacket)p)
                    .anyMatch(p->p.getSound().value().location().getPath().endsWith("_1")),"runtime voice budget retains signed stereo");
            require(bounded.stream().map(p->(ClientboundSoundEntityPacket)p).anyMatch(p->p.getSound().value().location().getPath().contains("tone_10")),"runtime voice budget keeps high-frequency detail");
            var invalid=ByteBuffer.allocate(TikTokMedia.SPECTRAL_BYTES).putShort((short)89).array();
            boolean rejected=false;try{TikTokAudio.spectralPackets(f.owner(),invalid,0);}catch(IllegalArgumentException expected){rejected=true;}
            require(rejected,"malformed sidecar cannot create an unbounded sound burst");
        }
        boolean step()throws Exception{
            now=System.nanoTime();int tick=game.server().getTickCount(),elapsed=tick-when;
            migrationCache.tick(tick,Set.of());
            require(tick-began<620,"bounded asynchronous native fixture");
            if(phase==0){
                if(now-windowStarted<1_200_000_000L)return false;
                var packets=frames(f.client().packets());require(packets.size()>=16&&packets.size()<=26,"actual near20FPS native packet cadence "+packets.size()+" elapsedMs="+((now-windowStarted)/1_000_000));
                require(packets.stream().map(p->p.contents().get(DataComponents.CUSTOM_MODEL_DATA)).distinct().count()>=12,"moving independent frame pixels");
                var last=packets.getLast();require(last.contents().is(Items.CARROT_ON_A_STICK)&&!last.contents().has(DataComponents.MAP_ID),"real held phone carrier, no map or extra entities");
                require(ItemStack.matches(authored,PolymerItemUtils.getRealItemStack(last.contents(),((net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)f.client().connection()).getPacketContext(),f.owner().registryAccess())),"projection roundtrip retains exact server phone/NBT");
                require(ItemStack.matches(authored,f.owner().getMainHandItem()),"all frames leave authoritative item unchanged");
                var normal = PolymerItemUtils.getPolymerItemStack(authored, TooltipFlag.NORMAL,
                        ((net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider) f.client().connection()).getPacketContext(), f.owner().registryAccess());
                require(Identifier.parse("lbc:zaika").equals(normal.get(DataComponents.ITEM_MODEL))
                        && !normal.has(DataComponents.CUSTOM_MODEL_DATA),
                        "ordinary slot, inventory and equipment projection cannot bypass the media window");
                require(frames(f.audience().packets()).isEmpty(),"frames private to viewer");
                var audio=SmokeVolumeChecks.flatten(f.client().packets()).stream().filter(ClientboundSoundEntityPacket.class::isInstance).map(ClientboundSoundEntityPacket.class::cast).filter(p->p.getSound().value().location().getPath().startsWith("tiktok.v2.noise_")).toList();
                require(!audio.isEmpty()&&audio.size()<=16*80,"independent60Hz sound clock emits bounded16-event slices");
                require(SmokeVolumeChecks.flatten(f.audience().packets()).stream().filter(ClientboundSoundEntityPacket.class::isInstance).map(ClientboundSoundEntityPacket.class::cast)
                        .noneMatch(p->p.getSound().value().location().getPath().startsWith("tiktok.v2.noise_")),"vocoder audio is also recipient-only");
                var wire=NetworkTransportChecks.encode(f.owner(),List.of(last),true,256,false);wireBytes=wire.bytes().length;
                require(wireBytes>0&&wireBytes<600000,"bounded actual native framed/compressed video bytes "+wireBytes);
                var payload=new RegistryFriendlyByteBuf(Unpooled.buffer(),f.owner().registryAccess());try{
                    ClientboundSetPlayerInventoryPacket.STREAM_CODEC.encode(payload,last);
                    var decoded=ClientboundSetPlayerInventoryPacket.STREAM_CODEC.decode(payload);
                    require(decoded.contents().get(DataComponents.CUSTOM_MODEL_DATA).equals(last.contents().get(DataComponents.CUSTOM_MODEL_DATA)),"actual full native item packet codec preserves frame colours");
                }finally{payload.release();}
                channel.unsafe().outboundBuffer().setUserDefinedWritability(1,false);next(1);return false;
            }
            if(phase==1){
                if(now-windowStarted<600_000_000L)return false;
                require(frames(f.client().packets()).isEmpty(),"unwritable channel admits no stale frames");
                var queue=(ThreadPoolExecutor)TikTokCheckFields.read(playback,"reader");require(queue.getQueue().isEmpty(),"slow network also stops frame work queue");
                channel.unsafe().outboundBuffer().setUserDefinedWritability(1,true);next(2);return false;
            }
            if(phase==2){
                if (now - windowStarted < 500_000_000L) return false;
                require(frames(f.client().packets()).size() >= 8 && frames(f.client().packets()).size() <= 12,
                        "resume latest frame clock without catch-up burst");
                echoMediaReceipts = false;
                next(11);
                return false;
            }
            if (phase == 11) {
                if (now - windowStarted < 350_000_000L) return false;
                var output = TikTokOutput.forPlayer(f.owner());
                int pendingFrames=frames(f.client().packets()).size();
                require(channel.isWritable()&&output!=null&&pendingFrames>=1&&pendingFrames<=4&&pendingFrames==output.outstandingFrames(),
                        "OS acceptance without client receipts cannot exceed the bounded adaptive video window");
                var noise = SmokeVolumeChecks.flatten(f.client().packets()).stream()
                        .filter(ClientboundSoundEntityPacket.class::isInstance).map(ClientboundSoundEntityPacket.class::cast)
                        .filter(p -> p.getSound().value().location().getPath().startsWith("tiktok.v2.noise_")).count();
                require(noise > 0 && noise <= 6 * 16, "independent bounded audio window: " + noise);
                require(!output.frameReady() && !output.soundReady(), "both media lanes wait for actual receipt");
                var foreign = new ServerboundPongPacket(123);
                channel.writeInbound(foreign);
                require(channel.readInbound() == foreign && !output.frameReady(), "unrelated Pong still reaches the game handler");
                var gameplay = new ClientboundSystemChatPacket(Component.literal("gameplay"), false);
                f.owner().connection.send(gameplay);
                require(f.client().packets().contains(gameplay), "gameplay can send while media waits");
                var receipts = f.client().packets().stream().filter(ClientboundPingPacket.class::isInstance)
                        .map(ClientboundPingPacket.class::cast).map(ClientboundPingPacket::getId).toList();
                for (int receipt : receipts) channel.writeInbound(new ServerboundPongPacket(receipt));
                while (channel.readInbound() != null) { }
                require(output.frameReady() && output.soundReady(), "native PLAY inbound handler releases both media receipts");
                for (int receipt : receipts) channel.writeInbound(new ServerboundPongPacket(receipt));
                require(channel.readInbound() instanceof ServerboundPongPacket, "a repeated receipt cannot open a second publication");
                while (channel.readInbound() != null) { }
                echoMediaReceipts = true;
                next(3);
                return false;
            }
            if(phase==3){
                if(now-started<5_200_000_000L)return false;
                use();require(((TikTokMedia)((Map<?,?>)TikTokCheckFields.read(playback,"views")).values().stream().map(v->TikTokCheckFields.read(v,"clip")).findFirst().orElseThrow()).id.equals("12345671"),"real right click advances after five seconds");
                var clock=(TikTokSoundClock)TikTokCheckFields.read(playback,"audioClock");long before=clock.emittedSlices.get();
                Thread.sleep(220);long emitted=clock.emittedSlices.get()-before;
                require(emitted>=3&&emitted<=6,"20Hz stereo clock advances independently through actual tick stall: "+emitted);
                next(4);return false;
            }
            if(phase==4){
                // Catch-up ticks after the deliberate220ms stall do not measure
                // wall-clock sound cadence. Keep an actual300ms observation.
                long observed=now-windowStarted;
                if(elapsed<6||observed<300_000_000L)return false;
                var upgraded=migrationCache.next("");
                require(upgraded!=null&&upgraded.hasSpectral()&&migrationCache.error().isEmpty(),"actual single background worker migrates old cache without cookies");
                require(Files.size(directory.resolve("migration/videos/12345672/original.mp4"))==1
                        &&Files.size(directory.resolve("migration/videos/12345672/clip.idx"))==160,"migration retains cached original/video index");
                var tones=SmokeVolumeChecks.flatten(f.client().packets()).stream().filter(ClientboundSoundEntityPacket.class::isInstance).map(ClientboundSoundEntityPacket.class::cast)
                        .filter(p->p.getSound().value().location().getPath().startsWith("tiktok.v3.tone_")).toList();
                long expected=observed/(1_000_000_000L/TikTokMedia.FPS);
                require(tones.size()>=2*(expected-2)&&tones.size()<=2*(expected+2),
                        "actual20Hz stereo transport, no60Hz duplicates: events="+tones.size()+" elapsedMs="+observed/1_000_000+" expectedFrames="+expected);
                require(SmokeVolumeChecks.flatten(f.audience().packets()).stream().filter(ClientboundSoundEntityPacket.class::isInstance).map(ClientboundSoundEntityPacket.class::cast)
                        .noneMatch(p->p.getSound().value().location().getPath().startsWith("tiktok.v3.tone_")),"stereo tones private to viewer");
                require(!Files.exists(directory.resolve("cache/videos/12345670"))&&Files.exists(directory.resolve("cache/videos/12345671/original.mp4")),"watched clip plus original removed only after its reader/view pin released");
                f.owner().getInventory().setItem(1,authored.copy());f.owner().getInventory().setSelectedSlot(1);next(5);return false;
            }
            if(phase==5){
                if(elapsed<4)return false;
                var all=SmokeVolumeChecks.flatten(f.client().packets()).stream().filter(ClientboundSetPlayerInventoryPacket.class::isInstance).map(ClientboundSetPlayerInventoryPacket.class::cast).toList();
                require(all.stream().anyMatch(p->p.slot()==0&&Identifier.parse("lbc:zaika").equals(p.contents().get(DataComponents.ITEM_MODEL))&&!p.contents().has(DataComponents.CUSTOM_MODEL_DATA)),"old hotbar slot restored to exact original presentation");
                require(frames(f.client().packets()).stream().anyMatch(p->p.slot()==1),"video follows native selected slot");
                f.owner().setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT));f.owner().setItemInHand(InteractionHand.OFF_HAND,authored.copy());next(6);return false;
            }
            if(phase==6){
                if(elapsed<4)return false;
                require(frames(f.client().packets()).stream().anyMatch(p->p.slot()==40),"offhand phone gets its real inventory slot");
                f.owner().setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);next(7);return false;
            }
            if(phase==7){
                if(elapsed<6)return false;
                require(playback.pins().isEmpty()&&frames(f.client().packets()).isEmpty(),"no held phone stops display/audio/I/O pins");
                f.owner().setItemInHand(InteractionHand.MAIN_HAND,authored.copy());next(8);return false;
            }
            if(phase==8){
                if(now-started<10_400_000_000L)return false;
                use();next(9);return false;
            }
            if(phase==9){
                if(elapsed<4)return false;
                require(!frames(f.client().packets()).isEmpty(),"real moving phone before Q drop");
                var before=Collections.newSetFromMap(new IdentityHashMap<net.minecraft.world.entity.Entity,Boolean>());f.level().getAllEntities().forEach(before::add);
                f.owner().drop(false);
                for(var entity:f.level().getAllEntities())if(!before.contains(entity)&&entity instanceof net.minecraft.world.entity.item.ItemEntity item)dropped.add(item);
                require(f.owner().getMainHandItem().isEmpty()&&dropped.size()==1&&ItemStack.matches(dropped.getFirst().getItem(),authored),"native Q creates original server phone without projected frame components");
                next(10);return false;
            }
            if(phase==10){
                if(elapsed<6)return false;
                require(playback.pins().isEmpty()&&frames(f.client().packets()).isEmpty(),"Q stops video and cannot republish stale frame");
                require(SmokeVolumeChecks.flatten(f.client().packets()).stream().filter(ClientboundSetPlayerInventoryPacket.class::isInstance)
                    .map(ClientboundSetPlayerInventoryPacket.class::cast).anyMatch(p->p.slot()==1&&p.contents().isEmpty()),"Q explicitly restores authoritative empty client slot");
                next(12);
            }
            if(!enhancedVerified){
                if(shapedPixels!=null&&!shapedLink())return false;
                enhanced();
                TikTokReplacementChecks.verify(game,f.owner(),cache.next(""),f.client().packets());
                enhancedVerified=true;
            }
            if(relay!=null&&!relay.step(this))return false;
            SkyWars2.LOGGER.info("[check] PASS tiktok-playback (native480p20FPS/60Hz old fallback,20Hz spectral stereo, actual cookie-free background cache upgrade; real Fabric Hello/version/channel gate, enhanced independent20Hz JPEG/48k stereo PCM, native shared codecs/JPEG bomb bound, no vanilla pixels/tones on enhanced path, private stream, bounded video/audio while writable, live receipt recovery, TimeStop, Q/Stop/empty-slot ordering; {} compressed vanilla bytes/frame, realForYouBinary={})",wireBytes,realReference);
            return true;
        }
        void enhanced()throws Exception{
            require(SmokeVolumeChecks.flatten(f.client().packets()).stream().noneMatch(p->p instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket c&&c.payload() instanceof me.skywars2.media.MediaPackets.Frame),"unmodified client never receives unsupported media payloads");
            MediaProtocolChecks.codecs(f.owner());MediaProtocolChecks.negotiate(f.owner());
            playback.reset();f.owner().setItemInHand(InteractionHand.MAIN_HAND,authored.copy());f.client().packets().clear();f.audience().packets().clear();
            use();require(playback.enhanced(f.owner()),"negotiated connection selects enhanced projection");
            var proxy=PolymerItemUtils.getPolymerItemStack(authored,TooltipFlag.NORMAL,((net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)f.client().connection()).getPacketContext(),f.owner().registryAccess());
            require(TikTokPlayback.CLIENT_MODEL.equals(proxy.get(DataComponents.ITEM_MODEL))&&!proxy.has(DataComponents.CUSTOM_MODEL_DATA),"enhanced renderer uses one quad and no per-frame tint NBT");
            Thread.sleep(280);var packets=SmokeVolumeChecks.flatten(f.client().packets());
            var video=packets.stream().filter(p->p instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket c&&c.payload() instanceof me.skywars2.media.MediaPackets.Frame).map(p->(me.skywars2.media.MediaPackets.Frame)((net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket)p).payload()).toList();
            require(video.size()>=4&&video.size()<=7,"enhanced20Hz clock advances through actual280ms world stall");
            require(video.stream().allMatch(p->p.width()==720&&p.height()==1280&&p.pcm().length==9600&&p.rate()==48000&&p.channels()==2),"bounded720p/48k stereo contract");
            require(packets.stream().noneMatch(ClientboundSoundEntityPacket.class::isInstance),"enhanced path never doubles PCM with vanilla vocoder");
            require(SmokeVolumeChecks.flatten(f.audience().packets()).stream().noneMatch(p->p instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket c&&c.payload() instanceof me.skywars2.media.MediaPackets.Frame),"enhanced video remains recipient-only");
            f.client().packets().clear();f.publishOwnerTimeStop();Thread.sleep(120);
            require(SmokeVolumeChecks.flatten(f.client().packets()).stream().noneMatch(p->p instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket c&&c.payload() instanceof me.skywars2.media.MediaPackets.Frame),"frozen client receives no future frames/PCM");
            TimeStopView.clear(f.owner());f.client().packets().clear();f.owner().setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);playback.tick();Thread.sleep(130);
            packets=SmokeVolumeChecks.flatten(f.client().packets());
            require(packets.stream().anyMatch(p->p instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket c&&c.payload() instanceof me.skywars2.media.MediaPackets.Stop),"unheld stops exact client session");
            require(packets.stream().noneMatch(p->p instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket c&&c.payload() instanceof me.skywars2.media.MediaPackets.Frame),"Stop gate prevents late frames after authoritative empty slot");
            require(!playback.enhanced(f.owner())&&ItemStack.matches(authored,PolymerItemUtils.getRealItemStack(proxy,((net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)f.client().connection()).getPacketContext(),f.owner().registryAccess())),"enhanced stream preserves exact server item/NBT and cleans screen");
        }
        public void close(){
            if(closing)return;closing=true;
            try{if(relay!=null)relay.close();playback.close();playbackField.set(null,previous);channelField.set(f.client().connection(),previousChannel);channel.finishAndReleaseAll();cache.close();migrationCache.close();dropped.forEach(net.minecraft.world.entity.Entity::discard);}
            catch(Exception error){throw new IllegalStateException("TikTok fixture service restore",error);}
            finally{
                f.close();try(var files=Files.walk(directory)){for(var path:files.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}
                catch(Exception error){throw new IllegalStateException("TikTok fixture files",error);}
            }
        }
    }
    private static final class RelayFixture implements AutoCloseable {
        final Path receipts;
        final URI origin;
        final byte[] mp4;
        final long expires=Instant.now().plusSeconds(300).getEpochSecond();
        final String cdn="https://fixture.tiktokcdn.com/video/tos/synthetic_fixture/?expire="+expires;
        HttpClient client;
        CompletableFuture<HttpResponse<byte[]>> download;
        UUID session;
        MediaPackets.Segment part;
        int phase;
        long since;
        static RelayFixture configured()throws Exception{
            String path=System.getProperty("skywars2.tiktok.relay.fixture");
            return path==null?null:new RelayFixture(Path.of(path));
        }
        RelayFixture(Path directory)throws Exception{
            origin=URI.create(Files.readString(directory.resolve("origin")).strip());
            int port=origin.getPort();
            require(port>0&&origin.equals(URI.create("https://127.0.0.1:"+port))
                    &&origin.toString().equals(System.getenv("SKYWARS2_MEDIA_RELAY_URI")),
                    "opt-in relay fixture uses the configured isolated TLS loopback");
            Path video=directory.resolve("original.mp4");
            require(Files.size(video)>32&&Files.size(video)<=1024*1024,"bounded generated MP4 fixture");
            mp4=Files.readAllBytes(video);
            require(new String(mp4,4,4,java.nio.charset.StandardCharsets.US_ASCII).equals("ftyp"),"local MP4 container");
            receipts=directory.resolve("puts");
            require(puts()==0,"fresh isolated relay with no uploads");
        }
        int puts()throws java.io.IOException{return Integer.parseInt(Files.readString(receipts).strip());}
        void stageClip(Path directory,String id)throws java.io.IOException{
            Files.write(directory.resolve("original.mp4"),mp4);
            var source=new com.google.gson.JsonObject();
            source.addProperty("clipId",id);
            source.addProperty("url",cdn);
            source.addProperty("expiresAt",expires);
            Files.writeString(directory.resolve("source.json"),source.toString());
        }
        void stageSegmentVideo(Path directory) throws IOException {
            int count = 2 * MediaClip.SEGMENT_FRAMES + 1;
            var picture = new java.awt.image.BufferedImage(720, 1280, java.awt.image.BufferedImage.TYPE_INT_RGB);
            byte[][] images = new byte[2][];
            for (int i = 0; i < images.length; i++) {
                picture.setRGB(20, 20, i == 0 ? 0xffff00 : 0x0022ff);
                var jpeg = new java.io.ByteArrayOutputStream();
                javax.imageio.ImageIO.write(picture, "jpeg", jpeg);
                images[i] = jpeg.toByteArray();
            }
            var map = new java.io.ByteArrayOutputStream();
            try (var zip = new java.util.zip.DeflaterOutputStream(map)) {
                byte[] pixels = new byte[TikTokMedia.PIXELS];
                Arrays.fill(pixels, (byte) 4);
                zip.write(pixels);
            }
            byte[] packed = map.toByteArray();
            var indices = ByteBuffer.allocate(count * Long.BYTES);
            try (var output = new java.io.DataOutputStream(Files.newOutputStream(directory.resolve("clip.tkp")))) {
                for (int value : new int[]{0x544b5033, count, TikTokMedia.FPS, TikTokMedia.WIDTH,
                        TikTokMedia.HEIGHT, TikTokMedia.RATE, TikTokMedia.BANDS}) output.writeInt(value);
                byte[] bands = new byte[TikTokMedia.BAND_BYTES];
                for (int i = 0; i < count; i++) {
                    indices.putLong(output.size());
                    byte[] image = images[i % images.length];
                    output.writeInt(packed.length);
                    output.writeInt(image.length);
                    output.write(packed);
                    output.write(bands);
                    output.write(ByteBuffer.allocate(TikTokMedia.AUDIO_BYTES).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                            .putShort((short) i).putShort((short) ~i).array());
                    output.write(image);
                }
            }
            Files.write(directory.resolve("clip.idx"), indices.array());
            var spectral = ByteBuffer.allocate(16 + count * TikTokMedia.SPECTRAL_BYTES);
            spectral.putInt(0x544b4131).putInt(count).putInt(20).putInt(88);
            for (int i = 0; i < count; i++) {
                int position = spectral.position();
                spectral.putShort((short) 2).putShort((short) (100 << 5)).putShort((short) 4096)
                        .putShort((short) ((200 << 5) | 1)).putShort((short) 2048);
                spectral.position(position + TikTokMedia.SPECTRAL_BYTES);
            }
            Files.write(directory.resolve("clip.tka"), spectral.array());
        }

        void getSegment(ServerPlayer player, MediaPackets.Segment advertised, long sequence) {
            part = MediaProtocolChecks.segmentWire(player, advertised);
            require(part.sequence() == sequence && part.totalFrames() == 81 && part.width() == 720 && part.height() == 1280,
                    "segmented route preserves exact clip length/full quality and monotonic sequence");
            var uri = URI.create(part.uri());
            require(origin.resolve("/").equals(uri.resolve("/")) && uri.getRawQuery() == null,
                    "segmented receipt uses only the trusted signed relay");
            download = client.sendAsync(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5)).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
        }

        boolean verifySegment(Fixture fixture) throws IOException {
            if (!download.isDone()) return false;
            var response = download.join();
            require(response.statusCode() == 200 && response.body().length <= MediaClip.MAX_SEGMENT_BYTES,
                    "actual authenticated HTTPS PUT and bounded signed GET");
            var temp = Files.createTempFile(fixture.directory, "part-", ".lwm");
            try {
                Files.write(temp, response.body());
                try (var clip = MediaClip.openSegment(temp, part.width(), part.height(), part.totalFrames(), part.sequence())) {
                    var cached = fixture.cache.next("");
                    require(clip.frameCount() == (part.sequence() % 3 == 2 ? 1 : 40), "actual exact segment interval");
                    int start = MediaClip.segmentIndex(part.sequence(), part.totalFrames()) * MediaClip.SEGMENT_FRAMES;
                    for (int i = 0; i < clip.frameCount(); i++) {
                        var frame = clip.next();
                        var expected = cached.clientFrame(start + i);
                        require(Arrays.equals(frame.jpeg(), expected.jpeg()) && Arrays.equals(frame.pcm(), expected.pcm()),
                                "no re-encoding, stereo swap or lost source frames across segment boundary");
                    }
                    require(clip.next() == null, "exact segment EOF");
                }
            } finally { Files.delete(temp); }
            return true;
        }

        void advance(Fixture fixture){phase++;since=fixture.now;}
        boolean step(Fixture fixture)throws Exception{
            var player=fixture.f.owner();
            var observer=fixture.f.other();
            var packets=fixture.f.client().packets();
            var others=fixture.f.audience().packets();
            var links=media(packets,MediaPackets.Link.class);
            var frames=media(packets,MediaPackets.Frame.class);
            var segments=media(packets,MediaPackets.Segment.class);
            long elapsed=fixture.now-since;
            require(media(others,MediaPackets.Link.class).isEmpty()&&media(others,MediaPackets.Frame.class).isEmpty()
                    &&media(others,MediaPackets.Stop.class).isEmpty()&&media(others,MediaPackets.Segment.class).isEmpty(),
                    "relay media stays on the receiving native connection");
            switch(phase){
                case 0->{
                    fixture.playback.reset();
                    MediaProtocolChecks.negotiateLinks(player);
                    MediaProtocolChecks.negotiateLinks(observer);
                    packets.clear();
                    player.setItemInHand(InteractionHand.MAIN_HAND,fixture.authored.copy());
                    fixture.use();
                    links=media(packets,MediaPackets.Link.class);
                    require(links.size()==1,"native phone sends an immediate direct Link");
                    var direct=MediaProtocolChecks.linkWire(player,links.getFirst());
                    require(direct.cdn().equals(cdn)&&direct.relay().isEmpty()&&!direct.relayMp4(),
                            "direct CDN advertisement contains only the authorized local test metadata");
                    session=direct.session();
                    MediaProtocolChecks.fallback(player,UUID.randomUUID(),false);
                    MediaProtocolChecks.fallback(observer,session,false);
                    MediaProtocolChecks.fallback(player,session,true);
                    advance(fixture);
                }
                case 1->{
                    if(elapsed<250_000_000L)return false;
                    require(links.size()==1&&frames.isEmpty()&&puts()==0,
                            "direct mode never uploads or streams and rejects foreign/stale/premature receipts");
                    MediaProtocolChecks.fallback(player,session,false);
                    MediaProtocolChecks.fallback(player,session,false);
                    advance(fixture);
                }
                case 2->{
                    require(frames.isEmpty(),"JPEG/PCM stays idle while the fallback uploads");
                    if(links.size()<2)return false;
                    require(links.size()==2&&puts()==1,"repeated fallback receipts publish one real authenticated upload");
                    var fallback=MediaProtocolChecks.linkWire(player,links.getLast());
                    require(fallback.session().equals(session)&&fallback.cdn().isEmpty()&&fallback.relayMp4(),
                            "relay-only MP4 Link identifies the original native session");
                    var signed=URI.create(fallback.relay());
                    require(origin.resolve("/").equals(signed.resolve("/"))&&signed.getRawQuery()==null
                            &&signed.getRawFragment()==null&&signed.getRawPath().matches("/luckywars-media/v1/[0-9a-f]{64}/[0-9]{10,12}/[0-9a-f]{64}"),
                            "signed download stays on the exact isolated HTTPS relay");
                    client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
                    download=client.sendAsync(HttpRequest.newBuilder(signed).timeout(Duration.ofSeconds(5)).GET().build(),
                            HttpResponse.BodyHandlers.ofByteArray());
                    advance(fixture);
                }
                case 3->{
                    require(frames.isEmpty()&&links.size()==2&&puts()==1,"relay play stays out of the game data path");
                    if(!download.isDone())return false;
                    var response=download.join();
                    require(response.statusCode()==200&&Arrays.equals(response.body(),mp4),
                            "actual HTTPS GET verifies signed download of the exact generated MP4");
                    MediaProtocolChecks.fallback(player,session,true);
                    advance(fixture);
                }
                case 4->{
                    if(frames.isEmpty())return false;
                    require(frames.stream().allMatch(p->p.session().equals(session)&&p.width()==720&&p.height()==1280
                            &&p.pcm().length==9600&&p.rate()==48000&&p.channels()==2)&&puts()==1&&links.size()==2,
                            "relay failure resumes the existing native JPEG/PCM route and session without another PUT");
                    player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
                    fixture.playback.tick();
                    require(media(packets,MediaPackets.Stop.class).stream().map(MediaPackets.Stop::session).toList().equals(List.of(session)),
                            "unholding the phone stops the original session once");
                    packets.clear();
                    MediaProtocolChecks.fallback(player,session,false);
                    advance(fixture);
                }
                case 5->{
                    if(elapsed<200_000_000L)return false;
                    require(frames.isEmpty()&&links.isEmpty()&&puts()==1,"stopped native session refuses later receipts and frames");
                    SkyWars2.LOGGER.info("[check] TikTok negotiated transport PASS (direct CDN Link, one on-demand TLS relay PUT and exact signed GET, same-session JPEG/PCM recovery, stale/foreign receipt and stop controls)");
                    fixture.playback.reset();
                    MediaProtocolChecks.negotiateSegments(player);
                    MediaProtocolChecks.negotiateSegments(observer);
                    packets.clear();
                    player.setItemInHand(InteractionHand.MAIN_HAND,fixture.authored.copy());
                    fixture.use();
                    advance(fixture);
                }
                case 6->{
                    require(links.isEmpty() && frames.isEmpty(), "no-FFmpeg enhanced client uses only segment receipts");
                    if (segments.isEmpty()) return false;
                    require(segments.size() == 1 && puts() == 2, "only initial requested segment was published");
                    getSegment(player, segments.getFirst(), 0);
                    session = part.session();
                    MediaProtocolChecks.requestSegment(player, UUID.randomUUID(), 1);
                    MediaProtocolChecks.requestSegment(observer, session, 1);
                    MediaProtocolChecks.requestSegment(player, session, 2);
                    advance(fixture);
                }
                case 7->{
                    if (elapsed < 250_000_000L || !verifySegment(fixture)) return false;
                    require(segments.size() == 1 && puts() == 2, "stale, foreign and skipped requests cannot publish a part");
                    MediaProtocolChecks.requestSegment(player, session, 1);
                    MediaProtocolChecks.requestSegment(player, session, 1);
                    advance(fixture);
                }
                case 8->{
                    if (segments.size() < 2) return false;
                    require(segments.size() == 2 && puts() == 3, "exactly one prefetch follows duplicate receipts");
                    getSegment(player, segments.getLast(), 1);
                    MediaProtocolChecks.requestSegment(player, session, 2);
                    MediaProtocolChecks.requestSegment(player, session, 3);
                    advance(fixture);
                }
                case 9->{
                    if (elapsed < 250_000_000L || !verifySegment(fixture)) return false;
                    require(segments.size() == 2 && puts() == 3, "a valid future page waits for its presentation cadence");
                    advance(fixture);
                }
                case 10->{
                    if (segments.size() < 3) return false;
                    require(segments.size() == 3 && puts() == 4 && links.isEmpty() && frames.isEmpty(),
                            "one demand-coded segment follows cadence without game media packets");
                    getSegment(player, segments.getLast(), 2);
                    advance(fixture);
                }
                case 11->{
                    if (!verifySegment(fixture)) return false;
                    MediaProtocolChecks.requestSegment(player, session, 3);
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    fixture.playback.tick();
                    require(media(packets,MediaPackets.Stop.class).stream().map(MediaPackets.Stop::session).toList().equals(List.of(session)),
                            "Stop retires exact segmented session and its queued next page");
                    packets.clear();
                    MediaProtocolChecks.requestSegment(player, session, 3);
                    advance(fixture);
                }
                case 12->{
                    if (elapsed < 2_100_000_000L) return false;
                    require(segments.isEmpty() && links.isEmpty() && frames.isEmpty() && puts() == 4
                                    && fixture.playback.pins().isEmpty(),
                            "stopped segment session releases clip and cannot publish from a queued or stale request");
                    SkyWars2.LOGGER.info("[check] TikTok segmented HTTPS PASS (3 real TLS parts, 40+40+1 frames, exact JPEG/stereo, per-connection requests, duplicate/out-of-order gates, cadence, Stop drops pending work)");
                    return true;
                }
                default->throw new IllegalStateException("unexpected relay fixture phase");
            }
            return false;
        }
        @Override public void close(){if(client!=null)client.shutdownNow();}
    }
    private static void clip(Path path,int seed)throws Exception{
        Files.createDirectories(path);Files.write(path.resolve("original.mp4"),new byte[]{1});
        int frames=20;
        var output=new java.io.ByteArrayOutputStream();var data=new java.io.DataOutputStream(output);
        data.writeInt(0x544b5033);data.writeInt(frames);data.writeInt(TikTokMedia.FPS);data.writeInt(TikTokMedia.WIDTH);data.writeInt(TikTokMedia.HEIGHT);data.writeInt(TikTokMedia.RATE);data.writeInt(TikTokMedia.BANDS);
        var indices=ByteBuffer.allocate(frames*8);var random=new Random(43+seed);
        for(int i=0;i<frames;i++){
            indices.putLong(output.size());byte[] pixels=new byte[TikTokMedia.PIXELS];
            for(int pixel=0;pixel<pixels.length;pixel++)pixels[pixel]=(byte)(4+random.nextInt(244));
            var packed=new java.io.ByteArrayOutputStream();try(var deflate=new java.util.zip.DeflaterOutputStream(packed)){deflate.write(pixels);}
            data.writeInt(packed.size());data.writeInt(4);packed.writeTo(data);
            byte[] bands=new byte[TikTokMedia.BAND_BYTES];Arrays.fill(bands,(byte)32);data.write(bands);
            data.write(new byte[TikTokMedia.AUDIO_BYTES]);data.write(new byte[]{(byte)255,(byte)216,(byte)255,(byte)217});
        }
        Files.write(path.resolve("clip.tkp"),output.toByteArray());Files.write(path.resolve("clip.idx"),indices.array());
        if(seed==1){
            var spectral=ByteBuffer.allocate(16+frames*TikTokMedia.SPECTRAL_BYTES);
            spectral.putInt(0x544b4131).putInt(frames).putInt(20).putInt(88);
            for(int i=0;i<frames;i++){
                int position=spectral.position();spectral.putShort((short)2);
                spectral.putShort((short)(100<<5)).putShort((short)4096);
                spectral.putShort((short)((200<<5)|1)).putShort((short)2048);
                spectral.position(position+TikTokMedia.SPECTRAL_BYTES);
            }
            Files.write(path.resolve("clip.tka"),spectral.array());
        }
    }
    public static void run(GameController game){require(running==null,"exclusive fixture");try{running=new Fixture(game);running.begin();}catch(Exception error){if(running!=null)running.close();running=null;throw new IllegalStateException("TikTok playback setup",error);}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL tiktok-playback",error);}}
}
