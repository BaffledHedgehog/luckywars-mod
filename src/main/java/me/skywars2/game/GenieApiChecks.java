package me.skywars2.game;

import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import me.skywars2.SkyWars2;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
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
        final boolean auth;
        final boolean wasOp;
        final int began;
        final java.lang.reflect.Field currentModel;
        final Object previousModel;
        Identifier savedId;

        Fixture(GameController game) throws Exception {
            this.game = game;
            require(!Files.exists(settings), "isolated fixture never overwrites credentials");
            participants = new WoolBucketChecks.Fixture(game, false);
            participants.prepareParticipants();
            auth = game.server().usesAuthentication();
            var players = game.server().getPlayerList();
            wasOp = players.isOp(participants.owner.nameAndId());
            currentModel = game.items().genie().getClass().getDeclaredField("scriptModel");
            currentModel.setAccessible(true);
            previousModel = currentModel.get(game.items().genie());
            currentModel.set(game.items().genie(), null);
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
                return ((CustomAll) dialog.actions().getFirst().action().orElseThrow()).id();
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
            var active = currentModel.get(game.items().genie());
            if (active == null) return false;
            var json = JsonParser.parseString(Files.readString(settings)).getAsJsonObject();
            require(json.get("provider").getAsString().equals("GEMINI")
                    && json.get("baseUrl").getAsString().equals("https://example.invalid/custom/v1beta/")
                    && json.get("model").getAsString().equals("gemini-fixture")
                    && json.get("key").getAsString().equals("synthetic-key"), "only accepted payload persisted");
            var transport = WoolBucketChecks.read(active, "transport");
            require(WoolBucketChecks.read(transport, "endpoint").toString()
                    .equals("https://example.invalid/custom/v1beta/models/gemini-fixture:generateContent"), "subsequent wishes use the new endpoint and model");
            open("skywars genie api");
            participants.client.listener.handleCustomClickAction(reply(savedId, "", "", "replay"));
            require(currentModel.get(game.items().genie()) == active, "reopening never exposes the key or accepts earlier replies");
            var config = Class.forName("me.skywars2.game.genie.GenieConfig");
            var load = config.getDeclaredMethod("loadScript");
            load.setAccessible(true);
            try (var loaded = (AutoCloseable) load.invoke(null)) {
                require(loaded != null, "settings persist across service recreation");
                var restored = WoolBucketChecks.read(loaded, "transport");
                require(WoolBucketChecks.read(restored, "endpoint").equals(WoolBucketChecks.read(transport, "endpoint")),
                        "restart uses the saved settings");
            }
            return true;
        }

        @Override public void close() throws Exception {
            game.server().setUsesAuthentication(auth);
            try {
                var service = game.items().genie();
                var active = (AutoCloseable) currentModel.get(service);
                currentModel.set(service, previousModel);
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
            SkyWars2.LOGGER.info("[check] PASS genie-api (native command, dialog and settings persistence, zero cloud requests)");
        } catch (Exception failure) {
            try { running.close(); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
            running = null;
            SkyWars2.LOGGER.error("[check] FAIL genie-api", failure);
        }
    }
}
