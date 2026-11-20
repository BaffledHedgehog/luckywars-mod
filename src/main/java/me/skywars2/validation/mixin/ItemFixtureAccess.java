package me.skywars2.validation.mixin;

import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Test-edition access to state owned by native loot tables. */
public final class ItemFixtureAccess {
    private ItemFixtureAccess() {}

    @Mixin(LootTable.class)
    public interface Loot {
        @Accessor("randomSequence") Optional<Identifier> validation$sequence();
        @Accessor("pools") List<LootPool> validation$pools();
    }
}
