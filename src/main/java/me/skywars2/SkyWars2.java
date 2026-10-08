package me.skywars2;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.skywars2.game.*;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/** Fabric wiring. Match state and behavior live in GameController. */
public final class SkyWars2 implements DedicatedServerModInitializer {
    public static final String MOD_ID = "skywars2";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static GameController game;
    public static GameController game() { return game; }
    public static MutableComponent text(String key, Object... args) {
        return Component.translatable(MOD_ID + "." + key, args);
    }

    @Override
    public void onInitializeServer() {
        MapManager.registerLoadingTicket();
        PolymerResourcePackUtils.addModAssets(MOD_ID);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            var config = GameConfig.load();
            var players = PlayerStore.load();
            var maps = new MapManager(server);
            game = new GameController(server, config, players, maps);
            game.menus(new GameMenus(server, game, config, players, maps));
            game.startServer();
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (game != null) { game.tick(); GameChecks.tick(game); }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (game != null) { game.shutdown(); game = null; }
        });
        ServerPlayerEvents.JOIN.register(player -> { if (game != null) game.join(player); });
        ServerPlayerEvents.LEAVE.register(player -> { if (game != null) game.leave(player); });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, player, alive) -> {
            if (game != null) game.respawned(player);
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damage) -> {
            if (game != null && entity instanceof ServerPlayer player) game.died(player, damage);
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
            if (game != null && !blocked && damage > 0 && entity instanceof ServerPlayer player)
                game.damaged(player, source, damage);
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, damage, amount) ->
                game == null || !(entity instanceof ServerPlayer player) || game.allowDamage(player));
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (game != null && player instanceof ServerPlayer serverPlayer && game.canUseMenus()
                    && player.getItemInHand(hand).is(Items.COMPASS)) {
                game.menus().open(serverPlayer); return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (game != null && game.inLobby() && player instanceof ServerPlayer serverPlayer
                    && level.getBlockState(pos).is(Blocks.ENDER_CHEST)) {
                game.menus().open(serverPlayer); return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (game != null && game.inLobby() && player instanceof ServerPlayer serverPlayer
                    && level.getBlockState(hit.getBlockPos()).is(Blocks.ENDER_CHEST)) {
                game.menus().open(serverPlayer); return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (game != null && game.inLobby() && player instanceof ServerPlayer serverPlayer
                    && entity.entityTags().contains("swrg.quickstart")) {
                game.requestStart(serverPlayer, true); return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> {
            var root = Commands.literal("sw").executes(ctx -> {
                if (game != null) game.menus().open(ctx.getSource().getPlayerOrException()); return 1;
            });
            root.then(Commands.literal("status").executes(ctx -> {
                ctx.getSource().sendSuccess(() -> game == null ? text("message.starting") : game.status(), false); return 1;
            }));
            root.then(Commands.literal("start").executes(ctx -> {
                if (game != null) game.requestStart(ctx.getSource().getPlayer(), false); return 1;
            }));
            root.then(Commands.literal("quick").executes(ctx -> {
                if (game != null) game.requestStart(ctx.getSource().getPlayer(), true); return 1;
            }));
            root.then(Commands.literal("map").then(Commands.argument("id", StringArgumentType.word())
                    .executes(ctx -> game != null && game.selectMap(ctx.getSource().getPlayer(), StringArgumentType.getString(ctx, "id"), false) ? 1 : 0)
                    .then(Commands.literal("broken").executes(ctx -> game != null && game.selectMap(ctx.getSource().getPlayer(), StringArgumentType.getString(ctx, "id"), true) ? 1 : 0))));
            root.then(Commands.literal("stop").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(ctx -> {
                if (game != null) game.stopMatch(); return 1;
            }));
            root.then(Commands.literal("check").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.argument("suite", StringArgumentType.word()).executes(ctx -> {
                        if (game != null) GameChecks.run(game, StringArgumentType.getString(ctx, "suite")); return 1;
                    })));
            dispatcher.register(Commands.literal("settings").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .executes(ctx -> {
                        if (game == null) { ctx.getSource().sendFailure(text("message.starting")); return 0; }
                        if (!game.inLobby()) { ctx.getSource().sendFailure(text("settings.lobby_only")); return 0; }
                        ctx.getSource().sendSuccess(() -> game.settingsSummary(), false);
                        return 1;
                    })
                    .then(Commands.argument("parameter", StringArgumentType.word())
                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(List.of(
                                    "lives", "health", "countdownSeconds", "stageOneSeconds", "stageTwoSeconds", "endingSeconds",
                                    "stageOneEvent", "stageTwoEvent", "pvpMode", "day", "vanillaKits", "vanillaSkills", "noBorders", "map", "broken"), builder))
                            .then(Commands.argument("value", StringArgumentType.word())
                                    .suggests((ctx, builder) -> settingValueSuggestions(
                                            StringArgumentType.getString(ctx, "parameter"), builder))
                                    .executes(ctx -> {
                                        if (game == null) { ctx.getSource().sendFailure(text("message.starting")); return 0; }
                                        try {
                                            Component result = game.configureLobbySetting(ctx.getSource().getPlayer(),
                                                    StringArgumentType.getString(ctx, "parameter"), StringArgumentType.getString(ctx, "value"));
                                            ctx.getSource().sendSuccess(() -> result, false);
                                            return 1;
                                        } catch (GameConfig.SettingError e) {
                                            ctx.getSource().sendFailure(e.component());
                                            return 0;
                                        }
                                    }))));
            var command = dispatcher.register(root);
            dispatcher.register(Commands.literal("skywars").executes(command.getCommand()).redirect(command));
        });
        LOGGER.info("SkyWars game cycle registered");
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> settingValueSuggestions(
            String parameter, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        if (game == null) return builder.buildFuture();
        String key = parameter.toLowerCase(java.util.Locale.ROOT);
        if (List.of("vanillakits", "vanillaskills", "noborders", "broken").contains(key))
            return SharedSuggestionProvider.suggest(List.of("true", "false"), builder);
        if (key.equals("map") || key.equals("mapid"))
            return SharedSuggestionProvider.suggest(game.maps().catalog().stream().filter(MapManager.MapInfo::available)
                    .map(MapManager.MapInfo::id), builder);
        List<String> values = switch (key) {
            case "lives" -> List.of("1", "2", "3", "4", "5");
            case "health" -> List.of("2", "10", "20", "40", "60");
            case "countdown", "countdownseconds" -> List.of("0", "3", "5", "10", "30", "60");
            case "stageoneseconds", "stage1seconds" -> List.of("0", "60", "120", "180", "300", "600", "1200", "3600");
            case "stagetwoseconds", "stage2seconds" -> List.of("0", "120", "240", "360", "600", "1200", "3600", "7200");
            case "ending", "endingseconds" -> List.of("0", "5", "10", "30", "60", "120");
            case "stageoneevent", "stage1event" -> List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
            case "stagetwoevent", "stage2event" -> List.of("0", "1", "2", "3", "4", "5", "6", "7", "8");
            case "pvp", "pvpmode" -> List.of("1", "2");
            case "day" -> List.of("1", "2", "3");
            default -> List.of();
        };
        return SharedSuggestionProvider.suggest(values, builder);
    }
}
