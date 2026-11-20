package me.skywars2.game.genie;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import me.skywars2.SkyWars2;

/** Bounded server-private records used to diagnose generated gifts. */
final class GenieWishAudit {
    private static final long MAX_FILE_BYTES = 2 * 1024 * 1024;
    private static final int MAX_REPLY_CHARS = 256 * 1024;

    private GenieWishAudit() {}

    static synchronized void record(String wish, String provider, String reply, String outcome) {
        var line = new JsonObject();
        line.addProperty("time", Instant.now().toString());
        line.addProperty("wish", wish);
        line.addProperty("provider", provider);
        line.addProperty("outcome", outcome);
        if (reply != null) {
            line.addProperty("reply", reply.substring(0, Math.min(reply.length(), MAX_REPLY_CHARS)));
            if (reply.length() > MAX_REPLY_CHARS) line.addProperty("truncated", true);
        }
        var bytes = (line + "\n").getBytes(StandardCharsets.UTF_8);
        var path = GenieConfig.apiPath().resolveSibling("wishes.jsonl");
        try {
            Files.createDirectories(path.getParent());
            if (Files.isRegularFile(path) && Files.size(path) + bytes.length > MAX_FILE_BYTES) {
                Files.move(path, path.resolveSibling("wishes.previous.jsonl"), StandardCopyOption.REPLACE_EXISTING);
            }
            if (!Files.exists(path)) {
                if (Files.getFileStore(path.getParent()).supportsFileAttributeView("posix")) {
                    Files.createFile(path, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
                } else {
                    Files.createFile(path);
                }
            }
            Files.write(path, bytes, StandardOpenOption.APPEND);
        } catch (IOException | RuntimeException failure) {
            SkyWars2.LOGGER.warn("Genie wish audit could not be written ({})", failure.getClass().getSimpleName());
        }
    }
}
