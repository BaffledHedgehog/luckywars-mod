package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.function.Predicate;
import me.skywars2.game.genie.GenieCommandAccess;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.CloneCommands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class GenieCommandMixin {
    private GenieCommandMixin() {}

    @Mixin(CommandSourceStack.class)
    public interface Source extends GenieCommandAccess.SourceAccess {
        @Override @Accessor("source") CommandSource skywars2$genieOutput();
    }

    @Mixin(BlockInput.class)
    public interface BlockPayload extends GenieCommandAccess.BlockAccess {
        @Override @Accessor("tag") CompoundTag skywars2$geniePayload();
    }

    @Mixin(EntitySelector.class)
    public abstract static class Selector {
        @WrapOperation(method={"findEntities", "findPlayers"},at=@At(value="INVOKE",
                target="Lnet/minecraft/commands/arguments/selector/EntitySelector;isWorldLimited()Z"))
        private boolean skywars2$localSearch(EntitySelector selector, Operation<Boolean> original,
                                            @Local(argsOnly=true) CommandSourceStack source) {
            return GenieCommandAccess.level(source)!=null || original.call(selector);
        }

        @WrapOperation(method="findEntities",at=@At(value="INVOKE",
                target="Lnet/minecraft/server/MinecraftServer;getAllLevels()Ljava/lang/Iterable;"))
        private Iterable<ServerLevel> skywars2$localLookup(MinecraftServer server, Operation<Iterable<ServerLevel>> original,
                                                           @Local(argsOnly=true) CommandSourceStack source) {
            var level=GenieCommandAccess.level(source);
            return level==null ? original.call(server) : List.of(level);
        }

        // Explicit player names and UUIDs bypass the general selector enumeration.
        @Inject(method={"findEntities", "findPlayers"},at=@At("RETURN"),cancellable=true)
        private void skywars2$localResults(CommandSourceStack source, CallbackInfoReturnable<List<? extends Entity>> cir) {
            var level=GenieCommandAccess.level(source);
            if(level!=null && cir.getReturnValue().stream().anyMatch(entity->entity.level()!=level))
                cir.setReturnValue(cir.getReturnValue().stream().filter(entity->entity.level()==level).toList());
        }
    }

    @Mixin(CloneCommands.class)
    public abstract static class Clone {
        // Native clone computes loaded absolute positions and collects all cells before its first write.
        @WrapOperation(method="clone",at=@At(value="INVOKE",target="Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z"))
        private static boolean skywars2$cloneSource(Predicate<BlockInWorld> predicate, Object input, Operation<Boolean> original,
                                                     @Local(argsOnly=true) CommandSourceStack source) throws CommandSyntaxException {
            boolean selected=original.call(predicate,input);
            var level=GenieCommandAccess.level(source);
            if(selected && level!=null) GenieCommandAccess.checkCloneSource((BlockInWorld)input,level);
            return selected;
        }

        @WrapOperation(method="clone",at=@At(value="INVOKE",
                target="Lnet/minecraft/server/level/ServerLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
        private static BlockState skywars2$cloneDestination(ServerLevel level, BlockPos position, Operation<BlockState> original,
                                                             @Local(argsOnly=true) CommandSourceStack source) throws CommandSyntaxException {
            var state=original.call(level,position);
            if(GenieCommandAccess.level(source)!=null) GenieCommandAccess.checkDestination(state);
            return state;
        }
    }
}
