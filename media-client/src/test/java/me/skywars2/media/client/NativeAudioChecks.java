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
            channel.setVolume(0);if(AL10.alGetSourcef(source,AL10.AL_GAIN)!=0)throw new AssertionError("live mute");
            channel.setVolume(.25f);if(Math.abs(AL10.alGetSourcef(source,AL10.AL_GAIN)-.25f)>1e-6f)throw new AssertionError("live volume");
            channel.pause();if(channel.playing())throw new AssertionError("pause immediate");channel.unpause();
            channel.stop();if(!channel.stopped())throw new AssertionError("stop immediate");library.releaseChannel(channel);
            if(stream.read(192000)!=null||!timeline.running())throw new AssertionError("channel closes PCM without killing shared video timeline");
            timeline.offer(8,new byte[9600]);if(!timeline.running())throw new AssertionError("video keeps accepting frames after external sound stop");
            timeline.close();if(timeline.queued()!=0)throw new AssertionError("session cleanup clears shared buffers");
            System.out.println("PASS native OpenAL null output: real4buffer stereo queue,50ms cap,play/pause/stop/live mute/volume,independent video lifetime and session cleanup");
        }finally{library.cleanup();}
    }
}
