package me.skywars2.game.genie;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.Holder;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.*;

/** Separates native gameplay commands from server management and external command execution. */
final class GenieCommandAccess {
    private static final Set<String> GAMEPLAY = Set.of(
        "attribute", "clear", "clone", "damage", "data", "datapack", "effect", "enchant", "execute", "experience",
        "fill", "fillbiome", "give", "item", "kill", "locate", "loot", "me", "msg", "particle", "playsound", "random",
        "return", "ride", "say", "setblock", "spawnpoint", "spreadplayers", "stopsound", "summon", "tag", "teleport",
        "tell", "tellraw", "title", "tp", "w", "xp");
    private static final Set<String> EXTERNAL_EXECUTION = Set.of("function", "store", "in");

    private GenieCommandAccess() {}
    static List<String> commands() { return GAMEPLAY.stream().sorted().toList(); }

    static void check(ParseResults<CommandSourceStack> parsed, RootCommandNode<CommandSourceStack> root) {
        for (var context = parsed.getContext(); context != null; context = context.getChild()) {
            var nodes = context.getNodes();
            for (int i = 0; i < nodes.size(); i++) {
                var node = nodes.get(i).getNode();
                if (!(node instanceof LiteralCommandNode<?>)) continue;
                String name = node.getName();
                var redirect = node.getRedirect();
                if (root.getChild(name) == node) requireGameplay(name, nodes, i);
                if (redirect != null && root.getChild(redirect.getName()) == redirect)
                    requireGameplay(redirect.getName(), nodes, i);
                if (EXTERNAL_EXECUTION.contains(name)) throw outsideScope();
            }
            for (var argument : context.getArguments().values()) checkArgument(argument.getResult());
        }
    }

    private static void checkArgument(Object value) {
        if (value instanceof BlockInput block && controlBlock(block.getState().getBlock())) throw outsideScope();
        if (value instanceof Holder<?> holder && holder.value()==EntityTypes.COMMAND_BLOCK_MINECART) throw outsideScope();
        if (value instanceof CompoundTag tag) checkEntityData(tag);
        if (value instanceof ItemInput item) {
            var type=item.item().value();
            if (type==Items.COMMAND_BLOCK_MINECART || type instanceof BlockItem block && controlBlock(block.getBlock())) throw outsideScope();
            for (var component : item.components().split().added()) {
                if (component.value() instanceof TypedEntityData<?> data) {
                    if (data.type()==EntityTypes.COMMAND_BLOCK_MINECART) throw outsideScope();
                    checkEntityData(data.copyTagWithoutId());
                }
            }
        }
    }

    private static boolean controlBlock(Block block) {
        return block instanceof CommandBlock || block instanceof StructureBlock || block instanceof JigsawBlock;
    }

    private static void checkEntityData(CompoundTag root) {
        var pending=new ArrayDeque<Tag>();pending.add(root);
        while (!pending.isEmpty()) {
            switch (pending.removeLast()) {
                case CompoundTag tag -> {
                    if (tag.contains("Command")) throw outsideScope();
                    String id=tag.getStringOr("id", "");
                    if (id.equals("command_block_minecart") || id.equals("minecraft:command_block_minecart")) throw outsideScope();
                    for (var key : tag.keySet()) {
                        var field=tag.get(key);
                        if (field instanceof CompoundTag || field instanceof ListTag) pending.add(field);
                    }
                }
                case ListTag list -> {
                    for (var field : list) if (field instanceof CompoundTag || field instanceof ListTag) pending.add(field);
                }
                default -> { }
            }
        }
    }

    private static void requireGameplay(String name, List<? extends com.mojang.brigadier.context.ParsedCommandNode<?>> nodes, int index) {
        String command = name.startsWith("minecraft:") ? name.substring("minecraft:".length()) : name;
        if (!GAMEPLAY.contains(command)) throw outsideScope();
        if (command.equals("datapack") || command.equals("data")) {
            String readAction = command.equals("datapack") ? "list" : "get";
            if (index + 1 >= nodes.size() || !nodes.get(index + 1).getNode().getName().equals(readAction)) throw outsideScope();
        }
    }

    private static IllegalArgumentException outsideScope() {
        return new IllegalArgumentException("command is outside item gameplay authority");
    }
}
