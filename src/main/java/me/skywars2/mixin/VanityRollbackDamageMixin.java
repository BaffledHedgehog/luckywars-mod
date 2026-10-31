package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Damage admission is separate from participant eligibility used by already accepted spells. */
@Mixin(LivingEntity.class)
public abstract class VanityRollbackDamageMixin {
    @Inject(method="hurtServer",at=@At("HEAD"),cancellable=true)
    private void skywars2$holdDamage(CallbackInfoReturnable<Boolean> cir){
        var game=SkyWars2.game();if(game!=null&&game.vanity().busy())cir.setReturnValue(false);
    }
    @Mixin(ServerPlayer.class)
    public abstract static class Players {
        @Inject(method="hurtServer",at=@At("HEAD"),cancellable=true)
        private void skywars2$holdPlayerDamage(CallbackInfoReturnable<Boolean> cir){
            var game=SkyWars2.game();if(game!=null&&game.vanity().blocked((ServerPlayer)(Object)this))cir.setReturnValue(false);
        }
    }
}
