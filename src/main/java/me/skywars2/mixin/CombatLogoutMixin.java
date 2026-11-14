package me.skywars2.mixin;

import me.skywars2.game.CombatLogout;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A departing combatant drops every carried stack without changing the world's keep-inventory rule. */
@Mixin(Player.class)
public abstract class CombatLogoutMixin {
    @Inject(method="dropEquipment",at=@At("HEAD"),cancellable=true)
    private void skywars2$forfeitInventory(ServerLevel level,CallbackInfo callback){
        if((Object)this instanceof ServerPlayer player&&CombatLogout.active(player)){
            player.getInventory().dropAll();callback.cancel();
        }
    }
}
