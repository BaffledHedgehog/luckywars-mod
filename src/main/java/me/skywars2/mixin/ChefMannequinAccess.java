package me.skywars2.mixin;

import net.minecraft.world.entity.decoration.Mannequin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Hide the native NPC subtitle in synchronized metadata, retaining the authored name. */
@Mixin(Mannequin.class)
public interface ChefMannequinAccess {
    @Invoker("setHideDescription") void skywars2$hideDescription(boolean hidden);
}
