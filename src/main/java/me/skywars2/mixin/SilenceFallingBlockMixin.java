package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Falling blocks can enter a cleared Silence sphere after spawning outside it. */
@Mixin(FallingBlockEntity.class)
public abstract class SilenceFallingBlockMixin {
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void skywars2$eraseBeforeLanding(CallbackInfo ci) {
        var game=SkyWars2.game();
        if(game!=null && game.items().consuming().eraseLoaded((Entity)(Object)this)) ci.cancel();
    }
}
