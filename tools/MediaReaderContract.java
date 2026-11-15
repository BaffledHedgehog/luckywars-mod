package me.skywars2.game;

import java.nio.file.Path;
import java.util.Arrays;

/** Runs against the installed server's real reader class, without booting a server. */
public final class MediaReaderContract {
    public static void main(String[] args)throws Exception{
        int checked;
        try(var media=new TikTokMedia("12345678",Path.of(args[0]))){
            checked=media.frames;
            for(int number=0;number<media.frames;number++){
                var complete=media.frame(number,true);var enhanced=media.clientFrame(number);
                if(!Arrays.equals(complete.jpeg(),enhanced.jpeg())||!Arrays.equals(complete.pcm(),enhanced.pcm()))
                    throw new AssertionError("enhanced seek differs from independent full-record reader: "+number);
                if(enhanced.map().length!=0||enhanced.bands().length!=0||enhanced.pcm().length!=9600
                        ||enhanced.jpeg().length<4||enhanced.jpeg()[0]!=(byte)255||enhanced.jpeg()[1]!=(byte)216)
                    throw new AssertionError("real enhanced record bounds: "+number);
                if(media.hasSpectral()){
                    var audio=java.nio.ByteBuffer.wrap(media.spectralAudio(number));
                    int count=Short.toUnsignedInt(audio.getShort());
                    if(audio.capacity()!=354||count>88)throw new AssertionError("spectral frame bound");
                    for(int i=0;i<count;i++){
                        int key=Short.toUnsignedInt(audio.getShort()),gain=Short.toUnsignedInt(audio.getShort());
                        if((key>>>5)<2||(key>>>5)>=1150||gain<2)throw new AssertionError("spectral coefficient bound");
                    }
                }
            }
            for(int invalid:new int[]{-1,media.frames}){
                boolean rejected=false;try{media.clientFrame(invalid);}catch(java.io.IOException expected){rejected=true;}
                if(!rejected)throw new AssertionError("out-of-range client frame");
            }
        }
        System.out.println("PASS actual server reader: all"+checked+" real frames preserve exact JPEG and48k stereo PCM, independent bounded spectral sidecar, skip palette allocation and reject invalid indexes");
    }
}
