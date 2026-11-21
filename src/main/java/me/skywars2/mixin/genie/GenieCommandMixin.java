package me.skywars2.mixin.genie;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import me.skywars2.game.genie.GenieCommandAccess;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.SlotSourceArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.CloneCommands;
import net.minecraft.server.commands.FillCommand;
import net.minecraft.server.commands.LootCommand;
import net.minecraft.server.commands.SetBlockCommand;
import net.minecraft.server.commands.item.ItemAccessor;
import net.minecraft.server.commands.item.ItemCommands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.slot.CountingModifier;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.ModifyArg;
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

    @Mixin(SetBlockCommand.class)
    public abstract static class SetBlock {
        @Inject(method="setBlock",at=@At("HEAD"))
        private static void skywars2$checkReplacement(CommandSourceStack source, BlockPos pos,
                BlockInput input, SetBlockCommand.Mode mode, Predicate<BlockInWorld> predicate, boolean strict,
                CallbackInfoReturnable<Integer> cir) throws CommandSyntaxException {
            if (GenieCommandAccess.level(source)==null) return;
            var level=source.getLevel();
            GenieCommandAccess.checkLevel(source,level);
            GenieCommandAccess.checkDestination(level.getBlockState(pos));
        }
    }

    @Mixin(targets="net.minecraft.server.commands.FillCommand$Mode")
    public interface FillMode {
        @Accessor("filter") FillCommand.Filter skywars2$genieFilter();
        @Accessor("affector") FillCommand.Affector skywars2$genieAffector();
    }

    @Mixin(FillCommand.class)
    public abstract static class Fill {
        @WrapMethod(method="fillBlocks")
        private static int skywars2$preflight(CommandSourceStack source, BoundingBox region, BlockInput target,
                @Coerce Object mode, Predicate<BlockInWorld> predicate, boolean strict,
                Operation<Integer> original) throws CommandSyntaxException {
            if (GenieCommandAccess.level(source)==null) return original.call(source,region,target,mode,predicate,strict);
            var level=source.getLevel();
            GenieCommandAccess.checkLevel(source,level);
            long area=(long)region.getXSpan()*region.getYSpan()*region.getZSpan();
            if (area>level.getGameRules().get(GameRules.MAX_BLOCK_MODIFICATIONS)||level.isDebug())
                return original.call(source,region,target,mode,predicate,strict);
            var from=new BlockPos(region.minX(),region.minY(),region.minZ());
            var to=new BlockPos(region.maxX(),region.maxY(),region.maxZ());
            if (!level.hasChunksAt(from,to)) throw BlockPosArgument.ERROR_NOT_LOADED.create();
            var operation=(FillMode)mode;
            for (var pos:BlockPos.betweenClosed(from,to)) {
                var existing=level.getBlockState(pos);
                if (GenieCommandAccess.controlState(existing)
                    && (predicate==null||predicate.test(new BlockInWorld(level,pos,false)))
                    && (operation.skywars2$genieAffector()!=FillCommand.Affector.NOOP
                        || operation.skywars2$genieFilter().filter(region,pos,target,level)!=null)) {
                    GenieCommandAccess.checkDestination(existing);
                }
            }
            // Neighbor updates can change a predicate during the native loop.
            Predicate<BlockInWorld> checked=cell->!GenieCommandAccess.controlState(cell.getState())
                && (predicate==null||predicate.test(cell));
            return original.call(source,region,target,mode,checked,strict);
        }
    }

    @Mixin(ItemCommands.class)
    public abstract static class ItemTransfer {
        @WrapMethod(method="getItems")
        private static Collection<ItemStack> skywars2$checkedItems(CommandSourceStack source, ItemAccessor<?> accessor,
                SlotSourceArgument.Result slots, net.minecraft.core.Holder<LootItemFunction> modifier,
                Operation<Collection<ItemStack>> original) throws CommandSyntaxException {
            try {
                var items=original.call(source,accessor,slots,modifier);
                GenieCommandAccess.checkItems(source,items);
                return items;
            } catch (GenieCommandAccess.DeniedItemModifier denied) {
                throw denied.commandError();
            }
        }

        @Inject(method="applyModifier",at=@At("RETURN"),cancellable=true)
        private static void skywars2$checkedModifier(CommandSourceStack source, LootItemFunction modifier,
                CallbackInfoReturnable<UnaryOperator<ItemStack>> cir) {
            if (GenieCommandAccess.level(source)!=null)
                cir.setReturnValue(GenieCommandAccess.itemModifier(source,cir.getReturnValue()));
        }
    }

    @Mixin(CountingModifier.class)
    public abstract static class ItemModify {
        @Shadow @Final private UnaryOperator<ItemStack> modifier;
        @Shadow private int updatedCount;

        @WrapMethod(method="accept(Lnet/minecraft/world/entity/SlotAccess;)V")
        private void skywars2$modifySlot(SlotAccess slot, Operation<Void> original) {
            if (modifier instanceof GenieCommandAccess.ScopedItemModifier scoped) {
                var changed=scoped.checked(slot.get());
                if (changed!=null && slot.set(changed)) updatedCount++;
            } else original.call(slot);
        }
    }

    @Mixin(LootCommand.class)
    public abstract static class Loot {
        @ModifyArg(method={"dropBlockLoot", "dropKillLoot", "drop"},index=1,at=@At(value="INVOKE",
                target="Lnet/minecraft/server/commands/LootCommand$DropConsumer;accept(Lcom/mojang/brigadier/context/CommandContext;Ljava/util/List;Lnet/minecraft/server/commands/CommandResponseTracker;)V"))
        private static List<ItemStack> skywars2$checkedLoot(List<ItemStack> drops,
                @Local(argsOnly=true) CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
            GenieCommandAccess.checkItems(context.getSource(),drops);
            return drops;
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
            if(selected && level!=null) {
                var block=(BlockInWorld)input;
                if (block.getLevel() instanceof ServerLevel nativeLevel) GenieCommandAccess.checkLevel(source,nativeLevel);
                GenieCommandAccess.checkCloneSource(block,level);
            }
            return selected;
        }

        @WrapOperation(method="clone",at=@At(value="INVOKE",
                target="Lnet/minecraft/server/level/ServerLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
        private static BlockState skywars2$cloneDestination(ServerLevel level, BlockPos position, Operation<BlockState> original,
                                                             @Local(argsOnly=true) CommandSourceStack source) throws CommandSyntaxException {
            var state=original.call(level,position);
            if(GenieCommandAccess.level(source)!=null) {
                GenieCommandAccess.checkLevel(source,level);
                GenieCommandAccess.checkDestination(state);
            }
            return state;
        }
    }
}
