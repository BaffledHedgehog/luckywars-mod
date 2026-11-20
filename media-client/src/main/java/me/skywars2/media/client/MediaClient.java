package me.skywars2.media.client;

import com.mojang.blaze3d.audio.Library;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import me.skywars2.media.MediaClip;
import me.skywars2.media.MediaPackets;
import me.skywars2.media.MediaRelayEndpoint;
import me.skywars2.media.client.mixin.MediaSoundAccess;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.sounds.*;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;

/** Optional phone player. Both transports feed the same sound engine and texture. */
public final class MediaClient implements ClientModInitializer {
    private static final Identifier TEXTURE=Identifier.parse("skywars2:phone_stream");
    private static final Object LOCK=new Object();
    private static final LinkedHashSet<UUID> retired=new LinkedHashSet<>();
    private static final ExecutorService decoder=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"skywars-media-decode");t.setDaemon(true);t.setPriority(Thread.MIN_PRIORITY);return t;});
    private static final ExecutorService linkDecoder = Executors.newSingleThreadExecutor(r -> {
        var thread = new Thread(r, "skywars-media-https");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });
    private static final URI relayOrigin = relayOrigin();
    private static final Optional<Mp4Decoder.Tools> mp4Tools = Mp4Decoder.findTools();
    private static final ArrayBlockingQueue<Decoded> decoded=new ArrayBlockingQueue<>(8);
    private static Session session;
    private static MediaPackets.Frame pending;
    private static MediaPackets.Link pausedLink;
    private static MediaDownload downloads;
    private static boolean decoding,paused,announced;
    private static DynamicTexture texture;
    private record Decoded(Session owner,long serial,NativeImage image){}
    private static final class Session {
        final UUID id;final int rate,channels;final MediaTimeline timeline;
        final MediaSegmentQueue segments;
        volatile ChannelAccess.ChannelHandle sound;
        volatile float volume;
        volatile boolean active=true;
        volatile boolean silent;
        final MediaPackets.Link link;
        volatile Mp4Decoder mp4;
        Future<?> linkedWork;
        Boolean deferredFallback;
        long deferredSegment;
        boolean linked() { return link != null || segments != null; }
        boolean soundReady,soundStarted;
        boolean opening;long shown=-1,received,last=-1;
        Session(MediaPackets.Frame p){id=p.session();rate=p.rate();channels=p.channels();link=null;segments=null;timeline=new MediaTimeline();}
        Session(MediaPackets.Link link){this.link=link;id=link.session();rate=48000;channels=2;segments=null;timeline=new MediaTimeline();}
        Session(MediaPackets.Segment part){
            id=part.session();rate=48000;channels=2;link=null;segments=new MediaSegmentQueue(part);
            timeline=new MediaTimeline(true);
        }
    }
    @Override public void onInitializeClient(){
        MediaPackets.register();
        SpecialModelRenderers.ID_MAPPER.put(Identifier.parse("skywars2:phone_video"),PhoneRenderer.Unbaked.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(MediaPackets.Frame.TYPE,(p,c)->receive(p));
        ClientPlayNetworking.registerGlobalReceiver(MediaPackets.Link.TYPE,(p,c)->receive(p));
        ClientPlayNetworking.registerGlobalReceiver(MediaPackets.Segment.TYPE,(p,c)->receive(p));
        ClientPlayNetworking.registerGlobalReceiver(MediaPackets.Stop.TYPE,(p,c)->{synchronized(LOCK){
            retire(p.session());
            if(pausedLink!=null&&pausedLink.session().equals(p.session()))pausedLink=null;
            if(session!=null&&session.id.equals(p.session()))close(false);
        }});
        ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->{
            reset(); // Server channel advertisement may arrive after the JOIN callback.
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->reset());
        ClientTickEvents.END_CLIENT_TICK.register(client->tick());
    }
    public static Identifier texture(){synchronized(LOCK){return texture!=null&&session!=null?TEXTURE:null;}}
    private static void retire(UUID id){retired.add(id);while(retired.size()>32)retired.remove(retired.iterator().next());}
    private static void reset(){synchronized(LOCK){close(false);pausedLink=null;retired.clear();paused=false;announced=false;}}
    private static void close(boolean keepIdentity){
        if(session!=null){session.active=false;session.timeline.close();
            var movie=session.mp4;
            if(movie!=null)movie.cancel();
            if(session.segments!=null)session.segments.close();
            if(session.linkedWork!=null)session.linkedWork.cancel(true);
            if(session.sound!=null)session.sound.execute(c->c.stop());if(keepIdentity)retire(session.id);session=null;}
        pending=null;Decoded d;while((d=decoded.poll())!=null)d.image.close();
        if(texture!=null){Minecraft.getInstance().getTextureManager().release(TEXTURE);texture=null;}
        LOCK.notifyAll();
    }
    public static void frozen(boolean value){synchronized(LOCK){
        if(paused==value)return;
        paused=value;
        if (value) {
            if (session == null || !session.linked()) close(false);
            else pauseLinked(session, true);
        } else {
            if (pausedLink != null) {
                var link = pausedLink;
                pausedLink = null;
                receive(link);
            }
            if (session != null && session.linked()) {
                pauseLinked(session, false);
                if (session.deferredFallback != null) {
                    boolean relayFailed = session.deferredFallback;
                    session.deferredFallback = null;
                    session.deferredSegment = 0;
                    signalFallback(session, relayFailed);
                } else if (session.deferredSegment != 0) {
                    var sequence = session.deferredSegment;
                    session.deferredSegment = 0;
                    sendRequest(session, sequence);
                }
            }
            LOCK.notifyAll();
        }
    }}

    private static void pauseLinked(Session selected, boolean value) {
        selected.timeline.setPaused(value, System.nanoTime());
        var handle = selected.sound;
        if (handle == null) return;
        handle.execute(channel -> {
            synchronized (LOCK) {
                if (session != selected || selected.sound != handle || paused != value || !selected.soundReady) return;
                if (value) channel.pause();
                else if (selected.soundStarted) channel.unpause();
                else {
                    channel.play();
                    selected.soundStarted = true;
                }
            }
        });
    }

    private static URI relayOrigin() {
        String configured = System.getProperty("skywars2.media.relay");
        if (configured == null || configured.isBlank()) return MediaRelayEndpoint.VELOCITY;
        try {
            URI origin = URI.create(configured);
            MediaDownload.validateRelayOrigin(origin);
            return origin;
        }
        catch (IllegalArgumentException invalid) { return null; }
    }

    private static void receive(MediaPackets.Link link) {
        synchronized(LOCK) {
            if(relayOrigin==null||retired.contains(link.session()))return;
            boolean sameSession = session != null && session.id.equals(link.session());
            var previous = pausedLink != null ? pausedLink : sameSession ? session.link : null;
            if (previous != null && previous.session().equals(link.session())
                    && (link.relay().isEmpty()
                    || previous.relay().equals(link.relay()) && previous.relayMp4() == link.relayMp4())) return;
            if (paused) {
                if (pausedLink != null && !pausedLink.session().equals(link.session())) retire(pausedLink.session());
                if (session != null && !sameSession) close(true);
                pausedLink = link;
                return;
            }
            if (sameSession && link.relay().isEmpty()) return;
            if (!ensureDownloads()) return;
            close(!sameSession);
            var selected=new Session(link);
            session=selected;
            selected.linkedWork=linkDecoder.submit(()->decodeLink(selected));
        }
    }

    private static boolean ensureDownloads() {
        if (downloads != null) return true;
        Path cache = Minecraft.getInstance().gameDirectory.toPath().resolve("skywars2-phone-cache");
        try {
            downloads = new MediaDownload(cache, relayOrigin);
            return true;
        } catch (IllegalArgumentException invalid) { return false; }
    }

    private static void receive(MediaPackets.Segment part) {
        synchronized (LOCK) {
            if (relayOrigin == null || retired.contains(part.session())) return;
            boolean sameSession = session != null && session.id.equals(part.session());
            if (sameSession && session.segments != null) {
                offerSegment(session, part);
                return;
            }
            if (part.sequence() != 0 || !ensureDownloads()) return;
            if (pausedLink != null && !pausedLink.session().equals(part.session())) retire(pausedLink.session());
            pausedLink = null;
            close(!sameSession);
            var selected = new Session(part);
            session = selected;
            if (paused) pauseLinked(selected, true);
            if (offerSegment(selected, part)) {
                selected.linkedWork = linkDecoder.submit(() -> decodeSegments(selected));
            }
        }
    }

    private static boolean offerSegment(Session selected, MediaPackets.Segment part) {
        try {
            // URL is checked against the existing configured Velocity origin before opening any connection.
            return selected.segments.offer(part, uri -> downloads.segment(URI.create(uri)));
        } catch (IllegalArgumentException unavailable) {
            signalFallback(selected, true);
            return false;
        }
    }

    private static void requestNext(Session selected, long sequence) {
        Minecraft.getInstance().execute(() -> {
            synchronized (LOCK) {
                if (session != selected || !selected.active) return;
                if (paused) selected.deferredSegment = sequence;
                else sendRequest(selected, sequence);
            }
        });
    }

    private static void sendRequest(Session selected, long sequence) {
        if (ClientPlayNetworking.canSend(MediaPackets.RequestSegment.TYPE) && selected.segments.expect(sequence)) {
            ClientPlayNetworking.send(new MediaPackets.RequestSegment(selected.id, sequence));
        }
    }

    private static void decodeSegments(Session selected) {
        try {
            while (awaitLinkedPlayback(selected)) {
                var part = selected.segments.take();
                if (part == null) return;
                var description = part.description();
                boolean single = MediaClip.segmentCount(description.totalFrames()) == 1;
                if (!single) requestNext(selected, description.sequence() + 1);
                Path file;
                try (part) {
                    file = part.download().file().toCompletableFuture().get();
                    playSegment(selected, description, file, single);
                } catch (ExecutionException firstFailure) {
                    if (!selected.active) return;
                    // The first object can fail during a brief relay/TLS overload. One retry owns the
                    // same current slot; the previous partial was deleted by its Transfer.
                    try (var retry = downloads.segment(URI.create(description.uri()))) {
                        file = retry.file().toCompletableFuture().get();
                        playSegment(selected, description, file, single);
                    }
                }
            }
        } catch (InterruptedException canceled) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | IOException | IllegalArgumentException unavailable) {
            selected.segments.close();
            signalFallback(selected, true);
        }
    }

    private static void playSegment(Session selected, MediaPackets.Segment description, Path path, boolean single)
            throws IOException, InterruptedException {
        if (!awaitLinkedPlayback(selected)) return;
        try (var clip = MediaClip.openSegment(path, description.width(), description.height(),
                description.totalFrames(), description.sequence())) {
            while (awaitLinkedPlayback(selected)) {
                var frame = clip.next();
                if (frame == null) {
                    if (!single) return;
                    clip.rewind();
                } else linkedFrame(selected, JpegDecoder.decode(frame.jpeg(), clip.width(), clip.height()), frame.pcm());
            }
        }
    }

    private static void decodeLink(Session selected) {
        try {
            if (!awaitLinkedPlayback(selected)) return;
            if (!selected.link.cdn().isEmpty()) {
                if(mp4Tools.isPresent()){
                    try(var transfer=downloads.direct(URI.create(selected.link.cdn()))){
                        playMovie(selected, transfer);
                        return;
                    }catch(ExecutionException|IOException|IllegalArgumentException unavailable){
                        if(!selected.active)return;
                    }
                }
            }
            if (!awaitLinkedPlayback(selected)) return;
            if (selected.link.relay().isEmpty()) {
                signalFallback(selected, false);
                return;
            }
            if (selected.link.relayMp4()) {
                try (var transfer = downloads.fallback(URI.create(selected.link.relay()))) {
                    playMovie(selected, transfer);
                }
                return;
            }
            try(var transfer=downloads.fallback(URI.create(selected.link.relay()));
                var clip=MediaClip.open(transfer.file().toCompletableFuture().get())){
                while (awaitLinkedPlayback(selected)) {
                    var frame = clip.next();
                    if (frame == null) {
                        clip.rewind();
                        continue;
                    }
                    var image=JpegDecoder.decode(frame.jpeg(),clip.width(),clip.height());
                    linkedFrame(selected,image,frame.pcm());
                }
            }
        } catch(InterruptedException canceled){
            Thread.currentThread().interrupt();
        } catch(ExecutionException|IOException|IllegalArgumentException unavailable){
            signalFallback(selected, true);
        }
    }

    private static void playMovie(Session selected, MediaDownload.Transfer transfer)
            throws IOException, ExecutionException, InterruptedException {
        if (mp4Tools.isEmpty()) throw new IOException("MP4 decoder unavailable");
        Path file = transfer.file().toCompletableFuture().get();
        while (awaitLinkedPlayback(selected)) {
            try (var movie = new Mp4Decoder(file, mp4Tools.get())) {
                selected.mp4 = movie;
                if (!awaitLinkedPlayback(selected)) return;
                var frame = movie.next();
                if (frame == null) throw new IOException("Empty phone video");
                while (frame != null) {
                    linkedFrame(selected, frame.image(), frame.pcm());
                    if (!awaitLinkedPlayback(selected)) return;
                    frame = movie.next();
                }
            } finally {
                selected.mp4 = null;
            }
        }
    }

    private static void signalFallback(Session selected, boolean relayFailed) {
        Minecraft.getInstance().execute(() -> {
            synchronized (LOCK) {
                if (session != selected || !selected.active) return;
                if (paused) selected.deferredFallback = relayFailed;
                else if (ClientPlayNetworking.canSend(MediaPackets.Fallback.TYPE)) {
                    ClientPlayNetworking.send(new MediaPackets.Fallback(selected.id, relayFailed));
                }
            }
        });
    }

    private static boolean awaitLinkedPlayback(Session selected) throws InterruptedException {
        synchronized (LOCK) {
            while (session == selected && paused) LOCK.wait();
            return session == selected && selected.active;
        }
    }

    private static void linkedFrame(Session selected,NativeImage image,byte[] pcm) throws InterruptedException {
        boolean queued=false;
        try {
            synchronized(LOCK){
                while (session == selected && (paused || selected.timeline.queued() >= 6)) {
                    if (paused) LOCK.wait();
                    else LOCK.wait(8L);
                }
                if(session!=selected||!selected.active)return;
                long serial=selected.last+1;
                if(!selected.timeline.offer(serial,pcm))return;
                selected.last=serial;
                selected.received=System.nanoTime();
                while(!decoded.offer(new Decoded(selected,serial,image))){var old=decoded.poll();if(old!=null)old.image.close();}
                queued=true;
            }
        }finally{if(!queued)image.close();}
    }
    private static void receive(MediaPackets.Frame p){
        synchronized(LOCK){
            if(paused||retired.contains(p.session())||!MediaPackets.jpegMatches(p.jpeg(),p.width(),p.height()))return;
            if(session==null||!session.id.equals(p.session())){close(true);session=new Session(p);}
            else if(session.linked()){close(false);session=new Session(p);}
            if(session.rate!=p.rate()||session.channels!=p.channels())return;
            if(session.last>=0&&p.serial()-session.last>8){close(false);session=new Session(p);}
            session.last=Math.max(session.last,p.serial());
            if(!session.timeline.offer(p.serial(),p.pcm()))return;
            session.received=System.nanoTime();pending=p;
            if(!decoding){decoding=true;decoder.execute(MediaClient::decode);}
        }
    }
    private static void decode(){
        for(;;){
            MediaPackets.Frame p;Session owner;
            synchronized(LOCK){p=pending;pending=null;owner=session;if(p==null){decoding=false;return;}}
            try{
                NativeImage image=JpegDecoder.decode(p.jpeg(),p.width(),p.height());
                synchronized(LOCK){
                    if(owner!=session||!owner.active){image.close();continue;}
                    while(!decoded.offer(new Decoded(owner,p.serial(),image))){var old=decoded.poll();if(old!=null)old.image.close();}
                }
            }catch(Exception ignored){/* Invalid JPEG cannot leak native memory or kill the networking thread. */}
        }
    }
    private static void tick(){
        if(!announced&&Minecraft.getInstance().getConnection()!=null&&ClientPlayNetworking.canSend(MediaPackets.Hello.TYPE)){
            int features=MediaPackets.FEATURES;
            if (relayOrigin != null && ClientPlayNetworking.canSend(MediaPackets.Fallback.TYPE)) {
                features |= MediaPackets.HTTPS_PLAYBACK;
                if (mp4Tools.isPresent()) features |= MediaPackets.DIRECT_MP4;
                if (ClientPlayNetworking.canSend(MediaPackets.RequestSegment.TYPE)) features |= MediaPackets.HTTPS_SEGMENTS;
            }
            ClientPlayNetworking.send(new MediaPackets.Hello(MediaPackets.VERSION,features));announced=true;
        }
        synchronized(LOCK){
            var s=session;if(s==null||paused)return;
            if(!s.active||(!s.linked()&&System.nanoTime()-s.received>2_000_000_000L)){close(false);return;}
            // Re-prime from fresh frames after a device reload or an audio-buffer underrun.
            if (s.sound != null && s.sound.isStopped()) {
                if (!s.linked()) { close(false); return; }
                s.sound = null;
                s.silent = true;
                s.timeline.start(System.nanoTime());
            }
            float volume=Minecraft.getInstance().options.getFinalSoundSourceVolume(SoundSource.RECORDS);
            if(s.sound!=null&&s.volume!=volume){s.volume=volume;s.sound.execute(channel->channel.setVolume(volume));}
            if(s.timeline.primed()&&!s.opening){s.opening=true;openAudio(s);}
            long now = System.nanoTime();
            if (s.silent) s.timeline.discardPlayed(now);
            long target=s.timeline.presented(now);Decoded selected=null;
            while(decoded.peek()!=null&&decoded.peek().serial<=target){
                var d=decoded.poll();if(selected!=null)selected.image.close();selected=d;
            }
            if(selected!=null){
                if(selected.owner!=s||selected.serial<=s.shown){selected.image.close();return;}
                s.shown=selected.serial;
                if(texture==null){texture=new DynamicTexture(()->"skywars phone video",selected.image);Minecraft.getInstance().getTextureManager().register(TEXTURE,texture);}
                else{texture.setPixels(selected.image);texture.upload();}
            }
        }
    }
    private static void openAudio(Session s){
        var mc=Minecraft.getInstance();
        s.volume=mc.options.getFinalSoundSourceVolume(SoundSource.RECORDS);
        var engine=((MediaSoundAccess.Manager)mc.getSoundManager()).skywars2$engine();
        var access=((MediaSoundAccess.Engine)engine).skywars2$channels();
        access.createHandle(Library.Pool.STREAMING).thenAccept(handle->{
            synchronized (LOCK) {
                if (session != s || !s.active) {
                    if (handle != null) handle.execute(channel -> channel.stop());
                    return;
                }
                if (handle == null) {
                    s.timeline.start(System.nanoTime());
                    s.silent = true;
                    return;
                }
                s.sound = handle;
            }
            handle.execute(channel->{
                if(!s.active){channel.stop();return;}
                channel.setRelative(true);channel.disableAttenuation();channel.setPitch(1);channel.setLooping(false);
                channel.setVolume(s.volume);
                s.timeline.start(System.nanoTime());
                channel.attachBufferStream(new PcmStream(s.timeline,s.rate,s.channels));
                synchronized (LOCK) {
                    if (session != s || s.sound != handle) {
                        channel.stop();
                        return;
                    }
                    s.soundReady = true;
                    if (!paused) {
                        channel.play();
                        s.soundStarted = true;
                    }
                }
            });
        });
    }
}
