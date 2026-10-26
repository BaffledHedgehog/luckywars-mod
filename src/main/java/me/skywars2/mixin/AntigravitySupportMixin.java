package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.skywars2.SkyWars2;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.List;

/** Keep admitted riders collidable with their moving terrain, alongside native terrain/actors. */
@Mixin(Entity.class)
public abstract class AntigravitySupportMixin {
    @WrapOperation(method="collide",at=@At(value="INVOKE",target=
            "Lnet/minecraft/world/level/Level;getEntityCollisions(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<VoxelShape> skywars2$movingTerrain(Level level,Entity actor,AABB area,Operation<List<VoxelShape>> original) {
        var shapes=original.call(level,actor,area);var game=SkyWars2.game();
        return game==null?shapes:game.items().antigravity().collisions(actor,area,shapes);
    }
}
