package me.skywars2.game.genie;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.*;

/** Separates native gameplay commands from server management and external command execution. */
public final class GenieCommandAccess {
    private static final Set<String> GAMEPLAY = Set.of(
        "attribute", "clear", "clone", "damage", "data", "datapack", "effect", "enchant", "execute", "experience",
        "fill", "fillbiome", "give", "item", "kill", "locate", "loot", "msg", "particle", "playsound", "random",
        "return", "ride", "setblock", "spawnpoint", "spreadplayers", "stopsound", "summon", "tag", "teleport",
        "tell", "tellraw", "title", "tp", "w", "xp");
    private static final Set<String> EXTERNAL_EXECUTION = Set.of("function", "store", "in");

    private static final Set<String> CONTROL_IDS = Set.of(
        "command_block", "chain_command_block", "repeating_command_block", "command_block_minecart", "structure_block", "jigsaw");
    private static final String DENIED = "command is outside item gameplay authority";
    private static final SimpleCommandExceptionType NATIVE_DENIED = new SimpleCommandExceptionType(Component.literal(DENIED));

    public interface SourceAccess {
        CommandSource skywars2$genieOutput();
    }

    public interface BlockAccess {
        CompoundTag skywars2$geniePayload();
    }

    private record ScopedOutput(ServerLevel level, CommandSource delegate) implements CommandSource {
        @Override public void sendSystemMessage(Component message) { delegate.sendSystemMessage(message); }
        @Override public boolean acceptsSuccess() { return delegate.acceptsSuccess(); }
        @Override public boolean acceptsFailure() { return delegate.acceptsFailure(); }
        @Override public boolean shouldInformAdmins() { return false; }
    }

    private GenieCommandAccess() {}
    static List<String> commands() { return GAMEPLAY.stream().sorted().toList(); }

    static CommandSource scopedOutput(ServerLevel level, CommandSource delegate) {
        return new ScopedOutput(level, delegate);
    }

    public static ServerLevel level(CommandSourceStack source) {
        return ((SourceAccess) (Object) source).skywars2$genieOutput() instanceof ScopedOutput output ? output.level() : null;
    }

    static void check(ParseResults<CommandSourceStack> parsed, RootCommandNode<CommandSourceStack> root) {
        var source=parsed.getContext().getSource();
        var level=level(source);
        var ops=source.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        for (var context = parsed.getContext(); context != null; context = context.getChild()) {
            var nodes = context.getNodes();
            for (int i = 0; i < nodes.size(); i++) {
                var node = nodes.get(i).getNode();
                if (node instanceof ArgumentCommandNode<?,?> argument && argument.getType() instanceof DimensionArgument) {
                    var parsedDimension=context.getArguments().get(node.getName());
                    if (level==null || parsedDimension==null || !level.dimension().identifier().equals(parsedDimension.getResult()))
                        throw outsideScope();
                }
                if (!(node instanceof LiteralCommandNode<?>)) continue;
                String name = node.getName();
                var redirect = node.getRedirect();
                if (root.getChild(name) == node) requireGameplay(name, nodes, i);
                if (redirect != null && root.getChild(redirect.getName()) == redirect)
                    requireGameplay(redirect.getName(), nodes, i);
                if (EXTERNAL_EXECUTION.contains(name)) throw outsideScope();
            }
            for (var argument : context.getArguments().values()) checkArgument(argument.getResult(),ops);
        }
    }

    private static void checkArgument(Object value, RegistryOps<Tag> ops) {
        if (value instanceof BlockInput block) {
            if (controlBlock(block.getState().getBlock())) throw outsideScope();
            var payload=((BlockAccess) (Object) block).skywars2$geniePayload();
            if (payload!=null) checkPayload(payload);
        }
        if (value instanceof Holder<?> holder && holder.value()==EntityTypes.COMMAND_BLOCK_MINECART) throw outsideScope();
        if (value instanceof CompoundTag tag) checkPayload(tag);
        if (value instanceof Component text) checkPayload(ComponentSerialization.CODEC.encodeStart(ops,text).getOrThrow());
        if (value instanceof ItemInput item) {
            var type=item.item().value();
            if (type==Items.COMMAND_BLOCK_MINECART || type instanceof BlockItem block && controlBlock(block.getBlock())) throw outsideScope();
            if (!item.components().isEmpty())
                checkPayload(DataComponentPatch.CODEC.encodeStart(ops,item.components()).getOrThrow());
        }
    }

    public static void checkCloneSource(net.minecraft.world.level.block.state.pattern.BlockInWorld block,
                                        ServerLevel level) throws CommandSyntaxException {
        checkDestination(block.getState());
        var entity=block.getEntity();
        if (entity==null) return;
        try {
            checkPayload(entity.saveWithFullMetadata(level.registryAccess()));
        } catch (IllegalArgumentException denied) {
            throw NATIVE_DENIED.create();
        }
    }

    public static void checkDestination(net.minecraft.world.level.block.state.BlockState block) throws CommandSyntaxException {
        if (controlBlock(block.getBlock())) throw NATIVE_DENIED.create();
    }


    private static boolean controlBlock(Block block) {
        return block instanceof CommandBlock || block instanceof StructureBlock || block instanceof JigsawBlock;
    }

    private static void checkPayload(Tag root) {
        var pending=new ArrayDeque<Tag>();pending.add(root);
        while (!pending.isEmpty()) {
            switch (pending.removeLast()) {
                case CompoundTag tag -> {
                    if (tag.contains("Command") || "run_command".equals(tag.getStringOr("action", ""))) throw outsideScope();
                    String id=tag.getStringOr("id", "");
                    if (CONTROL_IDS.contains(id.startsWith("minecraft:")?id.substring("minecraft:".length()):id)) throw outsideScope();
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
        return new IllegalArgumentException(DENIED);
    }
}
