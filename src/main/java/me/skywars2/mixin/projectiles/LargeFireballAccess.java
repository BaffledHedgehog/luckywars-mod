package me.skywars2.mixin.projectiles;

import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LargeFireball.class)
public interface LargeFireballAccess {
    @Accessor("explosionPower") int skywars2$explosionPower();
}
