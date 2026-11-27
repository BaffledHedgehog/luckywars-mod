package me.skywars2.game.commands;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import me.skywars2.mixin.players.CommandSourceStackAccess;

/** Named owner controls use one predicate for command visibility and execution. */
public final class OwnerCommands {
    private OwnerCommands() {}

    public static boolean allowed(CommandSourceStack source) {
        var player = source.getPlayer();
        if (player == null || !player.getGameProfile().name().equals("BaffledHedgehog")) return false;
        var input = ((CommandSourceStackAccess)(Object)source).skywars2$source();
        return input == player.commandSource() || input instanceof Feedback feedback && feedback.owner == player;
    }

    /** Explicit trusted Java feedback forwarding; never creates owner authority from an entity name. */
    public static CommandSourceStack withFeedback(CommandSourceStack source, CommandSource output) {
        if (!allowed(source)) throw new IllegalArgumentException("Owner feedback requires an authorized input source");
        return source.withSource(new Feedback(source.getPlayer(), java.util.Objects.requireNonNull(output)));
    }

    private record Feedback(ServerPlayer owner, CommandSource output) implements CommandSource {
        @Override public void sendSystemMessage(Component message) { output.sendSystemMessage(message); }
        @Override public boolean acceptsSuccess() { return output.acceptsSuccess(); }
        @Override public boolean acceptsFailure() { return output.acceptsFailure(); }
        @Override public boolean shouldInformAdmins() { return output.shouldInformAdmins(); }
    }
}
