package me.skywars2.mixin;

import me.skywars2.game.LbcSkills;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tick tail runs only after native item ticking has passed its pause guards. */
@Mixin(ItemEntity.class)
public abstract class LbcLooperItemMixin {
    @Inject(method="tick",at=@At("TAIL"))
    private void skywars2$looper(CallbackInfo ci){
        LbcSkills.groundTick((ItemEntity)(Object)this);
    }
}
