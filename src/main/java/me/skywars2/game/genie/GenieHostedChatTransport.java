package me.skywars2.game.genie;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import me.skywars2.game.genie.script.ScriptJson;

/** A dedicated ChatGPT browser profile on the machine running the server. */
final class GenieHostedChatTransport implements GenieScriptModel.Transport {
    private static final ReentrantLock PROFILE_LOCK = new ReentrantLock(true);
    private static final int MAX_REPLY_BYTES = 4_194_304;
    private static final String STATUS_PREFIX = "GENIE_HOSTED_";
    private final Path node, playwright, chromium, profile, requests;

    enum Issue {
        AUTH_REQUIRED("Открой авторизованную сессию ChatGPT для джина."),
        ACCESS_CHALLENGE("ChatGPT запросил проверку браузера на хостинге."),
        RATE_LIMITED("Исчерпан лимит веб-чата ChatGPT."),
        MODEL_UNAVAILABLE("В аккаунте недоступна нужная модель ChatGPT."),
        INVALID_BROWSER_STATE("Файл не содержит корректный экспорт cookies для chatgpt.com."),
        PROFILE_REQUIRED("Не удалось открыть выделенный профиль Chrome для джина."),
        CANCELLED("Запрос ChatGPT прерван."),
        UNAVAILABLE("Не удалось запустить веб-чат ChatGPT на хостинге.");

        final String hint;
        Issue(String hint) { this.hint = hint; }
    }

    static final class Unavailable extends IOException {
        final Issue issue;
        Unavailable(Issue issue) { super(issue.name()); this.issue = issue; }
    }

    GenieHostedChatTransport(Path node, Path playwright, Path chromium, Path profile, Path requests) {
        this.node = node;
        this.playwright = playwright;
        this.chromium = chromium;
        this.profile = profile;
        this.requests = requests;
    }

    @Override public GenieScriptModel.Observation run(String prompt, String knowledge, String contract, byte[] verifier)
            throws IOException, InterruptedException {
        var reply = invoke(prompt, knowledge, contract, verifier, null);
        var witnesses = new ArrayList<JsonObject>();
        for (var execution : reply.getAsJsonArray("executions")) witnesses.add(execution.getAsJsonObject());
        return new GenieScriptModel.Observation(reply.get("reply").getAsString(), List.copyOf(witnesses));
    }

    void connect(String importedState) throws IOException, InterruptedException {
        var reply = invoke(null, null, null, null, importedState);
        if (!ready(reply)) throw new Unavailable(Issue.UNAVAILABLE);
    }

    private static boolean ready(JsonObject reply) {
        return reply.has("ready") && reply.get("ready").getAsBoolean();
    }

    private JsonObject invoke(String prompt, String knowledge, String contract, byte[] verifier, String importedState)
            throws IOException, InterruptedException {
        PROFILE_LOCK.lockInterruptibly();
        try {
            Path previous = profile.resolveSibling("chatgpt-profile-previous");
            if (Files.isDirectory(previous) && !Files.exists(profile)) {
                Files.move(previous, profile, StandardCopyOption.ATOMIC_MOVE);
            }
            Files.createDirectories(requests);
            boolean posix = Files.getFileStore(requests).supportsFileAttributeView("posix");
            var privateDirectory = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
            Path request = posix
                    ? Files.createTempDirectory(requests, "chatgpt-", privateDirectory)
                    : Files.createTempDirectory(requests, "chatgpt-");
            try {
                Path workerProfile = importedState == null ? profile : request.resolve("imported-profile");
                // The worker resolves the directory before starting the browser. Imported
                // state always starts in its own temporary profile until the probe succeeds.
                if (posix) Files.createDirectories(workerProfile, privateDirectory);
                else Files.createDirectories(workerProfile);
                var result = runWorker(request, workerProfile, prompt, knowledge, contract, verifier, importedState);
                if (importedState != null && ready(result)) replaceProfile(workerProfile, previous);
                return result;
            } finally {
                removeTree(request);
            }
        } finally {
            PROFILE_LOCK.unlock();
        }
    }

    private void replaceProfile(Path imported, Path previous) throws IOException {
        if (!Files.isDirectory(imported)) throw new Unavailable(Issue.UNAVAILABLE);
        Files.createDirectories(profile.getParent());
        if (Files.exists(previous)) removeTree(previous);
        if (Files.exists(profile)) Files.move(profile, previous, StandardCopyOption.ATOMIC_MOVE);
        try {
            Files.move(imported, profile, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException failure) {
            if (Files.exists(previous)) Files.move(previous, profile, StandardCopyOption.ATOMIC_MOVE);
            throw failure;
        }
        if (Files.exists(previous)) removeTree(previous);
    }

    private static void removeTree(Path root) throws IOException {
        List<Path> entries;
        try (var paths = Files.walk(root)) {
            entries = paths.sorted(Comparator.reverseOrder()).toList();
        }
        for (var entry : entries) Files.deleteIfExists(entry);
    }

    private JsonObject runWorker(Path request, Path workerProfile, String prompt, String knowledge, String contract, byte[] verifier,
                                 String importedState) throws IOException, InterruptedException {
        Path worker = GenieFirefoxTransport.resource(request, "genie_hosted_chat_worker.mjs");
        GenieFirefoxTransport.resource(request, "genie_chat_worker.mjs");
        GenieFirefoxTransport.resource(request, "genie_chat_page.js");
        String operation;
        if (prompt != null) {
            Files.writeString(request.resolve("prompt.txt"), prompt);
            Files.writeString(request.resolve("game_knowledge.json"), knowledge);
            Files.writeString(request.resolve("contract.json"), contract);
            Files.write(request.resolve("verify_genie_program.py"), verifier);
            operation = request.toString();
        } else operation = importedState == null ? "--probe" : "--import-probe";

        Path output = request.resolve("response.json"), failure = request.resolve("worker.status");
        var builder = new ProcessBuilder(node.toString(), worker.toString(), operation, workerProfile.toString())
                .directory(request.toFile()).redirectOutput(output.toFile()).redirectError(failure.toFile());
        builder.environment().put("SKYWARS2_PLAYWRIGHT_MODULE", playwright.toString());
        builder.environment().put("SKYWARS2_BROWSER_EXECUTABLE", chromium.toString());
        builder.environment().remove("SKYWARS2_CHATGPT_BROWSER_STATE");
        if (importedState != null) {
            Path state = request.resolve("browser-state.json");
            Files.writeString(state, importedState);
            builder.environment().put("SKYWARS2_CHATGPT_BROWSER_STATE", state.toString());
        }
        Process process = builder.start();
        try {
            if (prompt != null) process.waitFor();
            else if (!process.waitFor(80, TimeUnit.SECONDS)) throw new Unavailable(Issue.UNAVAILABLE);
            if (process.exitValue() != 0) throw new Unavailable(readIssue(failure));
            try (var stream = Files.newInputStream(output)) {
                byte[] data = stream.readNBytes(MAX_REPLY_BYTES + 1);
                if (data.length > MAX_REPLY_BYTES) throw new Unavailable(Issue.UNAVAILABLE);
                return ScriptJson.parse(new String(data, StandardCharsets.UTF_8));
            }
        } finally {
            if (process.isAlive()) GenieFirefoxTransport.cancel(process);
        }
    }

    private static Issue readIssue(Path file) throws IOException {
        try (var stream = Files.newInputStream(file)) {
            var output = new String(stream.readNBytes(8192), StandardCharsets.UTF_8);
            for (var issue : Issue.values()) {
                if (output.lines().anyMatch((STATUS_PREFIX + issue.name())::equals)) return issue;
            }
            return Issue.UNAVAILABLE;
        }
    }
}
