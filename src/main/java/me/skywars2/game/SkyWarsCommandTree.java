package me.skywars2.game;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/** Registers canonical children once; /sw redirects to the same /skywars tree. */
public final class SkyWarsCommandTree {
    private SkyWarsCommandTree(){}
    public static LiteralCommandNode<CommandSourceStack> add(CommandDispatcher<CommandSourceStack> dispatcher,
            LiteralArgumentBuilder<CommandSourceStack> builder){
        var node=builder.build();
        dispatcher.register(Commands.literal("skywars"));
        var root=dispatcher.getRoot().getChild("skywars");
        if(dispatcher.getRoot().getChild("sw")==null)
            dispatcher.register(Commands.literal("sw").requires(root::canUse)
                    .executes(context->dispatcher.execute("skywars",context.getSource())).redirect(root));
        // Brigadier returns the newly built node even when it merged with an
        // existing root; later children must be added to the canonical tree.
        root.addChild(node);return node;
    }
}
