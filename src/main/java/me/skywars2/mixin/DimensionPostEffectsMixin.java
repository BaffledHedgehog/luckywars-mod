package me.skywars2.mixin;

import me.skywars2.game.SignedEffectVisuals;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;

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
            me.skywars2.game.WeaponSpreadVisuals.forget(player);
            SignedEffectVisuals.synchronize(player);
        }
    }
}
