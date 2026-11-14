package me.skywars2.game;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Seekable prepared video; only an index and one frame are resident, never a whole clip. */
final class TikTokMedia implements AutoCloseable {
    static final int WIDTH=64, HEIGHT=112, PIXELS=WIDTH*HEIGHT, FPS=20, RATE=24000, BANDS=24;
    static final int AUDIO_BYTES=RATE/FPS*2, MAX_JPEG=48000;
    record Frame(byte[] map, byte[] bands, byte[] pcm, byte[] jpeg) {}
    final String id;
    final Path directory;
    final int frames;
    private final FileChannel video;
    private final long[] offsets;

    TikTokMedia(String id, Path directory) throws IOException {
        if (!id.matches("[0-9]{8,24}|local")) throw new IOException("clip_invalid");
        this.id=id; this.directory=directory;
        var channel=FileChannel.open(directory.resolve("clip.tkp"),StandardOpenOption.READ);
        try {
            var header=ByteBuffer.allocate(28); read(channel,header,0); header.flip();
            if (header.getInt()!=0x544b5031) throw new IOException("clip_invalid");
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
                    if (offsets[i]<=previous || offsets[i]>channel.size()-4L-PIXELS-BANDS-AUDIO_BYTES)
                        throw new IOException("index_invalid");
                    previous=offsets[i];
                }
                if (offsets[0]!=28) throw new IOException("index_invalid");
            }
            video=channel;
        } catch (Throwable error) { channel.close(); throw error; }
    }

    Frame frame(int number, boolean highQuality) throws IOException {
        if (number<0 || number>=frames) throw new IOException("frame_invalid");
        long offset=offsets[number];
        var size=ByteBuffer.allocate(4); read(video,size,offset); size.flip(); int jpegLength=size.getInt();
        if (jpegLength<1 || jpegLength>MAX_JPEG) throw new IOException("frame_invalid");
        long end=offset+4L+PIXELS+BANDS+AUDIO_BYTES+jpegLength;
        if (end!=(number+1<frames?offsets[number+1]:video.size())) throw new IOException("frame_invalid");
        var bytes=ByteBuffer.allocate(PIXELS+BANDS+AUDIO_BYTES+(highQuality?jpegLength:0));
        read(video,bytes,offset+4); bytes.flip();
        byte[] map=new byte[PIXELS], bands=new byte[BANDS], pcm=new byte[AUDIO_BYTES];
        bytes.get(map); bytes.get(bands); bytes.get(pcm);
        byte[] jpeg=new byte[highQuality?jpegLength:0]; bytes.get(jpeg);
        return new Frame(map,bands,pcm,jpeg);
    }

    private static void read(FileChannel file, ByteBuffer buffer, long offset) throws IOException {
        while (buffer.hasRemaining()) { int count=file.read(buffer,offset); if(count<1)throw new IOException("clip_truncated"); offset+=count; }
    }
    @Override public void close() throws IOException { video.close(); }
}
