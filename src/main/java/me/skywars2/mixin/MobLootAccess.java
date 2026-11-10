package me.skywars2.mixin;

import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Mob.class)
public interface MobLootAccess {
    @Accessor("lootTable") void skywars2$lootTable(Optional<ResourceKey<LootTable>> table);
}
