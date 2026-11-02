package me.skywars2.mixin;

import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cloned hooks retain causal owners without claiming their player's primary rod ledger. */
@Mixin(FishingHook.class)
public abstract class CloneFishingLedgerMixin {
    @Inject(method="updateOwnerInfo(Lnet/minecraft/world/entity/projectile/FishingHook;)V",at=@At("HEAD"),cancellable=true)
    private void skywars2$preservePrimaryHook(FishingHook hook,CallbackInfo ci){
        if(((FishingHook)(Object)this).entityTags().contains("skywars2.cloned_fishing"))ci.cancel();
    }
}
