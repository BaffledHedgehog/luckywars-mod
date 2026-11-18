package me.skywars2.game;

import java.util.UUID;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;

/** Independent wall clock:20Hz stereo tones,60Hz old-cache fallback, no world state or backlog. */
final class TikTokSoundClock implements AutoCloseable {
    private static final long PERIOD=1_000_000_000L/TikTokMedia.AUDIO_FPS;
    private static final class Stream {
        final ServerPlayer identity;
        final TikTokOutput output;
        final TikTokMedia clip;final long started;volatile boolean active=true;long last=-1,lastSpectral=-1;
        long lastClientTick=Long.MIN_VALUE;
        final TikTokVoiceBudget voices=new TikTokVoiceBudget();
        Stream(ServerPlayer player, TikTokMedia clip, long started, TikTokOutput output) {
            identity=player;this.clip=clip;this.started=started;this.output=output;
        }
    }
    private final ConcurrentMap<UUID,Stream> streams=new ConcurrentHashMap<>();
    private final Consumer<String> pin,release;
    private final ScheduledExecutorService clock=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"skywars-tiktok-sound-60hz");t.setDaemon(true);return t;});
    final AtomicLong emittedSlices=new AtomicLong();
    TikTokSoundClock(Consumer<String> pin,Consumer<String> release){
        this.pin=pin;this.release=release;clock.scheduleAtFixedRate(this::pulse,0,PERIOD,TimeUnit.NANOSECONDS);
    }
    void play(ServerPlayer player, TikTokMedia clip, long started, TikTokOutput output) {
        stop(player);
        streams.put(player.getUUID(), new Stream(player, clip, started, output));
    }
    void stop(ServerPlayer player){var stream=streams.remove(player.getUUID());if(stream!=null)stream.active=false;}
    private void pulse(){
        long now=System.nanoTime();
        for(var stream:streams.values()){
            long serial=Math.max(0,now-stream.started)/PERIOD;
            if(serial==stream.last)continue;stream.last=serial;
            if (!stream.active || !stream.output.soundReady() || TimeStopView.frozen(stream.identity)) continue;
            pin.accept(stream.clip.id);
            try{
                // These constructors read only getId()/getUUID(), not mutable entity position or inventory.
                List<Packet<? super ClientGamePacketListener>> packets;
                long clientTick = stream.output.clientTicks();
                if(stream.clip.hasSpectral()){
                    long frame=serial/TikTokMedia.SLICES;
                    if(frame==stream.lastSpectral)continue;stream.lastSpectral=frame;
                    // Vanilla releases finished OpenAL sources on client ticks. Never
                    // pile up several old audio frames while that client is stalled.
                    // This signal is collected on Netty, independently of server TPS.
                    if(clientTick>=0&&clientTick==stream.lastClientTick)continue;
                    stream.lastClientTick=clientTick;
                    int limit=Math.min(72,stream.voices.available(now,clientTick));if(limit<=0)continue;
                    packets=TikTokAudio.spectralPackets(stream.identity,stream.clip.spectralAudio(frame),frame,limit);
                }else {
                    packets=TikTokAudio.packets(stream.identity,stream.clip.audio(serial),serial);
                    if(packets.size()>stream.voices.available(now,clientTick))continue;
                }
                if (!packets.isEmpty() && stream.output.offerSound(
                        new ClientboundBundlePacket(packets), now + 100_000_000L,
                        () -> stream.active && !TimeStopView.frozen(stream.identity))) {
                    stream.voices.emitted(System.nanoTime(), clientTick, packets.size());
                    emittedSlices.incrementAndGet();
                }
            }catch(Exception ignored){/* A retired/closed clip or connection has no sound backlog. */}
            finally{release.accept(stream.clip.id);}
        }
    }
    void clear(){streams.values().forEach(s->s.active=false);streams.clear();}
    @Override public void close(){clear();clock.shutdownNow();try{clock.awaitTermination(2,TimeUnit.SECONDS);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}}
}
