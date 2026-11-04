package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Server name/chat and native TAB components. Vanilla overhead names remain team-formatted on clients. */
public final class CosmeticNameMixin {
    private CosmeticNameMixin() {}

    @Mixin(net.minecraft.world.entity.player.Player.class)
    public abstract static class Player {
        @Inject(method="getDisplayName",at=@At("RETURN"),cancellable=true)
        private void skywars2$individualName(CallbackInfoReturnable<Component> ci){
            if(!((Object)this instanceof ServerPlayer player))return;
            var game=SkyWars2.game();
            if(game!=null)ci.setReturnValue(game.items().cosmetics().displayName(player,ci.getReturnValue()));
        }
    }

    @Mixin(ServerPlayer.class)
    public abstract static class Tab {
        @Inject(method="getTabListDisplayName",at=@At("RETURN"),cancellable=true)
        private void skywars2$individualTabName(CallbackInfoReturnable<Component> ci){
            var game=SkyWars2.game();if(game==null)return;
            var player=(ServerPlayer)(Object)this;
            Component original=ci.getReturnValue();
            ci.setReturnValue(original==null?player.getDisplayName():game.items().cosmetics().displayName(player,original));
        }
    }
}
