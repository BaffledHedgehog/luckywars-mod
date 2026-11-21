package me.skywars2.mixin.projectiles;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import me.skywars2.game.entities.projectiles.LbcClouds;

import java.util.function.Predicate;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ProjectileUtil.class)
public abstract class AcidProjectileSweepMixin {
    @WrapOperation(method="getHitResult",at=@At(value="INVOKE",target=
            "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/level/Level;"
                    +"Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;"
                    +"Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;"))
    private static EntityHitResult skywars2$clipAcidSearch(Level level,Entity source,Vec3 from,Vec3 end,
            AABB search,Predicate<Entity> canHit,float margin,Operation<EntityHitResult> original) {
        if(source instanceof LbcClouds.AcidProjectile && level instanceof ServerLevel) {
            // The native block/border hit has already shortened end. A target
            // outside this box cannot touch the surviving segment even with its pick margin.
            search=search.intersect(new AABB(from,end).inflate(margin+1e-7));
        }
        return original.call(level,source,from,end,search,canHit,margin);
    }
}
