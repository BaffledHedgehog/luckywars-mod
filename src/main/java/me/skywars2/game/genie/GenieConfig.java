package me.skywars2.game.genie;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Objects;
import java.util.UUID;
import me.skywars2.SkyWars2;
import net.fabricmc.loader.api.FabricLoader;
import me.skywars2.game.genie.GenieGeminiWebTransport.Cookies;

/** Official server API when configured, with the local browser bridge for existing installations. */
final class GenieConfig {
    private static final String GEMINI_KEY = "SKYWARS2_GENIE_GEMINI_API_KEY";
    private static final String OPENAI_KEY = "SKYWARS2_GENIE_OPENAI_API_KEY";
    private static final Gson JSON = new Gson();
    private record FirefoxOptions(String node, String mcpDirectory, String stateDirectory, Integer port) {}

    enum Provider {
        GEMINI(GenieGeminiTransport.DEFAULT_BASE_URL, GenieGeminiTransport.DEFAULT_MODEL),
        OPENAI(GenieOpenAiTransport.DEFAULT_BASE_URL, GenieOpenAiTransport.DEFAULT_MODEL);

        final String baseUrl;
        final String model;

        Provider(String baseUrl, String model) {
            this.baseUrl = baseUrl;
            this.model = model;
        }
    }

    record Api(Provider provider, String baseUrl, String model, String key) {
        Api {
            Objects.requireNonNull(provider);
            baseUrl = baseUrl.strip();
            model = model.strip();
            key = key.strip();
            if (baseUrl.isEmpty()) baseUrl = provider.baseUrl;
            if (model.isEmpty()) model = provider.model;
            URI uri = URI.create(baseUrl);
            if (baseUrl.length() > 1024 || !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                    || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || !uri.equals(uri.normalize()) || !uri.getRawPath().matches("(?:/[A-Za-z0-9._~-]+)*/?")
                    || uri.getPort() == 0 || uri.getPort() > 65535
                    || !model.matches(provider == Provider.GEMINI ? "[A-Za-z0-9][A-Za-z0-9._-]{0,199}"
                            : "[A-Za-z0-9][A-Za-z0-9._:/-]{0,199}")
                    || !key.matches("[!-~]{1,4096}")) {
                throw new IllegalArgumentException("Invalid Genie API configuration");
            }
            if (!baseUrl.endsWith("/")) baseUrl += "/";
        }

        URI base() { return URI.create(baseUrl); }

        GenieScriptModel.Transport transport() {
            return switch (provider) {
                case GEMINI -> new GenieGeminiTransport(base(), model, key);
                case OPENAI -> new GenieOpenAiTransport(base(), model, key);
            };
        }

        @Override public String toString() { return "Genie API (" + provider + ")"; }
    }

    private static Path folder() {
        return FabricLoader.getInstance().getConfigDir().resolve("skywars2/genie").toAbsolutePath().normalize();
    }

    static Path apiPath() { return folder().resolve("api.json"); }
    static Path fallbackPath() { return folder().resolve("openai-fallback.json"); }

    static GenieScriptModel.Transport openAiFallback() {
        try {
            var settings = readApi(fallbackPath());
            if (settings != null && settings.provider() == Provider.OPENAI) return settings.transport();
        } catch (IOException | RuntimeException unreadable) {
            SkyWars2.LOGGER.warn("Genie OpenAI fallback configuration could not be loaded");
        }
        String key = System.getenv(OPENAI_KEY);
        return key == null || key.isBlank() ? null : new GenieOpenAiTransport(key.strip());
    }

    static Api readApi(Path path) throws IOException {
        var data = readConfiguration(path);
        return data == null ? null : JSON.fromJson(data, Api.class);
    }

    private static JsonObject readConfiguration(Path path) throws IOException {
        if (!Files.exists(path)) return null;
        try (var input = Files.newInputStream(path)) {
            byte[] data = input.readNBytes(32769);
            if (data.length > 32768) throw new IOException("Genie API configuration is too large");
            return Objects.requireNonNull(JSON.fromJson(new String(data, StandardCharsets.UTF_8), JsonObject.class));
        }
    }

    static synchronized void saveApi(Path path, Api settings) throws IOException {
        saveConfiguration(path, JSON.toJson(settings));
    }

    record WebSession(Cookies cookies, UUID id) {
        static WebSession read(JsonObject settings) {
            return new WebSession(Cookies.parse(settings.toString()), settings.has("session_id")
                    ? UUID.fromString(settings.get("session_id").getAsString()) : null);
        }
    }

    static synchronized void saveGeminiWeb(Path path, WebSession session) throws IOException {
        var settings = session.cookies().json();
        settings.addProperty("provider", "GEMINI_WEB");
        if (session.id() != null) settings.addProperty("session_id", session.id().toString());
        saveConfiguration(path, JSON.toJson(settings));
    }

    static synchronized boolean refreshGeminiWeb(Path path, WebSession expected, WebSession refreshed) throws IOException {
        var stored = readConfiguration(path);
        if (stored == null || !stored.has("provider") || !"GEMINI_WEB".equals(stored.get("provider").getAsString())
                || !expected.equals(WebSession.read(stored))) return false;
        saveGeminiWeb(path, refreshed);
        return true;
    }

    static GenieGeminiWebTransport geminiWeb(WebSession session) {
        return new GenieGeminiWebTransport(session, apiPath());
    }

    private static void saveConfiguration(Path path, String serialized) throws IOException {
        Files.createDirectories(path.getParent());
        var store = Files.getFileStore(path.getParent());
        Path pending = store.supportsFileAttributeView("posix")
                ? Files.createTempFile(path.getParent(), ".genie-api-", ".tmp",
                        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))
                : Files.createTempFile(path.getParent(), ".genie-api-", ".tmp");
        try {
            Files.writeString(pending, serialized);
            Files.move(pending, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(pending);
        }
    }

    static GenieScriptModel loadScript() {
        try {
            var settings = readConfiguration(apiPath());
            if (settings != null) {
                if ("GEMINI_WEB".equals(settings.get("provider").getAsString())) {
                    return new GenieScriptModel(geminiWeb(WebSession.read(settings)));
                }
                return new GenieScriptModel(JSON.fromJson(settings, Api.class).transport());
            }
        } catch (Exception unreadable) {
            SkyWars2.LOGGER.warn("Genie API configuration could not be loaded");
            return null;
        }
        String key = System.getenv(GEMINI_KEY);
        if (key != null && !key.isBlank()) return new GenieScriptModel(new GenieGeminiTransport(key.strip()));
        key = System.getenv(OPENAI_KEY);
        if (key != null && !key.isBlank()) return new GenieScriptModel(new GenieOpenAiTransport(key.strip()));
        var folder = folder();
        try {
            Path config = folder.resolve("firefox.json");
            FirefoxOptions options = Files.isRegularFile(config)
                ? new Gson().fromJson(Files.readString(config), FirefoxOptions.class)
                : new FirefoxOptions(null, null, null, null);
            Path shared = Path.of(".browser-tools").toAbsolutePath().normalize();
            Path mcp = options.mcpDirectory == null ? shared.resolve("firefox-mcp") : local(options.mcpDirectory);
            Path state = options.stateDirectory == null ? shared.resolve("firefox-state") : local(options.stateDirectory);
            int port = options.port == null ? 1025 : options.port;
            if (port < 1 || port > 65535 || !Files.isRegularFile(mcp.resolve("dist/server/index.js"))) return null;
            Path node = options.node == null ? defaultNode() : local(options.node);
            return new GenieScriptModel(new GenieFirefoxTransport(node, mcp, state, port, folder.resolve("requests")));
        } catch (Exception unreadable) {
            return null;
        }
    }

    private static Path local(String path) { return Path.of(path).toAbsolutePath().normalize(); }

    private static Path defaultNode() {
        String programs = System.getenv("ProgramFiles");
        if (programs != null) {
            Path installed = Path.of(programs,"nodejs","node.exe");
            if (Files.isRegularFile(installed)) return installed;
        }
        return Path.of("node");
    }
}
