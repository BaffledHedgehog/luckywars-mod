package me.skywars2.game.items.loot;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.skywars2.SkyWars2;
import me.skywars2.game.GameController;
import me.skywars2.game.commands.SkyWarsCommandTree;
import me.skywars2.game.gui.loadouts.LbcSourceUnlocks;
import me.skywars2.game.match.checkpoints.MatchValueSnapshot;
import me.skywars2.game.players.PlayerStore;
import me.skywars2.game.players.SavedAdvancements;
import me.skywars2.game.players.PlayerStore.Profile;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;

/** Administration of the three source manual prizes, using their original completion flags. */
public final class LbcDropRequests {
    private LbcDropRequests() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var listing = SkyWarsCommandTree.add(dispatcher, Commands.literal("droprequests")
                .requires(me.skywars2.game.commands.OwnerCommands::allowed)
                .executes(ctx -> list(ctx.getSource())));
        dispatcher.register(Commands.literal("droprequests").requires(listing::canUse).executes(listing.getCommand()));
        var request = Commands.literal("droprequest")
                .requires(me.skywars2.game.commands.OwnerCommands::allowed);
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
        SkyWarsCommandTree.add(dispatcher, request);
    }

    public static String earnedKey(int id) {
        if (id < 1 || id > 3) throw new IllegalArgumentException("Request ID must be 1, 2 or 3");
        return "swrg:rewards/drop_" + id;
    }

    public static String fulfilledKey(int id) { return earnedKey(id) + "_requested"; }

    public static List<Integer> pending(PlayerStore.Profile profile) {
        var result = new ArrayList<Integer>();
        for (int id = 1; id <= 3; id++)
            if (profile.legacyUnlocks.contains(earnedKey(id)) && !profile.legacyUnlocks.contains(fulfilledKey(id))) result.add(id);
        return List.copyOf(result);
    }

    public static boolean validName(String name) { return name != null && name.matches("[A-Za-z0-9_]{1,16}"); }

    /** Resolve only existing canonical profiles; ambiguous historical names require correction first. */
    public static UUID resolve(Map<UUID, PlayerStore.Profile> profiles, String name) {
        if (!validName(name)) throw new IllegalArgumentException("Invalid player name");
        UUID found = null;
        for (var entry : profiles.entrySet()) if (name.equalsIgnoreCase(entry.getValue().username)) {
            if (found != null) throw new IllegalArgumentException("Ambiguous known player name: " + name);
            found = entry.getKey();
        }
        if (found == null) throw new IllegalArgumentException("Unknown player: " + name);
        return found;
    }

    private record SavedRequests(SavedAdvancements files, Map<String, AdvancementRequirements> requirements) {
        static SavedRequests of(MinecraftServer server) {
            var requirements = new LinkedHashMap<String, AdvancementRequirements>();
            for (int id = 1; id <= 3; id++) {
                for (String key : List.of(earnedKey(id), fulfilledKey(id))) {
                    var flag = server.getAdvancements().get(Identifier.parse(key));
                    if (flag != null) requirements.put(key, flag.value().requirements());
                }
            }
            return new SavedRequests(SavedAdvancements.of(server), Map.copyOf(requirements));
        }

        Set<String> read(UUID player) throws IOException {
            return read(files.read(player));
        }

        Set<String> read(SavedAdvancements.Entry saved) throws IOException {
            var flags = new HashSet<String>();
            for (var entry : requirements.entrySet())
                if (saved.progress(entry.getKey(), entry.getValue()).isDone()) flags.add(entry.getKey());
            return flags;
        }
    }

    private static boolean importSaved(PlayerStore.Profile profile, Set<String> flags) {
        boolean changed = false;
        for (int id = 1; id <= 3; id++) {
            if (flags.contains(earnedKey(id))) changed |= profile.legacyUnlocks.add(earnedKey(id));
            if (!profile.dropRequestOverrides.contains(Integer.toString(id)) && flags.contains(fulfilledKey(id)))
                changed |= profile.legacyUnlocks.add(fulfilledKey(id));
        }
        return changed;
    }

    private static void importForList(GameController game, Map<UUID, Set<String>> offline) throws IOException {
        var snapshots = new ArrayList<MatchValueSnapshot>();
        try {
            for (var entry : game.players().knownProfiles().entrySet()) {
                var player = game.server().getPlayerList().getPlayer(entry.getKey());
                var flags = offline.get(entry.getKey());
                if (player == null && flags == null) continue;
                var profile = entry.getValue();
                var before = new MatchValueSnapshot(profile);
                snapshots.add(before);
                boolean changed = player != null ? LbcSourceUnlocks.importLegacy(player, profile) : importSaved(profile, flags);
                if (!changed) snapshots.removeLast();
            }
            if (!snapshots.isEmpty()) game.players().saveAtomic();
        } catch (IOException | RuntimeException error) {
            snapshots.forEach(MatchValueSnapshot::restore);
            throw error;
        }
    }

    private static int list(CommandSourceStack source) {
        var game = SkyWars2.game();
        if (game == null) return 0;
        var server = game.server();
        var files = SavedRequests.of(server);
        var offline = game.players().knownProfiles().keySet().stream()
                .filter(uuid -> server.getPlayerList().getPlayer(uuid) == null).toList();
        CompletableFuture.supplyAsync(() -> {
            var result = new LinkedHashMap<UUID, Set<String>>();
            for (UUID uuid : offline) {
                try {
                    var flags = files.read(uuid);
                    if (!flags.isEmpty()) result.put(uuid, flags);
                } catch (IOException failure) {
                    SkyWars2.LOGGER.warn("Could not read saved drop requests for {}", uuid, failure);
                }
            }
            return result;
        }, Util.ioPool()).whenCompleteAsync((flags, failure) -> {
            if (SkyWars2.game() != game) return;
            if (failure != null) {
                source.sendFailure(Component.literal("Could not read saved manual prizes: " + failure.getMessage()));
                return;
            }
            showList(source, game, flags);
        }, server);
        return 1;
    }

    private static void showList(CommandSourceStack source, GameController game, Map<UUID, Set<String>> offline) {
        try {
            importForList(game, offline);
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
        } catch (IOException | RuntimeException error) {
            source.sendFailure(Component.literal("Could not list manual prizes: " + error.getMessage()));
        }
    }

    private static int mark(CommandSourceStack source, String name, int id, boolean made) {
        var game = SkyWars2.game();
        if (game == null) return 0;
        try {
            UUID uuid = resolve(game.players().knownProfiles(), name);
            var profile = game.players().knownProfiles().get(uuid);
            var player = game.server().getPlayerList().getPlayer(uuid);
            String key = fulfilledKey(id);
            String overrideId = Integer.toString(id);
            var before = new MatchValueSnapshot(profile);
            boolean updated;
            try {
                var files = player == null ? SavedRequests.of(game.server()) : null;
                var saved = files == null ? null : files.files().read(uuid);
                boolean imported = saved == null ? LbcSourceUnlocks.importLegacy(player, profile)
                        : importSaved(profile, files.read(saved));
                if (!profile.legacyUnlocks.contains(earnedKey(id))) throw new IllegalArgumentException("Player has not earned drop_" + id);
                updated = profile.legacyUnlocks.contains(key) != made || !profile.dropRequestOverrides.contains(overrideId);
                if (made) profile.legacyUnlocks.add(key); else profile.legacyUnlocks.remove(key);
                profile.dropRequestOverrides.add(overrideId);
                boolean profileChanged = imported || updated;
                var flag = game.server().getAdvancements().get(Identifier.parse(key));
                if (saved != null && flag != null) {
                    var progress = saved.progress(key, flag.value().requirements());
                    boolean projected = false;
                    for (String criterion : flag.value().criteria().keySet())
                        projected |= made ? progress.grantProgress(criterion) : progress.revokeProgress(criterion);
                    if (projected) {
                        saved.put(key, progress);
                        saved.save(game.players(), profileChanged);
                        profileChanged = false;
                    }
                }
                if (profileChanged) game.players().saveAtomic();
            } catch (IOException | RuntimeException error) {
                before.restore();
                throw error;
            }
            if (player != null) LbcSourceUnlocks.publishDropRequest(player, id, made);
            source.sendSuccess(() -> Component.literal(name + " drop_" + id + (updated ? ": " : " is already ")
                    + (made ? "fulfilled" : "unfulfilled")), updated);
            return updated ? 1 : 0;
        } catch (IOException | RuntimeException error) {
            source.sendFailure(Component.literal("Could not update manual prize: " + error.getMessage()));
            return 0;
        }
    }
}
