package me.skywars2.mixin.media;

import me.skywars2.SkyWars2;
import me.skywars2.game.items.phone.TikTokAccounts;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import me.skywars2.game.GameController;

/** Import replies use the same authenticated player and native thread boundary as build dialogs. */
@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class TikTokDialogMixin {
    @Inject(method="handleCustomClickAction",at=@At("HEAD"),cancellable=true)
    private void skywars2$tiktok(ServerboundCustomClickActionPacket packet,CallbackInfo ci){
        if(!packet.id().getNamespace().equals("skywars2")||!packet.id().getPath().startsWith("tiktok/"))return;
        var game=SkyWars2.game();
        if(game!=null&&(Object)this instanceof ServerGamePacketListenerImpl connection){
            PacketUtils.ensureRunningOnSameThread(packet,connection,game.server().packetProcessor());
            TikTokAccounts.click(connection.player,packet.id(),packet.payload());
        }
        ci.cancel();
    }
}
