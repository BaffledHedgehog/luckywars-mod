package me.skywars2.game.genie;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.StringJoiner;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Places the block portion of a native compressed structure in the owner's dimension. */
final class GenieStructures {
    private GenieStructures() {}
    private record Cell(BlockPos position, BlockState state) {}

    static JsonPrimitive place(ServerPlayer owner, String archive, JsonElement offset) {
        if (!archive.startsWith("nbt:") || archive.length() > 262148)
            throw new IllegalArgumentException("expected nbt: and a bounded Base64 gzip structure");
        var root = read(archive.substring(4));
        var dimensions = position(root.getListOrEmpty("size"));
        if (dimensions.getX() < 1 || dimensions.getY() < 1 || dimensions.getZ() < 1
                || dimensions.getX() > 64 || dimensions.getY() > 64 || dimensions.getZ() > 64)
            throw new IllegalArgumentException("structure axes must be between 1 and 64");
        if (!root.getListOrEmpty("entities").isEmpty())
            throw new IllegalArgumentException("use entity.launch for structure entities");
        var paletteTag = root.getListOrEmpty("palette");
        if (paletteTag.isEmpty()) paletteTag = root.getListOrEmpty("palettes").getListOrEmpty(0);
        if (paletteTag.isEmpty() || paletteTag.size() > 4096) throw new IllegalArgumentException("invalid structure palette");
        var level = owner.level();
        var registry = level.registryAccess().lookupOrThrow(Registries.BLOCK);
        var palette = new ArrayList<BlockState>(paletteTag.size());
        for (var element : paletteTag) {
            if (!(element instanceof CompoundTag tag)) throw new IllegalArgumentException("expected palette compound");
            String id = tag.getStringOr("Name", tag.getStringOr("id", ""));
            if (!id.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+")) throw new IllegalArgumentException("invalid structure block identifier");
            var properties = tag.getCompoundOrEmpty(tag.contains("Properties")?"Properties":"properties");
            var suffix = new StringJoiner(",", "[", "]");
            for (String key : properties.keySet()) {
                String value = properties.getStringOr(key, "");
                if (!key.matches("[a-z0-9_]+") || !value.matches("[a-z0-9_-]+"))
                    throw new IllegalArgumentException("invalid structure block property");
                suffix.add(key + "=" + value);
            }
            try {
                var block = BlockStateParser.parseForBlock(registry, id + suffix, false).blockState();
                if (GenieCommandAccess.controlState(block)) throw new IllegalArgumentException("control blocks cannot be placed");
                palette.add(block);
            } catch (CommandSyntaxException error) {
                throw new IllegalArgumentException("unknown structure block or state");
            }
        }
        var blockList = root.getListOrEmpty("blocks");
        if (blockList.isEmpty() || blockList.size() > 4096) throw new IllegalArgumentException("expect 1 to 4096 structure blocks");
        if (!offset.isJsonArray() || offset.getAsJsonArray().size() != 3)
            throw new IllegalArgumentException("structure offset must be [dx,dy,dz]");
        var delta = offset.getAsJsonArray();
        BlockPos base = owner.blockPosition();
        var anchor = new BlockPos(add(base.getX(), delta.get(0)), add(base.getY(), delta.get(1)), add(base.getZ(), delta.get(2)));
        var cells = new ArrayList<Cell>();
        var seen = new HashSet<BlockPos>();
        for (var element : blockList) {
            if (!(element instanceof CompoundTag tag) || tag.contains("nbt"))
                throw new IllegalArgumentException("block entity payloads are outside the structure API");
            var relative = position(tag.getListOrEmpty("pos"));
            int index = tag.getIntOr("state", -1);
            if (index < 0 || index >= palette.size() || relative.getX() < 0 || relative.getY() < 0 || relative.getZ() < 0
                    || relative.getX() >= dimensions.getX() || relative.getY() >= dimensions.getY() || relative.getZ() >= dimensions.getZ()
                    || !seen.add(relative)) throw new IllegalArgumentException("invalid or duplicate structure block");
            var block = palette.get(index);
            if (block.is(Blocks.STRUCTURE_VOID)) continue;
            BlockPos target = anchor.offset(relative);
            if (level.isOutsideBuildHeight(target) || !level.getWorldBorder().isWithinBounds(target) || !level.hasChunkAt(target)
                    || GenieCommandAccess.controlState(level.getBlockState(target))) return new JsonPrimitive(0);
            cells.add(new Cell(target, block));
        }
        int changed = 0;
        for (Cell cell : cells)
            if (level.setBlock(cell.position, cell.state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)) changed++;
        return new JsonPrimitive(changed);
    }

    private static int add(int coordinate, JsonElement offset) {
        if (!offset.isJsonPrimitive() || !offset.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("integer structure offset required");
        int result = Math.addExact(coordinate, offset.getAsBigDecimal().intValueExact());
        if (result < -30_000_000 || result > 30_000_000) throw new IllegalArgumentException("structure is outside world bounds");
        return result;
    }

    private static BlockPos position(ListTag tag) {
        if (tag.size() != 3) throw new IllegalArgumentException("three integer NBT coordinates required");
        return new BlockPos(tag.getIntOr(0,Integer.MIN_VALUE), tag.getIntOr(1,Integer.MIN_VALUE), tag.getIntOr(2,Integer.MIN_VALUE));
    }

    private static CompoundTag read(String encoded) {
        try {
            return NbtIo.readCompressed(new ByteArrayInputStream(Base64.getDecoder().decode(encoded)), NbtAccounter.create(2L*1024*1024));
        } catch (IOException | RuntimeException invalid) {
            throw new IllegalArgumentException("invalid compressed structure");
        }
    }
}
