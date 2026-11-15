package me.skywars2.media.client;

import java.nio.ByteBuffer;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.AudioStream;

/** Native Channel asks for one second; deliberately return only one 50ms slice per buffer. */
public final class PcmStream implements AudioStream {
    private final MediaTimeline timeline;
    private final AudioFormat format;
    public PcmStream(MediaTimeline timeline,int rate,int channels){this.timeline=timeline;format=new AudioFormat(rate,16,channels,true,false);}
    @Override public AudioFormat getFormat(){return format;}
    @Override public ByteBuffer read(int expected){byte[] data=timeline.read();return ByteBuffer.allocateDirect(data.length).put(data).flip();}
    @Override public void close(){timeline.close();}
}
