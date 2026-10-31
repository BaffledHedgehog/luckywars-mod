package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.skywars2.SkyWars2;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Preserve the released raw projectile's policy on the cloud constructed by vanilla. */
@Mixin(DragonFireball.class)
public abstract class SpecialFireballCloudMixin {
    @WrapOperation(method="onHit",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean skywars2$cloudSource(Level level,Entity cloud,Operation<Boolean> original){
        var game=SkyWars2.game();if(game!=null)game.items().specialFireballs().inherit((Entity)(Object)this,cloud);
        return original.call(level,cloud);
    }
}
