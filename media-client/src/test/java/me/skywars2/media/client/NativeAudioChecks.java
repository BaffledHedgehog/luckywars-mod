package me.skywars2.media.client;

import com.mojang.blaze3d.audio.*;
import org.lwjgl.openal.AL10;

/** Real OpenAL queue on the null output backend: silent, no user audio device is opened. */
public final class NativeAudioChecks {
    public static void main(String[] args)throws Exception{
        if(!"null".equals(System.getenv("ALSOFT_DRIVERS")))throw new AssertionError("null output required");
        var library=new Library();library.init(null,DeviceList.query(),false);
        try{
            var channel=library.acquireChannel(Library.Pool.STREAMING);if(channel==null)throw new AssertionError("native streaming channel");
            var timeline=new MediaTimeline();
            for(int i=0;i<8;i++)timeline.offer(i,new byte[9600]);timeline.start(System.nanoTime());
            var stream=new PcmStream(timeline,48000,2);
            if(stream.read(192000).remaining()!=9600||stream.getFormat().getChannels()!=2)throw new AssertionError("native request bounded to50ms stereo");
            channel.setRelative(true);channel.disableAttenuation();channel.attachBufferStream(stream);
            var field=Channel.class.getDeclaredField("source");field.setAccessible(true);int source=field.getInt(channel);
            if(AL10.alGetSourcei(source,AL10.AL_BUFFERS_QUEUED)!=4)throw new AssertionError("four real buffers primed");
            channel.play();if(!channel.playing())throw new AssertionError("native PCM playing");
            channel.pause();if(channel.playing())throw new AssertionError("pause immediate");channel.unpause();
            channel.stop();if(!channel.stopped())throw new AssertionError("stop immediate");library.releaseChannel(channel);
            if(timeline.running())throw new AssertionError("native channel release closes the stream");
            System.out.println("PASS native OpenAL null output: real4buffer stereo queue,50ms cap,play/pause/stop/cleanup");
        }finally{library.cleanup();}
    }
}
