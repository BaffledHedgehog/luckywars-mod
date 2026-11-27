package me.skywars2.mixin.projectiles;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.skywars2.game.entities.mobs.summons.LbcStand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import org.spongepowered.asm.mixin.Mixin;

/** Distinguishes native arrow delivery from raw potion/command status carriers. */
@Mixin(Arrow.class)
public abstract class StandArrowEffectMixin {
    @WrapMethod(method="doPostHurtEffects")
    private void skywars2$oneStandAttempt(LivingEntity target,Operation<Void> original){
        LbcStand.nativeArrowEffects((Arrow)(Object)this,()->original.call(target));
    }
}
