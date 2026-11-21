package me.skywars2.media.client;

import java.nio.ByteBuffer;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.AudioStream;

/** Native Channel asks for one second; queue a short group of complete source-frame PCM slices. */
public final class PcmStream implements AudioStream {
    private final MediaTimeline timeline;
    private final AudioFormat format;
    private boolean closed;
    public PcmStream(MediaTimeline timeline,int rate,int channels){this.timeline=timeline;format=new AudioFormat(rate,16,channels,true,false);}
    @Override public AudioFormat getFormat(){return format;}
    @Override public ByteBuffer read(int expected){
        if(closed)return null;
        int count=timeline.audioSlices(),length=0;byte[][] slices=new byte[count][];
        for(int i=0;i<count;i++){slices[i]=timeline.read();length+=slices[i].length;}
        var result=ByteBuffer.allocateDirect(length);for(var slice:slices)result.put(slice);return result.flip();
    }
    // SoundEngine also closes channels on /stopsound and resource reload. The session owns video lifetime.
    @Override public void close(){closed=true;}
}
