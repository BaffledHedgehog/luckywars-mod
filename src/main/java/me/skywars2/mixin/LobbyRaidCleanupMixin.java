package me.skywars2.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Raids.class)
public interface LobbyRaidCleanupMixin {
    @Accessor("raidMap") Int2ObjectMap<Raid> skywars2$raids();
}
