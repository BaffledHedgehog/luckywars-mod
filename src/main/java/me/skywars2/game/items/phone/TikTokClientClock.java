package me.skywars2.game.items.phone;

import me.skywars2.game.TimeStopView;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;
import me.skywars2.media.MediaClip;
import me.skywars2.media.MediaPackets;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/** Independent 20 Hz JPEG/PCM clock. Main thread owns admission/lifecycle; no async world reads. */
final class TikTokClientClock implements AutoCloseable {
    private static final long FRAME_NANOS = 50_000_000L, SEGMENT_RETRY_NANOS = 250_000_000L;
    private static final int SEGMENT_RETRIES = 2;

    private static final class Stream {
        final UUID id=UUID.randomUUID();final ServerPlayer player;final ServerGamePacketListenerImpl listener;
        final TikTokOutput output;
        final boolean mp4, supportsSegments;
        final TikTokMedia clip;final long started;
        volatile boolean active = true;
        volatile boolean selectingLink, linked;
        boolean fallbackRequested;
        boolean relayPublished, segmented;
        long nextSegment, wantedSegment = -1, nextSegmentAt, segmentRetryAt;
        int segmentRetries;
        TikTokVelocityRelay.SegmentLease segmentLease;
        long last = -1;
        Stream(ServerPlayer player, TikTokMedia clip, long now, TikTokOutput output) {
            this.player=player;listener=player.connection;this.clip=clip;started=now;this.output=output;
            mp4 = TikTokClientSupport.mp4(player);
            supportsSegments = TikTokClientSupport.segments(player);
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
        } else if (https && stream.supportsSegments && !stream.mp4) {
            synchronized (stream) { beginSegments(stream); }
        } else if (https) prepareFallback(stream);
    }

    private Stream stream(ServerPlayer player, UUID session) {
        var stream = streams.get(player.getUUID());
        if (relay == null || stream == null || stream.player != player || stream.listener != player.connection
                || !stream.output.activeFor(player) || !stream.id.equals(session)) return null;
        return stream;
    }

    void requestSegment(ServerPlayer player, MediaPackets.RequestSegment request) {
        var stream = stream(player, request.session());
        if (stream == null) return;
        synchronized (stream) {
            if (stream.active && stream.segmented && stream.wantedSegment == -1
                    && MediaClip.segmentCount(stream.clip.frames) > 1 && stream.nextSegment == request.sequence()) {
                stream.wantedSegment = request.sequence();
            }
        }
    }

    private static void beginSegments(Stream stream) {
        stream.selectingLink = false;
        stream.linked = true;
        stream.relayPublished = false;
        stream.segmented = true;
        stream.wantedSegment = 0;
    }

    private static TikTokVelocityRelay.SegmentLease endSegments(Stream stream) {
        var lease = stream.segmentLease;
        stream.segmentLease = null;
        stream.segmented = false;
        stream.wantedSegment = -1;
        stream.linked = false;
        return lease;
    }

    private void offerSegment(Stream stream, long now) {
        if (!stream.linked) return;
        TikTokVelocityRelay.SegmentLease lease;
        long sequence;
        synchronized (stream) {
            if (!stream.active || !stream.segmented || stream.segmentLease != null || stream.wantedSegment < 0
                    || (stream.nextSegmentAt != 0 && now - stream.nextSegmentAt < 0)
                    || (stream.segmentRetryAt != 0 && now - stream.segmentRetryAt < 0)
                    || !stream.output.activeFor(stream.player)) return;
            sequence = stream.wantedSegment;
            try {
                lease = relay.prepareSegment(stream.clip, sequence,
                        () -> pin.accept(stream.clip.id), () -> release.accept(stream.clip.id));
                stream.segmentLease = lease;
            } catch (RejectedExecutionException busy) {
                stream.segmentRetryAt = now + SEGMENT_RETRY_NANOS;
                return;
            }
        }
        lease.link.whenComplete((delivery, error) -> {
            try {
                synchronized (stream) {
                    if (stream.segmentLease != lease) return;
                    stream.segmentLease = null;
                    if (!stream.active || !stream.segmented || !stream.output.activeFor(stream.player)) return;
                    if (error != null) {
                        if (stream.segmentRetries++ < SEGMENT_RETRIES) {
                            stream.segmentRetryAt = System.nanoTime() + SEGMENT_RETRY_NANOS;
                        } else endSegments(stream);
                        return;
                    }
                    stream.segmentRetries = 0;
                    stream.segmentRetryAt = 0;
                    stream.nextSegment = sequence + 1;
                    stream.wantedSegment = -1;
                    if (sequence > 0) {
                        // The client starts downloading the next part while playing this part's predecessor.
                        // Align to admission so ordinary upload and encode time do not accumulate per page.
                        int played = MediaClip.framesInSegment(sequence - 1, stream.clip.frames);
                        long previousDue = stream.nextSegmentAt;
                        long anchor = previousDue != 0 && now - previousDue < FRAME_NANOS ? previousDue : now;
                        stream.nextSegmentAt = anchor + played * FRAME_NANOS;
                    }
                    stream.listener.send(new ClientboundCustomPayloadPacket(new MediaPackets.Segment(
                            stream.id, sequence, stream.clip.frames, TikTokMedia.JPEG_WIDTH,
                            TikTokMedia.JPEG_HEIGHT, delivery.uri().toASCIIString())));
                }
            } finally { lease.close(); }
        });
    }

    void requestFallback(ServerPlayer player, MediaPackets.Fallback request) {
        var stream = stream(player, request.session());
        if (stream == null) return;
        if (request.relayFailed()) {
            TikTokVelocityRelay.SegmentLease lease = null;
            synchronized (stream) {
                if (!stream.active) return;
                if (stream.segmented && stream.nextSegment > 0) lease = endSegments(stream);
                else if (stream.relayPublished) {
                    if (stream.supportsSegments) beginSegments(stream);
                    else {
                        stream.relayPublished = false;
                        stream.linked = false;
                    }
                }
            }
            if (lease != null) lease.close();
        } else prepareFallback(stream);
    }

    private void prepareFallback(Stream stream) {
        synchronized (stream) {
            if (!stream.active || stream.segmented || stream.fallbackRequested) return;
            stream.fallbackRequested = true;
            stream.selectingLink = true;
        }
        pin.accept(stream.clip.id);
        relay.prepare(stream.clip, stream.mp4, stream.supportsSegments).whenComplete((delivery,error) -> {
            try {
                synchronized (stream) {
                    try {
                        stream.linked = false;
                        if (stream.active && error == null && stream.output.activeFor(stream.player)) {
                            stream.listener.send(new ClientboundCustomPayloadPacket(
                                    new MediaPackets.Link(stream.id, "", delivery.uri().toASCIIString(), delivery.mp4())));
                            stream.relayPublished = true;
                            stream.linked = true;
                        } else if (stream.active && stream.supportsSegments && stream.output.activeFor(stream.player)) {
                            beginSegments(stream);
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
            TikTokVelocityRelay.SegmentLease lease;
            synchronized (stream) {
                stream.active = false;
                lease = endSegments(stream);
                // Any earlier write on the channel loop precedes Stop. A queued write sees inactive.
                if (stream.output.active()) stream.listener.send(new ClientboundCustomPayloadPacket(new MediaPackets.Stop(stream.id)));
            }
            if (lease != null) lease.close();
        }
    }
    private void pulse(){
        long now=System.nanoTime();
        for(var s:streams.values()){
            if (relay != null) offerSegment(s, now);
            long serial=Math.max(0,now-s.started)/FRAME_NANOS;
            if(s.last==serial)continue;s.last=serial;
            if (!s.active || s.selectingLink || s.linked || !s.output.activeFor(s.player)
                    || !s.output.frameReady() || TimeStopView.frozen(s.player)) continue;
            pin.accept(s.clip.id);
            try{
                var data=s.clip.clientFrame((int)(serial%s.clip.frames));
                var packet=new ClientboundCustomPayloadPacket(new MediaPackets.Frame(s.id,serial,TikTokMedia.JPEG_WIDTH,TikTokMedia.JPEG_HEIGHT,TikTokMedia.RATE,2,data.jpeg(),data.pcm()));
                s.output.offerFrame(packet, s.started + (serial + 3) * FRAME_NANOS,
                        () -> s.active && s.output.activeFor(s.player) && !TimeStopView.frozen(s.player));
            }catch(Exception ignored){/* Failed/retired reads drop this frame; never accumulate a backlog. */}
            finally{release.accept(s.clip.id);}
        }
    }
    void clear(){for(var s:streams.values())stop(s.player,s.listener);}
    @Override public void close(){clear();if(relay!=null)relay.close();clock.shutdownNow();try{clock.awaitTermination(2,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
}
