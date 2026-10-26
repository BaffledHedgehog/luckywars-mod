package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.skywars2.SkyWars2;
import net.minecraft.commands.CommandSourceStack;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import java.util.Collection;
import java.util.List;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.GameModeCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** A deliberate authorized command can restore participation; ordinary setGameMode cannot. */
@Mixin(GameModeCommand.class)
public abstract class OperatorGameModeMixin {
    @WrapOperation(method = "setMode",
            at = @At(value = "INVOKE", target =
                    "Lnet/minecraft/server/commands/GameModeCommand;setGameMode(Lnet/minecraft/commands/CommandSourceStack;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/GameType;)Z"))
    private static boolean skywars2$returnToMatch(CommandSourceStack source, ServerPlayer player, GameType type,
                                                 Operation<Boolean> original) {
        var game = SkyWars2.game();
        boolean restored = source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
                && game != null && game.returnFromGameModeCommand(player, type);
        boolean changed = original.call(source, player, type);
        if (restored && !changed) {
            var mode = Component.translatable("gameMode." + type.getName());
            if (source.getEntity() == player)
                source.sendSuccess(() -> Component.translatable("commands.gamemode.success.self", mode), true);
            else {
                if (source.getLevel().getGameRules().get(GameRules.SEND_COMMAND_FEEDBACK))
                    player.sendSystemMessage(Component.translatable("gameMode.changed", mode));
                source.sendSuccess(() -> Component.translatable("commands.gamemode.success.other", player.getDisplayName(), mode), true);
            }
        }
        return changed || restored;
    }

    /** Literal aliases also parse correctly on an unmodified vanilla client. */
    @Inject(method = "register", at = @At("TAIL"))
    private static void skywars2$numericAliases(CommandDispatcher<CommandSourceStack> dispatcher, CallbackInfo ci) {
        var root = Commands.literal("gamemode").requires(Commands.hasPermission(GameModeCommand.PERMISSION_CHECK));
        var modes = new GameType[]{GameType.SURVIVAL, GameType.CREATIVE, GameType.ADVENTURE, GameType.SPECTATOR};
        for (int index = 0; index < modes.length; index++) {
            GameType mode = modes[index];
            root.then(Commands.literal(Integer.toString(index))
                    .executes(context -> CommandAccess.skywars2$setMode(context,
                            List.of(context.getSource().getPlayerOrException()), mode))
                    .then(Commands.argument("target", EntityArgument.players())
                            .executes(context -> CommandAccess.skywars2$setMode(context,
                                    EntityArgument.getPlayers(context, "target"), mode))));
        }
        dispatcher.register(root);
    }

    @Mixin(GameModeCommand.class)
    public interface CommandAccess {
        @Invoker("setMode")
        static int skywars2$setMode(CommandContext<CommandSourceStack> context,
                                    Collection<ServerPlayer> players, GameType mode) {
            throw new AssertionError("Mixin invoker");
        }
    }
}
