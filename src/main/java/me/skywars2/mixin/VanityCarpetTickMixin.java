package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional Carpet actions run before vanilla player simulation and need the same input barrier. */
@Pseudo
@Mixin(targets="carpet.patches.EntityPlayerMPFake",remap=false)
public abstract class VanityCarpetTickMixin {
    @Inject(method="tick",at=@At("HEAD"),cancellable=true,remap=false)
    private void skywars2$holdActions(CallbackInfo ci){
        var game=SkyWars2.game();if(game!=null&&game.vanity().blocked((ServerPlayer)(Object)this))ci.cancel();
    }
}
