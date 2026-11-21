package me.skywars2.mixin.loot;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Presence of legacy Thrower survives even when that player is offline. */
@Mixin(ItemEntity.class)
public interface TinyWandItemAccess {
    @Accessor("thrower") EntityReference<Entity> skywars2$thrower();
}
