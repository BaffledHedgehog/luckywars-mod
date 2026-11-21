package me.skywars2.mixin.combat;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import me.skywars2.game.GameController;

@Mixin(Player.class)
public abstract class TechnorunnerKatanaMixin {
    @Inject(method = "attack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
    private void skywars2$refreshKatanaDamage(Entity target, CallbackInfo callback) {
        var game = SkyWars2.game();
        if (game != null && (Object) this instanceof ServerPlayer player)
            game.items().shishEncounter().refreshKatanaAttack(player);
    }
}
