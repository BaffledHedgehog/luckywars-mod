package me.skywars2.mixin;

import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.item.alchemy.PotionContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AreaEffectCloud.class)
public interface StaticChargeCloudAccess {
    @Accessor("potionContents") PotionContents skywars2$staticPotion();
    @Accessor("reapplicationDelay") void skywars2$staticReapplication(int ticks);
}
