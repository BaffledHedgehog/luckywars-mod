package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Cursor3D;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.CollisionGetter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Native collision shapes and border classification stay intact; absent vertical terrain is never enumerated. */
@Mixin(BlockCollisions.class)
public abstract class BlockCollisionHeightMixin {
    @Shadow @Final private CollisionGetter collisionGetter;
    @WrapOperation(method="<init>(Lnet/minecraft/world/level/CollisionGetter;Lnet/minecraft/world/phys/shapes/CollisionContext;Lnet/minecraft/world/phys/AABB;ZLjava/util/function/BiFunction;)V",
            at=@At(value="NEW",target="(IIIIII)Lnet/minecraft/core/Cursor3D;"))
    private Cursor3D skywars2$buildHeightCursor(int x0,int y0,int z0,int x1,int y1,int z1,Operation<Cursor3D> original,
                                               @Local(argsOnly=true) net.minecraft.world.phys.AABB box){
        // Keep one air row on each side: Cursor3D uses boundary-face types to admit large block shapes.
        // Compute in double before narrowing: native floor(hugeY)+1 can itself overflow the int cursor.
        double low=Math.max(Math.floor(box.minY-1e-7)-1,(double)collisionGetter.getMinY()-1);
        double high=Math.min(Math.floor(box.maxY+1e-7)+1,(double)collisionGetter.getMaxY()+1);
        if(low>high)return original.call(0,0,0,-1,-1,-1);
        return original.call(x0,(int)low,z0,x1,(int)high,z1);
    }
}
