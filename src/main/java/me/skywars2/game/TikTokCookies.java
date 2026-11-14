package me.skywars2.game;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;

/** Imported account credentials, isolated from profiles, command arguments and log messages. */
final class TikTokCookies {
    static final int MAX_INPUT = 4*1024*1024;
    // Raw vanilla dialog strings cannot exceed their own 64 KiB UTF-8 packet/string cap.
    static final int MAX_DIALOG_INPUT = 20000;
    private TikTokCookies() {}

    static String normalize(String input) {
        if (input == null || input.isBlank() || input.length() > MAX_INPUT
                || input.getBytes(StandardCharsets.UTF_8).length > MAX_INPUT)
            throw new IllegalArgumentException("cookies_invalid");
        final JsonElement parsed;
        try { parsed = JsonParser.parseString(input); }
        catch (RuntimeException invalid) { throw new IllegalArgumentException("cookies_invalid"); }
        JsonElement list = parsed.isJsonObject() ? parsed.getAsJsonObject().get("cookies") : parsed;
        if (list == null || !list.isJsonArray() || list.getAsJsonArray().size() > 200)
            throw new IllegalArgumentException("cookies_invalid");
        var result = new JsonArray();
        try {
            for (var element : list.getAsJsonArray()) {
                if (!element.isJsonObject()) throw new IllegalArgumentException();
                var cookie = element.getAsJsonObject();
                String domain = cookie.get("domain").getAsString().toLowerCase(java.util.Locale.ROOT);
                String host = domain.startsWith(".") ? domain.substring(1) : domain;
                if (!host.equals("tiktok.com") && !host.endsWith(".tiktok.com")) continue;
                String name = cookie.get("name").getAsString(), value = cookie.get("value").getAsString();
                String path = cookie.has("path") ? cookie.get("path").getAsString() : "/";
                if (domain.length()>253 || name.isBlank() || name.length() > 256 || value.length() > 8192 || !path.startsWith("/") || path.length()>2048
                        || (domain + name + value + path).chars().anyMatch(c -> c < 32 || c == 127))
                    throw new IllegalArgumentException();
                var clean = new JsonObject();
                clean.addProperty("domain", domain); clean.addProperty("name", name);
                clean.addProperty("value", value); clean.addProperty("path", path);
                clean.addProperty("secure", !cookie.has("secure") || cookie.get("secure").isJsonNull() || cookie.get("secure").getAsBoolean());
                clean.addProperty("httpOnly", cookie.has("httpOnly") && !cookie.get("httpOnly").isJsonNull() && cookie.get("httpOnly").getAsBoolean());
                for (String key : new String[]{"expirationDate", "expires"}) if (cookie.has(key)&&!cookie.get(key).isJsonNull()) {
                    double expires = cookie.get(key).getAsDouble();
                    if (!Double.isFinite(expires)) throw new IllegalArgumentException();
                    clean.addProperty("expires", expires); break;
                }
                if (cookie.has("sameSite")&&!cookie.get("sameSite").isJsonNull()) clean.addProperty("sameSite", cookie.get("sameSite").getAsString());
                result.add(clean);
            }
        } catch (RuntimeException invalid) { throw new IllegalArgumentException("cookies_invalid"); }
        if (result.isEmpty()) throw new IllegalArgumentException("cookies_invalid");
        return result.toString();
    }

    static String fromFile(Path file)throws IOException{
        if(!Files.isRegularFile(file)||Files.size(file)>MAX_INPUT)throw new IOException("cookie_file_invalid");
        byte[] bytes;
        try(var stream=Files.newInputStream(file)){bytes=stream.readNBytes(MAX_INPUT+1);}
        if(bytes.length>MAX_INPUT)throw new IOException("cookie_file_invalid");
        String input=StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString();
        if(input.startsWith("\ufeff"))input=input.substring(1);
        return normalize(input);
    }

    static void save(Path file, String normalized) throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "cookies-", ".tmp");
        try {
            if (Files.getFileStore(temporary).supportsFileAttributeView("posix"))
                Files.setPosixFilePermissions(temporary, PosixFilePermissions.fromString("rw-------"));
            Files.writeString(temporary, normalized, StandardCharsets.UTF_8);
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
