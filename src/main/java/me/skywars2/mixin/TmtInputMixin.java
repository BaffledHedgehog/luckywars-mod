package me.skywars2.mixin;

import me.skywars2.game.LbcTmtTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native punch and sneak press boundaries. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class TmtInputMixin {
    @Shadow public ServerPlayer player;
    @Inject(method={"handleAttack","handlePunch"},at=@At(value="INVOKE",target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",shift=At.Shift.AFTER))
    private void skywars2$tmtLeft(CallbackInfo ci){LbcTmtTriggers.input(player,LbcTmtTriggers.Input.LEFT_CLICK);}
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method="handlePlayerInput")
    private void skywars2$tmtSneakEdge(net.minecraft.network.protocol.game.ServerboundPlayerInputPacket packet,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original){
        net.minecraft.network.protocol.PacketUtils.ensureRunningOnSameThread(packet,(ServerGamePacketListenerImpl)(Object)this,player.level());
        boolean was=player.isShiftKeyDown();original.call(packet);
        if(!was&&player.isShiftKeyDown())LbcTmtTriggers.input(player,LbcTmtTriggers.Input.SNEAK_PRESS);
    }
    @Mixin(net.minecraft.world.entity.Entity.class)
    public abstract static class UseModifier {
        @Inject(method="isShiftKeyDown",at=@At("HEAD"),cancellable=true)
        private void skywars2$tmtActivationModifier(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir){
            if(LbcTmtTriggers.suppressUseModifier((net.minecraft.world.entity.Entity)(Object)this))cir.setReturnValue(false);
        }
    }
    @Mixin(LivingEntity.class)
    public abstract static class SourceView {
        @Inject(method="getItemBySlot",at=@At("HEAD"),cancellable=true)
        private void skywars2$tmtCapturedSource(net.minecraft.world.entity.EquipmentSlot slot,
                org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<ItemStack> cir){
            var source=LbcTmtTriggers.readHeld((LivingEntity)(Object)this,slot);
            if(source!=null)cir.setReturnValue(source);
        }
    }
}
