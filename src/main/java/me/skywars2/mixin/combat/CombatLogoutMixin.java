package me.skywars2.mixin.combat;

import com.llamalad7.mixinextras.sugar.Local;

import me.skywars2.game.match.combat.CombatLogout;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native player damage and inventory hooks for combat logout. */
@Mixin(Player.class)
public abstract class CombatLogoutMixin {
    // Player returns here after consuming absorption, before recording any HP damage.
    @Inject(method = "actuallyHurt", at = @At(value = "RETURN", ordinal = 1))
    private void skywars2$recordAbsorption(ServerLevel level, DamageSource source, float amount,
                                           CallbackInfo callback, @Local(index = 5) float absorbed) {
        if (absorbed > 0 && Float.isFinite(absorbed) && (Object) this instanceof ServerPlayer player) {
            CombatLogout.recordAbsorbedHit(player, source);
        }
    }

    @Inject(method="dropEquipment",at=@At("HEAD"),cancellable=true)
    private void skywars2$forfeitInventory(ServerLevel level,CallbackInfo callback){
        if((Object)this instanceof ServerPlayer player&&CombatLogout.active(player)){
            player.getInventory().dropAll();callback.cancel();
        }
    }
}
