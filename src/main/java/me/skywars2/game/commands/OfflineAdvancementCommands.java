package me.skywars2.game.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.skywars2.SkyWars2;
import me.skywars2.game.match.checkpoints.MatchValueSnapshot;
import me.skywars2.game.players.PlayerStore;
import me.skywars2.game.players.PlayerStore.Profile;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import me.skywars2.game.GameController;
import me.skywars2.game.gui.loadouts.LbcSourceUnlocks;
import me.skywars2.game.items.loot.LbcDropRequests;
import me.skywars2.game.players.BulkPlayerProfiles;
import me.skywars2.game.players.SavedAdvancements;

public final class OfflineAdvancementCommands {
    private static final DynamicCommandExceptionType AMBIGUOUS_NAME =
            new DynamicCommandExceptionType(name -> Component.literal("Ambiguous known player name: " + name));

    private OfflineAdvancementCommands() {}

    public static void registerQuery(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.getRoot().getChild("advancement").addChild(Commands.literal("query")
                .then(Commands.argument("player", EntityArgument.player())
                        .suggests((context, builder) -> suggestNames(EntityArgument.player(), context, builder))
                        .then(Commands.argument("advancement", ResourceKeyArgument.key(Registries.ADVANCEMENT))
                                .suggests(OfflineAdvancementCommands::suggestAdvancements)
                                .executes(OfflineAdvancementCommands::query))).build());
    }

    public static CompletableFuture<Suggestions> suggestNames(EntityArgument vanilla,
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        var game = SkyWars2.game();
        if (game == null || builder.getRemaining().startsWith("@")) return vanilla.listSuggestions(context, builder);
        var names = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        for (var profile : game.players().knownProfiles().values())
            if (LbcDropRequests.validName(profile.username)) names.add(profile.username);
        var offline = SharedSuggestionProvider.suggest(names, builder.createOffset(builder.getStart()));
        return vanilla.listSuggestions(context, builder)
                .thenCombine(offline, (a, b) -> Suggestions.merge(builder.getInput(), List.of(a, b)));
    }

    public static CompletableFuture<Suggestions> suggestAdvancements(CommandContext<CommandSourceStack> context,
                                                                    SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(context.getSource().getServer().getAdvancements()
                .getAllAdvancements().stream().map(AdvancementHolder::id), builder);
    }

    private record Target(UUID id, String name, PlayerStore.Profile profile) {}

    private static Target offlineTarget(CommandContext<CommandSourceStack> context, String argument) throws CommandSyntaxException {
        var token = context.getNodes().stream().filter(node -> node.getNode().getName().equals(argument)).findFirst().orElseThrow();
        String name = new StringReader(token.getRange().get(context.getInput())).readString();
        var server = context.getSource().getServer();
        if (!LbcDropRequests.validName(name) || server.getPlayerList().getPlayerByName(name) != null) return null;
        var game = SkyWars2.game();
        if (game == null || game.server() != server) return null;
        var known = BulkPlayerProfiles.cached(server, name);
        UUID id = known == null ? null : known.fallbackId();
        var matched = game.players().knownProfiles().entrySet().stream()
                .filter(entry -> name.equalsIgnoreCase(entry.getValue().username)).toList();
        if (matched.size() > 1) throw AMBIGUOUS_NAME.create(name);
        if (!matched.isEmpty()) {
            var entry = matched.getFirst();
            if (server.getPlayerList().getPlayer(entry.getKey()) != null) return null;
            return new Target(entry.getKey(), entry.getValue().username, entry.getValue());
        }
        return id == null || server.getPlayerList().getPlayer(id) != null ? null
                : new Target(id, known.fallbackName(), game.players().knownProfiles().get(id));
    }

    public static int edit(CommandContext<CommandSourceStack> context, Command<CommandSourceStack> vanilla) throws CommandSyntaxException {
        var target = offlineTarget(context, "targets");
        if (target == null) return vanilla.run(context);
        var source = context.getSource();
        boolean grant = hasNode(context, "grant");
        String criterion = hasNode(context, "criterion") ? context.getArgument("criterion", String.class) : null;
        var manager = source.getServer().getAdvancements();
        Collection<AdvancementHolder> selected;
        if (hasNode(context, "everything")) selected = manager.getAllAdvancements();
        else {
            var first = ResourceKeyArgument.getAdvancement(context, "advancement");
            if (criterion != null && !first.value().criteria().containsKey(criterion)) {
                source.sendFailure(Component.literal("Unknown criterion " + criterion + " for " + first.id()));
                return 0;
            }
            var node = manager.tree().get(first);
            var branch = new ArrayList<AdvancementHolder>();
            if (node != null && (hasNode(context, "until") || hasNode(context, "through")))
                for (var parent = node.parent(); parent != null; parent = parent.parent()) branch.add(parent.holder());
            branch.add(first);
            if (node != null && (hasNode(context, "from") || hasNode(context, "through"))) addChildren(node, branch);
            selected = branch;
        }

        try {
            var saved = SavedAdvancements.of(source.getServer()).read(target.id());
            var profile = target.profile();
            var prior = profile == null ? null : new MatchValueSnapshot(profile);
            boolean profileChanged = false;
            int count = 0;
            try {
                for (var flag : selected) {
                    String key = flag.id().toString();
                    var progress = effective(saved, flag, profile);
                    if (grant && criterion == null && progress.isDone()) continue;
                    boolean changed = false;
                    for (String name : criterion == null ? flag.value().criteria().keySet() : List.of(criterion))
                        changed |= grant ? progress.grantProgress(name) : progress.revokeProgress(name);
                    if (!changed) continue;
                    saved.put(key, progress);
                    profileChanged |= syncProfile(profile, key, progress.isDone());
                    count++;
                }
                if (count == 0) {
                    source.sendFailure(Component.literal("No advancement progress changed for " + target.name()));
                    return 0;
                }
                saved.save(SkyWars2.game().players(), profileChanged);
            } catch (IOException | RuntimeException failure) {
                if (prior != null) prior.restore();
                throw failure;
            }
            int total = count;
            source.sendSuccess(() -> Component.literal((grant ? "Granted " : "Revoked ") + total
                    + " advancement(s) for " + target.name()), true);
            return count;
        } catch (IOException | RuntimeException failure) {
            source.sendFailure(Component.literal("Could not edit " + target.name() + "'s advancements: " + failure.getMessage()));
            SkyWars2.LOGGER.warn("Could not edit saved advancements for {}", target.id(), failure);
            return 0;
        }
    }

    private static int query(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var source = context.getSource();
        var flag = ResourceKeyArgument.getAdvancement(context, "advancement");
        var offline = offlineTarget(context, "player");
        String name;
        boolean done;
        if (offline == null) {
            var player = EntityArgument.getPlayer(context, "player");
            name = player.getScoreboardName();
            done = player.getAdvancements().getOrStartProgress(flag).isDone();
        } else {
            name = offline.name();
            try {
                done = effective(SavedAdvancements.of(source.getServer()).read(offline.id()), flag, offline.profile()).isDone();
            } catch (IOException failure) {
                source.sendFailure(Component.literal("Could not read " + name + "'s advancements: " + failure.getMessage()));
                return 0;
            }
        }
        source.sendSuccess(() -> Component.literal(name + (done ? " has " : " does not have ") + flag.id()), false);
        return done ? 1 : 0;
    }

    private static boolean hasNode(CommandContext<CommandSourceStack> context, String name) {
        return context.getNodes().stream().anyMatch(node -> node.getNode().getName().equals(name));
    }

    private static void addChildren(AdvancementNode node, List<AdvancementHolder> result) {
        for (var child : node.children()) {
            result.add(child.holder());
            addChildren(child, result);
        }
    }

    private static AdvancementProgress effective(SavedAdvancements.Entry saved, AdvancementHolder flag,
                                                   PlayerStore.Profile profile) throws IOException {
        String key = flag.id().toString();
        var progress = saved.progress(key, flag.value().requirements());
        if (profile == null || !LbcSourceUnlocks.caseAdvancement(flag.id())) return progress;
        boolean granted = profile.legacyUnlocks.contains(key);
        boolean revoked = profile.revokedLegacyUnlocks.contains(key);
        for (int id = 1; id <= 3; id++)
            if (key.equals(LbcDropRequests.fulfilledKey(id)) && profile.dropRequestOverrides.contains(Integer.toString(id)))
                revoked = !granted;
        for (String criterion : flag.value().criteria().keySet()) {
            if (revoked) progress.revokeProgress(criterion);
            else if (granted) progress.grantProgress(criterion);
        }
        return progress;
    }

    private static boolean syncProfile(PlayerStore.Profile profile, String key, boolean done) {
        if (profile == null || !LbcSourceUnlocks.valid(key)) return false;
        boolean changed = done ? profile.legacyUnlocks.add(key) : profile.legacyUnlocks.remove(key);
        if (done) changed |= profile.revokedLegacyUnlocks.remove(key);
        for (int id = 1; id <= 3; id++)
            if (key.equals(LbcDropRequests.fulfilledKey(id))) changed |= profile.dropRequestOverrides.add(Integer.toString(id));
        return changed;
    }
}
