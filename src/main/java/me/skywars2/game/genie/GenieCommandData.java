package me.skywars2.game.genie;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.ArgProvider;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.StorageDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Scope attached to the existing command data providers, including deferred execute-store writes. */
public final class GenieCommandData {
    private static final List<String> ENTITY_IDENTITY = List.of(Entity.TAG_ID, Entity.TAG_UUID, "Team");
    private static final List<String> BLOCK_IDENTITY = List.of("id", "x", "y", "z");

    public interface EntityAccess { Entity skywars2$genieDataEntity(); }
    public interface BlockAccess { BlockEntity skywars2$genieDataBlockEntity(); }

    private GenieCommandData() {}

    public static List<ArgProvider<DataAccessor>> wrap(List<ArgProvider<DataAccessor>> providers) {
        return providers.stream().<ArgProvider<DataAccessor>>map(Provider::new).toList();
    }

    private record Provider(ArgProvider<DataAccessor> delegate) implements ArgProvider<DataAccessor> {
        @Override public DataAccessor access(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            var source = context.getSource();
            var scope = GenieCommandAccess.level(source);
            if (scope == null) return delegate.access(context);
            GenieCommandAccess.checkLevel(source, source.getLevel());
            return new ScopedData(delegate.access(context), scope);
        }

        @Override public ArgumentBuilder<CommandSourceStack, ?> wrap(ArgumentBuilder<CommandSourceStack, ?> parent,
                Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> children) {
            return delegate.wrap(parent, children);
        }
    }

    private static final class ScopedData implements DataAccessor {
        private final DataAccessor delegate;
        private final ServerLevel level;
        private Map<String, Tag> identity;

        ScopedData(DataAccessor delegate, ServerLevel level) {
            this.delegate = delegate;
            this.level = level;
        }

        @Override public CompoundTag getData() throws CommandSyntaxException {
            var fields = checkLocalBody();
            // Native modify/remove mutate their input before calling setData. Always detach it
            // first, including CommandStorage and any shared item-component NBT in a local body.
            var result = delegate.getData().copy();
            var saved = new HashMap<String, Tag>();
            for (String field : fields) {
                var value = result.get(field);
                saved.put(field, value == null ? null : value.copy());
            }
            identity = saved;
            return result;
        }

        @Override public void setData(CompoundTag candidate) throws CommandSyntaxException {
            checkLocalBody();
            if (delegate instanceof StorageDataAccessor || identity == null) throw GenieCommandAccess.nativeDenial();
            if (delegate instanceof EntityAccess accessor) {
                var entity = accessor.skywars2$genieDataEntity();
                if (entity instanceof Player || entity.getType() == EntityTypes.COMMAND_BLOCK_MINECART)
                    throw GenieCommandAccess.nativeDenial();
                checkGeometry(candidate.get(Entity.TAG_POS), false);
                checkGeometry(candidate.get(Entity.TAG_MOTION), false);
                checkGeometry(candidate.get("Rotation"), true);
            } else if (delegate instanceof BlockAccess accessor) {
                var entity = accessor.skywars2$genieDataBlockEntity();
                GenieCommandAccess.checkDestination(level.getBlockState(entity.getBlockPos()));
            }
            for (var field : identity.entrySet()) {
                if (!Objects.equals(field.getValue(), candidate.get(field.getKey()))) throw GenieCommandAccess.nativeDenial();
            }
            try { GenieCommandAccess.checkPayload(candidate); }
            catch (IllegalArgumentException denied) { throw GenieCommandAccess.nativeDenial(); }
            delegate.setData(candidate);
        }

        private List<String> checkLocalBody() throws CommandSyntaxException {
            if (delegate instanceof EntityAccess accessor) {
                var entity = accessor.skywars2$genieDataEntity();
                if (entity.level() == level && level.getEntity(entity.getUUID()) == entity) return ENTITY_IDENTITY;
            } else if (delegate instanceof BlockAccess accessor) {
                var entity = accessor.skywars2$genieDataBlockEntity();
                var pos = entity.getBlockPos();
                if (entity.getLevel() == level && level.hasChunkAt(pos) && level.getBlockEntity(pos) == entity) return BLOCK_IDENTITY;
            } else if (delegate instanceof StorageDataAccessor) return List.of();
            throw GenieCommandAccess.nativeDenial();
        }

        private static void checkGeometry(Tag value, boolean floats) throws CommandSyntaxException {
            if (!(value instanceof ListTag list)) return;
            for (var coordinate : list) {
                if (coordinate instanceof NumericTag number
                        && (floats ? !Float.isFinite(number.floatValue()) : !Double.isFinite(number.doubleValue())))
                    throw GenieCommandAccess.nativeDenial();
            }
        }

        @Override public Component getModifiedSuccess() { return delegate.getModifiedSuccess(); }
        @Override public Component getPrintSuccess(Tag data) { return delegate.getPrintSuccess(data); }
        @Override public Component getPrintSuccess(NbtPathArgument.NbtPath path, double scale, int value) {
            return delegate.getPrintSuccess(path, scale, value);
        }
    }
}
