package me.skywars2.game;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** Operator convenience branch; Carpet remains responsible for creating each fake player. */
public final class BulkPlayerCommand {
    private BulkPlayerCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!FabricLoader.getInstance().isModLoaded("carpet")) return;
        SkyWarsCommandTree.add(dispatcher,Commands.literal("player")
                .then(Commands.literal("spawn").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("prefix", StringArgumentType.word())
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 1000))
                                        .executes(ctx -> spawn(dispatcher, ctx.getSource(),
                                                StringArgumentType.getString(ctx, "prefix"),
                                                IntegerArgumentType.getInteger(ctx, "count")))))));
    }

    private static int spawn(CommandDispatcher<CommandSourceStack> dispatcher, CommandSourceStack source,
                             String prefix, int count) throws CommandSyntaxException {
        // Validate the longest generated name before creating any part of the batch.
        // The restricted alphabet also keeps the delegated command a single literal name.
        if (!prefix.matches("[A-Za-z0-9_]+") || (prefix + count).length() > 16)
            throw new SimpleCommandExceptionType(Component.literal(
                    "Use letters, digits or underscores; generated player names must be at most 16 characters.")).create();
        int requested = 0, skipped = 0;
        for (int i = 1; i <= count; i++) {
            String name = prefix + i;
            if (source.getServer().getPlayerList().getPlayerByName(name) != null) {
                skipped++;
                continue;
            }
            // Keep the original source: Carpet permission/rule checks, dimension, position,
            // rotation and game mode must behave exactly like /player <name> spawn.
            requested += dispatcher.execute("player " + name + " spawn", source.withSuppressedOutput()) > 0 ? 1 : 0;
        }
        int accepted = requested, occupied = skipped;
        source.sendSuccess(() -> Component.literal("Carpet spawn requests: " + accepted
                + "; already online: " + occupied + "; rejected: " + (count - accepted - occupied) + "."), false);
        return requested;
    }
}
