package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import me.skywars2.game.OfflineAdvancementCommands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.commands.AdvancementCommands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementCommands.class)
public abstract class OfflineAdvancementCommandMixin {
    @ModifyExpressionValue(method = "register", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/commands/Commands;argument(Ljava/lang/String;Lcom/mojang/brigadier/arguments/ArgumentType;)Lcom/mojang/brigadier/builder/RequiredArgumentBuilder;"))
    private static RequiredArgumentBuilder<CommandSourceStack, ?> skywars2$suggestOfflinePlayers(
            RequiredArgumentBuilder<CommandSourceStack, ?> node) {
        if (node.getType() instanceof EntityArgument players)
            node.suggests((context, builder) -> OfflineAdvancementCommands.suggestNames(players, context, builder));
        else if (node.getName().equals("advancement"))
            node.suggests(OfflineAdvancementCommands::suggestAdvancements);
        return node;
    }

    @ModifyArg(method = "register", at = {
            @At(value = "INVOKE", target = "Lcom/mojang/brigadier/builder/RequiredArgumentBuilder;executes(Lcom/mojang/brigadier/Command;)Lcom/mojang/brigadier/builder/ArgumentBuilder;"),
            @At(value = "INVOKE", target = "Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;executes(Lcom/mojang/brigadier/Command;)Lcom/mojang/brigadier/builder/ArgumentBuilder;")
    })
    private static Command<CommandSourceStack> skywars2$handleOfflinePlayer(Command<CommandSourceStack> original) {
        return context -> OfflineAdvancementCommands.edit(context, original);
    }

    @Inject(method = "register", at = @At("TAIL"))
    private static void skywars2$query(CommandDispatcher<CommandSourceStack> dispatcher, CallbackInfo callback) {
        OfflineAdvancementCommands.registerQuery(dispatcher);
    }
}
