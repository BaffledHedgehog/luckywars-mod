import java.nio.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import org.lwjgl.openal.*;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.*;

/** Actual Minecraft LWJGL Vorbis/OpenAL libraries, silent offline loopback only. */
public final class TikTokToneBankChecks {
    static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    static void error(){int error=AL10.alGetError();require(error==AL10.AL_NO_ERROR,"OpenAL error "+error);}
    public static void main(String[] args)throws Exception{
        Path bank=Path.of(args[0]),sidecar=Path.of(args[1]),output=Path.of(args[2]);
        long device=SOFTLoopback.alcLoopbackOpenDeviceSOFT((ByteBuffer)null);
        require(device!=0,"silent loopback device");
        var capabilities=ALC.createCapabilities(device);
        int[] attributes={ALC10.ALC_FREQUENCY,48000,SOFTLoopback.ALC_FORMAT_CHANNELS_SOFT,SOFTLoopback.ALC_STEREO_SOFT,
                SOFTLoopback.ALC_FORMAT_TYPE_SOFT,SOFTLoopback.ALC_FLOAT_SOFT,ALC11.ALC_STEREO_SOURCES,256,0};
        long context=ALC10.alcCreateContext(device,attributes);
        require(context!=0&&ALC10.alcMakeContextCurrent(context),"loopback stereo context");
        AL.createCapabilities(capabilities);
        var buffers=new HashMap<Integer,Integer>();var sources=new ArrayList<Integer>();
        FloatBuffer rendered=MemoryUtil.memAllocFloat(4800);
        try{
            double maximumChannelError=0;int files=0;
            try(var paths=Files.list(bank)){
                for(var path:paths.filter(p->p.toString().endsWith(".ogg")).sorted().toList()){
                    String[] parts=path.getFileName().toString().replace(".ogg","").split("_");
                    int base=Integer.parseInt(parts[1]),phase=Integer.parseInt(parts[2]),side=Integer.parseInt(parts[3]);
                    try(var stack=MemoryStack.stackPush()){
                        var channels=stack.mallocInt(1);var rate=stack.mallocInt(1);
                        var pcm=STBVorbis.stb_vorbis_decode_filename(path.toString(),channels,rate);
                        require(pcm!=null,"native Vorbis decode "+path);
                        try{
                            require(channels.get(0)==2&&rate.get(0)==48000&&pcm.remaining()==4608*2,"native Vorbis layout "+path);
                            for(int i=0;i<4608;i++)maximumChannelError=Math.max(maximumChannelError,
                                    Math.abs(pcm.get(i*2)-(side==0?1:-1)*pcm.get(i*2+1))/32768.);
                            int buffer=AL10.alGenBuffers();AL10.alBufferData(buffer,AL10.AL_FORMAT_STEREO16,pcm,48000);
                            require(buffers.put((base*16+phase)*2+side,buffer)==null,"unique phase/stereo asset");files++;
                        }finally{MemoryUtil.memFree(pcm);}
                    }
                }
            }
            require(files==3552&&maximumChannelError<.0002,"all native stereo atoms preserve signed channels: "+maximumChannelError);
            for(int i=0;i<176;i++){
                int source=AL10.alGenSources();error();sources.add(source);
                AL10.alSourcei(source,AL10.AL_SOURCE_RELATIVE,AL10.AL_TRUE);
                AL10.alSourcef(source,AL10.AL_ROLLOFF_FACTOR,0);
            }
            // Independent negative/positive channel controls through actual OpenAL.
            for(int side=0;side<2;side++){
                int source=sources.getFirst();AL10.alSourcei(source,AL10.AL_BUFFER,buffers.get((50*16)*2+side));
                AL10.alSourcef(source,AL10.AL_GAIN,.25f);AL10.alSourcef(source,AL10.AL_PITCH,1);AL10.alSourcePlay(source);
                rendered.clear();SOFTLoopback.alcRenderSamplesSOFT(device,rendered,2400);
                double power=0,channelError=0;
                for(int i=128;i<2400;i++){
                    double l=rendered.get(i*2),r=rendered.get(i*2+1);power+=l*l;channelError+=Math.pow(l-(side==0?r:-r),2);
                }
                require(power>.001&&channelError/power<1e-6,"actual native mid/side output "+side);
                AL10.alSourceStop(source);
            }
            byte[] data=Files.readAllBytes(sidecar);var input=ByteBuffer.wrap(data);
            require(input.getInt()==0x544b4131,"spectral header");int frames=input.getInt();
            require(input.getInt()==20&&input.getInt()==88&&data.length==16+frames*354,"bounded spectral layout");
            long emitted=0;int maxActive=0;double peak=0;
            try(var out=Files.newOutputStream(output)){
                var encoded=ByteBuffer.allocate(4800*4).order(ByteOrder.LITTLE_ENDIAN);
                for(int frame=0;frame<frames;frame++){
                    input.position(16+frame*354);int count=Short.toUnsignedInt(input.getShort());require(count<=88,"coefficient burst");
                    for(int n=0;n<count;n++){
                        int key=Short.toUnsignedInt(input.getShort()),gain=Short.toUnsignedInt(input.getShort());
                        int bin=key>>>5,phase=(key>>>1)&15,side=key&1;require(bin>=2&&bin<1150,"frequency range");
                        int source=sources.stream().filter(s->{int state=AL10.alGetSourcei(s,AL10.AL_SOURCE_STATE);return state!=AL10.AL_PLAYING;})
                                .findFirst().orElseThrow(()->new AssertionError("native ideal-cadence source pool exhausted"));
                        double frequency=bin*10.;int base=(int)Math.round(12*Math.log(frequency/20)/Math.log(2));
                        AL10.alSourcei(source,AL10.AL_BUFFER,buffers.get((base*16+phase)*2+side));
                        AL10.alSourcef(source,AL10.AL_GAIN,gain/65535f);
                        AL10.alSourcef(source,AL10.AL_PITCH,(float)(frequency/(20*Math.pow(2,base/12.))));
                        AL10.alSourcePlay(source);emitted++;
                    }
                    int active=0;for(int source:sources)if(AL10.alGetSourcei(source,AL10.AL_SOURCE_STATE)==AL10.AL_PLAYING)active++;
                    maxActive=Math.max(maxActive,active);require(active<=176,"ideal voice ceiling");
                    rendered.clear();SOFTLoopback.alcRenderSamplesSOFT(device,rendered,2400);encoded.clear();
                    for(int i=0;i<4800;i++){float value=rendered.get(i);require(Float.isFinite(value),"finite loopback output");peak=Math.max(peak,Math.abs(value));encoded.putFloat(value);}
                    out.write(encoded.array());error();
                }
            }
            System.out.println("PASS native Vorbis/OpenAL silent loopback: files="+files+" frames="+frames+" events="+emitted+" maxActive="+maxActive+" peak="+peak+" channelError="+maximumChannelError);
        }finally{
            MemoryUtil.memFree(rendered);
            for(int source:sources){AL10.alSourceStop(source);AL10.alDeleteSources(source);}
            for(int buffer:buffers.values())AL10.alDeleteBuffers(buffer);
            ALC10.alcMakeContextCurrent(0);ALC10.alcDestroyContext(context);ALC10.alcCloseDevice(device);
        }
    }
}
