package me.skywars2.game.genie;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import me.skywars2.SkyWars2;
import me.skywars2.game.genie.GenieConfig.WebSession;
import me.skywars2.game.genie.script.ScriptJson;

/** A fresh Gemini web conversation, with verification in a separate worker process. */
final class GenieGeminiWebTransport implements GenieScriptModel.Transport {
    static final int MAX_IMPORT = 128 * 1024;
    private static final int MAX_REPLY = 4_194_304;
    private static final String SID = "__Secure-1PSID", SID_TS = "__Secure-1PSIDTS";
    private volatile WebSession session;
    private WebSession persistedSession;
    private final Path configuration;
    private final Path directory;
    private volatile ScheduledExecutorService maintenance;

    enum SessionIssue {
        UNAUTHENTICATED("Вход в Gemini не подтверждён. Войди в Gemini и импортируй свежий JSON cookies."),
        LOCATION_REJECTED("Gemini недоступен из региона сервера."),
        TERMS_REQUIRED("Открой Gemini в браузере и прими актуальные условия использования."),
        ACCOUNT_UNTRUSTED("Gemini ограничил возможности этой сессии после проверки аккаунта."),
        ACCESS_TEMPORARILY_UNAVAILABLE("Gemini временно ограничил доступ для этой сессии."),
        ACCESS_RESTRICTED("Gemini отклонил доступ для этой сессии. Проверь доступ в веб-чате."),
        QUOTA_EXHAUSTED("Лимит Gemini исчерпан."),
        PRO_UNAVAILABLE("В текущей сессии Gemini недоступна модель Pro."),
        DEPENDENCY_UNAVAILABLE("На сервере не запустился Python или gemini-webapi."),
        NETWORK_TIMEOUT("Gemini не ответил вовремя. Проверь соединение сервера с Gemini."),
        NETWORK_ERROR("Серверу не удалось связаться с Gemini. Проверь его сетевой маршрут."),
        ATTACHMENT_FAILED("Gemini не смог прочитать проверочный файл. Проверь загрузку файлов в веб-чате.");

        final String hint;
        SessionIssue(String hint) { this.hint = hint; }
    }

    static final class SessionFailure extends IOException {
        final SessionIssue issue;
        SessionFailure(SessionIssue issue) {
            super(issue.name());
            this.issue = issue;
        }
    }

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

    GenieGeminiWebTransport(WebSession session, Path configuration) {
        this.session = session;
        this.persistedSession = session;
        this.configuration = configuration;
        this.directory = configuration.getParent().resolve("requests");
    }

    void startMaintenance() {
        if (maintenance != null) return;
        var worker = Executors.newSingleThreadScheduledExecutor(task -> {
            var thread = new Thread(task, "skywars2-genie-session");
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
        });
        maintenance = worker;
        worker.scheduleWithFixedDelay(() -> renew(worker), 1, 1, TimeUnit.MINUTES);
    }

    void stopMaintenance() {
        var worker = maintenance;
        maintenance = null;
        if (worker != null) worker.shutdownNow();
    }

    private void renew(ScheduledExecutorService worker) {
        if (maintenance != worker) return;
        try {
            if (!persistSession(worker)) return;
            WebSession expected = session;
            var result = invoke(expected.cookies(), true, null, null, null, null);
            if (maintenance != worker || Thread.currentThread().isInterrupted()) return;
            var timestamp = result.get("timestamp");
            if (timestamp == null || !timestamp.isJsonPrimitive() || !timestamp.getAsJsonPrimitive().isString())
                throw new IOException("Gemini returned no refreshed timestamp");
            var next = new WebSession(new Cookies(expected.cookies().psid(), timestamp.getAsString()), expected.id());
            if (!next.equals(expected)) {
                session = next;
                persistSession(worker);
            }
        } catch (SessionFailure failure) {
            if (maintenance == worker) {
                SkyWars2.LOGGER.info("Gemini web session refresh requires a fresh import ({})", failure.issue);
                worker.shutdown();
            }
        } catch (InterruptedException canceled) {
            Thread.currentThread().interrupt();
        } catch (IOException | RuntimeException failure) {
            if (maintenance == worker) SkyWars2.LOGGER.warn("Gemini web session refresh failed ({})",
                    failure.getClass().getSimpleName());
        }
    }

    private boolean persistSession(ScheduledExecutorService worker) throws IOException {
        if (session.equals(persistedSession)) return true;
        if (GenieConfig.refreshGeminiWeb(configuration, persistedSession, session)) {
            persistedSession = session;
            return true;
        }
        worker.shutdown();
        return false;
    }

    void testConnection() throws IOException, InterruptedException {
        if (!invoke(session.cookies(), false, null, null, null, null).has("connected"))
            throw new IOException("Gemini web session unavailable");
    }

    @Override public boolean independentConversations() { return true; }

    @Override public GenieScriptModel.Observation run(String prompt, String knowledge, String contract, byte[] verifier)
            throws IOException, InterruptedException {
        JsonObject result = invoke(session.cookies(), false, prompt, knowledge, contract, verifier);
        var receipt = result.getAsJsonObject("receipt");
        return new GenieScriptModel.Observation(result.get("reply").getAsString(), receipt == null ? List.of() : List.of(receipt));
    }

    private JsonObject invoke(Cookies cookies, boolean refresh, String prompt, String knowledge, String contract, byte[] verifier)
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
            var command = new ArrayList<>(List.of(python, script.toString(), directory.getParent().resolve("python").toString()));
            if (refresh) command.add("refresh");
            Process process;
            try {
                process = new ProcessBuilder(command).directory(request.toFile())
                        .redirectOutput(result.toFile()).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            } catch (IOException failure) {
                throw new SessionFailure(SessionIssue.DEPENDENCY_UNAVAILABLE);
            }
            try {
                try (var stdin = process.getOutputStream()) {
                    stdin.write(cookies.json().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                if (prompt != null) process.waitFor();
                else if (!process.waitFor(70, TimeUnit.SECONDS)) throw new SessionFailure(SessionIssue.NETWORK_TIMEOUT);
                if (process.exitValue() != 0 || Files.size(result) > MAX_REPLY) throw new IOException("Gemini web worker failed");
                var reply = ScriptJson.parse(Files.readString(result));
                if (reply.has("session_error")) {
                    try {
                        throw new SessionFailure(SessionIssue.valueOf(reply.get("session_error").getAsString()));
                    } catch (IllegalArgumentException invalidStatus) {
                        throw new IOException("Gemini web worker returned an unknown status");
                    }
                }
                return reply;
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
