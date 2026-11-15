package me.skywars2.media.client;

import com.mojang.blaze3d.audio.Library;
import com.mojang.blaze3d.platform.NativeImage;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import javax.sound.sampled.AudioFormat;
import me.skywars2.media.MediaPackets;
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

/** Optional RGB/PCM player. No cookies, HTTP, persistent cache, or second audio device. */
public final class MediaClient implements ClientModInitializer {
    private static final Identifier TEXTURE=Identifier.parse("skywars2:phone_stream");
    private static final Object LOCK=new Object();
    private static final LinkedHashSet<UUID> retired=new LinkedHashSet<>();
    private static final ExecutorService decoder=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"skywars-media-decode");t.setDaemon(true);t.setPriority(Thread.MIN_PRIORITY);return t;});
    private static final ArrayBlockingQueue<Decoded> decoded=new ArrayBlockingQueue<>(8);
    private static Session session;
    private static MediaPackets.Frame pending;
    private static boolean decoding,paused,announced;
    private static DynamicTexture texture;
    private record Decoded(Session owner,long serial,NativeImage image){}
    private static final class Session {
        final UUID id;final int rate,channels;final MediaTimeline timeline=new MediaTimeline();
        volatile ChannelAccess.ChannelHandle sound;
        volatile float volume;
        volatile boolean active=true;
        boolean opening;long shown=-1,received,last=-1;
        Session(MediaPackets.Frame p){id=p.session();rate=p.rate();channels=p.channels();}
    }
    @Override public void onInitializeClient(){
        MediaPackets.register();
        SpecialModelRenderers.ID_MAPPER.put(Identifier.parse("skywars2:phone_video"),PhoneRenderer.Unbaked.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(MediaPackets.Frame.TYPE,(p,c)->receive(p));
        ClientPlayNetworking.registerGlobalReceiver(MediaPackets.Stop.TYPE,(p,c)->{synchronized(LOCK){retire(p.session());if(session!=null&&session.id.equals(p.session()))close(false);}});
        ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->{
            reset(); // Server channel advertisement may arrive after the JOIN callback.
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->reset());
        ClientTickEvents.END_CLIENT_TICK.register(client->tick());
    }
    public static Identifier texture(){synchronized(LOCK){return texture!=null&&session!=null?TEXTURE:null;}}
    private static void retire(UUID id){retired.add(id);while(retired.size()>32)retired.remove(retired.iterator().next());}
    private static void reset(){synchronized(LOCK){close(false);retired.clear();paused=false;announced=false;}}
    private static void close(boolean keepIdentity){
        if(session!=null){session.active=false;session.timeline.close();if(session.sound!=null)session.sound.execute(c->c.stop());if(keepIdentity)retire(session.id);session=null;}
        pending=null;Decoded d;while((d=decoded.poll())!=null)d.image.close();
        if(texture!=null){Minecraft.getInstance().getTextureManager().release(TEXTURE);texture=null;}
    }
    public static void frozen(boolean value){synchronized(LOCK){if(paused==value)return;paused=value;close(false);}}
    private static void receive(MediaPackets.Frame p){
        synchronized(LOCK){
            if(paused||retired.contains(p.session())||!MediaPackets.jpegMatches(p.jpeg(),p.width(),p.height()))return;
            if(session==null||!session.id.equals(p.session())){close(true);session=new Session(p);}
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
            ClientPlayNetworking.send(new MediaPackets.Hello(MediaPackets.VERSION,MediaPackets.FEATURES));announced=true;
        }
        synchronized(LOCK){
            var s=session;if(s==null)return;
            if(System.nanoTime()-s.received>2_000_000_000L){close(false);return;}
            float volume=Minecraft.getInstance().options.getFinalSoundSourceVolume(SoundSource.RECORDS);
            if(s.sound!=null&&s.volume!=volume){s.volume=volume;s.sound.execute(channel->channel.setVolume(volume));}
            if(s.timeline.primed()&&!s.opening){s.opening=true;openAudio(s);}
            long target=s.timeline.presented(System.nanoTime());Decoded selected=null;
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
            if(handle==null){s.timeline.start(System.nanoTime());return;}
            s.sound=handle;
            handle.execute(channel->{
                if(!s.active){channel.stop();return;}
                channel.setRelative(true);channel.disableAttenuation();channel.setPitch(1);channel.setLooping(false);
                channel.setVolume(s.volume);
                s.timeline.start(System.nanoTime());
                channel.attachBufferStream(new PcmStream(s.timeline,s.rate,s.channels));
                channel.play();
            });
        });
    }
}
