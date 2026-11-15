package me.skywars2.game;

import io.netty.channel.Channel;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;
import me.skywars2.media.MediaPackets;
import me.skywars2.mixin.MediaTransportAccess;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/** Independent 20 Hz JPEG/PCM clock. Main thread owns admission/lifecycle; no async world reads. */
final class TikTokClientClock implements AutoCloseable {
    private static final class Stream {
        final UUID id=UUID.randomUUID();final ServerPlayer player;final ServerGamePacketListenerImpl listener;
        final Channel channel;final TikTokMedia clip;final long started;
        boolean active=true;long last=-1;
        Stream(ServerPlayer player,TikTokMedia clip,long now){
            this.player=player;listener=player.connection;this.clip=clip;started=now;
            var connection=((MediaTransportAccess.Listener)listener).skywars2$mediaConnection();
            channel=((MediaTransportAccess.Transport)connection).skywars2$mediaChannel();
        }
    }
    private final ConcurrentMap<UUID,Stream> streams=new ConcurrentHashMap<>();
    private final Consumer<String> pin,release;
    private final ScheduledExecutorService clock=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"skywars-media-20hz");t.setDaemon(true);t.setPriority(Thread.MIN_PRIORITY);return t;});
    TikTokClientClock(Consumer<String> pin,Consumer<String> release){this.pin=pin;this.release=release;clock.scheduleAtFixedRate(this::pulse,0,50,TimeUnit.MILLISECONDS);}
    void play(ServerPlayer player,TikTokMedia clip,long now){
        stop(player);if(TikTokClientSupport.enhanced(player))streams.put(player.getUUID(),new Stream(player,clip,now));
    }
    void stop(ServerPlayer player){
        var stream=streams.remove(player.getUUID());
        // This gate orders the last possible old frame before Stop and the main-thread slot restoration.
        if(stream!=null)synchronized(stream){stream.active=false;if(stream.channel!=null&&stream.channel.isActive())stream.listener.send(new ClientboundCustomPayloadPacket(new MediaPackets.Stop(stream.id)));}
    }
    private void pulse(){
        long now=System.nanoTime();
        for(var s:streams.values()){
            long serial=Math.max(0,now-s.started)/50_000_000L;
            if(s.last==serial)continue;s.last=serial;
            if(s.channel==null||!s.channel.isActive()||!s.channel.isWritable()||TimeStopView.frozen(s.player))continue;
            pin.accept(s.clip.id);
            try{
                var data=s.clip.clientFrame((int)(serial%s.clip.frames));
                var packet=new ClientboundCustomPayloadPacket(new MediaPackets.Frame(s.id,serial,TikTokMedia.JPEG_WIDTH,TikTokMedia.JPEG_HEIGHT,TikTokMedia.RATE,2,data.jpeg(),data.pcm()));
                synchronized(s){if(s.active&&s.channel.isWritable()&&!TimeStopView.frozen(s.player))s.listener.send(packet);}
            }catch(Exception ignored){/* Failed/retired reads drop this frame; never accumulate a backlog. */}
            finally{release.accept(s.clip.id);}
        }
    }
    void clear(){for(var s:streams.values())stop(s.player);}
    @Override public void close(){clear();clock.shutdownNow();try{clock.awaitTermination(2,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
}
