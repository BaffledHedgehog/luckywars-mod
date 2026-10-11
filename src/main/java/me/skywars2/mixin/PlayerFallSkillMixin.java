package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Landing uses full fall distance, including landings that deal no health damage. */
@Mixin(Player.class)
public abstract class PlayerFallSkillMixin {
    @Inject(method = "causeFallDamage", at = @At("RETURN"))
    private void skywars2$landed(double distance, float modifier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        var game = SkyWars2.game();
        if (game != null && (Object) this instanceof ServerPlayer player && !player.getAbilities().mayfly) {
            game.landed(player, distance);
        }
    }
}
