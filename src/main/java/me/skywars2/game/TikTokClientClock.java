package me.skywars2.game;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;
import me.skywars2.media.MediaPackets;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/** Independent 20 Hz JPEG/PCM clock. Main thread owns admission/lifecycle; no async world reads. */
final class TikTokClientClock implements AutoCloseable {
    private static final class Stream {
        final UUID id=UUID.randomUUID();final ServerPlayer player;final ServerGamePacketListenerImpl listener;
        final TikTokOutput output;
        final boolean mp4;
        final TikTokMedia clip;final long started;
        volatile boolean active = true;
        volatile boolean selectingLink, linked;
        boolean fallbackRequested;
        boolean relayPublished;
        long last = -1;
        Stream(ServerPlayer player, TikTokMedia clip, long now, TikTokOutput output) {
            this.player=player;listener=player.connection;this.clip=clip;started=now;this.output=output;
            mp4 = TikTokClientSupport.mp4(player);
        }
    }
    private final ConcurrentMap<UUID,Stream> streams=new ConcurrentHashMap<>();
    private final Consumer<String> pin,release;
    private final TikTokVelocityRelay relay = TikTokVelocityRelay.configured();
    private final ScheduledExecutorService clock=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"skywars-media-20hz");t.setDaemon(true);t.setPriority(Thread.MIN_PRIORITY);return t;});
    TikTokClientClock(Consumer<String> pin,Consumer<String> release){this.pin=pin;this.release=release;clock.scheduleAtFixedRate(this::pulse,0,50,TimeUnit.MILLISECONDS);}
    void play(ServerPlayer player, TikTokMedia clip, long now, TikTokOutput output) {
        if (!output.activeFor(player) || !TikTokClientSupport.enhanced(player)) return;
        var previous=streams.get(player.getUUID());
        if(previous!=null)stop(previous.player,previous.listener);
        var stream = new Stream(player, clip, now, output);
        boolean https = relay != null && TikTokClientSupport.httpsPlayback(player);
        String cdn = https && stream.mp4 ? clip.cdnSource().map(source -> source.uri().toASCIIString()).orElse("") : "";
        stream.linked = !cdn.isEmpty();
        stream.selectingLink = https && cdn.isEmpty();
        streams.put(player.getUUID(), stream);
        if (stream.linked) {
            stream.listener.send(new ClientboundCustomPayloadPacket(new MediaPackets.Link(stream.id, cdn, "", false)));
        } else if (https) {
            prepareFallback(stream);
        }
    }

    void requestFallback(ServerPlayer player, MediaPackets.Fallback request) {
        var stream = streams.get(player.getUUID());
        if (relay == null || stream == null || stream.player != player || stream.listener != player.connection
                || !stream.output.activeFor(player) || !stream.id.equals(request.session())) return;
        if (request.relayFailed()) {
            synchronized (stream) {
                if (stream.active && stream.relayPublished) {
                    stream.relayPublished = false;
                    stream.linked = false;
                }
            }
        } else prepareFallback(stream);
    }

    private void prepareFallback(Stream stream) {
        synchronized (stream) {
            if (!stream.active || stream.fallbackRequested) return;
            stream.fallbackRequested = true;
            stream.selectingLink = true;
        }
        pin.accept(stream.clip.id);
        relay.prepare(stream.clip, stream.mp4).whenComplete((delivery,error) -> {
            try {
                synchronized (stream) {
                    try {
                        stream.linked = false;
                        if (stream.active && error == null && stream.output.activeFor(stream.player)) {
                            stream.listener.send(new ClientboundCustomPayloadPacket(
                                    new MediaPackets.Link(stream.id, "", delivery.uri().toASCIIString(), delivery.mp4())));
                            stream.relayPublished = true;
                            stream.linked = true;
                        }
                    } finally {
                        stream.selectingLink = false;
                    }
                }
            } finally { release.accept(stream.clip.id); }
        });
    }
    void stop(ServerPlayer player, ServerGamePacketListenerImpl listener){
        var stream=streams.get(player.getUUID());
        if (stream != null && stream.player == player && stream.listener == listener && streams.remove(player.getUUID(),stream)) {
            synchronized (stream) {
                stream.active = false;
                // Any earlier write on the channel loop precedes Stop. A queued write sees inactive.
                if (stream.output.active()) stream.listener.send(new ClientboundCustomPayloadPacket(new MediaPackets.Stop(stream.id)));
            }
        }
    }
    private void pulse(){
        long now=System.nanoTime();
        for(var s:streams.values()){
            long serial=Math.max(0,now-s.started)/50_000_000L;
            if(s.last==serial)continue;s.last=serial;
            if (!s.active || s.selectingLink || s.linked || !s.output.activeFor(s.player)
                    || !s.output.frameReady() || TimeStopView.frozen(s.player)) continue;
            pin.accept(s.clip.id);
            try{
                var data=s.clip.clientFrame((int)(serial%s.clip.frames));
                var packet=new ClientboundCustomPayloadPacket(new MediaPackets.Frame(s.id,serial,TikTokMedia.JPEG_WIDTH,TikTokMedia.JPEG_HEIGHT,TikTokMedia.RATE,2,data.jpeg(),data.pcm()));
                s.output.offerFrame(packet, s.started + (serial + 3) * 50_000_000L,
                        () -> s.active && s.output.activeFor(s.player) && !TimeStopView.frozen(s.player));
            }catch(Exception ignored){/* Failed/retired reads drop this frame; never accumulate a backlog. */}
            finally{release.accept(s.clip.id);}
        }
    }
    void clear(){for(var s:streams.values())stop(s.player,s.listener);}
    @Override public void close(){clear();if(relay!=null)relay.close();clock.shutdownNow();try{clock.awaitTermination(2,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
}
