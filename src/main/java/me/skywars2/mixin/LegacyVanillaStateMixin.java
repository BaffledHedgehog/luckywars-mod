package me.skywars2.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Recompute native caches and flags at the mode boundary without rewriting attribute base values. */
public final class LegacyVanillaStateMixin {
    private LegacyVanillaStateMixin(){}
    @Mixin(AttributeInstance.class)
    public interface Attribute {@Invoker("setDirty") void skywars2$invalidate();}
    @Mixin(LivingEntity.class)
    public interface Visibility {@Invoker("updateInvisibilityStatus") void skywars2$nativeVisibility();}
}
