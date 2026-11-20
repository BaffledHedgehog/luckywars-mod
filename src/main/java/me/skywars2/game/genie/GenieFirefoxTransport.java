package me.skywars2.game.genie;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import me.skywars2.game.genie.script.ScriptJson;

/** Invokes a local worker using the operator's already-paired Firefox extension. */
final class GenieFirefoxTransport implements GenieScriptModel.Transport {
    private final Path node, mcp, state, workingDirectory;
    private final int port;

    GenieFirefoxTransport(Path node, Path mcp, Path state, int port, Path workingDirectory) {
        this.node = node;
        this.mcp = mcp;
        this.state = state;
        this.port = port;
        this.workingDirectory = workingDirectory;
    }

    @Override public GenieScriptModel.Observation run(String prompt, String knowledge, String contract, byte[] verifier)
            throws IOException, InterruptedException {
        Files.createDirectories(workingDirectory);
        Path request = Files.createTempDirectory(workingDirectory, "wish-");
        try {
            Path script = resource(request, "genie_chat_worker.mjs");
            resource(request, "genie_chat_page.js");
            Files.write(request.resolve("verify_genie_program.py"), verifier);
            Files.writeString(request.resolve("contract.json"), contract);
            Files.writeString(request.resolve("game_knowledge.json"), knowledge);
            Files.writeString(request.resolve("prompt.txt"), prompt);
            Path result = request.resolve("result.json");
            var process = new ProcessBuilder(node.toString(), script.toString(), mcp.toString(), state.toString(),
                    Integer.toString(port), request.toString())
                .directory(mcp.toFile()).redirectOutput(result.toFile()).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            try {
                if (!process.waitFor(250, TimeUnit.SECONDS)) throw new IOException("Firefox wish timed out");
                if (process.exitValue() != 0) throw new IOException("paired Firefox wish failed");
                if (Files.size(result) > 4_194_304) throw new IOException("Firefox reply exceeds input bounds");
                JsonObject observation = ScriptJson.parse(Files.readString(result));
                var witnesses = new ArrayList<JsonObject>();
                for (var execution : observation.getAsJsonArray("executions")) witnesses.add(execution.getAsJsonObject());
                return new GenieScriptModel.Observation(observation.get("reply").getAsString(), witnesses);
            } finally {
                if (process.isAlive()) cancel(process);
            }
        } finally {
            // This is a flat directory with files made by this one request.
            try (var files = Files.newDirectoryStream(request)) {
                for (var file : files) Files.deleteIfExists(file);
            }
            Files.deleteIfExists(request);
        }
    }

    static Path resource(Path directory, String name) throws IOException {
        Path file = directory.resolve(name);
        try (var input = GenieFirefoxTransport.class.getResourceAsStream("/skywars2/genie/" + name)) {
            if (input == null) throw new IOException("missing packaged browser worker resource");
            Files.copy(input, file);
        }
        return file;
    }

    static void cancel(Process process) {
        try (var stdin = process.getOutputStream()) {
            stdin.write("cancel\n".getBytes(StandardCharsets.UTF_8));
            stdin.flush();
            if (!process.waitFor(5, TimeUnit.SECONDS)) process.destroyForcibly();
        } catch (IOException failure) {
            process.destroyForcibly();
        } catch (InterruptedException interrupted) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }
}
