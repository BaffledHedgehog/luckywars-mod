package me.skywars2.mixin;

import java.util.function.Consumer;
import me.skywars2.SkyWars2;
import me.skywars2.game.LbcProjectileLevelups;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SnowballItem;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hooks only genuine SnowballItem use; eggs/pearls/dispensers never get a direct speed trait. */
@Mixin(SnowballItem.class)
public abstract class ProjectileLevelupSpawnMixin {
    @WrapOperation(method="use",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectileFromRotation(Lnet/minecraft/world/entity/projectile/Projectile$ProjectileFactory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;FFF)Lnet/minecraft/world/entity/projectile/Projectile;"))
    private Projectile skywars2$nativeSnowball(Projectile.ProjectileFactory<Projectile> factory,ServerLevel level,ItemStack stack,
            LivingEntity owner,float offset,float speed,float uncertainty,Operation<Projectile> original){
        Projectile.ProjectileFactory<Projectile> marked=(world,source,item)->LbcProjectileLevelups.pendingSnowball(factory.create(world,source,item));
        return original.call(marked,level,stack,owner,offset,speed,uncertainty);
    }
    @Mixin(Projectile.class)
    public abstract static class Spawn {
        @Inject(method="spawnProjectile(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Consumer;)Lnet/minecraft/world/entity/projectile/Projectile;",
                at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
        private static void skywars2$finalSnowball(Projectile body,ServerLevel level,ItemStack item,Consumer<Projectile> configure,CallbackInfoReturnable<Projectile> ci){
            var game=SkyWars2.game();if(game!=null)game.items().projectileLevelups().beforeNativeSpawn(body);
            else body.entityTags().remove(LbcProjectileLevelups.SNOWBALL_PENDING);
        }
    }
}
