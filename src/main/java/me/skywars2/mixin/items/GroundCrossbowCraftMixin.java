package me.skywars2.mixin.items;

import me.skywars2.game.items.weapons.ranged.LbcGroundCrossbowCrafts;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class GroundCrossbowCraftMixin {
    @Inject(method="tick",at=@At("HEAD"))
    private void skywars2$groundReforge(CallbackInfo ci){LbcGroundCrossbowCrafts.tick((ItemEntity)(Object)this);}
}
