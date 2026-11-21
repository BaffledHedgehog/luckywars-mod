package me.skywars2.media.client;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Extract only this JAR's native tools into a versioned local cache, without downloads or reflection. */
final class BundledMediaTools {
    private BundledMediaTools() {}

    static synchronized Optional<Mp4Decoder.Tools> find() {
        String os=System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String platform=(os.startsWith("windows")?"windows":os.contains("mac")?"macosx":"linux")+"-"
                +(Set.of("aarch64","arm64").contains(System.getProperty("os.arch"))?"arm64":"x86_64");
        String root="/skywars2/media/native/"+platform+"/";
        try(var stream=BundledMediaTools.class.getResourceAsStream(root+"manifest.properties")) {
            if(stream==null)return Optional.empty();
            var manifest=new Properties();manifest.load(stream);
            String version=manifest.getProperty("version");
            if(version==null||!version.matches("[0-9.\\-]+"))throw new IOException("Invalid bundled tool version");
            Path cache=Path.of(System.getProperty("skywars2.media.toolCache","skywars2-media-tools"))
                    .toAbsolutePath().normalize().resolve(version).resolve(platform);
            Files.createDirectories(cache);
            for(String name:new TreeSet<>(manifest.stringPropertyNames())) {
                if(name.equals("version"))continue;
                if(!name.matches("[A-Za-z0-9_.+\\-]+"))throw new IOException("Invalid bundled tool entry");
                String hash=manifest.getProperty(name);
                Path file=cache.resolve(name);
                if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)||!hash.equals(digest(file))) {
                    Path temporary=Files.createTempFile(cache,"unpack-",".tmp");
                    try {
                        try(var binary=BundledMediaTools.class.getResourceAsStream(root+name)) {
                            if(binary==null)throw new IOException("Missing bundled tool entry");
                            Files.copy(binary,temporary,StandardCopyOption.REPLACE_EXISTING);
                        }
                        if(!hash.equals(digest(temporary)))throw new IOException("Bundled tool checksum mismatch");
                        Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING);
                    } finally { Files.deleteIfExists(temporary); }
                }
                if(!platform.startsWith("windows-"))file.toFile().setExecutable(true,true);
            }
            String suffix=platform.startsWith("windows-")?".exe":"";
            return Optional.of(new Mp4Decoder.Tools(cache.resolve("ffmpeg"+suffix),cache.resolve("ffprobe"+suffix)));
        } catch(IOException|RuntimeException error) {
            org.slf4j.LoggerFactory.getLogger("skywars2-media").warn("Bundled phone decoder unavailable ({})",error.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private static String digest(Path path)throws IOException {
        try {
            var digest=MessageDigest.getInstance("SHA-256");
            try(var input=Files.newInputStream(path)){
                byte[] buffer=new byte[65536];int count;
                while((count=input.read(buffer))!=-1)digest.update(buffer,0,count);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch(NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
}
