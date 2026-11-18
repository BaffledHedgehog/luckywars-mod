package me.skywars2.game.genie;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import me.skywars2.game.genie.script.ScriptJson;

/** A fresh Gemini web conversation, with verification in a separate worker process. */
final class GenieGeminiWebTransport implements GenieScriptModel.Transport {
    static final int MAX_IMPORT = 128 * 1024;
    private static final int MAX_REPLY = 4_194_304;
    private static final String SID = "__Secure-1PSID", SID_TS = "__Secure-1PSIDTS";
    private final Cookies cookies;
    private final Path directory;

    record Cookies(String psid, String psidts) {
        Cookies {
            if (!valid(psid) || !valid(psidts)) throw new IllegalArgumentException("Invalid Gemini cookie export");
        }

        private static boolean valid(String value) {
            return value != null && value.length() >= 8 && value.length() <= 4096
                    && value.chars().allMatch(c -> c >= 33 && c <= 126 && c != ';');
        }

        static Cookies parse(String input) {
            if (input.length() > MAX_IMPORT) throw new IllegalArgumentException("Gemini cookie export too large");
            JsonElement rows = JsonParser.parseString(input);
            if (rows.isJsonObject()) {
                var object = rows.getAsJsonObject();
                rows = object.get("cookies");
                if (rows == null) return new Cookies(string(object, SID), string(object, SID_TS));
            }
            if (!rows.isJsonArray()) throw new IllegalArgumentException("Expected a Gemini cookie export");
            String sid = null, timestamp = null;
            for (var row : rows.getAsJsonArray()) {
                if (!row.isJsonObject()) continue;
                var cookie = row.getAsJsonObject();
                String name = string(cookie, "name");
                String domain = string(cookie, "domain").toLowerCase(java.util.Locale.ROOT);
                if (domain.startsWith(".")) domain = domain.substring(1);
                if (!domain.equals("google.com") && !domain.equals("gemini.google.com")) continue;
                if (name.equals(SID)) sid = unique(sid, string(cookie, "value"));
                if (name.equals(SID_TS)) timestamp = unique(timestamp, string(cookie, "value"));
            }
            return new Cookies(sid, timestamp);
        }

        private static String string(JsonObject value, String key) {
            var field = value.get(key);
            return field != null && field.isJsonPrimitive() && field.getAsJsonPrimitive().isString() ? field.getAsString() : "";
        }

        private static String unique(String previous, String value) {
            if (previous != null && !previous.equals(value)) throw new IllegalArgumentException("Conflicting Gemini cookies");
            return value;
        }

        JsonObject json() {
            var json = new JsonObject();
            json.addProperty(SID, psid);
            json.addProperty(SID_TS, psidts);
            return json;
        }

        @Override public String toString() { return "Gemini web session"; }
    }

    GenieGeminiWebTransport(Cookies cookies, Path directory) {
        this.cookies = cookies;
        this.directory = directory;
    }

    void testConnection() throws IOException, InterruptedException {
        if (!invoke(null, null, null, null).has("connected")) throw new IOException("Gemini web session unavailable");
    }

    @Override public GenieScriptModel.Observation run(String prompt, String knowledge, String contract, byte[] verifier)
            throws IOException, InterruptedException {
        JsonObject result = invoke(prompt, knowledge, contract, verifier);
        var receipt = result.getAsJsonObject("receipt");
        return new GenieScriptModel.Observation(result.get("reply").getAsString(), receipt == null ? List.of() : List.of(receipt));
    }

    private JsonObject invoke(String prompt, String knowledge, String contract, byte[] verifier)
            throws IOException, InterruptedException {
        Files.createDirectories(directory);
        Path request = Files.createTempDirectory(directory, "gemini-");
        try {
            Path script = request.resolve("genie_gemini_web.py");
            try (var source = getClass().getResourceAsStream("/skywars2/genie/genie_gemini_web.py")) {
                if (source == null) throw new IOException("missing Gemini web worker");
                Files.copy(source, script);
            }
            if (prompt != null) {
                Files.writeString(request.resolve("prompt.txt"), prompt);
                Files.writeString(request.resolve("game_knowledge.json"), knowledge);
                Files.writeString(request.resolve("contract.json"), contract);
                Files.write(request.resolve("verify_genie_program.py"), verifier);
            }
            Path result = request.resolve("response.json");
            String python = System.getenv("SKYWARS2_GENIE_PYTHON");
            if (python == null || python.isBlank()) python = java.io.File.separatorChar == '\\' ? "python" : "python3";
            var process = new ProcessBuilder(python, script.toString(), directory.getParent().resolve("python").toString())
                    .directory(request.toFile()).redirectOutput(result.toFile()).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            try {
                try (var stdin = process.getOutputStream()) {
                    stdin.write(cookies.json().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                if (!process.waitFor(prompt == null ? 70 : 180, TimeUnit.SECONDS)) throw new IOException("Gemini web worker timed out");
                if (process.exitValue() != 0 || Files.size(result) > MAX_REPLY) throw new IOException("Gemini web worker failed");
                return ScriptJson.parse(Files.readString(result));
            } finally {
                if (process.isAlive()) {
                    process.destroyForcibly();
                    process.waitFor(5, TimeUnit.SECONDS);
                }
            }
        } finally {
            // The client may leave its session cache here if the process was interrupted.
            try (var paths = Files.walk(request)) {
                for (var file : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(file);
            }
        }
    }
}
