package me.skywars2.game;

import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import me.skywars2.SkyWars2;
import me.skywars2.game.genie.GenieScriptModel;
import me.skywars2.game.genie.GenieService;
import me.skywars2.validation.mixin.GenieFixtureAccess;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.action.CustomAll;
import net.minecraft.server.dialog.input.TextInput;
import net.minecraft.server.permissions.PermissionSet;

/** Exercise the actual command and vanilla dialog packets with a synthetic credential. */
final class GenieApiChecks {
    private static Fixture running;

    private static void require(boolean success, String message) {
        if (!success) throw new IllegalStateException("Genie API: " + message);
    }

    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final WoolBucketChecks.Fixture participants;
        final Path settings = FabricLoader.getInstance().getConfigDir().resolve("skywars2/genie/api.json");
        final Path hostedEnabled = settings.resolveSibling("chatgpt-enabled.json");
        final boolean auth;
        final boolean wasOp;
        final int began;
        final GenieFixtureAccess.Service access;
        Identifier savedId;

        Fixture(GameController game) throws Exception {
            this.game = game;
            require(!Files.exists(settings) && !Files.exists(hostedEnabled), "isolated fixture never overwrites credentials");
            access = (GenieFixtureAccess.Service) (Object) game.items().genie();
            require(access.validation$scriptModel() == null, "isolated fixture requires no configured script provider");
            participants = new WoolBucketChecks.Fixture(game, false);
            participants.prepareParticipants();
            auth = game.server().usesAuthentication();
            var players = game.server().getPlayerList();
            wasOp = players.isOp(participants.owner.nameAndId());
            if (!wasOp) players.op(participants.owner.nameAndId());
            began = game.server().getTickCount();
        }

        Identifier open(String command) throws Exception {
            var dispatcher = game.server().getCommands().getDispatcher();
            require(dispatcher.execute(command, participants.owner.createCommandSourceStack()) == 1, "owner opens form");
            var packet = participants.client.packets().stream().filter(ClientboundShowDialogPacket.class::isInstance)
                    .map(ClientboundShowDialogPacket.class::cast).toList().getLast();
            var buffer = Unpooled.buffer();
            try {
                ClientboundShowDialogPacket.CONTEXT_FREE_STREAM_CODEC.encode(buffer, packet);
                var dialog = (MultiActionDialog) ClientboundShowDialogPacket.CONTEXT_FREE_STREAM_CODEC.decode(buffer).dialog().value();
                var inputs = dialog.common().inputs();
                var keys = command.endsWith(" api")
                        ? java.util.List.of("provider", "base_url", "model", "key") : java.util.List.of("cookies");
                require(inputs.stream().map(net.minecraft.server.dialog.Input::key).toList().equals(keys),
                        "wire form includes the expected credential inputs");
                require(inputs.getLast().control() instanceof TextInput key && key.initial().isEmpty(), "key is blank on the wire");
                if (command.endsWith(" gemini import") || command.endsWith(" gemini importfile")) {
                    var field = (TextInput) inputs.getLast().control();
                    require(field.maxLength() == (command.endsWith(" importfile") ? 2048 : 20000),
                            "Gemini opens the selected file or cookie JSON form");
                }
                var id = ((CustomAll) dialog.actions().getFirst().action().orElseThrow()).id();
                if (command.endsWith(" chatgpt import")) {
                    var field = (TextInput) inputs.getLast().control();
                    require(field.maxLength() == 21_840 && field.multiline().isPresent()
                                    && field.label().getContents() instanceof TranslatableContents label
                                    && label.getKey().equals("genie.api.chatgpt_cookies")
                                    && id.getPath().startsWith("genie/api/chatgpt/"),
                            "ChatGPT opens a multiline JSON field under only its cookie reply path");
                }
                return id;
            } finally {
                buffer.release();
            }
        }

        ServerboundCustomClickActionPacket reply(Identifier id, String url, String model, String key) {
            var payload = new CompoundTag();
            payload.putString("provider", "GEMINI");
            payload.putString("base_url", url);
            payload.putString("model", model);
            payload.putString("key", key);
            return new ServerboundCustomClickActionPacket(id, Optional.of(payload));
        }

        ServerboundCustomClickActionPacket cookieReply(Identifier id, String cookies) {
            var payload = new CompoundTag();
            payload.putString("cookies", cookies);
            var buffer = Unpooled.buffer();
            try {
                ServerboundCustomClickActionPacket.STREAM_CODEC.encode(buffer,
                        new ServerboundCustomClickActionPacket(id, Optional.of(payload)));
                var decoded = ServerboundCustomClickActionPacket.STREAM_CODEC.decode(buffer);
                require(!buffer.isReadable() && decoded.id().equals(id) && decoded.payload().orElseThrow().equals(payload),
                        "real native common-action codec preserves one complete JSON string");
                return decoded;
            } finally {
                buffer.release();
            }
        }

        void checkChatGptImport(GenieScriptModel configuredModel) throws Exception {
            var tree = game.server().getCommands().getDispatcher().getRoot().getChild("skywars").getChild("genie").getChild("chatgpt");
            require(tree != null && tree.getChild("import") != null && tree.getChild("import").getChildren().isEmpty()
                            && tree.getChild("importfile") != null && tree.getChild("importfile").getChild("path") != null
                            && tree.getChild("probe") != null,
                    "text import has no file argument; existing trusted file and probe commands are retained");
            game.server().setUsesAuthentication(false);
            try {
                var superseded = open("sw genie chatgpt import");
                var current = open("skywars genie chatgpt import");
                require(!superseded.equals(current), "ChatGPT text import is login-bound and renewed on reopen");
                String malformed = "\u6c34".repeat(21_840);
                var rejected = cookieReply(current, malformed);
                boolean vanillaRejected = false;
                try { cookieReply(Identifier.parse("skywars2:genie/api/fixture"), malformed); }
                catch (RuntimeException expected) { vanillaRejected = true; }
                require(vanillaRejected, "ordinary API dialogs retain the native 32 KiB accounting");
                int from = participants.client.packets().size();
                int otherFrom = participants.audience.packets().size();
                participants.audience.listener.handleCustomClickAction(rejected);
                participants.client.listener.handleCustomClickAction(cookieReply(superseded, malformed));
                require(participants.client.packets().size() == from && participants.audience.packets().size() == otherFrom,
                        "foreign and superseded replies never reach the cookie processor");
                participants.client.listener.handleCustomClickAction(rejected);
                participants.client.listener.handleCustomClickAction(rejected);
                var packets = participants.client.packets();
                var sent = packets.subList(from, packets.size()).stream().filter(ClientboundSystemChatPacket.class::isInstance)
                        .map(ClientboundSystemChatPacket.class::cast).toList();
                require(sent.size() == 1 && sent.getFirst().content().getContents() instanceof TranslatableContents error
                                && error.getKey().equals("genie.api.chatgpt_invalid_cookie_json")
                                && !sent.getFirst().content().getString().contains("\u6c34"),
                        "malformed input yields one private, credential-free rejection and cannot be replayed");
                require(!Files.exists(hostedEnabled) && access.validation$scriptModel() == configuredModel,
                        "malformed browser state neither enables ChatGPT nor replaces the configured Gemini provider");
            } finally {
                game.server().setUsesAuthentication(true);
            }
        }

        void begin() throws Exception {
            var commands = game.server().getCommands().getDispatcher();
            var owner = participants.owner.createCommandSourceStack();
            var node = commands.getRoot().getChild("skywars").getChild("genie");
            require(node != null && !node.canUse(owner.withPermission(PermissionSet.NO_PERMISSIONS)),
                    "command tree denies non-operators");
            game.server().setUsesAuthentication(false);
            require(commands.execute("skywars genie api", owner) == 0, "fixture uses an offline non-loopback connection");
            require(!open("sw genie gemini import").equals(open("skywars genie gemini importfile")),
                    "cookie paste and server file forms have distinct nonces");
            game.server().setUsesAuthentication(true);
            var invalid = open("skywars genie api");
            participants.client.listener.handleCustomClickAction(reply(invalid, "http://example.invalid/v1", "", "synthetic-key"));
            require(!Files.exists(settings), "plaintext API address rejected");
            participants.client.listener.handleCustomClickAction(reply(invalid, "", "", "synthetic-key"));
            savedId = open("sw genie api");
            var good = reply(savedId, "https://example.invalid/custom/v1beta", "gemini-fixture", "synthetic-key");
            participants.audience.listener.handleCustomClickAction(good);
            require(!Files.exists(settings), "foreign and spent nonces cannot save settings");
            participants.client.listener.handleCustomClickAction(good);
        }

        boolean step() throws Exception {
            require(game.server().getTickCount() - began < 160, "async form save completed");
            var active = access.validation$scriptModel();
            if (active == null) return false;
            var json = JsonParser.parseString(Files.readString(settings)).getAsJsonObject();
            require(json.get("provider").getAsString().equals("GEMINI")
                    && json.get("baseUrl").getAsString().equals("https://example.invalid/custom/v1beta/")
                    && json.get("model").getAsString().equals("gemini-fixture")
                    && json.get("key").getAsString().equals("synthetic-key"), "only accepted payload persisted");
            var endpoint = endpoint(active);
            require(endpoint.equals(URI.create("https://example.invalid/custom/v1beta/models/gemini-fixture:generateContent")),
                    "subsequent wishes use the new endpoint and model");
            open("skywars genie api");
            participants.client.listener.handleCustomClickAction(reply(savedId, "", "", "replay"));
            require(access.validation$scriptModel() == active, "reopening never exposes the key or accepts earlier replies");
            checkChatGptImport(active);
            try (var restarted = new GenieService(game)) {
                var loaded = ((GenieFixtureAccess.Service) (Object) restarted).validation$scriptModel();
                require(loaded != null && loaded != active, "settings load onto a newly initialized native service");
                require(endpoint(loaded).equals(endpoint), "restart uses the saved settings");
            }
            require(access.validation$scriptModel() == active, "closing the fresh service leaves the live provider intact");
            return true;
        }

        private static URI endpoint(GenieScriptModel model) {
            var transport = ((GenieFixtureAccess.Model) (Object) model).validation$transport();
            if (transport instanceof GenieFixtureAccess.Gemini gemini) return gemini.validation$endpoint();
            throw new IllegalStateException("Genie API: active provider is not native Gemini");
        }

        @Override public void close() throws Exception {
            game.server().setUsesAuthentication(auth);
            try {
                var active = access.validation$scriptModel();
                access.validation$scriptModel(null);
                if (active != null) active.close();
                Files.deleteIfExists(settings);
            } finally {
                if (!wasOp) game.server().getPlayerList().deop(participants.owner.nameAndId());
                participants.close();
            }
        }
    }

    static void run(GameController game) {
        require(running == null, "exclusive fixture");
        try {
            var fixture = new Fixture(game);
            running = fixture;
            fixture.begin();
        } catch (Exception error) {
            if (running != null) {
                try { running.close(); } catch (Exception cleanup) { error.addSuppressed(cleanup); }
                running = null;
            }
            throw new IllegalStateException("Genie API check setup", error);
        }
    }

    static void tick() {
        if (running == null) return;
        try {
            if (!running.step()) return;
            running.close();
            running = null;
            SkyWars2.LOGGER.info("[check] PASS genie-api (native command, dialogs, large private ChatGPT reply and replay/foreign rejection, settings persistence, zero cloud requests)");
        } catch (Exception failure) {
            try { running.close(); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
            running = null;
            SkyWars2.LOGGER.error("[check] FAIL genie-api", failure);
        }
    }
}
