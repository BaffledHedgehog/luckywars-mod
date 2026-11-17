package me.skywars2.game.genie;

import com.google.gson.Gson;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Local Firefox bridge configuration. Authentication stays with the browser profile. */
final class GenieConfig {
    private record FirefoxOptions(String node, String mcpDirectory, String stateDirectory, Integer port) {}

    static GenieScriptModel loadScript() {
        var folder = FabricLoader.getInstance().getConfigDir().resolve("skywars2/genie").toAbsolutePath().normalize();
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
