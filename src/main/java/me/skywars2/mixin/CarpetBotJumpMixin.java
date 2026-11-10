package me.skywars2.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** LocalPlayer normally enters airborne travel immediately; Carpet's one-shot jump needs that transition. */
@Pseudo
@Mixin(targets="carpet.helpers.EntityPlayerActionPack$ActionType$3",remap=false)
public abstract class CarpetBotJumpMixin {
    @Inject(method="execute",at=@At("RETURN"),remap=false)
    private void skywars2$nativeTakeoff(ServerPlayer player,@Coerce Object action,CallbackInfoReturnable<Boolean> ci){
        var game=me.skywars2.SkyWars2.game();
        if(game!=null&&game.carpetBots().managed(player)&&player.getDeltaMovement().y>0)player.setOnGround(false);
    }
}
