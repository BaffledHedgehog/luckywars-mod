package me.skywars2.mixin.visuals;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import me.skywars2.game.effects.SignedEffectVisuals;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import me.skywars2.game.GameController;

/** Covers command, portal and native dimension transfers without a second visual timer. */
@Mixin(ServerPlayer.class)
public abstract class DimensionPostEffectsMixin {
    @WrapMethod(method="setServerLevel")
    private void skywars2$worldProjection(ServerLevel destination,Operation<Void> original){
        var player=(ServerPlayer)(Object)this;
        var from=player.level();
        original.call(destination);
        var game=me.skywars2.SkyWars2.game();
        if(game!=null&&game.items()!=null)game.items().hedgehogDimensions().changedWorld(player,from,player.level());
        if(player.connection!=null){
            me.skywars2.game.presentation.projectiles.WeaponSpreadVisuals.forget(player);
            SignedEffectVisuals.synchronize(player);
        }
    }
}
