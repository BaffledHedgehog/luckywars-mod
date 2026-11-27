package me.skywars2.game;

import java.util.List;
import java.util.UUID;
import me.skywars2.game.commands.OwnerCommands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;

/** Real Brigadier admission, with a packet-free named fixture that never enters the player list. */
public final class OwnerCommandChecks {
    private OwnerCommandChecks() {}
    public static CommandSourceStack ownerSource(MinecraftServer server) {
        var owner = new ServerPlayer(server, server.overworld(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "BaffledHedgehog"),
                net.minecraft.server.level.ClientInformation.createDefault());
        var input = owner.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS);
        // The detached fixture has no packet listener: preserve its real input identity and forward feedback explicitly.
        return OwnerCommands.withFeedback(input, server);
    }
    private static void require(boolean ok, String why) { if (!ok) throw new IllegalStateException("Owner commands: " + why); }
    public static void run(GameController game, ServerPlayer other) {
        var dispatcher = game.server().getCommands().getDispatcher();
        var owner = ownerSource(game.server());
        var op = other.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS);
        var console = game.server().createCommandSourceStack();
        var substitutedOp = op.withEntity(owner.getEntity());
        var substitutedConsole = console.withEntity(owner.getEntity());
        require(OwnerCommands.allowed(owner) && !OwnerCommands.allowed(op) && !OwnerCommands.allowed(console),
                "exact named player only, regardless of operator permission or console");
        require(OwnerCommands.allowed(owner.withSource(owner.getPlayer().commandSource())), "native direct owner input has matching original source identity");
        require(!OwnerCommands.allowed(substitutedOp) && !OwnerCommands.allowed(substitutedConsole),
                "native execute-as entity substitution retains original untrusted operator/console input");
        var root = dispatcher.getRoot().getChild("skywars");
        for (var path : List.of(List.of("droprequests"), List.of("droprequest"), List.of("settings", "event"),
                List.of("genie", "gemini"), List.of("genie", "chatgpt"), List.of("tiktok", "import"), List.of("tiktok", "importfile"))) {
            var node = root;
            for (String part : path) node = java.util.Objects.requireNonNull(node.getChild(part), "registered " + path);
            require(node.canUse(owner) && !node.canUse(op) && !node.canUse(console)
                    && !node.canUse(substitutedOp) && !node.canUse(substitutedConsole), "command visibility/execution admission " + path);
        }
        require(!dispatcher.getRoot().getChild("droprequests").canUse(op), "standalone drop listing alias has same owner gate");
        var visible = dispatcher.getSmartUsage(root, op).values();
        require(visible.stream().noneMatch(s -> s.startsWith("droprequest")), "other operator's command suggestions omit drop controls");
        for (var spoofed : List.of(substitutedOp, substitutedConsole)) {
            boolean denied = false;
            try { dispatcher.execute("skywars droprequests", spoofed); }
            catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) { denied = true; }
            require(denied, "actual dispatcher refuses owner commands after execute-as substitution");
            require(dispatcher.getSmartUsage(root, spoofed).values().stream().noneMatch(s -> s.startsWith("droprequest")),
                    "substituted input does not gain owner-only suggestions");
            boolean feedbackDenied = false;
            try { OwnerCommands.withFeedback(spoofed, game.server()); }
            catch (IllegalArgumentException expected) { feedbackDenied = true; }
            require(feedbackDenied, "trusted feedback factory cannot launder an execute-as input");
        }
    }
}
