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
        var tools = Mp4Decoder.findTools().orElse(null);
        if (tools == null) {
            System.out.println("SKIP native MP4: optional FFmpeg and ffprobe not found");
            return;
        }
        Path temporary = Files.createTempDirectory(Path.of("build"), "native-mp4-");
        Path stereo = temporary.resolve("stereo.mp4");
        Path silent = temporary.resolve("silent.mp4");
        Path invalid = temporary.resolve("invalid.mp4");
        try {
            source(tools, stereo, true);
            source(tools, silent, false);
            check(tools, stereo, true);
            check(tools, silent, false);
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
        System.out.println("PASS native MP4: H.264 15 to 20 FPS; 720x1280 RGBA; AAC 44.1 to 48k stereo; silence; invalid file; cancellation");
    }

    private static void source(Mp4Decoder.Tools tools, Path path, boolean withAudio) throws Exception {
        var args = new ArrayList<>(List.of(tools.ffmpeg().toString(), "-hide_banner", "-loglevel", "error",
                "-nostdin", "-f", "lavfi", "-i", "color=c=0xe02020:s=180x320:r=15:d=0.6"));
        if (withAudio) {
            args.addAll(List.of("-f", "lavfi", "-i", "aevalsrc=0.25*sin(2*PI*440*t)|0.25*sin(2*PI*880*t):s=44100:d=0.6",
                    "-c:a", "aac"));
        }
        args.addAll(List.of("-c:v", "libx264", "-profile:v", "main", "-pix_fmt", "yuv420p", "-threads", "1",
                "-movflags", "+faststart", path.toString()));
        Process child = new ProcessBuilder(args).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        try {
            require(child.waitFor(10, TimeUnit.SECONDS) && child.exitValue() == 0, "generate small native MP4 fixture");
        } finally {
            if (child.isAlive()) child.toHandle().destroyForcibly();
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
                    require(pcm.length == 9_600, "one 50ms PCM frame at 48 kHz with two signed 16-bit channels");
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
        require(frames == 12 && nonzero == withAudio && differentStereo == withAudio,
                "0.6 seconds at 20 fps with exact stereo or silence");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
