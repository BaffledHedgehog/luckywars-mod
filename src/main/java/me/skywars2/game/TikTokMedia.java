package me.skywars2.game;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.Inflater;
import java.util.zip.DataFormatException;

/** Seekable 480p palette frames, independently compressed on disk; bounded positional reads. */
final class TikTokMedia implements AutoCloseable {
    static final int WIDTH=480, HEIGHT=854, PIXELS=WIDTH*HEIGHT, FPS=20, RATE=48000, BANDS=48;
    static final int JPEG_WIDTH=720,JPEG_HEIGHT=1280;
    static final int AUDIO_FPS=60, SLICES=AUDIO_FPS/FPS, BAND_BYTES=BANDS*SLICES;
    static final int AUDIO_BYTES=RATE/FPS*4, MAX_JPEG=131072, MAX_COMPRESSED=PIXELS+1024;
    static final int SPECTRAL_PARTIALS=88, SPECTRAL_BYTES=2+4*SPECTRAL_PARTIALS;
    record Frame(byte[] map, byte[] bands, byte[] pcm, byte[] jpeg) {}
    final String id;
    final Path directory;
    final int frames;
    private final FileChannel video;
    private final long[] offsets;
    private volatile FileChannel spectral;

    TikTokMedia(String id, Path directory) throws IOException {
        if (!id.matches("[0-9]{8,24}|local")) throw new IOException("clip_invalid");
        this.id=id; this.directory=directory;
        var channel=FileChannel.open(directory.resolve("clip.tkp"),StandardOpenOption.READ);
        try {
            var header=ByteBuffer.allocate(28); read(channel,header,0); header.flip();
            if (header.getInt()!=0x544b5033) throw new IOException("clip_invalid");
            frames=header.getInt();
            if (frames<1 || frames>6000 || header.getInt()!=FPS || header.getInt()!=WIDTH
                    || header.getInt()!=HEIGHT || header.getInt()!=RATE || header.getInt()!=BANDS)
                throw new IOException("clip_invalid");
            offsets=new long[frames];
            try (var index=FileChannel.open(directory.resolve("clip.idx"),StandardOpenOption.READ)) {
                if (index.size()!=frames*8L) throw new IOException("index_invalid");
                var table=ByteBuffer.allocate(frames*8); read(index,table,0); table.flip();
                long previous=27;
                for (int i=0;i<frames;i++) {
                    offsets[i]=table.getLong();
                    if (offsets[i]<=previous || offsets[i]>channel.size()-8L-BAND_BYTES-AUDIO_BYTES-2)
                        throw new IOException("index_invalid");
                    previous=offsets[i];
                }
                if (offsets[0]!=28) throw new IOException("index_invalid");
            }
            video=channel;
            openSpectral();
        } catch (Throwable error) { channel.close(); throw error; }
    }
    private int[] sizes(int number) throws IOException {
        if(number<0||number>=frames)throw new IOException("frame_invalid");
        var size=ByteBuffer.allocate(8);read(video,size,offsets[number]);size.flip();int compressed=size.getInt(),jpeg=size.getInt();
        if(compressed<1||compressed>MAX_COMPRESSED||jpeg<1||jpeg>MAX_JPEG
                ||offsets[number]+8L+compressed+BAND_BYTES+AUDIO_BYTES+jpeg!=(number+1<frames?offsets[number+1]:video.size()))
            throw new IOException("frame_invalid");
        return new int[]{compressed,jpeg};
    }
    Frame frame(int number, boolean highQuality) throws IOException {
        var sizes=sizes(number);int compressed=sizes[0],jpegLength=sizes[1];
        var bytes=ByteBuffer.allocate(compressed+BAND_BYTES+AUDIO_BYTES+(highQuality?jpegLength:0));
        read(video,bytes,offsets[number]+8);bytes.flip();
        byte[] packed=new byte[compressed],map=new byte[PIXELS],bands=new byte[BAND_BYTES],pcm=new byte[AUDIO_BYTES];
        bytes.get(packed);bytes.get(bands);bytes.get(pcm);byte[] jpeg=new byte[highQuality?jpegLength:0];bytes.get(jpeg);
        var inflater=new Inflater();try{
            inflater.setInput(packed);int count=0;
            while(count<map.length){int n=inflater.inflate(map,count,map.length-count);if(n==0)break;count+=n;}
            if(count!=map.length||!inflater.finished()||inflater.getRemaining()!=0)throw new IOException("frame_invalid");
        }catch(DataFormatException error){throw new IOException("frame_invalid",error);}finally{inflater.end();}
        return new Frame(map,bands,pcm,jpeg);
    }
    /** The enhanced path skips both the palette bytes and decompression/tint allocation. */
    Frame clientFrame(int number) throws IOException {
        var sizes=sizes(number);var bytes=ByteBuffer.allocate(AUDIO_BYTES+sizes[1]);
        read(video,bytes,offsets[number]+8L+sizes[0]+BAND_BYTES);bytes.flip();
        byte[] pcm=new byte[AUDIO_BYTES],jpeg=new byte[sizes[1]];bytes.get(pcm);bytes.get(jpeg);
        return new Frame(new byte[0],new byte[0],pcm,jpeg);
    }
    /** The 60 Hz sound clock reads only its own small envelope, never decodes a video frame. */
    byte[] audio(long serial) throws IOException {
        int number=(int)((serial/SLICES)%frames),slice=(int)(serial%SLICES);var sizes=sizes(number);
        var bytes=ByteBuffer.allocate(BANDS);read(video,bytes,offsets[number]+8L+sizes[0]+slice*BANDS);return bytes.array();
    }
    /** Optional independently prepared audio; old video/PCM files stay valid. */
    boolean openSpectral() {
        if(spectral!=null)return true;
        FileChannel channel=null;
        try{
            channel=FileChannel.open(directory.resolve("clip.tka"),StandardOpenOption.READ);
            var header=ByteBuffer.allocate(16);read(channel,header,0);header.flip();
            if(header.getInt()!=0x544b4131||header.getInt()!=frames||header.getInt()!=FPS
                    ||header.getInt()!=SPECTRAL_PARTIALS||channel.size()!=16L+frames*SPECTRAL_BYTES)
                throw new IOException("spectral_invalid");
            spectral=channel;return true;
        }catch(IOException invalid){if(channel!=null)try{channel.close();}catch(IOException ignored){}return false;}
    }
    boolean hasSpectral(){return spectral!=null;}
    byte[] spectralAudio(long serial) throws IOException {
        if(serial<0)throw new IOException("frame_invalid");
        var channel=spectral;if(channel==null)throw new IOException("spectral_missing");
        var bytes=ByteBuffer.allocate(SPECTRAL_BYTES);
        read(channel,bytes,16L+(serial%frames)*SPECTRAL_BYTES);return bytes.array();
    }
    private static void read(FileChannel file, ByteBuffer buffer, long offset) throws IOException {
        while (buffer.hasRemaining()) { int count=file.read(buffer,offset); if(count<1)throw new IOException("clip_truncated"); offset+=count; }
    }
    @Override public void close() throws IOException {try{video.close();}finally{var audio=spectral;if(audio!=null)audio.close();}}
}
