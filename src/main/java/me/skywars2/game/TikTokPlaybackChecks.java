package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
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
    private static final class Fixture implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;
        final Path directory;final TikTokCache cache;final TikTokPlayback playback,previous;
        final TikTokCache migrationCache;
        final java.lang.reflect.Field playbackField,channelField;final Object previousChannel;
        final EmbeddedChannel channel = new EmbeddedChannel();
        final ItemStack authored;
        volatile boolean echoMediaReceipts = true;
        final List<net.minecraft.world.entity.item.ItemEntity> dropped=new ArrayList<>();
        long started,windowStarted;int began,phase,when;long now,wireBytes;boolean closing,realReference;
        Fixture(GameController game)throws Exception{
            this.game=game;f=new WoolBucketChecks.Fixture(game,false);f.prepareParticipants();
            directory=Files.createTempDirectory(game.server().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT),"tiktok-stream-");
            for(int i=0;i<2;i++)clip(directory.resolve("cache/videos/1234567"+i),i);
            cache=new TikTokCache(directory.resolve("cache"),directory.resolve("no-cookies.json"),false);
            clip(directory.resolve("migration/videos/12345672"),2);
            migrationCache=new TikTokCache(directory.resolve("migration"),directory.resolve("no-migration-cookies.json"));
            playback=new TikTokPlayback(game.server(),cache);
            playbackField=TikTokAccounts.class.getDeclaredField("playback");playbackField.setAccessible(true);
            previous=(TikTokPlayback)playbackField.get(null);require(previous!=null,"registered real lifecycle service");playbackField.set(null,playback);
            channelField=net.minecraft.network.Connection.class.getDeclaredField("channel");channelField.setAccessible(true);
            previousChannel=channelField.get(f.client.connection);channelField.set(f.client.connection,channel);
            var recorded=((Object)f.client.connection).getClass().getDeclaredField("packets");recorded.setAccessible(true);
            channel.pipeline().addLast("packet_handler", new ChannelInboundHandlerAdapter());
            ServerNetworkTransport.configure(channel.pipeline(), PacketFlow.SERVERBOUND, true, ConnectionProtocol.PLAY);
            recorded.set(f.client.connection, new java.util.concurrent.CopyOnWriteArrayList<Packet<?>>(f.client.packets()) {
                @Override public boolean add(Packet<?> packet) {
                    boolean added = super.add(packet);
                    if (echoMediaReceipts && packet instanceof ClientboundPingPacket ping) {
                        TikTokOutput.acknowledge(channel, ping.getId());
                    }
                    return added;
                }
            });
            authored=new ItemStack(LbcSpecialArtifactItems.Kind.ADMIN_ZAIKA.item);f.owner.getInventory().setSelectedSlot(0);f.owner.setItemInHand(InteractionHand.MAIN_HAND,authored.copy());
            began=when=f.level.getServer().getTickCount();
        }
        void use(){require(f.owner.gameMode.useItem(f.owner,f.level,f.owner.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction(),"real native phone use accepted");}
        void next(int value){phase=value;when=game.server().getTickCount();windowStarted=System.nanoTime();f.client.packets().clear();f.audience.packets().clear();}
        void begin()throws Exception{
            pixels();audio();use();started=System.nanoTime();
            int amount=frames(f.client.packets()).size();use();
            require(frames(f.client.packets()).size()==amount,"hidden five-second cooldown does not restart video");
            var chats=SmokeVolumeChecks.flatten(f.client.packets()).stream().filter(ClientboundSystemChatPacket.class::isInstance).map(ClientboundSystemChatPacket.class::cast).toList();
            require(chats.stream().anyMatch(p->p.content().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                    &&t.getKey().equals("tiktok.loading")&&!p.content().getStyle().isItalic()&&p.content().getStyle().getColor().getValue()==0xff5555),"exact red nonitalic loading key");
            var clock=(TikTokSoundClock)WoolBucketChecks.read(playback,"audioClock");long slices=clock.emittedSlices.get();
            Thread.sleep(200);require(clock.emittedSlices.get()-slices>=7,"60Hz audio advances independently during an actual200ms fixture tick stall");
            next(0);
        }
        void pixels()throws Exception{
            byte[] data=new byte[TikTokMedia.PIXELS];for(int i=0;i<data.length;i++)data[i]=(byte)(4+i%244);
            var model=TikTokPixels.encode(data);require(model.colors().size()==TikTokPixels.TINTS,"native triplet tint count");
            for(int y=0;y<TikTokMedia.HEIGHT;y++)for(int x=0;x<TikTokMedia.WIDTH;x++){
                int packed=model.colors().get(y*TikTokPixels.GROUPS_PER_ROW+x/3);int index=(packed>>(16-8*(x%3)))&255;
                require(index==Byte.toUnsignedInt(data[y*TikTokMedia.WIDTH+x]),"every native pixel decodes independently");
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
                    for(int frame:List.of(0,actual.frames/2,actual.frames-1)){
                        var payload=actual.frame(frame,true);var colours=TikTokPixels.encode(payload.map());
                        require(colours.colors().size()==TikTokPixels.TINTS&&payload.jpeg().length>4&&payload.jpeg()[0]==(byte)255&&payload.jpeg()[1]==(byte)216,"real worker binary/header/index/JPEG native reader compatibility");
                        require(payload.pcm().length==TikTokMedia.AUDIO_BYTES&&payload.bands().length==TikTokMedia.BAND_BYTES,"real worker audiovisual frame layout");
                    }
                }
                realReference=true;
            }
        }
        void audio(){
            TikTokVoiceBudget.verify();
            require(TikTokAudio.mixture(0,0,0)==null,"silence emits no audio");
            for(int a=0;a<=4;a++)for(int b=0;b<=4-a;b++){
                int c=4-a-b;var mix=TikTokAudio.mixture(a*63,b*63,c*63);
                require(mix.index()==a*(11-a)/2+b&&Float.isFinite(mix.volume())&&mix.volume()>0&&mix.volume()<=2,"every barycentric audio mixture native index/volume");
            }
            var packets=TikTokAudio.packets(f.owner,new byte[TikTokMedia.BANDS],0);require(packets.isEmpty(),"silent frame no packets");
            byte[] bands=new byte[TikTokMedia.BANDS];Arrays.fill(bands,(byte)40);packets=TikTokAudio.packets(f.owner,bands,1);
            require(packets.size()==16,"sixteen-sound limit per60Hz slice");
            for(var packet:packets){
                var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),f.owner.registryAccess());try{
                    ClientboundSoundEntityPacket.STREAM_CODEC.encode(buf,(ClientboundSoundEntityPacket)packet);
                    var decoded=ClientboundSoundEntityPacket.STREAM_CODEC.decode(buf);
                    require(decoded.getId()==f.owner.getId()&&decoded.getSound().value().location().getPath().startsWith("tiktok.v2.noise_"),"actual native direct custom sound event/entity codec");
                }finally{buf.release();}
            }
            for(int bin:new int[]{2,16,100,500,1149})for(int phase=0;phase<16;phase++)for(int side=0;side<2;side++){
                var record=ByteBuffer.allocate(TikTokMedia.SPECTRAL_BYTES);
                record.putShort((short)1).putShort((short)((bin<<5)|(phase<<1)|side)).putShort((short)32768);
                var tones=TikTokAudio.spectralPackets(f.owner,record.array(),17);
                require(tones.size()==1,"one source coefficient is one native event");
                var tone=(ClientboundSoundEntityPacket)tones.getFirst();
                require(tone.getVolume()>0&&tone.getVolume()<=1&&tone.getPitch()>.97f&&tone.getPitch()<1.03f,"native gain/pitch clamps cannot change basis");
                require(tone.getSound().value().location().getPath().endsWith("_"+phase+"_"+side),"phase and signed stereo identity retained");
                var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),f.owner.registryAccess());try{
                    ClientboundSoundEntityPacket.STREAM_CODEC.encode(buf,tone);
                    var decoded=ClientboundSoundEntityPacket.STREAM_CODEC.decode(buf);
                    require(decoded.getId()==f.owner.getId()&&decoded.getSound().value().location().equals(tone.getSound().value().location())
                            &&decoded.getVolume()==tone.getVolume()&&decoded.getPitch()==tone.getPitch(),"actual native spectral event/volume/pitch codec");
                }finally{buf.release();}
            }
            require(TikTokAudio.spectralPackets(f.owner,new byte[TikTokMedia.SPECTRAL_BYTES],0).isEmpty(),"spectral silence emits no events");
            var busy=ByteBuffer.allocate(TikTokMedia.SPECTRAL_BYTES).putShort((short)88);
            for(int i=0;i<88;i++)busy.putShort((short)(((2+i*13)<<5)|(i%2))).putShort((short)(200+i));
            var bounded=TikTokAudio.spectralPackets(f.owner,busy.array(),0,72);
            require(bounded.size()==72&&bounded.stream().map(p->(ClientboundSoundEntityPacket)p)
                    .anyMatch(p->p.getSound().value().location().getPath().endsWith("_1")),"runtime voice budget retains signed stereo");
            require(bounded.stream().map(p->(ClientboundSoundEntityPacket)p).anyMatch(p->p.getSound().value().location().getPath().contains("tone_10")),"runtime voice budget keeps high-frequency detail");
            var invalid=ByteBuffer.allocate(TikTokMedia.SPECTRAL_BYTES).putShort((short)89).array();
            boolean rejected=false;try{TikTokAudio.spectralPackets(f.owner,invalid,0);}catch(IllegalArgumentException expected){rejected=true;}
            require(rejected,"malformed sidecar cannot create an unbounded sound burst");
        }
        boolean step()throws Exception{
            now=System.nanoTime();int tick=game.server().getTickCount(),elapsed=tick-when;
            migrationCache.tick(tick,Set.of());
            require(tick-began<400,"bounded asynchronous native fixture");
            if(phase==0){
                if(now-windowStarted<1_200_000_000L)return false;
                var packets=frames(f.client.packets());require(packets.size()>=16&&packets.size()<=26,"actual near20FPS native packet cadence "+packets.size()+" elapsedMs="+((now-windowStarted)/1_000_000));
                require(packets.stream().map(p->p.contents().get(DataComponents.CUSTOM_MODEL_DATA)).distinct().count()>=12,"moving independent frame pixels");
                var last=packets.getLast();require(last.contents().is(Items.CARROT_ON_A_STICK)&&!last.contents().has(DataComponents.MAP_ID),"real held phone carrier, no map or extra entities");
                require(ItemStack.matches(authored,PolymerItemUtils.getRealItemStack(last.contents(),((net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)f.client.connection).getPacketContext(),f.owner.registryAccess())),"projection roundtrip retains exact server phone/NBT");
                require(ItemStack.matches(authored,f.owner.getMainHandItem()),"all frames leave authoritative item unchanged");
                var normal = PolymerItemUtils.getPolymerItemStack(authored, TooltipFlag.NORMAL,
                        ((net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider) f.client.connection).getPacketContext(), f.owner.registryAccess());
                require(Identifier.parse("lbc:zaika").equals(normal.get(DataComponents.ITEM_MODEL))
                        && !normal.has(DataComponents.CUSTOM_MODEL_DATA),
                        "ordinary slot, inventory and equipment projection cannot bypass the media window");
                require(frames(f.audience.packets()).isEmpty(),"frames private to viewer");
                var audio=SmokeVolumeChecks.flatten(f.client.packets()).stream().filter(ClientboundSoundEntityPacket.class::isInstance).map(ClientboundSoundEntityPacket.class::cast).filter(p->p.getSound().value().location().getPath().startsWith("tiktok.v2.noise_")).toList();
                require(!audio.isEmpty()&&audio.size()<=16*80,"independent60Hz sound clock emits bounded16-event slices");
                require(SmokeVolumeChecks.flatten(f.audience.packets()).stream().filter(ClientboundSoundEntityPacket.class::isInstance).map(ClientboundSoundEntityPacket.class::cast)
                        .noneMatch(p->p.getSound().value().location().getPath().startsWith("tiktok.v2.noise_")),"vocoder audio is also recipient-only");
                var wire=NetworkTransportChecks.encode(f.owner,List.of(last),true,256,false);wireBytes=wire.bytes().length;
                require(wireBytes>0&&wireBytes<600000,"bounded actual native framed/compressed video bytes "+wireBytes);
                var payload=new RegistryFriendlyByteBuf(Unpooled.buffer(),f.owner.registryAccess());try{
                    ClientboundSetPlayerInventoryPacket.STREAM_CODEC.encode(payload,last);
                    var decoded=ClientboundSetPlayerInventoryPacket.STREAM_CODEC.decode(payload);
                    require(decoded.contents().get(DataComponents.CUSTOM_MODEL_DATA).equals(last.contents().get(DataComponents.CUSTOM_MODEL_DATA)),"actual full native item packet codec preserves frame colours");
                }finally{payload.release();}
                channel.unsafe().outboundBuffer().setUserDefinedWritability(1,false);next(1);return false;
            }
            if(phase==1){
                if(now-windowStarted<600_000_000L)return false;
                require(frames(f.client.packets()).isEmpty(),"unwritable channel admits no stale frames");
                var queue=(ThreadPoolExecutor)WoolBucketChecks.read(playback,"reader");require(queue.getQueue().isEmpty(),"slow network also stops frame work queue");
                channel.unsafe().outboundBuffer().setUserDefinedWritability(1,true);next(2);return false;
            }
            if(phase==2){
                if (now - windowStarted < 500_000_000L) return false;
                require(frames(f.client.packets()).size() >= 8 && frames(f.client.packets()).size() <= 12,
                        "resume latest frame clock without catch-up burst");
                echoMediaReceipts = false;
                next(11);
                return false;
            }
            if (phase == 11) {
                if (now - windowStarted < 350_000_000L) return false;
                require(channel.isWritable() && frames(f.client.packets()).size() == 1,
                        "OS acceptance without a client receipt admits only one video frame");
                var noise = SmokeVolumeChecks.flatten(f.client.packets()).stream()
                        .filter(ClientboundSoundEntityPacket.class::isInstance).map(ClientboundSoundEntityPacket.class::cast)
                        .filter(p -> p.getSound().value().location().getPath().startsWith("tiktok.v2.noise_")).count();
                require(noise > 0 && noise <= 6 * 16, "independent bounded audio window: " + noise);
                var output = TikTokOutput.forPlayer(f.owner);
                require(output != null && !output.frameReady() && !output.soundReady(), "both media lanes wait for actual receipt");
                var foreign = new ServerboundPongPacket(123);
                channel.writeInbound(foreign);
                require(channel.readInbound() == foreign && !output.frameReady(), "unrelated Pong still reaches the game handler");
                var gameplay = new ClientboundSystemChatPacket(Component.literal("gameplay"), false);
                f.owner.connection.send(gameplay);
                require(f.client.packets().contains(gameplay), "gameplay can send while media waits");
                var receipts = f.client.packets().stream().filter(ClientboundPingPacket.class::isInstance)
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
                use();require(((TikTokMedia)((Map<?,?>)WoolBucketChecks.read(playback,"views")).values().stream().map(v->WoolBucketChecks.read(v,"clip")).findFirst().orElseThrow()).id.equals("12345671"),"real right click advances after five seconds");
                var clock=(TikTokSoundClock)WoolBucketChecks.read(playback,"audioClock");long before=clock.emittedSlices.get();
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
                var tones=SmokeVolumeChecks.flatten(f.client.packets()).stream().filter(ClientboundSoundEntityPacket.class::isInstance).map(ClientboundSoundEntityPacket.class::cast)
                        .filter(p->p.getSound().value().location().getPath().startsWith("tiktok.v3.tone_")).toList();
                long expected=observed/(1_000_000_000L/TikTokMedia.FPS);
                require(tones.size()>=2*(expected-2)&&tones.size()<=2*(expected+2),
                        "actual20Hz stereo transport, no60Hz duplicates: events="+tones.size()+" elapsedMs="+observed/1_000_000+" expectedFrames="+expected);
                require(SmokeVolumeChecks.flatten(f.audience.packets()).stream().filter(ClientboundSoundEntityPacket.class::isInstance).map(ClientboundSoundEntityPacket.class::cast)
                        .noneMatch(p->p.getSound().value().location().getPath().startsWith("tiktok.v3.tone_")),"stereo tones private to viewer");
                require(!Files.exists(directory.resolve("cache/videos/12345670"))&&Files.exists(directory.resolve("cache/videos/12345671/original.mp4")),"watched clip plus original removed only after its reader/view pin released");
                f.owner.getInventory().setItem(1,authored.copy());f.owner.getInventory().setSelectedSlot(1);next(5);return false;
            }
            if(phase==5){
                if(elapsed<4)return false;
                var all=SmokeVolumeChecks.flatten(f.client.packets()).stream().filter(ClientboundSetPlayerInventoryPacket.class::isInstance).map(ClientboundSetPlayerInventoryPacket.class::cast).toList();
                require(all.stream().anyMatch(p->p.slot()==0&&Identifier.parse("lbc:zaika").equals(p.contents().get(DataComponents.ITEM_MODEL))&&!p.contents().has(DataComponents.CUSTOM_MODEL_DATA)),"old hotbar slot restored to exact original presentation");
                require(frames(f.client.packets()).stream().anyMatch(p->p.slot()==1),"video follows native selected slot");
                f.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIRT));f.owner.setItemInHand(InteractionHand.OFF_HAND,authored.copy());next(6);return false;
            }
            if(phase==6){
                if(elapsed<4)return false;
                require(frames(f.client.packets()).stream().anyMatch(p->p.slot()==40),"offhand phone gets its real inventory slot");
                f.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);next(7);return false;
            }
            if(phase==7){
                if(elapsed<6)return false;
                require(playback.pins().isEmpty()&&frames(f.client.packets()).isEmpty(),"no held phone stops display/audio/I/O pins");
                f.owner.setItemInHand(InteractionHand.MAIN_HAND,authored.copy());next(8);return false;
            }
            if(phase==8){
                if(now-started<10_400_000_000L)return false;
                use();next(9);return false;
            }
            if(phase==9){
                if(elapsed<4)return false;
                require(!frames(f.client.packets()).isEmpty(),"real moving phone before Q drop");
                var before=Collections.newSetFromMap(new IdentityHashMap<net.minecraft.world.entity.Entity,Boolean>());f.level.getAllEntities().forEach(before::add);
                f.owner.drop(false);
                for(var entity:f.level.getAllEntities())if(!before.contains(entity)&&entity instanceof net.minecraft.world.entity.item.ItemEntity item)dropped.add(item);
                require(f.owner.getMainHandItem().isEmpty()&&dropped.size()==1&&ItemStack.matches(dropped.getFirst().getItem(),authored),"native Q creates original server phone without projected frame components");
                next(10);return false;
            }
            if(elapsed<6)return false;
            require(playback.pins().isEmpty()&&frames(f.client.packets()).isEmpty(),"Q stops video and cannot republish stale frame");
            require(SmokeVolumeChecks.flatten(f.client.packets()).stream().filter(ClientboundSetPlayerInventoryPacket.class::isInstance)
                    .map(ClientboundSetPlayerInventoryPacket.class::cast).anyMatch(p->p.slot()==1&&p.contents().isEmpty()),"Q explicitly restores authoritative empty client slot");
            enhanced();
            SkyWars2.LOGGER.info("[check] PASS tiktok-playback (native480p20FPS/60Hz old fallback,20Hz spectral stereo, actual cookie-free background cache upgrade; real Fabric Hello/version/channel gate, enhanced independent20Hz JPEG/48k stereo PCM, native shared codecs/JPEG bomb bound, no vanilla pixels/tones on enhanced path, private stream, bounded video/audio while writable, live receipt recovery, TimeStop, Q/Stop/empty-slot ordering; {} compressed vanilla bytes/frame, realForYouBinary={})",wireBytes,realReference);
            return true;
        }
        void enhanced()throws Exception{
            require(SmokeVolumeChecks.flatten(f.client.packets()).stream().noneMatch(p->p instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket c&&c.payload() instanceof me.skywars2.media.MediaPackets.Frame),"unmodified client never receives unsupported media payloads");
            MediaProtocolChecks.codecs(f.owner);MediaProtocolChecks.negotiate(f.owner);
            playback.reset();f.owner.setItemInHand(InteractionHand.MAIN_HAND,authored.copy());f.client.packets().clear();f.audience.packets().clear();
            use();require(playback.enhanced(f.owner),"negotiated connection selects enhanced projection");
            var proxy=PolymerItemUtils.getPolymerItemStack(authored,TooltipFlag.NORMAL,((net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)f.client.connection).getPacketContext(),f.owner.registryAccess());
            require(TikTokPlayback.CLIENT_MODEL.equals(proxy.get(DataComponents.ITEM_MODEL))&&!proxy.has(DataComponents.CUSTOM_MODEL_DATA),"enhanced renderer uses one quad and no per-frame tint NBT");
            Thread.sleep(280);var packets=SmokeVolumeChecks.flatten(f.client.packets());
            var video=packets.stream().filter(p->p instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket c&&c.payload() instanceof me.skywars2.media.MediaPackets.Frame).map(p->(me.skywars2.media.MediaPackets.Frame)((net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket)p).payload()).toList();
            require(video.size()>=4&&video.size()<=7,"enhanced20Hz clock advances through actual280ms world stall");
            require(video.stream().allMatch(p->p.width()==720&&p.height()==1280&&p.pcm().length==9600&&p.rate()==48000&&p.channels()==2),"bounded720p/48k stereo contract");
            require(packets.stream().noneMatch(ClientboundSoundEntityPacket.class::isInstance),"enhanced path never doubles PCM with vanilla vocoder");
            require(SmokeVolumeChecks.flatten(f.audience.packets()).stream().noneMatch(p->p instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket c&&c.payload() instanceof me.skywars2.media.MediaPackets.Frame),"enhanced video remains recipient-only");
            f.client.packets().clear();TimeStopView.publish(f.owner,true);Thread.sleep(120);
            require(SmokeVolumeChecks.flatten(f.client.packets()).stream().noneMatch(p->p instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket c&&c.payload() instanceof me.skywars2.media.MediaPackets.Frame),"frozen client receives no future frames/PCM");
            TimeStopView.clear(f.owner);f.client.packets().clear();f.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);playback.tick();Thread.sleep(130);
            packets=SmokeVolumeChecks.flatten(f.client.packets());
            require(packets.stream().anyMatch(p->p instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket c&&c.payload() instanceof me.skywars2.media.MediaPackets.Stop),"unheld stops exact client session");
            require(packets.stream().noneMatch(p->p instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket c&&c.payload() instanceof me.skywars2.media.MediaPackets.Frame),"Stop gate prevents late frames after authoritative empty slot");
            require(!playback.enhanced(f.owner)&&ItemStack.matches(authored,PolymerItemUtils.getRealItemStack(proxy,((net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)f.client.connection).getPacketContext(),f.owner.registryAccess())),"enhanced stream preserves exact server item/NBT and cleans screen");
        }
        public void close(){
            if(closing)return;closing=true;
            try{playback.close();playbackField.set(null,previous);channelField.set(f.client.connection,previousChannel);channel.finishAndReleaseAll();cache.close();migrationCache.close();dropped.forEach(net.minecraft.world.entity.Entity::discard);}
            catch(Exception error){throw new IllegalStateException("TikTok fixture service restore",error);}
            finally{
                f.close();try(var files=Files.walk(directory)){for(var path:files.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}
                catch(Exception error){throw new IllegalStateException("TikTok fixture files",error);}
            }
        }
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
