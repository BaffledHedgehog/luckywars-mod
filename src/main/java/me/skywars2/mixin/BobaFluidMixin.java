package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.util.Iterator;
import me.skywars2.game.BobaRegionPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native liquid source/level/slope/replacement rules, evaluated in a bounded map frame. */
@Mixin(FlowingFluid.class)
public abstract class BobaFluidMixin {
    @Shadow protected abstract boolean isSolidFace(BlockGetter level,BlockPos pos,Direction direction);
    @WrapMethod(method="tick")
    private void boba$tick(ServerLevel level,BlockPos pos,BlockState block,FluidState fluid,Operation<Void> original){
        int previous=BobaRegionPhysics.FLUID_FRAME.get();
        BobaRegionPhysics.FLUID_FRAME.set(BobaRegionPhysics.turns(level,pos));
        try{original.call(level,pos,block,fluid);}finally{BobaRegionPhysics.FLUID_FRAME.set(previous);}
    }
    @WrapMethod(method="getNewLiquid")
    private FluidState boba$newLiquid(ServerLevel level,BlockPos pos,BlockState state,Operation<FluidState> original){
        int previous=BobaRegionPhysics.FLUID_FRAME.get();
        BobaRegionPhysics.FLUID_FRAME.set(BobaRegionPhysics.turns(level,pos));
        try{return original.call(level,pos,state);}finally{BobaRegionPhysics.FLUID_FRAME.set(previous);}
    }
    @ModifyExpressionValue(method="*",at=@At(value="FIELD",target="Lnet/minecraft/core/Direction;DOWN:Lnet/minecraft/core/Direction;"))
    private static Direction boba$down(Direction value){return BobaRegionPhysics.rotate(value,BobaRegionPhysics.FLUID_FRAME.get());}
    @ModifyExpressionValue(method="*",at=@At(value="FIELD",target="Lnet/minecraft/core/Direction;UP:Lnet/minecraft/core/Direction;"))
    private static Direction boba$up(Direction value){return BobaRegionPhysics.rotate(value,BobaRegionPhysics.FLUID_FRAME.get());}
    @WrapOperation(method="*",at=@At(value="INVOKE",target="Lnet/minecraft/core/Direction$Plane;iterator()Ljava/util/Iterator;"))
    private Iterator<Direction> boba$plane(Direction.Plane plane,Operation<Iterator<Direction>> original){
        int turns=BobaRegionPhysics.FLUID_FRAME.get();if(turns==0)return original.call(plane);
        return java.util.Arrays.stream(new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST})
                .map(d->BobaRegionPhysics.rotate(d,turns)).iterator();
    }
    @WrapOperation(method="*",at=@At(value="INVOKE",target="Lnet/minecraft/core/BlockPos;below()Lnet/minecraft/core/BlockPos;"))
    private static BlockPos boba$below(BlockPos pos,Operation<BlockPos> original){return pos.relative(BobaRegionPhysics.rotate(Direction.DOWN,BobaRegionPhysics.FLUID_FRAME.get()));}
    @WrapOperation(method="*",at=@At(value="INVOKE",target="Lnet/minecraft/core/BlockPos;above()Lnet/minecraft/core/BlockPos;"))
    private static BlockPos boba$above(BlockPos pos,Operation<BlockPos> original){return pos.relative(BobaRegionPhysics.rotate(Direction.UP,BobaRegionPhysics.FLUID_FRAME.get()));}
    @WrapMethod(method="getFlow")
    private Vec3 boba$flowFrame(BlockGetter level,BlockPos pos,FluidState state,Operation<Vec3> original){
        int previous=BobaRegionPhysics.FLUID_FRAME.get();BobaRegionPhysics.FLUID_FRAME.set(BobaRegionPhysics.turns(level,pos));
        try{return original.call(level,pos,state);}finally{BobaRegionPhysics.FLUID_FRAME.set(previous);}
    }
    @Inject(method="getFlow",at=@At("HEAD"),cancellable=true)
    private void boba$flow(BlockGetter level,BlockPos pos,FluidState state,CallbackInfoReturnable<Vec3> ci){
        int turns=BobaRegionPhysics.turns(level,pos);if(turns==0)return;
        var self=(FlowingFluid)(Object)this;Vec3 flow=Vec3.ZERO;
        var down=BobaRegionPhysics.rotate(Direction.DOWN,turns);var up=down.getOpposite();
        for(var local:new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST}){
            var direction=BobaRegionPhysics.rotate(local,turns);var neighborPos=pos.relative(direction);
            var neighbor=level.getFluidState(neighborPos);
            if(!neighbor.isEmpty()&&!neighbor.getType().isSame(self))continue;
            float height=neighbor.getOwnHeight(),distance=0;
            if(height==0){
                if(!level.getBlockState(neighborPos).is(BlockTags.BLOCKS_FLUID_FLOW)){
                    var below=level.getFluidState(neighborPos.relative(down));
                    if((below.isEmpty()||below.getType().isSame(self))&&below.getOwnHeight()>0)
                        distance=state.getOwnHeight()-(below.getOwnHeight()-.8888889F);
                }
            }else distance=state.getOwnHeight()-height;
            flow=flow.add(direction.getStepX()*distance,direction.getStepY()*distance,direction.getStepZ()*distance);
        }
        if(state.getValue(FlowingFluid.FALLING)){
            for(var local:new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST}){
                var d=BobaRegionPhysics.rotate(local,turns);var p=pos.relative(d);
                // isSolidFace's special UP comparison must use this frame as well.
                int previous=BobaRegionPhysics.FLUID_FRAME.get();BobaRegionPhysics.FLUID_FRAME.set(turns);
                boolean wall;try{wall=isSolidFace(level,p,d)||isSolidFace(level,p.relative(up),d);}finally{BobaRegionPhysics.FLUID_FRAME.set(previous);}
                if(wall){flow=flow.normalize().add(down.getStepX()*6,down.getStepY()*6,down.getStepZ()*6);break;}
            }
        }
        ci.setReturnValue(flow.normalize());
    }
    @WrapMethod(method="getHeight")
    private float boba$height(FluidState state,BlockGetter level,BlockPos pos,Operation<Float> original){
        int previous=BobaRegionPhysics.FLUID_FRAME.get();BobaRegionPhysics.FLUID_FRAME.set(BobaRegionPhysics.turns(level,pos));
        try{return original.call(state,level,pos);}finally{BobaRegionPhysics.FLUID_FRAME.set(previous);}
    }
    @Inject(method="getShape",at=@At("HEAD"),cancellable=true)
    private void boba$shape(FluidState state,BlockGetter level,BlockPos pos,CallbackInfoReturnable<VoxelShape> ci){
        int turns=BobaRegionPhysics.turns(level,pos);if(turns==0)return;
        var up=BobaRegionPhysics.rotate(Direction.UP,turns);
        double h=state.getType().isSame(level.getFluidState(pos.relative(up)).getType())?1:state.getOwnHeight();
        ci.setReturnValue(switch(turns){case 1->Shapes.box(0,0,0,1,1,h);case 2->Shapes.box(0,1-h,0,1,1,1);case 3->Shapes.box(0,0,1-h,1,1,1);default->Shapes.block();});
    }
    @Mixin(targets="net.minecraft.world.level.material.FlowingFluid$SpreadContext")
    public abstract static class SpreadContext {
        @Shadow @Final private BlockPos origin;
        @Inject(method="getCacheKey",at=@At("HEAD"),cancellable=true)
        private void boba$key(BlockPos pos,CallbackInfoReturnable<Short> ci){
            int turns=BobaRegionPhysics.FLUID_FRAME.get();if(turns==0)return;
            var relative=BobaRegionPhysics.rotate(new Vec3(pos.getX()-origin.getX(),pos.getY()-origin.getY(),pos.getZ()-origin.getZ()),-turns);
            ci.setReturnValue((short)((((int)relative.x+128)&255)<<8|(((int)relative.z+128)&255)));
        }
        @WrapOperation(method="*",at=@At(value="INVOKE",target="Lnet/minecraft/core/BlockPos;below()Lnet/minecraft/core/BlockPos;"))
        private BlockPos boba$below(BlockPos pos,Operation<BlockPos> original){return pos.relative(BobaRegionPhysics.rotate(Direction.DOWN,BobaRegionPhysics.FLUID_FRAME.get()));}
    }
    @Mixin(net.minecraft.world.level.material.WaterFluid.class)
    public abstract static class Water {
        @ModifyExpressionValue(method="canBeReplacedWith",at=@At(value="FIELD",target="Lnet/minecraft/core/Direction;DOWN:Lnet/minecraft/core/Direction;"))
        private Direction boba$replacementDown(Direction down){return BobaRegionPhysics.rotate(down,BobaRegionPhysics.FLUID_FRAME.get());}
    }
    @Mixin(net.minecraft.world.level.material.FluidState.class)
    public abstract static class State {
        @Inject(method="getAABB",at=@At("HEAD"),cancellable=true)
        private void boba$bounds(BlockGetter level,BlockPos pos,CallbackInfoReturnable<net.minecraft.world.phys.AABB> ci){
            if(BobaRegionPhysics.turns(level,pos)==0)return;
            var self=(FluidState)(Object)this;var shape=self.getShape(level,pos);
            ci.setReturnValue(shape.isEmpty()?null:shape.bounds().move(pos));
        }
    }

    @Mixin(net.minecraft.world.level.material.LavaFluid.class)
    public abstract static class Lava {
        @ModifyExpressionValue(method="spreadTo",at=@At(value="FIELD",target="Lnet/minecraft/core/Direction;DOWN:Lnet/minecraft/core/Direction;"))
        private Direction boba$mixingDown(Direction down){return BobaRegionPhysics.rotate(down,BobaRegionPhysics.FLUID_FRAME.get());}
    }

}
