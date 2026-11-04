package me.skywars2.mixin;

import java.util.*;
import java.util.function.Predicate;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import me.skywars2.game.LbcBedwarsArena;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

/** Native template/clone all-cell preflight before temporary barriers, source clearing or BE loads. */
public final class BedwarsCopyProtectionMixin {
    private BedwarsCopyProtectionMixin(){}
    @Mixin(StructureTemplate.class)
    public abstract static class Template {
        @WrapMethod(method="placeInWorld")
        private boolean skywars2$copyTransaction(ServerLevelAccessor level,BlockPos at,BlockPos reference,StructurePlaceSettings settings,
                net.minecraft.util.RandomSource random,int flags,Operation<Boolean> original){
            var arena=LbcBedwarsArena.at(level);if(arena==null||!arena.active())return original.call(level,at,reference,settings,random,flags);
            try(var transaction=arena.nativeTransaction()){
                try{return original.call(level,at,reference,settings,random,flags);}catch(LbcBedwarsArena.Denied denied){return false;}
            }
        }
        @ModifyExpressionValue(method="placeInWorld",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;processBlockInfos(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;Ljava/util/List;)Ljava/util/List;"))
        private List<StructureTemplate.StructureBlockInfo> skywars2$processedCells(List<StructureTemplate.StructureBlockInfo> processed,
                @Local(argsOnly=true) ServerLevelAccessor level,@Local(argsOnly=true) StructurePlaceSettings settings){
            var arena=LbcBedwarsArena.at(level);if(arena==null||!arena.active())return processed;
            var changes=new LinkedHashMap<BlockPos,BlockState>();var bounds=settings.getBoundingBox();
            for(var cell:processed)if(bounds==null||bounds.isInside(cell.pos()))changes.put(cell.pos(),cell.state().mirror(settings.getMirror()).rotate(settings.getRotation()));
            arena.admitCopy(level,changes);return processed;
        }
    }
    @Mixin(targets="net.minecraft.server.commands.CloneCommands$DimensionAndPosition")
    public interface DimensionPosition {
        @Accessor("dimension") ServerLevel skywars2$level();
        @Accessor("position") BlockPos skywars2$position();
    }
    @Mixin(net.minecraft.server.commands.CloneCommands.class)
    public abstract static class Clone {
        @WrapMethod(method="clone")
        private static int skywars2$cloneTransaction(CommandSourceStack source,@Coerce Object start,@Coerce Object end,@Coerce Object destination,
                Predicate<BlockInWorld> predicate,@Coerce Object mode,boolean strict,Operation<Integer> original) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
            var from=(DimensionPosition)start;var last=(DimensionPosition)end;var to=(DimensionPosition)destination;
            var arena=LbcBedwarsArena.at(from.skywars2$level());if(arena==null||!arena.active())return original.call(source,start,end,destination,predicate,mode,strict);
            var box=BoundingBox.fromCorners(from.skywars2$position(),last.skywars2$position());
            long volume=(long)box.getXSpan()*box.getYSpan()*box.getZSpan();
            if(volume>source.getLevel().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MAX_BLOCK_MODIFICATIONS)
                    ||!from.skywars2$level().hasChunksAt(from.skywars2$position(),last.skywars2$position())
                    ||!to.skywars2$level().hasChunksAt(to.skywars2$position(),to.skywars2$position().offset(box.getLength())))
                return original.call(source,start,end,destination,predicate,mode,strict);
            var changes=new LinkedHashMap<BlockPos,BlockState>();var removals=new LinkedHashMap<BlockPos,BlockState>();
            var offset=to.skywars2$position().subtract(new BlockPos(box.minX(),box.minY(),box.minZ()));boolean move=((Enum<?>)mode).name().equals("MOVE");
            for(var cursor:BlockPos.betweenClosed(box.minX(),box.minY(),box.minZ(),box.maxX(),box.maxY(),box.maxZ())){
                var pos=cursor.immutable();var cell=new BlockInWorld(from.skywars2$level(),pos,false);
                if(predicate.test(cell)){changes.put(pos.offset(offset),cell.getState());if(move)removals.put(pos,Blocks.AIR.defaultBlockState());}
            }
            try(var transaction=arena.nativeTransaction()){
                try{
                    if(move)arena.admitCopy(from.skywars2$level(),removals);
                    arena.admitCopy(to.skywars2$level(),changes);
                    return original.call(source,start,end,destination,predicate,mode,strict);
                }catch(LbcBedwarsArena.Denied denied){return 0;}
            }
        }
    }
}
