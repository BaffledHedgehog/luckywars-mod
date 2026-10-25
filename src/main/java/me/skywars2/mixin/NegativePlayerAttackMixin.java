package me.skywars2.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import me.skywars2.game.NegativeAttributeEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class NegativePlayerAttackMixin {
    @Inject(method="attack",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;onAttack()V"),cancellable=true)
    private void skywars2$negativeMeleeHeals(Entity target,CallbackInfo ci,
            @Local(ordinal=0) float base,@Local(ordinal=2) float magic,@Local(ordinal=1) float strength){
        if(NegativeAttributeEffects.playerAttack((Player)(Object)this,target,base,magic,strength))ci.cancel();
    }
    @Inject(method="getCurrentItemAttackStrengthDelay",at=@At("RETURN"),cancellable=true)
    private void skywars2$signedDelayNeverUnderflowsToZero(CallbackInfoReturnable<Float> cir){
        float delay=cir.getReturnValue();
        if(delay==0)cir.setReturnValue(Math.copySign(Float.MIN_NORMAL,delay));
        else if(Float.isNaN(delay))cir.setReturnValue(Float.POSITIVE_INFINITY);
    }
}
