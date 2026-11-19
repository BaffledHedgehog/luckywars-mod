package me.skywars2.game.genie;

import com.mojang.brigadier.arguments.StringArgumentType;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import me.skywars2.SkyWars2;
import me.skywars2.game.SkyWarsCommandTree;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.DialogAction;
import net.minecraft.server.dialog.Input;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.action.CustomAll;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.dialog.input.SingleOptionInput;
import net.minecraft.server.dialog.input.TextInput;
import net.minecraft.server.level.ServerPlayer;

/** Owner-only configuration, with a blank key field and a single-use reply bound to the login. */
final class GenieApiDialog implements AutoCloseable {
    static final String PREFIX = "genie/api/";
    private static final int EXPIRY_TICKS = 2400;
    private final Map<ServerPlayer, Pending> pending = new WeakHashMap<>();
    private final ExecutorService writer = Executors.newSingleThreadExecutor(task -> {
        var thread = new Thread(task, "skywars2-genie-settings");
        thread.setDaemon(true);
        return thread;
    });
    private boolean saving;
    private boolean closed;

    private enum Form { API, GEMINI_COOKIES, GEMINI_FILE }
    private record Pending(Identifier id, int opened, Form form) {}

    static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) ->
                SkyWarsCommandTree.add(dispatcher, Commands.literal("genie")
                        .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                        .then(Commands.literal("api").executes(context -> open(context.getSource().getPlayerOrException(), Form.API)))
                        .then(Commands.literal("gemini")
                                .then(Commands.literal("import").executes(context -> open(context.getSource().getPlayerOrException(), Form.GEMINI_COOKIES)))
                                .then(Commands.literal("importfile")
                                        .executes(context -> open(context.getSource().getPlayerOrException(), Form.GEMINI_FILE))
                                        .then(Commands.argument("path", StringArgumentType.greedyString()).executes(context ->
                                                importFile(context.getSource(), StringArgumentType.getString(context, "path"))))))));
    }

    private static int open(ServerPlayer player, Form form) {
        var game = SkyWars2.game();
        return game != null && game.items().genie().api.openForm(player, form) ? 1 : 0;
    }

    private static int importFile(CommandSourceStack source, String path) {
        var game = SkyWars2.game();
        if (game == null) return 0;
        var service = game.items().genie();
        return service.api.importGemini(service, source.getServer(), true, path, source::sendSystemMessage) ? 1 : 0;
    }

    private static boolean owner(ServerPlayer player) {
        return player.level().getServer().getPlayerList().getPlayer(player.getUUID()) == player
                && Commands.hasPermission(Commands.LEVEL_OWNERS).test(player.createCommandSourceStack());
    }

    private static boolean privateConnection(ServerPlayer player) {
        // API key entry requires a direct authenticated or local connection.
        // Gemini imports also support an offline backend behind a proxy.
        return player.level().getServer().usesAuthentication()
                || player.connection.getRemoteAddress() instanceof InetSocketAddress remote
                && remote.getAddress() != null && remote.getAddress().isLoopbackAddress();
    }

    private boolean openForm(ServerPlayer player, Form form) {
        if (closed || !owner(player)) return false;
        if (form == Form.API && !privateConnection(player)) {
            message(player, "private_connection", "Для ввода ключа подключись локально или через сервер с online-mode=true.", true);
            return false;
        }
        var id = Identifier.fromNamespaceAndPath("skywars2", PREFIX + UUID.randomUUID());
        pending.put(player, new Pending(id, player.level().getServer().getTickCount(), form));
        player.openDialog(form == Form.API ? dialog(id) : geminiDialog(id, form == Form.GEMINI_FILE));
        return true;
    }

    private static Holder<net.minecraft.server.dialog.Dialog> geminiDialog(Identifier id, boolean file) {
        var field = new Input("cookies", new TextInput(360, text(file ? "file" : "cookies",
                file ? "Путь на сервере" : "JSON cookies Gemini"), true, "", file ? 2048 : 20000,
                file ? Optional.empty() : Optional.of(new TextInput.MultilineOptions(Optional.empty(), Optional.of(130)))));
        var data = new CommonDialogData(text("gemini_title", "Подключить веб-чат Gemini"), Optional.empty(), true, false,
                DialogAction.CLOSE, List.of(new PlainMessage(text(file ? "file_hint" : "cookies_hint",
                    file ? "Укажи путь к JSON с cookies Gemini на сервере."
                         : "Вставь JSON с cookies Gemini. Для большого файла используй /sw genie gemini importfile."), 360)),
                List.of(field));
        return confirmation(data, id);
    }

    private static Holder<net.minecraft.server.dialog.Dialog> dialog(Identifier id) {
        var profiles = List.of(
                new SingleOptionInput.Entry("GEMINI", Optional.of(Component.literal("Gemini · GenerateContent")), true),
                new SingleOptionInput.Entry("OPENAI", Optional.of(Component.literal("OpenAI · Responses")), false));
        var provider = new Input("provider", new SingleOptionInput(360, profiles, text("provider", "API"), true));
        var data = new CommonDialogData(text("title", "Подключить API джина"), Optional.empty(), true, false, DialogAction.CLOSE,
                List.of(new PlainMessage(text("hint", "Адрес и модель можно оставить пустыми для выбранного API. "
                        + "Укажи базовый HTTPS-адрес API, если используешь свой сервер. Поле ключа видно на экране."), 360)),
                List.of(provider, input("base_url", "Базовый HTTPS-адрес API", 1024),
                        input("model", "ID модели", 200), input("key", "API-ключ", 4096)));
        return confirmation(data, id);
    }

    private static Holder<net.minecraft.server.dialog.Dialog> confirmation(CommonDialogData data, Identifier id) {
        var save = new ActionButton(new CommonButtonData(Component.translatable("gui.done"), 150),
                Optional.of(new CustomAll(id, Optional.empty())));
        return Holder.direct(new MultiActionDialog(data, List.of(save), Optional.empty(), 1));
    }

    private static Input input(String id, String label, int limit) {
        return new Input(id, new TextInput(360, text(id, label), true, "", limit, Optional.empty()));
    }

    void click(GenieService service, ServerPlayer player, Identifier id, Optional<Tag> payload) {
        var form = pending.get(player);
        if (form == null || !form.id.equals(id)) return;
        pending.remove(player);
        var server = player.level().getServer();
        if (closed || server.getTickCount() - form.opened >= EXPIRY_TICKS || !owner(player)
                || (form.form == Form.API && !privateConnection(player))) return;
        if (form.form != Form.API) {
            String input = payload.orElse(null) instanceof CompoundTag tag ? tag.getStringOr("cookies", "") : "";
            importGemini(service, server, form.form == Form.GEMINI_FILE, input, response -> {
                if (owner(player)) player.sendSystemMessage(response);
            });
            return;
        }
        if (saving) {
            message(player, "busy", "Предыдущее изменение API ещё сохраняется. Открой окно заново.", true);
            return;
        }
        GenieConfig.Api settings;
        try {
            if (!(payload.orElse(null) instanceof CompoundTag tag)) throw new IllegalArgumentException();
            settings = new GenieConfig.Api(GenieConfig.Provider.valueOf(tag.getStringOr("provider", "")),
                    tag.getStringOr("base_url", ""), tag.getStringOr("model", ""), tag.getStringOr("key", ""));
        } catch (IllegalArgumentException invalid) {
            message(player, "invalid", "Проверь адрес HTTPS, ID модели и API-ключ. Открой окно заново.", true);
            return;
        }
        saving = true;
        writer.execute(() -> {
            GenieScriptModel.Transport transport = null;
            try {
                var configured = settings.transport();
                GenieConfig.saveApi(GenieConfig.apiPath(), settings);
                transport = configured;
            } catch (IOException | RuntimeException failure) {
                SkyWars2.LOGGER.warn("Genie API configuration could not be saved");
            }
            var saved = transport;
            server.execute(() -> {
                saving = false;
                if (closed) return;
                if (saved != null) service.activateApi(saved);
                if (!owner(player)) return;
                if (saved != null) {
                    message(player, "saved", "Настройки API сохранены. Джин применит их к следующему желанию.", false);
                } else {
                    message(player, "save_failed", "Не удалось сохранить настройки API.", true);
                }
            });
        });
    }

    private boolean importGemini(GenieService service, MinecraftServer server, boolean file, String input,
                                 Consumer<Component> report) {
        if (closed) return false;
        if (saving) {
            report.accept(status("busy", "Предыдущее изменение API ещё сохраняется. Повтори импорт после завершения.", true));
            return false;
        }
        saving = true;
        report.accept(status("gemini_checking", "Проверяю сессию Gemini…", false));
        writer.execute(() -> {
            GenieGeminiWebTransport ready = null;
            GenieGeminiWebTransport.SessionIssue issue = null;
            try {
                String json = file ? readCookieFile(input) : input;
                var cookies = GenieGeminiWebTransport.Cookies.parse(json);
                var session = new GenieConfig.WebSession(cookies, java.util.UUID.randomUUID());
                var transport = GenieConfig.geminiWeb(session);
                transport.testConnection();
                GenieConfig.saveGeminiWeb(GenieConfig.apiPath(), session);
                ready = transport;
            } catch (GenieGeminiWebTransport.SessionFailure failure) {
                issue = failure.issue;
                SkyWars2.LOGGER.info("Gemini web import unavailable ({})", issue);
            } catch (InterruptedException canceled) {
                Thread.currentThread().interrupt();
            } catch (IOException | RuntimeException failure) {
                SkyWars2.LOGGER.warn("Gemini web import failed ({})", failure.getClass().getSimpleName());
            }
            var saved = ready;
            var sessionIssue = issue;
            server.execute(() -> {
                saving = false;
                if (closed) return;
                if (saved != null) {
                    service.activateApi(saved);
                    report.accept(status("gemini_saved", "Веб-чат Gemini подключён.", false));
                } else if (sessionIssue != null) {
                    report.accept(status("gemini_session_" + sessionIssue.name().toLowerCase(java.util.Locale.ROOT),
                            sessionIssue.hint, true));
                } else {
                    report.accept(status("gemini_failed", "Gemini не подключился. Проверь cookies и наличие gemini-webapi на сервере.", true));
                }
            });
        });
        return true;
    }

    private static String readCookieFile(String input) throws IOException {
        if (input.isBlank() || input.length() > 2048) throw new IllegalArgumentException("Invalid cookie file path");
        Path file = FabricLoader.getInstance().getGameDir().resolve(Path.of(input.strip())).normalize();
        if (!Files.isRegularFile(file)) throw new IOException("Missing cookie file");
        try (var stream = Files.newInputStream(file)) {
            var data = stream.readNBytes(GenieGeminiWebTransport.MAX_IMPORT + 1);
            if (data.length > GenieGeminiWebTransport.MAX_IMPORT) throw new IOException("Cookie file too large");
            return new String(data, StandardCharsets.UTF_8);
        }
    }

    private static MutableComponent text(String id, String fallback) {
        return Component.translatableWithFallback("genie.api." + id, fallback);
    }

    private static void message(ServerPlayer player, String id, String fallback, boolean error) {
        player.sendSystemMessage(status(id, fallback, error));
    }

    private static Component status(String id, String fallback, boolean error) {
        return text(id, fallback).withStyle(error ? ChatFormatting.RED : ChatFormatting.GREEN);
    }

    @Override public void close() {
        closed = true;
        pending.clear();
        writer.shutdownNow();
    }
}
