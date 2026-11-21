package me.skywars2.game.items.phone;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.Inflater;
import java.util.zip.DataFormatException;

/** Seekable 480p palette frames, independently compressed on disk; bounded positional reads. */
final class TikTokMedia implements AutoCloseable {
    static final int WIDTH=480, HEIGHT=854, PIXELS=WIDTH*HEIGHT, FPS=20, RATE=48000, BANDS=48;
    static final int JPEG_WIDTH=720,JPEG_HEIGHT=1280;
    static final int AUDIO_FPS=60, SLICES=AUDIO_FPS/FPS, BAND_BYTES=BANDS*SLICES;
    static final int AUDIO_BYTES=RATE/FPS*4, MAX_JPEG=131072, MAX_COMPRESSED=PIXELS+1024;
    static final int SPECTRAL_PARTIALS=88, SPECTRAL_BYTES=2+4*SPECTRAL_PARTIALS;
    private static final int MAX_SOURCE_URL=4096, SOURCE_TTL_SECONDS=3600;
    private static final List<String> CDN_HOSTS=List.of("tiktokcdn.com","tiktokcdn-us.com","tiktokcdn-eu.com",
            "tiktokcdn-in.com","tiktokv.com","muscdn.com");
    private static final Set<String> CDN_EXPIRATION_KEYS=Set.of("expire","expires","x-expires");
    // Mirrors the isolated Python worker. Only media routing and resource-scoped signing keys are admitted.
    private static final Set<String> CDN_QUERY_KEYS=Set.of("a","br","bt","btag","bti","cd","ch","codec","cr",
            "cs","cv","dr","ds","er","expire","expires","ft","key-pair-id","l","lr","mime_type","ply_type","policy",
            "qs","rc","signature","signaturev3","tk","x-expires","x-keyid","x-signature");
    private static final Pattern CDN_URL_TEXT=Pattern.compile("[\\x21-\\x7e]+");
    private static final Pattern CDN_PATH=Pattern.compile("/video/tos/(?:[A-Za-z0-9_-]+/)*[A-Za-z0-9_-]+/?");
    private static final Pattern CDN_VALUE=Pattern.compile("[A-Za-z0-9_.,:~|+/!-]*={0,2}");
    private static final Pattern UNIX_SECONDS=Pattern.compile("[0-9]{9,12}");
    record Frame(byte[] map, byte[] bands, byte[] pcm, byte[] jpeg) {}
    record CdnSource(String clipId, URI uri, long expiresAt) {
        @Override public String toString(){return "CdnSource[clipId="+clipId+", expiresAt="+expiresAt+"]";}
    }
    final String id;
    final Path directory;
    final int frames;
    private final FileChannel video;
    private final long[] offsets;
    private final CdnSource source;
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
            source=readSource(id,directory.resolve("source.json"),Instant.now().getEpochSecond());
            openSpectral();
        } catch (Throwable error) { channel.close(); throw error; }
    }
    Optional<CdnSource> cdnSource() {
        return source!=null && source.expiresAt()>Instant.now().getEpochSecond() ? Optional.of(source) : Optional.empty();
    }
    private static CdnSource readSource(String id, Path sidecar, long now) {
        if (id.equals("local") || !Files.isRegularFile(sidecar,LinkOption.NOFOLLOW_LINKS)) return null;
        int maxBytes=MAX_SOURCE_URL+256;
        try (var stream=Files.newInputStream(sidecar,LinkOption.NOFOLLOW_LINKS)) {
            byte[] bytes=stream.readNBytes(maxBytes+1);
            if (bytes.length>maxBytes) return null;
            var json=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
            if (!json.keySet().equals(Set.of("clipId","url","expiresAt"))) return null;
            var clipId=json.getAsJsonPrimitive("clipId");
            var url=json.getAsJsonPrimitive("url");
            var expiresAt=json.getAsJsonPrimitive("expiresAt");
            if (!clipId.isString() || !clipId.getAsString().equals(id) || !url.isString() || !expiresAt.isNumber()
                    || !UNIX_SECONDS.matcher(expiresAt.getAsString()).matches()) return null;
            long expires=expiresAt.getAsLong();
            String raw=url.getAsString();
            if (expires<=now || expires>now+SOURCE_TTL_SECONDS || raw.length()>MAX_SOURCE_URL
                    || !CDN_URL_TEXT.matcher(raw).matches()) return null;
            URI uri=URI.create(raw);
            return cdnExpiration(uri)>=expires ? new CdnSource(id,uri,expires) : null;
        } catch (IOException | RuntimeException unavailable) {
            // This optional sidecar cannot invalidate an otherwise playable clip.
            return null;
        }
    }
    private static long cdnExpiration(URI uri) {
        var host=uri.getHost();
        if (!"https".equalsIgnoreCase(uri.getScheme()) || host==null || !host.equalsIgnoreCase(uri.getRawAuthority())
                || uri.getRawFragment()!=null || !CDN_PATH.matcher(uri.getRawPath()).matches()) return -1;
        String lower=host.toLowerCase(Locale.ROOT);
        if (CDN_HOSTS.stream().noneMatch(domain->lower.equals(domain)||lower.endsWith("."+domain))) return -1;
        var query=uri.getRawQuery();
        if (query==null) return -1;
        var pairs=query.split("&",33);
        if (pairs.length>32) return -1;
        var seen=new HashSet<String>();
        long expires=-1;
        for (var pair:pairs) {
            int split=pair.indexOf('=');
            if (split<1) return -1;
            var key=pair.substring(0,split).toLowerCase(Locale.ROOT);
            if (!CDN_QUERY_KEYS.contains(key) || !seen.add(key)) return -1;
            var value=URLDecoder.decode(pair.substring(split+1).replace("+","%2B"),StandardCharsets.UTF_8);
            if (!CDN_VALUE.matcher(value).matches() || key.equals("tk") && !value.equals("tt_chain_token")) return -1;
            if (CDN_EXPIRATION_KEYS.contains(key)) {
                if (expires!=-1 || !UNIX_SECONDS.matcher(value).matches()) return -1;
                expires=Long.parseLong(value);
            }
        }
        return expires;
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
