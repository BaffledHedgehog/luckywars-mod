package me.skywars2.media.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import me.skywars2.media.MediaPackets;

/**
 * One-pass MP4 decoding on the media worker. The downloader owns the source file and the
 * consumer owns each returned image. Calling cancel does not wait for a pipe or a child.
 * The caller should use HTTP prepared frames if no local FFmpeg/ffprobe pair is available.
 */
final class Mp4Decoder implements AutoCloseable {
    static final int WIDTH = MediaPackets.MAX_WIDTH;
    static final int HEIGHT = MediaPackets.MAX_HEIGHT;
    static final int RATE = 48_000;
    static final int CHANNELS = 2;
    static final int PCM_BYTES = RATE / MediaPackets.FPS * CHANNELS * Short.BYTES;
    private static final long MAX_FILE_BYTES = 64L * 1024 * 1024;
    private static final int MAX_SECONDS = 300;
    private static final int MAX_FRAMES = MAX_SECONDS * 120;
    private static final int RGBA_BYTES = WIDTH * HEIGHT * 4;
    private static final String VIDEO_FILTER = "scale=" + WIDTH + ":"
            + HEIGHT + ":force_original_aspect_ratio=decrease:force_divisible_by=2:flags=bilinear,pad=" + WIDTH
            + ":" + HEIGHT + ":(ow-iw)/2:(oh-ih)/2:black,setsar=1";

    record Tools(Path ffmpeg, Path ffprobe) {}

    record Frame(long serial, NativeImage image, byte[] pcm) implements AutoCloseable {
        @Override public void close() { image.close(); }
    }

    /** Executable overrides are local JVM options, never network or server configuration. */
    static Optional<Tools> findTools() {
        if(System.getProperty("skywars2.media.ffmpeg")==null&&System.getProperty("skywars2.media.ffprobe")==null){
            var bundled=BundledMediaTools.find();
            if(bundled.isPresent())return bundled;
        }
        String suffix = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows") ? ".exe" : "";
        Path ffmpeg = find("skywars2.media.ffmpeg", "ffmpeg" + suffix, null);
        if (ffmpeg == null) return Optional.empty();
        Path ffprobe = find("skywars2.media.ffprobe", "ffprobe" + suffix, ffmpeg.getParent());
        return ffprobe == null ? Optional.empty() : Optional.of(new Tools(ffmpeg, ffprobe));
    }

    private static Path find(String property, String name, Path alongside) {
        String override = System.getProperty(property);
        if (override != null) return executable(null, override);
        if (alongside != null) {
            Path neighbor = executable(alongside, name);
            if (neighbor != null) return neighbor;
        }
        String search = System.getenv("PATH");
        for (String directory : (search == null ? "" : search).split(Pattern.quote(java.io.File.pathSeparator))) {
            if (directory.isBlank()) continue;
            try {
                Path file = executable(Path.of(directory), name);
                if (file != null) return file;
            } catch (InvalidPathException invalid) { /* Continue to the next PATH entry. */ }
        }
        // Launchers can retain the PATH from before a WinGet installation. Still use the
        // user's installed tools without requiring a launcher restart or downloading binaries.
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")) {
            String local = System.getenv("LOCALAPPDATA");
            if (local != null) {
                Path file = executable(Path.of(local,"Microsoft","WinGet","Links"), name);
                if (file != null) return file;
            }
        }
        Path portable = executable(Path.of("skywars2-media-tools"), name);
        if (portable != null) return portable;
        return null;
    }

    private static Path executable(Path directory, String name) {
        try {
            Path path = (directory == null ? Path.of(name) : directory.resolve(name)).toAbsolutePath().normalize();
            return Files.isRegularFile(path) && Files.isExecutable(path) ? path : null;
        } catch (InvalidPathException invalid) {
            return null;
        }
    }

    private final Path source;
    private final Tools tools;
    private final List<Process> children = new ArrayList<>(2);
    private volatile boolean cancelled;
    private Process video;
    private volatile ServerSocket videoListener;
    private volatile Socket videoSocket;
    private InputStream videoInput;
    private Process audio;
    private byte[] rgba;
    private int serial;
    private boolean exhausted;
    private MediaRate frameRate=MediaRate.DEFAULT;
    MediaRate frameRate(){return frameRate;}

    Mp4Decoder(Path file, Tools tools) {
        this.source = file.toAbsolutePath().normalize();
        this.tools = tools;
    }

    /** Pull one normalized pair. Only one worker may call this method. Null means clean EOF. */
    Frame next() throws IOException {
        try {
            checkCancelled();
            if (exhausted) return null;
            if (video == null) start();
            int bytes = videoInput.readNBytes(rgba, 0, RGBA_BYTES);
            checkCancelled();
            if (bytes == 0) {
                await(video, 2);
                if (serial == 0) throw new IOException("media_mp4_video_empty");
                exhausted = true;
                return null;
            }
            if (bytes != RGBA_BYTES || serial >= MAX_FRAMES) throw new IOException("media_mp4_video_frame");
            byte[] pcm = readAudio();
            checkCancelled();
            var image = new NativeImage(WIDTH, HEIGHT, false);
            try {
                image.getPixelBytes().put(rgba);
                return new Frame(serial++, image, pcm);
            } catch (Throwable failure) {
                image.close();
                throw failure;
            }
        } catch (IOException error) {
            cancel();
            throw error;
        }
    }

    private void start() throws IOException {
        if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) throw new IOException("media_mp4_source");
        long size = Files.size(source);
        if (size < 12 || size > MAX_FILE_BYTES) throw new IOException("media_mp4_size");
        boolean withAudio = hasAudio();
        frameRate=probeFrameRate();
        // Windows anonymous process pipes use tiny buffers for multi-megabyte RGBA
        // frames. A buffered loopback connection avoids thousands of pipe wakeups.
        var listener=new ServerSocket();videoListener=listener;
        listener.setReceiveBufferSize(1024*1024);
        listener.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),0),1);
        listener.setSoTimeout(500);
        var videoArgs = ffmpeg();
        videoArgs.addAll(List.of("-max_pixels", "16777216", "-i", source.toString(), "-map", "0:v:0", "-an",
                "-sn", "-dn", "-vf", "fps="+frameRate+":start_time=0,"+VIDEO_FILTER, "-frames:v", Integer.toString(frameRate.framesFor(MAX_SECONDS)), "-threads:v", "1",
                "-c:v", "rawvideo", "-pix_fmt", "rgba", "-f", "rawvideo",
                "tcp://127.0.0.1:"+listener.getLocalPort()+"?tcp_nodelay=1&send_buffer_size=1048576"));
        video = launch(videoArgs);
        long deadline=System.nanoTime()+30_000_000_000L;
        try{
            for(;;){
                checkCancelled();
                try{videoSocket=listener.accept();break;}
                catch(SocketTimeoutException waiting){
                    if(!video.isAlive()){await(video,1);throw new IOException("media_mp4_empty_output");}
                    if(System.nanoTime()>deadline)throw new IOException("media_mp4_decoder_timeout");
                }
            }
            videoSocket.setTcpNoDelay(true);videoSocket.setReceiveBufferSize(1024*1024);
            videoInput=videoSocket.getInputStream();
        }finally{listener.close();videoListener=null;}
        if (withAudio) {
            var audioArgs = ffmpeg();
            audioArgs.addAll(List.of("-i", source.toString(), "-map", "0:a:0", "-vn", "-sn", "-dn",
                    "-af", "aresample=48000:async=1:first_pts=0", "-ar", Integer.toString(RATE), "-ac",
                    Integer.toString(CHANNELS), "-t", Integer.toString(MAX_SECONDS),
                    "-c:a", "pcm_s16le", "-f", "s16le", "pipe:1"));
            audio = launch(audioArgs);
        }
        rgba = new byte[RGBA_BYTES];
    }

    private List<String> input(Path executable) {
        return new ArrayList<>(List.of(executable.toString(), "-hide_banner", "-loglevel", "error",
                "-max_alloc", Long.toString(MAX_FILE_BYTES), "-protocol_whitelist", "file,pipe",
                "-f", "mov", "-enable_drefs", "0", "-use_absolute_path", "0"));
    }

    private List<String> ffmpeg() {
        var args = input(tools.ffmpeg());
        args.addAll(List.of("-nostdin", "-xerror", "-threads", "1", "-filter_threads", "1"));
        return args;
    }

    private boolean hasAudio() throws IOException {
        var args = input(tools.ffprobe());
        args.addAll(List.of("-select_streams", "a:0", "-show_entries", "stream=index", "-of", "csv=p=0",
                source.toString()));
        Process probe = launch(args);
        try {
            // Exactly one optional stream index can be printed, so the probe cannot fill its pipe.
            // A freshly extracted executable can take longer on first launch while the OS
            // scans its DLLs. This is a cancellable decoder worker, never the render thread.
            await(probe, 30);
            byte[] result = probe.getInputStream().readNBytes(32);
            String index = new String(result, StandardCharsets.US_ASCII).strip();
            if (index.isEmpty()) return false;
            if (!index.matches("[0-9]{1,8}")) throw new IOException("media_mp4_stream_index");
            return true;
        } finally {
            synchronized (children) { children.remove(probe); }
            release(probe);
        }
    }

    private MediaRate probeFrameRate()throws IOException {
        var args=input(tools.ffprobe());
        args.addAll(List.of("-select_streams","v:0","-show_entries","stream=avg_frame_rate","-of","csv=p=0",source.toString()));
        Process probe=launch(args);
        try{
            await(probe,30);
            String text=new String(probe.getInputStream().readNBytes(64),StandardCharsets.US_ASCII).strip();
            try{return MediaRate.parse(text);}catch(IllegalArgumentException missing){return MediaRate.DEFAULT;}
        }finally{synchronized(children){children.remove(probe);}release(probe);}
    }

    private byte[] readAudio() throws IOException {
        byte[] pcm = new byte[frameRate.pcmBytes(serial)];
        if (audio == null) return pcm;
        int bytes = audio.getInputStream().readNBytes(pcm, 0, pcm.length);
        if (bytes < pcm.length) {
            await(audio, 2);
            if (bytes % (CHANNELS * Short.BYTES) != 0) throw new IOException("media_mp4_pcm_frame");
            audio = null; // The tail of the final audio frame and any later frames stay silent.
        }
        return pcm;
    }

    private Process launch(List<String> command) throws IOException {
        checkCancelled();
        Process process;
        try {
            process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        } catch (IOException unavailable) {
            throw new IOException("media_mp4_decoder_unavailable");
        }
        boolean accepted;
        synchronized (children) {
            accepted = !cancelled;
            if (accepted) children.add(process);
        }
        if (!accepted) {
            release(process);
            throw new InterruptedIOException("media_mp4_cancelled");
        }
        process.getOutputStream().close();
        return process;
    }

    private void await(Process process, long seconds) throws IOException {
        try {
            if (!process.waitFor(seconds, TimeUnit.SECONDS)) throw new IOException("media_mp4_decoder_timeout");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("media_mp4_cancelled");
        }
        checkCancelled();
        if (process.exitValue() != 0) throw new IOException("media_mp4_unsupported");
    }

    private void checkCancelled() throws InterruptedIOException {
        if (cancelled || Thread.currentThread().isInterrupted()) throw new InterruptedIOException("media_mp4_cancelled");
    }

    void cancel() {
        synchronized (children) {
            if (cancelled) return;
            cancelled = true;
            try{if(videoListener!=null)videoListener.close();}catch(IOException ignored){}
            try{if(videoSocket!=null)videoSocket.close();}catch(IOException ignored){}
            for (var process : children) {
                if (process.isAlive()) process.toHandle().destroyForcibly();
            }
        }
    }

    /** Final pipe cleanup belongs to the same worker that pulls frames. */
    @Override public void close() {
        cancel();
        List<Process> remaining;
        synchronized (children) {
            remaining = List.copyOf(children);
            children.clear();
        }
        for (var process : remaining) release(process);
    }

    private static void release(Process process) {
        if (process.isAlive()) process.toHandle().destroyForcibly();
        try { process.getInputStream().close(); }
        catch (IOException ignored) { /* A terminated child may have closed the pipe first. */ }
        try { process.getOutputStream().close(); }
        catch (IOException ignored) { }
        // Let Windows release the downloaded file before the worker closes its Transfer.
        try { process.waitFor(2, TimeUnit.SECONDS); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
    }
}
