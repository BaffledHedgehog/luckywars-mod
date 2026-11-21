package me.skywars2.mixin.genie;

import me.skywars2.SkyWars2;
import me.skywars2.game.genie.GenieItems;
import me.skywars2.game.genie.GenieRuntime;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.util.Prediction;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import me.skywars2.game.GameController;

/** Inventory restrictions are checked before native transactions consume or publish the lamp. */
@Mixin(AbstractContainerMenu.class)
public abstract class GenieInteractionMixin {
    @Inject(method="clicked",at=@At("HEAD"),cancellable=true)
    private void skywars2$pendingLamp(int slot,int button,ContainerInput input,Player player,CallbackInfo ci){
        var game=SkyWars2.game();
        if(game!=null&&player instanceof ServerPlayer owner&&game.items().genie().blockedClick(owner,(AbstractContainerMenu)(Object)this,slot,button,input))ci.cancel();
    }
    @Mixin(ServerCommonPacketListenerImpl.class)
    public abstract static class Dialog {
        @Inject(method="handleCustomClickAction",at=@At("HEAD"),cancellable=true)
        private void skywars2$genieReply(ServerboundCustomClickActionPacket packet,CallbackInfo ci){
            if(!packet.id().getNamespace().equals("skywars2")||!packet.id().getPath().startsWith("genie/"))return;
            var game=SkyWars2.game();
            if(game!=null&&(Object)this instanceof ServerGamePacketListenerImpl connection){
                PacketUtils.ensureRunningOnSameThread(packet,connection,game.server().packetProcessor());
                game.items().genie().click(connection.player,packet.id(),packet.payload());
            }
            ci.cancel();
        }
    }
    @Mixin(ServerPlayer.class)
    public abstract static class Drop {
        @Inject(method = "die", at = @At("HEAD"))
        private void skywars2$releaseWishBeforeDeath(DamageSource damage, CallbackInfo ci) {
            var game = SkyWars2.game();
            if (game != null) game.items().genie().beforeDeath((ServerPlayer)(Object)this);
        }

        @Inject(method="drop(Z)V",at=@At("HEAD"),cancellable=true)
        private void skywars2$noLampThrow(boolean all,CallbackInfo ci){
            var owner=(ServerPlayer)(Object)this;var game=SkyWars2.game();
            if(game!=null&&game.items().genie().locked(owner,owner.getInventory().getSelectedItem())){game.items().genie().refuse(owner);ci.cancel();}
        }
        @Inject(method="drop(Lnet/minecraft/world/item/ItemStack;ZLnet/minecraft/util/Prediction;)Lnet/minecraft/world/entity/item/ItemEntity;",at=@At("HEAD"),cancellable=true)
        private void skywars2$noExtractedLamp(ItemStack stack,boolean hand,Prediction prediction,CallbackInfoReturnable<ItemEntity> cir){
            var game=SkyWars2.game();if(game!=null&&game.items().genie().interceptDrop((ServerPlayer)(Object)this,stack))cir.setReturnValue(null);
        }
    }
    @Mixin(ServerGamePacketListenerImpl.class)
    public abstract static class EntityUse {
        @org.spongepowered.asm.mixin.Shadow public ServerPlayer player;
        @Inject(method="handleInteract",at=@At(value="INVOKE",target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",shift=At.Shift.AFTER),cancellable=true)
        private void skywars2$noPendingFrame(net.minecraft.network.protocol.game.ServerboundInteractPacket packet,CallbackInfo ci){
            var game=SkyWars2.game();if(game!=null&&game.items().genie().locked(player,player.getItemInHand(packet.hand()))){game.items().genie().refuse(player);ci.cancel();}
        }
        @Inject(method="handleSetCreativeModeSlot",at=@At(value="INVOKE",target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",shift=At.Shift.AFTER),cancellable=true)
        private void skywars2$noCreativeCopy(net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket packet,CallbackInfo ci){
            var game=SkyWars2.game();if(game!=null&&game.items().genie().creativeSlot(player,packet.slotNum(),packet.itemStack()))ci.cancel();
        }
    }
    @Mixin(ItemStack.class)
    public abstract static class Use {
        @Inject(method="<init>(Lnet/minecraft/core/Holder;ILnet/minecraft/core/component/DataComponentPatch;)V",at=@At("RETURN"))
        private void skywars2$restoreGiftPresentation(Holder<Item> item,int count,DataComponentPatch patch,CallbackInfo ci) {
            // Native saved stacks and container item templates both materialize here.
            var stack=(ItemStack)(Object)this;
            if (!patch.isEmpty() && GenieRuntime.matches(stack)) GenieItems.applyGiftPresentation(stack);
        }

        private boolean skywars2$locked(Player player){
            var game=SkyWars2.game();if(game==null||!(player instanceof ServerPlayer owner)||!game.items().genie().locked(owner,(ItemStack)(Object)this))return false;
            game.items().genie().refuse(owner);return true;
        }
        @Inject(method="use",at=@At("HEAD"),cancellable=true)
        private void skywars2$noPendingUse(Level level,Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResult> cir){if(skywars2$locked(player))cir.setReturnValue(InteractionResult.FAIL);}
        @Inject(method="useOn",at=@At("HEAD"),cancellable=true)
        private void skywars2$noPendingBlock(UseOnContext context,CallbackInfoReturnable<InteractionResult> cir){if(skywars2$locked(context.getPlayer()))cir.setReturnValue(InteractionResult.FAIL);}
        @Inject(method="interactLivingEntity",at=@At("HEAD"),cancellable=true)
        private void skywars2$noPendingTransfer(Player player,LivingEntity target,InteractionHand hand,CallbackInfoReturnable<InteractionResult> cir){if(skywars2$locked(player))cir.setReturnValue(InteractionResult.FAIL);}
    }
}
