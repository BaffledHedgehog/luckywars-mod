package me.skywars2.media.client;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Exercises real H.264/AAC, PCM resampling, silent playback, and cancellation without Minecraft networking. */
public final class NativeMp4Checks {
    private NativeMp4Checks() {}

    public static void main(String[] args) throws Exception {
        var tools = BundledMediaTools.find().orElseThrow(()->new AssertionError("FFmpeg/ffprobe must be inside the standalone JAR"));
        require(tools.equals(Mp4Decoder.findTools().orElseThrow()),"bundled decoder takes priority over system PATH");
        Path temporary = Files.createTempDirectory(Path.of("build"), "native-mp4-");
        Path stereo = temporary.resolve("stereo.mp4");
        Path silent = temporary.resolve("silent.mp4");
        Path invalid = temporary.resolve("invalid.mp4");
        try {
            source(tools, stereo, true);
            source(tools, silent, false);
            check(tools, stereo, true);
            check(tools, silent, false);
            for(int fps:new int[]{30,60})checkMotion(tools,temporary,fps);
            try (var decoder = new Mp4Decoder(stereo, tools)) {
                try (var frame = decoder.next()) {
                    require(frame != null, "native decoder produced a frame before cancellation");
                }
                decoder.cancel();
                try {
                    decoder.next();
                    throw new AssertionError("cancelled decoder must reject later pulls");
                } catch (InterruptedIOException expected) {}
            }
            Files.write(invalid, new byte[128]);
            try (var decoder = new Mp4Decoder(invalid, tools)) {
                try {
                    decoder.next();
                    throw new AssertionError("malformed MP4 must be rejected");
                } catch (IOException expected) {}
            }
        } finally {
            Files.deleteIfExists(invalid);
            Files.deleteIfExists(silent);
            Files.deleteIfExists(stereo);
            Files.deleteIfExists(temporary);
        }
        System.out.println("PASS native MP4: original15/30/60FPS H.264; 720x1280 RGBA; AAC44.1 to48k stereo; silence; invalid file; cancellation");
    }

    private static void checkMotion(Mp4Decoder.Tools tools,Path folder,int fps)throws Exception {
        Path file=folder.resolve("motion-"+fps+".mp4");
        try {
            try(var reference=NativeMp4Checks.class.getResourceAsStream("/media/h264-motion-"+fps+".mp4")){
                require(reference!=null,"motion source fixture");Files.copy(reference,file);
            }
            long started=System.nanoTime();int frames=0;long audioBytes=0;var hashes=new java.util.HashSet<Integer>();
            try(var decoder=new Mp4Decoder(file,tools)){
                for(var frame=decoder.next();frame!=null;frame=decoder.next()){
                    try(var current=frame){
                        require(decoder.frameRate().equals(new MediaRate(fps,1)),"preserve original source frame rate");
                        int hash=1;for(int n=0;n<64;n++)hash=31*hash+frame.image().getPixel(n*113%720,n*317%1280);
                        hashes.add(hash);audioBytes+=frame.pcm().length;frames++;
                    }
                }
            }
            double elapsed=(System.nanoTime()-started)/1e9;
            require(frames==fps*3&&audioBytes==48000*4*3,"all source frames and three seconds of audio decoded");
            require(hashes.size()>frames/2,"motion reference actually changes across source frames");
            System.out.printf(java.util.Locale.ROOT,"MP4_BENCH sourceFPS=%d frames=%d seconds=%.3f decodeFPS=%.1f%n",fps,frames,elapsed,frames/elapsed);
            require(elapsed<3,"local decoder must sustain the real source playback rate");
        }finally{Files.deleteIfExists(file);}
    }

    private static void source(Mp4Decoder.Tools tools, Path path, boolean withAudio) throws Exception {
        // Fixed H.264/AAC reference encoded with libx264: the LGPL decoder build
        // need not ship a GPL encoder just to run its playback regression.
        try(var input=NativeMp4Checks.class.getResourceAsStream(withAudio?"/media/h264-stereo.mp4":"/media/h264-silent.mp4")){
            require(input!=null,"packaged H.264 decoder reference");Files.copy(input,path);
        }
    }

    private static void check(Mp4Decoder.Tools tools, Path path, boolean withAudio) throws Exception {
        int frames = 0;
        boolean nonzero = false;
        boolean differentStereo = false;
        try (var decoder = new Mp4Decoder(path, tools)) {
            for (var next = decoder.next(); next != null; next = decoder.next()) {
                try (var frame = next) {
                    var image = frame.image();
                    var pcm = frame.pcm();
                    require(frame.serial() == frames && image.getWidth() == 720 && image.getHeight() == 1280,
                            "consecutive 20 FPS frames have phone render dimensions");
                    int pixel = image.getPixel(360, 640);
                    require((pixel >>> 24) == 255 && (pixel >>> 16 & 255) > 190
                                    && (pixel >>> 8 & 255) < 70 && (pixel & 255) < 70,
                            "decoded image keeps native opaque RGBA channel order");
                    require(pcm.length == decoder.frameRate().pcmBytes(frames), "exact source-frame PCM slice at48kHz stereo");
                    for (int i = 0; i < pcm.length; i += 4) {
                        nonzero |= (pcm[i] | pcm[i + 1] | pcm[i + 2] | pcm[i + 3]) != 0;
                        differentStereo |= pcm[i] != pcm[i + 2] || pcm[i + 1] != pcm[i + 3];
                    }
                    frames++;
                }
                require(next.image().isClosed(), "caller closes the consumed native image");
            }
            require(decoder.next() == null, "subsequent reads remain at clean EOF");
        }
        require(frames == 9 && nonzero == withAudio && differentStereo == withAudio,
                "0.6 seconds at original15fps with exact stereo or silence");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
