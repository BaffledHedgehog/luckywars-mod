package me.skywars2.game.genie;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import me.skywars2.SkyWars2;

/** One Responses request and one temporary Code Interpreter container per wish. */
final class GenieOpenAiTransport implements GenieScriptModel.Transport {
    static final String DEFAULT_BASE_URL = "https://api.openai.com/v1/";
    static final String DEFAULT_MODEL = "gpt-5.6-sol";
    private static final Duration REQUEST_LIMIT = Duration.ofSeconds(15);
    private final HttpClient client;
    private final URI api;
    private final String model;
    private final String key;

    GenieOpenAiTransport(String key) {
        this(URI.create(DEFAULT_BASE_URL), DEFAULT_MODEL, key);
    }

    GenieOpenAiTransport(URI api, String model, String key) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), api, model, key);
    }

    GenieOpenAiTransport(HttpClient client, URI api, String model, String key) {
        this.client = client;
        this.api = api;
        this.model = model;
        this.key = key;
    }

    @Override
    public GenieScriptModel.Observation run(String prompt, String knowledge, String contract, byte[] verifier)
            throws IOException, InterruptedException {
        var containerRequest = new JsonObject();
        containerRequest.addProperty("name", "LuckyWars item verifier");
        containerRequest.addProperty("memory_limit", "1g");
        var expires = new JsonObject();
        expires.addProperty("anchor", "last_active_at");
        expires.addProperty("minutes", 20);
        containerRequest.add("expires_after", expires);
        String container = string(post("containers", containerRequest, REQUEST_LIMIT), "id");
        if (!container.matches("cntr_[A-Za-z0-9_-]+")) {
            throw new IOException("unexpected verifier container ID");
        }

        try {
            String verifierPath = upload(container, "verify_genie_program.py", verifier);
            String contractPath = upload(container, "contract.json", contract.getBytes(StandardCharsets.UTF_8));
            String knowledgePath = upload(container, "game_knowledge.json", knowledge.getBytes(StandardCharsets.UTF_8));
            var body = new JsonObject();
            body.addProperty("model", model);
            body.addProperty("store", false);
            body.addProperty("max_output_tokens", 16384);
            body.addProperty("max_tool_calls", 6);
            var reasoning = new JsonObject();
            reasoning.addProperty("effort", "medium");
            body.add("reasoning", reasoning);
            var tools = new JsonArray();
            var python = new JsonObject();
            python.addProperty("type", "code_interpreter");
            python.addProperty("container", container);
            tools.add(python);
            body.add("tools", tools);
            var include = new JsonArray();
            include.add("code_interpreter_call.outputs");
            body.add("include", include);
            body.addProperty("instructions", """
                    The three request files are already mounted in the Python tool.
                    Read the game index at %s before proposing an item. For a grant write the proposed
                    program to /mnt/data/program.json. Run the original verifier with Python using
                    arguments [%s, %s, /mnt/data/program.json] and print its stdout.
                    A printed copy of a guessed receipt is invalid. Return the exact checked program.
                    """.formatted(knowledgePath, verifierPath, contractPath));
            body.addProperty("input", prompt);
            return observation(post("responses", body, Duration.ofSeconds(230)), container);
        } finally {
            deleteContainer(container);
        }
    }

    private String upload(String container, String filename, byte[] content) throws IOException, InterruptedException {
        String boundary = "lbc-" + UUID.randomUUID();
        String header = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\""
                + filename + "\"\r\nContent-Type: application/octet-stream\r\n\r\n";
        var body = HttpRequest.BodyPublishers.concat(HttpRequest.BodyPublishers.ofString(header),
                HttpRequest.BodyPublishers.ofByteArray(content),
                HttpRequest.BodyPublishers.ofString("\r\n--" + boundary + "--\r\n"));
        var request = request("containers/" + container + "/files", REQUEST_LIMIT)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary).POST(body).build();
        String path = string(send(request, "container file upload"), "path");
        if (!path.matches("/mnt/data/[A-Za-z0-9_.-]+")) {
            throw new IOException("unexpected verifier file path");
        }
        return path;
    }

    private void deleteContainer(String container) {
        try {
            var response = client.send(request("containers/" + container, Duration.ofSeconds(8)).DELETE().build(),
                    HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 300 && response.statusCode() != 404) {
                SkyWars2.LOGGER.warn("Genie verifier container cleanup returned HTTP {}", response.statusCode());
            }
        } catch (IOException cleanup) {
            SkyWars2.LOGGER.warn("Genie verifier container cleanup failed ({})", cleanup.getClass().getSimpleName());
        } catch (InterruptedException cancelled) {
            Thread.currentThread().interrupt();
        }
    }

    private HttpRequest.Builder request(String path, Duration timeout) {
        return HttpRequest.newBuilder(api.resolve(path)).timeout(timeout).header("Authorization", "Bearer " + key);
    }

    private JsonObject post(String path, JsonObject body, Duration timeout) throws IOException, InterruptedException {
        return send(request(path, timeout).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(), path);
    }

    private JsonObject send(HttpRequest request, String operation) throws IOException, InterruptedException {
        var response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            SkyWars2.LOGGER.warn("Genie OpenAI {} returned HTTP {}", operation, response.statusCode());
            throw new IOException("OpenAI " + operation + " returned HTTP " + response.statusCode());
        }
        if (response.body().length() > 4_194_304) throw new IOException("OpenAI response exceeds input bounds");
        try {
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (RuntimeException invalid) {
            throw new IOException("OpenAI " + operation + " returned invalid JSON");
        }
    }

    static GenieScriptModel.Observation observation(JsonObject response, String container) throws IOException {
        if (!"completed".equals(string(response, "status"))) throw new IOException("OpenAI response is incomplete");
        String reply = null;
        var executions = new ArrayList<JsonObject>();
        for (var item : array(response.get("output"))) {
            if (!item.isJsonObject()) continue;
            var out = item.getAsJsonObject();
            if ("message".equals(string(out, "type")) && "assistant".equals(string(out, "role"))
                    && !"commentary".equals(string(out, "phase"))) {
                var text = new StringBuilder();
                for (var content : array(out.get("content"))) {
                    if (!content.isJsonObject()) continue;
                    var part = content.getAsJsonObject();
                    if ("output_text".equals(string(part, "type"))) text.append(string(part, "text"));
                }
                if (!text.isEmpty()) reply = text.toString();
            }
            if ("code_interpreter_call".equals(string(out, "type")) && container.equals(string(out, "container_id"))
                    && "completed".equals(string(out, "status"))) {
                for (var result : array(out.get("outputs"))) {
                    if (!result.isJsonObject()) continue;
                    var part = result.getAsJsonObject();
                    if ("logs".equals(string(part, "type"))) collectReceipts(string(part, "logs"), executions);
                }
            }
        }
        if (reply == null || reply.isBlank()) throw new IOException("OpenAI did not return a wish reply");
        return new GenieScriptModel.Observation(reply, List.copyOf(executions));
    }

    private static void collectReceipts(String logs, List<JsonObject> results) {
        if (logs.length() > 65_536) return;
        for (String line : logs.split("\\R")) {
            if (results.size() >= 32) return;
            String text = line.strip();
            if (!text.startsWith("{") || text.length() > 700) continue;
            try {
                var result = JsonParser.parseString(text).getAsJsonObject();
                if (result.has("ok") && result.get("ok").equals(new com.google.gson.JsonPrimitive(true))) results.add(result);
            } catch (RuntimeException invalidOutput) {
                // Ordinary Python diagnostics do not provide a verifier receipt.
            }
        }
    }

    private static JsonArray array(JsonElement element) {
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : new JsonArray();
    }

    private static String string(JsonObject object, String key) {
        var value = object.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString() : "";
    }
}
