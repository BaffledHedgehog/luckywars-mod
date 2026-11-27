package me.skywars2.game.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import me.skywars2.SkyWars2;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import me.skywars2.game.players.BulkPlayerProfiles;

/** Operator convenience branch; Carpet remains responsible for creating each fake player. */
public final class BulkPlayerCommand {
    private static final int SPAWNS_PER_TICK = 8;
    private static final Map<MinecraftServer, ArrayDeque<Batch>> PENDING = new IdentityHashMap<>();

    static {
        ServerTickEvents.END_SERVER_TICK.register(BulkPlayerCommand::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            var pending = PENDING.remove(server);
            if (pending == null) return;
            var batch = pending.getFirst();
            if (batch.lookup != null) batch.lookup.cancel(false);
        });
    }

    private BulkPlayerCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!FabricLoader.getInstance().isModLoaded("carpet")) return;
        var node=SkyWarsCommandTree.add(dispatcher,Commands.literal("player")
                .then(Commands.literal("spawn").requires(source -> OwnerCommands.allowed(source) && Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source))
                        .then(Commands.argument("prefix", StringArgumentType.word())
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 1000))
                                        .executes(ctx -> spawn(dispatcher, ctx.getSource(),
                                                StringArgumentType.getString(ctx, "prefix"),
                                                IntegerArgumentType.getInteger(ctx, "count")))))));
        dispatcher.register(Commands.literal("player"));
        dispatcher.getRoot().getChild("player").addChild(node.getChild("spawn"));
    }

    private static int spawn(CommandDispatcher<CommandSourceStack> dispatcher, CommandSourceStack source,
                             String prefix, int count) throws CommandSyntaxException {
        // Validate the longest generated name before creating any part of the batch.
        // The restricted alphabet also keeps the delegated command a single literal name.
        if (!prefix.matches("[A-Za-z0-9_]+") || (prefix + count).length() > 16)
            throw new SimpleCommandExceptionType(Component.literal(
                    "Use letters, digits or underscores; generated player names must be at most 16 characters.")).create();
        var names = new ArrayList<String>(count);
        for (int i = 1; i <= count; i++) {
            String name = prefix + i;
            if (source.getServer().getPlayerList().getPlayerByName(name) == null) names.add(name);
        }
        var batch = new Batch(dispatcher, source, names, count - names.size());
        if (names.isEmpty()) batch.finish();
        else {
            PENDING.computeIfAbsent(source.getServer(), unused -> new ArrayDeque<>()).addLast(batch);
            source.sendSuccess(() -> Component.literal("Queued " + names.size() + " Carpet player spawn requests."), false);
        }
        return names.size();
    }

    private static void tick(MinecraftServer server) {
        var queue = PENDING.get(server);
        if (queue == null) return;
        var batch = queue.getFirst();
        if (!batch.tick(server)) return;
        queue.removeFirst();
        if (queue.isEmpty()) PENDING.remove(server);
        batch.finish();
    }

    private static final class Batch {
        final CommandDispatcher<CommandSourceStack> dispatcher;
        final CommandSourceStack source;
        final List<String> names;
        final int total;
        int next, accepted, occupied;
        CompletableFuture<BulkPlayerProfiles.Lookup> lookup;

        Batch(CommandDispatcher<CommandSourceStack> dispatcher, CommandSourceStack source, List<String> names, int occupied) {
            this.dispatcher = dispatcher;
            this.source = source;
            this.names = names;
            this.occupied = occupied;
            this.total = names.size() + occupied;
        }

        boolean tick(MinecraftServer server) {
            for (int work = 0; work < SPAWNS_PER_TICK && next < names.size(); work++) {
                String name = names.get(next);
                if (server.getPlayerList().getPlayerByName(name) != null) {
                    if (lookup != null) lookup.cancel(false);
                    lookup = null;
                    occupied++;
                    next++;
                    continue;
                }
                var prepared = BulkPlayerProfiles.cached(server, name);
                if (prepared == null) {
                    if (lookup == null) {
                        var repository = server.services().profileRepository();
                        boolean useOnlineLookup = !server.isSingleplayer() && server.usesAuthentication();
                        lookup = CompletableFuture.supplyAsync(
                                () -> BulkPlayerProfiles.fetch(repository, name, useOnlineLookup), Util.ioPool());
                    }
                    if (!lookup.isDone()) return false;
                    try {
                        prepared = lookup.join();
                    } catch (CompletionException failure) {
                        SkyWars2.LOGGER.warn("Could not prepare a Carpet player profile ({})", failure.getCause().getClass().getSimpleName());
                    }
                }
                lookup = null;
                next++;
                if (prepared == null) continue;
                try {
                    if (ScopedValue.where(BulkPlayerProfiles.CURRENT, prepared).call(
                            () -> dispatcher.execute("player " + name + " spawn", source.withSuppressedOutput())) > 0) accepted++;
                } catch (CommandSyntaxException rejected) {
                    // The delegated Carpet command retains its rule and permission checks.
                }
            }
            return next == names.size();
        }

        void finish() {
            int rejected = total - accepted - occupied;
            source.sendSuccess(() -> Component.literal("Carpet spawn requests: " + accepted
                    + "; already online: " + occupied + "; rejected: " + rejected + "."), false);
        }
    }
}
