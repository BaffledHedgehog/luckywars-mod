package me.skywars2.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Publish native hit motion state for the shared accepted raw-HP response. */
@Mixin(Entity.class)
public interface SymbioteTransferAccess {
    @Invoker("markHurt") void skywars2$transferMarkHurt();
}
