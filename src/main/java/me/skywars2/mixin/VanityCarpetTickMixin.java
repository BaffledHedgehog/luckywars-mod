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
        var game=SkyWars2.game();
        if(game!=null){
            var player=(ServerPlayer)(Object)this;
            if(game.vanity().blocked(player))ci.cancel();
            else if(game.carpetBots().recover(player))ci.cancel();
            else game.carpetBots().tick(player);
        }
    }
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="die",at=@At(value="INVOKE",
            target="Lcarpet/patches/EntityPlayerMPFake;kill(Lnet/minecraft/network/chat/Component;)V"),remap=false)
    private void skywars2$keepRemainingLives(@org.spongepowered.asm.mixin.injection.Coerce ServerPlayer player,
            net.minecraft.network.chat.Component message,com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original){
        var game=SkyWars2.game();if(game==null||!game.carpetBots().died(player))original.call(player,message);
    }

}
