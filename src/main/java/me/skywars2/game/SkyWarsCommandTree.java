package me.skywars2.game;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/** Registers a real child, with its own permission/arguments, only under /skywars. */
public final class SkyWarsCommandTree {
    private SkyWarsCommandTree(){}
    public static LiteralCommandNode<CommandSourceStack> add(CommandDispatcher<CommandSourceStack> dispatcher,
            LiteralArgumentBuilder<CommandSourceStack> builder){
        var node=builder.build();
        dispatcher.register(Commands.literal("skywars"));
        // Brigadier returns the newly built node even when it merged with an
        // existing root; later children must be added to the canonical tree.
        dispatcher.getRoot().getChild("skywars").addChild(node);return node;
    }
}
