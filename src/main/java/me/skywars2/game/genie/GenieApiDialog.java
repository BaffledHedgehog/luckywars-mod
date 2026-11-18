package me.skywars2.game.genie;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import me.skywars2.SkyWars2;
import me.skywars2.game.SkyWarsCommandTree;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
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

    private record Pending(Identifier id, int opened) {}

    static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) ->
                SkyWarsCommandTree.add(dispatcher, Commands.literal("genie")
                        .requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
                        .then(Commands.literal("api").executes(context -> {
                            var player = context.getSource().getPlayerOrException();
                            var game = SkyWars2.game();
                            return game != null && game.items().genie().api.open(player) ? 1 : 0;
                        }))));
    }

    private static boolean owner(ServerPlayer player) {
        return player.level().getServer().getPlayerList().getPlayer(player.getUUID()) == player
                && Commands.hasPermission(Commands.LEVEL_OWNERS).test(player.createCommandSourceStack());
    }

    private static boolean privateConnection(ServerPlayer player) {
        // Offline mode sends vanilla PLAY packets unencrypted and accepts unauthenticated names.
        // Keep its credential form to local connections (including local SSH tunnel endpoints).
        return player.level().getServer().usesAuthentication()
                || player.connection.getRemoteAddress() instanceof InetSocketAddress remote
                && remote.getAddress() != null && remote.getAddress().isLoopbackAddress();
    }

    private boolean open(ServerPlayer player) {
        if (closed || !owner(player)) return false;
        if (!privateConnection(player)) {
            message(player, "private_connection", "Для ввода ключа подключись локально или через сервер с online-mode=true.", true);
            return false;
        }
        var id = Identifier.fromNamespaceAndPath("skywars2", PREFIX + UUID.randomUUID());
        pending.put(player, new Pending(id, player.level().getServer().getTickCount()));
        player.openDialog(dialog(id));
        return true;
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
                || !privateConnection(player)) return;
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

    private static MutableComponent text(String id, String fallback) {
        return Component.translatableWithFallback("genie.api." + id, fallback);
    }

    private static void message(ServerPlayer player, String id, String fallback, boolean error) {
        player.sendSystemMessage(text(id, fallback).withStyle(error ? ChatFormatting.RED : ChatFormatting.GREEN));
    }

    @Override public void close() {
        closed = true;
        pending.clear();
        writer.shutdown();
    }
}
