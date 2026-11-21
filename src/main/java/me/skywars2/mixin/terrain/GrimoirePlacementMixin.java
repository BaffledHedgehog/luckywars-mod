package me.skywars2.mixin.terrain;

import me.skywars2.SkyWars2;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;

/** Only the dungeon's accepted main-thread placement bypasses neighbour/shape cascades. */
@Mixin(StructureTemplate.class)
public abstract class GrimoirePlacementMixin {
    private static boolean placing(){var game=SkyWars2.game();return game!=null&&game.server().isSameThread()&&game.items().grimoireDungeon().placingStructure();}
    @ModifyVariable(method="placeInWorld",at=@At("HEAD"),argsOnly=true)
    private StructurePlaceSettings skywars2$knownDungeonShape(StructurePlaceSettings settings){
        return placing()?settings.setKnownShape(true):settings;
    }
    @ModifyVariable(method="placeInWorld",at=@At("HEAD"),argsOnly=true)
    private int skywars2$strictDungeonWrites(int flags){return placing()?flags|Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS:flags;}
    @WrapOperation(method="placeInWorld",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;processBlockInfos(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;Ljava/util/List;)Ljava/util/List;"))
    private List<StructureTemplate.StructureBlockInfo> skywars2$clipDungeonBeforeProcessing(ServerLevelAccessor level,BlockPos origin,BlockPos reference,StructurePlaceSettings settings,List<StructureTemplate.StructureBlockInfo> input,Operation<List<StructureTemplate.StructureBlockInfo>> original){
        var box=settings.getBoundingBox();
        // The authored dungeon has only position-preserving ignore/jigsaw processors.
        // Never preclip quota/gravity/custom processors that can depend on the full palette.
        if(placing()&&box!=null&&settings.getProcessors().stream().allMatch(p->p instanceof BlockIgnoreProcessor||p instanceof JigsawReplacementProcessor)){
            var clipped=new ArrayList<StructureTemplate.StructureBlockInfo>();
            for(var row:input)if(box.isInside(StructureTemplate.calculateRelativePosition(settings,row.pos()).offset(origin)))clipped.add(row);
            input=clipped;
        }
        return original.call(level,origin,reference,settings,input);
    }
}
