package me.skywars2.game;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.skywars2.SkyWars2;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Administration of the three source manual prizes, using their original completion flags. */
public final class LbcDropRequests {
    private LbcDropRequests() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("skywars");
        root.then(Commands.literal("droprequests")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> list(ctx.getSource())));
        var request = Commands.literal("droprequest")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        for (boolean made : new boolean[]{true, false}) {
            request.then(Commands.literal(made ? "made" : "unmade")
                    .then(Commands.argument("name", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                var game = SkyWars2.game();
                                return SharedSuggestionProvider.suggest(game == null ? List.<String>of()
                                        : game.players().knownProfiles().values().stream().map(p -> p.username)
                                                .filter(LbcDropRequests::validName).distinct().sorted().toList(), builder);
                            })
                            .then(Commands.argument("id", IntegerArgumentType.integer(1, 3))
                                    .executes(ctx -> mark(ctx.getSource(), StringArgumentType.getString(ctx, "name"),
                                            IntegerArgumentType.getInteger(ctx, "id"), made)))));
        }
        dispatcher.register(root.then(request));
    }

    static String earnedKey(int id) {
        if (id < 1 || id > 3) throw new IllegalArgumentException("Request ID must be 1, 2 or 3");
        return "swrg:rewards/drop_" + id;
    }

    static String fulfilledKey(int id) { return earnedKey(id) + "_requested"; }

    static List<Integer> pending(PlayerStore.Profile profile) {
        var result = new ArrayList<Integer>();
        for (int id = 1; id <= 3; id++)
            if (profile.legacyUnlocks.contains(earnedKey(id)) && !profile.legacyUnlocks.contains(fulfilledKey(id))) result.add(id);
        return List.copyOf(result);
    }

    private static boolean validName(String name) { return name != null && name.matches("[A-Za-z0-9_]{1,16}"); }

    /** Resolve only existing canonical profiles; ambiguous historical names require correction first. */
    static UUID resolve(Map<UUID, PlayerStore.Profile> profiles, String name) {
        if (!validName(name)) throw new IllegalArgumentException("Invalid player name");
        UUID found = null;
        for (var entry : profiles.entrySet()) if (name.equalsIgnoreCase(entry.getValue().username)) {
            if (found != null) throw new IllegalArgumentException("Ambiguous known player name: " + name);
            found = entry.getKey();
        }
        if (found == null) throw new IllegalArgumentException("Unknown player: " + name);
        return found;
    }

    private static void importOnline(GameController game) throws IOException {
        var snapshots = new ArrayList<MatchValueSnapshot>();
        boolean changed = false;
        try {
            for (ServerPlayer player : game.server().getPlayerList().getPlayers()) {
                var profile = game.players().knownProfiles().get(player.getUUID());
                if (profile == null) continue;
                snapshots.add(new MatchValueSnapshot(profile));
                changed |= LbcSourceUnlocks.importLegacy(player, profile);
            }
            if (changed) game.players().saveAtomic();
        } catch (IOException | RuntimeException error) {
            snapshots.forEach(MatchValueSnapshot::restore);
            throw error;
        }
    }

    private static int list(CommandSourceStack source) {
        var game = SkyWars2.game();
        if (game == null) return 0;
        try {
            importOnline(game);
            int count = 0;
            for (var entry : game.players().knownProfiles().entrySet()) {
                var profile = entry.getValue();
                for (int id : pending(profile)) {
                    String label = validName(profile.username) ? profile.username : entry.getKey().toString();
                    String line = label + " [" + entry.getKey() + "] drop_" + id + " — earned, unfulfilled";
                    source.sendSuccess(() -> Component.literal(line), false);
                    count++;
                }
            }
            int total = count;
            source.sendSuccess(() -> Component.literal("Unfulfilled manual prizes: " + total), false);
            return count;
        } catch (IOException | RuntimeException error) {
            source.sendFailure(Component.literal("Could not list manual prizes: " + error.getMessage()));
            return 0;
        }
    }

    private static int mark(CommandSourceStack source, String name, int id, boolean made) {
        var game = SkyWars2.game();
        if (game == null) return 0;
        try {
            UUID uuid = resolve(game.players().knownProfiles(), name);
            importOnline(game);
            var profile = game.players().knownProfiles().get(uuid);
            if (!profile.legacyUnlocks.contains(earnedKey(id))) throw new IllegalArgumentException("Player has not earned drop_" + id);
            String key = fulfilledKey(id);
            boolean previous = profile.legacyUnlocks.contains(key);
            String overrideId = Integer.toString(id);
            boolean override = profile.dropRequestOverrides.contains(overrideId);
            if (previous == made && override) {
                source.sendSuccess(() -> Component.literal(name + " drop_" + id + " is already " + (made ? "fulfilled" : "unfulfilled")), false);
                return 0;
            }
            if (made) profile.legacyUnlocks.add(key); else profile.legacyUnlocks.remove(key);
            profile.dropRequestOverrides.add(overrideId);
            try { game.players().saveAtomic(); }
            catch (IOException | RuntimeException error) {
                if (previous) profile.legacyUnlocks.add(key); else profile.legacyUnlocks.remove(key);
                if (!override) profile.dropRequestOverrides.remove(overrideId);
                throw error;
            }
            // Revoke the native completion projection as well, or the next import would undo unmade.
            var player = game.server().getPlayerList().getPlayer(uuid);
            if (player != null) {
                var flag = game.server().getAdvancements().get(Identifier.parse(key));
                if (flag != null) for (String criterion : flag.value().criteria().keySet()) {
                    if (made) player.getAdvancements().award(flag, criterion);
                    else player.getAdvancements().revoke(flag, criterion);
                }
            }
            source.sendSuccess(() -> Component.literal(name + " drop_" + id + ": " + (made ? "fulfilled" : "unfulfilled")), true);
            return 1;
        } catch (IOException | RuntimeException error) {
            source.sendFailure(Component.literal("Could not update manual prize: " + error.getMessage()));
            return 0;
        }
    }
}
