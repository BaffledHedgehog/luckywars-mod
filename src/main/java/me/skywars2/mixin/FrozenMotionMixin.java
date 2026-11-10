package me.skywars2.mixin;

import me.skywars2.game.LbcFrozenMotion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class FrozenMotionMixin {
    @ModifyVariable(method="setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",at=@At("HEAD"),argsOnly=true)
    private Vec3 skywars2$lockedVelocity(Vec3 requested){
        return ((Entity)(Object)this).entityTags().contains(LbcFrozenMotion.LOCK)?Vec3.ZERO:requested;
    }
}
