package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Тонкие точки входа механик игрока: приземление и валидная попытка начать планирование. */
@Mixin(Player.class)
public abstract class PlayerMechanicsMixin {
    /** Удар не выполняется и при прямом нативном вызове, минующем обработчик клиентского пакета. */
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void skywars2$frozenAttack(Entity target, CallbackInfo ci) {
        var game = SkyWars2.game();
        if (game != null && (Object) this instanceof ServerPlayer player && game.items().actionsBlocked(player)) ci.cancel();
    }

    @Inject(method = "causeFallDamage", at = @At("RETURN"))
    private void skywars2$landed(double distance, float modifier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        var game = SkyWars2.game();
        if (game != null && (Object) this instanceof ServerPlayer player && !player.getAbilities().mayfly) {
            game.landed(player, distance);
        }
    }
}
