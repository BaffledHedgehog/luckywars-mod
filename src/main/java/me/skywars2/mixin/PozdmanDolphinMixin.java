package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserve source mob-function order before native dolphin movement and land impulses. */
@Mixin(Dolphin.class)
public abstract class PozdmanDolphinMixin {
    @Inject(method="tick",at=@At("HEAD"))
    private void skywars2$pozdmanActionBeforeTravel(CallbackInfo ci){
        var game=SkyWars2.game();
        if(game!=null)game.items().pozdmanBot().nativeDolphinTick((Dolphin)(Object)this);
    }
}
